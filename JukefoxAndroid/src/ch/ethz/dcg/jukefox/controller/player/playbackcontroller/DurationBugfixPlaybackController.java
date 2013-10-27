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
package ch.ethz.dcg.jukefox.controller.player.playbackcontroller;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IPlaybackInfoBroadcaster;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.IMediaPlayerWrapper;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.OnMediaPlayerEventListener;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommands;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;

/**
 * Class to fix the bug of Android 2.3 that return wrong durations of certain file formats. This lead to a malfuctioning
 * gapless player. This Class does work around this by not offering gapless playback for these files.
 * 
 * @author saaam
 * 
 */
public class DurationBugfixPlaybackController extends GaplessPlaybackController {

	public static final String TAG = DurationBugfixPlaybackController.class.getSimpleName();
	private int durationOfSongInPlayer1 = 0;
	private int durationOfSongInPlayer2 = 0;

	public DurationBugfixPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager currentPlaylistManager, int autoGapRemoveTime, int manualgapRemoveTime,
			IMediaPlayerWrapper mediaPlayer1, IMediaPlayerWrapper mediaPlayer2) {

		super(listenerInformer, collectionModel, playerModel, currentPlaylistManager, autoGapRemoveTime,
				manualgapRemoveTime, mediaPlayer1, mediaPlayer2);

		mediaPlayer1.setOnMediaPlayerEventListener(new OnMediaPlayerEventListener() {

			@Override
			public void onSongCompleted(IMediaPlayerWrapper mediaPlayer) {
				DurationBugfixPlaybackController.this.onSongCompleted(mediaPlayer.getCurrentSong());
			}

		});
		mediaPlayer2.setOnMediaPlayerEventListener(new OnMediaPlayerEventListener() {

			@Override
			public void onSongCompleted(IMediaPlayerWrapper mediaPlayer) {
				DurationBugfixPlaybackController.this.onSongCompleted(mediaPlayer.getCurrentSong());
			}

		});
	}

	@Override
	protected boolean loadSongIntoPlayer(PlaylistSong<BaseArtist, BaseAlbum> song, String path,
			IMediaPlayerWrapper currentMP) {
		boolean success = super.loadSongIntoPlayer(song, path, currentMP);
		int duration = readSongDurationWithTagLibrary(lastSongPath);
		if (currentMP == mediaPlayer) {
			durationOfSongInPlayer1 = duration;
		} else {
			durationOfSongInPlayer2 = duration;
		}
		return success;
	}

	@Override
	public synchronized int getDuration() {
		int duration = -1;
		if (currentMediaPlayerId == 1) {
			duration = durationOfSongInPlayer1;
		} else if (currentMediaPlayerId == 2) {
			duration = durationOfSongInPlayer2;
		}
		if (duration > 0) {
			return duration;
		}
		return super.getDuration();
	}

	@Override
	protected synchronized void setNewPrepareTimer() {
		// Log.v(TAG, "cancelTimers() setNewPrepareTimer()");
		cancelTimers();
		if (getState() != PlayerState.PLAY) {
			// Log.v(TAG,
			// "Player not in play mode. Not setting prepare timer.");
			return;
		}
		IMediaPlayerWrapper currentMediaPlayer = getCurrentMediaPlayer();
		prepareTimer = new Timer();
		int prepareIn = getDuration() - getCurrentPosition(currentMediaPlayer) - gaplessSongPrepareOffset;
		if (prepareIn <= 0) {
			prepareNextSongToPlay();
			return;
		}
		prepareTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				prepareNextSongToPlay();
			}
		}, prepareIn);
	}

	protected synchronized void setNewPrepareTimer(int position) {
		cancelTimers();
		if (getState() != PlayerState.PLAY) {
			return;
		}
		prepareTimer = new Timer();
		int prepareIn = getDuration() - position - gaplessSongPrepareOffset;
		if (prepareIn <= 0) {
			prepareNextSongToPlay();
			return;
		}
		prepareTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				prepareNextSongToPlay();
			}
		}, prepareIn);
	}

	private synchronized void prepareNextSongToPlay() {

		setNewPlayTimer();
		preloadNextSong();

		// Log.v(TAG, "Play timer set");
	}

	@Override
	protected synchronized void setNewPlayTimer() {
		// Log.v(TAG, "cancelTimers() in setNewPlayTimers()");
		cancelTimers();

		if (getState() != PlayerState.PLAY) {
			// Log.v(TAG, "Player not in play mode. Not setting play timer.");
			return;
		}
		playTimer = new Timer();
		TimerTask playPreparedSongTask = new TimerTask() {

			@Override
			public void run() {
				playPreloadedSong();
			}
		};
		IMediaPlayerWrapper currentMediaPlayer = getCurrentMediaPlayer();
		int playIn = getDuration() - getCurrentPosition(currentMediaPlayer) - manualGapRemoveTime
				- gaplessTimeMeasures.getGapTime();
		if (playIn < 0) {
			playPreloadedSong();
			return;
		}
		playTimer.schedule(playPreparedSongTask, playIn);
		// Log.v(TAG, "Set play timer");
	}

	@Override
	protected synchronized void playPreloadedSong() {
		if (!isNextSongPrepared) {
			try {
				lastCommands = currentPlaylistManager.getPlayMode().next(currentPlaylistManager.getCurrentPlaylist());
				PlaylistSong<BaseArtist, BaseAlbum> songToLoad = applyPreloadControlCommands(lastCommands);
				Log.v(TAG, "preloadNextSong() " + songToLoad.getArtist().getName() + " - " + songToLoad.getName());
				try {
					isNextSongPrepared = loadSongIntoPlayer(songToLoad, collectionModel.getOtherDataProvider()
							.getSongPath(songToLoad), getCurrentMediaPlayer());
					lastPreloadedSongId = songToLoad.getId();
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}
				applyPlayPreloadedControlCommands(lastCommands);
			} catch (NoNextSongException e) {
				Log.w(TAG, e);
				stop();
			}
			return;
		}
		IMediaPlayerWrapper currentMP = switchMediaPlayer();
		// play(currentMP);
		try {
			applyPlayPreloadedControlCommands(lastCommands);
		} catch (Exception e) {
		}
		setMeasureTimer();
		lastPlayedSongId = lastPreloadedSongId;
		Log.i(TAG, "started " + System.currentTimeMillis() + " " + currentMediaPlayerId);
		isNextSongPrepared = false;
		setNewPrepareTimer();
	}

	@Override
	protected synchronized void preloadNextSong() {
		gaplessTimeMeasures.setSong1Times(System.currentTimeMillis(), getPlaybackPosition(), getDuration());
		if (isNextSongPrepared) {
			return;
		}
		// Log.v(TAG, "preloadNextSong");
		IMediaPlayerWrapper nextMP = getNextMediaPlayer();
		PlaylistSong<BaseArtist, BaseAlbum> songToLoad;
		try {
			PlayerControllerCommands commands = currentPlaylistManager.getPlayMode().next(
					currentPlaylistManager.getCurrentPlaylist());
			songToLoad = applyPreloadControlCommands(commands);
			lastCommands = commands;
			Log.v(TAG, "preloadNextSong() " + songToLoad.getArtist().getName() + " - " + songToLoad.getName());
			try {
				isNextSongPrepared = loadSongIntoPlayer(songToLoad, collectionModel.getOtherDataProvider().getSongPath(
						songToLoad), nextMP);
				lastPreloadedSongId = songToLoad.getId();
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
		} catch (NoNextSongException e) {
			// Log.w(TAG, e);
		}
	}

	@Override
	public synchronized void seekTo(int position) {
		// Log.v(TAG, "cancelTimers() in seekTo()");
		cancelTimers();

		seekTo(getCurrentMediaPlayer(), position);

		setNewPrepareTimer(position);
	}

	// @Override
	// protected void seekTo(AndroidMediaPlayerWrapper mp, int position) {
	// if (getState() != PlayerState.PAUSE && getState() != PlayerState.PLAY) {
	// rememberSeekToPosition(position);
	// if (getState() == PlayerState.STOP) {
	// setPlayerState(PlayerState.PAUSE);
	// }
	// }
	// try {
	// int realDuration = getDuration();
	// int mpDuration = mp.getDuration();
	// position = Math.min(realDuration, position);
	// position = Math.max(0, position);
	// Log.v(TAG, "seek to pos: " + position);
	// long tempPosition = position;
	// if (realDuration != mpDuration) {
	// tempPosition = tempPosition * mpDuration / realDuration;
	// }
	// position = (int) tempPosition;
	// Log.v(TAG, "seek to pos2: " + position);
	// mp.seekTo(position);
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// }
	// }

	/**
	 * returns the song duration in milliseconds or -1 if it is not able to read the duration
	 * 
	 * @param path
	 * @return
	 */
	private int readSongDurationWithTagLibrary(String path) {
		float exactDuration = -1;
		try {
			// Log.v("Tag Reader", "Reading tags from " + song.getName());
			File f = new File(path);
			AudioFile af = AudioFileIO.read(f);
			exactDuration = af.getPreciseLength();

			int duration = (int) Math.floor(exactDuration * 1000);
			Log.v(TAG, "Read duration from tag: " + duration + "ms");
			return duration;
		} catch (Exception e) {
			Log.w(TAG, e);
			return -1;
		}
	}

}
