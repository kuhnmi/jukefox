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
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.DefaultHttpClient;

import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.HttpHelper;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.AlbumProvider;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;

public abstract class AbstractAlbumCoverFetcherThread extends JoinableThread implements ICoordinateFetcherConsumer {

	public static final String TAG = AbstractAlbumCoverFetcherThread.class.getSimpleName();

	public static class AlbumFetcherResult {

		private final int albumId;
		private final String highResPath;
		private final String lowResPath;
		private final int color;
		private final AlbumStatus status;

		public AlbumFetcherResult(String highResPath, String lowResPath, int color, AlbumStatus status, int albumId) {
			this.albumId = albumId;
			this.highResPath = highResPath;
			this.lowResPath = lowResPath;
			this.color = color;
			this.status = status;
		}

		public String getHighResPath() {
			return highResPath;
		}

		public String getLowResPath() {
			return lowResPath;
		}

		public int getColor() {
			return color;
		}

		public AlbumStatus getStatus() {
			return status;
		}

		public int getAlbumId() {
			return albumId;
		}

	}

	protected AlbumProvider albumProvider;
	protected ModifyProvider modifyProvider;
	protected OtherDataProvider otherDataProvider;
	protected FilenameFilter imageFilter;
	protected DefaultHttpClient httpClient;
	protected BlockingQueue<Integer> inQueue;
	protected boolean producerFinished;
	protected ImportState importState;
	protected int fetchedNr;
	protected int numberOfAlbums;

	public AbstractAlbumCoverFetcherThread(AbstractCollectionModelManager collectionModelManager,
			List<AlbumCoverFetcherListener> listeners, ImportState importState) {
		this.albumProvider = collectionModelManager.getAlbumProvider();
		this.otherDataProvider = collectionModelManager.getOtherDataProvider();
		this.modifyProvider = collectionModelManager.getModifyProvider();
		this.inQueue = new LinkedBlockingQueue<Integer>();
		this.importState = importState;
		createFileNameFilter();
		initializeHttpClient();
	}

	protected void initializeHttpClient() {
		// Initialize Http client
		httpClient = HttpHelper.createHttpClientWithDefaultSettings();
	}

	private void createFileNameFilter() {
		imageFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename == null) {
					return false;
				}
				String lowercase = filename.toLowerCase();
				if (lowercase.endsWith("cover.jpg") || lowercase.endsWith("cover.png")
						|| lowercase.endsWith("folder.png") || lowercase.endsWith("folder.jpg")
						|| lowercase.endsWith("albumart.png") || lowercase.endsWith("albumart.jpg")) {
					return true;
				}
				return false;
			}

		};
	}

	@Override
	public void run() {
		Set<AlbumFetcherResult> results = new HashSet<AlbumFetcherResult>();
		while (inQueue.size() > 0 || !isProducerFinished()) {

			if (importState.shouldAbortImport()) {
				importState.setCoordinatesProgress(1, 1, "fetched album covers");
				return;
			}

			if (!isReadyToDownloadCovers()) {
				break;
			}
			try {
				fetchedNr++;
				Integer albumId = inQueue.poll(1, TimeUnit.SECONDS);
				if (albumId != null) {
					CompleteAlbum album = albumProvider.getCompleteAlbum(albumId);
					importState.setCoversProgress(fetchedNr, numberOfAlbums, "Getting cover for: " + album.getName());
					Log.v(TAG, "processAlbum: " + album.getName());
					AlbumFetcherResult result = getAlbumCovers(album);
					if (result != null) {
						results.add(result);
						//												modifyProvider.insertAlbumArtInfo(album, result.getHighResPath(), result.getLowResPath(),
						//														result.getColor(), result.getStatus());
					}
				}
			} catch (Exception e) {
				// TODO: should we react differently to (repeated?)
				// save-exceptions?
				Log.w(TAG, e);
			}
		}

		// execute update Batch
		modifyProvider.batchInsertAlbumArtInfo(results);
		importState.setCoversProgress(1, 1, "fetched album covers");
		// Log.v(TAG, "AlbumCoverFetcher completed()");
	}

	protected abstract boolean isReadyToDownloadCovers();

	public boolean isProducerFinished() {
		return producerFinished;
	}

	public void setProducerFinished(boolean producerFinished) {
		this.producerFinished = producerFinished;
	}

	/**
	 * 
	 * @param album
	 *            Album for which the album art has to be fetched
	 * @return an Array with the paths to two files: 1. The high Resolution album art, 2. the low resolution album art
	 * @throws Exception
	 */
	protected abstract AlbumFetcherResult getAlbumCovers(CompleteAlbum album) throws Exception;

	@Override
	public BlockingQueue<Integer> getQueue() {
		return inQueue;
	}

	@Override
	public void setNumberOfAlbums(int n) {
		this.numberOfAlbums = n;
	}
}
