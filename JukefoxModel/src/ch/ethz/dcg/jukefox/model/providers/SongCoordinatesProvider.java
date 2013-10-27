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
package ch.ethz.dcg.jukefox.model.providers;

import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;

/**
 * Provides all possible data access options for song coordinates-objects
 */
public class SongCoordinatesProvider {

	private static final String TAG = SongCoordinatesProvider.class.getSimpleName();
	private final IDbDataPortal dbDataPortal;
	private final PreloadedDataManager preloadedDataManager;

	/**
	 * Creates a new instance of {@link SongCoordinatesProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param preloadedDataManager
	 *            The preloaded data manager which will be used
	 */
	public SongCoordinatesProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
	}

	// /**
	// * Gets a list of {@link SongCoords} of random songs
	// *
	// * @param numberOfSongs
	// * The maximum numbers ({@link Integer}) of results
	// * @return A list of {@link SongCoords} of random songs
	// */
	// public List<SongCoords> getRandomSongsWithCoords(int numberOfSongs)
	// throws DataUnavailableException {
	// List<SongCoords> songCoords = new ArrayList<SongCoords>(numberOfSongs);
	// List<Integer> ids =
	// preloadedDataManager.getData().getIdsOfRandomSongsWithCoords(numberOfSongs);
	//
	// for (Integer id : ids) {
	// songCoords.add(dbDataPortal.getSongCoordsById(id));
	// }
	// return songCoords;
	// }

	/**
	 * Gets the {@link SongCoords} of a given {@link BaseSong}
	 * 
	 * @param baseSong
	 *            The {@link BaseSong} of the returned {@link SongCoords}
	 * @return The {@link SongCoords} of the given {@link BaseSong}
	 */
	public SongCoords getSongCoordinates(BaseSong<BaseArtist, BaseAlbum> baseSong) throws DataUnavailableException {
		float[] coords = dbDataPortal.getCoordsForSongById(baseSong.getId());
		return new SongCoords(0, coords);
	}

	/**
	 * Gets a list of all available {@link SongCoords}
	 * 
	 * @param includeSongWithoutCoords
	 *            Include songs without {@link SongCoords}? ({@link Boolean})
	 * @return A list of all available {@link SongCoords}
	 */
	public List<SongCoords> getSongCoords(boolean includeSongWithoutCoords) {
		return dbDataPortal.getSongCoords(includeSongWithoutCoords);
	}

	public List<SongCoords> getRandomSongsWithCoords(int numberOfSongs) throws DataUnavailableException {

		List<Integer> ids = preloadedDataManager.getData().getIdsOfRandomSongsWithCoords(numberOfSongs);

		return dbDataPortal.getSongCoordsById(ids);
	}
}
