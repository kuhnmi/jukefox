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
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistContextMenu;
import ch.ethz.dcg.pancho3.view.overlays.SavePlaylistMenu;

public class SavePlaylistMenuEventListener extends BaseJukefoxEventListener {

	SavePlaylistMenu activity;

	public SavePlaylistMenuEventListener(Controller controller, SavePlaylistMenu activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onListItemClicked(String playlistName) {
		controller.doHapticFeedback();
		activity.setEditText(playlistName);
	}

	public void onSavePlaylistButtonClicked(String name) {
		controller.doHapticFeedback();
		JukefoxApplication.getPlayerModel().getPlaylistManager().writePlaylistToFile(
				controller.getPlayerController().getCurrentPlaylist(), name);
		activity.finish();
	}

	public boolean onListItemLongClicked(String path) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, PlaylistContextMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_PATH, path);
		activity.startActivity(intent);
		return true;
	}

}
