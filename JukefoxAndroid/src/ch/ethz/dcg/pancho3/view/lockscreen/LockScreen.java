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
package ch.ethz.dcg.pancho3.view.lockscreen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.BitmapReflection;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.commons.TriggeringSlider;

public class LockScreen extends JukefoxActivity {

	public static final String TAG = LockScreen.class.getSimpleName();
	private IOnPlaylistStateChangeListener playlistStateEventListener;
	private IOnPlayerStateChangeListener playerStateEventListener;
	private ImageView playPauseButton;
	private Handler handler;

	private TriggeringSlider sliderLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Log.v(TAG, "onCreate()");

		if (!settings.isLockScreenControls() || !playerController.isReady()
				|| playerController.getPlayerState() != PlayerState.PLAY) {
			// Log.v(TAG, "onCreate() 2");
			finish();
			return;
		}

		handler = new Handler();

		setWindowProperties();

		setContentView(R.layout.lockscreen);

		sliderLock = (TriggeringSlider) findViewById(R.id.sliderLock);
		Bitmap lockBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.d087_lockscreen_lock);
		sliderLock.setBitmap(lockBitmap);
		sliderLock.setLockHeight(50);
		sliderLock.setLockWidth(64);
		sliderLock.addOnTriggerListener(new TriggeringSlider.OnTriggerListener() {

			@Override
			public void onTrigger() {
				finish();
			}
		});

		updateCoverAndText();

		registerButtons();

		registerPlayerEventListeners();

		updatePlayerState(playerController.getPlayerState());

		Log.v(TAG, "onCreate() 3");
	}

	private void setWindowProperties() {

		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		LayoutParams params = getWindow().getAttributes();
		getWindow().setAttributes(params);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				// dismiss();
				return true;
			case KeyEvent.KEYCODE_SEARCH:
			case KeyEvent.KEYCODE_BACK:
				// finish();
				// return true;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return true;
			default:
				return super.onKeyDown(keyCode, event);
		}
	}

	private void updateCoverAndText() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					BaseSong<BaseArtist, BaseAlbum> song = playerController.getCurrentSong();
					TextView nowPlaying = (TextView) findViewById(R.id.nowPlayingText);
					nowPlaying.setText(song.getArtist().getName() + " - " + song.getName());
					ImageView albumArt = (ImageView) findViewById(R.id.bigCoverArt);
					try {
						Bitmap bitmap = albumArtProvider.getAlbumArt(song.getAlbum(), false);
						albumArt.setImageBitmap(BitmapReflection.getReflection(bitmap));
					} catch (NoAlbumArtException e) {
						albumArt.setImageResource(R.drawable.d005_empty_cd);
					}
				} catch (EmptyPlaylistException e) {
					Log.w(TAG, e);
				}
			}
		});

	}

	private void registerButtons() {
		playPauseButton = (ImageView) findViewById(R.id.playPauseButton);
		playPauseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LockScreen.this, PlayerService.class);
				intent.setAction(PlayerService.ACTION_PLAY_PAUSE);
				startService(intent);
			}
		});
		ImageView previousButton = (ImageView) findViewById(R.id.previousButton);
		previousButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LockScreen.this, PlayerService.class);
				intent.setAction(PlayerService.ACTION_PREVIOUS);
				startService(intent);
			}
		});
		ImageView nextButton = (ImageView) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Log.v(TAG, "nextButton clicked");
				Intent intent = new Intent(LockScreen.this, PlayerService.class);
				intent.setAction(PlayerService.ACTION_NEXT);
				startService(intent);
			}
		});
		// View statusBar = findViewById(R.id.statusBarWindow);
		// statusBar.setOnClickListener(new OnClickListener() {
		//			
		// @Override
		// public void onClick(View v) {
		//				
		// }
		// });

		// ImageView logo = (ImageView) findViewById(R.id.jukefoxLogo);
		// logo.setOnTouchListener(new OnTouchListener() {
		//
		// @Override
		// public boolean onTouch(View v, MotionEvent event) {
		// return onLogoTouched(v, event);
		// }
		//
		// });
	}

	@SuppressWarnings("unused")
	private boolean onLogoTouched(View v, MotionEvent event) {
		return true;
	}

	private void registerPlayerEventListeners() {
		playlistStateEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				updateCoverAndText();
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {

			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {

			}
		};
		super.playerController.addOnPlaylistStateChangeListener(playlistStateEventListener);

		playerStateEventListener = new IOnPlayerStateChangeListener() {

			@Override
			public void onPlayerStateChanged(PlayerState newPlayerState) {
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

	private void updatePlayerState(final PlayerState newPlayerState) {
		handler.post(new Runnable() {

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

	@Override
	protected void onPause() {
		Log.v(TAG, "onPause()");
		super.onPause();
	}

	protected boolean isScreenOff() {
		return !JukefoxApplication.isScreenOn();
	}

	@Override
	protected void onResume() {
		// Log.v(TAG, "onResume()");
		// if (!JukefoxApplication.isScreenLocked()) {
		// Log.v(TAG, "Screen not locked. Terminating lock screen.");
		// Intent intent = new Intent(this, PlayerActivity.class);
		// startActivity(intent);
		// finish();
		// }
		updateCoverAndText();
		super.onResume();
	}

}
