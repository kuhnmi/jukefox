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

import java.io.File;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

public class JukefoxTabActivity extends JukefoxActivity {

	public static enum Tab {
		PLAYER(0), LISTS(1), SEARCH(2), SPACE(3), MAP(4);

		public int id;

		Tab(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	private static final String TAG = JukefoxTabActivity.class.getSimpleName();

	protected Tab currentTab = Tab.PLAYER;
	private TabEventListener tabEventListener;
	private ImageView playPauseButton;
	private ImageView playModeButton;
	private IOnPlayerStateChangeListener playerStateEventListener;
	private IOnPlaylistStateChangeListener playlistStateEventListener;
	private OnSharedPreferenceChangeListener settingsChangeListener;
	public static Tab lastActiveTab = null;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setBackground();

		setTabButtonListener();
		setPlayerControlButtonListeners();

		settingsChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (key.equals(getString(R.string.KEY_USE_WALLPAPER_BACKGROUND))
						|| key.equals(getString(R.string.KEY_PICK_BACKGROUND_FROM_GALLERY))
						|| key.equals(getString(R.string.KEY_GALLERY_BACKGROUND_PATH))) {
					setBackground();
				}
			}
		};

		settings.addSettingsChangeListener(settingsChangeListener);
	}

	private void setBackground() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.background);
		try {
			if (settings.isUseWallpaperBackground()) {
				// WallpaperManager wpm = WallpaperManager.getInstance(this);
				// Drawable drawable = wpm.getFastDrawable();
				// if (ll != null)
				// ll.setBackgroundDrawable(drawable);
				// return;
			} else if (settings.isUseGalleryBackground()) {
				// Log.v(TAG, "Setting background Bitmap");
				BitmapDrawable bm = deocdeBackgroundBitmap(settings.getGalleryBackgroundPath());
				bm.setGravity(Gravity.CENTER);
				if (ll != null) {
					ll.setBackgroundDrawable(bm);
				}
				return;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}

		BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.d044_background);
		if (ll != null) {
			ll.setBackgroundDrawable(drawable);
		}
		return;
	}

	private BitmapDrawable deocdeBackgroundBitmap(String path) {
		// check whether path is ok
		File file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			return null;
		}
		Display display = getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();
		int maxSize = Math.max(screenHeight, screenWidth);
		Bitmap bm = AndroidUtils.getBitmapFromFile(path, maxSize);
		return new BitmapDrawable(bm);
	}

	private void setPlayerControlButtonListeners() {
		if (!tabHasPlayerControl()) {
			return;
		}
		registerPlayerButtonEventListener();
		if (playerController.isReady()) {
			updatePlayModeInfo(playerController.getPlayMode().getPlayModeType());
			setPlayPauseButton(playerController.getPlayerState());
			registerPlayerEventListeners();
		} else {
			// Do rest of operations in a thread to allow the onCreate method to
			// complete
			JoinableThread initializer = new JoinableThread(new Runnable() {

				@Override
				public void run() {
					waitForPlaybackFunctionality();
					registerPlayerEventListeners();
					// View operations must run in the main thread
					JukefoxApplication.getHandler().post(new Runnable() {

						@Override
						public void run() {

							updatePlayModeInfo(playerController.getPlayMode().getPlayModeType());
							setPlayPauseButton(playerController.getPlayerState());
						}

					});
				}

			});
			initializer.start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	private boolean tabHasPlayerControl() {
		View control = findViewById(R.id.playerConsole);
		if (control == null) {
			return false;
		}
		return true;
	}

	private void setTabButtonListener() {

		tabEventListener = controller.createTabEventListener(JukefoxTabActivity.this, currentTab);

		OnClickListener listener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Log.v(TAG, "clicked");
				tabEventListener.tabButtonClicked(v);
			}
		};
		OnLongClickListener longClickListener = new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				tabEventListener.tabButtonLongClicked(v);
				return true;
			}
		};
		ImageView playerButton = (ImageView) findViewById(R.id.playerViewButton);
		if (playerButton != null) {
			playerButton.setId(Tab.PLAYER.getId());
			playerButton.setOnClickListener(listener);
			playerButton.setOnLongClickListener(longClickListener);
		}
		ImageView listsButton = (ImageView) findViewById(R.id.listViewButton);
		if (listsButton != null) {
			listsButton.setId(Tab.LISTS.getId());
			listsButton.setOnClickListener(listener);
			listsButton.setOnLongClickListener(longClickListener);
		}
		ImageView searchButton = (ImageView) findViewById(R.id.searchViewButton);
		if (searchButton != null) {
			searchButton.setId(Tab.SEARCH.getId());
			searchButton.setOnClickListener(listener);
			searchButton.setOnLongClickListener(longClickListener);
		}
		ImageView spaceButton = (ImageView) findViewById(R.id.spaceViewButton);
		if (spaceButton != null) {
			spaceButton.setId(Tab.SPACE.getId());
			spaceButton.setOnClickListener(listener);
			spaceButton.setOnLongClickListener(longClickListener);
		}
		ImageView mapButton = (ImageView) findViewById(R.id.mapViewButton);
		if (mapButton != null) {
			mapButton.setId(Tab.MAP.getId());
			mapButton.setOnClickListener(listener);
			mapButton.setOnLongClickListener(longClickListener);
		}

	}

	protected void setCurrentTab(Tab tab) {
		currentTab = tab;
		if (tab == Tab.PLAYER) {
			ImageView playerButton = (ImageView) findViewById(R.id.playerViewButton);
			if (playerButton != null) {
				playerButton.setBackgroundResource(R.drawable.d099_bg_left_tab_button_highlighted);
				playerButton.setImageResource(R.drawable.d019_playlist);
			}
		} else if (tab == Tab.LISTS) {
			ImageView listsButton = (ImageView) findViewById(R.id.listViewButton);
			if (listsButton != null) {
				listsButton.setBackgroundResource(R.drawable.d103_bg_middle_tab_button_highlighted);
				listsButton.setImageResource(R.drawable.d010_list);
			}
		} else if (tab == Tab.SEARCH) {
			ImageView searchButton = (ImageView) findViewById(R.id.searchViewButton);
			if (searchButton != null) {
				searchButton.setBackgroundResource(R.drawable.d103_bg_middle_tab_button_highlighted);
				searchButton.setImageResource(R.drawable.d029_search);
			}
		} else if (tab == Tab.SPACE) {
			ImageView spaceButton = (ImageView) findViewById(R.id.spaceViewButton);
			if (spaceButton != null) {
				spaceButton.setBackgroundResource(R.drawable.d103_bg_middle_tab_button_highlighted);
				spaceButton.setImageResource(R.drawable.d039_space);
			}
		} else if (tab == Tab.MAP) {
			ImageView mapButton = (ImageView) findViewById(R.id.mapViewButton);
			if (mapButton != null) {
				mapButton.setBackgroundResource(R.drawable.d101_bg_right_tab_button_highlighted);
				mapButton.setImageResource(R.drawable.d013_map);
			}
		}
	}

	@Override
	protected void showStatusInfo() {
		showStatusInfo(getString(R.string.jukefox_is_currently_importing));
	}

	private void setPlayPauseButton(final PlayerState newPlayerState) {
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				if (newPlayerState == PlayerState.PLAY) {
					playPauseButton.setImageResource(R.drawable.d016_pause_button);
				} else {
					playPauseButton.setImageResource(R.drawable.d017_play_button);
				}
			}
		});
	}

	private void updatePlayModeInfo(final PlayModeType newPlayMode) {
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				Log.v(TAG, "switching play modes");
				switch (newPlayMode) {
					case SIMILAR:
						playModeButton.setImageResource(R.drawable.d035_similar);
						break;
					case SMART_SHUFFLE:
						playModeButton.setImageResource(R.drawable.d037_smart_shuffle);
						break;
					case REPEAT:
						playModeButton.setImageResource(R.drawable.d025_repeat);
						break;
					case PLAY_ONCE:
						playModeButton.setImageResource(R.drawable.d058_play_once);
						break;
					case RANDOM_SHUFFLE:
						playModeButton.setImageResource(R.drawable.d033_shuffle_collection);
						break;
					case SHUFFLE_PLAYLIST:
						playModeButton.setImageResource(R.drawable.d034_shuffle_playlist);
						break;
					case CONTEXT_SHUFFLE:
						playModeButton.setImageResource(R.drawable.d162_context_shuffle);
						break;
				}
			}
		});
	}

	private void registerPlayerButtonEventListener() {
		playModeButton = (ImageView) findViewById(R.id.playModeButton);
		playModeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tabEventListener.onPlayModeButtonClicked();
			}
		});
		playPauseButton = (ImageView) findViewById(R.id.playPauseButton);
		playPauseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tabEventListener.onPlayPauseButtonClicked();
			}
		});
		ImageView previousButton = (ImageView) findViewById(R.id.previousButton);
		previousButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tabEventListener.onPreviousButtonClicked();
			}
		});
		ImageView nextButton = (ImageView) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tabEventListener.onNextButtonClicked();
			}
		});
		ImageView playlistMenuButton = (ImageView) findViewById(R.id.playlistMenuButton);
		playlistMenuButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tabEventListener.onPlaylistMenuButtonClicked();
			}
		});
	}

	private void registerPlayerEventListeners() {

		playlistStateEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {
				Log.v(TAG, "play mode changed");
				updatePlayModeInfo(newPlayMode.getPlayModeType());
			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
			}
		};
		super.playerController.addOnPlaylistStateChangeListener(playlistStateEventListener);
		Log.v(TAG, "set playlistEventListener");

		playerStateEventListener = new IOnPlayerStateChangeListener() {

			@Override
			public void onPlayerStateChanged(PlayerState newPlayerState) {
				setPlayPauseButton(newPlayerState);
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
		playerController.removeOnPlayerStateChangeListener(playerStateEventListener);
		playerController.removeOnPlaylistStateChangeListener(playlistStateEventListener);
		super.onDestroy();
		settings.removeSettingsChangeListener(settingsChangeListener);
	}

}
