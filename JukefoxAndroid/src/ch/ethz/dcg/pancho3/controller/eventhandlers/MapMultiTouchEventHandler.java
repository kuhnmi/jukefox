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
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;

public class MapMultiTouchEventHandler {

	private float multiTouchPosX1 = -1;
	private float multiTouchPosX2 = -1;
	private float multiTouchPosY1 = -1;
	private float multiTouchPosY2 = -1;
	private float multiTouchDist = -1;
	private float startCamPosY = -1;
	private float startCamPosX = -1;
	private float startCamPosZ = -1;

	private float[] targetCamPosXY = null;

	private boolean inMultiTouch = false;
	private final MapRenderer mapRenderer;
	private final MapEventListener mapEventListener;
	private final boolean allowMultiTouchPanning;

	public MapMultiTouchEventHandler(MapRenderer mapRenderer, MapEventListener mapEventListener,
			boolean allowMultiTouchPanning) {
		this.mapRenderer = mapRenderer;
		this.mapEventListener = mapEventListener;
		this.allowMultiTouchPanning = allowMultiTouchPanning;
	}

	public boolean handleEvent(MotionEvent event) {
		if (event.getPointerCount() <= 1) {
			if (inMultiTouch) { // before we had two touch points
				// set start position of single touch event handler such that
				// the camera position keeps the same.
				mapEventListener.multiTouchFinished(event);
				inMultiTouch = false;
				return true;
			}

			inMultiTouch = false;
			// Let the normal touch handler handle the event
			return false;
		}

		// now we are in a multitouch event => set time used above
		inMultiTouch = true;
		mapEventListener.cancelRegionSelectHoldTimeTimer();

		if (event.getAction() == MotionEvent.ACTION_POINTER_1_DOWN
				|| event.getAction() == MotionEvent.ACTION_POINTER_2_DOWN) {
			// persHelper.mEventHandler.setClickTime(System.currentTimeMillis());

			multiTouchPosX1 = event.getX(0);
			multiTouchPosY1 = event.getY(0);
			multiTouchPosX2 = event.getX(1);
			multiTouchPosY2 = event.getY(1);
			float diffX = multiTouchPosX1 - multiTouchPosX2;
			float diffY = multiTouchPosY1 - multiTouchPosY2;
			startCamPosY = mapRenderer.getCamera().getPosY(); // height
			startCamPosX = mapRenderer.getCamera().getPosX();
			startCamPosZ = mapRenderer.getCamera().getPosZ();

			multiTouchDist = (float) Math.sqrt(diffX * diffX + diffY * diffY);

			float[] screenXY = new float[2];
			screenXY[0] = (event.getX(0) + event.getX(1)) / 2;
			screenXY[1] = (event.getY(0) + event.getY(1)) / 2;
			targetCamPosXY = getModelCoordsXY(screenXY, startCamPosX, startCamPosY, startCamPosZ);
		}

		float dX = event.getX(0) - event.getX(1);
		float dY = event.getY(0) - event.getY(1);
		float dist = (float) Math.sqrt(dX * dX + dY * dY);

		float ratio = multiTouchDist / dist;

		float newCamPosY = startCamPosY * ratio; // height

		// Do ugly offset correction
		// newCamPosY = newCamPosY -
		// activity.getMapRenderer().getCamera().getPosY();
		// newCamPosY /= 4;
		// newCamPosY += activity.getMapRenderer().getCamera().getPosZ();
		// persHelper.mPersRenderer.setZoomByZPos(newCamPosY);

		float diffX = (targetCamPosXY[0] - startCamPosX) * (1 - ratio);
		float diffZ = (targetCamPosXY[1] - startCamPosZ) * (1 - ratio);
		// persHelper.mViewSettings.posX = startCamPosX + diffX;
		// persHelper.mViewSettings.posZ = startCamPosZ + diffZ;

		float deltaX = 0;
		float deltaZ = 0;

		deltaX = (event.getX(0) + event.getX(1) - multiTouchPosX1 - multiTouchPosX2) / 2;
		deltaZ = (event.getY(0) + event.getY(1) - multiTouchPosY1 - multiTouchPosY2) / 2;

		Pair<Float, Float> cameraMovementDiff = MapEventListener.GET_MOVEMENT_FACTOR(mapRenderer, deltaX, deltaZ);

		float cameraMovementDiffX = 0;
		float cameraMovementDiffY = 0;

		if (Math.abs(cameraMovementDiff.first) > 0.1 && allowMultiTouchPanning) {
			cameraMovementDiffX = cameraMovementDiff.first;
		}
		if (Math.abs(cameraMovementDiff.second) > 0.1 && allowMultiTouchPanning) {
			cameraMovementDiffY = cameraMovementDiff.second;
		}
		mapRenderer.getCamera().setCameraPosition(startCamPosX + diffX + cameraMovementDiffX, newCamPosY,
				startCamPosZ + diffZ + cameraMovementDiffY, false);
		return true;
	}

	public float[] getModelCoordsXY(float[] p, float camPosX, float camPosY, float camPosZ) {
		float widthFactor = mapRenderer.getViewRatio() / mapRenderer.getCamera().getFrontClippingPlane();
		float heightFactor = 1f / mapRenderer.getCamera().getFrontClippingPlane();
		float screenMinX = camPosX - camPosY * widthFactor;
		float screenMaxX = camPosX + camPosY * widthFactor;
		float screenMinZ = camPosZ - camPosY * heightFactor;
		float screenMaxZ = camPosZ + camPosY * heightFactor;

		float[] coords = new float[2];
		coords[0] = screenMinX + (screenMaxX - screenMinX) / mapRenderer.getViewWidth() * p[0];
		coords[1] = screenMinZ + (screenMaxZ - screenMinZ) / mapRenderer.getViewHeight() * p[1];

		return coords;
	}
}
