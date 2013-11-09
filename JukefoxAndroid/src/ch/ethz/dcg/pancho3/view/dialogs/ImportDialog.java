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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ImportDialogEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class ImportDialog extends Activity {

	public static final String TAG = ImportDialog.class.getSimpleName();

	private String sharedPrefKey;
	private ImportDialogEventListener listener;
	private CheckBox dontShowAgain;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		JukefoxApplication app = (JukefoxApplication) getApplication();
		listener = app.getController().createImportDialogEventListener(this);

		// Have the system blur any windows behind this one.
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		setContentView(R.layout.importdialog);

		registerButtons();
	}

	private void registerButtons() {
		findViewById(R.id.incrementallyButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				listener.incrementalImportButtonClicked();
			}

		});
		findViewById(R.id.everythingButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				listener.fullImportButtonClicked();
			}

		});
	}

}
