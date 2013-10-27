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
package ch.ethz.dcg.pancho3.tablet.view.queue;

import java.util.List;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.widget.MagicListAdapter.MagicListInnerAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.QueueItem;

/**
 * An adapter that is used to display the contents of a playlist. The top song
 * which is displayed is always the one playing; the songs before are not
 * displayed by this adapter.
 * 
 * This adapter needs to be used with MagicPlaylistController which contains the
 * corresponding logic.
 */
public class PlaylistAdapter implements
		MagicListInnerAdapter<PlaylistSong<BaseArtist, BaseAlbum>> {

	private static final String TAG = PlaylistAdapter.class.getSimpleName();

	// Used to notify data set changes.
	private final DataSetObservable dataSetObservable = new DataSetObservable();
	// Used to obtain views for the songs.
	private final ViewFactory viewFactory;
	// We make changes to the playlist only via the playlist controller.
	private final MagicPlayMode magicPlaylistController;
	private final AndroidPlayerController playerController;
	// The current playlist which this adapter is displaying.
	// We start with a dummy playlist to avoid having a null value.
	private IReadOnlyPlaylist playlist = new Playlist();
	// The index of the song is currently playing or how many 
	// songs are not displayed (since they are above the current song).
	// This value is the same as magicListController.getCurrentSongIndex()
	// or 0, if the current playlist is empty.
	private int playlistOffset = 0;

	public PlaylistAdapter(ViewFactory viewFactory,
			MagicPlayMode magicPlaylistController) {
		this.viewFactory = viewFactory;
		this.magicPlaylistController = magicPlaylistController;
		this.playerController = JukefoxApplication.getPlayerController();
	}

	/**
	 * This is called whenever the playlist changed with either a new playlist
	 * or also the same playlist object which is already in this adapter. If a
	 * new playlist object is provided, it is played from the beginning, if it's
	 * the same playlist object, then we only adjust the display and change the
	 * song if the top song position changed.
	 */
	public void playlistChanged(IReadOnlyPlaylist playlist) {
		// TODO: what do we actually need to do here.
		if (this.playlist != playlist) {
			// We have a new playlist and start fresh.
			this.playlist = playlist;
			playlistOffset = 0;
			try {
				playlistOffset = playerController.getCurrentSongIndex();
			} catch (EmptyPlaylistException e) {
				Log.w(TAG, e);
			}
		}
		// We need to update the displayed items.
		notifyDataSetChanged();
	}

	public boolean clearPlaylistExceptPlayingSong() {
		return magicPlaylistController.clearPlaylistExceptPlayingSong(playerController);
	}

	public boolean shuffle() {
		return magicPlaylistController.shuffle(playerController);
	}

	public void undoClear() {
		magicPlaylistController.undoClear(playerController);
	}

	/**
	 * Needs to be called when the current song changes so the view can react
	 * accordingly.
	 */
	public void currentSongChanged() {
		try {
			playlistOffset = playerController.getCurrentSongIndex();
		} catch (EmptyPlaylistException e) {
			playlistOffset = 0;
			Log.w(TAG, e);
		}
		notifyDataSetChanged();
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getItem(int position) {
		try {
			return playlist.getSongAtPosition(position + playlistOffset);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
			throw new RuntimeException("Playlist position out of range exception.");
		}
	}

	@Override
	public void appendItem(PlaylistSong<BaseArtist, BaseAlbum> song) {
		playerController.appendSongAtEnd(song);
	}

	@Override
	public void moveItem(int startPosition, int endPosition) {
		try {
			magicPlaylistController.moveSong(playerController, startPosition + playlistOffset,
					endPosition + playlistOffset);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public void removeItem(int position) {
		try {
			if (position == 0) {
				playerController.playNext();
			} else {
				magicPlaylistController.removeSongFromPlaylist(playerController, position + playlistOffset);
			}
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public QueueItem getView(int position, View convertView, ViewGroup parent) {
		QueueItem queueItemConvertView = null;
		if (convertView instanceof QueueItem) {
			queueItemConvertView = (QueueItem) convertView;
		}
		try {
			return viewFactory.getSongViewQueue(playlist.getSongAtPosition(position + playlistOffset),
					queueItemConvertView, parent, position);
		} catch (PlaylistPositionOutOfRangeException e) {
			return null;
		}
	}

	@Override
	public View getDraggingView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent);
	}

	@Override
	public void notifyDataSetChanged() {
		dataSetObservable.notifyChanged();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	@Override
	public int getCount() {
		return playlist.getPlaylistSize() - playlistOffset;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return playlist.getPlaylistSize() - playlistOffset == 0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		dataSetObservable.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		dataSetObservable.unregisterObserver(observer);
	}

	@Override
	public void insertItemsAndRemoveLast(List<PlaylistSong<BaseArtist, BaseAlbum>> items,
			int insertPosition) {
		magicPlaylistController.insertSongsAndRemoveLast(playerController, items, insertPosition + playlistOffset);
	}
}
