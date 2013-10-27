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

import android.view.MotionEvent;
import android.view.View;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.pancho3.view.tabs.SpaceActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.opengl.SpaceRenderer;

public class SpaceActivityEventListener extends MainTabButtonEventListener {

	// private static final float CLICK_DIST_THRESH = 20;

	private float lastTouchPosX;
	private float lastTouchPosY;
	private float touchDownPosX;
	private float touchDownPosY;
	private long touchDownTime;

	private boolean isKineticScrolling;
	private SpaceRenderer spaceRenderer;

	public SpaceActivityEventListener(Controller controller, SpaceActivity activity) {
		super(controller, activity, Tab.SPACE);
		spaceRenderer = activity.getSpaceRenderer();
	}

	public static final String TAG = SpaceActivityEventListener.class.getSimpleName();

	public boolean onGlTouch(View v, MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			return handleTouchDownEvent(event);
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			return handleTouchMoveEvent(event);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			return handleTouchUpEvent(event);
		}
		return true;
	}

	private boolean handleTouchUpEvent(MotionEvent event) {

		float thresh = (spaceRenderer.getViewHeight() + spaceRenderer.getViewWidth()) / 10;
		if (event.getEventTime() - touchDownTime < 300
				&& AndroidUtils.distance(lastTouchPosX, lastTouchPosY, touchDownPosX, touchDownPosY) < thresh) {
			// Log.v(TAG, "doSelection()");
			doSelection(lastTouchPosX, lastTouchPosY);
			spaceRenderer.getCamera().stopMotion();

		}

		spaceRenderer.getCamera().setGrasped(false);
		return true;
	}

	private void doSelection(float touchPosX, float touchPosY) {

		BaseAlbum album = spaceRenderer.getSelection(touchPosX, touchPosY);
		if (album == null) {
			return;
		}
		controller.doHapticFeedback();
		controller.showAlbumDetailInfo(activity, album);

	}

	private boolean handleTouchMoveEvent(MotionEvent event) {

		float diffX = event.getX() - lastTouchPosX;
		float diffY = event.getY() - lastTouchPosY;

		float dist = 8.66f;
		float camPosX = spaceRenderer.getCamera().getPosX();
		float camPosZ = spaceRenderer.getCamera().getPosZ();
		float widthFactor = spaceRenderer.getViewRatio() / spaceRenderer.getCamera().getFrontClippingPlane();
		float visibleMinX = camPosX - dist * widthFactor;
		float visibleMaxX = camPosX + dist * widthFactor;
		float rangeXX = visibleMaxX - visibleMinX;

		camPosX -= diffX / spaceRenderer.getViewWidth() * rangeXX * 1.5f;

		// mViewSettings.posY = mViewSettings.fixedPCACamY;
		camPosZ -= diffY / 40;
		spaceRenderer.getCamera().setCameraPosition(camPosX, SpaceRenderer.CAMERA_HEIGHT, camPosZ, isKineticScrolling);

		lastTouchPosX = event.getX();
		lastTouchPosY = event.getY();

		return true;
	}

	private boolean handleTouchDownEvent(MotionEvent event) {

		if (isKineticScrolling) {
			spaceRenderer.getCamera().setGrasped(true);
		}

		lastTouchPosX = event.getX();
		lastTouchPosY = event.getY();
		touchDownPosX = lastTouchPosX;
		touchDownPosY = lastTouchPosY;

		touchDownTime = event.getDownTime();

		return true;
	}

	public void setKineticMovement(boolean kineticMovement) {
		isKineticScrolling = kineticMovement;
	}

	public void onPause() {
		controller.getSettingsEditor().setLastPositionInPcaMapX(spaceRenderer.getCamera().getPosX());
		controller.getSettingsEditor().setLastPositionInPcaMapY(spaceRenderer.getCamera().getPosZ());
	}
}
