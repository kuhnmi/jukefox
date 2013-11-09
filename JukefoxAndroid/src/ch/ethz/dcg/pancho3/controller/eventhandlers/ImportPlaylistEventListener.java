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

import java.io.IOException;

import android.app.Activity;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.ImportedPlaylist;
import ch.ethz.dcg.pancho3.model.PlaylistImporter.PlaylistInfo;

public class ImportPlaylistEventListener {

	private Controller controller;

	public ImportPlaylistEventListener(Controller controller) {
		this.controller = controller;
	}

	public void showAlertDialog(Activity activity, Integer requestCode, String msg) {
		controller.showStandardDialog(activity, requestCode, msg);
	}

	public ImportedPlaylist importPlaylist(PlaylistInfo info) throws IOException {
		return controller.importPlaylist(info);
	}

}
