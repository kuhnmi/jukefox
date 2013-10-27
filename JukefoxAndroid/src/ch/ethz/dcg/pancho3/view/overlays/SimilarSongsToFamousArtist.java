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
package ch.ethz.dcg.pancho3.view.overlays;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarSongsToFamousArtistEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.view.tabs.lists.TextSectionAdapter;

public class SimilarSongsToFamousArtist extends JukefoxOverlayActivity {

	private SimilarSongsToFamousArtistEventListener eventListener;

	public static final String TAG = SimilarSongsToFamousArtist.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.similarsongstofamousartist);

		eventListener = controller.createSimilarSongsToFamousArtistEventListener(this);

		processIntent();

		registerListener();

	}

	private void registerListener() {
		final ListView list = (ListView) findViewById(R.id.similarSongsToArtistList);
		list.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Object o = list.getItemAtPosition(arg2);
				if (o instanceof BaseSong<?, ?>) {
					BaseSong<BaseArtist, BaseAlbum> song = (BaseSong<BaseArtist, BaseAlbum>) o;
					eventListener.onSongClicked(song);
				}
			}
		});
	}

	private void processIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			finish();
			return;
		}
		BaseArtist artist = ((ParcelableArtist) extras.get(Controller.INTENT_EXTRA_BASE_ARTIST)).getBaseArtist();
		loadSimilarSongForArtist(artist);
	}

	private void loadSimilarSongForArtist(BaseArtist artist) {
		try {
			CompleteArtist completeArtist = artistProvider.getCompleteArtist(artist);
			List<BaseSong<BaseArtist, BaseAlbum>> songs = songProvider.getClosestBaseSongsToPosition(completeArtist
					.getCoords(), 10);
			setList(songs, artist);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void setList(List<BaseSong<BaseArtist, BaseAlbum>> songs, BaseArtist artist) {
		TextView title = (TextView) findViewById(R.id.similarSongsToArtistTitle);
		title.setText(getString(R.string.songs_might_be_similar) + " " + artist.getName());
		ListView list = (ListView) findViewById(R.id.similarSongsToArtistList);
		TextSectionAdapter adapter = new TextSectionAdapter(this, R.layout.textlistitem, songs, getSettings()
				.isIgnoreLeadingThe());
		list.setAdapter(adapter);
	}

}
