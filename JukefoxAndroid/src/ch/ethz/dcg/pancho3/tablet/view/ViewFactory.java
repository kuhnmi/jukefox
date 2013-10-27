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
import java.util.HashMap;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.AllAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllRelatedAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllSongsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.I18nManager;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.SearchOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.widget.AlbumImageView;
import ch.ethz.dcg.pancho3.tablet.widget.GridRow;
import ch.ethz.dcg.pancho3.tablet.widget.ImageLoadingPool;
import ch.ethz.dcg.pancho3.tablet.widget.QueueItem;

/**
 * Factory to create different views...
 */
public class ViewFactory implements OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>> {

	public static final int DRAG_SHADOW_WIDTH = 200;
	public static final int DRAG_SHADOW_HEIGHT = 60;

	private final LayoutInflater layoutInflater;
	private final DataFetcher dataFetcher;
	private final MenuInflater menuInflater;
	private final DragManager dragManager;
	private final float densityScale;
	private final I18nManager i18nManager;
	private final Resources resources;
	private final ImageLoadingPool imageLoadingPool;
	private final Bitmap borderBitmap;

	private HashMap<BaseAlbum, ArrayList<ImageView>> albumWaitMap =
			new HashMap<BaseAlbum, ArrayList<ImageView>>();

	public ViewFactory(LayoutInflater layoutInflater, DataFetcher dataFetcher,
			MenuInflater menuInflater, Resources resources, DragManager dragManager,
			float densityScale, I18nManager i18nManager) {
		this.layoutInflater = layoutInflater;
		this.dataFetcher = dataFetcher;
		this.menuInflater = menuInflater;
		this.dragManager = dragManager;
		this.densityScale = densityScale;
		this.i18nManager = i18nManager;
		this.resources = resources;

		imageLoadingPool = new ImageLoadingPool(dataFetcher);
		borderBitmap = BitmapFactory.decodeResource(resources, R.drawable.d158_border);
	}

	public QueueItem getSongViewQueue(BaseSong<BaseArtist, BaseAlbum> song, QueueItem convertView,
			ViewGroup parent, int position) {
		final QueueItem view;
		if (convertView == null || convertView.isDismissed()) {
			view = (QueueItem) layoutInflater.inflate(R.layout.tablet_listitem_queue, parent, false)
					.findViewById(R.id.item);
		} else {
			view = convertView;
		}
		((TextView) view.findViewById(R.id.text1)).setText(song.getTitle());
		((TextView) view.findViewById(R.id.text2)).setText(song.getArtist().getName() + " - " +
				song.getAlbum().getName());
		view.position = position;
		return view;
	}

	public View getArtistView(BaseArtist artist, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = layoutInflater.inflate(R.layout.tablet_listitem_single, parent, false);
			view.setOnTouchListener(new OnDragTouchListener(dragManager));
		} else {
			view = convertView;
		}
		((TextView) view.findViewById(R.id.text1)).setText(artist.getTitle());
		return view;
	}

	public View getSongView(BaseSong<BaseArtist, BaseAlbum> song, View convertView,
			ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = layoutInflater.inflate(R.layout.tablet_listitem, parent, false);
			view.setOnTouchListener(new OnDragTouchListener(dragManager));
		} else {
			view = convertView;
		}
		((TextView) view.findViewById(R.id.text1)).setText(song.getTitle());
		((TextView) view.findViewById(R.id.text2)).setText(song.getArtist().getName() + " - " +
				song.getAlbum().getName());
		return view;
	}

	public View getAlbumImageView(MapAlbum album, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = layoutInflater.inflate(R.layout.tablet_albumitem, parent, false);
			view.setOnTouchListener(new OnDragTouchListener(dragManager));
		} else {
			view = convertView;
		}
		bindAlbumImageView(album, view);
		return view;
	}

	private void bindAlbumImageView(MapAlbum album, View view) {
		ImageView albumArt = (ImageView) view.findViewById(R.id.image);
		albumArt.setImageBitmap(borderBitmap);
		albumArt.setTag(album);
		if (album instanceof AllAlbumsRepresentative) {
			applyImageStack(album.getFirstArtist(), albumArt, 200);
		} else if (album instanceof AllSongsRepresentative) {
			applyImageStack(albumArt, 200);
		} else if (album instanceof AllRelatedAlbumsRepresentative) {
			applyImageStack(((AllRelatedAlbumsRepresentative) album).getRepresentedAlbums(),
					albumArt, 200);
		} else {
			/*synchronized (albumWaitMap) {
				ArrayList<ImageView> list = albumWaitMap.get(album);
				if (list == null) {
					list = new ArrayList<ImageView>();
				}
				list.add(albumArt);
				albumWaitMap.put(album, list);
			}
			dataFetcher.fetchAlbumArt(this, false, false, album);*/
		}
		((TextView) view.findViewById(R.id.text1)).setText(album.getName());
		((TextView) view.findViewById(R.id.text2)).setText(album.getFirstArtist().getName());
	}

	public View getAlbumRow(List<? extends MapAlbum> albums, int startIndex, int endIndex,
			final OnItemClickListener onItemClickListener,
			final OnItemLongClickListener onItemLongClickListener, View convertView,
			int positionOffset) {

		final GridRow row;
		if (convertView != null && convertView instanceof GridRow) {
			row = (GridRow) convertView;
		} else {
			row = new GridRow(layoutInflater.getContext());
			for (int i = 0; i < 3; i++) {
				AlbumImageView view =
						(AlbumImageView) layoutInflater.inflate(R.layout.tablet_albumitem, null);
				view.setListeners(onItemClickListener, onItemLongClickListener);
				row.addView(view);
			}
		}
		int j = 0;
		for (int i = startIndex; i < endIndex; i++, j++) {
			AlbumImageView view = (AlbumImageView) row.getChildAt(j);
			view.bind(albums.get(i), i + positionOffset);
			view.setVisibility(View.VISIBLE);
			imageLoadingPool.add(view, albums.get(i));
		}
		for (; j < 3; j++) {
			row.getChildAt(j).setVisibility(View.GONE);
		}
		return row;
	}

	@Override
	public void onDataFetched(List<Pair<Bitmap, BaseAlbum>> data) {
		Bitmap bitmap = data.get(0).first;
		BaseAlbum album = data.get(0).second;
		synchronized (albumWaitMap) {
			ArrayList<ImageView> albumArts = albumWaitMap.get(album);
			if (albumArts != null) {
				for (ImageView albumArt : albumArts) {
					if (albumArt != null && album.equals(albumArt.getTag())) {
						albumArt.setImageBitmap(bitmap);
						albumArt.setVisibility(View.VISIBLE);
					}
				}
			}
			albumWaitMap.remove(album);
		}
	}

	public void applyImageStack(BaseArtist artist, final ImageView albumArt,
			final int pixelSize) {
		dataFetcher.fetchAlbumsOfArtist(artist, new OnDataFetchedListener<List<MapAlbum>>() {

			@Override
			public void onDataFetched(List<MapAlbum> albums) {
				ListAlbum[] array = new ListAlbum[getNumberOfImagesOnStack(albums.size())];
				for (int i = 0; i < array.length; i++) {
					array[i] = albums.get(i);
				}
				dataFetcher.fetchAlbumArt(
						new OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>>() {

							@Override
							public void onDataFetched(List<Pair<Bitmap, BaseAlbum>> data) {
								albumArt.setImageBitmap(getImageStackImpl(data, pixelSize));
							}
						}, false, false, array);
			}
		});
	}

	public void applyImageStack(final ImageView albumArt, final int pixelSize) {

		dataFetcher.fetchAllAlbums(new OnDataFetchedListener<List<MapAlbum>>() {

			@Override
			public void onDataFetched(List<MapAlbum> albums) {
				ListAlbum[] array = new ListAlbum[getNumberOfImagesOnStack(albums.size())];
				for (int i = 0; i < array.length; i++) {
					array[i] = albums.get(i);
				}
				dataFetcher.fetchAlbumArt(
						new OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>>() {

							@Override
							public void onDataFetched(List<Pair<Bitmap, BaseAlbum>> data) {
								albumArt.setImageBitmap(getImageStackImpl(data, pixelSize));
							}
						}, false, false, array);
			}
		});
	}

	public void applyImageStack(List<? extends ListAlbum> albums, final ImageView albumArt,
			final int pixelSize) {
		ListAlbum[] array = new ListAlbum[getNumberOfImagesOnStack(albums.size())];
		for (int i = 0; i < array.length; i++) {
			array[i] = albums.get(i);
		}
		dataFetcher.fetchAlbumArt(
				new OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>>() {

					@Override
					public void onDataFetched(List<Pair<Bitmap, BaseAlbum>> data) {
						albumArt.setImageBitmap(getImageStackImpl(data, pixelSize));
					}
				}, false, false, array);
	}

	private Bitmap getImageStackImpl(List<? extends Pair<Bitmap, ?>> data, int pixelSize) {
		pixelSize *= densityScale;
		Bitmap output = Bitmap.createBitmap(pixelSize, pixelSize, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint();
		int numberOfImagesOnStack = data.size();
		for (int i = 0; i < numberOfImagesOnStack; i++) {
			Bitmap bitmap = data.get(i).first;
			Matrix matrix = new Matrix();
			float xScale;
			float yScale;
			float xTranslate;
			float yTranslate;
			if (i < 3 || numberOfImagesOnStack <= 4) {
				xScale = (float) pixelSize / (2 * bitmap.getWidth());
				yScale = (float) pixelSize / (2 * bitmap.getHeight());
				xTranslate = i % 2 == 0 ? 0 : pixelSize / 2;
				yTranslate = i < 2 ? 0 : pixelSize / 2;
			} else {
				xScale = (float) pixelSize / (4 * bitmap.getWidth());
				yScale = (float) pixelSize / (4 * bitmap.getHeight());
				xTranslate = i % 2 == 1 ? pixelSize / 2 : 3 * pixelSize / 4;
				yTranslate = i < 5 ? pixelSize / 2 : 3 * pixelSize / 4;
			}
			matrix.postScale(xScale, yScale);
			matrix.postTranslate(xTranslate, yTranslate);

			canvas.drawBitmap(bitmap, matrix, paint);
		}
		return output;
	}

	private int getNumberOfImagesOnStack(int size) {
		return size < 7 ? Math.min(4, size) : 7;
	}

	public void inflateMenuExploreMap(Menu menu, BaseArtist artist, BaseAlbum album) {
		menu.clear();
		menuInflater.inflate(R.menu.overlay_menu_explore_map, menu);
		menu.findItem(R.id.explore).setTitle(i18nManager.getExploreArtistText(artist));
		menu.findItem(R.id.map).setTitle(i18nManager.getMapAlbumText(album));
	}

	public void inflateMenuExplore(Menu menu, BaseArtist artist) {
		menu.clear();
		menuInflater.inflate(R.menu.overlay_menu_explore, menu);
		menu.findItem(R.id.explore).setTitle(i18nManager.getExploreArtistText(artist));
	}

	public void inflateMenuMap(Menu menu, BaseAlbum album) {
		menu.clear();
		menuInflater.inflate(R.menu.overlay_menu_map, menu);
		menu.findItem(R.id.map).setTitle(i18nManager.getMapAlbumText(album));
	}

	public View createSearchSongView(ViewGroup parent) {
		View view = layoutInflater.inflate(R.layout.tablet_listitem, parent, false);
		view.setOnTouchListener(new OnDragTouchListener(dragManager));
		return view;
	}

	public SearchView createSearchView(SearchOverlayPresenter presenter) {
		SearchView searchView = new SearchView(layoutInflater.getContext());
		searchView.setIconifiedByDefault(false);
		searchView.setOnQueryTextListener(presenter);
		return searchView;
	}

	public DragShadowBuilder createDragShadowBuilderSongs(int numberOfSongs) {
		return createDragShadowBuilderImpl(i18nManager.getNumberOfSongsText(numberOfSongs));
	}

	public DragShadowBuilder createDragShadowBuilderAlbums(int numberOfAlbums) {
		return createDragShadowBuilderImpl(i18nManager.getNumberOfAlbumsText(numberOfAlbums));
	}

	public DragShadowBuilder createDragShadowBuilderAllSongs() {
		return createDragShadowBuilderImpl(resources.getString(R.string.all_songs));
	}

	private DragShadowBuilder createDragShadowBuilderImpl(String text) {
		View view = layoutInflater.inflate(R.layout.tablet_overlay_droparea, null);
		view.setBackgroundResource(R.drawable.d171_box_background_highlight);
		((TextView) view.findViewById(R.id.text)).setText(text);
		return new TabletDragShadowBuilder(view, (int) (DRAG_SHADOW_WIDTH * densityScale),
				(int) (DRAG_SHADOW_HEIGHT * densityScale));
	}

	public DragShadowBuilder createDragShadowBuilderArtist() {
		return createDragShadowBuilderImpl(resources.getString(R.string._1_artist));
	}

	public View getListSectionView(String title, View convertView, ViewGroup parent) {
		TextView text = new TextView(layoutInflater.getContext());
		text.setFocusable(false);
		text.setEnabled(false);
		text.setText(title);
		return text;
	}
}
