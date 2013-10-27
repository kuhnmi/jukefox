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
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;
import ch.ethz.dcg.jukefox.model.player.PlayerState;

/**
 * Chooses a smart song once the playlist is at an end.
 */
public class SmartShufflePlayMode extends BasePlayMode implements IOnPlayerStateChangeListener {

	private final static String TAG = SmartShufflePlayMode.class.getSimpleName();
	public final static int SMART_SHUFFLING_MIN_SONGS = 20;

	private final SmartShuffleManager smartShuffleManager;

	private boolean actualSongSkipped = false;

	private PlaylistSong<BaseArtist, BaseAlbum> peekedSongSkipped = null;
	private PlaylistSong<BaseArtist, BaseAlbum> peekedSongNonSkipped = null;

	// private boolean isInitialized = false;

	public SmartShufflePlayMode(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			SmartShuffleManager smartShuffleManager, IReadOnlyPlayerController playerController) {
		super(collectionModel, playerModel);
		this.smartShuffleManager = smartShuffleManager;
		playerController.addOnPlayerStateChangeListener(this);
	}

	@Override
	public PlayerControllerCommands initialize(IReadOnlyPlaylist currentPlaylist) {
		PlayerControllerCommands changes = deleteSubsequentSongs(currentPlaylist);
		// isInitialized = true;
		return changes;
	}

	private PlayerControllerCommands deleteSubsequentSongs(IReadOnlyPlaylist currentPlaylist) {
		// On the first start, we remove all the songs from the playlist
		// below the song
		// we play.
		int curPos = currentPlaylist.getPositionInList();
		int numSongToRemove = currentPlaylist.getSongList().size() - curPos - 1;
		PlayerControllerCommands changes = new PlayerControllerCommands();
		for (int i = 0; i < numSongToRemove; i++) {
			changes.removeSong(curPos + 1);
		}
		return changes;
	}

	@Override
	public PlayerControllerCommands next(IReadOnlyPlaylist playlist) {
		PlayerControllerCommands commands = new PlayerControllerCommands();
		try {

			int currentSongId = getCurrentSongIdOrDefaultIfEmptyPlaylist(-1, playlist);
			PlaylistSong<BaseArtist, BaseAlbum> nextSong = peekNextSong(actualSongSkipped, playlist);
			clearPeekedSongs();

			if (playlist.isPlaylistEmpty()) {
				commands.addSong(nextSong, 0);
			} else if (playlist.getPositionInList() >= playlist.getSize() - 1) {
				commands.addSong(nextSong, playlist.getPositionInList() + 1);
			}
			// currentlyPlaying = nextSong.getId();
			Log.v(TAG, "next: selected song id " + nextSong.getId());

			if (currentSongId != -1) {
				float rating = smartShuffleManager.getRatingForSignal(actualSongSkipped);
				smartShuffleManager.processSong(currentSongId, rating);
				commands.setListPos(playlist.getPositionInList() + 1);
			} else {
				commands.setListPos(0);
			}
			commands.playerAction(PlayerAction.PLAY);

		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
		}
		return commands;
	}

	private int getCurrentSongIdOrDefaultIfEmptyPlaylist(int defaultValue, IReadOnlyPlaylist playlist)
			throws PlaylistPositionOutOfRangeException {
		int currentSongId = defaultValue;
		try {
			currentSongId = playlist.getSongList().get(playlist.getPositionInList()).getId();
		} catch (Exception e) {
			// We will still add a next song.
		}
		return currentSongId;
	}

	@Override
	public PlayModeType getPlayModeType() {
		return PlayModeType.SMART_SHUFFLE;
	}

	public PlaylistSong<BaseArtist, BaseAlbum> peekNextSong(boolean skipped, IReadOnlyPlaylist playlist)
			throws NoNextSongException {
		Log.v(TAG, "peekNext()");

		PlaylistSong<BaseArtist, BaseAlbum> nextSong = getPeekedSong(skipped);
		if (nextSong != null) {
			return nextSong;
		}
		try {

			if (playlist.getPositionInList() < playlist.getSize() - 1) {
				nextSong = playlist.getSongAtPosition(playlist.getPositionInList() + 1);
				setPeekedSong(nextSong, skipped);
				return nextSong;
			}
			int currentlyPlaying = getCurrentSongIdOrDefaultIfEmptyPlaylist(-1, playlist);

			nextSong = smartShuffleManager.getSong(currentlyPlaying, skipped);
			setPeekedSong(nextSong, skipped);

			return nextSong;
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		try {
			return new PlaylistSong<BaseArtist, BaseAlbum>(collectionModel.getSongProvider().getBaseSong(
					collectionModel.getOtherDataProvider().getRandomSongId()), SongSource.RANDOM_SONG);
		} catch (DataUnavailableException e1) {
			e1.printStackTrace();
		}
		throw new NoNextSongException();
	}

	private void setPeekedSong(PlaylistSong<BaseArtist, BaseAlbum> song, boolean skipped) {
		if (skipped) {
			peekedSongSkipped = song;
		} else {
			peekedSongNonSkipped = song;
		}
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getPeekedSong(boolean skipped) {
		if (skipped) {
			return peekedSongSkipped;
		} else {
			return peekedSongNonSkipped;
		}
	}

	protected void clearPeekedSongs() {
		peekedSongSkipped = null;
		peekedSongNonSkipped = null;
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {

	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		actualSongSkipped = false;
		smartShuffleManager.processSong(song.getId(), smartShuffleManager.getRatingForSignal(false));
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
		actualSongSkipped = true;
		smartShuffleManager.processSong(song.getId(), smartShuffleManager.getRatingForSignal(true));
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		smartShuffleManager.addToPlayed(song.getId());
	}

}
