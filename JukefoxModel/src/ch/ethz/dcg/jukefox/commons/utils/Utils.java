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
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;

public class Utils {

	private static final String TAG = Utils.class.getSimpleName();

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

	public static boolean isNullOrEmpty(String string, boolean trim) {
		if (string == null || string.length() == 0) {
			return true;
		}
		if (trim && string.trim().length() == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true iff <code>(left == null) ? (right == null) : left.equals(right)</code>
	 * 
	 * @param left
	 *            The left hand side
	 * @param right
	 *            The right hand side
	 * @return If they are equals
	 */
	public static boolean nullEquals(Object left, Object right) {
		return (left == null) ? (right == null) : left.equals(right);
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

	public static float distance(float[] p1, float[] p2) {
		return (float) Math.sqrt(squareDistance(p1, p2));
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

	public static boolean deleteFile(String path) {
		Log.v(TAG, "Delete file: " + path);
		File f = new File(path);
		return f.delete();
	}

}
