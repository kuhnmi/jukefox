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
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TitleSearchMenuEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;

public class SongMenu extends JukefoxOverlayActivity {

	public static final String TAG = SongMenu.class.getSimpleName();
	private BaseSong<BaseArtist, BaseAlbum> song;
	private TitleSearchMenuEventListener eventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.songmenu);

		eventListener = controller.createTitleSearchMenuEventListener(this);

		initializeView();

		registerButtons();

	}

	private void registerButtons() {
		LinearLayout playButton = (LinearLayout) findViewById(R.id.playSongButton);
		playButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onPlayButtonClicked();
			}
		});
		LinearLayout appendButton = (LinearLayout) findViewById(R.id.appendSongButton);
		appendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onAppendButtonClicked();
			}
		});
		LinearLayout insertButton = (LinearLayout) findViewById(R.id.insertSongButton);
		insertButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onInsertButtonClicked();
			}
		});
		LinearLayout showAlbumButton = (LinearLayout) findViewById(R.id.showAlbumButton);
		showAlbumButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onShowAlbumButtonClicked();
			}
		});
		LinearLayout showArtistButton = (LinearLayout) findViewById(R.id.showArtistButton);
		showArtistButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onShowArtistButtonClicked();
			}
		});
		LinearLayout deleteSongButton = (LinearLayout) findViewById(R.id.deleteSongButton);
		deleteSongButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onDeleteSongButtonClicked();
			}
		});
	}

	private void initializeView() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null && extras.containsKey(Controller.INTENT_EXTRA_BASE_SONG)) {
			ParcelableSong pSong = extras.getParcelable(Controller.INTENT_EXTRA_BASE_SONG);
			song = pSong.getBaseSong();
		} else {
			finish();
		}
		TextView title = (TextView) findViewById(R.id.titleSearchResultMenuTitle);
		title.setText(song.getName());
	}

	public PlaylistSong<BaseArtist, BaseAlbum> getSong() {
		return new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED);
	}

}
