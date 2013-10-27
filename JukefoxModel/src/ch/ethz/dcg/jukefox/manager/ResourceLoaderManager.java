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
package ch.ethz.dcg.jukefox.manager;

import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.data.ResourceLoader;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;

/**
 * Manages all data access for {@link ResourceLoader}
 */
public class ResourceLoaderManager {

	private final IDbDataPortal dbDataPortal;
	private ResourceLoader resourceLoader;

	/**
	 * Creates a new instance of {@link ResourceLoaderManager}
	 * 
	 * @param dbDataPortal
	 *            The database data portal provider which will be used
	 */
	public ResourceLoaderManager(IDbDataPortal dbDataPortal, DirectoryManager directoryManager) {
		this.dbDataPortal = dbDataPortal;
		this.resourceLoader = new ResourceLoader(directoryManager);
	}

	/**
	 * Writes all tags to an .txt file
	 */
	public void writeTagsToSdCard() throws DataWriteException {
		resourceLoader.writeTagsToSdCard();
	}

	/**
	 * Reads all tags and returns it as a {@link List} of {@link CompleteTag}
	 */
	public List<CompleteTag> readTags() throws DataUnavailableException {
		return resourceLoader.readTags();
	}

	/**
	 * Loads all famous artists and inserts it in the database
	 */
	public void loadFamousArtists() throws DataUnavailableException {
		resourceLoader.loadFamousArtists(dbDataPortal);
	}
}
