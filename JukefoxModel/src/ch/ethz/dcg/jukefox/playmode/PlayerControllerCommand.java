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

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

/**
 * An object of this class represents a command that can be executed on a
 * {@link Playlist}. Use the static methods to create a command of s specific
 * type
 * 
 * @author swelten
 * 
 */
public class PlayerControllerCommand {

	public enum PlayerControllerCommandType {
		ADD_SONG, // Add a song after a certain position to the playlist
		REMOVE_SONG, // Remove a song at a certain position in the playlist
		PLAYER_ACTION, // Perform a player action
		SET_POS_IN_LIST, // Jump to a specified position in the playlist
		SET_POS_IN_SONG, // Jump to a specified position in the current song
		SET_PLAY_MODE; // Set the play mode property of the playlist
	}

	private PlayerControllerCommandType type;
	private Integer position;
	private PlaylistSong<BaseArtist, BaseAlbum> song;
	private PlayerAction action;
	private PlayModeType playMode;

	/**
	 * Constructor of PlaylistCommands. It is private to ensure that only the
	 * internal create methods use this constructor, so that a command is always
	 * initialized with the correct arguments
	 * 
	 * @param type
	 *            Type of the command
	 * @param position
	 * @param song
	 * @param state
	 * @param playMode
	 */
	private PlayerControllerCommand(PlayerControllerCommandType type, Integer position,
			PlaylistSong<BaseArtist, BaseAlbum> song, PlayerAction action, PlayModeType playMode) {
		super();
		this.type = type;
		this.position = position;
		this.song = song;
		this.action = action;
		this.playMode = playMode;
	}

	/**
	 * Creates a command that add a specified song at a position in a playlist
	 * 
	 * @param song
	 *            Song to add
	 * @param position
	 *            position after which the song is inserted
	 * @return the created Command
	 */
	public static PlayerControllerCommand createAddSongCommand(PlaylistSong<BaseArtist, BaseAlbum> song,
			Integer position) {
		return new PlayerControllerCommand(PlayerControllerCommandType.ADD_SONG, position, song, null, null);
	}

	/**
	 * Creates a command that removes the song at a position in a playlist
	 * 
	 * @param position
	 *            Position of the song to remove
	 * @return the created Command
	 */
	public static PlayerControllerCommand createRemoveSongCommand(Integer position) {
		return new PlayerControllerCommand(PlayerControllerCommandType.REMOVE_SONG, position, null, null, null);
	}

	/**
	 * Creates a command that performs a player action (PLAY/PAUSE/STOP)
	 * 
	 * @param state
	 *            action to perform
	 * @return the created Command
	 */
	public static PlayerControllerCommand createPlayerActionCommand(PlayerAction action) {
		return new PlayerControllerCommand(PlayerControllerCommandType.PLAYER_ACTION, null, null, action, null);
	}

	/**
	 * Creates a command that sets the current position in the playlist to a
	 * specified number
	 * 
	 * @param position
	 *            position to set in the playlist
	 * @return the created Command
	 */
	public static PlayerControllerCommand createSetListPosCommand(Integer position) {
		return new PlayerControllerCommand(PlayerControllerCommandType.SET_POS_IN_LIST, position, null, null, null);
	}

	/**
	 * Creates a command that seeks to a specified position in the current song
	 * 
	 * @param position
	 *            position to seek to in milliseconds
	 * @return the created Command
	 */
	public static PlayerControllerCommand createSetSongPosCommand(Integer position) {
		return new PlayerControllerCommand(PlayerControllerCommandType.SET_POS_IN_SONG, position, null, null, null);
	}

	/**
	 * Creates a command that sets the play mode-property of the playlist
	 * 
	 * @param playMode
	 *            the play mode that should be set
	 * @return the created Command
	 */
	public static PlayerControllerCommand createSetPlayModeCommand(PlayModeType playMode) {
		return new PlayerControllerCommand(PlayerControllerCommandType.SET_PLAY_MODE, null, null, null, playMode);
	}

	public PlayerControllerCommandType getType() {
		return type;
	}

	public Integer getPosition() {
		return position;
	}

	public PlaylistSong<BaseArtist, BaseAlbum> getSong() {
		return song;
	}

	public PlayerAction getPlayerAction() {
		return action;
	}

	public PlayModeType getPlayMode() {
		return playMode;
	}

	@Override
	public String toString() {
		switch (getType()) {
			case ADD_SONG:
				return "ADD SONG: " + getSong() + " at pos: " + getPosition();
			case REMOVE_SONG:
				return "REMOVE SONG at pos: " + getPosition();
			case PLAYER_ACTION:
				if (getPlayerAction() == PlayerAction.PLAY) {
					return "PlayerAction: PLAY";
				} else if (getPlayerAction() == PlayerAction.PAUSE) {
					return "PlayerAction: PAUSE";
				} else if (getPlayerAction() == PlayerAction.STOP) {
					return "PlayerAction: STOP";
				}
				break;
			case SET_POS_IN_LIST:
				return "SET POS IN LIST: " + getPosition();
			case SET_POS_IN_SONG:
				return "SET POS IN SONG: " + getPosition();
			case SET_PLAY_MODE:
				return "SET POS PLAY MODE: " + getPlayMode();
		}
		return super.toString();
	}

}
