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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.ResourceLoader;

public class DirectoryManager {

	private static final String TAG = DirectoryManager.class.getSimpleName();

	public static final String FS = File.separator;

	// -----=== ABSOLUTE PATHS ===-----

	private static final String DEFAULT_JUKEFOX_ROOT_DIR = System.getProperty("user.dir") + FS + ".jukefox";

	// changeable path
	private static String jukefoxRootDir = DEFAULT_JUKEFOX_ROOT_DIR;

	// -----=== RELATIVE PATHS ===-----

	// --- Main directories ---

	private static final String COVER_DIRECTORY = "covers";
	private static final String DATA_DIRECTORY = "data";
	private static final String BLACKLIST_DIRECTORY = "blacklists";
	private static final String PLAYER_MODEL_DIRECTORY = "playerModels";

	// --- Sub paths ---

	// data
	private static final String LOGCAT_FILE = "loggingOutput";
	private static final String SETTINGS_FILENAME = "settings.set";
	private static final String TAG_FILENAME = "tags.txt";
	protected static final String MATRIX_FILE = "matrix.txt";

	// blacklist
	private static final String MUSIC_FILE_BLACKLIST_FILE = "fileblacklist.txt";
	private static final String MUSIC_DIRECTORIES_BLACKLIST_FILE = "dirblacklist.txt";

	// resource loader
	private final static String TAG_DATA_PATH = System.getProperty("user.dir") + FS + "res" + FS + "raw" + FS + "tags.dat";
	private final static String ARTIST_DATA_PATH = System.getProperty("user.dir") + FS + "res" + FS + "raw" + FS + "artists.dat";

	private final static String TAG_DATA_JAR_PATH = "raw/tags.dat";
	private final static String ARTIST_DATA_JAR_PATH = "raw/artists.dat";

	// database
	private static final String DB_FILE = "museek.db";
	private static final String DB_PATH = jukefoxRootDir + FS + DATA_DIRECTORY + FS + DB_FILE;
	private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

	// playlist
	private static final String PLAYLIST_DIRECTORY = "playlists";

	public void deleteDirectories() {
		File rootDir = new File(jukefoxRootDir);
		deleteRecursive(rootDir);
	}

	public void createAllDirectories() {
		// root
		createDirectory(jukefoxRootDir);

		// root/data
		createDirectory(jukefoxRootDir + FS + DATA_DIRECTORY);

		// root/covers
		createDirectory(jukefoxRootDir + FS + COVER_DIRECTORY);

		// root/blacklists
		createDirectory(jukefoxRootDir + FS + BLACKLIST_DIRECTORY);

		// root/playerModels
		createDirectory(jukefoxRootDir + FS + PLAYER_MODEL_DIRECTORY);
	}

	private void createDirectory(String dir) {
		File directory = new File(dir);
		if (!directory.exists() || !directory.isDirectory()) {
			if (!directory.mkdir()) {
				Log.w(TAG, "Could not create directory: " + directory.getAbsolutePath());
			}
		}
	}

	public boolean isDirectoryMissing() {
		return isDirectoryMissing(jukefoxRootDir) || isDirectoryMissing(jukefoxRootDir + FS + DATA_DIRECTORY)
				|| isDirectoryMissing(jukefoxRootDir + FS + COVER_DIRECTORY) || isDirectoryMissing(jukefoxRootDir + FS + BLACKLIST_DIRECTORY)
				|| isDirectoryMissing(jukefoxRootDir + FS + PLAYER_MODEL_DIRECTORY);
	}

	private void deleteRecursive(File location) {
		if (!location.exists()) {
			return;
		}
		if (!location.isDirectory()) {
			location.delete();
			return;
		}
		File[] files = location.listFiles();
		if (files == null) {
			if (!location.delete()) {
				Log.w(TAG, "Could not delete file: " + location.getAbsolutePath());
			}
			return;
		}
		for (File file : files) {
			deleteRecursive(file);
		}
		if (!location.delete()) {
			Log.w(TAG, "Could not delete: " + location.getAbsolutePath());
		}
	}

	private boolean isDirectoryMissing(String dir) {
		File directory = new File(dir);
		return !directory.exists() || !directory.isDirectory();
	}

	public void emptyCoverDirectory() {
		File coverDir = getAlbumCoverDirectory();
		deleteRecursive(coverDir);
		createDirectory(jukefoxRootDir + FS + COVER_DIRECTORY);
	}

	/**
	 * Gets the path to the artist data resource file which is included in the
	 * JAR
	 */
	public InputStream getArtistDataResourceInputStream() {
		InputStream resourceStream = ResourceLoader.class.getClassLoader().getResourceAsStream(ARTIST_DATA_JAR_PATH);
		if (resourceStream != null) {
			return resourceStream;
		} else {
			Log.w(TAG, "Could not load artist data from JAR file.");
			File f = new File(ARTIST_DATA_PATH);
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
			}
		}
		throw new RuntimeException("Could not load artist data from file system.");
	}

	/**
	 * Gets the path to the tag data resource file which is included in the JAR
	 */
	public InputStream getTagDataResourceInputStream() {
		InputStream resourceStream = ResourceLoader.class.getClassLoader().getResourceAsStream(TAG_DATA_JAR_PATH);
		if (resourceStream != null) {
			Log.w(TAG, "Could load tag data from JAR file path.");
			return resourceStream;
		} else {
			Log.w(TAG, "Could not load tag data from JAR file.");
			File f = new File(TAG_DATA_PATH);
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
			}
		}
		throw new RuntimeException("Could not load tag data from file system.");
	}

	/**
	 * Gets the album cover directory
	 */
	public File getAlbumCoverDirectory() {
		return new File(jukefoxRootDir + FS + COVER_DIRECTORY);
	}

	/**
	 * Gets the log output file base path
	 */
	public String getLogFileBasePath() {
		return jukefoxRootDir + FS + DATA_DIRECTORY + FS + LOGCAT_FILE;
	}

	/**
	 * Gets the data base connection string (URL)
	 */
	public String getDataBaseConnectionString() {
		return DB_URL;
	}

	/**
	 * Gets the data base file (for deleting)
	 */
	public File getDataBaseFile() {
		return new File(DB_PATH);
	}

	/**
	 * Gets the music files blacklist file
	 */
	public File getMusicFilesBlacklistFile() {
		return new File(jukefoxRootDir + FS + BLACKLIST_DIRECTORY + FS + MUSIC_FILE_BLACKLIST_FILE);
	}

	/**
	 * Gets the music directories blacklist file
	 */
	public File getMusicDirectoriesBlacklistFile() {
		return new File(jukefoxRootDir + FS + BLACKLIST_DIRECTORY + FS + MUSIC_DIRECTORIES_BLACKLIST_FILE);
	}

	/**
	 * Gets the playlist directory of the given player model
	 * 
	 * @param playerModelName
	 *            The name of the current player model manager
	 */
	public File getPlaylistDirectory(String playerModelName) {
		File playlistDirectory = new File(
				jukefoxRootDir + FS + PLAYER_MODEL_DIRECTORY + FS + playerModelName + FS + PLAYLIST_DIRECTORY);

		if (!playlistDirectory.exists()) {
			playlistDirectory.mkdirs();
		}

		return playlistDirectory;
	}

	/**
	 * Gets the settings file
	 */
	public File getSettingsFile() {
		return new File(jukefoxRootDir + FS + DATA_DIRECTORY + FS + SETTINGS_FILENAME);
	}

	/**
	 * Gets the tag file
	 */
	public File getTagFile() {
		return new File(jukefoxRootDir + FS + DATA_DIRECTORY + FS + TAG_FILENAME);
	}

	/**
	 * Gets the matrix file in which the PCA transform is saved
	 */
	public File getMatrixFile() {
		return new File(jukefoxRootDir + FS + DATA_DIRECTORY + MATRIX_FILE);
	}

	/**
	 * Sets a new jukefox root directory
	 */
	public void setJukefoxRootDir(String newJukefoxRootDir) {
		if (isDirectoryMissing(newJukefoxRootDir)) {
			Log.w(TAG, "Cannot set jukefox root directory. New directory doesn't exists: " + newJukefoxRootDir);
			return;
		}

		jukefoxRootDir = newJukefoxRootDir;
	}

}
