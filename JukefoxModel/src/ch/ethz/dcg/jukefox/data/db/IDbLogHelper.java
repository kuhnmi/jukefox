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

import java.util.List;

import ch.ethz.dcg.jukefox.data.log.ILogEntry;
import ch.ethz.dcg.jukefox.data.log.PackedLogEntry;

public interface IDbLogHelper {

	/**
	 * Writes a log entry to the db.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param logEntry
	 *            The log entry
	 */
	public void writeLogEntry(int profileId, ILogEntry logEntry);

	/**
	 * Returns all log entries which are in the db.
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @return The log entries
	 */
	public List<PackedLogEntry> getLogEntryStrings(int profileId);

	/**
	 * Removes all log entries which have <code>{@link TblLogEntry#LOGENTRY_ID} <= entryId</code>
	 * 
	 * @param profileId
	 *            On which profile we should work
	 * @param entryId
	 *            The most recent entry id which should be removed
	 */
	public void removeLogEntriesOlderThan(int profileId, int entryId);

}
