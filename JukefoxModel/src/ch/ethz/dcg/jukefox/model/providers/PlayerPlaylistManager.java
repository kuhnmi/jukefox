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
import ch.ethz.dcg.jukefox.data.playlist.PlaylistWriter;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;

/**
 * Provides all possible data access options for playlist-objects
 */
public class PlayerPlaylistManager {

	private final IDbDataPortal dbDataPortal;
	private final DirectoryManager directoryManager;
	private final String playerModelName;

	/**
	 * Creates a new instance of {@link PlayerPlaylistManager}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public PlayerPlaylistManager(IDbDataPortal dbDataPortal, String playModelName, DirectoryManager directoryManager) {
		this.dbDataPortal = dbDataPortal;
		this.directoryManager = directoryManager;
		this.playerModelName = playModelName;
	}

	/**
	 * Loads a {@link Playlist} from a file with the given name
	 * 
	 * @param name
	 *            The name ({@link String}) of the playlist file to be loaded
	 * @return A {@link Playlist} from a file with the given name
	 */
	public Playlist loadPlaylistFromFileByName(String name) throws DataUnavailableException {
		Playlist playlist = null;

		try {
			playlist = PlaylistReader.loadPlaylistFromFileByName(directoryManager, dbDataPortal, name, playerModelName);
		} catch (IOException e) {
			throw new DataUnavailableException(e);
		}

		return playlist;
	}

	/**
	 * Write a playlist into a file
	 * 
	 * @param playlist
	 *            The {@link IReadOnlyPlaylist} to be saved
	 * @param name
	 *            The name ({@link String}) of the {@link IReadOnlyPlaylist}
	 *            file to be saved
	 */
	public void writePlaylistToFile(IReadOnlyPlaylist playlist, String name) {
		PlaylistWriter.writePlaylist(directoryManager, dbDataPortal, playlist, name, playerModelName);
	}

	/**
	 * Gets the playlist directory ({@link File})
	 */
	public File getPlaylistDirectory() {
		return directoryManager.getPlaylistDirectory(playerModelName);
	}

}
