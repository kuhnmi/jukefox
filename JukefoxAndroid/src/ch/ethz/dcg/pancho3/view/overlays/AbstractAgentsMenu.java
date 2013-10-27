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
package ch.ethz.dcg.pancho3.view.overlays;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;
import ch.ethz.dcg.pancho3.R;

public abstract class AbstractAgentsMenu extends JukefoxOverlayActivity {

	protected AgentManager agentManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		agentManager = AgentManager.initialize(collectionModel.getDbDataPortal(), songProvider, statisticsProvider);

		setContentView(R.layout.agentsmenu);

		registerButtonListeners();
		createAgentBars();
	}

	private void registerButtonListeners() {
		// Auto adjust weights
		CheckBox ckbAutoAdjustWeights = (CheckBox) findViewById(R.id.ckb_auto_adjust_agent_weights);
		ckbAutoAdjustWeights.setChecked(agentManager.isAutoAdjustAgentWeights());

		ckbAutoAdjustWeights.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				agentManager.setAutoAdjustAgentWeights(isChecked);
			}
		});
	}

	private void createAgentBars() {
		// Find the grouped agent list
		Map<AgentType, List<IAgent>> agentsByType = new HashMap<AgentType, List<IAgent>>();
		for (IAgent agent : agentManager.getAgents()) {
			AgentType type = agent.getAgentType();

			if (!agentsByType.containsKey(type)) {
				agentsByType.put(type, new ArrayList<IAgent>());
			}
			agentsByType.get(type).add(agent);
		}

		// Remove all old entries from the agents table
		TableLayout agentsTable = (TableLayout) findViewById(R.id.layout_agents);
		agentsTable.removeAllViews();

		// Create the agent bars. The order here is the order they appear
		createAgentBars(AgentType.Suggested, agentsByType.get(AgentType.Suggested));
		createAgentBars(AgentType.Top, agentsByType.get(AgentType.Top));
		createAgentBars(AgentType.Repetition, agentsByType.get(AgentType.Repetition));
		createAgentBars(AgentType.Random, agentsByType.get(AgentType.Random));

		setSeekBarPositions();
	}

	/**
	 * Creates the agent bar(s) for the given agents of the type agentType.
	 * 
	 * @param agentType
	 *            The type all the agents have in common
	 * @param agents
	 *            The agents
	 */
	protected abstract void createAgentBars(AgentType agentType, List<IAgent> agents);

	/**
	 * Creates a agent weight seek bar and adds the mapping to {@link #agentToSeekBar}.
	 * 
	 * @param agent
	 *            The agent
	 * @return The seek bar
	 */
	protected SeekBar createSeekBar() {
		SeekBar seekBar = new SeekBar(this);
		seekBar.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		seekBar.setPadding(15, 0, 0, 0);
		seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

		return seekBar;
	}

	/**
	 * Returns the group title for the given agentType.
	 * 
	 * @param agentType
	 * @return
	 */
	protected String getGroupTitle(AgentType agentType) {
		switch (agentType) {
			case Random:
				return getString(R.string.agent_title_random);

			case Repetition:
				return getString(R.string.agent_title_repetition);

			case Suggested:
				return getString(R.string.agent_title_suggested);

			case Top:
				return getString(R.string.agent_title_top);

			default:
				assert false;
				return "";
		}
	}

	/**
	 * Fills the seek bar positions by the agent weights.
	 */
	protected abstract void setSeekBarPositions();

	/**
	 * Returns the exponent which is used in {@link #getSeekBarPositionForWeight(double, int)} and
	 * {@link #getWeightFromSeekBarPosition(int, int)}. The bigger it is the more the weights get stretched.
	 * 
	 * @return The exponent
	 */
	protected abstract double getSeekBarStretchFactor();

	/**
	 * Returns the seek bar position for the given weight. The position is calculated by the following formula, to have
	 * more space for small agent weights since they are more likely. We also do a range adjustment: We map the weight
	 * range [{@link AgentManager#MIN_AGENT_WEIGHT}, 1] to [0, maxPos].
	 * 
	 * <pre>
	 * sf = {@link #getSeekBarStretchFactor()}
	 * position(weight) = ((weight - {@link AgentManager#MIN_AGENT_WEIGHT}) * 1/(1 - {@link AgentManager#MIN_AGENT_WEIGHT}))<sup>1/sf</sup>
	 * </pre>
	 * 
	 * @param weight
	 *            The agents weight
	 * @param maxPos
	 *            The maximum position of the seek bar
	 */
	protected int getSeekBarPositionForWeight(double weight, int maxPos) {
		double minWeight = AgentManager.MIN_AGENT_WEIGHT;

		// Ensure minimum constraint of weight
		weight = Math.max(weight, minWeight);

		// Calculate the position
		double adjustedWeight = (weight - minWeight) / (1 - minWeight);
		double pos = Math.pow(adjustedWeight, 1 / getSeekBarStretchFactor());

		return (int) (pos * maxPos);
	}

	/**
	 * This is the inverse function of {@link #getSeekBarPositionForWeight(double, int)}
	 * 
	 * @param position
	 *            The seek bar position
	 * @param maxPos
	 *            The maximum position of the seek bar
	 * @return The weight
	 */
	private double getWeightFromSeekBarPosition(int position, int maxPos) {
		double pos = position / (double) maxPos;
		double adjustedWeight = Math.pow(pos, getSeekBarStretchFactor());

		double minWeight = AgentManager.MIN_AGENT_WEIGHT;
		double weight = adjustedWeight * (1 - minWeight) + minWeight;

		return weight;
	}

	/**
	 * @see #getSeekBarPositionForWeight(double, int)
	 * 
	 * @param seekBar
	 *            The seek bar
	 * @param weight
	 *            The agents weight
	 */
	protected void setSeekBarPosition(SeekBar seekBar, double weight) {
		seekBar.setProgress(getSeekBarPositionForWeight(weight, seekBar.getMax()));
	}

	/**
	 * Returns the weight which represents the seek bar position.
	 * 
	 * @see #getWeightFromSeekBarPosition(int, int)
	 * 
	 * @param seekBar
	 *            The seek bar
	 * @return The weight
	 */
	private double getWeightFromSeekBar(SeekBar seekBar) {
		return getWeightFromSeekBarPosition(seekBar.getProgress(), seekBar.getMax());
	}

	/**
	 * Returns the seek bars.
	 * 
	 * @return The seek bars
	 */
	protected abstract Collection<SeekBar> getSeekBars();

	/**
	 * Handles the event, when the seek bar positions have changed. The weights should be passed to
	 * {@link #agentManager}.
	 * 
	 * @param weights
	 *            The seekBar-to-weight map
	 */
	protected abstract void onSeekBarsChanged(Map<SeekBar, Double> weights);

	/**
	 * Event listener for the seeks, to adjust the other seeks, if one is changed.
	 */
	private final OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {

		private double oldWeight;
		private double otherWeightSum;
		private boolean hasChanged = false;

		@Override
		public void onStartTrackingTouch(SeekBar changedSeekBar) {
			hasChanged = true;

			oldWeight = getWeightFromSeekBar(changedSeekBar);

			otherWeightSum = 0.0d;
			for (SeekBar seekBar : getSeekBars()) {
				if (seekBar != changedSeekBar) {
					otherWeightSum += getWeightFromSeekBar(seekBar);
				}
			}
		}

		private Integer saveRound = 0;

		@Override
		public void onStopTrackingTouch(SeekBar changedSeekBar) {
			// Read out the weights
			final Collection<SeekBar> seekBars = getSeekBars();

			final Map<SeekBar, Double> seekBarWeights = new HashMap<SeekBar, Double>(seekBars.size());
			for (SeekBar seekBar : seekBars) {
				double weight = getWeightFromSeekBar(seekBar);
				seekBarWeights.put(seekBar, weight);
			}

			// Set them async
			final int ourSaveRound = ++saveRound;
			new Thread(new Runnable() {

				@Override
				public void run() {
					synchronized (saveRound) {
						if (ourSaveRound < saveRound) {
							return;
						}

						hasChanged = false;

						onSeekBarsChanged(seekBarWeights);

						if (!hasChanged) {
							// Reset the seek bar positions to the agentManager values (might be adjusted)
							setSeekBarPositions();
						}
					}
				}
			}).start();
		}

		/**
		 * If a seek is changed by the user always adjust the other seeks, so that the weights sum up to 1 again.
		 */
		@Override
		public void onProgressChanged(SeekBar changedSeekBar, int progress, boolean fromUser) {
			if (!fromUser) {
				return;
			}

			// Calculate adjustment
			double newWeight = getWeightFromSeekBar(changedSeekBar);
			double adjustment = 1 + (oldWeight - newWeight) / otherWeightSum;

			// Adjust the other seek bars
			for (SeekBar seekBar : getSeekBars()) {
				if (seekBar != changedSeekBar) {
					setSeekBarPosition(seekBar, adjustment * getWeightFromSeekBar(seekBar));
				}
			}

			// Prepare for the next call
			oldWeight = newWeight;
			otherWeightSum *= adjustment;
		}
	};

}
