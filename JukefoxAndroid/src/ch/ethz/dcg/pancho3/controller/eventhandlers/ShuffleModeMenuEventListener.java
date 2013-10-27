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

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

public class ShuffleModeMenuEventListener extends BaseJukefoxEventListener {

	public ShuffleModeMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	public boolean onShufflePlaylistButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().setPlayMode(PlayModeType.SHUFFLE_PLAYLIST, 0,
				Constants.SAME_SONG_AVOIDANCE_NUM);
		activity.finish();
		return true;
	}

	public boolean onShuffleCollectionButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().setPlayMode(PlayModeType.RANDOM_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		activity.finish();
		return true;
	}

}
