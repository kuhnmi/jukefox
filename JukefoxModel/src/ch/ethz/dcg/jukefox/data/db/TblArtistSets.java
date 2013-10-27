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

public class TblArtistSets {

	public final static String TBL_NAME = "tblArtistSets";

	public final static String ARTIST_SET_ID = "artistSetId";
	public final static String ARTIST_SET_HASH = "artistSetHash";
	public final static String ARTIST_ID = "artistId";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" + ARTIST_SET_ID + " INTEGER KEY, " + ARTIST_SET_HASH
				+ " INTEGER KEY, " + ARTIST_ID + " INTEGER KEY, " + "UNIQUE (" + ARTIST_SET_ID + ", " + ARTIST_ID + ")"
				+ ")";
		return sql;
	}

}
