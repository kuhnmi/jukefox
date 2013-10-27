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
package ch.ethz.dcg.jukefox.controller.player.mediaplayer;

import java.io.File;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.miniplayer.MiniPlayer;
import ch.ethz.dcg.miniplayer.PlayerEventListener;

public class MiniMediaPlayerWrapper implements IMediaPlayerWrapper, PlayerEventListener {

	public static final String TAG = MiniMediaPlayerWrapper.class.getSimpleName();

	private OnMediaPlayerEventListener listener;
	protected int seekTo = -1; // store in milliseconds
	protected boolean isPlaying;
	protected boolean isPaused;
	protected boolean isSongPrepared = false;
	protected float volume = 0.9f;
	protected PlaylistSong<BaseArtist, BaseAlbum> currentSong = null;

	protected MiniPlayer player;

	public MiniMediaPlayerWrapper() {
		isPlaying = false;
		isPaused = false;
	}

	@Override
	public int getCurrentPosition() {
		if (player != null)
			return player.getPositionMs();
		else
			return 0;
	}

	@Override
	public int getDuration() {
		if (player != null)
			return player.getDuration();
		
		return 0;
	}

	@Override
	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void setVolume(float left, float right) {
	}

	@Override
	public void onDestroy() {
		player.close();
	}

	@Override
	public void pause() {
		player.pause();

		isPlaying = false;
		isPaused = true;
	}

	@Override
	public void play() {
		if (isPlaying)
			return;

		if (player != null) {
			player.play();
		}

		isPlaying = true;
		isPaused = false;
	}

	@Override
	public void reset() {
		isPlaying = false;
		isPaused = false;
		isSongPrepared = false;

		if (player != null) {
			player.close();
		}
	}

	@Override
	public void seekTo(int position) {
		player.stop();
		player.setPositionMs(position);

		if (isPlaying) {
			player.play();
		}
	}

	@Override
	public void setSong(PlaylistSong<BaseArtist, BaseAlbum> song, String path) throws InvalidPathException {
		File file = new File(path);
		if (player != null) {
			player.setPlayerEventListener(null);
			player.close();
		}

		player = new MiniPlayer(file);
		player.setPlayerEventListener(this);
		player.preparePlayer();

		currentSong = song;
		isPlaying = false;
		isPaused = false;
		isSongPrepared = true;
	}

	@Override
	public void stop() {
		reset();
	}

	@Override
	public void setOnMediaPlayerEventListener(OnMediaPlayerEventListener listener) {
		this.listener = listener;
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong() {
		return currentSong;
	}

	@Override
	public boolean isSongReadyToPlay() {
		return isSongPrepared;
	}

	@Override
	public void onSongCompleted() {
		if (this.listener != null)
			listener.onSongCompleted(this);
	}
}
