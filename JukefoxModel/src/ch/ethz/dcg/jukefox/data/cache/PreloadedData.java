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
package ch.ethz.dcg.jukefox.data.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.AdvancedKdTree;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.DateTag;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;

public class PreloadedData {

	private final static String TAG = PreloadedData.class.getSimpleName();

	private Random random;

	private AdvancedKdTree<Integer> kdTree;
	private ArrayList<Integer> idsOfSongsWithoutCoords;
	private ArrayList<Integer> idsOfSongsWithCoords;
	private HashMap<Integer, float[]> pcaSongCoords;

	private HashMap<Integer, CompleteTag> tags;
	private ArrayList<DateTag> sortedDateTags;

	public PreloadedData() {
		this.random = RandomProvider.getRandom();

	}

	public int getIdOfRandomSongWithCoords() throws DataUnavailableException {
		if (getNumberOfSongsWithCoords() == 0) {
			throw new DataUnavailableException("no ids of songs with coordinates available");
		}
		return getRandomId(idsOfSongsWithCoords);
	}

	public int getRandomSongId() throws DataUnavailableException {
		if (getNumberOfSongs() == 0) {
			// If the id-arrays are null (which should never be the case), a
			// null pointer exception is thrown. If the arrays are just empty, a
			// data unavailable exception is thrown.
			throw new DataUnavailableException("no song ids available");
		}
		int numWithCoords = getNumberOfSongsWithCoords();
		int numWithoutCoords = getNumberOfSongsWithoutCoords();
		float pWithCoords = (float) numWithCoords / (numWithCoords + numWithoutCoords);
		if (random.nextFloat() < pWithCoords) {
			return getRandomId(idsOfSongsWithCoords);
		}
		return getRandomId(idsOfSongsWithoutCoords);
	}

	/**
	 * Returns the number of songs in the collection
	 * 
	 * @return
	 */
	public int getNumberOfSongs() {
		return getNumberOfSongsWithCoords() + getNumberOfSongsWithoutCoords();
	}

	public void setSongCoords(List<SongCoords> songCoords) {
		kdTree = new AdvancedKdTree<Integer>(Constants.DIM);

		// first recompute all data structures
		idsOfSongsWithCoords = new ArrayList<Integer>(songCoords.size());
		for (SongCoords sc : songCoords) {
			idsOfSongsWithCoords.add(sc.getId());
			try {
				kdTree.insert(sc.getCoords(), sc.getId());
			} catch (KeySizeException e) {
				Log.w(TAG, e);
			}
		}
	}

	// public void setSongCoords(AdvancedKdTree<Integer> songKdTree, int[] ids)
	// {
	// this.kdTree = songKdTree;
	// this.ids = ids;
	// }

	public HashMap<Integer, float[]> getPcaCoords() {
		return pcaSongCoords;
	}

	public void setPcaCoords(HashMap<Integer, float[]> pcaCoords) {
		this.pcaSongCoords = pcaCoords;
	}

	public Collection<CompleteTag> getTags() {
		return tags.values();
	}

	public void setTags(Collection<CompleteTag> tags) {
		HashMap<Integer, CompleteTag> tempTags = new HashMap<Integer, CompleteTag>(tags.size());
		for (CompleteTag tag : tags) {
			tempTags.put(tag.getId(), tag);
			// Log.d(TAG, "Put tag with id " + tag.getId());
		}
		setTags(tempTags);

		Log.v(TAG, "set tags in list");
	}

	public void setTags(HashMap<Integer, CompleteTag> tags) {
		this.tags = tags;
		Log.v(TAG, "set tags in hashmap");
		calculateSortedDateTags();
		Log.v(TAG, "sorted date tags calculated.");
	}

	private void calculateSortedDateTags() {
		sortedDateTags = new ArrayList<DateTag>();
		for (CompleteTag t : tags.values()) {
			DateTag dateTag = DateTag.getDateTag(t);
			if (dateTag == null) {
				continue;
			}
			sortedDateTags.add(dateTag);
		}
		Collections.sort(sortedDateTags, new Comparator<DateTag>() {

			@Override
			public int compare(DateTag t1, DateTag t2) {
				if (t1.getTime() < t2.getTime()) {
					return -1;
				}
				if (t1.getTime() > t2.getTime()) {
					return 1;
				}
				return 0;
			}
		});
	}

	public Vector<KdTreePoint<Integer>> getSongsCloseToPosition(float[] position, int number) throws KeySizeException {
		return kdTree.getNearestPoints(position, number);
	}

	/**
	 * @see KDTree#nearestEuclidean(float[], float)
	 * @return The song nodes
	 */
	public Vector<KdTreePoint<Integer>> getSongsAroundPositionEuclidian(float[] position, float distance)
			throws KeySizeException {
		return kdTree.nearestEuclidean(position, distance);
	}

	/**
	 * @see KDTree#nearestHamming(float[], float)
	 * @return The song nodes
	 */
	public Vector<KdTreePoint<Integer>> getSongsAroundPositionHamming(float[] position, float distance)
			throws KeySizeException {
		return kdTree.nearestHamming(position, distance);
	}

	public CompleteTag getTagById(int id) {
		return tags.get(id);
	}

	public ArrayList<Integer> getAllSongIds() {
		ArrayList<Integer> ids = new ArrayList<Integer>(getNumberOfSongs());
		ids.addAll(idsOfSongsWithCoords);
		ids.addAll(idsOfSongsWithoutCoords);
		return ids;
	}

	/**
	 * Returns the number of songs for which the collection model has music similarity coordinates
	 * 
	 * @return
	 * @throws DataUnavailableException
	 *             if preloaded data is not yet loaded
	 */
	public int getNumberOfSongsWithCoords() {
		return idsOfSongsWithCoords.size();
	}

	/**
	 * Returns the number of songs for which the collection model has no music similarity coordinates
	 * 
	 * @return
	 */
	public int getNumberOfSongsWithoutCoords() {
		return idsOfSongsWithoutCoords.size();
	}

	private int getRandomId(ArrayList<Integer> ids) {
		int idx = random.nextInt(ids.size());
		return ids.get(idx);
	}

	public void setSongCoords(AdvancedKdTree<Integer> songKdTree, ArrayList<Integer> idsWithCoords,
			ArrayList<Integer> idsWithoutCoords) {
		Log.v(TAG, "set song coords: " + idsWithCoords.size() + ", " + idsWithoutCoords.size());
		kdTree = songKdTree;
		idsOfSongsWithCoords = idsWithCoords;
		idsOfSongsWithoutCoords = idsWithoutCoords;
	}

	public ArrayList<Integer> getIdsOfSongsWithCoords() {
		return idsOfSongsWithCoords;
	}

	public void setIdsOfSongsWithoutCoords(ArrayList<Integer> idsOfSongsWithoutCoords) {
		this.idsOfSongsWithoutCoords = idsOfSongsWithoutCoords;
	}

	public List<Integer> getIdsOfRandomSongsWithCoords(int numberOfSongs) {
		numberOfSongs = Math.min(numberOfSongs, idsOfSongsWithCoords.size());
		Log.v(TAG, "number of random songs with coordinates: " + idsOfSongsWithCoords.size());
		ArrayList<Integer> ids = new ArrayList<Integer>(numberOfSongs);
		ArrayList<Integer> positions = MathUtils.getRandomNumbers(idsOfSongsWithCoords.size(), numberOfSongs, random);
		for (Integer pos : positions) {
			ids.add(idsOfSongsWithCoords.get(pos));
		}
		return ids;
	}

	public ArrayList<DateTag> getSortedDateTags() {
		return sortedDateTags;
	}

	public List<Pair<CompleteTag, Float>> getTagsForCoordinate(float[] coords) {
		List<Pair<CompleteTag, Float>> albumTags = new ArrayList<Pair<CompleteTag, Float>>();
		for (CompleteTag tag : tags.values()) {
			Float prob = MathUtils.dotProduct(coords, tag.getPlsaCoords());
			Float mean = tag.getMeanPlsaProb();
			Float var = tag.getVariancePlsaProb();
			albumTags.add(new Pair<CompleteTag, Float>(tag, (prob - mean) / Float.valueOf((float) Math.sqrt(var))));
		}
		Collections.sort(albumTags, new Comparator<Pair<CompleteTag, Float>>() {

			@Override
			public int compare(Pair<CompleteTag, Float> object1, Pair<CompleteTag, Float> object2) {
				if (object1.second < object2.second) {
					return 1;
				} else if (object1.second > object2.second) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		return albumTags;
	}

}
