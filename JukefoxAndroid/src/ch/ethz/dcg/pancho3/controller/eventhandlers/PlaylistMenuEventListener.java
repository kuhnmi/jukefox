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

import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.overlays.ImportPlaylistActivity;
import ch.ethz.dcg.pancho3.view.overlays.LoadPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.LoadVideoPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.SavePlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.SleepMenu;

public class PlaylistMenuEventListener extends BaseJukefoxEventListener {

	public PlaylistMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);

	}

	public void onNewPlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().stop();
		controller.getPlayerController().clearPlaylist();
		activity.finish();
	}

	public void onLoadPlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, LoadPlaylistMenu.class);
		activity.finish();
	}

	public void onSavePlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, SavePlaylistMenu.class);
		activity.finish();
	}

	public void onSleepButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, SleepMenu.class);
		activity.finish();
	}

	public void onVideoPlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, LoadVideoPlaylistMenu.class);
		// activity.finish();
	}

	public void onImportPlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, ImportPlaylistActivity.class);
		activity.finish();
	}

}
