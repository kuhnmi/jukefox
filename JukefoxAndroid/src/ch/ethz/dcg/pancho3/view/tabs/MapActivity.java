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
package ch.ethz.dcg.pancho3.view.tabs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.MapEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.view.commons.StatusInfo;
import ch.ethz.dcg.pancho3.view.tabs.opengl.GLView;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer.CurrentAlbumProvider;

public class MapActivity extends JukefoxTabActivity implements CurrentAlbumProvider {

	public static final String TAG = MapActivity.class.getSimpleName();
	private GLView glView;
	private MapRenderer mapRenderer;
	private MapEventListener eventListener;
	private MapAlbum currentAlbum;
	protected IOnPlaylistStateChangeListener playlistEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "MapActivity: setting content view...");

		setContentView(R.layout.map);

		Log.v(TAG, "MapActivity: setting current tab...");
		setCurrentTab(Tab.MAP);

		Log.v(TAG, "MapActivity: creating map renderer...");
		mapRenderer = new MapRenderer(settings, collectionModel, collectionModel.getLibraryImportManager()
				.getImportState(), this, this, getIntent());

		Log.v(TAG, "MapActivity: creating map event listener...");
		eventListener = controller.createMapEventListener(this);
		eventListener.setMapActivity(this);

		Log.v(TAG, "MapActivity: register touch event listener...");
		registerTouchEventListener();

		Log.v(TAG, "MapActivity: register playlist listener...");
		registerPlaylistListener();

		Log.v(TAG, "MapActivity: register checking app status...");
		checkAppStatus();

		Log.v(TAG, "MapActivity: setting current album...");
		setCurrentAlbum();
	}

	private void checkAppStatus() {
		if (applicationState.isImporting() && !applicationState.isMapDataCommitted()) {
			showStatusInfo(getString(R.string.map_not_yet_loaded));
		} else if (applicationState.isImporting() && !applicationState.isCoversFetched()) {
			showStatusInfo(getString(R.string.covers_not_yet_fetched));
		} else if (mapRenderer.getAlbums().size() == 0) {
			StatusInfo.showInfo(this, getString(R.string.no_song_coordinates));
		}
	}

	private void registerPlaylistListener() {
		playlistEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				try {
					currentAlbum = albumProvider.getMapAlbum(newSong);
					mapRenderer.newCurrentAlbum();
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {
			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
			}
		};
		playerController.addOnPlaylistStateChangeListener(playlistEventListener);
	}

	private void setCurrentAlbum() {
		try {
			currentAlbum = albumProvider.getMapAlbum(playerController.getCurrentSong());
			mapRenderer.newCurrentAlbum();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	private void registerTouchEventListener() {
		findViewById(R.id.zoomBar).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomBarTouch(v, event);
			}
		});
		findViewById(R.id.zoomIn).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomInTouch(v, event);
			}
		});
		findViewById(R.id.zoomOut).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomOutTouch(v, event);
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (eventListener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "Map activity onPause");
		super.onPause();
		eventListener.onPause();
		mapRenderer.onPause();
		if (glView != null) {
			glView.onPause();
		}
	}

	@Override
	protected void onRestart() {
		// Log.v(TAG, "Map activity onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// Log.v(TAG, "Map activity onResume");
		super.onResume();
		mapRenderer.onResume();
		boolean kineticMovement = settings.isKineticMovement();
		eventListener.setKineticMovement(kineticMovement);

		initGlView();

		if (glView != null) {
			glView.onResume();
		}
		checkIfHideZoomBar();
		checkIntent();
	}

	private void initGlView() {

		if (glView == null) {
			Log.v(TAG, "MapActivity: setting gl view...");
			glView = (GLView) findViewById(R.id.glview);

			Log.v(TAG, "MapActivity: setting renderer...");
			glView.setRenderer(mapRenderer);

			glView.setOnTouchListener(new OnTouchListener() {

				public boolean onTouch(View v, MotionEvent event) {
					return eventListener.onGlTouch(v, event);
				}

			});
		}
	}

	private void checkIfHideZoomBar() {
		boolean showZoomBar = settings.isShowZoomBar();
		if (!showZoomBar) {
			findViewById(R.id.zoomBar).setVisibility(View.GONE);
		} else {
			findViewById(R.id.zoomBar).setVisibility(View.VISIBLE);
		}
	}

	public void setCameraPosition() {
		float posX = settings.getLastPositionInPcaMapX();
		float posZ = settings.getLastPositionInPcaMapY();
		// Only set map to last position if no album is set to go to
		mapRenderer.getCamera().setCameraPosition(posX, MapRenderer.DEFAULT_CAMERA_HEIGHT, posZ, false);
		Log.v(TAG, "Set camera to position x: " + posX + " y: " + posZ);
	}

	@Override
	protected void onDestroy() {
		Log.v(TAG, "Map activity onDestroy");
		super.onDestroy();
		if (glView != null) {
			glView.onPause();
		}
		playerController.removeOnPlaylistStateChangeListener(playlistEventListener);
	}

	@Override
	public MapAlbum getCurrentAlbum() {
		return currentAlbum;
	}

	public MapRenderer getMapRenderer() {
		return mapRenderer;
	}

	protected void checkIntent() {
		Intent intent = getIntent();
		Log.v(TAG, "checking intent");
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		ParcelableAlbum pAlbum = extras.getParcelable(Controller.INTENT_EXTRA_BASE_ALBUM);
		BaseAlbum album = pAlbum.getBaseAlbum();
		intent.removeExtra(Controller.INTENT_EXTRA_BASE_ALBUM);
		if (album == null) {
			return;
		}
		Log.v(TAG, "intent with album extra");
		mapRenderer.goToAlbum(album);
	}

	public void showRegionSelectDialog() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setMessage(getString(R.string.start_region_select));
		dialogBuilder.setCancelable(true);
		dialogBuilder.setPositiveButton(getString(R.string.ok), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				eventListener.startCreatingRegionPlaylist();
			}
		});
		dialogBuilder.setNegativeButton(getString(R.string.cancel), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		dialogBuilder.create().show();
	}

}
