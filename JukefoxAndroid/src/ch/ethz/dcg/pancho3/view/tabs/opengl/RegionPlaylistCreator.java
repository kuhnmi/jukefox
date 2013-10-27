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
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import ch.ethz.dcg.jukefox.model.collection.MapAlbum;

public class RegionPlaylistCreator {

	@SuppressWarnings("unused")
	private static final String TAG = RegionPlaylistCreator.class.getCanonicalName();
	private final MapRenderer mapRenderer;
	private final OnRegionCreatedListener onRegionCreatedListener;

	private int verts = 1;

	private FloatBuffer mFVertexBuffer;
	private ShortBuffer mIndexBuffer;
	private List<Float> coords;
	private boolean create = false;

	public static interface OnRegionCreatedListener {

		void onRegionCreated(List<MapAlbum> albumsInRegion);
	}

	public RegionPlaylistCreator(MapRenderer mapRenderer, OnRegionCreatedListener onRegionCreatedListener,
			float screenPosX, float screenPosY) {
		this.mapRenderer = mapRenderer;
		this.onRegionCreatedListener = onRegionCreatedListener;

		float[] mapCoords = transformScreenToMapCoordinates(screenPosX, screenPosY);

		coords = new ArrayList<Float>();

		ByteBuffer vbb = ByteBuffer.allocateDirect(verts * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mFVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer ibb = ByteBuffer.allocateDirect(verts * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();

		coords.add(mapCoords[0]);
		coords.add(1f);
		coords.add(mapCoords[1]);

		mFVertexBuffer.put(mapCoords[0]);
		mFVertexBuffer.put(1f);
		mFVertexBuffer.put(mapCoords[1]);

		for (int i = 0; i < verts; i++) {
			mIndexBuffer.put((short) i);
		}

		mFVertexBuffer.position(0);
		mIndexBuffer.position(0);
	}

	public float[] transformScreenToMapCoordinates(float screenPosX, float screenPosY) {
		float coverSize = GlAlbumCover.COVER_SIZE;

		float camPosZ = mapRenderer.getCamera().getPosZ();
		float camPosX = mapRenderer.getCamera().getPosX();
		float camPosY = mapRenderer.getCamera().getPosY();
		float widthFactor = mapRenderer.getViewRatio() / mapRenderer.getCamera().getFrontClippingPlane();
		float heightFactor = 1f / mapRenderer.getCamera().getFrontClippingPlane();
		float visibleMinX = camPosX - camPosY * widthFactor - 2 * coverSize;
		float visibleMaxX = camPosX + camPosY * widthFactor + 2 * coverSize;
		float visibleMinZ = camPosZ - camPosY * heightFactor - 2 * coverSize;
		float visibleMaxZ = camPosZ + camPosY * heightFactor + 2 * coverSize;
		float posX = visibleMinX + (visibleMaxX - visibleMinX) * screenPosX / mapRenderer.getViewWidth();
		float posZ = visibleMinZ + (visibleMaxZ - visibleMinZ) * screenPosY / mapRenderer.getViewHeight();
		return new float[] { posX, posZ };
	}

	public void addPoint(float screenPosX, float screenPosY) {
		float[] mapCoords = transformScreenToMapCoordinates(screenPosX, screenPosY);

		ByteBuffer vbb = ByteBuffer.allocateDirect((verts + 1) * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer tempBuffer1 = vbb.asFloatBuffer();

		ByteBuffer ibb = ByteBuffer.allocateDirect((verts + 1) * 2);
		ibb.order(ByteOrder.nativeOrder());
		ShortBuffer tempBuffer2 = ibb.asShortBuffer();

		coords.add(mapCoords[0]);
		coords.add(1f);
		coords.add(mapCoords[1]);

		for (Float f : coords) {
			tempBuffer1.put(f);
		}

		for (int i = 0; i < verts + 1; i++) {
			tempBuffer2.put((short) i);
		}

		tempBuffer1.position(0);
		tempBuffer2.position(0);

		mFVertexBuffer = tempBuffer1;
		mIndexBuffer = tempBuffer2;
		verts++;
	}

	public void createPlaylist() {
		create = true;
	}

	public boolean hasToGetAlbums() {
		return create;
	}

	public void getAlbumsInPlaylist(GL10 gl) {

		float coverSize = GlAlbumCover.COVER_SIZE;

		float camPosZ = mapRenderer.getCamera().getPosZ();
		float camPosX = mapRenderer.getCamera().getPosX();
		float camPosY = mapRenderer.getCamera().getPosY();
		float widthFactor = mapRenderer.getViewRatio() / mapRenderer.getCamera().getFrontClippingPlane();
		float heightFactor = 1f / mapRenderer.getCamera().getFrontClippingPlane();
		float visibleMinX = camPosX - camPosY * widthFactor - 2 * coverSize;
		float visibleMaxX = camPosX + camPosY * widthFactor + 2 * coverSize;
		float visibleMinZ = camPosZ - camPosY * heightFactor - 2 * coverSize;
		float visibleMaxZ = camPosZ + camPosY * heightFactor + 2 * coverSize;

		// Add offset to max/min values to get real visible area
		float screenMinX = visibleMinX;// + 4*coverSize;
		float screenMaxX = visibleMaxX;// - 4*coverSize;
		float screenMinZ = visibleMinZ;// + 4*coverSize;
		float screenMaxZ = visibleMaxZ;// - 4*coverSize;

		float rangeX = screenMaxX - screenMinX;
		float rangeZ = screenMaxZ - screenMinZ;
		float width = mapRenderer.getViewWidth();
		float height = mapRenderer.getViewHeight();

		List<GlAlbumCover> albums = mapRenderer.getAlbums();
		ByteBuffer result = ByteBuffer.allocate(4);

		List<MapAlbum> albumsInRegion = new ArrayList<MapAlbum>();

		for (GlAlbumCover glAlbum : albums) {
			float[] coords = glAlbum.getMapAlbum().getGridCoords();

			float readX = (coords[0] - screenMinX) * width / rangeX;
			float readY = (-coords[1] + screenMaxZ) * height / rangeZ;

			gl.glReadPixels((int) readX, (int) readY, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, result);
			// Only check items that are on the screen
			if (readX < 0 || readX > width || readY < 0 || readY > height) {
				continue;
			}
			int res0 = result.get(0) & 0xFF;
			int res1 = result.get(1) & 0xFF;
			int res2 = result.get(2) & 0xFF;

			if (res0 > 0 && res1 > 0 && res2 > 0) {
				albumsInRegion.add(glAlbum.getMapAlbum());
			}
		}
		onRegionCreatedListener.onRegionCreated(albumsInRegion);

	}

	/**
	 * draw the Plane
	 * 
	 * @param gl
	 *            the OpenGl object
	 */
	public void draw(GL10 gl) {

		gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
		gl.glColor4f(1, 1, 1, 0.2f);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLE_FAN, verts, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
	}
}
