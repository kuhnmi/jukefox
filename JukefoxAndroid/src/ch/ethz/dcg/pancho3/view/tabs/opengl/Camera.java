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

import java.util.LinkedList;

import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;

public class Camera {

	@SuppressWarnings("unused")
	private static final String TAG = Camera.class.getSimpleName();

	protected static final float REAR_CLIPPING_PLANE = 200;
	protected static final float FRONT_CLIPPING_PLANE = 3f;
	private static final int KINETIC_SCROLL_BUFFER_SIZE = 7;

	private float posX;
	private float posY;
	private float posZ;

	private float borderMinX;
	private float borderMinY;
	private float borderMinZ;

	private float borderMaxX;
	private float borderMaxY;
	private float borderMaxZ;

	private float speedX;
	private float speedY;
	private float speedZ;

	private float startSpeedX;
	private float startSpeedY;
	private float startSpeedZ;

	// private long lastSetPositionTime;
	private long startKineticMotionTime;
	private long lastMovementTime;
	private long maxMotionTime = 1000;

	private LinkedList<KineticScrollEntry> kineticScrollBuffer;
	private KineticScrollEntry kineticScrollStart;

	private boolean grasped;

	private class KineticScrollEntry {

		private final long time;
		private final float[] pos;

		public KineticScrollEntry(float[] pos, long time) {
			this.time = time;
			this.pos = pos;
		}

		private float[] getPos() {
			return pos;
		}

		private long getTime() {
			return time;
		}
	}

	public Camera() {
		borderMaxY = REAR_CLIPPING_PLANE - 1;
		borderMinY = FRONT_CLIPPING_PLANE + 1;
		kineticScrollBuffer = new LinkedList<KineticScrollEntry>();
	}

	public void updatePosition(long currentTime) {

		if (grasped) {
			return;
		}

		float timeDiff = currentTime - lastMovementTime;
		float startTimeDiff = currentTime - startKineticMotionTime;
		lastMovementTime = currentTime;

		float oldPosX = posX;
		float oldPosY = posY;
		float oldPosZ = posZ;

		posX += speedX * timeDiff;
		posY += speedY * timeDiff;
		posZ += speedZ * timeDiff;

		if (Float.isNaN(posX)) {
			posX = oldPosX;
			if (Float.isInfinite(posX) || Float.isNaN(posX)) {
				posX = 0;
			}
		}
		if (Float.isNaN(posY)) {
			posY = oldPosY;
			if (Float.isInfinite(posY) || Float.isNaN(posY)) {
				posY = 0;
			}
		}
		if (Float.isNaN(posZ)) {
			posZ = oldPosZ;
			if (Float.isInfinite(posZ) || Float.isNaN(posZ)) {
				posZ = 0;
			}
		}
		// Log.v(TAG, "Camera speed: x: " + speedX + " y: " + speedY + " z: " +
		// speedZ);
		// Log.v(TAG, "Camera position: x: " + posX + " y: " + posY + " z: " +
		// posZ);
		adjustPositionToBorders();

		float damping = 1f - startTimeDiff / maxMotionTime;
		damping = Math.max(0f, damping);

		speedX = startSpeedX * 1f * damping;
		speedY = startSpeedY * 1f * damping;
		speedZ = startSpeedZ * 1f * damping;

		// if (Math.abs(speedX) > 1) speedX = 0;
		// if (Math.abs(speedY) > 1) speedY = 0;
		// if (Math.abs(speedZ) > 1) speedZ = 0;

		if (startTimeDiff > maxMotionTime) {
			stopMotion();
		}

	}

	private void adjustPositionToBorders() {
		if (posX < borderMinX) {
			posX = borderMinX;
		}
		if (posZ < borderMinZ) {
			posZ = borderMinZ;
		}
		if (posX > borderMaxX) {
			posX = borderMaxX;
		}
		if (posZ > borderMaxZ + 2 * FRONT_CLIPPING_PLANE) {
			posZ = borderMaxZ + 2 * FRONT_CLIPPING_PLANE;
		}
		if (posY < borderMinY) {
			posY = borderMinY;
		}
		if (posY > borderMaxY) {
			posY = borderMaxY;
		}
	}

	public void stopMotion() {
		startSpeedX = 0;
		startSpeedY = 0;
		startSpeedZ = 0;
		speedX = 0;
		speedY = 0;
		speedZ = 0;
		kineticScrollBuffer.clear();
	}

	// public void setKineticSpeed(float speedX, float speedY, float speedZ) {
	//    	
	// startKineticMotionTime = System.currentTimeMillis();
	//    	    	
	// startSpeedX = speedX;
	// startSpeedY = speedY;
	// startSpeedZ = speedZ;
	// this.speedX = speedX;
	// this.speedY = speedY;
	// this.speedZ = speedZ;
	// }

	public float getFrontClippingPlane() {
		return FRONT_CLIPPING_PLANE;
	}

	public float getRearClippingPlane() {
		return REAR_CLIPPING_PLANE;
	}

	public float getPosX() {
		return posX;
	}

	public float getPosY() {
		return posY;
	}

	public float getPosZ() {
		return posZ;
	}

	public void setCameraPosition(float posX, float posY, float posZ, boolean kineticMovement) {
		if (kineticMovement) {
			updateKineticSpeed(posX, posY, posZ);
		}
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		adjustPositionToBorders();

	}

	private void updateKineticSpeed(float posX, float posY, float posZ) {
		// long currentTime = System.currentTimeMillis();
		// int timeDiff = (int) (currentTime-lastSetPositionTime);
		// startSpeedX = (posX -this.posX)/timeDiff;
		// startSpeedY = (posY -this.posY)/timeDiff;
		// startSpeedZ = (posZ -this.posZ)/timeDiff;
		// startKineticMotionTime = currentTime;
		// lastSetPositionTime = currentTime;
		// // Log.v(TAG, "Set kinetic speed to " + startSpeedX + " " +
		// startSpeedZ);

		long time = System.currentTimeMillis();
		float[] pos = new float[] { posX, posY, posZ };
		KineticScrollEntry kse = new KineticScrollEntry(pos, time);
		if (kineticScrollBuffer.size() == 0) {
			kineticScrollStart = kse;
		}
		kineticScrollBuffer.add(kse);
		while (kineticScrollBuffer.size() > KINETIC_SCROLL_BUFFER_SIZE) {
			kineticScrollBuffer.removeFirst();
		}

		KineticScrollEntry kseLast = kineticScrollBuffer.getLast();
		KineticScrollEntry kseFirst = kineticScrollBuffer.getFirst();

		float[] v1 = getSpeed(kineticScrollStart, kseLast);
		float[] v2 = getSpeed(kseFirst, kseLast);

		float absV1 = AndroidUtils.norm2(v1);
		float absV2 = AndroidUtils.norm2(v2);

		float[] v;
		if (kineticScrollBuffer.size() >= 2) {
			KineticScrollEntry kseSecondLast = kineticScrollBuffer.get(kineticScrollBuffer.size() - 2);
			v = getSpeed(kseSecondLast, kseLast);
		} else {
			v = new float[] { 0, 0, 0 };
		}
		// float absV = (float)Math.sqrt(Math.pow(v1[0], 2) + Math.pow(v1[1],
		// 2));
		// Log.v(TAG, "absV: " + absV);

		if (absV1 > 1.1 * absV2) {
			startSpeedX = 0f;
			startSpeedY = 0f;
			startSpeedZ = 0f;
		} else {
			startSpeedX = v[0];
			startSpeedY = v[1];
			startSpeedZ = v[2];
		}

		startKineticMotionTime = time;
	}

	private float[] getSpeed(KineticScrollEntry kseStart, KineticScrollEntry kseEnd) {
		int timeDiff = (int) (kseEnd.getTime() - kseStart.getTime());
		float[] v = new float[3];
		v[0] = (kseEnd.getPos()[0] - kseStart.getPos()[0]) / timeDiff;
		v[1] = (kseEnd.getPos()[1] - kseStart.getPos()[1]) / timeDiff;
		v[2] = (kseEnd.getPos()[2] - kseStart.getPos()[2]) / timeDiff;
		return v;
	}

	public void setXBorders(float minX, float maxX) {
		this.borderMinX = minX;
		this.borderMaxX = maxX;
		adjustPositionToBorders();
	}

	public void setYBorders(float minY, float maxY) {
		this.borderMinY = minY;
		this.borderMaxY = maxY;
		adjustPositionToBorders();
	}

	public void setZBorders(float minZ, float maxZ) {
		this.borderMinZ = minZ;
		this.borderMaxZ = maxZ;
		adjustPositionToBorders();
	}

	public boolean isGrasped() {
		return grasped;
	}

	public void setGrasped(boolean grasped) {
		this.grasped = grasped;
		if (grasped) {
			stopMotion();
		}
	}

}
