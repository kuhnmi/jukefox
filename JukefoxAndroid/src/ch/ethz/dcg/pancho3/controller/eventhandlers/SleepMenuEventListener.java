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

import android.widget.Toast;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.overlays.SleepMenu;

public class SleepMenuEventListener extends BaseJukefoxEventListener {

	SleepMenu activity;

	public SleepMenuEventListener(Controller controller, SleepMenu activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onApplySleepButtonClicked() {
		int time = activity.getSleepTime();
		if (time <= 0) {
			activity.finish();
			return;
		}
		activity.rememberLastSleepTimePos();
		controller.doHapticFeedback();
		controller.setSleepTimer(time);
		Toast t = Toast.makeText(activity, activity.getString(R.string.sleep_toast1) + " " + time + " "
				+ activity.getString(R.string.sleep_toast2), Toast.LENGTH_LONG);
		t.show();
		activity.finish();
	}

	public void onCancelSleepButtonClicked() {
		controller.doHapticFeedback();
		controller.cancelSleepTimer();
		activity.finish();
	}

}
