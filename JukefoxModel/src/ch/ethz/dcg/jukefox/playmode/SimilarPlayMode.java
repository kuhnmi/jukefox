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

import java.util.Collections;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;

/**
 * Plays similar songs to the first song being played.
 * 
 * TODO: this mode is broken.
 */
public class SimilarPlayMode extends BasePlayMode {

	public static final String TAG = SimilarPlayMode.class.getSimpleName();

	private static final int NUM_CANDIDATES = 5;

	// private final LinkedHashSet<Integer> recentSongHistory = new
	// LinkedHashSet<Integer>();
	// private final LinkedHashSet<Integer> recentArtistHistory = new
	// LinkedHashSet<Integer>();
	private BaseSong<BaseArtist, BaseAlbum> seedSong;
	// private boolean isInitialized = false;
	private int similarArtistAvoidanceNumber;
	private int equalSongAvoidanceNumber;

	public SimilarPlayMode(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			int artistAvoidance, int equalSongAvoidance, BaseSong<BaseArtist, BaseAlbum> seedSong) {
		super(collectionModel, playerModel);
		this.seedSong = seedSong;
		similarArtistAvoidanceNumber = artistAvoidance;
		equalSongAvoidanceNumber = equalSongAvoidance;
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
	public PlayModeType getPlayModeType() {
		return PlayModeType.SIMILAR;
	}

	@Override
	public PlayerControllerCommands next(IReadOnlyPlaylist currentPlaylist) throws NoNextSongException {
		PlayerControllerCommands changes = new PlayerControllerCommands();
		try {
			PlaylistSong<BaseArtist, BaseAlbum> songToAdd;

			if (currentPlaylist.isPlaylistEmpty()) {
				songToAdd = getRandomSongWithCoordinates();
				changes.addSong(songToAdd, 0);
				changes.setListPos(0);
			} else if (!isPlaylistAtEnd(currentPlaylist)) {
				Integer nextPos = currentPlaylist.getPositionInList() + 1;
				changes.setListPos(nextPos);
			} else {
				songToAdd = getSimilarSong(currentPlaylist);
				Integer curPos = currentPlaylist.getPositionInList() + 1;
				changes.addSong(songToAdd, curPos);
				changes.setListPos(curPos);
			}
			changes.playerAction(PlayerAction.PLAY);
			return changes;

		} catch (Exception e) {
			Log.w(TAG, e);
			throw new NoNextSongException(e.getMessage(), e);
		}
	}

	private boolean isPlaylistAtEnd(IReadOnlyPlaylist currentPlaylist) {
		return currentPlaylist.getPositionInList() >= currentPlaylist.getSize() - 1;
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getRandomSongWithCoordinates() throws NoNextSongException {
		try {
			PlaylistSong<BaseArtist, BaseAlbum> song = null;
			int loopCnt = 0;
			boolean checkArtist = similarArtistAvoidanceNumber > 0;
			do {
				List<PlaylistSong<BaseArtist, BaseAlbum>> songs = collectionModel.getSongProvider()
						.getRandomSongWithCoordinates(1);
				if (songs == null || songs.size() == 0) {
					throw new NoNextSongException("No random song with coordinates available!");
				}
				song = songs.get(0);
				// getRandomSong is only called for empty playlists, thus we can
				// pass -1 as current song id to isForbiddenSong
			} while (isForbiddenSong(null, song, checkArtist) && loopCnt++ < 10);
			return song;
		} catch (DataUnavailableException e) {
			throw new NoNextSongException(e.getMessage(), e);
		} catch (Exception e) {
			Log.w(TAG, e);
			throw new NoNextSongException(e.getMessage(), e);
		}
	}

	private boolean isForbiddenSong(BaseSong<BaseArtist, BaseAlbum> currentSong,
			BaseSong<BaseArtist, BaseAlbum> candidate, boolean checkArtist) {
		if (currentSong != null && currentSong.getId() == candidate.getId()) {
			return true;
		}
		if (playerModel.getPlayLog().isSongInRecentHistory(candidate, equalSongAvoidanceNumber)) {
			return true;
		}
		if (checkArtist) {
			int artistId = candidate.getArtist().getId();
			if (currentSong != null && currentSong.getArtist().getId() == artistId) {
				return true;
			}
			if (playerModel.getPlayLog().isArtistInRecentHistory(candidate.getArtist(), similarArtistAvoidanceNumber)) {
				return true;
			}
		}
		return false;
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getSimilarSong(IReadOnlyPlaylist playlist) throws NoNextSongException {
		try {

			float[] meanPos = computeSeedPosition(playlist.getPositionInList(), playlist);

			PlaylistSong<BaseArtist, BaseAlbum> song = getCloseSong(playlist, meanPos);

			return song;
		} catch (PlaylistPositionOutOfRangeException e) {
			throw new NoNextSongException();
		}
	}

	private float[] computeSeedPosition(int currentPosition, IReadOnlyPlaylist playlist)
			throws PlaylistPositionOutOfRangeException {
		BaseSong<BaseArtist, BaseAlbum> currentSong = playlist.getSongAtPosition(currentPosition);
		if (seedSong == null) {
			seedSong = playlist.getSongAtPosition(0);
		}
		float[] seedSongCoords;
		try {
			// TODO: should we save seedCoords instead of seedSong??
			seedSongCoords = collectionModel.getOtherDataProvider().getSongCoordinates(seedSong);
		} catch (DataUnavailableException e1) {
			seedSongCoords = new float[Constants.DIM];
		}
		float[] currentSongCoords;
		try {
			currentSongCoords = collectionModel.getOtherDataProvider().getSongCoordinates(currentSong);
		} catch (DataUnavailableException e1) {
			currentSongCoords = new float[Constants.DIM];
		}
		float[] meanPos = Utils.getMean(seedSongCoords, currentSongCoords);
		return meanPos;
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getCloseSong(IReadOnlyPlaylist playlist, float[] meanPos)
			throws NoNextSongException {
		int numCandidates = NUM_CANDIDATES;

		PlaylistSong<BaseArtist, BaseAlbum> currentSong = getCurrentSongOrNull(playlist);
		// int currentSongId = currentSong == null ? -1 : currentSong.getId();

		PlaylistSong<BaseArtist, BaseAlbum> song = null;
		int loopCnt = -1;

		boolean checkArtist = similarArtistAvoidanceNumber > 0;

		do {
			loopCnt++;
			List<BaseSong<BaseArtist, BaseAlbum>> candidates;
			try {
				candidates = collectionModel.getSongProvider().getClosestBaseSongsToPosition(meanPos, numCandidates);
			} catch (DataUnavailableException e) {
				// TODO: inform that similar song mode is not yet available
				throw new NoNextSongException();
			}
			Collections.shuffle(candidates);
			Utils.printSongCollection("candidates:", candidates);
			Log.v("Utils", "currentSongId: " + (currentSong == null ? null : currentSong.getId()));
			for (BaseSong<BaseArtist, BaseAlbum> candidate : candidates) {
				// Utils.printCollection("recent song history:",
				// recentSongHistory);				
				if (!isForbiddenSong(currentSong, candidate, checkArtist)) {
					song = new PlaylistSong<BaseArtist, BaseAlbum>(candidate, SongSource.SIMILAR_PLAY_MODE);
					break;
				}
			}
			if (song == null) {
				numCandidates *= 2;
			}
		} while (song == null && loopCnt <= 7);
		// TODO: handle song == null here!
		return song;
	}

	private PlaylistSong<BaseArtist, BaseAlbum> getCurrentSongOrNull(IReadOnlyPlaylist playlist) {
		try {
			return playlist.getSongAtPosition(playlist.getPositionInList());
		} catch (PlaylistPositionOutOfRangeException e) {
			return null;
		}
	}
}
