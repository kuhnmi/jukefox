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

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.Constants;

/**
 * Contains simple static methods required in conjunction with SQL statements etc. (e.g. coord-SQL, java.util.Date ->
 * java.sql.Date, etc.)
 * 
 * @author kuhnmi
 * 
 */
public class DbUtils {

	public static String getCoordStructureString(String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix + "0 FLOAT");
		for (int i = 1; i < Constants.DIM; i++) {
			sb.append(", " + prefix + i + " FLOAT");
		}
		return sb.toString();
	}

	public static String getCoordString(String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix + "0");
		for (int i = 1; i < Constants.DIM; i++) {
			sb.append(", " + prefix + i);
		}
		return sb.toString();
	}

	public static String getCoordValuesString(float[] coords) {
		StringBuilder sb = new StringBuilder();
		sb.append(coords[0]);
		for (int i = 1; i < Constants.DIM; i++) {
			sb.append(", " + coords[i]);
		}
		return sb.toString();
	}

	public static String getValuesString(Collection<? extends Object> coll, String enclosedBy) {
		if (coll == null || coll.size() == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Object o : coll) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(enclosedBy + o.toString() + enclosedBy);
			i++;
		}
		return sb.toString();
	}

	public static int getArtistSetHash(Set<Integer> artistIds) {
		int hashCode = 0;
		Random random = new Random();
		for (Integer id : artistIds) {
			random.setSeed(id);
			hashCode ^= random.nextInt();
		}
		return hashCode;
	}

	public static String escapeString(String term) {
		return term.replace("'", "''");
	}

	/**
	 * 
	 * 
	 * @param o
	 * @return
	 */
	public static String formatQueryValue(Object o) {
		if (o == null) {
			return "NULL";
		} else if (o.getClass().equals(String.class)) {
			return "'" + o.toString() + "'";
		} else {
			return o.toString();
		}
	}

}
