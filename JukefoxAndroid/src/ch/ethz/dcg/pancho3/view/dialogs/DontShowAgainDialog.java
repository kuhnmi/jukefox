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
package ch.ethz.dcg.pancho3.view.dialogs;

import android.os.Bundle;
import android.widget.CheckBox;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.DontShowAgainEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class DontShowAgainDialog extends StandardDialog {

	public static final String TAG = DontShowAgainDialog.class.getSimpleName();

	public final static String SHARED_PREF_KEY = "SHARED_PREF_KEY";

	private String sharedPrefKey;
	private DontShowAgainEventListener listener;
	private CheckBox dontShowAgain;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		JukefoxApplication app = (JukefoxApplication) getApplication();
		listener = new DontShowAgainEventListener(AndroidSettingsManager.getAndroidSettingsEditor());
	}

	@Override
	protected void closeDialog() {
		listener.setDontShowAgain(sharedPrefKey, dontShowAgain.isChecked());
		super.closeDialog();
	}

	@Override
	protected void processExtras(Bundle extras) {
		super.processExtras(extras);
		sharedPrefKey = extras.getString(SHARED_PREF_KEY);
	}

	@Override
	protected void setContentView() {
		Log.v(TAG, "setting content view of don't show again dialog.");
		setContentView(R.layout.dontshowagaindialog);
		dontShowAgain = (CheckBox) findViewById(R.id.dont_show_again);
	}

}
