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
import android.view.KeyEvent;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;

public class MainTabButtonEventListener extends TabEventListener {

	private Tab currentTab;

	public MainTabButtonEventListener(Controller controller, JukefoxActivity activity, Tab currentTab) {
		super(controller, activity, currentTab);
		this.currentTab = currentTab;
	}

	public static final String TAG = MainTabButtonEventListener.class.getSimpleName();

	@Override
	public boolean onKey(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKey()");
		if (keyCode != KeyEvent.KEYCODE_BACK) {
			return super.onKey(keyCode, event);
		}
		Log.v(TAG, "Back key pressed");
		if (currentTab == Tab.PLAYER) {
			return false;
		}
		startPlayerActivity();
		activity.finish();
		return true;
	}

	private void startPlayerActivity() {
		Intent intent = new Intent(activity.getApplicationContext(), PlayerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
	}
}
