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

public class TblTags {

	public final static String TBL_NAME = "tblTags";

	public final static String TAG_ID = "tagId";
	public final static String ME_TAG_ID = "meTagId";
	public final static String NAME = "name";
	public final static String MEAN_PLSA_PROB = "meanPlsaProb";
	public final static String MEAN_PCA_SPACE_X = "meanPcaSpaceX";
	public final static String MEAN_PCA_SPACE_Y = "meanPcaSpaceY";
	public final static String VARIANCE_PLSA_PROB = "variancePlsaProb";
	public final static String VARIANCE_PCA_SPACE = "variancePcaSpace";
	public final static String IS_RELEVANT = "isRelevant";
	public final static String IS_MAP_TAG = "isMapTag";
	public final static String COORD_PREFIX = "c";

	public static String getCreateSql() {
		String sql = "CREATE TABLE " + TBL_NAME + " (" + TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ME_TAG_ID
				+ " INTEGER KEY, " + NAME + " TEXT UNIQUE, " + MEAN_PLSA_PROB + " FLOAT, " + MEAN_PCA_SPACE_X
				+ " FLOAT, " + MEAN_PCA_SPACE_Y + " FLOAT, " + VARIANCE_PLSA_PROB + " FLOAT KEY, " + VARIANCE_PCA_SPACE
				+ " FLOAT KEY, " + IS_RELEVANT + " BOOLEAN KEY, " + IS_MAP_TAG + " BOOLEAN KEY, "
				+ DbUtils.getCoordStructureString(COORD_PREFIX) + ")";
		return sql;
	}

}
