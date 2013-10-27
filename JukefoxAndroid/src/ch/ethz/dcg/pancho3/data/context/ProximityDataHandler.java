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

public class ProximityDataHandler extends FloatSensorDataHandler<SensorEvent> {

	public static final String TAG = ProximityDataHandler.class.getSimpleName();
	private float maxValue;

	@Override
	public synchronized void onNewEvent(SensorEvent event) {
		if (event == null) {
			return;
		}
		float dist = event.values[0];
		// TODO: basically the proximity sensor should return the distance. But
		// a lot of prox. sensors are binary and should therefore return either
		// max value or something lower (according to the android specs.).
		// However, the DEFY sensor says the max
		// value is 1E8 but returns 1E9 if the proximity sensor measures
		// nothing. Therefore a threshhold is just set to 30 centimeters
		if (dist > 30) {
			dist = 1;
		} else {
			dist = 0;
		}
//		Log.v(TAG, "Proximity: " + dist);
		appendSensorData(dist, System.currentTimeMillis());
	}

	public void setMax(float maximumRange) {
		maxValue = maximumRange;
//		Log.v(TAG, "max value: " + maxValue);
	}

}
