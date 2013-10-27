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

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;

/**
 * Provides all possible data access options for album-objects
 */
public class AlbumProvider {

	private final IDbDataPortal dbDataPortal;
	private final PreloadedDataManager preloadedDataManager;

	/**
	 * Creates a new instance of {@link AlbumProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param preloadedDataManager
	 *            The preloaded data manager which will be used
	 */
	public AlbumProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
	}

	// ----- BASE ALBUM -----

	/**
	 * Gets a list of all available {@link BaseAlbum} without album art
	 * 
	 * @return A list of all available {@link BaseAlbum} without album art
	 */
	public List<BaseAlbum> getAllBaseAlbumsWithoutAlbumArt() {
		return dbDataPortal.getAllAlbumsWithoutAlbumArt();
	}

	// ----- COMPLETE ALBUM -----

	/**
	 * Gets a {@link CompleteAlbum} of a given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            A {@link BaseAlbum} on which the returned
	 *            {@link CompleteAlbum} will be based
	 * @return A {@link CompleteAlbum} of the given {@link BaseAlbum}
	 */
	public CompleteAlbum getCompleteAlbum(BaseAlbum baseAlbum) throws DataUnavailableException {
		return dbDataPortal.getCompleteAlbum(baseAlbum);
	}

	/**
	 * Gets a {@link CompleteAlbum} of a given album id
	 * 
	 * @param albumId
	 *            The album id ({@link Integer}) of the returned
	 *            {@link CompleteAlbum}
	 * @return A {@link CompleteAlbum} of the given album id
	 */
	public CompleteAlbum getCompleteAlbum(int albumId) throws DataUnavailableException {
		return dbDataPortal.getCompleteAlbumById(albumId);
	}

	// ----- LIST ALBUM -----

	/**
	 * Gets a list of all available {@link ListAlbum}
	 * 
	 * @return A list of all available {@link ListAlbum}
	 */
	public List<ListAlbum> getAllListAlbums() {
		return dbDataPortal.getAllAlbumsAsListAlbums();
	}

	/**
	 * Gets a list of {@link ListAlbum} of the given {@link BaseArtist}
	 * 
	 * @param baseArtist
	 *            The {@link BaseArtist} of which you want generate the list of
	 *            {@link ListAlbum}
	 * @return A list of {@link ListAlbum} of the given {@link BaseArtist}
	 */
	public List<ListAlbum> getAllListAlbums(BaseArtist baseArtist) {
		return dbDataPortal.getAllAlbumsForArtist(baseArtist, true);
	}

	/**
	 * Gets a list of {@link ListAlbum} of the given {@link Genre}
	 * 
	 * @param genre
	 *            The {@link Genre} of which you want generate the list of
	 *            {@link ListAlbum}
	 * @return A list of {@link ListAlbum} of the given {@link Genre}
	 */
	public List<ListAlbum> getAllListAlbums(Genre genre) {
		return dbDataPortal.getAlbumsForGenre(genre);
	}

	/**
	 * Gets a list of {@link ListAlbum} searched by a search term
	 * 
	 * @param searchTerm
	 *            The search term ({@link String}) that describes the desired
	 *            return value
	 * @param maxResults
	 *            The maximum numbers ({@link Integer}) of results
	 * @return All results as a list of {@link ListAlbum} of the given search
	 *         terms
	 */
	public List<ListAlbum> findListAlbumBySearchString(String searchTerm, int maxResults) {
		return dbDataPortal.findAlbumBySearchString(searchTerm, maxResults);
	}

	// ----- MAP ALBUM -----

	/**
	 * Gets a list of all available {@link MapAlbum}
	 * 
	 * @return All available {@link MapAlbum}
	 */
	public List<MapAlbum> getAllMapAlbums() throws DataUnavailableException {
		return dbDataPortal.getAllMapAlbums();
	}

	/**
	 * Gets a {@link MapAlbum} of a given {@link BaseSong}
	 * 
	 * @param baseSong
	 *            A {@link BaseSong} of the returned {@link MapAlbum}
	 * @return A {@link MapAlbum} of the given {@link BaseSong}
	 */
	public MapAlbum getMapAlbum(BaseSong<? extends BaseArtist, ? extends BaseAlbum> baseSong)
			throws DataUnavailableException {
		return dbDataPortal.getMapAlbumBySong(baseSong);
	}

	/**
	 * Gets a {@link MapAlbum} of a given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            A {@link BaseAlbum} on which the returned {@link MapAlbum}
	 *            will be based
	 * @return A {@link MapAlbum} of the given {@link BaseAlbum}
	 */
	public MapAlbum getMapAlbum(BaseAlbum baseAlbum) throws DataUnavailableException {
		return dbDataPortal.getMapAlbum(baseAlbum);
	}

	/**
	 * Gets a list of {@link MapAlbum} and {@link Float} pairs ({@link Pair})
	 * which are similar to the given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            A {@link BaseAlbum} which is similar to the returned list of
	 *            {@link MapAlbum}
	 * @param number
	 *            Minimum number for the advanced kd tree algorithm
	 * @return A list of {@link MapAlbum} and {@link Float} pairs
	 */
	public List<Pair<MapAlbum, Float>> getSimilarAlbums(BaseAlbum baseAlbum, int number)
			throws DataUnavailableException {
		return preloadedDataManager.getSimilarAlbums(baseAlbum, number);
	}

}
