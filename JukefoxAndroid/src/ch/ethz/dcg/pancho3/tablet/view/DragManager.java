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
package ch.ethz.dcg.pancho3.tablet.view;

import java.util.ArrayList;
import java.util.List;

import android.content.ClipData;
import android.util.SparseBooleanArray;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import ch.ethz.dcg.jukefox.model.collection.AllAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllRelatedAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllSongsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.pancho3.tablet.interfaces.AlbumAdapter;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.lists.ArtistListAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.ImageAlbumListAdapter;

public class DragManager {

	private final DataFetcher dataFetcher;
	private final TabletPresenter tabletPresenter;
	private ViewFactory viewFactory;

	private static final ClipData EMPTY_CLIP_DATA = ClipData.newPlainText("", "");

	private float lastX, lastY;

	private final float screenDensityScale;
	private final float touchSlop;

	public DragManager(DataFetcher dataFetcher, TabletPresenter tabletPresenter,
			float screenDensityScale, float touchSlop) {
		this.dataFetcher = dataFetcher;
		this.tabletPresenter = tabletPresenter;
		this.screenDensityScale = screenDensityScale;
		this.touchSlop = touchSlop;
	}

	public static class DragDataContainer<T> implements OnDataFetchedListener<List<T>> {

		private List<T> data;

		public DragDataContainer() {
		}

		public DragDataContainer(T item) {
			data = new ArrayList<T>();
			data.add(item);
		}

		public DragDataContainer(List<T> items) {
			this.data = items;
		}

		public List<T> getData() {
			return data;
		}

		public boolean isReady() {
			return data != null;
		}

		@Override
		public void onDataFetched(List<T> data) {
			this.data = data;
		}
	}

	private void startDraggingSong(View view, BaseSong<BaseArtist, BaseAlbum> song) {
		PlaylistSong<BaseArtist, BaseAlbum> playlistSong = new PlaylistSong<BaseArtist, BaseAlbum>(
				song, SongSource.MANUALLY_SELECTED);
		startDraggingImpl(view, new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>(
				playlistSong), viewFactory.createDragShadowBuilderSongs(1));
	}

	public void startDraggingSongs(View view, List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		List<PlaylistSong<BaseArtist, BaseAlbum>> playlistSongs =
				new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			playlistSongs.add(new PlaylistSong<BaseArtist, BaseAlbum>(
					song, SongSource.MANUALLY_SELECTED));
		}
		startDraggingImpl(view, new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>(
				playlistSongs), viewFactory.createDragShadowBuilderSongs(songs.size()));
	}

	private void startDraggingArtist(View view, BaseArtist artist) {
		DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> dragDataContainer =
				new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>();
		dataFetcher.fetchPlaylistSongsOfArtist(artist, dragDataContainer,
				SongSource.MANUALLY_SELECTED);
		startDraggingImpl(view, dragDataContainer, viewFactory.createDragShadowBuilderArtist());
	}

	private void startDraggingAlbum(View view, ListAlbum album) {
		if (album instanceof AllAlbumsRepresentative) {
			startDraggingArtist(view, album.getFirstArtist());
		} else if (album instanceof AllSongsRepresentative) {
			startDraggingAllSongs(view);
		} else if (album instanceof AllRelatedAlbumsRepresentative) {
			startDraggingAlbums(view,
					((AllRelatedAlbumsRepresentative) album).getRepresentedAlbums());
		} else {
			DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> dragDataContainer =
					new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>();
			dataFetcher.fetchPlaylistSongsOfAlbum(album, dragDataContainer,
					SongSource.MANUALLY_SELECTED);
			startDraggingImpl(view, dragDataContainer,
					viewFactory.createDragShadowBuilderAlbums(1));
		}
	}

	private void startDraggingAlbums(View view, List<? extends ListAlbum> albums) {
		DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> dragDataContainer =
				new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>();
		dataFetcher.fetchPlaylistSongsOfAlbums(albums, dragDataContainer,
				SongSource.MANUALLY_SELECTED);
		startDraggingImpl(view, dragDataContainer,
				viewFactory.createDragShadowBuilderAlbums(albums.size()));
	}

	private void startDraggingAllSongs(View view) {
		DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> dragDataContainer =
				new DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>();
		dataFetcher.fetchAllPlaylistSongs(dragDataContainer, SongSource.MANUALLY_SELECTED);
		startDraggingImpl(view, dragDataContainer, viewFactory.createDragShadowBuilderAllSongs());
	}

	private void startDraggingImpl(View view,
			DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> dragDataContainer,
			DragShadowBuilder dragShadowBuilder) {
		tabletPresenter.highlight();
		view.startDrag(EMPTY_CLIP_DATA, dragShadowBuilder,
				dragDataContainer, 0);
	}

	public boolean onDragEvent(DragEvent event) {
		switch (event.getAction()) {
			case DragEvent.ACTION_DRAG_STARTED:
				return true;
			case DragEvent.ACTION_DRAG_ENDED:
				tabletPresenter.unhighlight(event.getResult());
				return true;
		}
		return false;
	}

	public void registerSongListViewDragging(final ListView listView, final ISongAdapter adapter) {
		// Drag and drop on long click.
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				if (view == null) {
					return false;
				}
				if (listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
					if (!listView.getCheckedItemPositions().get(position)) {
						listView.performItemClick(listView, position, id);
					}
					ArrayList<BaseSong<BaseArtist, BaseAlbum>> songs =
							new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
					SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
					int count = checkedItems.size();
					for (int i = 0; i < count; i++) {
						int key = checkedItems.keyAt(i);
						if (checkedItems.get(key)) {
							songs.add(adapter.getSong(key));
						}
					}
					startDraggingSongs(view, songs);
				} else {
					startDraggingSong(view, adapter.getSong(position));
				}
				return true;
			}
		});
	}

	public void registerArtistListViewDragging(final ListView listView,
			final ArtistListAdapter adapter) {
		// Drag and drop on long click.
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				if (view == null) {
					return false;
				}
				startDraggingArtist(view, adapter.getItem(position));
				return true;
			}
		});
	}

	public void registerAlbumViewDragging(final AdapterView<?> albumView,
			final ImageAlbumListAdapter adapter) {
		// Drag and drop on long click.
		albumView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO: this shouldn't be null, but it becomes null from
				// this touch listener when we perform long click. investigate, also
				// the three time same code above.
				if (view == null) {
					return false;
				}
				startDraggingAlbum(view, adapter.getItem(position));
				return true;
			}
		});
	}

	public void registerAlbumViewDragging(final AlbumAdapter adapter) {
		// Drag and drop on long click.
		adapter.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO: this shouldn't be null, but it becomes null from
				// this touch listener when we perform long click. investigate, also
				// the three time same code above.
				if (view == null) {
					return false;
				}
				startDraggingAlbum(view, adapter.getItem(position));
				return true;
			}
		});
	}

	public float getDensitiyScreenScale() {
		return screenDensityScale;
	}

	public void setLatestXY(float lastX, float lastY) {
		this.lastX = lastX;
		this.lastY = lastY;
	}

	public void setViewFactory(ViewFactory viewFactory) {
		this.viewFactory = viewFactory;
	}

	public float getTouchSlop() {
		return touchSlop;
	}
}
