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
package ch.ethz.dcg.jukefox.manager.model.albumart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;

public class AlbumCache {

	private final static String TAG = AlbumCache.class.getSimpleName();

	private IDbDataPortal dbWrapper;
	private HashMap<Integer, ListAlbum> listAlbums;
	private HashMap<Integer, MapAlbum> mapAlbums;
	private boolean loading = false;
	private boolean loaded = false;
	private JoinableThread loaderThread;

	public AlbumCache(IDbDataPortal dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

	public void loadFromDb() {
		loaderThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				loading = true;
				loadMapAlbumsFromDb();
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
		if (loading) {
			try {
				loaderThread.realJoin();
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
		ArrayList<ListAlbum> list = new ArrayList<ListAlbum>(listAlbums.size() + mapAlbums.size());
		list.addAll(listAlbums.values());
		list.addAll(mapAlbums.values());
		return list;
	}

	private void loadMapAlbumsFromDb() {
		List<MapAlbum> mapAlbumList = dbWrapper.getAllMapAlbums();
		mapAlbums = new HashMap<Integer, MapAlbum>(mapAlbumList.size());
		for (MapAlbum ma : mapAlbumList) {
			mapAlbums.put(ma.getId(), ma);
		}
	}

	private void loadListAlbumsFromDb() {
		List<ListAlbum> listAlbumList = dbWrapper.getAllAlbumsAsListAlbums();
		listAlbums = new HashMap<Integer, ListAlbum>(listAlbumList.size());
		for (ListAlbum la : listAlbumList) {
			if (!mapAlbums.containsKey(la.getId())) {
				listAlbums.put(la.getId(), la);
			}
		}
	}

}
