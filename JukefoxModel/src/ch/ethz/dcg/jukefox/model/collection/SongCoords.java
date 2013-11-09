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

import ch.ethz.dcg.jukefox.model.commons.ModelUtils;

public class SongCoords {

	int id;
	float[] coords;

	public SongCoords(int id, float[] coords) {
		super();
		this.id = id;
		this.coords = coords;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public float[] getCoords() {
		return coords;
	}

	public void setCoords(float[] coords) {
		this.coords = coords;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o.getClass() != getClass()) {
			return false;
		}
		SongCoords c = (SongCoords) o;
		if (coords == null) {
			return getId() == c.getId() && c.getCoords() == null;
		}
		if (c.getCoords() == null) {
			return false;
		}

		return getId() == c.getId() && ModelUtils.equals(coords, c.getCoords());
	}

	@Override
	public int hashCode() {
		int idHash = new Integer(id).hashCode();
		int coordHash = coords == null ? 0 : ModelUtils.getHashCode(coords);
		return idHash ^ coordHash;
	}

}
