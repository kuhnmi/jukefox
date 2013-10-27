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
package ch.ethz.dcg.pancho3.controller.eventhandlers;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreListMenu;

public class GenreListEventListener extends BaseJukefoxEventListener {

	public GenreListEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);

	}

	public void onListItemClicked(AdapterView<?> arg0, View arg1, int arg2) {
		controller.doHapticFeedback();
		IBaseListItem temp = (IBaseListItem) arg0.getItemAtPosition(arg2);
		Genre genre = new Genre(temp.getId(), temp.getTitle());
		Intent intent = new Intent(activity, GenreListMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_GENRE, new ParcelableGenre(genre));
		activity.startActivity(intent);
	}

}
