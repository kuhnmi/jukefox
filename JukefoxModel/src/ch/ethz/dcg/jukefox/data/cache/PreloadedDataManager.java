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
package ch.ethz.dcg.jukefox.data.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.AdvancedKdTree;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.manager.ResourceLoaderManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import edu.wlu.cs.levy.CG.KeySizeException;

public class PreloadedDataManager implements MapDataCalculatorListener, ImportStateListener {

	private final static String TAG = PreloadedDataManager.class.getSimpleName();

	public enum State {
		IDLE, LOADING, COMPUTING
	}

	private final IDbDataPortal dbDataPortal;
	private final ImportState importState;
	private final LinkedList<PreloadedDataManagerListener> listeners;
	private PreloadedData data;
	private PreloadedAlbums preloadedAlbums;
	private ResourceLoaderManager resourceLoaderManager;
	private final ModelSettingsManager modelSettingsManager;

	private JoinableThread tagLoaderThread;
	private JoinableThread songCoordsLoaderThread;

	private State state;
	private boolean aborted;
	private JoinableThread mapAlbumWriterThread;
	private JoinableThread songPcaCoordsWriterThread;
	private JoinableThread tagsWriterThread;

	/**
	 * loader thread will set this exception if anything went wrong during load.
	 */
	private Exception loaderException; // TODO: is there a cleaner way to do
	private DirectoryManager directoryManager;

	// this?

	public PreloadedDataManager(IDbDataPortal dbDataPortal, ResourceLoaderManager resourceLoaderProvider,
			ImportState importState, ModelSettingsManager modelSettingsManager, DirectoryManager directoryManager) {
		this.dbDataPortal = dbDataPortal;
		this.resourceLoaderManager = resourceLoaderProvider;
		this.importState = importState;
		this.modelSettingsManager = modelSettingsManager;
		this.directoryManager = directoryManager;
		listeners = new LinkedList<PreloadedDataManagerListener>();
		this.importState.addListener(this);
	}

	public void addListener(PreloadedDataManagerListener listener) {
		listeners.add(listener);
	}

	public synchronized void loadData() throws DataUnavailableException {
		setState(State.LOADING);
		Log.d(TAG, "load preloaded data called");
		try {
			loaderException = null;
			PreloadedData data = new PreloadedData();
			PreloadedAlbums preloadedAlbums = new PreloadedAlbums(dbDataPortal);
			loadAlbums(preloadedAlbums); // own thread
			loadTags(data); // own thread
			loadSongCoordsAndPcaCoords(data); // own thread

			// throws loader exception if something went wrong during load
			joinLoaderThreads();

			if (aborted) {
				return;
			}

			// loadCacheFileData(data);
			setData(data, preloadedAlbums); // this call is synchronized
			Log.v(TAG, "preloaded data loaded.");
		} catch (Exception e) {
			Log.w(TAG, e);
			// Load albums to ensure a quick loading of the activity
			PreloadedAlbums preloadedAlbums = new PreloadedAlbums(dbDataPortal);
			preloadedAlbums.loadFromDb(false);
			this.preloadedAlbums = preloadedAlbums;
			modelSettingsManager.incRecomputeTaskId();
			importState.setMapDataCalculated(false);
			importState.setMapDataCommitted(false);
			Log.v(TAG, "start recompute due  to loader exception.");
			recomputeAsync(modelSettingsManager.getRecomputeTaskId());
		}
	}

	private void recomputeAsync(final int recomputeTaskId) {
		importState.setImporting(true);

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					recompute(recomputeTaskId);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				} finally {
					importState.setImporting(false);
				}
			}

		});
		t.start();
	}

	private void joinLoaderThreads() throws Exception {
		try {
			songCoordsLoaderThread.realJoin();
			tagLoaderThread.realJoin();
			if (loaderException != null) {
				throw loaderException;
			}
		} catch (InterruptedException e) {
			Log.w(TAG, e);
		}
	}

	private void loadSongCoordsAndPcaCoords(final PreloadedData data) throws Exception {
		songCoordsLoaderThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					List<PreloadedSongInfo> songInfos = dbDataPortal.getPreloadedSongInfo();
					processSongInfos(songInfos, data);
				} catch (Exception e) {
					Log.w(TAG, "setting loader exception");
					Log.w(TAG, e);
					loaderException = e;
				}
			}
		});
		songCoordsLoaderThread.start();
	}

	private void processSongInfos(List<PreloadedSongInfo> songInfos, PreloadedData data) throws Exception {
		ArrayList<Integer> idsWithoutCoords = new ArrayList<Integer>();
		ArrayList<Integer> idsWithCoords = new ArrayList<Integer>();
		AdvancedKdTree<Integer> songKdTree = new AdvancedKdTree<Integer>(Constants.DIM);
		HashMap<Integer, float[]> pcaCoords = new HashMap<Integer, float[]>();

		// int[] ids = new int[songInfos.size()];
		// int i = 0;
		for (PreloadedSongInfo songInfo : songInfos) {
			if (songInfo.getSongPcaCoords() == null && songInfo.getSongCoords() != null) {
				throw new Exception("coords without pca coords, song id: " + songInfo.getSongId());
			}
			if (songInfo.getSongPcaCoords() != null && songInfo.getSongCoords() == null) {
				throw new Exception("pca coords without coords, song id: " + songInfo.getSongId());
			}
			if (songInfo.getSongCoords() == null) {
				idsWithoutCoords.add(songInfo.getSongId());
				continue;
			}
			idsWithCoords.add(songInfo.getSongId());
			try {
				songKdTree.insert(songInfo.getSongCoords(), songInfo.getSongId());
			} catch (KeySizeException e) {
				Log.w(TAG, e);
			}
			pcaCoords.put(songInfo.getSongId(), songInfo.getSongPcaCoords());
			// ids[i] = songInfo.getSongId();
			// i++;
		}
		data.setPcaCoords(pcaCoords);
		data.setSongCoords(songKdTree, idsWithCoords, idsWithoutCoords);
	}

	private void loadTags(final PreloadedData data) {
		tagLoaderThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					data.setTags(dbDataPortal.getCompleteTags(true));
				} catch (Exception e) {
					Log.w(TAG, "setting loader exception");
					Log.w(TAG, e);
					loaderException = e;
				}
			}
		});
		tagLoaderThread.start();
	}

	public void reloadAlbums() {
		PreloadedAlbums preloadedAlbums = new PreloadedAlbums(dbDataPortal);
		loadAlbums(preloadedAlbums);
		this.preloadedAlbums = preloadedAlbums;
	}

	public void recompute(int recomputeTaskId) throws DataUnavailableException {
		setState(State.COMPUTING);
		Log.v(TAG, "calculating map data...");
		MapDataCalculator mapDataCalculator = new MapDataCalculator(dbDataPortal, importState, directoryManager);
		mapDataCalculator.addListener(this);
		Collection<ListAlbum> allAlbums = dbDataPortal.getAllAlbumsAsListAlbums();
		List<CompleteTag> tags = getTags();
		if (tags == null) {
			Log.w(TAG, "tags is null");
		}
		mapDataCalculator.calculate(allAlbums, tags, preloadedAlbums, recomputeTaskId);
		importState.setMapDataCalculated(true);
		Log.v(TAG, "map data calculated.");

		joinDbWriterThreads();
		Log.v(TAG, "map data written to db");

		int currentRecomputeTaskId = modelSettingsManager.getRecomputeTaskId();
		if (currentRecomputeTaskId == recomputeTaskId) {
			// set task id to 0, to mark that no further recompute is
			// required.
			modelSettingsManager.resetRecomputeTaskId();
		}

		PreloadedAlbums preloadedAlbums = new PreloadedAlbums(dbDataPortal);
		Log.v(TAG, "loading albums from db");
		preloadedAlbums.loadFromDb(true);

		PreloadedData data = new PreloadedData();
		data.setSongCoords(mapDataCalculator.getSongCoords());
		data.setIdsOfSongsWithoutCoords(mapDataCalculator.getIdsOfSongsWithoutCoords());
		// TODO: Deal with songs without coordinates

		List<CompleteTag> relevantTags = mapDataCalculator.getRelevantTags();
		data.setTags(relevantTags);
		data.setPcaCoords(mapDataCalculator.getSongPcaCoords());
		// data.setAlbumQuadTree(mapDataCalculator.getAlbumQuadTree());

		setData(data, preloadedAlbums);
		importState.setMapDataCommitted(true);

	}

	private List<CompleteTag> getTags() throws DataUnavailableException {
		List<CompleteTag> tags;

		try {
			tags = new ArrayList<CompleteTag>(dbDataPortal.getCompleteTags(false).values());
		} catch (DataUnavailableException e) {
			Log.e(TAG, "getTags: Failed because of a database read exception");
			throw e;
		}

		if (tags.size() < 500) { // something is wrong with our tag table...
			tags = resourceLoaderManager.readTags();
			try {
				dbDataPortal.deleteTagTable();
			} catch (DataWriteException e1) {
				Log.w(TAG, e1);
				throw new DataUnavailableException(e1);
			}

			dbDataPortal.batchInsertTags(tags);

			//			int tagId;
			//			
			//			for (CompleteTag tag : tags) {
			//				try {
			//					tagId = dbDataPortal.insertTag(tag.getMeId(), tag.getName(), tag.getPlsaCoords());
			//				} catch (DataWriteException e) {
			//					Log.e(TAG, "getTags: Failed because of a database write/insert exception");
			//					return null;
			//				}
			//				tag.setId(tagId);
			//			}
		}
		return tags;
	}

	private void loadAlbums(PreloadedAlbums preloadedAlbums) {
		preloadedAlbums.loadFromDb(true);
	}

	private void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

	public void abort() {
		this.aborted = true;
	}

	public synchronized PreloadedData getData() throws DataUnavailableException {
		if (data == null) {
			throw new DataUnavailableException("preloaded data not yet loaded");
		}
		return data;
	}

	private synchronized void setData(PreloadedData data, PreloadedAlbums preloadedAlbums) {
		this.data = data;
		this.preloadedAlbums = preloadedAlbums;
		setState(State.IDLE);
	}

	public synchronized List<ListAlbum> getAllListAlbums() throws DataUnavailableException {
		if (preloadedAlbums == null) {
			waitForPreloadedAlbums();
		}
		if (!preloadedAlbums.isLoaded()) {
			preloadedAlbums.loadFromDb(true);
		}
		return preloadedAlbums.getAllListAlbums();
	}

	private void waitForPreloadedAlbums() throws DataUnavailableException {
		while (preloadedAlbums == null) {
			try {
				JoinableThread.sleep(20);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
	}

	public synchronized Collection<MapAlbum> getAllMapAlbums() throws DataUnavailableException {
		if (preloadedAlbums == null) {
			Log.v(TAG, "preloaded albums == null => waiting for preloaded albums");
			waitForPreloadedAlbums();
		}
		if (!preloadedAlbums.isLoaded()) {
			Log.v(TAG, "loading preloaded albums");
			preloadedAlbums.loadFromDb(true);
		}
		Log.v(TAG, "returning map albums from preloaded albums");
		return preloadedAlbums.getAllMapAlbums();
	}

	@Override
	public void onMapAlbumsCalculated(final Collection<MapAlbum> mapAlbums) {
		mapAlbumWriterThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				int i = 0; // TODO: debug only...
				for (MapAlbum ma : mapAlbums) {
					i++;
					if (ma.getGridCoords() == null) {
						Log.v(TAG, "grid coords of album " + i + " are null");
					} else {
						// Log.v(TAG, "album " + i + " has grid coords: ["
						// + ma.getGridCoords()[0] + ", "
						// + ma.getGridCoords()[1] + "]");
					}
				}

				try {
					dbDataPortal.updateMapAlbumsPcaCoords(mapAlbums);
				} catch (DataWriteException e) {
					Log.e(TAG, "onMapAlbumsCalculated: update album pca coords failed!");
					return;
				}
			}
		});
		mapAlbumWriterThread.start();
	}

	@Override
	public void onRelevantTagsCalculated(final Collection<CompleteTag> relevantTags) {
		tagsWriterThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					dbDataPortal.setRelevantTags(relevantTags);
				} catch (DataWriteException e) {
					Log.e(TAG, "onRelevantTagsCalculated: tags insertion failed!");
					return;
				}
				Log.v(TAG, "onRelevantTagsCalculated: tags inserted.");
			}
		});
		tagsWriterThread.start();
	}

	@Override
	public void onSongPcaCoordsCalculated(final HashMap<Integer, float[]> songPcaCoords) {
		songPcaCoordsWriterThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					dbDataPortal.updateSongsPcaCoords(songPcaCoords);
				} catch (DataWriteException e) {
					Log.e(TAG, "onSongPcaCoordsCalculated: update song pca coords failed!");
					return;
				}
			}
		});
		songPcaCoordsWriterThread.start();
	}

	private void joinDbWriterThreads() {
		try {
			// Log.v(TAG, "before join: mapAlbumWriterThread.state: " +
			// mapAlbumWriterThread.getState().name());
			mapAlbumWriterThread.realJoin();
			// Log.v(TAG, "after join: mapAlbumWriterThread.state: " +
			// mapAlbumWriterThread.getState().name());
			mapAlbumWriterThread = null;
			Log.v(TAG, "mapAlbumWriterThread joined.");
			tagsWriterThread.realJoin();
			Log.v(TAG, "tagsWriterThread joined.");
			songPcaCoordsWriterThread.realJoin();
			Log.v(TAG, "songPcaCoordsWriterThread joined.");
		} catch (InterruptedException e) {
			Log.wtf(TAG, e);
		}
	}

	public synchronized List<Pair<MapAlbum, Float>> getSimilarAlbums(BaseAlbum album, int number)
			throws DataUnavailableException {
		if (preloadedAlbums == null) {
			Log.v(TAG, "preloaded albums == null => waiting for preloaded albums");
			waitForPreloadedAlbums();
		}
		if (!preloadedAlbums.isLoaded()) {
			Log.v(TAG, "loading preloaded albums");
			preloadedAlbums.loadFromDb(true);
		}
		return preloadedAlbums.getSimilarAlbums(album, number);
	}

	@Override
	public void onAlbumCoversFetched() {
		Log.v(TAG, "album covers fetched.");
		Log.v(TAG, "reloading preloaded albums.");
		reloadAlbums();
	}

	@Override
	public void onBaseDataCommitted() {
		reloadAlbums();
	}

	@Override
	public void onCoordinatesFetched() {
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				int recomputeTaskId = modelSettingsManager.getRecomputeTaskId();
				Log.v(TAG, "Recompute Map data id: " + recomputeTaskId);
				if (recomputeTaskId == 0) {
					importState.setMapDataCalculated(true);
					importState.setMapDataCommitted(true);
					return;
				}
				reloadAlbums();
				abort();
				try {
					recompute(recomputeTaskId);
				} catch (Exception e) {
					Log.w(TAG, e);

					// make sure the import get set to completed.
					importState.setMapDataCalculated(true);
					importState.setMapDataCommitted(true);
				}
			}
		});
		t.start();
	}

	@Override
	public void onImportAborted(boolean hadChanges) {
	}

	@Override
	public void onImportCompleted(boolean hadChanges) {
	}

	@Override
	public void onImportStarted() {
	}

	@Override
	public void onImportProblem(Throwable e) {
	}

}
