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

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;

public abstract class IdGenerator {

	private IDbDataPortal dbDataPortal;
	private String tblName;
	private String pkeyName;

	private int currentId;

	public IdGenerator(IDbDataPortal dbDataPortal, String tblName, String pkeyName) {
		this.dbDataPortal = dbDataPortal;
		this.tblName = tblName;
		this.pkeyName = pkeyName;

		try {
			this.currentId = getDbDataPortal().getMaximumValue(this.tblName, this.pkeyName) + 1;
		} catch (DataUnavailableException e) {
			throw new RuntimeException(e);
		}
	}

	protected IDbDataPortal getDbDataPortal() {
		return dbDataPortal;
	}

	public int nextId() {
		return this.currentId++;
	}
}
