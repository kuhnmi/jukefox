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
package ch.ethz.dcg.miniplayer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class MiniPlayer implements PlaybackListener, IPositionListener {

	private static final int MS_PER_FRAMES = 26;

	private PlayThread playThread;
	private PreparePlayerThread preparePlayerThread;
	private File file;
	private int positionFrame;
	private PlayerEventListener listener;

	public MiniPlayer(File file) {
		this.file = file;
		this.positionFrame = 0;
	}

	public void preparePlayer() {
		if (preparePlayerThread != null) {
			preparePlayerThread.exit();
		}

		preparePlayerThread = new PreparePlayerThread(file);
		preparePlayerThread.start();
	}

	public void play() {
		// Wait for preparePlayerThread to complete loading
		try {
			preparePlayerThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		if (playThread != null) {
			playThread.exit();
		}

		preparePlayerThread.getPlayer().setPlayBackListener(this);
		preparePlayerThread.getPlayer().setPlayBackPositionListener(this);
		playThread = new PlayThread(preparePlayerThread.getPlayer(), positionFrame);
		playThread.start();
	}
	
	/**
	 * Stops the playback of the player and resets the current position to frame 0.
	 */
	public void stop() {
		stopPlayback();
		positionFrame = 0;
	}
	
	/**
	 * Stops the playback and stores the current position of the playback so it will be resumed when calling {@link #play()}.
	 */
	public void pause() {
		setPositionFrame(playThread.getPlayer().getPosition());
		stopPlayback();
	}
	
	/**
	 * Stops the playback but does not reset the current position.
	 */
	protected void stopPlayback() {
		if (playThread != null) {
			playThread.exit();
			preparePlayer();
		}
	}

	public void setPositionMs(int position) {
		setPositionFrame((int) Math.round(((double) position) / MS_PER_FRAMES));
	}

	public int getPositionMs() {
		return getPositionFrame() * 26;
	}

	public void setPositionFrame(int frame) {
		this.positionFrame = frame;
	}

	public int getPositionFrame() {
		return positionFrame;
	}

	public void close() {
		if (preparePlayerThread != null) {
			preparePlayerThread.exit();
		}

		if (playThread != null) {
			playThread.exit();
		}
	}

	protected void fireOnSongCompletedEvent() {
		if (listener != null)
			listener.onSongCompleted();
	}

	@Override
	public void playbackStarted(PlaybackEvent evt) {
	}

	@Override
	public void playbackStopped(PlaybackEvent evt) {
	}

	@Override
	public void playbackCompleted(PlaybackEvent evt) {
		fireOnSongCompletedEvent();
	}

	public void setPlayerEventListener(PlayerEventListener listener) {
		this.listener = listener;
	}

	/**
	 * Returns the duration of the song in milliseconds.
	 * 
	 * @return the duration of the song in milliseconds.
	 */
	public int getDuration() {
		AudioFileFormat baseFileFormat;
		try {
			baseFileFormat = new MpegAudioFileReader().getAudioFileFormat(file);
		} catch (UnsupportedAudioFileException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map<String, Object> properties = baseFileFormat.properties();
		long duration = (Long) properties.get("duration");

		int d = (int) Math.round(duration / 1000.0);

		return d;
	}

	@Override
	public void onNewPosition(int position) {
		positionFrame = position;
	}
}
