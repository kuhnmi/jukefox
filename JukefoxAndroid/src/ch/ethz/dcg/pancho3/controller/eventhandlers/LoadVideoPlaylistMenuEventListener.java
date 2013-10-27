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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.ContinueDialog;
import ch.ethz.dcg.pancho3.view.dialogs.StandardDialog;
import ch.ethz.dcg.pancho3.view.youtube.Query;
import ch.ethz.dcg.pancho3.view.youtube.SAXParser;

public class LoadVideoPlaylistMenuEventListener extends BaseJukefoxEventListener {

	public LoadVideoPlaylistMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	public void onListItemClicked(String playlistName) {
		controller.doHapticFeedback();
		Playlist playlist;
		try {
			playlist = JukefoxApplication.getPlayerModel().getPlaylistManager().loadPlaylistFromFileByName(
					playlistName);
		} catch (DataUnavailableException e1) {
			Log.w(TAG, e1);
			return;
		}
		List<PlaylistSong<BaseArtist, BaseAlbum>> l = playlist.getSongList();

		if (l.size() != 0) {
			ArrayList<String> video_urls = new ArrayList<String>();
			SAXParser pars = new SAXParser();

			try {
				for (BaseSong<BaseArtist, BaseAlbum> bs : l) {
					Query q = new Query(bs.getArtist().getName(), bs.getName());
					String res = q.getVideosFromYouTubeServer();
					ArrayList<String> videoIds = pars.getParseResult(res);
					if (videoIds.size() != 0) {
						video_urls.add(videoIds.get(0));
					}
				}

				Intent intent5 = new Intent(activity, ContinueDialog.class);
				intent5.putStringArrayListExtra(ContinueDialog.VIDEO_URL, video_urls);
				activity.startActivity(intent5);
			} catch (Exception e) {
				Intent intent3 = new Intent(activity, StandardDialog.class);
				intent3.putExtra(StandardDialog.DIALOG_MSG, activity.getString(R.string.connection_error_message));
				intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivity(intent3);
				Log.w(TAG, e);
			}
		} else {
			Intent intent4 = new Intent(activity, StandardDialog.class);
			intent4.putExtra(StandardDialog.DIALOG_MSG, activity.getString(R.string.empty_playlist));
			intent4.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent4);
		}
		activity.finish();
	}
}
