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
package ch.ethz.dcg.jukefox.controller.player.playlistmanager;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IPlaybackInfoBroadcaster;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IPlaylist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.ContextShuffleManager;
import ch.ethz.dcg.jukefox.playmode.ContextShufflePlayMode;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.jukefox.playmode.PlayOncePlayMode;
import ch.ethz.dcg.jukefox.playmode.RandomShufflePlayMode;
import ch.ethz.dcg.jukefox.playmode.RepeatAllPlayMode;
import ch.ethz.dcg.jukefox.playmode.RepeatSongPlayMode;
import ch.ethz.dcg.jukefox.playmode.ShufflePlaylistPlayMode;
import ch.ethz.dcg.jukefox.playmode.SimilarPlayMode;
import ch.ethz.dcg.jukefox.playmode.SmartShuffleManager;
import ch.ethz.dcg.jukefox.playmode.SmartShufflePlayMode;

public class PlaylistManager implements IPlaylistManager {

	private static final String TAG = PlaylistManager.class.getSimpleName();
	private IPlayMode currentPlayMode;
	private IPlaylist currentPlaylist = new Playlist();

	protected AbstractCollectionModelManager collectionModel;
	protected AbstractPlayerModelManager playerModel;
	protected IPlaybackInfoBroadcaster listenerInformer;
	protected IReadOnlyPlayerController playerController;

	protected List<IOnPlaylistStateChangeListener> stateChangeListeners;

	public PlaylistManager(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaybackInfoBroadcaster listenerInformer, IReadOnlyPlayerController playerController) {
		this.collectionModel = collectionModel;
		this.playerModel = playerModel;
		this.listenerInformer = listenerInformer;
		this.playerController = playerController;
		stateChangeListeners = new LinkedList<IOnPlaylistStateChangeListener>();
		setPlayMode(PlayModeType.SMART_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
	}

	@Override
	public void appendSongAtEnd(PlaylistSong<BaseArtist, BaseAlbum> song) {
		currentPlaylist.appendSongAtEnd(song);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void appendSongsAtEnd(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		currentPlaylist.appendSongsAtEnd(songs);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void clearPlaylist() {
		currentPlaylist = new Playlist();
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void insertSongAtPosition(PlaylistSong<BaseArtist, BaseAlbum> song, int position)
			throws PlaylistPositionOutOfRangeException {
		currentPlaylist.insertSongAtPosition(song, position);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void insertSongsAtPosition(List<PlaylistSong<BaseArtist, BaseAlbum>> songs, int position)
			throws PlaylistPositionOutOfRangeException {
		currentPlaylist.insertSongsAtPosition(songs, position);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void insertSongAsNext(PlaylistSong<BaseArtist, BaseAlbum> song) {
		try {
			int position;
			try {
				position = getCurrentSongIndex() + 1;
			} catch (EmptyPlaylistException e) {
				position = 0;
				Log.w(TAG, e);
			}
			if (currentPlaylist.getSize() == 0) {
				position = 0;
			} else if (position >= currentPlaylist.getSize()) {
				position = currentPlaylist.getSize();
			}
			currentPlaylist.insertSongAtPosition(song, position);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
		listenerInformer.informPlaylistChangeListener(currentPlaylist);
	}

	@Override
	public void insertSongsAsNext(List<PlaylistSong<BaseArtist, BaseAlbum>> songs)
			throws PlaylistPositionOutOfRangeException, EmptyPlaylistException {
		try {
			int position;
			try {
				position = getCurrentSongIndex() + 1;
			} catch (EmptyPlaylistException e) {
				position = 0;
				Log.w(TAG, e);
			}
			if (currentPlaylist.getSize() == 0) {
				position = 0;
			} else if (position >= currentPlaylist.getSize()) {
				position = currentPlaylist.getSize();
			}
			currentPlaylist.insertSongsAtPosition(songs, position);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}

		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void moveSong(int oldPosition, int newPosition) throws EmptyPlaylistException,
			PlaylistPositionOutOfRangeException {
		currentPlaylist.moveSong(oldPosition, newPosition);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void removeSongFromPlaylist(int position) throws EmptyPlaylistException, PlaylistPositionOutOfRangeException {
		currentPlaylist.removeSong(position);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	@Override
	public void setCurrentSongIndex(int index) throws PlaylistPositionOutOfRangeException {
		currentPlaylist.setPositionInList(index);
		try {
			//			Log.v(TAG, "SongPosChanged " + index);
			listenerInformer.informCurrentSongChangeListener(getCurrentSong());
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public void setPlayMode(IPlayMode playMode) {
		if (playMode != null) {
			currentPlayMode = playMode;
		}
	}

	@Override
	public void setPlaylist(IPlaylist playlist) {
		if (playlist == null) {
			return;
		}
		currentPlaylist = playlist;
		applyPlaylistExtras();
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

	private void applyPlaylistExtras() {
		if (currentPlaylist.hasExtras()) {
			//			PlayModeType playMode =
			//					PlayModeType.byValue(currentPlaylist.getPlayMode());
			//			setPlayMode(playMode);
			try {
				setCurrentSongIndex(currentPlaylist.getPositionInList());
			} catch (PlaylistPositionOutOfRangeException e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public IReadOnlyPlaylist getCurrentPlaylist() {
		// Update position in Song to the current value
		currentPlaylist.setPositionInSong(playerController.getPlaybackPosition());
		//		Log.v(TAG, "Cur Pos in Song: " + currentPlaylist.getPositionInSong());
		return currentPlaylist;
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getCurrentSong() throws EmptyPlaylistException {
		try {
			//			Log.v(TAG, "Playlist size: " + currentPlaylist.getPlaylistSize());
			return currentPlaylist.getSongAtPosition(getCurrentSongIndex());
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
			throw new EmptyPlaylistException();
		}
	}

	@Override
	public int getCurrentSongIndex() throws EmptyPlaylistException {
		if (currentPlaylist.isPlaylistEmpty()) {
			throw new EmptyPlaylistException();
		}
		return currentPlaylist.getPositionInList();
	}

	@Override
	public IPlayMode getPlayMode() {
		return currentPlayMode;
	}

	@Override
	public boolean isPlaylistEmptyOrAtEnd() {
		int size = currentPlaylist.getSongList().size();
		try {
			return size == 0 || getCurrentSongIndex() >= size - 1;
		} catch (EmptyPlaylistException e) {
			return true;
		}
	}

	@Override
	public void setPlayMode(PlayModeType playModeType, int artistAvoidance, int songAvoidance) {
		IPlayMode playMode = null;
		SmartShuffleManager smartShuffleManager = null;
		//		int artistAvoidance = 0; // TODO: read realSettings
		//		int songAvoidance = 50; // TODO: read realSettings
		switch (playModeType) {
			case MAGIC:
				smartShuffleManager = new SmartShuffleManager(collectionModel, playerModel);
				playMode = new MagicPlayMode(collectionModel, playerModel, smartShuffleManager, playerController);
				break;
			case PLAY_ONCE:
				playMode = new PlayOncePlayMode(collectionModel, playerModel);
				break;
			case RANDOM_SHUFFLE:
				playMode = new RandomShufflePlayMode(collectionModel, playerModel, songAvoidance);
				break;
			case REPEAT:
				playMode = new RepeatAllPlayMode(collectionModel, playerModel);
				break;
			case REPEAT_SONG:
				playMode = new RepeatSongPlayMode(collectionModel, playerModel);
				break;
			case SHUFFLE_PLAYLIST:
				playMode = new ShufflePlaylistPlayMode(collectionModel, playerModel, songAvoidance);
				break;
			case SIMILAR:
				BaseSong<BaseArtist, BaseAlbum> seedSong = null;
				try {
					seedSong = getCurrentSong();
				} catch (EmptyPlaylistException e) {
					e.printStackTrace();
				}
				playMode = new SimilarPlayMode(collectionModel, playerModel, artistAvoidance, songAvoidance, seedSong);
				break;
			case SMART_SHUFFLE:
				smartShuffleManager = new SmartShuffleManager(collectionModel, playerModel);
				playMode = new SmartShufflePlayMode(collectionModel, playerModel, smartShuffleManager, playerController);
				break;
			case CONTEXT_SHUFFLE:
				songAvoidance = 0; // TODO: read realSettings
				ContextShuffleManager contextShuffleManager = new ContextShuffleManager(collectionModel, playerModel);
				playMode = new ContextShufflePlayMode(collectionModel, playerModel, contextShuffleManager,
						playerController);
				break;
		}
		setPlayMode(playMode);
	}

	@Override
	public void shufflePlaylist(int startPosition) {
		currentPlaylist.shuffle(startPosition);
		listenerInformer.informPlaylistChangeListener(getCurrentPlaylist());
	}

}
