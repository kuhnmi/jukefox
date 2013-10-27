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
package ch.ethz.dcg.jukefox.model.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
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
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;

public interface IModelViewData {

	/**
	 * Returns the according album to a song
	 * 
	 * @param song
	 *            Song of which the album should be retrieved
	 * @return the complete information about the album containing the song
	 */
	public MapAlbum getMapAlbumBlocking(BaseSong<? extends BaseArtist, ? extends BaseAlbum> song)
			throws DataUnavailableException;

	public Collection<MapAlbum> getAllMapAlbumsBlocking() throws DataUnavailableException;

	public CompleteAlbum getCompleteAlbumBlocking(BaseAlbum album) throws DataUnavailableException;

	// public Bitmap getAlbumArt(BaseAlbum album, boolean forceLowResolution)
	// throws NoAlbumArtException;

	public List<MapTag> getMostRelevantTagsBlocking(int numTags);

	public List<BaseSong<BaseArtist, BaseAlbum>> getAllSongsBlocking();

	public List<ListAlbum> getAllAlbumsForArtistBlocking(BaseArtist artist, boolean includeCompilations);

	public List<BaseArtist> getAllArtistsBlocking();

	// public BitmapDrawable getListAlbumArt(ListAlbum album) throws
	// NoAlbumArtException;

	public List<ListAlbum> getAllListAlbumsBlocking() throws DataUnavailableException;

	public List<Genre> getAllGenresBlocking();

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForArtistBlocking(BaseArtist artist);

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForGenreBlocking(Genre genre);

	public Collection<CompleteTag> getCloudTagsBlocking(int numTags) throws DataUnavailableException;

	public List<BaseArtist> findArtistBySearchString(String searchTerm, int maxResults);

	public List<ListAlbum> findAlbumBySearchString(String searchTerm, int maxResults);

	public List<BaseSong<BaseArtist, BaseAlbum>> findTitleBySearchString(String searchTerm, int maxResults);

	// public Cursor findTitleBySearchStringCursor(String searchTerm, int
	// maxResults);

	public List<BaseArtist> findFamousArtistBySearchString(String searchTerm, int maxResults);

	public List<BaseSong<BaseArtist, BaseAlbum>> getClosestSongsToPosition(float[] position, int number)
			throws DataUnavailableException;

	public CompleteTag getTagById(int id) throws DataUnavailableException;

	public List<BaseArtist> getArtistsByGenreBlocking(Genre genre);

	public List<ListAlbum> getAllAlbumsForGenreBlocking(Genre genre);

	public List<BaseSong<BaseArtist, BaseAlbum>> getAllSongsForGenreBlocking(Genre genre);

	public MapAlbum getMapAlbumBlocking(BaseAlbum album) throws DataUnavailableException;

	public List<SongCoords> getRandomSongsWithCoords(int numberOfSongs) throws DataUnavailableException;

	public BaseSong<BaseArtist, BaseAlbum> getBaseSong(SongCoords song) throws DataUnavailableException;

	public CompleteArtist getCompleteArtist(BaseArtist baseArtist) throws DataUnavailableException;

	public int getNumberOfSongsWithCoords() throws DataUnavailableException;

	public Playlist readPlaylistFromFile(String fileName) throws DataUnavailableException;

	public void writeCoordsToDisk();

	public List<MapTag> getMapTagsBlocking(int numMapTags);

	public List<Pair<MapAlbum, Float>> getSimilarAlbums(BaseAlbum album, int number) throws DataUnavailableException;

	public List<Pair<CompleteTag, Float>> getTagsForArtist(CompleteArtist artist) throws DataUnavailableException;

	public List<Pair<CompleteTag, Float>> getTagsForAlbum(BaseAlbum album) throws DataUnavailableException;

	/**
	 * Returns the genres of all songs of an artist
	 * 
	 * @param artist
	 * @return a Genre with the number how often this genre occured at the
	 *         artist's songs
	 * @throws DataUnavailableException
	 */
	public List<Pair<Genre, Integer>> getGenresForArtist(BaseArtist artist) throws DataUnavailableException;

	public float[] getSongCoordinates(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException;

	public BaseSong<BaseArtist, BaseAlbum> getSongForTag(String tagName) throws DataUnavailableException;

	public ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> getSongsForTag(CompleteTag tag, int size, int sampleFactor)
			throws DataUnavailableException;

	public void groupAlbum(String name) throws Throwable;

	public void ungroupAlbum(String name) throws Throwable;

	// public List<PlaylistInfo> getImportablePlaylists();

}
