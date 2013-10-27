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
package ch.ethz.dcg.jukefox.data.log;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;

public class SmartShuffleNextSongLogEntry extends AbstractLogEntry {

	private final static String TYPE_IDENT = "smns";
	private final static int VERSION = 1;

	public enum SongSource {
		Playlist, // Song was already in playlist
		NextCalculator, // We took the proposal from a NextSongCalculationThread 
		ReusedNextCalculator, // We reused an old NextSongCalculationThread since the new calculations are not available yet
		Random // We chose a song at random
	}

	private SongSource songSource = null;
	private Integer currentMeSongId = null;
	private Double currentRating = null;
	private Double fractionPlayed = null;
	private Integer secondsPlayed = null;
	private Map<IAgent, Double> agentWeightsOpt = null; // If songSource \in {PlayList, Random} => null
	private Map<IAgent, Float> agentVotesOpt = null; // If songSource \in {PlayList, Random} => null

	private SmartShuffleNextSongLogEntry() {
	}

	public static Builder createInstance() {
		return new Builder();
	}

	@Override
	protected String getTypeIdent() {
		return TYPE_IDENT;
	}

	@Override
	public int getTypeVersion() {
		return VERSION;
	}

	public SongSource getSongSource() {
		return songSource;
	}

	public int getCurrentMeSongId() {
		return currentMeSongId;
	}

	public double getCurrentRating() {
		return currentRating;
	}

	public double getFractionPlayed() {
		return fractionPlayed;
	}

	public int getSecondsPlayed() {
		return secondsPlayed;
	}

	public Map<IAgent, Double> getAgentWeights() {
		return agentWeightsOpt;
	}

	public Map<IAgent, Float> getAgentVotes() {
		return agentVotesOpt;
	}

	@Override
	protected String packYourStuff() {
		String songSourceStr;
		switch (songSource) {
			case NextCalculator:
				songSourceStr = "n";
				break;

			case ReusedNextCalculator:
				songSourceStr = "rn";
				break;

			case Random:
				songSourceStr = "r";
				break;

			default:
				assert false;
				songSourceStr = "";
		}

		// Pack the vote-weight pairs
		Map<IAgent, Pair<Float, Double>> voteWeights = null;
		if ((agentWeightsOpt != null) && (agentVotesOpt != null)) {
			voteWeights = new HashMap<IAgent, Pair<Float, Double>>(agentWeightsOpt.size());
			for (Map.Entry<IAgent, Float> entry : agentVotesOpt.entrySet()) {
				IAgent agent = entry.getKey();
				float vote = entry.getValue();
				double weight = agentWeightsOpt.get(agent);

				voteWeights.put(agent, new Pair<Float, Double>(vote, weight));
			}
		}

		String agentVotesStr = getPackedMap(voteWeights, new AgentFormatter(), new Formatter<Pair<Float, Double>>() {

			@Override
			public String format(Pair<Float, Double> value) {
				return String.format("%.2f:%.2f", value.first, value.second); // vote:weight
			}
		});

		return String.format("%s|%d|%s|%.2f|%.2f|%d", // songSource|songId|agentVote:agentWeight|songRating|fractionPlayed|secondsPlayed
				songSourceStr, currentMeSongId, agentVotesStr, currentRating, fractionPlayed, secondsPlayed);
	}

	public final static class Builder extends AbstractLogEntry.Builder<SmartShuffleNextSongLogEntry, Builder> {

		private Builder() {
			super();
			init(new SmartShuffleNextSongLogEntry(), this);
		}

		public Builder setSongSource(SongSource songSource) {
			getInstance().songSource = songSource;
			return this;
		}

		public Builder setCurrentMeSongId(int currentMeSongId) {
			getInstance().currentMeSongId = currentMeSongId;
			return this;
		}

		public Builder setCurrentRating(double currentRating) {
			getInstance().currentRating = currentRating;
			return this;
		}

		public Builder setFractionPlayed(double fractionPlayed) {
			getInstance().fractionPlayed = fractionPlayed;
			return this;
		}

		public Builder setSecondsPlayed(int secondsPlayed) {
			getInstance().secondsPlayed = secondsPlayed;
			return this;
		}

		public Builder setOptAgentWeights(Map<IAgent, Double> agentWeights) {
			getInstance().agentWeightsOpt = agentWeights;
			return this;
		}

		public Builder setOptAgentVotes(Map<IAgent, Float> agentVotes) {
			getInstance().agentVotesOpt = agentVotes;
			return this;
		}
	}
}
