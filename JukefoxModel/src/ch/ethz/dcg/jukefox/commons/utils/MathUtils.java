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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import ch.ethz.dcg.jukefox.commons.Constants;

/**
 * A collection of mathematical functions.
 */
public class MathUtils {

	private final static String TAG = MathUtils.class.getSimpleName();

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

	public static float scalarProduct(float[] v1, float[] v2) {
		float s = 0;
		for (int i = 0; i < Constants.DIM; i++) {
			s += v1[i] * v2[i];
		}
		return s;
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
		//		long t1 = System.currentTimeMillis();
		if (count > range) {
			return null;
		}
		if (count < 0 || range < 0) {
			return null;
		}
		ArrayList<Integer> indices = new ArrayList<Integer>();
		if (count < range / 2) {
			HashMap<Integer, Integer> used = new HashMap<Integer, Integer>();
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
		} else {

			ArrayList<Integer> unused = new ArrayList<Integer>(range);
			for (int i = 0; i < range; i++) {
				//				Log.v(TAG, "TT" + i + " " + (System.currentTimeMillis() - t1));
				unused.add(i);
			}
			int u = 0;
			while (indices.size() < count) {
				//				Log.v(TAG, "TD" + u + " " + (System.currentTimeMillis() - t1));
				Integer r = unused.remove(rnd.nextInt(unused.size()));
				indices.add(r);
				u++;
			}
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

	/**
	 * Returns a random subset of the given list. The subset is of size not bigger than count.
	 * 
	 * @param list
	 * @param count
	 * @param rnd
	 *            Random generator
	 * @return
	 */
	public static <T> List<T> getRandomElements(List<T> list, int count, Random rnd) {
		count = Math.min(count, list.size());
		List<Integer> positions = getRandomNumbers(list.size(), count, rnd);

		List<T> ret = new ArrayList<T>(count);
		for (Integer pos : positions) {
			ret.add(list.get(pos));
		}
		return ret;
	}

	/**
	 * @see #getRandomElements(List, int, Random)
	 */
	public static <T> List<T> getRandomElements(List<T> list, int count) {
		return getRandomElements(list, count, RandomProvider.getRandom());
	}

	public static float dotProduct(float[] vector1, float[] vector2) {
		if (vector1 == null || vector2 == null) {
			return 0;
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

	public static float norm2(float[] vec) {
		float sum = 0;
		for (float f : vec) {
			sum += Math.pow(f, 2);
		}
		return (float) Math.sqrt(sum);
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

}
