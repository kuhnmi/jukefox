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
package ch.ethz.dcg.pancho3.view.tabs.lists;

import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;

public class AlbumList extends ListActivity {

	private final static String TAG = AlbumList.class.getSimpleName();

	private AlbumListEventListener eventListener;

	private HashMap<String, Integer> albumNameCnts;
	private int maxAlbumNameCnt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		eventListener = controller.createAlbumListEventListener(this);
		processIntent();

		registerEventListener();
	}

	public HashMap<String, Integer> getAlbumNameCnts() {
		Log.v(TAG, "getAlbumNameCnts(): albumNameCnts == null: " + (albumNameCnts == null));
		return albumNameCnts;
	}

	private void processIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (applicationState.isImporting() && !applicationState.isBaseDataCommitted()) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		} else if (applicationState.isImporting() && !applicationState.isCoversFetched() && settings.isUseIconLists()) {
			showStatusInfo(getString(R.string.covers_not_yet_fetched));
		}
		if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_ARTIST)) {
			ParcelableArtist pArtist = extras.getParcelable(Controller.INTENT_EXTRA_BASE_ARTIST);
			BaseArtist artist = pArtist.getBaseArtist();
			try {
				List<Pair<Genre, Integer>> genres = genreProvider.getGenresForArtist(artist);
				for (Pair<Genre, Integer> genre : genres) {
					Log.v(TAG, genre.first.getName() + ": " + genre.second);
				}
			} catch (DataUnavailableException e) {
				// TODO Auto-generated catch block
				Log.w(TAG, e);
			}
			loadArtistAlbumsList(artist);
		} else if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_GENRE)) {
			ParcelableGenre pGenre = extras.getParcelable(Controller.INTENT_EXTRA_BASE_GENRE);
			Genre genre = pGenre.getGenre();
			loadGenreAlbumList(genre);
		} else {
			loadAlbumsList();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadGenreAlbumList(Genre genre) {
		List<ListAlbum> albumList = albumProvider.getAllListAlbums(genre);
		if (settings.isUseIconLists()) {
			IconSectionAdapter adapter = new IconSectionAdapter(this, R.layout.iconlistitem, albumList, collectionModel);
			setList(getString(R.string.albums), adapter);
		} else {
			TextSectionAdapter<IBaseListItem> adapter = new TextSectionAdapter(this, R.layout.textlistitem, albumList,
					getSettings().isIgnoreLeadingThe());
			setList(getString(R.string.albums), adapter);
		}
		countAlbumNames(albumList);
		if (maxAlbumNameCnt > 2) {
			Log.w(TAG, "same album name appears more than twice...");
			eventListener.onContainsAlbumNameMoreThanTwice();
		}
	}

	private void registerEventListener() {
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				eventListener.onListItemClicked(arg0, arg1, arg2);
			}
		});

		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				return eventListener.onListItemLongClicked(parent, view, pos, id);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void loadArtistAlbumsList(BaseArtist artist) {
		// Log.v(TAG, "loadArtistAlbumsList() 1 " + System.currentTimeMillis());
		List<ListAlbum> albumList = albumProvider.getAllListAlbums(artist);
		// Log.v(TAG, "loadArtistAlbumsList() 2 " + System.currentTimeMillis());
		if (settings.isUseIconLists()) {
			IconSectionAdapter adapter = new IconSectionAdapter(this, R.layout.iconlistitem, albumList, collectionModel);
			setList(getString(R.string.albums), adapter);
		} else {
			TextSectionAdapter<IBaseListItem> adapter = new TextSectionAdapter(this, R.layout.textlistitem, albumList,
					getSettings().isIgnoreLeadingThe());
			setList(getString(R.string.albums), adapter);
		}
		countAlbumNames(albumList);
		if (maxAlbumNameCnt > 2) {
			Log.w(TAG, "same album name appears more than twice...");
			eventListener.onContainsAlbumNameMoreThanTwice();
		}
	}

	private void loadAlbumsList() {
		List<ListAlbum> albumList;
		albumList = albumProvider.getAllListAlbums();
		if (settings.isUseIconLists()) {
			IconSectionAdapter adapter = new IconSectionAdapter(this, R.layout.iconlistitem, albumList, collectionModel);
			setList(getString(R.string.albums), adapter);
		} else {
			TextSectionAdapter<ListAlbum> adapter = new TextSectionAdapter<ListAlbum>(this, R.layout.textlistitem,
					albumList, getSettings().isIgnoreLeadingThe());
			setList(getString(R.string.albums), adapter);
		}
		eventListener.afterListLoaded(list);
		countAlbumNames(albumList);
		if (maxAlbumNameCnt > 2) {
			Log.w(TAG, "same album name appears more than twice...");
			eventListener.onContainsAlbumNameMoreThanTwice();
		}
	}

	private void countAlbumNames(List<ListAlbum> albumList) {
		Log.v(TAG, "checking for multiple album names...");
		albumNameCnts = new HashMap<String, Integer>();
		maxAlbumNameCnt = -1;
		for (ListAlbum album : albumList) {
			if (album.getName().equals(JukefoxApplication.unknownAlbumAlias)) {
				continue;
			}
			Integer cnt = albumNameCnts.get(album.getName());
			if (cnt == null) {
				cnt = 1;
			} else {
				cnt++;
			}
			albumNameCnts.put(album.getName(), cnt);
			if (cnt > maxAlbumNameCnt) {
				maxAlbumNameCnt = cnt;
			}
		}
	}

	@Override
	protected void onDestroy() {
		eventListener.beforeActivityFinishes(list.getFirstVisiblePosition());
		super.onDestroy();
	}

}
