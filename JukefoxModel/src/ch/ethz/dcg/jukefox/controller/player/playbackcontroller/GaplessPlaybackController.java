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

import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.dcg.jukefox.commons.Constants;
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
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommand;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommands;

public class GaplessPlaybackController extends BasePlaybackController {

	private static final String TAG = GaplessPlaybackController.class.getSimpleName();
	protected IMediaPlayerWrapper mediaPlayer2;
	protected int currentMediaPlayerId = 1;
	protected boolean isNextSongPrepared = false;
	protected int lastPreloadedSongId = -1;
	protected int lastPlayedSongId = -1;
	protected Timer playTimer;
	protected Timer prepareTimer;
	protected Timer measureTimer;
	protected int gaplessSongPrepareOffset;
	protected int manualGapRemoveTime;
	protected GaplessTimeMeasures gaplessTimeMeasures;
	protected PlayerControllerCommands lastCommands;

	public final OnMediaPlayerEventListener mediaPlayerEventListener = new OnMediaPlayerEventListener() {

		@Override
		public boolean onError(IMediaPlayerWrapper mp, int what, int extra) {
			return GaplessPlaybackController.this.onError(mp, what, extra);
		}

		@Override
		public boolean onInfo(IMediaPlayerWrapper mp, int what, int extra) {
			return GaplessPlaybackController.this.onInfo(mp, what, extra);
		}

		@Override
		public void onSongCompleted(IMediaPlayerWrapper mediaPlayer) {
			GaplessPlaybackController.this.onSongCompleted(mediaPlayer.getCurrentSong());
		}

	};

	public GaplessPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager currentPlaylistManager, int autoGapRemoveTime, int manualGapRemoveTime,
			IMediaPlayerWrapper mediaPlayer1, IMediaPlayerWrapper mediaPlayer2) {

		super(listenerInformer, collectionModel, playerModel, currentPlaylistManager, mediaPlayer1);

		this.mediaPlayer2 = mediaPlayer2;
		this.manualGapRemoveTime = manualGapRemoveTime;
		mediaPlayer2.setOnMediaPlayerEventListener(mediaPlayerEventListener);
		gaplessTimeMeasures = new GaplessTimeMeasures(autoGapRemoveTime, manualGapRemoveTime);
		gaplessSongPrepareOffset = manualGapRemoveTime + gaplessTimeMeasures.getGapTime() + 8000;
	}

	@Override
	public synchronized int getDuration() {
		return getDuration(getCurrentMediaPlayer());
	}

	@Override
	public synchronized int getPlaybackPosition() {
		return getCurrentPosition(getCurrentMediaPlayer());
	}

	@Override
	public synchronized boolean jumpToPlaylistPosition(int position) {
		try {
			currentPlaylistManager.setCurrentSongIndex(position);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
			return false;
		}
		PlaylistSong<BaseArtist, BaseAlbum> song;
		try {
			song = currentPlaylistManager.getCurrentSong();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
			return false;
		}

		if (song == null) {
			// This song is no longer in the database!
			return false;
		}

		String path;
		try {
			path = collectionModel.getOtherDataProvider().getSongPath(song);
			Log.v(TAG, "loadSong() " + song.getArtist().getName() + " - " + song.getName());
			isNextSongPrepared = false;
			lastPreloadedSongId = -1;

			cancelTimers();

			if (loadSongIntoPlayer(song, path, getCurrentMediaPlayer())) {

				// Log.v(TAG, "cancelTimers() in loadSong1");
				return true;
			} else {
				// Log.v(TAG, "cancelTimers() in loadSong2");
				return false;
			}
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}

		return false;

	}

	protected synchronized void cancelTimers() {
		cancelPrepareTimer();
		cancelPlayTimer();
	}

	@Override
	public synchronized void pause() {
		// Log.v(TAG, "cancelTimers() in pause()");
		cancelTimers();

		pause(getCurrentMediaPlayer());

	}

	@Override
	public synchronized void play() {
		// Log.v(TAG, "cancelTimers() in play()");
		cancelTimers();

		play(getCurrentMediaPlayer());

		setNewPrepareTimer();
	}

	@Override
	public synchronized void stop() {
		// Log.v(TAG, "cancelTimers() in stop()");
		cancelTimers();

		stop(getCurrentMediaPlayer());
	}

	@Override
	public synchronized void seekTo(int position) {
		// Log.v(TAG, "cancelTimers() in seekTo()");
		cancelTimers();

		seekTo(getCurrentMediaPlayer(), position);

		setNewPrepareTimer();
	}

	private synchronized void prepareNextSongToPlay() {

		preloadNextSong();
		setNewPlayTimer();

		// Log.v(TAG, "Play timer set");
	}

	protected synchronized IMediaPlayerWrapper getCurrentMediaPlayer() {
		if (currentMediaPlayerId == 2) {
			return mediaPlayer2;
		}
		return mediaPlayer;
	}

	protected synchronized IMediaPlayerWrapper getNextMediaPlayer() {
		if (currentMediaPlayerId == 1) {
			return mediaPlayer2;
		}
		return mediaPlayer;
	}

	protected synchronized IMediaPlayerWrapper switchMediaPlayer() {
		if (currentMediaPlayerId == 1) {
			currentMediaPlayerId = 2;
			return mediaPlayer2;
		}
		currentMediaPlayerId = 1;
		return mediaPlayer;
	}

	@Override
	public void onDestroy() {

		stop();
		if (mediaPlayer2 != null) {
			stop(mediaPlayer2);
			mediaPlayer2.onDestroy();
		}
		super.onDestroy();
	}

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
		//		IMediaPlayerWrapper currentMP = switchMediaPlayer();
		switchMediaPlayer();
		// play(currentMP);
		try {
			applyPlayPreloadedControlCommands(lastCommands);
		} catch (Exception e) {
		}
		setMeasureTimer();
		lastPlayedSongId = lastPreloadedSongId;
		// Log.i(TAG, "started " + System.currentTimeMillis() + " " +
		// currentMediaPlayerId);
		isNextSongPrepared = false;
		setNewPrepareTimer();
	}

	protected void setMeasureTimer() {
		cancelMeasureTimer();
		measureTimer = new Timer();
		measureTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (lastLoadedSongId == lastPlayedSongId) {
					gaplessTimeMeasures.setSong2Times(System.currentTimeMillis(), getPlaybackPosition(),
							manualGapRemoveTime + gaplessTimeMeasures.getGapTime());
				}
			}

		}, 1000);
	}

	private void cancelMeasureTimer() {
		if (measureTimer != null) {
			measureTimer.cancel();
			measureTimer = null;
		}
	}

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
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		listenerInformer.informSongCompletedListeners(song);
	}

	private synchronized void cancelPlayTimer() {
		if (playTimer != null) {
			playTimer.cancel();
			// Log.v(TAG, "Cancelled play timer");
		}
	}

	private synchronized void cancelPrepareTimer() {
		if (prepareTimer != null) {
			prepareTimer.cancel();
		}
		// Log.v(TAG, "Cancelled prepare timer");
	}

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
		int playIn = getDuration(currentMediaPlayer) - getCurrentPosition(currentMediaPlayer) - manualGapRemoveTime
				- gaplessTimeMeasures.getGapTime();
		if (playIn < 0) {
			playPreloadedSong();
			return;
		}
		playTimer.schedule(playPreparedSongTask, playIn);
		// Log.v(TAG, "Set play timer");
	}

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
		int prepareIn = getDuration(currentMediaPlayer) - getCurrentPosition(currentMediaPlayer)
				- gaplessSongPrepareOffset;
		//		System.out
		//						.println("Set song-prepare-timer to load the song in " + prepareIn
		//								+ " msec. Duration: " + getDuration(currentMediaPlayer) + " position: " + getCurrentPosition(currentMediaPlayer));
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
		// Log.v(TAG, "Set prepare timer");
	}

	@Override
	public void reloadSettings() {
		super.reloadSettings();
	}

	@Override
	public void mute() {
		super.mute(getCurrentMediaPlayer());
	}

	@Override
	public void unmute() {
		super.unmute(getCurrentMediaPlayer());
	}

	/**
	 * Applies all commands until it reaches a playerAction PLAY command (not including the play command)
	 * 
	 * @param commands
	 * @return the song that should be preloaded in the next media player
	 */
	protected PlaylistSong<BaseArtist, BaseAlbum> applyPreloadControlCommands(PlayerControllerCommands commands) {

		int position = currentPlaylistManager.getCurrentPlaylist().getPositionInList();

		for (PlayerControllerCommand command : commands.getAllCommands()) {
			switch (command.getType()) {
				case ADD_SONG:
					try {
						currentPlaylistManager.insertSongAtPosition(command.getSong(), command.getPosition());
					} catch (PlaylistPositionOutOfRangeException e) {
						Log.w(TAG, e);
					}
					break;
				case REMOVE_SONG:
					try {
						currentPlaylistManager.removeSongFromPlaylist(command.getPosition());
					} catch (EmptyPlaylistException e) {
						Log.w(TAG, e);
					} catch (PlaylistPositionOutOfRangeException e) {
						Log.w(TAG, e);
					}
					break;
				case PLAYER_ACTION:
					// Don't execute player commands when preloading a song
					break;
				case SET_POS_IN_LIST:
					// Don't set a playlist position when preloading a song,
					// just remember the position
					position = command.getPosition();
					break;
				case SET_POS_IN_SONG:
					// Don't seek when preloading a song
					break;
				case SET_PLAY_MODE:
					setPlayMode(command.getPlayMode(), 0, Constants.SAME_SONG_AVOIDANCE_NUM); // TODO: use real parameters
					break;
			}
		}
		PlaylistSong<BaseArtist, BaseAlbum> song = currentPlaylistManager.getCurrentPlaylist().getSongList().get(
				position);
		return song;
	}

	/**
	 * Applies just playerAction PLAY commands (including the play command)
	 * 
	 * @param commands
	 */
	protected void applyPlayPreloadedControlCommands(PlayerControllerCommands commands) {

		for (PlayerControllerCommand command : commands.getAllCommands()) {
			switch (command.getType()) {
				case ADD_SONG:
					// Ignore when playing a preloaded song
					break;
				case REMOVE_SONG:
					// Ignore when playing a preloaded song
					break;
				case PLAYER_ACTION:
					if (command.getPlayerAction() == PlayerAction.PLAY) {
						play(getCurrentMediaPlayer());
					} else if (command.getPlayerAction() == PlayerAction.PAUSE) {
						pause(getCurrentMediaPlayer());
					} else if (command.getPlayerAction() == PlayerAction.STOP) {
						stop(getCurrentMediaPlayer());
					}
					break;
				case SET_POS_IN_LIST:
					try {
						currentPlaylistManager.setCurrentSongIndex(command.getPosition());
					} catch (PlaylistPositionOutOfRangeException e) {
						Log.w(TAG, e);
					}
					break;
				case SET_POS_IN_SONG:
					// Ignore when playing a preloaded song
					break;
				case SET_PLAY_MODE:
					setPlayMode(command.getPlayMode(), 0, Constants.SAME_SONG_AVOIDANCE_NUM); // TODO: use real parameters
					break;
			}
		}
	}

}
