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
package ch.ethz.dcg.pancho3.view.tour;

import android.os.Bundle;
import ch.ethz.dcg.pancho3.R;

public class GeneralTour extends Tour {

	public static final String TAG = GeneralTour.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		imageRessourceIds = new int[] { R.drawable.d059_general_tour_1,
				R.drawable.d060_general_tour_2, R.drawable.d061_general_tour_3,
				R.drawable.d062_general_tour_4, R.drawable.d063_general_tour_5,
				R.drawable.d064_general_tour_6, R.drawable.d065_general_tour_7,
				R.drawable.d066_general_tour_8, R.drawable.d067_general_tour_9,
				R.drawable.d068_general_tour_10,
				R.drawable.d069_general_tour_11 };
		textRessourceIds = new int[] { R.string.general_tour_1,
				R.string.general_tour_2, R.string.general_tour_3,
				R.string.general_tour_4, R.string.general_tour_5,
				R.string.general_tour_6, R.string.general_tour_7,
				R.string.general_tour_8, R.string.general_tour_9,
				R.string.general_tour_10, R.string.general_tour_11 };

		super.onCreate(savedInstanceState);
	}

}
