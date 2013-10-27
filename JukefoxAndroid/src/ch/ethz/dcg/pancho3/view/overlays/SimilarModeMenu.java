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

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarModeMenuEventListener;

public class SimilarModeMenu extends JukefoxOverlayActivity {

	private static final String TAG = SimilarModeMenu.class.getSimpleName();
	private SimilarModeMenuEventListener eventListener;
	private EditText numArtists;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.similarmodemenu);

		eventListener = controller.createSimilarModeMenuEventListener(this);

		registerButtonListeners();

	}

	private void registerButtonListeners() {
		numArtists = (EditText) findViewById(R.id.similarSongsNumArtists);
		numArtists.setText("" + settings.getSimilarArtistAvoidanceNumber());
		numArtists.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					// Hide the virtual keyboard
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(numArtists.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				return false;
			}
		});
		findViewById(R.id.applySimilarSettings).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onApplyButtonClicked();
			}
		});
		findViewById(R.id.similarModeMenuCancel).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onCancelButtonClicked();
			}
		});
	}

	public int getNumberOfSubsequentArtists() {
		String sleepTimeString = numArtists.getText().toString();
		int time = 0;
		try {
			time = Integer.parseInt(sleepTimeString);
		} catch (NumberFormatException e) {
			Log.w(TAG, e);
		}
		return time;
	}

}
