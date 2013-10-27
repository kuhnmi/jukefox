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
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SongListEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;

public class SongList extends ListActivity {

	private static final String TAG = SongList.class.getSimpleName();
	SongListEventListener eventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		eventListener = controller.createSongListEventListener(this);

		processIntent();

		registerEventListeners();

	}

	private void processIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		if (applicationState.isImporting() && !applicationState.isBaseDataCommitted()) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		}

		if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_ARTIST)) {
			ParcelableArtist pArtist = extras.getParcelable(Controller.INTENT_EXTRA_BASE_ARTIST);
			BaseArtist artist = pArtist.getBaseArtist();
			loadArtistSongsList(artist);
		} else if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_GENRE)) {
			ParcelableGenre pGenre = extras.getParcelable(Controller.INTENT_EXTRA_BASE_GENRE);
			Genre genre = pGenre.getGenre();
			loadGenreSongsList(genre);
		} else {
			loadSongsList();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadGenreSongsList(Genre genre) {
		List<BaseSong<BaseArtist, BaseAlbum>> songList = songProvider.getAllBaseSongs(genre);
		VideoSectionAdapter adapter = new VideoSectionAdapter(this, R.layout.videolistitem, songList, eventListener,
				getSettings().isIgnoreLeadingThe());
		setList(getString(R.string.songs), adapter);
	}

	@SuppressWarnings("unchecked")
	private void loadArtistSongsList(BaseArtist artist) {
		List<BaseSong<BaseArtist, BaseAlbum>> songList = songProvider.getAllBaseSongs(artist);
		VideoSectionAdapter adapter = new VideoSectionAdapter(this, R.layout.videolistitem, songList, eventListener,
				getSettings().isIgnoreLeadingThe());
		setList(getString(R.string.songs), adapter);
	}

	private void registerEventListeners() {
		list.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Object o = list.getItemAtPosition(position);
				if (o instanceof BaseSong<?, ?>) {
					BaseSong<BaseArtist, BaseAlbum> song = (BaseSong<BaseArtist, BaseAlbum>) o;
					eventListener.onItemClick(song);
				}
			}
			// }
		});
		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.v(TAG, "long click on item");
				return false;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void loadSongsList() {
		List<BaseSong<BaseArtist, BaseAlbum>> songList = songProvider.getAllBaseSongs();
		Log.v(TAG, "loadSongsList()");
		VideoSectionAdapter adapter = new VideoSectionAdapter(this, R.layout.videolistitem, songList, eventListener,
				getSettings().isIgnoreLeadingThe());
		setList(getString(R.string.songs), adapter);
	}
}
