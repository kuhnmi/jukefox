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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class ScrobbledroidPublisher implements IOnPlayerStateChangeListener, IOnPlaylistStateChangeListener,
		OnSharedPreferenceChangeListener {

	private final static String TAG = ScrobbledroidPublisher.class.getSimpleName();

	private final AndroidPlayerController controller;
	private final ISettingsReader settings;
	private boolean enabled;
	private PlaylistSong<BaseArtist, BaseAlbum> currentSong;

	public ScrobbledroidPublisher(AndroidPlayerController controller) {
		this.controller = controller;
		this.settings = AndroidSettingsManager.getAndroidSettingsReader();
		enabled = settings.isScrobbledroidEnabled();
		settings.addSettingsChangeListener(this);
		init();
		Log.v(TAG, "ScrobbleDroidPublisher created. enabled: " + enabled);
	}

	@Override
	public synchronized void onPlayerStateChanged(PlayerState playerState) {
		if (!enabled) {
			return;
		}
		informScrobbledroidAsync(isPlaying(), currentSong);
	}

	private void setCurrentSong() {
		try {
			currentSong = controller.getCurrentSong();
			if (currentSong == null) {
				return;
			}
			Log.v(TAG, "new current song set: " + currentSong.getArtist() + " - " + currentSong.getName());
		} catch (Exception e) {
			Log.w(TAG, e);
			currentSong = null;
		}
	}

	@Override
	public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
		if (!enabled) {
			return;
		}
		informScrobbledroidAsync(false, currentSong);
		setCurrentSong();
		Log.v(TAG, "onCurrentSongChanged.");
		informScrobbledroidAsync(isPlaying(), newSong);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Log.v(TAG, "onSharedPreferenceChanged: Key: " + key);
		Context ctx = JukefoxApplication.getAppContext();
		if (!ctx.getString(R.string.KEY_SCROBBLE_TYPE).equals(key)) {
			return;
		}
		if (enabled == settings.isScrobbledroidEnabled()) {
			return;
		}
		enabled = settings.isScrobbledroidEnabled();
		Log.v(TAG, "new state: enabled: " + enabled);
		if (!enabled) {
			return;
		}
		setCurrentSong();
		informScrobbledroidAsync(isPlaying(), currentSong);
	}

	private void informScrobbledroidAsync(final boolean playing, final PlaylistSong<BaseArtist, BaseAlbum> song) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					// BaseSong<BaseArtist, BaseAlbum> song =
					// controller.getPlayManager().getCurrentSong();
					if (song == null) {
						return;
					}

					Intent i = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
					i.putExtra("playing", playing);
					i.putExtra("artist", song.getArtist().getName());
					i.putExtra("track", song.getName());
					i.putExtra("album", song.getAlbum().getName());
					i.putExtra("secs", controller.getDuration() / 1000);

					JukefoxApplication.getAppContext().sendBroadcast(i);
					Log.v(TAG, "scrobble droid informed. track: " + song.getName() + ", playing: " + playing);
				} catch (Exception e) {
					Log.w(TAG, e);
					// TODO: issue toast?
				}
			}

		});
		t.start();
	}

	private boolean isPlaying() {
		boolean playing = controller.getPlayerState() == PlayerState.PLAY ? true : false;
		return playing;
	}

	private void init() {
		if (!enabled) {
			return;
		}
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				JukefoxApplication.getCollectionModel().getApplicationStateManager().getApplicationStateReader()
						.waitForPlaybackFunctionality();
				controller.addOnPlaylistStateChangeListener(ScrobbledroidPublisher.this);
				controller.addOnPlayerStateChangeListener(ScrobbledroidPublisher.this);
				setCurrentSong();
				informScrobbledroidAsync(isPlaying(), currentSong);
			}
		});
		t.start();
	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		if (!enabled) {
			return;
		}
		Log.v(TAG, "onSongCompleted.");
		informScrobbledroidAsync(false, song);
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
		if (!enabled) {
			return;
		}
		Log.v(TAG, "onSongCompleted.");
		informScrobbledroidAsync(false, song);
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

	public void onDestroy() {
		settings.removeSettingsChangeListener(this);
	}

}
