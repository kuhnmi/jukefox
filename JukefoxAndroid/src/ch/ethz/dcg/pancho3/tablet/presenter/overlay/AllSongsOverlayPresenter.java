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
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.I18nManager;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongListAdapter;

/**
 * This overlay displays all the songs.
 */
public class AllSongsOverlayPresenter extends AbstractOverlayPresenter {

	private final DataFetcher dataFetcher;
	private final SongListAdapter adapter;

	private Menu menu;

	/**
	 * The constructor takes the references needed by the superclass.
	 * 
	 * @param adapter
	 */
	public AllSongsOverlayPresenter(TabletPresenter tabletPresenter,
			MagicPlayMode magicPlaylistController,
			IOverlayView overlay, ViewFactory viewFactory, DataFetcher dataFetcher,
			SongListAdapter adapter, I18nManager i18nManager) {
		super(tabletPresenter, magicPlaylistController, overlay, viewFactory, i18nManager);
		this.dataFetcher = dataFetcher;
		this.adapter = adapter;
	}

	@Override
	public void viewFinishedInit() {
		super.viewFinishedInit();
		dataFetcher.fetchAllSongs(this);
		overlay.show(this);
		viewFactory.applyImageStack(overlay.getAlbumArt(), 500);
		overlay.setBackgroundColor(DEFAULT_CLOUD_COLOR);
	}

	/**
	 * In the action bar we set a title.
	 */
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		actionMode = mode;
		mode.setTitle(R.string.all_songs);
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
