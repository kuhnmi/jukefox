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
import java.util.LinkedList;
import java.util.ListIterator;

public class KdTreeEntry<I> implements Serializable {

	private static final long serialVersionUID = -3221357954752823034L;
	protected float[] key;
	protected LinkedList<KdTreePoint<I>> points;

	public KdTreeEntry() {
	}

	public KdTreeEntry(float[] key, KdTreePoint<I> point) {
		this.key = key;
		points = new LinkedList<KdTreePoint<I>>();
		points.add(point);
	}

	public void addPoint(KdTreePoint<I> point) {
		points.add(point);
	}

	public void removePointByIndex(int index) {
		points.remove(index);
	}

	public void removePoint(I id) {
		ListIterator<KdTreePoint<I>> it = points.listIterator();
		while (it.hasNext()) {
			KdTreePoint<I> point = it.next();
			if (point.getID().equals(id)) {
				it.remove();
				return;
			}
		}
	}

	public void setPosition(int index, float[] position) {
		points.get(index).setPosition(position);
	}

	public void setID(int index, I id) {
		points.get(index).setID(id);
	}

	public void setKey(float[] key) {
		this.key = key;
	}

	public LinkedList<KdTreePoint<I>> getPoints() {
		return points;
	}

	public float[] getKey() {
		return key;
	}

}
