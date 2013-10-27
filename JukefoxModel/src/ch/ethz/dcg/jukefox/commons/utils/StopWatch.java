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
package ch.ethz.dcg.jukefox.commons.utils;

public class StopWatch {

	/**
	 * The start time [in ns].
	 */
	private final long startTime;
	/**
	 * The stop time [in ns].
	 */
	private Long stopTime = null;

	private StopWatch() {
		startTime = System.nanoTime();
	}

	public static StopWatch start() {
		return new StopWatch();
	}

	/**
	 * Stops the watch and returns the elapsed time.
	 * 
	 * @return The elapsed time [in ms]
	 * @see #getTime()
	 */
	public long stop() {
		stopTime = System.nanoTime();
		return getTime();
	}

	/**
	 * Returns the elapsed time.
	 * 
	 * @return The time [in ms]
	 */
	public long getTime() {
		long end = stopTime;
		if (stopTime == null) {
			end = System.nanoTime();
		}

		return (end - startTime) / 1000 / 1000; // ns -> ms
	}
}
