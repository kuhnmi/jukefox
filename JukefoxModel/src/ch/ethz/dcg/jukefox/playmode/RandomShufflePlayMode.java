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

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.CheckedSqlException;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

/**
 * Chooses a random song once the playlist is at the end, but avoids choosing a recently played song.
 */
public class RandomShufflePlayMode extends BasePlayMode {

	private final static String TAG = RandomShufflePlayMode.class.getSimpleName();
	protected int songAvoidanceNumber;

	public RandomShufflePlayMode(AbstractCollectionModelManager collectionModel,
			AbstractPlayerModelManager playerModel, int songAvoidanceNumber) {
		super(collectionModel, playerModel);
		this.songAvoidanceNumber = songAvoidanceNumber;
	}

	@Override
	public PlayModeType getPlayModeType() {
		return PlayModeType.RANDOM_SHUFFLE;
	}

	// Returns a random song which hasn't been played recently
	private PlaylistSong<BaseArtist, BaseAlbum> getNextRandomSong(IReadOnlyPlaylist playlist)
			throws DataUnavailableException, CheckedSqlException {
		PlaylistSong<BaseArtist, BaseAlbum> currentSong = null;
		try {
			currentSong = playlist.getSongAtPosition(playlist.getPositionInList());
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
		PlaylistSong<BaseArtist, BaseAlbum> tempSong = collectionModel.getSongProvider().getRandomSong();
		while (playerModel.getPlayLog().isSongInRecentHistory(tempSong, songAvoidanceNumber)
				|| isEqualToCurrentSong(currentSong, tempSong)) {
			tempSong = collectionModel.getSongProvider().getRandomSong();
		}
		return tempSong;
	}

	private boolean isEqualToCurrentSong(PlaylistSong<BaseArtist, BaseAlbum> currentSong,
			PlaylistSong<BaseArtist, BaseAlbum> tempSong) {
		if (currentSong == null || tempSong == null) {
			return false;
		}
		return currentSong.getId() == tempSong.getId();
	}

	@Override
	public PlayerControllerCommands next(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException {
		PlayerControllerCommands commands = new PlayerControllerCommands();
		if (currentPlaylist.getPositionInList() < currentPlaylist.getSize() - 1) {
			commands.setListPos(currentPlaylist.getPositionInList() + 1);
			commands.playerAction(PlayerAction.PLAY);
		} else if (currentPlaylist.getPositionInList() == currentPlaylist.getSize() - 1) {
			try {
				PlaylistSong<BaseArtist, BaseAlbum> randomSong = getNextRandomSong(currentPlaylist);
				commands.addSong(randomSong, currentPlaylist.getPositionInList() + 1);
				commands.setListPos(currentPlaylist.getPositionInList() + 1);
				commands.playerAction(PlayerAction.PLAY);
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			} catch (CheckedSqlException e) {
				Log.w(TAG, e);
			}
		}
		return commands;
	}

}
