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
package ch.ethz.dcg.jukefox.controller;

import android.app.Activity;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumDetailEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ArtistListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ArtistListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.BaseJukefoxEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ContextShuffleConfigEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.DeleteSongMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.FeedbackDialogEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.GenreListMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ImportPlaylistEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ListSelectionEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.LoadVideoPlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.MapEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlayModeSelectionEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlayerActivityEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlaylistContextMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.PlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SavePlaylistMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SearchEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ShuffleModeMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarModeMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SimilarSongsToFamousArtistEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SleepMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SongContextMenuEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SongListEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SpaceActivityEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagCloudEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagPlaylistGenerationEventListener;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TitleSearchMenuEventListener;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.dialogs.FeedbackDialog;
import ch.ethz.dcg.pancho3.view.overlays.AlbumDetails;
import ch.ethz.dcg.pancho3.view.overlays.ContextShuffleConfig;
import ch.ethz.dcg.pancho3.view.overlays.DeleteSongMenu;
import ch.ethz.dcg.pancho3.view.overlays.LoadPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.LoadVideoPlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistContextMenu;
import ch.ethz.dcg.pancho3.view.overlays.PlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.SavePlaylistMenu;
import ch.ethz.dcg.pancho3.view.overlays.ShuffleModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimilarModeMenu;
import ch.ethz.dcg.pancho3.view.overlays.SimilarSongsToFamousArtist;
import ch.ethz.dcg.pancho3.view.overlays.SleepMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongContextMenu;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.overlays.TagPlaylistGenerationActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.MapActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.SearchActivity;
import ch.ethz.dcg.pancho3.view.tabs.SpaceActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistList;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreList;
import ch.ethz.dcg.pancho3.view.tabs.lists.GenreListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ListSelectionActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.SongList;
import ch.ethz.dcg.pancho3.view.tabs.lists.TagCloud;

public interface IViewController {

	/**
	 * Returns the Event Listener for the player activity
	 * 
	 * @param activity
	 *            the player activity
	 * @return the according eventListener
	 */
	public PlayerActivityEventListener createPlayerViewEventListener(PlayerActivity activity);

	public TabEventListener createTabEventListener(JukefoxTabActivity activity, Tab currentTab);

	public BaseJukefoxEventListener createBaseJukefoxEventListener(JukefoxActivity activity);

	public PlayModeSelectionEventListener createPlayModeSelectionEventListener(JukefoxActivity activity);

	public MapEventListener createMapEventListener(MapActivity activity);

	public AlbumDetailEventListener createAlbumDetailEventListener(AlbumDetails albumDetails);

	public ListSelectionEventListener createListSelectionEventListener(ListSelectionActivity listSelectionActivity);

	public SongListEventListener createSongListEventListener(SongList songList);

	public ShuffleModeMenuEventListener createShuffleModeMenuEventListener(ShuffleModeMenu shuffleModeMenu);

	public AlbumListEventListener createAlbumListEventListener(AlbumList albumList);

	public PlaylistMenuEventListener createPlaylistMenuEventListener(PlaylistMenu playlistMenu);

	public SleepMenuEventListener createSleepMenuEventListener(SleepMenu sleepMenu);

	public LoadPlaylistMenuEventListener createLoadPlaylistMenuEventListener(LoadPlaylistMenu loadPlaylistMenu);

	public LoadVideoPlaylistMenuEventListener createLoadVideoPlaylistMenuEventListener(
			LoadVideoPlaylistMenu loadVideoPlaylistMenu);

	public SavePlaylistMenuEventListener createSavePlaylistMenuEventListener(SavePlaylistMenu savePlaylistMenu);

	public ArtistListEventListener createArtistListEventListener(ArtistList artistList);

	public GenreListEventListener createGenreListEventListener(GenreList genreList);

	public SimilarModeMenuEventListener createSimilarModeMenuEventListener(SimilarModeMenu similarModeMenu);

	public GenreListMenuEventListener createGenreListMenuEventListener(GenreListMenu genreListMenu);

	public ArtistListMenuEventListener createArtistListMenuEventListener(ArtistListMenu artistListMenu);

	public TagCloudEventListener createTagCloudEventListener(TagCloud tagCloud);

	public SearchEventListener createSearchEventListener(SearchActivity searchActivity);

	public TitleSearchMenuEventListener createTitleSearchMenuEventListener(SongMenu titleSearchResultMenu);

	public SpaceActivityEventListener createSpaceEventListener(SpaceActivity spaceActivity);

	public SongContextMenuEventListener createSongContextMenuEventListener(SongContextMenu songContextMenu);

	public TagPlaylistGenerationEventListener createTagPlaylistGenerationEventListener(
			TagPlaylistGenerationActivity tagPlaylistGeneration);

	public SimilarSongsToFamousArtistEventListener createSimilarSongsToFamousArtistEventListener(
			SimilarSongsToFamousArtist similarSongsToFamousArtist);

	public PlaylistContextMenuEventListener createPlaylistContextMenuEventListener(
			PlaylistContextMenu playlistContextMenu);

	public FeedbackDialogEventListener createFeedbackDialogEventListener(FeedbackDialog feedbackDialog);

	public AlbumListMenuEventListener createAlbumListMenuEventListener(AlbumListMenu albumListMenu);

	public DeleteSongMenuEventListener createDeleteSongMenuEventListener(DeleteSongMenu deleteSongMenu);

	public ContextShuffleConfigEventListener createContextShuffleConfigEventListener(
			ContextShuffleConfig smartShuffleConfig);

	public ImportPlaylistEventListener createImportPlaylistEventListener();

	public void doHapticFeedback();

	public void showAlbumDetailInfo(JukefoxActivity activity, BaseAlbum album);

	public void showAlbumList(JukefoxActivity activity, BaseArtist artist);

	public void startActivity(Activity activity, Class<?> classToLoad);
}
