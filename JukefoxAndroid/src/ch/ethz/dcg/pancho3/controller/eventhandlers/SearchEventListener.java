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

import java.util.LinkedList;
import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.widget.AdapterView;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.model.collection.ParcelableArtist;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.overlays.SimilarSongsToFamousArtist;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;
import ch.ethz.dcg.pancho3.view.tabs.SearchActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistListMenu;
import ch.ethz.dcg.pancho3.view.tabs.lists.TextSectionAdapter;

public class SearchEventListener extends MainTabButtonEventListener {

	private static final int MAX_RESULTS = 25;
	private SearchActivity activity;
	private Handler handler;

	public SearchEventListener(Controller controller, SearchActivity activity) {
		super(controller, activity, Tab.SEARCH);
		this.activity = activity;
		handler = new Handler();
	}

	public void search(String searchTerm, boolean inArtists, boolean inTitles, boolean inFamousArtists, boolean inAlbums) {
		if (inArtists) {
			setArtistSearchResult(searchTerm);
		} else if (inTitles) {
			setTitleSearchResult(searchTerm);
		} else if (inFamousArtists) {
			Log.v(TAG, "searching for famous artists. search-term: " + searchTerm);
			setFamousArtistSearchResult(searchTerm);
		} else if (inAlbums) {
			setAlbumSearchResult(searchTerm);
		}
	}

	private void setFamousArtistSearchResult(String searchTerm) {

		if (!isFamousArtistsReady()) {
			Log.v(TAG, "famousArtists not ready!");
			activity.showFamousArtistsNotReadyInfo();
			setAdapter(new LinkedList<IBaseListItem>());
			return;
		}

		List<BaseArtist> artistList = activity.getCollectionModel().getArtistProvider()
				.findFamousBaseArtistBySearchString(searchTerm, MAX_RESULTS);
		setAdapter(artistList);
	}

	private boolean isFamousArtistsReady() {
		if (!activity.getCollectionModel().getModelSettingsManager().isFamousArtistsInserted()) {
			return false;
		}
		try {
			return activity.getCollectionModel().getOtherDataProvider().getNumberOfSongsWithCoordinates() != 0;
		} catch (DataUnavailableException e) {
			return false;
		}
	}

	private void setTitleSearchResult(String searchTerm) {

		if (activity.getApplicationState().isImporting() && !activity.getApplicationState().isBaseDataCommitted()) {
			JukefoxApplication.getHandler().post(new Runnable() {

				@Override
				public void run() {
					activity.showStatusInfo(activity.getString(R.string.list_not_yet_loaded));
				}
			});
		}

		List<BaseSong<BaseArtist, BaseAlbum>> songList = activity.getCollectionModel().getSongProvider()
				.findBaseSongsBySearchString(searchTerm, MAX_RESULTS);
		setAdapter(songList);
	}

	private void setArtistSearchResult(String searchTerm) {

		if (activity.getApplicationState().isImporting() && !activity.getApplicationState().isBaseDataCommitted()) {
			activity.showStatusInfo(activity.getString(R.string.list_not_yet_loaded));
		}

		List<BaseArtist> artistList = activity.getCollectionModel().getArtistProvider().findBaseArtistBySearchString(
				searchTerm, MAX_RESULTS);
		setAdapter(artistList);
	}

	private void setAlbumSearchResult(String searchTerm) {

		if (activity.getApplicationState().isImporting() && !activity.getApplicationState().isBaseDataCommitted()) {
			activity.showStatusInfo(activity.getString(R.string.list_not_yet_loaded));
		}

		List<ListAlbum> albumList = activity.getCollectionModel().getAlbumProvider().findListAlbumBySearchString(
				searchTerm, MAX_RESULTS);
		setAdapter(albumList);
	}

	private void setAdapter(final List<? extends IBaseListItem> list) {
		handler.post(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				TextSectionAdapter<IBaseListItem> adapter = new TextSectionAdapter(activity, R.layout.textlistitem,
						list, activity.getSettings().isIgnoreLeadingThe());
				activity.setResultList(activity.getString(R.string.artists), adapter);
				if (adapter.getCount() == MAX_RESULTS) {
					activity.getResultListTitle().setText(R.string.too_many_results);
					activity.getResultListTitle().setTextColor(Color.RED);

				} else {
					activity.getResultListTitle().setText(activity.getString(R.string.results));
					// activity.getResultListTitle().setTextColor(Color.GREEN);
				}
			}

		});
	}

	public void onItemClicked(AdapterView<?> list, int position, boolean inArtists, boolean inTitles,
			boolean inFamousArtists, boolean inAlbums) {
		if (inArtists) {
			onArtistItemClicked(list, position);
		} else if (inTitles) {
			onTitleItemClicked(list, position);
		} else if (inAlbums) {
			onAlbumItemClicked(list, position);
		} else if (inFamousArtists) {
			onFamousArtistItemClicked(list, position);
		}
	}

	public void onFamousArtistItemClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		IBaseListItem artist = (IBaseListItem) list.getItemAtPosition(position);
		BaseArtist baseArtist = new BaseArtist(artist.getId(), artist.getTitle());
		Intent intent = new Intent(activity, SimilarSongsToFamousArtist.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(baseArtist));
		activity.startActivity(intent);

		// try {
		// CompleteArtist completeArtist = activity.getData()
		// .getCompleteArtist(baseArtist);
		// List<BaseSong<BaseArtist, BaseAlbum>> songList;
		// songList = activity.getData().getClosestSongsToPosition(
		// completeArtist.getCoords(), 10);
		// setAdapter(songList);
		// handler.post(new Runnable() {
		//
		// @Override
		// public void run() {
		// activity
		// .getResultListTitle()
		// .setText(
		// activity
		// .getString(R.string.songs_might_be_similar));
		// activity.getResultListTitle().setTextColor(Color.GREEN);
		// }
		// });
		// } catch (DataUnavailableException e) {
		// Log.w(TAG, e);
		// }
	}

	@SuppressWarnings("unchecked")
	public void onTitleItemClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		BaseSong<BaseArtist, BaseAlbum> song = (BaseSong<BaseArtist, BaseAlbum>) list.getItemAtPosition(position);
		Intent intent = new Intent(activity, SongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);

	}

	public void onArtistItemClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		IBaseListItem artist = (IBaseListItem) list.getItemAtPosition(position);
		BaseArtist baseArtist = new BaseArtist(artist.getId(), artist.getTitle());
		controller.showAlbumList(activity, baseArtist);
	}

	public void onAlbumItemClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		IBaseListItem album = (IBaseListItem) list.getItemAtPosition(position);
		BaseAlbum baseAlbum = new BaseAlbum(album.getId(), album.getTitle());
		controller.showAlbumDetailInfo(activity, baseAlbum);
	}

	public boolean onItemLongClicked(AdapterView<?> list, int position, boolean inArtists, boolean inTitles,
			boolean inFamousArtists, boolean inAlbums) {
		if (inArtists) {
			return onArtistItemLongClicked(list, position);
		} else if (inTitles) {
			return onTitleItemLongClicked(list, position);
		} else if (inAlbums) {
			return onAlbumItemLongClicked(list, position);
		} else if (inFamousArtists) {
			return false;
		} else {
			return false;
		}
	}

	public boolean onFamousArtistsItemLongClicked(AdapterView<?> list, int position) {
		return false;
	}

	public boolean onTitleItemLongClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		BaseSong<BaseArtist, BaseAlbum> song = (BaseSong<BaseArtist, BaseAlbum>) list.getItemAtPosition(position);
		Intent intent = new Intent(activity, SongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);
		return true;
	}

	public boolean onArtistItemLongClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		BaseArtist artist = (BaseArtist) list.getItemAtPosition(position);
		Intent intent = new Intent(activity, ArtistListMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ARTIST, new ParcelableArtist(artist));
		activity.startActivity(intent);
		return true;
	}

	public boolean onAlbumItemLongClicked(AdapterView<?> list, int position) {
		controller.doHapticFeedback();
		BaseAlbum album = (BaseAlbum) list.getItemAtPosition(position);
		Intent intent = new Intent(activity, AlbumListMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_ALBUM, new ParcelableAlbum(album));
		activity.startActivity(intent);
		return true;
	}
}
