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

public class TblLogEntry {

	public final static String TBL_NAME = "tblLogEntry";

	public final static String LOGENTRY_ID = "logEntryId";
	public final static String PROFILE_ID = "profileId";
	public final static String LOG_STRING = "log";

	public static String getCreateSql8() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" +
				LOGENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				PROFILE_ID + " INTEGER KEY, " +
				LOG_STRING + " TEXT" +
				")";
		return sql;
	}

}
