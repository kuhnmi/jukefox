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

import java.util.Collections;
import java.util.List;

import android.database.Cursor;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.tablet.I18nManager;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongCursorAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongListAdapter;

public class SearchOverlayPresenter extends AbstractOverlayPresenter
		implements OnQueryTextListener {

	private final SongCursorAdapter adapter;
	private final DataFetcher dataFetcher;
	private SearchView searchView;
	private String queryText = "";
	private List<BaseSong<BaseArtist, BaseAlbum>> displayedSongsCache;
	private List<BaseSong<BaseArtist, BaseAlbum>> songListCache;
	private SongListAdapter songListAdapter;

	private Menu menu;

	public SearchOverlayPresenter(TabletPresenter tabletPresenter,
			MagicPlayMode magicPlaylistController, IOverlayView overlay,
			ViewFactory viewFactory, SongCursorAdapter adapter,
			SongListAdapter songListAdapter, DataFetcher dataFetcher, I18nManager i18nManager) {
		super(tabletPresenter, magicPlaylistController, overlay, viewFactory, i18nManager);
		this.adapter = adapter;
		this.dataFetcher = dataFetcher;
		this.songListAdapter = songListAdapter;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		actionMode = mode;
		searchView = viewFactory.createSearchView(this);
		mode.setCustomView(searchView);
		this.menu = menu;
		return true;
	}

	@Override
	public void viewFinishedInit() {
		super.viewFinishedInit();
		setHeaderItemTitle(0);
		new Handler().post(new Runnable() {

			@Override
			public void run() {
				searchView.requestFocus();
			}
		});
		overlay.show(this);
		overlay.setBackgroundColor(DEFAULT_CLOUD_COLOR);
	}

	@Override
	public synchronized boolean onQueryTextChange(String newText) {
		if (!newText.equals(queryText)) {
			uncheckAllSongs();
			queryText = newText;
			if (queryText.length() > 0) {
				dataFetcher.fetchQueriedSongs(newText,
						new OnDataFetchedListener<Pair<String, Cursor>>() {

							@Override
							public void onDataFetched(Pair<String, Cursor> data) {
								if (data.first.equals(queryText)) {
									setHeaderItemTitle(data.second.getCount());
									synchronized (this) {
										clearSongCaches();
										adapter.changeCursor(data.second);
									}
								}
							}
						});
			} else {
				setHeaderItemTitle(0);
				synchronized (this) {
					clearSongCaches();
					adapter.changeCursor(null);
				}
			}
		}
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	private void setHeaderItemTitle(int count) {
		overlay.initializeSongSeekbar(count);
		overlay.setHeaderItemTitle(i18nManager.getNumberOfSongsText(count));
	}

	@Override
	protected synchronized List<BaseSong<BaseArtist, BaseAlbum>> getDisplayedSongs() {
		if (displayedSongsCache == null) {
			displayedSongsCache = adapter.getSongList();
		}
		return displayedSongsCache;
	}

	@Override
	protected void displaySongs(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		displayedSongsCache = songs;
		songListAdapter.clear();
		songListAdapter.addAll(songs);
		overlay.setAdapter(songListAdapter);
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

	@Override
	protected List<BaseSong<BaseArtist, BaseAlbum>> selectRandomSongs(int number) {
		if (songListCache == null) {
			songListCache = adapter.getSongList();
		}
		Collections.shuffle(songListCache);
		return songListCache.subList(0, number);
	}

	private void clearSongCaches() {
		overlay.setAdapter(adapter);
		songListCache = null;
		displayedSongsCache = null;
	}
}
