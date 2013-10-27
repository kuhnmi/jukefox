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

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.pancho3.view.tabs.opengl.GLView.Renderer;

public class BaseRenderer implements Renderer {

	protected static final String TAG = BaseRenderer.class.getSimpleName();
	protected int numberOfVisibleTagLabels = 100;

	protected ImportState importState;
	protected Context context;
	protected Camera camera;

	protected int viewWidth;
	protected int viewHeight;
	protected float viewRatio;

	public BaseRenderer(ImportState importState, Context context) {
		super();
		this.importState = importState;
		this.context = context;

		camera = new Camera();
	}

	@Override
	public void drawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public int[] getConfigSpec() {
		int[] configSpec = { EGL10.EGL_DEPTH_SIZE, 0, EGL10.EGL_NONE };
		return configSpec;
	}

	@Override
	public void sizeChanged(GL10 gl, int width, int height) {
		viewHeight = height;
		viewWidth = width;
		Log.v(TAG, "Width: " + viewWidth + " Height: " + viewHeight);
		if (viewHeight == 0) {
			return;
		}
		viewRatio = (float) viewWidth / viewHeight;

		gl.glViewport(0, 0, viewWidth, viewHeight);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-viewRatio, viewRatio, -1, 1, camera.getFrontClippingPlane(), camera.getRearClippingPlane());
	}

	@Override
	public void surfaceCreated(GL10 gl) {

		setGlBaseSettings(gl);

	}

	private void setGlBaseSettings(GL10 gl) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		gl.glClearColor(0, 0, 0, 1);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glEnable(GL10.GL_BLEND);

		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
	}

	public Camera getCamera() {
		return camera;
	}

	public float getViewRatio() {
		return viewRatio;
	}

	public float getViewWidth() {
		return viewWidth;
	}

	public float getViewHeight() {
		return viewHeight;
	}
}
