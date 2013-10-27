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

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ArtistListEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;

public class ArtistList extends ListActivity {

	public static final String TAG = ListActivity.class.getSimpleName();
	private ArtistListEventListener eventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		eventListener = controller.createArtistListEventListener(this);

		processIntent();

		registerEventListeners();

	}

	private void processIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (applicationState.isImporting() && !applicationState.isBaseDataCommitted()) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		}
		if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_GENRE)) {
			ParcelableGenre pGenre = extras.getParcelable(Controller.INTENT_EXTRA_BASE_GENRE);
			Genre genre = pGenre.getGenre();
			loadArtistForGenreList(genre);
		} else {
			loadArtistsList();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadArtistForGenreList(Genre genre) {
		List<BaseArtist> artistList = artistProvider.getAllArtists(genre);
		TextSectionAdapter adapter = new TextSectionAdapter(this, R.layout.textlistitem, artistList, getSettings()
				.isIgnoreLeadingThe());
		setList(getString(R.string.artists), adapter);
	}

	private void registerEventListeners() {
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				eventListener.onListItemClicked(arg0, arg1, arg2);
			}
		});
		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				return eventListener.onListItemLongClicked(arg0, arg1, arg2);
			}
		});
	}

	private void loadArtistsList() {
		List<BaseArtist> artistList = artistProvider.getAllArtists();
		TextSectionAdapter<BaseArtist> adapter = new TextSectionAdapter<BaseArtist>(this, R.layout.textlistitem,
				artistList, getSettings().isIgnoreLeadingThe());
		setList(getString(R.string.artists), adapter);
		eventListener.afterListLoaded(list);
	}

	@Override
	public void finish() {
		eventListener.beforeActivityFinishes(list.getFirstVisiblePosition());
		super.finish();
	}

}
