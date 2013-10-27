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

import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListEventListener;

public class GenreList extends ListActivity {

	private GenreListEventListener eventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loadGenreList();

		eventListener = controller.createGenreListEventListener(this);

		registerEventListeners();

	}

	private void registerEventListeners() {
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				eventListener.onListItemClicked(arg0, arg1, arg2);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void loadGenreList() {
		if (applicationState.isImporting() && !applicationState.isBaseDataCommitted()) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		}
		List<Genre> genreList = genreProvider.getAllGenres();
		TextSectionAdapter adapter = new TextSectionAdapter(this, R.layout.textlistitem, genreList, getSettings()
				.isIgnoreLeadingThe());
		setList(getString(R.string.genres), adapter);
	}

}
