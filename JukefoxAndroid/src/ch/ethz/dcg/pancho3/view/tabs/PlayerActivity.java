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

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.SongTimeFormatter;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SmartShufflePlayMode2;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlayerActivityEventListener;
import ch.ethz.dcg.pancho3.view.commons.BitmapReflection;
import ch.ethz.dcg.pancho3.view.commons.SongProgressBar;
import ch.ethz.dcg.pancho3.view.overlays.AgentVotesToast;
import ch.ethz.dcg.pancho3.view.tabs.lists.MoveableTextListAdapter;

import com.commonsware.cwac.tlv.TouchListView;

public class PlayerActivity extends JukefoxTabActivity {

	public static final String TAG = PlayerActivity.class.getSimpleName();

	private PlayerActivityEventListener eventListener;
	private Handler handler;
	private Timer progressUpdateTimer;
	private SongProgressBar songProgressBar;
	private TextView progressText;
	private SongTimeFormatter songTimeFormatter;
	private TextView nowPlayingText;
	private TouchListView list;
	private ImageView albumArt;
	private IOnPlaylistStateChangeListener playlistStateEventListener;
	private IOnPlayerStateChangeListener playerStateEventListener;
	private MoveableTextListAdapter<IBaseListItem> adapter;

	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {

		@Override
		public void drop(int from, int to) {
			Log.v(TAG, "Move playlist item from " + from + " to " + to);
			eventListener.movePlaylistElement(from, to);
		}
	};

	private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {

		@Override
		public void remove(int which) {
			adapter.remove(adapter.getItem(which));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.player);

		Log.v(TAG, "Settings tabs");

		setCurrentTab(Tab.PLAYER);

		Log.v(TAG, "Registering Listeners");

		registerButtonEventListeners();

		// Progress Bar is initialized as soon as possible to avoid flickering
		// of the progress
		songProgressBar = (SongProgressBar) findViewById(R.id.songProgressBar);
		songProgressBar.setMax(100);
		songProgressBar.setProgress(0);
		eventListener = controller.createPlayerViewEventListener(this);

		registerListEventListeners();

		Log.v(TAG, "initialize view...");
		handler = new Handler();
		initializeView();

		setCoverClickHint();

		Log.v(TAG, "initialize view completed");

		if (!AndroidUtils.isSdCardOk()) {
			eventListener.sdCardProblemDetected();
			finish();
			return;
		}

		if (applicationState.isFirstStart()) {
			Log.d(TAG, "first start.");
			eventListener.detectedFirstStart();
		}

		Log.d(TAG, "PlayerActivity.onCreate() finished.");

	}

	private void setCoverClickHint() {
		int numberCoverClicked = getSettings().getCoverHintCountPlayer();
		if (numberCoverClicked < PlayerActivityEventListener.NUMBER_COVER_HINT_THRESSHOLD) {
			findViewById(R.id.clickCover).setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");
		super.onResume();
		if (settings.isAutomaticallyShowCover()) {
			eventListener.showAlbumArt();
		} else {
			eventListener.hideAlbumArt();
		}
		if (playerController.isReady()) {
			startUpdateTimer();
		}
		Log.v(TAG, "onResume() finished.");
	}

	@Override
	protected void onPause() {
		cancelUpdateTimer();
		super.onPause();
	}

	private void registerListEventListeners() {
		list = (TouchListView) findViewById(R.id.playlist);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				eventListener.onPlaylistItemClicked(position);
			}
		});
		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				return eventListener.onPlaylistItemLongClicked(position);
			}
		});
		list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				eventListener.onScroll(firstVisibleItem);
			}
		});
	}

	private void initializeView() {
		// Do rest of operations in a thread to allow the onCreate method to
		// complete
		JoinableThread initializer = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				waitForPlaybackFunctionality();
				// View operations must run in the main thread
				handler.post(new Runnable() {

					@Override
					public void run() {

						// Log.v(TAG, "Setting cover art");
						albumArt = (ImageView) findViewById(R.id.bigCoverArt);
						// if (!settings.isAutomaticallyShowCover() &&
						// (settings.isUseGalleryBackground() ||
						// settings.isUseWallpaperBackground())) {
						// albumArt.setVisibility(View.GONE);
						// }
						songTimeFormatter = new SongTimeFormatter();
						songProgressBar.setReactOnMoveEvents(true);
						songProgressBar.setOnProgressChangeListener(new SongProgressBar.OnProgressChangeListener() {

							public void onProgressChanged(View v, int progress) {
								eventListener.setProgress(progress);
							}
						});
						progressText = (TextView) findViewById(R.id.songProgressText);
						nowPlayingText = (TextView) findViewById(R.id.nowPlayingText);
						// Log.v(TAG, "Register player events");
						registerPlayerEventListeners();
						// Log.v(TAG, "updating view");
						updateView();
						// Log.v(TAG, "updated view");
					}

				});
			}

		});
		initializer.start();
	}

	private void updateView() {
		updatePlayerState(playerController.getPlayerState());
		try {
			updateSongInfo(playerController.getCurrentSong());
		} catch (EmptyPlaylistException e) {
			// Log.w(TAG, e);
			updateSongInfo(null);
		}
		loadPlaylist(playerController.getCurrentPlaylist());
		list.setSelection(PlayerActivity.this.playerController.getCurrentPlaylist().getPositionInList());
	}

	private void updateSongInfo(final BaseSong<? extends BaseArtist, ? extends BaseAlbum> currentSong) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (currentSong == null) {
					Log.v(TAG, "currentSong == null");
					nowPlayingText.setText(getString(R.string.artist_title_place_holder));
					albumArt.setImageResource(R.drawable.d137_fox_reflection);
					return;
				}
				nowPlayingText.setText(currentSong.getArtist().getName() + " - " + currentSong.getName());
				Log.v(TAG, "currentSong != null");
				updateProgress();
				setAlbumArt(currentSong);
				try {
					list.setSelection(PlayerActivity.this.playerController.getCurrentPlaylist().getPositionInList());
					// final View v = list.getSelectedView();
					// if (v != null) {
					// JukefoxApplication.getHandler().post(new Runnable() {
					//
					// @Override
					// public void run() {
					// v
					// .setBackgroundResource(R.drawable.d046_bg_button);
					// }
					//
					// });
					// }
					updateHighlightedListEntry();
				} catch (EmptyPlaylistException e1) {
					// TODO Auto-generated catch block
					Log.w(TAG, e1);
				}
			}

		});
	}

	private void setAlbumArt(final BaseSong<? extends BaseArtist, ? extends BaseAlbum> currentSong) {
		try {
			Bitmap bitmap = albumArtProvider.getAlbumArt(currentSong.getAlbum(), false);
			BitmapReflection.setReflectionToImageViewAsync(bitmap, albumArt);

		} catch (NoAlbumArtException e) {
			Log.w(TAG, e);
			albumArt.setImageResource(R.drawable.d005_empty_cd);
		}
	}

	private void updateHighlightedListEntry() throws EmptyPlaylistException {
		if (adapter != null) {
			adapter.setHighlightPosition(PlayerActivity.this.playerController.getCurrentPlaylist().getPositionInList());
		}
	}

	private void registerPlayerEventListeners() {
		// applicationState.waitForPlaybackFunctionality();
		playlistStateEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				updateSongInfo(newSong);

				// Show a toast with the votes of the top agents
				IPlayMode playMode = playerController.getPlayMode();
				if (playMode.getPlayModeType() == PlayModeType.SMART_SHUFFLE) {
					if (playMode instanceof SmartShufflePlayMode2) {
						SmartShufflePlayMode2 smartShufflePlayMode = (SmartShufflePlayMode2) playMode;

						Map<IAgent, Float> agentVotes = smartShufflePlayMode.getAgentVotesForCurrentSong();
						new AgentVotesToast(newSong, agentVotes, PlayerActivity.this).show();
					}
				}
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {

			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
				Log.v(TAG, "Playlist changed. New size: " + newPlaylist.getPlaylistSize());
				loadPlaylist(newPlaylist);
			}
		};
		super.playerController.addOnPlaylistStateChangeListener(playlistStateEventListener);

		playerStateEventListener = new IOnPlayerStateChangeListener() {

			@Override
			public void onPlayerStateChanged(PlayerState newPlayerState) {
				Log.v(TAG, "Player state changed");
				updatePlayerState(newPlayerState);
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
		};
		super.playerController.addOnPlayerStateChangeListener(playerStateEventListener);
	}

	@Override
	protected void onDestroy() {
		super.playerController.removeOnPlayerStateChangeListener(playerStateEventListener);
		super.playerController.removeOnPlaylistStateChangeListener(playlistStateEventListener);
		super.onDestroy();
	}

	private void updatePlayerState(PlayerState newPlayerState) {
		if (newPlayerState == PlayerState.PLAY) {
			startUpdateTimer();
		} else {
			cancelUpdateTimer();
			updateProgress();
		}
	}

	private void cancelUpdateTimer() {
		if (progressUpdateTimer != null) {
			progressUpdateTimer.cancel();
		}
	}

	private void startUpdateTimer() {
		cancelUpdateTimer();
		progressUpdateTimer = new Timer();
		progressUpdateTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				updateProgress();
			}

		}, 0, 1000);
		Log.w(TAG, "started update timer");
	}

	public void updateProgress() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				int songPos = playerController.getPlaybackPosition() < 0 ? 0 : playerController.getPlaybackPosition();
				int songDuration = playerController.getDuration() < 0 ? 0 : playerController.getDuration();
				//				Log.v(TAG, "songDuration: " + songDuration);
				//				Log.v(TAG, "songPos: " + songPos);
				// Check for null to avoid race conditions at the start
				if (songProgressBar != null) {
					songProgressBar.setMax(songDuration);
					songProgressBar.setProgress(songPos);
				}
				if (progressText != null) {
					progressText.setText(songTimeFormatter.format(songPos) + "/"
							+ songTimeFormatter.format(songDuration));
				}
			}

		});
	}

	private void loadPlaylist(final IReadOnlyPlaylist playlist) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				// int position = list.getFirstVisiblePosition();
				adapter = new MoveableTextListAdapter(PlayerActivity.this, R.layout.moveabletextlistitem, playlist
						.getSongList());
				try {
					adapter.setHighlightPosition(PlayerActivity.this.playerController.getCurrentSongIndex());
				} catch (EmptyPlaylistException e) {
					Log.w(TAG, "Empty playlist: Cannot highlight position.");
				}
				list.setAdapter(adapter);

				try {
					list.setSelection(PlayerActivity.this.playerController.getCurrentSongIndex());
				} catch (EmptyPlaylistException e) {
					Log.w(TAG, "Empty playlist: Cannot select song.");
				}

				list.setDropListener(onDrop);
				list.setRemoveListener(onRemove);

			}
		});
	}

	private void registerButtonEventListeners() {
		final ImageView albumArt = (ImageView) findViewById(R.id.bigCoverArt);
		albumArt.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onAlbumArtTouch(event, albumArt);
			}
		});
		ListView list = (ListView) findViewById(R.id.playlist);
		list.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return eventListener.onListTouch(event, albumArt);
			}
		});
	}

	public Handler getHandler() {
		return handler;
	}

	public TouchListView getList() {
		return list;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Log.v(TAG, "onNewIntent()");
		if (applicationState.isImporting()) {
			showStatusInfo(getString(R.string.jukefox_is_currently_importing));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (eventListener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
