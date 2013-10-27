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
package ch.ethz.dcg.jukefox.data.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;

/**
 * Saves, loads and manages the model settings
 */
public class ModelSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static String TAG = ModelSettings.class.getSimpleName();

	private transient final File settingsFile;

	private int recomputeTaskId;
	private int numberOfStartedImports;
	private int numberOfCompletedImports;
	private boolean famousArtistsInserted;
	private HashSet<String> albumNamesToGroup;
	private HashSet<String> libraryPaths;
	private int lastSentPlayLogId;
	private boolean logFileEnabled;
	private int currentLogFileNumber;
	private boolean isHelpImproveJukefox;
	private int coordinateVersion;

	/**
	 * Creates a new instance of {@link ModelSettings}
	 * 
	 * @param settingsFile
	 *            The settings {@link File} on which the settings are stored
	 */
	public ModelSettings(File settingsFile) {
		this.settingsFile = settingsFile;

		if (settingsFile.exists()) {
			try {
				loadSettingsFile();
			} catch (DataUnavailableException e) {
				Log
						.v(
								TAG,
								"Settings file not available (path: '" + settingsFile.getAbsolutePath() + "'). Loading default settings.");
				createDefaultSettings();
			}
		} else {
			createDefaultSettings();
		}
	}

	// -----=== GET ===-----

	/**
	 * Gets the recomputed task id
	 */
	public int getRecomputeTaskId() {
		return recomputeTaskId;
	}

	/**
	 * Gets the number of started imports
	 */
	public int getNumberOfStartedImports() {
		return numberOfStartedImports;
	}

	/**
	 * Gets the number of completed imports
	 */
	public int getNumberOfCompletedImports() {
		return numberOfCompletedImports;
	}

	/**
	 * Are the famous artists inserted?
	 * 
	 * @return true if the famous artists are inserted; otherwise false
	 */
	public boolean isFamousArtistsInserted() {
		return famousArtistsInserted;
	}

	/**
	 * Gets the album names to group
	 */
	public HashSet<String> getAlbumNamesToGroup() {
		return albumNamesToGroup;
	}

	/**
	 * Gets library paths
	 */
	public HashSet<String> getLibraryPaths() {
		return libraryPaths;
	}

	/**
	 * Gets the last sent play log ID
	 */
	public int getLastSentPlayLogId() {
		return lastSentPlayLogId;
	}

	/**
	 * Gets isLogFileEnabled
	 */
	public boolean isLogFileEnabled() {
		return logFileEnabled;
	}

	/**
	 * Gets the current log file number
	 */
	public int getCurrentLogFileNumber() {
		return currentLogFileNumber;
	}

	/**
	 * Gets the log string
	 */
	public String getLogString() {
		StringBuilder sb = new StringBuilder();
		sb.append("coordinateVersion: " + getCoordinateVersion() + "\n");
		sb.append("currentLogFileNumber: " + getCurrentLogFileNumber() + "\n");
		sb.append("famousArtistsInserted: " + isFamousArtistsInserted() + "\n");
		sb.append("isHelpImproveJukefox: " + isHelpImproveJukefox() + "\n");
		sb.append("lastSentPlayLogId: " + getLastSentPlayLogId() + "\n");
		sb.append("logFileEnabled: " + isLogFileEnabled() + "\n");
		sb.append("numberOfStartedImports: " + getNumberOfStartedImports() + "\n");
		sb.append("recomputeTaskId: " + getRecomputeTaskId() + "\n");
		return sb.toString();
	}

	/**
	 * Is the option <help to improve jukefox> active?
	 */
	public boolean isHelpImproveJukefox() {
		return isHelpImproveJukefox;
	}

	/**
	 * Gets the coordinate version
	 */
	public int getCoordinateVersion() {
		return coordinateVersion;
	}

	// -----=== SET ===-----

	/**
	 * Sets the recomputed task id
	 */
	public void setRecomputeTaskId(int recomputeTaskId) {
		this.recomputeTaskId = recomputeTaskId;
		saveSettingsFile();
	}

	/**
	 * Sets the number of started imports
	 */
	public void setNumberOfStartedImports(int numberOfStartedImports) {
		this.numberOfStartedImports = numberOfStartedImports;
		saveSettingsFile();
	}

	/**
	 * Sets the number of started imports
	 */
	public void setNumberOfCompletedImports(int numberOfCompletedImports) {
		this.numberOfCompletedImports = numberOfCompletedImports;
		saveSettingsFile();
	}

	/**
	 * Are the famous artists inserted?
	 * 
	 * @param famousArtistsInserted
	 *            true if the famous artists are inserted; otherwise false
	 */
	public void setFamousArtistsInserted(boolean famousArtistsInserted) {
		this.famousArtistsInserted = famousArtistsInserted;
		saveSettingsFile();
	}

	/**
	 * Sets the album names to group
	 */
	public void setAlbumNamesToGroup(HashSet<String> albumNamesToGroup) {
		this.albumNamesToGroup = albumNamesToGroup;
		saveSettingsFile();
	}

	/**
	 * Sets library paths
	 */
	public void setLibraryPaths(HashSet<String> libraryPaths) {
		this.libraryPaths = libraryPaths;
		saveSettingsFile();
	}

	/**
	 * Sets the last sent play log ID
	 */
	public void setLastSentPlayLogId(int lastSentPlayLogId) {
		this.lastSentPlayLogId = lastSentPlayLogId;
		saveSettingsFile();
	}

	/**
	 * Sets logFileEnabled
	 */
	public void setLogFileEnabled(boolean logFileEnabled) {
		this.logFileEnabled = logFileEnabled;
		saveSettingsFile();
	}

	/**
	 * Sets the current log file number
	 */
	public void setCurrentLogFileNumber(int currentLogFileNumber) {
		this.currentLogFileNumber = currentLogFileNumber;
		saveSettingsFile();
	}

	/**
	 * Set the option <help to improve jukefox>
	 */
	public void setHelpImproveJukefox(boolean isHelpImproveJukefox) {
		this.isHelpImproveJukefox = isHelpImproveJukefox;
		saveSettingsFile();
	}

	public void setCoordinateVersion(int coordinateVersion) {
		this.coordinateVersion = coordinateVersion;
		saveSettingsFile();
	}

	// -----=== METHODS ===-----

	/**
	 * Increases the recompute task id by one
	 */
	public void incRecomputeTaskId() {
		setRecomputeTaskId(recomputeTaskId + 1);
	}

	/**
	 * Resets the recompute task id to zero
	 */
	public void resetRecomputeTaskId() {
		setRecomputeTaskId(0);
	}

	/**
	 * Saves the current settings in the settings {@link File}
	 */
	private void saveSettingsFile() {
		ObjectOutputStream oos = null;
		FileOutputStream fileOut = null;

		try {
			fileOut = new FileOutputStream(settingsFile);
			oos = new ObjectOutputStream(fileOut);

			oos.writeObject(this);

		} catch (Exception e) {
			Log.w(TAG, e);

		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (Exception e) {
				}
			}
			if (fileOut != null) {
				try {
					fileOut.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Loads the settings from the settings {@link File}
	 */
	private void loadSettingsFile() throws DataUnavailableException {
		ObjectInputStream ois = null;
		FileInputStream fileIn = null;
		ModelSettings settings = null;

		try {
			fileIn = new FileInputStream(settingsFile);
			ois = new ObjectInputStream(fileIn);

			settings = (ModelSettings) ois.readObject();

			this.famousArtistsInserted = settings.isFamousArtistsInserted();
			this.numberOfStartedImports = settings.getNumberOfStartedImports();
			this.numberOfCompletedImports = settings.getNumberOfCompletedImports();
			this.recomputeTaskId = settings.getRecomputeTaskId();
			this.albumNamesToGroup = settings.getAlbumNamesToGroup();
			this.libraryPaths = settings.getLibraryPaths();
			this.lastSentPlayLogId = settings.getLastSentPlayLogId();
			this.logFileEnabled = settings.isLogFileEnabled();
			this.currentLogFileNumber = settings.getCurrentLogFileNumber();

		} catch (Exception e) {
			Log.w(TAG, e);
			throw new DataUnavailableException(e);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (Exception e) {
				}
			}
			if (fileIn != null) {
				try {
					fileIn.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Creates the default settings
	 * 
	 * TODO: Where to create this default settings?
	 */
	private void createDefaultSettings() {
		setFamousArtistsInserted(false);
		setNumberOfStartedImports(0);
		setNumberOfCompletedImports(0);
		setRecomputeTaskId(0);
		setAlbumNamesToGroup(new HashSet<String>());
		setLibraryPaths(new HashSet<String>());
		setLastSentPlayLogId(0);
		setLogFileEnabled(false);
		setCurrentLogFileNumber(0);
		setHelpImproveJukefox(true);
		setCoordinateVersion(0);
	}

	/**
	 * Adds a given album name ({@link String})
	 * 
	 * @param albumNameToGroup
	 *            The album name to be added
	 */
	public void addAlbumNameToGroup(String albumNameToGroup) {
		HashSet<String> albumNamesToGroup = getAlbumNamesToGroup();
		albumNamesToGroup.add(albumNameToGroup);
		setAlbumNamesToGroup(albumNamesToGroup);
	}

	/**
	 * Removes a given album name ({@link String})
	 * 
	 * @param albumNameToGroup
	 *            The album name to be removed
	 */
	public void removeAlbumNameToGroup(String albumNameToGroup) {
		HashSet<String> albumNamesToGroup = getAlbumNamesToGroup();
		albumNamesToGroup.remove(albumNameToGroup);
		setAlbumNamesToGroup(albumNamesToGroup);
	}

	/**
	 * Adds a given library path ({@link String})
	 * 
	 * @param libraryPath
	 *            The path to be added
	 */
	public void addLibraryPath(String libraryPath) {
		HashSet<String> libraryPaths = getLibraryPaths();
		libraryPaths.add(libraryPath);
		setLibraryPaths(libraryPaths);
	}

	/**
	 * Removes a given library path ({@link String})
	 * 
	 * @param libraryPath
	 *            The path to be removed
	 */
	public void removeLibraryPath(String libraryPath) {
		HashSet<String> libraryPaths = getLibraryPaths();
		libraryPaths.remove(libraryPath);
		setLibraryPaths(libraryPaths);
	}

}
