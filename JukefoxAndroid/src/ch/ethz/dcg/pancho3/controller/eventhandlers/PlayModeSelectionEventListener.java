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
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.SmartShufflePlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.IReadOnlyAndroidApplicationState;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.overlays.ContextShuffleConfig;
import ch.ethz.dcg.pancho3.view.overlays.ShuffleModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimilarModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimpleAgentsMenu;

public class PlayModeSelectionEventListener extends BaseJukefoxEventListener {

	private final IReadOnlyAndroidApplicationState appState;

	public PlayModeSelectionEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
		appState = activity.getCollectionModel().getApplicationStateManager().getApplicationStateReader();
	}

	public boolean onRepeatAllButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().setPlayMode(PlayModeType.REPEAT, 0, 0);
		activity.finish();
		return true;
	}

	public boolean onShuffleButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, ShuffleModeMenu.class);
		activity.finish();
		return true;
	}

	public boolean onSimilarButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, SimilarModeMenu.class);
		activity.finish();
		return true;
	}

	public boolean onSmartShuffleButtonClicked() {
		controller.doHapticFeedback();
		if (!appState.isMapDataCommitted()
				|| appState.getNumbersOfSongsWithCoordinates() < SmartShufflePlayMode.SMART_SHUFFLING_MIN_SONGS) {
			controller.showStandardDialog(activity.getString(R.string.smart_shuffling_not_available));
			// controller.showDontShowAgainDialog(activity
			// .getString(R.string.smart_shuffling_not_available));
			return false;
		}
		controller.getPlayerController().setPlayMode(PlayModeType.SMART_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		activity.finish();
		return true;
	}

	public boolean onSmartShuffleSettingsButtonClicked() {
		controller.doHapticFeedback();

		Intent intent = new Intent(activity, SimpleAgentsMenu.class); // TODO @Sämy Choose which class to use (Simple or Complex)
		activity.startActivity(intent);
		return true;
	}

	public boolean onPlayOnceButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().setPlayMode(PlayModeType.PLAY_ONCE, 0, 0);
		activity.finish();
		return true;
	}

	public boolean onContextShuffleButtonClicked() {
		controller.doHapticFeedback();
		if (!appState.isMapDataCommitted()
				|| appState.getNumbersOfSongsWithCoordinates() < SmartShufflePlayMode.SMART_SHUFFLING_MIN_SONGS) {
			controller.showStandardDialog(activity.getString(R.string.smart_shuffling_not_available));
			// controller.showDontShowAgainDialog(activity
			// .getString(R.string.smart_shuffling_not_available));
			return false;
		}
		controller.getPlayerController()
				.setPlayMode(PlayModeType.CONTEXT_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		controller.startActivity(activity, ContextShuffleConfig.class);
		activity.finish();
		return true;
	}
}
