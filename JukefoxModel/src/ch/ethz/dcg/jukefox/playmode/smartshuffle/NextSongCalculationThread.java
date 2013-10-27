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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.StopWatch;
import ch.ethz.dcg.jukefox.commons.utils.TimingLogger;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.log.LogManager;
import ch.ethz.dcg.jukefox.data.log.NextSongCalculationLogEntry;
import ch.ethz.dcg.jukefox.data.log.NextSongCalculationLogEntry.AgentsTiming;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLog;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;

/**
 * This class searches for the next song which should be played. It asks several agents to return their proposals and
 * lets them rate all proposals. These ratings get weighted by the importance of the agents and a weighted drawing of
 * the proposed songs is done.<br/>
 * To reach regions in the music space which are not explored yet or we "forgot about", random entries from the set of
 * long not rated songs get added to the proposal list. <br/>
 * <br/>
 * There are two calculation modes: One which assumes, that the current song gets a positive and one that it gets a
 * negative rating. This is important, since we are calculating the next song asynchronous while the current song is
 * still playing.
 */
public abstract class NextSongCalculationThread extends JoinableThread {

	public enum Case {
		Positive,
		Negative
	};

	/**
	 * How many songs should be chosen at random from the list of long not rated songs and added to the song proposals
	 * (Set to zero to disable this feature).
	 */
	private static final int LONG_NOT_RATED_SONG_COUNT = 1;

	/**
	 * From when on a song is considered as long not played. [in h]
	 */
	private static final int LONG_NOT_RATED_THRESHOLD = 24 * 200; // 200d

	/**
	 * The vote for long not played songs.
	 */
	private static final double LONG_NOT_RATED_SONG_VOTE = 0.2d;
	// Do not define TAG, use getTag()

	/**
	 * How many songs are allowed to be proposed per agent.
	 */
	private static final int SONG_PROPOSAL_COUNT = 30;

	/**
	 * How many of the proposed songs of an agent actually get used.
	 */
	private static final int SONG_PROPOSAL_USAGE_COUNT = 7;

	private final AgentManager agentManager;
	private final IDbDataPortal dbDataPortal;
	private final PlayLog playLog;
	private final LogManager logManager;
	private final StatisticsProvider statisticsProvider;
	private final OtherDataProvider otherDataProvider;

	private boolean abortCalculation;

	private final BaseSong<BaseArtist, BaseAlbum> currentSong;
	private List<BaseSong<BaseArtist, BaseAlbum>> proposedSongs = null; // List of song proposals (randomized by vote)
	private Map<BaseSong<BaseArtist, BaseAlbum>, Double> songVotes = null; // map containing the overall vote for a song
	private Map<BaseSong<BaseArtist, BaseAlbum>, Map<IAgent, Float>> agentVotesBySong = null; // map containing the different agent votes for a song
	private Map<IAgent, Double> agentWeights = null;

	public NextSongCalculationThread(BaseSong<BaseArtist, BaseAlbum> currentSong, AgentManager agentManager,
			IDbDataPortal dbDataPortal, PlayLog playLog, LogManager logManager, StatisticsProvider statisticsProvider,
			OtherDataProvider otherDataProvider) {
		this.currentSong = currentSong;
		this.agentManager = agentManager;
		this.dbDataPortal = dbDataPortal;
		this.playLog = playLog;
		this.logManager = logManager;
		this.statisticsProvider = statisticsProvider;
		this.otherDataProvider = otherDataProvider;
	}

	@Override
	public void run() {
		try {
			throwIfAborted();

			TimingLogger timingLogger = new TimingLogger(getTag(), "next");

			// Begin an immediate transaction
			dbDataPortal.beginTransaction();
			timingLogger.addSplit("Transaction start");

			Set<IAgent> agents = agentManager.getAgents();
			agentWeights = agentManager.getAgentWeights();

			// Enter a fake rating entry to calculate the next song on the predicted future
			Integer meSongId = null;
			if (currentSong != null) {
				double fractionPlayed = (getCalculationCase() == Case.Positive) ? 0.66d : 0.33d;
				playLog.writeToPlayLog(new Date(), new PlaylistSong<BaseArtist, BaseAlbum>(currentSong,
						SongSource.SMART_SHUFFLE), true, (int) (currentSong.getDuration() * fractionPlayed));

				try {
					meSongId = otherDataProvider.getMusicExplorerSongId(currentSong);
				} catch (DataUnavailableException e) {
					Log.w(getTag(), e);
				}
			}
			timingLogger.addSplit("Rating entry");

			// Get the song proposals
			AgentsTiming proposalTimes = new AgentsTiming();
			{
				proposedSongs = getProposedSongs(proposalTimes);
			}
			timingLogger.addSplit("Proposals");

			// Get the agent votes for the proposals
			Map<IAgent, List<SongVote>> agentVotes;
			AgentsTiming voteTimes = new AgentsTiming();
			{
				agentVotes = new HashMap<IAgent, List<SongVote>>();
				songVotes = getVotesForProposed(proposedSongs, agentVotes, voteTimes);
			}
			timingLogger.addSplit("Votes");

			// Fill the song->agentVotes map
			agentVotesBySong = new HashMap<BaseSong<BaseArtist, BaseAlbum>, Map<IAgent, Float>>(proposedSongs.size());
			for (BaseSong<BaseArtist, BaseAlbum> song : proposedSongs) {
				agentVotesBySong.put(song, new HashMap<IAgent, Float>(agents.size()));
			}
			for (Map.Entry<IAgent, List<SongVote>> entry : agentVotes.entrySet()) {
				for (SongVote vote : entry.getValue()) {
					Map<IAgent, Float> agentVote = agentVotesBySong.get(vote.getSong());
					agentVote.put(entry.getKey(), vote.getVote());
				}
			}

			// Add long time not listened songs
			addLongTimeNotListenedSongProposals();

			/*// Order the next songs by rating
			Collections.sort(proposedSongs, new Comparator<BaseSong<BaseArtist, BaseAlbum>>() {

				@Override
				public int compare(BaseSong<BaseArtist, BaseAlbum> left, BaseSong<BaseArtist, BaseAlbum> right) {
					Double leftVote = songVotes.get(left);
					Double rightVote = songVotes.get(right);

					return rightVote.compareTo(leftVote);
				}
			});*/

			// Randomize the song proposals by their vote
			proposedSongs = getShuffledSongsAtWeightedRandom(songVotes);

			// FIXME @sämy: this is only for debugging
			StringBuffer proposalsSb = new StringBuffer();
			proposalsSb.append("song ident:overall vote");
			for (IAgent agent : agents) {
				proposalsSb.append(':');
				proposalsSb.append(agent.getIdentifier());
				proposalsSb.append(String.format(" (%.2f)", agentWeights.get(agent)));
			}
			proposalsSb.append('|');
			for (BaseSong<BaseArtist, BaseAlbum> song : proposedSongs) {
				proposalsSb.append(song); // song ident
				proposalsSb.append(':');
				proposalsSb.append(String.format("%.3f", songVotes.get(song))); // overall vote

				Map<IAgent, Float> agentVotes2 = agentVotesBySong.get(song);
				for (IAgent agent : agents) {
					proposalsSb.append(String.format(":%.3f", agentVotes2.get(agent)));
				}
				proposalsSb.append('|');
			}
			Log.d(getTag(), "Proposals: " + proposalsSb.toString());
			// end debug only

			// never ever call dbDataPortal.setTransactionSucessful() !!
			dbDataPortal.endTransaction(); // ROLLBACK the transaction. Things below that line get written persistently to the db!

			// Write a log entry about this run
			NextSongCalculationLogEntry.Builder log = NextSongCalculationLogEntry.createInstance()
					.setPredictionCase(getCalculationCase())
					.setCurrentSong((meSongId != null) ? meSongId : 0)
					.setProposalTimes(proposalTimes)
					.setVoteTimes(voteTimes);
			logManager.addLogEntry(log.build());

			// Write the time used in the different parts to the debug log
			timingLogger.dumpToLog();
		} catch (AbortException e) {
			// Just ignore it, we want to land here
		} finally {
			if (dbDataPortal.inTransaction()) {
				dbDataPortal.endTransaction(); // never ever call dbDataPortal.setTransactionSucessful() !!
			}
		}
	}

	/**
	 * Adds songs which did not get any rating data assigned for a long time (or never) to the {proposedSongs} list.
	 * This ensures reaching regions in the music space which are not explored yet (or have been forgotten about).<br/>
	 * The agents vote for these songs is set to {@value #LONG_NOT_RATED_SONG_VOTE}.
	 * 
	 * @param proposedSongs
	 *            The proposal list
	 * @param agentVotes
	 *            The votes list
	 */
	private void addLongTimeNotListenedSongProposals() {

		// Fetch long not rated songs
		List<BaseSong<BaseArtist, BaseAlbum>> longNotRatedSongs;
		try {
			longNotRatedSongs = statisticsProvider.getLongNotRatedSongs(
					LONG_NOT_RATED_SONG_COUNT, LONG_NOT_RATED_THRESHOLD);
		} catch (DataUnavailableException e) {
			// Just ignore the warning and do not add any songs
			Log.w(getTag(), e);
			longNotRatedSongs = new LinkedList<BaseSong<BaseArtist, BaseAlbum>>();
		}

		// Prepare agent votes for songs
		Map<IAgent, Float> agentVotesForProposedSongs = new HashMap<IAgent, Float>();
		for (IAgent agent : agentManager.getAgents()) {
			agentVotesForProposedSongs.put(agent, (float) LONG_NOT_RATED_SONG_VOTE);
		}

		// Add long not played songs as proposals
		for (BaseSong<BaseArtist, BaseAlbum> song : longNotRatedSongs) {
			proposedSongs.add(song);
			songVotes.put(song, LONG_NOT_RATED_SONG_VOTE);
			agentVotesBySong.put(song, agentVotesForProposedSongs);
		}
	}

	/**
	 * Returns the set of songs which are proposed by the agents.
	 * 
	 * @param proposalTimes
	 * @return The songs
	 */
	private List<BaseSong<BaseArtist, BaseAlbum>> getProposedSongs(AgentsTiming proposalTimes) {
		Set<IAgent> agents = agentManager.getAgents();

		// Using set here to ensure no duplicate entries
		Set<BaseSong<BaseArtist, BaseAlbum>> proposedSongs = new HashSet<BaseSong<BaseArtist, BaseAlbum>>(
				SONG_PROPOSAL_USAGE_COUNT * agents.size());
		for (IAgent agent : agents) {
			throwIfAborted();

			StopWatch stopWatch = StopWatch.start();

			List<BaseSong<BaseArtist, BaseAlbum>> agentProposals = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(
					agent.suggestSongs(SONG_PROPOSAL_COUNT));
			// Get #SONG_PROPOSAL_USAGE_COUNT items of them at random
			while (agentProposals.size() > SONG_PROPOSAL_USAGE_COUNT) {
				agentProposals.remove(RandomProvider.getRandom().nextInt(agentProposals.size()));
			}
			proposedSongs.addAll(agentProposals);

			proposalTimes.addAgentTiming(agent, stopWatch.stop());
		}

		return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(proposedSongs);
	}

	/**
	 * Returns the average votes of the agents for the given songs. The votes gets weighted by the importance of the
	 * agents.
	 * 
	 * @param proposedSongs
	 *            The proposed songs
	 * @param agentsVotes
	 *            (out) The votes for all the songs by agent
	 * @param voteTimes
	 * @return The weighted mean votes for the songs
	 */
	private Map<BaseSong<BaseArtist, BaseAlbum>, Double> getVotesForProposed(
			List<BaseSong<BaseArtist, BaseAlbum>> proposedSongs, Map<IAgent, List<SongVote>> agentsVotes,
			AgentsTiming voteTimes) {

		Set<IAgent> agents = agentManager.getAgents();

		// Init the ratings list & the weight sum table
		Map<BaseSong<BaseArtist, BaseAlbum>, Double> votes = new HashMap<BaseSong<BaseArtist, BaseAlbum>, Double>();
		Map<BaseSong<BaseArtist, BaseAlbum>, Double> weightSums = new HashMap<BaseSong<BaseArtist, BaseAlbum>, Double>(
				agents.size());
		for (BaseSong<BaseArtist, BaseAlbum> song : proposedSongs) {
			votes.put(song, 0.0d);
			weightSums.put(song, 0.0d);
		}

		// Calculate the song ratings
		for (IAgent agent : agents) {
			throwIfAborted();

			// Get the agent vote
			List<SongVote> agentVotes;

			StopWatch stopWatch = StopWatch.start();
			{
				agentVotes = agent.vote(Collections.unmodifiableList(proposedSongs));
			}
			voteTimes.addAgentTiming(agent, stopWatch.stop());

			// Fill up the list to ensure that a vote for all songs exist
			List<BaseSong<BaseArtist, BaseAlbum>> noVote = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(
					proposedSongs);
			for (int i = 0; i < agentVotes.size();) {
				final SongVote vote = agentVotes.get(i);
				int idx = noVote.indexOf(vote.getSong());
				if (idx >= 0) {
					noVote.remove(idx);
					++i;
				} else {
					// No vote for this song is required
					agentVotes.remove(i);
				}
			}
			for (BaseSong<BaseArtist, BaseAlbum> song : noVote) {
				agentVotes.add(new SongVote(song, 0.0f));
			}
			agentsVotes.put(agent, agentVotes);

			for (SongVote vote : agentVotes) {
				final BaseSong<BaseArtist, BaseAlbum> song = vote.getSong();

				double oldRating = votes.get(song);
				double agentWeight = getAgentWeights().get(agent);
				double oldW = weightSums.get(song);
				double newW = oldW + agentWeight;
				double newRating = (oldRating * oldW + vote.getVote() * agentWeight) / newW; // Continuous, weighted mean calculation
				votes.put(song, newRating);
				weightSums.put(song, newW);
			}
		}

		return votes;
	}

	/**
	 * Shuffles the song proposals at weighted random. For every position in the list
	 * 
	 * <pre>
	 * P[song choosen] = shifted_vote / votesSum
	 * </pre>
	 * 
	 * The votes get shifted to be pure positive and stretched, so that higher ratings are more probable to appear first
	 * 
	 * <pre>
	 * shifted_vote = (vote[song] + 1)<sup>STRETCH_FACTOR</sup>
	 * </pre>
	 * 
	 * @param votes
	 *            The song votes
	 * @return The shuffled song list
	 */
	private List<BaseSong<BaseArtist, BaseAlbum>> getShuffledSongsAtWeightedRandom(
			Map<BaseSong<BaseArtist, BaseAlbum>, Double> votes) {

		final double STRETCH_FACTOR = 4.0f;

		Map<BaseSong<BaseArtist, BaseAlbum>, Double> remainingVotes = new HashMap<BaseSong<BaseArtist, BaseAlbum>, Double>(
				votes);

		// Calculate the total vote sum
		double voteSum = 0.0d;
		for (Map.Entry<BaseSong<BaseArtist, BaseAlbum>, Double> entry : remainingVotes.entrySet()) {
			double adjustedVote = Math.pow(entry.getValue() + 1.0d, STRETCH_FACTOR); // to not have negative votes

			remainingVotes.put(entry.getKey(), adjustedVote);
			voteSum += adjustedVote;
		}

		// Shuffle songs 
		List<BaseSong<BaseArtist, BaseAlbum>> ret = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(votes.size());
		while (ret.size() < votes.size()) {
			// Search the next weighted random song 
			double rand = RandomProvider.getRandom().nextDouble() * voteSum;

			for (Map.Entry<BaseSong<BaseArtist, BaseAlbum>, Double> entry : remainingVotes.entrySet()) {
				if (rand <= entry.getValue()) {
					// Add the song
					ret.add(entry.getKey());

					// Reduce the search space for the next round
					remainingVotes.remove(entry.getKey());
					voteSum -= entry.getValue();

					break;
				} else {
					rand -= entry.getValue();
				}
			}
		}

		return ret;
	}

	/**
	 * Returns the song with the maximal votes.
	 * 
	 * @param votes
	 *            The votes list
	 * @return The song
	 */
	/*private PlaylistSong<BaseArtist, BaseAlbum> getSongWithMaxVotes(Map<BaseSong<BaseArtist, BaseAlbum>, Double> votes) {
		PlaylistSong<BaseArtist, BaseAlbum> song = null;
		double maxVote = -1000;
		for (Map.Entry<BaseSong<BaseArtist, BaseAlbum>, Double> entry : votes.entrySet()) {
			if (entry.getValue() > maxVote) {
				maxVote = entry.getValue();
				song = new PlaylistSong<BaseArtist, BaseAlbum>(entry.getKey(), SongSource.SMART_SHUFFLE);
			}
		}
		return song;
	}*/

	/**
	 * If this instance should calculate the negative or positive case.
	 * 
	 * @return
	 */
	public abstract Case getCalculationCase();

	/**
	 * Returns true, if we should stop the calculation as soon as possible.
	 * 
	 * @return
	 */
	public boolean isAborted() {
		return abortCalculation;
	}

	/**
	 * Throws an {@link AbortException} if this thread should be aborted.
	 */
	protected void throwIfAborted() {
		if (isAborted()) {
			throw new AbortException();
		}
	}

	/**
	 * Stop the calculation as soon as possible.
	 */
	public void abortCalculation() {
		abortCalculation = true;
	}

	/**
	 * True, if the calculation is finished and the proposed songs can be fetched.
	 * 
	 * @return
	 */
	public boolean isReady() {
		return proposedSongs != null;
	}

	/**
	 * Returns the i-th {@link #proposedSongs}. If the calculation is not ready yet, this method will return null.
	 * 
	 * @param i
	 * @return The i-th proposed song
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getProposedSong(int i) {
		if (proposedSongs == null) {
			return null;
		}
		if (i >= proposedSongs.size()) {
			return null;
		}
		return new PlaylistSong<BaseArtist, BaseAlbum>(proposedSongs.get(i), SongSource.SMART_SHUFFLE);
	}

	/**
	 * Returns the agent votes for the i-th {@link #proposedSongs}. If the calculation is not ready yet, this method
	 * will return null.
	 * 
	 * @param i
	 * @return The votes for the i-th proposed song
	 */
	public Map<IAgent, Float> getAgentVotes(int i) {
		if (agentVotesBySong == null) {
			return null;
		}
		return agentVotesBySong.get(getProposedSong(i));
	}

	/**
	 * Returns the weights of the agents at the time of the calculation.
	 * 
	 * @return The weights
	 */
	public Map<IAgent, Double> getAgentWeights() {
		return agentWeights;
	}

	/**
	 * Returns the song upon which the prediction is made.
	 * 
	 * @return The song
	 */
	public BaseSong<BaseArtist, BaseAlbum> getCurrentSong() {
		return currentSong;
	}

	private String getTag() {
		return String.format("%s [%s]", NextSongCalculationThread.class.getSimpleName(),
				(getCalculationCase() == Case.Positive) ? "positive" : "negative");
	}

	/**
	 * Dummy exception which is thrown if we should not continue working after
	 * {@link NextSongCalculationThread#abortCalculation()} is called.
	 */
	protected class AbortException extends RuntimeException {

		private static final long serialVersionUID = 7100275906852059482L;
	}
}
