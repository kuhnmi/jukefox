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

/**
 * @author Samuel Zihlmann
 * 
 */
public interface ICursor {

	/**
	 * Closes the Cursor, releasing all of its resources and making it
	 * completely invalid.
	 */
	void close();

	/**
	 * Returns true if the value in the indicated column is null.
	 */
	boolean isNull(int i);

	// /**
	// * Returns the numbers of rows in the cursor.
	// */
	// int getCount();

	/**
	 * Return total number of columns.
	 */
	int getColumnCount();

	/**
	 * Returns the value of the requested column as a String.
	 */
	String getString(int i);

	/**
	 * Returns the value of the requested column as an int.
	 */
	int getInt(int i);

	/**
	 * Returns the value of the requested column as a long.
	 */
	long getLong(int i);

	/**
	 * Returns the value of the requested column as a float.
	 */
	float getFloat(int i);

	/**
	 * Move the cursor to the next row.
	 */
	boolean moveToNext();

	// /**
	// * Move the cursor to the first row.
	// */
	// boolean moveToFirst();

}
