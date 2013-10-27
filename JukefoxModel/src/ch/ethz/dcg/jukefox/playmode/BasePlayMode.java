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
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

public abstract class BasePlayMode implements IPlayMode {

	public static final String TAG = BasePlayMode.class.getSimpleName();

	protected AbstractCollectionModelManager collectionModel;
	protected AbstractPlayerModelManager playerModel;

	public BasePlayMode(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		this.collectionModel = collectionModel;
		this.playerModel = playerModel;
	}

	@Override
	public PlayerControllerCommands initialize(IReadOnlyPlaylist currentPlaylist) {
		return new PlayerControllerCommands();
	}

	@Override
	public void reset() {
	}

	/**
	 * Jumps back one position in the playlist or says at the posiion if it was position 0
	 */
	@Override
	public PlayerControllerCommands previous(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException {
		int oldPos = currentPlaylist.getPositionInList();
		int newPos = oldPos - 1;
		if (newPos < 0) {
			newPos = 0;
		}
		PlayerControllerCommands changes = new PlayerControllerCommands();
		changes.setListPos(newPos);
		changes.playerAction(PlayerAction.PLAY);
		return changes;
	}

	protected boolean isPlaylistAtEnd(Playlist playlist) {
		return playlist.getPositionInList() >= playlist.getSongList().size() - 1;
	}

}
