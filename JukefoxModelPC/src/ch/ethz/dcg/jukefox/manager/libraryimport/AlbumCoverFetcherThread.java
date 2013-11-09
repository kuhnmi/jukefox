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
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class AlbumCoverFetcherThread extends AbstractAlbumCoverFetcherThread {

	public AlbumCoverFetcherThread(
			AbstractCollectionModelManager collectionModelManager,
			BlockingQueue<Integer> inQueue,
			List<AlbumCoverFetcherListener> listeners, ImportState importState) {
		super(collectionModelManager, listeners, importState);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean isReadyToDownloadCovers() {
		// TODO return true, and make nothing :)
		return true;
	}

	@Override
	protected AlbumFetcherResult getAlbumCovers(CompleteAlbum album)
			throws Exception {
		return new AlbumFetcherResult(null, null, 0, AlbumStatus.CREATED_DEFAULT_COVER, album.getId());
	}

}
