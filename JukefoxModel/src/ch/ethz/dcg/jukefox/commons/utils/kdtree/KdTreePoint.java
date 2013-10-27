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
package ch.ethz.dcg.jukefox.commons.utils.kdtree;

import java.io.Serializable;

public class KdTreePoint<I> implements Serializable {

	private static final long serialVersionUID = 1L;
	protected float[] position;
	protected I id;

	public KdTreePoint(float[] position, I id) {
		this.position = position;
		this.id = id;
	}

	public float[] getPosition() {
		return position;
	}

	public I getID() {
		return id;
	}

	public void setPosition(float[] position) {
		this.position = position;
	}

	public void setID(I id) {
		this.id = id;
	}

}
