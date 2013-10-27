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

import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistList;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreList;
import ch.ethz.dcg.pancho3.view.tabs.lists.SongList;
import ch.ethz.dcg.pancho3.view.tabs.lists.TagCloud;

public class ListSelectionEventListener extends MainTabButtonEventListener {

	public ListSelectionEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity, Tab.LISTS);
	}

	public void artistListSelected() {
		controller.doHapticFeedback();
		controller.startActivity(activity, ArtistList.class);
	}

	public void albumListSelected() {
		controller.doHapticFeedback();
		controller.startActivity(activity, AlbumList.class);
	}

	public void songListSelected() {
		controller.doHapticFeedback();
		controller.startActivity(activity, SongList.class);
	}

	public void genreListSelected() {
		controller.doHapticFeedback();
		controller.startActivity(activity, GenreList.class);
	}

	public void tagListSelected() {
		controller.doHapticFeedback();
		controller.startActivity(activity, TagCloud.class);
	}

}
