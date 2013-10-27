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
package ch.ethz.dcg.jukefox.playmode.smartshuffle.agents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AbstractRecentAgent.TimeFilter;

public class AgentManager {

	private final static String TAG = AgentManager.class.getSimpleName();

	/**
	 * The general type of an agent. There are subtypes depending on the agent type.
	 */
	public enum AgentType {
		Random,
		Top,
		Suggested,
		Repetition
	}

	/**
	 * The minimum weight an agent can have. &in; [0, {@value #MAX_AGENT_WEIGHT})
	 */
	public final static double MIN_AGENT_WEIGHT = 0.01d;

	/**
	 * The maximum weight an agent can have. &in; ({@value #MIN_AGENT_WEIGHT}, 1]
	 */
	public final static double MAX_AGENT_WEIGHT = 1.0d;

	/**
	 * The factor which will be used to adjust the weights in {@link #adjustAgentWeights(Map, float)}.
	 */
	private final static double WEIGHT_CHANGE_FACTOR = 0.1d;

	/**
	 * Agents with their weights. Weights are &in; [{@value #MIN_AGENT_WEIGHT}, {@value #MAX_AGENT_WEIGHT}].
	 */
	private Map<IAgent, Double> agentWeights;
	private IDbDataPortal dbDataPortal;

	/**
	 * The singleton instance
	 */
	private static AgentManager instance = null;
	private boolean initialized = false;
	private Map<IAgent, Double> adjustmentBoost = new HashMap<IAgent, Double>();

	private AgentManager() {
	}

	/**
	 * Initializes the AgentManager singleton instance. If this method is called twice it simply does return the
	 * singleton-instance and does no reinitialization.
	 * 
	 * @param dbDataPortal
	 * @param songProvider
	 * @param statisticsProvider
	 * @return The singleton instance
	 */
	public static AgentManager initialize(IDbDataPortal dbDataPortal, SongProvider songProvider,
			StatisticsProvider statisticsProvider) {
		AgentManager self = AgentManager.getInstance();
		synchronized (self) {
			if (self.initialized) {
				return self;
			}
			self.initialized = true;
		}

		self.dbDataPortal = dbDataPortal;

		// Register the agents
		self.agentWeights = new HashMap<IAgent, Double>();
		self.registerAgent(new RandomAgent(songProvider), 0.5d);
		self.registerAgent(new TopArtistAgent(songProvider, statisticsProvider, TimeFilter.HOUR_OF_THE_DAY), 0.5d);
		//self.registerAgent(new TopArtistAgent(songProvider, statisticsProvider, TimeFilter.DAY_OF_THE_WEEK), 0.5d);
		self.registerAgent(new TopArtistAgent(songProvider, statisticsProvider, TimeFilter.RECENTLY), 0.5d);
		self.registerAgent(new TopArtistAgent(songProvider, statisticsProvider, TimeFilter.NONE), 0.5d);
		self.registerAgent(new SuggestedAgent(statisticsProvider, TimeFilter.HOUR_OF_THE_DAY), 0.5d);
		//self.registerAgent(new SuggestedAgent(statisticsProvider, TimeFilter.DAY_OF_THE_WEEK), 0.5d);
		self.registerAgent(new SuggestedAgent(statisticsProvider, TimeFilter.RECENTLY), 0.5d);
		self.registerAgent(new SuggestedAgent(statisticsProvider, TimeFilter.NONE), 0.5d);
		self.registerAgent(new SongRepetitionAgent(statisticsProvider), 0.5d);
		self.registerAgent(new ArtistRepetitionAgent(statisticsProvider), 0.5d);

		// Normalize their weights
		self.normalizeAgentWeights();

		return self;
	}

	public static AgentManager getInstance() {
		if (instance == null) {
			synchronized (AgentManager.class) {
				if (instance == null) {
					instance = new AgentManager();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the registered agents.
	 * 
	 * @return The agents
	 */
	public Set<IAgent> getAgents() {
		return agentWeights.keySet();
	}

	/**
	 * Adds the given agent to the list and loads its weight from the database. If no weight is in the database, we set
	 * it to defaultWeight.
	 * 
	 * @param agent
	 *            The agent
	 * @param defaultWeight
	 *            The weight of this agent if it can not be found in the database
	 */
	protected void registerAgent(IAgent agent, double defaultWeight) {
		double weight;
		try {
			weight = dbDataPortal.getKeyValueDouble("agents.weight", agent.getIdentifier());
		} catch (DataUnavailableException e) {
			weight = defaultWeight;
		}

		agentWeights.put(agent, weight);
	}

	/**
	 * @see #normalizeWeights(Map)
	 */
	private void normalizeAgentWeights() {
		normalizeWeights(agentWeights);
	}

	/**
	 * Normalizes the weights in the given map, so that they are in [{@value #MIN_AGENT_WEIGHT},
	 * {@value #MAX_AGENT_WEIGHT}] and sum up to 1.
	 * 
	 * @param weights
	 *            The weights
	 */
	public static <T extends Object> void normalizeWeights(Map<T, Double> weights) {
		double weightSum = 0.0d;
		for (double weight : weights.values()) {
			weight = Math.max(weight, MIN_AGENT_WEIGHT);
			weight = Math.min(weight, MAX_AGENT_WEIGHT);

			weightSum += weight;
		}

		for (Map.Entry<T, Double> entry : weights.entrySet()) {
			double newWeight = entry.getValue() * 1 / weightSum;
			newWeight = Math.max(newWeight, MIN_AGENT_WEIGHT);
			newWeight = Math.min(newWeight, MAX_AGENT_WEIGHT);

			weights.put(entry.getKey(), newWeight);
		}
	}

	/**
	 * Returns true, if we should adjust the agents weight automatically by their voting correctness.
	 * 
	 * @return
	 */
	public boolean isAutoAdjustAgentWeights() {
		try {
			return dbDataPortal.getKeyValueInt("agents", "auto_adjust_weight") != 0;
		} catch (DataUnavailableException e) {
			return true;
		}
	}

	/**
	 * Setter for if we should adjust the agents weight automatically by their voting correctness.
	 * 
	 * @param doIt
	 *            If we should do it
	 */
	public void setAutoAdjustAgentWeights(boolean doIt) {
		try {
			dbDataPortal.setKeyValue("agents", "auto_adjust_weight", doIt ? 1 : 0);
		} catch (DataWriteException e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * Returns the weight of the given agent.
	 * 
	 * @param agent
	 *            The agent
	 * @return The weight &in; [{@value #MIN_AGENT_WEIGHT}, {@value #MAX_AGENT_WEIGHT}]
	 */
	public double getAgentWeight(IAgent agent) {
		return agentWeights.get(agent);
	}

	/**
	 * Returns the weight of all the agents.
	 * 
	 * @return The weight &in; [{@value #MIN_AGENT_WEIGHT}, {@value #MAX_AGENT_WEIGHT}]
	 */
	public Map<IAgent, Double> getAgentWeights() {
		return agentWeights;
	}

	/**
	 * Adjusts out of bounds weights and sets it for the agent afterwards.
	 * 
	 * @param agent
	 *            The agent
	 * @param weight
	 *            The new weight &in; [{@value #MIN_AGENT_WEIGHT}, {@value #MAX_AGENT_WEIGHT}]
	 */
	private void setAgentWeight(IAgent agent, double weight) {
		weight = Math.max(MIN_AGENT_WEIGHT, weight);
		weight = Math.min(MAX_AGENT_WEIGHT, weight);
		agentWeights.put(agent, weight);
	}

	/**
	 * Sets the weight of the agents and saves them.<br/>
	 * Attention: In most cases you want to call {@link #adjustAgentWeights(Map, float)}!
	 * 
	 * @param agentWeights
	 *            The agent-weight map
	 */
	public void setAgentWeights(Map<IAgent, Double> agentWeights) {
		// Set the weights
		for (Map.Entry<IAgent, Double> entry : agentWeights.entrySet()) {
			setAgentWeight(entry.getKey(), entry.getValue());
		}

		// Normalize the weights
		normalizeAgentWeights();

		// Save the weights
		saveAgentWeights();
	}

	/**
	 * Sets the weights of the agents in the given agent group. The groupWeight is distributed to the agents by the same
	 * fraction the old weights are distributed among them.
	 * 
	 * @param agents
	 *            The agents in this group
	 * @param groupWeight
	 *            The new weight of the whole group
	 */
	private void setAgentsGroupWeight(List<IAgent> agents, double groupWeight) {
		double agentsWeightSum = 0.0d;
		for (IAgent agent : agents) {
			agentsWeightSum += getAgentWeight(agent);
		}

		for (IAgent agent : agents) {
			double weightFraction = getAgentWeight(agent) / agentsWeightSum;
			setAgentWeight(agent, groupWeight * weightFraction);
		}
	}

	/**
	 * Sets the weights of the agents in the given agent groups.
	 * 
	 * @param groupWeights
	 *            The weights of the groups
	 * @see #setAgentsGroupWeight(List, double)
	 */
	public void setAgentsGroupWeights(Map<List<IAgent>, Double> groupWeights) {
		// Set the weights
		for (Map.Entry<List<IAgent>, Double> entry : groupWeights.entrySet()) {
			setAgentsGroupWeight(entry.getKey(), entry.getValue());
		}

		// Normalize the weights
		normalizeAgentWeights();

		// Save the weights
		saveAgentWeights();
	}

	/**
	 * Stores the weights of the agents to the database.<br/>
	 * Please call {@link #normalizeAgentWeights()} first.
	 */
	private void saveAgentWeights() {
		for (Map.Entry<IAgent, Double> entry : agentWeights.entrySet()) {
			try {
				dbDataPortal.setKeyValue("agents.weight", entry.getKey().getIdentifier(), entry.getValue());
			} catch (DataWriteException e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * Increases or decreases the weight of the agents. The base for the adjustment is agentVote*songRating. Therefore,
	 * if an agents vote is similar to the actual rating, its weight is increased and if it is very different, the
	 * weight is decreased. Moreover, agents with strong votes (where |vote| &asymp; 1) get more adjusted than these
	 * with moderate votes (A vote of 0 results in no weight adjustment at all).<br/>
	 * We also are choosing the adjustments to get bigger if an agent votes wrong multiple times in a row (see
	 * {@link #getAdjustment(IAgent, double)}).
	 * 
	 * <pre>
	 * 	  w<sub>new</sub> = (1 - {@value #WEIGHT_CHANGE_FACTOR}) * w<sub>old</sub> + {@value #WEIGHT_CHANGE_FACTOR} * adjustment
	 * </pre>
	 * 
	 * To see, how <code>adjustment</code> is determined, see
	 * 
	 * @param agentVotes
	 *            The agents votes
	 * @param songRating
	 *            The song rating
	 * @see #getAdjustment(IAgent, double)
	 */
	public void adjustAgentWeights(Map<IAgent, Float> agentVotes, float songRating) {
		if (agentVotes == null) {
			return;
		}
		if (!isAutoAdjustAgentWeights()) {
			return;
		}

		// Adjust the weights
		Map<IAgent, Double> oldWeights = new HashMap<IAgent, Double>(agentWeights);
		for (Map.Entry<IAgent, Float> agentVote : agentVotes.entrySet()) {
			final IAgent agent = agentVote.getKey();

			double ratingVoteProduct = (double) songRating * agentVote.getValue(); // in [-1, 1]
			double adjustment = getAdjustment(agent, ratingVoteProduct);

			double oldWeight = getAgentWeight(agent);
			oldWeights.put(agent, oldWeight);

			double newWeight = (1 - WEIGHT_CHANGE_FACTOR) * oldWeight + WEIGHT_CHANGE_FACTOR * adjustment;

			setAgentWeight(agent, newWeight);
		}

		// Normalize the weights
		normalizeAgentWeights();

		String diffMsg = "Weight diffs:\n";
		for (Map.Entry<IAgent, Double> entry : agentWeights.entrySet()) {
			IAgent agent = entry.getKey();
			diffMsg += String.format("  %s: %.2f\n", agent.getIdentifier(),
					entry.getValue() - oldWeights.get(agent));
		}
		Log.d(TAG, diffMsg);

		// Store them 
		saveAgentWeights();
	}

	/**
	 * Returns the adjustment for the given agent-ratingVoteProduct combination. The returned value is
	 * 
	 * <pre>
	 * adjustment = weight(agent) + boost<sub>i</sub>(agent) * ratingVoteProduct
	 * </pre>
	 * 
	 * Boost is calculated as
	 * 
	 * <pre>
	 * ratingVoteProduct >= 0 => boost<sub>i</sub>(agent) = boost<sub>i+1</sub>(agent) = 1<br/>
	 * ratingVoteProduct  < 0 => boost<sub>i+1</sub>(agent) = boost<sub>i</sub>(agent) + |ratingVoteProduct|
	 * </pre>
	 * 
	 * We use boost to penalize repeatly negative {ratingVoteProduct}s.
	 * 
	 * @param agent
	 *            The agent
	 * @param ratingVoteProduct
	 *            The rating of the song times the vote of the agent.
	 * @return The adjustment which should be used
	 * @see #adjustAgentWeights(Map, float)
	 */
	private double getAdjustment(final IAgent agent, double ratingVoteProduct) {
		Double boost = adjustmentBoost.get(agent);
		if ((ratingVoteProduct >= 0) || (boost == null)) {
			boost = 1.0d;
		}

		double adjustment = agentWeights.get(agent) + boost * ratingVoteProduct;

		if (ratingVoteProduct < 0) {
			boost += -1 * ratingVoteProduct; // equals boost += abs(ratingVoteProduct)
		}
		adjustmentBoost.put(agent, boost);

		return adjustment;
	}
}
