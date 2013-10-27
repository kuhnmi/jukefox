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
package ch.ethz.dcg.jukefox.data.db;

import java.util.Date;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsArtist;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsGenre;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsSong;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;

public interface IDbStatisticsHelper {

	/**
	 * The direction of statistics data.
	 */
	public enum Direction {
		TOP,
		FLOP,
		ALL
	}

	/**
	 * The data range that should be considered.
	 */
	public enum TimeFilter {
		HOUR_OF_THE_DAY,
		DAY_OF_THE_WEEK,
		NONE
	}

	// *** Backup / Restore *** //

	/**
	 * Saves the statistics data into a backup table.
	 */
	public void backupStatisticsData();

	/**
	 * Saves the rating data into a backup table.
	 */
	public void backupRatingData();

	/**
	 * Tries to restore statistics data from a previous backup.
	 */
	public void restoreStatisticsData();

	/**
	 * Tries to restore rating data from a previous backup.
	 */
	public void restoreRatingData();

	// *** Rating *** //

	/**
	 * Writes a rating entry into the db.
	 * 
	 * @see RatingEntry#RatingEntry(int, int, Date, int, double, double, RatingSource)
	 * @return The id of the last inserted row
	 * @throws DataWriteException
	 */
	public long writeRatingEntry(int profileId, int songId, Date timestamp, double rating, double weight,
			RatingSource ratingSource) throws DataWriteException;

	/**
	 * Returns the ratings of the given songs.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param songs
	 *            The songs
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The rated songs
	 */
	public <T extends BaseSong<BaseArtist, BaseAlbum>> List<StatisticsSong<BaseArtist, BaseAlbum>> getSongRatings(
			int profileId, List<T> songs, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			RatingSource[] ratingSources, boolean smoothed);

	/**
	 * Returns the ratings of the given artists.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param artists
	 *            The artists
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The rated artists
	 */
	public <T extends BaseArtist> List<StatisticsArtist> getArtistRatings(int profileId, List<T> artists,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, RatingSource[] ratingSources, boolean smoothed);

	// *** Top *** //

	/**
	 * Gets the top songs by skipping behaviour. If top data is requested, only positive weighted data is returned and
	 * only negative otherwise.<br/>
	 * Only data from before maxTimestamp is used. The rating aging function gets adjusted to represent the rating from
	 * "then".
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param maxNum
	 *            The maximal number of songs which should be returned
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param direction
	 *            If we want to get the top or the flop data
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The top {@link StatisticsSong<BaseArtits, BaseAlbum>}s
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getTopSongs(int profileId, int maxNum,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, Direction direction, boolean smoothed);

	/**
	 * Gets the top albums by skipping behaviour. If top data is requested, only positive weighted data is returned and
	 * only negative otherwise.<br/>
	 * Only data from before maxTimestamp is used. The rating aging function gets adjusted to represent the rating from
	 * "then".
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param maxNum
	 *            The maximal number of albums which should be returned
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param direction
	 *            If we want to get the top or the flop data
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The top {@link StatisticsAlbum}s
	 */
	public List<StatisticsAlbum> getTopAlbums(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed);

	/**
	 * Gets the top artists by skipping behaviour. If top data is requested, only positive weighted data is returned and
	 * only negative otherwise.<br/>
	 * Only data from before maxTimestamp is used. The rating aging function gets adjusted to represent the rating from
	 * "then".
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param maxNum
	 *            The maximal number of artists which should be returned
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param direction
	 *            If we want to get the top or the flop data
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The top {@link StatisticsArtist}s
	 */
	public List<StatisticsArtist> getTopArtists(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed);

	/**
	 * Gets the top genres by skipping behaviour. If top data is requested, only positive weighted data is returned and
	 * only negative otherwise.<br/>
	 * Only data from before maxTimestamp is used. The rating aging function gets adjusted to represent the rating from
	 * "then".
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param maxNum
	 *            The maximal number of genres which should be returned
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param direction
	 *            If we want to get the top or the flop data
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The top {@link StatisticsGenre}s
	 */
	public List<StatisticsGenre> getTopGenres(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed);

	// *** Suggested *** //

	/**
	 * Returns the ratings of the given suggested songs.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param songs
	 *            The songs which should be rated
	 * @param cutBetweenOnceAndLately
	 *            Before this time we talk from once and after it about lately
	 * @param maxLatelyListeningTime
	 *            The threshold for low listening time [in ms]. If negative, we do not filter by the listening time but
	 *            are using its absolute value to prevent returning long played recently imported items.
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The rated songs
	 */
	public <T extends BaseSong<BaseArtist, BaseAlbum>> List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongRatings(
			int profileId, List<T> songs, Date cutBetweenOnceAndLately, long maxLatelyListeningTime,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, boolean smoothed);

	/**
	 * Returns songs which the user could find interresting. Is it that their rating was high once but have low
	 * listening time lately or their neighbors are rated good. <br/>
	 * The songs are ordered by their rating.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param cutBetweenOnceAndLately
	 *            Before this time we talk from once and after it about lately
	 * @param maxLatelyListeningTime
	 *            The threshold for max listening time of a song [in ms]. If negative, we do not filter by the listening
	 *            time but are using its absolute value to prevent returning too much played recently imported items.
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param maxNum
	 *            The maximal number of songs which should be returned
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The suggested {@link StatisticsSong}s
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongs(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed);

	/**
	 * Returns albums which the user could find interresting. Is it that their rating was high once but have low
	 * listening time lately or their neighbors are rated good. <br/>
	 * The songs are ordered by their rating.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param cutBetweenOnceAndLately
	 *            Before this time we talk from once and after it about lately
	 * @param maxLatelyListeningTime
	 *            The threshold for max listening time of an album [in ms]. If negative, we do not filter by the
	 *            listening time but are using its absolute value to prevent returning too much played recently imported
	 *            items.
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param maxNum
	 *            The maximal number of albums which should be returned
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The suggested {@link StatisticsAlbum}s
	 */
	public List<StatisticsAlbum> getSuggestedAlbums(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed);

	/**
	 * Returns artists which the user could find interresting. Is it that their rating was high once but have low
	 * listening time lately or their neighbors are rated good. <br/>
	 * The songs are ordered by their rating.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param cutBetweenOnceAndLately
	 *            Before this time we talk from once and after it about lately
	 * @param maxLatelyListeningTime
	 *            The threshold for max listening time of an artist [in ms]. If negative, we do not filter by the
	 *            listening time but are using its absolute value to prevent returning too much played recently imported
	 *            items.
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param maxNum
	 *            The maximal number of artists which should be returned
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The suggested {@link StatisticsArtist}s
	 */
	public List<StatisticsArtist> getSuggestedArtists(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed);

	/**
	 * Returns genres which the user could find interresting. Is it that their rating was high once but have low
	 * listening time lately or their neighbors are rated good. <br/>
	 * The songs are ordered by their rating.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param cutBetweenOnceAndLately
	 *            Before this time we talk from once and after it about lately
	 * @param maxLatelyListeningTime
	 *            The threshold for max listening time of a genre [in ms]. If negative, we do not filter by the
	 *            listening time but are using its absolute value to prevent returning too much played recently imported
	 *            items.
	 * @param timeRange
	 *            The time range in which the considered ratings must lie
	 * @param timeFilter
	 *            An additional filter for the ratings by weekday, hour of the day, ...
	 * @param maxNum
	 *            The maximal number of genres which should be returned
	 * @param smoothed
	 *            If the ratings should be smoothed with an initial rating or not
	 * @return The suggested {@link StatisticsGenre}s
	 */
	public List<StatisticsGenre> getSuggestedGenres(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed);

	// *** Imports *** //

	/**
	 * Returns songs which were imported in the given time range.
	 * 
	 * @param timeRange
	 *            The time range in which the import timestamp has to lie
	 * @param maxNum
	 *            The maximal number of songs which should be returned
	 * @return The {@link StatisticsSong}s
	 */
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getImportedSongs(Pair<Date, Date> timeRange, int maxNum);

	/**
	 * Returns albums which contain at least one song which was imported in the given time range.
	 * 
	 * @param timeRange
	 *            The time range in which the import timestamp has to lie
	 * @param maxNum
	 *            The maximal number of albums which should be returned
	 * @return The {@link StatisticsAlbum}s
	 */
	public List<StatisticsAlbum> getImportedAlbums(Pair<Date, Date> timeRange, int maxNum);

	/**
	 * Returns artists which contain at least one song which was imported in the given time range.
	 * 
	 * @param timeRange
	 *            The time range in which the import timestamp has to lie
	 * @param maxNum
	 *            The maximal number of artists which should be returned
	 * @return The {@link StatisticsArtist}s
	 */
	public List<StatisticsArtist> getImportedArtists(Pair<Date, Date> timeRange, int maxNum);

	/**
	 * Returns genres which contain at least one song which was imported in the given time range.
	 * 
	 * @param timeRange
	 *            The time range in which the import timestamp has to lie
	 * @param maxNum
	 *            The maximal number of genres which should be returned
	 * @return The {@link StatisticsGenre}s
	 */
	public List<StatisticsGenre> getImportedGenres(Pair<Date, Date> timeRange, int maxNum);

	// *** Other *** //

	/**
	 * Returns the most recent timestamp this song was played.
	 * 
	 * @param song
	 *            The song
	 * @return The timestamp
	 * @throws DataUnavailableException
	 *             If this song has not been played yet
	 */
	public Date getLastPlayedTime(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException;

	/**
	 * Returns the most recent timestamp a song of this artist was played.
	 * 
	 * @param artist
	 *            The artist
	 * @return The timestamp
	 * @throws DataUnavailableException
	 *             If this artist has no song that has been played yet
	 */
	public Date getLastPlayedTime(BaseArtist artist) throws DataUnavailableException;

	/**
	 * Returns {num} random songs which have not been rated for at least {longNotRatedThreshold} hours or were never
	 * rated at all.
	 * 
	 * @param num
	 *            The number of songs
	 * @param longNotRatedThreshold
	 *            When a song is considered as long not rated. [h]
	 * @return The song list
	 * @throws DataUnavailableException
	 *             If the data could not be fetched
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getLongNotRatedSongs(int num, int longNotRatedThreshold)
			throws DataUnavailableException;
}
