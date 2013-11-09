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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.ImportedPlaylist;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ImportPlaylistEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.PlaylistImporter.PlaylistInfo;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

public class ImportPlaylistActivity extends JukefoxActivity {

	private final static String TAG = ImportPlaylistActivity.class.getSimpleName();

	private final static int REQUEST_CODE = 0;

	private ImportPlaylistEventListener eventHandler;
	private ProgressDialog loadingDialog;

	private ListView listView;
	private List<PlaylistInfo> playlists;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.importplaylistactivity);
		eventHandler = controller.createImportPlaylistEventListener();
		loadingDialog = new ProgressDialog(this);
		listView = (ListView) findViewById(R.id.playlist_list);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				PlaylistInfo info = playlists.get(position);
				StringBuilder msg = new StringBuilder();
				try {
					ImportedPlaylist playlist = eventHandler.importPlaylist(info);
					if (playlist.isPlaylistEmpty() && playlist.getErrorCount() == 0) {
						msg.append(getString(R.string.imported_playlist_empty));
					} else if (playlist.getErrorCount() > 0) {
						msg.append(getString(R.string.errors_occurred_during_playlist_import));
						appendProblemString(playlist, msg);
					} else {
						msg.append(getString(R.string.successfully_imported_playlist));
					}
					// importPlaylist(path);
				} catch (Exception e) {
					Log.w(TAG, e);
					msg.append(getString(R.string.could_not_import_playlist) + "\nError: " + e.getMessage());
				}
				eventHandler.showAlertDialog(ImportPlaylistActivity.this, REQUEST_CODE, msg.toString());
			}
		});

		setAdapter();
	}

	private void setAdapter() {
		loadingDialog = ProgressDialog.show(this, "Searching for playlists", "searching");

		new JoinableThread(new Runnable() {

			@Override
			public void run() {
				final ArrayAdapter<PlaylistInfo> adapter = createAdapter();
				JukefoxApplication.getHandler().post(new Runnable() {

					@Override
					public void run() {
						listView.setAdapter(adapter);
						loadingDialog.dismiss();
					}
				});
			}
		}).start();
	}

	private ArrayAdapter<PlaylistInfo> createAdapter() {
		playlists = collectionModel.getPlaylistImporter().getPlaylists();
		// files = searchPlaylists();
		Log.v(TAG, "number of playlists found: " + playlists.size());
		return new ArrayAdapter<PlaylistInfo>(this, android.R.layout.simple_list_item_1, playlists);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != REQUEST_CODE) {
			return;
		}
		finish();
	}

	private void appendProblemString(ImportedPlaylist playlist, StringBuilder msg) {
		msg.append("\n" + getString(R.string.problems) + ":");
		msg.append("Urls: " + playlist.getUrlCnt() + "\n");
		msg.append("Windows paths: " + playlist.getWindowsPathCnt() + "\n");
		msg.append("Included playlists: " + playlist.getEmbeddedPlaylistCnt() + "\n");
		msg.append("Invalid song paths: " + playlist.getInvalidSongPathCnt() + "\n");
		msg.append("Unrecognized lines: " + playlist.getUnrecognizedLineCnt() + "\n");
	}

}
