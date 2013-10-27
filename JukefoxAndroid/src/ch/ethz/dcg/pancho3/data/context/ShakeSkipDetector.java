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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.SensorEvent;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class ShakeSkipDetector implements OnSharedPreferenceChangeListener {

	public static final String TAG = ShakeSkipDetector.class.getSimpleName();
	public static final float GRAVITY_CONSTANT = 9.81f;

	private long lastLeftMotion = 0;
	private long lastRightMotion = 0;
	private long lastSensorReadingTime = 0;
	private long lastMotionControl = 0;
	private long motionControlBlockTime = 3000;
	private float accThreshholdX = 1;
	private float accAverageX = 0;
	private int timeThreshhold = 1000;
	private JukefoxApplication application;

	public ShakeSkipDetector() {
		this.application = JukefoxApplication.getInstance();
		readSettings();
		AndroidSettingsManager.getAndroidSettingsReader().addSettingsChangeListener(this);
	}

	private void readSettings() {
		accThreshholdX = (6 - AndroidSettingsManager.getAndroidSettingsReader().getShakeSkipThreshhold()) * 5
				* GRAVITY_CONSTANT / 9;
		Log.v(TAG, "Set shake threshhold: " + accThreshholdX + "m/(s*s)");
	}

	public void detectShake(SensorEvent event) {
		float x = event.values[0];
		// float y = event.values[1];
		// float z = event.values[2];
		long eventTime = System.currentTimeMillis();
		// Log.v(TAG, "x: " + x + ", y: " + y + ", z: " + z);
		if (lastMotionControl + motionControlBlockTime > eventTime) {
			return;
		}
		if (x > accThreshholdX + accAverageX) {
			lastSensorReadingTime = eventTime;
			lastLeftMotion = eventTime;
		} else if (x < -(accThreshholdX + accAverageX)) {
			lastSensorReadingTime = eventTime;
			lastRightMotion = eventTime;
		} else {
			if (eventTime - lastSensorReadingTime > 1000) {
				lastSensorReadingTime = eventTime;
				accAverageX = accAverageX * 0.97f + Math.abs(x) * 0.03f;
			}
			return;
		}

		if (Math.abs(lastLeftMotion - lastRightMotion) < timeThreshhold) {
			shakeDetected(eventTime);
			return;
		}
	}

	private void shakeDetected(long currentTime) {
		Log.v(TAG, "Shake detected");
		if (AndroidSettingsManager.getAndroidSettingsReader().isShakeSkip()) {
			lastMotionControl = currentTime;
			application.getController().nextButtonPressed();
			application.getController().doHapticFeedback();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(application.getString(R.string.KEY_SHAKE_SKIP_THRESHHOLD))) {
			readSettings();
		}
	}
}
