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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity;

public class ListActivity extends JukefoxTabActivity {

	public static final String EXTRA_KEY_LIST_TYPE = "listType";
	public static final String BUNDLE_KEY_LAST_POS = "lastPosition";

	public enum ListType {
		ARTISTS,
		ARTIST_ALBUMS,
		ALBUMS,
		SONGS,
		GENRES,
		GENRE_ALBUMS,
		GENRE_SONGS,
		GENRE_ARTISTS,
		TAGS
	};

	private ListType listType;
	protected ListView list;
	private TextView listTitle;
	protected int lastPositionInList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list);

		setCurrentTab(Tab.LISTS);

		list = (ListView) findViewById(R.id.listlist);
		listTitle = (TextView) findViewById(R.id.listTitle);
		if (savedInstanceState != null) {
			lastPositionInList = savedInstanceState.getInt(BUNDLE_KEY_LAST_POS);
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	protected void setList(String listTitle, ListAdapter adapter) {

		list.setFastScrollEnabled(false);
		list.setAdapter(adapter);
		list.setFastScrollEnabled(true);
		list.setSelection(lastPositionInList);
	}
}
