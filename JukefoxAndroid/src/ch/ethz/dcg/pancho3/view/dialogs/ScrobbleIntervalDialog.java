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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

public class ScrobbleIntervalDialog extends JukefoxActivity {

	@SuppressWarnings("unused")
	private final static String TAG = "GaplessSettingsDialog";
	private EditText scrobbleInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scrobbleintervaldialog);
		// Have the system blur any windows behind this one.
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		scrobbleInterval = (EditText) findViewById(R.id.scrobbleInterval);
		scrobbleInterval.setText("" + settings.getScrobbleInterval());
		scrobbleInterval.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					// Hide the virtual keyboard
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(scrobbleInterval.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}

		});

		registerOkButton();
	}

	private void registerOkButton() {
		Button okButton = (Button) findViewById(R.id.scrobbleIntervalOkButton);
		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int offset = 0;
				try {
					offset = Integer.parseInt(scrobbleInterval.getText().toString());
				} catch (Exception e) {
					Log.w("Settings", e);
				}
				Application application = getApplication();
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt(getString(R.string.KEY_SCROBBLE_INTERVAL), offset);
				editor.commit();
				finish();
			}
		});
	}

}
