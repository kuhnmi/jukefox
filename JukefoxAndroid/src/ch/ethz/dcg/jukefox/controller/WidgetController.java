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
package ch.ethz.dcg.jukefox.controller;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.BitmapReflection;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.widgets.JukefoxWidgetLargeSize;
import ch.ethz.dcg.pancho3.view.widgets.JukefoxWidgetNormalSize;

public class WidgetController {

	private static final String TAG = WidgetController.class.getSimpleName();
	Controller controller;
	IOnPlaylistStateChangeListener playlistChangeListener = null;
	IOnPlayerStateChangeListener playstateChangeListener = null;
	IOnPlaylistStateChangeListener playlistChangeListenerBig = null;
	IOnPlayerStateChangeListener playstateChangeListenerBig = null;

	public WidgetController(Controller controller) {
		this.controller = controller;
		updateNormalWidget();
		updateLargeWidget();
	}

	public synchronized void updateNormalWidget() {

		JoinableThread updateWidgetThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				Log.v(TAG, "initializing widget...");
				Context context = controller.getApplicationContext();
				RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetnormalsize);

				while (!controller.getPlayerController().isReady()) {
					try {
						JoinableThread.sleep(100);
					} catch (InterruptedException e) {
						Log.w(TAG, e);
					}
				}

				BaseSong<BaseArtist, BaseAlbum> song;
				try {
					song = controller.getPlayerController().getCurrentSong();
				} catch (EmptyPlaylistException e1) {
					song = null;
				}

				setSongInfoAndAlbumArt(remoteViews, song, true);

				setControlButtons(remoteViews);

				registerButtons(controller.getApplicationContext(), remoteViews);

				updateNormalWidgetView(context, remoteViews);

				if (playlistChangeListener == null || playstateChangeListener == null) {
					playlistChangeListener = getPlaylistChangeListener(R.layout.widgetnormalsize);
					controller.getPlayerController().addOnPlaylistStateChangeListener(playlistChangeListener);
					playstateChangeListener = getPlayerStateListener(R.layout.widgetnormalsize);
					controller.getPlayerController().addOnPlayerStateChangeListener(playstateChangeListener);
				}

				Log.v(TAG, "widget initialized.");
			}

		});
		updateWidgetThread.start();

	}

	public void updateLargeWidget() {
		JoinableThread updateWidgetThread2 = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				Log.v(TAG, "initializing widget...");
				Context context = controller.getApplicationContext();
				RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetbigsize);

				while (!controller.getPlayerController().isReady()) {
					try {
						JoinableThread.sleep(100);
					} catch (InterruptedException e) {
						Log.w(TAG, e);
					}
				}

				BaseSong<BaseArtist, BaseAlbum> song;
				try {
					song = controller.getPlayerController().getCurrentSong();
				} catch (EmptyPlaylistException e1) {
					song = null;
				}

				setSongInfoAndAlbumArt(remoteViews, song, false);

				setControlButtons(remoteViews);

				registerButtons(controller.getApplicationContext(), remoteViews);

				updateLargeWidgetView(context, remoteViews);

				if (playlistChangeListenerBig == null || playstateChangeListenerBig == null) {
					playlistChangeListenerBig = getPlaylistChangeListener(R.layout.widgetbigsize);
					controller.getPlayerController().addOnPlaylistStateChangeListener(playlistChangeListenerBig);
					playstateChangeListenerBig = getPlayerStateListener(R.layout.widgetbigsize);
					controller.getPlayerController().addOnPlayerStateChangeListener(playstateChangeListenerBig);
				}

				Log.v(TAG, "widget initialized.");
			}

		});
		updateWidgetThread2.start();
	}

	private void updateNormalWidgetView(Context context, RemoteViews remoteViews) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		ComponentName cn = new ComponentName(context, JukefoxWidgetNormalSize.class);
		try {
			Log.v(TAG, "Updating normal widget");
			appWidgetManager.updateAppWidget(cn, remoteViews);
		} catch (Exception e) {
			// Sometimes things go wrong when updateing the widget (Null pointer
			// exceptions)
			Log.w(TAG, e);
		}
	}

	private void updateLargeWidgetView(Context context, RemoteViews remoteViews) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		ComponentName cn = new ComponentName(context, JukefoxWidgetLargeSize.class);
		try {
			Log.v(TAG, "Updating large widget");
			appWidgetManager.updateAppWidget(cn, remoteViews);
		} catch (Exception e) {
			// Sometimes things go wrong when updateing the widget (Null pointer
			// exceptions)
			Log.w(TAG, e);
		}
	}

	private void setControlButtons(RemoteViews remoteViews) {
		if (controller.getPlayerController().getPlayerState() == PlayerState.PLAY) {
			remoteViews.setImageViewResource(R.id.widgetPlayPauseButton, R.drawable.d016_pause_button);
		} else {
			remoteViews.setImageViewResource(R.id.widgetPlayPauseButton, R.drawable.d017_play_button);
		}
	}

	private void setSongInfoAndAlbumArt(RemoteViews remoteViews, BaseSong<BaseArtist, BaseAlbum> song, boolean lowRes) {
		Log.v(TAG, "Setting Song Info");
		String title;
		if (song != null) {
			BaseAlbum album = song.getAlbum();
			title = song.getArtist().getName() + " - " + song.getName();
			Log.v(TAG, "Setting Song Info title: " + title);
			Bitmap bitmap;
			try {
				bitmap = JukefoxApplication.getCollectionModel().getAlbumArtProvider().getAlbumArt(album, lowRes);
				if (!lowRes) {
					bitmap = BitmapReflection.getReflection(bitmap);
				}
				if (bitmap != null) {
					remoteViews.setImageViewBitmap(R.id.widgetAlbumArt, bitmap);
				} else {
					remoteViews.setImageViewResource(R.id.widgetAlbumArt, R.drawable.d005_empty_cd);
				}
			} catch (NoAlbumArtException e) {
				remoteViews.setImageViewResource(R.id.widgetAlbumArt, R.drawable.d005_empty_cd);
			}
		} else {
			Log.e(TAG, "Setting Song Info3");
			title = controller.getApplicationContext().getString(R.string.artist_title_place_holder);
			remoteViews.setImageViewResource(R.id.widgetAlbumArt, R.drawable.d005_empty_cd);
		}
		remoteViews.setTextViewText(R.id.widget_label_track, title);
	}

	private IOnPlaylistStateChangeListener getPlaylistChangeListener(final int widgetLayoutId) {
		return new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(final PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				// Workaround by Sämy: this takes sometimes a long time (when no album art is around?) -> do it as a background task
				JukefoxApplication.getHandler().post(new Runnable() {

					@Override
					public void run() {

						Log.v(TAG, "current song changed: " + newSong);
						RemoteViews remoteViews = new RemoteViews(controller.getApplicationContext().getPackageName(),
								widgetLayoutId);
						if (remoteViews != null) {
							if (widgetLayoutId == R.layout.widgetnormalsize) {
								setSongInfoAndAlbumArt(remoteViews, newSong, true);
								updateNormalWidgetView(controller.getApplicationContext(), remoteViews);
							} else if (widgetLayoutId == R.layout.widgetbigsize) {
								setSongInfoAndAlbumArt(remoteViews, newSong, false);
								updateLargeWidgetView(controller.getApplicationContext(), remoteViews);
							}
						}
					}
				});
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {
			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
			}
		};
	}

	private IOnPlayerStateChangeListener getPlayerStateListener(final int widgetLayoutId) {
		return new IOnPlayerStateChangeListener() {

			@Override
			public void onPlayerStateChanged(PlayerState newPlayerState) {
				RemoteViews remoteViews = new RemoteViews(controller.getApplicationContext().getPackageName(),
						widgetLayoutId);
				if (remoteViews != null) {
					setControlButtons(remoteViews);
					if (widgetLayoutId == R.layout.widgetnormalsize) {
						updateNormalWidgetView(controller.getApplicationContext(), remoteViews);
					} else if (widgetLayoutId == R.layout.widgetbigsize) {
						updateLargeWidgetView(controller.getApplicationContext(), remoteViews);
					}
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

	public static void registerButtons(Context context, RemoteViews remoteViews) {
		// Intent albumArtIntent = new Intent(context, PlayerActivity.class);
		Intent albumArtIntent = new Intent(context, PlayerActivity.class);

		PendingIntent albumArtPendingIntent = PendingIntent.getActivity(context, 0, albumArtIntent, 0);

		remoteViews.setOnClickPendingIntent(R.id.widgetAlbumArt, albumArtPendingIntent);

		Intent playButtonIntent = new Intent(context, PlayerService.class);
		playButtonIntent.setAction(PlayerService.ACTION_WIDGET_PLAY_PAUSE);

		PendingIntent playButtonPendingIntent = PendingIntent.getService(context, 0, playButtonIntent, 0);

		remoteViews.setOnClickPendingIntent(R.id.widgetPlayPauseButton, playButtonPendingIntent);

		Intent previousButtonIntent = new Intent(context, PlayerService.class);
		previousButtonIntent.setAction(PlayerService.ACTION_PREVIOUS);

		PendingIntent previousButtonPendingIntent = PendingIntent.getService(context, 0, previousButtonIntent, 0);

		remoteViews.setOnClickPendingIntent(R.id.widgetPreviousButton, previousButtonPendingIntent);

		Intent nextButtonIntent = new Intent(context, PlayerService.class);
		nextButtonIntent.setAction(PlayerService.ACTION_NEXT);

		PendingIntent nextButtonPendingIntent = PendingIntent.getService(context, 0, nextButtonIntent, 0);

		remoteViews.setOnClickPendingIntent(R.id.widgetNextButton, nextButtonPendingIntent);
	}
}
