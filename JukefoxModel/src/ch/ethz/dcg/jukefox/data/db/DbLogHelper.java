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

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.data.log.ILogEntry;
import ch.ethz.dcg.jukefox.data.log.PackedLogEntry;

public class DbLogHelper<ContentValues extends IContentValues> implements IDbLogHelper {

	protected final SqlDbDataPortal<ContentValues> sqlDbDataPortal;

	/**
	 * Constructor.
	 * 
	 * @param sqlDbDataPortal
	 */
	public DbLogHelper(SqlDbDataPortal<ContentValues> sqlDbDataPortal) {
		this.sqlDbDataPortal = sqlDbDataPortal;
	}

	@Override
	public void writeLogEntry(int profileId, ILogEntry logEntry) {
		ContentValues initialValues = sqlDbDataPortal.createContentValues();
		initialValues.put(TblLogEntry.PROFILE_ID, profileId);
		initialValues.put(TblLogEntry.LOG_STRING, logEntry.getPacked());

		sqlDbDataPortal.insertOrThrow(TblLogEntry.TBL_NAME, initialValues);

	}

	@Override
	public List<PackedLogEntry> getLogEntryStrings(int profileId) {
		List<PackedLogEntry> logEntries = new LinkedList<PackedLogEntry>();
		ICursor cur = null;
		try {
			String sql = "SELECT l." + TblLogEntry.LOGENTRY_ID + ", l." + TblLogEntry.LOG_STRING + " " +
					"FROM " + TblLogEntry.TBL_NAME + " AS l " +
					"WHERE (" + TblLogEntry.PROFILE_ID + " = ?)";

			cur = sqlDbDataPortal.execSelect(sql, new String[] { "" + profileId });
			while (cur.moveToNext()) {
				int logEntryId = cur.getInt(0);
				String log = cur.getString(1);

				logEntries.add(new PackedLogEntry(logEntryId, log));
			}

			return logEntries;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void removeLogEntriesOlderThan(int profileId, int entryId) {
		String sql = String.format(
				"DELETE FROM " + TblLogEntry.TBL_NAME + " " +
						"WHERE (" + TblLogEntry.PROFILE_ID + " = %d)" +
						"  AND (" + TblLogEntry.LOGENTRY_ID + " <= %d)", +
				profileId, entryId);
		sqlDbDataPortal.execSQL(sql);
	}
}
