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

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;

public class AlbumArtTextureLoader extends JoinableThread {

	private static final String TAG = AlbumArtTextureLoader.class.getSimpleName();
	private boolean mDone;
	private HashMap<GlAlbumCover, Integer> currentlyLoadedCovers;
	private LinkedList<Integer> availableTextureIds;
	private BaseAlbumRenderer mapRenderer;
	private AndroidCollectionModelManager data;

	public AlbumArtTextureLoader(BaseAlbumRenderer mapRenderer, AndroidCollectionModelManager data, int[] textureIds) {
		super();
		this.mapRenderer = mapRenderer;
		this.data = data;
		mDone = false;
		currentlyLoadedCovers = new HashMap<GlAlbumCover, Integer>();
		availableTextureIds = new LinkedList<Integer>();
		for (int i = 0; i < textureIds.length; i++) {
			availableTextureIds.add(textureIds[i]);
		}
		Log.v(TAG, "AlbumArtTexture loaded with textureId array of size " + textureIds.length);
	}

	/**
	 * The thread function calling the cover load function
	 */
	@Override
	public void run() {
		while (!mDone) {

			loadCovers();
			try {
				JoinableThread.sleep(20);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
	}

	public void terminate() {
		mDone = true;
	}

	/**
	 * Cover loading function for the Plane space
	 */
	private void loadCovers() {

		// logCurrentlyLoadedCovers();

		GlAlbumCover currentAlbumToLoad = mapRenderer.getAlbumArtToLoad();
		if (currentAlbumToLoad == null || currentlyLoadedCovers.containsKey(currentAlbumToLoad)) {
			return;
		}

		float camPosX = mapRenderer.getCamera().getPosX();
		float camPosZ = mapRenderer.getCamera().getPosZ();
		float distance = -1;
		GlAlbumCover farthestAlbum = null;
		if (availableTextureIds.size() > 0) {
			loadAlbumArtToTextureId(currentAlbumToLoad, availableTextureIds.removeFirst());
			return;
		}
		for (GlAlbumCover album : currentlyLoadedCovers.keySet()) {
			float diffX = camPosX - album.getMapAlbum().getGridCoords()[0];
			float diffZ = camPosZ - album.getMapAlbum().getGridCoords()[1];
			float temp = Math.abs(diffX) + Math.abs(diffZ);
			// Check if album is visible
			if (temp > distance) {
				farthestAlbum = album;
				distance = temp;
			}
		}
		if (farthestAlbum == null) {
			return;
		}
		float diffX = camPosX - currentAlbumToLoad.getMapAlbum().getGridCoords()[0];
		float diffZ = camPosZ - currentAlbumToLoad.getMapAlbum().getGridCoords()[1];
		float temp = Math.abs(diffX) + Math.abs(diffZ);
		if (temp < distance) {
			replaceAlbumArt(currentAlbumToLoad, farthestAlbum);
		}
	}

	// private void logCurrentlyLoadedCovers() {
	// for (Entry<GlAlbumCover, Integer> e : currentlyLoadedCovers.entrySet()) {
	// Log.v(TAG, "Mapping album " + e.getKey().getMapAlbum().getName() +
	// " to textureId " + e.getValue()
	// + " (id in album: " + e.getKey().getTextureId() + ")");
	// }
	// Log.v(TAG, "===================================");
	//
	// }

	private void replaceAlbumArt(GlAlbumCover albumToLoad, GlAlbumCover albumToReplace) {
		// Log.v(TAG, "Replace " + albumToReplace.getMapAlbum().getName()
		// + " with " + albumToLoad.getMapAlbum().getName());
		removeAlbumArt(albumToReplace);
		if (availableTextureIds.size() > 0) {
			int textureId = availableTextureIds.removeFirst();
			loadAlbumArtToTextureId(albumToLoad, textureId);
		} else {
			throw new NullPointerException("availableTextureIds.size() == 0");
		}
	}

	private void removeAlbumArt(GlAlbumCover albumToReplace) {
		albumToReplace.resetTexture();
		Integer textureId = currentlyLoadedCovers.remove(albumToReplace);
		// if (textureId == null)
		// Log.v(TAG, "textureId is null");
		if (textureId != null) {
			availableTextureIds.add(textureId);
		} else {
			throw new NullPointerException("textureId == null");
		}
	}

	private void loadAlbumArtToTextureId(GlAlbumCover albumToLoad, int textureId) {
		try {
			// if (textureId == 0) Log.v(TAG, "Loading textureId 0. ava: " +
			// availableTextureIds.size());
			Bitmap bitmap = data.getAlbumArtProvider().getAlbumArt(albumToLoad.getMapAlbum(), true);
			albumToLoad.loadTexture(bitmap, textureId);
			currentlyLoadedCovers.put(albumToLoad, textureId);
		} catch (NoAlbumArtException e) {
			Log.w(TAG, e);
		}
	}

}
