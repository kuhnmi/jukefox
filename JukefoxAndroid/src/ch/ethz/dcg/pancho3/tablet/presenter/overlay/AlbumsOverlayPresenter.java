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
package ch.ethz.dcg.pancho3.tablet.presenter.overlay;

import java.util.List;

import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.tablet.I18nManager;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongListAdapter;

/**
 * We overlay several albums if the user selects a region in the map. Then we
 * display all the songs of those albums.
 */
public class AlbumsOverlayPresenter extends AbstractOverlayPresenter {

	private final DataFetcher dataFetcher;
	// The albums about which we display information.
	private final List<? extends ListAlbum> albums;
	private final SongListAdapter adapter;

	private Menu menu;

	/**
	 * The constructor takes some references and a list of albums which we want
	 * to display.
	 */
	public AlbumsOverlayPresenter(TabletPresenter tabletPresenter,
			MagicPlayMode magicPlaylistController,
			IOverlayView overlay, ViewFactory viewFactory,
			DataFetcher dataFetcher, List<? extends ListAlbum> albums,
			SongListAdapter adapter, I18nManager i18nManager) {
		super(tabletPresenter, magicPlaylistController, overlay, viewFactory, i18nManager);
		this.dataFetcher = dataFetcher;
		this.albums = albums;
		this.adapter = adapter;
	}

	@Override
	public void viewFinishedInit() {
		super.viewFinishedInit();
		dataFetcher.fetchSongsOfAlbums(albums, this);
		overlay.show(this);
		viewFactory.applyImageStack(albums, overlay.getAlbumArt(), 500);
		overlay.setBackgroundColor(DEFAULT_CLOUD_COLOR);
	}

	/**
	 * In the action bar we set a title.
	 */
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		actionMode = mode;
		mode.setTitle(i18nManager.getDisplayNumberOfAlbumsText(albums.size()));
		mode.setSubtitle("");
		this.menu = menu;
		return true;
	}

	@Override
	protected void displaySongs(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		adapter.clear();
		adapter.addAll(songs);
	}

	@Override
	public ISongAdapter getSongAdapter() {
		return adapter;
	}

	@Override
	public void onCheckedIndicesChange(int numberOfIndices, SparseBooleanArray indices,
			int latestChangeIndex) {
		super.onCheckedIndicesChange(numberOfIndices, indices, latestChangeIndex);
		BaseSong<BaseArtist, BaseAlbum> song = getDisplayedSongs().get(latestChangeIndex);
		showNavigationExploreMap(song.getArtist(), song.getAlbum(), menu);
	}
}
