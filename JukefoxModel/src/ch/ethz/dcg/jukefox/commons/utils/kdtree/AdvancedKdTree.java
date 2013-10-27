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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import edu.wlu.cs.levy.CG.Checker;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class AdvancedKdTree<I> implements Serializable {

	private static final long serialVersionUID = -6342366349021652876L;

	private static final String TAG = AdvancedKdTree.class.getSimpleName();

	protected KDTree<KdTreeEntry<I>> kdTree;
	protected int size;
	protected int distinctCoordsCnt;

	protected float initialMargin = 0.5f;
	protected float margin;
	protected int dim;

	protected Hashtable<I, KdTreeEntry<I>> table;

	protected int deletionCount = 0;
	// as soon as deletionCount / size > rebuildThreshold, the tree is going
	// to be reconstructed.
	protected float rebuildThreshold = 0.3f;
	protected int rebuildCount = 0;

	public AdvancedKdTree(int dim) {
		this.dim = dim;
		margin = initialMargin;
		initDataStructure();
	}

	public void insert(float[] key, I id) throws KeySizeException {
		KdTreePoint<I> point = new KdTreePoint<I>(key, id);
		insert(key, point);
	}

	public void insert(float[] key, KdTreePoint<I> point) throws KeySizeException {
		if (key == point.getPosition()) {
			key = key.clone();
		}
		KdTreeEntry<I> entry = new KdTreeEntry<I>(key, point);
		try {
			kdTree.insert(key, entry);
			distinctCoordsCnt++;
		} catch (KeyDuplicateException e) {
			entry = kdTree.search(key);
			entry.addPoint(point);
		}
		table.put(point.getID(), entry);
		size++;
	}

	public LinkedList<KdTreePoint<I>> getRange(float[] lowerBounds, float[] upperBounds) throws KeySizeException {
		Vector<KdTreeEntry<I>> entries = getExtendedRange(lowerBounds, upperBounds);
		LinkedList<KdTreePoint<I>> points = new LinkedList<KdTreePoint<I>>();
		for (int i = 0; i < entries.size(); i++) {
			ListIterator<KdTreePoint<I>> it = entries.elementAt(i).getPoints().listIterator();
			while (it.hasNext()) {
				KdTreePoint<I> point = it.next();
				if (rangeContainsPoint(lowerBounds, upperBounds, point)) {
					points.add(point);
				}
			}
		}
		return points;
	}

	public LinkedList<KdTreePoint<I>> getAll() {
		Iterator<Entry<I, KdTreeEntry<I>>> it = table.entrySet().iterator();
		LinkedList<KdTreePoint<I>> points = new LinkedList<KdTreePoint<I>>();
		while (it.hasNext()) {
			Entry<I, KdTreeEntry<I>> entry = it.next();
			ListIterator<KdTreePoint<I>> pointIt = entry.getValue().getPoints().listIterator();
			while (pointIt.hasNext()) {
				KdTreePoint<I> point = pointIt.next();
				if (point.getID().equals(entry.getKey())) {
					points.add(point);
				}
			}
		}
		return points;
	}

	public KdTreePoint<I> getPoint(I id) {
		KdTreeEntry<I> entry = table.get(id);
		if (entry == null) {
			return null;
		}
		ListIterator<KdTreePoint<I>> it = entry.getPoints().listIterator();
		KdTreePoint<I> point = it.next();
		while (!point.getID().equals(id) && it.hasNext()) {
			point = it.next();
		}
		return point;
	}

	/**
	 * Moves the point to a
	 * 
	 * @return new key, null if ID does not exist.
	 */
	public float[] movePoint(I id, float[] newPosition) throws KeySizeException {
		KdTreePoint<I> point = getPoint(id);
		if (point == null) {
			return null;
		}
		point.setPosition(newPosition);
		KdTreeEntry<I> entry = table.get(id);
		if (isOutsideMargin(point, entry)) {
			if (entry.getPoints().size() > 1) {
				entry.removePoint(id);
			} else {
				try {
					kdTree.delete(entry.getKey());
					deletionCount++;
					if ((float) deletionCount / (float) size > rebuildThreshold) {
						rebuild();
						return table.get(id).getKey();
					}
				} catch (KeySizeException e) {
					throw e;
				} catch (Exception e) {
					Log.w(TAG, "KeyMissingException in " + "AdvancedKDTree.movePoint(). " + "Should never happen...");
					// System.exit(0);
				}
			}
			table.remove(id);
			insert(newPosition, point);
			return newPosition;
		}
		return entry.getKey();
	}

	public Vector<KdTreeEntry<I>> getNearest(float[] p, int n) throws KeySizeException {
		if (n == 0) {
			return new Vector<KdTreeEntry<I>>(0);
		}
		// Object[] objects = kdTree.nearest(p, Math.min(distinctCoordsCnt, n));
		List<KdTreeEntry<I>> objects = kdTree.nearest(p, Math.min(distinctCoordsCnt, n));
		// Vector<I> ret = new Vector<I>();
		Vector<KdTreeEntry<I>> entries = new Vector<KdTreeEntry<I>>(objects.size());
		for (int i = 0; i < objects.size(); i++) {
			KdTreeEntry<I> entry = objects.get(i);
			entries.add(entry);
		}
		return entries;
	}

	public Vector<KdTreeEntry<I>> getNearest(float[] p, int n, Checker<KdTreeEntry<I>> c) throws KeySizeException {
		if (n == 0) {
			return new Vector<KdTreeEntry<I>>(0);
		}
		// Object[] objects = kdTree.nearest(p, Math.min(distinctCoordsCnt, n));
		List<KdTreeEntry<I>> objects = kdTree.nearest(p, Math.min(distinctCoordsCnt, n), c);
		// Vector<I> ret = new Vector<I>();
		Vector<KdTreeEntry<I>> entries = new Vector<KdTreeEntry<I>>(objects.size());
		for (int i = 0; i < objects.size(); i++) {
			KdTreeEntry<I> entry = objects.get(i);
			entries.add(entry);
		}

		return entries;
	}

	public Vector<KdTreePoint<I>> getNearestPoints(float[] p, int minN) throws KeySizeException {
		minN = Math.min(minN, size);
		Vector<KdTreePoint<I>> ret = new Vector<KdTreePoint<I>>();
		Vector<KdTreeEntry<I>> entries = getNearest(p, minN);
		int cnt = 0;
		int i = 0;
		while (cnt < minN) {
			KdTreeEntry<I> entry = entries.get(i++);
			for (KdTreePoint<I> point : entry.getPoints()) {
				ret.add(point);
				cnt++;
			}
		}
		return ret;
	}

	public Vector<I> getNearestPoints(float[] p, int minN, Checker<KdTreeEntry<I>> c) throws KeySizeException {
		Vector<I> ret = new Vector<I>();
		Vector<KdTreeEntry<I>> entries = getNearest(p, minN, c);
		int cnt = 0;
		int i = 0;
		while (cnt < minN) {
			KdTreeEntry<I> entry = entries.get(i++);
			for (KdTreePoint<I> point : entry.getPoints()) {
				ret.add(point.getID());
				cnt++;
			}
		}
		return ret;
	}

	/**
	 * @see KDTree#nearestEuclidean(float[], float)
	 * @return The element nodes
	 */
	public Vector<KdTreePoint<I>> nearestEuclidean(float[] key, float dist) throws KeySizeException {
		List<KdTreeEntry<I>> entries = kdTree.nearestEuclidean(key, dist);
		return getIdsFromKdTreeEntryList(entries);
	}

	/**
	 * @see KDTree#nearestHamming(float[], float)
	 * @return The element nodes
	 */
	public Vector<KdTreePoint<I>> nearestHamming(float[] key, float dist) throws KeySizeException {
		List<KdTreeEntry<I>> entries = kdTree.nearestHamming(key, dist);
		return getIdsFromKdTreeEntryList(entries);
	}

	/**
	 * Reads the element points from the given {@link List<KdTreeEntry<I>>} into a {@link Vector<KdTreePoint<I>>}.
	 * 
	 * @param entries
	 *            The entries
	 * @return The entry points
	 */
	private Vector<KdTreePoint<I>> getIdsFromKdTreeEntryList(List<KdTreeEntry<I>> entries) {
		Vector<KdTreePoint<I>> ret = new Vector<KdTreePoint<I>>();
		for (KdTreeEntry<I> entry : entries) {
			ret.addAll(entry.getPoints());
		}
		return ret;
	}

	public boolean contains(I item) {
		return table.containsKey(item);
	}

	public int getSize() {
		return size;
	}

	public int getDeletionCount() {
		return deletionCount;
	}

	public int getRebuildCount() {
		return rebuildCount;
	}

	public void setMargin(float margin) {
		this.margin = margin;
	}

	public double getMaring() {
		return margin;
	}

	protected void initDataStructure() {
		kdTree = new KDTree<KdTreeEntry<I>>(dim);
		table = new Hashtable<I, KdTreeEntry<I>>();
	}

	protected void rebuild() {
		LinkedList<KdTreePoint<I>> points = getAll();
		initDataStructure();
		ListIterator<KdTreePoint<I>> it = points.listIterator();
		while (it.hasNext()) {
			KdTreePoint<I> point = it.next();
			try {
				insert(point.getPosition().clone(), point);
			} catch (KeySizeException e) {
				Log.w(TAG, "Should never happen. " + "AdvancedKDTree.rebuild");
			}
		}
		deletionCount = 0;
		rebuildCount++;
	}

	protected Vector<KdTreeEntry<I>> getExtendedRange(float[] lowerBounds, float[] upperBounds) throws KeySizeException {
		float[] lower = new float[lowerBounds.length];
		float[] upper = new float[upperBounds.length];
		for (int i = 0; i < lowerBounds.length; i++) {
			lower[i] = lowerBounds[i] - margin;
			upper[i] = upperBounds[i] + margin;
		}
		// Object[] objects = kdTree.range(lower, upper);
		List<KdTreeEntry<I>> objects = kdTree.range(lower, upper);
		Vector<KdTreeEntry<I>> entries = new Vector<KdTreeEntry<I>>(objects.size());
		for (int i = 0; i < objects.size(); i++) {
			entries.add(objects.get(i));
		}
		return entries;
	}

	protected boolean rangeContainsPoint(float[] lowerBounds, float[] upperBounds, KdTreePoint<I> point) {
		float[] pos = point.getPosition();
		for (int i = 0; i < lowerBounds.length; i++) {
			if (pos[i] < lowerBounds[i] || pos[i] > upperBounds[i]) {
				return false;
			}
		}
		return true;
	}

	protected boolean isOutsideMargin(KdTreePoint<I> point, KdTreeEntry<I> entry) {
		float[] pos = point.getPosition();
		float[] key = entry.getKey();
		for (int i = 0; i < pos.length; i++) {
			if (pos[i] > key[i] + margin || pos[i] < key[i] - margin) {
				return true;
			}
		}
		return false;
	}

}
