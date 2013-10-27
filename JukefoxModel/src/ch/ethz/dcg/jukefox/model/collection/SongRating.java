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
package ch.ethz.dcg.jukefox.model.collection;

public class SongRating implements IRating {

	public static final String TAG = SongRating.class.getSimpleName();

	private Float explicitRating;
	private int numTimesSkipped;
	private int numTimesNotSkipped;

	@Override
	public Float getExplicitRating() {
		return explicitRating;
	}

	@Override
	public Float getImplicitRating() {
		if (numTimesSkipped == 0) {
			if (numTimesNotSkipped == 0) {
				return null;
			} else {
				return 1f;
			}
		} else {
			if (numTimesNotSkipped == 0) {
				return -1f;
			} else {
				if (numTimesSkipped > numTimesNotSkipped) {
					return -1f;
				} else if (numTimesSkipped < numTimesNotSkipped) {
					return 1f;
				} else {
					return 0f;
				}
			}
		}
	}

}
