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

import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class PlaylistAutosaveController implements IOnPlaylistStateChangeListener, IOnPlayerStateChangeListener {

	protected static final String TAG = PlaylistAutosaveController.class.getSimpleName();

	private final Controller controller;
	private Timer autosaveTimer;

	public PlaylistAutosaveController(Controller controller) {
		this.controller = controller;
		registerListeners();
	}

	private void registerListeners() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO: replace this hack by event mechansim (see
				// Model.waitForPlaylistFunctionality)
				JukefoxApplication.getCollectionModel().getApplicationStateManager().getApplicationStateReader()
						.waitForPlaybackFunctionality();
				controller.getPlayerController().addOnPlayerStateChangeListener(PlaylistAutosaveController.this);
				controller.getPlayerController().addOnPlaylistStateChangeListener(PlaylistAutosaveController.this);
			}
		}).start();
	}

	@Override
	public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
		savePlaylistAsync(controller.getPlayerController().getCurrentPlaylist());
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {
		if (playerState == PlayerState.PLAY) {
			startAutosaveTimer();
		} else {
			cancelAutosaveTimer();
			savePlaylistAsync(controller.getPlayerController().getCurrentPlaylist());
		}
	}

	private synchronized void cancelAutosaveTimer() {
		if (autosaveTimer == null) {
			return;
		}
		autosaveTimer.cancel();
		autosaveTimer = null;
	}

	private synchronized void startAutosaveTimer() {
		if (autosaveTimer != null) {
			cancelAutosaveTimer();
		}
		autosaveTimer = new Timer();
		autosaveTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					savePlaylistAsync(controller.getPlayerController().getCurrentPlaylist());
				} catch (Throwable e) {
					Log.w(TAG, "autosaving playlist failed.");
					Log.w(TAG, e);
				}
			}
		}, 0, 60000);
	}

	public void savePlaylist(IReadOnlyPlaylist playlist) {
		JukefoxApplication.getPlayerModel().getPlaylistManager().writePlaylistToFile(playlist,
				AndroidConstants.CURRENT_PLAYLIST_NAME);
	}

	public synchronized void saveCurrentPlaylist() {
		savePlaylist(controller.getPlayerController().getCurrentPlaylist());
	}

	public void cancelTimers() {
		cancelAutosaveTimer();
	}

	private void savePlaylistAsync(final IReadOnlyPlaylist playlist) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				savePlaylist(playlist);
			}
		}).start();
	}

	@Override
	public void onPlayModeChanged(IPlayMode newPlayMode) {

	}

	@Override
	public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
		savePlaylistAsync(newPlaylist);
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

}
