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
package ch.ethz.dcg.pancho3.model;

import java.util.Timer;
import java.util.TimerTask;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import ch.ethz.dcg.jukefox.manager.libraryimport.ImportProgressListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.Progress;

/**
 * Manages the wake locks of the player and of the import process that ensure it
 * continues when the screen is switched off. The Import Wake lock is canceled
 * automatically in case the import does not make any progress for a certain
 * time to make sure that it does not use too much battery if the import process
 * is stuck
 */
public class WakeLockManager implements ImportProgressListener {

	public static final String TAG = WakeLockManager.class.getSimpleName();
	public static final int WAKE_LOCK_TIMEOUT = 3 * 60 * 1000;

	private WakeLock wlPlayer = null;
	private WakeLock wlImport = null;
	private long lastImportProgress = 0;
	private Timer cancelImportWakeLockTimer = null;

	public WakeLockManager(PowerManager pm) {
		wlPlayer = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerWakeLock");
		wlImport = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ImportWakeLock");
	}

	public void acquirePlayerWakeLock() {
		if (wlPlayer != null && !wlPlayer.isHeld()) {
			wlPlayer.acquire();
		}
	}

	public void releasePlayerWakeLock() {
		if (wlPlayer != null && wlPlayer.isHeld()) {
			wlPlayer.release();
		}
	}

	public void acquireImportWakeLock() {
		if (wlImport != null && !wlImport.isHeld()) {
			wlImport.acquire();
		}
		/** Ensures that wake lock will be released if nothing happens **/
		scheduleNewCancelImportTimer();
	}

	private void scheduleNewCancelImportTimer() {
		if (cancelImportWakeLockTimer != null) {
			cancelImportWakeLockTimer.cancel();
		}
		cancelImportWakeLockTimer = new Timer();
		cancelImportWakeLockTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (System.currentTimeMillis() - lastImportProgress > WAKE_LOCK_TIMEOUT) {
					releaseImportWakeLock();
				} else {
					scheduleNewCancelImportTimer();
				}
			}

		}, WAKE_LOCK_TIMEOUT);
	}

	public void releaseImportWakeLock() {
		if (wlImport != null && wlImport.isHeld()) {
			wlImport.release();
		}
	}

	@Override
	public void onProgressChanged(Progress progress) {
		lastImportProgress = System.currentTimeMillis();
	}
}
