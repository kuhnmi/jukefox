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
package ch.ethz.dcg.pancho3.tablet.view.queue;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.SongTimeFormatter;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabletActivityEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.presenter.queue.QueuePresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.queue.QueuePresenter.IQueue;
import ch.ethz.dcg.pancho3.tablet.view.TabletActivity;
import ch.ethz.dcg.pancho3.tablet.widget.MagicListAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.MagicView;
import ch.ethz.dcg.pancho3.view.commons.SongProgressBar;

public class QueueFragment extends Fragment implements IQueue {

	public static final String TAG = TabletActivity.class.getSimpleName();
	private View view;
	private QueuePresenter presenter;
	private TabletActivityEventListener eventListener;
	private final Handler handler = new Handler();
	private ImageView playPauseButton;
	private Timer progressUpdateTimer;
	private Timer undoDisplayTimer;
	private SongProgressBar songProgressBar;
	private TextView progressText;
	private View undoClear;
	private SongTimeFormatter songTimeFormatter;
	private IOnPlaylistStateChangeListener playlistStateEventListener;
	private IOnPlayerStateChangeListener playerStateEventListener;
	private MagicListAdapter<PlaylistSong<BaseArtist, BaseAlbum>> adapter;
	private PlaylistAdapter playlistAdapter;

	private IReadOnlyPlayerController playManager;

	private View[] borders;
	private int borderColorUnhighlighted;
	private int borderColorHighlighted;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		playManager = JukefoxApplication.getPlayerController();
		view = inflater.inflate(R.layout.tablet_queue, null);
		borders = new View[] { view.findViewById(R.id.border0), view.findViewById(R.id.border1),
				view.findViewById(R.id.border2), view.findViewById(R.id.border3) };
		// Progress Bar is initialized as soon as possible to avoid flickering
		// of the progress.
		songProgressBar = (SongProgressBar) view.findViewById(R.id.songProgressBar);
		songProgressBar.setMax(100);
		songProgressBar.setProgress(0);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Resources resources = activity.getResources();
		borderColorUnhighlighted = resources.getColor(R.color.queue_dark);
		borderColorHighlighted = resources.getColor(R.color.highlight);
	}

	@Override
	public void highlight() {
		for (View border : borders) {
			border.setBackgroundColor(borderColorHighlighted);
		}
	}

	@Override
	public void unhighlight() {
		for (View border : borders) {
			border.setBackgroundColor(borderColorUnhighlighted);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (JukefoxApplication.getPlayerController().isReady()) {
			startUpdateTimer();
		}
	}

	@Override
	public void onPause() {
		cancelUpdateTimer();
		super.onPause();
	}

	public void initialize() {
		TabletFactory tabletFactory = ((TabletFactoryGetter) getActivity()).getTabletFactory();
		eventListener = tabletFactory.getEventListener();
		playlistAdapter = tabletFactory.createPlaylistAdapter();
		MagicView magicView = (MagicView) view.findViewById(R.id.magicview);
		adapter = tabletFactory.createMagicListAdapter(playlistAdapter, new MagicListAdapter.NewItemListener() {

			@Override
			public void onRequestNewItem() {
				// TODO: we are not using this ATM.
			}
		});
		magicView.setAdapter(adapter);

		magicView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					eventListener.onSongSelected(adapter.getHeaderItem());
				} else {
					eventListener.onSongSelected(adapter.getItem(position - 1));
				}
			}
		});
		songTimeFormatter = new SongTimeFormatter();
		songProgressBar.setReactOnMoveEvents(true);
		songProgressBar.setOnProgressChangeListener(new SongProgressBar.OnProgressChangeListener() {

			public void onProgressChanged(View v, int progress) {
				eventListener.setProgress(progress);
			}
		});
		progressText = (TextView) view.findViewById(R.id.songProgressText);

		registerButtonEventListeners(view);
		registerPlayerEventListeners();
		updateView();
		presenter = tabletFactory.getQueuePresenter();

		undoClear = view.findViewById(R.id.undo_container);
		view.findViewById(R.id.undo).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playlistAdapter.undoClear();
				undoClear.setVisibility(View.GONE);
			}
		});
		presenter.viewFinishedInit(this);
	}

	private void updateView() {
		updatePlayerState(playManager.getPlayerState());
		try {
			updateSongInfo(playManager.getCurrentSong());
		} catch (EmptyPlaylistException e) {
			updateSongInfo(null);
		}
		loadPlaylist(playManager.getCurrentPlaylist());
	}

	private void updateSongInfo(final BaseSong<? extends BaseArtist, ? extends BaseAlbum> currentSong) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (currentSong != null) {
					updateProgress();
				}
			}
		});
	}

	private void registerPlayerEventListeners() {
		playlistStateEventListener = new IOnPlaylistStateChangeListener() {

			@Override
			public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {
				updateSongInfo(newSong);
				playlistAdapter.currentSongChanged();
			}

			@Override
			public void onPlayModeChanged(IPlayMode newPlayMode) {

			}

			@Override
			public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {
				Log.v(TAG, "Playlist changed. New size: " + newPlaylist.getPlaylistSize());
				loadPlaylist(newPlaylist);
				undoClear.setVisibility(View.GONE);
			}
		};
		playManager.addOnPlaylistStateChangeListener(playlistStateEventListener);

		playerStateEventListener = new IOnPlayerStateChangeListener() {

			@Override
			public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
			}

			@Override
			public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
			}

			@Override
			public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
			}

			@Override
			public void onPlayerStateChanged(PlayerState playerState) {
				updatePlayerState(playerState);
			}
		};
		playManager.addOnPlayerStateChangeListener(playerStateEventListener);
	}

	@Override
	public void onDestroy() {
		playManager.removeOnPlayerStateChangeListener(playerStateEventListener);
		playManager.removeOnPlaylistStateChangeListener(playlistStateEventListener);
		super.onDestroy();
	}

	private void updatePlayerState(PlayerState newPlayerState) {
		if (newPlayerState == PlayerState.PLAY) {
			startUpdateTimer();
		} else {
			cancelUpdateTimer();
			updateProgress();
		}
		setPlayPauseButton(newPlayerState);
	}

	private void setPlayPauseButton(final PlayerState newPlayerState) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (newPlayerState == PlayerState.PLAY) {
					playPauseButton.setImageResource(R.drawable.d164_pause);
				} else {
					playPauseButton.setImageResource(R.drawable.d165_play);
				}
			}
		});
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
				int songPos = playManager.getPlaybackPosition() < 0 ? 0 : playManager.getPlaybackPosition();
				int songDuration = playManager.getDuration() < 0 ? 0 : playManager.getDuration();
				songProgressBar.setMax(songDuration);
				songProgressBar.setProgress(songPos);
				// TODO: can we get rid of this null check and ensure they are
				// not null?
				if (progressText != null && songTimeFormatter != null) {
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
				playlistAdapter.playlistChanged(playlist);
			}
		});
	}

	private void registerButtonEventListeners(View view) {
		playPauseButton = (ImageView) view.findViewById(R.id.playPauseButton);
		playPauseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onPlayPauseButtonClicked();
			}
		});
		playPauseButton.setOnLongClickListener(bottomBarOnLongClickListener);
		final View shuffleButton = view.findViewById(R.id.shuffleButton);
		shuffleButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playlistAdapter.shuffle();
			}
		});
		shuffleButton.setOnLongClickListener(bottomBarOnLongClickListener);
		final View clearButton = view.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clear();
			}
		});
		clearButton.setOnLongClickListener(bottomBarOnLongClickListener);
		View skipButton = view.findViewById(R.id.skipButton);
		skipButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playlistAdapter.removeItem(0);
			}
		});
		skipButton.setOnLongClickListener(bottomBarOnLongClickListener);
		playlistAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				int count = playlistAdapter.getCount();
				boolean shuffleButtonEnabled = count > 2 ? true : false;
				shuffleButton.setEnabled(shuffleButtonEnabled);
				shuffleButton.setAlpha(shuffleButtonEnabled ? 1.0f : 0.3f);
				boolean clearButtonEnabled = count > 1 ? true : false;
				clearButton.setEnabled(clearButtonEnabled);
				clearButton.setAlpha(clearButtonEnabled ? 1.0f : 0.3f);
			}
		});
	}

	public Handler getHandler() {
		return handler;
	}

	private void clear() {
		if (playlistAdapter.clearPlaylistExceptPlayingSong()) {
			if (undoDisplayTimer != null) {
				undoDisplayTimer.cancel();
			}
			undoClear.setAlpha(0.0f);
			undoClear.setVisibility(View.VISIBLE);
			undoClear.animate().alpha(0.8f);
			undoDisplayTimer = new Timer();
			undoDisplayTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					handler.post(new Runnable() {

						@Override
						public void run() {
							undoClear.setVisibility(View.GONE);
						}
					});
				}
			}, 10000);
		}
	}

	private OnLongClickListener bottomBarOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			Toast.makeText(getActivity(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
			return true;
		}
	};
}
