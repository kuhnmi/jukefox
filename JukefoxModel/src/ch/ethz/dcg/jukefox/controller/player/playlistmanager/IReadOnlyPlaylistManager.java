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

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;

/**
 * The readonly part of the playlist manager contains the registration of
 * listeners.
 */
public interface IReadOnlyPlaylistManager {

	/**
	 * Returns a readonly version of the current playlist.
	 */
	IReadOnlyPlaylist getCurrentPlaylist();

	/**
	 * Returns the current song.
	 */
	PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong() throws EmptyPlaylistException;

	/**
	 * Returns the the playmode that is used in the current playlist.
	 */
	IPlayMode getPlayMode();

	/**
	 * Returns the current position, i.e. the song which is being played (or is
	 * paused).
	 */
	int getCurrentSongIndex() throws EmptyPlaylistException;

	/**
	 * Returns true if the current song is at the end of the playlist or if the
	 * playlist is empty.
	 */
	boolean isPlaylistEmptyOrAtEnd();
}
