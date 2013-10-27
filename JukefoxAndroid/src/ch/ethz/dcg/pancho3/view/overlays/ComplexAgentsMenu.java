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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AbstractRecentAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AbstractRepetitionAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.ArtistRepetitionAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.SongRepetitionAgent;
import ch.ethz.dcg.pancho3.R;

public class ComplexAgentsMenu extends AbstractAgentsMenu {

	private Map<SeekBar, IAgent> seekBarToAgent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		seekBarToAgent = new HashMap<SeekBar, IAgent>();

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void createAgentBars(AgentType agentType, List<IAgent> agents) {
		TableLayout agentsTable = (TableLayout) findViewById(R.id.layout_agents);

		TableRow mainRow = new TableRow(this);
		mainRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mainRow.setPadding(0, 10, 0, 0);
		{
			TextView mainTitle = new TextView(this);
			mainTitle.setLayoutParams(new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
					(agents.size() == 1) ? LayoutParams.MATCH_PARENT : android.app.ActionBar.LayoutParams.WRAP_CONTENT,
					1.0f));
			mainTitle.setGravity(Gravity.CENTER_VERTICAL);
			mainTitle.setText(getGroupTitle(agentType));

			mainRow.addView(mainTitle);
		}
		agentsTable.addView(mainRow);

		if (agents.size() == 1) {
			// Show a one-liner 
			SeekBar seekBar = createSeekBar();
			seekBarToAgent.put(seekBar, agents.get(0));
			mainRow.addView(seekBar);
		} else {
			// Sort the agents
			Collections.sort(agents, new Comparator<IAgent>() {

				@Override
				public int compare(IAgent left, IAgent right) {
					return getSubTitle(left).compareTo(getSubTitle(right));
				}
			});

			// Add placeholder to finish main row
			View placeHolder = new View(this);
			placeHolder.setLayoutParams(new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT, 0));
			mainRow.addView(placeHolder);

			// Add row for every agent
			for (IAgent agent : agents) {
				TableRow row = new TableRow(this);
				row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				{
					// Create text
					TextView subTitle = new TextView(this);
					subTitle.setLayoutParams(new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
							LayoutParams.MATCH_PARENT, 1.0f));
					subTitle.setPadding(30, 0, 0, 0);
					subTitle.setGravity(Gravity.CENTER_VERTICAL);
					subTitle.setText(getSubTitle(agent));

					// Create seek bar
					SeekBar seekBar = createSeekBar();
					seekBarToAgent.put(seekBar, agent);
					seekBar.setMax(10000);

					row.addView(subTitle);
					row.addView(seekBar);
				}
				agentsTable.addView(row);
			}
		}
	}

	/**
	 * Returns the sub-title for the given agent.
	 * 
	 * @param agent
	 *            The agent
	 * @return The sub-title
	 */
	private String getSubTitle(IAgent agent) {
		switch (agent.getAgentType()) {
			case Random:
				/* Only one instance of a random agent should be there (and therefore getSubTitle() not be called), 
				 * if not, make a distinction of the instances */
				assert false;
				return "";

			case Repetition:
				AbstractRepetitionAgent repAgent = (AbstractRepetitionAgent) agent;
				if (repAgent.getRepetitionType().equals(SongRepetitionAgent.REPETITION_TYPE)) {
					return getString(R.string.agent_subtitle_repetition_song);
				} else if (repAgent.getRepetitionType().equals(ArtistRepetitionAgent.REPETITION_TYPE)) {
					return getString(R.string.agent_subtitle_repetition_artist);
				} else {
					assert false;
					return "";
				}

			case Suggested:
			case Top:
				AbstractRecentAgent recAgent = (AbstractRecentAgent) agent;
				switch (recAgent.getTimeFilter()) {
					case DAY_OF_THE_WEEK:
						return getString(R.string.agent_subtitle_recent_day_of_the_week);
					case HOUR_OF_THE_DAY:
						return getString(R.string.agent_subtitle_recent_hour_of_the_day);
					case RECENTLY:
						return getString(R.string.agent_subtitle_recent_recently);
					case NONE:
						return getString(R.string.agent_subtitle_recent_overall);
					default:
						assert false;
						return "";
				}

			default:
				assert false;
				return "";
		}
	}

	@Override
	protected void setSeekBarPositions() {
		for (Map.Entry<SeekBar, IAgent> entry : seekBarToAgent.entrySet()) {
			setSeekBarPosition(entry.getKey(), agentManager.getAgentWeight(entry.getValue()));
		}
	}

	@Override
	protected Collection<SeekBar> getSeekBars() {
		return seekBarToAgent.keySet();
	}

	@Override
	protected void onSeekBarsChanged(Map<SeekBar, Double> weights) {
		final Map<IAgent, Double> agentWeights = new HashMap<IAgent, Double>(weights.size());
		for (Map.Entry<SeekBar, Double> entry : weights.entrySet()) {
			final IAgent agent = seekBarToAgent.get(entry.getKey());
			agentWeights.put(agent, entry.getValue());
		}

		// save the weights
		agentManager.setAgentWeights(agentWeights);
	}

	@Override
	protected double getSeekBarStretchFactor() {
		return 4.0d;
	}

}
