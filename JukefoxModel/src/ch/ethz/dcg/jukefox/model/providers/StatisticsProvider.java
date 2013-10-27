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
package ch.ethz.dcg.jukefox.model.providers;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsArtist;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsGenre;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsSong;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;

/**
 * Provides statistics data
 */
public class StatisticsProvider {

	private final IDbDataPortal dbDataPortal;
	private final int profileId;

	private final static float MAX_LISTENED_PERCENTAGE = 0.3f; // 30%;

	private final static int AVERAGE_SONG_DURATION = 253353; // 4m 13s 353ms
	private final static int AVERAGE_ALBUM_DURATION = 1533544; // 25m 33s 544ms
	private final static int AVERAGE_ARTIST_DURATION = 1869546; // 31m 09s 546ms
	private final static int AVERAGE_GENRE_DURATION = 8532800; // 2h 22m 12s 800ms

	/**
	 * Creates a new instance of {@link StatisticsProvider}
	 * 
	 * @param profileId
	 *            The profile on which we should work
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public StatisticsProvider(int profileId, IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
		this.profileId = profileId;
	}

	/**
	 * Returns the timerange from the beginning of time until now.
	 * 
	 * @return The all-the-time timerange
	 */
	public Pair<Date, Date> allTheTime() {
		return new Pair<Date, Date>(new Date(0), new Date());
	}

	/**
	 * @see IDbStatisticsHelper#getSongRatings(int, List, Pair, TimeFilter, RatingSource[], boolean)
	 */
	public <T extends BaseSong<BaseArtist, BaseAlbum>> List<StatisticsSong<BaseArtist, BaseAlbum>> getSongRatings(
			List<T> songs, Pair<Date, Date> timeRange, TimeFilter timeFilter, RatingSource[] ratingSources,
			boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getSongRatings(profileId, songs, timeRange, timeFilter,
				ratingSources, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getArtistRatings(int, List, Pair, TimeFilter, RatingSource[], boolean)
	 */
	public <T extends BaseArtist> List<StatisticsArtist> getArtistRatings(List<T> artists, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, RatingSource[] ratingSources, boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getArtistRatings(profileId, artists, timeRange, timeFilter,
				ratingSources, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getLastPlayedTime(BaseSong)
	 */
	public Date getLastPlayedTime(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException {
		return dbDataPortal.getStatisticsHelper().getLastPlayedTime(song);
	}

	/**
	 * @see IDbStatisticsHelper#getLastPlayedTime(BaseArtist)
	 */
	public Date getLastPlayedTime(BaseArtist artist) throws DataUnavailableException {
		return dbDataPortal.getStatisticsHelper().getLastPlayedTime(artist);
	}

	/**
	 * @see IDbStatisticsHelper#getTopSongs(int, int, Pair, TimeFilter, Direction, boolean)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getTopSongs(int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getTopSongs(profileId, maxNum, timeRange, timeFilter, direction,
				smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getTopAlbums(int, int, Pair, TimeFilter, Direction, boolean)
	 */
	public List<StatisticsAlbum> getTopAlbums(int maxNum, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			Direction direction, boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getTopAlbums(profileId, maxNum, timeRange, timeFilter, direction,
				smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getTopArtists(int, int, Pair, TimeFilter, Direction, boolean)
	 */
	public List<StatisticsArtist> getTopArtists(int maxNum, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			Direction direction, boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getTopArtists(profileId, maxNum, timeRange, timeFilter, direction,
				smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getTopGenres(int, int, Pair, TimeFilter, Direction, boolean)
	 */
	public List<StatisticsGenre> getTopGenres(int maxNum, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			Direction direction, boolean smoothed) {
		return dbDataPortal.getStatisticsHelper().getTopGenres(profileId, maxNum, timeRange, timeFilter, direction,
				smoothed);
	}

	/**
	 * Returns the utc timestamp from now minus one month.
	 * 
	 * @return The timestamp in ms
	 */
	private Date getTimestampOfBeforeOneMonth() {
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MONTH, -1);
		return now.getTime();
	}

	public static final Date DEFAULT_CUT_BETWEEN_ONCE_AND_LATELY = null;
	public static final long DEFAULT_MAX_LATELY_LISTENING_TIME = Long.MAX_VALUE;
	public static final long NO_MAX_LATELY_LISTENING_TIME_FILTER = Long.MIN_VALUE;

	/**
	 * @see IDbStatisticsHelper#getSuggestedSongRatings(int, List, Date, long, Pair, TimeFilter, boolean)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongRatings(
			List<BaseSong<BaseArtist, BaseAlbum>> songs, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			boolean smoothed) {

		return getSuggestedSongRatings(songs, DEFAULT_CUT_BETWEEN_ONCE_AND_LATELY, DEFAULT_MAX_LATELY_LISTENING_TIME,
				timeRange, timeFilter, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedSongRatings(int, List, Date, long, Pair, TimeFilter, boolean)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongRatings(
			List<BaseSong<BaseArtist, BaseAlbum>> songs, Date cutBetweenOnceAndLately, long maxLatelyListeningTime,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, boolean smoothed) {

		if ((maxLatelyListeningTime == DEFAULT_MAX_LATELY_LISTENING_TIME) || (maxLatelyListeningTime == NO_MAX_LATELY_LISTENING_TIME_FILTER)) {
			maxLatelyListeningTime = (long) (MAX_LISTENED_PERCENTAGE * AVERAGE_SONG_DURATION)
					* ((maxLatelyListeningTime < 0) ? -1 : 1); // see the documentation, for why we do this
		}
		if (Utils.nullEquals(cutBetweenOnceAndLately, DEFAULT_CUT_BETWEEN_ONCE_AND_LATELY)) {
			cutBetweenOnceAndLately = getTimestampOfBeforeOneMonth();
		}

		return dbDataPortal.getStatisticsHelper().getSuggestedSongRatings(profileId, songs, cutBetweenOnceAndLately,
				maxLatelyListeningTime, timeRange, timeFilter, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedSongs(int, Date, long, Pair, TimeFilter, int, boolean)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongs(Pair<Date, Date> timeRange,
			TimeFilter timeFilter, int maxNum, boolean smoothed) {
		return getSuggestedSongs(DEFAULT_CUT_BETWEEN_ONCE_AND_LATELY, DEFAULT_MAX_LATELY_LISTENING_TIME, timeRange,
				timeFilter, maxNum, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedSongs(int, Date, long, Pair, TimeFilter, int, boolean)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongs(Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum, boolean smoothed) {

		if ((maxLatelyListeningTime == DEFAULT_MAX_LATELY_LISTENING_TIME) || (maxLatelyListeningTime == NO_MAX_LATELY_LISTENING_TIME_FILTER)) {
			maxLatelyListeningTime = (long) (MAX_LISTENED_PERCENTAGE * AVERAGE_SONG_DURATION)
					* ((maxLatelyListeningTime < 0) ? -1 : 1); // see the documentation, for why we do this
		}
		if (Utils.nullEquals(cutBetweenOnceAndLately, DEFAULT_CUT_BETWEEN_ONCE_AND_LATELY)) {
			cutBetweenOnceAndLately = getTimestampOfBeforeOneMonth();
		}

		return dbDataPortal.getStatisticsHelper().getSuggestedSongs(profileId, cutBetweenOnceAndLately,
				maxLatelyListeningTime, timeRange, timeFilter, maxNum, smoothed);

	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedAlbums(int, Date, long, Pair, TimeFilter, int, boolean)
	 */
	public List<StatisticsAlbum> getSuggestedAlbums(Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {
		Date cutBetweenOnceAndLately = getTimestampOfBeforeOneMonth();
		long maxLatelyListeningTime = (long) (MAX_LISTENED_PERCENTAGE * AVERAGE_ALBUM_DURATION);

		return dbDataPortal.getStatisticsHelper().getSuggestedAlbums(profileId, cutBetweenOnceAndLately,
				maxLatelyListeningTime, timeRange, timeFilter, maxNum, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedArtists(int, Date, long, Pair, TimeFilter, int, boolean)
	 */
	public List<StatisticsArtist> getSuggestedArtists(Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {
		Date cutBetweenOnceAndLately = getTimestampOfBeforeOneMonth();
		long maxLatelyListeningTime = (long) (MAX_LISTENED_PERCENTAGE * AVERAGE_ARTIST_DURATION);

		return dbDataPortal.getStatisticsHelper().getSuggestedArtists(profileId, cutBetweenOnceAndLately,
				maxLatelyListeningTime, timeRange, timeFilter, maxNum, smoothed);
	}

	/**
	 * @see IDbStatisticsHelper#getSuggestedGenres(int, Date, long, Pair, TimeFilter, int, boolean)
	 */
	public List<StatisticsGenre> getSuggestedGenres(Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {
		Date cutBetweenOnceAndLately = getTimestampOfBeforeOneMonth();
		long maxLatelyListeningTime = (long) (MAX_LISTENED_PERCENTAGE * AVERAGE_GENRE_DURATION);

		return dbDataPortal.getStatisticsHelper().getSuggestedGenres(profileId, cutBetweenOnceAndLately,
				maxLatelyListeningTime, timeRange, timeFilter, maxNum, smoothed);
	}

	/**
	 * Returns the utc timestamp from now minus two weeks.
	 * 
	 * @return The timestamp in ms
	 */
	private Date getTimestampOfBeforeTwoWeeks() {
		Calendar now = Calendar.getInstance();
		now.add(Calendar.WEEK_OF_YEAR, -2);
		return now.getTime();
	}

	/**
	 * @see IDbStatisticsHelper#getImportedSongs(Pair, int)
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getRecentlyImportedSongs(int maxNum) {
		Pair<Date, Date> importRange = new Pair<Date, Date>(getTimestampOfBeforeTwoWeeks(), new Date());

		return dbDataPortal.getStatisticsHelper().getImportedSongs(importRange, maxNum);
	}

	/**
	 * @see IDbStatisticsHelper#getImportedAlbums(Pair, int)
	 */
	public List<StatisticsAlbum> getRecentlyImportedAlbums(int maxNum) {
		Pair<Date, Date> importRange = new Pair<Date, Date>(getTimestampOfBeforeTwoWeeks(), new Date());

		return dbDataPortal.getStatisticsHelper().getImportedAlbums(importRange, maxNum);
	}

	/**
	 * @see IDbStatisticsHelper#getImportedArtists(Pair, int)
	 */
	public List<StatisticsArtist> getRecentlyImportedArtists(int maxNum) {
		Pair<Date, Date> importRange = new Pair<Date, Date>(getTimestampOfBeforeTwoWeeks(), new Date());

		return dbDataPortal.getStatisticsHelper().getImportedArtists(importRange, maxNum);
	}

	/**
	 * @see IDbStatisticsHelper#getImportedGenres(Pair, int)
	 */
	public List<StatisticsGenre> getRecentlyImportedGenres(int maxNum) {
		Pair<Date, Date> importRange = new Pair<Date, Date>(getTimestampOfBeforeTwoWeeks(), new Date());

		return dbDataPortal.getStatisticsHelper().getImportedGenres(importRange, maxNum);
	}

	/**
	 * @see IDbStatisticsHelper#getLongNotRatedSongs(int, int)
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getLongNotRatedSongs(int num, int longNotRatedThreshold)
			throws DataUnavailableException {
		return dbDataPortal.getStatisticsHelper().getLongNotRatedSongs(num, longNotRatedThreshold);
	}

}