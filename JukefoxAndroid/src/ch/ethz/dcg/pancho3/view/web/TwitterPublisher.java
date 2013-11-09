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
package ch.ethz.dcg.pancho3.view.web;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class TwitterPublisher implements IOnPlayerStateChangeListener, IOnPlaylistStateChangeListener,
		OnSharedPreferenceChangeListener {

	private final static String TAG = TwitterPublisher.class.getSimpleName();
	private final static long SUBMIT_DELAY = 12000;

	private final Controller controller;
	private final ISettingsReader settings;
	private BaseSong<BaseArtist, BaseAlbum> currentSong;
	private Timer submitTimer;
	private boolean enabled;

	public TwitterPublisher(Controller controller) {
		this.controller = controller;
		this.settings = controller.getSettingsReader();
		enabled = settings.isTwitterEnabled();
		settings.addSettingsChangeListener(this);
		init();
		Log.v(TAG, "TwitterPublisher created. enabled: " + enabled);
	}

	@Override
	public synchronized void onPlayerStateChanged(PlayerState playerState) {
		if (!enabled) {
			return;
		}
		Log.v(TAG, "on player state changed.");
		if (playerState == PlayerState.PLAY && currentSong != null) {
			if (submitTimer == null) {
				startSubmitTimer();
			}
			return;
		}

		// other state (i.e. not playing anymore...) => don't submit
		cancelSubmitTimer();
	}

	@Override
	public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
		if (!enabled) {
			return;
		}
		Log.v(TAG, "on current song changed.");
		setCurrentSong(newSong);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.v(TAG, "onSharedPreferenceChanged: Key: " + key);
		Context ctx = JukefoxApplication.getAppContext();
		if (!ctx.getString(R.string.KEY_TWITTER_NOWPLAYING_ENABLED).equals(key)) {
			return;
		}
		if (enabled == settings.isTwitterEnabled()) {
			return;
		}
		enabled = settings.isTwitterEnabled();
		Log.v(TAG, "new state: enabled: " + enabled);
		if (!enabled) {
			cancelSubmitTimer();
			return;
		}

		// sending to twitter has just become enabled
		try {
			BaseSong<BaseArtist, BaseAlbum> song = controller.getPlayerController().getCurrentSong();
			Log.v(TAG, "current song: " + song);
			setCurrentSong(song);
		} catch (Exception e) {
			Log.w(TAG, e);
			// TODO: issue toast??
		}
	}

	private synchronized void setCurrentSong(BaseSong<BaseArtist, BaseAlbum> newSong) {
		// if (currentSong == null && newSong == null) {
		// return;
		// }
		// if (currentSong != null && currentSong.equals(newSong)) {
		// return;
		// }
		cancelSubmitTimer();
		currentSong = newSong;
		if (currentSong != null && controller.getPlayerController().getPlayerState() == PlayerState.PLAY) {
			startSubmitTimer();
		}
	}

	private synchronized void cancelSubmitTimer() {
		if (submitTimer == null) {
			return;
		}
		submitTimer.cancel();
		submitTimer = null;
	}

	private synchronized void startSubmitTimer() {
		submitTimer = new Timer();
		submitTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				submit();
			}

		}, SUBMIT_DELAY);
		Log.v(TAG, "submit timer started.");
	}

	private void submit() {
		Log.v(TAG, "submitting...");
		if (currentSong == null) {
			Log.v(TAG, "currentSong null => don't submit");
			return;
		}
		final Intent intent = new Intent(Intent.ACTION_SEND);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Intent.EXTRA_TEXT, "This is a sample message via Public Intent");
		intent.setType("application/twitter");
		// JukefoxApplication.getAppContext().sendBroadcast(sendIntent);
		try {
			Intent chooserIntent = Intent.createChooser(intent, null);
			chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			JukefoxApplication.getAppContext().startActivity(chooserIntent);
		} catch (Exception e) {
			Log.w(TAG, e);
			Log.v(TAG, "flags: " + intent.getFlags());
		}
		Log.v(TAG, "submit intent sent.");

	}

	public void cancel() {
		cancelSubmitTimer();
		currentSong = null;
	}

	private void init() {
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				JukefoxApplication.getCollectionModel().getApplicationStateManager().getApplicationStateReader()
						.waitForPlaybackFunctionality();
				controller.getPlayerController().addOnPlaylistStateChangeListener(TwitterPublisher.this);
				controller.getPlayerController().addOnPlayerStateChangeListener(TwitterPublisher.this);
				try {
					setCurrentSong(controller.getPlayerController().getCurrentSong());
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
		});
		t.start();
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
	public void onPlayModeChanged(IPlayMode newPlayMode) {

	}

	@Override
	public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {

	}

}
