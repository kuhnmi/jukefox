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
package ch.ethz.dcg.jukefox.commons.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

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
import ch.ethz.dcg.jukefox.commons.Constants;
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

	public static float distance(float x1, float y1, float x2, float y2) {
		float d1 = x1 - x2;
		float d2 = y1 - y2;
		return (float) Math.sqrt((float) Math.pow(d1, 2) + Math.pow(d2, 2));
	}

	public static float squareDistance(float[] p1, float[] p2) throws ArrayIndexOutOfBoundsException {
		if (p1.length != p2.length) {
			throw new ArrayIndexOutOfBoundsException(Math.max(p1.length, p2.length));
		}
		float sum = 0;
		for (int i = 0; i < p1.length; i++) {
			float d = p1[i] - p2[i];
			sum += d * d;
		}
		return sum;
	}

	public static float distance(float[] p1, float[] p2) {
		return (float) Math.sqrt(squareDistance(p1, p2));
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

	public static boolean isNullOrEmpty(String string, boolean trim) {
		if (string == null || string.length() == 0) {
			return true;
		}
		if (trim && string.trim().length() == 0) {
			return true;
		}
		return false;
	}

	public static String replaceXmlEntities(String str) {
		if (str == null) {
			return null;
		}
		// $evilChars=array ( '&', '"', "'", '<', '>' );
		// $niceChards=array ( '&amp;' , '&quot;', '&apos;' , '&lt;' , '&gt;' );

		str = str.replace("&amp;", "&");
		str = str.replace("&quot;", "\"");
		str = str.replace("&apos;", "'");
		str = str.replace("&lt;", "<");
		str = str.replace("&gt;", ">");

		return str;
	}

	public static String readBufferToString(BufferedReader br) throws IOException {
		if (br == null) {
			return null;
		}
		StringBuffer buf = new StringBuffer();
		String line = br.readLine();
		while (line != null) {
			buf.append(line);
			buf.append("\n");
			line = br.readLine();
		}
		String content = buf.toString();
		return content;
	}

	public static boolean isNetworkException(Exception e) {
		if (e == null) {
			return false;
		}
		if (e instanceof SocketException) {
			return true;
		}
		if (e instanceof SocketTimeoutException) {
			return true;
		}
		if (e instanceof UnknownHostException) {
			return true;
		}
		return false;
	}

	public static float scalarProduct(float[] v1, float[] v2) {
		float s = 0;
		for (int i = 0; i < Constants.DIM; i++) {
			s += v1[i] * v2[i];
		}
		return s;
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

	public static float[] normalizeCoordsSum(float[] vector, float newSum) {
		float sum = 0;
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i];
		}
		for (int i = 0; i < vector.length; i++) {
			vector[i] /= sum * newSum;
		}
		return vector;
	}

	public static float[] getMean(float[] coords1, float[] coords2) {
		if (coords1.length != coords2.length) {
			Log.wtf(TAG, new NullPointerException());
			return null;
		}
		float[] mean = new float[coords1.length];
		for (int i = 0; i < coords1.length; i++) {
			mean[i] = (coords1[i] + coords2[i]) / 2;
		}
		return mean;
	}

	public static ArrayList<Integer> getRandomNumbers(int range, int count, Random rnd) {
		if (count > range) {
			return null;
		}
		if (count < 0 || range < 0) {
			return null;
		}
		HashMap<Integer, Integer> used = new HashMap<Integer, Integer>();
		ArrayList<Integer> indices = new ArrayList<Integer>();
		int n = range;
		while (indices.size() < count) {
			Integer r = Integer.valueOf(rnd.nextInt(n));
			if (used.containsKey(r)) {
				indices.add(used.get(r));
			} else {
				indices.add(r);
			}
			addToUsed(used, r, n - 1);
			n--;
		}
		return indices;
	}

	private static void addToUsed(HashMap<Integer, Integer> used, Integer key, Integer value) {
		if (used.containsKey(value)) {
			value = used.get(value);
			addToUsed(used, key, value);
		}
		used.put(key, value);
	}

	public static float dotProduct(float[] vector1, float[] vector2) {
		if (vector1 == null || vector2 == null) {

		}
		float sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			float float1 = vector1[i];
			float float2 = vector2[i];
			// sum += vector1[i] * vector2[i];
			sum += float1 * float2;
		}
		return sum;
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

	public static float norm2(float[] vec) {
		float sum = 0;
		for (float f : vec) {
			sum += Math.pow(f, 2);
		}
		return (float) Math.sqrt(sum);
	}

	public static String getString(int resId) {
		return JukefoxApplication.getAppContext().getString(resId);
	}

	public static DefaultHttpClient createHttpClientWithDefaultSettings() {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, Constants.CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, Constants.CONNECTION_TIMEOUT);
		DefaultHttpClient httpClient = new DefaultHttpClient(params);
		HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(2, true);
		httpClient.setHttpRequestRetryHandler(retryHandler);
		return httpClient;
	}

	/**
	 * Add a weighted vector to another resultVec = resultVec + (summand*weight)
	 * 
	 * @param resultVec
	 * @param summand
	 */
	public static void addWeightedVector(float[] resultVec, float[] summand, float weight) {
		for (int i = 0; i < resultVec.length; i++) {
			resultVec[i] += summand[i] * weight;
		}
	}

	/**
	 * Divide all element of a vector by a scalar
	 * 
	 * @param resultVec
	 * @param divisor
	 */
	public static void divideVector(float[] resultVec, float divisor) {
		for (int i = 0; i < resultVec.length; i++) {
			resultVec[i] /= divisor;
		}
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

	public static boolean equals(float[] p1, float[] p2) {
		if (p1 == null) {
			return p2 == null;
		}
		if (p2 == null) {
			return false;
		}
		if (p1.length != p2.length) {
			return false;
		}
		for (int i = 0; i < p1.length; i++) {
			if (p1[i] != p2[i]) {
				return false;
			}
		}
		return true;
	}

	public static int getHashCode(float[] p) {
		if (p == null) {
			return 0;
		}
		int hash = 0;
		for (Float f : p) {
			hash ^= f.hashCode();
		}
		return hash;
	}

	public static List<String> listFilesRecursive(String startPath, FilenameFilter filter) {
		List<String> files = new ArrayList<String>();
		File start = new File(startPath);
		addFilesRecursive(files, start, filter);
		return files;
	}

	public static void addFilesRecursive(List<String> files, File location, FilenameFilter filter) {
		if (!location.exists()) {
			return;
		}

		if (!location.isDirectory()) {
			if (filter.accept(location.getParentFile(), location.getName())) {
				files.add(location.getAbsolutePath());
			}
		}

		// we are in a directory => add all files matching filter and then
		// recursively add all files in subdirectories
		File[] tmp = location.listFiles(filter);
		if (tmp != null) {
			for (File file : tmp) {
				files.add(file.getAbsolutePath());
			}
		}

		File[] dirs = location.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		if (dirs == null) {
			return;
		}
		for (File dir : dirs) {
			addFilesRecursive(files, dir, filter);
		}
	}

}
