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

import java.util.List;

import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;

public class TabletMapEventListener extends MapEventListener {

	private TabletPresenter tabletPresenter;

	public TabletMapEventListener(Controller controller, JukefoxActivity activity,
			MapRenderer mapRenderer, TabletPresenter tabletPresenter) {
		super(controller, activity, mapRenderer, true);
		this.tabletPresenter = tabletPresenter;
	}

	@Override
	protected void showAlbumDetailInfo(JukefoxActivity activity, MapAlbum mapAlbum) {
		tabletPresenter.displayOverlay(mapAlbum, false, true);
	}

	/**
	 * Returns true if the given map renderer points to the same map renderer as
	 * the one used in this class.
	 */
	public boolean containsSameMapRenderer(MapRenderer mapRenderer) {
		return this.mapRenderer == mapRenderer;
	}

	@Override
	public void onRegionCreated(List<MapAlbum> albumsInRegion) {
		if (albumsInRegion.size() == 1) {
			tabletPresenter.displayOverlay(albumsInRegion.get(0), false, true);
		} else {
			tabletPresenter.displayOverlay(albumsInRegion);
		}
	}
}
