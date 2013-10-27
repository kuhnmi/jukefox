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
package ch.ethz.dcg.pancho3.controller.player;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxNotificationManager;

public class PlayerService extends Service {

	public static final String ACTION_PLAY_PAUSE = "ch.ethz.dcg.pancho3.playPause";
	public static final String ACTION_HEADSET_PLAY_PAUSE = "ch.ethz.dcg.pancho3.headsetPlayPause";
	public static final String ACTION_WIDGET_PLAY_PAUSE = "ch.ethz.dcg.pancho3.widgetPlayPause";
	public static final String ACTION_PREVIOUS = "ch.ethz.dcg.pancho3.previous";
	public static final String ACTION_NEXT = "ch.ethz.dcg.pancho3.next";
	public static final String ACTION_HEADSET_PLUGGED = "ch.ethz.dcg.pancho3.headsetPlugged";
	public static final String ACTION_HEADSET_UNPLUGGED = "ch.ethz.dcg.pancho3.headsetUnplugged";
	public static final String ACTION_CALL_START = "ch.ethz.dcg.pancho3.callStart";
	public static final String ACTION_CALL_END = "ch.ethz.dcg.pancho3.callEnd";
	public static final String ACTION_AUDIOBECOMING_NOISY = "ch.ethz.dcg.pancho3.audioBecomingNoisy";
	public static final String ACTION_MEDIA_SCANNER_FINISHED = "ch.ethz.dcg.pancho3.mediaScannerFinished";
	public static final String ACTION_DO_IMPORT = "ch.ethz.dcg.pancho3.doImport";
	public static final String ACTION_PLAY_DATE_RANGE = "ch.ethz.dcg.pancho3.playDateRange";
	public static final String ACTION_PLAY_TAG = "ch.ethz.dcg.pancho3.playTag";
	public static final String ACTION_STOP_MUSIC = "ch.ethz.dcg.pancho3.stopMusic";
	public static final String ACTION_UPDATE_LARGE_WIDGET = "ch.ethz.dcg.pancho3.updateLargeWidget";
	public static final String ACTION_UPDATE_NORMAL_WIDGET = "ch.ethz.dcg.pancho3.updateNormalWidget";
	public static final String ACTION_MEDIA_MOUNTED = "ch.ethz.dcg.pancho3.mediaMounted";
	public static final String ACTION_CANCEL_IMPORT = "ch.ethz.dcg.pancho3.cancelImport";

	public static final String TAG = PlayerService.class.getSimpleName();

	private JukefoxApplication application;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		super.onCreate();
		application = (JukefoxApplication) getApplication();
		AndroidPlayerController.setPlayerService(this);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v(TAG, "onStart()");
		executeIntentAction(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand()");
		executeIntentAction(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return Service.START_STICKY;
	}

	private void executeIntentAction(final Intent intent) {
		if (intent == null || intent.getAction() == null) {
			Log.v(TAG, "executeIntentAction: no intent or no intent action");
			return;
		}
		Log.v(TAG, "executeIntentAction: " + intent.getAction());

		JoinableThread executeAction = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				while (JukefoxApplication.getPlayerController() == null
						|| !JukefoxApplication.getPlayerController().isReady()) {
					try {
						JoinableThread.sleep(50);
					} catch (InterruptedException e) {
						Log.w(TAG, e);
					}
				}
				Log.v(TAG, "Got Intent action: " + intent.getAction());
				String action = intent.getAction();
				if (action.equals(ACTION_PLAY_PAUSE) || action.equals(ACTION_WIDGET_PLAY_PAUSE)) {
					application.getController().playPauseButtonPressed();
				}
				if (action.equals(ACTION_HEADSET_PLAY_PAUSE)) {
					long eventTime = intent.getLongExtra(Controller.INTENT_EXTRA_EVENT_TIME, 0);
					application.getController().headsetPlayPausePressed(eventTime);
				} else if (action.equals(ACTION_PREVIOUS)) {
					application.getController().previousButtonPressed();
				} else if (action.equals(ACTION_NEXT)) {
					application.getController().nextButtonPressed();
				} else if (action.equals(ACTION_HEADSET_PLUGGED)) {
					application.getController().headsetPlugged();
				} else if (action.equals(ACTION_HEADSET_UNPLUGGED)) {
					application.getController().headsetUnplugged();
				} else if (action.equals(ACTION_CALL_START)) {
					application.getController().callStarted();
				} else if (action.equals(ACTION_CALL_END)) {
					application.getController().callEnded();
				} else if (action.equals(ACTION_AUDIOBECOMING_NOISY)) {
					application.getController().audioBecameNoisy();
				} else if (action.equals(ACTION_MEDIA_SCANNER_FINISHED)) {
					application.getController().androidMediaScannerFinished();
				} else if (action.equals(ACTION_DO_IMPORT)) {
					application.getController().doImportAsync(false, false);
				} else if (action.equals(ACTION_PLAY_DATE_RANGE)) {
					application.getController().playDateRangeFromIntent(intent);
				} else if (action.equals(ACTION_PLAY_TAG)) {
					application.getController().playTagFromIntent(intent);
				} else if (action.equals(ACTION_STOP_MUSIC)) {
					application.getController().stopMusicFromIntent(intent);
				} else if (action.equals(ACTION_UPDATE_LARGE_WIDGET)) {
					application.getController().updateLargeWidget();
				} else if (action.equals(ACTION_UPDATE_NORMAL_WIDGET)) {
					application.getController().updateNormalWidget();
				} else if (action.equals(ACTION_MEDIA_MOUNTED)) {
					try {

						Playlist playlist = JukefoxApplication.getPlayerModel().getPlaylistManager()
								.loadPlaylistFromFileByName(AndroidConstants.CURRENT_PLAYLIST_NAME);
						JukefoxApplication.getPlayerController().setPlaylist(playlist);
					} catch (DataUnavailableException e) {
						Log.w(TAG, e);
					}
					application.getController().updateLargeWidget();
					application.getController().updateNormalWidget();
				} else if (action.equals(ACTION_CANCEL_IMPORT)) {
					application.getController().abortImportAsync();
				} else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
					Log.v(TAG, "Connectivity changed");
				}
				if (!application.getController().stopServiceIfNotNeeded()) {
					JukefoxNotificationManager nm = JukefoxApplication.getNotificationManager();
					startForeground(nm.getPlayerNotificationId(), nm.getCurrentNotification());
				}
			}

		});
		executeAction.start();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy()");
		// playerController.onServiceDestroy();
		super.onDestroy();
	}

}
