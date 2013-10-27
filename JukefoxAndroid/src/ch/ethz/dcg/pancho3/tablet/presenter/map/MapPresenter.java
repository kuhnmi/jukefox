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
package ch.ethz.dcg.pancho3.tablet.presenter.map;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ActionMode.Callback;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer.CurrentAlbumProvider;

/**
 * Presenter for the map view. Note: For the map view we reuse a lot of code
 * from the phone player which doesn't follow an MVP interface. Thus, this
 * presenter is quite sparse.
 */
public class MapPresenter implements CurrentAlbumProvider, IOnPlaylistStateChangeListener,
		Callback {

	private final AndroidCollectionModelManager data;
	// An interface to the map view.
	private IMapView map;
	// If not null, the pending album is the one which should be focused on
	// once it is possible.
	private BaseAlbum pendingAlbum;

	private MapAlbum currentlyPlayingMapAlbum;

	public MapPresenter(AndroidCollectionModelManager data) {
		this.data = data;
	}

	/**
	 * An interface to the map view.
	 */
	public static interface IMapView {

		/**
		 * Tells the camera to center the given album.
		 */
		void goToAlbum(BaseAlbum album);

		void newCurrentAlbum();

		void stopSelectingRegion();
	}

	/**
	 * This method needs to be called before this presenter is functional. The
	 * last call to {@link #goToAlbum(ListAlbum)} before this is called will be
	 * kept until the view is ready and executed then.
	 */
	public void setMap(IMapView map) {
		this.map = map;
	}

	/**
	 * Tells the presenter that this album should be focused.
	 */
	public void goToAlbum(BaseAlbum album) {
		if (map != null) {
			map.goToAlbum(album);
		} else {
			// We cache the request.
			pendingAlbum = album;
		}
	}

	/**
	 * Called by the view when its initialization finished.
	 */
	public void viewFinishedInit() {
		if (pendingAlbum != null) {
			// We execute the cached request.
			map.goToAlbum(pendingAlbum);
			map.newCurrentAlbum();
			pendingAlbum = null;
		}
	}

	@Override
	public MapAlbum getCurrentAlbum() {
		return currentlyPlayingMapAlbum;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		map.stopSelectingRegion();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle("Select a region of albums by drawing on the map.");
		mode.setSubtitle("All songs from the albums within this region will be shown.");
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// NOP.
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// NOP.
		return true;
	}

	@Override
	public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
		if (newSong != null) {
			try {
				currentlyPlayingMapAlbum = data.getAlbumProvider().getMapAlbum(newSong.getAlbum());
			} catch (DataUnavailableException e) {
				currentlyPlayingMapAlbum = null;
			}
			if (map != null) {
				map.newCurrentAlbum();
			}
		}
	}

	@Override
	public void onPlayModeChanged(IPlayMode newPlayMode) {
		// NOP.
	}

	@Override
	public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
		// NOP.
	}
}
