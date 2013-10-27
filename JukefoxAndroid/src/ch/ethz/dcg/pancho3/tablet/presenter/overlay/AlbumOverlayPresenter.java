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
 * If we overlay an album we display all its songs. Further, we also display a
 * fancy background using the album's color and display its album art as a
 * background of the center part of the overlay.
 */
public class AlbumOverlayPresenter extends AbstractOverlayPresenter {

	// The album about which we display information.
	private final ListAlbum album;
	// Used to fetch the album art and color.
	private final DataFetcher dataFetcher;

	private final SongListAdapter adapter;

	private final boolean needsMapNavigation;
	private final boolean needsExploreNavigation;

	/**
	 * The constructor needs some references and an album which will be
	 * displayed.
	 */
	public AlbumOverlayPresenter(TabletPresenter tabletPresenter,
			MagicPlayMode magicPlaylistController, DataFetcher dataFetcher,
			IOverlayView overlay, ViewFactory viewFactory, ListAlbum album,
			SongListAdapter adapter, boolean needsMapNavigation, boolean needsExploreNavigation,
			I18nManager i18nManager) {
		super(tabletPresenter, magicPlaylistController, overlay, viewFactory, i18nManager);
		this.dataFetcher = dataFetcher;
		this.album = album;
		this.adapter = adapter;
		this.needsMapNavigation = needsMapNavigation;
		this.needsExploreNavigation = needsExploreNavigation;
	}

	@Override
	public void viewFinishedInit() {
		super.viewFinishedInit();
		dataFetcher.fetchSongsOfAlbum(album, this);
		overlay.show(this);
		try {
			// TODO: maybe asynchronous....
			overlay.setAlbumArt(dataFetcher.getData().getAlbumArtProvider().getAlbumArt(album, false));
			overlay.setBackgroundColor(dataFetcher.getData().getAlbumProvider().getMapAlbum(album).getColor());
		} catch (Exception e) {
			overlay.setBackgroundColor(DEFAULT_CLOUD_COLOR);
		}
	}

	/**
	 * Creates the action mode: We set title, subtitle and inflate the menu.
	 */
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		actionMode = mode;
		mode.setTitle(album.getName());
		mode.setSubtitle(album.getFirstArtist().getName());
		if (needsExploreNavigation && needsMapNavigation) {
			showNavigationExploreMap(album.getFirstArtist(), album, menu);
		} else if (needsExploreNavigation) {
			showNavigationExplore(album.getFirstArtist(), menu);
		} else if (needsMapNavigation) {
			showNavigationMap(album, menu);
		}
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

	public Object getAlbum() {
		return album;
	}
}
