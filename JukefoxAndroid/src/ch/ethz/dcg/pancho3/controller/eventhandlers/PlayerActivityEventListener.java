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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.dialogs.CancelImportDialog;
import ch.ethz.dcg.pancho3.view.overlays.SongContextMenu;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;

public class PlayerActivityEventListener extends MainTabButtonEventListener {

	public static final int AUTO_SHOW_COVER_TIME = 8000;
	public static final int NUMBER_COVER_HINT_THRESSHOLD = 5;

	private static final String TAG = PlayerActivityEventListener.class.getSimpleName();
	private Timer showAlbumArtTimer;
	private boolean isCoverVisible = true;
	private PlayerActivity activity;
	private long coverTouchDownTime;
	private long lastListTouchTime;
	private float playlistTouchDownPosX;
	private boolean shownCover;
	private int firstVisibleItemInList;

	public PlayerActivityEventListener(Controller controller, PlayerActivity activity) {
		super(controller, activity, Tab.PLAYER);
		this.activity = activity;
	}

	public boolean onAlbumArtTouch(MotionEvent event, ImageView albumArt) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			coverTouchDownTime = System.currentTimeMillis();
		} else if (event.getAction() == MotionEvent.ACTION_UP) {

			// Check if was already handled
			if (coverTouchDownTime == 0) {
				return true;
			}

			long endTime = System.currentTimeMillis();
			if (endTime - coverTouchDownTime < 1000) {
				if (activity.getPlayerController().getCurrentPlaylist().getPlaylistSize() > 0) {
					controller.doHapticFeedback();
					hideAlbumArt();
				} else {
					controller.doHapticFeedback();
					controller.showToast(activity.getString(R.string.empty_playlist_use_tabs));
				}
				return true;
			}

		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			long endTime = System.currentTimeMillis();
			if (coverTouchDownTime == 0) {
				return true;
				// float endTouchPosX = event.getX();
				// float endTouchPosY = event.getY();
			}

			// if (endTouchPosX - coverTouchDownPosX > albumArt.getWidth() / 3)
			// {
			// controller.doHapticFeedback();
			// Toast.makeText(activity, activity.getString(R.string.next),
			// Toast.LENGTH_SHORT).show();
			// controller.nextButtonPressed();
			// coverTouchDownTime = 0;
			// return true;
			// } else if (coverTouchDownPosX - endTouchPosX >
			// albumArt.getWidth() / 3) {
			// controller.doHapticFeedback();
			// Toast.makeText(activity, activity.getString(R.string.previous),
			// Toast.LENGTH_SHORT).show();
			// controller.previousButtonPressed();
			// coverTouchDownTime = 0;
			// return true;
			// } else if (endTouchPosY - coverTouchDownPosY >
			// albumArt.getHeight() / 3) {
			// controller.doHapticFeedback();
			// Toast.makeText(activity,
			// activity.getString(R.string.play_pause),
			// Toast.LENGTH_SHORT).show();
			// controller.playPauseButtonPressed();
			// coverTouchDownTime = 0;
			// return true;
			// } else if (coverTouchDownPosY - endTouchPosY >
			// albumArt.getHeight() / 3) {
			// controller.doHapticFeedback();
			// BaseSong<BaseArtist, BaseAlbum> currentSong;
			// try {
			// currentSong = activity.getPlaylist().getCurrentSong();
			// if (currentSong != null) {
			// controller.showAlbumDetailInfo(activity, currentSong
			// .getAlbum());
			// }
			// } catch (EmptyPlaylistException e) {
			// Log.w(TAG, e);
			// }
			// coverTouchDownTime = 0;
			// return true;
			// } else
			if (endTime - coverTouchDownTime > 1000) {
				controller.doHapticFeedback();
				BaseSong<BaseArtist, BaseAlbum> currentSong;
				try {
					currentSong = activity.getPlayerController().getCurrentSong();
					int currentSongPosition = activity.getPlayerController().getCurrentSongIndex();
					controller.showSongContextMenu(activity, currentSong, currentSongPosition);
				} catch (EmptyPlaylistException e) {
					Log.w(TAG, e);
				}
				coverTouchDownTime = 0;
				return true;
			}
		}
		return true;
	}

	private void resetShowCoverTimer() {
		if (!controller.getSettingsReader().isAutomaticallyShowCover()) {
			return;
		}
		cancelShowAlbumArtTimer();
		showAlbumArtTimer = new Timer();
		showAlbumArtTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (System.currentTimeMillis() > lastListTouchTime + AUTO_SHOW_COVER_TIME) {
					showAlbumArt();
				} else {
					resetShowCoverTimer();
				}
			}
		}, AUTO_SHOW_COVER_TIME);
	}

	private void cancelShowAlbumArtTimer() {
		if (showAlbumArtTimer != null) {
			showAlbumArtTimer.cancel();
		}
		showAlbumArtTimer = null;
	}

	public void hideAlbumArt() {
		if (!isCoverVisible) {
			return;
		}
		isCoverVisible = false;
		activity.getHandler().post(new Runnable() {

			@Override
			public void run() {
				activity.findViewById(R.id.playlist).setVisibility(View.VISIBLE);
				int numberCoverClicked = activity.getSettings().getCoverHintCountPlayer();
				if (numberCoverClicked < NUMBER_COVER_HINT_THRESSHOLD) {
					controller.getSettingsEditor().setCoverHintCountPlayer(numberCoverClicked + 1);
					activity.findViewById(R.id.clickCover).setVisibility(View.GONE);
				}
				// activity.findViewById(R.id.bigCoverArt)
				// .setVisibility(View.GONE);
				ListView playlist = (ListView) activity.findViewById(R.id.playlist);
				int position = 0;
				try {
					position = controller.getPlayerController().getCurrentSongIndex();
					playlist.setSelection(position);
				} catch (EmptyPlaylistException e) {
				}
			}
		});
		resetShowCoverTimer();
	}

	public void showAlbumArt() {
		if (isCoverVisible) {
			return;
		}
		isCoverVisible = true;
		activity.getHandler().post(new Runnable() {

			@Override
			public void run() {
				activity.findViewById(R.id.playlist).setVisibility(View.GONE);
				int numberCoverClicked = activity.getSettings().getCoverHintCountPlayer();
				if (numberCoverClicked < NUMBER_COVER_HINT_THRESSHOLD) {
					activity.findViewById(R.id.clickCover).setVisibility(View.VISIBLE);
				}
				// activity.findViewById(R.id.bigCoverArt).setVisibility(
				// View.VISIBLE);
			}
		});
	}

	public void setProgress(int progress) {
		PlayerState state = controller.getPlayerController().getPlayerState();
		//		if (state == PlayerState.PLAY || state == PlayerState.PAUSE) {
		controller.getPlayerController().seekTo(progress);
		activity.updateProgress();
		//		}
	}

	public void onPlaylistItemClicked(int position) {
		try {
			//			Log.v(TAG, "Clicked at pos: " + position);
			controller.getPlayerController().playSongAtPosition(position);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
	}

	public void onScroll(int firstVisibleItem) {
		if (firstVisibleItemInList != firstVisibleItem) {
			Log.v(TAG, "onScroll()");
			resetShowCoverTimer();
			firstVisibleItemInList = firstVisibleItem;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onPlaylistItemLongClicked(int position) {
		if (!isCoverVisible) {
			BaseSong<BaseArtist, BaseAlbum> song = (BaseSong<BaseArtist, BaseAlbum>) activity.getList()
					.getItemAtPosition(position);
			Intent intent = new Intent(activity, SongContextMenu.class);
			intent.putExtra(Controller.INTENT_EXTRA_SONG_PLAYLIST_POSITION, position);
			intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
			activity.startActivity(intent);
		}
		return true;
	}

	public void detectedFirstStart() {
		// controller.showDirectorySelectionDialog(activity);
		// controller.showTakeATourDialog();
		controller.showFirstStartDialog();
	}

	public void sdCardProblemDetected() {
		controller.showSdCardProblemDialog();
	}

	public boolean onListTouch(MotionEvent event, ImageView albumArt) {
		lastListTouchTime = event.getEventTime();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			playlistTouchDownPosX = event.getX();
			shownCover = false;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE && shownCover == false) {
			float endTouchPosX = event.getX();

			if (Math.abs(endTouchPosX - playlistTouchDownPosX) > (float) albumArt.getWidth() / 2) {
				shownCover = true;
				controller.doHapticFeedback();
				cancelShowAlbumArtTimer();
				showAlbumArt();
				return true;
			}
		}
		return false;
	}

	public void movePlaylistElement(int from, int to) {
		try {
			controller.getPlayerController().moveSong(from, to);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public boolean onKey(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKey()");
		if (keyCode != KeyEvent.KEYCODE_BACK) {
			return super.onKey(keyCode, event);
		}
		if (activity.getSettings().isAutomaticallyShowCover() && !isCoverVisible) {
			cancelShowAlbumArtTimer();
			showAlbumArt();
			return true;
		} else {
			if (activity.getApplicationState().isImporting()) {
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						Intent intent = new Intent(activity, CancelImportDialog.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
						activity.startActivity(intent);
					}

				}, 2000);
			}
			return super.onKey(keyCode, event);
		}
	}
}
