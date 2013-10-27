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
import android.widget.ListView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistListMenu;

public class ArtistListEventListener extends BaseJukefoxEventListener {

	public ArtistListEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	public void onListItemClicked(AdapterView<?> arg0, View arg1, int arg2) {
		controller.doHapticFeedback();
		IBaseListItem artist = (IBaseListItem) arg0.getItemAtPosition(arg2);
		BaseArtist baseArtist = new BaseArtist(artist.getId(), artist.getTitle());
		controller.showAlbumList(activity, baseArtist);
	}

	public boolean onListItemLongClicked(AdapterView<?> arg0, View arg1, int arg2) {
		controller.doHapticFeedback();
		IBaseListItem temp = (IBaseListItem) arg0.getItemAtPosition(arg2);
		BaseArtist artist = new BaseArtist(temp.getId(), temp.getTitle());
		Intent intent = new Intent(activity, ArtistListMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(artist));
		activity.startActivity(intent);
		return true;
	}

	public void beforeActivityFinishes(int listPosition) {
		controller.getSettingsEditor().setArtistListPosition(listPosition);
		Log.v(TAG, "Saved list pos " + listPosition);
	}

	public void afterListLoaded(ListView list) {
		list.setSelection(controller.getSettingsReader().getArtistListPosition());
		Log.v(TAG, "Read list pos " + controller.getSettingsReader().getArtistListPosition());
	}

}
