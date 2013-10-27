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
package ch.ethz.dcg.pancho3.tablet.presenter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.res.Resources;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.model.collection.AllAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllRelatedAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllSongsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.RecentSongsRepresentative;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.map.MapPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AbstractOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AlbumOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.queue.QueuePresenter;
import ch.ethz.dcg.pancho3.tablet.view.DragManager;
import ch.ethz.dcg.pancho3.tablet.view.exploration.ExplorationSelectionFragment;
import ch.ethz.dcg.pancho3.tablet.view.map.MapSelectionFragment;

/**
 * The main presenter of the tablet interface. It presents the tablet activity
 * and is the central hub for events fired across different fragments and parts
 * of the interface.
 */
public class TabletPresenter {

	public static enum SelectionView {
		SELECTION_VIEW_EXPLORATION(R.string.explore_mode_name,
				ExplorationSelectionFragment.class, "Exploration"),
		SELECTION_VIEW_MAP(R.string.map_mode_name,
				MapSelectionFragment.class, "Map");

		// Name for the selection view to display.
		private final int displayStringResource;
		// The class of which the instantiation represents this selection view.
		private final Class<? extends Fragment> fragmentClass;

		private final String id;

		private SelectionView(int displayStringResource, Class<? extends Fragment> fragmentClass,
				String id) {
			this.displayStringResource = displayStringResource;
			this.fragmentClass = fragmentClass;
			this.id = id;
		}

		/**
		 * Returns a string to display as a name of this selection view.
		 */
		public String getDisplayString(Resources resources) {
			return resources.getString(displayStringResource);
		}

		/**
		 * Returns a class of the fragment to instantiate to obtain the actual
		 * view of this selection view.
		 */
		public Class<? extends Fragment> getFragmentClass() {
			return fragmentClass;
		}

		public String getTag() {
			return id;
		}
	}

	// The view which is presented by this class.
	private final IMainView mainView;
	// Manages the different selection views.
	private final TabletFactory tabletFactory;
	private final MagicPlayMode magicPlaylistController;
	private final HistoryManager historyManager;
	private final AndroidPlayerController playerController;

	// The presenter of the exploration selection view.
	private ExplorationPresenter explorationPresenter;
	// The presenter of the map selection view.
	private MapPresenter mapPresenter;
	private QueuePresenter queuePresenter;
	// We can also have an overlay referenced which isn't open anymore.
	// Before using this overlay we check isActive.
	private WeakReference<AbstractOverlayPresenter> currentOverlayPresenter =
			new WeakReference<AbstractOverlayPresenter>(null);

	/**
	 * The constructor sets the references.
	 */
	public TabletPresenter(IMainView mainView, TabletFactory tabletFactory,
			MagicPlayMode magicPlaylistController,
			HistoryManager historyManager) {
		this.mainView = mainView;
		this.tabletFactory = tabletFactory;
		this.magicPlaylistController = magicPlaylistController;
		this.historyManager = historyManager;
		this.playerController = JukefoxApplication.getPlayerController();
	}

	/**
	 * Needs to be called to make this presenter functional. Not passed into the
	 * constructor because of circular dependencies.
	 */
	public void setExplorationPresenter(ExplorationPresenter explorationPresenter) {
		this.explorationPresenter = explorationPresenter;
	}

	/**
	 * Needs to be called to make this presenter functional. Not passed into the
	 * constructor because of circular dependencies.
	 */
	public void setMapPresenter(MapPresenter mapPresenter) {
		this.mapPresenter = mapPresenter;
	}

	/**
	 * An interface to the main view of the tablet UI.
	 */
	public static interface IMainView {

		/**
		 * TODO: comment
		 */
		void displayOverlay(Fragment overlayFragment);

		/**
		 * Hides the overlay or is a NOP if none was present.
		 * updateActionBarNavigation TODO: does more now.
		 */
		void clearLocalUI();

		/**
		 * Makes the view display a fragment.
		 */
		void displayFragment(SelectionView selectionView);

		void updateActionBar(String title, boolean displayHomeAsUpEnabled,
				boolean displayMapMenuItem, int logoResId);
	}

	/**
	 * Called by other presenters such that this presenter makes the view
	 * display an overlay.
	 */
	public void displayOverlay(ListAlbum album, boolean needsMapNavigation,
			boolean needsExploreNavigation) {
		if (album instanceof AllAlbumsRepresentative) {
			displayOverlay(album.getFirstArtist());
		} else if (album instanceof AllSongsRepresentative) {
			displayOverlay();
		} else if (album instanceof AllRelatedAlbumsRepresentative) {
			displayOverlay(((AllRelatedAlbumsRepresentative) album).getRepresentedAlbums());
		} else if (album instanceof RecentSongsRepresentative) {
			List<BaseSong<BaseArtist, BaseAlbum>> songs = magicPlaylistController.getRecentSongs();
			mainView.displayOverlay(
					tabletFactory.createSongsOverlayFragment(songs));
		} else {
			mainView.displayOverlay(tabletFactory.createOverlayFragment(album,
					needsMapNavigation, needsExploreNavigation));
		}
	}

	public void displayOverlay(List<? extends ListAlbum> albums) {
		mainView.displayOverlay(tabletFactory.createOverlayFragment(albums));
	}

	public void displayOverlay(BaseArtist artist) {
		mainView.displayOverlay(tabletFactory.createOverlayFragment(artist));
	}

	public void displayOverlay() {
		mainView.displayOverlay(tabletFactory.createOverlayFragment());
	}

	public void displayArtistChooser() {
		mainView.displayOverlay(tabletFactory.createArtistChooserFragment());
	}

	public void displaySearch() {
		mainView.displayOverlay(tabletFactory.createSearchOverlayFragment());
	}

	public boolean handleBackButton() {
		boolean handled = historyManager.popHistory();
		updateActionBar();
		return handled;
	}

	/**
	 * Called by other presenters such that this presenter makes the view
	 * explore a certain artist.
	 */
	public void exploreArtistMaybe(BaseArtist artist) {
		historyManager.exploreArtistMaybe(artist);
		updateActionBar();
	}

	/**
	 * Called by other presenters such that this presenter makes the view map an
	 * album.
	 */
	public void mapAlbumMaybe(BaseAlbum album) {
		historyManager.mapAlbumMaybe(album);
		updateActionBar();
	}

	public void mapMaybe() {
		historyManager.mapMaybe();
		updateActionBar();
	}

	public void exploreAllAlbumsMaybe() {
		historyManager.exploreAllAlbumsMaybe();
		updateActionBar();
	}

	public void exploreTagMaybe(BaseTag tag) {
		historyManager.exploreTagMaybe(tag);
		updateActionBar();
	}

	private void updateActionBar() {
		mainView.updateActionBar(historyManager.getCurrentActionBarTitle(),
				!historyManager.isCurrentHome(), historyManager.isCurrentHome(),
				historyManager.getCurrentActionBarIcon());
	}

	public void mapAlbum(BaseAlbum album) {
		mapPresenter.goToAlbum(album);
		mainView.displayFragment(SelectionView.SELECTION_VIEW_MAP);
	}

	public void map() {
		mainView.displayFragment(SelectionView.SELECTION_VIEW_MAP);
	}

	public void exploreAllAlbums() {
		mainView.displayFragment(SelectionView.SELECTION_VIEW_EXPLORATION);
		explorationPresenter.exploreAllAlbums();
	}

	public void exploreArtist(BaseArtist artist) {
		mainView.displayFragment(SelectionView.SELECTION_VIEW_EXPLORATION);
		explorationPresenter.exploreArtist(artist);
	}

	public void exploreTag(BaseTag tag) {
		mainView.displayFragment(SelectionView.SELECTION_VIEW_EXPLORATION);
		explorationPresenter.exploreTag(tag);
	}

	/**
	 * Called by the presenters view to signal that initialization has finished.
	 */
	public void viewFinishedInit() {
		exploreAllAlbumsMaybe();
	}

	public void onSongChosen(BaseSong<BaseArtist, BaseAlbum> song) {
		AbstractOverlayPresenter presenter = currentOverlayPresenter.get();
		if (presenter != null && presenter.isActive()) {
			if (presenter instanceof AlbumOverlayPresenter) {
				AlbumOverlayPresenter albumPresenter = (AlbumOverlayPresenter) presenter;
				if (albumPresenter.getAlbum().equals(song.getAlbum())) {
					return; // We are already overlaying this artist.
				} else {
					mainView.clearLocalUI();
				}
			}
		}
		/*switch (selectionViewManager.getCurrentSelectionView()) {
			case SELECTION_VIEW_EXPLORATION:
				explorationPresenter.exploreArtist(song.getArtist());
				displayOverlay(new ListAlbum(song), true, false);
				break;
			case SELECTION_VIEW_MAP:
				mapPresenter.goToAlbum(song.getAlbum());
				displayOverlay(new ListAlbum(song), false, true);
				break;
		// TODO: special if search displayed
		}*/
	}

	public DragManager getDragManager() {
		return tabletFactory.getDragManager();
	}

	public void setQueuePresenter(QueuePresenter queuePresenter) {
		this.queuePresenter = queuePresenter;
	}

	public void highlight() {
		queuePresenter.highlight();
		AbstractOverlayPresenter presenter = currentOverlayPresenter.get();
		if (presenter != null && presenter.isActive()) {
			presenter.highlight();
		}
	}

	public void unhighlight(boolean successful) {
		queuePresenter.unhighlight();
		AbstractOverlayPresenter presenter = currentOverlayPresenter.get();
		if (presenter != null && presenter.isActive()) {
			presenter.unhighlight(successful);
		}
	}

	public void setCurrentOverlayPresenter(AbstractOverlayPresenter currentOverlayPresenter) {
		this.currentOverlayPresenter =
				new WeakReference<AbstractOverlayPresenter>(currentOverlayPresenter);
	}

	public void enqueueSong(BaseSong<BaseArtist, BaseAlbum> song) {
		playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(song,
				SongSource.MANUALLY_SELECTED));
	}

	public void enqueueSongs(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> playlistSongs =
				new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			playlistSongs.add(new PlaylistSong<BaseArtist, BaseAlbum>(song,
					SongSource.MANUALLY_SELECTED));
		}
		playerController.appendSongsAtEnd(playlistSongs);
	}
}