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
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;


public class AlbumListMenuEventListener extends BaseJukefoxEventListener {

	public AlbumListMenuEventListener(Controller controller,
			JukefoxActivity activity) {
		super(controller, activity);

	}

	public void onGoToAlbumButtonClicked(BaseAlbum album) {
		controller.doHapticFeedback();
		controller.goToAlbum(activity, album);
	}

	public void onGroupAlbumButtonClicked(BaseAlbum album,
			int numAlbumsWithThisName) {
		
		if (numAlbumsWithThisName < 2
				|| album.getName().equals(JukefoxApplication.unknownAlbumAlias)) {
			String msg = activity
					.getString(R.string.msg_album_with_this_name_exists_only_once);
			controller.showStandardDialog(msg);
			return;
		}
		
//		controller.showAlbumGroupingDialog(activity, album);
		controller.groupAlbum(album.getName());
		activity.finish();
	}

	public void onUngroupAlbumButtonClicked(BaseAlbum album) {
		controller.ungroupAlbum(activity, album.getName());
	}

}
