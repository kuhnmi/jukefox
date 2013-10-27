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
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Genre;

/**
 * Provides all possible data access options for genre-objects
 */
public class GenreProvider {

	private final IDbDataPortal dbDataPortal;

	/**
	 * Creates a new instance of {@link GenreProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public GenreProvider(IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	// ----- GENRE -----

	/**
	 * Gets a list of all available {@link Genre}
	 * 
	 * @return All available {@link Genre}
	 */
	public List<Genre> getAllGenres() {
		return dbDataPortal.getAllGenres();
	}

	/**
	 * Gets a list of {@link Genre} and {@link Integer} pairs ({@link Pair}) of
	 * a given {@link BaseArtist}
	 * 
	 * @param baseArtist
	 *            The {@link BaseArtist} of which you want generate the list of
	 *            {@link Genre}
	 * @return A list of {@link Genre} and {@link Integer} pairs of a given
	 *         {@link BaseArtist}
	 */
	public List<Pair<Genre, Integer>> getGenresForArtist(BaseArtist baseArtist) throws DataUnavailableException {
		return dbDataPortal.getGenresForArtist(baseArtist);
	}
}
