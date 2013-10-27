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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.DeleteSongMenuEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;

public class DeleteSongMenu extends JukefoxOverlayActivity {

	private static final String TAG = DeleteSongMenu.class.getSimpleName();
	private DeleteSongMenuEventListener eventListener;
	private BaseSong<BaseArtist, BaseAlbum> song;
	private int positionInPlaylist;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.deletesongmenu);

		eventListener = controller.createDeleteSongMenuEventListener(this);

		getSongFromIntent();
		if (song == null) {
			Log.w(TAG, "SeleteSongMenu called without song in intent extra");
			finish();
		}

		registerButtonListeners();
	}

	private void getSongFromIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		positionInPlaylist = -1;
		if (extras.containsKey(Controller.INTENT_EXTRA_SONG_PLAYLIST_POSITION)) {
			positionInPlaylist = extras.getInt(Controller.INTENT_EXTRA_SONG_PLAYLIST_POSITION);
		}
		ParcelableSong pSong = extras.getParcelable(Controller.INTENT_EXTRA_BASE_SONG);
		song = pSong.getBaseSong();
		if (song == null) {
			finish();
			return;
		}
		TextView songTitle = (TextView) findViewById(R.id.deleteSongSongTitle);
		songTitle.setText(song.getArtist().getName() + " - " + song.getName());
	}

	private void registerButtonListeners() {
		findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onCancelButtonClicked();
			}
		});
		findViewById(R.id.ignoreSongButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onIgnoreSongButtonClicked();
			}
		});
		findViewById(R.id.deleteSongButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onDeleteSongButtonClicked();
			}
		});
	}

	public BaseSong<BaseArtist, BaseAlbum> getSong() {
		return song;
	}

	public int getPositionInPlaylist() {
		return positionInPlaylist;
	}
}
