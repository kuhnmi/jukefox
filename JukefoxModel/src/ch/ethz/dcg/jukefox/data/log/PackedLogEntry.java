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
package ch.ethz.dcg.jukefox.data.log;

import java.util.Date;

public class PackedLogEntry extends AbstractLogEntry {

	private final int dbLogEntryId;
	private final int typeVersion;
	private final String typeIdent;
	private final String wholeLog;

	public PackedLogEntry(int dbLogEntryId, String log) {
		this.dbLogEntryId = dbLogEntryId;

		wholeLog = log;

		//version|timestamp|type-ident|type-version|type-log
		String[] parts = log.split("\\|", 5);
		setTimestamp(new Date(Long.parseLong(parts[1])));
		typeIdent = parts[2];
		typeVersion = Integer.parseInt(parts[3]);
	}

	public int getDbLogEntryId() {
		return dbLogEntryId;
	}

	@Override
	public int getTypeVersion() {
		return typeVersion;
	}

	@Override
	protected String getTypeIdent() {
		return typeIdent;
	}

	@Override
	protected String packYourStuff() {
		// we implement toString on our own, do not call this in any other way
		assert false;
		return null;
	}

	@Override
	public String getPacked() {
		return wholeLog;
	}
}
