// Hyper-Point class supporting KDTree class

package edu.wlu.cs.levy.CG;

import java.io.Serializable;

@SuppressWarnings("serial")
class HPoint implements Serializable {

	protected float[] coord;

	protected HPoint(int n) {
		coord = new float[n];
	}

	protected HPoint(float[] x) {

		coord = new float[x.length];
		for (int i = 0; i < x.length; ++i) {
			coord[i] = x[i];
		}
	}

	@Override
	protected Object clone() {

		return new HPoint(coord);
	}

	protected boolean equals(HPoint p) {

		// seems faster than java.util.Arrays.equals(), which is not
		// currently supported by Matlab anyway
		for (int i = 0; i < coord.length; ++i) {
			if (coord[i] != p.coord[i]) {
				return false;
			}
		}

		return true;
	}

	protected static float sqrdist(HPoint x, HPoint y) {

		return EuclideanDistance.sqrdist(x.coord, y.coord);
	}

	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < coord.length; ++i) {
			s = s + coord[i] + " ";
		}
		return s;
	}

}
