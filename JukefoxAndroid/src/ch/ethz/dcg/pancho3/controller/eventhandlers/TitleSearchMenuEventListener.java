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

import android.content.Intent;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.overlays.AlbumDetails;
import ch.ethz.dcg.pancho3.view.overlays.DeleteSongMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;

public class TitleSearchMenuEventListener extends BaseJukefoxEventListener {

	public static final String TAG = TitleSearchMenuEventListener.class.getSimpleName();
	SongMenu activity;

	public TitleSearchMenuEventListener(Controller controller, SongMenu activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onAppendButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().appendSongAtEnd(activity.getSong());
		activity.finish();
	}

	public void onInsertButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().insertSongAsNext(activity.getSong());
		activity.finish();
	}

	public void onShowAlbumButtonClicked() {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, AlbumDetails.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(activity.getSong().getAlbum()));
		activity.startActivity(intent);
		activity.finish();
	}

	public void onShowArtistButtonClicked() {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, AlbumList.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(activity.getSong().getArtist()));
		activity.startActivity(intent);
		activity.finish();
	}

	public void onPlayButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().stop();
		controller.getPlayerController().clearPlaylist();
		controller.getPlayerController().appendSongAtEnd(activity.getSong());
		try {
			controller.getPlayerController().playSongAtPosition(0);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
		controller.startActivity(activity, PlayerActivity.class);
		activity.finish();
	}

	public void onDeleteSongButtonClicked() {
		Intent intent = new Intent(activity, DeleteSongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(activity.getSong()));
		activity.startActivity(intent);
		activity.finish();
	}
}
