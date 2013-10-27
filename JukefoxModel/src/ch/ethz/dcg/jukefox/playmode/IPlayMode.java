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
package ch.ethz.dcg.jukefox.playmode;

import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;

public interface IPlayMode {

	/**
	 * Return the actions that have to be taken upon intialization of the play mode
	 * 
	 * @return the Commands that should be executed upon the initialization of the play mode;
	 */
	public PlayerControllerCommands initialize(IReadOnlyPlaylist currentPlaylist);

	/**
	 * Resets the playmode
	 */
	public void reset();

	/**
	 * Returns the actions that have to be taken if the current song has ended or when it was skipped. Calling this
	 * method does not change the state of the play mode.<br/>
	 * Please ensure, that IOnPlaylistStateChangeListener signals are sent before calling this method.
	 * 
	 * @param currentPlaylist
	 *            the playlist on which the play mode works
	 */
	PlayerControllerCommands next(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException;

	/**
	 * Returns the playlist changes which are necessary to get to the previous song
	 */
	PlayerControllerCommands previous(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException;

	/**
	 * Returns the type of the playmode.
	 */
	PlayModeType getPlayModeType();

}
