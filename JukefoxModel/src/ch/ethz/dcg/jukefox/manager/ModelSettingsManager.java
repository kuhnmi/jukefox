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

import java.util.HashSet;

import ch.ethz.dcg.jukefox.data.settings.ModelSettings;

/**
 * Manages all model settings
 */
public class ModelSettingsManager {

	private ModelSettings modelSettings = null;

	public ModelSettingsManager(DirectoryManager directoryManager) {
		this.modelSettings = new ModelSettings(directoryManager.getSettingsFile());
	}

	private ModelSettings getModelSettings() {
		// if (modelSettings == null) {
		// modelSettings = new
		// ModelSettings(DirectoryManager.getSettingsFile());
		// }
		return modelSettings;
	}

	// -----=== GET ===-----

	/**
	 * Gets the recompute task id count
	 */
	public int getRecomputeTaskId() {
		return getModelSettings().getRecomputeTaskId();
	}

	/**
	 * Gets the number of started imports
	 */
	public int getNumberOfStartedImports() {
		return getModelSettings().getNumberOfStartedImports();
	}

	/**
	 * Gets the number of started imports
	 */
	public int getNumberOfCompletedimports() {
		return getModelSettings().getNumberOfCompletedImports();
	}

	/**
	 * Are the famous artists inserted?
	 * 
	 * @return true if the famous artists are inserted; otherwise false
	 */
	public boolean isFamousArtistsInserted() {
		return getModelSettings().isFamousArtistsInserted();
	}

	/**
	 * Gets the album names to group
	 */
	public HashSet<String> getAlbumNamesToGroup() {
		return getModelSettings().getAlbumNamesToGroup();
	}

	/**
	 * Gets library paths
	 */
	public HashSet<String> getLibraryPaths() {
		return getModelSettings().getLibraryPaths();
	}

	/**
	 * Gets the last sent play log ID
	 */
	public int getLastSentPlayLogId() {
		return getModelSettings().getLastSentPlayLogId();
	}

	/**
	 * Gets isLogFileEnabled
	 */
	public boolean isLogFileEnabled() {
		return getModelSettings().isLogFileEnabled();
	}

	/**
	 * Gets the current log file number
	 */
	public int getCurrentLogFileNumber() {
		return getModelSettings().getCurrentLogFileNumber();
	}

	/**
	 * Gets the log string
	 */
	public String getLogString() {
		return getModelSettings().getLogString();
	}

	/**
	 * Is the option <help to improve jukefox> active?
	 */
	public boolean isHelpImproveJukefox() {
		return getModelSettings().isHelpImproveJukefox();
	}

	/**
	 * Gets the coordinate version
	 */
	public int getCoordinateVersion() {
		return getModelSettings().getCoordinateVersion();
	}

	// -----=== SET ===-----

	/**
	 * Sets the number of started imports
	 * 
	 * @param numberOfStartedImports
	 *            The number ({@link Integer}) of started imports
	 */
	public void setNumberOfStartedImports(int numberOfStartedImports) {
		getModelSettings().setNumberOfStartedImports(numberOfStartedImports);
	}

	/**
	 * Sets the number of completed imports
	 * 
	 * @param numberOfStartedImports
	 *            The number ({@link Integer}) of started imports
	 */
	public void setNumberOfCompletedImports(int numberOfCompletedImports) {
		getModelSettings().setNumberOfCompletedImports(numberOfCompletedImports);
	}

	/**
	 * Are the famous artists inserted?
	 * 
	 * @param famousArtistsInserted
	 *            true if the famous artists are inserted; otherwise false
	 */
	public void setFamousArtistsInserted(boolean famousArtistsInserted) {
		getModelSettings().setFamousArtistsInserted(famousArtistsInserted);
	}

	/**
	 * Sets the album names to group
	 */
	public void setAlbumNamesToGroup(HashSet<String> albumNamesToGroup) {
		getModelSettings().setAlbumNamesToGroup(albumNamesToGroup);
	}

	/**
	 * Sets library paths
	 */
	public void setLibraryPaths(HashSet<String> libraryPaths) {
		getModelSettings().setLibraryPaths(libraryPaths);
	}

	/**
	 * Sets the last sent play log ID
	 */
	public void setLastSentPlayLogId(int lastSentPlayLogId) {
		getModelSettings().setLastSentPlayLogId(lastSentPlayLogId);
	}

	/**
	 * Sets logFileEnabled
	 */
	public void setLogFileEnabled(boolean logFileEnabled) {
		getModelSettings().setLogFileEnabled(logFileEnabled);
	}

	/**
	 * Sets the current log file number
	 */
	public void setCurrentLogFileNumber(int currentLogFileNumber) {
		getModelSettings().setCurrentLogFileNumber(currentLogFileNumber);
	}

	/**
	 * Set the option <help to improve jukefox>
	 */
	public void setHelpImproveJukefox(boolean isHelpImproveJukefox) {
		getModelSettings().setHelpImproveJukefox(isHelpImproveJukefox);
	}

	public void setCoordinateVersion(int coordinateVersion) {
		getModelSettings().setCoordinateVersion(coordinateVersion);
	}

	// -----=== METHODS ===-----

	/**
	 * Increases the recompute task id count by one
	 */
	public void incRecomputeTaskId() {
		getModelSettings().incRecomputeTaskId();
	}

	/**
	 * Resets the recompute task id count
	 */
	public void resetRecomputeTaskId() {
		getModelSettings().resetRecomputeTaskId();
	}

	/**
	 * Adds a given album name ({@link String})
	 * 
	 * @param albumNameToGroup
	 *            The album name to be added
	 */
	public void addAlbumNameToGroup(String albumNameToGroup) {
		getModelSettings().addAlbumNameToGroup(albumNameToGroup);
	}

	/**
	 * Removes a given album name ({@link String})
	 * 
	 * @param albumNameToGroup
	 *            The album name to be removed
	 */
	public void removeAlbumNameToGroup(String albumNameToGroup) {
		getModelSettings().removeAlbumNameToGroup(albumNameToGroup);
	}

	/**
	 * Adds a given library path ({@link String})
	 * 
	 * @param libraryPath
	 *            The path to be added
	 */
	public void addLibraryPath(String libraryPath) {
		getModelSettings().addLibraryPath(libraryPath);
	}

	/**
	 * Removes a given library path ({@link String})
	 * 
	 * @param libraryPath
	 *            The path to be removed
	 */
	public void removeLibraryPath(String libraryPath) {
		getModelSettings().removeLibraryPath(libraryPath);
	}

}
