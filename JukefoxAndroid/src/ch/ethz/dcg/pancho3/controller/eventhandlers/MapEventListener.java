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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.MapActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.opengl.GlAlbumCover;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;
import ch.ethz.dcg.pancho3.view.tabs.opengl.RegionPlaylistCreator;
import ch.ethz.dcg.pancho3.view.tabs.opengl.RegionPlaylistCreator.OnRegionCreatedListener;

public class MapEventListener extends MainTabButtonEventListener implements OnRegionCreatedListener {

	private static final float CLICK_DIST_THRESH = 20;
	private static final String TAG = MapEventListener.class.getSimpleName();

	private float lastTouchPosX;
	private float lastTouchPosY;
	private float touchDownPosX;
	private float touchDownPosY;
	private boolean canBeClick;
	private long touchDownTime;
	private Timer regionSelectHoldTimeTimer;

	private float lastZoomBarTouchY;
	private float lastZoomBarMoveY;
	private long lastZoomClickTime;
	private long lastToastTime;

	private boolean isKineticScrolling;
	protected final MapRenderer mapRenderer;
	private RegionPlaylistCreator regionPlaylist;
	private boolean isCreatingRegionPlaylist;

	private MapMultiTouchEventHandler multiTouchHandler;
	private MapActivity mapActivity;

	public MapEventListener(Controller controller, JukefoxActivity activity, MapRenderer mapRenderer,
			boolean allowMultiTouchPanning) {
		super(controller, activity, Tab.MAP);
		this.mapRenderer = mapRenderer;
		if (AndroidUtils.isMultiTouchOs()) {
			multiTouchHandler = new MapMultiTouchEventHandler(mapRenderer, this, allowMultiTouchPanning);
		}
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public boolean onKey(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_CENTER:
					startCreatingRegionPlaylist();
					return true;
			}
		}
		return super.onKey(keyCode, event);
	}

	public void startCreatingRegionPlaylist() {
		isCreatingRegionPlaylist = true;
	}

	public void stopCreatingRegionPlaylist() {
		isCreatingRegionPlaylist = false;
	}

	public boolean onGlTouch(View v, MotionEvent event) {
		if (multiTouchHandler != null) {
			if (multiTouchHandler.handleEvent(event)) {
				return true;
			}
		}
		return handleSingleTouchEvent(event);
	}

	private boolean handleSingleTouchEvent(MotionEvent event) {
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
		if (isCreatingRegionPlaylist) {
			if (regionPlaylist != null) {
				regionPlaylist.createPlaylist();
				mapRenderer.stopDrawingRegionPlaylist();
			}
			isCreatingRegionPlaylist = false;
		} else if (canBeClick) {
			if (event.getEventTime() - touchDownTime < 300) { // normal click
				doSelection(lastTouchPosX, lastTouchPosY);
				mapRenderer.getCamera().stopMotion();
			}
		}

		cancelRegionSelectHoldTimeTimer();
		canBeClick = false;
		mapRenderer.getCamera().setGrasped(false);

		return true;
	}

	private void doSelection(float touchPosX, float touchPosY) {
		float camPosZ = mapRenderer.getCamera().getPosZ();
		float camPosX = mapRenderer.getCamera().getPosX();
		float camPosY = mapRenderer.getCamera().getPosY();
		float widthFactor = mapRenderer.getViewRatio() / mapRenderer.getCamera().getFrontClippingPlane();
		float heightFactor = 1f / mapRenderer.getCamera().getFrontClippingPlane();
		float screenMinX = camPosX - camPosY * widthFactor;
		float screenMaxX = camPosX + camPosY * widthFactor;
		float screenMinZ = camPosZ - camPosY * heightFactor;
		float screenMaxZ = camPosZ + camPosY * heightFactor;

		float mapTouchX = screenMinX + (screenMaxX - screenMinX) / mapRenderer.getViewWidth() * touchPosX;
		float mapTouchY = screenMinZ + (screenMaxZ - screenMinZ) / mapRenderer.getViewHeight() * touchPosY;
		List<GlAlbumCover> visibleAlbums = mapRenderer.getAlbums();
		for (GlAlbumCover glAlbum : visibleAlbums) {
			MapAlbum mapAlbum = glAlbum.getMapAlbum();
			if (mapAlbum.getGridCoords()[1] < screenMinZ) {
				continue;
			}
			if (mapAlbum.getGridCoords()[1] > screenMaxZ) {
				break;
			}
			if (mapTouchX > mapAlbum.getGridCoords()[0] - 2 * GlAlbumCover.COVER_SIZE
					&& mapTouchX < mapAlbum.getGridCoords()[0] + 2 * GlAlbumCover.COVER_SIZE
					&& mapTouchY > mapAlbum.getGridCoords()[1] - 2 * GlAlbumCover.COVER_SIZE
					&& mapTouchY < mapAlbum.getGridCoords()[1] + 2 * GlAlbumCover.COVER_SIZE) {
				Log.v("Selected", mapAlbum.getName());
				controller.doHapticFeedback();
				showAlbumDetailInfo(activity, mapAlbum);
			}
		}
	}

	protected void showAlbumDetailInfo(JukefoxActivity activity, MapAlbum mapAlbum) {
		controller.showAlbumDetailInfo(activity, mapAlbum);
	}

	private boolean handleTouchMoveEvent(MotionEvent event) {
		float diffX = event.getX() - lastTouchPosX;
		float diffY = event.getY() - lastTouchPosY;

		if (isCreatingRegionPlaylist) {
			if (Math.abs(event.getX() - lastTouchPosX) > 10 || Math.abs(event.getY() - lastTouchPosY) > 10) {
				if (regionPlaylist != null) {
					regionPlaylist.addPoint(lastTouchPosX, lastTouchPosY);
				}
			} else {
				return true;
			}

		} else {
			Pair<Float, Float> newCamPosDiff = GET_MOVEMENT_FACTOR(mapRenderer, diffX, diffY);
			mapRenderer.getCamera().setCameraPosition(mapRenderer.getCamera().getPosX() + newCamPosDiff.first,
					mapRenderer.getCamera().getPosY(), mapRenderer.getCamera().getPosZ() + newCamPosDiff.second,
					isKineticScrolling);
		}
		lastTouchPosX = event.getX();
		lastTouchPosY = event.getY();
		if (AndroidUtils.distance(lastTouchPosX, lastTouchPosY, touchDownPosX, touchDownPosY) > CLICK_DIST_THRESH) {
			canBeClick = false;
			cancelRegionSelectHoldTimeTimer();
		}
		// if (mapActivity != null && canBeClick
		// && (event.getEventTime() - touchDownTime) > 1500) { // long
		// // click
		// canBeClick = false;
		// mapActivity.showRegionSelectDialog();
		// }
		return true;
	}

	public static Pair<Float, Float> GET_MOVEMENT_FACTOR(MapRenderer mapRenderer, float diffX, float diffY) {
		float xFactor = diffX / mapRenderer.getViewWidth() * mapRenderer.getCamera().getPosY();
		float yFactor = diffY / mapRenderer.getViewHeight() * mapRenderer.getCamera().getPosY();
		float newCamPosXDiff = -2f * mapRenderer.getViewRatio() / mapRenderer.getCamera().getFrontClippingPlane()
				* xFactor;
		float newCamPosZDiff = -2f / mapRenderer.getCamera().getFrontClippingPlane() * yFactor;
		return new Pair<Float, Float>(newCamPosXDiff, newCamPosZDiff);
	}

	private boolean handleTouchDownEvent(MotionEvent event) {
		initTouchDownPositions(event);

		startRegionSelectHoldTimeTimer();
		canBeClick = true;

		if (isCreatingRegionPlaylist) {
			regionPlaylist = new RegionPlaylistCreator(mapRenderer, this, lastTouchPosX, lastTouchPosY);
			mapRenderer.startDrawingRegionPlaylist(regionPlaylist);
		}

		return true;
	}

	public void multiTouchFinished(MotionEvent event) {
		initTouchDownPositions(event);
	}

	private void initTouchDownPositions(MotionEvent event) {
		if (isKineticScrolling) {
			mapRenderer.getCamera().setGrasped(true);
		}

		lastTouchPosX = event.getX();
		lastTouchPosY = event.getY();
		touchDownPosX = lastTouchPosX;
		touchDownPosY = lastTouchPosY;
		touchDownTime = event.getDownTime();
	}

	public boolean onZoomBarTouch(View v, MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			lastZoomBarTouchY = event.getY();
			lastZoomBarMoveY = event.getY();
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			float absDiffY = Math.abs(event.getY() - lastZoomBarMoveY);
			if (absDiffY > 20) {
				if (event.getY() > lastZoomBarMoveY) {
					lastZoomBarMoveY += 20;
				}
				if (event.getY() < lastZoomBarMoveY) {
					lastZoomBarMoveY -= 20;
				}
				controller.doHapticFeedback();
			}
			float diffY = event.getY() - lastZoomBarTouchY;
			lastZoomBarTouchY = event.getY();

			mapRenderer.getCamera().setCameraPosition(mapRenderer.getCamera().getPosX(),
					mapRenderer.getCamera().getPosY() + diffY / 5, mapRenderer.getCamera().getPosZ(), false);
		}
		return true;
	}

	public boolean onZoomInTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			controller.doHapticFeedback();
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastZoomClickTime < 500 && currentTime - lastToastTime > 20 * 1000) {
				Toast.makeText(controller.getApplicationContext(), R.string.use_zoom_bar, Toast.LENGTH_LONG).show();
				lastToastTime = currentTime;
			}
			lastZoomClickTime = currentTime;
			mapRenderer.getCamera().setCameraPosition(mapRenderer.getCamera().getPosX(),
					mapRenderer.getCamera().getPosY() - 6f, mapRenderer.getCamera().getPosZ(), false);
		}

		return true;
	}

	public boolean onZoomOutTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			controller.doHapticFeedback();
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastZoomClickTime < 500 && currentTime - lastToastTime > 20 * 1000) {
				Toast.makeText(controller.getApplicationContext(), R.string.use_zoom_bar, Toast.LENGTH_LONG).show();
				lastToastTime = currentTime;
			}
			lastZoomClickTime = currentTime;
			mapRenderer.getCamera().setCameraPosition(mapRenderer.getCamera().getPosX(),
					mapRenderer.getCamera().getPosY() + 6f, mapRenderer.getCamera().getPosZ(), false);
		}

		return true;
	}

	public void setKineticMovement(boolean kineticMovement) {
		isKineticScrolling = kineticMovement;
	}

	public void onPause() {
		controller.getSettingsEditor().setLastPositionInPcaMapX(mapRenderer.getCamera().getPosX());
		controller.getSettingsEditor().setLastPositionInPcaMapY(mapRenderer.getCamera().getPosZ());
		Log.v(TAG, "Saving cam pos x: " + mapRenderer.getCamera().getPosX() + " y: "
				+ mapRenderer.getCamera().getPosZ());
	}

	// @Override
	// public void onRegionCreated(List<MapAlbum> albumsInRegion) {
	//
	// boolean at_least_one_album = false;
	//
	// List<PlaylistSong<BaseArtist, BaseAlbum>> songs = new
	// ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
	// for (MapAlbum mapAlbum : albumsInRegion) {
	// CompleteAlbum album;
	// try {
	// album = activity.getData().getCompleteAlbumBlocking(mapAlbum);
	// } catch (DataUnavailableException e) {
	// Log.w(TAG, e);
	// continue; // Ignore this album
	// }
	//
	// if (!at_least_one_album) {
	// controller.getPlaylistController().clearPlaylist();
	// }
	//
	// for (BaseSong<BaseArtist, BaseAlbum> song : album.getSongs()) {
	// controller.getPlaylistController().appendSongAtEnd(
	// new PlaylistSong<BaseArtist, BaseAlbum>(song,
	// SongSource.MANUALLY_SELECTED));
	// }
	//			
	//
	// if (!at_least_one_album) {
	// at_least_one_album = true;
	//
	// // Set play-mode to shuffle and play
	// controller.getPlaylistController().setPlayMode(
	// PlayModeType.SHUFFLE_PLAYLIST);
	// try {
	// controller.getPlayManager().play();
	// controller.getPlaylistController().next(true); // TODO
	// // verify
	// // 'true'
	// } catch (Exception e) {
	// Log.w(TAG, e);
	// }
	// }
	// }
	// }

	@Override
	public void onRegionCreated(List<MapAlbum> albumsInRegion) {
		long startTime = System.currentTimeMillis();
		controller.showProgressDialog(activity, activity.getString(R.string.loading));
		try {
			List<PlaylistSong<BaseArtist, BaseAlbum>> songs = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
			for (MapAlbum mapAlbum : albumsInRegion) {
				CompleteAlbum album;
				try {
					album = activity.getCollectionModel().getAlbumProvider().getCompleteAlbum(mapAlbum);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
					continue; // Ignore this album
				}

				for (BaseSong<BaseArtist, BaseAlbum> song : album.getSongs()) {
					songs.add(new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED));
				}
			}

			if (songs.size() == 0) {
				return;
			}
			long intermediateTime = System.currentTimeMillis();

			Log.v(TAG, "number of songs in region playlist: " + songs.size());
			controller.getPlayerController().clearPlaylist();
			controller.getPlayerController().appendSongsAtEnd(songs);

			long endTime = System.currentTimeMillis();

			Log.v(TAG, "timing: total: " + (endTime - startTime) + ", reading: " + (intermediateTime - startTime));

			// Set play-mode to shuffle and play
			controller.getPlayerController().setPlayMode(PlayModeType.SHUFFLE_PLAYLIST, 0,
					Constants.SAME_SONG_AVOIDANCE_NUM);
			try {
				controller.getPlayerController().playSongAtPosition(RandomProvider.getRandom().nextInt(songs.size()));
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		} finally {
			controller.removeProgressDialog();
		}
	}

	private synchronized void startRegionSelectHoldTimeTimer() {
		if (mapActivity == null) {
			return;
		}
		cancelRegionSelectHoldTimeTimer();
		regionSelectHoldTimeTimer = new Timer();
		regionSelectHoldTimeTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				canBeClick = false;
				regionSelectHoldTimeTimer = null;
				JukefoxApplication.getHandler().post(new Runnable() {

					@Override
					public void run() {
						mapActivity.showRegionSelectDialog();
					}
				});
			}
		}, 1500);
	}

	public void cancelRegionSelectHoldTimeTimer() {
		if (regionSelectHoldTimeTimer != null) {
			regionSelectHoldTimeTimer.cancel();
			regionSelectHoldTimeTimer = null;
		}
	}

}
