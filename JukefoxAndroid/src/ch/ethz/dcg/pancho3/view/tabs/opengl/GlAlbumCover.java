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

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLUtils;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;

public class GlAlbumCover {

	private final static int VERTS = 4;
	public final static float COVER_SIZE = 0.35f;
	@SuppressWarnings("unused")
	private static final String TAG = GlAlbumCover.class.getSimpleName();

	private FloatBuffer mFVertexBuffer;
	private FloatBuffer mTexBuffer;
	private ShortBuffer mIndexBuffer;
	private boolean horizontal = false;

	private boolean textureLoaded = false;
	private int textureId = -1;

	private boolean loadTextureBitmap = false;
	private Bitmap textureBitmap;

	private MapAlbum mapAlbum;

	public GlAlbumCover(MapAlbum mapAlbum, boolean horizontal) {
		this.mapAlbum = mapAlbum;
		this.horizontal = horizontal;

		float x = mapAlbum.getGridCoords()[0];
		float y = mapAlbum.getGridCoords()[1];

		// Log.v(TAG, "Map album with coords X. " + x + " y: " + y);

		createBuffers();

		float coords[] = null;

		if (horizontal) {
			coords = getHorizontalCoords(x, 0, y);
		} else {
			coords = getVerticalCoords(x, 0, y);
		}

		fillVertexBuffer(coords);
		fillTextureBuffer();
		fillIndexBuffer();

		mFVertexBuffer.position(0);
		mTexBuffer.position(0);
		mIndexBuffer.position(0);
		// Log.v(TAG, "Returned map album with color " +
		// Integer.toHexString(mapAlbum.getColor()) + " r: " +
		// Color.red(mapAlbum.getColor()) + " g: " +
		// Color.green(mapAlbum.getColor()));
	}

	private float[] getVerticalCoords(float x, float y, float z) {
		float[] coords = {
				// x,y,z
				x - COVER_SIZE, y - COVER_SIZE, z, x + COVER_SIZE, y - COVER_SIZE, z, x - COVER_SIZE, y + COVER_SIZE,
				z, x + COVER_SIZE, y + COVER_SIZE, z

		};
		return coords;
	}

	private float[] getHorizontalCoords(float x, float y, float z) {
		float[] coords = {
				// x,y,z
				x - 2 * COVER_SIZE, y, z + 2 * COVER_SIZE, x + 2 * COVER_SIZE, y, z + 2 * COVER_SIZE,
				x - 2 * COVER_SIZE, y, z - 2 * COVER_SIZE, x + 2 * COVER_SIZE, y, z - 2 * COVER_SIZE

		};
		return coords;
	}

	private void createBuffers() {
		// Buffers to be passed to gl*Pointer() functions
		// must be direct, i.e., they must be placed on the
		// native heap where the garbage collector cannot
		// move them.
		//
		// Buffers with multi-byte datatypes (e.g., short, int, float)
		// must have their byte order set to native order

		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mFVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTexBuffer = tbb.asFloatBuffer();

		ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();
	}

	private void fillTextureBuffer() {
		mTexBuffer.put(0f);
		mTexBuffer.put(1f);

		mTexBuffer.put(1f);
		mTexBuffer.put(1f);

		mTexBuffer.put(0f);
		mTexBuffer.put(0f);

		mTexBuffer.put(1f);
		mTexBuffer.put(0f);
	}

	private void fillIndexBuffer() {
		for (int i = 0; i < VERTS; i++) {
			mIndexBuffer.put((short) i);
		}
	}

	private void fillVertexBuffer(float[] coords) {
		for (int i = 0; i < VERTS; i++) {
			for (int j = 0; j < 3; j++) {
				mFVertexBuffer.put(coords[i * 3 + j] * 1.0f);
			}
		}
	}

	/**
	 * draw the cover
	 * 
	 * @param gl
	 *            the OpenGl object
	 */
	public void draw(GL10 gl, boolean drawTexture) {

		if (loadTextureBitmap) {
			loadBitmapForGl(gl);
		}

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
		if (drawTexture && textureLoaded) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
		} else {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
			gl.glColor4f(Color.red(mapAlbum.getColor()) / 255f, Color.green(mapAlbum.getColor()) / 255f, Color
					.blue(mapAlbum.getColor()) / 255f, 1f);
		}

		gl.glFrontFace(GL10.GL_CCW);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
	}

	private void loadBitmapForGl(GL10 gl) {
		// Log.v(TAG, "loading Bitmap for album " + mapAlbum.getName());
		loadTextureBitmap = false;
		if (textureBitmap != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textureBitmap, 0);
			textureLoaded = true;
			textureBitmap = null;
		}
	}

	public void loadTexture(Bitmap bitmap, int textureId) {
		// Log.v(TAG, "Set to load texture at " + textureId + " for "
		// + mapAlbum.getName());
		textureBitmap = bitmap;
		this.textureId = textureId;
		loadTextureBitmap = true;
	}

	public void moveCover(float dX, float dY, float dZ) {

		mFVertexBuffer.put(0, mFVertexBuffer.get(0) + dX);
		mFVertexBuffer.put(3, mFVertexBuffer.get(3) + dX);
		mFVertexBuffer.put(6, mFVertexBuffer.get(6) + dX);
		mFVertexBuffer.put(9, mFVertexBuffer.get(9) + dX);

		mFVertexBuffer.put(1, mFVertexBuffer.get(1) + dY);
		mFVertexBuffer.put(4, mFVertexBuffer.get(4) + dY);
		mFVertexBuffer.put(7, mFVertexBuffer.get(7) + dY);
		mFVertexBuffer.put(10, mFVertexBuffer.get(10) + dY);

		mFVertexBuffer.put(2, mFVertexBuffer.get(2) + dZ);
		mFVertexBuffer.put(5, mFVertexBuffer.get(5) + dZ);
		mFVertexBuffer.put(8, mFVertexBuffer.get(8) + dZ);
		mFVertexBuffer.put(11, mFVertexBuffer.get(11) + dZ);
	}

	/**
	 * Flip the label for the overview mode and back
	 */
	public void flipCover() {
		if (!horizontal) {

			float posX = mFVertexBuffer.get(0) + COVER_SIZE;
			float posY = mFVertexBuffer.get(1) + COVER_SIZE;
			float posZ = mFVertexBuffer.get(2);

			mFVertexBuffer.put(0, posX - 2 * COVER_SIZE);
			mFVertexBuffer.put(1, posY);
			mFVertexBuffer.put(2, posZ + 2 * COVER_SIZE);

			mFVertexBuffer.put(3, posX + 2 * COVER_SIZE);
			mFVertexBuffer.put(4, posY);
			mFVertexBuffer.put(5, posZ + 2 * COVER_SIZE);

			mFVertexBuffer.put(6, posX - 2 * COVER_SIZE);
			mFVertexBuffer.put(7, posY);
			mFVertexBuffer.put(8, posZ - 2 * COVER_SIZE);

			mFVertexBuffer.put(9, posX + 2 * COVER_SIZE);
			mFVertexBuffer.put(10, posY);
			mFVertexBuffer.put(11, posZ - 2 * COVER_SIZE);

			horizontal = true;

		} else {

			float posX = mFVertexBuffer.get(0) + 2 * COVER_SIZE;
			float posY = mFVertexBuffer.get(1);
			float posZ = mFVertexBuffer.get(2) - 2 * COVER_SIZE;

			mFVertexBuffer.put(0, posX - COVER_SIZE);
			mFVertexBuffer.put(1, posY - COVER_SIZE);
			mFVertexBuffer.put(2, posZ);

			mFVertexBuffer.put(3, posX + COVER_SIZE);
			mFVertexBuffer.put(4, posY - COVER_SIZE);
			mFVertexBuffer.put(5, posZ);

			mFVertexBuffer.put(6, posX - COVER_SIZE);
			mFVertexBuffer.put(7, posY + COVER_SIZE);
			mFVertexBuffer.put(8, posZ);

			mFVertexBuffer.put(9, posX + COVER_SIZE);
			mFVertexBuffer.put(10, posY + COVER_SIZE);
			mFVertexBuffer.put(11, posZ);

			horizontal = false;
		}
	}

	public MapAlbum getMapAlbum() {
		return mapAlbum;
	}

	public int getTextureId() {
		return textureId;
	}

	public boolean isTextureLoaded() {
		return textureLoaded || textureBitmap != null;
	}

	public void resetTexture() {
		loadTextureBitmap = false;
		textureBitmap = null;
		textureLoaded = false;
		textureId = 0;
	}

	public void drawForSelection(GL10 gl, int red, int green, int blue) {
		// Log.v(TAG, "Drawn selection mode for "
		// + mapAlbum.getName() + " with red " +red + " green " + green +
		// " blue " + blue);
		gl.glColor4f(red / 255f, green / 255f, blue / 255f, 1f);
		// gl.glColor4x(red, green, blue, 255);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
	}

}
