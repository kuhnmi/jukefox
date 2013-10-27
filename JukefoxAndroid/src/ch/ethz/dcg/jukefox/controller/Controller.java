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
package ch.ethz.dcg.jukefox.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.MethodNotImplementedException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.data.cache.ImportStateListener;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.ImportService;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AndroidPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ImportedPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsEditor;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumDetailEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ArtistListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ArtistListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.BaseJukefoxEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ContextShuffleConfigEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.DeleteSongMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.FeedbackDialogEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ImportDialogEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ImportPlaylistEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ListSelectionEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadVideoPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.MapEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlayModeSelectionEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlayerActivityEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlaylistContextMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SavePlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SearchEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ShuffleModeMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarModeMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarSongsToFamousArtistEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SleepMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SongContextMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SongListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SpaceActivityEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagCloudEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagPlaylistGenerationEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TitleSearchMenuEventListener;
import ch.ethz.dcg.pancho3.model.IAndroidApplicationStateController;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.PlaylistImporter.PlaylistInfo;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.DirectorySelectionDialog;
import ch.ethz.dcg.pancho3.view.dialogs.DontShowAgainDialog;
import ch.ethz.dcg.pancho3.view.dialogs.FeedbackDialog;
import ch.ethz.dcg.pancho3.view.dialogs.FirstStartDialog;
import ch.ethz.dcg.pancho3.view.dialogs.ImportDialog;
import ch.ethz.dcg.pancho3.view.dialogs.SdCardProblemDialog;
import ch.ethz.dcg.pancho3.view.dialogs.StandardDialog;
import ch.ethz.dcg.pancho3.view.dialogs.TakeATourDialog;
import ch.ethz.dcg.pancho3.view.overlays.AlbumDetails;
import ch.ethz.dcg.pancho3.view.overlays.ContextShuffleConfig;
import ch.ethz.dcg.pancho3.view.overlays.DeleteSongMenu;
import ch.ethz.dcg.pancho3.view.overlays.LoadPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.LoadVideoPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.PlayModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistContextMenu;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.SavePlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.ShuffleModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimilarModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimilarSongsToFamousArtist;
import ch.ethz.dcg.pancho3.view.overlays.SleepMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongContextMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.overlays.TagPlaylistGenerationActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.MapActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.SearchActivity;
import ch.ethz.dcg.pancho3.view.tabs.SpaceActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistList;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreList;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ListSelectionActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.SongList;
import ch.ethz.dcg.pancho3.view.tabs.lists.TagCloud;
import ch.ethz.dcg.pancho3.view.webpublisher.IAudioScrobblerListener;

public class Controller implements IViewController, ImportStateListener, IOnPlayerStateChangeListener,
		SharedPreferences.OnSharedPreferenceChangeListener, IAudioScrobblerListener {

	public static final int BACK_BUTTON_THRESH = 3000;
	public static final String INTENT_EXTRA_BASE_ARTIST = "artist";
	public static final String INTENT_EXTRA_BASE_ALBUM = "album";
	public static final String INTENT_EXTRA_BASE_SONG = "song";
	public static final String INTENT_EXTRA_SONG_POSITION = "songPosition";
	public static final String INTENT_EXTRA_BASE_GENRE = "genre";
	public static final String INTENT_EXTRA_BASE_TAG = "tag";
	public static final String INTENT_EXTRA_SONG_PLAYLIST_POSITION = "songPlaylistPosition";
	public static final String INTENT_EXTRA_PATH = "path";
	public static final String INTENT_EXTRA_FROM_TIMESTAMP = "fromTimestamp";
	public static final String INTENT_EXTRA_TO_TIMESTAMP = "toTimestamp";
	public static final String INTENT_EXTRA_TAG_NAME = "tagName";
	public static final String INTENT_EXTRA_NUMBER_OF_ALBUMS_WITH_THIS_NAME = "numAlbumsWithThisName";
	public static final String INTENT_EXTRA_EVENT_TIME = "eventTime";

	public static final int DOUBLE_CLICK_THRESH = 500;

	private final static String TAG = Controller.class.getSimpleName();

	private final JukefoxApplication application;
	private final IAndroidApplicationStateController appStateController;
	private AndroidPlayerModelManager playerModel;
	private final AndroidPlayerController playerController;
	private LibraryImportManager libraryImportManager;
	private final ISettingsEditor settingsEditor;
	private final ISettingsReader settingsReader;
	private final WidgetController widgetController;
	private final Vibrator vibrator;
	private boolean pausedDueToCall = false;
	private boolean pausedDueToHeadphone = false;
	private boolean isHeadsetPlugged = false;
	private Timer sleepTimer;

	private long lastPlayPauseButtonPressedTime;

	private final AndroidCollectionModelManager collectionModel;

	private int loadingIdCounter = 0;
	private ProgressDialog loadingDialog;
	private HashMap<Integer, String> loadingMessages;

	private final StartupManager startupManager;
	private JoinableThread startupThread;
	private final Handler handler = new Handler();
	// private final TwitterPublisher twitterPublisher;
	private final PlaylistAutosaveController playlistAutosaveController;

	private ProgressDialog progressDialog;

	public Controller(JukefoxApplication appCtx, AndroidPlayerController playerController,
			AndroidCollectionModelManager collectionModel, AndroidPlayerModelManager playerModel) {
		this.application = appCtx;
		this.collectionModel = collectionModel;
		this.playerModel = playerModel;
		this.appStateController = collectionModel.getApplicationStateManager().getApplicationStateController();
		this.playerController = playerController;
		this.settingsEditor = AndroidSettingsManager.getAndroidSettingsEditor();
		this.settingsReader = AndroidSettingsManager.getAndroidSettingsReader();

		settingsReader.addSettingsChangeListener(this);

		vibrator = (Vibrator) appCtx.getSystemService(Context.VIBRATOR_SERVICE);
		startupManager = new StartupManager(this, appStateController, appCtx);

		widgetController = new WidgetController(this);
		// twitterPublisher = new TwitterPublisher(this);
		playlistAutosaveController = new PlaylistAutosaveController(this);

		loadingMessages = new HashMap<Integer, String>();
		playerController.addOnPlayerStateChangeListener(this);
		libraryImportManager = collectionModel.getLibraryImportManager();
	}

	public void startStartupManager() {
		startupThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "starting startup manager thread...");
				startupManager.start();
				Log.d(TAG, "startup manager thread finished.");
			}
		});
		startupThread.start();
	}

	public AndroidPlayerController getPlayerController() {
		return playerController;
	}

	public PlayerActivityEventListener createPlayerViewEventListener(PlayerActivity activity) {
		return new PlayerActivityEventListener(this, activity);
	}

	public void startPlayModeSelection(Activity activity) {
		if (activity == null) {
			Intent intent = new Intent(application, PlayModeMenu.class);
			application.startActivity(intent);
		} else {
			Log.v(TAG, "start playModeActivity");
			// activity.startActivity(new Intent(activity,
			// PlayModeSelection.class));
			startActivity(activity, PlayModeMenu.class);
			doOverlayAnimation(activity);
		}
	}

	private void doOverlayAnimation(Activity activity) {
		// activity.overridePendingTransition(R.anim.overlay_entry_animation,
		// R.anim.hold);
	}

	public void doHapticFeedback() {
		if (settingsReader.isHapticFeedback()) {
			vibrator.vibrate(AndroidConstants.VIBRATION_DURATION);
		}
	}

	public void startActivity(Activity activity, Class<?> classToLoad) {
		Intent intent = new Intent(activity, classToLoad);
		activity.startActivity(intent);
	}

	public void startReplacingActivity(Activity activity, Class<?> classToLoad) {
		activity.startActivity(new Intent(activity, classToLoad));
	}

	public boolean ignoreUserEvents() {
		// TODO: decide if user events are ignored
		return false;
	}

	@Override
	public BaseJukefoxEventListener createBaseJukefoxEventListener(JukefoxActivity activity) {
		return new BaseJukefoxEventListener(this, activity);
	}

	@Override
	public TabEventListener createTabEventListener(JukefoxTabActivity activity, Tab currentTab) {
		return new TabEventListener(this, activity, currentTab);
	}

	public void showFirstStartDialog() {
		if (FirstStartDialog.isOpen()) {
			return;
		}
		Log.v(TAG, "showFirstStartDialog()");
		Intent intent = new Intent(application, FirstStartDialog.class);

		intent.putExtra(StandardDialog.DIALOG_MSG, application.getString(R.string.welcome_text));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void showDirectorySelectionDialog(Activity activity) {
		DirectorySelectionDialog dsd = new DirectorySelectionDialog(activity);
		dsd.show();
	}

	public void showPreloadedDataProblemDialog(Throwable e) {
		String msg = application.getString(R.string.unknown_exception_while_loading_data);
		msg += e.getMessage();
		showStandardDialog(msg);
	}

	public void showLibraryImportProblemDialog(Throwable e) {
		String msg;
		msg = application.getString(R.string.unknown_exception_during_import_1);
		msg += e.getMessage() + "\n";
		msg += application.getString(R.string.unknown_exception_during_import_2);
		showStandardDialog(msg);
	}

	public void showCouldNotCreateDirectoriesDialog() {
		if (AndroidConstants.THROW_METHOD_STUB_EXCEPTIONS) {
			throw new MethodNotImplementedException();
		}
	}

	public void showCouldNotOpenDbDialog() {
		if (AndroidConstants.THROW_METHOD_STUB_EXCEPTIONS) {
			throw new MethodNotImplementedException();
		}
	}

	@Override
	public PlayModeSelectionEventListener createPlayModeSelectionEventListener(JukefoxActivity activity) {
		return new PlayModeSelectionEventListener(this, activity);
	}

	public MapEventListener createMapEventListener(MapActivity activity) {
		return new MapEventListener(this, activity, activity.getMapRenderer(), false);
	}

	public Context getApplicationContext() {
		return application;
	}

	public PlaylistAutosaveController getPlaylistAutosaveController() {
		return playlistAutosaveController;
	}

	public void showAlbumDetailInfo(JukefoxActivity activity, BaseAlbum album) {
		Intent intent = new Intent(application, AlbumDetails.class);
		intent.putExtra(INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(album));
		if (activity != null) {
			activity.startActivity(intent);
		} else {
			application.startActivity(intent);
		}
	}

	public void previousButtonPressed() {
		Log.i(TAG, "MAGIC: PREV BUTTON PRESSED");
		pausedDueToCall = false;
		pausedDueToHeadphone = false;
		if (playerController.getPlaybackPosition() > BACK_BUTTON_THRESH) {
			playerController.seekTo(0);
		} else {
			try {
				playerController.playPrevious();
			} catch (EmptyPlaylistException e) {
				Log.w(TAG, e);
			} catch (NoNextSongException e) {
				Log.w(TAG, e);
			}
		}
	}

	public void nextButtonPressed() {
		try {
			playNextSong();
		} catch (Exception e) {
			Log.w(TAG, e);
			if (!AndroidUtils.isSdCardOk()) {
				showSdCardProblemDialog();
			}
		}
	}

	private void playNextSong() throws NoNextSongException, EmptyPlaylistException {
		pausedDueToCall = false;
		pausedDueToHeadphone = false;
		playerController.playNext(); // TODO: verify 'true'
	}

	public void headsetPlayPausePressed(long eventTime) {
		if (JukefoxApplication.ignoreEventsUntil > System.currentTimeMillis()) {
			return;
		}
		mediaButtonPlayPausePressed(eventTime);
	}

	public void mediaButtonPlayPausePressed(long eventTime) {
		if (JukefoxApplication.ignoreEventsUntil > System.currentTimeMillis()) {
			return;
		}
		Log.v(TAG, "mediaButton PLayPause EventTime:" + eventTime + " lastTime: " + lastPlayPauseButtonPressedTime);
		if (eventTime - lastPlayPauseButtonPressedTime < DOUBLE_CLICK_THRESH) {
			nextButtonPressed();
		} else {
			lastPlayPauseButtonPressedTime = eventTime;
			playPauseButtonPressed();
		}
	}

	public void playPauseButtonPressed() {
		pausedDueToCall = false;
		pausedDueToHeadphone = false;
		if (playerController.getPlayerState() == PlayerState.PLAY) {
			playerController.pause();
		} else if (playerController.getPlayerState() == PlayerState.PAUSE) {
			try {
				playerController.play();
			} catch (Exception e) {
				if (!AndroidUtils.isSdCardOk()) {
					showSdCardProblemDialog();
				}
				Log.w(TAG, e);
			}
		} else { // STOP or ERROR
			try {
				playerController.playSongAtPosition(playerController.getCurrentSongIndex());
			} catch (PlaylistPositionOutOfRangeException e) {
				Log.w(TAG, e);
				playerController.stop();
			} catch (EmptyPlaylistException e) {
				try {
					playNextSong();
				} catch (Exception e2) {
					if (!AndroidUtils.isSdCardOk()) {
						showSdCardProblemDialog();
					}
				}
			}
		}
	}

	@Override
	public AlbumDetailEventListener createAlbumDetailEventListener(AlbumDetails albumDetails) {
		return new AlbumDetailEventListener(this, albumDetails);
	}

	@Override
	public ListSelectionEventListener createListSelectionEventListener(ListSelectionActivity activity) {
		return new ListSelectionEventListener(this, activity);
	}

	@Override
	public SongListEventListener createSongListEventListener(SongList songList) {
		return new SongListEventListener(this, songList);
	}

	public ISettingsEditor getSettingsEditor() {
		return settingsEditor;
	}

	public ISettingsReader getSettingsReader() {
		return settingsReader;
	}

	public void showSongContextMenu(PlayerActivity activity, BaseSong<BaseArtist, BaseAlbum> song,
			int currentSongPosition) {
		Intent intent = new Intent(activity, SongContextMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_SONG_PLAYLIST_POSITION, currentSongPosition);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);
	}

	@Override
	public ShuffleModeMenuEventListener createShuffleModeMenuEventListener(ShuffleModeMenu shuffleModeMenu) {
		return new ShuffleModeMenuEventListener(this, shuffleModeMenu);
	}

	@Override
	public AlbumListEventListener createAlbumListEventListener(AlbumList albumList) {
		return new AlbumListEventListener(this, albumList);
	}

	public void updateLargeWidget() {
		widgetController.updateLargeWidget();
	}

	public void updateNormalWidget() {
		widgetController.updateNormalWidget();
	}

	public void headsetPlugged() {
		if (isHeadsetPlugged) {
			// return, as we already know that headset is plugged
			return;
		}
		isHeadsetPlugged = true;
		resumeMusicDueToHeadphonesIfNeeded();
	}

	public void headsetUnplugged() {
		if (!isHeadsetPlugged) {
			// return, as we already know that headset is unplugged
			return;
		}
		isHeadsetPlugged = false;
		pausedDueToCall = false;
	}

	private void resumeMusicDueToHeadphonesIfNeeded() {
		if (pausedDueToHeadphone && settingsReader.isAutomaticallyResumeOnHeadsetPlugged()) {
			try {
				playerController.play();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public void callStarted() {
		pausedDueToHeadphone = false;
		if (playerController.getPlayerState() == PlayerState.PLAY) {
			pausedDueToCall = true;
			playerController.pause();
		}
	}

	public void callEnded() {
		if (pausedDueToCall) {
			try {
				playerController.play();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public void audioBecameNoisy() {
		JukefoxApplication.setIgnoreEventsTime(System.currentTimeMillis() + 2000);
		headsetUnplugged();
		if (playerController.getPlayerState() == PlayerState.PLAY) {
			pauseMusicDueToHeadPhones();
		}
	}

	private void pauseMusicDueToHeadPhones() {
		playerController.pause();
		pausedDueToHeadphone = true;
		pausedDueToCall = false;
	}

	public void androidMediaScannerFinished() {
		if (getSettingsReader().isAutomaticImports()) {
			doImportAsync(false, true); // flags: don't clean db; do reduced
			// scan
		}
	}

	public void doImportAsync(final boolean clearDb, final boolean reduced) {
		libraryImportManager.doImportAsync(clearDb, reduced);
	}

	public void abortImportAsync() {
		collectionModel.getLibraryImportManager().abortImportAsync();
	}

	@Override
	public PlaylistMenuEventListener createPlaylistMenuEventListener(PlaylistMenu playlistMenu) {
		return new PlaylistMenuEventListener(this, playlistMenu);
	}

	@Override
	public LoadPlaylistMenuEventListener createLoadPlaylistMenuEventListener(LoadPlaylistMenu loadPlaylistMenu) {
		return new LoadPlaylistMenuEventListener(this, loadPlaylistMenu);
	}

	@Override
	public LoadVideoPlaylistMenuEventListener createLoadVideoPlaylistMenuEventListener(
			LoadVideoPlaylistMenu loadVideoPlaylistMenu) {
		return new LoadVideoPlaylistMenuEventListener(this, loadVideoPlaylistMenu);
	}

	@Override
	public SavePlaylistMenuEventListener createSavePlaylistMenuEventListener(SavePlaylistMenu savePlaylistMenu) {
		return new SavePlaylistMenuEventListener(this, savePlaylistMenu);
	}

	@Override
	public SleepMenuEventListener createSleepMenuEventListener(SleepMenu sleepMenu) {
		return new SleepMenuEventListener(this, sleepMenu);
	}

	public void setSleepTimer(int minutes) {
		if (sleepTimer != null) {
			sleepTimer.cancel();
		}
		sleepTimer = new Timer();
		sleepTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				playerController.pause();
			}

		}, minutes * 60000L);
		Log.v(TAG, "sleep timer set");
	}

	public void cancelSleepTimer() {
		if (sleepTimer != null) {
			sleepTimer.cancel();
			sleepTimer = null;
		}
	}

	public void showAlbumList(JukefoxActivity activity, BaseArtist artist) {
		Intent intent = new Intent(application, AlbumList.class);
		intent.putExtra(INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(artist));
		if (activity != null) {
			activity.startActivity(intent);
		} else {
			application.startActivity(intent);
		}
	}

	@Override
	public ArtistListEventListener createArtistListEventListener(ArtistList artistList) {
		return new ArtistListEventListener(this, artistList);
	}

	@Override
	public GenreListEventListener createGenreListEventListener(GenreList genreList) {
		return new GenreListEventListener(this, genreList);
	}

	@Override
	public SimilarModeMenuEventListener createSimilarModeMenuEventListener(SimilarModeMenu similarModeMenu) {
		return new SimilarModeMenuEventListener(this, similarModeMenu);
	}

	@Override
	public GenreListMenuEventListener createGenreListMenuEventListener(GenreListMenu genreListMenu) {
		return new GenreListMenuEventListener(this, genreListMenu);
	}

	@Override
	public ArtistListMenuEventListener createArtistListMenuEventListener(ArtistListMenu artistListMenu) {
		return new ArtistListMenuEventListener(this, artistListMenu);
	}

	@Override
	public TagCloudEventListener createTagCloudEventListener(TagCloud tagCloud) {
		return new TagCloudEventListener(this, tagCloud);
	}

	@Override
	public SearchEventListener createSearchEventListener(SearchActivity searchActivity) {
		return new SearchEventListener(this, searchActivity);
	}

	@Override
	public void onBaseDataCommitted() {
	}

	@Override
	public void onCoordinatesFetched() {

	}

	@Override
	public void onAlbumCoversFetched() {

	}

	public int setLoading(final Context activityContext, final String msg) {
		final int id = loadingIdCounter;
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (loadingMessages.size() == 0) {
					loadingDialog = ProgressDialog.show(activityContext, activityContext
							.getString(R.string.please_wait), msg, true);
				} else {
					Log.v(TAG, "Loading msg size " + loadingMessages.size());
					loadingDialog.setMessage(msg);
				}
				loadingIdCounter++;
				loadingMessages.put(id, msg);
			}

		});
		return id;
	}

	public void finishLoading(int id) {
		loadingMessages.remove(id);
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (loadingMessages.size() == 0) {
					loadingDialog.dismiss();
				} else {
					loadingDialog.setMessage(loadingMessages.values().iterator().next());
				}
			}

		});
	}

	@Override
	public TitleSearchMenuEventListener createTitleSearchMenuEventListener(SongMenu titleSearchResultMenu) {
		return new TitleSearchMenuEventListener(this, titleSearchResultMenu);
	}

	@Override
	public SpaceActivityEventListener createSpaceEventListener(SpaceActivity spaceActivity) {
		return new SpaceActivityEventListener(this, spaceActivity);
	}

	@Override
	public SongContextMenuEventListener createSongContextMenuEventListener(SongContextMenu songContextMenu) {
		return new SongContextMenuEventListener(this, songContextMenu);
	}

	@Override
	public TagPlaylistGenerationEventListener createTagPlaylistGenerationEventListener(
			TagPlaylistGenerationActivity tagPlaylistGeneration) {
		return new TagPlaylistGenerationEventListener(this, tagPlaylistGeneration);
	}

	// public void showSdCardProblemDialog() {
	// Intent intent = new Intent(appCtx, DialogDisplayer.class);
	// intent.putExtra(DialogDisplayer.INTENT_EXTRA_DIALOG_TYPE,
	// DialogDisplayer.DialogType.STANDARD);
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// appCtx.startActivity(intent);
	// }

	@Override
	public SimilarSongsToFamousArtistEventListener createSimilarSongsToFamousArtistEventListener(
			SimilarSongsToFamousArtist similarSongsToFamousArtist) {
		return new SimilarSongsToFamousArtistEventListener(this, similarSongsToFamousArtist);
	}

	public void showStandardDialog(String msg) {
		showStandardDialog(null, null, msg);
	}

	public void showStandardDialog(Activity activity, Integer requestCode, String msg) {
		Intent intent = new Intent(activity == null ? application : activity, StandardDialog.class);
		intent.putExtra(StandardDialog.DIALOG_MSG, msg);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (activity == null) {
			application.startActivity(intent);
			return;
		}
		activity.startActivityForResult(intent, requestCode);
	}

	public void showDontShowAgainDialog(String msg, String dontShowSharedPrefKey) {
		if (settingsReader.isDontShowAgain(dontShowSharedPrefKey)) {
			return;
		}
		Intent intent = new Intent(application, DontShowAgainDialog.class);
		intent.putExtra(StandardDialog.DIALOG_MSG, msg);
		intent.putExtra(DontShowAgainDialog.SHARED_PREF_KEY, dontShowSharedPrefKey);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	@Override
	public void onImportCompleted(boolean hadChanges) {
		stopImportService();
		if (hadChanges) {
			// Notify user that import completed (if there were any changes)
			handler.post(new Runnable() {

				@Override
				public void run() {
					Toast toast = Toast.makeText(application, application.getString(R.string.import_completed),
							Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}

	private void startImportService() {
		Log.v(TAG, "startImportService()");
		Intent intent = new Intent(JukefoxApplication.getAppContext(), ImportService.class);
		JukefoxApplication.getAppContext().startService(intent);
	}

	private void stopImportService() {
		Intent intent = new Intent(JukefoxApplication.getAppContext(), ImportService.class);
		JukefoxApplication.getAppContext().stopService(intent);
		Log.v(TAG, "stopService()");
	}

	@Override
	public PlaylistContextMenuEventListener createPlaylistContextMenuEventListener(
			PlaylistContextMenu playlistContextMenu) {
		return new PlaylistContextMenuEventListener(this, playlistContextMenu);
	}

	public ImportDialogEventListener createImportDialogEventListener(ImportDialog importDialog) {
		return new ImportDialogEventListener(this, importDialog);
	}

	public void showTakeATourDialog() {
		Intent intent = new Intent(application, TakeATourDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void showAboutDialog() {
		showStandardDialog("jukefox " + "v" + AndroidUtils.getVersionName() + "\n"
				+ application.getString(R.string.about_info));
	}

	public void playDateRangeFromIntent(Intent intent) {
		/*
		 * Playlist playlist; try { playlist =
		 * model.createDateRangePlaylist(intent); // TODO: this model here needs
		 * an interface for this method. //
		 * ((Model)data).getDbWrapper().printPlayLog(); // TODO: debug only... }
		 * catch (DataUnavailableException e1) { Log.w(TAG, e1); return; } if
		 * (playlist.size() < 1) { Log.w(TAG,
		 * "Failed to create playlist for date range"); return; }
		 * playlistController.clearPlaylist();
		 * playlistController.appendSongsAtEnd(playlist.getSongList()); try {
		 * playlistController.setPlayMode(PlayModeType.SIMILAR); int position =
		 * JukefoxApplication.getRandom().nextInt(playlist.size());
		 * playlistController.playSongAtPosition(position); } catch
		 * (PlaylistPositionOutOfRangeException e) { Log.w(TAG, e); }
		 */
	}

	public void playTagFromIntent(Intent intent) {
		String tagName = null;
		try {
			tagName = intent.getExtras().getString(INTENT_EXTRA_TAG_NAME);
			BaseSong<BaseArtist, BaseAlbum> song = collectionModel.getSongProvider().getBaseSong(tagName);
			Playlist playlist = new Playlist();
			playlist.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.AUTOMATICALLY_SELECTED));
			playerController.stop();
			playerController.clearPlaylist();
			playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.TAG_BASED));
			int artistAvoidance = AndroidSettingsManager.getAndroidSettingsReader().getSimilarArtistAvoidanceNumber();
			playerController.setPlayMode(PlayModeType.SIMILAR, artistAvoidance, Constants.SAME_SONG_AVOIDANCE_NUM);
			playerController.play();
		} catch (Exception e) {
			Log.w(TAG, e);
			showStandardDialog(application.getString(R.string.could_not_play_tag_intent_playlist) + " " + tagName);
		}
	}

	public void stopMusicFromIntent(Intent intent) {
		stopButtonPressed();
	}

	public void showSdCardProblemDialog() {
		if (SdCardProblemDialog.isOpen()) {
			return;
		}
		String msg = application.getString(R.string.no_sd_card_available);
		Intent intent = new Intent(application, SdCardProblemDialog.class);
		intent.putExtra(SdCardProblemDialog.DIALOG_MSG, msg);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void onFirstStartDialogClosed() {
		startStartupManager();
		showTakeATourDialog();
		appStateController.setFirstStart(false);
	}

	// public void showFbSendActivity(String msg, String caption) {
	// Intent intent = new Intent(appCtx, FbSendActivity.class);
	// intent.putExtra(FbSendActivity.KEY_MSG, msg);
	// intent.putExtra(FbSendActivity.KEY_COLLAGE_CAPTION, caption);
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// appCtx.startActivity(intent);
	// }

	public void stopButtonPressed() {
		try {
			pausedDueToCall = false;
			pausedDueToHeadphone = false;
			// playlistController.savePlaylist(playlistController.getCurrentPlaylist(),
			// // TODO save playlist not possible anymore
			// Constants.CURRENT_PLAYLIST_NAME);
			playerController.stop();
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
	}

	public void showFeedbackDialog() {
		Intent intent = new Intent(application, FeedbackDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void startMarketWithJukefox() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=ch.ethz.dcg.pancho3"));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void startEmailActivity() {
		StringBuilder body = new StringBuilder();
		try {
			body.append("\n-----------------\n");
			body.append("android-version: " + AndroidUtils.getAndroidVersionName() + "\n");
			body.append("model: " + AndroidUtils.getModel() + "\n");
			body.append("jukefox-version: " + AndroidUtils.getVersionName() + "\n");
			body.append("-----------------\n");
		} catch (Error e) {
			Log.w(TAG, e);
		}

		String subject = "[jukefox]";
		try {
			subject = "[jukefox " + AndroidUtils.getVersionName() + "]";
		} catch (Error e) {
			Log.w(TAG, e);
		}

		// Intent intent = new Intent(Intent.ACTION_SENDTO,
		// Uri.fromParts("mailto", Constants.MUSICEXPLORER_EMAIL, null));
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", Constants.MUSICEXPLORER_EMAIL, null));
		// intent.putExtra(Intent.EXTRA_EMAIL, "kuhnmi@tik.ee.ethz.ch");
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, body.toString());

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		application.startActivity(intent);
	}

	public void goToAlbum(Activity activity, BaseAlbum album) {
		Intent intent = new Intent(activity, MapActivity.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(album));
		activity.startActivity(intent);
		activity.finish();
	}

	@Override
	public FeedbackDialogEventListener createFeedbackDialogEventListener(FeedbackDialog feedbackDialog) {
		return new FeedbackDialogEventListener(this);
	}

	@Override
	public AlbumListMenuEventListener createAlbumListMenuEventListener(AlbumListMenu albumListMenu) {
		return new AlbumListMenuEventListener(this, albumListMenu);
	}

	public void groupAlbum(final String name) {
		try {
			collectionModel.getModelSettingsManager().addAlbumNameToGroup(name);
			Log.v(TAG, "group albums done");
			postToast(application.getString(R.string.grouping_album_finished_toast));
		} catch (Throwable e) {
			Log.w(TAG, e);
			postToast(application.getString(R.string.grouping_album_failed_toast));
			return;
		}
		Log.v(TAG, "performing library import after album grouping");
		doImportAsync(false, false);
		// String msg = appCtx.getString(R.string.grouping_album_toast);
		// new JoinableThread(new Runnable() {
		// @Override
		// public void run() {
		// try {
		// data.groupAlbum(name);
		// postToast(appCtx.getString(R.string.grouping_album_finished_toast));
		// } catch (Throwable e) {
		// Log.w(TAG, e);
		// postToast(appCtx.getString(R.string.grouping_album_failed_toast));
		// return;
		// }
		// doImportAsync(false, false);
		// }
		// }).start();
	}

	public void postToast(final String msg) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(application, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void ungroupAlbum(Activity activity, String name) {
		try {
			collectionModel.getModelSettingsManager().removeAlbumNameToGroup(name);
			postToast(application.getString(R.string.ungrouping_album_finished_toast));
		} catch (Throwable e) {
			Log.w(TAG, e);
			postToast(application.getString(R.string.ungrouping_album_failed_toast));
			return;
		}
		doImportAsync(false, false);
		activity.finish();
	}

	public void showToast(final String message) {
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				Toast t = Toast.makeText(application, message, Toast.LENGTH_LONG);
				t.show();
			}

		});
	}

	public void scrobbleAuthenticationFailed() {
		showToast(application.getString(R.string.could_not_connect_audioscrobbler));
	}

	@Override
	public void onImportAborted(boolean hadChanges) {
		stopImportService();

	}

	@Override
	public void onImportStarted() {
		startImportService();
	}

	public void performVersionChanges() {
		// TODO: here, we should check whether this is a new installation. If
		// not, we should check whether the app is up-to-date (i.e. current
		// version is the same as the last known version (=> new shared pref),
		// and if not, apply all changes recursively back to the last known
		// version.
		if (settingsReader.isScrobblingEnabled()) {
			settingsEditor.updateScrobblingEnabledPref();
		}
	}

	@Override
	public DeleteSongMenuEventListener createDeleteSongMenuEventListener(DeleteSongMenu deleteSongMenu) {
		return new DeleteSongMenuEventListener(this, deleteSongMenu);
	}

	public void ignoreSong(BaseSong<BaseArtist, BaseAlbum> song) {
		collectionModel.getModifyProvider().ignoreSong(song);
	}

	public void deleteSong(BaseSong<BaseArtist, BaseAlbum> song) {
		collectionModel.getModifyProvider().deleteSong(song);
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {
		application.updateJukefoxIntentReceiver();
	}

	/**
	 * stops the playerservice if the music is not playing
	 * 
	 * @return returns true if the service was stopped, false otherwise
	 */
	public boolean stopServiceIfNotNeeded() {
		if (playerController.getPlayerState() != PlayerState.PLAY) {
			playerController.stopPlayerService();
			return true;
		}
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(application.getString(R.string.KEY_LOCK_SCREEN_CONTROLS))) {
			application.updateJukefoxIntentReceiver();
		}
	}

	@Override
	public ContextShuffleConfigEventListener createContextShuffleConfigEventListener(
			ContextShuffleConfig smartShuffleConfig) {
		return new ContextShuffleConfigEventListener(this, smartShuffleConfig);
	}

	public void showProgressDialog(final JukefoxActivity activity, final String msg) {
		if (progressDialog != null) {
			return;
		}
		handler.post(new Runnable() {

			@Override
			public void run() {
				Log.v(TAG, "showing progress dialog.");
				progressDialog = new ProgressDialog(activity);
				progressDialog.setMessage(msg);
				progressDialog.show();
			}
		});
	}

	public void removeProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		handler.post(new Runnable() {

			@Override
			public void run() {
				Log.v(TAG, "removing progress dialog.");
				progressDialog.dismiss();
				progressDialog = null;
			}
		});
	}

	@Override
	public ImportPlaylistEventListener createImportPlaylistEventListener() {
		return new ImportPlaylistEventListener(this);
	}

	public ImportedPlaylist importPlaylist(PlaylistInfo info) throws IOException {
		ImportedPlaylist playlist = collectionModel.getPlaylistImporter().parse(info);
		playerController.setPlaylist(playlist);
		if (!playlist.isPlaylistEmpty()) {
			try {
				playerController.loadSongAtPosition(0); // playSongAtPosition(0);
			} catch (PlaylistPositionOutOfRangeException e) {
				// should never happen!
				Log.w(TAG, e);
			}
		}
		return playlist;
	}

	public void onTerminate() {
		try {
			getPlaylistAutosaveController().saveCurrentPlaylist();
			getPlaylistAutosaveController().cancelTimers();
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public void onAuthenticationFailed() {
		// TODO Auto-generated method stub
		if (AndroidConstants.THROW_METHOD_STUB_EXCEPTIONS) {
			throw new MethodNotImplementedException();
		}

	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
	}

	@Override
	public void onImportProblem(Throwable e) {
		showLibraryImportProblemDialog(e);
	}

}