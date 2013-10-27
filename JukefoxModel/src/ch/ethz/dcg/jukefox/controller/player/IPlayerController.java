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

import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;

/**
 * Read/write interface to the player controller.
 */
public interface IPlayerController extends IReadOnlyPlayerController {

	public void play();

	public void pause();

	public void stop();

	public void seekTo(int position);

	/**
	 * Sets the current song, which can then be controlled by play, pause etc...
	 */
	public void jumpToPlaylistPosition(int position);

	public void mute();

	public void unmute();

	public void playNext() throws EmptyPlaylistException, NoNextSongException;

	public void playPrevious() throws EmptyPlaylistException, NoNextSongException;

	public void playSongAtPosition(int position) throws PlaylistPositionOutOfRangeException;

	public void loadSongAtPosition(int position) throws PlaylistPositionOutOfRangeException;

}
