/* 
 * Copyright 2008-2013, ETH Zürich, Samuel Welten, Michael Kuhn, Tobias Langner,
 * Sandro Affentranger, Lukas Bossard, Michael Grob, Rahul Jain, 
 * Dominic Langenegger, Sonia Mayor Alonso, Roger Odermatt, Tobias Schlueter,
 * Yannick Stucki, Sebastian Wendland, Samuel Zehnder, Samuel Zihlmann,       
 * Samuel Zweifel
 *
 * This file is part of Jukefox.
 *
 * Jukefox is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or any later version. Jukefox is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Jukefox. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.dcg.jukefox.data.db;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.SqlDbDataPortal.ISqlDbConnection;

/**
 * This helper class supports the SqlDbDataPortal instances by synchronizing the accesses to the db. This is very useful
 * for SQLite databases, since they throw an error if two connections simultaneously try to write to the db (or some
 * other combinations). This helper holds such conflicting operations back (instead of throwing an exception) and
 * schedules them for later.<br/>
 * <br/>
 * The SqlDbDataPortal is required to acquire a {@link LockType#SHARED}-lock whenever it tries to read from the
 * db-connection. Immediate transactions are required to acquire a {@link LockType#RESERVED}-lock and exclusive
 * transactions or write operations need to acquire a {@link LockType#EXCLUSIVE}-lock.<br/>
 * After the work is done, you need to release each lock. The order does not matter. Older locks kept locked until all
 * newer locks are released.<br/>
 * <br/>
 * If a transaction is started, locks kept locked until the transaction finishes (strict two-phase-locking).
 */
public final class LockHelper {

	private final static String TAG = LockHelper.class.getSimpleName();

	public enum LockType {
		SHARED(0), RESERVED(1), EXCLUSIVE(2);

		private int level;

		private LockType(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}
	}

	private enum DbLockType {
		UNLOCKED(0), // No lock around 
		SHARED(1), // Only read access locks  
		RESERVED(2), // Write-in-the-future lock; shared locks can coexist and can be created in the future 
		PENDING(3), // Write-outstanding lock; waits until all shared locks released. No new shared locks are allowed to be created 
		EXCLUSIVE(4); // Exclusive lock; no other locks are allowed to coexist

		private int level;

		private DbLockType(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}
	}

	/**
	 * Representation of a Thread-dbDataPortal pair which is used as key for all locking.
	 */
	public class LockHolder {

		public final Thread thread;
		public final ISqlDbConnection dbConnection;

		private LockHolder(Thread thread, ISqlDbConnection connection) {
			this.thread = thread;
			this.dbConnection = connection;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof LockHolder)) {
				return false;
			}

			final LockHolder other = (LockHolder) obj;

			boolean equals = true;
			equals &= (thread == null) ? (other.thread == null) : (thread.equals(other.thread));
			equals &= (dbConnection == null) ? (other.dbConnection == null) : (dbConnection.equals(other.dbConnection));

			return equals;
		}

		@Override
		public int hashCode() {
			return thread.hashCode() ^ dbConnection.hashCode();
		}
	}

	public class Lock {

		public final LockHolder lockHolder;
		public final LockType lockType;

		private boolean released = false;

		private Lock(LockHolder lockHolder, LockType lockType) {
			this.lockHolder = lockHolder;
			this.lockType = lockType;
		}

		/**
		 * Releases this lock.
		 * <ul>
		 * <li>If we are in a transaction or the top of the lock stack is not released yet (a newer lock is still open),
		 * this lock is just marked as released.</li>
		 * <li>If the top of the lockStack is marked as released and and locks that are marked as released are
		 * following, we are releasing them as well (this time for real).</li>
		 * </ul>
		 * This results in strict Two-phase-locking.
		 */
		public void release() {
			synchronized (LockHelper.this) {
				if (released) {
					throw new IllegalStateException("This lock has already been released.");
				}
				released = true;

				if (dbDataPortal.inTransaction()) {
					// We are in a transaction
					return;
				}

				// Get our lockStack
				Stack<Lock> lockStack = lockStacks.get(lockHolder);
				if (lockStack == null) {
					throw new IllegalStateException(
							"Internal error: Lock has to be released, but the lock stack is null!");
				}

				if (!lockStack.peek().released) {
					// There are locks that are newer than us, but are not released yet
					return;
				}

				// Release all locks which are marked as released
				while (!lockStack.isEmpty() && lockStack.peek().released) {
					Lock toBeReleasedLock = lockStack.pop();

					// Release the lock
					switch (toBeReleasedLock.lockType) {
						case SHARED:
							break;

						case RESERVED:
						case EXCLUSIVE:
							try {
								if (exclusiveLockStack.pop() != toBeReleasedLock.lockType) {
									throw new IllegalStateException(
											"Internal error: Exclusive lock has to be released, but its type does not match the top of the exclusive lock stack!");
								}
								if (exclusiveLockStack.isEmpty()) {
									assert (reservedLockCount == 0) && (exclusiveLockCount == 0);
									exclusiveLockHolder = null;
								}
								if (toBeReleasedLock.lockType == LockType.RESERVED) {
									--reservedLockCount;
								} else {
									--exclusiveLockCount;
								}
							} catch (EmptyStackException e) {
								throw new IllegalStateException(
										"Internal error: Exclusive lock has to be released, but the exclusive lock stack is empty!");
							}
					}
				}

				// Remove old lock stacks
				if (lockStack.isEmpty()) {
					lockStacks.remove(lockHolder);
				}

				// Inform that the lockType changed and the lock may now be free for someone
				LockHelper.this.notifyAll();
			}
		}
	}

	private final Map<LockHolder, Stack<Lock>> lockStacks = new HashMap<LockHolder, Stack<Lock>>();
	private final Set<LockHolder> pendingLockQueue = new HashSet<LockHolder>();
	private LockHolder exclusiveLockHolder = null;
	private int reservedLockCount = 0; // Number of reserved locks in exclusiveLockStack
	private int exclusiveLockCount = 0; // Nuber of exclusive locks in exclusiveLockStack
	private final Stack<LockType> exclusiveLockStack = new Stack<LockType>(); // lockType \in {RESERVED, EXCLUSIVE}

	private final SqlDbDataPortal<? extends IContentValues> dbDataPortal;

	public LockHelper(SqlDbDataPortal<? extends IContentValues> dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	/**
	 * Creates a lock of the given level.
	 * 
	 * @param lockType
	 *            The lock level
	 * @param dbConnection
	 *            On which connection the lock should be held
	 * @return The created lock
	 */
	public synchronized Lock lock(LockType lockType, ISqlDbConnection dbConnection) {
		// Create the lockHolder
		LockHolder lh = new LockHolder(Thread.currentThread(), dbConnection);

		// Get the lock
		boolean wait;
		do {
			// Get the db lock type
			final DbLockType dbLock = getDbLockType();

			// Check if we are the exclusive lock holder
			boolean weAreHoldingExclusive = (exclusiveLockHolder == null) || exclusiveLockHolder.equals(lh);

			/* Reduce an EXCLUSIVE lock to a RESERVED one if we are in an IMMEDIATE-transaction. This enables other connections to 
			 * continue reading from the db. */
			if ((dbLock == DbLockType.RESERVED) && weAreHoldingExclusive) {
				lockType = LockType.RESERVED;
			}

			wait = false;
			switch (lockType) {
				case SHARED:
					wait = (dbLock.level() > DbLockType.RESERVED.level()) // Exclusive lock around (RESERVED excl.)
							&& !weAreHoldingExclusive; // We are not holding it
					break;

				case RESERVED:
					wait = (dbLock.level() >= DbLockType.RESERVED.level()) // Exclusive lock around
							&& !weAreHoldingExclusive; // We are not holding it
					break;

				case EXCLUSIVE:
					wait = ((dbLock.level() >= DbLockType.RESERVED.level()) // Exclusive lock around
							&& !weAreHoldingExclusive) // We are not holding it
							//
							|| (!lockStacks.containsKey(lh) && lockStacks.size() > 0) // We hold no locks, others do
							|| (lockStacks.containsKey(lh) && lockStacks.size() > 1); // We hold locks, others do as well

					if (wait) {
						pendingLockQueue.add(lh);
					}

					break;
			}

			if (wait) {
				try {
					wait();
				} catch (InterruptedException e) {
					Log.w(TAG, e);
				}
			}
		} while (wait);

		/* Remove us from the pending queue, because if we are in it, we do not need to be afterwards, since we acquired 
		 * the EXCLUSIVE lock. */
		pendingLockQueue.remove(lh);

		// Register LockHolder
		switch (lockType) {
			case SHARED:
				break;

			case RESERVED:
			case EXCLUSIVE:
				exclusiveLockHolder = lh;
				exclusiveLockStack.add(lockType);
				if (lockType == LockType.RESERVED) {
					++reservedLockCount;
				} else {
					++exclusiveLockCount;
				}
				break;
		}

		// Add this lock to the lockChain of this lockHolder
		Stack<Lock> ourLockChain = lockStacks.get(lh);
		if (ourLockChain == null) {
			ourLockChain = new Stack<Lock>();
			lockStacks.put(lh, ourLockChain);
		}

		Lock lock = new Lock(lh, lockType);
		ourLockChain.add(lock);

		return lock;
	}

	/**
	 * Returns the lock type the database is actually in.
	 * 
	 * @return The lock type
	 */
	private synchronized DbLockType getDbLockType() {
		// Find the maximum exclusive lockType
		//int maxLockLevel = -1;
		LockType exclusiveLockType = null;
		if (reservedLockCount > 0) {
			exclusiveLockType = LockType.RESERVED;
		}
		if (exclusiveLockCount > 0) {
			exclusiveLockType = LockType.EXCLUSIVE;
		}
		/*for (LockType lockType : exclusiveLockStack) {
			if (lockType.level() > maxLockLevel) {
				maxLockLevel = lockType.level();

				exclusiveLockType = lockType;

				if (lockType == LockType.EXCLUSIVE) {
					break; // We can not get higher
				}
			}
		}*/

		// Get the dbLockType
		if ((exclusiveLockHolder != null) && (exclusiveLockType == LockType.EXCLUSIVE)) {
			return DbLockType.EXCLUSIVE;
		}

		/* Do not promote PENDING if there exists an IMMEDIATE-transaction. This helps to allow SHARED locks 
		 * in parallel to an IMMEDIATE-transaction to reduce waiting times if the IMMEDIATE-transaction takes
		 * some time. */
		if ((exclusiveLockHolder != null) && (exclusiveLockType == LockType.RESERVED)) {
			return DbLockType.RESERVED;
		}

		if (pendingLockQueue.size() > 0) {
			return DbLockType.PENDING;
		}

		if (lockStacks.size() > 0) {
			return DbLockType.SHARED;
		}

		return DbLockType.UNLOCKED;
	}
}
