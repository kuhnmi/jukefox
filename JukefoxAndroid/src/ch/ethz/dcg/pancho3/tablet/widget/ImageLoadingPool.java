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
package ch.ethz.dcg.pancho3.tablet.widget;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.Bitmap;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;

public class ImageLoadingPool implements OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>> {

	private final DataFetcher dataFetcher;

	private final ConcurrentHashMap<BaseAlbum, AlbumImageView> albumWaitMap =
			new ConcurrentHashMap<BaseAlbum, AlbumImageView>();

	private final ConcurrentLinkedQueue<QueueItem> queue = new ConcurrentLinkedQueue<QueueItem>();
	private FetchingThread fetchingThread = null;

	public ImageLoadingPool(DataFetcher dataFetcher) {
		this.dataFetcher = dataFetcher;
	}

	static class QueueItem {

		public BaseAlbum album;
		public AlbumImageView view;
		public long timestamp;
	}

	public void add(AlbumImageView view, BaseAlbum album) {
		QueueItem item = new QueueItem();
		item.view = view;
		item.album = album;
		item.timestamp = System.currentTimeMillis();
		queue.add(item);
		if (fetchingThread == null || !fetchingThread.isRunning()) {
			fetchingThread = new FetchingThread();
			fetchingThread.start();
		}
	}

	private class FetchingThread extends Thread {

		private static final long MIN_WAIT_TIME = 100;

		private boolean running = true;

		@Override
		public void run() {
			while (true) {
				if (queue.isEmpty()) {
					break;
				}
				QueueItem item = queue.peek();
				long delta = System.currentTimeMillis() - item.timestamp;
				if (delta < MIN_WAIT_TIME) {
					try {
						Thread.sleep(delta);
					} catch (InterruptedException e) {
					}
				} else {
					queue.poll();
					if (item.view.matchAlbum(item.album)) {
						fetchAlbumMap(item.album, item.view);
					}
				}
			}
			synchronized (this) {
				running = false;
			}
		}

		public synchronized boolean isRunning() {
			return running;
		}
	}

	private void fetchAlbumMap(BaseAlbum album, AlbumImageView view) {
		albumWaitMap.put(album, view);
		dataFetcher.fetchAlbumArt(this, false, false, album);
	}

	@Override
	public void onDataFetched(List<Pair<Bitmap, BaseAlbum>> data) {
		Bitmap bitmap = data.get(0).first;
		BaseAlbum album = data.get(0).second;
		AlbumImageView view = albumWaitMap.remove(album);
		if (view != null) {
			view.setBitmap(bitmap, album);
		}
	}
}
