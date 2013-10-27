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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;


public class Tour extends JukefoxActivity {

	public static final String TAG = Tour.class.getSimpleName();
	protected int currentPageNr = 0;
	protected int[] imageRessourceIds;
	protected int[] textRessourceIds;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tour);
		
		registerButtons();
		
		setCurrentText(textRessourceIds[currentPageNr]);
		setCurrentImage(imageRessourceIds[currentPageNr]);
		
	}

	private void registerButtons() {
		findViewById(R.id.previousButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				previousPage();
			}
		});
		findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				nextPage();
			}
		});
		findViewById(R.id.menuButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void setCurrentImage(int id) {
		ImageView image = (ImageView) findViewById(R.id.tourImage);
		if (image != null) {
			image.setImageResource(id);
		}
	}
	
	private void setCurrentText(int id) {
		TextView image = (TextView) findViewById(R.id.tourDescription);
		if (image != null) {
			image.setText(id);
		}
	}
	
	private void nextPage() {
		currentPageNr++;
		if (currentPageNr >=  imageRessourceIds.length) {
			finish();
			return;
		}
		setCurrentText(textRessourceIds[currentPageNr]);
		setCurrentImage(imageRessourceIds[currentPageNr]);
	}
	
	private void previousPage() {
		currentPageNr--;
		if (currentPageNr < 0) {
			finish();
			return;
		}
		setCurrentText(textRessourceIds[currentPageNr]);
		setCurrentImage(imageRessourceIds[currentPageNr]);
	}
}
