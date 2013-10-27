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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.AbstractPlayerController;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;
import ch.ethz.dcg.jukefox.model.player.PlayerState;

/**
 * TOOD: work in progress.
 */
public class MagicPlayMode extends BasePlayMode implements IOnPlayerStateChangeListener {

	private static final String TAG = MagicPlayMode.class.getSimpleName();
	private static final int NUM_RECENT_SONGS = 50;
	private final SmartShuffleManager smartShuffleManager;

	private PlaylistSong<BaseArtist, BaseAlbum> peekedSongSkipped = null;
	private PlaylistSong<BaseArtist, BaseAlbum> peekedSongNonSkipped = null;

	private final ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> clearedSongs =
			new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();

	private final LinkedHashMap<BaseSong<BaseArtist, BaseAlbum>, Boolean> recentSongs =
			new LinkedHashMap<BaseSong<BaseArtist, BaseAlbum>, Boolean>() {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean removeEldestEntry(Entry<BaseSong<BaseArtist, BaseAlbum>, Boolean> eldest) {
					if (size() > NUM_RECENT_SONGS) {
						return true;
					}
					return false;
				}
			};

	private int autofillNumberOfSongs = 0;
	private boolean actualSongSkipped = false;

	public MagicPlayMode(AbstractCollectionModelManager collectionModel,
			AbstractPlayerModelManager playerModel,
			SmartShuffleManager smartShuffleManager, IReadOnlyPlayerController playerController) {
		super(collectionModel, playerModel);
		this.smartShuffleManager = smartShuffleManager;
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
		return PlayModeType.MAGIC;
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
		actualSongSkipped = false;
		smartShuffleManager.processSong(song.getId(), smartShuffleManager.getRatingForSignal(true));
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		smartShuffleManager.addToPlayed(song.getId());
	}

	public void swapPlayingSongWith(AbstractPlayerController playerController,
			List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		int currentSongIndex = 0;
		try {
			currentSongIndex = playerController.getCurrentSongIndex();
		} catch (EmptyPlaylistException e) {
		}
		try {
			playerController.insertSongsAtPosition(songs, currentSongIndex);
		} catch (PlaylistPositionOutOfRangeException e) {
		}
		try {
			playerController.removeSongFromPlaylist(songs.size() + currentSongIndex);
		} catch (PlaylistPositionOutOfRangeException e) {
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		playerController.jumpToPlaylistPosition(0);
		playerController.play();
	}

	public void rateSong(int songId, boolean positive) {
		float rating = positive ? 1 : -1;
		smartShuffleManager.processSong(songId, rating);
	}

	public void moveSong(AbstractPlayerController playerController, int oldPosition, int newPosition)
			throws PlaylistPositionOutOfRangeException {
		if (oldPosition == newPosition || oldPosition < 0 || newPosition < 0
				|| oldPosition >= playerController.getCurrentPlaylist().getPlaylistSize()
				|| newPosition >= playerController.getCurrentPlaylist().getPlaylistSize()
				|| oldPosition == newPosition) {
			return;
		}
		try {
			playerController.moveSong(oldPosition, newPosition);
		} catch (EmptyPlaylistException e1) {
			Log.w(TAG, e1);
		}

		try {
			int currentSongIndex = playerController.getCurrentSongIndex();
			if (newPosition == currentSongIndex || oldPosition == currentSongIndex) {
				playerController.jumpToPlaylistPosition(currentSongIndex);
				playerController.play();
			}
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getSong(AbstractPlayerController playerController, boolean skipped) {
		PlaylistSong<BaseArtist, BaseAlbum> nextSong = null;
		try {
			int currentlyPlaying = -1;
			try {
				currentlyPlaying = playerController.getCurrentSong().getId();
			} catch (EmptyPlaylistException e) {
				Log.w(TAG, e);
			}
			nextSong = smartShuffleManager.getSong(currentlyPlaying, skipped);
			smartShuffleManager.addToPlayed(nextSong.getId());
			rateSong(nextSong.getId(), true);
			if (nextSong == null) {
				nextSong = collectionModel.getSongProvider().getRandomSong();
			}
		} catch (DataUnavailableException e) {
		}
		return nextSong;
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

	private void clearPeekedSongs() {
		peekedSongSkipped = null;
		peekedSongNonSkipped = null;
	}

	public void insertSongsAndRemoveLast(AbstractPlayerController playerController,
			List<PlaylistSong<BaseArtist, BaseAlbum>> songs,
			int insertPosition) {
		try {
			playerController.insertSongsAtPosition(songs, insertPosition);
			playerController.removeSongFromPlaylist(playerController.getCurrentPlaylist().getPlaylistSize() - 1);
			if (insertPosition == playerController.getCurrentSongIndex()) {
				playerController.jumpToPlaylistPosition(insertPosition);
				playerController.play();
			}
		} catch (PlaylistPositionOutOfRangeException e) {
		} catch (EmptyPlaylistException e) {
		}
		autofill(playerController);
	}

	public boolean clearPlaylistExceptPlayingSong(AbstractPlayerController playerController) {
		boolean cleared = false;
		try {
			IReadOnlyPlaylist oldPlaylist = playerController.getCurrentPlaylist();
			int size = oldPlaylist.getPlaylistSize();
			if (size > playerController.getCurrentSongIndex() + 1) {
				clearedSongs.clear();
				for (int i = playerController.getCurrentSongIndex() + 1; i < size; i++) {
					clearedSongs.add(oldPlaylist.getSongAtPosition(i));
				}
				cleared = true;
			}
		} catch (EmptyPlaylistException e) {
		} catch (PlaylistPositionOutOfRangeException e) {
		}
		Playlist playlist = new Playlist();
		try {
			playlist.appendSongAtEnd(playerController.getCurrentSong());
		} catch (EmptyPlaylistException e) {
		}
		playerController.setPlaylist(playlist);
		playerController.jumpToPlaylistPosition(0);
		autofill(playerController);
		return cleared;
	}

	public void undoClear(AbstractPlayerController playerController) {
		try {
			playerController.insertSongsAsNext(clearedSongs);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		clearedSongs.clear();
	}

	public void removeSongFromPlaylist(AbstractPlayerController playerController, int position)
			throws PlaylistPositionOutOfRangeException {
		PlaylistSong<BaseArtist, BaseAlbum> song = null;
		try {
			song = playerController.getCurrentSong();
		} catch (EmptyPlaylistException e1) {
			Log.w(TAG, e1);
		}
		if (song != null) {
			addToRecentSongs(song);
		}
		rateSong(playerController.getCurrentPlaylist().getSongAtPosition(position).getId(), false);
		try {
			playerController.removeSongFromPlaylist(position);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		autofill(playerController);
	}

	public void autofill(AbstractPlayerController playerController) {
		int currentSongIndex = 0;
		try {
			currentSongIndex = playerController.getCurrentSongIndex();
		} catch (EmptyPlaylistException e) {
		}
		while (autofillNumberOfSongs - playerController.getCurrentPlaylist().getPlaylistSize() + currentSongIndex > 0) {
			playerController.appendSongAtEnd(getSong(playerController, false));
		}
	}

	public boolean shuffle(AbstractPlayerController playerController) {
		int currentSongIndex = 0;
		try {
			currentSongIndex = playerController.getCurrentSongIndex();
		} catch (EmptyPlaylistException e) {
		}
		if (currentSongIndex == playerController.getCurrentPlaylist().getPlaylistSize() - 1) {
			return false;
		}
		playerController.shufflePlaylist(currentSongIndex + 1);
		return true;
	}

	public int getAutofillNumberOfSongs() {
		return autofillNumberOfSongs;
	}

	public void setAutofillNumberOfSongs(AbstractPlayerController playerController,
			int autofillNumberOfSongs) {
		this.autofillNumberOfSongs = autofillNumberOfSongs;
		autofill(playerController);
	}

	private void addToRecentSongs(PlaylistSong<BaseArtist, BaseAlbum> song) {
		recentSongs.remove(song);
		recentSongs.put(song, Boolean.TRUE);
	}

	public List<BaseSong<BaseArtist, BaseAlbum>> getRecentSongs() {
		ArrayList<BaseSong<BaseArtist, BaseAlbum>> recentSongList =
				new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		for (Entry<BaseSong<BaseArtist, BaseAlbum>, Boolean> entry : recentSongs.entrySet()) {
			recentSongList.add(entry.getKey());
		}
		return recentSongList;
	}
}
