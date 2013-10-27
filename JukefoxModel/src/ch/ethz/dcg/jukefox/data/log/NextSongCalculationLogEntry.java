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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.dcg.jukefox.playmode.smartshuffle.NextSongCalculationThread.Case;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;

public final class NextSongCalculationLogEntry extends AbstractLogEntry {

	private final static int VERSION = 1;
	private final static String TYPE_IDENT = "nsc";

	private Case predictionCase = null;
	private Integer currentMeSongId = null;
	private AgentsTiming proposalTimes = null;
	private AgentsTiming voteTimes = null;

	private NextSongCalculationLogEntry() {
	}

	public static Builder createInstance() {
		return new Builder();
	}

	public Case getPredictionCase() {
		return predictionCase;
	}

	public int getCurrentMeSongId() {
		return currentMeSongId;
	}

	public AgentsTiming getProposalTimes() {
		return proposalTimes;
	}

	public AgentsTiming getVoteTimes() {
		return voteTimes;
	}

	@Override
	public int getTypeVersion() {
		return VERSION;
	}

	@Override
	protected String getTypeIdent() {
		return TYPE_IDENT;
	}

	@Override
	protected String packYourStuff() {
		String caze;
		switch (getPredictionCase()) {
			case Positive:
				caze = "p";
				break;

			case Negative:
				caze = "n";
				break;

			default:
				assert false;
				caze = "";
		}

		// pack the times (agentIdent=proposalTime:voteTime)
		Map<IAgent, List<Long>> agentTimes = new HashMap<IAgent, List<Long>>();
		for (Map.Entry<IAgent, Long> entry : proposalTimes.getAgentTimings().entrySet()) {
			IAgent agent = entry.getKey();
			long proposalTime = entry.getValue();
			long voteTime = voteTimes.getAgentTiming(agent);

			List<Long> times = new ArrayList<Long>(2);
			times.add(proposalTime);
			times.add(voteTime);
			agentTimes.put(agent, times);
		}

		String timesStr = getPackedMap(agentTimes, new AgentFormatter(), new ListFormatter<Long>(new TimingFormatter()));

		return String.format("%s|%d|%s", // case|currentSong|agentTimes 
				caze, currentMeSongId, timesStr);
	}

	private class TimingFormatter implements Formatter<Long> {

		@Override
		public String format(Long value) {
			double time = value / 1000.0d; // ms -> s
			return String.format("%.1f", time);
		}

	}

	public final static class Builder extends AbstractLogEntry.Builder<NextSongCalculationLogEntry, Builder> {

		private Builder() {
			super();
			init(new NextSongCalculationLogEntry(), this);
		}

		public Builder setPredictionCase(Case predictionCase) {
			getInstance().predictionCase = predictionCase;
			return this;
		}

		public Builder setCurrentSong(int currentMeSongId) {
			getInstance().currentMeSongId = currentMeSongId;
			return this;
		}

		public Builder setProposalTimes(AgentsTiming proposalTimes) {
			getInstance().proposalTimes = proposalTimes;
			return this;
		}

		public Builder setVoteTimes(AgentsTiming voteTimes) {
			getInstance().voteTimes = voteTimes;
			return this;
		}
	}

	public final static class AgentsTiming {

		private final Map<IAgent, Long> agentTimings = new HashMap<IAgent, Long>();

		public AgentsTiming() {
		}

		/**
		 * Adds an agent timing entry.
		 * 
		 * @param agent
		 *            The agent
		 * @param timing
		 *            The timing [in ms]
		 * @return This builder instance
		 */
		public void addAgentTiming(IAgent agent, long timing) {
			agentTimings.put(agent, timing);
		}

		/**
		 * Returns the timings of the agents.
		 * 
		 * @return The timings [in ms]
		 */
		public Map<IAgent, Long> getAgentTimings() {
			return agentTimings;
		}

		/**
		 * Returns the timing of the given agent.
		 * 
		 * @param agent
		 *            The agent
		 * @return The timing [in ms]
		 */
		public long getAgentTiming(IAgent agent) {
			return agentTimings.get(agent);
		}

	}
}
