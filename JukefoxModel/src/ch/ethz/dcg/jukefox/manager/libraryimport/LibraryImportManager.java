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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.cache.ImportStateListener;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.manager.ResourceLoaderManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.DbAccessProvider;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;

public class LibraryImportManager implements CoordinateFetcherListener, ImportStateListener {

	private final static String TAG = LibraryImportManager.class.getSimpleName();

	private final ModifyProvider modifyProvider;
	private final DbAccessProvider dbAccessProvider;
	private final AbstractLibraryScanner libraryScanner;
	private final AbstractGenreManager genreManager;
	private final WebDataFetcher webDataFetcher;
	private final ImportState importState;
	private final DirectoryManager directoryManager;
	private final ModelSettingsManager modelSettingsManager;
	private final ResourceLoaderManager resourceLoaderManager;
	private CollectionPropertiesFetcherThread collectionPropertiesFetcher;

	private ImportStatistics importStatistics;
	private int baseDataSongInsertProgressMax;
	private int baseDataSongInsertProgressCnt;
	private LinkedHashSet<LibraryChangeDetectedListener> libraryChangeDetectedListeners;

	private boolean pendingImportRequest = false;
	private boolean pendingImportRequestedCleanDb = false;
	private boolean pendingImportRequestedReducedScan = true;

	public LibraryImportManager(AbstractLibraryScanner libraryScanner,
			IAlbumCoverFetcherThreadFactory coverFetcherThreadFactory,
			AbstractCollectionModelManager collectionModelManager,
			AbstractGenreManager genreManager, ImportState importState) {

		this.modifyProvider = collectionModelManager.getModifyProvider();
		this.dbAccessProvider = collectionModelManager.getDbAccessProvider();
		this.genreManager = genreManager;
		this.libraryScanner = libraryScanner;
		this.importState = importState;
		this.importState.addListener(this);
		this.directoryManager = collectionModelManager.getDirectoryManager();
		this.modelSettingsManager = collectionModelManager.getModelSettingsManager();
		this.resourceLoaderManager = collectionModelManager.getResourceLoaderManager();
		webDataFetcher = new WebDataFetcher(collectionModelManager, importState, coverFetcherThreadFactory);
		webDataFetcher.addCoordinateThreadListener(this);
		collectionPropertiesFetcher = new CollectionPropertiesFetcherThread(
				collectionModelManager.getOtherDataProvider(), collectionModelManager.getSongCoordinatesProvider(),
				importState);
		libraryChangeDetectedListeners = new LinkedHashSet<LibraryChangeDetectedListener>();
	}

	public void doImportAsync(final boolean clearDb, final boolean reduced) {
		Log.v(TAG, "Try to do import");
		JoinableThread importThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					pendingImportRequest = false;
					if (startImport(clearDb, reduced)) {
						pendingImportRequestedCleanDb = false;
						pendingImportRequestedReducedScan = true;
						return;
					}
					pendingImportRequestedCleanDb = pendingImportRequestedCleanDb | clearDb;
					pendingImportRequestedReducedScan = pendingImportRequestedReducedScan & reduced;
					pendingImportRequest = true;
				} catch (Throwable e) {
					Log.w(TAG, e);
					importStatistics.setThrowable(e);
					importState.setImportCompleted();
					importState.setImportProblem(e);
				}
			}

		});
		importThread.start();
	}

	/**
	 * Scan and Import all files in a given directory, ignoring other collection changes
	 * 
	 * @param directory
	 *            the directory to scan and import
	 */
	public void doImportAsync(final File directory) {
		Log.v(TAG, "Try to do import");
		JoinableThread importThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					pendingImportRequest = false;
					if (startImport(directory)) {
						pendingImportRequestedCleanDb = false;
						pendingImportRequestedReducedScan = true;
						return;
					}
					pendingImportRequest = true;
				} catch (Throwable e) {
					Log.w(TAG, e);
					importStatistics.setThrowable(e);
					importState.setImportCompleted();
					importState.setImportProblem(e);
				}
			}

		});
		importThread.start();
	}

	/**
	 * @return true if import was started, false otherwise
	 * @throws Exception
	 */
	private boolean startImport(boolean clearDb, boolean reduced) throws Exception {
		if (importState.isImporting()) {
			// make sure we have only one import at a time
			Log.v(TAG, "Import postponed because already running import");
			return false;
		}
		doImport(clearDb, reduced);
		return true;
	}

	/**
	 * @return true if import was started, false otherwise
	 * @throws Exception
	 */
	private boolean startImport(File directory) throws Exception {
		if (importState.isImporting()) {
			// make sure we have only one import at a time
			Log.v(TAG, "Import postponed because already running import");
			return false;
		}
		if (!directory.isDirectory()) {
			Log.v(TAG, "Import not executed because given directory is no directory: " + directory.getAbsolutePath());
			return false;
		}
		doImport(directory);
		return true;
	}

	private void doImport(boolean clearDb, boolean reduced) throws DataUnavailableException {
		Log.v(TAG, "doImport: clearDb: " + clearDb + ", reduced: " + reduced);
		importState.setImportStarted();
		importStatistics = createImportStatistics(clearDb, reduced);
		long scanStartTime = System.currentTimeMillis();

		if (clearDb) {
			reduced = false;
			doDbAndDirectoryCleaning();
		}

		boolean libraryChangeDetectedListenersInformed = false;

		if (reduced) {
			long redScanStartTime = System.currentTimeMillis();
			boolean changes = libraryScanner.reducedScan();
			Log.v(TAG, "reduced scan time: " + (System.currentTimeMillis() - redScanStartTime));
			Log.v(TAG, "changes found during reduced scan: " + changes);
			if (changes) {
				// there are changes => now it's worth to do a full scan...
				reduced = false;
				informLibraryChangeDetectedListeners();
				libraryChangeDetectedListenersInformed = true;
			} else {
				// Maybe we should fetch web data anyway???
				libraryScanner.clearData();
				importState.setImportCompleted();
				return;
			}
		}

		if (!reduced) {

			// Famous artists are inserted before the import really starts to avoid inconsistencies in the database.
			if (!modelSettingsManager.isFamousArtistsInserted()) {
				insertFamousArtists();
			} else {
				Log.v(TAG, "famous artists are already in the db.");
			}

			// Now the real import can start.
			libraryScanner.scan();

			long scanEndTime = System.currentTimeMillis();
			LibraryChanges libraryChanges = libraryScanner.getLibraryChanges();
			if (!libraryChangeDetectedListenersInformed && libraryChanges.hasChanges()) {
				informLibraryChangeDetectedListeners();
			}

			importState.setLibraryScanned(true, libraryChanges.hasChanges());
			Log.d(TAG, "library scanned. time: " + (scanEndTime - scanStartTime));

			if (libraryChanges.hasChanges()) {
				modelSettingsManager.incRecomputeTaskId();
			}

			libraryChanges.printLogD();

			commitBaseData(libraryChanges);
			importState.setBaseDataCommitted(true);
		}

		// Start the collection properties fetcher thread
		collectionPropertiesFetcher.start();

		// TODO: remove libraryChanges parameter, as this should be executed
		// anyways?
		boolean serverDataChanges = commitServerData(reduced);
		Log.v(TAG, "Server data changes: " + serverDataChanges);

		// Wait for the collectionPropertiesFetcher thread to finish
		try {
			collectionPropertiesFetcher.realJoin();
		} catch (InterruptedException e) {
			Log.w(TAG, e);
			throw new DataUnavailableException();
		}

		libraryScanner.clearData();

		// not task of the importer
		// setState(State.COMPUTING_CACHED_DATA);
		// computeCachedData();
	}

	private void doImport(File directory) throws DataUnavailableException {
		Log.v(TAG, "doImport: directory: " + directory);
		importState.setImportStarted();
		importStatistics = createImportStatistics(false, true);
		long scanStartTime = System.currentTimeMillis();

		boolean libraryChangeDetectedListenersInformed = false;

		// Famous artists are inserted before the import really starts to avoid inconsistencies in the database.
		if (!modelSettingsManager.isFamousArtistsInserted()) {
			insertFamousArtists();
		} else {
			Log.v(TAG, "famous artists are already in the db.");
		}

		// Now the real import can start.
		libraryScanner.scanDirectory(directory);

		long scanEndTime = System.currentTimeMillis();
		LibraryChanges libraryChanges = libraryScanner.getLibraryChanges();
		if (!libraryChangeDetectedListenersInformed && libraryChanges.hasChanges()) {
			informLibraryChangeDetectedListeners();
		}

		importState.setLibraryScanned(true, libraryChanges.hasChanges());
		Log.d(TAG, "library scanned. time: " + (scanEndTime - scanStartTime));

		if (libraryChanges.hasChanges()) {
			modelSettingsManager.incRecomputeTaskId();
		}

		libraryChanges.printLogD();

		commitBaseData(libraryChanges);
		importState.setBaseDataCommitted(true);

		// Start the collection properties fetcher thread
		collectionPropertiesFetcher.start();

		// TODO: remove libraryChanges parameter, as this should be executed
		// anyways?
		boolean serverDataChanges = commitServerData(false);
		Log.v(TAG, "Server data changes: " + serverDataChanges);

		// Wait for the collectionPropertiesFetcher thread to finish
		try {
			collectionPropertiesFetcher.realJoin();
		} catch (InterruptedException e) {
			Log.w(TAG, e);
			throw new DataUnavailableException();
		}

		libraryScanner.clearData();

		// not task of the importer
		// setState(State.COMPUTING_CACHED_DATA);
		// computeCachedData();
	}

	private void insertFamousArtists() {
		try {
			Log.v(TAG, "inserting famous artists...");
			resourceLoaderManager.loadFamousArtists();
			modelSettingsManager.setFamousArtistsInserted(true);
			Log.v(TAG, "famous artists inserted.");
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private ImportStatistics createImportStatistics(boolean clearDb, boolean reduced) {
		ImportStatistics importStatistics = new ImportStatistics();
		importStatistics.setClearDb(clearDb);
		importStatistics.setReduced(reduced);
		importStatistics.setStartTime(System.currentTimeMillis());
		int numberOfStartedImports = modelSettingsManager.getNumberOfStartedImports();
		numberOfStartedImports++;
		modelSettingsManager.setNumberOfStartedImports(numberOfStartedImports);
		importStatistics.setNumberOfStartedImports(numberOfStartedImports);
		return importStatistics;
	}

	public void addLibraryChangeDetectedListener(LibraryChangeDetectedListener l) {
		if (!libraryChangeDetectedListeners.contains(l)) {
			libraryChangeDetectedListeners.add(l);
		}
	}

	public void removeLibraryChangeDetectedListener(LibraryChangeDetectedListener l) {
		libraryChangeDetectedListeners.remove(l);
	}

	private void informLibraryChangeDetectedListeners() {
		Log.v(TAG, "informLibaryChangeDetectedListeners: numListeners: " + libraryChangeDetectedListeners.size());
		importStatistics.setHadChanges(true);
		for (LibraryChangeDetectedListener l : libraryChangeDetectedListeners) {
			Log.v(TAG, "listener: " + l.toString());
			l.onLibraryChangeDetected();
		}
	}

	private void doDbAndDirectoryCleaning() {
		dbAccessProvider.resetDatabase();
		modelSettingsManager.setFamousArtistsInserted(false);
		directoryManager.emptyCoverDirectory();
	}

	private void commitBaseData(LibraryChanges changes) {
		Log.v(TAG, "removing songs...");
		removeSongs(changes);
		Log.v(TAG, "songs removed.");

		Log.v(TAG, "inserting songs...");
		insertSongs(changes);
		Log.v(TAG, "songs inserted.");

		long startTime = System.currentTimeMillis();
		genreManager.updateGenres(changes);
		long endTime = System.currentTimeMillis();
		Log.v(TAG, "update genres completed. time: " + (endTime - startTime));
	}

	private void removeSongs(LibraryChanges changes) {
		try {
			removeSongSet(changes.getSongsToRemove());
			removeSongSet(changes.getSongsToChange());

			// only remove unused artists/albums if at least one song is changed/removed
			if (!(changes.getSongsToRemove().isEmpty() && changes.getSongsToChange().isEmpty())) {
				modifyProvider.removeUnusedAlbums();
				modifyProvider.updateUnusedArtists();
				Log.v(TAG, "removeSongs: set transaction successful");
			}
		} catch (DataWriteException e) {
			Log.w(TAG, "removeSongs: failed");
			Log.w(TAG, e);
		}
	}

	private void removeSongSet(Set<ImportSong> songs) {
		int count = 0;
		dbAccessProvider.beginTransaction();
		try {
			for (ImportSong s : songs) {
				Log.v(TAG, "removing song: " + s.getName());
				try {
					modifyProvider.removeSongById(s.getJukefoxId());
				} catch (DataWriteException e) {
					Log.w(TAG, e);
					// TODO: how to proceed...? We assume that songs that should
					// later be inserted are getting removed here (for songs to
					// change...)
				}
				count++;
				if (count >= 50) {
					count = 0;
					dbAccessProvider.setTransactionSuccessful();
					dbAccessProvider.endTransaction();
					JoinableThread.sleepWithoutThrowing(10);
					dbAccessProvider.beginTransaction();
				}
			}
			dbAccessProvider.setTransactionSuccessful();
		} finally {
			dbAccessProvider.endTransaction();
		}
	}

	private void insertSongs(LibraryChanges changes) {
		baseDataSongInsertProgressMax = changes.getSongsToAdd().size() + changes.getSongsToChange().size();
		baseDataSongInsertProgressCnt = 0;
		insertSongSet(changes.getSongsToChange(), changes.getContentProviderIdToJukefoxIdMap());
		insertSongSet(changes.getSongsToAdd(), changes.getContentProviderIdToJukefoxIdMap());
	}

	private void insertSongSet(Set<ImportSong> songs, HashMap<ContentProviderId, Integer> songIdMap) {
		// TODO set the import state properly and maybe insert songs in slabs of 50 songs to get some progress measure...
		try {
			modifyProvider.batchInsertSongs(songs);
		} catch (DataWriteException e1) {
			e1.printStackTrace();
		}

		for (ImportSong s : songs) {
			if (s.getContentProviderId() != null) {
				// add contentProviderId from new/changed songs to idMap if
				// exists
				songIdMap.put(s.getContentProviderId(), s.getJukefoxId());
			}
		}

		if (this != null) {
			return;
		}

		// old code, not running for now.

		int count = 0;
		dbAccessProvider.beginTransaction();
		try {
			Log.v(TAG, "inserting " + songs.size() + " songs into db");
			for (ImportSong s : songs) {
				try {
					int id = modifyProvider.insertSong(s);
					if (s.getContentProviderId() != null) {
						// add contentProviderId from new/changed songs to idMap if
						// exists
						songIdMap.put(s.getContentProviderId(), id);
					}
					// set/change new jukefoxId
					s.setJukefoxId(id);
				} catch (DataWriteException e) {
					Log.w(TAG, e);
					// TODO: what should we do with this song
				}
				count++;
				// there are 3 steps (prescan, scan, commit) for the base data
				// import (thus 3*songs.size())
				// 3rd step: progress is 2/3 + 1/3 * count / songs.size()
				importState.setBaseDataProgress(2 * baseDataSongInsertProgressMax + baseDataSongInsertProgressCnt,
						3 * baseDataSongInsertProgressMax, "Inserting: " + s.getName());
				baseDataSongInsertProgressCnt++;
				if (count >= 50) {
					count = 0;
					dbAccessProvider.setTransactionSuccessful();
					dbAccessProvider.endTransaction();
					JoinableThread.sleepWithoutThrowing(10);
					dbAccessProvider.beginTransaction();
				}
			}
			dbAccessProvider.setTransactionSuccessful();
		} finally {
			dbAccessProvider.endTransaction();
		}
	}

	/**
	 * @param libraryChanges
	 * @return true if there were changes, false otherwise.
	 */
	private boolean commitServerData(boolean reduced) {
		// if (!libraryChanges.hasChanges()) {
		// if (!settingsReader.isCommittingServerData()) {
		// Log.d(TAG, "no library changes => do not fetch server data.");
		// return;
		// }
		// }
		Log.d(TAG, "committing server data...");
		boolean hasChanges = webDataFetcher.fetchData(reduced);
		Log.d(TAG, "server data committed.");
		return hasChanges;
	}

	/**
	 * Listener method of coordinate thread
	 */
	@Override
	public void onCoordinateFetcherChangeDetected() {
		modelSettingsManager.incRecomputeTaskId();
	}

	/**
	 * Gets the import statistics
	 */
	public ImportStatistics getImportStatistics() {
		return importStatistics;
	}

	/**
	 * Gets the import state
	 */
	public ImportState getImportState() {
		return importState;
	}

	public void abortImportAsync() {
		importState.setAbortImport();
	}

	private void sendImportStats() {
		try {
			ImportStatistics stats = getImportStatistics();
			// DefaultHttpClient httpClient = Utils
			// .createHttpClientWithDefaultSettings();
			if (stats.isReduced() && !stats.hadChanges()) {
				Log.v(TAG, "reduced import without changes => don't send statistics.");
				return;
			}
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(Constants.FORMAT_IMPORT_STATS_URL);
			httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");

			Log.v(TAG, stats.getStatsString());
			httpPost.setEntity(new StringEntity(stats.getStatsString()));

			// Execute HTTP Post Request
			HttpResponse response = httpClient.execute(httpPost);
			String serverReply = EntityUtils.toString(response.getEntity());
			Log.v(TAG, "import stats sent: server-reply: " + serverReply);
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public void onAlbumCoversFetched() {

	}

	@Override
	public void onBaseDataCommitted() {

	}

	@Override
	public void onCoordinatesFetched() {
		// Restore data from the backup tables
		dbAccessProvider.restoreDataAfterCoordinatesFetched();
	}

	@Override
	public void onImportAborted(boolean hadChanges) {
		if (pendingImportRequest) {
			doImportAsync(pendingImportRequestedCleanDb, pendingImportRequestedReducedScan);
		}
	}

	@Override
	public void onImportCompleted(boolean hadChanges) {
		if (pendingImportRequest) {
			doImportAsync(pendingImportRequestedCleanDb, pendingImportRequestedReducedScan);
		}
		if (importStatistics != null) {
			int numberOfCompletedImports = modelSettingsManager.getNumberOfCompletedimports() + 1;
			importStatistics.setNumberOfCompletedImports(numberOfCompletedImports);
			modelSettingsManager.setNumberOfCompletedImports(numberOfCompletedImports);
			importStatistics.setEndTime(System.currentTimeMillis());
			sendImportStats();
		}
	}

	@Override
	public void onImportStarted() {

	}

	@Override
	public void onImportProblem(Throwable e) {

	}

}
