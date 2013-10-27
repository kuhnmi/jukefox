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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.PlaylistImporter.PlaylistInfo;

public class LoadPlaylistMenu extends JukefoxOverlayActivity {

	private static class LoadablePlaylist extends PlaylistInfo {

		private final boolean external;

		public LoadablePlaylist(String path, boolean external, Date dateModified, Date dateAdded) {
			super(path, getPlaylistName(path), dateModified, dateAdded);
			this.external = external;
		}

		public boolean isExternal() {
			return external;
		}

		private static String getPlaylistName(String path) {
			String name = new File(path).getName();
			int idx = name.lastIndexOf(".");
			// Log.v(TAG,name);
			if (idx < 0) {
				return name;
			}
			return name.substring(0, idx);
		}
	}

	private class LoadPlaylistAdapter extends BaseAdapter {

		private List<LoadablePlaylist> playlists;

		public LoadPlaylistAdapter(List<LoadablePlaylist> playlists) {
			this.playlists = playlists;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.playlist_list_item, parent, false);
			}
			LoadablePlaylist playlist = getPlaylist(position);
			TextView name = (TextView) v.findViewById(R.id.playlist_name);
			TextView path = (TextView) v.findViewById(R.id.playlist_path);
			TextView external = (TextView) v.findViewById(R.id.external_label);
			if (playlist == null) {
				// should never happen!
				name.setText(R.string.error);
				return v;
			}

			name.setText(playlist.getName());
			path.setText(playlist.getPath());
			if (playlist.isExternal()) {
				external.setText(R.string.external);
			} else {
				external.setText("");
			}
			return v;
		}

		@Override
		public int getCount() {
			return playlists.size();
		}

		@Override
		public Object getItem(int position) {
			return getPlaylist(position);
		}

		public LoadablePlaylist getPlaylist(int position) {
			return playlists.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

	}

	private LoadPlaylistMenuEventListener eventListener;
	private ListView playlistList;
	private LoadPlaylistAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.loadplaylistmenu);

		playlistList = (ListView) findViewById(R.id.loadPlaylistNameList);
		eventListener = controller.createLoadPlaylistMenuEventListener(this);

		registerButtonListeners();

	}

	private List<LoadablePlaylist> getInternalPlaylists() {
		File topDirectory = JukefoxApplication.getDirectoryManager().getPlaylistDirectory(
				AndroidConstants.PLAYER_MODEL_NAME);
		File[] files = topDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".m3u") || filename.endsWith(".txt");
			}
		});
		if (files == null) {
			return new ArrayList<LoadablePlaylist>(0);
		}
		List<LoadablePlaylist> playlists = new ArrayList<LoadablePlaylist>(files.length);
		for (File file : files) {
			playlists.add(new LoadablePlaylist(file.getAbsolutePath(), false, null, null));
		}
		return playlists;
	}

	private void showNoPlaylistsMessage() {
		TextView title = (TextView) findViewById(R.id.loadPlaylistTitle);
		title.setText(R.string.no_playlists_available);
	}

	private void registerButtonListeners() {
		playlistList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				LoadablePlaylist playlist = adapter.getPlaylist(position);

				if (playlist.isExternal()) {
					eventListener.onLoadExternalPlaylistClicked(playlist);
				} else {
					eventListener.onLoadInternalPlaylistClicked(playlist.getName());
				}
			}
		});
		playlistList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
				LoadablePlaylist playlist = adapter.getPlaylist(position);
				if (playlist.isExternal()) {
					// ignore long click for external playlists
					return true;
				}
				return eventListener.onListItemLongClicked(playlist.getPath());
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<LoadablePlaylist> playlists = getLoadablePlaylists();
		if (playlists.size() == 0) {
			showNoPlaylistsMessage();
		} else {
			setAdapter(playlists);
		}
		// loadPlaylistList();
		// getPlaylistsFromMediaProvide
	}

	private void setAdapter(final List<LoadablePlaylist> playlists) {
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				adapter = new LoadPlaylistAdapter(playlists);
				playlistList.setAdapter(adapter);
			}
		});
	}

	private List<LoadablePlaylist> getLoadablePlaylists() {
		List<LoadablePlaylist> playlists = getInternalPlaylists();
		List<LoadablePlaylist> external = getExternalPlaylists();

		// avoid duplicates
		HashSet<String> internalPaths = getInternalPaths(playlists);
		for (LoadablePlaylist playlist : external) {
			if (!internalPaths.contains(playlist.getPath())) {
				playlists.add(playlist);
			}
		}
		return playlists;
	}

	private HashSet<String> getInternalPaths(List<LoadablePlaylist> playlists) {
		HashSet<String> internalPaths = new HashSet<String>();
		for (LoadablePlaylist playlist : playlists) {
			internalPaths.add(playlist.getPath());
		}
		return internalPaths;
	}

	private List<LoadablePlaylist> getExternalPlaylists() {
		List<PlaylistInfo> tmp = collectionModel.getPlaylistImporter().getPlaylists();
		List<LoadablePlaylist> playlists = new ArrayList<LoadPlaylistMenu.LoadablePlaylist>(tmp.size());
		for (PlaylistInfo info : tmp) {

			playlists.add(new LoadablePlaylist(info.getPath(), true, info.getDateModified(), info.getDateAdded()));
		}
		return playlists;
	}

	// private void getPlaylistsFromMediaProvider(Uri uri) {
	//
	// String[] projection = new String[] {
	// android.provider.MediaStore.Audio.PlaylistsColumns.NAME};
	// Cursor cur = null;
	// try {
	//
	// Log.v(TAG, "uri: " + uri);
	// ContentResolver cr = application.getContentResolver();
	// cur = cr.query(uri,
	// projection, null, null, null);
	// Log.v(TAG, "cur == null: " + (cur == null));
	// int numPlaylists = cur.getCount();
	// Log.v(TAG, "Total number of songs on device: " + numPlaylists);
	// while (cur.moveToNext()) {;
	// // String artist = cur.getString(1);
	// // String album = cur.getString(0);
	// // String path = cur.getString(2);
	// // if (album == null) {
	// // continue;
	// // }
	// }
	// } finally {
	// if (cur != null) {
	// cur.close();
	// }
	// }
	//
	// }

}
