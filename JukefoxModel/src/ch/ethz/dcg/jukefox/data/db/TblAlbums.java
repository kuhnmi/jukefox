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

public class TblAlbums {

	public final static String TBL_NAME = "tblAlbums";

	public final static String ALBUM_ID = "albumId";
	public final static String ALBUM_NAME = "albumName";
	public final static String ARTIST_SET_ID = "artistSetId";
	public final static String LOW_RES_COVER_PATH = "lowResCoverPath";
	public final static String HIGH_RES_COVER_PATH = "highResCoverPath";
	public final static String COLOR = "color";
	public final static String PCA_COORDS_X = "pcaX";
	public final static String PCA_COORDS_Y = "pcaY";
	public final static String ALBUM_STATUS = "albumStatus";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				ALBUM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				ALBUM_NAME + " TEXT, " +
				ARTIST_SET_ID + " INTEGER KEY, " +
				LOW_RES_COVER_PATH + " TEXT, " +
				HIGH_RES_COVER_PATH + " TEXT, " +
				COLOR + " INTEGER, " +
				PCA_COORDS_X + " FLOAT, " +
				PCA_COORDS_Y + " FLOAT, " +
				ALBUM_STATUS + " INTEGER KEY, " +
				"UNIQUE (" + ALBUM_NAME + ", " + ARTIST_SET_ID + ")" + ")";
		return sql;
	}
}
