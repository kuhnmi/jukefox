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
package ch.ethz.dcg.pancho3.view.overlays;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Spinner;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SleepMenuEventListener;

public class SleepMenu extends JukefoxOverlayActivity {

	private static final String TAG = SleepMenu.class.getSimpleName();
	private SleepMenuEventListener eventListener;
	private Spinner sleepTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sleepmenu);

		eventListener = controller.createSleepMenuEventListener(this);

		registerButtonListeners();

		loadLastSleepTimePos();

	}

	private void registerButtonListeners() {
		sleepTime = (Spinner) findViewById(R.id.sleep_num_min);
		// sleepTime.setOnKeyListener(new OnKeyListener() {
		//
		// @Override
		// public boolean onKey(View v, int keyCode, KeyEvent event) {
		// if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode ==
		// KeyEvent.KEYCODE_ENTER) {
		// // Hide the virtual keyboard
		// InputMethodManager inputManager = (InputMethodManager)
		// getSystemService(Context.INPUT_METHOD_SERVICE);
		// inputManager.hideSoftInputFromWindow(sleepTime.getWindowToken(),
		// InputMethodManager.HIDE_NOT_ALWAYS);
		// return true;
		// }
		// return false;
		// }
		// });
		findViewById(R.id.applySleepButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onApplySleepButtonClicked();
			}
		});
		findViewById(R.id.cancelSleepButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onCancelSleepButtonClicked();
			}
		});
	}

	public int getSleepTime() {
		int time = 0;
		Log.v(TAG, "Selected sleep time at pos: " + sleepTime.getSelectedItemPosition());
		switch (sleepTime.getSelectedItemPosition()) {
			case 0:
				time = 2;
				break;
			case 1:
				time = 5;
				break;
			case 2:
				time = 10;
				break;
			case 3:
				time = 20;
				break;
			case 4:
				time = 30;
				break;
			case 5:
				time = 45;
				break;
			case 6:
				time = 60;
				break;
			case 7:
				time = 90;
				break;
		}
		return time;
	}

	public void loadLastSleepTimePos() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		int pos = prefs.getInt("lastSleepTimePos", 3); // set to 20 minutes if
		// used the first time
		sleepTime.setSelection(pos);
	}

	public void rememberLastSleepTimePos() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("lastSleepTimePos", sleepTime.getSelectedItemPosition());
		editor.commit();
	}

}
