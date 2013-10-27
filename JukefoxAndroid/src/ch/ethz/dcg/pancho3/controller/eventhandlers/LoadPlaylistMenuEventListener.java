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
package ch.ethz.dcg.pancho3.controller.eventhandlers;

import java.io.IOException;

import android.content.Intent;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.PlaylistImporter.PlaylistInfo;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistContextMenu;

public class LoadPlaylistMenuEventListener extends BaseJukefoxEventListener {

	private final static String TAG = LoadPlaylistMenuEventListener.class.getSimpleName();

	public LoadPlaylistMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	public void onLoadInternalPlaylistClicked(String playlistName) {
		controller.doHapticFeedback();
		controller.getPlayerController().stop();
		Playlist playlist;
		try {
			playlist = JukefoxApplication.getPlayerModel().getPlaylistManager().loadPlaylistFromFileByName(
					playlistName);
			controller.getPlayerController().setPlaylist(playlist);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		activity.finish();
	}

	public void onLoadExternalPlaylistClicked(final PlaylistInfo playlistInfo) {
		controller.doHapticFeedback();
		new JoinableThread(new Runnable() {

			@Override
			public void run() {
				try {
					controller.getPlayerController().stop();
					Log.v(TAG, "importing playlist...");
					controller.importPlaylist(playlistInfo);
					Log.v(TAG, "playlist imported.");
				} catch (IOException e) {
					Log.w(TAG, e);
				}
			}
		}).start();
		activity.finish();
	}

	public boolean onListItemLongClicked(String path) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, PlaylistContextMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_PATH, path);
		activity.startActivity(intent);
		return true;
		// activity.finish();
	}
}
