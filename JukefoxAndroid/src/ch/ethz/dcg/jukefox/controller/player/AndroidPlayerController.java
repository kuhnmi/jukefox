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
package ch.ethz.dcg.jukefox.controller.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.AndroidMediaPlayerWrapper;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.BasePlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.CrossfadingPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.DurationBugfixPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.GaplessPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.IPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.PlaylistManager;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.web.ScrobbledroidPublisher;
import ch.ethz.dcg.pancho3.view.webpublisher.AudioScrobbler;

public class AndroidPlayerController extends AbstractPlayerController implements OnSharedPreferenceChangeListener {

	private static final String TAG = AndroidPlayerController.class.getSimpleName();
	private AudioScrobbler audioScrobbler = null;
	private ScrobbledroidPublisher scrobbleDroidScrobbler = null;
	private boolean scrobbleEnabled = AndroidSettingsManager.getAndroidSettingsReader().isInternalScrobblingEnabled();
	private JukefoxApplication application;
	private static PlayerService playerService;

	public AndroidPlayerController(JukefoxApplication application, AbstractCollectionModelManager collectionModel,
			AbstractPlayerModelManager playerModel) {
		super(collectionModel, playerModel);
		this.application = application;

		loadLastPlaylist();

		PreferenceManager.getDefaultSharedPreferences(application).registerOnSharedPreferenceChangeListener(this);
		readAudioScrobblerSettings();
	}

	private void loadLastPlaylist() {
		setPlayMode(PlayModeType.SMART_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		// load the default playlist first
		Playlist playlist;
		try {
			playlist = JukefoxApplication.getPlayerModel().getPlaylistManager().loadPlaylistFromFileByName(
					AndroidConstants.CURRENT_PLAYLIST_NAME);
			setPlaylist(playlist);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	public void readAudioScrobblerSettings() {
		if (AndroidSettingsManager.getAndroidSettingsReader().isInternalScrobblingEnabled()) {
			scrobbleEnabled = true;
			if (audioScrobbler == null) {
				audioScrobbler = new AudioScrobbler(application.getController(), this);
			}
			audioScrobbler.readSettings();
		} else if (AndroidSettingsManager.getAndroidSettingsReader().isScrobbledroidEnabled()) {
			scrobbleEnabled = true;
			if (scrobbleDroidScrobbler == null) {
				scrobbleDroidScrobbler = new ScrobbledroidPublisher(this);
			}
		} else {
			scrobbleEnabled = false;
			if (scrobbleDroidScrobbler != null) {
				scrobbleDroidScrobbler.onDestroy();
				scrobbleDroidScrobbler = null;
			}
			if (audioScrobbler != null) {
				audioScrobbler.onDestroy();
				audioScrobbler = null;
			}
		}
	}

	@Override
	public void informSongCompletedListeners(PlaylistSong<BaseArtist, BaseAlbum> song) {
		super.informSongCompletedListeners(song);
	}

	@Override
	public void informSongStartedListeners(PlaylistSong<BaseArtist, BaseAlbum> song) {
		super.informSongStartedListeners(song);
	}

	public void stopPlayerService() {
		Log.v(TAG, "stopService(): playerSerivce == null: " + (playerService == null));
		if (playerService != null) {
			playerService.stopSelf();
		}
		// Log.v(TAG, "stopService()");
	}

	private void startService() {
		Log.v(TAG, "startService()");
		Intent playButtonIntent = new Intent(JukefoxApplication.getAppContext(), PlayerService.class);
		ComponentName tmp = JukefoxApplication.getAppContext().startService(playButtonIntent);
		Log.v(TAG, "service started. ComponentName: " + tmp);
	}

	@Override
	public void pause() {
		super.pause();
		JukefoxApplication.getWakeLockManager().releasePlayerWakeLock();
		stopPlayerService();
	}

	@Override
	public void play() {
		super.play();
		startService();
		JukefoxApplication.getWakeLockManager().acquirePlayerWakeLock();
	}

	@Override
	public void stop() {
		super.stop();
		JukefoxApplication.getWakeLockManager().releasePlayerWakeLock();
		stopPlayerService();
	}

	public static void setPlayerService(PlayerService service) {
		playerService = service;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
		Context ctx = JukefoxApplication.getAppContext();

		String keyLockScreenControls = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_LOCK_SCREEN_CONTROLS);
		String keyGapless = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_GAPLESS);
		String keyGaplessOffset = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_GAPLESS_OFFSET);
		String keyScrobbleEnabled = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_SCROBBLE_ENABLED);
		String keyCrossfading = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_CROSSFADING);
		String keyBeatMatching = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_BEAT_MATCHING);
		String keyScrobbleType = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_SCROBBLE_TYPE);

		if (key.equals(keyLockScreenControls)) {
			// if (application.getModel().getSettingsReader()
			// .isLockScreenControls()) {
			// // if (getPlayerState() == PlayerState.PLAY) {
			// // disableStandardLockScreen();
			// // }
			// } else {
			// JukefoxApplication.enableLockScreen();
			// }
			return;
		} else if (key.equals(keyGapless)) {
			recreatePlaybackController();
		} else if (key.equals(keyCrossfading)) {
			recreatePlaybackController();
		} else if (key.equals(keyBeatMatching)) {
			recreatePlaybackController();
		} else if (key.equals(keyGaplessOffset)) {
			recreatePlaybackController();
		} else if (key.equals(keyScrobbleEnabled) || key.equals(keyScrobbleType)) {
			readAudioScrobblerSettings();
		}
	}

	private void recreatePlaybackController() {
		// Kind of a hack to make the playlistManager write an updated value of
		// position in song to the playlist
		IReadOnlyPlaylist playlist = currentPlaylistManager.getCurrentPlaylist();
		PlayerState state = getPlayerState();
		stop();
		playbackController.onDestroy();
		playbackController = createPlaybackController(this, collectionModel, playerModel, currentPlaylistManager);
		try {
			if (state == PlayerState.PLAY) {
				playSongAtPosition(playlist.getPositionInList());
				seekTo(playlist.getPositionInSong());
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	@Override
	protected IPlaybackController createPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager playlistManager) {
		ISettingsReader settings = AndroidSettingsManager.getAndroidSettingsReader();
		IPlaybackController player = null;
		AndroidMediaPlayerWrapper mediaPlayer1 = new AndroidMediaPlayerWrapper();
		if (AndroidUtils.isDurationProblemOs()) {
			AndroidMediaPlayerWrapper mediaPlayer2 = new AndroidMediaPlayerWrapper();
			player = new DurationBugfixPlaybackController(listenerInformer, collectionModel, playerModel,
					currentPlaylistManager, settings.getAutoGaplessGapRemoveTime(), settings.getGaplessGapRemoveTime(),
					mediaPlayer1, mediaPlayer2);
		} else if (settings.isCrossfadingEnabled()) {
			AndroidMediaPlayerWrapper mediaPlayer2 = new AndroidMediaPlayerWrapper();
			player = new CrossfadingPlaybackController(listenerInformer, collectionModel, playerModel,
					currentPlaylistManager, settings.getAutoGaplessGapRemoveTime(), settings.getGaplessGapRemoveTime(),
					mediaPlayer1, mediaPlayer2, settings.isBeatMatchingEnabled());
		} else if (settings.isGapless()) {
			AndroidMediaPlayerWrapper mediaPlayer2 = new AndroidMediaPlayerWrapper();
			player = new GaplessPlaybackController(listenerInformer, collectionModel, playerModel,
					currentPlaylistManager, settings.getAutoGaplessGapRemoveTime(), settings.getGaplessGapRemoveTime(),
					mediaPlayer1, mediaPlayer2);
		}
		if (player == null) {
			player = new BasePlaybackController(listenerInformer, collectionModel, playerModel, currentPlaylistManager,
					mediaPlayer1);
		}
		return player;

	}

	@Override
	protected IPlaylistManager createPlaylistManager(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		return new PlaylistManager(collectionModel, playerModel, this, this);
	}
}
