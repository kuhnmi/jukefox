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
package ch.ethz.dcg.jukefox.model.libraryimport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class GenreSongMap {

	public static class GenreSongEntry {

		private final int genreId;
		private final int songId;

		public GenreSongEntry(int genreId, int songId) {
			this.genreId = genreId;
			this.songId = songId;
		}

		public int getGenreId() {
			return genreId;
		}

		public int getSongId() {
			return songId;
		}
	}

	private final static String TAG = GenreSongMap.class.getSimpleName();

	private HashMap<Integer, HashSet<Integer>> map;

	public GenreSongMap() {
		map = new HashMap<Integer, HashSet<Integer>>();
	}

	public void put(Integer genreId, Integer songId) {
		HashSet<Integer> songs = map.get(genreId);
		if (songs == null) {
			songs = new HashSet<Integer>();
			map.put(genreId, songs);
		}
		songs.add(songId);
	}

	public void putSongs(Integer genreId, HashSet<Integer> songs) {
		map.put(genreId, songs);
	}

	public void putAll(GenreSongMap inc) {
		for (Entry<Integer, HashSet<Integer>> e : inc.map.entrySet()) {
			Integer genreId = e.getKey();
			HashSet<Integer> songs = map.get(genreId);
			if (songs == null) {
				songs = new HashSet<Integer>();
				map.put(genreId, songs);
			}
			songs.addAll(e.getValue());
		}
	}

	public boolean remove(Integer genreId, Integer songId) {
		HashSet<Integer> songs = map.get(genreId);
		if (songs == null) {
			return false;
		}
		if (songs.size() == 0) {
			Log.w(TAG, "should never happen");
			return false;
		}
		boolean success = songs.remove(songId);
		if (songs.size() == 0) {
			map.remove(genreId);
		}
		return success;
	}

	public List<GenreSongEntry> getAll() {
		LinkedList<GenreSongEntry> list = new LinkedList<GenreSongEntry>();
		for (Entry<Integer, HashSet<Integer>> e : map.entrySet()) {
			int genreId = e.getKey();
			for (Integer songId : e.getValue()) {
				list.add(new GenreSongEntry(genreId, songId));
			}
		}
		return list;
	}

}
