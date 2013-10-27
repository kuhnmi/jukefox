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
package ch.ethz.dcg.jukefox.model.libraryimport;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.cache.ImportStateListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.ImportProgressListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.Progress;

public class ImportState {

	public static final String TAG = ImportState.class.getSimpleName();

	private boolean importing;
	private boolean libraryScanned;
	private boolean baseDataCommitted;
	private boolean coordinatesFetched;
	private boolean coversFetched;
	private boolean mapDataCalculated;
	private boolean mapDataCommitted;
	private boolean collectionPropertiesCalculated;
	private boolean hasChanges;
	private boolean abortingImport = false;
	private List<ImportStateListener> listeners;
	private List<ImportProgressListener> progressListeners;
	private Progress progress;

	public ImportState() {
		listeners = new LinkedList<ImportStateListener>();
		progressListeners = new LinkedList<ImportProgressListener>();
		progress = new Progress("", false);
		setIdle();
	}

	public synchronized void setImportStarted() {
		Log.v(TAG, "set import started.");
		// addProgressListener(JukefoxApplication.getWakeLockManager());
		// JukefoxApplication.getWakeLockManager().acquireImportWakeLock();
		// startImportService();
		importing = true;
		baseDataCommitted = false;
		coordinatesFetched = false;
		coversFetched = false;
		mapDataCalculated = false;
		mapDataCommitted = false;
		collectionPropertiesCalculated = false;
		abortingImport = false;
		progress.reset();
		informListenersImportStarted();
	}

	public synchronized void setImportCompleted() {
		Log.v(TAG, "setting import completed...");
		// JukefoxApplication.getWakeLockManager().releaseImportWakeLock();
		// stopImportService();
		boolean hadChanges = this.hasChanges;
		setIdle();
		informListenersImportCompleted(hadChanges);
		informListenersProgressChanged();
		Log.v(TAG, "import completed set.");
	}

	public synchronized void setAbortImport() {
		abortingImport = true;
	}

	public synchronized boolean shouldAbortImport() {
		return abortingImport;
	}

	public synchronized void setImportAborted() {
		Log.v(TAG, "setting import aborted...");
		// JukefoxApplication.getWakeLockManager().releaseImportWakeLock();
		// startImportService();
		boolean hadChanges = this.hasChanges;
		setIdle();
		informListenersImportAborted(hadChanges);
		informListenersProgressChanged();
		Log.v(TAG, "import aborted set.");
	}

	private void setIdle() {
		importing = false;
		baseDataCommitted = true;
		coordinatesFetched = true;
		coversFetched = true;
		mapDataCalculated = true;
		mapDataCommitted = true;
		collectionPropertiesCalculated = true;
		hasChanges = false;
		abortingImport = false;
	}

	public synchronized boolean isImporting() {
		return importing;
	}

	public synchronized boolean isBaseDataCommitted() {
		return baseDataCommitted;
	}

	public synchronized void setBaseDataCommitted(boolean baseDataCommitted) {
		this.baseDataCommitted = baseDataCommitted;
		if (shouldAbortImport()) {
			setImportAborted();
			return;
		}
		informListenersBaseDataCommitted();
	}

	public void setBaseDataProgress(int progress, int maxProgress, String message) {
		this.progress.setBaseDataProgress(progress);
		this.progress.setBaseDataMaxProgress(maxProgress);
		this.progress.setStatusMessage(message);
		informListenersProgressChanged();
	}

	public synchronized boolean isCoordinatesFetched() {
		return coordinatesFetched;
	}

	public synchronized void setCoordinatesFetched(boolean coordinatesFetched) {
		Log.v(TAG, "setCoordinatesFetched(): " + coordinatesFetched);
		this.coordinatesFetched = coordinatesFetched;
		informListenersCoordinatesFetched();
	}

	public void setCoordinatesProgress(int progress, int maxProgress, String message) {
		this.progress.setCoordinatesProgress(progress);
		this.progress.setCoordinatesMaxProgress(maxProgress);
		this.progress.setStatusMessage(message);
		informListenersProgressChanged();
	}

	public synchronized boolean isCoversFetched() {
		return coversFetched;
	}

	public synchronized void setCoversFetched(boolean coversFetched) {
		Log.v(TAG, "setting covers fetched: " + coversFetched + " (map data committed: " + mapDataCommitted + ")");
		informListenersAlbumCoversFetched();
		if (mapDataCommitted && coversFetched && collectionPropertiesCalculated) {
			setImportCompleted();
			return;
		}
		this.coversFetched = coversFetched;
	}

	public void setCoversProgress(int progress, int maxProgress, String message) {
		this.progress.setAlbumArtProgress(progress);
		this.progress.setAlbumArtMaxProgress(maxProgress);
		this.progress.setStatusMessage(message);
		informListenersProgressChanged();
	}

	public synchronized boolean isMapDataCalculated() {
		return mapDataCalculated;
	}

	public synchronized void setMapDataCalculated(boolean mapDataCalculated) {
		Log.v(TAG, "setting map data calculated to " + mapDataCalculated);
		this.mapDataCalculated = mapDataCalculated;
		// Log.v(TAG, "map data calculated set.");
	}

	public void setMapDataProgress(int progress, int maxProgress, String message) {
		this.progress.setMapDataProgress(progress);
		this.progress.setMapDataMaxProgress(maxProgress);
		this.progress.setStatusMessage(message);
		informListenersProgressChanged();
	}

	public synchronized boolean isMapDataCommitted() {
		return mapDataCommitted;
	}

	public synchronized void setMapDataCommitted(boolean mapDataCommitted) {
		Log.v(TAG, "setting map data committed: " + mapDataCommitted + " (covers fetched: " + coversFetched + ")");
		if (coversFetched && mapDataCommitted && collectionPropertiesCalculated) {
			setImportCompleted();
			return;
		}
		this.mapDataCommitted = mapDataCommitted;
	}

	public synchronized boolean isCollectionPropertiesCalculated() {
		return collectionPropertiesCalculated;
	}

	public synchronized void setCollectionPropertiesCalculated(boolean collectionPropertiesCalculated) {
		Log.v(TAG, "Setting collection properties calculated to " + collectionPropertiesCalculated);
		if (mapDataCommitted && coversFetched && collectionPropertiesCalculated) {
			setImportCompleted();
			return;
		}
		this.collectionPropertiesCalculated = collectionPropertiesCalculated;
	}

	public void setCollectionPropertiesProgress(int progress, int maxProgress, String message) {
		this.progress.setCollectionPropertiesProgress(progress);
		this.progress.setCollectionPropertiesMaxProgress(maxProgress);
		this.progress.setStatusMessage(message);
		informListenersProgressChanged();
	}

	public synchronized void setLibraryScanned(boolean libraryScanned, boolean hasChanges) {
		// Log.v(TAG, "Library scanned");
		this.libraryScanned = libraryScanned;
		this.hasChanges = hasChanges;
	}

	public synchronized boolean isLibraryScanned() {
		return libraryScanned;
	}

	public synchronized void setImporting(boolean importing) {
		Log.v(TAG, "set import started.");
		this.importing = importing;
	}

	public void addListener(ImportStateListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ImportStateListener listener) {
		listeners.remove(listener);
	}

	private void informListenersBaseDataCommitted() {
		Log.v(TAG, "informing listeners base data comitted...");
		for (ImportStateListener l : listeners) {
			l.onBaseDataCommitted();
		}
		// Log.v(TAG, "listeners informed base data committed.");
	}

	private void informListenersCoordinatesFetched() {
		Log.v(TAG, "informing listeners coordinates fetched...");
		for (ImportStateListener l : listeners) {
			l.onCoordinatesFetched();
		}
		// Log.v(TAG, "listeners informed cooridnates fetched.");
	}

	private void informListenersAlbumCoversFetched() {
		Log.v(TAG, "informing listeners album covers fetched...");
		for (ImportStateListener l : listeners) {
			l.onAlbumCoversFetched();
		}
		// Log.v(TAG, "listeners informed album covers fetched.");
	}

	private void informListenersImportStarted() {
		Log.v(TAG, "informing import started...");
		for (ImportStateListener l : listeners) {
			l.onImportStarted();
		}
		// Log.v(TAG, "listeners informed import started.");
	}

	private void informListenersImportCompleted(boolean hadChanges) {
		Log.v(TAG, "informing import completed...");
		for (ImportStateListener l : listeners) {
			l.onImportCompleted(hadChanges);
		}
		// Log.v(TAG, "listeners informed import completed.");
	}

	private void informListenersImportAborted(boolean hadChanges) {
		Log.v(TAG, "informing import aborted...");
		for (ImportStateListener l : listeners) {
			l.onImportCompleted(hadChanges);
		}
		// Log.v(TAG, "listeners informed import completed.");
	}

	private synchronized void informListenersProgressChanged() {
		progress.setImportFinished(!importing);
		for (ImportProgressListener l : progressListeners) {
			if (l != null) {
				l.onProgressChanged(progress);
			}
		}
	}

	public synchronized void addProgressListener(ImportProgressListener listener) {
		progressListeners.add(listener);
	}

	public synchronized void removeProgressListener(ImportProgressListener listener) {
		Log.v(TAG, "remove progress listener. size before: " + progressListeners.size());
		progressListeners.remove(listener);
	}

	public Progress getProgress() {
		return progress;
	}

	public void setImportProblem(Throwable e) {
		informListenersAboutImportProblem(e);
	}

	private void informListenersAboutImportProblem(Throwable e) {
		Log.v(TAG, "informing about inport problem...");
		for (ImportStateListener l : listeners) {
			l.onImportProblem(e);
		}
	}

}
