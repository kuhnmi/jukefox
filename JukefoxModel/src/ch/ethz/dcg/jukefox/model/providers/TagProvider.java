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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.DateTag;
import ch.ethz.dcg.jukefox.model.collection.MapTag;

/**
 * Provides all possible data access options for tag-objects
 */
public class TagProvider {

	private final IDbDataPortal dbDataPortal;
	private final PreloadedDataManager preloadedDataManager;

	/**
	 * Creates a new instance of {@link TagProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param preloadedDataManager
	 *            The preloaded data manager which will be used
	 */
	public TagProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
	}

	// ----- COMPLETE TAG -----

	/**
	 * Gets a collection of all available {@link CompleteTag}
	 * 
	 * @return All available {@link CompleteTag}
	 */
	public Collection<CompleteTag> getAllCompleteTags(int numTags) throws DataUnavailableException {
		return preloadedDataManager.getData().getTags();
	}

	/**
	 * Gets a list of {@link CompleteTag} and {@link Float} pairs ({@link Pair})
	 * of a given {@link CompleteArtist}
	 * 
	 * @param completeArtist
	 *            The {@link CompleteArtist} of which you want generate the list
	 *            of {@link CompleteTag}
	 * @return A list of {@link CompleteTag} and {@link Float} pairs of a given
	 *         {@link CompleteArtist}
	 * @throws DataUnavailableException
	 */
	public List<Pair<CompleteTag, Float>> getAllCompleteTags(CompleteArtist completeArtist)
			throws DataUnavailableException {
		return preloadedDataManager.getData().getTagsForCoordinate(completeArtist.getCoords());
	}

	/**
	 * Gets a list of {@link CompleteTag} and {@link Float} pairs ({@link Pair})
	 * of a given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            The {@link BaseAlbum} of which you want generate the list of
	 *            {@link CompleteTag}
	 * @return A list of {@link CompleteTag} and {@link Float} pairs of a given
	 *         {@link BaseAlbum}
	 */
	public List<Pair<CompleteTag, Float>> getAllCompleteTags(BaseAlbum baseAlbum) throws DataUnavailableException {
		float[] coords = dbDataPortal.getCoordsForAlbum(baseAlbum);
		return preloadedDataManager.getData().getTagsForCoordinate(coords);
	}

	/**
	 * Gets a {@link CompleteTag} of a given id
	 * 
	 * @param id
	 *            The tag id ({@link Integer}) of the returned
	 *            {@link CompleteTag}
	 * @return A {@link CompleteTag} of the given tag id
	 */
	public CompleteTag getCompleteTag(int id) throws DataUnavailableException {
		return preloadedDataManager.getData().getTagById(id);
	}

	/**
	 * Gets a {@link HashMap} of all available {@link CompleteTag}
	 * 
	 * @param onlyRelevantTags
	 *            Returns only relevant tags? ({@link Boolean})
	 * @return A {@link HashMap} of all available {@link CompleteTag}
	 */
	public HashMap<Integer, CompleteTag> getCompleteTags(boolean onlyRelevantTags) throws DataUnavailableException {
		return dbDataPortal.getCompleteTags(onlyRelevantTags);
	}

	// ----- MAP TAG -----

	/**
	 * Gets a list of all available {@link MapTag}
	 * 
	 * @return All available {@link MapTag}
	 */
	public List<MapTag> getAllMapTags() {
		return dbDataPortal.getMapTags();
	}

	/**
	 * Gets a list of the most relevant {@link MapTag}
	 * 
	 * @param numTags
	 *            The maximum number ({@link Integer}) of results
	 * @return A list of the most relevant {@link MapTag}
	 */
	public List<MapTag> getMostRelevantMapTags(int numTags) {
		return dbDataPortal.getHighestVarianceTags(numTags);
	}

	// ----- DATE TAG -----

	/**
	 * Gets all sorted {@link DateTag}
	 * 
	 * @return All sorted {@link DateTag}
	 */
	public ArrayList<DateTag> getSortedDateTags() throws DataUnavailableException {
		return preloadedDataManager.getData().getSortedDateTags();
	}

}
