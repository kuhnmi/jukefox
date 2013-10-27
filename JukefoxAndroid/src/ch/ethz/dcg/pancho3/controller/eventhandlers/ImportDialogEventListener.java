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

import android.app.Activity;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.R;


public class ImportDialogEventListener {

	public static final String TAG = ImportDialogEventListener.class
	.getSimpleName();
	
	private Activity activity;
	private Controller controller;
	
	public ImportDialogEventListener(Controller controller,
			Activity activity) {
		this.activity = activity;
		this.controller = controller;
	}

	public void incrementalImportButtonClicked() {
		controller.doHapticFeedback();
		controller.doImportAsync(false, false); // flags: don't delete db; do
												// full (i.e. NOT reduced) scan)
		showToast();
		activity.finish();
	}

	public void fullImportButtonClicked() {
		controller.doHapticFeedback();
		controller.doImportAsync(true, false); // flags: delete db, do full scan
		showToast();
		activity.finish();
	}
	
	private void showToast() {
		Toast toast = Toast.makeText(activity, activity.getString(R.string.will_do_import_in_background), Toast.LENGTH_LONG);
		toast.show();
	}
	
}
