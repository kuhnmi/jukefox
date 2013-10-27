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
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId.Type;

public class AndroidGenreManager extends AbstractGenreManager {

	private final static String TAG = AndroidGenreManager.class.getSimpleName();
	private ContentResolver contentResolver;

	public AndroidGenreManager(AbstractCollectionModelManager collectionModelManager, ImportState importState,
			ContentResolver contentResolver) {
		super(collectionModelManager, importState);
		this.contentResolver = contentResolver;
	}

	@Override
	public void updateGenres(LibraryChanges changes) {
		HashMap<ContentProviderId, Integer> extGenreIdMap;

		try {
			extGenreIdMap = insertGenres(Type.EXTERNAL);

			GenreSongMap cpMap = readGenreSongMapFromCp(extGenreIdMap, changes.getContentProviderIdToJukefoxIdMap());

			GenreSongMap dbMap = otherDataProvider.getGenreSongMappings();

			// removes all inserted mappings from dbMap, such that only those
			// that need to be removed remain.
			insertGenreSongMappings(cpMap, dbMap);

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

	private GenreSongMap readGenreSongMapFromCp(HashMap<ContentProviderId, Integer> genreIdMap,
			HashMap<ContentProviderId, Integer> songIdMap) {

		GenreSongMap genreSongMap = new GenreSongMap();
		for (Entry<ContentProviderId, Integer> e : genreIdMap.entrySet()) {
			ContentProviderId cpGenreId = e.getKey();
			HashSet<Integer> songs = getSongsForGenre(cpGenreId, songIdMap);

			Integer genreId = e.getValue();
			genreSongMap.putSongs(genreId, songs);
		}
		return genreSongMap;
	}

	private HashSet<Integer> getSongsForGenre(ContentProviderId genreId, HashMap<ContentProviderId, Integer> songIdMap) {
		Cursor cur = null;
		try {
			HashSet<Integer> songs = new HashSet<Integer>();
			String[] projection = new String[] { MediaStore.Audio.Genres.Members._ID };
			Uri uri;
			if (genreId.getType() == Type.EXTERNAL) {
				uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId.getId());
			} else {
				// return songs; // TODO: how to handle internal songs
				// correctly?
				uri = MediaStore.Audio.Genres.Members.getContentUri("internal", genreId.getId());
			}
			cur = contentResolver.query(uri, projection, null, null, null);
			while (cur.moveToNext()) {
				ContentProviderId cpId = new ContentProviderId(cur.getInt(0), genreId.getType());
				Integer songId = songIdMap.get(cpId);
				if (songId == null) {
					// hmm... but can happen if the content provider content has
					// changed since we have set up the songId map.
					// Log.v(TAG, "No jukefoxId found for content provider "
					// + "songId. Maybe the content has changed during "
					// + "the import process?");
					continue;
				}
				songs.add(songId);
			}
			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private HashMap<ContentProviderId, Integer> insertGenres(Type type) throws DataWriteException {
		HashMap<ContentProviderId, Integer> idMap = new HashMap<ContentProviderId, Integer>();
		String[] projection = new String[] { MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME };
		Uri uri;
		if (type == Type.EXTERNAL) {
			uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
		} else {
			uri = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI;
		}
		Log.v(TAG, "uri: " + uri);
		Cursor cur = null;
		try {
			cur = contentResolver.query(uri, projection, null, null, null);
			Log.v(TAG, "Number of genres from content provider: " + cur.getCount());
			while (cur.moveToNext()) {
				ContentProviderId cpId = new ContentProviderId(cur.getInt(0), type);
				String name = cur.getString(1);
				int jukefoxId = modifyProvider.insertGenre(name);
				idMap.put(cpId, jukefoxId);
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		return idMap;
	}
}
