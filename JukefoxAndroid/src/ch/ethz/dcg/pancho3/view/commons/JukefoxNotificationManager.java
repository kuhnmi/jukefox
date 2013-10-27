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
package ch.ethz.dcg.pancho3.view.commons;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;

public class JukefoxNotificationManager {

	protected static final String TAG = JukefoxNotificationManager.class.getSimpleName();
	private final int PLAYER_NOTIFICATION_ID = 89765;
	private final int IMPORT_NOTIFICATION_ID = 89766;
	private final Context context;
	private final NotificationManager notificationManager;
	private final IReadOnlyPlayerController playerController;
	private IOnPlaylistStateChangeListener playlistListener;
	private IOnPlayerStateChangeListener playerListener;
	private ISettingsReader settingsReader;
	private long lastNotifiedAboutSong = 0;
	private static final int MIN_NOTIFICATION_INTERVAL = 3000;
	private BaseSong<BaseArtist, BaseAlbum> currentSong;
	private boolean notificationIsShown;
	private Notification currentNotification;

	public JukefoxNotificationManager(Context context, final IReadOnlyPlayerController playerController) {
		this.context = context;
		this.playerController = playerController;
		this.settingsReader = AndroidSettingsManager.getAndroidSettingsReader();

		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		createListeners();
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					if (playerController.isReady()) {
						playerController.addOnPlaylistStateChangeListener(playlistListener);
						playerController.addOnPlayerStateChangeListener(playerListener);
						break;
					} else {
						try {
							JoinableThread.sleep(50);
						} catch (InterruptedException e) {
							Log.w(TAG, e);
						}
					}
				}
			}

		});
		t.start();
	}

	private void createListeners() {
		playlistListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				if (settingsReader.areNotificationsShown()) {
					if (playerController.getPlayerState() == PlayerState.PLAY) {
						notifyAboutNewSong(newSong);
						lastNotifiedAboutSong = System.currentTimeMillis();
					}
				}
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {
			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
			}
		};
		playerListener = new IOnPlayerStateChangeListener() {

			@Override
			public void onPlayerStateChanged(PlayerState playerState) {
				if (playerState == PlayerState.PLAY) {
					try {
						if (System.currentTimeMillis() - lastNotifiedAboutSong > MIN_NOTIFICATION_INTERVAL) {
							notifyAboutNewSong(playerController.getCurrentSong());
						}
					} catch (EmptyPlaylistException e) {
						Log.w(TAG, e);
					}
				} else {
					lastNotifiedAboutSong = 0;
					clearNotification();
				}
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

		};
	}

	public void notifyAboutFinishedImport() {
		notify(context.getString(R.string.use_jukefox), context.getString(R.string.use_jukefox), true);
	}

	private void notifyAboutNewSong(BaseSong<BaseArtist, BaseAlbum> song) {
		if (song == null || notificationIsShown && song.getId() == currentSong.getId()) {
			return;
		}
		notificationIsShown = true;
		currentSong = song;
		notify(song.getArtist().getName() + " - " + song.getName(), context.getString(R.string.is_played_by_jukefox),
				false);

	}

	private synchronized void notify(String title, String text, boolean clearable) {
		if (!settingsReader.areNotificationsShown()) {
			return;
		}
		Notification notification = new Notification(R.drawable.d095_fox_head, title, System.currentTimeMillis());
		Intent contentIntent = new Intent(context, PlayerActivity.class);
		contentIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		PendingIntent appIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
		notification.setLatestEventInfo(context, title, text, appIntent);
		if (!clearable) {
			notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		}
		notificationManager.cancel(PLAYER_NOTIFICATION_ID);
		notificationManager.notify(PLAYER_NOTIFICATION_ID, notification);
		this.currentNotification = notification;
	}

	private synchronized void clearNotification() {
		notificationManager.cancel(PLAYER_NOTIFICATION_ID);
		notificationIsShown = false;
	}

	public int getPlayerNotificationId() {
		return PLAYER_NOTIFICATION_ID;
	}

	public Notification getCurrentNotification() {
		if (currentNotification == null) {
			String title = "jukefox";
			String text = "";
			boolean clearable = true;
			currentNotification = new Notification(R.drawable.d095_fox_head, title, System.currentTimeMillis());
			Intent contentIntent = new Intent(context, PlayerActivity.class);
			contentIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			PendingIntent appIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
			currentNotification.setLatestEventInfo(context, title, text, appIntent);
			if (!clearable) {
				currentNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
			}
		}
		return currentNotification;
	}

	public Notification getImportNotification() {
		String title = "jukefox";
		String text = "...is importing your library";
		boolean clearable = true;
		Notification importNotification = new Notification(R.drawable.d095_fox_head, title, System.currentTimeMillis());
		Intent contentIntent = new Intent(context, PlayerActivity.class);
		contentIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		PendingIntent appIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
		importNotification.setLatestEventInfo(context, title, text, appIntent);
		if (!clearable) {
			importNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		}
		return importNotification;
	}

	public int getImportNotificationId() {
		return IMPORT_NOTIFICATION_ID;
	}
}
