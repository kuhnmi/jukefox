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

public class TblPlayLog {

	public final static String TBL_NAME = "tblPlayLog";

	public final static String PLAY_LOG_ID = "playLogId";
	public final static String PROFILE_ID = "profileId";
	public final static String TIMESTAMP = "timestamp";
	public final static String TIME_ZONE_OFFSET = "timeZoneOff";
	public final static String HOUR = "hour";
	public final static String DAY = "day";
	public final static String SONG_ID = "songId";
	public final static String ARTIST_ID = "artistId";
	public final static String ME_SONG_ID = "meSongId";
	public final static String ME_ARTIST_ID = "meArtistId";
	public final static String PLAY_MODE = "playMode";
	public static final String SKIPPED = "skipped";
	public final static String SONG_SOURCE = "songSource";
	public final static String CONTEXT = "context";
	public final static String PLAYBACK_POSITION = "playbackPosition";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				PLAY_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				PROFILE_ID + " INTEGER, " +
				TIMESTAMP + " INTEGER KEY, " +
				TIME_ZONE_OFFSET + " INTEGER, " +
				HOUR + " INTEGER KEY, " +
				DAY + " INTEGER KEY, " +
				SONG_ID + " INTEGER, " +
				ARTIST_ID + " INTEGER, " +
				ME_SONG_ID + " INTEGER, " +
				ME_ARTIST_ID + " INTEGER, " +
				PLAY_MODE + " INTEGER KEY, " +
				SKIPPED + " INTEGER KEY, " +
				SONG_SOURCE + " INTEGER KEY, " +
				CONTEXT + " TEXT)";
		return sql;
	}

	// -----=== ALTER TABLE VER. 3 to VER. 4 ===-----

	public static List<String> getConvertTblPlayLogQueries4() {
		List<String> sql = new ArrayList<String>();
		sql.add("ALTER TABLE " + TBL_NAME + " RENAME TO " + TBL_NAME + "_backup");
		sql.add(getCreateSql());
		sql.add("INSERT INTO " + TBL_NAME + " (" + PLAY_LOG_ID + ", " + TIMESTAMP + ", " +
				TIME_ZONE_OFFSET + ", " + HOUR + ", " + DAY + ", " + ME_SONG_ID + ", " +
				ME_ARTIST_ID + ", " + PLAY_MODE + ", " + SKIPPED + ", " + SONG_SOURCE + ") " +
				"SELECT " + PLAY_LOG_ID + ", " + TIMESTAMP + ", " +
				TIME_ZONE_OFFSET + ", " + HOUR + ", " + DAY + ", " + ME_SONG_ID + ", " +
				ME_ARTIST_ID + ", " + PLAY_MODE + ", " + SKIPPED + ", " + SONG_SOURCE + " FROM " +
				TBL_NAME + "_backup");
		// Update song_id & artist_id
		sql.add("UPDATE " + TBL_NAME + " SET " + SONG_ID + " = ( SELECT " +
				TblSongs.TBL_NAME + "." + TblSongs.SONG_ID + " FROM " + TblSongs.TBL_NAME + " WHERE " +
				TblSongs.TBL_NAME + "." + TblSongs.ME_SONG_ID + "=" + TBL_NAME + "." + ME_SONG_ID + " ), " +
				ARTIST_ID + " = ( SELECT " + TblSongs.TBL_NAME + "." + TblSongs.ARTIST_ID + " FROM " +
				TblSongs.TBL_NAME + " WHERE " + TblSongs.TBL_NAME + "." + TblSongs.ME_SONG_ID + "=" +
				TBL_NAME + "." + ME_SONG_ID + " ) WHERE EXISTS ( SELECT * FROM " + TblSongs.TBL_NAME +
				" WHERE " + TblSongs.TBL_NAME + "." + TblSongs.ME_SONG_ID + "=" + TBL_NAME + "." + ME_SONG_ID + " )");
		sql.add("UPDATE " + TBL_NAME + " SET " + PROFILE_ID + "=1");
		// we have access to all contexts ... when we do not delete
		// TblPlayLog_backup
		// TODO create a context string and update the new table?
		sql.add("DROP TABLE " + TBL_NAME + "_backup");

		return sql;
	}

	// -----=== ALTER TABLE VER. 4 to VER. 5 ===-----

	/**
	 * Diff to previous version: Added playback position column.
	 * 
	 * @return The create table sql
	 */
	private static String getCreateSql5() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				PLAY_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				PROFILE_ID + " INTEGER, " +
				TIMESTAMP + " INTEGER KEY, " +
				TIME_ZONE_OFFSET + " INTEGER, " +
				HOUR + " INTEGER KEY, " +
				DAY + " INTEGER KEY, " +
				SONG_ID + " INTEGER, " +
				ARTIST_ID + " INTEGER, " +
				ME_SONG_ID + " INTEGER, " +
				ME_ARTIST_ID + " INTEGER, " +
				PLAY_MODE + " INTEGER KEY, " +
				SONG_SOURCE + " INTEGER KEY, " +
				CONTEXT + " TEXT, " +
				SKIPPED + " INTEGER KEY, " +
				PLAYBACK_POSITION + " INTEGER NOT NULL DEFAULT 0)";
		return sql;
	}

	/**
	 * Create new version of the playlog table. We have to completely recreate the table since removing columns and
	 * doing sub-selects in updates is not supported by SQLite.
	 * 
	 * @return The sql statements for the version update of the table
	 */
	public static List<String> getConvertTblPlayLogQueries5() {
		List<String> sql = new ArrayList<String>();
		sql.add("ALTER TABLE " + TBL_NAME + " RENAME TO " + TBL_NAME + "_backup");
		sql.add(getCreateSql5());
		sql.add("INSERT INTO " + TBL_NAME + " " +
				"SELECT b." + PLAY_LOG_ID + ", b." + PROFILE_ID + ", b." + TIMESTAMP + ", " +
				"b." + TIME_ZONE_OFFSET + ", b." + HOUR + ", b." + DAY + ", b." + SONG_ID + ", " +
				"b." + ARTIST_ID + ", b." + ME_SONG_ID + ", b." + ME_ARTIST_ID + ", b." + PLAY_MODE + ", " +
				"b." + SONG_SOURCE + ", b." + CONTEXT + ", b." + SKIPPED + ", (CASE WHEN b." + SKIPPED + "=0 THEN 1 ELSE 0.5 END)*s.duration " + // if skipped assume 50% playback time
				"FROM " + TBL_NAME + "_backup AS b " +
				"JOIN " + TblSongs.TBL_NAME + " AS s ON ((s." + TblSongs.ME_SONG_ID + " != 0) AND (s." + TblSongs.ME_SONG_ID + " = b." + TblPlayLog.ME_SONG_ID + ")) " +
				"OR ((s." + TblSongs.ME_SONG_ID + " = 0) AND (s." + TblSongs.SONG_ID + " = b." + TblPlayLog.SONG_ID + ")) " +
				"GROUP BY b." + PLAY_LOG_ID); // To not get multiple entries when there are multiple songs with the same (me)SongId. (And right joins aren't supported by now.)
		sql.add("DROP TABLE " + TBL_NAME + "_backup");

		return sql;
	}
}
