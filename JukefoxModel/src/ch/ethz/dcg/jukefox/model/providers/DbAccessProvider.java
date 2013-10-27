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
package ch.ethz.dcg.jukefox.model.providers;

import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper;

/**
 * Provides advanced database access options
 */
public class DbAccessProvider {

	private final IDbDataPortal dbDataPortal;

	/**
	 * Creates a new instance of {@link DbAccessProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public DbAccessProvider(IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	/**
	 * Closes the database
	 */
	public void close() {
		dbDataPortal.close();
	}

	/**
	 * Is the database open?
	 */
	public boolean isDbOpen() {
		return dbDataPortal.isOpen();
	}

	/**
	 * Begins a transaction.
	 * 
	 * @see IDbDataPortal#beginTransaction()
	 */
	public void beginTransaction() {
		dbDataPortal.beginTransaction();
	}

	/**
	 * Sets a transaction to successful.
	 * 
	 * @see IDbDataPortal#setTransactionSuccessful()
	 */
	public void setTransactionSuccessful() {
		dbDataPortal.setTransactionSuccessful();
	}

	/**
	 * Ends a transaction.
	 * 
	 * @see IDbDataPortal#endTransaction()
	 */
	public void endTransaction() {
		dbDataPortal.endTransaction();
	}

	/**
	 * @see IDbDataPortal#resetDatabase()
	 */
	public void resetDatabase() {
		dbDataPortal.resetDatabase();
	}

	/**
	 * <b>Attention!</b> This <u>DELETES the whole database</u>! You will loose all information!<br/>
	 * You won't need to call this function very often...
	 * 
	 * @see IDbDataPortal#deleteDatabase()
	 */
	public void deleteDatabase() {
		dbDataPortal.deleteDatabase();
	}

	/**
	 * Restores the data which needed the coordinates to be fetched.
	 * 
	 * @see IDbStatisticsHelper#restoreStatisticsData()
	 * @see IDbStatisticsHelper#restoreRatingData()
	 */
	public void restoreDataAfterCoordinatesFetched() {
		dbDataPortal.getStatisticsHelper().restoreStatisticsData();
		dbDataPortal.getStatisticsHelper().restoreRatingData();
	}
}