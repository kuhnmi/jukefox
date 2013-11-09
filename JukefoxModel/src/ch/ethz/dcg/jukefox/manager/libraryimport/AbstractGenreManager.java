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

import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap.GenreSongEntry;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.GenreProvider;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;

public abstract class AbstractGenreManager {

	private final static String TAG = AbstractGenreManager.class.getSimpleName();

	protected ImportState importState;
	protected ModifyProvider modifyProvider;
	protected OtherDataProvider otherDataProvider;
	protected GenreProvider genreProvider;
	protected SongProvider songProvider;

	public AbstractGenreManager(AbstractCollectionModelManager collectionModelManager, ImportState importState) {
		this.importState = importState;
		this.modifyProvider = collectionModelManager.getModifyProvider();
		this.otherDataProvider = collectionModelManager.getOtherDataProvider();
		this.genreProvider = collectionModelManager.getGenreProvider();
		this.songProvider = collectionModelManager.getSongProvider();
	}

	/**
	 * this function makes an update on the song/genre mappings, called by the
	 * library import manager after inserting new songs into database.
	 * 
	 * @return the Commands that should be executed upon the initialization of
	 *         the play mode;
	 */
	public abstract void updateGenres(LibraryChanges changes);

	protected void removeObsoleteGenreSongMappings(GenreSongMap toRemove) throws DataWriteException {
		Log.v(TAG, "removing obsolete genres/song mappings...");

		for (GenreSongEntry gse : toRemove.getAll()) {
			int songId = gse.getSongId();
			int genreId = gse.getGenreId();
			modifyProvider.deleteGenreSongMapping(genreId, songId);
		}
	}

	protected void insertGenreSongMappings(GenreSongMap cpMap, GenreSongMap dbMap) throws DataWriteException {
		Log.v(TAG, "inserting genres/song mappings...");

		importState.setBaseDataProgress(100, 100, "Inserting genres");

		GenreSongMap newMappings = new GenreSongMap();

		for (GenreSongEntry gse : cpMap.getAll()) {
			int genreId = gse.getGenreId();
			int songId = gse.getSongId();

			// remove mapping from dbMap to make sure only obsolete mappings
			// remain.
			if (!dbMap.remove(genreId, songId)) {
				// remove was unsuccessfull, i.e. the mapping is not yet in the
				// db => insert it.

				// Now we don't insert the mapping here but aggregate them and batch insert them later to speed the whole thing up.
				//				modifyProvider.insertSongGenreMapping(genreId, songId);
				newMappings.put(genreId, songId);
			}
		}
		modifyProvider.insertSongGenreMappings(newMappings);

	}

}