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
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.ContinueDialog;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.youtube.Query;
import ch.ethz.dcg.pancho3.view.youtube.SAXParser;

public class ArtistListMenuEventListener extends BaseJukefoxEventListener {

	public ArtistListMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);

	}

	public void onPlayArtistButtonClicked(BaseArtist artist) {
		controller.doHapticFeedback();
		controller.getPlayerController().stop();
		controller.getPlayerController().clearPlaylist();
		List<BaseSong<BaseArtist, BaseAlbum>> songs = activity.getCollectionModel().getSongProvider().getAllBaseSongs(
				artist);
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			controller.getPlayerController().appendSongAtEnd(
					new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED));
		}
		try {
			controller.getPlayerController().playSongAtPosition(0);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
		Intent intent = new Intent(activity.getApplicationContext(), PlayerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
		activity.finish();
	}

	public void onAppendArtistButtonClicked(BaseArtist artist) {
		controller.doHapticFeedback();
		List<BaseSong<BaseArtist, BaseAlbum>> songs = activity.getCollectionModel().getSongProvider().getAllBaseSongs(
				artist);
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			controller.getPlayerController().appendSongAtEnd(
					new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED));
		}
		activity.finish();
	}

	public void onInsertArtistButtonClicked(BaseArtist artist) {
		controller.doHapticFeedback();
		List<BaseSong<BaseArtist, BaseAlbum>> songs = activity.getCollectionModel().getSongProvider().getAllBaseSongs(
				artist);
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			controller.getPlayerController().appendSongAtEnd(
					new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED));
		}
		activity.finish();
	}

	public void onPlayArtistVideosButtonClicked(BaseArtist artist) {
		controller.doHapticFeedback();
		youTubePlayer(artist);
	}

	public void youTubePlayer(BaseArtist artist) {
		SAXParser pars = new SAXParser();
		Query q = new Query(artist.getName());

		try {
			String res = q.getVideosFromYouTubeServer();
			// Log.v(TAG,"And the result is: "+res);

			ArrayList<String> videoIds = pars.getParseResult(res);

			if (videoIds.size() == 0) {
				controller.showStandardDialog(activity.getString(R.string.not_available_videos));
				// Intent intent = new Intent(activity, StandardDialog.class);
				// intent.putExtra(StandardDialog.DIALOG_MSG,
				// activity.getString(R.string.not_available_videos));
				// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// activity.startActivity(intent);
			} else {
				Intent intent = new Intent(activity, ContinueDialog.class);
				intent.putStringArrayListExtra(ContinueDialog.VIDEO_URL, videoIds);
				activity.startActivity(intent);

			}
		} catch (Exception e) {
			Log.w(TAG, e);
			controller.showStandardDialog(activity.getString(R.string.connection_error_message));
			// Intent intent = new Intent(activity, StandardDialog.class);
			// intent.putExtra(StandardDialog.DIALOG_MSG,
			// activity.getString(R.string.connection_error_message));
			// // intent3.putExtra(DontShowAgainDialog.SHARED_PREF_KEY,
			// activity.getString(R.string.connection_error_message));
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// activity.startActivity(intent);
			// Log.w(TAG, e);
		}

	}

}
