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
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.Genre;

/**
 * Provides all possible data access options for artist-objects
 */
public class ArtistProvider {

	private final IDbDataPortal dbDataPortal;

	/**
	 * Creates a new instance of {@link ArtistProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public ArtistProvider(IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	// ----- BASE ARTIST -----

	/**
	 * Gets a list of all available {@link BaseArtist}
	 * 
	 * @return A list of all available {@link BaseArtist}
	 */
	public List<BaseArtist> getAllArtists() {
		return dbDataPortal.getAllArtists();
	}

	/**
	 * Gets a list of {@link BaseArtist} of the given {@link Genre}
	 * 
	 * @param genre
	 *            The {@link Genre} of which you want generate the list of
	 *            {@link BaseArtist}
	 * @return A list of {@link BaseArtist} of the given {@link Genre}
	 */
	public List<BaseArtist> getAllArtists(Genre genre) {
		return dbDataPortal.getArtistsForGenre(genre);
	}

	/**
	 * Gets a list of {@link BaseArtist} searched by a search term
	 * 
	 * @param searchTerm
	 *            The search term ({@link String}) that describes the desired
	 *            return value
	 * @param maxResults
	 *            The maximum numbers ({@link Integer}) of results
	 * @return All results as a list of {@link BaseArtist} of the given search
	 *         terms
	 */
	public List<BaseArtist> findBaseArtistBySearchString(String searchTerm, int maxResults) {
		return dbDataPortal.findArtistBySearchString(searchTerm, maxResults);
	}

	/**
	 * Gets a list of famous {@link BaseArtist} searched by a search term
	 * 
	 * @param searchTerm
	 *            The search term ({@link String}) that describes the desired
	 *            return value
	 * @param maxResults
	 *            The maximum numbers ({@link Integer}) of results
	 * @return All results as a list of {@link BaseArtist} of the given search
	 *         terms
	 */
	public List<BaseArtist> findFamousBaseArtistBySearchString(String searchTerm, int maxResults) {
		return dbDataPortal.findFamousArtistBySearchString(searchTerm, maxResults);
	}

	// ----- COMPLETE ARTIST -----

	/**
	 * Gets a {@link CompleteArtist} of a given {@link BaseArtist}
	 * 
	 * @param album
	 *            A {@link BaseArtist} on which the returned
	 *            {@link CompleteArtist} will be based
	 * @return A {@link CompleteArtist} of the given {@link BaseArtist}
	 */
	public CompleteArtist getCompleteArtist(BaseArtist baseArtist) throws DataUnavailableException {
		return dbDataPortal.getCompleteArtist(baseArtist);
	}
}
