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

import android.content.Intent;
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
import ch.ethz.dcg.pancho3.controller.eventhandlers.SpaceActivityEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.view.commons.StatusInfo;
import ch.ethz.dcg.pancho3.view.tabs.opengl.GLView;
import ch.ethz.dcg.pancho3.view.tabs.opengl.SpaceRenderer;

public class SpaceActivity extends JukefoxTabActivity {

	public static final String TAG = SpaceActivity.class.getSimpleName();

	private GLView glView;
	private SpaceRenderer spaceRenderer;
	private SpaceActivityEventListener eventListener;
	private MapAlbum currentAlbum;
	protected IOnPlaylistStateChangeListener playlistEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.space);

		setCurrentTab(Tab.SPACE);

		spaceRenderer = new SpaceRenderer(settings, collectionModel, collectionModel.getLibraryImportManager()
				.getImportState(), this, this);

		eventListener = controller.createSpaceEventListener(this);

		registerTouchEventListener();

		registerPlaylistListener();

		checkAppStatus();

		// setCameraPosition();

		setCurrentAlbum();

	}

	private void checkAppStatus() {
		if (applicationState.isImporting() && !applicationState.isMapDataCommitted()) {
			showStatusInfo(getString(R.string.map_not_yet_loaded));
		} else if (applicationState.isImporting() && !applicationState.isCoversFetched()) {
			showStatusInfo(getString(R.string.covers_not_yet_fetched));
		} else if (spaceRenderer.getAlbums().size() == 0) {
			StatusInfo.showInfo(this, getString(R.string.no_song_coordinates));
		}
	}

	private void registerPlaylistListener() {
		playlistEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				try {
					currentAlbum = albumProvider.getMapAlbum(newSong);
					spaceRenderer.goToAlbum(currentAlbum);
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

	private void registerTouchEventListener() {

	}

	@Override
	protected void onPause() {
		Log.v(TAG, "Map activity onPause");
		super.onPause();
		eventListener.onPause();
		spaceRenderer.onPause();
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
		// setCameraPosition();
		spaceRenderer.onResume();
		boolean kineticMovement = settings.isKineticMovement();
		eventListener.setKineticMovement(kineticMovement);

		initGlView();

		if (glView != null) {
			glView.onResume();
		}
	}

	private void initGlView() {
		if (glView == null) {
			glView = (GLView) findViewById(R.id.glview);
			glView.setRenderer(spaceRenderer);
			glView.setOnTouchListener(new OnTouchListener() {

				public boolean onTouch(View v, MotionEvent event) {
					return eventListener.onGlTouch(v, event);
				}

			});
		}
	}

	// public void setCameraPosition() {
	// float posX = settings.getLastPositionInPcaMapX();
	// float posZ = settings.getLastPositionInPcaMapY();
	// spaceRenderer.getCamera().setCameraPosition(posX,
	// SpaceRenderer.CAMERA_HEIGHT, posZ, false);
	// Log.v(TAG, "Set camera to position x: " + posX + " y: " + posZ);
	// }

	@Override
	protected void onDestroy() {
		Log.v(TAG, "Space activity onDestroy");
		super.onDestroy();
		if (glView != null) {
			glView.onPause();
		}
		playerController.removeOnPlaylistStateChangeListener(playlistEventListener);
	}

	public MapAlbum getCurrentAlbum() {
		return currentAlbum;
	}

	private void setCurrentAlbum() {
		try {
			currentAlbum = albumProvider.getMapAlbum(playerController.getCurrentSong());
			spaceRenderer.goToAlbum(currentAlbum);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	public SpaceRenderer getSpaceRenderer() {
		return spaceRenderer;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (eventListener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		ParcelableAlbum pAlbum = extras.getParcelable(Controller.INTENT_EXTRA_BASE_ALBUM);
		BaseAlbum album = pAlbum.getBaseAlbum();
		if (album == null) {
			return;
		}
		spaceRenderer.goToAlbum(album);
	}
}
