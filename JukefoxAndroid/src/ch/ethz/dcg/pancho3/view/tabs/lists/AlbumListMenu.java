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
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListMenuEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.view.overlays.JukefoxOverlayActivity;

public class AlbumListMenu extends JukefoxOverlayActivity {

	private AlbumListMenuEventListener eventListener;
	private BaseAlbum album;
	private int numAlbumsWithThisName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.albumlistmenu);

		setAlbum();
		((TextView) findViewById(R.id.albumListMenuTitle)).setText(album.getName());

		Intent intent = getIntent();
		numAlbumsWithThisName = intent.getIntExtra(Controller.INTENT_EXTRA_NUMBER_OF_ALBUMS_WITH_THIS_NAME, -1);

		eventListener = controller.createAlbumListMenuEventListener(this);
		registerEventListener();

	}

	private void setAlbum() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			finish();
			return;
		}
		album = ((ParcelableAlbum) extras.get(Controller.INTENT_EXTRA_BASE_ALBUM)).getBaseAlbum();
		if (album == null) {
			finish();
			return;
		}
	}

	private void registerEventListener() {
		findViewById(R.id.goToAlbumButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onGoToAlbumButtonClicked(album);
			}
		});
		findViewById(R.id.groupAlbumButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onGroupAlbumButtonClicked(album, numAlbumsWithThisName);
			}
		});
		findViewById(R.id.ungroupAlbumButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onUngroupAlbumButtonClicked(album);
			}
		});

	}
}
