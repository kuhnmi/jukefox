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
import android.view.View;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistMenu;
import ch.ethz.dcg.pancho3.view.tabs.MapActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.SearchActivity;
import ch.ethz.dcg.pancho3.view.tabs.SpaceActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.lists.ListSelectionActivity;

public class TabEventListener extends BaseJukefoxEventListener {

	private Tab currentTab;

	public TabEventListener(Controller controller, JukefoxActivity activity, Tab currentTab) {
		super(controller, activity);
		this.currentTab = currentTab;
	}

	public void tabButtonClicked(View v) {
		controller.doHapticFeedback();
		if (controller.ignoreUserEvents()) {
			return;
		}
		int tabId = v.getId();
		Intent intent = null;
		if (tabId == currentTab.getId()) {
			return;
		}
		if (tabId == Tab.PLAYER.id) {
			intent = new Intent(activity.getApplicationContext(), PlayerActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		} else if (tabId == Tab.LISTS.id) {
			intent = new Intent(activity.getApplicationContext(), ListSelectionActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		} else if (tabId == Tab.SEARCH.id) {
			intent = new Intent(activity.getApplicationContext(), SearchActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		} else if (tabId == Tab.SPACE.id) {
			intent = new Intent(activity.getApplicationContext(), SpaceActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		} else if (tabId == Tab.MAP.id) {
			intent = new Intent(activity.getApplicationContext(), MapActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		} else {
			Log.wtf(TAG, new Exception("Tab button clicked on non-tab!"));
			return;
		}
		activity.startActivity(intent);
		if (currentTab != Tab.PLAYER) {
			activity.finish();
			// activity.finish();
		}

		// doTabAnimation(activity);
	}

	public void tabButtonLongClicked(View v) {
		tabButtonClicked(v);
		return;
		// controller.doHapticFeedback();
		// if (controller.ignoreUserEvents()) {
		// return;
		// }
		// int tabId = v.getId();
		// if (tabId == currentTab.getId()) return;
		// if (tabId == Tab.PLAYER.id) {
		// startPlayerActivity();
		// if (currentTab != Tab.PLAYER) activity.finish();
		// } else if (tabId == Tab.LISTS.id) {
		// Intent intent = new Intent(activity.getApplicationContext(),
		// ListSelectionActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// activity.startActivity(intent);
		// if (currentTab != Tab.PLAYER) activity.finish();
		// } else if (tabId == Tab.SEARCH.id) {
		// Intent intent = new Intent(activity.getApplicationContext(),
		// SearchActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// activity.startActivity(intent);
		// if (currentTab != Tab.PLAYER) activity.finish();
		// } else if (tabId == Tab.SPACE.id) {
		// Intent intent = new Intent(activity.getApplicationContext(),
		// AlbumSwipeActivity.class);
		// // Intent intent = new Intent(activity.getApplicationContext(),
		// SpaceActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// activity.startActivity(intent);
		// if (currentTab != Tab.PLAYER) activity.finish();
		// } else if (tabId == Tab.MAP.id) {
		// Intent intent = new Intent(activity.getApplicationContext(),
		// MapActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// activity.startActivity(intent);
		// if (currentTab != Tab.PLAYER) activity.finish();
		// }
		// doTabAnimation(activity);
	}

	public boolean backKeyPressed(Tab currentTab) {
		return false;
	}

	public void onPlayModeButtonClicked() {
		controller.doHapticFeedback();
		controller.startPlayModeSelection(activity);
	}

	public void onPlayPauseButtonClicked() {
		controller.doHapticFeedback();
		controller.playPauseButtonPressed();
	}

	public void onPreviousButtonClicked() {
		controller.doHapticFeedback();
		controller.previousButtonPressed();
	}

	public void onNextButtonClicked() {
		controller.doHapticFeedback();
		controller.nextButtonPressed();
	}

	public void onPlaylistMenuButtonClicked() {
		controller.doHapticFeedback();
		controller.startActivity(activity, PlaylistMenu.class);
	}
}
