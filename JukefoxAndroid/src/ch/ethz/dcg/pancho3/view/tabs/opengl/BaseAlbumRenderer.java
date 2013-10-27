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
package ch.ethz.dcg.pancho3.view.tabs.opengl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapTag;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class BaseAlbumRenderer extends BaseRenderer {

	private static final int NUM_ALBUM_TEXTURES = 40;
	protected List<GlTagLabel> tags;
	protected List<GlAlbumCover> albums;
	protected AlbumArtTextureLoader albumArtTextureLoader;
	private GlAlbumCover albumToLoadAlbumArt;
	private AndroidCollectionModelManager collectionModel;

	protected int[] albumTextures;

	public BaseAlbumRenderer(AndroidCollectionModelManager collectionModel, ImportState importState,
			Context context) {
		super(importState, context);
		this.collectionModel = collectionModel;
		albumTextures = new int[0];
	}

	@Override
	public void drawFrame(GL10 gl) {
		super.drawFrame(gl);

		long currentTime = System.currentTimeMillis();
		camera.updatePosition(currentTime);

	}

	@Override
	public void surfaceCreated(GL10 gl) {
		super.surfaceCreated(gl);

		Log.v(TAG, "Surface created");

		createTextureArray(gl);

		albumArtTextureLoader = new AlbumArtTextureLoader(this, collectionModel, albumTextures);
		albumArtTextureLoader.start();
	}

	private void createTextureArray(GL10 gl) {

		albumTextures = new int[NUM_ALBUM_TEXTURES];
		gl.glGenTextures(NUM_ALBUM_TEXTURES, albumTextures, 0);

	}

	public List<GlAlbumCover> getAlbums() {
		return albums;
	}

	public List<GlTagLabel> getTags() {
		return tags;
	}

	protected void createGlAlbums(Collection<MapAlbum> mapAlbums, boolean horizontal) {

		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;

		albums = new ArrayList<GlAlbumCover>(mapAlbums.size());

		for (MapAlbum mapAlbum : mapAlbums) {
			if (mapAlbum.getGridCoords()[0] < minX) {
				minX = mapAlbum.getGridCoords()[0];
			}
			if (mapAlbum.getGridCoords()[0] > maxX) {
				maxX = mapAlbum.getGridCoords()[0];
			}
			if (mapAlbum.getGridCoords()[1] < minZ) {
				minZ = mapAlbum.getGridCoords()[1];
			}
			if (mapAlbum.getGridCoords()[1] > maxZ) {
				maxZ = mapAlbum.getGridCoords()[1];
			}
			albums.add(new GlAlbumCover(mapAlbum, horizontal));
			// Log.v(TAG, "Album with posX: " + mapAlbum.getGridCoords()[0] +
			// " posZ: " + mapAlbum.getGridCoords()[1]);
		}

		sortAlbums();

		camera.setXBorders(minX, maxX);
		camera.setZBorders(minZ, maxZ);
		// Log.v(TAG, "Album with minX: " + minX + " maxX: " + maxX + " minZ: "
		// + minZ + " + maxZ: + " + maxZ);
	}

	protected void createGlTags(List<MapTag> mapTags, boolean horizontal) {
		tags = new ArrayList<GlTagLabel>();

		if (mapTags == null) {
			return;
		}

		for (MapTag mapTag : mapTags) {
			tags.add(new GlTagLabel(mapTag, horizontal));
		}

		sortTags();
	}

	private void sortAlbums() {
		Collections.sort(albums, new Comparator<GlAlbumCover>() {

			@Override
			public int compare(GlAlbumCover object1, GlAlbumCover object2) {
				if (object1.getMapAlbum().getGridCoords()[1] > object2.getMapAlbum().getGridCoords()[1]) {
					return 1;
				} else if (object1.getMapAlbum().getGridCoords()[1] == object2.getMapAlbum().getGridCoords()[1]) {
					return 0;
				} else {
					return -1;
				}
			}

		});
	}

	private void sortTags() {
		Collections.sort(tags, new Comparator<GlTagLabel>() {

			@Override
			public int compare(GlTagLabel object1, GlTagLabel object2) {
				if (object1.getMapTag().getCoordsPca2D()[1] > object2.getMapTag().getCoordsPca2D()[1]) {
					return 1;
				} else if (object1.getMapTag().getCoordsPca2D()[1] == object2.getMapTag().getCoordsPca2D()[1]) {
					return 0;
				} else {
					return -1;
				}
			}

		});
	}

	public synchronized GlAlbumCover getAlbumArtToLoad() {
		return albumToLoadAlbumArt;
	}

	protected synchronized void setAlbumToLoadAlbumArt(GlAlbumCover albumForCoverLoad) {
		albumToLoadAlbumArt = albumForCoverLoad;
	}
}
