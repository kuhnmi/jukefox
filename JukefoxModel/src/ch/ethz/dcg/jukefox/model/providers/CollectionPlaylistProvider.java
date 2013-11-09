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

import java.io.File;
import java.io.IOException;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.playlist.PlaylistReader;
import ch.ethz.dcg.jukefox.model.collection.Playlist;

/**
 * Provides all possible data access options for playlist-objects
 */
public class CollectionPlaylistProvider {

	private final IDbDataPortal dbDataPortal;

	/**
	 * Creates a new instance of {@link CollectionPlaylistProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public CollectionPlaylistProvider(IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
	}

	/**
	 * Loads a {@link Playlist} from a file with the given path
	 * 
	 * @param filePath
	 *            The path ({@link String}) of the playlist file to be loaded
	 * @return A {@link Playlist} from a file with the given path
	 */
	public Playlist loadPlaylistFromFile(String filePath) throws DataUnavailableException {
		Playlist playlist = null;

		try {
			playlist = PlaylistReader.loadPlaylistFromFile(dbDataPortal, filePath);
		} catch (IOException e) {
			throw new DataUnavailableException(e);
		}

		return playlist;
	}

	/**
	 * Loads a {@link Playlist} from a given {@link File}
	 * 
	 * @param file
	 *            The {@link File} of the playlist file to be loaded
	 * @return A {@link Playlist} from a given {@link File}
	 */
	public Playlist loadPlaylistFromFile(File file) throws DataUnavailableException {
		Playlist playlist = null;

		try {
			playlist = PlaylistReader.loadPlaylistFromFile(dbDataPortal, file);
		} catch (IOException e) {
			throw new DataUnavailableException(e);
		}

		return playlist;
	}
}
