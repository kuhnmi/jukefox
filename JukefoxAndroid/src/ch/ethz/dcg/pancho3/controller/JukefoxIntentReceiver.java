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
package ch.ethz.dcg.pancho3.controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.lockscreen.LockScreen;

public class JukefoxIntentReceiver extends BroadcastReceiver {

	/* Support for Android below 2.2, because better Headset Handling is only available from 2.2
	 * See http://android-developers.blogspot.ch/2010/06/allowing-applications-to-play-nicer.html*/
	static {
		initializeRemoteControlRegistrationMethods();
	}

	/* Support for Android below 2.2, because better Headset Handling is only available from 2.2*/
	private static Method mRegisterMediaButtonEventReceiver;
	private static Method mUnregisterMediaButtonEventReceiver;

	private final static String TAG = JukefoxIntentReceiver.class.getSimpleName();
	// public static long ignoreEventsUntil = 0;
	private long lastEventTime = 0;

	public JukefoxIntentReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String intentAction = intent.getAction();
		long currentTime = System.currentTimeMillis();
		Log.v(TAG, "intentAction: " + intentAction + ", time: " + currentTime);

		if (ignoreEventsAndCancel(currentTime, intent, intentAction)) {
			return;
		}

		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
			JukefoxApplication.setIgnoreEventsTime(currentTime + 2000);
			handleAudioBecomingNoisy(context, currentTime);
		} else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			handleMediaButtons(context, intent);
		} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intentAction)) {
			handlePhoneState(context, currentTime, intent);
		} else if (Intent.ACTION_HEADSET_PLUG.equals(intentAction)) {
			handleHeadsetPlug(context, currentTime, intent);
		} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intentAction)) {
			handleMediaScannerFinished(context);
		} else if (Intent.ACTION_SCREEN_ON.equals(intentAction)) {
			Log.v(TAG, "Screen on");
			JukefoxApplication.setScreenOn(true);
		} else if (Intent.ACTION_SCREEN_OFF.equals(intentAction)) {
			Log.v(TAG, "Screen off");
			JukefoxApplication.setScreenOn(false);
			/*
			 * unregister and register sensors to make them available even if
			 * the screen is off (see
			 * http://code.google.com/p/android/issues/detail?id=3708)
			 */
			JukefoxApplication.reregisterSensors();
			Intent intent2 = new Intent(context, LockScreen.class);
			intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent2);
		} else if (PlayerService.ACTION_PLAY_DATE_RANGE.equals(intentAction)) {
			Intent intent2 = new Intent(context, PlayerService.class);
			intent2.setAction(PlayerService.ACTION_PLAY_DATE_RANGE);
			intent2.putExtras(intent.getExtras());
			context.startService(intent2);
		} else if (PlayerService.ACTION_PLAY_TAG.equals(intentAction)) {
			Intent intent2 = new Intent(context, PlayerService.class);
			intent2.setAction(PlayerService.ACTION_PLAY_TAG);
			intent2.putExtras(intent.getExtras());
			context.startService(intent2);
		} else if (PlayerService.ACTION_STOP_MUSIC.equals(intentAction)) {
			Intent intent2 = new Intent(context, PlayerService.class);
			intent2.setAction(PlayerService.ACTION_STOP_MUSIC);
			if (intent.getExtras() != null) {
				intent2.putExtras(intent.getExtras());
			}
			context.startService(intent2);
		} else if (Intent.ACTION_MEDIA_EJECT.equals(intentAction)) {
			Intent intent2 = new Intent(context, PlayerService.class);
			intent2.setAction(PlayerService.ACTION_STOP_MUSIC);
			if (intent.getExtras() != null) {
				intent2.putExtras(intent.getExtras());
			}
			context.startService(intent2);
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(intentAction)) {
			Intent intent2 = new Intent(context, PlayerService.class);
			intent2.setAction(PlayerService.ACTION_MEDIA_MOUNTED);
			if (intent.getExtras() != null) {
				intent2.putExtras(intent.getExtras());
			}
			context.startService(intent2);
		}

	}

	private void handleAudioBecomingNoisy(Context context, long currentTime) {
		JukefoxApplication.ignoreEventsUntil = currentTime + 2000;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerService.ACTION_AUDIOBECOMING_NOISY);
		context.startService(intent);
	}

	private void handleMediaScannerFinished(Context context) {
		if (AndroidUtils.isSdCardOk()) {
			Intent intent = new Intent(context, PlayerService.class);
			intent.setAction(PlayerService.ACTION_MEDIA_SCANNER_FINISHED);
			context.startService(intent);
		}
	}

	private void handleHeadsetPlug(Context context, long currentTime, Intent intent) {
		int state = intent.getIntExtra("state", 0);
		Intent serviceIntent = new Intent(context, PlayerService.class);
		if (state > 0) {
			Log.v(TAG, "handling headset plugged");
			serviceIntent.setAction(PlayerService.ACTION_HEADSET_PLUGGED);
		} else {
			Log.v(TAG, "handling headset unplugged");
			serviceIntent.setAction(PlayerService.ACTION_HEADSET_UNPLUGGED);
		}
		context.startService(serviceIntent);
	}

	private void handlePhoneState(Context context, long currentTime, Intent intent) {
		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		Intent serviceIntent = new Intent(context, PlayerService.class);
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) || state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			serviceIntent.setAction(PlayerService.ACTION_CALL_START);

		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			serviceIntent.setAction(PlayerService.ACTION_CALL_END);
		}
		context.startService(serviceIntent);
	}

	private void handleMediaButtons(Context context, Intent intent) {
		if (JukefoxApplication.ignoreMediaButtons()) {
			return;
		}
		KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (event == null) {
			return;
		}

		if (event.getEventTime() - lastEventTime < 100) {
			lastEventTime = event.getEventTime();
			abortBroadcast();
			return;
		}

		lastEventTime = event.getEventTime();
		int action = event.getAction();

		if (action == KeyEvent.ACTION_DOWN) {
			Log.v("Key Down event time:", "" + event.getEventTime());
			handleMediaKeyDown(context, event);
		}

		abortBroadcast();
	}

	private void handleMediaKeyDown(Context context, KeyEvent event) {
		int keyCode = event.getKeyCode();
		long eventTime = event.getEventTime();
		Log.v("Receiver", "got keycode " + keyCode);
		try {
			switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_STOP:
					sendStopIntent(context);
					break;
				case KeyEvent.KEYCODE_HEADSETHOOK:
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					sendPlayPauseIntent(context, eventTime);
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					sendNextIntent(context);
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					sendPreviousIntent(context);
					break;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private boolean ignoreEventsAndCancel(long currentTime, Intent intent, String intentAction) {
		if (currentTime < JukefoxApplication.ignoreEventsUntil) {
			try {
				Log.v(TAG, intent.getAction());
				if (!Intent.ACTION_HEADSET_PLUG.equals(intentAction)) {
					abortBroadcast();
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			// abortBroadcast();
			return true;
		}
		return false;
	}

	private void sendPlayPauseIntent(Context context, long eventTime) {
		Intent playButtonIntent = new Intent(context, PlayerService.class);
		playButtonIntent.setAction(PlayerService.ACTION_HEADSET_PLAY_PAUSE);
		playButtonIntent.putExtra(Controller.INTENT_EXTRA_EVENT_TIME, eventTime);
		context.startService(playButtonIntent);
	}

	private void sendStopIntent(Context context) {
		Intent pauseButtonIntent = new Intent(context, PlayerService.class);
		pauseButtonIntent.setAction(PlayerService.ACTION_STOP_MUSIC);
		context.startService(pauseButtonIntent);
	}

	private void sendNextIntent(Context context) {
		Intent nextButtonIntent = new Intent(context, PlayerService.class);
		nextButtonIntent.setAction(PlayerService.ACTION_NEXT);
		context.startService(nextButtonIntent);
	}

	private void sendPreviousIntent(Context context) {
		Intent previousButtonIntent = new Intent(context, PlayerService.class);
		previousButtonIntent.setAction(PlayerService.ACTION_PREVIOUS);
		context.startService(previousButtonIntent);
	}

	public static synchronized void updateJukefoxIntentReceiver(JukefoxApplication application) {
		AudioManager audioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
		ComponentName eventReceiver = new ComponentName(application.getPackageName(),
				JukefoxIntentReceiver.class.getName());
		registerRemoteControl(audioManager, eventReceiver);
		if (application.getIntentReceiver() != null) {
			try {
				application.unregisterReceiver(application.getIntentReceiver());
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e);
			}
			application.setIntentReceiver(null);
		}
		IntentFilter intf = new IntentFilter();
		intf.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		intf.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		intf.addAction(Intent.ACTION_MEDIA_BUTTON);
		intf.addAction(Intent.ACTION_HEADSET_PLUG);
		// if (isPlaybackFunctionalityInitialized) Log.v(TAG, "PlayerState: " +
		// playManager.getPlayerState());
		// if (isPlaybackFunctionalityInitialized &&
		// playManager.getPlayerState() == PlayerState.PLAY &&
		// settings.isLockScreenControls()) {
		if (JukefoxApplication.getPlayerController().isReady() && JukefoxApplication.getPlayerController()
				.getPlayerState() == PlayerState.PLAY) {
			intf.addAction(Intent.ACTION_SCREEN_ON);
			intf.addAction(Intent.ACTION_SCREEN_OFF);
		}
		intf.addAction(PlayerService.ACTION_PLAY_DATE_RANGE);
		intf.addAction(PlayerService.ACTION_PLAY_TAG);
		intf.addAction(PlayerService.ACTION_STOP_MUSIC);
		intf.setPriority(1000);

		JukefoxIntentReceiver intentReceiver = new JukefoxIntentReceiver();
		application.setIntentReceiver(intentReceiver);

		application.registerReceiver(intentReceiver, intf);

		intf = new IntentFilter();
		intf.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intf.addAction(Intent.ACTION_MEDIA_EJECT);
		intf.addAction(Intent.ACTION_MEDIA_MOUNTED);
		intf.addDataScheme("file");

		application.registerReceiver(intentReceiver, intf);
	}

	/* Support for Android below 2.2, because better Headset Handling is only available from 2.2*/
	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (mRegisterMediaButtonEventReceiver == null) {
				mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
						"registerMediaButtonEventReceiver",
						new Class[] { ComponentName.class });
			}
			if (mUnregisterMediaButtonEventReceiver == null) {
				mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
						"unregisterMediaButtonEventReceiver",
						new Class[] { ComponentName.class });
			}
			/* success, this device will take advantage of better remote */
			/* control event handling                                    */
		} catch (NoSuchMethodException nsme) {
			/* failure, still using the legacy behavior, but this app    */
			/* is future-proof!                                          */
		}
	}

	/* Support for Android below 2.2, because better Headset Handling is only available from 2.2*/
	private static void registerRemoteControl(AudioManager mAudioManager, ComponentName intentReceiver) {
		try {
			if (mRegisterMediaButtonEventReceiver == null) {
				return;
			}
			mRegisterMediaButtonEventReceiver.invoke(mAudioManager,
					intentReceiver);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			Log.e("MyApp", "unexpected " + ie);
		}
	}

	/* Support for Android below 2.2, because better Headset Handling is only available from 2.2*/
	private static void unregisterRemoteControl(AudioManager mAudioManager, ComponentName intentReceiver) {
		try {
			if (mUnregisterMediaButtonEventReceiver == null) {
				return;
			}
			mUnregisterMediaButtonEventReceiver.invoke(mAudioManager,
					intentReceiver);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			System.err.println("unexpected " + ie);
		}
	}
}
