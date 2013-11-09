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

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class TblArtistCoords {

	private final static String TAG = TblArtistCoords.class.getSimpleName();

	public final static String TBL_NAME = "tblArtistCoords";

	public final static String ARTIST_ID = "artistId";
	public final static String ME_ARTIST_ID = "meArtistId";
	public final static String COORD_PREFIX = "c";

	public static String getCreateSql() {
		/* Never change this statement! Create a getCreateSql[Version] method with the changes in there.
		 * Since an upgrade from an old db-version to a newer one runs through all versions in between, we must
		 * ensure, that the db is in the state of the actual version at every update point. Since getCreateSql is called 
		 * in version 1 we can't ensure this, if we update the table in here... 
		 */
		String sql = "CREATE TABLE " + TBL_NAME + " (" + ARTIST_ID + " INTEGER PRIMARY KEY, "
				+ DbUtils.getCoordStructureString(COORD_PREFIX) + ")";
		Log.v(TAG, sql);
		return sql;
	}

	public static String getAddMeArtistIdColumnString() {
		return "ALTER TABLE " + TBL_NAME + " ADD COLUMN " + ME_ARTIST_ID + " INTEGER";
	}

	private static String getCreateSql6() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" + ME_ARTIST_ID + " INTEGER PRIMARY KEY, "
				+ DbUtils.getCoordStructureString(COORD_PREFIX) + ")";
		Log.v(TAG, sql);
		return sql;
	}

	public static List<String> getRemoveArtistIdColumnString() {
		List<String> sql = new ArrayList<String>();
		sql.add("CREATE TEMPORARY TABLE " + TBL_NAME + "_backup(" + ARTIST_ID + "," + ME_ARTIST_ID + "," + DbUtils
				.getCoordString(COORD_PREFIX) + ")");
		sql.add("INSERT INTO " + TBL_NAME + "_backup SELECT " + ARTIST_ID + "," + ME_ARTIST_ID + "," + DbUtils
				.getCoordString(COORD_PREFIX) + " FROM " + TBL_NAME);
		sql.add("DROP TABLE " + TBL_NAME);
		sql.add(getCreateSql6());
		sql.add("INSERT INTO " + TBL_NAME + " SELECT " + ME_ARTIST_ID + ", "
				+ DbUtils.getCoordStructureString(COORD_PREFIX) + " FROM " + TBL_NAME + "_backup");
		sql.add("DROP TABLE " + TBL_NAME + "_backup");
		return sql;
	}

}
