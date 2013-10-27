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
package ch.ethz.dcg.pancho3.tablet.presenter.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.I18nManager;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;

/**
 * An abstract base class for presenters of the overlay fragment.
 */
public abstract class AbstractOverlayPresenter implements
		OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>>, OnSeekBarChangeListener,
		Callback {

	protected final static int DEFAULT_CLOUD_COLOR = Color.GRAY;
	private static final String TAG = AbstractOverlayPresenter.class.getSimpleName();
	// A reference to the main presenter to make explore or map calls.
	protected final TabletPresenter tabletPresenter;
	// The playlist controller is needed to add songs to the queue.
	protected final MagicPlayMode magicPlaylistController;
	protected final AndroidPlayerController playerController;
	// An interface to the overlay view.
	protected final IOverlayView overlay;
	// Used to inflate menus.
	protected final ViewFactory viewFactory;
	protected final I18nManager i18nManager;
	// The contextual action mode for the current action bar. Reference is kept to finish the mode.
	protected ActionMode actionMode;
	// All the songs in this overlay.
	private List<BaseSong<BaseArtist, BaseAlbum>> allSongs;
	// The songs currently displayed in this overlay.
	private List<BaseSong<BaseArtist, BaseAlbum>> selectedSongs;

	private boolean highlightByDrag;
	private boolean highlightByCheckedItems;
	private SparseBooleanArray latestIndices = new SparseBooleanArray();
	private boolean allSongsChecked = false;

	private BaseArtist navigationArtist;
	private BaseAlbum navigationAlbum;

	private boolean active = true;

	/**
	 * The constructor sets the references.
	 */
	public AbstractOverlayPresenter(TabletPresenter tabletPresenter,
			MagicPlayMode magicPlaylistController, IOverlayView overlay,
			ViewFactory viewFactory, I18nManager i18nManager) {
		this.tabletPresenter = tabletPresenter;
		this.magicPlaylistController = magicPlaylistController;
		this.overlay = overlay;
		this.viewFactory = viewFactory;
		this.i18nManager = i18nManager;
		this.playerController = JukefoxApplication.getPlayerController();
	}

	/**
	 * An interface to the overlay view.
	 */
	public static interface IOverlayView {

		/**
		 * Hides (removes) the overlay.
		 */
		void hide();

		/**
		 * The overlay displays itself with the given callback for the action
		 * mode.
		 */
		void show(Callback callback);

		/**
		 * Sets a fancy background using this color for the whole view.
		 */
		void setBackgroundColor(int color);

		/**
		 * Initializes the song seekbar which needs to know how many songs there
		 * are for its max value.
		 */
		void initializeSongSeekbar(int numberOfSongs);

		/**
		 * Sets the background image of the center piece of the overlay.
		 */
		void setAlbumArt(Bitmap bitmap);

		ImageView getAlbumArt();

		/**
		 * Sets the title of the header item. The header item is an item
		 * representing all the songs displayed.
		 */
		void setHeaderItemTitle(String title);

		void highlight();

		void unhighlight();

		void showDropArea();

		void hideDropArea();

		void hideSongbarDescription();

		void uncheckAllSongs();

		void setHeaderChecked(boolean checked, boolean updateListItems);

		void setAdapter(ISongAdapter adapter);
	}

	public void onHeaderItemClick() {
		if (!getDisplayedSongs().isEmpty()) {
			if (allSongsChecked) {
				overlay.setHeaderChecked(false, true);
				highlightByCheckedItems = false;
			} else {
				overlay.setHeaderChecked(true, true);
				highlightByCheckedItems = true;
			}
			allSongsChecked = !allSongsChecked;
			updateHighlighting();
		}
	}

	/**
	 * When this is called, the header item which represents all the songs
	 * starts dragging.
	 */
	public boolean onHeaderItemLongClick(View headerItem) {
		if (!getDisplayedSongs().isEmpty()) {
			tabletPresenter.getDragManager().startDraggingSongs(headerItem, getDisplayedSongs());
		}
		return true;
	}

	/**
	 * Signals that the initialization of the view has finished. Tell it what
	 * album we're displaying.
	 */
	public void viewFinishedInit() {
		tabletPresenter.setCurrentOverlayPresenter(this);
	}

	@Override
	public void onDataFetched(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		this.allSongs = songs;
		this.selectedSongs = songs;
		displaySongs(songs);
		overlay.initializeSongSeekbar(songs.size());
		overlay.setHeaderItemTitle(i18nManager.getNumberOfSongsText(songs.size()));
	}

	/**
	 * Called when the seekbar selecting the amount of songs displayed has
	 * changed. We select a new set of random songs and display it.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			uncheckAllSongs();
			this.selectedSongs = selectRandomSongs(progress);
			displaySongs(selectedSongs);
			overlay.setHeaderItemTitle(i18nManager.getNumberOfSongsText(progress));
		}
	}

	/**
	 * Called when the overlay is being hidden: We finish the action mode.
	 */
	public void onHideOverlay() {
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	/**
	 * Called when the user clicks an action item in the action bar.
	 */
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.explore:
				overlay.hide();
				tabletPresenter.exploreArtistMaybe(navigationArtist);
				break;
			case R.id.map:
				overlay.hide();
				tabletPresenter.mapAlbumMaybe(navigationAlbum);
				break;
		}
		return true;
	}

	/**
	 * Called when the user clicks 'Done' in the action bar. We hide the
	 * overlay.
	 */
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		active = false;
		overlay.hide();
	}

	protected List<BaseSong<BaseArtist, BaseAlbum>> selectRandomSongs(int number) {
		List<BaseSong<BaseArtist, BaseAlbum>> randomSongs =
				new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(allSongs);
		Collections.shuffle(randomSongs);
		return randomSongs.subList(0, number);
	}

	public void highlight() {
		highlightByDrag = true;
		overlay.highlight();
		updateHighlighting();
	}

	public void unhighlight(boolean successful) {
		highlightByDrag = false;
		overlay.unhighlight();
		if (successful) {
			uncheckAllSongs(); // includes updateHighlighting()
		} else {
			updateHighlighting();
		}
	}

	public void playNext(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		try {
			playerController.insertSongsAsNext(songs);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		} catch (EmptyPlaylistException e) {
			Log.w(TAG, e);
			playerController.appendSongsAtEnd(songs);
		}
	}

	public void playNow(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		magicPlaylistController.swapPlayingSongWith(playerController, songs);
	}

	public void enqueue(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		playerController.appendSongsAtEnd(songs);
	}

	public void playNextClicked() {
		playNext(getCheckedSongs());
		uncheckAllSongs();
	}

	public void playNowClicked() {
		playNow(getCheckedSongs());
		uncheckAllSongs();
	}

	public void enqueueClicked() {
		enqueue(getCheckedSongs());
		uncheckAllSongs();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		overlay.hideSongbarDescription();
	}

	public void onCheckedIndicesChange(int numberOfCheckedItems, SparseBooleanArray indices,
			int latestChangeIndex) {
		this.latestIndices = indices;
		if (numberOfCheckedItems == getDisplayedSongs().size()) {
			overlay.setHeaderChecked(true, false);
			allSongsChecked = true;
		} else {
			overlay.setHeaderChecked(false, false);
			allSongsChecked = false;
		}
		highlightByCheckedItems = numberOfCheckedItems > 0;
		updateHighlighting();
	}

	private void updateHighlighting() {
		if (highlightByDrag || highlightByCheckedItems) {
			overlay.showDropArea();
		} else {
			overlay.hideDropArea();
		}
	}

	private ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> getCheckedSongs() {
		ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> songList =
				new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		if (allSongsChecked) {
			for (BaseSong<BaseArtist, BaseAlbum> song : getDisplayedSongs()) {
				songList.add(new PlaylistSong<BaseArtist, BaseAlbum>(song,
						SongSource.MANUALLY_SELECTED));
			}
		} else {
			int count = latestIndices.size();
			for (int i = 0; i < count; i++) {
				int key = latestIndices.keyAt(i);
				if (latestIndices.get(key)) {
					songList.add(new PlaylistSong<BaseArtist, BaseAlbum>(
							getDisplayedSongs().get(key), SongSource.MANUALLY_SELECTED));
				}
			}
		}
		return songList;
	}

	protected void uncheckAllSongs() {
		highlightByCheckedItems = false;
		updateHighlighting();
		overlay.uncheckAllSongs();
	}

	protected void showNavigationExploreMap(BaseArtist artist, BaseAlbum album, Menu menu) {
		this.navigationArtist = artist;
		this.navigationAlbum = album;
		viewFactory.inflateMenuExploreMap(menu, artist, album);
	}

	protected void showNavigationExplore(BaseArtist artist, Menu menu) {
		this.navigationArtist = artist;
		viewFactory.inflateMenuExplore(menu, artist);
	}

	protected void showNavigationMap(BaseAlbum album, Menu menu) {
		this.navigationAlbum = album;
		viewFactory.inflateMenuMap(menu, album);
	}

	/**
	 * If false, this overlay presenter isn't used anymore (and will never be
	 * used again). Its overlay isn't not visible anymore.
	 */
	public boolean isActive() {
		return active;
	}

	// Below here are methods to override.

	protected abstract void displaySongs(List<BaseSong<BaseArtist, BaseAlbum>> songs);

	public abstract ISongAdapter getSongAdapter();

	protected List<BaseSong<BaseArtist, BaseAlbum>> getDisplayedSongs() {
		return selectedSongs;
	}

	// Below here are NOPs.

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
}
