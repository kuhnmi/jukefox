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

import java.util.Collection;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportBlacklistManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.AbstractAlbumCoverFetcherThread.AlbumFetcherResult;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.WebDataSong;

/**
 * Provides all possible data access options for default data objects like
 * {@link Integer}, {@link Float} and base types
 */
/**
 * @author langnert
 * 
 */
/**
 * @author swelten
 * 
 */
public class ModifyProvider {

	private static final String TAG = ModifyProvider.class.getSimpleName();
	private final IDbDataPortal dbDataPortal;
	private PreloadedDataManager preloadedDataManager;
	private final DirectoryManager directorymanager;

	/**
	 * Creates a new instance of {@link ModifyProvider}
	 * 
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 */
	public ModifyProvider(IDbDataPortal dbDataPortal, PreloadedDataManager preloadedDataManager,
			DirectoryManager directoryManager) {
		this.dbDataPortal = dbDataPortal;
		this.preloadedDataManager = preloadedDataManager;
		this.directorymanager = directoryManager;
	}

	// /**
	// * Writes a {@link IReadOnlyPlaylist} to a file with the given name
	// *
	// * @param playlist
	// * The {@link IReadOnlyPlaylist} to be saved
	// * @param playlistDirectory
	// * The directory ({@link File}) to which the playlist
	// * {@link File} will be saved.
	// * @param name
	// * The name ({@link String}) of the {@link Playlist} file to be
	// * saved
	// */
	// public void writePlaylistToFile(IReadOnlyPlaylist playlist, File
	// playlistDirectory, String name) {
	// PlaylistWriter.writePlaylistToFile(dbDataPortal, playlist,
	// playlistDirectory, name);
	// }

	// ----------------------------------------------------------------------------------------
	// DB INSERTS
	// ----------------------------------------------------------------------------------------

	/**
	 * Insert a importSong ({@link ImportSong}) into DB
	 * 
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public int insertSong(ImportSong s) throws DataWriteException {
		return dbDataPortal.insertSong(s);
	}

	public void batchInsertSongs(Set<ImportSong> songs) throws DataWriteException {
		dbDataPortal.batchInsertSongs(songs);
	}

	public void batchUpdateWebData(Set<WebDataSong> songs) throws DataWriteException {
		dbDataPortal.batchUpdateWebData(songs);
	}

	/**
	 * Insert a Song/Genre Mapping
	 */
	public void insertSongGenreMapping(int genreId, int songId) throws DataWriteException {
		dbDataPortal.insertSongGenreMapping(genreId, songId);
	}

	/**
	 * @param newMappings
	 */
	public void insertSongGenreMappings(GenreSongMap newMappings) {
		dbDataPortal.batchInsertSongGenreMappings(newMappings);
	}

	/**
	 * Insert artist coordinates (Array of {@link Float}) into DB
	 */
	public void insertArtistCoords(int meArtistId, float[] coords) throws DataWriteException {
		dbDataPortal.insertArtistCoords(meArtistId, coords);
	}

	/**
	 * Insert a album art informations into DB
	 * 
	 * @param album
	 *            The album, the album art belongs to
	 * @param highResPath
	 *            Path ({@link String}) of the high resolution art file
	 * @param lowResPath
	 *            Path ({@link String}) of the low resolution art file
	 * @param color
	 *            The color ({@link Integer}) of the album
	 * @param status
	 *            The {@link AlbumStatus} of the album
	 */
	public void insertAlbumArtInfo(BaseAlbum album, String highResPath, String lowResPath, int color, AlbumStatus status)
			throws DataWriteException {
		dbDataPortal.insertAlbumArtInfo(album, highResPath, lowResPath, color, status);
	}

	/**
	 * Insert genre ({@link String}) into DB
	 * 
	 * @param name
	 *            The name of the Genre
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public int insertGenre(String name) throws DataWriteException {
		return dbDataPortal.insertGenre(name);
	}

	/**
	 * Insert a song tag into DB
	 * 
	 * @param meId
	 *            The unique MeID ({@link Integer}) of the tag
	 * @param name
	 *            The name ({@link String}) of the tag
	 * @param coords
	 *            The coordinates (Array of {@link Float}) of the tag
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public int insertTag(int meId, String name, float[] coords) throws DataWriteException {
		return dbDataPortal.insertTag(meId, name, coords);
	}

	// ----------------------------------------------------------------------------------------
	// UPDATES
	// ----------------------------------------------------------------------------------------

	/**
	 * Insert a song tag into DB
	 * 
	 * @param meId
	 *            The unique MeID ({@link Integer}) of the artist
	 * @param name
	 *            The name ({@link String}) of the artist
	 * @param meName
	 *            The unique MeName ({@link String}) of the artist
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public int replaceFamousArtist(int meId, String name, String meName) throws DataWriteException {
		return dbDataPortal.replaceFamousArtist(meId, name, meName);
	}

	/**
	 * Deletes all unused Artist
	 */
	public void updateUnusedArtists() throws DataWriteException {
		dbDataPortal.updateUnusedArtists();
	}

	/**
	 * Updates the specified {@link WebDataSong}
	 */
	public void updateWebDataSong(WebDataSong song) throws DataWriteException {
		dbDataPortal.updateWebDataSong(song);
	}

	/**
	 * Updates the pca coordinates of a {@link Collection} of {@link MapAlbum}
	 */
	public void updateMapAlbumsPcaCoords(Collection<MapAlbum> mapAlbums) throws DataWriteException {
		dbDataPortal.updateMapAlbumsPcaCoords(mapAlbums);
	}

	/**
	 * Sets the flag RELEVANT of all {@link CompleteTag} in the {@link Collection}
	 */
	public void setRelevantTags(Collection<CompleteTag> relevantTags) throws DataWriteException {
		dbDataPortal.setRelevantTags(relevantTags);
	}

	// ----------------------------------------------------------------------------------------
	// DELETES
	// ----------------------------------------------------------------------------------------

	/**
	 * Deletes a song from DB
	 * 
	 * @param jukefoxId
	 *            database ID of the song
	 */
	public void removeSongById(int jukefoxId) throws DataWriteException {
		dbDataPortal.removeSongById(jukefoxId);
	}

	/**
	 * Deletes all unused albums
	 */
	public void removeUnusedAlbums() throws DataWriteException {
		dbDataPortal.removeUnusedAlbums();
	}

	/**
	 * Deletes obsolete genres
	 */
	public void removeObsoleteGenres() throws DataWriteException {
		dbDataPortal.removeObsoleteGenres();
	}

	/**
	 * Deletes the given Genre/Song mapping
	 */
	public void deleteGenreSongMapping(int genreId, int songId) throws DataWriteException {
		dbDataPortal.deleteGenreSongMapping(genreId, songId);
	}

	/**
	 * Removes all entries of artist table
	 */
	public void emptyArtistsTable() throws DataWriteException {
		dbDataPortal.emptyArtistsTable();
	}

	/**
	 * Removes all entries of tag table
	 */
	public void emptyTagsTable() throws DataWriteException {
		dbDataPortal.emptyTagsTable();
	}

	/**
	 * Removes all entries of tag table and resets the auto incremented id
	 */
	public void deleteTagTable() throws DataWriteException {
		dbDataPortal.deleteTagTable();
	}

	/**
	 * Removes all entries of album-coordinates table
	 */
	public void emptyArtistCoordsTable() throws DataWriteException {
		dbDataPortal.emptyArtistCoordsTable();
	}

	/**
	 * Ignores the given {@link BaseSong} and adds it to the blacklist
	 * 
	 * @param baseSong
	 *            The {@link BaseSong} to be ignored
	 * @return true if the song was ignored successfully; otherwise false
	 */
	public boolean ignoreSong(BaseSong<BaseArtist, BaseAlbum> baseSong) {
		try {
			LibraryImportBlacklistManager.appendFileBlacklistPath(directorymanager, dbDataPortal.getSongPath(baseSong));
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return false;
		}
		boolean ok = true;
		try {
			dbDataPortal.removeSongById(baseSong.getId());
			preloadedDataManager.loadData();
		} catch (Exception e) {
			Log.w(TAG, e);
			ok = false;
		}
		return ok;
	}

	/**
	 * Deletes the given {@link BaseSong} from the filesystem. Use with caution!
	 * 
	 * @param baseSong
	 *            The {@link BaseSong} to be deleted
	 * @return true if the song was deleted successfully; otherwise false
	 */
	public boolean deleteSong(BaseSong<BaseArtist, BaseAlbum> baseSong) {
		String path;
		try {
			path = dbDataPortal.getSongPath(baseSong);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return false;
		}
		ignoreSong(baseSong);
		boolean ok = true;
		try {
			Utils.deleteFile(path);
		} catch (Exception e) {
			Log.w(TAG, e);
			ok = false;
		}
		return ok;
	}

	/**
	 * Inserts the Album art information into the database
	 * 
	 * @param results
	 */
	public void batchInsertAlbumArtInfo(Set<AlbumFetcherResult> results) {
		dbDataPortal.batchUpdateAlbumCovers(results);
	}

}
