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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;

public class LibraryChanges {

	private final static String TAG = LibraryChanges.class.getSimpleName();

	private Set<ImportSong> songsToRemove;
	private Set<ImportSong> songsToAdd;
	private Set<ImportSong> songsToChange;

	// TODO cannot stay here ... needs other solution
	private HashMap<ContentProviderId, Integer> contentProviderIdToJukefoxIdMap;
	private HashMap<String, HashSet<Integer>> collectionSongGenreMap;

	public LibraryChanges() {
		songsToRemove = new HashSet<ImportSong>();
		songsToAdd = new HashSet<ImportSong>();
		songsToChange = new HashSet<ImportSong>();

		// TODO cannot stay here ... needs other solution
		contentProviderIdToJukefoxIdMap = new HashMap<ContentProviderId, Integer>();
		collectionSongGenreMap = new HashMap<String, HashSet<Integer>>();
	}

	public Set<ImportSong> getSongsToRemove() {
		return songsToRemove;
	}

	public Set<ImportSong> getSongsToAdd() {
		return songsToAdd;
	}

	public Set<ImportSong> getSongsToChange() {
		return songsToChange;
	}

	public void addSongToRemove(ImportSong song) {
		songsToRemove.add(song);
	}

	public void addSongToAdd(ImportSong song) {
		songsToAdd.add(song);
	}

	public void addSongToChange(ImportSong song) {
		songsToChange.add(song);
	}

	public boolean hasChanges() {
		int numChanges = songsToRemove.size() + songsToAdd.size() + songsToChange.size();
		return numChanges != 0;
	}

	public void printLogD() {
		Log.d(TAG, "number of songs to remove: " + songsToRemove.size());
		Log.d(TAG, "number of songs to add: " + songsToAdd.size());
		Log.d(TAG, "number of songs to change: " + songsToChange.size());
	}

	public HashMap<ContentProviderId, Integer> getContentProviderIdToJukefoxIdMap() {
		return contentProviderIdToJukefoxIdMap;
	}

	public HashMap<String, HashSet<Integer>> getCollectionSongGenreMap() {
		return collectionSongGenreMap;
	}

}
