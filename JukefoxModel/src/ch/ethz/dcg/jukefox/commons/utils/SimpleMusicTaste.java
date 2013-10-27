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
import java.util.List;
import java.util.Random;

/**
 * Class to represent the music taste of a subject based on a collection of
 * songs or artists. It works by taking a set of weighted music similarity
 * coordinates that fit the music taste of the subject. It then trains a model
 * based on kmeans that represents these coordinates. Once the music taste is
 * defined one can query a point in the music similarity space and get a rating
 * for it.
 * 
 * @author swelten
 * 
 */
public class SimpleMusicTaste {

	public static final String TAG = SimpleMusicTaste.class.getSimpleName();
	public static final int DEFAULT_NUM_ITERATIONS = 100;

	private float[][] classCenters;
	private float[] classCentersDist;
	private float[] classCentersNum;
	private int[] assignedClassCenter;
	private float[] weightSums;
	private int numCenters;
	private float maxDist = 0;

	/**
	 * Construct and initialize a music taste. May take some time, depending on
	 * the number of coordinates and maxNumCenters
	 * 
	 * @param weightedPreferences
	 *            A List of points in the music similarity space the subject
	 *            likes. The higher the weight of a coordinate the more
	 *            important it is.
	 * @param maxNumCenters
	 *            The maximal number of classes kmeans uses to model the taste
	 */
	public SimpleMusicTaste(List<Pair<float[], Integer>> weightedPreferences, int maxNumCenters) {
		if (weightedPreferences.size() == 0) {
			return;
		}

		classCenters = new float[maxNumCenters][weightedPreferences.get(0).first.length];
		assignedClassCenter = new int[weightedPreferences.size()];
		weightSums = new float[maxNumCenters];
		classCentersDist = new float[maxNumCenters];
		classCentersNum = new float[maxNumCenters];

		// Find optimal number of class centers. Therefore increase the number until we don't make enough progress
		double prevMaxDist = Float.MAX_VALUE;
		for (int i = 1; i <= maxNumCenters; i++) {

			double bestMaxDist = computeMusicTaste(weightedPreferences, i);

			Log.v(TAG, "Tried " + i + " classcenters: MaxDist: " + bestMaxDist + " prevMaxDist " + prevMaxDist);
			// If we don't improve by 5% => cancel and use the last numClasses value
			if (!(bestMaxDist < prevMaxDist - (prevMaxDist * 0.05))) {
				i--;
				computeMusicTaste(weightedPreferences, i);
				break;
			}
			prevMaxDist = bestMaxDist;
		}
	}

	/**
	 * Returns the rating of a certain point in the music similarity space based
	 * on the music taste. A rating of smaller than 1 means that it fits the
	 * taste. Larger than 1 means that it is outside of the current music taste.
	 * The larger the value is, the more distant from the current taste it is.
	 * 
	 * @param position
	 *            The position in the music similarity space that should be
	 *            rated
	 * @return a rating between 0 and infinity
	 */
	public float getRating(float[] position) {
		float nearestDist = Float.MAX_VALUE;
		for (int centerNr = 0; centerNr < numCenters; centerNr++) {
			float dist = distance(position, classCenters[centerNr]) / classCentersDist[centerNr];
			if (dist < nearestDist) {
				nearestDist = dist;
			}
		}
		return nearestDist;
	}

	private double computeMusicTaste(List<Pair<float[], Integer>> weightedPreferences, int numCenters) {

		this.numCenters = numCenters;
		int dimensionality = weightedPreferences.get(0).first.length;
		float bestMaxDist = Float.MAX_VALUE;

		// find best random start class centers
		// Try 10 different random class centers and run kmeans with the best centers at the end
		for (int i = 0; i < 10; i++) {

			performKmeans(weightedPreferences, numCenters, dimensionality, DEFAULT_NUM_ITERATIONS);
			if (maxDist < bestMaxDist) {
				bestMaxDist = maxDist;
			}

		}

		// Do best kmeans
		performKmeans(weightedPreferences, numCenters, dimensionality, DEFAULT_NUM_ITERATIONS);

		// printDebugOutput(numCenters);
		return bestMaxDist;
	}

	private void printDebugOutput(int numCenters, List<Pair<float[], Integer>> weightedPreferences) {
		Log.v(TAG, "Centers: " + numCenters + ", MaxDist: " + maxDist);
		for (int centerNr = 0; centerNr < numCenters; centerNr++) {
			Log.v(TAG, "C " + centerNr + ": " + classCentersDist[centerNr] + ", " + classCentersNum[centerNr]);
			if (classCentersDist[centerNr] == maxDist) {
				for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
					Pair<float[], Integer> entry = weightedPreferences.get(prefPos);

					int centerNum = assignedClassCenter[prefPos];
					float dist = distance(entry.first, classCenters[centerNum]);
					Log.v(TAG, "D: " + dist);

				}
			}
		}
	}

	private void performKmeans(List<Pair<float[], Integer>> weightedPreferences, int numCenters, int dimensionality,
			int numIterations) {

		classCenters = getRandomClassCenters(numCenters, dimensionality, weightedPreferences);

		for (int it = 0; it < numIterations; it++) {
			doKmeansIteration(weightedPreferences, numCenters, assignedClassCenter, weightSums);
		}

		//		printDebugOutput(numCenters, weightedPreferences);

		// Remove Outliers
		List<Pair<float[], Integer>> weightedPreferencesCleaned = removeOutliers(weightedPreferences, numCenters,
				assignedClassCenter, weightSums);

		classCenters = getRandomClassCenters(numCenters, dimensionality, weightedPreferencesCleaned);

		for (int it = 0; it < numIterations; it++) {
			doKmeansIteration(weightedPreferencesCleaned, numCenters, assignedClassCenter, weightSums);
		}

		//		printDebugOutput(numCenters, weightedPreferences);
	}

	private List<Pair<float[], Integer>> removeOutliers(List<Pair<float[], Integer>> weightedPreferences,
			int numCenters, int[] assignedClassCenter, float[] weightSums) {

		List<Pair<float[], Integer>> weightedPreferencesCleaned = new ArrayList<Pair<float[], Integer>>();
		double[] means = computeMeanDists(weightedPreferences);
		double[] vars = computeVar(weightedPreferences, means);

		//		for (int i = 0; i < numCenters; i++) {
		//			System.out.println("Center " + i + ": Dist mean: " + means[i] + ", Dist variance: " + vars[i]);
		//		}
		for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
			Pair<float[], Integer> entry = weightedPreferences.get(prefPos);

			int centerNum = assignedClassCenter[prefPos];
			float dist = distance(entry.first, classCenters[centerNum]);

			// Only keep it if it is not too far from the center
			if (dist < 2 * means[centerNum]) {
				weightedPreferencesCleaned.add(entry);
			} else {
				//				Log.v(TAG, "Removed entry with distance: " + dist + " from class center: " + centerNum);
			}

		}

		return weightedPreferencesCleaned;
	}

	private double[] computeVar(List<Pair<float[], Integer>> weightedPreferences, double[] means) {
		double[] vars = new double[numCenters];
		for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
			Pair<float[], Integer> entry = weightedPreferences.get(prefPos);

			int centerNum = assignedClassCenter[prefPos];
			float dist = distance(entry.first, classCenters[centerNum]);
			vars[centerNum] += (dist - means[centerNum]) * (dist - means[centerNum]);

		}
		for (int i = 0; i < numCenters; i++) {
			vars[i] /= classCentersNum[i];
		}
		return vars;
	}

	private double[] computeMeanDists(List<Pair<float[], Integer>> weightedPreferences) {
		double[] means = new double[numCenters];
		for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
			Pair<float[], Integer> entry = weightedPreferences.get(prefPos);

			float dist = distance(entry.first, classCenters[assignedClassCenter[prefPos]]);
			means[assignedClassCenter[prefPos]] += dist;

		}
		for (int i = 0; i < numCenters; i++) {
			means[i] /= classCentersNum[i];
		}
		return means;
	}

	private void doKmeansIteration(List<Pair<float[], Integer>> weightedPreferences, int numCenters,
			int[] assignedClassCenter, float[] weightSums) {
		reset(classCentersDist);
		reset(classCentersNum);
		maxDist = 0;

		// Expectation (Assign preferences to class centers
		for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
			Pair<float[], Integer> entry = weightedPreferences.get(prefPos);
			int nearestCenter = 0;
			float nearestDist = Float.MAX_VALUE;
			for (int centerNr = 0; centerNr < numCenters; centerNr++) {
				float dist = distance(entry.first, classCenters[centerNr]);
				if (dist < nearestDist) {
					nearestCenter = centerNr;
					nearestDist = dist;
				}
			}
			assignedClassCenter[prefPos] = nearestCenter;
			classCentersNum[nearestCenter] += 1;
			if (nearestDist > classCentersDist[nearestCenter]) {
				classCentersDist[nearestCenter] = nearestDist;
			}
			if (nearestDist > maxDist) {
				maxDist = nearestDist;
			}
		}
		//		System.out.println("MaxDist: " + maxDist);

		// Maximization (Recompute class centers)
		reset(weightSums);
		for (int centerNr = 0; centerNr < numCenters; centerNr++) {
			reset(classCenters[centerNr]);
		}
		for (int prefPos = 0; prefPos < weightedPreferences.size(); prefPos++) {
			Pair<float[], Integer> entry = weightedPreferences.get(prefPos);
			int assignedCenter = assignedClassCenter[prefPos];
			add(classCenters[assignedCenter], entry.first, entry.second);
			weightSums[assignedCenter] += entry.second;
		}
		for (int centerNr = 0; centerNr < numCenters; centerNr++) {
			divide(classCenters[centerNr], weightSums[centerNr]);
		}
	}

	private float[][] getRandomClassCenters(int numberOfCenters, int dimensionality,
			List<Pair<float[], Integer>> weightedPreferences) {
		List<Pair<float[], Integer>> potentialCenters = new ArrayList<Pair<float[], Integer>>(weightedPreferences);
		float[][] classCenters = new float[numberOfCenters][dimensionality];
		Random r = new Random();
		for (int i = 0; i < classCenters.length; i++) {
			Pair<float[], Integer> center = potentialCenters.remove(r.nextInt(potentialCenters.size()));
			for (int u = 0; u < classCenters[0].length; u++) {
				classCenters[i][u] = center.first[u];
			}
		}
		return classCenters;
	}

	private float distance(float[] v1, float[] v2) {
		double dist = 0, diff = 0;
		for (int i = 0; i < v1.length; i++) {
			diff = v1[i] - v2[i];
			dist += diff * diff;
		}
		return (float) Math.sqrt(dist);
	}

	/**
	 * return v1 = v1 + weight * v2
	 */
	private float[] add(float[] v1, float[] v2, float weight) {
		for (int i = 0; i < v1.length; i++) {
			v1[i] = v1[i] + weight * v2[i];
		}
		return v1;
	}

	/**
	 * return v1 = v1 / divisor
	 */
	private float[] divide(float[] v1, float divisor) {
		for (int i = 0; i < v1.length; i++) {
			v1[i] = v1[i] / divisor;
		}
		return v1;
	}

	/**
	 * return v1 = 0
	 */
	private float[] reset(float[] v1) {
		for (int i = 0; i < v1.length; i++) {
			v1[i] = 0;
		}
		return v1;
	}
}
