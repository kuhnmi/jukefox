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

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SavePlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class SavePlaylistMenu extends JukefoxOverlayActivity {

	private SavePlaylistMenuEventListener eventListener;
	private EditText playlistName;
	private ListView playlistList;
	private String[] playlistNames;
	private String[] playlistPaths;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.saveplaylistmenu);

		playlistName = (EditText) findViewById(R.id.savePlaylistName);
		playlistList = (ListView) findViewById(R.id.savePlaylistNameList);
		eventListener = controller.createSavePlaylistMenuEventListener(this);

		registerButtonListeners();

	}

	private void loadPlaylistList() {
		File topDirectory = JukefoxApplication.getDirectoryManager().getPlaylistDirectory(
				AndroidConstants.PLAYER_MODEL_NAME);
		File[] files = topDirectory.listFiles();
		int numPlaylists = 0;
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory() && files[i].getName().endsWith(".m3u")) {
					numPlaylists++;
				}
			}
			playlistNames = new String[numPlaylists];
			playlistPaths = new String[numPlaylists];
			int u = 0;
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory() && files[i].getName().endsWith(".m3u")) {
					String tempName = files[i].getName();
					playlistNames[u] = tempName.substring(0, tempName.length() - 4);
					playlistPaths[u] = files[i].getAbsolutePath();
					u++;
				}
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
					playlistNames);
			playlistList.setAdapter(adapter);
		}
		playlistName.setText(getString(R.string.playlist) + (numPlaylists + 1));
	}

	private void registerButtonListeners() {
		playlistList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				eventListener.onListItemClicked(playlistNames[position]);
			}
		});
		playlistList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				return eventListener.onListItemLongClicked(playlistPaths[position]);
			}

		});
		findViewById(R.id.savePlaylistButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String name = playlistName.getText().toString();
				if (name.trim().equals("")) {
					playlistName.setText(getString(R.string.playlist) + playlistNames.length + 1);
				}
				eventListener.onSavePlaylistButtonClicked(name);
			}
		});
		playlistName.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					// Hide the virtual keyboard
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(playlistName.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				return false;
			}
		});
	}

	public void setEditText(String text) {
		playlistName.setText(text);
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadPlaylistList();
	}

}
