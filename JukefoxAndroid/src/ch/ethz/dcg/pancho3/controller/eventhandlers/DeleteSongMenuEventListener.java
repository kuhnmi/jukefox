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

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.view.overlays.DeleteSongMenu;

public class DeleteSongMenuEventListener extends BaseJukefoxEventListener {

	private final DeleteSongMenu activity;

	public DeleteSongMenuEventListener(Controller controller, DeleteSongMenu activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onCancelButtonClicked() {
		controller.doHapticFeedback();
		activity.finish();
	}

	public void onIgnoreSongButtonClicked() {
		controller.doHapticFeedback();
		removeFromPlaylist();
		try {
			BaseSong<BaseArtist, BaseAlbum> song = controller.getPlayerController().getCurrentSong();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		controller.ignoreSong(activity.getSong());
		activity.finish();
	}

	private void removeFromPlaylist() {
		if (activity.getPositionInPlaylist() >= 0) {
			try {
				controller.getPlayerController().removeSongFromPlaylist(activity.getPositionInPlaylist());
			} catch (EmptyPlaylistException e) {
				Log.w(TAG, e);
			} catch (PlaylistPositionOutOfRangeException e) {
				Log.w(TAG, e);
			}
		}
	}

	public void onDeleteSongButtonClicked() {
		controller.doHapticFeedback();
		removeFromPlaylist();
		controller.deleteSong(activity.getSong());
		activity.finish();
	}

}
