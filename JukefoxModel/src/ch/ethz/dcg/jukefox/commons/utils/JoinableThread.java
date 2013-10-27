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
package ch.ethz.dcg.jukefox.commons.utils;

/**
 * Solves a bug in the Android 1.5 API (see https://code.google.com/p/android/issues/detail?id=6261). In particular,
 * join does not work correctly in Android 1.5, as it sometimes returns before the joined thread has finished execution.
 * Use this class instead of java.lang.Thread, and use the realJoin method to be sure that the joined thread has
 * finished execution.
 * 
 * @author kuhnmi
 * 
 */
public class JoinableThread extends java.lang.Thread {

	private static final String TAG = JoinableThread.class.getSimpleName();

	public JoinableThread() {
		super();
	}

	public JoinableThread(Runnable runnable, String threadName) {
		super(runnable, threadName);
	}

	public JoinableThread(Runnable runnable) {
		super(runnable);
	}

	public JoinableThread(String threadName) {
		super(threadName);
	}

	public JoinableThread(ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
		super(group, runnable, threadName, stackSize);
	}

	public JoinableThread(ThreadGroup group, Runnable runnable, String threadName) {
		super(group, runnable, threadName);
	}

	public JoinableThread(ThreadGroup group, Runnable runnable) {
		super(group, runnable);
	}

	public JoinableThread(ThreadGroup group, String threadName) {
		super(group, threadName);
	}

	public void realJoin() throws InterruptedException {
		while (isAlive()) {
			super.join();
			if (isAlive()) {
				// Log.v(TAG, "join leaked...");
			}
		}
	}

	public static void sleepWithoutThrowing(long time) {
		try {
			sleep(time);
		} catch (InterruptedException e) {
			// Log.w(TAG, e);
		}
	}
}
