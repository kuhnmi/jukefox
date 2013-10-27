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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class Progress {

	public static final String TAG = Progress.class.getSimpleName();

	private String statusMessage;

	private int overallProgress;
	private int overallMaxProgress;

	private int baseDataProgress;
	private int baseDataMaxProgress;

	private int coordinatesProgress;
	private int coordinatesMaxProgress;

	private int albumArtProgress;
	private int albumArtMaxProgress;

	private int mapDataProgress;
	private int mapDataMaxProgress;

	private int collectionPropertiesProgress;
	private int collectionPropertiesMaxProgress;

	private boolean isImportFinished;

	public Progress(String statusMessage, boolean isImportFinished) {
		reset();
		this.statusMessage = statusMessage;
		this.isImportFinished = isImportFinished;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public int getOverallProgress() {
		overallProgress = 0;

		overallProgress += albumArtMaxProgress == 0 ? 100 : albumArtProgress * 100 / albumArtMaxProgress;

		overallProgress += coordinatesMaxProgress == 0 ? 100 : coordinatesProgress * 100 / coordinatesMaxProgress;

		overallProgress += baseDataMaxProgress == 0 ? 100 : baseDataProgress * 100 / baseDataMaxProgress;

		overallProgress += mapDataMaxProgress == 0 ? 100 : mapDataProgress * 100 / mapDataMaxProgress;

		return overallProgress;
	}

	public void logProgress() {
		overallProgress = 0;

		Log.v(TAG, "album art progress: "
				+ (albumArtMaxProgress == 0 ? 100 : albumArtProgress * 100 / albumArtMaxProgress));

		Log.v(TAG, "coordinates progress: "
				+ (coordinatesMaxProgress == 0 ? 100 : coordinatesProgress * 100 / coordinatesMaxProgress));

		Log.v(TAG, "base data progress: "
				+ (baseDataMaxProgress == 0 ? 100 : baseDataProgress * 100 / baseDataMaxProgress));

		Log
				.v(TAG, "map data progress: " + (mapDataMaxProgress == 0 ? 100
						: mapDataProgress * 100 / mapDataMaxProgress));
	}

	@Override
	public String toString() {
		overallProgress = 0;

		String res = "album art progress: "
				+ (albumArtMaxProgress == 0 ? 100 : albumArtProgress * 100 / albumArtMaxProgress) + "\n";

		res += "coordinates progress: "
				+ (coordinatesMaxProgress == 0 ? 100 : coordinatesProgress * 100 / coordinatesMaxProgress) + "\n";

		res += "base data progress: "
				+ (baseDataMaxProgress == 0 ? 100 : baseDataProgress * 100 / baseDataMaxProgress) + "\n";

		res += "map data progress: " + (mapDataMaxProgress == 0 ? 100
				: mapDataProgress * 100 / mapDataMaxProgress) + "\n";

		res += "collection properties progress: "
				+ (collectionPropertiesProgress == 0 ? 100
						: collectionPropertiesProgress * 100 / collectionPropertiesMaxProgress) + "\n";

		res += "Currently doing: " + getStatusMessage();
		return res;
	}

	// public void setOverallProgress(int overallProgress) {
	// this.overallProgress = overallProgress;
	// }

	public int getOverallMaxProgress() {
		overallMaxProgress = 400;
		return overallMaxProgress;
	}

	// public void setOverallMaxProgress(int overallMaxProgress) {
	// this.overallMaxProgress = overallMaxProgress;
	// }

	public int getBaseDataProgress() {
		return baseDataProgress;
	}

	public void setBaseDataProgress(int baseDataProgress) {
		this.baseDataProgress = baseDataProgress;
	}

	public int getBaseDataMaxProgress() {
		return baseDataMaxProgress;
	}

	public void setBaseDataMaxProgress(int baseDataMaxProgress) {
		this.baseDataMaxProgress = baseDataMaxProgress;
	}

	public int getCoordinatesProgress() {
		return coordinatesProgress;
	}

	public void setCoordinatesProgress(int coordinatesProgress) {
		this.coordinatesProgress = coordinatesProgress;
	}

	public int getCoordinatesMaxProgress() {
		return coordinatesMaxProgress;
	}

	public void setCoordinatesMaxProgress(int coordinatesMaxProgress) {
		this.coordinatesMaxProgress = coordinatesMaxProgress;
	}

	public int getAlbumArtProgress() {
		return albumArtProgress;
	}

	public void setAlbumArtProgress(int albumArtProgress) {
		this.albumArtProgress = albumArtProgress;
	}

	public int getAlbumArtMaxProgress() {
		return albumArtMaxProgress;
	}

	public void setAlbumArtMaxProgress(int albumArtMaxProgress) {
		this.albumArtMaxProgress = albumArtMaxProgress;
	}

	public int getMapDataProgress() {
		return mapDataProgress;
	}

	public void setMapDataProgress(int mapDataProgress) {
		this.mapDataProgress = mapDataProgress;
	}

	public int getMapDataMaxProgress() {
		return mapDataMaxProgress;
	}

	public void setMapDataMaxProgress(int mapDataMaxProgress) {
		this.mapDataMaxProgress = mapDataMaxProgress;
	}

	public int getCollectionPropertiesDataProgress() {
		return collectionPropertiesProgress;
	}

	public void setCollectionPropertiesProgress(int collectionPropertiesProgress) {
		this.collectionPropertiesProgress = collectionPropertiesProgress;
	}

	public int getCollectionPropertiesMaxProgress() {
		return collectionPropertiesMaxProgress;
	}

	public void setCollectionPropertiesMaxProgress(int collectionPropertiesMaxProgress) {
		this.collectionPropertiesMaxProgress = collectionPropertiesMaxProgress;
	}

	public boolean isImportFinished() {
		return isImportFinished;
	}

	public void setImportFinished(boolean isImportFinished) {
		this.isImportFinished = isImportFinished;
	}

	public void reset() {
		overallProgress = 0;
		overallMaxProgress = 1;

		baseDataProgress = 0;
		baseDataMaxProgress = 1;

		coordinatesProgress = 0;
		coordinatesMaxProgress = 1;

		albumArtProgress = 0;
		albumArtMaxProgress = 1;

		mapDataProgress = 0;
		mapDataMaxProgress = 1;
	}

}
