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
package ch.ethz.dcg.pancho3.model;

import java.io.File;
import java.util.Random;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.utils.AndroidLogPrinter;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Log.LogLevel;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.manager.AndroidDirectoryManager;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AndroidPlayerModelManager;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsEditor;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.commons.utils.AndroidUtils;
import ch.ethz.dcg.pancho3.controller.JukefoxIntentReceiver;
import ch.ethz.dcg.pancho3.view.commons.JukefoxNotificationManager;

public class JukefoxApplication extends Application {

	private final static String TAG = JukefoxApplication.class.getSimpleName();

	public static String unknownTitleAlias;
	public static String unknownArtistAlias;
	public static String unknownAlbumAlias;
	public static String albumArtistAlias;

	private static String uniqueId;
	private static Context appCtx;
	private static JukefoxApplication instance;
	private static PowerManager powerManager;
	private static boolean screenOn;

	private static Random random = RandomProvider.getRandom();
	private static AndroidCollectionModelManager collectionModel;
	private static AndroidPlayerModelManager playerModel;
	private static AndroidPlayerController playerController;
	private static Controller controller;
	private JukefoxIntentReceiver intentReceiver;
	private static AndroidDirectoryManager directoryManager;

	@SuppressWarnings("unused")
	private static JukefoxNotificationManager notificationManager;
	// private Timer playlistSaveTimer;
	private static Handler handler;
	private static WakeLockManager wakeLockManager;
	private static KeyguardManager keyguardManager;
	// private static KeyguardLock lock = null;
	public static long ignoreEventsUntil = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.createLogPrinter(new AndroidLogPrinter());
		directoryManager = new AndroidDirectoryManager();
		Log.setLogFileBasePath(directoryManager.getLogFileBasePath());
		Log.setLogLevel(LogLevel.VERBOSE); // FIXME debug only; should be LogLevel.ERROR
		appCtx = this;
		instance = this;

		// Initialize the wake lock before the model and the controller because
		// they could be used by them
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLockManager = new WakeLockManager(powerManager);

		collectionModel = new AndroidCollectionModelManager(this, directoryManager);
		playerModel = (AndroidPlayerModelManager) collectionModel
				.getPlayerModelManager(AndroidConstants.PLAYER_MODEL_NAME);
		playerController = new AndroidPlayerController(this, collectionModel, playerModel);
		uniqueId = generateUniqueDeviceInstallationId();
		controller = new Controller(this, playerController, collectionModel, playerModel);

		if (!AndroidSettingsManager.getAndroidSettingsReader().isFirstStart()) {
			controller.startStartupManager();
		} else {
			// ensure clean start...
			directoryManager.deleteDirectories();
			directoryManager.createAllDirectories();
		}
		notificationManager = new JukefoxNotificationManager(this, playerController);
		keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
		unknownTitleAlias = getString(R.string.unknown_title_alias);
		unknownAlbumAlias = getString(R.string.unknown_album_alias);
		unknownArtistAlias = getString(R.string.unknown_artist_alias);
		albumArtistAlias = getString(R.string.album_artist_alias);

		handler = new Handler();

		JukefoxIntentReceiver.updateJukefoxIntentReceiver(this);

		// Send logs async
		new Thread(new Runnable() {

			@Override
			public void run() {
				playerModel.getLogManager().sendLogs();
			}
		}).start();

		// startPlaylistSaveTimer();
		Log.d(TAG, "JukefoxApplication.onCreate() finished.");
	}

	public static void setIgnoreEventsTime(long time) {
		ignoreEventsUntil = time;
	}

	public static WakeLockManager getWakeLockManager() {
		return wakeLockManager;
	}

	public static Handler getHandler() {
		return handler;
	}

	public static String getUniqueId() {
		return uniqueId;
	}

	public static Context getAppContext() {
		return appCtx;
	}

	/**
	 * TODO: should we make sure this function can only be called by some classes (views that are no activities?)
	 * 
	 * @return the application object (singleton)
	 */
	public static JukefoxApplication getInstance() {
		return instance;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		if (intentReceiver != null) {
			unregisterReceiver(intentReceiver);
		}
		collectionModel.onTerminate();
		Log.v(TAG, "onTerminate()");
	}

	// swelten: I think the following 5 methods are not needed
	// public IModelViewApplicationState getApplicationStateReader() {
	// return model;
	// }

	// public IModelViewData getDataReader() {
	// return model;
	// }
	//
	// public ISettingsReader getSettingsReader() {
	// return model.getSettingsReader();
	// }
	//
	// public IModelControllerApplicationState getApplicationController() {
	// return model;
	// }

	// public IModelControllerData getDataController() {
	// return model;
	// }

	public JukefoxIntentReceiver getIntentReceiver() {
		return intentReceiver;
	}

	public void setIntentReceiver(JukefoxIntentReceiver intentReceiver) {
		this.intentReceiver = intentReceiver;
	}

	public Controller getController() {
		return controller;
	}

	public static AndroidPlayerController getPlayerController() {
		return playerController;
	}

	public static AndroidCollectionModelManager getCollectionModel() {
		return collectionModel;
	}

	public static AndroidDirectoryManager getDirectoryManager() {
		return directoryManager;
	}

	public static AbstractPlayerModelManager getPlayerModel() {
		return playerModel;
	}

	private String generateUniqueDeviceInstallationId() {

		String uniqueId;
		try {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

			// Compute a user id which has a first part that is unique for the
			// device and one that is unique for the installation
			String imei = tm.getDeviceId();
			uniqueId = Integer.toString(imei.hashCode(), 16);
			uniqueId = getPaddedId(uniqueId, 8);
			imei = imei.substring(1, imei.length() - 1);
			Integer imeiHash = imei.hashCode();
			if (imeiHash == Integer.MIN_VALUE) {
				imeiHash = imeiHash + 1;
			}
			Integer absHash = Math.abs(imeiHash);
			uniqueId = uniqueId + getPaddedId(Integer.toString(absHash, 16), 8);
		} catch (Exception e) {
			Log.w(TAG, e);
			uniqueId = "0000000000000000";
		}

		try {
			ISettingsReader settingsReader = AndroidSettingsManager.getAndroidSettingsReader();
			Long randomNr = settingsReader.getRandomUserHash();
			if (randomNr == null) {
				ISettingsEditor settingsEditor = AndroidSettingsManager.getAndroidSettingsEditor();
				randomNr = Math.abs(random.nextLong());
				if (randomNr < 0) {
					randomNr = 0L;
				}
				settingsEditor.setRandomUserHash(randomNr);
			}
			uniqueId = uniqueId + getPaddedId(Long.toString(randomNr, 16), 16);
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		Log.v(TAG, "Hash: " + uniqueId);
		return uniqueId;
	}

	private String getPaddedId(String id, int len) {
		StringBuilder sb = new StringBuilder();
		int cnt = len - id.length();
		for (int i = 0; i < cnt; i++) {
			sb.append("0");
		}
		sb.append(id);
		return sb.toString();
	}

	public static void setScreenOn(boolean b) {
		screenOn = b;
	}

	public static boolean isScreenOn() {
		return screenOn;
	}

	public static String getJukefoxVersion() {
		try {
			ComponentName comp = new ComponentName(appCtx, JukefoxApplication.class);
			PackageInfo pinfo = appCtx.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		} catch (Exception e) {
			Log.wtf(TAG, e);
			return e.getMessage();
		}
	}

	public static boolean ignoreMediaButtons() {
		if (!AndroidUtils.isSdCardOk()) {
			return true;
		}
		File f = directoryManager.getIgnoreMediaButtonsFile();
		if (f.exists()) {
			return true;
		}
		return false;
	}

	public static boolean isScreenLocked() {
		if (keyguardManager != null) {
			return keyguardManager.inKeyguardRestrictedInputMode();
		}
		return false;
	}

	public static void reregisterSensors() {
		playerModel.getContextProvider().reregisterSensors();
	}

	public static JukefoxNotificationManager getNotificationManager() {
		return notificationManager;
	}

	public void updateJukefoxIntentReceiver() {
		JukefoxIntentReceiver.updateJukefoxIntentReceiver(this);
	}
}
