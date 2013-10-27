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
package ch.ethz.dcg.jukefox.controller;

import android.content.Context;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.model.IAndroidApplicationStateController;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class StartupManager {

	private final static String TAG = StartupManager.class.getSimpleName();

	private Controller controller;
	private Context appCtx;
	private IAndroidApplicationStateController appState;

	private JoinableThread preloadedDataThread;
	private JoinableThread libraryImportThread;

	public StartupManager(Controller controller, IAndroidApplicationStateController appState, Context appCtx) {
		this.controller = controller;
		this.appCtx = appCtx;
		this.appState = appState;

		// Timer t = new Timer();
		// t.schedule(new TimerTask() {
		//
		// @Override
		// public void run() {
		// StartupManager.this.controller.showStandardDialog("Attention: The accumulated sunlight you have received today has reached a critical limit!");
		// }
		//			
		// }, 10000, 20000);
	}

	public void start() {
		Log.d(TAG, "startup manager started.");

		// Log.d(TAG, "playback functionality initialized.");

		if (!AndroidUtils.isSdCardOk()) {
			// Wait a bit to see if i's a race condition like on startup of a
			// nexus s, where jukefox is started but the sdcard is only
			// available 1 or 2 seconds later
			for (int i = 0; i < 15; i++) {
				try {
					JoinableThread.sleep(1000);
				} catch (InterruptedException e) {
					Log.w(TAG, e);
				}
				if (AndroidUtils.isSdCardOk()) {
					break;
				}
			}
			if (!AndroidUtils.isSdCardOk()) {
				Log.d(TAG, "sd card problem.");
				// Don't show dialog as it is never useful (or is it?).
				// controller.showSdCardProblemDialog();
				return;
			}
		}

		// TODO: used for museek gallery
		// try {
		// if (!Utils.fileExists(Constants.TAG_FILENAME)) {
		// appState.writeTagsToSdCard();
		// }
		// } catch (Exception e) {
		// Log.w(TAG, e);
		// }

		// Check first start in PlayerActivity start
		// To make sure dialog is in front of player
		// if (appState.isFirstStart()) {
		// Log.d(TAG, "first start.");
		// controller.showFirstStartDialog();
		// }

		// if (appState.getSharedPreferencesVersion() <
		// Constants.SHARED_PREF_VERSION) {
		// appState.updateSharedPrefs();
		// }

		controller.performVersionChanges();

		initDirectories();
		Log.d(TAG, "directories initialized.");

		// if (appState.isFirstStart()) {
		// try {
		// appState.loadFamousArtists();
		// Log.d(TAG, "famous artists loaded.");
		// appState.loadTags();
		// Log.d(TAG, "tags loaded.");
		// appState.setFirstStart(false);
		// } catch (IOException e) {
		// Log.w(TAG, e);
		// } catch (CheckedSqlException e) {
		// Log.w(TAG, e);
		// }
		// }

		// own thread => responds with onCacheFileManagerCompleted
		Log.d(TAG, "init preloaded data...");
		// initPreloadedData();

		// own thread => responds with onBaseDataComitted and
		// onLibraryImportCompleted
		Log.d(TAG, "init library...");
		initLibrary();
	}

	// @Override
	// public void onPreloadedDataManagerCompleted() {
	// // TODO: show notification?
	// }

	// private void initPreloadedData() {
	// preloadedDataThread = new JoinableThread(new Runnable() {
	//
	// @Override
	// public void run() {
	// try {
	// // appState.addPreloadedDataManagerListener(StartupManager.this);
	// appState.loadPreloadedData();
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// controller.showPreloadedDataProblemDialog(e);
	// }
	// }
	// });
	// preloadedDataThread.start();
	// }

	private void initLibrary() {
		if (!controller.getSettingsReader().isAutomaticImports()) {
			return;
		}
		libraryImportThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					JukefoxApplication.getCollectionModel().getLibraryImportManager().doImportAsync(false, true); // flags:
					// don't
					// clear
					// db;
					// reduced import
				} catch (Throwable e) {
					Log.w(TAG, e);
					JukefoxApplication.getCollectionModel().getLibraryImportManager().getImportStatistics()
							.setThrowable(e);
					JukefoxApplication.getCollectionModel().getLibraryImportManager().getImportState()
							.setImportCompleted();
					controller.showLibraryImportProblemDialog(e);
				}
			}
		});
		libraryImportThread.start();
	}

	private void initDirectories() {
		if (JukefoxApplication.getDirectoryManager().isDirectoryMissing()) {
			try {
				JukefoxApplication.getDirectoryManager().deleteDirectories();
				JukefoxApplication.getDirectoryManager().createAllDirectories();
			} catch (Exception e) {
				Log.w(TAG, e);
				controller.showCouldNotCreateDirectoriesDialog();
			}
		}
	}

	// private void initDb() {
	// try {
	// appState.openDb();
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// controller.showCouldNotOpenDbDialog();
	// }
	// }

	// @Override
	// public void onBaseDataCommitted() {
	// controller.onBaseDataCommitted();
	// }

	// public void abortPreloadedDataLoading() {
	// State pdmState =
	// JukefoxApplication.getCollectionModel().getModifyProvider().getPreloadedDataManagerState();
	// if (pdmState == State.COMPUTING) {
	// JukefoxApplication.getCollectionModel().getModifyProvider().abortPreloadedDataManager();
	// }
	// if (pdmState != State.IDLE) {
	// try {
	// preloadedDataThread.realJoin(); // wait for the load to complete
	// } catch (InterruptedException e) {
	// Log.w(TAG, e);
	// }
	// }
	// }

}
