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
package ch.ethz.dcg.pancho3.view.statistics;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.TimeFilter;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsArtist;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsGenre;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsSong;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.commons.SimpleGestureFilter;
import ch.ethz.dcg.pancho3.view.commons.SimpleGestureFilter.SimpleGestureListener;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.statistics.IStatisticsDisplay.OnClickListener;
import ch.ethz.dcg.pancho3.view.statistics.IStatisticsDisplay.OnLongClickListener;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreListMenu;

public class StatisticsActivity extends JukefoxActivity {

	public static final String TAG = StatisticsActivity.class.getSimpleName();

	public static final String DATATYPE_KEY = "statistics:dataTypeKey";
	public static final int DATATYPE_ALBUM = 0;
	public static final int DATATYPE_ARTIST = 1;
	public static final int DATATYPE_GENRE = 2;
	public static final int DATATYPE_SONG = 3;

	public static final String WHAT_KEY = "statistics:whatKey";
	public static final int WHAT_NEW = 0;
	public static final int WHAT_TOP = 1;
	public static final int WHAT_FLOP = 2;
	public static final int WHAT_SUGGESTIONS = 3;

	public static final String DISPLAYTYPE_KEY = "statistics:displayType";
	public static final int DISPLAYTYPE_LIST = 0;
	public static final int DISPLAYTYPE_TAGCLOUD = 1;

	public static int currentDataType = DATATYPE_ARTIST; // Static, so that this will be remembered over multiple calls
	public static int currentWhat = WHAT_TOP;
	public static int currentDisplayType = DISPLAYTYPE_LIST;

	private static int MAX_DATA_ITEMS = 100;

	private ArtistClickListener artistClickListener;
	private AlbumClickListener albumClickListener;
	private GenreClickListener genreClickListener;
	private SongClickListener songClickListener;

	private SimpleGestureFilter gestureFilter;

	private TextView txtTitle;

	private ImageView btnAlbums;
	private ImageView btnArtists;
	private ImageView btnGenres;
	private ImageView btnSongs;

	private ImageView btnNew;
	private ImageView btnTop;
	private ImageView btnFlop;
	private ImageView btnSuggestion;

	private static final int MENU_ARTIST_ACTIVE = R.drawable.d002_artists;
	private static final int MENU_ALBUM_ACTIVE = R.drawable.d001_albums;
	private static final int MENU_SONG_ACTIVE = R.drawable.d038_songs;
	private static final int MENU_GENRE_ACTIVE = R.drawable.d009_genres;
	private static final int MENU_NEW_ACTIVE = R.drawable.d176_hot;
	private static final int MENU_TOP_ACTIVE = R.drawable.d173_thumbs_up;
	private static final int MENU_FLOP_ACTIVE = R.drawable.d174_thumbs_down;
	private static final int MENU_SUGGESTION_ACTIVE = R.drawable.d175_star;
	private static final int MENU_ARTIST_INACTIVE = R.drawable.d182_artists_inactive;
	private static final int MENU_ALBUM_INACTIVE = R.drawable.d181_albums_inactive;
	private static final int MENU_SONG_INACTIVE = R.drawable.d184_songs_inactive;
	private static final int MENU_GENRE_INACTIVE = R.drawable.d183_genres_inactive;
	private static final int MENU_NEW_INACTIVE = R.drawable.d180_hot_inactive;
	private static final int MENU_TOP_INACTIVE = R.drawable.d177_thumbs_up_inactive;
	private static final int MENU_FLOP_INACTIVE = R.drawable.d178_thumbs_down_inactive;
	private static final int MENU_SUGGESTION_INACTIVE = R.drawable.d179_star_inactive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.statistics);

		artistClickListener = new ArtistClickListener();
		albumClickListener = new AlbumClickListener();
		genreClickListener = new GenreClickListener();
		songClickListener = new SongClickListener();

		txtTitle = (TextView) findViewById(R.id.txtStatisticsTitle);

		btnAlbums = (ImageView) findViewById(R.id.btnStatisticsAlbums);
		btnArtists = (ImageView) findViewById(R.id.btnStatisticsArtists);
		btnGenres = (ImageView) findViewById(R.id.btnStatisticsGenres);
		btnSongs = (ImageView) findViewById(R.id.btnStatisticsSongs);

		btnNew = (ImageView) findViewById(R.id.btnStatisticsNewData);
		btnTop = (ImageView) findViewById(R.id.btnStatisticsTop);
		btnFlop = (ImageView) findViewById(R.id.btnStatisticsFlop);
		btnSuggestion = (ImageView) findViewById(R.id.btnStatisticsSuggestions);

		processIntent(getIntent());

		registerEventListeners();
	}

	private void processIntent(Intent intent) {
		Bundle extras = intent.getExtras();

		if (applicationState.isImporting() && !applicationState.isBaseDataCommitted()) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		}

		// Find out which data should be displayed
		if (extras != null && extras.containsKey(DATATYPE_KEY)) {
			currentDataType = extras.getInt(DATATYPE_KEY);
		}

		// Find out what information should be shown
		if (extras != null && extras.containsKey(WHAT_KEY)) {
			currentWhat = extras.getInt(WHAT_KEY);
		}

		// Find out what display type should be chosen
		if (extras != null && extras.containsKey(DISPLAYTYPE_KEY)) {
			currentDisplayType = extras.getInt(DISPLAYTYPE_KEY);
		}

		// Highlight the menus
		adjustMenu(currentDataType, currentWhat);

		// Get the display provider
		final IStatisticsDisplay display;
		switch (currentWhat) {
			case WHAT_NEW:
				display = new StatisticsListDisplay(this, StatisticsListDisplay.DataType.Date, collectionModel);
				break;

			case WHAT_TOP:
			case WHAT_FLOP:
			case WHAT_SUGGESTIONS:
			default:
				switch (currentDisplayType) {
					case DISPLAYTYPE_TAGCLOUD:
						display = new StatisticsTagCloudDisplay(this);
						break;

					case DISPLAYTYPE_LIST:
					default:
						display = new StatisticsListDisplay(this, StatisticsListDisplay.DataType.Rating,
								collectionModel);
						break;
				}
				break;
		}

		// Clear the old display
		LinearLayout l = (LinearLayout) findViewById(R.id.statistics_layout);
		l.removeAllViews();

		// Load the data in own thread and show progress spinner
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.loading));
		progress.setIndeterminate(true);

		class ProgressTimerTask extends TimerTask {

			public boolean showProgress = true;

			@Override
			public void run() {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						synchronized (progress) {
							if (showProgress) {
								progress.show();
							}
						}
					}
				});
			}
		}
		Timer t = new Timer();
		final ProgressTimerTask progressTimerTask = new ProgressTimerTask();
		t.schedule(progressTimerTask, 1000);

		Thread dataLoadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// Load the data
				final List<? extends IStatisticsData> data;
				final OnClickListener onClickListener;
				final OnLongClickListener onLongClickListener;
				switch (currentWhat) {
					case WHAT_NEW: {
						switch (currentDataType) {
							case DATATYPE_ALBUM:
								data = statisticsProvider.getRecentlyImportedAlbums(MAX_DATA_ITEMS);
								onClickListener = albumClickListener;
								onLongClickListener = albumClickListener;
								break;

							case DATATYPE_GENRE:
								data = statisticsProvider.getRecentlyImportedGenres(MAX_DATA_ITEMS);
								onClickListener = genreClickListener;
								onLongClickListener = null;
								break;

							case DATATYPE_SONG:
								data = statisticsProvider.getRecentlyImportedSongs(MAX_DATA_ITEMS);
								onClickListener = songClickListener;
								onLongClickListener = null;
								break;

							case DATATYPE_ARTIST:
							default:
								data = statisticsProvider.getRecentlyImportedArtists(MAX_DATA_ITEMS);
								onClickListener = artistClickListener;
								onLongClickListener = artistClickListener;
								break;
						}
						break;
					}

					case WHAT_SUGGESTIONS: {
						switch (currentDataType) {
							case DATATYPE_ALBUM:
								data = statisticsProvider.getSuggestedAlbums(statisticsProvider.allTheTime(),
										TimeFilter.NONE, MAX_DATA_ITEMS, true);
								onClickListener = albumClickListener;
								onLongClickListener = albumClickListener;
								break;

							case DATATYPE_GENRE:
								data = statisticsProvider.getSuggestedGenres(statisticsProvider.allTheTime(),
										TimeFilter.NONE, MAX_DATA_ITEMS, true);
								onClickListener = genreClickListener;
								onLongClickListener = null;
								break;

							case DATATYPE_SONG:
								data = statisticsProvider.getSuggestedSongs(statisticsProvider.allTheTime(),
										TimeFilter.NONE, MAX_DATA_ITEMS, true);
								onClickListener = songClickListener;
								onLongClickListener = null;
								break;

							case DATATYPE_ARTIST:
							default:
								data = statisticsProvider.getSuggestedArtists(statisticsProvider.allTheTime(),
										TimeFilter.NONE, MAX_DATA_ITEMS, true);
								onClickListener = artistClickListener;
								onLongClickListener = artistClickListener;
								break;
						}
						break;
					}

					case WHAT_TOP:
					case WHAT_FLOP:
					default: {
						IDbStatisticsHelper.Direction direction = (currentWhat == WHAT_FLOP) ? IDbStatisticsHelper.Direction.FLOP
								: IDbStatisticsHelper.Direction.TOP;

						switch (currentDataType) {
							case DATATYPE_SONG:
								data = statisticsProvider.getTopSongs(MAX_DATA_ITEMS, statisticsProvider.allTheTime(),
										TimeFilter.NONE, direction, true);
								onClickListener = songClickListener;
								onLongClickListener = null;
								break;

							case DATATYPE_ALBUM:
								data = statisticsProvider.getTopAlbums(MAX_DATA_ITEMS, statisticsProvider.allTheTime(),
										TimeFilter.NONE, direction, true);
								onClickListener = albumClickListener;
								onLongClickListener = albumClickListener;
								break;

							case DATATYPE_ARTIST:
							default:
								data = statisticsProvider.getTopArtists(MAX_DATA_ITEMS,
										statisticsProvider.allTheTime(), TimeFilter.NONE, direction, true);
								onClickListener = artistClickListener;
								onLongClickListener = artistClickListener;
								break;

							case DATATYPE_GENRE:
								data = statisticsProvider.getTopGenres(MAX_DATA_ITEMS, statisticsProvider.allTheTime(),
										TimeFilter.NONE, direction, true);
								onClickListener = genreClickListener;
								onLongClickListener = null;
								break;
						}
						break;
					}
				}

				Runnable uiRunnable = new Runnable() {

					@Override
					public void run() {
						// Show the data
						if (data != null) {
							display.inflate(data, onClickListener, onLongClickListener, R.id.statistics_layout);
						}

						synchronized (progressTimerTask) {
							progressTimerTask.showProgress = false;

							// Cancel the loading screen
							progress.cancel();
						}
					}
				};
				StatisticsActivity.this.runOnUiThread(uiRunnable);
			}
		});
		dataLoadThread.start();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		gestureFilter.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	/**
	 * Highlights the selected data type and what button and sets the correct title.
	 * 
	 * @param selectedDataType
	 *            The data type which should be highlighted
	 * @param selectedWhat
	 *            The what which should be highlighted
	 */
	private void adjustMenu(int selectedDataType, int selectedWhat) {
		// Hightlight the correct data type
		btnAlbums.setImageResource(DATATYPE_ALBUM == selectedDataType ? MENU_ALBUM_ACTIVE : MENU_ALBUM_INACTIVE);
		btnArtists.setImageResource(DATATYPE_ARTIST == selectedDataType ? MENU_ARTIST_ACTIVE : MENU_ARTIST_INACTIVE);
		btnGenres.setImageResource(DATATYPE_GENRE == selectedDataType ? MENU_GENRE_ACTIVE : MENU_GENRE_INACTIVE);
		btnSongs.setImageResource(DATATYPE_SONG == selectedDataType ? MENU_SONG_ACTIVE : MENU_SONG_INACTIVE);

		// Highlight the correct what
		btnNew.setImageResource(WHAT_NEW == selectedWhat ? MENU_NEW_ACTIVE : MENU_NEW_INACTIVE);
		btnTop.setImageResource(WHAT_TOP == selectedWhat ? MENU_TOP_ACTIVE : MENU_TOP_INACTIVE);
		btnFlop.setImageResource(WHAT_FLOP == selectedWhat ? MENU_FLOP_ACTIVE : MENU_FLOP_INACTIVE);
		btnSuggestion.setImageResource(WHAT_SUGGESTIONS == selectedWhat ? MENU_SUGGESTION_ACTIVE
				: MENU_SUGGESTION_INACTIVE);

		// Set the correct title
		String mainTitle;
		switch (selectedWhat) {
			case WHAT_NEW:
				mainTitle = getString(R.string.title_new);
				break;

			case WHAT_FLOP:
				mainTitle = getString(R.string.title_flop);
				break;

			case WHAT_SUGGESTIONS:
				mainTitle = getString(R.string.title_suggested);
				break;

			case WHAT_TOP:
			default:
				mainTitle = getString(R.string.title_top);
				break;
		}
		String subTitle;
		switch (selectedDataType) {
			case DATATYPE_ALBUM:
				subTitle = getString(R.string.albums);
				break;

			case DATATYPE_SONG:
				subTitle = getString(R.string.songs);
				break;

			case DATATYPE_GENRE:
				subTitle = getString(R.string.genres);
				break;

			case DATATYPE_ARTIST:
			default:
				subTitle = getString(R.string.artists);
				break;
		}

		String title = String.format(mainTitle, subTitle);
		txtTitle.setText(title);
	}

	/**
	 * Registers the event listeners for the different buttons.
	 */
	private void registerEventListeners() {
		BtnClickListener listener = new BtnClickListener();

		btnAlbums.setOnClickListener(listener);
		btnArtists.setOnClickListener(listener);
		btnGenres.setOnClickListener(listener);
		btnSongs.setOnClickListener(listener);

		btnNew.setOnClickListener(listener);
		btnTop.setOnClickListener(listener);
		btnFlop.setOnClickListener(listener);
		btnSuggestion.setOnClickListener(listener);

		gestureFilter = new SimpleGestureFilter(this, new SimpleGestureListener() {

			@Override
			public void onSwipe(int direction) {
				if ((direction != SimpleGestureFilter.SWIPE_RIGHT) && (direction != SimpleGestureFilter.SWIPE_LEFT)) {
					return;
				}

				if (currentWhat == WHAT_NEW) {
					// Tag cloud display is not supported for the recently imported view 
					return;
				}

				Intent intent = new Intent();

				int nextDisplayType = (currentDisplayType == DISPLAYTYPE_LIST) ? DISPLAYTYPE_TAGCLOUD
						: DISPLAYTYPE_LIST;
				intent.putExtra(DISPLAYTYPE_KEY, nextDisplayType);

				controller.doHapticFeedback();
				processIntent(intent);
			}

			@Override
			public void onDoubleTap() {
				return; // We are not interrested in this...
			}
		});
		gestureFilter.setMode(SimpleGestureFilter.MODE_TRANSPARENT);
	}

	/************* EVENT LISTENERS ***********/

	class BtnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();

			switch (v.getId()) {
				case R.id.btnStatisticsAlbums: {
					intent.putExtra(DATATYPE_KEY, DATATYPE_ALBUM);
					break;
				}

				case R.id.btnStatisticsArtists: {
					intent.putExtra(DATATYPE_KEY, DATATYPE_ARTIST);
					break;
				}

				case R.id.btnStatisticsGenres: {
					intent.putExtra(DATATYPE_KEY, DATATYPE_GENRE);
					break;
				}

				case R.id.btnStatisticsSongs: {
					intent.putExtra(DATATYPE_KEY, DATATYPE_SONG);
					break;
				}
			}

			switch (v.getId()) {
				case R.id.btnStatisticsNewData: {
					intent.putExtra(WHAT_KEY, WHAT_NEW);
					break;
				}

				case R.id.btnStatisticsTop: {
					intent.putExtra(WHAT_KEY, WHAT_TOP);
					break;
				}

				case R.id.btnStatisticsFlop: {
					intent.putExtra(WHAT_KEY, WHAT_FLOP);
					break;
				}

				case R.id.btnStatisticsSuggestions: {
					intent.putExtra(WHAT_KEY, WHAT_SUGGESTIONS);
					break;
				}
			}

			controller.doHapticFeedback();
			processIntent(intent);
		}
	}

	class ArtistClickListener implements OnClickListener, OnLongClickListener {

		@Override
		public void onClick(IStatisticsData item) {
			StatisticsArtist artist = (StatisticsArtist) item;

			controller.doHapticFeedback();
			Intent intent = new Intent(StatisticsActivity.this, ArtistListMenu.class);
			intent.putExtra(Controller.INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(artist));
			startActivity(intent);
		}

		@Override
		public boolean onLongClick(IStatisticsData item) {
			StatisticsArtist artist = (StatisticsArtist) item;

			controller.doHapticFeedback();
			controller.showAlbumList(StatisticsActivity.this, artist);
			return true;
		}
	}

	class AlbumClickListener implements OnClickListener, OnLongClickListener {

		@Override
		public void onClick(IStatisticsData item) {
			StatisticsAlbum album = (StatisticsAlbum) item;

			controller.doHapticFeedback();
			controller.showAlbumDetailInfo(StatisticsActivity.this, album);
		}

		@Override
		public boolean onLongClick(IStatisticsData item) {
			StatisticsAlbum album = (StatisticsAlbum) item;

			controller.doHapticFeedback();
			Intent intent = new Intent(StatisticsActivity.this, AlbumListMenu.class);
			intent.putExtra(Controller.INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(album));
			/* TODO: Implement this?!
			String albumName = album.getName();
			HashMap<String, Integer> albumNameCnts = StatisticsActivity.this.getAlbumNameCnts();
			Integer albumNameCnt = albumNameCnts.get(albumName);
			if (albumNameCnt == null) {
				albumNameCnt = 0;
			}
			intent.putExtra(Controller.INTENT_EXTRA_NUMBER_OF_ALBUMS_WITH_THIS_NAME, albumNameCnt);*/
			startActivity(intent);
			return true;
		}
	}

	class GenreClickListener implements OnClickListener {

		@Override
		public void onClick(IStatisticsData item) {
			StatisticsGenre genre = (StatisticsGenre) item;

			controller.doHapticFeedback();
			Intent intent = new Intent(StatisticsActivity.this, GenreListMenu.class);
			intent.putExtra(Controller.INTENT_EXTRA_BASE_GENRE, new ParcelableGenre(genre));
			startActivity(intent);
		}

	}

	class SongClickListener implements OnClickListener {

		@Override
		public void onClick(IStatisticsData item) {
			@SuppressWarnings("unchecked")
			StatisticsSong<BaseArtist, BaseAlbum> song = (StatisticsSong<BaseArtist, BaseAlbum>) item;

			controller.doHapticFeedback();
			Intent intent = new Intent(StatisticsActivity.this, SongMenu.class);
			intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
			startActivity(intent);
		}

	}

}
