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

import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

/**
 * Playlist controller which plays a playlist exactly once.
 * 
 * TODO: once the playlist is finished, the last song is repeated instead of the whole play stopped. Desired?
 */
public class PlayOncePlayMode extends BasePlayMode {

	@SuppressWarnings("unused")
	private static final String TAG = PlayOncePlayMode.class.getSimpleName();

	public PlayOncePlayMode(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		super(collectionModel, playerModel);
	}

	@Override
	public PlayModeType getPlayModeType() {
		return PlayModeType.PLAY_ONCE;
	}

	@Override
	public PlayerControllerCommands next(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException {
		PlayerControllerCommands commands = new PlayerControllerCommands();
		if (currentPlaylist.getPositionInList() < currentPlaylist.getSize() - 1) {
			commands.setListPos(currentPlaylist.getPositionInList() + 1);
			commands.playerAction(PlayerAction.PLAY);
		}
		return commands;
	}
}
