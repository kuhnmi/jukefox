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

public class PlaylistTour extends Tour {
	public static final String TAG = PlaylistTour.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		imageRessourceIds = new int[] {R.drawable.d070_playlist_tour_1, R.drawable.d071_playlist_tour_2, R.drawable.d072_playlist_tour_3};
		textRessourceIds = new int[] {R.string.playlists_tour_1, R.string.playlists_tour_2, R.string.playlists_tour_3};
		
		super.onCreate(savedInstanceState);
	}
	
	
}
