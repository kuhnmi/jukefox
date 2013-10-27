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

import java.util.List;

import ch.ethz.dcg.jukefox.data.db.IDbLogHelper;
import ch.ethz.dcg.jukefox.data.log.ILogEntry;
import ch.ethz.dcg.jukefox.data.log.PackedLogEntry;

/**
 * Provides statistics data
 */
public class LogProvider {

	private final IDbLogHelper logHelper;
	private final int profileId;

	/**
	 * Creates a new instance of {@link LogProvider}
	 * 
	 * @param profileId
	 *            The profile on which we should work
	 * @param logHelper
	 *            The log helper we should use
	 */
	public LogProvider(int profileId, IDbLogHelper logHelper) {
		this.logHelper = logHelper;
		this.profileId = profileId;
	}

	/**
	 * @see IDbLogHelper#writeLogEntry(int, ILogEntry)
	 */
	public void writeLogEntry(ILogEntry logEntry) {
		logHelper.writeLogEntry(profileId, logEntry);
	}

	/**
	 * @see IDbLogHelper#getLogEntryStrings(int)
	 */
	public List<PackedLogEntry> getLogEntryStrings() {
		return logHelper.getLogEntryStrings(profileId);
	}

	/**
	 * @see IDbLogHelper#removeLogEntriesOlderThan(int, int)
	 */
	public void removeLogEntriesOlderThan(int entryId) {
		logHelper.removeLogEntriesOlderThan(profileId, entryId);
	}

}