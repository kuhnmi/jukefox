// Hamming distance metric class

package edu.wlu.cs.levy.CG;

class EuclideanDistance extends DistanceMetric {

	@Override
	protected float distance(float[] a, float[] b) {

		return (float) Math.sqrt(sqrdist(a, b));

	}

	protected static float sqrdist(float[] a, float[] b) {

		float dist = 0;

		for (int i = 0; i < a.length; ++i) {
			float diff = a[i] - b[i];
			dist += diff * diff;
		}

		return dist;
	}
}
