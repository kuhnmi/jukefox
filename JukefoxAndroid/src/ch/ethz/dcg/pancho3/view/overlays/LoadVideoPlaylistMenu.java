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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadVideoPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class LoadVideoPlaylistMenu extends JukefoxOverlayActivity {

	private LoadVideoPlaylistMenuEventListener eventListener;
	private ListView playlistList;
	private String[] playlistNames;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.loadvideoplaylistmenu);

		playlistList = (ListView) findViewById(R.id.loadPlaylistNameList);
		eventListener = controller.createLoadVideoPlaylistMenuEventListener(this);

		loadPlaylistList();

		registerButtonListeners();

	}

	private void loadPlaylistList() {
		File topDirectory = JukefoxApplication.getDirectoryManager().getPlaylistDirectory(
				AndroidConstants.PLAYER_MODEL_NAME);
		File[] files = topDirectory.listFiles();

		if (files == null) {
			showNoPlaylistsMessage();
		} else {

			int numPlaylists = 0;
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory()
						&& (files[i].getName().endsWith(".m3u") || files[i].getName().endsWith(".txt"))) {
					numPlaylists++;
				}
			}
			playlistNames = new String[numPlaylists];
			int u = 0;
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory()
						&& (files[i].getName().endsWith(".m3u") || files[i].getName().endsWith(".txt"))) {
					String tempName = files[i].getName();
					playlistNames[u] = tempName.substring(0, tempName.length() - 4);
					u++;
				}
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
					playlistNames);
			playlistList.setAdapter(adapter);
		}
	}

	private void showNoPlaylistsMessage() {
		TextView title = (TextView) findViewById(R.id.loadPlaylistTitle);
		title.setText(R.string.no_playlists_available);
	}

	private void registerButtonListeners() {
		playlistList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				eventListener.onListItemClicked(playlistNames[position]);
			}
		});

	}
}
