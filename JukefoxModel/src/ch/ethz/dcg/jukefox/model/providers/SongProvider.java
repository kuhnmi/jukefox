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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.cache.PreloadedData;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.SongStatus;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.WebDataSong;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Provides all possible data access options for song-objects
 */
public class SongProvider {

	public static final int NUM_SONGS = 5;

	private static final String TAG = SongProvider.class.getSimpleName();

	private final IDbDataPortal dbDataPortal;
	private final PreloadedDataManager preloadedDataManager;

	/**
	 * Creates a new instance of {@link SongProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @param preloadedDataManager
	 *            The preloaded data manager which will be used
	 */
	public SongProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
	}

	// ----- BASE SONG -----

	/**
	 * Gets a list of all available {@link BaseSong}
	 * 
	 * @return A list of all available {@link BaseSong}
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllBaseSongs() {
		return dbDataPortal.getAllSongs();
	}

	/**
	 * Gets a list of {@link BaseSong} of the given {@link BaseArtist}
	 * 
	 * @param baseArtist
	 *            The {@link BaseArtist} of which you want generate the list of {@link BaseSong}
	 * @return A list of {@link BaseSong} of the given {@link BaseArtist}
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllBaseSongs(BaseArtist baseArtist) {
		return dbDataPortal.getSongsForArtist(baseArtist);
	}

	/**
	 * Gets a list of {@link BaseSong} of the given {@link Genre}
	 * 
	 * @param genre
	 *            The {@link Genre} of which you want generate the list of {@link BaseSong}
	 * @return A list of {@link BaseSong} of the given {@link Genre}
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllBaseSongs(Genre genre) {
		return dbDataPortal.getSongsForGenre(genre);
	}

	/**
	 * Gets a list of {@link BaseSong} searched by a search term
	 * 
	 * @param searchTerm
	 *            The search term ({@link String}) that describes the desired return value
	 * @param maxResults
	 *            The maximum numbers ({@link Integer}) of results
	 * @return All results as a list of {@link BaseSong} of the given search terms
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> findBaseSongsBySearchString(String searchTerm, int maxResults) {
		return dbDataPortal.findTitleBySearchString(searchTerm, maxResults);
	}

	/**
	 * Gets a list of {@link BaseSong} which are closest to a given position
	 * 
	 * @param position
	 *            The position (array of {@link Float}) of which the returned {@link BaseSong} should be closest to
	 * @param number
	 *            Minimum number for the advanced kd tree algorithm
	 * @return A list of {@link BaseSong} which are closest to the given position
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getClosestBaseSongsToPosition(float[] position, int number)
			throws DataUnavailableException {
		try {
			Vector<KdTreePoint<Integer>> points = preloadedDataManager.getData().getSongsCloseToPosition(position,
					number);
			return dbDataPortal.getSongListForIds(points);
		} catch (KeySizeException e) {
			return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		}
	}

	/**
	 * Gets a list of {@link BaseSong} which are closest to a given position
	 * 
	 * @param position
	 *            The position (array of {@link Float}) of which the returned {@link BaseSong} should be closest to
	 * @param number
	 *            Minimum number for the advanced kd tree algorithm
	 * @return A list of Pairs of {@link BaseSong} and their positions which are closest to the given position
	 */
	public List<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>> getClosestSongsToPosition2(
			float[] position, int number) throws DataUnavailableException {
		try {
			Vector<KdTreePoint<Integer>> points = preloadedDataManager.getData().getSongsCloseToPosition(position,
					number);
			return dbDataPortal.getSongListForIds2(points);
		} catch (KeySizeException e) {
			return new ArrayList<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>>();
		}
	}

	/**
	 * @see PreloadedData#getSongsAroundPositionEuclidian(float[], float)
	 */
	public Vector<KdTreePoint<Integer>> getSongsAroundPositionEuclidian(float[] position, float distance)
			throws DataUnavailableException {
		try {
			return preloadedDataManager.getData().getSongsAroundPositionEuclidian(position, distance);
		} catch (KeySizeException e) {
			throw new DataUnavailableException(e);
		}
	}

	/**
	 * @see PreloadedData#getSongsAroundPositionHamming(float[], float)
	 */
	public Vector<KdTreePoint<Integer>> getSongsAroundPositionHamming(float[] position, float distance)
			throws DataUnavailableException {
		try {
			return preloadedDataManager.getData().getSongsAroundPositionHamming(position, distance);
		} catch (KeySizeException e) {
			throw new DataUnavailableException(e);
		}
	}

	/**
	 * Gets a {@link BaseSong} of the given song coordinates
	 * 
	 * @param songCoords
	 *            The song coordinates ({@link SongCoords}) of the returned {@link BaseSong}
	 * @return A {@link BaseSong} of the given song coordinates
	 */
	public BaseSong<BaseArtist, BaseAlbum> getBaseSong(SongCoords songCoords) throws DataUnavailableException {
		return getBaseSong(songCoords.getId());
	}

	/**
	 * Gets a {@link BaseSong} of the given base song id
	 * 
	 * @param id
	 *            The base song id ({@link Integer}) of the returned {@link BaseSong}
	 * @return A {@link BaseSong} of the given base song id
	 */
	public BaseSong<BaseArtist, BaseAlbum> getBaseSong(int id) throws DataUnavailableException {
		return dbDataPortal.getBaseSongById(id);
	}

	/**
	 * Gets a {@link BaseSong} of the given tag name
	 * 
	 * @param tagName
	 *            The tag name ({@link String}) of the returned {@link BaseSong}
	 * @return A {@link BaseSong} of the given tag name
	 */
	public BaseSong<BaseArtist, BaseAlbum> getBaseSong(String tagName) throws DataUnavailableException {
		Integer tagId = dbDataPortal.getTagId(tagName, false);
		CompleteTag completeTag = dbDataPortal.getCompleteTagById(tagId);
		List<BaseSong<BaseArtist, BaseAlbum>> baseSongs = getClosestBaseSongsToPosition(MathUtils.normalizeCoordsSum(
				completeTag.getPlsaCoords(), 1), NUM_SONGS);
		BaseSong<BaseArtist, BaseAlbum> baseSong = baseSongs.get(RandomProvider.getRandom().nextInt(NUM_SONGS));
		return baseSong;
	}

	/**
	 * Gets a list of {@link BaseSong} of the given {@link BaseAlbum}
	 * 
	 * @param baseAlbum
	 *            The {@link BaseAlbum} of the returned {@link BaseSong}
	 * @return A list of {@link BaseSong} of the given {@link BaseAlbum}
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllBaseSongs(BaseAlbum baseAlbum) {
		return dbDataPortal.getSongListForAlbum(baseAlbum);
	}

	/**
	 * Gets an arbitrary {@link BaseSong} in the given time range
	 * 
	 * @param profileId
	 *            The profile ID ({@link Integer})
	 * @param fromTimestamp
	 *            Minimum timestamp ({@link Long}) of the returned {@link BaseSong}
	 * @param toTimestamp
	 *            Maximum timestamp ({@link Long}) of the returned {@link BaseSong}
	 * @return An arbitrary {@link BaseSong} in the given time range
	 */
	public BaseSong<BaseArtist, BaseAlbum> getArbitraryBaseSongInTimeRange(int profileId, long fromTimestamp,
			long toTimestamp) throws DataUnavailableException {
		return dbDataPortal.getArbitrarySongInTimeRange(profileId, fromTimestamp, toTimestamp);
	}

	/**
	 * Gets the {@link BaseSong} of the given music explorer ID
	 * 
	 * @param meId
	 *            The music explorer ID ({@link Integer}) of the returned {@link BaseSong}
	 * @return The {@link BaseSong} of the given music explorer ID
	 */
	public BaseSong<BaseArtist, BaseAlbum> getBaseSongByMusicExplorerId(int meId) throws DataUnavailableException {
		return dbDataPortal.getBaseSongByMusicExplorerId(meId);
	}

	/**
	 * Gets a {@link BaseSong} which is close to the given time range
	 * 
	 * @param profileId
	 *            The profile ID ({@link Integer})
	 * @param fromTimestamp
	 *            Minimum timestamp ({@link Long}) of the returned {@link BaseSong}
	 * @param toTimestamp
	 *            Maximum timestamp ({@link Long}) of the returned {@link BaseSong}
	 * @param toleranceRange
	 *            Maximum time difference tolerance
	 * @param toleranceGlobal
	 *            Global maximum time difference tolerance
	 * @return A {@link BaseSong} which is close to the given time range
	 */
	public BaseSong<BaseArtist, BaseAlbum> getBaseSongCloseToTimeRange(int profileId, long fromTimestamp,
			long toTimestamp, float toleranceRange, float toleranceGlobal) throws DataUnavailableException {
		return dbDataPortal.getSongCloseToTimeRange(profileId, fromTimestamp, toTimestamp, toleranceRange,
				toleranceGlobal);
	}

	/**
	 * Gets a list of {@link BaseSong} for the given IDs
	 * 
	 * @param points
	 *            A {@link Vector} of points with the {@link BaseSong} IDs
	 * @return A list of {@link BaseSong} for the given IDs
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllBaseSongsForIds(Vector<KdTreePoint<Integer>> points) {
		return dbDataPortal.getSongListForIds(points);
	}

	/**
	 * Gets a list of {@link Pair} of {@link BaseSong} and {@link KdTreePoint} for the given IDs
	 * 
	 * @param points
	 *            A {@link Vector} of points with the {@link BaseSong} IDs
	 * @return A list of {@link Pair} of {@link BaseSong} and {@link KdTreePoint} for the given IDs
	 */
	public List<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>> getAllBaseSongForIds2(
			Vector<KdTreePoint<Integer>> points) {
		return dbDataPortal.getSongListForIds2(points);
	}

	/**
	 * Get random songs for which there are music similarity coordinates
	 * 
	 * @ param numberOfSongs the number of random songs that should be returned
	 * 
	 * @return A random BaseSong from the collection
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getRandomSongWithCoordinates(int numberOfSongs)
			throws DataUnavailableException {

		List<PlaylistSong<BaseArtist, BaseAlbum>> songs = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>(
				numberOfSongs);
		List<Integer> ids = preloadedDataManager.getData().getIdsOfRandomSongsWithCoords(numberOfSongs);

		for (Integer id : ids) {
			songs
					.add(new PlaylistSong<BaseArtist, BaseAlbum>(dbDataPortal.getBaseSongById(id),
							SongSource.RANDOM_SONG));
		}
		return songs;
	}

	// ----- IMPORT SONG -----

	/**
	 * Gets a {@link HashMap} of all available {@link ImportSong}
	 * 
	 * @return Gets a {@link HashMap} of all available {@link ImportSong}
	 */
	public HashMap<String, ImportSong> getAllImportSongs() throws DataUnavailableException {
		return dbDataPortal.getAllSongsForImport();
	}

	// ----- PLAYLIST SONG -----

	/**
	 * Gets a list of all {@link PlaylistSong} for the given list of paths
	 * 
	 * @param paths
	 *            The {@link List} of paths ({@link String}) of the returned {@link PlaylistSong}
	 * @return A list of all {@link PlaylistSong} for the given list of paths
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getAllPlaylistSongsForPaths(List<String> paths) {
		return dbDataPortal.getSongListForPaths(paths);
	}

	/**
	 * Gets the {@link PlaylistSong} of the given path
	 * 
	 * @param path
	 *            The path ({@link String}) of the returned {@link PlaylistSong}
	 * @return The {@link PlaylistSong} of the given path
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getPlaylistSongForPath(String path) throws DataUnavailableException {
		return dbDataPortal.getSongForPath(path);
	}

	/**
	 * Gets the {@link PlaylistSong} of the given path
	 * 
	 * @param path
	 *            The path ({@link String}) of the returned {@link PlaylistSong}
	 * @param caseSensitive
	 *            Is the path case sensitive? ({@link Boolean})
	 * @return The {@link PlaylistSong} of the given path
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getSongForPath(String path, boolean caseSensitive)
			throws DataUnavailableException {
		return dbDataPortal.getSongForPath(path, caseSensitive);
	}

	/**
	 * Gets a list of all {@link PlaylistSong} for the given time range
	 * 
	 * @param profileId
	 *            The profile ID ({@link Integer})
	 * @param fromTimestamp
	 *            Minimum timestamp ({@link Long}) of the returned {@link PlaylistSong}
	 * @param toTimestamp
	 *            Maximum timestamp ({@link Long}) of the returned {@link PlaylistSong}
	 * @param maxResults
	 *            The maximum numbers ({@link Integer}) of results
	 * @return A list of all {@link PlaylistSong} for the given time range
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getPlaylistSongsForTimeRange(int profileId, long fromTimestamp,
			long toTimestamp, int maxResults) {
		return dbDataPortal.getSongsForTimeRange(profileId, fromTimestamp, toTimestamp, maxResults);
	}

	/**
	 * Returns a song that is picked from the collection uniformly at random
	 * 
	 * @return
	 * @throws DataUnavailableException
	 *             if no random song is available
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getRandomSong() throws DataUnavailableException {
		return getRandomSongs(1).get(0);
	}

	/**
	 * Returns songs that are picked from the collection uniformly at random
	 * 
	 * @param num
	 *            How many songs should returned
	 * @return
	 * @throws DataUnavailableException
	 *             if no random song is available
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getRandomSongs(int num) throws DataUnavailableException {
		num = Math.min(num, preloadedDataManager.getData().getNumberOfSongs());

		if (num == 0) {
			throw new DataUnavailableException("There are no songs in the collection");
		}

		// Find the random ids
		Set<Integer> songIds = new HashSet<Integer>(num);
		while (songIds.size() < num) {
			songIds.add(preloadedDataManager.getData().getRandomSongId());
		}

		// Load the songs
		List<PlaylistSong<BaseArtist, BaseAlbum>> ret = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>(num);
		for (Integer songId : songIds) {
			ret.add(new PlaylistSong<BaseArtist, BaseAlbum>(dbDataPortal.getBaseSongById(songId),
					SongSource.RANDOM_SONG));
		}

		return ret;
	}

	// ----- WEB DATA SONG -----

	/**
	 * Gets a list of all {@link WebDataSong} for the given {@link SongStatus} and {@link AlbumStatus}
	 * 
	 * @param songStatuses
	 *            The {@link SongStatus} of the returned {@link WebDataSong}
	 * @param albumStatuses
	 *            The {@link AlbumStatus} of the returned {@link WebDataSong}
	 * @return A list of all {@link WebDataSong} for the given {@link SongStatus} and {@link AlbumStatus}
	 */
	public List<WebDataSong> getWebDataSongsForStatus(SongStatus[] songStatuses, AlbumStatus[] albumStatuses) {
		return dbDataPortal.getWebDataSongsForStatus(songStatuses, albumStatuses);
	}

}
