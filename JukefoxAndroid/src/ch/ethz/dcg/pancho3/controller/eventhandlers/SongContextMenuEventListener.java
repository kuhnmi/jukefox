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
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.overlays.DeleteSongMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongContextMenu;

public class SongContextMenuEventListener extends BaseJukefoxEventListener {

	SongContextMenu activity;

	public SongContextMenuEventListener(Controller controller, SongContextMenu activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public static final String TAG = SongContextMenuEventListener.class.getSimpleName();

	public void onRemoveSongButtonClicked() {
		controller.doHapticFeedback();
		try {
			controller.getPlayerController().removeSongFromPlaylist(activity.getPositionInPlaylist());
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		activity.finish();
	}

	public void onShowAlbumButtonClicked() {
		controller.doHapticFeedback();
		BaseAlbum album = activity.getSong().getAlbum();
		controller.showAlbumDetailInfo(activity, album);
		activity.finish();
	}

	public void onGoToAlbumButtonClicked() {
		controller.doHapticFeedback();
		controller.goToAlbum(activity, activity.getSong().getAlbum());
	}

	public void onShowArtistButtonClicked() {
		controller.doHapticFeedback();
		BaseArtist baseArtist = activity.getSong().getArtist();
		controller.showAlbumList(activity, baseArtist);
		activity.finish();
	}

	public void onDeleteSongButtonClicked() {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, DeleteSongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_SONG_PLAYLIST_POSITION, activity.getPositionInPlaylist());
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(activity.getSong()));
		activity.startActivity(intent);
		activity.finish();
	}
}
