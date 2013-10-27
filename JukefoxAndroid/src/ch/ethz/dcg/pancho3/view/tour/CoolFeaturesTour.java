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

public class CoolFeaturesTour extends Tour {
	public static final String TAG = CoolFeaturesTour.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		imageRessourceIds = new int[] {R.drawable.d080_cool_features_tour_1, R.drawable.d081_cool_features_tour_2, R.drawable.d082_cool_features_tour_3};
		textRessourceIds = new int[] {R.string.cool_features_tour_1, R.string.cool_features_tour_2, R.string.cool_features_tour_3};
		
		super.onCreate(savedInstanceState);
	}
	
	
}
