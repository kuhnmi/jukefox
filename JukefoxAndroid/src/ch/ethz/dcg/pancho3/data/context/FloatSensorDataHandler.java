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

import java.util.ListIterator;


public abstract class FloatSensorDataHandler<SensorEventType> extends SensorDataHandler<Float, SensorEventType> {

	public static final String TAG = FloatSensorDataHandler.class
			.getSimpleName();
	
	public synchronized Float getMeanValue(int millisBack) {
		if (sensorData.size() == 0 || millisBack <= 0) {
			return null;
		}
		long currentTime = System.currentTimeMillis();
		ListIterator<SensorValue> it = sensorData.listIterator(sensorData.size());		
		SensorValue val = sensorData.getLast();
		Float tempSum = 0f;
		long lastTime = 0;
		int timeSum = 0;
		if (currentTime - val.time > millisBack) {
			return val.value;
		} else {
			tempSum = val.value*(currentTime-val.time);
			lastTime = val.time;
			timeSum = (int)(currentTime-val.time);
		}
		while (it.hasPrevious()) {
			val = it.previous();
			if (currentTime - val.time > millisBack) {
				long tempTime = lastTime - (currentTime-millisBack); 
				tempSum += val.value*tempTime;
				lastTime = val.time;
				timeSum += tempTime;
			} else {
				tempSum += val.value*(lastTime-val.time);
				timeSum += (lastTime-val.time);
				lastTime = val.time;			
			}
			if (lastTime <= currentTime-millisBack) {
				break;
			}
		}
		return tempSum/timeSum;
	}
	
}
