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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.data.cache.PreloadedData;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.cache.PreloadedSongInfo;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.db.PcaCoordinatesUnavailableException;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.CollectionProperties;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;

/**
 * Provides all possible data access options for default data objects like {@link Integer}, {@link Float} and other
 * types which don't exist in another provider
 */
public class OtherDataProvider {

	private final IDbDataPortal dbDataPortal;
	private final PreloadedDataManager preloadedDataManager;

	/**
	 * Creates a new instance of {@link OtherDataProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param preloadedDataManager
	 *            The preloaded data manager which will be used
	 */
	public OtherDataProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
	}

	// ----------========### BASE TYPES ###========----------

	// ----- BOOLEAN -----

	/**
	 * Has the artist with the given ID coordinates? ({@link Boolean})
	 * 
	 * @param meArtistId
	 *            The music explorer ID ({@link Integer}) of the artist
	 * @return True if the artist with the given ID has coordinates; otherwise false
	 */
	public boolean hasArtistCoords(int meArtistId) {
		return dbDataPortal.hasArtistCoords(meArtistId);
	}

	// ----- FLOAT -----

	/**
	 * Gets the coordinates ({@link Array} of {@link Float}) of the given {@link BaseSong}
	 * 
	 * @param baseSong
	 *            A {@link BaseSong} of the returned coordinates
	 * @return The coordinates of the given {@link BaseSong}
	 */
	public float[] getSongCoordinates(BaseSong<BaseArtist, BaseAlbum> baseSong) throws DataUnavailableException {
		return dbDataPortal.getCoordsForSongById(baseSong.getId());
	}

	/**
	 * Gets the coordinates ({@link Array} of {@link Float}) of the given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            A {@link BaseAlbum} of the returned coordinates
	 * @return The coordinates of the given {@link BaseAlbum}
	 */
	public float[] getSongCoordinates(BaseAlbum baseAlbum) throws DataUnavailableException {
		return dbDataPortal.getCoordsForAlbum(baseAlbum);
	}

	/**
	 * Returns the PCA coordinates (2D) of a Song
	 * 
	 * @param songId
	 * @return the PCA coordinates in a float array of length 2
	 * @throws DataUnavailableException
	 *             if no PCA coordinates are available for the specififed song
	 */
	public float[] getSongPcaCoords(int songId) throws DataUnavailableException {
		float[] coords = preloadedDataManager.getData().getPcaCoords().get(songId);
		if (coords == null) {
			throw new PcaCoordinatesUnavailableException("No coordinates available for specified song id");
		}
		return coords;
	}

	// ----- INTEGER -----

	/**
	 * Gets the ID ({@link Integer}) of a given tag
	 * 
	 * @param tagName
	 *            The name ({@link String}) of the tag
	 * @param onlyRelevantTags
	 *            Only return relevant tags? ({@link Boolean})
	 * @return The ID of a given tag
	 */
	public Integer getTagId(String tagName, boolean onlyRelevantTags) throws DataUnavailableException {
		return dbDataPortal.getTagId(tagName, onlyRelevantTags);
	}

	/**
	 * Gets a list of all available song IDs ({@link Integer}) for a given album Id
	 * 
	 * @param albumId
	 *            The ID ({@link Integer}) of the album
	 * @return A list of all available song IDs for a given album Id
	 */
	public List<Integer> getAllSongIds(int albumId) {
		return dbDataPortal.getSongIdsForAlbum(albumId);
	}

	/**
	 * Gets the music explorer artist ID ({@link Integer}) for a given {@link BaseArtist}
	 * 
	 * @param baseArtist
	 *            The {@link BaseArtist} of the returned music explorer artist ID
	 * @return The music explorer artist ID for a given {@link BaseArtist}
	 */
	public Integer getMusicExplorerArtistId(BaseArtist baseArtist) throws DataUnavailableException {
		return dbDataPortal.getMusicExplorerArtistId(baseArtist);
	}

	/**
	 * Gets the music explorer song ID ({@link Integer}) for a given {@link BaseSong}
	 * 
	 * @param baseSong
	 *            The {@link BaseSong} of the returned music explorer song ID
	 * @return The music explorer song ID for a given {@link BaseSong}
	 */
	public Integer getMusicExplorerSongId(BaseSong<BaseArtist, BaseAlbum> baseSong) throws DataUnavailableException {
		return dbDataPortal.getMusicExplorerIdForSong(baseSong);
	}

	/**
	 * Gets a random song ID ({@link Integer})
	 * 
	 * @return A random song ID
	 */
	public Integer getRandomSongId() throws DataUnavailableException {
		return dbDataPortal.getRandomSongId();
	}

	/**
	 * @see {@link PreloadedData#getNumberOfSongsWithCoords()}
	 * 
	 * @throws DataUnavailableException
	 *             if preloaded data is not yet loaded
	 */
	public int getNumberOfSongsWithCoordinates() throws DataUnavailableException {
		return preloadedDataManager.getData().getNumberOfSongsWithCoords();
	}

	/**
	 * @see {@link PreloadedData#getNumberOfSongsWithoutCoords()}
	 * 
	 * @throws DataUnavailableException
	 *             if preloaded data is not yet loaded
	 */
	public int getNumberOfSongsWithoutCoordinates() throws DataUnavailableException {
		return preloadedDataManager.getData().getNumberOfSongsWithoutCoords();
	}

	/**
	 * @see {@link PreloadedData#getNumberOfSongs()}
	 * 
	 * @throws DataUnavailableException
	 *             if preloaded data is not yet loaded
	 */
	public int getNumberOfSongs() throws DataUnavailableException {
		return preloadedDataManager.getData().getNumberOfSongs();
	}

	// ----- STRING -----

	/**
	 * Gets the path to an album art ({@link String}) from a given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            The {@link BaseArtist} of the returned path to an album art
	 * @param lowRes
	 *            The returned album art in low resolution? ({@link Boolean})
	 * @return The path to an album art from a given {@link BaseAlbum}
	 */
	public String getAlbumArtPath(BaseAlbum baseAlbum, boolean lowRes) throws DataUnavailableException {
		return dbDataPortal.getAlbumArtPath(baseAlbum, lowRes);
	}

	/**
	 * Gets the path ({@link String}) to the given {@link BaseSong}
	 * 
	 * @param baseSong
	 *            The {@link BaseSong} of the returned path
	 * @return The path to the given {@link BaseSong}
	 */
	public String getSongPath(BaseSong<BaseArtist, BaseAlbum> baseSong) throws DataUnavailableException {
		return dbDataPortal.getSongPath(baseSong);
	}

	/**
	 * Gets all available song paths ({@link HashSet} of {@link String})
	 * 
	 * @return All available song paths
	 */
	public HashSet<String> getAllSongsPaths() throws DataUnavailableException {
		return dbDataPortal.getAllSongsPaths();
	}

	/**
	 * Gets all available songPaths to SongId combinations ({@link HashMap} of {@link String} and {@link Integer})
	 * 
	 * @return All SongPath to SongId mappings
	 */
	public HashMap<String, Integer> getAllSongPathToIdMappings() throws DataUnavailableException {
		return dbDataPortal.getSongPathToIdMapping();
	}

	/**
	 * Gets a list of all available song paths ({@link String})
	 * 
	 * @param albumName
	 *            The album name ({@link String}) of the returned song paths
	 * @return A list of all available song paths
	 */
	public List<String> getAllSongPathsForAlbumName(String albumName) {
		return dbDataPortal.getSongPathsForAlbumName(albumName);
	}

	// ----------========### SPECIAL TYPES ###========----------7

	/**
	 * Gets a mapping for all genre/songs ({@link GenreSongMap})
	 * 
	 * @return {@link GenreSongMap}
	 */
	public GenreSongMap getGenreSongMappings() throws DataUnavailableException {
		return dbDataPortal.getGenreSongMappings();
	}

	/**
	 * Gets a list of all available {@link PreloadedSongInfo}
	 * 
	 * @return A list of all available {@link PreloadedSongInfo}
	 */
	public List<PreloadedSongInfo> getPreloadedSongInfo() {
		return dbDataPortal.getPreloadedSongInfo();
	}

	// ----------========### CollectionProperties ###========-----------

	/**
	 * @see IDbDataPortal#getCollectionProperties()
	 */
	public CollectionProperties getCollectionProperties() {
		return dbDataPortal.getCollectionProperties();
	}

	/**
	 * @see IDbDataPortal#setCollectionProperties(CollectionProperties)
	 */
	public void setCollectionProperties(CollectionProperties properties) throws DataWriteException {
		dbDataPortal.setCollectionProperties(properties);
	}

}
