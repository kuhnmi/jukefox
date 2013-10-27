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
package ch.ethz.dcg.jukefox.data.playlist;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;

/**
 * Writes a given {@link Playlist} to a file
 */
public class PlaylistWriter {

	private static final String TAG = PlaylistWriter.class.getSimpleName();

	/**
	 * Writes a {@link Playlist} to a file with the given name
	 * 
	 * @param directoryManager
	 *            The directory manager which will be used
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param playlist
	 *            The {@link IReadOnlyPlaylist} to be saved
	 * @param name
	 *            The name ({@link String}) of the {@link IReadOnlyPlaylist}
	 *            file to be saved
	 * @param playerModelName
	 *            The name ({@link String}) of the current
	 *            {@link AbstractPlayerModelManager}
	 */
	public static void writePlaylist(DirectoryManager directoryManager, IDbDataPortal dbDataPortal,
			IReadOnlyPlaylist playlist, String name, String playerModelName) {
		File playlistDirectory = directoryManager.getPlaylistDirectory(playerModelName);
		writePlaylistToFile(dbDataPortal, playlist, playlistDirectory, name);
	}

	/**
	 * Writes a {@link Playlist} to a file with the given name in a given
	 * directory
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param playlist
	 *            The {@link IReadOnlyPlaylist} to be saved
	 * @param playlistDirectory
	 *            The directory ({@link File}) to which the playlist
	 *            {@link File} will be saved.
	 * @param name
	 *            The name ({@link String}) of the {@link IReadOnlyPlaylist}
	 *            file to be saved
	 */
	public static void writePlaylistToFile(IDbDataPortal dbDataPortal, IReadOnlyPlaylist playlist,
			File playlistDirectory, String name) {
		File playlistFile = new File(playlistDirectory, name + ".m3u");
		FileOutputStream fileOutput = null;
		DataOutputStream dout = null;

		List<PlaylistSong<BaseArtist, BaseAlbum>> songList = playlist.getSongList();
		if (songList.size() == 0) {
			Log.v(TAG, "Not saving Playlist because it's empty");
			return;
		}

		try {
			fileOutput = new FileOutputStream(playlistFile, false);
			dout = new DataOutputStream(fileOutput);

			StringBuffer dataToWrite = new StringBuffer();
			dataToWrite.append(Constants.PLAYLIST_EXT_INFO_PREFIX);
			dataToWrite.append(",");
			dataToWrite.append(name);
			dataToWrite.append(",");
			dataToWrite.append(playlist.getPositionInList());
			dataToWrite.append(",");
			dataToWrite.append(playlist.getPositionInSong());
			dataToWrite.append(",");
			dataToWrite.append(playlist.getPlayMode());
			dataToWrite.append("\n");

			Log.v(TAG,
					"saved position in list: " + playlist.getPositionInList() + " and " + playlist.getPositionInSong());
			int length = playlist.getSongList().size();
			for (int i = 0; i < length; i++) {
				BaseSong<BaseArtist, BaseAlbum> song = playlist.getSongAtPosition(i);
				dataToWrite.append(dbDataPortal.getSongPath(song));
				dataToWrite.append("\n");
			}
			dout.writeUTF(dataToWrite.toString());
		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			if (dout != null) {
				try {
					dout.close();
				} catch (Exception e) {
				}
			}
			if (fileOutput != null) {
				try {
					fileOutput.close();
				} catch (Exception e) {
				}
			}
		}
	}

}
