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

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlaylistContextMenuEventListener;
import ch.ethz.dcg.pancho3.view.tabs.lists.TextListAdapter;

public class PlaylistContextMenu extends JukefoxOverlayActivity {

	private PlaylistContextMenuEventListener eventListener;
	private ListView playlist;
	private TextView title;
	private String playlistPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.playlistcontextmenu);

		eventListener = controller.createPlaylistContextMenuEventListener(this);

		registerListeners();

		processIntent();

	}

	private void processIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			finish();
			return;
		}
		playlistPath = extras.getString(Controller.INTENT_EXTRA_PATH);
		if (playlistPath == null) {
			finish();
			return;
		}
		File f = new File(playlistPath);
		String playlistName = f.getName();
		title.setText(playlistName);
		Playlist list;
		try {
			list = collectionModel.getPlaylistProvider().loadPlaylistFromFile(playlistPath);
			TextListAdapter<IBaseListItem> adapter = new TextListAdapter(PlaylistContextMenu.this,
					R.layout.textlistitem, list.getSongList());
			playlist.setAdapter(adapter);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	private void registerListeners() {
		playlist = (ListView) findViewById(R.id.playlist);
		title = (TextView) findViewById(R.id.playlistContextMenuTitle);
		findViewById(R.id.deletePlaylistButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onDeletePlaylistButtonClicked();
			}
		});
	}

	public String getPlaylistPath() {
		return playlistPath;
	}
}
