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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsSong;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SongVote;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;

/**
 * This agent votes for songs which are in the suggested list.
 * 
 * @see StatisticsProvider#getSuggestedSongs(java.util.Date, int)
 * @see StatisticsProvider#getLongNotRatedSongs(int, int)
 */
public class SuggestedAgent extends AbstractRecentAgent {

	/**
	 * The range of how much the cutBetweenOnceAndLately is allowed to be past when choosing one at random. [h]
	 */
	private static final Pair<Integer, Integer> CUT_BETWEEN_ONCE_AND_LATELY_RANDOM_RANGE = new Pair<Integer, Integer>(
			1 * 30 * 24, 6 * 30 * 24); // 1month, 6months

	private final StatisticsProvider statisticsProvider;

	private List<StatisticsSong<BaseArtist, BaseAlbum>> suggestedSongs;
	private Date currentCutBetweenOnceAndLately;

	public SuggestedAgent(final StatisticsProvider statisticsProvider, TimeFilter timeFilter) {
		super(timeFilter);

		this.statisticsProvider = statisticsProvider;
	}

	/**
	 * Suggests "suggested"-songs.
	 * 
	 * @see StatisticsProvider#getSuggestedSongs(Date, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      int, boolean)
	 */
	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> suggestSongs(int num) {
		if (getTimeFilter() == TimeFilter.RECENTLY) {
			Pair<Date, Date> timeRange = getTimeRange();
			currentCutBetweenOnceAndLately = timeRange.first; /*Nothing before recently*/

			suggestedSongs = statisticsProvider.getSuggestedSongs(
					currentCutBetweenOnceAndLately, StatisticsProvider.DEFAULT_MAX_LATELY_LISTENING_TIME, timeRange,
					getTimeFilter().toDbTimeFilter(), num, false);
		} else {
			// We choose the cutBetweenOnceAndLately at random to get different ancient top data in each round
			int hoursBack = RandomProvider.getRandom().nextInt(
					CUT_BETWEEN_ONCE_AND_LATELY_RANDOM_RANGE.second - CUT_BETWEEN_ONCE_AND_LATELY_RANDOM_RANGE.first) + CUT_BETWEEN_ONCE_AND_LATELY_RANDOM_RANGE.first;
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, -hoursBack);
			currentCutBetweenOnceAndLately = cal.getTime();

			suggestedSongs = statisticsProvider.getSuggestedSongs(currentCutBetweenOnceAndLately,
					StatisticsProvider.DEFAULT_MAX_LATELY_LISTENING_TIME, getTimeRange(),
					getTimeFilter().toDbTimeFilter(), num, false);
		}

		return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(suggestedSongs);
	}

	/**
	 * Votes according to the neighborhood rating. No playlog ratings are taken - only inherited ones.
	 */
	@Override
	public List<SongVote> vote(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		List<BaseSong<BaseArtist, BaseAlbum>> toBeVoted = new LinkedList<BaseSong<BaseArtist, BaseAlbum>>(songs);

		// We already have the rating of our songs
		List<StatisticsSong<BaseArtist, BaseAlbum>> songRatings = new ArrayList<StatisticsSong<BaseArtist, BaseAlbum>>(
				suggestedSongs);
		toBeVoted.removeAll(suggestedSongs);

		// Find the rating of the other songs
		List<StatisticsSong<BaseArtist, BaseAlbum>> songRatingsTmp;
		if (getTimeFilter() == TimeFilter.RECENTLY) {
			Pair<Date, Date> timeRange = getTimeRange();

			songRatingsTmp = statisticsProvider.getSuggestedSongRatings(toBeVoted, currentCutBetweenOnceAndLately,
					StatisticsProvider.DEFAULT_MAX_LATELY_LISTENING_TIME, // Exclude them as well, do not vote for recently listened songs! 
					timeRange, getTimeFilter().toDbTimeFilter(), false);
		} else {
			songRatingsTmp = statisticsProvider.getSuggestedSongRatings(toBeVoted, currentCutBetweenOnceAndLately,
					StatisticsProvider.DEFAULT_MAX_LATELY_LISTENING_TIME, // Exclude them as well, do not vote for recently listened songs!
					getTimeRange(), getTimeFilter().toDbTimeFilter(), false);
		}
		songRatings.addAll(songRatingsTmp);

		// Transform the StatisticsSongs into SongVotes
		List<SongVote> votes = new ArrayList<SongVote>(songRatings.size());
		for (StatisticsSong<BaseArtist, BaseAlbum> song : songRatings) {
			votes.add(new SongVote(song, (Float) song.getValue()));
		}

		return votes;
	}

	@Override
	public AgentType getAgentType() {
		return AgentType.Suggested;
	}
}
