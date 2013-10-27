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

import android.hardware.SensorEvent;

public class OrientationDataHandler extends FloatSensorDataHandler<SensorEvent> {

	public static final String TAG = OrientationDataHandler.class
			.getSimpleName();

	public static final float MEAN_FACTOR = 0.999f;
	public static final int AGGREGATION_TIME = 10 * 1000;

	private Float tempValue = 0f;
	private long tempValueTime = System.currentTimeMillis();
	private int numTempValElements = 0;

	private float[] lastVals = new float[3];
	private boolean lastValsSet = false;

	public void onNewEvent(SensorEvent event) {
		if (event == null) {
			return;
		}
		float x = Math.abs(event.values[0] - lastVals[0]);
		float y = Math.abs(event.values[1] - lastVals[1]);
		float z = Math.abs(event.values[2] - lastVals[2]);
		Double energy = Math.sqrt(x * x + y * y + z * z);
		long time = System.currentTimeMillis();
		if (lastValsSet) {
			if (time - tempValueTime > AGGREGATION_TIME) {
				if (numTempValElements > 0) {
					appendSensorData(tempValue / numTempValElements, time);
				} else {
					appendSensorData(energy.floatValue(), time);
				}
				numTempValElements = 1;
				tempValue = energy.floatValue();
				tempValueTime = time;
			} else {
				tempValue = tempValue + energy.floatValue();
				numTempValElements++;
			}
		}
		lastVals[0] = event.values[0];
		lastVals[1] = event.values[1];
		lastVals[2] = event.values[2];
		lastValsSet = true;
		// Log.v(TAG, "x: " + mean[0] + ", y: " + mean[1] + ", z: " + mean[2]);
	}

}
