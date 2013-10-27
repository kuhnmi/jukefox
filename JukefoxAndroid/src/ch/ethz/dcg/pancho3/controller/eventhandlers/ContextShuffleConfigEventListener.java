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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import android.content.Context;
import android.widget.EditText;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.ContextShufflePlayMode;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.data.context.MusicContext;
import ch.ethz.dcg.pancho3.view.overlays.ContextShuffleConfig;

public class ContextShuffleConfigEventListener extends BaseJukefoxEventListener {

	private ContextShuffleConfig activity;
	private AndroidPlayerController playManager;
	private ContextShufflePlayMode contextPlayMode;

	public ContextShuffleConfigEventListener(Controller controller, ContextShuffleConfig activity) {
		super(controller, activity);
		this.activity = activity;
		this.playManager = controller.getPlayerController();
	}

	public static final String TAG = ContextShuffleConfigEventListener.class.getSimpleName();

	public void onOkButtonClicked() {
		// is instance of ....
		IPlayMode playMode = playManager.getPlayMode();
		if (playMode instanceof ContextShufflePlayMode) {
			contextPlayMode = (ContextShufflePlayMode) playMode;
			contextPlayMode.lockRegion();
			saveContextShuffle();
			activity.finish();
		}
	}

	private void saveContextShuffle() {
		String contextName = ((EditText) activity.findViewById(R.id.contextName)).getText().toString();
		if (contextName.equals("")) {
			return;
		}
		String filename = contextName + ".ctx";
		try {
			FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			IPlayMode playMode = playManager.getPlayMode();
			if (playMode instanceof ContextShufflePlayMode) {
				contextPlayMode = (ContextShufflePlayMode) playMode;
				out.writeObject(new MusicContext(contextName, contextPlayMode.getPermanentSmartShuffleState()));
			}
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}

	public void onActivityStarted() {
		playManager.stop();
		if (playManager.getPlayMode().getPlayModeType() != PlayModeType.CONTEXT_SHUFFLE) {
			playManager.setPlayMode(PlayModeType.CONTEXT_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		} else {
			IPlayMode playMode = playManager.getPlayMode();
			if (playMode instanceof ContextShufflePlayMode) {
				contextPlayMode = (ContextShufflePlayMode) playMode;
				contextPlayMode.unlockRegion();
			}
		}
		playManager.clearPlaylist();
		appendRandomSong();
		// try {
		// playManager.playSongAtPosition(0);
		// } catch (PlaylistPositionOutOfRangeException e) {
		// // TODO Auto-generated catch block
		// Log.w(TAG, e);
		// }
		// seekToHalf();
	}

	private void seekToHalf() {
		int duration = playManager.getDuration();
		playManager.seekTo(duration / 2);
	}

	private void appendRandomSong() {
		List<SongCoords> songs = null;
		try {
			songs = activity.getCollectionModel().getSongCoordinatesProvider().getRandomSongsWithCoords(1);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		if (songs != null && songs.size() > 0) {
			BaseSong<BaseArtist, BaseAlbum> song;
			try {
				song = activity.getCollectionModel().getSongProvider().getBaseSong(songs.get(0));
				PlaylistSong<BaseArtist, BaseAlbum> pSong = new PlaylistSong<BaseArtist, BaseAlbum>(song,
						SongSource.RANDOM_SONG);
				playManager.appendSongAtEnd(pSong);
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
		}
	}

	public void onLikeButtonClicked() {
		appendRandomSong();
		try {
			contextPlayMode.likedSong(playManager.getCurrentSong());
			controller.getPlayerController().playNext();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
		}
		seekToHalf();
	}

	public void onDislikeButtonClicked() {
		appendRandomSong();
		try {
			contextPlayMode.dislikedSong(playManager.getCurrentSong());
			controller.getPlayerController().playNext();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
		}
		seekToHalf();
	}

	public void onTagClicked(MusicContext context) {
		playManager.stop();
		playManager.setPlayMode(PlayModeType.CONTEXT_SHUFFLE, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
		playManager.clearPlaylist();

		contextPlayMode = (ContextShufflePlayMode) playManager.getPlayMode();
		contextPlayMode.loadPermanentState(context.getSmartShuffleState());
		contextPlayMode.lockRegion();
		try {
			playManager.playNext();
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		} catch (NoNextSongException e) {
			Log.w(TAG, e);
		}
		activity.finish();
	}
}
