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
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.overlays.TagPlaylistGenerationActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;

public class TagPlaylistGenerationEventListener extends BaseJukefoxEventListener {

	public static final String TAG = TagPlaylistGenerationEventListener.class.getSimpleName();
	private TagPlaylistGenerationActivity activity;
	private int loadingID = 0;

	public TagPlaylistGenerationEventListener(Controller controller, TagPlaylistGenerationActivity activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onPlayButtonClicked() {
		controller.doHapticFeedback();
		Log.v(TAG, "playButton Clicked");
		List<PlaylistSong<BaseArtist, BaseAlbum>> playlist = activity.getCurrentPlaylist();
		controller.getPlayerController().stop();
		controller.getPlayerController().clearPlaylist();
		int artistAvoidance = AndroidSettingsManager.getAndroidSettingsReader().getSimilarArtistAvoidanceNumber();
		controller.getPlayerController().setPlayMode(PlayModeType.SIMILAR, artistAvoidance,
				Constants.SAME_SONG_AVOIDANCE_NUM);
		Log.v(TAG, "playlist size: " + playlist.size());
		for (PlaylistSong<BaseArtist, BaseAlbum> song : playlist) {
			controller.getPlayerController().appendSongAtEnd(song);
			Log.v(TAG, "added song");
		}
		try {
			controller.getPlayerController().playSongAtPosition(0);
		} catch (PlaylistPositionOutOfRangeException e) {
			// TODO Auto-generated catch block
			Log.w(TAG, e);
		}
		controller.startActivity(activity, PlayerActivity.class);
		activity.finish();
	}

	public void onRegenerateButtonClicked() {
		controller.doHapticFeedback();
		activity.generatePlaylist();
	}

	public void startLoadingPlaylist() {
		loadingID = controller.setLoading(activity, activity.getString(R.string.loading_playlist));
	}

	public void endLoadingPlaylist() {
		controller.finishLoading(loadingID);
	}

	public void onListItemClicked(int position) {
		controller.doHapticFeedback();
		Log.v(TAG, "playButton Clicked");
		List<PlaylistSong<BaseArtist, BaseAlbum>> playlist = activity.getCurrentPlaylist();
		PlaylistSong<BaseArtist, BaseAlbum> song = playlist.get(position);
		Intent intent = new Intent(activity, SongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);
	}
}
