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

public class TblArtists {

	public final static String TBL_NAME = "tblArtists";

	public final static String ARTIST_ID = "artistId";
	public final static String NAME = "name";
	public final static String ME_ARTIST_ID = "meArtistId";
	public final static String ME_NAME = "meArtistName";
	public final static String IS_FAMOUS_ARTIST = "isFamousArtist";
	public final static String IS_IN_COLLECTION = "isInCollection";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				ARTIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				NAME + " TEXT UNIQUE, " +
				ME_ARTIST_ID + " INTEGER, " +
				ME_NAME + " TEXT, " +
				IS_FAMOUS_ARTIST + " BOOLEAN KEY, " +
				IS_IN_COLLECTION + " BOOLEAN KEY" + ")";
		return sql;
	}

}
