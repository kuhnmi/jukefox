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
package ch.ethz.dcg.pancho3.tablet.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.jukefox.model.providers.AlbumProvider;
import ch.ethz.dcg.pancho3.view.commons.BitmapReflection;

/**
 * Class to fetch data asynchronously. Each fetch method expects a listener for
 * callback.
 */
public class DataFetcher {

	private static final String TAG = DataFetcher.class.getSimpleName();
	private final AndroidCollectionModelManager model;

	/**
	 * The constructor sets the model reference needed to obtain all the data.
	 */
	public DataFetcher(AndroidCollectionModelManager model) {
		this.model = model;
	}

	/**
	 * The universal listener for all the callbacks performed by DataFetcher.
	 * 
	 * @param <T>
	 *            The type of the value fetched.
	 */
	public static interface OnDataFetchedListener<T> {

		void onDataFetched(T data);
	}

	// Each method has its own private class to perform the asynchronous data
	// fetching.
	// Those classes all subclass this one.
	private abstract class Fetcher<Input, Result> extends AsyncTask<Input, Void, Result> {

		private final OnDataFetchedListener<Result> listener;

		public Fetcher(OnDataFetchedListener<Result> listener) {
			this.listener = listener;
		}

		// This happens on the UI thread.
		@Override
		protected void onPostExecute(Result result) {
			if (result != null) {
				listener.onDataFetched(result);
			}
		}
	}

	// Fetching related albums.

	/**
	 * Returns a list of related albums for a given artist. Each album comes
	 * with a float, which is the negative distance of this album to the closest
	 * artist's album which is was similar to.
	 * 
	 * The album/float pairs come in descending order by the floats, i.e. the
	 * album with the shortest distance comes first (since the values are
	 * negative).
	 */
	public void fetchRelatedAlbums(BaseArtist artist,
			OnDataFetchedListener<List<Pair<MapAlbum, Float>>> listener) {
		new RelatedAlbumFetcher(listener).execute(artist);
	}

	private class RelatedAlbumFetcher extends Fetcher<BaseArtist, List<Pair<MapAlbum, Float>>> {

		public RelatedAlbumFetcher(OnDataFetchedListener<List<Pair<MapAlbum, Float>>> listener) {
			super(listener);
		}

		@Override
		protected List<Pair<MapAlbum, Float>> doInBackground(BaseArtist... artist) {
			List<ListAlbum> albums = model.getAlbumProvider().getAllListAlbums(artist[0]);
			HashSet<ListAlbum> albumSet = new HashSet<ListAlbum>(albums);
			HashMap<MapAlbum, Float> allAlbums = new HashMap<MapAlbum, Float>();
			try {
				for (ListAlbum album : albums) {
					for (Pair<MapAlbum, Float> pair : model.getAlbumProvider().getSimilarAlbums(album, 20)) {
						Float value = allAlbums.get(pair.first);
						if (value == null) {
							value = -pair.second;
						} else {
							value = Math.max(value, -pair.second);
						}
						allAlbums.put(pair.first, value);
					}
				}
			} catch (DataUnavailableException e) {
			}

			List<Pair<MapAlbum, Float>> result = new ArrayList<Pair<MapAlbum, Float>>();

			for (Entry<MapAlbum, Float> entry : allAlbums.entrySet()) {
				if (!albumSet.contains(entry.getKey())) {
					result.add(new Pair<MapAlbum, Float>(entry.getKey(), entry.getValue()));
				}
			}
			Collections.sort(result, new WeightedItemComparator<Float>());
			return result;
		}
	}

	public void fetchRelatedAlbums2(BaseArtist artist,
			OnDataFetchedListener<List<MapAlbum>> listener) {
		new RelatedAlbumFetcher2(listener).execute(artist);
	}

	private class RelatedAlbumFetcher2 extends Fetcher<BaseArtist, List<MapAlbum>> {

		public RelatedAlbumFetcher2(OnDataFetchedListener<List<MapAlbum>> listener) {
			super(listener);
		}

		@Override
		protected ArrayList<MapAlbum> doInBackground(BaseArtist... artist) {
			TreeSet<MapAlbum> set = new TreeSet<MapAlbum>();
			try {
				// TODO: provide functionality lower in db
				AlbumProvider provider = model.getAlbumProvider();
				CompleteArtist completeArtist =
						model.getArtistProvider().getCompleteArtist(artist[0]);
				for (Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>> pair : model.getSongProvider()
						.getClosestSongsToPosition2(completeArtist.getCoords(), 50)) {
					set.add(provider.getMapAlbum(pair.first));
				}
			} catch (DataUnavailableException e) {
			}
			ArrayList<MapAlbum> list = new ArrayList<MapAlbum>();
			for (MapAlbum album : set) {
				if (!album.getArtists().contains(artist[0])) {
					list.add(album);
				}
			}
			return list;
		}
	}

	// Fetching albums

	/**
	 * Returns a list of all albums.
	 */
	public void fetchAllAlbums(OnDataFetchedListener<List<MapAlbum>> listener) {
		new FetchAlbums(listener).execute();
	}

	/**
	 * Returns a list of the albums of the given artist.
	 */
	public void fetchAlbumsOfArtist(BaseArtist artist,
			OnDataFetchedListener<List<MapAlbum>> listener) {
		new FetchAlbums(listener).execute(artist);
	}

	private class FetchAlbums extends Fetcher<BaseArtist, List<MapAlbum>> {

		public FetchAlbums(OnDataFetchedListener<List<MapAlbum>> listener) {
			super(listener);
		}

		@Override
		protected List<MapAlbum> doInBackground(BaseArtist... artist) {
			AlbumProvider provider = model.getAlbumProvider();
			List<MapAlbum> list;
			try {
				if (artist.length == 0) {
					list = provider.getAllMapAlbums();
				} else {
					// TODO: Provide this functionality at a lower level in the data base.
					list = new ArrayList<MapAlbum>();
					List<ListAlbum> listAlbums = provider.getAllListAlbums(artist[0]);
					for (ListAlbum album : listAlbums) {
						list.add(provider.getMapAlbum(album));
					}
				}
				Collections.sort(list);
			} catch (DataUnavailableException e) {
				list = new ArrayList<MapAlbum>();
			}
			return list;
		}
	}

	// Fetching songs

	public void fetchAllPlaylistSongs(final OnDataFetchedListener<List<PlaylistSong<BaseArtist, BaseAlbum>>> listener,
			final SongSource songSource) {
		new FetchSongs(new OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>>() {

			@Override
			public void onDataFetched(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
				listener.onDataFetched(toPlaylistSongs(songs, songSource));
			}
		}).execute();
	}

	/**
	 * Returns a list of all songs.
	 */
	public void fetchAllSongs(OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
		new FetchSongs(listener).execute();
	}

	/**
	 * Returns a list of the songs of a given artist.
	 */
	public void fetchSongsOfArtist(BaseArtist artist,
			OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
		new FetchSongs(listener).execute(artist);
	}

	public void fetchPlaylistSongsOfArtist(BaseArtist artist,
			final OnDataFetchedListener<List<PlaylistSong<BaseArtist, BaseAlbum>>> listener,
			final SongSource songSource) {
		new FetchSongs(new OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>>() {

			@Override
			public void onDataFetched(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
				listener.onDataFetched(toPlaylistSongs(songs, songSource));
			}
		}).execute(artist);
	}

	/**
	 * Returns a list of all the songs of a given album.
	 */
	public void fetchSongsOfAlbum(ListAlbum album,
			OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
		new FetchSongs(listener).execute(album);
	}

	public void fetchPlaylistSongsOfAlbum(ListAlbum album,
			final OnDataFetchedListener<List<PlaylistSong<BaseArtist, BaseAlbum>>> listener,
			final SongSource songSource) {
		new FetchSongs(new OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>>() {

			@Override
			public void onDataFetched(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
				listener.onDataFetched(toPlaylistSongs(songs, songSource));
			}
		}).execute(album);
	}

	private class FetchSongs extends Fetcher<IBaseListItem, List<BaseSong<BaseArtist, BaseAlbum>>> {

		public FetchSongs(OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
			super(listener);
		}

		// This happens asynchronously.
		@Override
		protected List<BaseSong<BaseArtist, BaseAlbum>> doInBackground(IBaseListItem... restriction) {
			if (restriction.length == 0) {
				// Display all the songs.
				List<BaseSong<BaseArtist, BaseAlbum>> songs = model.getSongProvider().getAllBaseSongs();
				Collections.sort(songs); // Sort alphabetically.
				return songs;
			} else if (restriction[0] instanceof BaseArtist) {
				BaseArtist artist = (BaseArtist) restriction[0];
				// Display all the songs of the artist.
				List<BaseSong<BaseArtist, BaseAlbum>> songs = model.getSongProvider().getAllBaseSongs(artist);
				Collections.sort(songs); // Sort alphabetically.
				return songs;
			} else if (restriction[0] instanceof ListAlbum) {
				// Display all the songs of the album
				ListAlbum album = (ListAlbum) restriction[0];
				try {
					// We don't sort here; the album already has a natural
					// order.
					return model.getAlbumProvider().getCompleteAlbum(album).getSongs();
				} catch (DataUnavailableException e) {
					return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
				}
			}
			// We shouldn't end up down here.
			return null;
		}
	}

	public void fetchSongsOfAlbums(List<? extends ListAlbum> albums,
			OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
		new FetchSongsFromAlbums(listener).execute(albums);
	}

	public void fetchPlaylistSongsOfAlbums(List<? extends ListAlbum> albums,
			final OnDataFetchedListener<List<PlaylistSong<BaseArtist, BaseAlbum>>> listener,
			final SongSource songSource) {
		new FetchSongsFromAlbums(new OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>>() {

			@Override
			public void onDataFetched(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
				listener.onDataFetched(toPlaylistSongs(songs, songSource));
			}
		}).execute(albums);
	}

	private class FetchSongsFromAlbums extends
			Fetcher<List<? extends ListAlbum>, List<BaseSong<BaseArtist, BaseAlbum>>> {

		public FetchSongsFromAlbums(
				OnDataFetchedListener<List<BaseSong<BaseArtist, BaseAlbum>>> listener) {
			super(listener);
		}

		// This happens asynchronously.
		@Override
		protected List<BaseSong<BaseArtist, BaseAlbum>> doInBackground(
				List<? extends ListAlbum>... restriction) {

			List<BaseSong<BaseArtist, BaseAlbum>> list = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
			for (ListAlbum album : restriction[0]) {
				// We don't sort here; the album already has a natural
				// order.
				try {
					list.addAll(model.getAlbumProvider().getCompleteAlbum(album).getSongs());
				} catch (DataUnavailableException e) {
				}
			}
			return list;
		}
	}

	// Fetching artists

	/**
	 * Returns a list of all the artists.
	 */
	public void fetchAllArtists(OnDataFetchedListener<List<BaseArtist>> listener) {
		new ArtistFetcher(listener).execute();
	}

	private class ArtistFetcher extends Fetcher<Void, List<BaseArtist>> {

		public ArtistFetcher(OnDataFetchedListener<List<BaseArtist>> listener) {
			super(listener);
		}

		// This happens asynchronously.
		@Override
		protected List<BaseArtist> doInBackground(Void... parameter) {
			List<BaseArtist> artists = model.getArtistProvider().getAllArtists();
			Collections.sort(artists); // Sort artists alphabetically.
			return artists;
		}
	}

	// Fetching Genres

	/**
	 * Returns a list of all the genres of a given artist where each genre comes
	 * with an integer which TODO: what exactly?
	 * 
	 * The genre/integer pairs come in descending order by the integer.
	 */
	public void fetchGenresForArtist(BaseArtist artist,
			OnDataFetchedListener<List<Pair<Genre, Integer>>> listener) {
		new GenreFetcher(listener).execute(artist);
	}

	private class GenreFetcher extends Fetcher<BaseArtist, List<Pair<Genre, Integer>>> {

		public GenreFetcher(OnDataFetchedListener<List<Pair<Genre, Integer>>> listener) {
			super(listener);
		}

		@Override
		protected List<Pair<Genre, Integer>> doInBackground(BaseArtist... artist) {
			try {
				List<Pair<Genre, Integer>> genres = model.getGenreProvider().getGenresForArtist(artist[0]);
				Collections.sort(genres, new WeightedItemComparator<Integer>());
				return genres;
			} catch (DataUnavailableException e) {
				return new ArrayList<Pair<Genre, Integer>>();
			}
		}
	}

	// Fetching Tags

	/**
	 * Returns a list of all the tags for a given artist. Each tag comes with a
	 * float which acts as a weight that this tag has for the given artist.
	 * 
	 * The tag/float pairs come sorted in descending order by the float.
	 */
	public void fetchTagsForArtist(BaseArtist artist,
			OnDataFetchedListener<List<Pair<CompleteTag, Float>>> listener) {
		new TagFetcher(listener).execute(artist);
	}

	private class TagFetcher extends Fetcher<BaseArtist, List<Pair<CompleteTag, Float>>> {

		public TagFetcher(OnDataFetchedListener<List<Pair<CompleteTag, Float>>> listener) {
			super(listener);
		}

		@Override
		protected List<Pair<CompleteTag, Float>> doInBackground(BaseArtist... artist) {
			List<Pair<CompleteTag, Float>> tags = new ArrayList<Pair<CompleteTag, Float>>();
			try {
				tags = model.getTagProvider()
						.getAllCompleteTags(model.getArtistProvider().getCompleteArtist(artist[0]));
				Collections.sort(tags, new WeightedItemComparator<Float>());
			} catch (DataUnavailableException e) {
				return new ArrayList<Pair<CompleteTag, Float>>();
			}
			return tags;
		}
	}

	public void fetchAlbumArt(OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>> listener,
			boolean forceLowResolution, boolean addReflection, BaseAlbum... albums) {
		new AlbumArtFetcher(listener, forceLowResolution, addReflection).execute(albums);
	}

	private class AlbumArtFetcher extends Fetcher<BaseAlbum, List<Pair<Bitmap, BaseAlbum>>> {

		private final boolean forceLowResolution;
		private final boolean addReflection;

		public AlbumArtFetcher(OnDataFetchedListener<List<Pair<Bitmap, BaseAlbum>>> listener,
				boolean forceLowResolution, boolean addReflection) {
			super(listener);
			this.forceLowResolution = forceLowResolution;
			this.addReflection = addReflection;
		}

		@Override
		protected List<Pair<Bitmap, BaseAlbum>> doInBackground(BaseAlbum... albums) {
			try {
				List<Pair<Bitmap, BaseAlbum>> list = new ArrayList<Pair<Bitmap, BaseAlbum>>();
				for (BaseAlbum album : albums) {
					if (album != null) {
						Bitmap albumArt = model.getAlbumArtProvider().getAlbumArt(album, forceLowResolution);
						if (addReflection) {
							albumArt = BitmapReflection.getReflection(albumArt);
						}
						list.add(new Pair<Bitmap, BaseAlbum>(albumArt, album));
					}
				}
				return list;
			} catch (NoAlbumArtException e) {
				return null;
			}
		}
	}

	public void fetchQueriedSongs(String query, OnDataFetchedListener<Pair<String, Cursor>> listener) {
		new QueriedSongsFetcher(listener).execute(query);
	}

	private class QueriedSongsFetcher extends Fetcher<String, Pair<String, Cursor>> {

		public QueriedSongsFetcher(OnDataFetchedListener<Pair<String, Cursor>> listener) {
			super(listener);
		}

		@Override
		protected Pair<String, Cursor> doInBackground(String... query) {
			return new Pair<String, Cursor>(query[0], model.getCursorProvider().findTitleBySearchStringCursor(
					query[0],
					100));
		}
	}

	public void fetchAlbumsForTag(BaseTag tag, OnDataFetchedListener<List<MapAlbum>> listener) {
		new AlbumsForTagFetcher(listener).execute(tag);
	}

	private class AlbumsForTagFetcher extends Fetcher<BaseTag, List<MapAlbum>> {

		public AlbumsForTagFetcher(OnDataFetchedListener<List<MapAlbum>> listener) {
			super(listener);
		}

		@Override
		protected List<MapAlbum> doInBackground(BaseTag... tags) {
			CompleteTag tag = getCompleteTag(tags[0]);
			try {
				// TODO: provide this functionality lower in the db.
				AlbumProvider provider = model.getAlbumProvider();
				TreeSet<MapAlbum> albums = new TreeSet<MapAlbum>();
				for (PlaylistSong<BaseArtist, BaseAlbum> song : model.getTagPlaylistGenerator().generatePlaylist(tag,
						30, 20)) {
					albums.add(provider.getMapAlbum(song));
				}
				return new ArrayList<MapAlbum>(albums);
			} catch (DataUnavailableException e) {
				return null;
			}
		}
	}

	public void fetchRelatedTagsForTag(BaseTag tag,
			OnDataFetchedListener<List<Pair<CompleteTag, Float>>> listener) {
		new TagForTagFetcher(listener).execute(tag);
	}

	private class TagForTagFetcher extends Fetcher<BaseTag, List<Pair<CompleteTag, Float>>> {

		public TagForTagFetcher(OnDataFetchedListener<List<Pair<CompleteTag, Float>>> listener) {
			super(listener);
		}

		@Override
		protected List<Pair<CompleteTag, Float>> doInBackground(BaseTag... tags) {
			ArrayList<Pair<CompleteTag, Float>> tagList = new ArrayList<Pair<CompleteTag, Float>>();
			try {
				Collection<CompleteTag> allTags = model.getTagProvider().getAllCompleteTags(200);
				CompleteTag tag = getCompleteTag(tags[0]);
				final float[] tagCoords = tag.getPlsaCoords();
				final float length = MathUtils.norm2(tagCoords);
				for (CompleteTag currentTag : allTags) {
					float[] currentCoords = currentTag.getPlsaCoords();

					if (currentCoords != null) {
						float value = (float) Math
								.acos(MathUtils.dotProduct(currentCoords, tagCoords)
										/ (MathUtils.norm2(currentCoords) * length));
						tagList.add(new Pair<CompleteTag, Float>(currentTag, value));
					}
				}

				Collections.sort(tagList, new Comparator<Pair<CompleteTag, Float>>() {

					@Override
					public int compare(Pair<CompleteTag, Float> object1,
							Pair<CompleteTag, Float> object2) {
						return Float.compare(object1.second, object2.second);
					}
				});
			} catch (DataUnavailableException e) {
			}
			return tagList.subList(0, Math.min(30, tagList.size()));
		}
	}

	private CompleteTag getCompleteTag(BaseTag tag) {
		final CompleteTag completeTag;
		if (tag instanceof CompleteTag) {
			completeTag = (CompleteTag) tag;
		} else {
			try {
				completeTag = model.getTagProvider().getCompleteTag(tag.getId());
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
				return null;
			}
		}
		return completeTag;
	}

	/**
	 * Comparator to sort pairs by descending order of the second element, a
	 * weight. If this weight is equal it sorts by the ascending order of the
	 * title of the first element (a base list item).
	 * 
	 * @param <T>
	 *            the class of the weights.
	 */
	private static class WeightedItemComparator<T extends Comparable<T>> implements
			Comparator<Pair<? extends IBaseListItem, T>> {

		@Override
		public int compare(Pair<? extends IBaseListItem, T> item1,
				Pair<? extends IBaseListItem, T> item2) {
			if (item1.second == item2.second) {
				return item1.first.getTitle().compareTo(item2.first.getTitle());
			}
			return item2.second.compareTo(item1.second);
		}
	}

	public AndroidCollectionModelManager getData() {
		return model;
	}

	private List<PlaylistSong<BaseArtist, BaseAlbum>> toPlaylistSongs(List<BaseSong<BaseArtist, BaseAlbum>> songs,
			SongSource songSource) {
		List<PlaylistSong<BaseArtist, BaseAlbum>> list = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			list.add(new PlaylistSong<BaseArtist, BaseAlbum>(song, songSource));
		}
		return list;
	}
}
