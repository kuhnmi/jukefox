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
package ch.ethz.dcg.jukefox.controller.player.playbackcontroller;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IPlaybackInfoBroadcaster;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.IMediaPlayerWrapper;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.InvalidPathException;
import ch.ethz.dcg.jukefox.controller.player.mediaplayer.OnMediaPlayerEventListener;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerAction;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommand;
import ch.ethz.dcg.jukefox.playmode.PlayerControllerCommands;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public class BasePlaybackController implements IPlaybackController {

	private static final String TAG = BasePlaybackController.class.getSimpleName();
	protected IMediaPlayerWrapper mediaPlayer;
	protected PlayerState state = PlayerState.STOP;
	protected int lastLoadedSongId;
	protected String lastSongPath;
	protected PlaylistSong<BaseArtist, BaseAlbum> lastSong;
	protected final IPlaybackInfoBroadcaster listenerInformer;
	protected long lastOnErrorTime = 0;
	protected final IPlaylistManager currentPlaylistManager;
	protected AbstractCollectionModelManager collectionModel;
	protected AbstractPlayerModelManager playerModel;

	public BasePlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager currentPlaylistManager, IMediaPlayerWrapper mediaPlayer) {

		this.currentPlaylistManager = currentPlaylistManager;
		this.listenerInformer = listenerInformer;
		this.collectionModel = collectionModel;
		this.playerModel = playerModel;

		OnMediaPlayerEventListener mediaPlayerEventListener = new OnMediaPlayerEventListener() {

			@Override
			public boolean onError(IMediaPlayerWrapper mp, int what, int extra) {
				return BasePlaybackController.this.onError(mp, what, extra);
			}

			@Override
			public boolean onInfo(IMediaPlayerWrapper mp, int what, int extra) {
				return BasePlaybackController.this.onInfo(mp, what, extra);
			}

			@Override
			public void onSongCompleted(IMediaPlayerWrapper mediaPlayer) {
				BasePlaybackController.this.onSongCompleted(mediaPlayer.getCurrentSong());
			}

		};
		this.mediaPlayer = mediaPlayer;
		this.mediaPlayer.setOnMediaPlayerEventListener(mediaPlayerEventListener);
	}

	@Override
	public int getDuration() {
		return getDuration(mediaPlayer);
	}

	@Override
	public void pause() {
		pause(mediaPlayer);
	}

	@Override
	public void play() {
		play(mediaPlayer);
	}

	@Override
	public void stop() {
		stop(mediaPlayer);
	}

	@Override
	public void seekTo(int position) {
		seekTo(mediaPlayer, position);
	}

	protected void seekTo(IMediaPlayerWrapper mp, int position) {
		mp.seekTo(position);
	}

	@Override
	public PlayerState getState() {
		return state;
	}

	protected void setPlayerState(final PlayerState newState) {
		if (newState != state) {
			state = newState;
			JoinableThread t = new JoinableThread(new Runnable() {

				@Override
				public void run() {
					listenerInformer.informPlayerStateChangedListeners(newState);
				}
			});
			t.start();
			// if (newState == PlayerState.PLAY) {
			// Log.v(TAG, "disable standard lock screen...");
			// Log.v(TAG, "standard lock screen disabled.");
			// } else {
			// try {
			// JoinableThread.sleep(100);
			// } catch (InterruptedException e) {
			// Log.w(TAG, e);
			// }
			// Log.v(TAG, "enable standard lock screen...");
			// JukefoxApplication.enableLockScreen();
			// Log.v(TAG, "standard lock screen enabled.");
			// }
		}
	}

	@Override
	public void onDestroy() {
		if (mediaPlayer != null) {
			stop();
			mediaPlayer.onDestroy();
		}
	}

	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		listenerInformer.informSongCompletedListeners(song);
		Log.v(TAG, "onSongCompleted()");
		PlayerControllerCommands commands;
		try {
			commands = currentPlaylistManager.getPlayMode().next(currentPlaylistManager.getCurrentPlaylist());
			applyPlayerControlCommands(commands);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
			// TODO: maybe do something meaningful if it does not work
		}
	}

	protected boolean loadSongIntoPlayer(PlaylistSong<BaseArtist, BaseAlbum> song, String path,
			IMediaPlayerWrapper currentMP) {
		if (song == null || path == null) {
			return false;
		}
		if (lastSongPath != null && lastSong != null && lastSongPath.equals(path) && lastSong.getId() == song.getId()) {
			if (currentMP.isSongReadyToPlay() && currentMP.getCurrentSong().getId() == song.getId()) {
				Log.v(TAG, "Ignoring loading command because the song is already loaded in the player and prepared.");
				return true;
			}
		}
		lastSongPath = path;
		lastSong = song;
		// readReplayGain(path);

		try {
			resetAndSetSourceAndPrepare(song, path, currentMP);
			lastLoadedSongId = song.getId();
			// doPlayPause(currentMP);
			return true;
		} catch (Exception e) {
			setPlayerState(PlayerState.ERROR);
			Log.w(TAG, "ERROR while trying to play " + path);
			Log.w(TAG, e);
			return false;
		}
	}

	/**
	 * returns the song duration in milliseconds or -1 if it is not able to read the duration
	 * 
	 * @param path
	 * @return
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private void readReplayGain(String path) {
		try {
			File f = new File(path);
			AudioFile af = AudioFileIO.read(f);
			Tag tag = af.getTag();
			Iterator<TagField> it = tag.getFields();
			while (it.hasNext()) {
				TagField field = it.next();
				TextId3Frame frame = (TextId3Frame) field;
				Log.v(TAG, frame.getContent());
			}
			if (tag.hasField("TXXX")) { // Replay gain tag in MP3s
				try {
					String content = readFieldContent(tag, "TXXX");
					Log.v(TAG, "Replay Gain: " + content);
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			} else if (tag.hasField("APETAGEX")) {
				try {
					String content = readFieldContent(tag, "APETAGEX");
					Log.v(TAG, "Replay Gain: " + content);
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			} else {
				Log.v(TAG, "File has no replay gain tag");
			}
		} catch (Exception e) {
			Log.w(TAG, e);
			return;
		}
	}

	@SuppressWarnings("unchecked")
	private String readFieldContent(Tag tag, String id) {
		try {
			List<TagField> fields = tag.get(id);
			if (fields == null || fields.size() == 0) {
				return "";
			}
			try {
				TextId3Frame frame = (TextId3Frame) fields.get(0);
				return frame.getContent();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			// TODO: try to read GenericId3Frame? => how to get encoding??
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return "";

	}

	protected void resetAndSetSourceAndPrepare(PlaylistSong<BaseArtist, BaseAlbum> song, String path,
			IMediaPlayerWrapper mp) throws IllegalStateException, IOException, InvalidPathException {
		if (!validatePath(path)) {
			throw new InvalidPathException();
		}
		mp.setSong(song, path);
	}

	protected synchronized void play(final IMediaPlayerWrapper mp) {
		PlaylistSong<BaseArtist, BaseAlbum> song = null;
		try {
			song = currentPlaylistManager.getCurrentSong();
		} catch (EmptyPlaylistException e) {
			// if we start the player for the first time
		}
		if (song == null) {
			next();
		} else {
			if (!mp.isSongReadyToPlay()) {
				try {
					loadSongIntoPlayer(song, collectionModel.getOtherDataProvider().getSongPath(song), mp);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
					return;
				}
				if (currentPlaylistManager.getCurrentPlaylist().hasExtras()) {
					seekTo(currentPlaylistManager.getCurrentPlaylist().getPositionInSong());
				}
			}

			try {
				try {
					// Log.v(TAG, "play (mp: " + mp + "), callstack:");
					// Log.printStackTrace(TAG,
					// Thread.currentThread().getStackTrace());
				} catch (Throwable t) {
					Log.w(TAG, t);
				}
				mp.play();
				if (mp.isPlaying()) {
					setPlayerState(PlayerState.PLAY);
					listenerInformer.informSongStartedListeners(mp.getCurrentSong());
				}
			} catch (Exception e) {
				Log.w(TAG, e);
				setPlayerState(PlayerState.ERROR);
			}
		}
	}

	protected synchronized void pause(final IMediaPlayerWrapper mp) {
		try {
			mp.pause();
			Log.v(TAG, "pause.");
			setPlayerState(PlayerState.PAUSE);
		} catch (Exception e) {
			Log.w(TAG, e);
			setPlayerState(PlayerState.ERROR);
		}
	}

	protected synchronized void stop(final IMediaPlayerWrapper mp) {
		try {
			mp.stop();
			mp.reset();
			setPlayerState(PlayerState.STOP);
		} catch (Exception e) {
			Log.w(TAG, e);
			setPlayerState(PlayerState.ERROR);
		}
	}

	protected synchronized int getDuration(IMediaPlayerWrapper mp) {
		if (getState() == PlayerState.STOP || getState() == PlayerState.ERROR) {
			return 0;
		}
		try {
			int duration = mp.getDuration();
			if (duration < 0) {
				duration = 0;
			}
			return duration;
		} catch (Exception e) {
			Log.w(TAG, e);
			return 0;
		}
	}

	protected synchronized int getCurrentPosition(IMediaPlayerWrapper mp) {
		return mp.getCurrentPosition();
	}

	/**
	 * 
	 * @param path
	 *            path of file to check
	 * @return returns true if path is a valid and readable file system path
	 */
	protected boolean validatePath(String path) {
		if (path == null) {
			return false;
		}
		try {
			File test = new File(path);
			if (!test.canRead()) {
				return false;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
			return false;
		}
		return true;
	}

	@Override
	public void reloadSettings() {
	}

	@Override
	public void mute() {
		mute(mediaPlayer);
	}

	@Override
	public void unmute() {
		unmute(mediaPlayer);
	}

	protected void mute(IMediaPlayerWrapper mp) {
		mp.setVolume(0f, 0f);
	}

	protected void unmute(IMediaPlayerWrapper mp) {
		mp.setVolume(0.9f, 0.9f);
	}

	public boolean onError(IMediaPlayerWrapper mp, int what, int extra) {
		Log.e(TAG, "onError() What: " + what + ", Extra: " + extra + ", playerState: " + state);
		try {
			if (System.currentTimeMillis() - lastOnErrorTime < 2000) {
				return true; // avoid endless loops due to erroneous player
				// handling
			}

			PlayerState origPlayerState = state;

			// TODO: should that be "what" or "extra"?? (see
			// http://developer.android.com/reference/android/media/MediaPlayer.OnErrorListener.html
			// and
			// http://android.git.kernel.org/?p=platform/external/opencore.git;a=blob;f=pvmi/pvmf/include/pvmf_return_codes.h;h=ed5a2539ca85ae60425229be41646b6bd7d9389c;hb=HEAD
			if (what == -38) {
				stop(mp);
				loadSongIntoPlayer(lastSong, lastSongPath, mp);
				if (origPlayerState == PlayerState.PLAY) {
					play(mp);
				}
			}
			return true;
		} finally {
			lastOnErrorTime = System.currentTimeMillis();
		}
	}

	public boolean onInfo(IMediaPlayerWrapper mp, int what, int extra) {
		// kuhnmi, 6.8.2011: changed output from onError to onInfo
		Log.i(TAG, "onInfo() What: " + what + ", Extra: " + extra);
		return true;
	}

	@Override
	public IPlaylistManager getCurrentPlaylistManager() {
		return currentPlaylistManager;
	}

	@Override
	public int getPlaybackPosition() {
		return getCurrentPosition(mediaPlayer);
	}

	@Override
	public PlayerState getPlayerState() {
		return state;
	}

	@Override
	public void next() {
		try {
			PlaylistSong<BaseArtist, BaseAlbum> song = currentPlaylistManager.getCurrentSong();
			if (song != null) {
				listenerInformer.informSongSkippedListeners(song);
			}
		} catch (EmptyPlaylistException e1) {
			// Log.w(TAG, e1);
			// We do not need to log this exception as this is can happen when
			// next is called in a n empty playlist
		}
		PlayerControllerCommands commands;
		try {
			commands = currentPlaylistManager.getPlayMode().next(currentPlaylistManager.getCurrentPlaylist());
			applyPlayerControlCommands(commands);
			// for (PlaylistSong<BaseArtist, BaseAlbum> song :
			// currentPlaylistManager.getCurrentPlaylist().getSongList()) {
			// Log.v(TAG, song.toString());
			// }
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
			// TODO: maybe do something meaningful if it does not work
		}
	}

	@Override
	public void previous() {
		PlayerControllerCommands commands;
		try {
			commands = currentPlaylistManager.getPlayMode().previous(currentPlaylistManager.getCurrentPlaylist());
			applyPlayerControlCommands(commands);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
			// TODO: maybe do something meaningful if it does not work
		}
	}

	protected void applyPlayerControlCommands(PlayerControllerCommands commands) {
		for (PlayerControllerCommand command : commands.getAllCommands()) {
			//			Log.v(TAG, command.toString());
			switch (command.getType()) {
				case ADD_SONG:
					try {
						currentPlaylistManager.insertSongAtPosition(command.getSong(), command.getPosition());
					} catch (PlaylistPositionOutOfRangeException e) {
						Log.w(TAG, e);
					}
					break;
				case REMOVE_SONG:
					try {
						currentPlaylistManager.removeSongFromPlaylist(command.getPosition());
					} catch (EmptyPlaylistException e) {
						Log.w(TAG, e);
					} catch (PlaylistPositionOutOfRangeException e) {
						Log.w(TAG, e);
					}
					break;
				case PLAYER_ACTION:
					if (command.getPlayerAction() == PlayerAction.PLAY) {
						play();
					} else if (command.getPlayerAction() == PlayerAction.PAUSE) {
						pause();
					} else if (command.getPlayerAction() == PlayerAction.STOP) {
						stop();
					}
					break;
				case SET_POS_IN_LIST:
					jumpToPlaylistPosition(command.getPosition());
					break;
				case SET_POS_IN_SONG:
					seekTo(command.getPosition());
					break;
				case SET_PLAY_MODE:
					setPlayMode(command.getPlayMode(), 0, Constants.SAME_SONG_AVOIDANCE_NUM); // TODO: use real parameters
					break;
			}
		}
	}

	/**
	 * Depending on the given play mode we create a different playlist controller core. The playlist controller core is
	 * returned, so that the invoker obtains a reference. One can safely cast such a reference, because the given play
	 * mode type clearly identifies which controller will be created.
	 */
	@Override
	public IPlayMode setPlayMode(PlayModeType playModeType, int artistAvoidance, int songAvoidance) {

		currentPlaylistManager.setPlayMode(playModeType, artistAvoidance, songAvoidance);
		PlayerControllerCommands commands = currentPlaylistManager.getPlayMode().initialize(
				currentPlaylistManager.getCurrentPlaylist());
		applyPlayerControlCommands(commands);
		listenerInformer.informPlayModeChangeListener(currentPlaylistManager.getPlayMode());
		return currentPlaylistManager.getPlayMode();
	}

	@Override
	public boolean jumpToPlaylistPosition(int position) {
		try {
			currentPlaylistManager.setCurrentSongIndex(position);
			PlaylistSong<BaseArtist, BaseAlbum> song = currentPlaylistManager.getCurrentSong();
			String path;
			try {
				path = collectionModel.getOtherDataProvider().getSongPath(song);
				Log.v(TAG, "loadSong() " + song.getArtist().getName() + " - " + song.getName());
				if (loadSongIntoPlayer(song, path, mediaPlayer)) {
					setPlayerState(PlayerState.STOP);
					return true;
				}
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
		return false;
	}

	@Override
	public void setPlayMode(IPlayMode playMode) {
		currentPlaylistManager.setPlayMode(playMode);
	}

	@Override
	public void setPlaylist(IPlaylist playlist) {
		int positionToSeek = playlist.getPositionInSong();
		currentPlaylistManager.setPlaylist(playlist);
		jumpToPlaylistPosition(playlist.getPositionInList());
		seekTo(positionToSeek);
	}
}
