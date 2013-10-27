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

import java.io.IOException;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;

public class AndroidMediaPlayerWrapper implements IMediaPlayerWrapper, OnCompletionListener, OnErrorListener,
		OnInfoListener {

	public static final String TAG = AndroidMediaPlayerWrapper.class.getSimpleName();

	private MediaPlayer mediaPlayer;
	private OnMediaPlayerEventListener listener;
	protected int seekTo = -1;
	protected boolean isPlaying;
	protected boolean isPaused;
	protected boolean isSongPrepared = false;
	protected PlaylistSong<BaseArtist, BaseAlbum> currentSong = null;

	public AndroidMediaPlayerWrapper() {
		mediaPlayer = new MediaPlayer();
		isPlaying = false;
		isPaused = false;
	}

	public int getCurrentPosition() {
		if (!isPlaying && !isPaused || !isSongPrepared) {
			if (seekTo > 0) {
				return seekTo;
			}
			return 0;
		}
		try {
			int position = mediaPlayer.getCurrentPosition();
			if (position < 0) {
				position = 0;
			}
			return position;
		} catch (Exception e) {
			Log.w(TAG, e);
			return 0;
		}
	}

	public int getDuration() {
		if (!isSongPrepared) {
			return 1000000;
		}
		return mediaPlayer.getDuration();
	}

	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	public void onDestroy() {
		isPlaying = false;
		isPaused = false;
		mediaPlayer.release();
	}

	public void pause() {
		mediaPlayer.pause();
		isPlaying = false;
		isPaused = true;
	}

	public void play() {
		mediaPlayer.start();
		if (mediaPlayer.isPlaying()) {
			isPlaying = true;
			isPaused = false;
			recallSeekToPosition();
		}
		// Log.v(TAG, "Time after play: " + System.currentTimeMillis() + " "
		// + mp.isPlaying());
	}

	public void reset() {
		mediaPlayer.reset();
		isPlaying = false;
		isPaused = false;
		isSongPrepared = false;
	}

	public void seekTo(int position) {
		seekTo(mediaPlayer, position);
	}

	public void setSong(PlaylistSong<BaseArtist, BaseAlbum> song, String path) throws InvalidPathException {
		if (!AndroidUtils.isSdCardOk()) {
			throw new InvalidPathException();
		}
		resetSeekToPosition();
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(path);
		} catch (IllegalArgumentException e) {
			Log.w(TAG, e);
			throw new InvalidPathException();
		} catch (IllegalStateException e) {
			Log.w(TAG, e);
			throw new InvalidPathException();
		} catch (IOException e) {
			Log.w(TAG, e);
			throw new InvalidPathException();
		}
		try {
			// kuhnmi 8.8.2011: replaced "MPService" with TAG
			Log.i(TAG, "Preparing to play song at path: " + path);
			mediaPlayer.prepare();
		} catch (Exception e) {
			Log.w("IOException during prepare.", e);
			listener.onError(this, 0, 0);
			return;
		}
		currentSong = song;
		isPlaying = false;
		isPaused = false;
		isSongPrepared = true;
	}

	public void stop() {
		mediaPlayer.stop();
		isPlaying = false;
		isPaused = false;
		isSongPrepared = false;
	}

	public void setVolume(float left, float right) {
		mediaPlayer.setVolume(left, right);
	}

	public void setOnMediaPlayerEventListener(OnMediaPlayerEventListener listener) {
		this.listener = listener;
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		listener.onSongCompleted(this);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		isSongPrepared = false;
		return listener.onError(this, what, extra);
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		return listener.onInfo(this, what, extra);
	}

	private void recallSeekToPosition() {
		Log.v(TAG, "recall seekto: " + seekTo);
		if (seekTo >= 0) {
			seekTo(seekTo);
		}
		resetSeekToPosition();
	}

	private void resetSeekToPosition() {
		seekTo = -1;
	}

	protected void seekTo(MediaPlayer mp, int position) {
		if (!isPlaying && !isPaused || !isSongPrepared) {
			rememberSeekToPosition(position);
			return;
		}
		Log.v(TAG, "seekto 2 " + position);
		try {
			int duration = getDuration();
			position = Math.min(duration, position);
			position = Math.max(0, position);
			mp.seekTo(position);
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	protected void rememberSeekToPosition(int position) {
		Log.v(TAG, "Remembering position: " + position);
		seekTo = position;
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong() {
		return currentSong;
	}

	@Override
	public boolean isSongReadyToPlay() {
		return isSongPrepared;
	}

}
