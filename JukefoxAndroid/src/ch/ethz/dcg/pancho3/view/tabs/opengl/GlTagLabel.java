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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.opengl.GLUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.MapTag;

public class GlTagLabel {

	private final static int VERTS = 4;
	private final static float LABEL_HEIGHT = 1f;
	private static final float VERT_LABEL_SIZE = 0.5f;
	private static final float HORIZ_LABEL_SIZE = 1f;
	@SuppressWarnings("unused")
	private static final String TAG = GlTagLabel.class.getSimpleName();

	private FloatBuffer mFVertexBuffer;
	private FloatBuffer mTexBuffer;
	private FloatBuffer mFVertexBuffer2;
	private FloatBuffer mTexBuffer2;
	private ShortBuffer mIndexBuffer;
	private String text;
	public boolean textureLoaded = false;
	public float zoomValue;
	public float posX;
	private float posY;
	private float posZ;
	private int ratio;
	public int[] textureId = new int[1];
	public boolean horizontal = false;
	private MapTag mapTag;

	/**
	 * Constructor
	 * 
	 * @param x
	 *            xposition of the tag label
	 * @param y
	 *            y position of the tag label
	 * @param z
	 *            z position of the tag label
	 * @param size
	 *            size of the tag label
	 * @param String
	 *            to print on the label
	 */
	public GlTagLabel(MapTag mapTag, boolean horizontal) {

		this.horizontal = horizontal;
		this.mapTag = mapTag;

		posX = mapTag.getCoordsPca2D()[0];
		posY = LABEL_HEIGHT;
		posZ = mapTag.getCoordsPca2D()[1];

		createBuffers();

		zoomValue = 10f / mapTag.getVarianceOverPCA();

		text = mapTag.getName();

		// Log.v(TAG, "created GL Tag at X: " + posX + " Z: " + posZ);

	}

	private void createBuffers() {
		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mFVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTexBuffer = tbb.asFloatBuffer();

		ByteBuffer vbb2 = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb2.order(ByteOrder.nativeOrder());
		mFVertexBuffer2 = vbb2.asFloatBuffer();

		ByteBuffer tbb2 = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb2.order(ByteOrder.nativeOrder());
		mTexBuffer2 = tbb2.asFloatBuffer();

		ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();
	}

	private float[] getHorizontalCoords(float size) {
		float[] coords = {
				// x,y,z
				posX - ratio * size, posY - size, posZ, posX + 2 * ratio * size, posY - size, posZ,
				posX - ratio * size, posY - size, posZ + 2 * size, posX + 2 * ratio * size, posY - size,
				posZ + 2 * size

		};
		return coords;
	}

	private float[] getVerticalCoords(float size) {
		// A square centered at x,y,z
		float[] coords = {
				// x,y,z
				posX - ratio * size, posY - size, posZ, posX + ratio * size, posY - size, posZ, posX - ratio * size,
				posY + size, posZ, posX + ratio * size, posY + size, posZ

		};
		return coords;
	}

	private void init(GL10 gl) {

		createBitmap(gl);

		float[] coords = null;
		if (horizontal) {
			coords = getHorizontalCoords(HORIZ_LABEL_SIZE);
		} else {
			coords = getVerticalCoords(VERT_LABEL_SIZE);
		}

		for (int i = 0; i < VERTS; i++) {
			for (int j = 0; j < 3; j++) {
				mFVertexBuffer.put(3 * i + j, coords[i * 3 + j] * 1.0f);
				// Log.v(TAG, "Set coords " + i +"," + j + "  to " +
				// mFVertexBuffer.get(3*i+j) );
			}
		}

		mTexBuffer.put(0f);
		mTexBuffer.put(1f);

		mTexBuffer.put(1f);
		mTexBuffer.put(1f);

		mTexBuffer.put(0f);
		mTexBuffer.put(0f);

		mTexBuffer.put(1f);
		mTexBuffer.put(0f);

		// Do the pole of the label
		float[] coords2 = {
				// x,y,z
				posX - 0.1f * VERT_LABEL_SIZE, posY - VERT_LABEL_SIZE, posZ, posX + 0.1f * VERT_LABEL_SIZE,
				posY - VERT_LABEL_SIZE, posZ, posX - 0.1f * VERT_LABEL_SIZE, 0, posZ, posX + 0.1f * VERT_LABEL_SIZE, 0,
				posZ

		};

		for (int i = 0; i < VERTS; i++) {
			for (int j = 0; j < 3; j++) {
				mFVertexBuffer2.put(coords2[i * 3 + j] * 1.0f);
			}
		}

		mTexBuffer2.put(0f);
		mTexBuffer2.put(1f);

		mTexBuffer2.put(1f);
		mTexBuffer2.put(1f);

		mTexBuffer2.put(0f);
		mTexBuffer2.put(0f);

		mTexBuffer2.put(1f);
		mTexBuffer2.put(0f);

		for (int i = 0; i < VERTS; i++) {
			mIndexBuffer.put((short) i);
		}

		mFVertexBuffer.position(0);
		mTexBuffer.position(0);
		mFVertexBuffer2.position(0);
		mTexBuffer2.position(0);
		mIndexBuffer.position(0);
	}

	/**
	 * create the tag label bitmap
	 * 
	 * @param tag
	 * @return
	 */
	private void createBitmap(GL10 gl) {

		int height = 20;
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.STROKE);
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setSubpixelText(true);
		paint.setAntiAlias(true);
		paint.setTextSize(height);
		float width = paint.measureText(text);

		int realWidth = 64;
		int realHeight = 32;
		if (width > realWidth) {
			while (realWidth < width) {
				realWidth *= 2;
			}
		}
		Bitmap label = null;
		try {
			label = Bitmap.createBitmap(realWidth, realHeight, Bitmap.Config.ARGB_8888);
		} catch (Error e) {
			System.gc();
			Log.w("BitmapFactory", e);
		}
		if (label != null) {
			Canvas canvas = new Canvas(label);
			canvas.drawARGB(0, 125, 125, 125);
			Paint paint2 = new Paint();
			// paint2.setARGB(255, 125, 125, 125);
			// TODO: activate transparency
			paint2.setARGB(175, 125, 125, 125);

			canvas.drawRect(0, 0, realWidth - 1, realHeight - 1, paint2);
			canvas.drawRect(0, 0, realWidth - 1, realHeight - 1, paint);
			canvas.drawRect(1, 1, realWidth - 2, realHeight - 2, paint);
			int offsetX = (realWidth - (int) width) / 2;
			canvas.drawText(text, offsetX, height, paint);

			gl.glGenTextures(1, textureId, 0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, label, 0);
			label.recycle();

			textureLoaded = true;
		}

		ratio = realWidth / realHeight;
	}

	/**
	 * Move the tag label to another position
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param size
	 */
	public void setCoords(float x, float y, float z, float size) {

		posX = x;
		posY = y;
		posZ = z;

		float[] coords = null;
		if (horizontal) {
			coords = getHorizontalCoords(size);
		} else {
			coords = getVerticalCoords(size);
		}

		for (int i = 0; i < VERTS; i++) {
			for (int j = 0; j < 3; j++) {
				mFVertexBuffer.put(i * 3 + j, coords[i * 3 + j] * 1.0f);
			}
		}
	}

	public void setSizeInHorizontalMode(float magnify) {
		if (horizontal) {
			mFVertexBuffer.put(8, posZ - 2 * magnify * HORIZ_LABEL_SIZE);

			mFVertexBuffer.put(3, posX + 2 * magnify * ratio * HORIZ_LABEL_SIZE);

			mFVertexBuffer.put(9, posX + 2 * magnify * ratio * HORIZ_LABEL_SIZE);
			mFVertexBuffer.put(11, posZ - 2 * magnify * HORIZ_LABEL_SIZE);
		}
	}

	/**
	 * draw the tag label
	 * 
	 * @param gl
	 *            the OpenGl object
	 */
	public void draw(GL10 gl) {

		if (!textureLoaded) {
			init(gl);
		}

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

		gl.glFrontFace(GL10.GL_CCW);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
		// Log.v(TAG, "draw tag: " + text + " at x: " + mFVertexBuffer.get(0) +
		// " y: " + mFVertexBuffer.get(1) + " z: " + mFVertexBuffer.get(2));

		// draw the pole only if the label is vertical
		if (!horizontal) {
			// set object color
			gl.glColor4f(1f, 1f, 1f, 1f);

			gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);

			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer2);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer2);
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
		}
	}

	public MapTag getMapTag() {
		return mapTag;
	}

	public void resetTexture() {
		textureLoaded = false;
	}

	public float getPosZ() {
		return posZ;
	}
}
