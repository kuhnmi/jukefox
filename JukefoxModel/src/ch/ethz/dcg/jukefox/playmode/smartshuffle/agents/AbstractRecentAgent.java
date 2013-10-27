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

import java.util.Calendar;
import java.util.Date;

import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper;

/**
 * The abstract class for all agents which operate on "recent" data, so that the definition of "recent" is only done at
 * some place.
 */
public abstract class AbstractRecentAgent extends AbstractAgent {

	/**
	 * How much back we consider the statistics as "current mood". [min]
	 */
	private final static int CURRENT_MOOD_OFFSET = 60; // 1h

	/**
	 * @see IDbStatisticsHelper.TimeFilter
	 */
	public enum TimeFilter {
		// IDbStatisticsHelper.TimeRange
		HOUR_OF_THE_DAY,
		DAY_OF_THE_WEEK,
		NONE,

		RECENTLY;

		/**
		 * Returns the IDbStatisticsHelper.TimeRange which matches to this extended version.
		 * 
		 * @return The matched enum
		 */
		public IDbStatisticsHelper.TimeFilter toDbTimeFilter() {
			switch (this) {
				case HOUR_OF_THE_DAY:
					return IDbStatisticsHelper.TimeFilter.HOUR_OF_THE_DAY;

				case DAY_OF_THE_WEEK:
					return IDbStatisticsHelper.TimeFilter.DAY_OF_THE_WEEK;

				case RECENTLY:
				case NONE:
					return IDbStatisticsHelper.TimeFilter.NONE;

				default:
					assert false;
					return null;
			}

		}
	}

	private final TimeFilter timeFilter;

	public AbstractRecentAgent(TimeFilter timeFilter) {
		super();

		this.timeFilter = timeFilter;
	}

	/**
	 * Returns the time range
	 * 
	 * @return
	 */
	public TimeFilter getTimeFilter() {
		return timeFilter;
	}

	/**
	 * Returns the time range the ratings are allowed to have.
	 * 
	 * @return The time range
	 */
	protected Pair<Date, Date> getTimeRange() {
		if (getTimeFilter() == TimeFilter.RECENTLY) {
			Calendar start = Calendar.getInstance();
			start.add(Calendar.MINUTE, -CURRENT_MOOD_OFFSET); // remove some minutes
			return new Pair<Date, Date>(start.getTime(), new Date());
		} else {
			return new Pair<Date, Date>(new Date(0), new Date()); // all the time
		}
	}

	@Override
	public final String getIdentifier() {
		String prefix = super.getIdentifier();
		switch (getTimeFilter()) {
			case DAY_OF_THE_WEEK:
				return prefix + "_dotw";
			case HOUR_OF_THE_DAY:
				return prefix + "_hotd";
			case NONE:
				return prefix + "_overall";
			case RECENTLY:
				return prefix + "_recently";
			default:
				assert false;
				return "";
		}
	}
}
