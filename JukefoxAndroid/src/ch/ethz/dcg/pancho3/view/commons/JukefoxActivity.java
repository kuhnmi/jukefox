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
package ch.ethz.dcg.pancho3.view.commons;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.Window;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.IViewController;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryChangeDetectedListener;
import ch.ethz.dcg.jukefox.manager.model.albumart.AlbumArtProvider;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.providers.AlbumProvider;
import ch.ethz.dcg.jukefox.model.providers.ArtistProvider;
import ch.ethz.dcg.jukefox.model.providers.CollectionPlaylistProvider;
import ch.ethz.dcg.jukefox.model.providers.GenreProvider;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongCoordinatesProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.model.providers.TagProvider;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.controller.eventhandlers.BaseJukefoxEventListener;
import ch.ethz.dcg.pancho3.model.IReadOnlyAndroidApplicationState;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class JukefoxActivity extends Activity implements LibraryChangeDetectedListener {

	private final static String TAG = JukefoxActivity.class.getSimpleName();

	public static final int MENU_INFO = 0;
	public static final int MENU_HELP = 1;
	public static final int MENU_UPDATE = 2;
	public static final int MENU_SETTINGS = 3;
	public static final int MENU_QUIT = 4;
	public static final int MENU_FACEBOOK = 5;
	public static final int MENU_FEEDBACK = 6;
	public static final int MENU_STATISTICS = 7;

	public static final int MENU_TMP = 100;

	protected IReadOnlyAndroidApplicationState applicationState;
	protected AlbumProvider albumProvider;
	protected ArtistProvider artistProvider;
	protected GenreProvider genreProvider;
	protected OtherDataProvider otherDataProvider;
	protected CollectionPlaylistProvider collectionPlaylistProvider;
	protected SongProvider songProvider;
	protected SongCoordinatesProvider songCoordinatesProvider;
	protected StatisticsProvider statisticsProvider;
	protected TagProvider tagProvider;
	protected AlbumArtProvider albumArtProvider;
	protected IReadOnlyPlayerController playerController;
	protected ISettingsReader settings;
	protected JukefoxApplication application;
	protected IViewController controller;
	protected AndroidCollectionModelManager collectionModel;
	private StatusInfo statusInfo;

	private BaseJukefoxEventListener baseJukefoxEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (JukefoxApplication) getApplication();
		applicationState = JukefoxApplication.getCollectionModel().getApplicationStateManager()
				.getApplicationStateReader();
		collectionModel = JukefoxApplication.getCollectionModel();

		// Assign Providers
		albumProvider = JukefoxApplication.getCollectionModel().getAlbumProvider();
		artistProvider = JukefoxApplication.getCollectionModel().getArtistProvider();
		genreProvider = JukefoxApplication.getCollectionModel().getGenreProvider();
		otherDataProvider = JukefoxApplication.getCollectionModel().getOtherDataProvider();
		collectionPlaylistProvider = JukefoxApplication.getCollectionModel().getPlaylistProvider();
		songProvider = JukefoxApplication.getCollectionModel().getSongProvider();
		songCoordinatesProvider = JukefoxApplication.getCollectionModel().getSongCoordinatesProvider();
		tagProvider = JukefoxApplication.getCollectionModel().getTagProvider();
		albumArtProvider = JukefoxApplication.getCollectionModel().getAlbumArtProvider();

		playerController = JukefoxApplication.getPlayerController();
		settings = AndroidSettingsManager.getAndroidSettingsReader();
		controller = application.getController();

		statisticsProvider = JukefoxApplication.getPlayerModel().getStatisticsProvider();

		if (hideTitleBar()) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		baseJukefoxEventListener = controller.createBaseJukefoxEventListener(this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

	}

	public AndroidCollectionModelManager getCollectionModel() {
		return collectionModel;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (baseJukefoxEventListener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Create main menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Set menu options
		menu.add(0, MENU_INFO, 0, getString(R.string.menu_about)).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, MENU_HELP, 0, getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, MENU_UPDATE, 0, getString(R.string.scan_music_library)).setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, MENU_SETTINGS, 0, getString(R.string.menu_settings))
				.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_FEEDBACK, 0, getString(R.string.menu_feedback)).setIcon(android.R.drawable.ic_menu_edit);
		menu.add(0, MENU_STATISTICS, 0, getString(R.string.menu_statistics))
				.setIcon(R.drawable.d172_menu_statistics);
		// menu.add(0, MENU_FACEBOOK, 0,
		// getString(R.string.menu_facebook)).setIcon(android.R.drawable.ic_menu_gallery);
		// menu.add(0, MENU_QUIT, 0,
		// getString(R.string.menu_quit)).setIcon(android.R.drawable.ic_menu_delete);
		// menu.add(0, MENU_TMP, 0,
		// "tmp").setIcon(android.R.drawable.ic_menu_edit);
		return true;
	}

	/**
	 * Handles item selections
	 * 
	 * @param the
	 *            Menu ite that was selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return baseJukefoxEventListener.onMenuOptionSelected(item);
	}

	public IReadOnlyAndroidApplicationState getApplicationState() {
		return applicationState;
	}

	public IReadOnlyPlayerController getPlayerController() {
		return playerController;
	}

	public ISettingsReader getSettings() {
		return settings;
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");
		setRequestedOrientation(settings.getPreferredScreenOrientation());
		applicationState.addLibraryChangeDetectedListener(this);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// applicationState.removeLibraryChangeDetectedListener(this);
		dismissStatusInfo();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		applicationState.removeLibraryChangeDetectedListener(this);
		dismissStatusInfo();
	}

	public void showStatusInfo(String message) {
		if (statusInfo != null) {
			statusInfo.dismiss();
		}
		statusInfo = StatusInfo.showInfo(this, message, getApplicationState());
	}

	protected void dismissStatusInfo() {
		if (statusInfo != null) {
			statusInfo.dismiss();
			statusInfo = null;
		}
	}

	@Override
	public void onLibraryChangeDetected() {
		Log.v(TAG, "onLibraryChangeDetected");
		showStatusInfo();
	}

	protected void showStatusInfo() {
		// don't show anything by default...
	}

	/**
	 * By default we hide the title bar if it is not necessary, but subclasses can change this behavior.
	 */
	@SuppressLint("NewApi")
	protected boolean hideTitleBar() {
		// We have to show the title bar on devices that have no menu button for
		// them to see the menu soft key
		if (AndroidUtils.getAndroidVersionName().startsWith("3")) {
			return false;
		}
		if (android.os.Build.VERSION.SDK_INT >= 11 && android.os.Build.VERSION.SDK_INT <= 13) {
			return false;
		}
		if (android.os.Build.VERSION.SDK_INT >= 14 && !ViewConfiguration.get(this).hasPermanentMenuKey()) {
			return false;
		}
		return true;
	}

	protected void waitForPlaybackFunctionality() {
		// TODO: replace this hack by event mechanism!!
		// OnPlaylistFunctionalityInitialized event (when registering, and it is
		// already initialized => invoke callback method on sender to make sure
		// the required actions are performed; otherwise, this will work like a
		// normal event).
		while (!playerController.isReady()) {
			try {
				JoinableThread.sleep(10);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
		}
	}
}
