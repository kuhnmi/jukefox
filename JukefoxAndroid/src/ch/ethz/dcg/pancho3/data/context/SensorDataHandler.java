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

import java.util.LinkedList;


public abstract class SensorDataHandler<DataType, EventType> {
	
	class SensorValue {
		public DataType value;
		public long time;
		public long validTime;

		public SensorValue(DataType value, long time, int validTime) {
			this.value = value;
			this.time = time;
			this.validTime = validTime;
		}
		
	}

	public static final String TAG = SensorDataHandler.class.getSimpleName();
	
	protected int dataKeepTime = 60*1000;
	
	LinkedList<SensorValue> sensorData;
	
	public SensorDataHandler() {
		sensorData = new LinkedList<SensorValue>();
	}
	
	protected synchronized void appendSensorData(DataType value, long time) {
		if (sensorData.size() > 0) {
			SensorValue sv = sensorData.getLast();
			sv.validTime = time - sv.time; 
		}
		sensorData.add(new SensorValue(value, time, 0));
		
		// remove old values in a fashion that we always have data since at least time - dataKeepTime
		while (sensorData.size() > 2) {
			if (sensorData.get(1).time < time - dataKeepTime) {
				sensorData.remove(0);
			} else {
				break;
			}
		}
	}	
	
	public synchronized DataType getLatestValue() {
		if (sensorData.size() > 0) {
			return sensorData.getLast().value;
		} else {
			return null;
		}
	}
	
	public abstract DataType getMeanValue(int millisBack);
	
	public abstract void onNewEvent(EventType event);
}
