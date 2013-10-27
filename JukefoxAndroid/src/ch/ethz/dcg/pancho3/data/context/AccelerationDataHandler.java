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

import android.hardware.SensorEvent;

/**
 * Computes the energy in the Acceleration signal and returns it
 * 
 * @author saaam
 * 
 */
public class AccelerationDataHandler extends
		SensorDataHandler<AccelerationData, SensorEvent> {

	public static final String TAG = AccelerationDataHandler.class
			.getSimpleName();
	public static final int AGGREGATION_TIME = 5 * 1000;

	public static final int ORIENTATION_X1 = 1;
	public static final int ORIENTATION_X2 = 2;
	public static final int ORIENTATION_Y1 = 3;
	public static final int ORIENTATION_Y2 = 4;
	public static final int ORIENTATION_Z1 = 5;
	public static final int ORIENTATION_Z2 = 6;

	// private Float tempValue = 0f;
	private long tempValueTime = System.currentTimeMillis();
	private int numTempValElements = 0;
	private ShakeSkipDetector shakeSkipDetector;

	private float[] mean = new float[3];
	private float meanEnergy = 0;

	private float[] lastVal = new float[3];
	private boolean lastValSet = false;

	public AccelerationDataHandler() {
		super();
		shakeSkipDetector = new ShakeSkipDetector();
	}

	public synchronized void onNewEvent(SensorEvent event) {
		if (event == null) {
			return;
		}
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		shakeSkipDetector.detectShake(event);
		float dx = x - lastVal[0];
		float dy = y - lastVal[1];
		float dz = z - lastVal[2];
		float energy = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (lastValSet) {
			long time = System.currentTimeMillis();
			if (time - tempValueTime > AGGREGATION_TIME) {
				if (numTempValElements > 0) {
					mean[0] = mean[0] / numTempValElements;
					mean[1] = mean[1] / numTempValElements;
					mean[2] = mean[2] / numTempValElements;
					AccelerationData data = new AccelerationData(mean);
					data.setEnergy(meanEnergy / numTempValElements);
//					Log.v(TAG, "1energy: " + data.getEnergy());
					appendSensorData(data, time);
				} else {
					AccelerationData data = new AccelerationData(event.values);
					data.setEnergy(energy);
//					Log.v(TAG, "2energy: " + data.getEnergy());
					appendSensorData(data, time);
				}
				numTempValElements = 1;
				mean[0] = x;
				mean[1] = y;
				mean[2] = z;
				meanEnergy = energy;
				tempValueTime = time;
			} else {
				mean[0] += x;
				mean[1] += y;
				mean[2] += z;
				meanEnergy += energy;
				numTempValElements++;
			}
		}
		lastVal[0] = x;
		lastVal[1] = y;
		lastVal[2] = z;
		lastValSet = true;
	}

	// public synchronized void onNewEvent(SensorEvent event) {
	// float x = event.values[0];
	// float y = event.values[1];
	// float z = event.values[2];
	// // Log.v(TAG, "x: " + mean[0] + ", y: " + mean[1] + ", z: " + mean[2]);
	// shakeSkipDetector.detectShake(event);
	// float energy = (float) Math.sqrt(x * x + y * y + z * z);
	// long time = System.currentTimeMillis();
	// if (time - tempValueTime > AGGREGATION_TIME) {
	// if (numTempValElements > 0) {
	// mean[0] = mean[0] / numTempValElements;
	// mean[1] = mean[1] / numTempValElements;
	// mean[2] = mean[2] / numTempValElements;
	// AccelerationData data = new AccelerationData(mean);
	// data.setEnergy(meanEnergy / numTempValElements);
	// Log.v(TAG, "1energy: " + data.getEnergy());
	// appendSensorData(data, time);
	// } else {
	// AccelerationData data = new AccelerationData(event.values);
	// data.setEnergy(energy);
	// Log.v(TAG, "2energy: " + data.getEnergy());
	// appendSensorData(data, time);
	// }
	// numTempValElements = 1;
	// mean[0] = x;
	// mean[1] = y;
	// mean[2] = z;
	// meanEnergy = energy;
	// tempValueTime = time;
	// } else {
	// mean[0] += x;
	// mean[1] += y;
	// mean[2] += z;
	// meanEnergy += energy;
	// numTempValElements++;
	// }
	// lastVal[0] = x;
	// lastVal[1] = y;
	// lastVal[2] = z;
	// lastValSet = true;
	// }

	@Override
	public synchronized AccelerationData getMeanValue(int millisBack) {
		if (sensorData.size() == 0 || millisBack <= 0) {
			return null;
		}
		long currentTime = System.currentTimeMillis();
		ListIterator<SensorValue> it = sensorData.listIterator(sensorData
				.size() - 1);
		SensorValue val = sensorData.getLast();
		float[] tempVal = val.value.getAcceleration();
		float tempEnergy = val.value.getEnergy();
		long lastTime = 0;
		int timeSum = 0;
		if (currentTime - val.time > millisBack) {
			return val.value;
		} else {
			int tempTime = (int) (currentTime - val.time);
			tempVal[0] *= tempTime;
			tempVal[1] *= tempTime;
			tempVal[2] *= tempTime;
			tempEnergy *= tempTime;
//			Log.v(TAG, "1tempTime. " + tempTime + " energy: "
//					+ val.value.getEnergy());
			lastTime = val.time;
			timeSum = tempTime;
		}
		while (it.hasPrevious()) {
			val = it.previous();
			float[] curAccs = val.value.getAcceleration();
			if (currentTime - val.time > millisBack) {
				long tempTime = lastTime - (currentTime - millisBack);
//				Log.v(TAG, "2tempTime. " + tempTime + " energy: "
//						+ val.value.getEnergy());
				tempVal[0] += curAccs[0] * tempTime;
				tempVal[1] += curAccs[1] * tempTime;
				tempVal[2] += curAccs[2] * tempTime;
				tempEnergy += val.value.getEnergy() * tempTime;
				lastTime = val.time;
				timeSum += tempTime;
			} else {
				long tempTime = lastTime - val.time;
//				Log.v(TAG, "2tempTime. " + tempTime + " energy: "
//						+ val.value.getEnergy());
				tempVal[0] += curAccs[0] * tempTime;
				tempVal[1] += curAccs[1] * tempTime;
				tempVal[2] += curAccs[2] * tempTime;
				tempEnergy += val.value.getEnergy() * tempTime;
				lastTime = val.time;
				timeSum += tempTime;
			}
			if (lastTime <= currentTime - millisBack) {
				break;
			}
		}
		tempVal[0] /= timeSum;
		tempVal[1] /= timeSum;
		tempVal[2] /= timeSum;
		tempEnergy /= timeSum;
		AccelerationData data = new AccelerationData(tempVal);
		data.setEnergy(tempEnergy);
		return data;
	}

}
