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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;

/**
 * Loads a {@link Playlist} from a given file or filename
 */
public class PlaylistReader {

	private static final String TAG = PlaylistReader.class.getSimpleName();

	/**
	 * Loads a {@link Playlist} from a file with the given name
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param name
	 *            The name ({@link String}) of the playlist file to be loaded
	 * @return A {@link Playlist} from a file with the given name
	 */
	public static Playlist loadPlaylistFromFileByName(DirectoryManager directoryManager,
			IDbDataPortal dbDataPortal, String name, String playerModelName) throws IOException {
		File file = new File(directoryManager.getPlaylistDirectory(playerModelName), name + ".m3u");

		return loadPlaylistFromFile(dbDataPortal, file);
	}

	/**
	 * Loads a {@link Playlist} from a file with the given path
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param filePath
	 *            The path ({@link String}) of the playlist file to be loaded
	 * @return A {@link Playlist} from a file with the given path
	 */
	public static Playlist loadPlaylistFromFile(IDbDataPortal dbDataPortal, String filePath) throws IOException {
		File file = new File(filePath);

		return loadPlaylistFromFile(dbDataPortal, file);
	}

	/**
	 * Loads a {@link Playlist} from a given {@link File}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param file
	 *            The {@link File} of the playlist file to be loaded
	 * @return A {@link Playlist} from a given {@link File}
	 */
	public static Playlist loadPlaylistFromFile(IDbDataPortal dbDataPortal, File file) throws IOException {
		Log.v(TAG, "Loading playlist from " + file.getAbsolutePath());

		Playlist playlist = readFileWithExtras(dbDataPortal, file);
		Log.v(TAG, "Song list size: " + playlist.getPlaylistSize());
		//		Log.v(TAG, "Position: " + playlist.getPositionInList() + " " + playlist.getPositionInSong());

		return playlist;
	}

	/**
	 * Reads all extra information from a playlist {@link File} if available
	 */
	private static Playlist readFileWithExtras(IDbDataPortal dbDataPortal, File playlistFile) throws IOException {
		Playlist playlist = new Playlist();

		FileInputStream fileInput = null;
		DataInputStream playlistStream = null;
		InputStreamReader inputStream = null;

		String[] infos = new String[0];

		try {
			fileInput = new FileInputStream(playlistFile);
			playlistStream = new DataInputStream(fileInput);
			inputStream = new InputStreamReader(fileInput);

			String encoding = inputStream.getEncoding().toLowerCase();
			Log.v(TAG, "Playlist encoding: " + encoding);

			infos = readUtf8File(dbDataPortal, playlistStream, playlist, infos);

		} catch (EOFException e) {
			Log.w(TAG, e);
			infos = readUnknownEncodingFile(dbDataPortal, playlistFile, playlist, infos);
		} finally {
			// close all streams
			if (playlistStream != null) {
				try {
					playlistStream.close();
				} catch (Exception e) {
				}
			}
			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (Exception e) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
				}
			}
		}

		if (infos.length == 5) {
			playlist.setName(infos[1]);
			try {
				playlist.setPositionInList(Integer.parseInt(infos[2]));
				playlist.setPositionInSong(Integer.parseInt(infos[3]));
				playlist.setPlayMode(Integer.parseInt(infos[4]));
				playlist.setHasExtras(true);
			} catch (NumberFormatException e) {
				Log.w(TAG, e);
			}
			// Log.v(TAG, "successfully read playlist extras");
		}
		return playlist;
	}

	private static String[] readUnknownEncodingFile(IDbDataPortal dbDataPortal, File file, Playlist playlist,
			String[] infos) throws IOException {
		FileInputStream fileInput = null;
		InputStreamReader inputStream = null;
		BufferedReader playlistStream = null;

		try {
			fileInput = new FileInputStream(file);
			inputStream = new InputStreamReader(fileInput);
			playlistStream = new BufferedReader(inputStream);

			String line = null;

			while ((line = playlistStream.readLine()) != null) {
				if (line.startsWith(Constants.PLAYLIST_EXT_INFO_PREFIX)) {
					infos = line.split(",");
				} else if (line.startsWith("#")) {
					// ignore
				} else {

					try {
						PlaylistSong<BaseArtist, BaseAlbum> songForPath = dbDataPortal.getSongForPath(line);
						playlist.appendSongAtEnd(songForPath);

						// Log.v(TAG, "line:"+line+":line");
						Log.v(TAG, "Read song from playlist '" + line + "'");

					} catch (DataUnavailableException e) {
						Log.w(TAG, e);
					}
				}
			}
		} finally {
			// close all streams
			if (playlistStream != null) {
				try {
					playlistStream.close();
				} catch (Exception e) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
				}
			}
			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (Exception e) {
				}
			}
		}
		return infos;
	}

	private static String[] readUtf8File(IDbDataPortal dbDataPortal, DataInputStream playlistStream, Playlist playlist,
			String[] infos) throws IOException {
		String data = playlistStream.readUTF();

		String[] lines = data.split("\\n");

		for (String line : lines) {
			if (line.startsWith(Constants.PLAYLIST_EXT_INFO_PREFIX)) {
				infos = line.split(",");
			} else if (line.startsWith("#")) {
				// ignore
			} else {
				try {
					PlaylistSong<BaseArtist, BaseAlbum> songForPath = dbDataPortal.getSongForPath(line);
					playlist.appendSongAtEnd(songForPath);

					// Log.v(TAG, "line:"+line+":line");
					Log.v(TAG, "Read song from playlist '" + line + "'");

				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}
			}
		}

		return infos;
	}

}
