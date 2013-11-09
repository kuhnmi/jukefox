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

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class WebDataFetcher {

	private final static String TAG = WebDataFetcher.class.getSimpleName();

	public final static int COORDINATE_PACKAGE_SIZE = 25;

	// public static final int CONNECTION_TIMEOUT = 20000;

	private final AbstractCollectionModelManager collectionModelManager;
	private final ImportState importState;

	private List<CoordinateFetcherListener> coordinateFetcherListeners;
	private List<AlbumCoverFetcherListener> coverFetcherListeners;
	private CoordinateFetcherThread coordinateThread;
	// private TagVarianceThread tagVarianceThread;
	private AbstractAlbumCoverFetcherThread albumCoverThread;
	private IAlbumCoverFetcherThreadFactory albumCoverThreadFactory;

	// private BlockingQueue<Integer> albumIdQueue;

	public WebDataFetcher(AbstractCollectionModelManager collectionModelManager, ImportState importState,
			IAlbumCoverFetcherThreadFactory coverFetcherThreadFactory) {
		super();
		this.collectionModelManager = collectionModelManager;
		this.importState = importState;
		coordinateFetcherListeners = new LinkedList<CoordinateFetcherListener>();
		this.albumCoverThreadFactory = coverFetcherThreadFactory;
		// TODO no need for this
		// albumIdQueue = new LinkedBlockingQueue<Integer>();
		coverFetcherListeners = new LinkedList<AlbumCoverFetcherListener>();
	}

	/**
	 * 
	 * @return true if there were changes, false otherwise.
	 */
	public boolean fetchData(boolean reduced) {
		Log.v(TAG, "fetchData()");
		// TODO is now in constructor
		// albumCoverThread = new
		// AlbumCoverFetcherThread(collectionModelManager, albumIdQueue,
		// coverFetcherListeners,
		// importState);

		albumCoverThread = albumCoverThreadFactory.getNewAlbumCoverFetcherThread();

		coordinateThread = new CoordinateFetcherThread(collectionModelManager, albumCoverThread,
				coordinateFetcherListeners, importState);
		// tagVarianceThread = new TagVarianceThread(dbWrapper, coordsQueue,
		// tagVarQueue);

		coordinateThread.setReduced(reduced);

		coordinateThread.start();

		albumCoverThread.setProducerFinished(false);
		albumCoverThread.start();

		try {
			coordinateThread.joinIncludingInnerThread();
			importState.setCoordinatesFetched(true);
			albumCoverThread.setProducerFinished(true);
			albumCoverThread.realJoin();
			importState.setCoversFetched(true);
		} catch (InterruptedException e) {
			Log.w(TAG, e);
		}

		boolean hasChanges = false;
		hasChanges |= coordinateThread.hasChanges();

		return hasChanges;
	}

	public void addCoordinateThreadListener(CoordinateFetcherListener listener) {
		coordinateFetcherListeners.add(listener);
	}

	public void addAlbumCoverThreadListener(AlbumCoverFetcherListener listener) {
		coverFetcherListeners.add(listener);
	}

}
