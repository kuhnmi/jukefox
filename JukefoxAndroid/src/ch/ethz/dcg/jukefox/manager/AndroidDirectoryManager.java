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
import java.io.InputStream;

import android.os.Environment;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class AndroidDirectoryManager extends DirectoryManager {

	private static final String TAG = AndroidDirectoryManager.class.getSimpleName();

	public static final String FS = File.separator;

	// -----=== ABSOLUTE PATHS ===-----

	private static final File SD_CARD = Environment.getExternalStorageDirectory();
	private static final String DEFAULT_JUKEFOX_ROOT_DIR = SD_CARD.getAbsolutePath() + FS + ".jukefox";

	private static String jukefoxRootDir = DEFAULT_JUKEFOX_ROOT_DIR;

	// -----=== RELATIVE PATHS ===-----

	// --- Main directories ---

	private static final String COVER_DIRECTORY = jukefoxRootDir + FS + "covers";
	private static final String DATA_DIRECTORY = jukefoxRootDir + FS + "data";
	private static final String BLACKLIST_DIRECTORY = jukefoxRootDir + FS + "blacklists";
	private static final String PLAYER_MODEL_DIRECTORY = jukefoxRootDir + FS + "playerModels";

	// --- Sub paths ---

	// data
	private static final String LOGCAT_FILE = jukefoxRootDir + FS + "loggingOutput";
	private static final String SETTINGS_FILENAME = jukefoxRootDir + FS + "settings.set";
	private static final String TAG_FILENAME = jukefoxRootDir + FS + "tags.txt";
	private static final String IGNORE_MEDIA_BUTTONS_FILE = jukefoxRootDir + FS + ".ignoreMediaButtons";

	// blacklist
	private static final String MUSIC_FILE_BLACKLIST_FILE = jukefoxRootDir + FS + "fileblacklist.txt";
	private static final String MUSIC_DIRECTORIES_BLACKLIST_FILE = jukefoxRootDir + FS + "dirblacklist.txt";

	// playlist
	private static final String PLAYLIST_DIRECTORY = jukefoxRootDir + FS + "playlists";

	private static final String SCROBBLE_BUFFER_FILE = jukefoxRootDir + FS + "scrobbleBuffer.txt";

	private static final String[] CORE_DIRECTORIES = new String[] { jukefoxRootDir, COVER_DIRECTORY, DATA_DIRECTORY,
			BLACKLIST_DIRECTORY, PLAYER_MODEL_DIRECTORY };
	private static final String[] ALL_DIRECTORIES = new String[CORE_DIRECTORIES.length];
	{
		for (int i = 0; i < CORE_DIRECTORIES.length; i++) {
			ALL_DIRECTORIES[i] = CORE_DIRECTORIES[i];
		}
		ALL_DIRECTORIES[ALL_DIRECTORIES.length - 1] = PLAYLIST_DIRECTORY;
	}

	public AndroidDirectoryManager() {
		createAllDirectories();
	}

	@Override
	public void deleteDirectories() {
		File rootDir = new File(jukefoxRootDir);
		deleteRecursive(rootDir);
	}

	@Override
	public void createAllDirectories() {
		for (String dir : ALL_DIRECTORIES) {
			createDirectory(dir);
		}
	}

	private void createDirectory(String dir) {
		File directory = new File(dir);
		if (!directory.exists() || !directory.isDirectory()) {
			if (!directory.mkdirs()) {
				Log.w(TAG, "Could not create directory: " + directory.getAbsolutePath());
			}
		}
	}

	@Override
	public boolean isDirectoryMissing() {
		for (String dir : CORE_DIRECTORIES) {
			if (isDirectoryMissing(dir)) {
				return true;
			}
		}
		return false;
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

	@Override
	public void emptyCoverDirectory() {
		File coverDir = new File(COVER_DIRECTORY);
		deleteRecursive(coverDir);
		createDirectory(COVER_DIRECTORY);
	}

	@Override
	public File getAlbumCoverDirectory() {
		return new File(COVER_DIRECTORY);
	}

	@Override
	public InputStream getArtistDataResourceInputStream() {
		return JukefoxApplication.getAppContext().getResources().openRawResource(R.raw.artists);
	}

	@Override
	public String getDataBaseConnectionString() {
		return super.getDataBaseConnectionString();
	}

	@Override
	public String getLogFileBasePath() {
		return LOGCAT_FILE;
	}

	@Override
	public File getMusicDirectoriesBlacklistFile() {
		return new File(MUSIC_DIRECTORIES_BLACKLIST_FILE);
	}

	@Override
	public File getMusicFilesBlacklistFile() {
		return new File(MUSIC_FILE_BLACKLIST_FILE);
	}

	@Override
	public File getPlaylistDirectory(String playerModelName) {
		return new File(PLAYLIST_DIRECTORY);
	}

	@Override
	public File getSettingsFile() {
		return new File(SETTINGS_FILENAME);
	}

	@Override
	public InputStream getTagDataResourceInputStream() {
		return JukefoxApplication.getAppContext().getResources().openRawResource(R.raw.tags);
	}

	@Override
	public File getTagFile() {
		return new File(TAG_FILENAME);
	}

	public File getIgnoreMediaButtonsFile() {
		return new File(IGNORE_MEDIA_BUTTONS_FILE);
	}

	public File getSdCardDirectory() {
		return SD_CARD;
	}

	public File getScrobbleBufferFile() {
		return new File(SCROBBLE_BUFFER_FILE);
	}

	@Override
	public File getMatrixFile() {
		return new File(jukefoxRootDir + FS + MATRIX_FILE);
	}

}
