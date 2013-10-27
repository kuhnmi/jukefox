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
package ch.ethz.dcg.jukefox.controller.player.playbackcontroller;

import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.model.collection.IPlaylist;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;

public interface IPlaybackController extends IReadOnlyPlaybackController {

	public void play();

	public void pause();

	public void stop();

	/**
	 * Let's the playback controller jump to the next song (i.e. skip event).
	 * This should only be called from the PlayerController
	 */
	public void next();

	public void previous();

	// public boolean setSong(BaseSong<BaseArtist, BaseAlbum> song); // Not
	// needed because one should only be able to play songs of the current
	// Playlist

	public boolean jumpToPlaylistPosition(int position);

	public void seekTo(int position);

	public void onDestroy();

	public PlayerState getState();

	public void reloadSettings();

	public void mute();

	public void unmute();

	public IPlaylistManager getCurrentPlaylistManager();

	/**
	 * @param playModeType
	 *            the PlayMode to Set
	 * @param artistAvoidance
	 *            the number of subsequent songs that should not be by the same
	 *            artist
	 * @param songAvoidance
	 *            the number of subsequent songs where a songs should not be
	 *            repeated
	 */
	public IPlayMode setPlayMode(PlayModeType playModeType, int artistAvoidance, int songAvoidance);

	public void setPlayMode(IPlayMode playMode);

	public void setPlaylist(IPlaylist playlist);

}
