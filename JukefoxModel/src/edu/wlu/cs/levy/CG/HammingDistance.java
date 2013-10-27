// Hamming distance metric class

package edu.wlu.cs.levy.CG;

class HammingDistance extends DistanceMetric {

	@Override
	protected float distance(float[] a, float[] b) {

		float dist = 0;

		for (int i = 0; i < a.length; ++i) {
			float diff = a[i] - b[i];
			dist += Math.abs(diff);
		}

		return dist;
	}
}
