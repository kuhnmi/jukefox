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
package ch.ethz.dcg.jukefox.data.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.AdvancedKdTree;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import edu.wlu.cs.levy.CG.KeySizeException;

public class PreloadedAlbums {

	private final static String TAG = PreloadedAlbums.class.getSimpleName();

	private IDbDataPortal dbDataPortal;
	private HashMap<Integer, ListAlbum> listAlbums;
	private HashMap<Integer, MapAlbum> mapAlbums;
	private AdvancedKdTree<Integer> albumGridCoordsQuadTree;
	private boolean loading = false;
	private boolean loaded = false;
	private JoinableThread loaderThread;

	public PreloadedAlbums(IDbDataPortal dbDataPortal) {
		this.dbDataPortal = dbDataPortal;
		albumGridCoordsQuadTree = new AdvancedKdTree<Integer>(2);
	}

	public void loadFromDb(final boolean includeMapAlbums) {
		if (loading) {
			return;
		}
		loaderThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				loading = true;

				// TODO: what about map albums that do not (yet) contain pca
				// coordinates, right after web data commit?
				if (includeMapAlbums) {
					loadMapAlbumsFromDb();
				} else {
					mapAlbums = new HashMap<Integer, MapAlbum>();
				}
				loadListAlbumsFromDb();
				loading = false;
				loaded = true;
			}
		});
		loaderThread.start();
	}

	public boolean isLoaded() {
		return loaded;
	}

	public List<ListAlbum> getAllListAlbums() {
		ensureLoadCompleted();
		ArrayList<ListAlbum> list = new ArrayList<ListAlbum>(listAlbums.size() + mapAlbums.size());
		list.addAll(listAlbums.values());
		list.addAll(mapAlbums.values());
		return list;
	}

	private void loadMapAlbumsFromDb() {
		List<MapAlbum> mapAlbumList = dbDataPortal.getAllMapAlbums();
		fillMapAlbums(mapAlbumList);
	}

	private void fillMapAlbums(Collection<MapAlbum> mapAlbumCollection) {
		mapAlbums = new HashMap<Integer, MapAlbum>(mapAlbumCollection.size());
		albumGridCoordsQuadTree = new AdvancedKdTree<Integer>(2);
		for (MapAlbum ma : mapAlbumCollection) {
			mapAlbums.put(ma.getId(), ma);
			try {
				albumGridCoordsQuadTree.insert(ma.getGridCoords(), ma.getId());
			} catch (KeySizeException e) {
				Log.w(TAG, e);
			}
		}
	}

	public List<Pair<MapAlbum, Float>> getSimilarAlbums(BaseAlbum album, int number) {
		List<Pair<MapAlbum, Float>> similarAlbums = new ArrayList<Pair<MapAlbum, Float>>();
		MapAlbum mapAlbum = mapAlbums.get(album.getId());
		if (mapAlbum == null) {
			return similarAlbums;
		}
		try {
			Vector<KdTreePoint<Integer>> results = albumGridCoordsQuadTree.getNearestPoints(mapAlbum.getGridCoords(),
					number + 1);
			for (KdTreePoint<Integer> point : results) {
				if (point.getID() != mapAlbum.getId()) {
					MapAlbum similarAlbum = mapAlbums.get(point.getID());
					Pair<MapAlbum, Float> p = new Pair<MapAlbum, Float>(similarAlbum, MathUtils.distance(
							mapAlbum.getGridCoords(), similarAlbum.getGridCoords()));
					similarAlbums.add(p);
				}
			}
		} catch (KeySizeException e) {
			Log.w(TAG, e);
			return similarAlbums;
		}
		return similarAlbums;
	}

	private void loadListAlbumsFromDb() {
		List<ListAlbum> listAlbumList = dbDataPortal.getAllAlbumsAsListAlbums();
		listAlbums = new HashMap<Integer, ListAlbum>(listAlbumList.size());
		for (ListAlbum la : listAlbumList) {
			if (!mapAlbums.containsKey(la.getId())) {
				listAlbums.put(la.getId(), la);
			}
		}
	}

	public Collection<MapAlbum> getAllMapAlbums() {
		ensureLoadCompleted();
		return mapAlbums.values();
	}

	// public void setMapAlbums(Collection<MapAlbum> mapAlbumCollection) {
	// fillMapAlbums(mapAlbumCollection);
	// }

	private void ensureLoadCompleted() {
		if (loading) {
			try {
				loaderThread.realJoin();
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
	}

}
