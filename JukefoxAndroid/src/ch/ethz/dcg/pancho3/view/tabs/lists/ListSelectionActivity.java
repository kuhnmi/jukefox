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
package ch.ethz.dcg.pancho3.view.tabs.lists;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ListSelectionEventListener;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity;


public class ListSelectionActivity extends JukefoxTabActivity {
	
	private ListSelectionEventListener eventlistener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);	        
        
        setContentView(R.layout.listselection);
        
        setCurrentTab(Tab.LISTS);
        
        eventlistener = controller.createListSelectionEventListener(this);
        
        registerListSelectionButtons();

	}

	private void registerListSelectionButtons() {
		LinearLayout artistListButton = (LinearLayout) findViewById(R.id.artistListButton);
		artistListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				eventlistener.artistListSelected();				
			}
		});
		LinearLayout albumListButton = (LinearLayout) findViewById(R.id.albumListButton);
		albumListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				eventlistener.albumListSelected();				
			}
		});
		LinearLayout songListButton = (LinearLayout) findViewById(R.id.songListButton);
		songListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				eventlistener.songListSelected();				
			}
		});
		LinearLayout genreListButton = (LinearLayout) findViewById(R.id.genreListButton);
		genreListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				eventlistener.genreListSelected();				
			}
		});
		LinearLayout tagListButton = (LinearLayout) findViewById(R.id.tagListButton);
		tagListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				eventlistener.tagListSelected();				
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (eventlistener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
}
