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

import java.util.HashMap;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumListMenu;

public class AlbumListEventListener extends BaseJukefoxEventListener {

	private AlbumList activity;

	public AlbumListEventListener(Controller controller, AlbumList activity) {
		super(controller, activity);
		this.activity = activity;
	}

	public void onListItemClicked(AdapterView<?> arg0, View arg1, int arg2) {
		controller.doHapticFeedback();
		IBaseListItem album = (IBaseListItem) arg0.getItemAtPosition(arg2);
		BaseAlbum baseAlbum = new BaseAlbum(album.getId(), album.getTitle());
		controller.showAlbumDetailInfo(activity, baseAlbum);
	}

	public boolean onListItemLongClicked(AdapterView<?> parent, View view, int pos, long id) {

		controller.doHapticFeedback();
		IBaseListItem album = (IBaseListItem) parent.getItemAtPosition(pos);
		BaseAlbum baseAlbum = new BaseAlbum(album.getId(), album.getTitle());
		Intent intent = new Intent(activity, AlbumListMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(baseAlbum));
		String albumName = baseAlbum.getName();
		HashMap<String, Integer> albumNameCnts = activity.getAlbumNameCnts();
		Integer albumNameCnt = albumNameCnts.get(albumName);
		if (albumNameCnt == null) {
			albumNameCnt = 0;
		}
		intent.putExtra(Controller.INTENT_EXTRA_NUMBER_OF_ALBUMS_WITH_THIS_NAME, albumNameCnt);
		activity.startActivity(intent);
		return true;
	}

	public void onContainsAlbumNameMoreThanTwice() {
		String msg = activity.getString(R.string.tip_long_click_on_album_for_grouping);
		controller.showDontShowAgainDialog(msg, activity.getString(R.string.KEY_ALBUM_NAME_MORE_THAN_TWICE_DIALOG));
	}

	public void beforeActivityFinishes(int listPosition) {
		controller.getSettingsEditor().setAlbumListPosition(listPosition);
		Log.v(TAG, "Saved list pos " + listPosition);
	}

	public void afterListLoaded(ListView list) {
		list.setSelection(controller.getSettingsReader().getAlbumListPosition());
		Log.v(TAG, "Read list pos " + controller.getSettingsReader().getAlbumListPosition());
	}
}
