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
package ch.ethz.dcg.jukefox.data.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.cache.PreloadedSongInfo;
import ch.ethz.dcg.jukefox.data.context.AbstractContextResult;
import ch.ethz.dcg.jukefox.manager.libraryimport.AbstractAlbumCoverFetcherThread.AlbumFetcherResult;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.SongStatus;
import ch.ethz.dcg.jukefox.model.collection.statistics.CollectionProperties;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.WebDataSong;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLogSendEntity;

public interface IDbDataPortal {

	public boolean isOpen();

	public void close();

	public String getSongPath(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException;

	public void insertSongGenreMapping(int genreId, int songId) throws DataWriteException;

	public void deleteGenreSongMapping(int genreId, int songId) throws DataWriteException;

	public GenreSongMap getGenreSongMappings() throws DataUnavailableException;

	public int insertGenre(String name) throws DataWriteException;

	/**
	 * Begins an immediate transaction. Transactions can be nested. When the outer transaction is ended all of the work
	 * done in that transaction and all of the nested transactions will be committed or rolled back. The changes will be
	 * rolled back if any transaction is ended without being marked as clean (by calling setTransactionSuccessful).
	 * Otherwise they will be committed.
	 * 
	 * <p>
	 * Here is the standard idiom for transactions:
	 * </p>
	 * 
	 * <pre>
	 *   db.beginTransaction();
	 *   try {
	 *     ...
	 *     db.setTransactionSuccessful();
	 *   } finally {
	 *     db.endTransaction();
	 *   }
	 * </pre>
	 */
	public void beginTransaction();

	/**
	 * Begins an exclusive transaction.
	 * 
	 * @see #beginTransaction()
	 */
	public void beginExclusiveTransaction();

	/**
	 * Returns true, if the current thread is in a transaction.
	 * 
	 * @return If we are in a transaction
	 */
	public boolean inTransaction();

	/**
	 * Marks the current transaction as successful. Do not do any more database work between calling this and calling
	 * endTransaction. Do as little non-database work as possible in that situation too. If any errors are encountered
	 * between this and endTransaction the transaction will still be committed.
	 */
	public void setTransactionSuccessful();

	/**
	 * End a transaction. See {@link #beginTransaction()} for notes about how to use this and when transactions are
	 * committed and rolled back.
	 */
	public void endTransaction();

	public void removeUnusedAlbums() throws DataWriteException;

	public void removeSongById(int jukefoxId) throws DataWriteException;

	public int insertSong(ImportSong s) throws DataWriteException;

	public HashMap<String, ImportSong> getAllSongsForImport() throws DataUnavailableException;

	public List<MapAlbum> getAllMapAlbums();

	public CompleteAlbum getCompleteAlbumById(int albumId) throws DataUnavailableException;

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongListForAlbum(BaseAlbum album);

	public String getAlbumArtPath(BaseAlbum album, boolean lowRes) throws DataUnavailableException;

	public List<MapTag> getHighestVarianceTags(int numTags);

	public int insertTag(int meId, String name, float[] coords) throws DataWriteException;

	public int replaceFamousArtist(int meId, String name, String meName) throws DataWriteException;

	public void batchInsertFamousArtists(int[] ids, String[] names, float[][] coords) throws DataWriteException;

	public void insertArtistCoords(int artistId, float[] coords) throws DataWriteException;

	public void emptyArtistsTable() throws DataWriteException;

	public void emptyTagsTable() throws DataWriteException;

	public void emptyArtistCoordsTable() throws DataWriteException;

	public List<ListAlbum> getAllAlbumsAsListAlbums();

	public void logArtistSetTable();

	public List<ListAlbum> getAllAlbumsForArtist(BaseArtist artist, boolean includeCompilations);

	public List<BaseArtist> getAllArtists();

	public List<BaseSong<BaseArtist, BaseAlbum>> getAllSongs();

	public BaseSong<BaseArtist, BaseAlbum> getBaseSongById(int randomId) throws DataUnavailableException;

	public List<BaseSong<BaseArtist, BaseAlbum>> batchGetBaseSongByIds(Set<Integer> randomIds)
			throws DataUnavailableException;

	public Integer getTagId(String tagName, boolean onlyRelevantTags) throws DataUnavailableException;

	public CompleteTag getCompleteTagById(int tagId) throws DataUnavailableException;

	/**
	 * Should only be called by the PreloadedDataManager If you need tags get it from the preloaded data
	 * 
	 * @return
	 */
	public HashMap<Integer, CompleteTag> getCompleteTags(boolean onlyRelevantTags) throws DataUnavailableException;

	/**
	 * returns songs that have one of the status specified in statuses or the according album has one of the statuses
	 * specified in albumStatuses TODO exception????
	 */
	public List<WebDataSong> getWebDataSongsForStatus(SongStatus[] statuses, AlbumStatus[] albumStatuses);

	public void updateWebDataSong(WebDataSong song) throws DataWriteException;

	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongListForPaths(List<String> paths);

	public PlaylistSong<BaseArtist, BaseAlbum> getSongForPath(String path) throws DataUnavailableException;

	public PlaylistSong<BaseArtist, BaseAlbum> getSongForPath(String path, boolean caseSensitive)
			throws DataUnavailableException;

	public List<Genre> getAllGenres();

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForArtist(BaseArtist artist);

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForGenre(Genre genre);

	public List<BaseAlbum> getAllAlbumsWithoutAlbumArt();

	public List<SongCoords> getSongCoords(boolean includeSongWithoutCoords);

	public void insertAlbumArtInfo(BaseAlbum album, String highResPath, String lowResPath, int color, AlbumStatus status)
			throws DataWriteException;

	public List<Integer> getSongIdsForAlbum(int albumId);

	public CompleteArtist getCompleteArtist(BaseArtist baseArtist) throws DataUnavailableException;

	public Integer getMusicExplorerArtistId(BaseArtist artist) throws DataUnavailableException;

	public List<ListAlbum> findAlbumBySearchString(String searchTerm, int maxResults);

	public List<BaseArtist> findArtistBySearchString(String searchTerm, int maxResults);

	public List<BaseArtist> findFamousArtistBySearchString(String searchTerm, int maxResults);

	public List<BaseSong<BaseArtist, BaseAlbum>> findTitleBySearchString(String searchTerm, int maxResults);

	public CompleteAlbum getCompleteAlbum(BaseAlbum album) throws DataUnavailableException;

	public List<PreloadedSongInfo> getPreloadedSongInfo();

	public Integer getMusicExplorerIdForSong(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException;

	public void setRelevantTags(Collection<CompleteTag> relevantTags) throws DataWriteException;

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongListForIds(Vector<KdTreePoint<Integer>> points);

	public List<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>> getSongListForIds2(
			Vector<KdTreePoint<Integer>> points);

	public void updateSongsPcaCoords(HashMap<Integer, float[]> songPcaCoords) throws DataWriteException;

	public void updateMapAlbumsPcaCoords(Collection<MapAlbum> mapAlbums) throws DataWriteException;

	public float[] getCoordsForSongById(int songId) throws DataUnavailableException;

	public List<ListAlbum> getAlbumsForGenre(Genre genre);

	public List<BaseArtist> getArtistsForGenre(Genre genre);

	/**
	 * 
	 * @param meArtistId
	 *            the <i>music explorer</i> artist ID.
	 * @return
	 */
	public boolean hasArtistCoords(int meArtistId);

	public MapAlbum getMapAlbum(BaseAlbum album) throws DataUnavailableException;

	public SongCoords getSongCoordsById(Integer songId) throws DataUnavailableException;

	public List<SongCoords> getSongCoordsById(List<Integer> songIds) throws DataUnavailableException;

	public void deleteTagTable() throws DataWriteException;

	public void removeObsoleteGenres() throws DataWriteException;

	public void updateUnusedArtists() throws DataWriteException;

	public MapAlbum getMapAlbumBySong(BaseSong<? extends BaseArtist, ? extends BaseAlbum> song)
			throws DataUnavailableException;

	/**
	 * Generates a string for the last played song that can then be sent to the server.
	 * 
	 * @param profileId
	 * @param playLogVersion
	 * @param coordinateVersion
	 * @param lastSentId
	 * @return
	 * @throws DataUnavailableException
	 */
	public PlayLogSendEntity getPlayLogString(int profileId, int playLogVersion, int coordinateVersion, long lastSentId)
			throws DataUnavailableException;

	public long writePlayLogEntry(int profileId, PlaylistSong<BaseArtist, BaseAlbum> song, long utcTime,
			int timeZoneOffset, int dayOfWeek, int hourOfDay, boolean skip, int playMode,
			AbstractContextResult contextData, int playbackPosition) throws DataWriteException;

	/**
	 * Drops all the tables (except backup tables) and recreates them with the newest definitions.
	 */
	public void resetDatabase();

	/**
	 * Deletes the entire database (factory reset). Backup tables don't survive this action.
	 * 
	 * @return True, if sucessful
	 */
	public boolean deleteDatabase();

	public List<MapTag> getMapTags();

	public BaseSong<BaseArtist, BaseAlbum> getArbitrarySongInTimeRange(int profileId, long fromTimestamp,
			long toTimestamp) throws DataUnavailableException;

	public BaseSong<BaseArtist, BaseAlbum> getBaseSongByMusicExplorerId(int meId) throws DataUnavailableException;

	/**
	 * TODO: for debugging only...
	 */
	public void printPlayLog(int profileId);

	public BaseSong<BaseArtist, BaseAlbum> getSongCloseToTimeRange(int profileId, long fromTimestamp, long toTimestamp,
			float toleranceRange, float toleranceGlobal) throws DataUnavailableException;

	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongsForTimeRange(int profileId, long fromTimestamp,
			long toTimestamp, int number);

	public int getRandomSongId() throws DataUnavailableException;

	public HashSet<String> getAllSongsPaths() throws DataUnavailableException;

	public HashMap<String, Integer> getSongPathToIdMapping() throws DataUnavailableException;

	public List<String> getSongPathsForAlbumName(String name);

	public List<Pair<Genre, Integer>> getGenresForArtist(BaseArtist artist) throws DataUnavailableException;

	public float[] getCoordsForAlbum(BaseAlbum album) throws DataUnavailableException;

	public int insertOrGetPlayerModelId(String name) throws DataWriteException;

	boolean isSongInRecentHistory(int playerModelId, BaseSong<BaseArtist, BaseAlbum> baseSong,
			int equalSongAvoidanceNumber) throws DataUnavailableException;

	boolean isArtistInRecentHistory(int playerModelId, BaseArtist baseArtist, int similarArtistAvoidanceNumber)
			throws DataUnavailableException;

	public int getMaximumValue(String tblName, String columnName) throws DataUnavailableException;

	/**
	 * Reads the {@link CollectionProperties} from the database.
	 * 
	 * @return The {@link CollectionProperties} instance
	 */
	public CollectionProperties getCollectionProperties();

	/**
	 * Writes the {@link CollectionProperties} into the database. It only writes the properties which actually contain
	 * data.
	 * 
	 * @param properties
	 *            The {@link CollectionProperties}
	 * @throws DataWriteException
	 */
	public void setCollectionProperties(CollectionProperties properties) throws DataWriteException;

	/**
	 * Returns the {@link IDbLogHelper} instance.
	 * 
	 * @return The instance
	 */
	public IDbLogHelper getLogHelper();

	/**
	 * Returns the {@link IDbStatisticsHelper} instance.
	 * 
	 * @return The instance
	 */
	public IDbStatisticsHelper getStatisticsHelper();

	/**
	 * Inserts the given song-genre mappings into the database.
	 * 
	 * @param newMappings
	 *            the mappings to be inserted.
	 */
	public void batchInsertSongGenreMappings(GenreSongMap newMappings);

	/**
	 * Inserts the given songs into the database at once.
	 * 
	 * @param songs
	 *            The songs to be inserted.
	 */
	public void batchInsertSongs(Set<ImportSong> songs);

	/**
	 * Updates the web data for all given songs.
	 * 
	 * @param songs
	 *            The songs whose web data should be updated.
	 */
	public void batchUpdateWebData(Set<WebDataSong> songs);

	/**
	 * Updates the album cover information for all given albums.
	 * 
	 * @param songs
	 *            The songs whose web data should be updated.
	 */
	public void batchUpdateAlbumCovers(Set<AlbumFetcherResult> albumCovers);

	/**
	 * Inserts all given tags to the database.
	 * 
	 * @param tags
	 *            The tags to be inserted into the database
	 */
	public void batchInsertTags(List<CompleteTag> tags);

	// *** Key / Value *** //

	/**
	 * Returns the key value pair in the given namespace. If the given key is not found, a
	 * {@link DataUnavailableException} is thrown.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @return The value
	 * @throws DataUnavailableException
	 */
	public String getKeyValue(String namespace, String key) throws DataUnavailableException;

	/**
	 * Returns the key value pair in the given namespace. If the given key is not found, a
	 * {@link DataUnavailableException} is thrown.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @return The value
	 * @throws DataUnavailableException
	 */
	public int getKeyValueInt(String namespace, String key) throws DataUnavailableException;

	/**
	 * Returns the key value pair in the given namespace. If the given key is not found, a
	 * {@link DataUnavailableException} is thrown.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @return The value
	 * @throws DataUnavailableException
	 */
	public double getKeyValueDouble(String namespace, String key) throws DataUnavailableException;

	/**
	 * Sets the key value pair in the given namespace.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 * @throws DataWriteException
	 */
	public void setKeyValue(String namespace, String key, String value) throws DataWriteException;

	/**
	 * Sets the key value pair in the given namespace.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 * @throws DataWriteException
	 */
	public void setKeyValue(String namespace, String key, int value) throws DataWriteException;

	/**
	 * Sets the key value pair in the given namespace.
	 * 
	 * @param namespace
	 *            The namespace
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 * @throws DataWriteException
	 */
	public void setKeyValue(String namespace, String key, double value) throws DataWriteException;
}