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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListMenuEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;
import ch.ethz.dcg.pancho3.view.overlays.JukefoxOverlayActivity;

public class GenreListMenu extends JukefoxOverlayActivity {

	private GenreListMenuEventListener eventListener;
	private Genre genre;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.genrelistmenu);

		eventListener = controller.createGenreListMenuEventListener(this);
		registerEventListener();

		setTitle();
	}

	private void setTitle() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			finish();
			return;
		}
		genre = ((ParcelableGenre) extras.get(Controller.INTENT_EXTRA_BASE_GENRE)).getGenre();
		if (genre == null) {
			finish();
			return;
		}
		((TextView) findViewById(R.id.genreListMenuTitle)).setText(genre.getName());
	}

	private void registerEventListener() {
		findViewById(R.id.playGenreButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onPlayGenreButtonClicked(genre);
			}
		});
		findViewById(R.id.showGenreArtistsButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onShowGenreArtistsClicked(genre);
			}
		});
		findViewById(R.id.showGenreAlbumsButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onShowGenreAlbumsClicked(genre);
			}
		});
		findViewById(R.id.showGenreSongsButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onShowGenreSongsClicked(genre);
			}
		});
	}
}
