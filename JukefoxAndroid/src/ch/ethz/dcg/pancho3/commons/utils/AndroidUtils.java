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
package ch.ethz.dcg.pancho3.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.PatchInputStream;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class AndroidUtils {

	private final static String TAG = AndroidUtils.class.getSimpleName();

	private static Boolean isSensorProblemDevice = null;

	public static String getVersionName() {
		try {
			Context context = JukefoxApplication.getAppContext();
			ComponentName comp = new ComponentName(context, JukefoxApplication.class);
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return "";
		}
	}

	public static int getVersionCode() {
		try {
			Context context = JukefoxApplication.getAppContext();
			ComponentName comp = new ComponentName(context, JukefoxApplication.class);
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return -1;
		}
	}

	public static String getModel() {
		return android.os.Build.MODEL;
	}

	public static String getAndroidVersionName() {
		return android.os.Build.VERSION.RELEASE;
	}

	public static long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	public static long getAvailableExternalMemorySize() {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	// public static boolean isSdCardOk() {
	// try {
	// Log.d(TAG, "1");
	// File f = new File(Constants.SDCARD_PATH);
	// boolean b = f.exists() && f.isDirectory() && f.canWrite()
	// && f.canRead();
	// if (!b) {
	// return false;
	// }
	// Log.d(TAG, "2");
	// String[] files = f.list();
	// Log.d(TAG, "3");
	// if (files.length > 0) {
	// return true;
	// } else {
	// return false;
	// }
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// return false;
	// }
	// }

	public static boolean isSdCardOk() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Log.w(TAG, "Access to sd-card is read only");
			return false;
		} else {
			return false;
		}

	}

	public static boolean fileExists(String filename) throws IOException {
		File f = new File(filename);
		return f.exists();
	}

	public static boolean deleteFile(String filename) throws IOException {
		Log.v(TAG, "Delete file: " + filename);
		File f = new File(filename);
		return f.delete();
	}

	public static boolean isMultiTouchOs() {
		Log.v("Version String", android.os.Build.VERSION.RELEASE);
		// double osVersion =
		// Double.parseDouble(android.os.Build.VERSION.RELEASE);
		// return osVersion >= version;
		try {
			if (android.os.Build.VERSION.RELEASE.length() < 1) {
				return false;
			}
			String firstLetter = android.os.Build.VERSION.RELEASE.substring(0, 1);
			Integer v = Integer.parseInt(firstLetter);
			return v >= 2;
		} catch (Exception e) {
			Log.w("PersHelper", e);
			return android.os.Build.VERSION.RELEASE.startsWith("2");
		}

	}

	/**
	 * return true if the current os version has a problem with reading the
	 * duration of certain media files.
	 * 
	 * @param version
	 * @return
	 */
	public static boolean isDurationProblemOs() {
		Log.v(TAG, "Version String: " + android.os.Build.VERSION.RELEASE);
		try {
			if (android.os.Build.VERSION.RELEASE.length() < 1) {
				return false;
			}
			return android.os.Build.VERSION.RELEASE.startsWith("2.3");
		} catch (Exception e) {
			Log.w(TAG, e);
			return false;
		}

	}

	public static boolean isMediaScannerScanning(Context context) {
		boolean result = false;

		Cursor cursor = query(context, MediaStore.getMediaScannerUri(),
				new String[] { MediaStore.MEDIA_SCANNER_VOLUME }, null, null, null);
		if (cursor != null) {
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				result = "external".equals(cursor.getString(0));
			}
			cursor.close();
		}

		return result;
	}

	public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
	}

	public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder, int limit) {
		try {
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) {
				return null;
			}
			if (limit > 0) {
				uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
			}
			return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
		} catch (UnsupportedOperationException ex) {
			return null;
		}

	}

	public static Bitmap getBitmapFromResource(Resources res, int resourceId, int maxSize) {
		Bitmap bitmap = null;
		int dummySize = maxSize * maxSize / 4; // KB
		while (bitmap == null && dummySize <= maxSize * maxSize * 4) {
			try {
				int sampleFactor = getSampleFactor(res, resourceId, maxSize);
				BitmapFactory.Options resample = new BitmapFactory.Options();
				resample.inSampleSize = sampleFactor;
				bitmap = BitmapFactory.decodeResource(res, resourceId, resample);
			} catch (Throwable e) {
				System.gc();
				// Avoid that heap has to be grown for the BitmapFactory,
				// as this would lead to an out of memory error
				int[] dummyArray = new int[dummySize * 1024];
				// Avoid being eliminated by optimization of compiler
				if (dummyArray != null) {
					dummyArray = null;
					System.gc();
				}
				Log.w(TAG, e);
				dummySize *= 2;
			}
		}
		return bitmap;
	}

	public static Bitmap getBitmapFromInputStream(InputStream is, int maxSize) {

		byte[] inputBytes;
		try {
			// TODO: Check if PatchInputStream is still necessary. see
			// http://code.google.com/p/android/issues/detail?id=6066
			inputBytes = getByteArrayFromInputStream(new PatchInputStream(is));
		} catch (IOException e3) {
			Log.w(TAG, e3);
			return null;
		}

		return getBitmapFromByteArray(inputBytes, maxSize);

	}

	private static byte[] getByteArrayFromInputStream(InputStream is) throws IOException {
		// byte[] byteArray = null;
		//
		// byteArray = new byte[is.available()];
		// is.read(byteArray);
		// return byteArray;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}

	public static Bitmap getBitmapFromByteArray(byte[] byteArray, int maxSize) {
		Bitmap bitmap = null;
		try {
			try {

				int sampleFactor = getSampleFactor(byteArray, maxSize);
				BitmapFactory.Options resample = new BitmapFactory.Options();
				resample.inSampleSize = sampleFactor;
				bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, resample);
			} catch (Error e) {

				// Avoid that heap has to be grown for the BitmapFactory,
				// as this would lead to an out of memory error
				int[] dummyArray = new int[byteArray.length];
				// Avoid being eliminated by optimization of compiler
				if (dummyArray != null) {
					dummyArray = null;
					System.gc();
				}
				Log.w("BitmapFactory", e);
			}
			if (bitmap == null) {
				try {
					int sampleFactor = getSampleFactor(byteArray, maxSize);
					BitmapFactory.Options resample = new BitmapFactory.Options();
					resample.inSampleSize = sampleFactor;
					bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, resample);
				} catch (Error e) {
					System.gc();
					Log.w("BitmapFactory", e);
				}
			}
		} catch (Throwable e) {
			Log.w("Bitmap", e);
		}
		return bitmap;
	}

	public static Bitmap getBitmapFromFile(String path, int maxSize) {
		Bitmap bitmap = null;
		int dummySize = maxSize * maxSize / 4; // KB
		while (bitmap == null && dummySize <= maxSize * maxSize * 4) {
			try {
				int sampleFactor = getSampleFactor(path, maxSize);
				BitmapFactory.Options resample = new BitmapFactory.Options();
				resample.inSampleSize = sampleFactor;
				bitmap = BitmapFactory.decodeFile(path, resample);
			} catch (Throwable e) {
				// Avoid that heap has to be grown for the BitmapFactory,
				// as this would lead to an out of memory error
				int[] dummyArray = new int[dummySize * 1024];
				// Avoid being eliminated by optimization of compiler
				if (dummyArray != null) {
					dummyArray = null;
					System.gc();
				}
				Log.w(TAG, e);
				dummySize *= 2;
			}
		}
		return bitmap;

	}

	private static int getSampleFactor(String path, int maxSize) {
		BitmapFactory.Options bounds = getBounds(path);
		return getSampleFactor(maxSize, bounds);
	}

	private static int getSampleFactor(byte[] byteArray, int maxSize) {
		BitmapFactory.Options bounds = getBounds(byteArray);
		return getSampleFactor(maxSize, bounds);
	}

	private static int getSampleFactor(Resources res, int resrouceId, int maxSize) {
		BitmapFactory.Options bounds = getBounds(res, resrouceId);
		return getSampleFactor(maxSize, bounds);
	}

	private static int getSampleFactor(int maxSize, BitmapFactory.Options bounds) {
		int width = bounds.outWidth;
		int height = bounds.outHeight;
		if (width > height) {
			if (width > maxSize) {
				return width / maxSize + 1;
			} else {
				return 1;
			}
		} else {
			if (height > maxSize) {
				return height / maxSize + 1;
			} else {
				return 1;
			}
		}
	}

	public static BitmapFactory.Options getBounds(byte[] byteArray) {
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, bounds);
		return bounds;
	}

	public static BitmapFactory.Options getBounds(Resources res, int resourceId) {
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resourceId, bounds);
		return bounds;
	}

	public static BitmapFactory.Options getBounds(String path) {
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bounds);
		return bounds;
	}

	public static boolean hasInternetConnection(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(TAG, "couldn't get connectivity manager");

		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();

			if (info != null) {

				for (int i = 0; i < info.length; i++) {

					if (info[i].getState() == NetworkInfo.State.CONNECTED) {

						return true;

					}

				}

			}
		}
		return false;
	}

	/**
	 * On the Motorola Atrix, activating the sensors results in a buzzing sound.
	 * On Android 2.3.x devices activating the sensors leads to a huge battery
	 * consumption
	 * 
	 * @return
	 */
	public static boolean isSensorProblemDevice() {
		if (isSensorProblemDevice == null) {
			// Compare it to the model name of the motorola atrix
			if (android.os.Build.MODEL.equals("MB860")) {
				Log.v(TAG, "SensorProblemDevice - MB860");
				isSensorProblemDevice = true;
			} else if (android.os.Build.VERSION.RELEASE.startsWith("2.3")) {
				Log.v(TAG, "SensorProblemDevice - 2.3.x");
				isSensorProblemDevice = true;
			} else {
				isSensorProblemDevice = false;
			}
		}
		Log.v(TAG, "isSensorProblemDevice: " + isSensorProblemDevice);
		return isSensorProblemDevice;
	}

	public static void printSongCollection(String label,
			Collection<? extends BaseSong<? extends BaseArtist, ? extends BaseAlbum>> songs) {

		Log.v(TAG, label);
		for (BaseSong<? extends BaseArtist, ? extends BaseAlbum> s : songs) {
			Log.v(TAG, "id: " + s.getId() + ", " + s.getArtist() + " - " + s.getTitle());
		}
	}

	public static void printCollection(String label, Collection<? extends Object> collection) {
		Log.v(TAG, label);
		for (Object o : collection) {
			Log.v(TAG, o.toString());
		}
	}
}
