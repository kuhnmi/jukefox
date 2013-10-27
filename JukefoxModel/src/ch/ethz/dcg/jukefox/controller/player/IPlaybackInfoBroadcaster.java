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
package ch.ethz.dcg.jukefox.controller.player;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;

/**
 * Interface to inform of changes which are player/playlist related. This
 * interface should only be implemented by the {@link AbstractPlayerController}.
 * Classes that are interested in these event have to register at the
 * PlayerController.
 */
public interface IPlaybackInfoBroadcaster {

	// The playlist changed. This can be either a new
	void informPlaylistChangeListener(IReadOnlyPlaylist playlist);

	// The playmode changed.
	void informPlayModeChangeListener(IPlayMode newPlayMode);

	void informCurrentSongChangeListener(PlaylistSong<BaseArtist, BaseAlbum> newSong);

	void informSongCompletedListeners(PlaylistSong<BaseArtist, BaseAlbum> playlistSong);

	void informSongStartedListeners(PlaylistSong<BaseArtist, BaseAlbum> playlistSong);

	void informPlayerStateChangedListeners(PlayerState playerState);

	void informSongSkippedListeners(PlaylistSong<BaseArtist, BaseAlbum> playlistSong);
}
