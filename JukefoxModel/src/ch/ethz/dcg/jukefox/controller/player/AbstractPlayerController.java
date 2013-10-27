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

import java.util.ArrayList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.IPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IPlaylist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;

/**
 * Manager for everything player or playlist related. This class can be used safely once isReady returns true.
 */
public abstract class AbstractPlayerController implements IPlayerController, IPlaylistManager,
		IPlaybackInfoBroadcaster, IPlayerListenerInformer, IPlaylistListenerInformer {

	@SuppressWarnings("unused")
	private final static String TAG = AbstractPlayerController.class.getSimpleName();

	protected final AbstractCollectionModelManager collectionModel;
	protected final AbstractPlayerModelManager playerModel;

	protected IPlaylistManager currentPlaylistManager;
	protected IPlaybackController playbackController;

	// Playlist state change is handled here.
	private final List<IOnPlaylistStateChangeListener> playlistStateChangeListeners = new ArrayList<IOnPlaylistStateChangeListener>();

	// Player state change is handled here.
	private final List<IOnPlayerStateChangeListener> playerStateChangeListeners = new ArrayList<IOnPlayerStateChangeListener>();

	private boolean isReady = false;

	public AbstractPlayerController(AbstractCollectionModelManager model, AbstractPlayerModelManager playerModel) {
		this.collectionModel = model;
		this.playerModel = playerModel;
		this.currentPlaylistManager = createPlaylistManager(this, collectionModel, playerModel);

		// TODO: do decision if it should create a gapless player or not
		this.playbackController = createPlaybackController(this, collectionModel, playerModel, currentPlaylistManager);
		if (currentPlaylistManager.getCurrentPlaylist() == null) {
			// Ensure that we have a valid playlist from the beginning
			currentPlaylistManager.setPlaylist(new Playlist());
		}
		playerModel.getPlayLog().setPlayerController(this);
		playerModel.getContextProvider().setPlayerController(this);

		isReady = true;
	}

	@Override
	public boolean isReady() {
		return isReady;
	}

	protected abstract IPlaybackController createPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager playlistManager);

	protected abstract IPlaylistManager createPlaylistManager(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel);

	@Override
	public int getDuration() {
		return playbackController.getDuration();
	}

	@Override
	public PlayerState getPlayerState() {
		return playbackController.getPlayerState();
	}

	@Override
	public int getPlaybackPosition() {
		return playbackController.getPlaybackPosition();
	}

	@Override
	public void jumpToPlaylistPosition(int position) {
		playbackController.jumpToPlaylistPosition(position);
	}

	@Override
	public void mute() {
		playbackController.mute();
	}

	@Override
	public void pause() {
		playbackController.pause();
	}

	@Override
	public void play() {
		// Log.v(TAG, "playerController: " + playbackController.toString());
		playbackController.play();
	}

	@Override
	public void addOnPlayerStateChangeListener(IOnPlayerStateChangeListener listener) {
		if (listener != null) {
			playerStateChangeListeners.add(listener);
		}
	}

	@Override
	public void removeOnPlayerStateChangeListener(IOnPlayerStateChangeListener listener) {
		if (listener != null) {
			playerStateChangeListeners.remove(listener);
		}
	}

	@Override
	public void seekTo(int position) {
		playbackController.seekTo(position);
	}

	@Override
	public void stop() {
		playbackController.stop();
	}

	@Override
	public void unmute() {
		playbackController.unmute();
	}

	// Below here are the methods implementing IPlaylistController.
	@Override
	public void addOnPlaylistStateChangeListener(IOnPlaylistStateChangeListener listener) {
		synchronized (playlistStateChangeListeners) {
			if (listener != null) {
				playlistStateChangeListeners.add(listener);
			}
		}
	}

	@Override
	public void removeOnPlaylistStateChangeListener(IOnPlaylistStateChangeListener listener) {
		if (listener != null) {
			synchronized (playlistStateChangeListeners) {
				playlistStateChangeListeners.remove(listener);
			}
		}
	}

	@Override
	public void appendSongAtEnd(PlaylistSong<BaseArtist, BaseAlbum> song) {
		currentPlaylistManager.appendSongAtEnd(song);
	}

	@Override
	public void appendSongsAtEnd(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		currentPlaylistManager.appendSongsAtEnd(songs);
	}

	@Override
	public void clearPlaylist() {
		playbackController.stop();
		currentPlaylistManager.clearPlaylist();
		currentPlaylistManager.getPlayMode().reset();
	}

	@Override
	public IReadOnlyPlaylist getCurrentPlaylist() {
		return currentPlaylistManager.getCurrentPlaylist();
	}

	@Override
	public int getCurrentSongIndex() throws EmptyPlaylistException {
		return currentPlaylistManager.getCurrentSongIndex();
	}

	@Override
	public void setCurrentSongIndex(int index) throws PlaylistPositionOutOfRangeException {
		currentPlaylistManager.setCurrentSongIndex(index);
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong() throws EmptyPlaylistException {
		return currentPlaylistManager.getCurrentSong();
	}

	@Override
	public IPlayMode getPlayMode() {
		// Log.v(TAG, playbackController.toString());
		// Log.v(TAG,
		// playbackController.getCurrentPlaylistManager().toString());
		// Log.v(TAG,
		// playbackController.getCurrentPlaylistManager().getPlayMode().toString());
		return currentPlaylistManager.getPlayMode();
	}

	@Override
	public void insertSongAtPosition(PlaylistSong<BaseArtist, BaseAlbum> song, int position)
			throws PlaylistPositionOutOfRangeException {
		currentPlaylistManager.insertSongAtPosition(song, position);
	}

	@Override
	public void insertSongsAtPosition(List<PlaylistSong<BaseArtist, BaseAlbum>> songs, int position)
			throws PlaylistPositionOutOfRangeException {
		currentPlaylistManager.insertSongsAtPosition(songs, position);
	}

	@Override
	public void insertSongAsNext(PlaylistSong<BaseArtist, BaseAlbum> song) {
		currentPlaylistManager.insertSongAsNext(song);
	}

	@Override
	public void insertSongsAsNext(List<PlaylistSong<BaseArtist, BaseAlbum>> songs)
			throws PlaylistPositionOutOfRangeException, EmptyPlaylistException {
		currentPlaylistManager.insertSongsAsNext(songs);
	}

	@Override
	public boolean isPlaylistEmptyOrAtEnd() {
		return currentPlaylistManager.isPlaylistEmptyOrAtEnd();
	}

	@Override
	public void setPlaylist(IPlaylist playlist) {
		playbackController.setPlaylist(playlist);
	}

	@Override
	public void moveSong(int oldPosition, int newPosition) throws EmptyPlaylistException,
			PlaylistPositionOutOfRangeException {
		currentPlaylistManager.moveSong(oldPosition, newPosition);
	}

	@Override
	public void playNext() throws EmptyPlaylistException, NoNextSongException {
		playbackController.next();
	}

	@Override
	public void playSongAtPosition(int position) throws PlaylistPositionOutOfRangeException {
		playbackController.jumpToPlaylistPosition(position);
		playbackController.play();
	}

	@Override
	public void loadSongAtPosition(int position) throws PlaylistPositionOutOfRangeException {
		playbackController.jumpToPlaylistPosition(position);
	}

	@Override
	public void playPrevious() throws EmptyPlaylistException, NoNextSongException {
		playbackController.previous();
		playbackController.play();
	}

	@Override
	public void removeSongFromPlaylist(int position) throws EmptyPlaylistException, PlaylistPositionOutOfRangeException {
		currentPlaylistManager.removeSongFromPlaylist(position);
	}

	// Below here are the methods implementing IPlayListener.
	@Override
	public void informPlaylistChangeListener(IReadOnlyPlaylist playlist) {
		synchronized (playlistStateChangeListeners) {
			for (IOnPlaylistStateChangeListener listener : playlistStateChangeListeners) {
				listener.onPlaylistChanged(playlist);
			}
		}
	}

	@Override
	public void informPlayModeChangeListener(IPlayMode newPlayMode) {
		if (newPlayMode == null) {
			return;
		}
		synchronized (playlistStateChangeListeners) {
			for (IOnPlaylistStateChangeListener listener : playlistStateChangeListeners) {
				listener.onPlayModeChanged(newPlayMode);
			}
		}
	}

	@Override
	public void informCurrentSongChangeListener(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
		if (newSong == null) {
			return;
		}
		synchronized (playlistStateChangeListeners) {
			for (IOnPlaylistStateChangeListener listener : playlistStateChangeListeners) {
				listener.onCurrentSongChanged(newSong);
			}
		}
	}

	@Override
	public void informSongCompletedListeners(PlaylistSong<BaseArtist, BaseAlbum> song) {
		if (song == null) {
			return;
		}
		Log.v(TAG, "song completed informer start");
		for (IOnPlayerStateChangeListener listener : playerStateChangeListeners) {
			listener.onSongCompleted(song);
		}
		Log.v(TAG, "song completed informer end");
	}

	@Override
	public void informSongSkippedListeners(PlaylistSong<BaseArtist, BaseAlbum> song) {
		if (song == null) {
			return;
		}
		Log.v(TAG, "song completed informer start");
		for (IOnPlayerStateChangeListener listener : playerStateChangeListeners) {
			listener.onSongSkipped(song, playbackController.getPlaybackPosition());
		}
		// Log.v(TAG,"song completed informer end");
	}

	@Override
	public void informSongStartedListeners(PlaylistSong<BaseArtist, BaseAlbum> song) {
		if (song == null) {
			return;
		}
		Log.v(TAG, "informSongStartedListeners()");
		for (IOnPlayerStateChangeListener listener : playerStateChangeListeners) {
			if (listener != null) {
				listener.onSongStarted(song);
				Log.v(TAG, "informSongStartedListeners() inform");
			}
		}
		Log.v(TAG, "informSongStartedListeners() 2");
	}

	@Override
	public void informPlayerStateChangedListeners(PlayerState playerState) {
		if (playerState == null) {
			return;
		}
		Log.v(TAG, "informPlayerStateChanged started");
		for (IOnPlayerStateChangeListener listener : playerStateChangeListeners) {
			listener.onPlayerStateChanged(playerState);
		}
		Log.v(TAG, "informPlayerStateChanged ended");
	}

	// public void loadPlaylist(String name, boolean readPositionInfo) throws
	// IOException {
	// PlaylistReader.loadPlaylistFromFileByName(this, playerController,
	// collectionModel.getDbWrapper(), name,
	// readPositionInfo);
	// }
	//
	// public void loadVideoPlaylist(String name, boolean readPositionInfo)
	// throws IOException {
	// PlaylistReader.loadVideoPlaylistFromFileByName(this, playerController,
	// collectionModel.getDbWrapper(), name,
	// readPositionInfo);
	// }

	// public void savePlaylist(IReadOnlyPlaylist playlist, String name) {
	// PlaylistWriter
	// .writePlaylistToFile(collectionModel.getDbWrapper(), this,
	// playerController, playlist, name, true);
	// }

	@Override
	public void setPlayMode(PlayModeType playModeType, int artistAvoidance, int songAvoidance) {
		playbackController.setPlayMode(playModeType, artistAvoidance, songAvoidance);
	}

	@Override
	public void setPlayMode(IPlayMode playMode) {
		playbackController.setPlayMode(playMode);
	}

	@Override
	public void shufflePlaylist(int startPosition) {
		currentPlaylistManager.shufflePlaylist(startPosition);
	}
}
