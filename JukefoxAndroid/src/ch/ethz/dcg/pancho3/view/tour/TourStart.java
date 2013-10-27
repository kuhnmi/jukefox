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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

public class TourStart extends JukefoxActivity {

	public static final String TAG = TourStart.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tourstart);

		registerButtons();
	}

	private void registerButtons() {
		findViewById(R.id.generalTourButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(TourStart.this,
								GeneralTour.class);
						TourStart.this.startActivity(intent);
					}
				});
		findViewById(R.id.playlistTourButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(TourStart.this,
								PlaylistTour.class);
						TourStart.this.startActivity(intent);
					}
				});
		findViewById(R.id.playModesTourButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(TourStart.this,
								PlayModesTour.class);
						TourStart.this.startActivity(intent);
					}
				});
		findViewById(R.id.coolFeaturesTourButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(TourStart.this,
								CoolFeaturesTour.class);
						TourStart.this.startActivity(intent);
					}
				});
	}

}
