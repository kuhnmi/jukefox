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
package ch.ethz.dcg.pancho3.tablet.view.map;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.controller.eventhandlers.MapEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.presenter.map.MapPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.map.MapPresenter.IMapView;
import ch.ethz.dcg.pancho3.view.tabs.MapActivity;
import ch.ethz.dcg.pancho3.view.tabs.opengl.GLView;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;

/**
 * Fragment to display a map of albums.
 */
public class MapSelectionFragment extends Fragment implements IMapView {

	public static final String TAG = MapActivity.class.getSimpleName();
	private ViewGroup mainViewContainer;
	private View mainView;
	private GLView glView;
	private MapRenderer mapRenderer;
	private MapEventListener eventListener;
	private ISettingsReader settings;
	private ImportState importState;
	private MapPresenter presenter;
	private View selectRegionButton;

	private boolean running = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (mainViewContainer == null) {
			mainViewContainer = new FrameLayout(getActivity());
			settings = AndroidSettingsManager.getAndroidSettingsReader();
			importState = JukefoxApplication.getCollectionModel().getLibraryImportManager().getImportState();
			mapRenderer = new MapRenderer(settings, JukefoxApplication.getCollectionModel(), importState,
					getActivity(), null, null);
		}
		return mainViewContainer;
	}

	@Override
	public void stopSelectingRegion() {
		eventListener.stopCreatingRegionPlaylist();
		selectRegionButton.setVisibility(View.VISIBLE);
	}

	private void checkAppStatus() {
		if (importState.isImporting()
				&& !importState.isMapDataCommitted()) {
			//	showStatusInfo(getString(R.string.map_not_yet_loaded));
		} else if (importState.isImporting()
				&& !importState.isCoversFetched()) {
			//	showStatusInfo(getString(R.string.covers_not_yet_fetched));
		} else if (mapRenderer.getAlbums().size() == 0) {
			//StatusInfo.showInfo(getActivity(), getString(R.string.no_song_coordinates));
		}
	}

	private void registerTouchEventListener() {
		mainView.findViewById(R.id.zoomBar).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomBarTouch(v, event);
			}
		});
		mainView.findViewById(R.id.zoomIn).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomInTouch(v, event);
			}
		});
		mainView.findViewById(R.id.zoomOut).setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onZoomOutTouch(v, event);
			}
		});

		selectRegionButton = mainView.findViewById(R.id.selectregionbutton);
		selectRegionButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getActivity().startActionMode(presenter);
				selectRegionButton.setVisibility(View.INVISIBLE);
				eventListener.startCreatingRegionPlaylist();
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		stopRunning();
	}

	private void stopRunning() {
		if (running) {
			running = false;
			if (eventListener != null) {
				eventListener.onPause();
			}
			if (mapRenderer != null) {
				mapRenderer.onPause();
			}
			if (glView != null) {
				glView.onPause();
			}
		}
	}

	private void startRunning() {
		if (!running) {
			running = true;
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mainViewContainer.getWindowToken(), 0);
			mainViewContainer.removeAllViews();
			mainView = LayoutInflater.from(getActivity()).inflate(R.layout.tablet_mapfragment, null);
			mainViewContainer.addView(mainView);

			registerTouchEventListener();
			//checkAppStatus();

			mapRenderer.onResume();
			initGlView();
			glView.onResume();

			checkIfHideZoomBar();

			new AsyncTask<Void, Void, TabletFactory>() {

				@Override
				protected TabletFactory doInBackground(Void... params) {
					TabletFactoryGetter tabletFactoryGetter = (TabletFactoryGetter) getActivity();
					while (!tabletFactoryGetter.isTabletFactoryReady()) {
						try {
							JoinableThread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
					return tabletFactoryGetter.getTabletFactory();
				}

				@Override
				protected void onPostExecute(TabletFactory tabletFactory) {
					presenter = tabletFactory.getMapPresenter();
					mapRenderer.setCurrentAlbumProvider(presenter);
					eventListener = tabletFactory.getTabletMapEventListener(mapRenderer);
					presenter.setMap(MapSelectionFragment.this);
					presenter.viewFinishedInit();
					boolean kineticMovement = settings.isKineticMovement();
					eventListener.setKineticMovement(kineticMovement);
				}
			}.execute();
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (hidden) {
			stopRunning();
		} else {
			startRunning();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startRunning();
	}

	private void initGlView() {
		glView = (GLView) mainView.findViewById(R.id.glview);

		glView.setRenderer(mapRenderer);

		glView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onGlTouch(v, event);
			}
		});
	}

	private void checkIfHideZoomBar() {
		boolean showZoomBar = settings.isShowZoomBar();
		if (!showZoomBar) {
			mainView.findViewById(R.id.zoomBar).setVisibility(View.GONE);
		} else {
			mainView.findViewById(R.id.zoomBar).setVisibility(View.VISIBLE);
		}
	}

	public void setCameraPosition() {
		float posX = settings.getLastPositionInPcaMapX();
		float posZ = settings.getLastPositionInPcaMapY();
		// Only set map to last position if no album is set to go to
		mapRenderer.getCamera().setCameraPosition(posX,
				MapRenderer.DEFAULT_CAMERA_HEIGHT, posZ, false);
		Log.v(TAG, "Set camera to position x: " + posX + " y: " + posZ);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (glView != null) {
			glView.onPause();
		}
	}

	@Override
	public void goToAlbum(BaseAlbum album) {
		mapRenderer.goToAlbum(album);
	}

	@Override
	public void newCurrentAlbum() {
		mapRenderer.newCurrentAlbum();
	}
}
