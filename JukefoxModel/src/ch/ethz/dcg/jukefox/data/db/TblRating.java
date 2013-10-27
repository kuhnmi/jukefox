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
import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;

public class TblRating {

	public final static String TBL_NAME = "tblRating";

	public final static String RATING_ID = "ratingId";
	public final static String PROFILE_ID = "profileId";
	public final static String TIMESTAMP = "timestamp";
	public final static String SONG_ID = "songId";
	public final static String RATING = "rating";
	public final static String WEIGHT = "weight";
	public final static String RATING_SOURCE = "ratingSource";
	public final static String HOUR_OF_THE_DAY = "hourOfTheDay";
	public final static String DAY_OF_THE_WEEK = "dayOfTheWeek";

	public static String getCreateSql7() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				RATING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				PROFILE_ID + " INTEGER KEY, " +
				TIMESTAMP + " INTEGER KEY, " +
				SONG_ID + " INTEGER KEY, " +
				RATING + " FLOAT, " +
				WEIGHT + " FLOAT, " +
				RATING_SOURCE + " INTEGER KEY, " +
				"UNIQUE (" + PROFILE_ID + ", " + TIMESTAMP + ", " + SONG_ID + ") " +
				")";
		return sql;
	}

	@SuppressWarnings("deprecation")
	public static List<String> getCreateRatingsFromPlayLog7Sql() {
		List<String> sql = new ArrayList<String>();
		sql.add(String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) " +
				"SELECT %s, %s, %s, %s, %d, %d " +
				"FROM " + ViewRating.VIEW_NAME,
				TBL_NAME, PROFILE_ID, TIMESTAMP, SONG_ID, RATING, WEIGHT, RATING_SOURCE,
				ViewRating.PROFILE_ID, ViewRating.TIMESTAMP, ViewRating.SONG_ID, ViewRating.RATING, 1,
				RatingSource.Playlog.value()));
		return sql;
	}

	/**
	 * Adds the columns {@link #HOUR_OF_THE_DAY} and {@link #DAY_OF_THE_WEEK} to the table.
	 * 
	 * @return
	 */
	public static List<String> getUpdateTo9() {
		List<String> sql = new LinkedList<String>();
		sql.add("ALTER TABLE " + TBL_NAME + " ADD COLUMN " + HOUR_OF_THE_DAY + " INTEGER KEY;");
		sql.add("ALTER TABLE " + TBL_NAME + " ADD COLUMN " + DAY_OF_THE_WEEK + " INTEGER KEY;");
		sql.add("UPDATE " + TBL_NAME + " SET " +
				HOUR_OF_THE_DAY + "=(strftime('%H', " + TIMESTAMP + "/1000, 'unixepoch')), " +
				DAY_OF_THE_WEEK + "=(strftime('%w', " + TIMESTAMP + "/1000, 'unixepoch')) ");
		return sql;
	}
}
