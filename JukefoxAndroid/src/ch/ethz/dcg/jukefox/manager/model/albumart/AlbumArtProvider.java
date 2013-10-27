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
package ch.ethz.dcg.jukefox.manager.model.albumart;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;

public class AlbumArtProvider {

	private IDbDataPortal dbWrapper;
	private LinkedHashMap<Integer, String> lowResPathCache;
	private LinkedHashMap<Integer, byte[]> lowResByteCache;
	private LinkedHashMap<Integer, Bitmap> lowResBitmapCache;
	private LinkedHashMap<Integer, String> highResPathCache;
	private LinkedHashMap<Integer, byte[]> highResByteCache;
	private LinkedHashMap<Integer, Bitmap> highResBitmapCache;

	public static final int MAX_LOW_RES_PATH_ITEMS = 2;
	public static final int MAX_LOW_RES_BYTE_ARRAY_ITEMS = 2;
	public static final int MAX_LOW_RES_BITMAP_ITEMS = 2;
	public static final int MAX_HIGH_RES_PATH_ITEMS = 2;
	public static final int MAX_HIGH_RES_BYTE_ARRAY_ITEMS = 2;
	public static final int MAX_LOW_HIGH_BITMAP_ITEMS = 2;
	private static final String TAG = AlbumArtProvider.class.getCanonicalName();

	public AlbumArtProvider(IDbDataPortal dbWrapper) {
		this.dbWrapper = dbWrapper;
		initializeLowResCaches();
		initializeHighResCaches();
	}

	private void initializeLowResCaches() {
		lowResPathCache = new LinkedHashMap<Integer, String>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
				if (this.size() > MAX_LOW_RES_PATH_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
		lowResByteCache = new LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
				if (this.size() > MAX_LOW_RES_BYTE_ARRAY_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
		lowResBitmapCache = new LinkedHashMap<Integer, Bitmap>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, Bitmap> eldest) {
				if (this.size() > MAX_LOW_RES_BITMAP_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
	}

	private void initializeHighResCaches() {
		highResPathCache = new LinkedHashMap<Integer, String>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
				if (this.size() > MAX_LOW_RES_PATH_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
		highResByteCache = new LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
				if (this.size() > MAX_LOW_RES_BYTE_ARRAY_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
		highResBitmapCache = new LinkedHashMap<Integer, Bitmap>(16, 0.75f, true) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, Bitmap> eldest) {
				if (this.size() > MAX_LOW_RES_BITMAP_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
	}

	public Bitmap getAlbumArt(BaseAlbum album, boolean forceLowResolution) throws NoAlbumArtException {
		if (forceLowResolution) {
			return getLowResAlbumArt(album);
		} else {
			return getHighResAlbumArtIfPossible(album);
		}
	}

	private Bitmap getHighResAlbumArtIfPossible(BaseAlbum album) throws NoAlbumArtException {
		Bitmap albumArt = getHighResAlbumArt(album);
		if (albumArt == null) {
			albumArt = getLowResAlbumArt(album);
		}
		return albumArt;
	}

	private Bitmap getHighResAlbumArt(BaseAlbum album) throws NoAlbumArtException {
		int albumId = album.getId();
		if (highResBitmapCache.containsKey(albumId)) {
			return highResBitmapCache.get(albumId);
		} else if (highResByteCache.containsKey(albumId)) {
			return AndroidUtils.getBitmapFromByteArray(highResByteCache.get(albumId),
					AndroidConstants.COVER_SIZE_HIGH_RES);
		} else if (highResPathCache.containsKey(albumId)) {
			return decodeAlbumArtPath(albumId, highResPathCache.get(albumId), AndroidConstants.COVER_SIZE_HIGH_RES);
		} else {
			String path;
			try {
				path = dbWrapper.getAlbumArtPath(album, false);
				if (path == null) {
					throw new NoAlbumArtException();
				}
			} catch (DataUnavailableException e) {
				throw new NoAlbumArtException();
			}
			try {
				highResPathCache.put(albumId, path);
			} catch (Exception e) {
				// TODO: wtf
			}
			return decodeAlbumArtPath(albumId, path, AndroidConstants.COVER_SIZE_HIGH_RES);
		}
	}

	private Bitmap getLowResAlbumArt(BaseAlbum album) throws NoAlbumArtException {
		int albumId = album.getId();
		if (lowResBitmapCache.containsKey(albumId)) {
			return lowResBitmapCache.get(albumId);
		} else if (lowResByteCache.containsKey(albumId)) {
			return AndroidUtils.getBitmapFromByteArray(lowResByteCache.get(albumId),
					AndroidConstants.COVER_SIZE_LOW_RES);
		} else if (lowResPathCache.containsKey(albumId)) {
			return decodeAlbumArtPath(albumId, lowResPathCache.get(albumId), AndroidConstants.COVER_SIZE_LOW_RES);
		} else {
			String path;
			try {
				path = dbWrapper.getAlbumArtPath(album, true);
				if (path == null) {
					throw new NoAlbumArtException();
				}
			} catch (DataUnavailableException e) {
				throw new NoAlbumArtException();
			}
			lowResPathCache.put(albumId, path);
			return decodeAlbumArtPath(albumId, path, AndroidConstants.COVER_SIZE_LOW_RES);
		}
	}

	private Bitmap decodeAlbumArtPath(int albumId, String path, int maxRes) throws NoAlbumArtException {
		InputStream inputStream;
		Bitmap bitmap = null;
		try {
			if (path == null || !AndroidUtils.fileExists(path)) {
				throw new NoAlbumArtException();
			}
			inputStream = new FileInputStream(path);
			bitmap = AndroidUtils.getBitmapFromInputStream(inputStream, maxRes);
		} catch (Throwable e) {
			Log.w(TAG, e);
			throw new NoAlbumArtException();
		}

		// byte[] byteArray = null;
		//
		// try {
		//
		// byteArray = new byte[inputStream.available()];
		// inputStream.read(byteArray);
		//
		// if (lowRes) {
		// lowResByteCache.put(albumId, byteArray);
		// } else {
		// highResByteCache.put(albumId, byteArray);
		// }
		//
		// } catch (Exception e) {
		// Log.w(TAG, e);
		// } finally {
		// try {
		// inputStream.close();
		// } catch (IOException e) {
		// Log.w("Bitmap", e.toString());
		// }
		// }

		if (bitmap == null) {
			throw new NoAlbumArtException();
		} else {
			try {
				if (maxRes == AndroidConstants.COVER_SIZE_LOW_RES) {
					lowResBitmapCache.put(albumId, bitmap);
				} else if (maxRes == AndroidConstants.COVER_SIZE_HIGH_RES) {
					highResBitmapCache.put(albumId, bitmap);
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
		// Log.v(TAG, "Returning bitmap with width: " + bitmap.getWidth() +
		// " and height: " + bitmap.getHeight() + " from path: " + path);
		return bitmap;
	}

	public BitmapDrawable getListAlbumArt(ListAlbum album) throws NoAlbumArtException {
		Bitmap bitmap;
		try {
			bitmap = getVeryLowResAlbumArt(album);
		} catch (NullPointerException e) {
			Log.w(TAG, e);
			throw new NoAlbumArtException();
		}
		BitmapDrawable drawable = new BitmapDrawable(bitmap);
		return drawable;
	}

	private Bitmap getVeryLowResAlbumArt(BaseAlbum album) throws NoAlbumArtException {
		int albumId = album.getId();
		if (lowResByteCache.containsKey(albumId)) {
			return AndroidUtils.getBitmapFromByteArray(lowResByteCache.get(albumId),
					AndroidConstants.COVER_SIZE_LOW_RES / 2);
		} else if (lowResPathCache.containsKey(albumId)) {
			return decodeAlbumArtPath(albumId, lowResPathCache.get(albumId), AndroidConstants.COVER_SIZE_LOW_RES / 2);
		} else {
			String path;
			try {
				path = dbWrapper.getAlbumArtPath(album, true);
				if (path == null) {
					throw new NoAlbumArtException();
				}
			} catch (DataUnavailableException e) {
				throw new NoAlbumArtException();
			}
			lowResPathCache.put(albumId, path);
			return decodeAlbumArtPath(albumId, path, AndroidConstants.COVER_SIZE_LOW_RES / 2);
		}
	}
}
