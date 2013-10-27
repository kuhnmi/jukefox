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

public class TblSongs {

	public final static String TBL_NAME = "tblSongs";

	public final static String SONG_ID = "songId";
	public final static String NAME = "name";
	public final static String ME_SONG_ID = "meSongId";
	public final static String ME_NAME = "meName";
	public final static String ARTIST_ID = "artistId";
	public final static String ALBUM_ID = "albumId";
	public final static String TRACK_NR = "trackNr";
	public final static String DATA = "data";
	public final static String DURATION = "duration";
	public final static String SONG_PCA_X = "songPcaX";
	public final static String SONG_PCA_Y = "songPcaY";
	public final static String SONG_STATUS = "songStatus";
	public final static String IMPORT_TIMESTAMP = "importTimestamp";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				NAME + " TEXT, " +
				ME_SONG_ID + " INTEGER, " +
				ME_NAME + " TEXT, " +
				ARTIST_ID + " INTEGER KEY, " +
				ALBUM_ID + " INTEGER KEY, " +
				TRACK_NR + " INTEGER, " +
				DATA + " TEXT NOT NULL UNIQUE, " +
				DURATION + " INTEGER, " +
				SONG_PCA_X + " FLOAT, " +
				SONG_PCA_Y + " FLOAT, " +
				SONG_STATUS + " INTEGER KEY" + ")";
		return sql;
	}

	// -----=== ALTER TABLE VER. 3 to VER. 4 ===-----

	public static List<String> getConvertTblSongsQueries4() {
		List<String> sql = new ArrayList<String>();
		sql.add("ALTER TABLE " + TBL_NAME + " RENAME TO " + TBL_NAME + "_backup");
		sql.add(getCreateSql());
		sql.add("INSERT INTO " + TBL_NAME + " SELECT " + SONG_ID + ", " + NAME + ", " +
				ME_SONG_ID + ", " + ME_NAME + ", " + ARTIST_ID + ", " + ALBUM_ID + ", " +
				TRACK_NR + ", " + DATA + ", " + DURATION + ", " + SONG_PCA_X + ", " +
				SONG_PCA_Y + ", " + SONG_STATUS + " FROM " + TBL_NAME + "_backup");
		sql.add("DROP TABLE " + TBL_NAME + "_backup");
		return sql;
	}

	// -----=== ALTER TABLE VER. 5 to VER. 6 ===-----

	/**
	 * Diff to previous version: Added import timestamp column.
	 * 
	 * @return The update statements for this table
	 */
	public static List<String> getConvertTblSongsQueries6() {
		List<String> sql = new ArrayList<String>();
		sql.add("ALTER TABLE " + TBL_NAME + " ADD COLUMN " + IMPORT_TIMESTAMP + " INTEGER NOT NULL DEFAULT 0");

		return sql;
	}

}
