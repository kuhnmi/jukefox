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
package ch.ethz.dcg.pancho3.tablet;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import ch.ethz.dcg.pancho3.tablet.view.TabletActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;

public class StartActivity extends Activity {
	private static boolean HONEYCOMB_SUPPORT = android.os.Build.VERSION.SDK_INT > 10;
	private static boolean TABLET_VERSION_ENABLED = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent;
		int screenLayout = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		// screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE && 
		if (HONEYCOMB_SUPPORT
				&& TABLET_VERSION_ENABLED) {
			intent = new Intent(this, TabletActivity.class);
		} else {
			intent = new Intent(this, PlayerActivity.class);
		}
		startActivity(intent);
		finish();
	}
}
