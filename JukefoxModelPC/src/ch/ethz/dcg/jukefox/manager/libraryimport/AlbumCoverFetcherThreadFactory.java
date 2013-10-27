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

import java.util.List;
import java.util.concurrent.BlockingQueue;

import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class AlbumCoverFetcherThreadFactory implements IAlbumCoverFetcherThreadFactory {

	public static final String TAG = AlbumCoverFetcherThreadFactory.class.getSimpleName();
	private AbstractCollectionModelManager collectionModelManager;
	private BlockingQueue<Integer> inQueue;
	private List<AlbumCoverFetcherListener> listeners;
	private ImportState importState;

	public AlbumCoverFetcherThreadFactory(AbstractCollectionModelManager collectionModelManager,
			BlockingQueue<Integer> inQueue, List<AlbumCoverFetcherListener> listeners, ImportState importState) {
		this.collectionModelManager = collectionModelManager;
		this.inQueue = inQueue;
		this.listeners = listeners;
		this.importState = importState;
	}

	@Override
	public AbstractAlbumCoverFetcherThread getNewAlbumCoverFetcherThread() {
		return new AlbumCoverFetcherThread(collectionModelManager, inQueue, listeners, importState);
	}

}
