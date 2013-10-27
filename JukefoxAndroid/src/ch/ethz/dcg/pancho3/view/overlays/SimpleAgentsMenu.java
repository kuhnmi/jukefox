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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;
import ch.ethz.dcg.pancho3.R;

public class SimpleAgentsMenu extends AbstractAgentsMenu {

	private Map<AgentType, List<IAgent>> agentTypeToAgents;
	private Map<SeekBar, AgentType> seekBarToAgentType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		agentTypeToAgents = new HashMap<AgentType, List<IAgent>>();
		seekBarToAgentType = new HashMap<SeekBar, AgentType>();

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void createAgentBars(AgentType agentType, List<IAgent> agents) {
		agentTypeToAgents.put(agentType, agents);

		TableLayout agentsTable = (TableLayout) findViewById(R.id.layout_agents);

		TableRow mainRow = new TableRow(this);
		mainRow.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mainRow.setPadding(0, 20, 0, 0);
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

		// Show a one-liner 
		SeekBar seekBar = createSeekBar();
		seekBarToAgentType.put(seekBar, agentType);
		mainRow.addView(seekBar);
	}

	@Override
	protected void setSeekBarPositions() {
		for (Map.Entry<SeekBar, AgentType> entry : seekBarToAgentType.entrySet()) {
			double groupWeight = 0.0d;
			for (IAgent agent : agentTypeToAgents.get(entry.getValue())) {
				groupWeight += agentManager.getAgentWeight(agent);
			}

			setSeekBarPosition(entry.getKey(), groupWeight);
		}
	}

	@Override
	protected Collection<SeekBar> getSeekBars() {
		return seekBarToAgentType.keySet();
	}

	@Override
	protected void onSeekBarsChanged(Map<SeekBar, Double> weights) {
		final Map<List<IAgent>, Double> groupWeights = new HashMap<List<IAgent>, Double>();
		for (Map.Entry<SeekBar, Double> entry : weights.entrySet()) {
			final AgentType agentType = seekBarToAgentType.get(entry.getKey());
			final double groupWeight = entry.getValue();

			groupWeights.put(agentTypeToAgents.get(agentType), groupWeight);
		}

		// save the weights
		agentManager.setAgentsGroupWeights(groupWeights);
	}

	@Override
	protected double getSeekBarStretchFactor() {
		return 3.3d;
	}

}
