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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.HttpHelper;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.utils.AndroidUtils;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class AndroidAlbumCoverFetcherThread extends AbstractAlbumCoverFetcherThread {

	private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
	private ConnectivityManager cm;
	private Bitmap emptyCoverBitmap;
	private boolean useCoverFiles;
	private Context context;
	private SongProvider songProvider;
	private ContentResolver contentResolver;

	public AndroidAlbumCoverFetcherThread(AbstractCollectionModelManager collectionModelManager,
			List<AlbumCoverFetcherListener> listeners, ImportState importState, Context context) {
		super(collectionModelManager, listeners, importState);
		emptyCoverBitmap = AndroidUtils.getBitmapFromResource(context.getResources(), R.drawable.d005_empty_cd, 512);
		this.context = context;
		// SettingsProvider settingsProvider =
		// collectionModelManager.getSettingsProvider();
		// useCoverFiles = settingsProvider.isUseAlbumArtFiles();
		cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		songProvider = collectionModelManager.getSongProvider();
		contentResolver = context.getContentResolver();
	}

	@Override
	protected boolean isReadyToDownloadCovers() {
		boolean ready = AndroidUtils.isSdCardOk();
		if (!ready) {
			Log.v(TAG, "Sd-card is not Ok. Cancelling album cover fetcher thread");
			showSdcardNotAvailableToast();
		}
		return ready;
	}

	private void showSdcardNotAvailableToast() {
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				Toast toast = Toast.makeText(context, context.getString(R.string.cover_fetcher_no_sd),
						Toast.LENGTH_LONG);
				toast.show();
			}

		});
	}

	/**
	 * 
	 * @param album
	 *            Album for which the album art has to be fetched
	 * @return an Array with the paths to two files: 1. The high Resolution album art, 2. the low resolution album art
	 * @throws Exception
	 */
	@Override
	protected AlbumFetcherResult getAlbumCovers(CompleteAlbum album) throws Exception {

		AlbumFetcherResult result = null;

		// Log.v(TAG, "Getting covers for: " + album.getName() + ", status: " +
		// album.getAlbumCoverStatus());
		if (album.getAlbumCoverStatus() != AlbumStatus.COVER_UNCHECKED
				&& album.getAlbumCoverStatus() != AlbumStatus.WEB_ERROR) {
			Log.w(TAG, "Album should only be handled in coverFetcher if status is UNCHECKED or WEB_ERROR!");
			return null;
		}

		if (album.getAlbumCoverStatus() == AlbumStatus.COVER_UNCHECKED) {

			// Check for cover file in directory
			if (useCoverFiles) {
				result = getAlbumCoverFromDirectory(album);
				if (result != null) {
					// Log.v(TAG, "Fetched Album Art from directory for album: "
					// + album.getName());
					return result;
				}
			}

			// See if album image is already on the phone
			result = getAlbumCoverFromMediaProvider(album);
			if (result != null) {
				Log.v(TAG, "Got album cover from media provider");
				return result;
			}
		}

		// Log.v(TAG, "checking for compilation albums (artist: "
		// + album.getArtists().get(0).getName());
		if (album.getArtists().get(0).getName().equals(JukefoxApplication.albumArtistAlias)) {
			// compilation album (as explicitly grouped by jukefox, or TCMP tag)
			Log.v(TAG, "compilation album detected");
			result = getAlbumCoverFromWeb(album, true, false);
			if (result != null) {
				return result;
			}
			Log.v(TAG, "no last.fm cover found for album name/various artists");
			if (album.getAlbumCoverStatus() == AlbumStatus.COVER_UNCHECKED) {
				result = getAlbumCoverFromMediaProviderBySong(album);
				if (result != null) {
					return result;
				}
			}
			Log.v(TAG, "no cover found for songs from media provider");
			// result = getAlbumCoverFromWeb(album, false, true);
			// if (result != null) {
			// return result;
			// }
			// Log.v(TAG, "no last.fm cover found for songs from web");
		} else {
			// "normal" album
			result = getAlbumCoverFromWeb(album, true, true);
			if (result != null) {
				return result;
			}
		}

		if (album.getAlbumCoverStatus() == AlbumStatus.CREATED_DEFAULT_COVER) {
			return null; // return nothing if album was already previously
			// created
		}
		// Log.v(TAG, "Create Album Art for album: " + album.getName());
		// Else create cover
		return createEmptyAlbumCover(album, AlbumStatus.CREATED_DEFAULT_COVER);
	}

	private AlbumFetcherResult getAlbumCoverFromWeb(CompleteAlbum album, boolean requestByAlbum, boolean requestBySong) {
		AlbumFetcherResult result = null;
		// else: get cover from last.fm
		try {
			// Log.v(TAG, "Connection Manager: " + cm);
			// Log.v(TAG, "Active Connection: " + cm.getActiveNetworkInfo());
			if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
				if (requestByAlbum) {
					result = getAlbumCoverFromWeb(album);
					if (result != null) {
						return result;
					}
				}
				if (requestBySong) {
					result = getAlbumCoverFromWebBySong(album);
					if (result != null) {
						return result;
					}
				}
			}
		} catch (Exception e) {
			if (HttpHelper.isNetworkException(e)) {
				if (album.getAlbumCoverStatus() == AlbumStatus.WEB_ERROR) {
					return null; // return nothing if status was web error
					// before
				} else {
					return createEmptyAlbumCover(album, AlbumStatus.WEB_ERROR);
				}
			}
			Log.w(TAG, e);
		}
		return null;
	}

	private AlbumFetcherResult getAlbumCoverFromDirectory(CompleteAlbum album) {
		String path;
		try {
			path = otherDataProvider.getSongPath(album.getSongs().get(0));
		} catch (DataUnavailableException e1) {
			Log.w(TAG, e1);
			return null;
		}

		File song = new File(path);
		File songDir = song.getParentFile();
		if (songDir == null) {
			return null;
		}

		File[] fileList = songDir.listFiles(imageFilter);

		if (fileList == null || fileList.length == 0) {
			return null;
		}

		FileInputStream is = null;
		try {
			is = new FileInputStream(fileList[0]);

			Bitmap bitmapHigh = AndroidUtils.getBitmapFromInputStream(is, AndroidConstants.COVER_SIZE_HIGH_RES * 2);
			bitmapHigh = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_HIGH_RES);
			Bitmap bitmapLow = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_LOW_RES);
			int color = getColorFromBitmap(bitmapLow);
			if (bitmapHigh == null) {
				return null;
			} else {
				// String highResPath = fileList[0].getAbsolutePath();
				String highResPath = getDefaultCoverPath(false, album.getId());
				saveImage(bitmapHigh, highResPath);
				String lowResPath = getDefaultCoverPath(true, album.getId());
				saveImage(bitmapLow, lowResPath);
				return new AlbumFetcherResult(highResPath, lowResPath, color, AlbumStatus.DIRECTORY_COVER, album
						.getId());
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
		return null;
	}

	private int getColorFromBitmap(Bitmap bitmap) {
		int p1 = bitmap.getPixel(25, 25);
		int p2 = bitmap.getPixel(25, 75);
		int p3 = bitmap.getPixel(75, 25);
		int p4 = bitmap.getPixel(75, 75);
		int p5 = bitmap.getPixel(50, 50);

		int red = Color.red(p1) + Color.red(p2) + Color.red(p3) + Color.red(p4) + Color.red(p5);
		red /= 5;

		int green = Color.green(p1) + Color.green(p2) + Color.green(p3) + Color.green(p4) + Color.green(p5);
		green /= 5;

		int blue = Color.blue(p1) + Color.blue(p2) + Color.blue(p3) + Color.blue(p4) + Color.blue(p5);
		blue /= 5;

		return Color.argb(255, red, green, blue);

	}

	/**
	 * Resize a bitmap to targetResxtargetRex pixels
	 * 
	 * @param bm
	 *            bitmap to resize
	 * @param targetRes
	 *            height and width of the new bitmap
	 * @return
	 */
	private Bitmap resizeBitmap(Bitmap bm, int targetRes) {
		// scale the image for opengl
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = (float) targetRes / width;
		float scaleHeight = (float) targetRes / height;

		// scale matrix
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		if (resizedBitmap == null) {
			Log.w(TAG, "ResizedBitmap is null");
		}
		return resizedBitmap;
	}

	private void saveImage(Bitmap bitmap, String fileName) throws CouldNotSaveCoverException {
		try {
			FileOutputStream stream = new FileOutputStream(fileName);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
			bitmap.recycle();
			stream.close();
		} catch (Exception e) {
			Log.w(TAG, e);
			throw new CouldNotSaveCoverException(e.getMessage(), e);
		}
	}

	private AlbumFetcherResult createEmptyAlbumCover(CompleteAlbum album, AlbumStatus status) {

		try {

			Bitmap bitmapHigh = emptyCoverBitmap.copy(Bitmap.Config.RGB_565, true);
			int bitmapSize = bitmapHigh.getHeight();
			float textHeight = bitmapSize / 10;

			// Draw Artist name and album name on cover
			drawTextOnBitmap(album, bitmapHigh, bitmapSize, textHeight);

			Bitmap bitmapLow = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_LOW_RES);
			int color = getColorFromBitmap(bitmapLow);

			String highResPath = getDefaultCoverPath(false, album.getId());

			String lowResPath = getDefaultCoverPath(true, album.getId());
			saveImage(bitmapLow, lowResPath);
			saveImage(bitmapHigh, highResPath);

			return new AlbumFetcherResult(highResPath, lowResPath, color, status, album.getId());
		} catch (Exception e) {
			Log.w(TAG, e);
			return null;
		}
	}

	private String getDefaultCoverPath(boolean lowRes, int albumId) {
		if (lowRes) {
			return JukefoxApplication.getDirectoryManager().getAlbumCoverDirectory().getAbsolutePath() + Constants.FS
					+ "album" + albumId + "L.jpg";
		}
		// else: high res
		return JukefoxApplication.getDirectoryManager().getAlbumCoverDirectory().getAbsolutePath() + Constants.FS
				+ "album" + albumId + "H.jpg";
	}

	private void drawTextOnBitmap(CompleteAlbum album, Bitmap bitmapHigh, int bitmapSize, float textHeight) {
		Canvas canvas = new Canvas(bitmapHigh);
		canvas.drawARGB(0, 125, 125, 125);
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setTypeface(Typeface.SERIF);
		paint.setSubpixelText(true);
		paint.setTextSize(textHeight);
		paint.setAntiAlias(true);
		String artist = album.getArtists().get(0).getName();
		String shortenedText = new String(artist);

		int textLength = artist.length();
		if (textLength > 18) {
			shortenedText = artist.substring(0, 15) + "...";
			textLength = 18;
		} else if (textLength < 8) {
			while (shortenedText.length() < 8) {
				shortenedText = " " + shortenedText + " ";
			}
		}
		float pixelLength = paint.measureText(shortenedText);
		paint.setTextSize(textHeight * (bitmapSize * 2 / 3) / pixelLength);

		canvas.drawText(shortenedText, bitmapSize / 6, bitmapSize / 3, paint);

		shortenedText = album.getName();
		textLength = album.getName().length();
		if (textLength > 18) {
			shortenedText = album.getName().substring(0, 15) + "...";
			textLength = 18;
		} else if (textLength < 8) {
			while (shortenedText.length() < 8) {
				shortenedText = " " + shortenedText + " ";
			}
		}
		paint.setTextSize(bitmapSize / 10f);
		pixelLength = paint.measureText(shortenedText);
		textHeight = textHeight * bitmapSize * 2f / 3f / pixelLength;
		paint.setTextSize(textHeight);

		canvas.drawText(shortenedText, bitmapSize / 6f, bitmapSize * 2 / 3f + textHeight, paint);
	}

	/**
	 * Try to fetch an album cover from the internal DB
	 */
	private AlbumFetcherResult getAlbumCoverFromMediaProvider(CompleteAlbum album) throws CouldNotSaveCoverException {

		return getAlbumCoverFromMediaProvider(album.getId(), album.getArtists().get(0).getName(), album.getName());
	}

	private AlbumFetcherResult getAlbumCoverFromMediaProviderBySong(CompleteAlbum album)
			throws CouldNotSaveCoverException {
		int albumId = album.getId();
		String albumName = album.getName();
		for (BaseSong<BaseArtist, BaseAlbum> s : album.getSongs()) {
			String artistName = s.getArtist().getName();
			AlbumFetcherResult result = getAlbumCoverFromMediaProvider(albumId, artistName, albumName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Try to fetch an album cover from the internal DB
	 */
	private AlbumFetcherResult getAlbumCoverFromMediaProvider(int albumId, String artist, String albumName)
			throws CouldNotSaveCoverException {

		String where = MediaStore.Audio.Media.ARTIST + " = ? AND " + MediaStore.Audio.Media.ALBUM + " = ?";
		// Add in the filtering constraints
		String[] keywords = new String[] { artist, albumName };
		String[] cols = new String[] { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART };
		Cursor cur = null;
		try {
			cur = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, cols, where, keywords, null);

			if (cur.moveToFirst()) {
				return extractBitmapsFromMediaProviderCursor(albumId, cur);
			}
			// Log.v("Didn't get cover from media provider", album.getArtists()
			// .get(0).getName()
			// + " - " + album.getName());

			return null;
		} catch (CouldNotSaveCoverException e) {
			throw e;
		} catch (Exception e) {
			// Log.w(TAG, "error reading cover from media provider");
			// Log.v(TAG, "Error reading cover from media provider: "
			// + e.getMessage());
			return null;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private AlbumFetcherResult extractBitmapsFromMediaProviderCursor(int albumId, Cursor cur) throws Exception {
		int cpAlbumId = cur.getInt(0);
		Uri uri = ContentUris.withAppendedId(sArtworkUri, cpAlbumId);

		if (uri != null) {
			InputStream in = null;
			try {
				in = contentResolver.openInputStream(uri);
				Bitmap bitmapHigh = AndroidUtils.getBitmapFromInputStream(in, 2 * AndroidConstants.COVER_SIZE_HIGH_RES);
				bitmapHigh = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_HIGH_RES);
				if (bitmapHigh == null) {
					return null;
				}
				// Log.v("Got cover from phone DB", album.getArtists().get(0)
				// .getName()
				// + " - "
				// + album.getName()
				// + " "
				// + cpAlbumId
				// + " HResPath: " + highResPath);
				Bitmap bitmapLow = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_LOW_RES);
				int color = getColorFromBitmap(bitmapLow);
				String lowResPath = getDefaultCoverPath(true, albumId);
				String highResPath = getDefaultCoverPath(false, albumId);
				saveImage(bitmapHigh, highResPath);
				saveImage(bitmapLow, lowResPath);
				return new AlbumFetcherResult(highResPath, lowResPath, color, AlbumStatus.CONTENT_PROVIDER_COVER,
						albumId);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
					Log.w(TAG, ex);
				}
			}
		}
		return null;
	}

	private AlbumFetcherResult getAlbumCoverFromWeb(CompleteAlbum album) throws Exception {
		String urlStr = "";
		String albumName = URLEncoder.encode(album.getName());

		Integer artistMeId = otherDataProvider.getMusicExplorerArtistId(album.getArtists().get(0));
		if (artistMeId == null) {
			// Log.d(TAG, "MusicExplorerId for artist "
			// + album.getArtists().get(0).getName() + " is null");
			return null;
		}

		// Get Cover art URL from our server by the album name
		urlStr = String.format(Constants.FORMAT_IMAGE_URL_REQUEST_PER_ALBUM, Integer.toString(artistMeId), albumName);
		AlbumFetcherResult result = getAlbumCoverFromWeb(album, urlStr);

		// if (result != null) {
		// return result;
		// }
		//
		// // Get Cover Art from our server by the song title
		// return getAlbumCoverFromWebBySong(album);

		return result;

	}

	private AlbumFetcherResult getAlbumCoverFromWebBySong(CompleteAlbum album) throws Exception {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = album.getSongs();
		AlbumFetcherResult result = null;
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			Integer songMeId = otherDataProvider.getMusicExplorerSongId(song);
			if (songMeId == null) {
				continue;
			}
			String urlStr = String.format(Constants.FORMAT_IMAGE_URL_REQUEST_PER_SONG, Integer.toString(songMeId));
			result = getAlbumCoverFromWeb(album, urlStr);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private AlbumFetcherResult getAlbumCoverFromWeb(CompleteAlbum album, String urlStr) throws Exception {
		InputStream in = null;
		try {
			String url = getAlbumCoverUrlFromJukefoxServer(urlStr);
			// Log.d(TAG, urlStr + " resolved " + url);

			if (url == null) {
				return null;
			}

			try {
				in = getAlbumCoverInputStreamFromUrl(url);
			} catch (Exception e) {
				Log.w(TAG, e);
				informJukefoxServerAboutInvalidUrl();
				throw e;
			}

			Bitmap bitmapHigh = AndroidUtils.getBitmapFromInputStream(in, 2 * AndroidConstants.COVER_SIZE_HIGH_RES);
			if (bitmapHigh == null) {
				// Log.d(TAG, "could not get bitmap at " + url);
				return null;
			}
			bitmapHigh = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_HIGH_RES);
			Bitmap bitmapLow = resizeBitmap(bitmapHigh, AndroidConstants.COVER_SIZE_LOW_RES);
			int color = getColorFromBitmap(bitmapLow);

			String lowResPath = getDefaultCoverPath(true, album.getId());
			String highResPath = getDefaultCoverPath(false, album.getId());

			saveImage(bitmapLow, lowResPath);
			saveImage(bitmapHigh, highResPath);

			return new AlbumFetcherResult(highResPath, lowResPath, color, AlbumStatus.WEB_COVER, album.getId());
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	// private AlbumFetcherResult saveImages(CompleteAlbum album, String
	// highResPath,
	// Bitmap bitmapHigh, Bitmap bitmapLow, int color)
	// throws CouldNotSaveCoverException {
	//
	// try {
	// String lowResPath = Constants.COVER_DIRECTORY + "/title"
	// + album.getId() + "L.png";
	// saveImage(album.getId(), bitmapLow, lowResPath);
	// saveImage(album.getId(), bitmapHigh, highResPath);
	//
	// return new AlbumFetcherResult(highResPath, lowResPath, color,
	// AlbumStatus.WEB_COVER);
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// throw new CouldNotSaveCoverException(e.getMessage(), e);
	// }
	// }

	private void informJukefoxServerAboutInvalidUrl() {
		// TODO Auto-generated method stub

	}

	private String getAlbumCoverUrlFromJukefoxServer(String urlStr) throws Exception {
		InputStream is = null;
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			HttpResponse httpResp = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResp.getEntity();
			is = httpEntity.getContent();
			BufferedReader bufread = new BufferedReader(new InputStreamReader(is));
			String flag = bufread.readLine();
			if (!flag.equals("URL:")) {
				throw new UnknownHostException("Could not contact jukefox server for album image url!");
			}
			String url = bufread.readLine();

			if (!url.startsWith("http")) {
				return null;
			}

			return url;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}

	}

	private InputStream getAlbumCoverInputStreamFromUrl(String url) throws Exception {

		HttpGet httpGet = new HttpGet(url);
		HttpResponse httpResp = httpClient.execute(httpGet);
		HttpEntity httpEntity = httpResp.getEntity();
		InputStream is = httpEntity.getContent();
		return is;
	}

}
