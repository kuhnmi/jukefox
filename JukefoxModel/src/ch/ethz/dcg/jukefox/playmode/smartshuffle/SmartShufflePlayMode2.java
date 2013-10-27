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
package ch.ethz.dcg.jukefox.playmode.smartshuffle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.log.SmartShuffleNextSongLogEntry;
import ch.ethz.dcg.jukefox.data.log.SmartShuffleNextSongLogEntry.SongSource;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;
import ch.ethz.dcg.jukefox.model.rating.RatingHelper;
import ch.ethz.dcg.jukefox.playmode.BasePlayMode;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommands;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;

public class SmartShufflePlayMode2 extends BasePlayMode {

	private final static String TAG = SmartShufflePlayMode2.class.getSimpleName();

	private final AgentManager agentManager;

	private IReadOnlyPlaylist playlist = null;

	private final List<AgentWeightsAdjustmentThread> agentWeightAdjustmentThreads = new LinkedList<AgentWeightsAdjustmentThread>();

	private int negativeOfSameCalculatorCount = 0;
	private int lastNextSongCalculatorPosition = 0;
	private NextSongCalculationThread lastNextSongCalculator = null;
	private final NextSongCalculationThreadManager nextSongCalculationThreadManager;

	private Pair<BaseSong<BaseArtist, BaseAlbum>, SmartShuffleNextSongLogEntry.Builder> currentLog = null;

	public SmartShufflePlayMode2(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		super(collectionModel, playerModel);

		agentManager = AgentManager.initialize(collectionModel.getDbDataPortal(), collectionModel.getSongProvider(),
				playerModel.getStatisticsProvider());

		nextSongCalculationThreadManager = new NextSongCalculationThreadManager(nextSongCalculationThreadManagerHelper);
	}

	/**
	 * Clears the playlist from the current position to the end and starts the next song calculation.
	 */
	@Override
	public PlayerControllerCommands initialize(IReadOnlyPlaylist currentPlaylist) {
		// Start the next song calculation
		nextSongCalculationThreadManager.currentSongChanged(getCurrentSong(currentPlaylist));
		nextSongCalculationThreadManager.start();

		// Remove songs which are further in the list as our current position
		PlayerControllerCommands commands = new PlayerControllerCommands();
		for (int i = currentPlaylist.getSize() - 1; i > currentPlaylist.getPositionInList(); --i) {
			commands.removeSong(i);
		}
		return commands;
	}

	/**
	 * Reads out the next song from the asynchronous calculation threads and starts new ones for the next song.<br/>
	 * If the time for caluclating the next song was too short (very early skip), an older calculation will be
	 * considered or if none exists, a random song will be picked.
	 */
	@Override
	public synchronized PlayerControllerCommands next(IReadOnlyPlaylist playlist) throws NoNextSongException {
		this.playlist = playlist;

		// Get the finished song and its final rating
		BaseSong<BaseArtist, BaseAlbum> finishedSong = getCurrentSong(playlist);
		double rating = getCurrentRating(playlist);

		// Inform the nextSongCalculationThreadManager about the finished song
		nextSongCalculationThreadManager.songFinished(finishedSong, rating);

		// Write the log entry
		if ((currentLog != null) && Utils.nullEquals(currentLog.first, finishedSong)) {
			// Get finished meSongId
			Integer finishedMeSongId = null;
			try {
				finishedMeSongId = collectionModel.getOtherDataProvider().getMusicExplorerSongId(finishedSong);
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}

			SmartShuffleNextSongLogEntry.Builder log = currentLog.second;
			log
					.setCurrentMeSongId((finishedMeSongId != null) ? finishedMeSongId : 0)
					.setCurrentRating(rating)
					.setFractionPlayed(getCurrentFraction(playlist))
					.setSecondsPlayed(getMilliSecondsPlayed(playlist) / 1000);
			playerModel.getLogManager().addLogEntry(log.build());
		}
		// Start new log entry
		SmartShuffleNextSongLogEntry.Builder log = SmartShuffleNextSongLogEntry.createInstance();

		// Adjust the agent weights
		if ((lastNextSongCalculator != null) && (lastNextSongCalculatorPosition == 0)) {
			// Do it async to ensure no delays in this method (i.e. in case of db locks)
			AgentWeightsAdjustmentThread awat = new AgentWeightsAdjustmentThread(getAgentVotesForCurrentSong(),
					(float) rating);
			agentWeightAdjustmentThreads.add(awat);
			awat.start();
		}

		// Choose the next song
		PlaylistSong<BaseArtist, BaseAlbum> nextSong = null;
		PlayerControllerCommands commands = new PlayerControllerCommands();
		if (playlist.getPositionInList() < (playlist.getSize() - 1)) {
			// We are in the middle of the playlist -> just jump to the next song 
			commands.setListPos(playlist.getPositionInList() + 1);

			log.setSongSource(SongSource.Playlist);

			// Adjust currentSong
			try {
				nextSong = playlist.getSongAtPosition(playlist.getPositionInList() + 1);
				lastNextSongCalculator = null;
			} catch (PlaylistPositionOutOfRangeException e) {
				Log.w(TAG, e);
				assert false;
			}
		} else {
			// We are at the end of the playlist 

			// Get the most up-to-date NextSongCalculationThread
			NextSongCalculationThread nextSongCalculationThread = nextSongCalculationThreadManager
					.getNextSongCalculationThread();
			if (Utils.nullEquals(lastNextSongCalculator, nextSongCalculationThread)) {
				Log.d(TAG, "Getting song from ALREADY USED lastNextSongCalculator");
				++lastNextSongCalculatorPosition;
			} else {
				Log.d(TAG, "Getting song from NEW lastNextSongCalculator");
				lastNextSongCalculator = nextSongCalculationThread;
				lastNextSongCalculatorPosition = 0;
				negativeOfSameCalculatorCount = 0;
			}

			if (rating < 0) {
				++negativeOfSameCalculatorCount;
			}

			// Get the next song & agent votes
			if (nextSongCalculationThread != null) {
				if (negativeOfSameCalculatorCount == 0) {
					// First entry of this calculation thread -> just use the proposal
					log.setSongSource(SongSource.NextCalculator);
					nextSong = nextSongCalculationThread.getProposedSong(lastNextSongCalculatorPosition);
				} else if (negativeOfSameCalculatorCount < 3) {
					// We are reusing this calculation thread, but acceptance rate is good enough
					log.setSongSource(SongSource.ReusedNextCalculator);
					BaseArtist lastArtist = finishedSong.getArtist();
					--lastNextSongCalculatorPosition; // cancel ++ on first run 
					do {
						++lastNextSongCalculatorPosition;
						nextSong = nextSongCalculationThread.getProposedSong(lastNextSongCalculatorPosition);
					} while ((rating < 0) && Utils.nullEquals(lastArtist, nextSong.getArtist()) && (nextSong != null)); // Enforce that an artist is not played twice in a row if it was rated negative

					if (nextSong == null) {
						Log.d(TAG, "Using random song, since NextSongCalculation has no song left in its list.");
					}
				} else {
					// Too much negative proposals of the same calculator -> use random since it voted not that promising
					Log.d(TAG, "Using random song, since prediction is too bad.");
					log.setSongSource(SongSource.Random);
					nextSong = getRandomSong(playlist);
				}

				if (negativeOfSameCalculatorCount < 3) {
					log
							.setOptAgentVotes(nextSongCalculationThread.getAgentVotes(lastNextSongCalculatorPosition))
							.setOptAgentWeights(nextSongCalculationThread.getAgentWeights());
				}
			}

			if (nextSong == null) {
				// Choose random song
				if (nextSongCalculationThread == null) {
					Log.w(TAG, "Ran out of time - had to choose a random song");
				} else {
					Log.d(TAG, "We are choosing a random song (a NextSongCalculation is around)");
				}
				log.setSongSource(SongSource.Random);
				nextSong = getRandomSong(playlist);
			}
			if (nextSong == null) {
				// We are fu*** up.. How should we recover? Just play one song from the playlist once more...
				log.setSongSource(SongSource.Random);
				nextSong = getRandomSongFromPlaylist(playlist);
			}

			if (nextSong != null) {
				// Add next song to the end of the playlist
				commands.addSong(nextSong, playlist.getSize());
				commands.setListPos(playlist.getSize());
			}
		}

		if (nextSong != null) {
			commands.playerAction(PlayerAction.PLAY);

			nextSongCalculationThreadManager.currentSongChanged(nextSong);

			currentLog = new Pair<BaseSong<BaseArtist, BaseAlbum>, SmartShuffleNextSongLogEntry.Builder>(nextSong, log);
		} else {
			throw new NoNextSongException();
		}

		return commands;
	}

	/**
	 * Returns a random song, which is not already in the given playlist. If after 10 attemps no song could be found,
	 * null is returned.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The random song
	 */
	private PlaylistSong<BaseArtist, BaseAlbum> getRandomSong(IReadOnlyPlaylist playlist) {
		try {
			int i = 0;
			while (i < 10) { // Ensure termination
				PlaylistSong<BaseArtist, BaseAlbum> song = collectionModel.getSongProvider().getRandomSong();
				if (playlist.getSongList().indexOf(song) == -1) {
					// This song is not in the playlist yet
					return song;
				}
				++i;
			}
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}

		return null;
	}

	/**
	 * Returns a random song from the given playlist. If the playlist is empty <code>null</code> is returned.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The random song
	 */
	private PlaylistSong<BaseArtist, BaseAlbum> getRandomSongFromPlaylist(IReadOnlyPlaylist playlist) {
		if (playlist.isPlaylistEmpty()) {
			return null;
		}

		int pos = RandomProvider.getRandom().nextInt(playlist.getSize());
		try {
			return playlist.getSongAtPosition(pos);
		} catch (PlaylistPositionOutOfRangeException e) {
			// This really should never occur...
			assert false;
			return null;
		}
	}

	/**
	 * Returns the currently played song (could be paused as well ;) ). Returns <code>null</code> if the playlist is
	 * empty.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The currently played song
	 */
	private PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong(IReadOnlyPlaylist playlist) {
		if ((playlist == null) || playlist.isPlaylistEmpty()) {
			return null;
		}

		try {
			return playlist.getSongAtPosition(playlist.getPositionInList());
		} catch (PlaylistPositionOutOfRangeException e) {
			assert false;
			return null;
		}
	}

	/**
	 * Returns the rating out of the actual playback position of the played song or <code>-1</code> if no such song
	 * exists.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The temporary rating
	 */
	private double getCurrentRating(IReadOnlyPlaylist playlist) {
		return RatingHelper.getRatingFromFractionPlayed(getCurrentFraction(playlist));
	}

	/**
	 * Returns the playback-fraction of the current song or <code>0</code> if no such song exists.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The temporary fraction played
	 */
	private double getCurrentFraction(IReadOnlyPlaylist playlist) {
		BaseSong<BaseArtist, BaseAlbum> currentSong = getCurrentSong(playlist);

		if (currentSong != null) {
			return getMilliSecondsPlayed(playlist) / (double) currentSong.getDuration();
		} else {
			return 0.0d; // We are at the start of the non-existing song
		}
	}

	/**
	 * Returns the playback-position of the current song.
	 * 
	 * @param playlist
	 *            The playlist
	 * @return The temporary fraction played
	 */
	private int getMilliSecondsPlayed(IReadOnlyPlaylist playlist) {
		return playlist.getPositionInSong();
	}

	/**
	 * Returns the agent votes for the currently playing song. If no such ratings exist, <code>null</code> will be
	 * returned.
	 * 
	 * @return The agent votes for the currently playing song
	 */
	public Map<IAgent, Float> getAgentVotesForCurrentSong() {
		if (lastNextSongCalculator == null) {
			return null;
		}
		return lastNextSongCalculator.getAgentVotes(lastNextSongCalculatorPosition);
	}

	/**
	 * @see NextSongCalculationThreadManager.Helper
	 */
	private final NextSongCalculationThreadManager.Helper nextSongCalculationThreadManagerHelper = new NextSongCalculationThreadManager.Helper() {

		@Override
		public double getTemporaryRatingForSong(BaseSong<BaseArtist, BaseAlbum> song) {
			if (Utils.nullEquals(getCurrentSong(playlist), song)) {
				return getCurrentRating(playlist);
			} else {
				return -1;
			}
		}

		@Override
		public PositiveNextSongCalculationThread createPositiveNextSongCalculationThread(
				BaseSong<BaseArtist, BaseAlbum> currentSong) {

			return new PositiveNextSongCalculationThread(currentSong, agentManager, collectionModel.getDbDataPortal(),
					playerModel.getPlayLog(), playerModel.getLogManager(), playerModel.getStatisticsProvider(),
					collectionModel.getOtherDataProvider());
		}

		@Override
		public NegativeNextSongCalculationThread createNegativeNextSongCalculationThread(
				BaseSong<BaseArtist, BaseAlbum> currentSong) {

			return new NegativeNextSongCalculationThread(currentSong, agentManager, collectionModel.getDbDataPortal(),
					playerModel.getPlayLog(), playerModel.getLogManager(), playerModel.getStatisticsProvider(),
					collectionModel.getOtherDataProvider());
		}
	};

	@Override
	public PlayModeType getPlayModeType() {
		return PlayModeType.SMART_SHUFFLE;
	}

	//******************************
	// CLASSES
	//*******************************

	private class AgentWeightsAdjustmentThread extends JoinableThread {

		private final Map<IAgent, Float> agentRatings;
		private final float realRating;

		public AgentWeightsAdjustmentThread(Map<IAgent, Float> agentRatings, float realRating) {
			this.agentRatings = agentRatings;
			this.realRating = realRating;
		}

		@Override
		public void run() {
			super.run();

			agentManager.adjustAgentWeights(agentRatings, realRating);

			// Mark this work as done
			agentWeightAdjustmentThreads.remove(this);
		}

	}
}
