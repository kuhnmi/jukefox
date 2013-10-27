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
package ch.ethz.dcg.pancho3.view.statistics.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.RatingBar;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.pancho3.R;

/**
 * Shows a list of the data. The number of rating stars is calculated as a linear distribution over [min, max]. If we
 * are looking at top data, then the slope of the distribution is taken from the range [mean, max] with f(max) =
 * MAX_STARS and f(mean) = MEAN_STARS. If we are looking at flop data ({@link StatisticsRatingListAdapter#inverted} ==
 * true), then the slope of the distribution is taken from the range [min, mean] with f(min) = MIN_STARS and f(mean) =
 * MEAN_STARS. The max and min values can take arbitrary values but are forced to be at least +-2 to get a more accurate
 * display when only little data is available.<br/>
 * <br/>
 * The values of the data items have to be an instance of {@link Float}.
 */
public class StatisticsRatingListAdapter<T extends IStatisticsData> extends StatisticsBaseListAdapter<T> {

	private float minRating;
	private float maxRating;
	private final float meanRating = 0f; // Force the mean to be zero.
	private boolean inverted;

	/**
	 * List adapter for statistics data. Assumes, that data is sorted.
	 * 
	 * @param data
	 *            The to be displayed data
	 */
	public StatisticsRatingListAdapter(Context context, List<T> data) {
		super(context, R.layout.statistics_ratinglistitem, data);

		minRating = 0f;
		maxRating = 0f;
		inverted = false;
		if (data.size() > 0) {
			minRating = (Float) data.get(data.size() - 1).getValue();
			maxRating = (Float) data.get(0).getValue();

			if (minRating > maxRating) {
				inverted = true;
				float tmp = minRating;
				minRating = maxRating;
				maxRating = tmp;
			}

			maxRating = Math.max(1, maxRating);
			minRating = Math.min(-1, minRating);
		}
	}

	@Override
	protected void fillSpecialFields(View v, T item) {
		// Set a rating of 0 to fill numStars/2. The rest is linear.
		RatingBar rating = (RatingBar) v.findViewById(R.id.rating);
		final float MAX_STARS = rating.getNumStars();
		final float MIN_STARS = 0;
		final float meanStars = (MAX_STARS + MIN_STARS) / 2.0f;

		float itemRating = (Float) item.getValue();
		float numStars;
		if (!inverted) {
			// Top data
			numStars = (MAX_STARS - meanStars) / (maxRating - meanRating) * (itemRating - maxRating) + MAX_STARS;
		} else {
			// Flop data
			numStars = (MIN_STARS - meanStars) / (minRating - meanRating) * (itemRating - minRating) + MIN_STARS;
		}
		numStars = Math.min(Math.max(numStars, MIN_STARS), MAX_STARS); // Cut off at the boundaries
		rating.setRating(numStars);
	}

}