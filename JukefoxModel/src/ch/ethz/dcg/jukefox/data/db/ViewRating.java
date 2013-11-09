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
import java.util.List;

@Deprecated
public class ViewRating {

	public final static String VIEW_NAME = "viewRating";

	public final static String PROFILE_ID = TblPlayLog.PROFILE_ID;
	public final static String TIMESTAMP = TblPlayLog.TIMESTAMP;
	public final static String TIME_ZONE_OFFSET = TblPlayLog.TIME_ZONE_OFFSET;
	public final static String HOUR = TblPlayLog.HOUR;
	public final static String DAY = TblPlayLog.DAY;
	public final static String SONG_ID = TblPlayLog.SONG_ID;
	public final static String ARTIST_ID = TblPlayLog.ARTIST_ID;
	public final static String ME_SONG_ID = TblPlayLog.ME_SONG_ID;
	public final static String ME_ARTIST_ID = TblPlayLog.ME_ARTIST_ID;
	public final static String PLAY_MODE = TblPlayLog.PLAY_MODE;
	public final static String SONG_SOURCE = TblPlayLog.SONG_SOURCE;
	public final static String CONTEXT = TblPlayLog.CONTEXT;
	public final static String PLAYBACK_POSITION = TblPlayLog.PLAYBACK_POSITION;
	public final static String RATING = "rating";

	public static String getCreateSql() {
		String percent = "(l." + TblPlayLog.PLAYBACK_POSITION + " / CAST(s." + TblSongs.DURATION + " AS REAL))";

		/* Rating function: (x is percent of song which was played)
		 * 
		 * 		  (	-1,     if x in [0, 0.25)
		 * f(x) = { 4x - 2, if x in [0.25, 0.75)  // Linear growth between 1/4 & 3/4
		 *        ( 1,	    if x in [0.75, 1]
		 */
		String sql = "CREATE VIEW " + VIEW_NAME + " AS " +
				"SELECT l." + TblPlayLog.TIMESTAMP + ", " +
				"l." + TblPlayLog.PROFILE_ID + ", " +
				"l." + TblPlayLog.TIME_ZONE_OFFSET + ", " +
				"l." + TblPlayLog.HOUR + ", " +
				"l." + TblPlayLog.DAY + ", " +
				"l." + TblPlayLog.SONG_ID + ", " +
				"l." + TblPlayLog.ARTIST_ID + ", " +
				"l." + TblPlayLog.ME_SONG_ID + ", " +
				"l." + TblPlayLog.ME_ARTIST_ID + ", " +
				"l." + TblPlayLog.PLAY_MODE + ", " +
				"l." + TblPlayLog.SONG_SOURCE + ", " +
				"l." + TblPlayLog.CONTEXT + ", " +
				"l." + TblPlayLog.PLAYBACK_POSITION + ", " +
				"s." + TblSongs.DURATION + ", " +
				"MIN(MAX(4 * " + percent + " - 2, -1), 1) AS " + RATING + " " + // f(x)
				"FROM " + TblPlayLog.TBL_NAME + " AS l " +
				"INNER JOIN " + TblSongs.TBL_NAME + " AS s ON ((s." + TblSongs.ME_SONG_ID + " != 0) AND (s." + TblSongs.ME_SONG_ID + " = l." + TblPlayLog.ME_SONG_ID + ")) " +
				"OR ((s." + TblSongs.ME_SONG_ID + " = 0) AND (s." + TblSongs.SONG_ID + " = l." + TblPlayLog.SONG_ID + ")) ";
		return sql;
	}

	/**
	 * Made the inner join only use the {@link ViewRating#SONG_ID}.
	 * 
	 * @return The sql statements for the update
	 */
	public static List<String> getUpdateTo6() {
		List<String> sql = new ArrayList<String>();
		sql.add("DROP VIEW " + ViewRating.VIEW_NAME);

		/* Rating function: (x is percent of song which was played)
		 * 
		 * 		  (	-1,     if x in [0, 0.25)
		 * f(x) = { 4x - 2, if x in [0.25, 0.75)  // Linear growth between 1/4 & 3/4
		 *        ( 1,	    if x in [0.75, 1]
		 */
		String percent = "(l." + TblPlayLog.PLAYBACK_POSITION + " / CAST(s." + TblSongs.DURATION + " AS REAL))";
		sql.add("CREATE VIEW " + VIEW_NAME + " AS " +
				"SELECT l." + TblPlayLog.TIMESTAMP + ", " +
				"l." + TblPlayLog.PROFILE_ID + ", " +
				"l." + TblPlayLog.TIME_ZONE_OFFSET + ", " +
				"l." + TblPlayLog.HOUR + ", " +
				"l." + TblPlayLog.DAY + ", " +
				"l." + TblPlayLog.SONG_ID + ", " +
				"l." + TblPlayLog.ARTIST_ID + ", " +
				"l." + TblPlayLog.ME_SONG_ID + ", " +
				"l." + TblPlayLog.ME_ARTIST_ID + ", " +
				"l." + TblPlayLog.PLAY_MODE + ", " +
				"l." + TblPlayLog.SONG_SOURCE + ", " +
				"l." + TblPlayLog.CONTEXT + ", " +
				"l." + TblPlayLog.PLAYBACK_POSITION + ", " +
				"s." + TblSongs.DURATION + ", " +
				"MIN(MAX(4 * " + percent + " - 2, -1), 1) AS " + RATING + " " + // f(x)
				"FROM " + TblPlayLog.TBL_NAME + " AS l " +
				"INNER JOIN " + TblSongs.TBL_NAME + " AS s ON (s." + TblSongs.SONG_ID + " = l." + TblPlayLog.SONG_ID + ") ");

		return sql;
	}

	/**
	 * Drops this view, since it is not needed anymore.
	 * 
	 * @return
	 */
	public static List<String> getDrop9() {
		List<String> sql = new ArrayList<String>();
		sql.add("DROP VIEW " + ViewRating.VIEW_NAME);
		return sql;
	}
}
