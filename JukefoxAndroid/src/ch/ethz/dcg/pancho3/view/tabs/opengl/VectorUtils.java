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
package ch.ethz.dcg.pancho3.view.tabs.opengl;

import android.util.Log;

public class VectorUtils {
	
	private static final String TAG = "VectorUtils";

	public static float[] crossProduct3D(float[] vec1, float[] vec2) {
		if (vec1.length != 3 || vec2.length != 3) return null;
		float[] result = new float[3];
		result[0] = vec1[1]*vec2[2] - vec1[2]*vec2[1];
		result[1] = vec1[2]*vec2[0] - vec1[0]*vec2[2];
		result[2] = vec1[0]*vec2[1] - vec1[1]*vec2[0];
		return result;
	}
	
	public static float angle3D(float[] vec1, float[] vec2) {
		if (vec1.length != 3 || vec2.length != 3) return 0;
		float length1 = (float) Math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1] +vec1[2]*vec1[2]);
		float length2 = (float) Math.sqrt(vec2[0]*vec2[0] + vec2[1]*vec2[1] +vec2[2]*vec2[2]);	
		float dotProd = dotProduct(vec1,vec2);
		float returnVal = (float) Math.acos(dotProd/(length1*length2));
		if (Float.isNaN(returnVal)) {
			Log.w(TAG, "Ret is null: " + vec1[0] + ", " + vec1[1] + ", " + vec1[2] + ", " + vec2[0] + ", " + vec2[1] + ", " + vec2[2] + "d: " + dotProd);
		}
		return returnVal;
	}
	
	public static float dotProduct(float[] vec1, float[] vec2) {
		if (vec1.length != vec2.length) return 0;
		float dotProd = 0;
		for (int i = 0; i < vec1.length; i++) {
			dotProd += vec1[i]*vec2[i];
		}
		return dotProd;
	}

	public static float[] rotateAroundZ3D(float[] vec, float angle) {
		if (vec.length != 3) return null;
		float[] result = new float[3];
		result[0] = (float) (Math.cos(angle)*vec[0] - Math.sin(angle)*vec[1]);
		result[1] = (float) (Math.sin(angle)*vec[0] + Math.cos(angle)*vec[1]);
		result[2] = vec[2];
		return result;
	}
	
	public static float[] rotateAroundY3D(float[] vec, float angle) {
		if (vec.length != 3) return null;
		float[] result = new float[3];
		result[0] = (float) (Math.cos(angle)*vec[0] + Math.sin(angle)*vec[2]);		
		result[1] = vec[1];
		result[2] = (float) (-Math.sin(angle)*vec[0] + Math.cos(angle)*vec[2]);
		return result;
	}
	
	public static float[] rotateAroundX3D(float[] vec, float angle) {
		if (vec.length != 3) return null;
		float[] result = new float[3];
		result[0] = vec[0];
		result[1] = (float) (Math.cos(angle)*vec[1] - Math.sin(angle)*vec[2]);				
		result[2] = (float) (Math.sin(angle)*vec[1] + Math.cos(angle)*vec[2]);
		return result;
	}

	public static void normalize(float[] vec) {
		float sum = 0;
		for (int i = 0; i < vec.length; i++) {
			sum += vec[i]*vec[i];
		}
		sum = (float) Math.sqrt(sum);
		for (int i = 0; i < vec.length; i++) {
			vec[i] /= sum;
		}
	}
}
