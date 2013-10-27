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
package ch.ethz.dcg.pancho3.tablet;

import java.util.List;

import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewConfiguration;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.jukefox.playmode.MagicPlayMode;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabletActivityEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabletMapEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.SelectionViewManager;
import ch.ethz.dcg.pancho3.tablet.presenter.HistoryManager;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ArtistChooserPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ValueSelector;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ArtistChooserPresenter.IArtistChooserView;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationView;
import ch.ethz.dcg.pancho3.tablet.presenter.map.MapPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AbstractOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AlbumOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AlbumsOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AllSongsOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.ArtistOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.SearchOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.SongsOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.queue.QueuePresenter;
import ch.ethz.dcg.pancho3.tablet.view.DragManager;
import ch.ethz.dcg.pancho3.tablet.view.TabletActivity;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.view.exploration.ArtistChooserFragment;
import ch.ethz.dcg.pancho3.tablet.view.lists.ArtistListAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.CachingImageAlbumListAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.ImageAlbumListAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongCursorAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.SongListAdapter;
import ch.ethz.dcg.pancho3.tablet.view.overlay.CloudDrawer;
import ch.ethz.dcg.pancho3.tablet.view.overlay.OverlayFragment;
import ch.ethz.dcg.pancho3.tablet.view.queue.PlaylistAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.MagicListAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;
import ch.ethz.dcg.pancho3.tablet.widget.MagicListAdapter.MagicListInnerAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.MagicListAdapter.NewItemListener;
import ch.ethz.dcg.pancho3.view.tabs.lists.TagCloudCreator;
import ch.ethz.dcg.pancho3.view.tabs.opengl.MapRenderer;

/**
 * A factory to create or obtain almost any object specific to the tablet
 * interface. One can obtain the factory through the the interface
 * {@link TabletFactoryGetter}.
 * 
 * Other classes in the tablet package should not use any 'new' keywords to
 * instantiate classes that they can find in here.
 */
public class TabletFactory {

	private final TabletActivity activity;
	private final JukefoxApplication application;
	private final LayoutInflater layoutInflater;
	private final AndroidCollectionModelManager data;
	private final MagicPlayMode magicPlaylistController;
	private final ViewFactory viewFactory;
	private final TabletPresenter tabletPresenter;
	private final ExplorationPresenter explorationPresenter;
	private final QueuePresenter queuePresenter;
	private final MapPresenter mapPresenter;
	private final DataFetcher dataFetcher;
	private final ISettingsReader settings;
	private final DragManager dragManager;
	private final float screenDensityScale;
	private final I18nManager i18nManager;

	// Caches for pseudo singleton classes.
	private TabletActivityEventListener eventListener;
	private TabletMapEventListener mapEventListener;
	private CloudDrawer cloudDrawer;
	private AndroidPlayerController playerController;

	// TODO: comment
	private AbstractOverlayPresenter currentOverlayPresenter;

	public static interface TabletFactoryGetter {

		TabletFactory getTabletFactory();

		boolean isTabletFactoryReady();
	}

	public TabletFactory(TabletActivity activity, JukefoxApplication application) {
		this.activity = activity;
		this.application = application;
		this.layoutInflater = LayoutInflater.from(activity);
		this.data = JukefoxApplication.getCollectionModel();
		i18nManager = new I18nManager(activity.getResources());
		dataFetcher = new DataFetcher(data);
		screenDensityScale = activity.getResources().getDisplayMetrics().density;

		playerController = JukefoxApplication.getPlayerController();
		IPlayMode playlistController = playerController.getPlayMode();
		if (playlistController instanceof MagicPlayMode) {
			magicPlaylistController = (MagicPlayMode) playlistController;
		} else {
			playerController.setPlayMode(PlayModeType.MAGIC, 0, Constants.SAME_SONG_AVOIDANCE_NUM);
			magicPlaylistController = (MagicPlayMode) playerController.getPlayMode();
		}

		HistoryManager historyManager = new HistoryManager(i18nManager);
		tabletPresenter = new TabletPresenter(activity, this, magicPlaylistController,
				historyManager);
		historyManager.setTabletPresenter(tabletPresenter);

		queuePresenter = new QueuePresenter();
		tabletPresenter.setQueuePresenter(queuePresenter);
		explorationPresenter = new ExplorationPresenter(tabletPresenter, dataFetcher,
				new ValueSelector());
		tabletPresenter.setExplorationPresenter(explorationPresenter);
		mapPresenter = new MapPresenter(data);
		playerController.addOnPlaylistStateChangeListener(
				mapPresenter);
		tabletPresenter.setMapPresenter(mapPresenter);
		settings = AndroidSettingsManager.getAndroidSettingsReader();

		dragManager = new DragManager(dataFetcher, tabletPresenter,
				activity.getResources().getDisplayMetrics().density,
				ViewConfiguration.get(activity).getScaledPagingTouchSlop());
		this.viewFactory = new ViewFactory(layoutInflater, dataFetcher, activity
				.getMenuInflater(), activity.getResources(), dragManager, screenDensityScale,
				i18nManager);
		dragManager.setViewFactory(viewFactory);
	}

	public SelectionViewManager createSelectionViewManager() {
		return new SelectionViewManager(activity);
	}

	public ImageAlbumListAdapter createImageAlbumListAdapter() {
		return new ImageAlbumListAdapter(activity, viewFactory);
	}

	public CachingImageAlbumListAdapter createCachingImageAlbumListAdapter() {
		return new CachingImageAlbumListAdapter(activity, viewFactory);
	}

	public ArtistListAdapter createArtistListAdapter(PinnedHeaderListView listView) {
		return new ArtistListAdapter(activity, viewFactory, listView);
	}

	public SongListAdapter createSongListAdapter() {
		return new SongListAdapter(activity, viewFactory);
	}

	public PlaylistAdapter createPlaylistAdapter() {
		PlaylistAdapter playlistAdapter = new PlaylistAdapter(viewFactory, magicPlaylistController);
		playlistAdapter.playlistChanged(playerController.getCurrentPlaylist());
		return playlistAdapter;
	}

	public DragManager getDragManager() {
		return dragManager;
	}

	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	public TabletActivityEventListener getEventListener() {
		if (eventListener == null) {
			eventListener = new TabletActivityEventListener(application.getController(), activity,
					tabletPresenter);
		}
		return eventListener;
	}

	public TabletPresenter getTabletPresenter() {
		return tabletPresenter;
	}

	public ISettingsReader getSettingsReader() {
		return settings;
	}

	public TabletMapEventListener getTabletMapEventListener(MapRenderer mapRenderer) {
		if (mapEventListener == null || !mapEventListener.containsSameMapRenderer(mapRenderer)) {
			mapEventListener = new TabletMapEventListener(application.getController(), activity,
					mapRenderer, tabletPresenter);
		}
		return mapEventListener;
	}

	public CloudDrawer getCloudDrawer() {
		if (cloudDrawer == null) {
			cloudDrawer = new CloudDrawer(activity.getResources());
		}
		return cloudDrawer;
	}

	public MagicListAdapter<PlaylistSong<BaseArtist, BaseAlbum>> createMagicListAdapter(
			MagicListInnerAdapter<PlaylistSong<BaseArtist, BaseAlbum>> innerAdapter,
			NewItemListener newItemListener) {
		return new MagicListAdapter<PlaylistSong<BaseArtist, BaseAlbum>>(application, innerAdapter,
				newItemListener, dragManager);
	}

	public ExplorationPresenter getExplorationPresenter(IExplorationView listener) {
		explorationPresenter.setExplorationView(listener);
		return explorationPresenter;
	}

	public ExplorationPresenter getExplorationPresenter() {
		return explorationPresenter;
	}

	public QueuePresenter getQueuePresenter() {
		return queuePresenter;
	}

	public MapPresenter getMapPresenter() {
		return mapPresenter;
	}

	private AlbumOverlayPresenter createAlbumOverlayPresenter(OverlayFragment fragment,
			ListAlbum album, boolean needsMapNavigation, boolean needsExploreNavigation) {
		return new AlbumOverlayPresenter(tabletPresenter, magicPlaylistController,
				dataFetcher, fragment, viewFactory, album, createSongListAdapter(),
				needsMapNavigation, needsExploreNavigation, i18nManager);
	}

	private AlbumsOverlayPresenter createAlbumsOverlayPresenter(OverlayFragment fragment,
			List<? extends ListAlbum> albums) {
		return new AlbumsOverlayPresenter(tabletPresenter, magicPlaylistController,
				fragment, viewFactory, dataFetcher, albums, createSongListAdapter(), i18nManager);
	}

	private ArtistOverlayPresenter createArtistOverlayPresenter(OverlayFragment fragment,
			BaseArtist artist) {
		return new ArtistOverlayPresenter(tabletPresenter, magicPlaylistController,
				fragment, artist, viewFactory, dataFetcher, createSongListAdapter(), i18nManager);
	}

	private SongsOverlayPresenter createSongsOverlayPresenter(OverlayFragment fragment,
			List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		return new SongsOverlayPresenter(tabletPresenter, magicPlaylistController,
				fragment, viewFactory, songs, createSongListAdapter(), i18nManager);
	}

	private AllSongsOverlayPresenter createAllSongsOverlayPresenter(
			OverlayFragment fragment) {
		return new AllSongsOverlayPresenter(tabletPresenter, magicPlaylistController,
				fragment, viewFactory, dataFetcher, createSongListAdapter(), i18nManager);
	}

	private SearchOverlayPresenter createSearchOverlayPresenter(OverlayFragment fragment) {
		return new SearchOverlayPresenter(tabletPresenter, magicPlaylistController, fragment,
				viewFactory, createSongCursorAdapter(), createSongListAdapter(), dataFetcher,
				i18nManager);
	}

	public AbstractOverlayPresenter getCurrentOverlayPresenter() {
		return currentOverlayPresenter;
	}

	public Fragment createOverlayFragment(List<? extends ListAlbum> albums) {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createAlbumsOverlayPresenter(fragment, albums);
		return fragment;
	}

	public Fragment createOverlayFragment(ListAlbum album, boolean needsMapNavigation,
			boolean needsExploreNavigation) {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createAlbumOverlayPresenter(fragment, album,
				needsMapNavigation, needsExploreNavigation);
		return fragment;
	}

	public Fragment createOverlayFragment(BaseArtist artist) {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createArtistOverlayPresenter(fragment, artist);
		return fragment;
	}

	public Fragment createSongsOverlayFragment(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createSongsOverlayPresenter(fragment, songs);
		return fragment;
	}

	public Fragment createOverlayFragment() {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createAllSongsOverlayPresenter(fragment);
		return fragment;
	}

	public Fragment createSearchOverlayFragment() {
		OverlayFragment fragment = new OverlayFragment();
		currentOverlayPresenter = createSearchOverlayPresenter(fragment);
		return fragment;
	}

	private SongCursorAdapter createSongCursorAdapter() {
		return new SongCursorAdapter(activity, viewFactory);
	}

	public ArtistChooserFragment createArtistChooserFragment() {
		return new ArtistChooserFragment();
	}

	public TagCloudCreator createTagCloudCreator(int textSize) {
		return new TagCloudCreator(activity, textSize);
	}

	public ArtistChooserPresenter getArtistChooserPresenter(IArtistChooserView view) {
		return new ArtistChooserPresenter(view, explorationPresenter, dataFetcher);
	}

	public MagicPlayMode getMagicPlaylistController() {
		return magicPlaylistController;
	}
}
