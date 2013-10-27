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
package ch.ethz.dcg.pancho3.data.context;

public class AccelerationData {

	public static final String TAG = AccelerationData.class.getSimpleName();

	private float[] acceleration;
	private float energy;
	private int orientation;

	public AccelerationData(float[] acceleration) {
		super();
		if (acceleration == null || acceleration.length != 3) {
			acceleration = new float[3];
		}
		setAcceleration(acceleration);
	}

	public void setAcceleration(float[] acceleration) {
		this.acceleration = acceleration;
		float x = acceleration[0];
		float y = acceleration[1];
		float z = acceleration[2];		
		orientation = getOrientationForValue(x, y, z);
	}
	
	private int getOrientationForValue(float x, float y, float z) {
		float absX = Math.abs(x);
		float absY = Math.abs(y);
		float absZ = Math.abs(z);
		if (absX >= absY && absX >= absZ) {
			if (x > 0) {
				return AccelerationDataHandler.ORIENTATION_X1;
			} else {
				return AccelerationDataHandler.ORIENTATION_X2;
			}
		} else if (absY >= absX && absY >= absZ) {
			if (y > 0) {
				return AccelerationDataHandler.ORIENTATION_Y1;
			} else {
				return AccelerationDataHandler.ORIENTATION_Y2;
			}
		} else {
			if (z > 0) {
				return AccelerationDataHandler.ORIENTATION_Z1;
			} else {
				return AccelerationDataHandler.ORIENTATION_Z2;
			}
		}
		
	}

	public float[] getAcceleration() {
		return acceleration;
	}

	public float getEnergy() {
		return energy;
	}

	public int getOrientation() {
		return orientation;
	}

	
	public void setEnergy(float energy) {
		this.energy = energy;
	}
	
	

}
