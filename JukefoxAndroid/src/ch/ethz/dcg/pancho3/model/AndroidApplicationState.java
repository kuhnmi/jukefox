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

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.ImportProgressListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryChangeDetectedListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.Progress;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;

public class AndroidApplicationState implements IAndroidApplicationStateController {

	public static final String TAG = AndroidApplicationState.class.getSimpleName();

	private AndroidCollectionModelManager collectionModel;

	public AndroidApplicationState(AndroidCollectionModelManager collectionModel) {
		super();
		this.collectionModel = collectionModel;
	}

	@Override
	public void setFirstStart(boolean b) {
		AndroidSettingsManager.getAndroidSettingsEditor().setFirstStart(b);
	}

	@Override
	public void addImportProgressListener(ImportProgressListener listener) {
		collectionModel.getLibraryImportManager().getImportState().addProgressListener(listener);
	}

	@Override
	public Progress getImportProgress() {
		return collectionModel.getLibraryImportManager().getImportState().getProgress();
	}

	@Override
	public int getNumbersOfSongsWithCoordinates() {
		try {
			return collectionModel.getOtherDataProvider().getNumberOfSongsWithCoordinates();
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return 0;
		}
	}

	@Override
	public boolean isBaseDataCommitted() {
		return collectionModel.getLibraryImportManager().getImportState().isBaseDataCommitted();
	}

	@Override
	public boolean isCoversFetched() {
		return collectionModel.getLibraryImportManager().getImportState().isCoversFetched();
	}

	@Override
	public boolean isFirstStart() {
		return AndroidSettingsManager.getAndroidSettingsReader().isFirstStart();
	}

	@Override
	public boolean isImporting() {
		return collectionModel.getLibraryImportManager().getImportState().isImporting();
	}

	@Override
	public boolean isMapDataCommitted() {
		return collectionModel.getLibraryImportManager().getImportState().isMapDataCommitted();
	}

	@Override
	public void waitForPlaybackFunctionality() {
		// TODO: replace this hack by event mechanism!!
		// OnPlaylistFunctionalityInitialized event (when registering, and it is
		// already initialized => invoke callback method on sender to make sure
		// the required actions are performed; otherwise, this will work like a
		// normal event).
		while (!JukefoxApplication.getPlayerController().isReady()) {
			try {
				JoinableThread.sleep(10);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public void addLibraryChangeDetectedListener(LibraryChangeDetectedListener listener) {
		collectionModel.getLibraryImportManager().addLibraryChangeDetectedListener(listener);
	}

	@Override
	public void removeLibraryChangeDetectedListener(LibraryChangeDetectedListener listener) {
		collectionModel.getLibraryImportManager().removeLibraryChangeDetectedListener(listener);
	}

	@Override
	public void removeImportProgressListener(ImportProgressListener listener) {
		collectionModel.getLibraryImportManager().getImportState().removeProgressListener(listener);
	}
}
