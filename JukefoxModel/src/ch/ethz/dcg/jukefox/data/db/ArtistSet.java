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
package ch.ethz.dcg.jukefox.data.db;

import java.util.HashSet;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class ArtistSet {

	private final static String TAG = ArtistSet.class.getSimpleName();

	private final HashSet<Integer> artistIds;
	private final int artistSetId;

	public ArtistSet(int artistSetId, HashSet<Integer> artistIds) {
		this.artistSetId = artistSetId;
		this.artistIds = artistIds;
	}

	public HashSet<Integer> getArtistIds() {
		return artistIds;
	}

	public Integer getArtistSetId() {
		return artistSetId;
	}

	@Override
	public int hashCode() {
		return DbUtils.getArtistSetHash(artistIds);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		try {
			ArtistSet as = (ArtistSet) o;
			if (artistSetId != as.artistSetId) {
				return false;
			}
			if (!artistIds.equals(as.artistIds)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			Log.w(TAG, e);
			return super.equals(o);
		}
	}

}
