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

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.data.context.AbstractContextResult;
import ch.ethz.dcg.jukefox.data.context.IContextProvider;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.commons.utils.AndroidUtils;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class AndroidContextProvider implements IOnPlayerStateChangeListener, IContextProvider {

	public static final String TAG = AndroidContextProvider.class.getSimpleName();

	private static final long NETWORK_STATE_POLL_INTERVALL = 10 * 1000;

	private SensorManager sm;
	private SensorEventListener proximityListener;
	private SensorEventListener lightListener;
	private SensorEventListener orientationListener;
	private SensorEventListener accelerationListener;
	private Sensor proximitySensor;
	private Sensor lightSensor;
	private Sensor orientationSensor;
	private Sensor accelerationSensor;
	private boolean sensorsRegistered = false;
	private AccelerationDataHandler accelerationDataHandler;
	private LightDataHandler lightDataHandler;
	private ProximityDataHandler proximityDataHandler;
	private OrientationDataHandler orientationDataHandler;
	private NetworkStateDataHandler networkStateDataHandler;

	private ConnectivityManager connectivity = null;
	private Timer networkConnectivityPoll = null;

	private IReadOnlyPlayerController playerController;

	public AndroidContextProvider() {
		sm = (SensorManager) JukefoxApplication.getAppContext().getSystemService(Activity.SENSOR_SERVICE);
		accelerationDataHandler = new AccelerationDataHandler();
		orientationDataHandler = new OrientationDataHandler();
		lightDataHandler = new LightDataHandler();
		proximityDataHandler = new ProximityDataHandler();
		networkStateDataHandler = new NetworkStateDataHandler();
		connectivity = (ConnectivityManager) JukefoxApplication.getAppContext().getSystemService(
				Context.CONNECTIVITY_SERVICE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.pancho3.data.context.IContextProvider#getLatestContextValues
	 * ()
	 */
	public AbstractContextResult getLatestContextValues() {
		AbstractContextResult result = new AndroidContextResult();
		result.setOrientationChange(orientationDataHandler.getLatestValue());
		AccelerationData accData = accelerationDataHandler.getLatestValue();
		if (accData != null) {
			result.setOrientation(accData.getOrientation());
			result.setAccelerationEnergy(accData.getEnergy());
		}
		result.setLight(proximityDataHandler.getLatestValue());
		result.setAccelerationEnergy(lightDataHandler.getLatestValue());
		result.setNetworkState(networkStateDataHandler.getLatestValue());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.pancho3.data.context.IContextProvider#getMeanContextValues
	 * (int)
	 */
	public AbstractContextResult getMeanContextValues(int millisBack) {
		AbstractContextResult result = new AndroidContextResult();
		try {
			result.setOrientationChange(orientationDataHandler.getMeanValue(millisBack));
			AccelerationData accData = accelerationDataHandler.getMeanValue(millisBack);
			if (accData != null) {
				result.setOrientation(accData.getOrientation());
				result.setAccelerationEnergy(accData.getEnergy());
			}
			result.setLight(lightDataHandler.getMeanValue(millisBack));
			result.setProximity(proximityDataHandler.getMeanValue(millisBack));
			result.setNetworkState(networkStateDataHandler.getMeanValue(millisBack));
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return result;
	}

	private synchronized void registerSensors() {
		// Do not register or unregister Sensors for devices for which the
		// sensors can not be properly unregistered
		if (AndroidUtils.isSensorProblemDevice()) {
			if (AndroidSettingsManager.getAndroidSettingsReader().isShakeSkip()) {
				registerAccelerometerOnly();
			}
			return;
		}
		Log.v(TAG, "registorSensors: sensorsRegistered: " + sensorsRegistered);
		if (sensorsRegistered) {
			return;
		}
		try {
			createSensorListeners();
			accelerationSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (accelerationSensor != null) {
				sm.registerListener(accelerationListener, accelerationSensor, SensorManager.SENSOR_DELAY_GAME);
			}

			proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (proximitySensor != null) {
				proximityDataHandler.setMax(proximitySensor.getMaximumRange());
				sm.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
			lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
			if (lightSensor != null) {
				sm.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
			orientationSensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (orientationSensor != null) {
				sm.registerListener(orientationListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
			networkConnectivityPoll = new Timer();
			networkConnectivityPoll.schedule(new TimerTask() {

				@Override
				public void run() {
					networkStateDataHandler.onNewEvent(connectivity.getActiveNetworkInfo());
				}

			}, 0, NETWORK_STATE_POLL_INTERVALL);
		} finally {
			sensorsRegistered = true;
		}
	}

	private void registerAccelerometerOnly() {
		accelerationListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				accelerationDataHandler.onNewEvent(event);
			}

		};
		accelerationSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerationSensor != null) {
			sm.registerListener(accelerationListener, accelerationSensor, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	private void unregisterAccelerometerOnly() {
		unregisterSensorListener(accelerationListener);
		accelerationListener = null;
		accelerationSensor = null;
	}

	private synchronized void unregisterSensors() {
		// Do not register or unregister Sensors for devices for which the
		// sensors can not be properly unregistered
		if (AndroidUtils.isSensorProblemDevice()) {
			if (AndroidSettingsManager.getAndroidSettingsReader().isShakeSkip()) {
				unregisterAccelerometerOnly();
			}
			return;
		}
		Log.v(TAG, "unregistorSensors: sensorsRegistered: " + sensorsRegistered);
		if (!sensorsRegistered) {
			return;
		}
		try {

			unregisterSensorListener(proximityListener);
			proximityListener = null;
			proximitySensor = null;
			unregisterSensorListener(lightListener);
			lightListener = null;
			lightSensor = null;
			unregisterSensorListener(orientationListener);
			orientationListener = null;
			orientationSensor = null;
			unregisterSensorListener(accelerationListener);
			accelerationListener = null;
			accelerationSensor = null;
			if (networkConnectivityPoll != null) {
				networkConnectivityPoll.cancel();
			}
		} finally {
			sensorsRegistered = false;
		}
	}

	private void unregisterSensorListener(SensorEventListener listener) {
		try {
			sm.unregisterListener(listener);
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private synchronized void createSensorListeners() {
		proximityListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				proximityDataHandler.onNewEvent(event);
			}

		};
		lightListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				lightDataHandler.onNewEvent(event);
			}

		};
		orientationListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				orientationDataHandler.onNewEvent(event);
			}

		};
		accelerationListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				accelerationDataHandler.onNewEvent(event);
			}

		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.pancho3.data.context.IContextProvider#reregisterSensors()
	 */
	public void reregisterSensors() {
		unregisterSensors();
		registerSensors();
	}

	@Override
	public void onPlayerStateChanged(final PlayerState playerState) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (playerState == PlayerState.PLAY) {
					// Log.v(TAG, "registering sensors");
					registerSensors();
				} else {
					// Log.v(TAG, "unregistering sensors");
					unregisterSensors();
				}
			}
		}).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.pancho3.data.context.IContextProvider#onSongCompleted()
	 */
	@Override
	public void onSongCompleted() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.pancho3.data.context.IContextProvider#onSongStarted()
	 */
	@Override
	public void onSongStarted() {
	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
	}

	@Override
	public void setPlayerController(IReadOnlyPlayerController playerController) {
		this.playerController = playerController;
		this.playerController.addOnPlayerStateChangeListener(this);
	}

}
