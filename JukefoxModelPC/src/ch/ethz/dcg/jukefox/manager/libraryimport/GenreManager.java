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
import java.util.Iterator;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class GenreManager extends AbstractGenreManager {

	private final static String TAG = GenreManager.class.getSimpleName();

	public GenreManager(AbstractCollectionModelManager collectionModelManager, ImportState importState) {
		super(collectionModelManager, importState);
	}

	@Override
	public void updateGenres(LibraryChanges changes) {

		try {

			GenreSongMap newMap = generateNewGenreSongMap(changes);

			GenreSongMap dbMap = otherDataProvider.getGenreSongMappings();

			// removes all inserted mappings from dbMap, such that only those
			// that need to be removed remain .
			insertGenreSongMappings(newMap, dbMap);

			// can only occur if genre changes happens
			removeObsoleteGenreSongMappings(dbMap);

			modifyProvider.removeObsoleteGenres();

		} catch (DataWriteException e) {
			// TODO
			Log.w(TAG, e);
		} catch (DataUnavailableException e) {
			// TODO
			Log.w(TAG, e);
		}
	}

	private GenreSongMap generateNewGenreSongMap(LibraryChanges changes) {

		GenreSongMap genreSongMap = new GenreSongMap();
		HashMap<String, HashSet<Integer>> collectionSongGenreMap = changes.getCollectionSongGenreMap();
		HashMap<String, Integer> songPathToIdMapping = new HashMap<String, Integer>();
		Iterator<String> songPaths = collectionSongGenreMap.keySet().iterator();

		try {

			songPathToIdMapping = otherDataProvider.getAllSongPathToIdMappings();

			while (songPaths.hasNext()) {
				String songPath = songPaths.next();
				int songId = songPathToIdMapping.get(songPath);
				Iterator<Integer> genreIds = collectionSongGenreMap.get(songPath).iterator();

				while (genreIds.hasNext()) {
					genreSongMap.put(genreIds.next(), songId);
				}

			}

		} catch (DataUnavailableException e) {
			// TODO
		}
		return genreSongMap;
	}

}
