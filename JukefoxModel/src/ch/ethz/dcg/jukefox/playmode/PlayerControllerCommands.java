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

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

/**
 * Class that contains commands that can be applied to a {@link Playlist}. It is
 * assumed that the commands are executed in the order of the list that can be
 * retrieved with {@link #getAllCommands()}
 * 
 * @author swelten
 * 
 */
public class PlayerControllerCommands {

	private List<PlayerControllerCommand> commands;

	public PlayerControllerCommands() {
		commands = new LinkedList<PlayerControllerCommand>();
	}

	private void appendCommand(PlayerControllerCommand command) {
		commands.add(command);
	}

	public List<PlayerControllerCommand> getAllCommands() {
		return commands;
	}

	/**
	 * Appends a command that add a specified song at a position in a playlist
	 * 
	 * @param song
	 *            Song to add
	 * @param position
	 *            position after which the song is inserted
	 * @return the created Command
	 */
	public void addSong(PlaylistSong<BaseArtist, BaseAlbum> song, Integer position) {
		appendCommand(PlayerControllerCommand.createAddSongCommand(song, position));
	}

	/**
	 * Appends a command that removes the song at a position in a playlist
	 * 
	 * @param position
	 *            Position of the song to remove
	 * @return the created Command
	 */
	public void removeSong(Integer position) {
		appendCommand(PlayerControllerCommand.createRemoveSongCommand(position));
	}

	/**
	 * Appends a command that performs a player action (PLAY/PAUSE/STOP)
	 * 
	 * @param action
	 *            action to perform
	 * @return the created Command
	 */
	public void playerAction(PlayerAction action) {
		appendCommand(PlayerControllerCommand.createPlayerActionCommand(action));
	}

	/**
	 * Appends a command that sets the current position in the playlist to a
	 * specified number
	 * 
	 * @param position
	 *            position to set in the playlist
	 * @return the created Command
	 */
	public void setListPos(Integer position) {
		appendCommand(PlayerControllerCommand.createSetListPosCommand(position));
	}

	/**
	 * Appends a command that seeks to a specified position in the current song
	 * 
	 * @param position
	 *            position to seek to in milliseconds
	 * @return the created Command
	 */
	public void setSongPos(Integer position) {
		appendCommand(PlayerControllerCommand.createSetSongPosCommand(position));
	}

	/**
	 * Appends a command that sets the play mode-property of the playlist. Most
	 * probably this method is not needed and will be removed!
	 * 
	 * @param playMode
	 *            the play mode that should be set
	 * @return the created Command
	 */
	@Deprecated
	public void setPlayMode(PlayModeType playMode) {
		appendCommand(PlayerControllerCommand.createSetPlayModeCommand(playMode));
	}

}
