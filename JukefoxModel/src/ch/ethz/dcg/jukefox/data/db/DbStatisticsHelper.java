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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.StopWatch;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsArtist;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsGenre;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsSong;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;

public class DbStatisticsHelper<ContentValues extends IContentValues> implements IDbStatisticsHelper {

	private final static String TAG = DbStatisticsHelper.class.getSimpleName();

	/**
	 * What the rating of a song, ... should be, when no or only few data is available. This helps stabilizing the top
	 * list at the start of the app usage. Must be &isin& [-1, 1].
	 */
	private final static double INITIAL_RATING = 0.0d;

	/**
	 * How much the initial rating should weight. The higher, the longer it takes to change it by listening behaviour.
	 */
	private final static double INITIAL_RATING_WEIGHT = 2.0d;

	/**
	 * How much the rating should contribute to the final rating.
	 */
	private final static double WEIGHT_RATING = 0.66d;
	/**
	 * How much the listening time should contribute to the final rating.
	 */
	private final static double WEIGHT_LISTENING_TIME = 0.33d;

	/**
	 * How much an effective rating entry should count.
	 */
	private final static double WEIGHT_PLAYLOG_RATING = 1.0d;
	/**
	 * How much a neighborhood rating entry should count.
	 */
	private final static double WEIGHT_NEIGHBORHOOD_RATING = 0.8d;

	/**
	 * When the aging should start [h].
	 */
	protected final static int RATING_AGING_BEGIN = -30 * 24; // 1 month
	/**
	 * When a rating is at its oldest point [h].
	 */
	protected final static int RATING_AGING_END = -200 * 24; // 200 days
	/**
	 * How much the rating weight should be at the begin. &isin; [{@value #RATING_AGING_END_VALUE}, 1]
	 */
	protected final static double RATING_AGING_BEGIN_VALUE = 1.0d;
	/**
	 * How much the rating weight should be at the end. &isin; [0, {@value #RATING_AGING_BEGIN_VALUE}]
	 */
	protected final static double RATING_AGING_END_VALUE = 0.25d;

	/**
	 * The maximal rating an import gets in suggested songs. &isin; [-1, 1]
	 */
	protected final static double RATING_RECENTLY_IMPORTED_MAX_RATING = 1.0d;
	/**
	 * The maximal distance an import can have from now until its rating drops to 0 [in ms].
	 */
	protected final static int RATING_RECENTLY_IMPORTED_MAX_AGE = 3 * 7 * 24 * 60 * 60 * 1000; // 3 weeks

	protected final SqlDbDataPortal<ContentValues> sqlDbDataPortal;

	private enum DataType {
		Songs, Albums, Artists, Genres;
	};

	public DbStatisticsHelper(SqlDbDataPortal<ContentValues> sqlDbDataPortal) {
		this.sqlDbDataPortal = sqlDbDataPortal;
	}

	/******** Backup / Restore *********/

	@Override
	public void backupStatisticsData() {
		sqlDbDataPortal.beginTransaction();
		try {
			// Remove old temp table 
			sqlDbDataPortal.execSQL("DROP TABLE IF EXISTS backup_" + TblPlayLog.TBL_NAME + "_tmp");

			// Create new temp table from the playlog data
			sqlDbDataPortal
					.execSQL("CREATE TABLE IF NOT EXISTS backup_" + TblPlayLog.TBL_NAME + "_tmp AS " +
							"SELECT pl.*, s." + TblSongs.DATA + ", s." + TblSongs.NAME + " AS songName, a." + TblArtists.NAME + " AS artistName " +
							"FROM " + TblPlayLog.TBL_NAME + " AS pl " +
							"  INNER JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ") " +
							"  INNER JOIN " + TblArtists.TBL_NAME + " AS A ON (a." + TblArtists.ARTIST_ID + " = s." + TblSongs.ARTIST_ID + ") ");

			try {
				// Create backup table if not exists
				sqlDbDataPortal
						.execSQL("CREATE TABLE backup_" + TblPlayLog.TBL_NAME + " AS SELECT * FROM backup_" + TblPlayLog.TBL_NAME + "_tmp");
			} catch (UncheckedSqlException e) {
				// Backup table exists --> merge the new data into the existing table

				// Remove duplicates
				sqlDbDataPortal.execSQL("DELETE FROM backup_" + TblPlayLog.TBL_NAME + "_tmp " +
						"WHERE (" + TblPlayLog.PLAY_LOG_ID + " IN (" +
						"  SELECT " + TblPlayLog.PLAY_LOG_ID + " " +
						"  FROM backup_" + TblPlayLog.TBL_NAME +
						"))");

				// Insert new data into the backup table
				sqlDbDataPortal
						.execSQL("REPLACE INTO backup_" + TblPlayLog.TBL_NAME + " SELECT * FROM backup_" + TblPlayLog.TBL_NAME + "_tmp");
			}

			// Drop the temp table
			sqlDbDataPortal.execSQL("DROP TABLE backup_" + TblPlayLog.TBL_NAME + "_tmp");

			sqlDbDataPortal.setTransactionSuccessful();
		} catch (UncheckedSqlException e) {
			// Ignore it
			Log.w(TAG, e);
		} finally {
			sqlDbDataPortal.endTransaction();
		}
	}

	@Override
	public void backupRatingData() {
		sqlDbDataPortal.beginTransaction();
		try {
			// Remove old temp table 
			sqlDbDataPortal.execSQL("DROP TABLE IF EXISTS backup_" + TblRating.TBL_NAME + "_tmp");

			// Create new temp table from the playlog data
			sqlDbDataPortal
					.execSQL("CREATE TABLE IF NOT EXISTS backup_" + TblRating.TBL_NAME + "_tmp AS " +
							"SELECT r.*, s." + TblSongs.ME_SONG_ID + " AS meSongId, s." + TblSongs.DATA + " AS songData, " +
							"    s." + TblSongs.NAME + " AS songName, a." + TblArtists.NAME + " AS artistName " +
							"FROM " + TblRating.TBL_NAME + " AS r " +
							"  INNER JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = r." + TblPlayLog.SONG_ID + ") " +
							"  INNER JOIN " + TblArtists.TBL_NAME + " AS A ON (a." + TblArtists.ARTIST_ID + " = s." + TblSongs.ARTIST_ID + ") ");

			try {
				// Create backup table if not exists
				sqlDbDataPortal
						.execSQL("CREATE TABLE backup_" + TblRating.TBL_NAME + " AS SELECT * FROM backup_" + TblRating.TBL_NAME + "_tmp");
			} catch (UncheckedSqlException e) {
				// Backup table exists --> merge the new data into the existing table

				// Remove duplicates
				sqlDbDataPortal.execSQL("DELETE FROM backup_" + TblRating.TBL_NAME + "_tmp " +
						"WHERE (" + TblRating.RATING_ID + " IN (" +
						"  SELECT " + TblRating.RATING_ID + " " +
						"  FROM backup_" + TblRating.TBL_NAME +
						"))");

				// Insert new data into the backup table
				sqlDbDataPortal
						.execSQL("REPLACE INTO backup_" + TblRating.TBL_NAME + " SELECT * FROM backup_" + TblRating.TBL_NAME + "_tmp");
			}

			// Drop the temp table
			sqlDbDataPortal.execSQL("DROP TABLE backup_" + TblRating.TBL_NAME + "_tmp");

			sqlDbDataPortal.setTransactionSuccessful();
		} catch (UncheckedSqlException e) {
			// Ignore it
			Log.w(TAG, e);
		} finally {
			sqlDbDataPortal.endTransaction();
		}
	}

	@Override
	public void restoreStatisticsData() {
		sqlDbDataPortal.beginTransaction();
		try {
			ICursor cur = sqlDbDataPortal.execSelect("SELECT name FROM sqlite_master WHERE type='table' AND name=?;",
					new String[] { "backup_" + TblPlayLog.TBL_NAME });
			boolean hasBackup = cur.moveToNext();
			cur.close();
			if (!hasBackup) {
				return;
			}

			String sql = "INSERT INTO " + TblPlayLog.TBL_NAME + " (" +
					"  " + TblPlayLog.PROFILE_ID + ", " +
					"  " + TblPlayLog.TIMESTAMP + ", " +
					"  " + TblPlayLog.TIME_ZONE_OFFSET + ", " +
					"  " + TblPlayLog.HOUR + ", " +
					"  " + TblPlayLog.DAY + ", " +
					"  " + TblPlayLog.ME_SONG_ID + ", " +
					"  " + TblPlayLog.ME_ARTIST_ID + ", " +
					"  " + TblPlayLog.PLAY_MODE + ", " +
					"  " + TblPlayLog.SONG_SOURCE + ", " +
					"  " + TblPlayLog.CONTEXT + ", " +
					"  " + TblPlayLog.SKIPPED + ", " +
					"  " + TblPlayLog.PLAYBACK_POSITION + ", " +
					"  " + TblPlayLog.SONG_ID + ", " +
					"  " + TblPlayLog.ARTIST_ID + " " +
					") " +
					"SELECT b." + TblPlayLog.PROFILE_ID + ", b." + TblPlayLog.TIMESTAMP + ", b." + TblPlayLog.TIME_ZONE_OFFSET + ", " +
					"  b." + TblPlayLog.HOUR + ", b." + TblPlayLog.DAY + ", b." + TblPlayLog.ME_SONG_ID + ", " +
					"  b." + TblPlayLog.ME_ARTIST_ID + ", b." + TblPlayLog.PLAY_MODE + ", b." + TblPlayLog.SONG_SOURCE + ", " +
					"  b." + TblPlayLog.CONTEXT + ", b." + TblPlayLog.SKIPPED + ", b." + TblPlayLog.PLAYBACK_POSITION + ", " +
					"  x." + TblSongs.SONG_ID + ", x." + TblArtists.ME_ARTIST_ID + " " +
					"FROM backup_" + TblPlayLog.TBL_NAME + " AS b " +
					"  LEFT JOIN (SELECT s.*, a." + TblArtists.NAME + " AS artistName, a." + TblArtists.ME_ARTIST_ID + " " + // Hack because nested joins have a problem with column propagation {@linkplain http://www.sqlite.org/cvstrac/tktview?tn=1994}
					"             FROM " + TblSongs.TBL_NAME + " AS s " +
					"               INNER JOIN " + TblArtists.TBL_NAME + " AS a ON (a." + TblArtists.ARTIST_ID + " = s." + TblSongs.ARTIST_ID + ") " +
					"  ) AS x ON (x." + TblSongs.ME_SONG_ID + " = b." + TblPlayLog.ME_SONG_ID + ") " + // Matches by ME_SONG_ID
					"     OR (x." + TblSongs.DATA + " = b." + TblSongs.DATA + ") " + // Matches by path
					"     OR ((x." + TblSongs.NAME + " = b.songName) AND (x." + TblArtists.NAME + " = b.artistName)) " + // Matches by song- and artist name
					"WHERE (x." + TblSongs.SONG_ID + " != 0) " +
					"GROUP BY b." + TblPlayLog.PLAY_LOG_ID + " " +
					"HAVING (COUNT(*) = 1)"; // Be shure that we don't use the same data multiple times. If it can be mapped to multiple songs, just forget it.

			sqlDbDataPortal.execSQL(sql);
			sqlDbDataPortal.execSQL("DROP TABLE backup_" + TblPlayLog.TBL_NAME);

			sqlDbDataPortal.setTransactionSuccessful();
		} catch (UncheckedSqlException e) {
			// Just ignore it.
			Log.d(TAG, e.getMessage());
		} finally {
			sqlDbDataPortal.endTransaction();
		}
	}

	@Override
	public void restoreRatingData() {
		sqlDbDataPortal.beginTransaction();
		try {
			ICursor cur = sqlDbDataPortal.execSelect("SELECT name FROM sqlite_master WHERE type='table' AND name=?;",
					new String[] { "backup_" + TblRating.TBL_NAME });
			boolean hasBackup = cur.moveToNext();
			cur.close();
			if (!hasBackup) {
				return;
			}

			String sql = "INSERT INTO " + TblRating.TBL_NAME + " (" +
					"  " + TblRating.PROFILE_ID + ", " +
					"  " + TblRating.TIMESTAMP + ", " +
					"  " + TblRating.RATING + ", " +
					"  " + TblRating.WEIGHT + ", " +
					"  " + TblRating.RATING_SOURCE + ", " +
					"  " + TblRating.SONG_ID + " " +
					") " +
					"SELECT b." + TblRating.PROFILE_ID + ", b." + TblRating.TIMESTAMP + ", b." + TblRating.RATING + ", " +
					"  b." + TblRating.WEIGHT + ", b." + TblRating.RATING_SOURCE + ", x." + TblSongs.SONG_ID + " " +
					"FROM backup_" + TblRating.TBL_NAME + " AS b " +
					"  LEFT JOIN (SELECT s.*, a." + TblArtists.NAME + " AS artistName " + // Hack because nested joins have a problem with column propagation {@linkplain http://www.sqlite.org/cvstrac/tktview?tn=1994}
					"             FROM " + TblSongs.TBL_NAME + " AS s " +
					"               INNER JOIN " + TblArtists.TBL_NAME + " AS a ON (a." + TblArtists.ARTIST_ID + " = s." + TblSongs.ARTIST_ID + ") " +
					"  ) AS x ON (x." + TblSongs.ME_SONG_ID + " = b.meSongId) " + // Matches by ME_SONG_ID
					"     OR (x." + TblSongs.DATA + " = b.songData) " + // Matches by path
					"     OR ((x." + TblSongs.NAME + " = b.songName) AND (x." + TblArtists.NAME + " = b.artistName)) " + // Matches by song- and artist name
					"WHERE (x." + TblSongs.SONG_ID + " != 0) " + // Matching succeeded
					"GROUP BY b." + TblRating.RATING_ID + " " +
					"HAVING (COUNT(*) = 1)"; // Be shure that we don't use the same data multiple times. If it can be mapped to multiple songs, just forget it.

			sqlDbDataPortal.execSQL(sql);
			sqlDbDataPortal.execSQL("DROP TABLE backup_" + TblRating.TBL_NAME);

			sqlDbDataPortal.setTransactionSuccessful();
		} catch (UncheckedSqlException e) {
			// Just ignore it.
			Log.d(TAG, e.getMessage());
		} finally {
			sqlDbDataPortal.endTransaction();
		}
	}

	// *** Top *** //

	private SubStatement getListeningTimeSelect(int profileId, DataType whichSelect, Pair<Date, Date> timeRange,
			TimeFilter timeFilter) {
		String id = "";
		String join = "";
		String groupBy = "";

		switch (whichSelect) {
			case Songs:
				id = "pl." + TblPlayLog.SONG_ID + " AS songId";
				join = "";
				groupBy = "pl." + TblPlayLog.SONG_ID;
				break;

			case Albums:
				id = "s." + TblSongs.ALBUM_ID + " AS albumId";
				join = "JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ")";
				groupBy = "s." + TblSongs.ALBUM_ID;
				break;

			case Artists:
				id = "s." + TblSongs.ARTIST_ID + " AS artistId";
				join = "JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ")";
				groupBy = "s." + TblSongs.ARTIST_ID;
				break;

			case Genres:
				id = "sg." + TblSongGenres.GENRE_ID + " AS genreId";
				join = "JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON (sg." + TblSongGenres.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ")";
				groupBy = "sg." + TblSongGenres.GENRE_ID;
				break;

			default:
				assert false;
		}

		// Time range
		String timeFilterWhere = "";
		switch (timeFilter) {
			case HOUR_OF_THE_DAY:
				int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				timeFilterWhere = "AND (pl." + TblPlayLog.HOUR + " = " + hourOfDay + ")";
				break;

			case DAY_OF_THE_WEEK:
				int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // sun = 1, sat = 7 -> adjust to 0..6
				timeFilterWhere = "AND (pl." + TblPlayLog.DAY + " = " + dayOfWeek + ")";
				break;

			case NONE:
				break;

			default:
				assert false;
		}

		// Final statement
		String sql = String.format(
				"SELECT %s, SUM(pl." + TblPlayLog.PLAYBACK_POSITION + ") AS playbackTime " +
						"FROM " + TblPlayLog.TBL_NAME + " AS pl " +
						"%s " +
						"WHERE (pl." + TblPlayLog.PROFILE_ID + " = " + profileId + ") " +
						"  AND (pl." + TblRating.TIMESTAMP + " <= ?) " +
						"  AND (pl." + TblRating.TIMESTAMP + " > ?) " +
						"  %s " +
						"GROUP BY %s",
				id, join, timeFilterWhere, groupBy);
		String[] values = new String[] { "" + timeRange.second.getTime(), "" + timeRange.first.getTime() };

		return new SubStatement(sql, values);
	}

	/**
	 * Returns the select statement for the weighted rating. The selected columns are:
	 * <p>
	 * {song, album, artist, genre}Id, ratingSum, weightSum
	 * </p>
	 * 
	 * @param ratingSources
	 *            Which rating sources should be considered
	 * @param smoothed
	 *            If the ratings should be smoothed with {@link #INITIAL_RATING} and {@link #INITIAL_RATING_WEIGHT} or
	 *            not
	 * @return
	 */
	private SubStatement getWeightedRatingSelect(int profileId, DataType whichSelect, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, RatingSource[] ratingSources, boolean smoothed) {
		String id = "";
		String join = "";
		String groupBy = "";
		switch (whichSelect) {
			case Songs:
				id = "vr.songId";
				join = "";
				groupBy = "songId";
				break;

			case Albums:
				id = "s." + TblSongs.ALBUM_ID + " AS albumId";
				join = "JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = vr.songId)";
				groupBy = "s." + TblSongs.ALBUM_ID;
				break;

			case Artists:
				id = "s." + TblSongs.ARTIST_ID + " AS artistId";
				join = "JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = vr.songId)";
				groupBy = "s." + TblSongs.ARTIST_ID;
				break;

			case Genres:
				id = "sg." + TblSongGenres.GENRE_ID + " AS genreId";
				join = "JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON (sg." + TblSongGenres.SONG_ID + " = vr.songId)";
				groupBy = "sg." + TblSongGenres.GENRE_ID;
				break;

			default:
				assert false;
		}

		// Time range
		String timeFilterWhere = "";
		switch (timeFilter) {
			case HOUR_OF_THE_DAY:
				int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				timeFilterWhere = "AND (vr.hourOfTheDay = " + hourOfDay + ")";
				break;

			case DAY_OF_THE_WEEK:
				int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // sun = 1, sat = 7 -> adjust to 0..6
				timeFilterWhere = "AND (vr.dayOfTheWeek = " + dayOfWeek + ")";
				break;

			case NONE:
				break;

			default:
				assert false;
		}

		// Rating source, this is a performance hack (We want IN (...) to have a fixed number of ? in it, so that the query can be cached)
		RatingSource[] allRatingSources = RatingSource.values();
		List<RatingSource> ratingSourcesLst = Arrays.asList(ratingSources);
		StringBuffer ratingSourcesInSql = new StringBuffer();
		List<String> ratingSourcesValues = new ArrayList<String>(allRatingSources.length);
		for (RatingSource rs : allRatingSources) {
			if (ratingSourcesLst.contains(rs)) {
				ratingSourcesValues.add("" + rs.value());
			} else {
				ratingSourcesValues.add("-1"); // Non-existing value
			}

			ratingSourcesInSql.append("?,");
		}
		ratingSourcesInSql.deleteCharAt(ratingSourcesInSql.length() - 1); // remove last ','

		// Should results be smoothed?
		String ratingAddition = "";
		String weightAddition = "";
		if (smoothed) {
			ratingAddition = " + " + (INITIAL_RATING * INITIAL_RATING_WEIGHT);
			weightAddition = " + " + INITIAL_RATING_WEIGHT;
		}

		// Get aging function for ratings
		int shift = (int) (((new Date()).getTime() - timeRange.second.getTime()) / 1000 / 60 / 60); // How many hours we would like to shift the aging function
		double agingM = (RATING_AGING_BEGIN_VALUE - RATING_AGING_END_VALUE) / (RATING_AGING_BEGIN - RATING_AGING_END);
		double agingB = RATING_AGING_BEGIN_VALUE - agingM * (RATING_AGING_BEGIN - shift);

		// Inner statement
		String innerSelect = "SELECT r." + TblRating.RATING_ID + " AS ratingId, " +
				"r." + TblRating.PROFILE_ID + " AS profileId, " +
				"r." + TblRating.SONG_ID + " AS songId, " +
				"r." + TblRating.TIMESTAMP + " AS timestamp, " +
				"r." + TblRating.RATING + " AS rating, " +
				"r." + TblRating.RATING_SOURCE + " AS ratingSource, " +
				"r." + TblRating.WEIGHT + " AS weight, " +
				"r." + TblRating.HOUR_OF_THE_DAY + " AS hourOfTheDay, " +
				"r." + TblRating.DAY_OF_THE_WEEK + " AS dayOfTheWeek, " +

				"CASE r." + TblRating.RATING_SOURCE + " " +
				"  WHEN " + RatingSource.Playlog.value() + " THEN " + WEIGHT_PLAYLOG_RATING + " " +
				"  WHEN " + RatingSource.Neighbor.value() + " THEN " + WEIGHT_NEIGHBORHOOD_RATING + " " +
				"  ELSE 0.0 " +
				"END AS weightRatingSource, " +

				"MIN(" + RATING_AGING_BEGIN_VALUE + ", MAX(" + RATING_AGING_END_VALUE + ", " +
				"  ? * (r." + TblRating.TIMESTAMP + "/1000/60/60 - " + ((new Date()).getTime() / 1000 / 60 / 60) + ") + ?" + // we work in [h]; ?agingM ?agingB
				")) AS weightAging " +

				"FROM " + TblRating.TBL_NAME + " AS r " +
				"WHERE (r." + TblRating.TIMESTAMP + " <= ?) " + // ?ts1
				"  AND (r." + TblRating.TIMESTAMP + " > ?)"; // ?ts2

		// Final statement
		String weight = "vr.weight * vr.weightAging * vr.weightRatingSource";
		String sql = "SELECT " + id + ", " +
				"  (SUM(vr.rating * " + weight + ")" + ratingAddition + ") AS ratingSum, " +
				"  (SUM(" + weight + ")" + weightAddition + ") AS weightSum " +
				"FROM (" + innerSelect + ") AS vr " +
				join + " " +
				"WHERE (vr.profileId = " + profileId + ") " +
				"  AND (vr.ratingSource IN (" + ratingSourcesInSql.toString() + ")) " + // ?rs
				timeFilterWhere + " " +
				"GROUP BY " + groupBy;

		List<String> values = new LinkedList<String>();
		values.add(String.format("%.3f", agingM)); // ?agingM
		values.add(String.format("%.3f", agingB)); // ?agingB
		values.add("" + timeRange.second.getTime()); // ?ts1
		values.add("" + timeRange.first.getTime()); // ?ts2
		values.addAll(ratingSourcesValues); // ?rs

		return new SubStatement(sql, values);
	}

	/**
	 * Returns the statement for the range which will be adjusted from
	 * <p>
	 * [-WEIGHT_RATING/(WEIGHT_RATING + WEIGHT_LISTENING_TIME), 1] to [-1, 1]
	 * </p>
	 * We simply stretch the negative range.
	 * 
	 * @return The statement
	 */
	private String getRangeAdjustedRatingStatement() {
		/* - WEIGHT_LISTENING_TIME% of the final rating is from the listening time: How much listening time has 
		 *   this song compared to the maximum listening time for a song? (linear, nonnegative)
		 * - WEIGHT_RATING% of the final rating is the weighted ratings (linear, negative possible)
		 */
		double leftBound = -WEIGHT_RATING / (WEIGHT_RATING + WEIGHT_LISTENING_TIME);
		/*double rightBound = 1.0d;
		double moveAdjustment = -leftBound;
		double widthAdjustment = 1 / (rightBound + moveAdjustment) * 2;
		String rangeAdjustment = String.format("(%s + %.5f) * %.5f - 1", "%s", moveAdjustment, widthAdjustment);*/

		String weightedRating = "((IFNULL(lt.rating*" + WEIGHT_LISTENING_TIME + ", 0) + IFNULL(wr.rating*" + WEIGHT_RATING + ", 0)) " +
				"/ (" + (WEIGHT_LISTENING_TIME + WEIGHT_RATING) + "))"; // (a*w1 + a*w2) / (w1 + w2)

		return "CASE " +
				"  WHEN " + weightedRating + " >= 0 THEN " + weightedRating + " " +
				"  ELSE " + weightedRating + " / " + Math.abs(leftBound) + " " +
				"END AS finalRating";
	}

	@Override
	public <T extends BaseSong<BaseArtist, BaseAlbum>> List<StatisticsSong<BaseArtist, BaseAlbum>> getSongRatings(
			int profileId, List<T> songs, Pair<Date, Date> timeRange, TimeFilter timeFilter,
			RatingSource[] ratingSources, boolean smoothed) {
		if (songs.size() > 0) {

			SubStatement songIdsStmt = getSongIdsStmt(songs);
			SubStatement additionalWhereStmt = new SubStatement(
					"AND (s." + TblSongs.SONG_ID + " IN (" + songIdsStmt.getSql() + "))", songIdsStmt.getValues());

			List<StatisticsSong<BaseArtist, BaseAlbum>> ret = getTopSongs(profileId, songs.size(), timeRange,
					timeFilter, Direction.ALL, smoothed, ratingSources, additionalWhereStmt);

			// Add songs to the returned list, for which no rating is there
			for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
				if (ret.indexOf(song) == -1) {
					ret.add(new StatisticsSong<BaseArtist, BaseAlbum>(song, 0.0f));
				}
			}
			return ret;
		} else {
			return new ArrayList<StatisticsSong<BaseArtist, BaseAlbum>>();
		}
	}

	@Override
	public <T extends BaseArtist> List<StatisticsArtist> getArtistRatings(int profileId, List<T> artists,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, RatingSource[] ratingSources, boolean smoothed) {
		if (artists.size() > 0) {
			SubStatement artistIdsStmt = getArtistIdsStmt(artists);
			SubStatement additionalWhereStmt = new SubStatement(
					"AND (a." + TblArtists.ARTIST_ID + " IN (" + artistIdsStmt.getSql() + "))",
					artistIdsStmt.getValues());

			List<StatisticsArtist> ret = getTopArtists(profileId, artists.size(), timeRange, timeFilter, Direction.ALL,
					smoothed, ratingSources, additionalWhereStmt);

			// Add songs to the returned list, for which no rating is there
			for (BaseArtist artist : artists) {
				if (ret.indexOf(artist) == -1) {
					ret.add(new StatisticsArtist(artist, 0.0f));
				}
			}
			return ret;
		} else {
			return new ArrayList<StatisticsArtist>();
		}
	}

	@Override
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getTopSongs(int profileId, int maxNum,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, Direction direction, boolean smoothed) {
		return getTopSongs(profileId, maxNum, timeRange, timeFilter, direction, smoothed,
				new RatingSource[] { RatingSource.Playlog }, null);
	}

	/**
	 * @see #getTopSongs(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction, boolean)
	 * 
	 * @param ratingSources
	 *            Which rating sources should be considered
	 * @param additionalWhereExpr
	 *            Additional expressions for the WHERE clause in the final sql query
	 */
	private List<StatisticsSong<BaseArtist, BaseAlbum>> getTopSongs(int profileId, int maxNum,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, Direction direction, boolean smoothed,
			RatingSource[] ratingSources, SubStatement additionalWhereExpr) {

		if (additionalWhereExpr == null) {
			additionalWhereExpr = new SubStatement("", new String[] {});
		}

		// Get the listening time select statement
		SubStatement listeningTimeSelect = getListeningTimeSelect(profileId, DataType.Songs, timeRange, timeFilter);

		// Get the weighted rating select statement
		SubStatement weightedRatingSelect = getWeightedRatingSelect(profileId, DataType.Songs, timeRange, timeFilter,
				ratingSources, smoothed);

		// Select the top songs
		String having;
		String orderBy;
		switch (direction) {
			case TOP:
				having = "HAVING (finalRating > 0)";
				orderBy = "ORDER BY finalRating DESC";
				break;

			case FLOP:
				having = "HAVING (finalRating <= 0)";
				orderBy = "ORDER BY finalRating ASC";
				break;

			case ALL:
				having = "";
				orderBy = "";
				break;

			default:
				assert false;
				having = "";
				orderBy = "";
		}
		String sql = "SELECT s." + TblSongs.SONG_ID + " AS songId, " + getRangeAdjustedRatingStatement() + " " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +

				// Listening time
				"LEFT JOIN (" +
				"  SELECT lti.songId, " +
				"    CAST(lti.playbackTime AS REAL)/ltMax.maxPlaybackTime AS rating " + // playback fraction compared to max listened
				"  FROM (" + listeningTimeSelect + ") AS lti " + // ?lt1
				"  INNER JOIN (" +
				"    SELECT MAX(playbackTime) AS maxPlaybackTime " + // Get the max playback time <- prefetch this in an own statement?
				"    FROM (" + listeningTimeSelect + ") " + // ?lt2
				"  ) AS ltMax " + // no ON!
				") AS lt ON (lt.songId = s." + TblSongs.SONG_ID + ") " +

				// Weighted rating
				"LEFT JOIN (" +
				"  SELECT wri.songId, " +
				"    wri.ratingSum/wri.weightSum AS rating " + // Get weighted rating
				"  FROM (" + weightedRatingSelect + ") AS wri " + // ?wr
				") AS wr ON (wr.songId = s." + TblSongs.SONG_ID + ") " +

				"WHERE (NOT (lt.rating IS NULL AND wr.rating IS NULL)) " + // Only select if some data is available
				additionalWhereExpr + // ?additionalWhere
				"GROUP BY songId " + // Needed for having to work
				having + " " +
				orderBy + " " +
				"LIMIT ?"; // ?limit

		try {
			Log.d("getTopSongs", "Stmt: " + sql);

			List<String> values = new LinkedList<String>();
			values.addAll(listeningTimeSelect.getValues()); // ?lt1
			values.addAll(listeningTimeSelect.getValues()); // ?lt2
			values.addAll(weightedRatingSelect.getValues()); // ?wr
			values.addAll(additionalWhereExpr.getValues()); // ?additionalWhere
			values.add("" + maxNum); // ?limit

			StopWatch sw = StopWatch.start();
			List<StatisticsSong<BaseArtist, BaseAlbum>> ret = getSongsFromSql(sql, values.toArray(new String[0]),
					FLOAT_PARSER);
			sw.stop();

			Log.d("getTopSongs", "Time: " + String.format("%.2f", sw.getTime() / 1000.0d));

			/*
			// is result correct?
			String sql2 = sql;
			for (String val : values) {
				sql2 = sql2.replaceFirst("\\?", val);
			}
			StopWatch sw2 = StopWatch.start();
			List<StatisticsSong<BaseArtist, BaseAlbum>> ret2 = getSongsFromSql(sql2, new String[] {}, FLOAT_PARSER);
			sw2.stop();
			Log.d("getTopSongs", "Time2: " + String.format("%.1f", sw2.getTime() / 1000.0d));
			for (StatisticsSong<BaseArtist, BaseAlbum> song2 : ret2) {
				StatisticsSong<BaseArtist, BaseAlbum> song;
				if (!ret.contains(song2)) {
					Log.e(TAG, "Song not there...");
					continue;
				} else {
					song = ret.get(ret.indexOf(song2));
				}

				if (!song.getValue().equals(song2.getValue())) {
					Log.e(TAG, String.format("Not same values: %.3f <-> %.3f", song.getValue(), song2.getValue()));
				}
			}*/

			return ret;
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return new ArrayList<StatisticsSong<BaseArtist, BaseAlbum>>();
		}
	}

	@Override
	public List<StatisticsAlbum> getTopAlbums(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed) {
		return getTopAlbums(profileId, maxNum, timeRange, timeFilter, direction, smoothed,
				new RatingSource[] { RatingSource.Playlog });
	}

	/**
	 * @see #getTopAlbums(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction, boolean)
	 * @param ratingSources
	 *            Which rating sources should be considered
	 */
	private List<StatisticsAlbum> getTopAlbums(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed, RatingSource[] ratingSources) {
		assert direction == Direction.TOP || direction == Direction.FLOP; // All other not implemented yet

		// Get the listening time select statement
		SubStatement listeningTimeSelect = getListeningTimeSelect(profileId, DataType.Albums, timeRange, timeFilter);

		// Get the weighted rating select statement
		SubStatement weightedRatingSelect = getWeightedRatingSelect(profileId, DataType.Albums, timeRange, timeFilter,
				ratingSources, smoothed);

		// Select the top albums
		String sql = "SELECT a." + TblAlbums.ALBUM_ID + " AS albumId, a." + TblAlbums.ALBUM_NAME + ", " + getRangeAdjustedRatingStatement() + " " +
				"FROM " + TblAlbums.TBL_NAME + " AS a " +

				// Listening time
				"LEFT JOIN (" +
				"  SELECT lti.albumId, " +
				"    CAST(lti.playbackTime AS REAL)/ltMax.maxPlaybackTime AS rating " + // playback fraction compared to max listened
				"  FROM (" + listeningTimeSelect + ") AS lti " + // ?lt1
				"  INNER JOIN (" +
				"    SELECT MAX(playbackTime) AS maxPlaybackTime " + // Get the max playback time
				"    FROM (" + listeningTimeSelect + ") " + // ?lt2
				"  ) AS ltMax " + // no ON!
				") AS lt ON (lt.albumId = a." + TblAlbums.ALBUM_ID + ") " +

				// Weighted rating
				"LEFT JOIN (" +
				"  SELECT wri.albumId, " +
				"    wri.ratingSum/wri.weightSum AS rating " + // Get weighted rating
				"  FROM (" + weightedRatingSelect + ") AS wri " + // ?wr
				") AS wr ON (wr.albumId = a." + TblAlbums.ALBUM_ID + ") " +

				"WHERE (NOT (lt.rating IS NULL AND wr.rating IS NULL)) " + // Only select if some data is available
				"GROUP BY albumId " + // Needed for having to work
				"HAVING (finalRating " + ((direction == Direction.TOP) ? ">" : "<=") + " 0) " +
				"ORDER BY finalRating " + ((direction == Direction.TOP) ? "DESC" : "ASC") + " " +
				"LIMIT ?"; // ?limit

		try {
			List<String> values = new LinkedList<String>();
			values.addAll(listeningTimeSelect.getValues()); // ?lt1
			values.addAll(listeningTimeSelect.getValues()); // ?lt2
			values.addAll(weightedRatingSelect.getValues()); // ?wr
			values.add("" + maxNum); // ?limit

			return getAlbumsFromSql(sql, values.toArray(new String[0]), FLOAT_PARSER);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return new ArrayList<StatisticsAlbum>();
		}
	}

	@Override
	public List<StatisticsArtist> getTopArtists(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed) {
		return getTopArtists(profileId, maxNum, timeRange, timeFilter, direction, smoothed,
				new RatingSource[] { RatingSource.Playlog }, null);
	}

	/**
	 * @see #getTopArtists(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction, boolean)
	 * @param ratingSources
	 *            Which rating sources should be considered
	 * @param additionalWhereExpr
	 *            Additional expressions for the WHERE clause in the final sql query
	 */
	private List<StatisticsArtist> getTopArtists(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed, RatingSource[] ratingSources,
			SubStatement additionalWhereExpr) {

		if (additionalWhereExpr == null) {
			additionalWhereExpr = new SubStatement("", new String[] {});
		}

		// Get the listening time select statement
		SubStatement listeningTimeSelect = getListeningTimeSelect(profileId, DataType.Artists, timeRange, timeFilter);

		// Get the weighted rating select statement
		SubStatement weightedRatingSelect = getWeightedRatingSelect(profileId, DataType.Artists, timeRange, timeFilter,
				ratingSources, smoothed);

		// Select the top albums
		// Select the top songs
		String having;
		String orderBy;
		switch (direction) {
			case TOP:
				having = "HAVING (finalRating > 0)";
				orderBy = "ORDER BY finalRating DESC";
				break;

			case FLOP:
				having = "HAVING (finalRating <= 0)";
				orderBy = "ORDER BY finalRating ASC";
				break;

			case ALL:
				having = "";
				orderBy = "";
				break;

			default:
				assert false;
				having = "";
				orderBy = "";
		}
		String sql = "SELECT a." + TblArtists.ARTIST_ID + " AS artistId, a." + TblArtists.NAME + ", " + getRangeAdjustedRatingStatement() + " " +
				"FROM " + TblArtists.TBL_NAME + " AS a " +

				// Listening time
				"LEFT JOIN (" +
				"  SELECT lti.artistId, " +
				"    CAST(lti.playbackTime AS REAL)/ltMax.maxPlaybackTime AS rating " + // playback fraction compared to max listened
				"  FROM (" + listeningTimeSelect + ") AS lti " + // ?lt1
				"  INNER JOIN (" +
				"    SELECT MAX(playbackTime) AS maxPlaybackTime " + // Get the max playback time
				"    FROM (" + listeningTimeSelect + ") " + // ?lt2
				"  ) AS ltMax " + // no ON!
				") AS lt ON (lt.artistId = a." + TblArtists.ARTIST_ID + ") " +

				// Weighted rating
				"LEFT JOIN (" +
				"  SELECT wri.artistId, " +
				"    wri.ratingSum/wri.weightSum AS rating " + // Get weighted rating
				"  FROM (" + weightedRatingSelect + ") AS wri " + // ?wr
				") AS wr ON (wr.artistId = a." + TblArtists.ARTIST_ID + ") " +

				"WHERE (NOT (lt.rating IS NULL AND wr.rating IS NULL)) " + // Only select if some data is available
				additionalWhereExpr + " " + // ?additionalWhere
				"GROUP BY artistId " + // Needed for having to work
				having + " " +
				orderBy + " " +
				"LIMIT ?"; // ?limit

		Log.d("getTopArtists", "Stmt: " + sql);

		List<String> values = new LinkedList<String>();
		values.addAll(listeningTimeSelect.getValues()); // ?lt1
		values.addAll(listeningTimeSelect.getValues()); // ?lt2
		values.addAll(weightedRatingSelect.getValues()); // ?wr
		values.addAll(additionalWhereExpr.getValues()); // ?additionalWhere
		values.add("" + maxNum); // ?limit

		StopWatch sw = StopWatch.start();
		List<StatisticsArtist> ret = getArtistsFromSql(sql, values.toArray(new String[0]),
				FLOAT_PARSER);
		sw.stop();

		Log.d("getTopArtists", "Time: " + String.format("%.2f", sw.getTime() / 1000.0d));

		return ret;
	}

	@Override
	public List<StatisticsGenre> getTopGenres(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed) {
		return getTopGenres(profileId, maxNum, timeRange, timeFilter, direction, smoothed,
				new RatingSource[] { RatingSource.Playlog });
	}

	/**
	 * @see #getTopGenres(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction, boolean)
	 * @param ratingSources
	 *            Which rating sources should be considered
	 */
	private List<StatisticsGenre> getTopGenres(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, Direction direction, boolean smoothed, RatingSource[] ratingSources) {
		assert direction == Direction.TOP || direction == Direction.FLOP; // All other not implemented yet

		// Get the listening time select statement
		SubStatement listeningTimeSelect = getListeningTimeSelect(profileId, DataType.Genres, timeRange, timeFilter);

		// Get the weighted rating select statement
		SubStatement weightedRatingSelect = getWeightedRatingSelect(profileId, DataType.Genres, timeRange, timeFilter,
				ratingSources, smoothed);

		// Select the top albums
		String sql = "SELECT g." + TblGenres.GENRE_ID + " AS genreId, g." + TblGenres.NAME + ", " + getRangeAdjustedRatingStatement() + " " +
				"FROM " + TblGenres.TBL_NAME + " AS g " +

				// Listening time
				"LEFT JOIN (" +
				"  SELECT lti.genreId, " +
				"    CAST(lti.playbackTime AS REAL)/ltMax.maxPlaybackTime AS rating " + // playback fraction compared to max listened
				"  FROM (" + listeningTimeSelect + ") AS lti " +
				"  INNER JOIN (" +
				"    SELECT MAX(playbackTime) AS maxPlaybackTime " + // Get the max playback time
				"    FROM (" + listeningTimeSelect + ") " +
				"  ) AS ltMax " + // no ON!
				") AS lt ON (lt.genreId = g." + TblGenres.GENRE_ID + ") " +

				// Weighted rating
				"LEFT JOIN (" +
				"  SELECT wri.genreId, " +
				"    wri.ratingSum/wri.weightSum AS rating " + // Get weighted rating
				"  FROM (" + weightedRatingSelect + ") AS wri " +
				") AS wr ON (wr.genreId = g." + TblGenres.GENRE_ID + ") " +

				"WHERE (NOT (lt.rating IS NULL AND wr.rating IS NULL)) " + // Only select if some data is available
				"GROUP BY genreId " + // Needed for having to work
				"HAVING (finalRating " + ((direction == Direction.TOP) ? ">" : "<=") + " 0) " +
				"ORDER BY finalRating " + ((direction == Direction.TOP) ? "DESC" : "ASC") + " " +
				"LIMIT ?";

		List<String> values = new LinkedList<String>();
		values.addAll(listeningTimeSelect.getValues()); // ?lt1
		values.addAll(listeningTimeSelect.getValues()); // ?lt2
		values.addAll(weightedRatingSelect.getValues()); // ?wr
		values.add("" + maxNum); // ?limit

		return getGenresFromSql(sql, values.toArray(new String[0]), FLOAT_PARSER);
	}

	// *** Suggested *** //

	/**
	 * Returns the sql statement for the entries which were played for longer than maxLatelyListeningTime.
	 * 
	 * @return The sql statement
	 * @see #getSuggestedSongs(int, Date, long, int)
	 */
	private SubStatement getLatelyListeningAboveThresholdSql(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, DataType whichData) {

		String id = "";
		String join = "";
		String groupBy = "";
		switch (whichData) {
			case Songs:
				id = "pl." + TblPlayLog.SONG_ID + " AS songId";
				join = "";
				groupBy = "pl." + TblPlayLog.SONG_ID;
				break;

			case Albums:
				id = "s." + TblSongs.ALBUM_ID + " AS albumId";
				join = "JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ")";
				groupBy = "s." + TblSongs.ALBUM_ID;
				break;

			case Artists:
				id = "pl." + TblPlayLog.ARTIST_ID + " AS artistId";
				join = "";
				groupBy = "pl." + TblPlayLog.ARTIST_ID;
				break;

			case Genres:
				id = "sg." + TblSongGenres.GENRE_ID + " AS genreID";
				join = "LEFT JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON (sg." + TblSongGenres.SONG_ID + " = pl." + TblPlayLog.SONG_ID + ")";
				groupBy = "sg." + TblSongGenres.GENRE_ID;
				break;

			default:
				assert false;
		}

		String sql = "SELECT " + id + " " +
				"FROM " + TblPlayLog.TBL_NAME + " AS pl " +
				join + " " +
				"WHERE (pl." + TblPlayLog.TIMESTAMP + " >= ?) " +
				"GROUP BY " + groupBy + " " +
				"HAVING (SUM(pl." + TblPlayLog.PLAYBACK_POSITION + ") > " + maxLatelyListeningTime + ") "; // having does not allow ?

		String[] values = new String[] { "" + cutBetweenOnceAndLately.getTime() };

		return new SubStatement(sql, values);
	}

	/**
	 * @see #getLatelyListeningAboveThresholdSql(int, Date, long, DataType)
	 */
	private List<Integer> getLatelyListeningAboveThreshold(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, DataType whichData) {

		SubStatement stmt = getLatelyListeningAboveThresholdSql(profileId, cutBetweenOnceAndLately,
				maxLatelyListeningTime, whichData);

		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(stmt.getSql(), stmt.getValues().toArray(new String[0]));

			List<Integer> ret = new ArrayList<Integer>();
			while (cur.moveToNext()) {
				ret.add(cur.getInt(0));
			}
			return ret;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * Adds the {@link IStatisticsData}-entry to the given map if there is not already an entry with this id in there or
	 * the rating of the entry in the map is smaller than the rating of the given entry.<br/>
	 * If the include filter is non-empty then the entrys id must be in there. It also must not be in the excludeFilter
	 * 
	 * @param map
	 * @param entry
	 *            The entry
	 * @param includeFilter
	 * @param excludeFilter
	 */
	private <T extends IStatisticsData> void addToMap(Map<Integer, T> map, T entry, List<Integer> includeFilter,
			List<Integer> excludeFilter) {
		if (!includeFilter.isEmpty() && !includeFilter.contains(entry.getId())) {
			// Not in include filter
			return;
		}
		if (excludeFilter.contains(entry.getId())) {
			// In exclude filter
			if (includeFilter.isEmpty()) {
				// No explicit include filter -> so just ignore this entry
				return;
			} else {
				// An explicit include filter is set and we therefore need to rate this entry (very bad!)
				entry.setValue(-1.0f);
			}
		}

		T entryAlreadyThere = map.get(entry.getId());
		if (entryAlreadyThere != null) {
			if ((Float) entry.getValue() <= (Float) entryAlreadyThere.getValue()) {
				// smaller rating -> continue
				return;
			}
		}

		map.put(entry.getId(), entry);
	}

	private final Map<TopEntriesAgoCacheEntry, List<? extends IStatisticsData>> topEntriesAgoCache = new HashMap<TopEntriesAgoCacheEntry, List<? extends IStatisticsData>>();

	/**
	 * Returns top entries from ago. If a call to this function whith approximatively the same timeRange and the same
	 * other parameters, a cached result is returned.
	 * 
	 * @see #getTopSongs(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter,
	 *      ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction, boolean, RatingSource[], SubStatement)
	 * @return The top entries
	 */
	@SuppressWarnings("unchecked")
	private <T extends IStatisticsData> List<T> getTopEntriesAgo(int profileId, int maxNum, Pair<Date, Date> timeRange,
			TimeFilter timeFilter, boolean smoothed, RatingSource[] ratingSources, DataType dataType,
			SubStatement additionalWhere) {

		TopEntriesAgoCacheEntry ce = new TopEntriesAgoCacheEntry(profileId, timeRange, timeFilter, smoothed,
				ratingSources, dataType, additionalWhere);
		if (topEntriesAgoCache.containsKey(ce)) {
			return (List<T>) topEntriesAgoCache.get(ce);
		}

		List<T> data;
		switch (dataType) {
			case Songs:
				data = (List<T>) getTopSongs(profileId, maxNum, timeRange, timeFilter, Direction.TOP, smoothed,
						ratingSources, additionalWhere);
				break;

			case Albums:
				data = (List<T>) getTopAlbums(profileId, maxNum, timeRange, timeFilter, Direction.TOP, smoothed,
						ratingSources);
				break;

			case Artists:
				data = (List<T>) getTopArtists(profileId, maxNum, timeRange, timeFilter, Direction.TOP, smoothed,
						ratingSources, additionalWhere);
				break;

			case Genres:
				data = (List<T>) getTopGenres(profileId, maxNum, timeRange, timeFilter, Direction.TOP, smoothed,
						ratingSources);
				break;

			default:
				assert false;
				data = null;
		}

		topEntriesAgoCache.put(ce, data);
		return data;
	}

	@Override
	public <T extends BaseSong<BaseArtist, BaseAlbum>> List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongRatings(
			int profileId, List<T> songs, Date cutBetweenOnceAndLately, long maxLatelyListeningTime,
			Pair<Date, Date> timeRange, TimeFilter timeFilter, boolean smoothed)
	{
		List<Integer> idFilter = new ArrayList<Integer>(songs.size());
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			idFilter.add(song.getId());
		}

		return getSuggested(profileId, cutBetweenOnceAndLately, maxLatelyListeningTime, timeRange, timeFilter,
				songs.size(), DataType.Songs, smoothed, idFilter);
	}

	@SuppressWarnings("unchecked")
	private <T extends IStatisticsData> List<T> getSuggested(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			DataType dataType, boolean smoothed, List<Integer> entryIdIncludeFilter) {

		if (entryIdIncludeFilter == null) {
			entryIdIncludeFilter = new ArrayList<Integer>();
		}

		if (cutBetweenOnceAndLately.before(timeRange.first)) {
			cutBetweenOnceAndLately = timeRange.first;
		}
		if (cutBetweenOnceAndLately.after(timeRange.second)) {
			cutBetweenOnceAndLately = timeRange.second;
		}

		boolean excludeThoseWithTooMuchListeningTime = maxLatelyListeningTime >= 0;

		List<Integer> entryIdExcludeFilter = new ArrayList<Integer>();

		// Init the different lists
		List<T> topEntriesNeighborhood = null;
		List<T> topEntriesAgo = null;
		List<T> flopEntriesTmp = null;
		List<T> recentlyImportedEntriesTmp = null;

		// Get the import time range
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, -RATING_RECENTLY_IMPORTED_MAX_AGE);
		Date importMaxAge = cal.getTime();
		if (importMaxAge.getTime() < timeRange.first.getTime()) {
			// out of timeRange -> adjust it
			importMaxAge = timeRange.first;
		}
		Pair<Date, Date> importTimeRange = new Pair<Date, Date>(importMaxAge, timeRange.second);

		Pair<Date, Date> agoTimeRange = new Pair<Date, Date>(timeRange.first, cutBetweenOnceAndLately);
		switch (dataType) {
			case Songs: {
				// Get additionalWhere from tooMuchListeningTime & entryIdFilter entries
				SubStatement additionalWhereStmt = new SubStatement("", new String[] {});
				if (entryIdIncludeFilter.size() > 0) {
					SubStatement idsStmt = getIdsStmt(entryIdIncludeFilter);
					additionalWhereStmt = new SubStatement(
							"AND (s." + TblSongs.SONG_ID + " IN (" + idsStmt.getSql() + ")) ", idsStmt.getValues());
					// TODO @sämy: use a temporary table with the filtered ids here? then the query would be completely static again
				}

				if (excludeThoseWithTooMuchListeningTime) {
					SubStatement tooMuchListeningTimeStmtTmp = getLatelyListeningAboveThresholdSql(profileId,
							cutBetweenOnceAndLately, maxLatelyListeningTime, dataType);

					String sql = "AND (s." + TblSongs.SONG_ID + " NOT IN (" + tooMuchListeningTimeStmtTmp.getSql() + ")) ";
					SubStatement tooMuchListeningTimeStmt = new SubStatement(sql,
							tooMuchListeningTimeStmtTmp.getValues());

					List<String> values = new ArrayList<String>(additionalWhereStmt.getValues());
					values.addAll(tooMuchListeningTimeStmt.getValues());
					additionalWhereStmt = new SubStatement(
							additionalWhereStmt.getSql() + tooMuchListeningTimeStmt.getSql(), values);
				}

				topEntriesNeighborhood = (List<T>) getTopSongs(profileId, maxNum, timeRange, timeFilter, Direction.TOP,
						smoothed, new RatingSource[] { RatingSource.Neighbor }, additionalWhereStmt);
				topEntriesAgo = getTopEntriesAgo(profileId, maxNum, agoTimeRange, timeFilter, smoothed,
						new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor }, dataType,
						additionalWhereStmt);
				flopEntriesTmp = (List<T>) getTopSongs(profileId, maxNum, timeRange, timeFilter, Direction.FLOP,
						smoothed, new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor },
						additionalWhereStmt);
				recentlyImportedEntriesTmp = (List<T>) getImportedSongs(importTimeRange, maxNum);
				break;
			}

			case Albums:
				// Fetch the entries which were played too much
				if (excludeThoseWithTooMuchListeningTime) {
					entryIdExcludeFilter.addAll(getLatelyListeningAboveThreshold(profileId, cutBetweenOnceAndLately,
							maxLatelyListeningTime, dataType));
				}

				topEntriesNeighborhood = (List<T>) getTopAlbums(profileId, maxNum, timeRange, timeFilter,
						Direction.TOP, smoothed, new RatingSource[] { RatingSource.Neighbor });
				topEntriesAgo = getTopEntriesAgo(profileId, maxNum, agoTimeRange, timeFilter, smoothed,
						new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor }, dataType, null);
				flopEntriesTmp = (List<T>) getTopAlbums(profileId, maxNum, timeRange, timeFilter, Direction.FLOP,
						smoothed, new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor });
				recentlyImportedEntriesTmp = (List<T>) getImportedAlbums(importTimeRange, maxNum);
				break;

			case Artists: {
				// Get additionalWhere from tooMuchListeningTime & entryIdFilter entries
				SubStatement additionalWhereStmt = new SubStatement("", new String[] {});
				if (entryIdIncludeFilter.size() > 0) {
					SubStatement idsStmt = getIdsStmt(entryIdIncludeFilter);
					additionalWhereStmt = new SubStatement(
							"AND (a." + TblArtists.ARTIST_ID + " IN (" + idsStmt.getSql() + ")) ", idsStmt.getValues());
					// TODO @sämy: use a temporary table with the filtered ids here? then the query would be completely static again
				}

				if (excludeThoseWithTooMuchListeningTime) {
					SubStatement tooMuchListeningTimeStmtTmp = getLatelyListeningAboveThresholdSql(profileId,
							cutBetweenOnceAndLately, maxLatelyListeningTime, dataType);

					String sql = "AND (a." + TblArtists.ARTIST_ID + " NOT IN (" + tooMuchListeningTimeStmtTmp.getSql() + ")) ";
					SubStatement tooMuchListeningTimeStmt = new SubStatement(sql,
							tooMuchListeningTimeStmtTmp.getValues());

					List<String> values = new ArrayList<String>(additionalWhereStmt.getValues());
					values.addAll(tooMuchListeningTimeStmt.getValues());
					additionalWhereStmt = new SubStatement(
							additionalWhereStmt.getSql() + tooMuchListeningTimeStmt.getSql(), values);
				}

				topEntriesNeighborhood = (List<T>) getTopArtists(profileId, maxNum, timeRange, timeFilter,
						Direction.TOP, smoothed, new RatingSource[] { RatingSource.Neighbor }, additionalWhereStmt);
				topEntriesAgo = getTopEntriesAgo(profileId, maxNum, agoTimeRange, timeFilter, smoothed,
						new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor }, dataType,
						additionalWhereStmt);
				flopEntriesTmp = (List<T>) getTopArtists(profileId, maxNum, timeRange, timeFilter, Direction.FLOP,
						smoothed, new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor },
						additionalWhereStmt);
				recentlyImportedEntriesTmp = (List<T>) getImportedArtists(importTimeRange, maxNum);
				break;
			}

			case Genres:
				// Fetch the entries which were played too much
				if (excludeThoseWithTooMuchListeningTime) {
					entryIdExcludeFilter.addAll(getLatelyListeningAboveThreshold(profileId, cutBetweenOnceAndLately,
							maxLatelyListeningTime, dataType));
				}

				topEntriesNeighborhood = (List<T>) getTopGenres(profileId, maxNum, timeRange, timeFilter,
						Direction.TOP, smoothed, new RatingSource[] { RatingSource.Neighbor });
				topEntriesAgo = getTopEntriesAgo(profileId, maxNum, agoTimeRange, timeFilter, smoothed,
						new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor }, dataType, null);
				flopEntriesTmp = (List<T>) getTopGenres(profileId, maxNum, timeRange, timeFilter, Direction.FLOP,
						smoothed, new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor });
				recentlyImportedEntriesTmp = (List<T>) getImportedGenres(importTimeRange, maxNum);
				break;

			default:
				assert false;
		}

		// Get the flop ids
		for (T flopEntry : flopEntriesTmp) {
			entryIdExcludeFilter.add(flopEntry.getId());
		}

		// Calculate the recently imported entries rating
		List<Integer> importExclusionFilter = new ArrayList<Integer>();
		if (!excludeThoseWithTooMuchListeningTime && !recentlyImportedEntriesTmp.isEmpty()) {
			// We need this additional filter here to not only return recently imported songs even if they got played a lot.			 
			importExclusionFilter.addAll(getLatelyListeningAboveThreshold(profileId, cutBetweenOnceAndLately,
					Math.abs(maxLatelyListeningTime), dataType));
		}

		List<T> recentlyImportedEntries = new ArrayList<T>();
		long now = new Date().getTime();
		for (T recentlyImportedEntry : recentlyImportedEntriesTmp) {
			if (importExclusionFilter.contains(recentlyImportedEntry.getId())) {
				continue;
			}

			long timestamp = ((Date) recentlyImportedEntry.getValue()).getTime();
			long timeAgo = now - timestamp;

			int bucketSize = 1000 * 60 * 60; // consider all timestamps which are from within the same hour as the same [ms]
			double rating = Math
					.max(RATING_RECENTLY_IMPORTED_MAX_RATING * (1 - (timeAgo / bucketSize) / (double) (RATING_RECENTLY_IMPORTED_MAX_AGE / bucketSize)),
							0); // RATING_RECENTLY_MAX_RATING when just imported, 0 when imported >= RATING_RECENTLY_MAX_TIMESTAMP ms ago

			T entry = null;
			switch (dataType) {
				case Songs:
					entry = (T) new StatisticsSong<BaseArtist, BaseAlbum>(
							(BaseSong<BaseArtist, BaseAlbum>) recentlyImportedEntry, (float) rating);
					break;

				case Albums:
					StatisticsAlbum rieAl = (StatisticsAlbum) recentlyImportedEntry;
					entry = (T) new StatisticsAlbum(rieAl.getId(), rieAl.getName(), rieAl.getArtists(), (float) rating);
					break;

				case Artists:
					StatisticsArtist rieAr = (StatisticsArtist) recentlyImportedEntry;
					entry = (T) new StatisticsArtist(rieAr.getId(), rieAr.getName(), (float) rating);
					break;

				case Genres:
					StatisticsGenre rieG = (StatisticsGenre) recentlyImportedEntry;
					entry = (T) new StatisticsGenre(rieG.getId(), rieG.getName(), (float) rating);
					break;

				default:
					assert false;
			}
			recentlyImportedEntries.add(entry);
		}

		// Combine all 
		Map<Integer, T> entryMap = new HashMap<Integer, T>();
		for (T entry : topEntriesNeighborhood) {
			addToMap(entryMap, entry, entryIdIncludeFilter, entryIdExcludeFilter);
		}
		for (T entry : topEntriesAgo) {
			addToMap(entryMap, entry, entryIdIncludeFilter, entryIdExcludeFilter);
		}
		for (T entry : recentlyImportedEntries) {
			addToMap(entryMap, entry, entryIdIncludeFilter, entryIdExcludeFilter);
		}

		// Sort by rating
		List<T> entries = new ArrayList<T>(entryMap.values());
		Collections.sort(entries, new Comparator<T>() {

			@Override
			public int compare(T e1, T e2) {
				return Float.compare((Float) e2.getValue(), (Float) e1.getValue());
			}
		});

		// Just return top maxNum entries
		entries = entries.subList(0, Math.min(maxNum, entries.size()));

		return entries;

	}

	@Override
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getSuggestedSongs(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {

		return getSuggested(profileId, cutBetweenOnceAndLately, maxLatelyListeningTime, timeRange, timeFilter, maxNum,
				DataType.Songs, smoothed, null);
	}

	@Override
	public List<StatisticsAlbum> getSuggestedAlbums(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {

		return getSuggested(profileId, cutBetweenOnceAndLately, maxLatelyListeningTime, timeRange, timeFilter, maxNum,
				DataType.Albums, smoothed, null);
	}

	@Override
	public List<StatisticsArtist> getSuggestedArtists(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {

		return getSuggested(profileId, cutBetweenOnceAndLately, maxLatelyListeningTime, timeRange, timeFilter, maxNum,
				DataType.Artists, smoothed, null);
	}

	@Override
	public List<StatisticsGenre> getSuggestedGenres(int profileId, Date cutBetweenOnceAndLately,
			long maxLatelyListeningTime, Pair<Date, Date> timeRange, TimeFilter timeFilter, int maxNum,
			boolean smoothed) {

		return getSuggested(profileId, cutBetweenOnceAndLately, maxLatelyListeningTime, timeRange, timeFilter, maxNum,
				DataType.Genres, smoothed, null);
	}

	// *** Recently *** //

	@Override
	public List<StatisticsSong<BaseArtist, BaseAlbum>> getImportedSongs(Pair<Date, Date> timeRange, int maxNum) {
		String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.IMPORT_TIMESTAMP + " " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +
				"WHERE (s." + TblSongs.IMPORT_TIMESTAMP + " BETWEEN ? AND ?) " + // ?ts1, ?ts2
				"ORDER BY s." + TblSongs.IMPORT_TIMESTAMP + " DESC " +
				"LIMIT ?"; // ?limit

		try {
			String[] values = new String[] {
					"" + timeRange.first.getTime(), // ?ts1
					"" + timeRange.second.getTime(), // ?ts2
					"" + maxNum // ?limit
			};
			return getSongsFromSql(sql, values, DATE_PARSER);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return new ArrayList<StatisticsSong<BaseArtist, BaseAlbum>>();
		}
	}

	@Override
	public List<StatisticsAlbum> getImportedAlbums(Pair<Date, Date> timeRange, int maxNum) {
		String sql = "SELECT a." + TblAlbums.ALBUM_ID + ", a." + TblAlbums.ALBUM_NAME + ", MAX(s." + TblSongs.IMPORT_TIMESTAMP + ") AS maxTimestamp " +
				"FROM " + TblAlbums.TBL_NAME + " AS a " +
				"  JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.ALBUM_ID + " = a." + TblAlbums.ALBUM_ID + ") " +
				"WHERE (s." + TblSongs.IMPORT_TIMESTAMP + " BETWEEN ? AND ?) " + // ?ts1, ?ts2
				"GROUP BY a." + TblAlbums.ALBUM_ID + " " +
				"HAVING maxTimestamp NOT NULL " +
				"ORDER BY maxTimestamp DESC " +
				"LIMIT ?"; // ?limit

		try {
			String[] values = new String[] {
					"" + timeRange.first.getTime(), // ?ts1
					"" + timeRange.second.getTime(), // ?ts2
					"" + maxNum // ?limit
			};
			return getAlbumsFromSql(sql, values, DATE_PARSER);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return new ArrayList<StatisticsAlbum>();
		}
	}

	@Override
	public List<StatisticsArtist> getImportedArtists(Pair<Date, Date> timeRange, int maxNum) {
		String sql = "SELECT a." + TblArtists.ARTIST_ID + ", a." + TblArtists.NAME + ", MAX(s." + TblSongs.IMPORT_TIMESTAMP + ") AS maxTimestamp " +
				"FROM " + TblArtists.TBL_NAME + " AS a " +
				"  JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID + ") " +
				"WHERE (s." + TblSongs.IMPORT_TIMESTAMP + " BETWEEN ? AND ?) " + // ?ts1, ?ts2
				"GROUP BY a." + TblArtists.ARTIST_ID + " " +
				"HAVING maxTimestamp NOT NULL " +
				"ORDER BY maxTimestamp DESC " +
				"LIMIT ?";

		String[] values = new String[] {
				"" + timeRange.first.getTime(), // ?ts1
				"" + timeRange.second.getTime(), // ?ts2
				"" + maxNum // ?limit
		};
		return getArtistsFromSql(sql, values, DATE_PARSER);
	}

	@Override
	public List<StatisticsGenre> getImportedGenres(Pair<Date, Date> timeRange, int maxNum) {
		String sql = "SELECT g." + TblGenres.GENRE_ID + ", g." + TblGenres.NAME + ", MAX(s." + TblSongs.IMPORT_TIMESTAMP + ") AS maxTimestamp " +
				"FROM " + TblGenres.TBL_NAME + " AS g " +
				"  JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON (sg." + TblSongGenres.GENRE_ID + " = g." + TblGenres.GENRE_ID + ") " +
				"  JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = sg." + TblSongGenres.SONG_ID + ") " +
				"WHERE (s." + TblSongs.IMPORT_TIMESTAMP + " BETWEEN ? AND ?) " + // ?ts1, ?ts2
				"GROUP BY g." + TblGenres.GENRE_ID + " " +
				"HAVING maxTimestamp NOT NULL " +
				"ORDER BY maxTimestamp DESC " +
				"LIMIT ?";

		String[] values = new String[] {
				"" + timeRange.first.getTime(), // ?ts1
				"" + timeRange.second.getTime(), // ?ts2
				"" + maxNum // ?limit
		};
		return getGenresFromSql(sql, values, DATE_PARSER);
	}

	// *** Db access helper methods *** //

	/**
	 * Returns a list of {@link StatisticsSong}s from the given sql string. The selectionArgs are used to fill in the
	 * "?"s in your sql string.<br/>
	 * The first two columns have to be: {@link TblSongs#SONG_ID} and the data.
	 * 
	 * @param sql
	 *            The sql
	 * @param selectionArgs
	 *            The replacements for the "?"s
	 * @param dataParser
	 *            The parser which reads the data from the cursor into an {@link Object}
	 * @return The list of the {@link StatisticsSong}s
	 * @throws DataUnavailableException
	 */
	protected List<StatisticsSong<BaseArtist, BaseAlbum>> getSongsFromSql(String sql, String[] selectionArgs,
			IDataParser dataParser) throws DataUnavailableException {
		ArrayList<StatisticsSong<BaseArtist, BaseAlbum>> songs = new ArrayList<StatisticsSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, selectionArgs);

			if (!cur.moveToNext()) {
				Log.d(TAG, "No songs found");
				return songs;
			}

			// Read the ids
			LinkedHashMap<Integer, Object> ids = new LinkedHashMap<Integer, Object>();
			do {
				int songId = cur.getInt(0);
				Object data = dataParser.getData(cur, 1);

				ids.put(songId, data);
			} while (cur.moveToNext());

			// Read the base songs
			List<BaseSong<BaseArtist, BaseAlbum>> baseSongs = sqlDbDataPortal.batchGetBaseSongByIds(ids.keySet());
			Map<Integer, BaseSong<BaseArtist, BaseAlbum>> baseSongsById = new HashMap<Integer, BaseSong<BaseArtist, BaseAlbum>>(
					baseSongs.size());
			for (BaseSong<BaseArtist, BaseAlbum> song : baseSongs) {
				baseSongsById.put(song.getId(), song);
			}

			// Transform the base songs into statistics songs
			for (Map.Entry<Integer, Object> entry : ids.entrySet()) {
				songs.add(new StatisticsSong<BaseArtist, BaseAlbum>(baseSongsById.get(entry.getKey()), entry.getValue()));
			}

			Log.d(TAG, "Number of songs: " + songs.size());
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

		return songs;
	}

	/**
	 * Returns a list of {@link StatisticsAlbum}s from the given sql string. The selectionArgs are used to fill in the
	 * "?"s in your sql string.<br/>
	 * The first three columns have to be: {@link TblAlbums#ALBUM_ID}, @{link {@link TblAlbums#ALBUM_NAME} and the
	 * calculated rating.
	 * 
	 * @param sql
	 *            The sql
	 * @param selectionArgs
	 *            The replacements for the "?"s
	 * @param dataParser
	 *            The parser which reads the data from the cursor into an {@link Object}
	 * @return The list of the {@link StatisticsAlbum}s
	 * @throws DataUnavailableException
	 */
	protected List<StatisticsAlbum> getAlbumsFromSql(String sql, String[] selectionArgs, IDataParser dataParser)
			throws DataUnavailableException {
		List<StatisticsAlbum> albums = new ArrayList<StatisticsAlbum>();
		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, selectionArgs);

			if (!cur.moveToNext()) {
				Log.d(TAG, "No statistics albums found");
				return albums;
			}

			int numFound = 0;
			do {
				numFound++; // TODO @sämy: make this faster!
				albums.add(new StatisticsAlbum(sqlDbDataPortal.getListAlbum(cur.getInt(0)), dataParser.getData(cur, 2)));
			} while (cur.moveToNext());

			Log.d(TAG, "Number of statistics albums: " + numFound);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

		return albums;
	}

	/**
	 * Returns a list of {@link StatisticsArtist}s from the given sql string. The selectionArgs are used to fill in the
	 * "?"s in your sql string.<br/>
	 * The first three columns have to be: {@link TblArtists#ARTIST_ID}, @{link TblArtists#NAME} and the calculated
	 * rating.
	 * 
	 * @param sql
	 *            The sql
	 * @param selectionArgs
	 *            The replacements for the "?"s
	 * @param dataParser
	 *            The parser which reads the data from the cursor into an {@link Object}
	 * @return The list of the {@link StatisticsArtist}s
	 */
	protected List<StatisticsArtist> getArtistsFromSql(String sql, String[] selectionArgs, IDataParser dataParser) {
		List<StatisticsArtist> artists = new ArrayList<StatisticsArtist>();
		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, selectionArgs);

			if (!cur.moveToNext()) {
				Log.d(TAG, "No statistics artists found");
				return artists;
			}

			int numFound = 0;
			do {
				numFound++;
				artists.add(new StatisticsArtist(cur.getInt(0), cur.getString(1), dataParser.getData(cur, 2)));
			} while (cur.moveToNext());

			Log.d(TAG, "Number of statistics artists: " + numFound);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

		return artists;
	}

	/**
	 * Returns a list of {@link StatisticsGenre}s from the given sql string. The selectionArgs are used to fill in the
	 * "?"s in your sql string.<br/>
	 * The first three columns have to be: {@link TblGenres#GENRE_ID}, @{link {@link TblGenres#NAME} and the calculated
	 * rating.
	 * 
	 * @param sql
	 *            The sql
	 * @param selectionArgs
	 *            The replacements for the "?"s
	 * @param dataParser
	 *            The parser which reads the data from the cursor into an {@link Object}
	 * @return The list of the {@link StatisticsGenre}s
	 */
	protected List<StatisticsGenre> getGenresFromSql(String sql, String[] selectionArgs, IDataParser dataParser) {
		List<StatisticsGenre> genres = new ArrayList<StatisticsGenre>();
		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, selectionArgs);

			if (!cur.moveToNext()) {
				Log.d(TAG, "No statistics genres found");
				return genres;
			}

			int numFound = 0;
			do {
				numFound++;
				genres.add(new StatisticsGenre(cur.getInt(0), cur.getString(1), dataParser.getData(cur, 2)));
			} while (cur.moveToNext());

			Log.d(TAG, "Number of statistics genres: " + numFound);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

		return genres;
	}

	// *** Rating *** //
	@Override
	public long writeRatingEntry(int profileId, int songId, Date timestamp, double rating, double weight,
			RatingSource ratingSource) throws DataWriteException {

		ContentValues initialValues = sqlDbDataPortal.createContentValues();
		initialValues.put(TblRating.PROFILE_ID, profileId);
		initialValues.put(TblRating.TIMESTAMP, timestamp.getTime());
		initialValues.put(TblRating.SONG_ID, songId);
		initialValues.put(TblRating.RATING, rating);
		initialValues.put(TblRating.WEIGHT, weight);
		initialValues.put(TblRating.RATING_SOURCE, ratingSource.value());

		long id = sqlDbDataPortal.insertOrThrow(TblRating.TBL_NAME, initialValues);

		Log.d(TAG, "Inserted rating " + id);

		return id;
	}

	// *** Other *** //

	@Override
	public Date getLastPlayedTime(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException {
		String sql = "SELECT MAX(pl." + TblPlayLog.TIMESTAMP + ") " +
				"FROM " + TblPlayLog.TBL_NAME + " AS pl " +
				"WHERE (pl." + TblPlayLog.SONG_ID + " = ?) ";

		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, new String[] { "" + song.getId() });

			if (!cur.moveToNext()) {
				throw new DataUnavailableException();
			}

			return new Date(cur.getLong(0));
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public Date getLastPlayedTime(BaseArtist artist) throws DataUnavailableException {
		String sql = "SELECT MAX(pl." + TblPlayLog.TIMESTAMP + ") " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +
				"  JOIN " + TblPlayLog.TBL_NAME + " AS pl ON (pl." + TblPlayLog.SONG_ID + " = s." + TblSongs.SONG_ID + ") " +
				"WHERE (s." + TblSongs.ARTIST_ID + " = ?) ";

		ICursor cur = null;
		try {
			cur = sqlDbDataPortal.execSelect(sql, new String[] { "" + artist.getId() });

			if (!cur.moveToNext()) {
				throw new DataUnavailableException();
			}

			return new Date(cur.getLong(0));
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getLongNotRatedSongs(int num, int longNotRatedThreshold)
			throws DataUnavailableException {

		String sql = "SELECT s.songId " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +
				"  LEFT OUTER JOIN (" +
				"    SELECT r." + TblRating.SONG_ID + " AS songId, MAX(r." + TblRating.TIMESTAMP + ") AS newestTimestamp " +
				"    FROM " + TblRating.TBL_NAME + " AS r " +
				"    GROUP BY r.songId " +
				"  ) AS rMax ON (rMax.songId = s." + TblSongs.SONG_ID + ") " +
				"WHERE (rMax.newestTimestamp < ?) " + // ?threshold
				"  OR (rMax.newestTimestamp IS NULL) " +
				"ORDER BY RANDOM() " +
				"LIMIT ?"; // ?limit

		ICursor cur = null;
		try {
			Date d = new Date();
			long thresholdTimestamp = d.getTime() - ((long) longNotRatedThreshold * 60 * 60 * 1000); // move from relative to fixed timestamp (longNotRatedThreshold: h -> ms)

			cur = sqlDbDataPortal.execSelect(sql, new String[] {
					"" + thresholdTimestamp, // ?threshold
					"" + num // ?limit
			});

			// Get the song ids
			Set<Integer> ids = new HashSet<Integer>(num);
			while (cur.moveToNext()) {
				ids.add(cur.getInt(0));
			}

			// Fetch the songs
			return sqlDbDataPortal.batchGetBaseSongByIds(ids);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * @see #getIdsStmt(List, IIdParser)
	 */
	private SubStatement getIdsStmt(List<Integer> ids) {
		return getIdsStmt(ids, new IIdParser<Integer>() {

			@Override
			public int getId(Integer item) {
				return item;
			}

		});
	}

	/**
	 * @see #getIdsStmt(List, IIdParser)
	 */
	private <T extends BaseSong<BaseArtist, BaseAlbum>> SubStatement getSongIdsStmt(List<T> songs) {
		return getIdsStmt(songs, new IIdParser<T>() {

			@Override
			public int getId(T item) {
				return item.getId();
			}

		});
	}

	/**
	 * @see #getIdsStmt(List, IIdParser)
	 */
	private <T extends BaseArtist> SubStatement getArtistIdsStmt(List<T> artists) {
		return getIdsStmt(artists, new IIdParser<T>() {

			@Override
			public int getId(T item) {
				return item.getId();
			}

		});
	}

	/**
	 * Returns a comma-seperated list of "?" (for each id one) and the ids as a sub statement.
	 * 
	 * @param items
	 *            The items
	 * @param idsParser
	 *            The parser which mapps an item to an integer
	 * @return
	 */
	private <T> SubStatement getIdsStmt(List<T> items, IIdParser<T> idsParser) {
		StringBuffer sqlSB = new StringBuffer();
		List<String> values = new ArrayList<String>(items.size());

		for (T item : items) {
			values.add("" + idsParser.getId(item));
			sqlSB.append("?,");
		}
		sqlSB.deleteCharAt(sqlSB.length() - 1); // remove last ','

		return new SubStatement(sqlSB.toString(), values);
	}

	private interface IIdParser<T> {

		public int getId(T item);
	}

	// *** IDataParser *** //

	/**
	 * An interface which has a method to convert the data stored in the given cursor and column into an {@link Object}.
	 */
	public interface IDataParser {

		public Object getData(ICursor cur, int column);
	}

	protected static IDataParser FLOAT_PARSER = new IDataParser() {

		@Override
		public Object getData(ICursor cur, int column) {
			return cur.getFloat(column);
		}
	};
	protected static IDataParser DATE_PARSER = new IDataParser() {

		@Override
		public Object getData(ICursor cur, int column) {
			return new Date(cur.getLong(column));
		}
	};

	/**
	 * Represents an sql statement with ? in it and the corresponding values.
	 * 
	 * @author saemy
	 * 
	 */
	private final class SubStatement {

		private final String sql;
		private final List<String> values;

		public SubStatement(String sql, String[] values) {
			this(sql, Arrays.asList(values));
		}

		public SubStatement(String sql, List<String> values) {
			this.sql = sql;
			this.values = values;
		}

		public String getSql() {
			return sql;
		}

		public List<String> getValues() {
			return values;
		}

		@Override
		public String toString() {
			return getSql();
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (!(other instanceof DbStatisticsHelper.TopEntriesAgoCacheEntry)) {
				return false;
			}

			@SuppressWarnings("unchecked")
			SubStatement otherS = (SubStatement) other;

			boolean equals = sql.equals(otherS.sql);
			equals &= values.size() == otherS.values.size();

			for (int i = 0; i < values.size() && !equals; ++i) {
				equals &= values.get(i) == otherS.values.get(i);
			}

			return equals;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}
	}

	/**
	 * Represents a call to
	 * {@link DbStatisticsHelper#getTopEntriesAgo(int, int, Pair, ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter, boolean, RatingSource[], String, DataType)}
	 */
	private final class TopEntriesAgoCacheEntry {

		private final int profileId;
		private final Pair<Date, Date> timeRange;
		private final TimeFilter timeFilter;
		private final boolean smoothed;
		private final RatingSource[] ratingSources;
		private final DataType dataType;
		private final SubStatement additionalWhere;

		public TopEntriesAgoCacheEntry(int profileId, Pair<Date, Date> timeRange, TimeFilter timeFilter,
				boolean smoothed, RatingSource[] ratingSources, DataType dataType, SubStatement additionalWhere) {
			this.profileId = profileId;
			this.timeRange = timeRange;
			this.timeFilter = timeFilter;
			this.smoothed = smoothed;
			this.ratingSources = ratingSources;
			this.dataType = dataType;
			this.additionalWhere = additionalWhere;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (!(other instanceof DbStatisticsHelper.TopEntriesAgoCacheEntry)) {
				return false;
			}

			@SuppressWarnings("unchecked")
			TopEntriesAgoCacheEntry otherC = (TopEntriesAgoCacheEntry) other;

			long diffStart = Math.abs(otherC.timeRange.first.getTime() - timeRange.first.getTime()) / 1000 / 60 / 60; // [h]		
			long diffEnd = Math.abs(otherC.timeRange.second.getTime() - timeRange.second.getTime()) / 1000 / 60 / 60; // [h]		
			boolean timeRangeApproxTheSame = (diffStart < 24) && (diffEnd < 24);

			boolean equals = profileId == otherC.profileId;
			equals &= timeRangeApproxTheSame;
			equals &= timeFilter.equals(otherC.timeFilter);
			equals &= (smoothed == otherC.smoothed);
			equals &= ratingSources.length == otherC.ratingSources.length; // check these arrays completely, but equals does not work..
			equals &= dataType.equals(otherC.dataType);
			equals &= additionalWhere.equals(otherC.additionalWhere);

			return equals;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

}
