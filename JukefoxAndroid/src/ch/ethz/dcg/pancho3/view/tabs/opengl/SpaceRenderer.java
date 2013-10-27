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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLU;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapTag;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.view.tabs.SpaceActivity;

public class SpaceRenderer extends BaseAlbumRenderer {

	protected static final String TAG = SpaceRenderer.class.getSimpleName();
	private RegionPlaylistCreator regionPlaylist = null;
	private boolean highlightCurrentAlbum;
	private SpaceActivity spaceActivity;
	private GlHighlight glHighlight;
	// private static final int PLANE_TAG_THRESHHOLD = 50;
	// private static final int NUM_MAP_TAGS = 100;
	public static final float CAMERA_HEIGHT = 1.7f;
	private LinkedList<KdTreePoint<GlAlbumCover>> visibleAlbums;
	private boolean doSelection;
	private float[] clickPos;
	private MapAlbum selectedAlbum;
	// private long lastLogTime;
	private AndroidCollectionModelManager data;
	private ISettingsReader settings;

	public SpaceRenderer(ISettingsReader settings, AndroidCollectionModelManager data,
			ImportState importState, Context context, SpaceActivity spaceActivity) {
		super(data, importState, context);
		this.spaceActivity = spaceActivity;
		this.data = data;
		this.settings = settings;

		Collection<MapAlbum> mapAlbums;
		try {
			mapAlbums = data.getAlbumProvider().getAllMapAlbums();
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			mapAlbums = new ArrayList<MapAlbum>();
		}
		Log.v(TAG, "Got " + mapAlbums.size() + " mapAlbums");
		createGlAlbums(mapAlbums, false);
		Log.v(TAG, "Created GL albums");
		setPosition(settings);
		// List<MapTag> mapTags =
		// data.getMostRelevantTagsBlocking(NUM_MAP_TAGS);
		List<MapTag> mapTags = data.getTagProvider().getAllMapTags();
		createGlTags(mapTags, false);
		Log.v(TAG, "Created " + mapTags.size() + " GlTags");
		highlightCurrentAlbum = settings.isCurrentAlbumHighlighted();
		glHighlight = new GlHighlight(0, 0, 0, false, spaceActivity);
		// camera.setCameraPosition(0, CAMERA_HEIGHT, 0, false);
		camera.setYBorders(0, camera.getRearClippingPlane());
	}

	private void setPosition(ISettingsReader settings) {

		if (settings.isGotoCurrentAlbumEnabled()) {
			MapAlbum album = spaceActivity.getCurrentAlbum();
			if (album != null) {
				goToAlbum(album);
				Log.v(TAG, "Set camera to album: " + album.getName());
				return;
			}
		}
		float posX = settings.getLastPositionInPcaMapX();
		float posZ = settings.getLastPositionInPcaMapY() + 1.5f * camera.getFrontClippingPlane();
		// First set map to last position
		camera.setCameraPosition(posX, SpaceRenderer.CAMERA_HEIGHT, posZ, false);
		Log.v(TAG, "Set camera to position x: " + posX + " y: " + posZ);
	}

	@Override
	public void drawFrame(GL10 gl) {
		super.drawFrame(gl);

		// log("Begin draw");

		float camPosZ = getCamera().getPosZ();
		float camPosX = getCamera().getPosX();
		float camPosY = getCamera().getPosY();

		// log("Before update cam");

		updateModelViewWithCamera(gl, camPosZ, camPosX, camPosY);

		// log("After update cam");

		float minDistToCenter = Float.MAX_VALUE;

		// log("Before selection");

		if (doSelection) {
			doSelection(gl, camPosZ, camPosX, minDistToCenter);
		}

		// log("After selection");

		doRegionPlaylist(gl);

		drawAlbums(gl, camPosZ, camPosX, minDistToCenter);

	}

	// private void log(String msg) {
	// long currentTime = System.currentTimeMillis();
	// Log.v(TAG, msg + " t: " + (currentTime - lastLogTime));
	// lastLogTime = currentTime;
	// }

	private void doSelection(GL10 gl, float camPosZ, float camPosX, float minDistToCenter) {

		Log.v(TAG, "doSelection()");
		selectedAlbum = null;

		clickPos[1] = getViewHeight() - clickPos[1];
		ByteBuffer result = ByteBuffer.allocate(4);

		float distantBorder = camPosZ - camera.getRearClippingPlane();
		float closeBorder = camPosZ - camera.getFrontClippingPlane();

		drawAlbumsInSelectionMode(gl, distantBorder, closeBorder);

		gl.glReadPixels((int) clickPos[0], (int) clickPos[1], 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, result);
		int clickRed = result.get(0) & 0xFF;
		int clickGreen = result.get(1) & 0xFF;
		int clickBlue = result.get(2) & 0xFF;
		if (clickRed == 0 && clickGreen == 0 && clickBlue == 0) {
			doSelection = false;
			Log.v(TAG, "Clicked in empty space");
			return;
		}

		getSelection(clickRed, clickGreen, clickBlue, distantBorder, closeBorder);

		doSelection = false;
	}

	private void getSelection(int clickRed, int clickGreen, int clickBlue, float distantBorder, float closeBorder) {
		int red = 25;
		int green = 25;
		int blue = 25;
		float minDist = Float.MAX_VALUE;
		for (GlAlbumCover glAlbumCover : albums) {

			MapAlbum mapAlbum = glAlbumCover.getMapAlbum();
			if (mapAlbum.getGridCoords()[1] < distantBorder) {
				continue;
			}
			if (mapAlbum.getGridCoords()[1] > closeBorder) {
				break;
			}
			float distance = getColorDistance(red, clickRed, green, clickGreen, blue, clickBlue);
			if (distance < minDist) {
				minDist = distance;
				selectedAlbum = mapAlbum;
			}
			red += 25;
			if (red > 250) {
				red = 25;
				green += 25;
				if (green > 250) {
					green = 25;
					blue += 25;
					if (blue > 250) {
						blue = 25;
						red = 25;
					}
				}
			}
		}
	}

	private void drawAlbumsInSelectionMode(GL10 gl, float distantBorder, float closeBorder) {
		int red = 25;
		int green = 25;
		int blue = 25;

		for (GlAlbumCover glAlbumCover : albums) {

			MapAlbum mapAlbum = glAlbumCover.getMapAlbum();
			if (mapAlbum.getGridCoords()[1] < distantBorder) {
				continue;
			}
			if (mapAlbum.getGridCoords()[1] > closeBorder) {
				break;
			}

			glAlbumCover.drawForSelection(gl, red, green, blue);
			red += 25;
			if (red > 250) {
				red = 25;
				green += 25;
				if (green > 250) {
					green = 25;
					blue += 25;
					if (blue > 250) {
						blue = 25;
						red = 25;
					}
				}
			}
		}
	}

	private float getColorDistance(float red, int clickRed, float green, int clickGreen, float blue, int clickBlue) {
		return (red - clickRed) * (red - clickRed) + (green - clickGreen) * (green - clickGreen) + (blue - clickBlue)
				* (blue - clickBlue);
	}

	private void updateModelViewWithCamera(GL10 gl, float camPosZ, float camPosX, float camPosY) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		GLU.gluLookAt(gl, camPosX, camPosY, camPosZ, camPosX, camPosY - 0.2f, camPosZ - 1, 0f, 1.0f, 0f);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	private void drawAlbums(GL10 gl, float camPosZ, float camPosX, float minDistToCenter) {
		float distantBorder = camPosZ - camera.getRearClippingPlane();
		float closeBorder = camPosZ - camera.getFrontClippingPlane();
		GlAlbumCover albumForCoverLoad = null;
		// if (tags.size() == 0) {
		// return;
		// }
		Iterator<GlTagLabel> tagIterator = tags.iterator();
		GlTagLabel currentTag = null;
		if (tags.size() > 0) {
			currentTag = tagIterator.next();
		}
		for (GlAlbumCover glAlbumCover : albums) {

			MapAlbum mapAlbum = glAlbumCover.getMapAlbum();
			if (mapAlbum.getGridCoords()[1] < distantBorder) {
				// Log.v(TAG, "not drawing album at: z: " +
				// mapAlbum.getGridCoords()[1]);
				continue;
			}
			if (mapAlbum.getGridCoords()[1] > closeBorder) {
				// Log.v(TAG, "not drawing album at: z: " +
				// mapAlbum.getGridCoords()[1]);
				break;
			}
			while (tags.size() > 0 && tagIterator.hasNext() && currentTag.getPosZ() < mapAlbum.getGridCoords()[1]) {
				drawTag(currentTag, camPosZ, gl, distantBorder, closeBorder);
				currentTag = tagIterator.next();
			}

			if (!glAlbumCover.isTextureLoaded()) {
				float diffX = camPosX - mapAlbum.getGridCoords()[0];
				float diffZ = camPosZ - mapAlbum.getGridCoords()[1];
				if (diffX < 0) {
					diffX = -diffX;
				}
				if (diffZ < 0) {
					diffZ = -diffZ;
				}
				float totalDiff = diffX + diffZ;
				if (totalDiff < minDistToCenter) {
					minDistToCenter = totalDiff;
					albumForCoverLoad = glAlbumCover;
				}
			}

			if (highlightCurrentAlbum && spaceActivity.getCurrentAlbum() != null
					&& spaceActivity.getCurrentAlbum().getId() == mapAlbum.getId()) {
				drawHighlight(gl);
			}
			// Log.v(TAG, "drawing albums");
			glAlbumCover.draw(gl, true);
		}
		setAlbumToLoadAlbumArt(albumForCoverLoad);
	}

	private void drawTag(GlTagLabel currentTag, float camPosZ, GL10 gl, float distantBorder, float closeBorder) {
		if (currentTag.getPosZ() < distantBorder || currentTag.getPosZ() > closeBorder) {
			return;
		}
		currentTag.draw(gl);
	}

	private void drawHighlight(GL10 gl) {
		// Maybe draw a highlight
		if (highlightCurrentAlbum && spaceActivity.getCurrentAlbum() != null) {
			MapAlbum album = spaceActivity.getCurrentAlbum();
			glHighlight.setCoords(album.getGridCoords()[0], 0f, album.getGridCoords()[1] - 0.01f);
			glHighlight.draw(gl, albumTextures[0]);
			// Log.v(TAG, "drawn highlight");
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

	public LinkedList<KdTreePoint<GlAlbumCover>> getVisibleAlbums() {
		return visibleAlbums;
	}

	public void onResume() {
		for (GlAlbumCover albumCover : albums) {
			albumCover.resetTexture();
		}
		for (GlTagLabel tag : tags) {
			tag.resetTexture();
		}
		glHighlight = new GlHighlight(0, 0, 0, false, spaceActivity);
	}

	public void onPause() {
		if (albumArtTextureLoader != null) {
			albumArtTextureLoader.terminate();
		}
	}

	public BaseAlbum getSelection(float touchPosX, float touchPosY) {
		doSelection = true;
		clickPos = new float[] { touchPosX, touchPosY };
		while (doSelection == true) {
			try {
				JoinableThread.sleep(50);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
		return selectedAlbum;
	}

	public void newCurrentAlbum(MapAlbum currentAlbum) {
		if (settings.isGotoCurrentAlbumEnabled()) {
			MapAlbum album = spaceActivity.getCurrentAlbum();
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
			camera.setCameraPosition(mapAlbum.getGridCoords()[0], CAMERA_HEIGHT, mapAlbum.getGridCoords()[1] + 1.5f
					* camera.getFrontClippingPlane(), false);
			Log.v(TAG, "Set camera to album: " + album.getName());
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

}
