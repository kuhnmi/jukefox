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
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLU;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapTag;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;

public class MapRenderer extends BaseAlbumRenderer {

	public static final float DEFAULT_CAMERA_HEIGHT = 13;
	protected static final String TAG = MapRenderer.class.getSimpleName();
	private static final int PLANE_TAG_THRESHHOLD = 50;
	// private static final int NUM_MAP_TAGS = 100;

	private final AndroidCollectionModelManager data;
	private final ISettingsReader settings;
	private CurrentAlbumProvider currentAlbumProvider;

	private RegionPlaylistCreator regionPlaylist = null;
	private boolean highlightCurrentAlbum;
	private GlHighlight glHighlight;
	private boolean atLeastOneAlbumWasVisible = false;

	public MapRenderer(ISettingsReader settings, AndroidCollectionModelManager model,
			ImportState importState, Context context,
			CurrentAlbumProvider currentAlbumProvider, Intent intent) {
		super(model, importState, context);
		this.data = model;
		this.settings = settings;
		this.currentAlbumProvider = currentAlbumProvider;

		Collection<MapAlbum> mapAlbums;
		try {
			mapAlbums = data.getAlbumProvider().getAllMapAlbums();
		} catch (DataUnavailableException e) {
			mapAlbums = new ArrayList<MapAlbum>();
			Log.w(TAG, e);
		}
		Log.v(TAG, "Got " + mapAlbums.size() + " mapAlbums");

		createGlAlbums(mapAlbums, true);

		Log.v(TAG, "Created GL albums");
		List<MapTag> mapTags = data.getTagProvider().getAllMapTags();
		createGlTags(mapTags, true);
		Log.v(TAG, "Created " + mapTags.size() + " GlTags");
		highlightCurrentAlbum = settings.isCurrentAlbumHighlighted();
		glHighlight = new GlHighlight(0, 0, 0, true, context);

		gotoCurrentAlbum();
		float posX = settings.getLastPositionInPcaMapX();
		float posZ = settings.getLastPositionInPcaMapY();
		// First set map to last position
		camera.setCameraPosition(posX, MapRenderer.DEFAULT_CAMERA_HEIGHT, posZ, false);
	}

	/**
	 * Interface which tells the renderer which is the current album so it can
	 * be highlighted.
	 */
	public static interface CurrentAlbumProvider {

		MapAlbum getCurrentAlbum();
	}

	public void setCurrentAlbumProvider(CurrentAlbumProvider currentAlbumProvider) {
		this.currentAlbumProvider = currentAlbumProvider;
		gotoCurrentAlbum();
	}

	private void gotoCurrentAlbum() {
		if (settings.isGotoCurrentAlbumEnabled() && currentAlbumProvider != null) {
			MapAlbum album = currentAlbumProvider.getCurrentAlbum();
			if (album != null) {
				goToAlbum(album);
				Log.v(TAG, "Set camera to album: " + album.getName());
				return;
			}
		}
	}

	public void goToAlbum(BaseAlbum album) {
		if (album == null) {
			return;
		}
		MapAlbum mapAlbum;
		try {
			mapAlbum = data.getAlbumProvider().getMapAlbum(album);
			camera.setCameraPosition(mapAlbum.getGridCoords()[0], DEFAULT_CAMERA_HEIGHT, mapAlbum.getGridCoords()[1],
					false);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public void drawFrame(GL10 gl) {
		super.drawFrame(gl);

		float camPosZ = getCamera().getPosZ();
		float camPosX = getCamera().getPosX();
		float camPosY = getCamera().getPosY();

		updateModelViewWithCamera(gl, camPosZ, camPosX, camPosY);

		float widthFactor = getViewRatio() / getCamera().getFrontClippingPlane();
		float heightFactor = 1f / getCamera().getFrontClippingPlane();
		float visibleMinX = camPosX - camPosY * widthFactor - 2 * GlAlbumCover.COVER_SIZE;
		float visibleMaxX = camPosX + camPosY * widthFactor + 2 * GlAlbumCover.COVER_SIZE;
		float visibleMinZ = camPosZ - camPosY * heightFactor - 2 * GlAlbumCover.COVER_SIZE;
		float visibleMaxZ = camPosZ + camPosY * heightFactor + 2 * GlAlbumCover.COVER_SIZE;

		doRegionPlaylist(gl);

		drawHighlight(gl);

		boolean drawTagsInsteadOfTextures = camPosY > PLANE_TAG_THRESHHOLD;

		drawAlbums(gl, camPosZ, camPosX, drawTagsInsteadOfTextures, visibleMinZ, visibleMaxZ, visibleMinX, visibleMaxX);

		if (drawTagsInsteadOfTextures) {
			drawTags(gl, camPosY, visibleMinX, visibleMaxX, visibleMinZ, visibleMaxZ);
		}

	}

	private void drawTags(GL10 gl, float camPosY, float visibleMinX, float visibleMaxX, float visibleMinZ,
			float visibleMaxZ) {

		// float proportional = (camPosY - PLANE_TAG_THRESHHOLD)
		// / (getCamera().getRearClippingPlane() - PLANE_TAG_THRESHHOLD);

		// int num_labels = (int) ((1 - proportional) * NUM_MAP_TAGS / 6f +
		// NUM_MAP_TAGS / 6f);

		int u = 0;
		for (GlTagLabel glTagLabel : tags) {

			MapTag mapTag = glTagLabel.getMapTag();
			if (mapTag.getCoordsPca2D()[1] < visibleMinZ) {
				continue;
			}
			if (mapTag.getCoordsPca2D()[1] > visibleMaxZ) {
				break;
			}

			// Check if tag labels should be drawn
			// if (u < num_labels) {

			glTagLabel.setSizeInHorizontalMode(camPosY / PLANE_TAG_THRESHHOLD);
			glTagLabel.draw(gl);
			u++;
			// }
			// if (u == num_labels)
			// break;
		}

	}

	private void updateModelViewWithCamera(GL10 gl, float camPosZ, float camPosX, float camPosY) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		GLU.gluLookAt(gl, camPosX, camPosY, camPosZ, camPosX, camPosY - 1, camPosZ, 0f, 0.0f, -1.0f);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	private void drawAlbums(GL10 gl, float camPosZ, float camPosX, boolean drawTagsInsteadOfTextures,
			float visibleMinZ, float visibleMaxZ, float visibleMinX, float visibleMaxX) {
		float minDistToCenter = Float.MAX_VALUE;
		GlAlbumCover albumForCoverLoad = null;
		for (GlAlbumCover glAlbumCover : albums) {

			MapAlbum mapAlbum = glAlbumCover.getMapAlbum();
			if (mapAlbum.getGridCoords()[1] < visibleMinZ) {
				continue;
			}
			if (mapAlbum.getGridCoords()[1] > visibleMaxZ) {
				break;
			}

			if (!drawTagsInsteadOfTextures && !glAlbumCover.isTextureLoaded()) {
				float diffX = Math.abs(camPosX - mapAlbum.getGridCoords()[0]);
				float diffZ = Math.abs(camPosZ - mapAlbum.getGridCoords()[1]);
				float dist = diffX + diffZ;
				if (dist < minDistToCenter) {
					minDistToCenter = dist;
					albumForCoverLoad = glAlbumCover;
				}
			}

			glAlbumCover.draw(gl, !drawTagsInsteadOfTextures);
			if (!atLeastOneAlbumWasVisible) {
				checkIfAlbumIsVisible(glAlbumCover, visibleMinX, visibleMaxX);
			}
		}

		setAlbumToLoadAlbumArt(albumForCoverLoad);

		if (!atLeastOneAlbumWasVisible && albums.size() > 0) {
			goToRandomAlbum();
		}
	}

	private void goToRandomAlbum() {
		int rnd = RandomProvider.getRandom().nextInt(albums.size());
		MapAlbum album = albums.get(rnd).getMapAlbum();
		camera.setCameraPosition(album.getGridCoords()[0], DEFAULT_CAMERA_HEIGHT, album.getGridCoords()[1], false);
	}

	private void checkIfAlbumIsVisible(GlAlbumCover glAlbumCover, float visibleMinX, float visibleMaxX) {
		MapAlbum mapAlbum = glAlbumCover.getMapAlbum();
		if (mapAlbum.getGridCoords()[0] > visibleMinX && mapAlbum.getGridCoords()[0] < visibleMaxX) {
			atLeastOneAlbumWasVisible = true;
		}
	}

	private void drawHighlight(GL10 gl) {
		if (currentAlbumProvider != null) {
			// Maybe draw a highlight
			MapAlbum album = currentAlbumProvider.getCurrentAlbum();
			if (highlightCurrentAlbum && album != null) {
				glHighlight.setCoords(album.getGridCoords()[0], -0.01f, album.getGridCoords()[1]);
				glHighlight.draw(gl, albumTextures[0]);
			}
		}
	}

	private void doRegionPlaylist(GL10 gl) {
		if (regionPlaylist != null) {
			regionPlaylist.draw(gl);
			if (regionPlaylist.hasToGetAlbums()) {
				regionPlaylist.getAlbumsInPlaylist(gl);
				regionPlaylist = null;
			}
		}
	}

	public void startDrawingRegionPlaylist(RegionPlaylistCreator regionPlaylist) {
		this.regionPlaylist = regionPlaylist;
	}

	public void stopDrawingRegionPlaylist() {
		if (regionPlaylist != null) {
			regionPlaylist.createPlaylist();
		}
	}

	public void onResume() {
		for (GlAlbumCover albumCover : albums) {
			albumCover.resetTexture();
		}
		for (GlTagLabel tag : tags) {
			tag.resetTexture();
		}
		glHighlight = new GlHighlight(0, 0, 0, true, context);
		atLeastOneAlbumWasVisible = false;
	}

	public void onPause() {
		if (albumArtTextureLoader != null) {
			albumArtTextureLoader.terminate();
		}
	}

	public void newCurrentAlbum() {
		if (settings.isGotoCurrentAlbumEnabled() && currentAlbumProvider != null) {
			MapAlbum album = currentAlbumProvider.getCurrentAlbum();
			if (album != null) {
				goToAlbum(album);
				Log.v(TAG, "Set camera to album: " + album.getName());
				return;
			}
		}
	}
}
