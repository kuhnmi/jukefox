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

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.StandardDialog;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.IVideoButtonCallback;
import ch.ethz.dcg.pancho3.view.youtube.Query;
import ch.ethz.dcg.pancho3.view.youtube.SAXParser;
import ch.ethz.dcg.pancho3.view.youtube.YoutubePlayer;

public class SongListEventListener extends BaseJukefoxEventListener implements IVideoButtonCallback {

	public SongListEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);

	}

	public void onItemClick(BaseSong<BaseArtist, BaseAlbum> song) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, SongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);
	}

	// public void onVideoClick(BaseSong song) {
	// controller.doHapticFeedback();
	// youTubePlayer(song);
	// Intent intent = new Intent(activity, YoutubePlayerActivity.class);
	// intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, song);
	// Log.v(TAG, "Intent created for "+ song.getArtist().getName()+
	// " and "+song.getName());
	// activity.startActivity(intent);
	//
	// }

	@Override
	public void onVideoButtonClicked(int songId) {
		Log.v(TAG, "Video button clicked");
		controller.doHapticFeedback();
		BaseSong<BaseArtist, BaseAlbum> song;
		try {
			song = activity.getCollectionModel().getSongProvider().getBaseSong(new SongCoords(songId, null));
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return;
		}

		youTubePlayer(song);

	}

	public void youTubePlayer(BaseSong<BaseArtist, BaseAlbum> song) {
		SAXParser pars = new SAXParser();
		Query q = new Query(song.getArtist().getName(), song.getName());

		try {
			String res = q.getVideosFromYouTubeServer();
			//
			Log.v(TAG, "And the result is: " + res);

			List<String> videoIds = pars.getParseResult(res);

			if (videoIds.size() == 0) {
				Intent intent4 = new Intent(activity, StandardDialog.class);
				intent4.putExtra(StandardDialog.DIALOG_MSG, activity.getString(R.string.not_available_videos));
				// intent4.putExtra(DontShowAgainDialog.SHARED_PREF_KEY,
				// activity.getString(R.string.not_available_videos));
				intent4.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivity(intent4);
			} else {
				YoutubePlayer p = new YoutubePlayer(videoIds.get(0));
				String url = p.getVideoUrl();
				Log.v(TAG, "the obtained url is " + url);
				Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				activity.startActivity(intent2);
			}
		} catch (Exception e) {
			Intent intent3 = new Intent(activity, StandardDialog.class);
			intent3.putExtra(StandardDialog.DIALOG_MSG, activity.getString(R.string.connection_error_message));
			// intent3.putExtra(DontShowAgainDialog.SHARED_PREF_KEY,
			// activity.getString(R.string.connection_error_message));
			intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent3);
			Log.w(TAG, e);
		}

	}

}
