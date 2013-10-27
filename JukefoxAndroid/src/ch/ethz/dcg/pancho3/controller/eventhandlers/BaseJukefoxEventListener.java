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
import android.view.MenuItem;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.ImportDialog;
import ch.ethz.dcg.pancho3.view.overlays.JukefoxPreferenceActivity;
import ch.ethz.dcg.pancho3.view.statistics.StatisticsActivity;
import ch.ethz.dcg.pancho3.view.tabs.SearchActivity;
import ch.ethz.dcg.pancho3.view.tour.TourStart;

public class BaseJukefoxEventListener {

	protected static final String TAG = BaseJukefoxEventListener.class.getSimpleName();
	protected Controller controller;
	protected JukefoxActivity activity;

	public BaseJukefoxEventListener(Controller controller, JukefoxActivity activity) {
		this.controller = controller;
		this.activity = activity;

	}

	public boolean onKey(int keyCode, KeyEvent event) {

		Log.v(TAG, "Got key Event " + keyCode);

		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					controller.nextButtonPressed();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					controller.previousButtonPressed();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					controller.mediaButtonPlayPausePressed(event.getEventTime());
					return true;
				case KeyEvent.KEYCODE_MEDIA_STOP:
					controller.stopButtonPressed();
					return true;
				case KeyEvent.KEYCODE_HEADSETHOOK:
					// if (!JukefoxApplication.ignoreMediaButtons()) {
					controller.headsetPlayPausePressed(event.getEventTime());
					// }
					return true;
				case KeyEvent.KEYCODE_SEARCH:
					controller.startActivity(activity, SearchActivity.class);
					// activity.finish();
					return true;
				case KeyEvent.KEYCODE_BACK:
					return false;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					return true;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					return true;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					return true;
				case KeyEvent.KEYCODE_HEADSETHOOK:
					return true;
				case KeyEvent.KEYCODE_SEARCH:
					return true;
			}
		}

		return false;
	}

	public boolean onMenuOptionSelected(MenuItem item) {
		// Find out which menu button was clicked
		switch (item.getItemId()) {
			case JukefoxActivity.MENU_HELP:
				// Intent intent = new Intent(Intent.ACTION_VIEW,
				// Uri.parse(Constants.HELP_URL));
				// activity.startActivity(intent);
				controller.doHapticFeedback();
				controller.startActivity(activity, TourStart.class);
				break;
			case JukefoxActivity.MENU_SETTINGS:
				controller.doHapticFeedback();
				controller.startActivity(activity, JukefoxPreferenceActivity.class);
				break;
			case JukefoxActivity.MENU_INFO:
				controller.doHapticFeedback();
				controller.showAboutDialog();
				break;
			case JukefoxActivity.MENU_UPDATE:
				controller.doHapticFeedback();
				showImportDialog();
				break;
			// case JukefoxActivity.MENU_QUIT:
			// controller.doHapticFeedback();
			// activity.getApplication().onTerminate();
			// System.exit(0);
			// break;
			case JukefoxActivity.MENU_FACEBOOK:
				// controller.writeCoordsToDisk();

				// controller.showFbSendActivity(Utils.getString(R.string.fb_send_manual_msg),
				// Utils
				// .getString(R.string.fb_collagetitlemanual));
				break;
			case JukefoxActivity.MENU_FEEDBACK:
				controller.showFeedbackDialog();
				break;
			case JukefoxActivity.MENU_STATISTICS:
				controller.doHapticFeedback();
				controller.startActivity(activity, StatisticsActivity.class);
				break;
		}
		return true;
	}

	private void showImportDialog() {
		Intent intent = new Intent(activity, ImportDialog.class);
		activity.startActivity(intent);
	}
}
