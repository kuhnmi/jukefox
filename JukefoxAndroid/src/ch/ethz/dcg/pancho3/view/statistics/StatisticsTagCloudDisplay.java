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
package ch.ethz.dcg.pancho3.view.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.pancho3.tablet.widget.FlowLayout;
import ch.ethz.dcg.pancho3.tablet.widget.SmallestAreaTextView;

public class StatisticsTagCloudDisplay implements IStatisticsDisplay {

	private StatisticsActivity statisticsActivity;

	public StatisticsTagCloudDisplay(StatisticsActivity statisticsActivity) {
		super();

		this.statisticsActivity = statisticsActivity;
	}

	/**
	 * Shows a tag cloud of the data. The font size is calculated as a linear
	 * distribution over all values. If we are looking at top data, then the
	 * slope of the distribution is taken from the range [mean, max] with f(max)
	 * = MAX_FONTSIZE, and f(mean) = MEAN_FONTSIZE. If we are looking at flop
	 * data ({@link StatisticsRatingListAdapter#inverted} == true), then the
	 * slope of the distribution is taken from the range [min, mean] with f(min)
	 * = MAX_FONTSIZE and f(mean) = MEAN_FONTSIZE. The max and min values can
	 * take arbitrary values but are forced to be at least +-2 to get a more
	 * accurate display when only little data is available. Note, that if data
	 * is sorted in descending direction, the biggest fontsize will be assgned
	 * to the item with the lowest value.<br/>
	 * <br/>
	 * The datas values have to be an instance of {@link Float}.
	 */
	@Override
	public <T extends IStatisticsData> void inflate(List<T> data, final OnClickListener onClickListener,
			final OnLongClickListener onLongClickListener, int parentId) {

		// Set the click listeners
		View.OnClickListener viewOnClickListener = null;
		if (onClickListener != null) {
			viewOnClickListener = new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					IStatisticsData item = (IStatisticsData) ((TextView) v).getTag();
					onClickListener.onClick(item);
				}
			};
		}
		View.OnLongClickListener viewOnLongClickListener = null;
		if (onLongClickListener != null) {
			viewOnLongClickListener = new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					IStatisticsData item = (IStatisticsData) ((TextView) v).getTag();
					return onLongClickListener.onLongClick(item);
				}
			};
		}

		// Find the data range
		float minRating = 0f;
		float maxRating = 0f;
		float meanRating = 0f;
		boolean inverted = false;
		if (data.size() > 0) {
			minRating = (Float) data.get(data.size() - 1).getValue();
			maxRating = (Float) data.get(0).getValue();

			for (IStatisticsData item : data) {
				meanRating += (Float) item.getValue();
			}
			meanRating /= data.size();

			if (minRating > maxRating) {
				inverted = true;
				float tmp = minRating;
				minRating = maxRating;
				maxRating = tmp;
			}

			if (maxRating > 0) {
				maxRating = Math.max(2, maxRating);
			}
			if (minRating < 0) {
				minRating = Math.min(-2, minRating);
			}
		}

		// get a shuffled access strategy to get a more interresting tag cloud
		List<Integer> accessStrategy = getShuffledAccessStrategy(data.size());

		final byte MIN_FONTSIZE = 8;
		final byte MAX_FONTSIZE = 40;
		byte meanFontSize = (MAX_FONTSIZE + MIN_FONTSIZE) / 2;

		LinearLayout l = (LinearLayout) statisticsActivity.findViewById(parentId);
		l.removeAllViews(); // Remove old elements

		// Create a scroll container
		ScrollView sv = new ScrollView(statisticsActivity);
		// TODO: Sämy, add 20px margin at the bottom.
		sv.setPadding(0, 0, 0, 80); // To be able to scroll all data above the settings button.
		sv.setFadingEdgeLength(0);

		// Create the table layout
		FlowLayout fl = new FlowLayout(statisticsActivity);

		// Create the tag cloud
		for (int index : accessStrategy) {
			IStatisticsData item = data.get(index);

			// Calculate the weight for this item
			float fontSize;
			float itemRating = (Float) item.getValue();
			if (!inverted) {
				// Top data
				fontSize = (MAX_FONTSIZE - meanFontSize) / (maxRating - meanRating) * (itemRating - maxRating) + MAX_FONTSIZE;
			} else {
				// Flop data
				fontSize = (MAX_FONTSIZE - meanFontSize) / (minRating - meanRating) * (itemRating - minRating) + MAX_FONTSIZE;
			}
			fontSize = Math.min(Math.max(fontSize, MIN_FONTSIZE), MAX_FONTSIZE); // Cut off at the boundaries

			// Create the text view
			TextView txt = createText(fontSize, item, viewOnClickListener, viewOnLongClickListener);
			fl.addView(txt);
		}

		// Add the flow layout to the scroll container
		sv.addView(fl);

		// Add the scroll container to the layout
		l.addView(sv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	/**
	 * Returns a (fixed) shuffled access strategy for the given list size.
	 * 
	 * @param n
	 *            The list size
	 * @return A list of indices in [0, n) which all appear only once.
	 */
	private List<Integer> getShuffledAccessStrategy(int n) {
		List<Integer> ret = new ArrayList<Integer>(n);
		for (int i = 0; i < n; ++i) {
			ret.add(i);
		}

		Random rnd = new Random(2340983240982340932l); // Chosen u.a.r. ;)
		for (int i = n - 1; i >= 0; --i) {
			int index = rnd.nextInt(i + 1);
			Collections.swap(ret, index, i);
		}

		return ret;
	}

	/**
	 * Creates a new {@link TextView} with the given {@code fontSize} and the
	 * title of the {@code item} as the text.
	 * 
	 * @param fontSize
	 * @param item
	 * @return The {@link TextView}
	 */
	private TextView createText(float fontSize, final IStatisticsData item, View.OnClickListener onClickListener,
			View.OnLongClickListener onLongClickListener) {
		TextView txt = new SmallestAreaTextView(statisticsActivity);
		txt.setLayoutParams(new FlowLayout.LayoutParams(true, 5, 3));
		txt.setGravity(Gravity.CENTER);
		txt.setTextSize(fontSize);
		txt.setText(item.getTitle());
		txt.setTag(item);
		if (onClickListener != null) {
			txt.setOnClickListener(onClickListener);
		}
		if (onLongClickListener != null) {
			txt.setOnLongClickListener(onLongClickListener);
		}

		return txt;
	}
}
