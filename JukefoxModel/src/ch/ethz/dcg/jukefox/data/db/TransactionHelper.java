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

import java.util.Stack;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.LockHelper.Lock;
import ch.ethz.dcg.jukefox.data.db.SqlDbDataPortal.ISqlDbConnection;

/**
 * This helper class enables the usage of EXCLUSIVE and IMMEDIATE transactions for SQLite databases where no two
 * transactions are allowed to run at the same time and nested transactions are not supported.<br/>
 * <br/>
 * All transactions are run on the connection retreived by {@link SqlDbDataPortal#getTransactionConnection()}. This
 * enables read-only queries to continue their work when the running transaction is of type
 * {@link TransactionType#IMMEDIATE}.
 */
public final class TransactionHelper {

	private static final String TAG = TransactionHelper.class.getSimpleName();

	private final SqlDbDataPortal<? extends IContentValues> dbDataPortal;

	public enum TransactionType {
		EXCLUSIVE, IMMEDIATE
	}

	private TransactionType currentTransactionType = null;
	private final Stack<Lock> transactionLockStack = new Stack<Lock>();

	private boolean transactionIsSuccessful;
	private boolean innerTransactionIsSuccessful;

	public TransactionHelper(SqlDbDataPortal<? extends IContentValues> dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	/**
	 * Starts an immediate transaction.
	 * 
	 * @see IDbDataPortal#beginTransaction()
	 */
	public void beginTransaction() {
		beginTransaction(TransactionType.IMMEDIATE);
	}

	/**
	 * Starts an exclusive transaction.
	 * 
	 * @see IDbDataPortal#beginExclusiveTransaction()
	 */
	public void beginExclusiveTransaction() {
		beginTransaction(TransactionType.EXCLUSIVE);
	}

	/**
	 * Adopted from the android source.
	 * 
	 * @see The 2.2_r1.1 version of SQLiteDatabase.java
	 */
	private void beginTransaction(TransactionType transactionType) {
		// Get the transaction connection
		ISqlDbConnection transConnection = dbDataPortal.getTransactionConnection();

		// Acquire the db-lock
		switch (transactionType) {
			case IMMEDIATE:
				Lock lR = dbDataPortal.lockR(transConnection);
				transactionLockStack.add(lR);

				if (currentTransactionType == null) {
					currentTransactionType = TransactionType.IMMEDIATE; // Only set it to IMMEDIATE if not already set --> strict two-phase-locking
				}
				break;

			case EXCLUSIVE:
				Lock lX = dbDataPortal.lockX(transConnection);
				transactionLockStack.add(lX);
				currentTransactionType = TransactionType.EXCLUSIVE;
				break;
		}

		boolean ok = false;
		try {
			if (transactionLockStack.size() > 1) {
				// A transaction is already open -> reuse it
				if (innerTransactionIsSuccessful) {
					String msg = "Cannot call beginTransaction between "
							+ "calling setTransactionSuccessful and endTransaction";
					IllegalStateException e = new IllegalStateException(msg);
					Log.w(TAG, e);
					throw e;
				}
				Log.d(TAG, "Reusing transaction, nesting level = " + transactionLockStack.size());
			} else {
				// No transaction is open yet -> begin one now
				switch (transactionType) {
					case EXCLUSIVE:
						dbDataPortal.execSQLNoLock("BEGIN EXCLUSIVE;", transConnection);
						break;

					case IMMEDIATE:
						dbDataPortal.execSQLNoLock("BEGIN IMMEDIATE;", transConnection);
						break;
				}

				transactionIsSuccessful = true;
				innerTransactionIsSuccessful = false;
			}

			ok = true;
		} finally {
			if (!ok) {
				// exception occured -> unlock
				transactionLockStack.pop().release();
			}
		}
	}

	/**
	 * Adopted from the android source.
	 * 
	 * @see The 2.2_r1.1 version of SQLiteDatabase.java
	 */
	public void setTransactionSuccessful() {
		if (!inTransaction()) {
			throw new IllegalStateException("no transaction pending");
		}
		if (innerTransactionIsSuccessful) {
			throw new IllegalStateException(
					"setTransactionSuccessful may only be called once per call to beginTransaction");
		}
		innerTransactionIsSuccessful = true;
	}

	/**
	 * Adopted from the android source.
	 * 
	 * @see The 2.2_r1.1 version of SQLiteDatabase.java
	 */
	public void endTransaction() {
		if (!inTransaction()) {
			throw new IllegalStateException("no transaction pending");
		}

		if (innerTransactionIsSuccessful) {
			innerTransactionIsSuccessful = false;
		} else {
			transactionIsSuccessful = false;
		}

		if (transactionLockStack.size() > 1) {
			transactionLockStack.pop().release();
			return;
		}

		// Get the transaction connection
		ISqlDbConnection transConnection = dbDataPortal.getTransactionConnection();

		// Commit or abort the transcation
		if (transactionIsSuccessful) {
			dbDataPortal.execSQLNoLock("COMMIT;", transConnection);
		} else {
			try {
				dbDataPortal.execSQLNoLock("ROLLBACK;", transConnection);
			} catch (UncheckedSqlException e) {
				Log.d(TAG, "exception during rollback, maybe the DB previously performed an auto-rollback");
			}
		}

		// Release our lock
		transactionLockStack.pop().release();
		currentTransactionType = null;
	}

	/**
	 * Returns true, if the current thread is in a transaction.
	 * 
	 * @return If we are in a transaction
	 */
	public boolean inTransaction() {
		return !transactionLockStack.isEmpty()
				&& Thread.currentThread().equals(transactionLockStack.peek().lockHolder.thread);
	}

	/**
	 * Returns the transaction type of the current thread (null if no transaction is running).
	 * 
	 * @return The transaction type
	 */
	public TransactionType getTransactionType() {
		if (!inTransaction()) {
			return null;
		}

		return currentTransactionType;
	}
}
