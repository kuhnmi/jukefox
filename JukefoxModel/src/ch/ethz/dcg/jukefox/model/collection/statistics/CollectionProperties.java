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
package ch.ethz.dcg.jukefox.model.collection.statistics;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;

/**
 * A container class which holds some statistics data about the music collection.
 */
public class CollectionProperties {

	private Double avgSongDistance;
	private Double songDistanceStdDeviation;

	public CollectionProperties() {
		avgSongDistance = null;
		songDistanceStdDeviation = null;
	}

	// *** Getters *** //

	/**
	 * Returns the average distance between two songs. If the data is not yet available a
	 * {@link DataUnavailableException} is thrown.
	 * 
	 * @return The average song distance
	 * @throws DataUnavailableException
	 */
	public double getAverageSongDistance() throws DataUnavailableException {
		if (!hasAverageSongDistance()) {
			throw new DataUnavailableException();
		}

		return avgSongDistance;
	}

	/**
	 * Returns the standard deviation of the song distances. If the data is not yet available a
	 * {@link DataUnavailableException} is thrown.
	 * 
	 * @return The standard deviation of the song distances
	 * @throws DataUnavailableException
	 */
	public double getSongDistanceStdDeviation() throws DataUnavailableException {
		if (!hasSongDistanceStdDeviation()) {
			throw new DataUnavailableException();
		}

		return songDistanceStdDeviation;
	}

	// *** Safe getters *** //

	/**
	 * Exception free version of {@link #getAverageSongDistance()}.
	 * 
	 * @return The average song distance
	 */
	public double getAverageSongDistanceSafe() {
		try {
			return getAverageSongDistance();
		} catch (DataUnavailableException e) {
			assert false;
			return 0;
		}
	}

	/**
	 * Exception free version of {@link #getSongDistanceStdDeviation()}.
	 * 
	 * @return The standard deviation of the song distances
	 */
	public double getSongDistanceStdDeviationSafe() {
		try {
			return getSongDistanceStdDeviation();
		} catch (DataUnavailableException e) {
			assert false;
			return 0;
		}
	}

	// *** Checkers *** //

	/**
	 * Returns true, if the average song distance property is set.
	 * 
	 * @return
	 */
	public boolean hasAverageSongDistance() {
		return avgSongDistance != null;
	}

	/**
	 * Returns true, if the song distance standard deviation property is set.
	 * 
	 * @return
	 */
	public boolean hasSongDistanceStdDeviation() {
		return songDistanceStdDeviation != null;
	}

	// *** Setters *** //

	public void setAverageSongDistance(double avgSongDistance) {
		this.avgSongDistance = avgSongDistance;
	}

	public void setSongDistanceStdDeviation(double songDistanceStdDeviation) {
		this.songDistanceStdDeviation = songDistanceStdDeviation;
	}

}
