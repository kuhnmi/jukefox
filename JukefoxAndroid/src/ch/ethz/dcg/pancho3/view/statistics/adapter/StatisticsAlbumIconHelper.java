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
package ch.ethz.dcg.pancho3.view.statistics.adapter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import ch.ethz.dcg.jukefox.manager.model.albumart.FastBitmapDrawable;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.pancho3.R;

/**
 * Helper that asynchronously loads the album icons into image views.
 */
public class StatisticsAlbumIconHelper<T extends StatisticsAlbum> {

	/**
	 * The size of the album icons [in px].
	 */
	private static final int ICON_SIZE = 100;

	/**
	 * How much the album icon loading should be delayed [in ms].
	 */
	private static final int ICON_LOAD_DELAY = 300;

	/**
	 * How much album icons should kept in cache.
	 */
	private static final int ICON_CACHE_SIZE = 100;

	/**
	 * Stores which icon is currently connected to which album.
	 */
	private final Map<ImageView, T> iconToAlbum;

	/**
	 * The album icon cache.
	 */
	private final List<AlbumIconCacheEntry> albumIconCache;

	/**
	 * Handler to run stuff in the ui thread from outside of it.
	 */
	private final Handler uiHandler;

	/**
	 * Timer to run the icon loading processes a little bit delayed.
	 */
	private final Timer timer;

	/**
	 * Album to use if none is available or the correct one is not loaded yet.
	 */
	private final FastBitmapDrawable defaultAlbumIcon;

	private final AndroidCollectionModelManager collectionModelManager;

	public StatisticsAlbumIconHelper(Context context, AndroidCollectionModelManager collectionModelManager) {
		this.collectionModelManager = collectionModelManager;

		iconToAlbum = new HashMap<ImageView, T>();
		uiHandler = new Handler(); // Please make sure, that we are in the ui-thread at this point
		timer = new Timer();

		albumIconCache = new LinkedList<AlbumIconCacheEntry>();

		// Load the default album icon
		Resources r = context.getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		Bitmap b = resizeBitmap(BitmapFactory.decodeResource(r, R.drawable.d005_empty_cd, options));
		defaultAlbumIcon = new FastBitmapDrawable(b);
	}

	public void fillAlbumIcon(View v, T album) {
		ImageView icon = (ImageView) v.findViewById(R.id.icon);

		// Unhide the icon
		icon.setVisibility(ImageView.VISIBLE);

		if (iconToAlbum.get(icon) == album) {
			// This image is already correctly loaded
			return;
		}
		iconToAlbum.put(icon, album); // Make this icon-album association the state of the art

		// Search for this album icon in the cache
		int cacheIndex = albumIconCache.indexOf(new AlbumIconCacheEntry(album, null));
		if (cacheIndex > -1) {
			// Cache hit
			icon.setBackgroundDrawable(albumIconCache.get(cacheIndex).getIcon());
		} else {
			// Load the album image async
			icon.setBackgroundDrawable(defaultAlbumIcon);
			icon.setPadding(0, 0, 1, 0);

			CoverLoadTask clt = new CoverLoadTask(icon, album);
			timer.schedule(clt, ICON_LOAD_DELAY);
		}
	}

	/**
	 * Resize the given bitmap to have the width & height equals {@value #ICON_SIZE}px.
	 * 
	 * @param bm
	 *            The source bitmap
	 * @return The resized bitmap
	 */
	private Bitmap resizeBitmap(Bitmap bm) {
		// scale the image for opengl
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = (float) ICON_SIZE / width;
		float scaleHeight = (float) ICON_SIZE / height;

		// scale matrix
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		return resizedBitmap;
	}

	/**
	 * TimerTask which loads the icon of the given album into the given image view.
	 */
	private class CoverLoadTask extends TimerTask {

		ListAlbum album;
		ImageView image;

		public CoverLoadTask(ImageView image, ListAlbum album) {
			this.album = album;
			this.image = image;
		}

		@Override
		public void run() {
			ListAlbum newAlbum = iconToAlbum.get(image);
			if (newAlbum != null && newAlbum == album) { // Check, if our icon-album association is still valid
				Drawable albumIcon;
				try {
					// Get the album icon
					albumIcon = collectionModelManager.getAlbumArtProvider().getListAlbumArt(album);
				} catch (NoAlbumArtException e) {
					// The album icon could not be loaded -> set the default one
					albumIcon = defaultAlbumIcon;
				}

				// Set the album icon in the ui-thread
				final Drawable iconToBeSet = albumIcon;
				uiHandler.post(new Runnable() {

					@Override
					public void run() {
						synchronized (albumIconCache) {
							albumIconCache.add(new AlbumIconCacheEntry(album, iconToBeSet));
							if (albumIconCache.size() > ICON_CACHE_SIZE) {
								albumIconCache.remove(0); // The cache is full -> remove first entry
							}
						}

						image.setBackgroundDrawable(iconToBeSet);
					}
				});

			}
		}
	}

	/**
	 * Cache entry for album icons.
	 */
	private class AlbumIconCacheEntry {

		private ListAlbum album;
		private Drawable icon;

		public AlbumIconCacheEntry(ListAlbum album, Drawable icon) {
			this.album = album;
			this.icon = icon;
		}

		public ListAlbum getAlbum() {
			return album;
		}

		public Drawable getIcon() {
			return icon;
		}

		/**
		 * Returns true, if the albums of the AlbumIconCacheEntries match.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object other) {
			if (other.getClass().equals(AlbumIconCacheEntry.class)) {
				return getAlbum().equals(((AlbumIconCacheEntry) other).getAlbum());
			}

			return false;
		}

	}
}