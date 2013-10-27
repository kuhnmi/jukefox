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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AbstractRecentAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AbstractRepetitionAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.ArtistRepetitionAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.RandomAgent;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.SongRepetitionAgent;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class AgentVotesToast {

	private static final String TAG = AgentVotesToast.class.getSimpleName();

	private static Integer currentInfoBoxId = 0;
	private static View shownInfoBox = null;

	private final BaseSong<BaseArtist, BaseAlbum> song;
	private final Map<IAgent, Float> agentVotes;
	private final Activity parent;

	private View infoBox = null;
	private int infoBoxId;

	public AgentVotesToast(BaseSong<BaseArtist, BaseAlbum> song, Map<IAgent, Float> agentVotes, Activity parent) {
		this.agentVotes = agentVotes;
		this.parent = parent;
		this.song = song;
	}

	/**
	 * Shows a toast with the top 3 agents (by weight) and their votes for a given song. Moreover their weight is shown
	 * as a vertical bar.
	 * 
	 * @param agentVotes
	 *            The agent votes
	 * @param parent
	 *            The parent for the toast
	 */
	public void show() {
		if (agentVotes == null) {
			return;
		}

		// Create the infoBox (if not already done)
		initInfoBox();

		synchronized (currentInfoBoxId) {
			infoBoxId = ++currentInfoBoxId;
		}

		// Show debug output of all ratings
		String votesMsg = String.format("The votes for [%s]: \n", song.toString());
		for (Map.Entry<IAgent, Float> entry : agentVotes.entrySet()) {
			votesMsg += "  " + getAgentTitle(entry.getKey()) + ": " + entry.getValue() + "\n";
		}
		Log.d(TAG, votesMsg);

		// Show it! (in the ui-thread)
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				doShow();
			}
		});
	}

	private void initInfoBox() {
		synchronized (this) {
			if (infoBox != null) {
				return;
			}
			infoBox = View.inflate(parent, R.layout.agentsvote_toast, null);
		}

		/*		// Fetch the agent weights
				List<Pair<IAgent, Double>> agentWeights = new ArrayList<Pair<IAgent, Double>>();
				for (IAgent agent : agentVotes.keySet()) {
					double weight = AgentManager.getInstance().getAgentWeight(agent);
					agentWeights.add(new Pair<IAgent, Double>(agent, weight));
				}

				// Sort the agent weights descending
				Collections.sort(agentWeights, new Comparator<Pair<IAgent, Double>>() {

					@Override
					public int compare(Pair<IAgent, Double> left, Pair<IAgent, Double> right) {
						return right.first.get-1 * left.second.compareTo(right.second); // -1 for descending
					}
				});*/

		// Load the ui elements
		View bar = infoBox.findViewById(R.id.agentsvote_toast_bar);
		bar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		// Prepare the agent votes list (sort them by their votes; desc)
		List<Map.Entry<IAgent, Float>> agentVotesList = new ArrayList<Map.Entry<IAgent, Float>>(agentVotes.entrySet());
		Collections.sort(agentVotesList, new Comparator<Map.Entry<IAgent, Float>>() {

			@Override
			public int compare(Entry<IAgent, Float> lhs, Entry<IAgent, Float> rhs) {
				float lhsV = Math.abs(lhs.getValue());
				float rhsV = Math.abs(rhs.getValue());
				return Float.compare(rhsV, lhsV);
			}
		});
		assert agentVotesList.size() >= 3;

		// Make the display a little more interresting by add some agent constraints and use the agents with the most extreme votes
		boolean hasPositive = false;
		IAgent[] agents = new IAgent[3];
		IAgent agentWithBestVote = null;
		float bestVote = -2.0f;
		int curAgent = 0;

		for (Map.Entry<IAgent, Float> entry : agentVotesList) {
			final IAgent agent = entry.getKey();

			if (agent instanceof RandomAgent) {
				// Do not show random votes
				continue;
			}

			/*if (agent instanceof AbstractRepetitionAgent) {
				// Do not show anti repetition votes (They are most likely 0 anyway) 
				continue;
			}*/

			if (bestVote < entry.getValue()) {
				bestVote = entry.getValue();
				agentWithBestVote = agent;
			}

			if (curAgent < agents.length) {
				if (entry.getValue() >= 0) {
					hasPositive = true;
				}

				agents[curAgent] = agent;
				++curAgent;
			}
		}

		if (!hasPositive) {
			agents[agents.length - 1] = agentWithBestVote; // if none of the chosen agents voted positive, then add the agent with the best vote
		}

		loadAgent(R.id.agent1, agents[0]);
		loadAgent(R.id.agent2, agents[1]);
		loadAgent(R.id.agent3, agents[2]);
	}

	public void dismiss() {
		// Run doDismiss in the ui-thread
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				doDismiss(infoBox);
			}
		});
	}

	/**
	 * Adds the {@link #infoBox} to the parent. If {@link #shownInfoBox} is not null, we first dismiss that one. After
	 * showning us, we set {@link #shownInfoBox} to {@link #infoBox}.<br/>
	 * Attention: This must be run in the ui thread!
	 */
	private void doShow() {
		synchronized (currentInfoBoxId) {
			if (currentInfoBoxId != infoBoxId) {
				// A newer box is around
				return;
			}

			if (shownInfoBox == infoBox) {
				// We are already shown
				return;
			}

			// Dismiss the shown infoBox (if any)
			doDismiss(shownInfoBox);

			// Add some margins to make its display nicer
			if (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				View tabs = parent.findViewById(R.id.tabsButtons);
				if (tabs != null) {
					View spacer = infoBox.findViewById(R.id.spacer_left);
					spacer.setLayoutParams(new LinearLayout.LayoutParams(tabs.getWidth() + 6,
							LayoutParams.MATCH_PARENT));

					((LinearLayout) infoBox).setGravity(Gravity.TOP);
				}
			} else {
				View playerConsole = parent.findViewById(R.id.playerConsole);
				if (playerConsole != null) {
					View spacer = infoBox.findViewById(R.id.spacer_bottom);
					spacer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
							playerConsole.getHeight() + 1));

					((LinearLayout) infoBox).setGravity(Gravity.BOTTOM);
				}
			}

			// Show us
			parent.addContentView(infoBox, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			shownInfoBox = infoBox;

			// Timer for automatic closing
			Timer t = new Timer();
			t.schedule(new TimerTask() {

				@Override
				public void run() {
					dismiss();
				}
			}, 8000);
		}
	}

	/**
	 * Removes the given infoBox from the parent and sets {@link #shownInfoBox} to null if the given infoBox equals it.<br/>
	 * Attention: This must be run in the ui thread!
	 */
	private void doDismiss(View infoBox) {
		if (infoBox == null) {
			return;
		}

		synchronized (currentInfoBoxId) {
			ViewGroup vg = (ViewGroup) infoBox.getParent();
			if (vg != null) {
				vg.removeView(infoBox);
			}

			if (infoBox == shownInfoBox) {
				shownInfoBox = null;
			}
		}
	}

	/**
	 * Loads the agent into the given ui element.
	 * 
	 * @param agentLineId
	 *            The id of the top ui element
	 * @param layout
	 *            The toast layout
	 * @param agent
	 *            The agent
	 */
	private void loadAgent(int agentLineId, IAgent agent) {
		View top = infoBox.findViewById(agentLineId);

		// Display the agent name
		TextView text = (TextView) top.findViewById(R.id.agent_title);
		text.setText(getAgentTitle(agent));

		// Display the vote
		SeekBar seekBar = (SeekBar) top.findViewById(R.id.agent_seekBar);
		float vote = agentVotes.get(agent);
		setAdjustedSeekPosition(seekBar, vote);
	}

	/**
	 * Moves the given vote from [-1, 1] to [0, {@link SeekBar#getMax()}] <s>and uses more space for votes around 0 than
	 * for bigger ones, since they are more likely</s>. It then assigns the calculated position to the seek bar.
	 * 
	 * @param seekBar
	 * @param vote
	 */
	private void setAdjustedSeekPosition(SeekBar seekBar, float vote) {
		//double importanceAdjusted = Math.signum(vote) * Math.pow(Math.abs(vote), 1 / 2.0d);
		double rangeAdjusted = (vote + 1.0d) / 2; // Move from [-1, 1] to [0, 1]
		seekBar.setProgress((int) (rangeAdjusted * seekBar.getMax()));
	}

	private String getAgentTitle(IAgent agent) {
		String mainTitle = getMainTitle(agent);
		String subTitle = getSubTitle(agent);

		if (!subTitle.isEmpty()) {
			mainTitle += String.format(" (%s)", subTitle);
		}
		return mainTitle;
	}

	private String getMainTitle(IAgent agent) {
		switch (agent.getAgentType()) {
			case Random:
				return parent.getString(R.string.agent_title_random_short);

			case Repetition:
				return parent.getString(R.string.agent_title_repetition_short);

			case Suggested:
				return parent.getString(R.string.agent_title_suggested_short);

			case Top:
				return parent.getString(R.string.agent_title_top_short);

			default:
				assert false;
				return "";
		}
	}

	private String getSubTitle(IAgent agent) {
		switch (agent.getAgentType()) {
			case Random:
				return "";

			case Repetition:
				AbstractRepetitionAgent repAgent = (AbstractRepetitionAgent) agent;
				if (repAgent.getRepetitionType().equals(ArtistRepetitionAgent.REPETITION_TYPE)) {
					return parent.getString(R.string.agent_subtitle_repetition_artist_short);
				} else if (repAgent.getRepetitionType().equals(SongRepetitionAgent.REPETITION_TYPE)) {
					return parent.getString(R.string.agent_subtitle_repetition_song_short);
				} else {
					assert false;
					return "";
				}

			case Suggested:
			case Top:
				AbstractRecentAgent recAgent = (AbstractRecentAgent) agent;
				switch (recAgent.getTimeFilter()) {
					case DAY_OF_THE_WEEK:
						return parent.getString(R.string.agent_subtitle_recent_day_of_the_week);
					case HOUR_OF_THE_DAY:
						return parent.getString(R.string.agent_subtitle_recent_hour_of_the_day);
					case NONE:
						return parent.getString(R.string.agent_subtitle_recent_overall);
					case RECENTLY:
						return parent.getString(R.string.agent_subtitle_recent_recently);

					default:
						assert false;
						return "";
				}

			default:
				assert false;
				return "";
		}
	}
}
