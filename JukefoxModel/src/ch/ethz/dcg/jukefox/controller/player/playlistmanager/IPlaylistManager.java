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
package ch.ethz.dcg.jukefox.controller.player.playlistmanager;

import java.util.List;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;

/**
 * A playlist manager manages a playlist related. It acts as a wrapper of the
 * playlist controller core: While the core changes whenever the play mode
 * changes, the class implementing this interface stays the same.
 */
public interface IPlaylistManager extends IReadOnlyPlaylistManager {

	/**
	 * Sets the play mode of the playlist
	 */
	void setPlayMode(IPlayMode playMode);

	/**
	 * Loads a new playlist to be played.
	 */
	void setPlaylist(IPlaylist playlist);

	/**
	 * Clears the current playlist.
	 */
	void clearPlaylist();

	/**
	 * Insert a song to be played right after the current one.
	 * 
	 * @throws EmptyPlaylistException
	 * @throws PlaylistPositionOutOfRangeException
	 */
	void insertSongAsNext(PlaylistSong<BaseArtist, BaseAlbum> song);

	/**
	 * Insert a song at a specified position in the playlist
	 * 
	 * @throws PlaylistPositionOutOfRangeException
	 */
	void insertSongAtPosition(PlaylistSong<BaseArtist, BaseAlbum> song, int position)
			throws PlaylistPositionOutOfRangeException;

	/**
	 * Insert songs at a specified position in the playlist
	 * 
	 * @throws PlaylistPositionOutOfRangeException
	 */
	void insertSongsAtPosition(List<PlaylistSong<BaseArtist, BaseAlbum>> songs, int position)
			throws PlaylistPositionOutOfRangeException;

	/**
	 * Insert a list of songs to be played right after the current one.
	 * 
	 * @throws EmptyPlaylistException
	 * @throws PlaylistPositionOutOfRangeException
	 */
	void insertSongsAsNext(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) throws PlaylistPositionOutOfRangeException,
			EmptyPlaylistException;

	/**
	 * Appends a song at the end of the playlist
	 */
	void appendSongAtEnd(PlaylistSong<BaseArtist, BaseAlbum> song);

	/**
	 * Moves the song from the oldPosition to the newPosition.
	 */
	void moveSong(int oldPosition, int newPosition) throws EmptyPlaylistException, PlaylistPositionOutOfRangeException;

	/**
	 * Removes a certain song from the playlist.
	 */
	void removeSongFromPlaylist(int position) throws EmptyPlaylistException, PlaylistPositionOutOfRangeException;

	/**
	 * Sets the current song index, but doesn't change anything otherwise. Only
	 * call this if you're sure you're not breaking any consistency.
	 */
	void setCurrentSongIndex(int index) throws PlaylistPositionOutOfRangeException;

	/**
	 * Appends a list of songs at the end of the playlist.
	 */
	void appendSongsAtEnd(List<PlaylistSong<BaseArtist, BaseAlbum>> songs);

	/**
	 * Create a default playmode of the specified type and sets it as play mode
	 * 
	 * @param playModeType
	 *            the PlayMode to Set
	 * @param artistAvoidance
	 *            the number of subsequent songs that should not be by the same
	 *            artist
	 * @param songAvoidance
	 *            the number of subsequent songs where a songs should not be
	 *            repeated
	 */
	void setPlayMode(PlayModeType playModeType, int artistAvoidance, int songAvoidance);

	void shufflePlaylist(int startPosition);

}
