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

import android.util.FloatMath;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IPlaybackInfoBroadcaster;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.IMediaPlayerWrapper;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommands;

public class CrossfadingPlaybackController extends DurationBugfixPlaybackController {

	public static final String TAG = CrossfadingPlaybackController.class.getSimpleName();

	private int crossfadingTime = 6000;
	private float maxVolume = 0.9f;

	public CrossfadingPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager currentPlaylistManager, int autoGapRemoveTime, int manualgapRemoveTime,
			IMediaPlayerWrapper mediaPlayer1, IMediaPlayerWrapper mediaPlayer2, boolean beatMatching) {

		super(listenerInformer, collectionModel, playerModel, currentPlaylistManager, autoGapRemoveTime,
				manualgapRemoveTime, mediaPlayer1, mediaPlayer2);

		Log.v(TAG, "CrossfadingPlaybackController created!");
	}

	@Override
	protected synchronized void setNewPrepareTimer() {
		// Log.v(TAG, "cancelTimers() setNewPrepareTimer()");
		cancelTimers();
		if (getState() != PlayerState.PLAY) {
			return;
		}
		IMediaPlayerWrapper currentMediaPlayer = getCurrentMediaPlayer();
		prepareTimer = new Timer();
		int crossfadeIn = getDuration() - getCurrentPosition(currentMediaPlayer) - gaplessSongPrepareOffset
				- getSongFadeoutOffset() - crossfadingTime;
		if (crossfadeIn <= crossfadingTime) {
			prepareNextSongToPlay();
			return;
		}
		prepareTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				prepareNextSongToPlay();
			}
		}, crossfadeIn);
	}

	private int getSongFadeoutOffset() {
		return getDuration() / 20;
	}

	private int getSongFadeinOffset() {
		return getDuration() / 20;
	}

	@Override
	protected synchronized void setNewPrepareTimer(int position) {
		cancelTimers();
		if (getState() != PlayerState.PLAY) {
			return;
		}
		prepareTimer = new Timer();
		int prepareIn = getDuration() - position - gaplessSongPrepareOffset - getSongFadeoutOffset() - crossfadingTime;
		if (prepareIn <= crossfadingTime) {
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
				- gaplessTimeMeasures.getGapTime() - getSongFadeoutOffset() - crossfadingTime;
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
		startCrossfading();
		//IMediaPlayerWrapper currentMP = switchMediaPlayer();
		//play(currentMP);
		try {
			applyPlayPreloadedControlCommands(lastCommands);
		} catch (Exception e) {
		}
		seekTo(getSongFadeinOffset());
		setMeasureTimer();
		lastPlayedSongId = lastPreloadedSongId;
		Log.i(TAG, "started " + System.currentTimeMillis() + " " + currentMediaPlayerId);
		isNextSongPrepared = false;
		setNewPrepareTimer();
	}

	private void startCrossfading() {
		final IMediaPlayerWrapper currentMP = switchMediaPlayer();
		IMediaPlayerWrapper tempMP = mediaPlayer;
		if (tempMP == currentMP) {
			tempMP = mediaPlayer2;
		}
		final IMediaPlayerWrapper otherMP = tempMP;
		currentMP.setVolume(0.0f, 0.0f);
		otherMP.setVolume(maxVolume, maxVolume);
		Thread crossfadingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				int elapsedTime = 0;
				int i = (int) (Math.random() * 4 + 0.49);
				Log.i(TAG, "crossfading mode " + i);
				long startTime = System.currentTimeMillis();
				//play(otherMP);
				while (elapsedTime < crossfadingTime) {
					elapsedTime = (int) (System.currentTimeMillis() - startTime);
					float currentVolume1 = getVolume((float) elapsedTime / crossfadingTime, i) * maxVolume;
					float currentVolume2 = getVolume(1f - (float) elapsedTime / crossfadingTime, i) * maxVolume;
					currentMP.setVolume(currentVolume1, currentVolume1);
					//otherMP.setVolume(maxVolume - currentVolume, maxVolume - currentVolume);
					otherMP.setVolume(currentVolume2, currentVolume2);
					try {
						Thread.sleep(40);
					} catch (InterruptedException e) {
						Log.w(TAG, e);
					}
				}
			}

		});
		crossfadingThread.start();
	}

	private float getVolume(float time, int function) {

		switch (function) {
			case 0:
				return FloatMath.sqrt(time);
			case 1:
				return time;
			case 2:
				return time * time;
			case 3:
				return time * time * time;
			case 4:
				return FloatMath.sqrt(time) * FloatMath.sqrt(time);
		}

		return 1;

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

}
