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

import java.util.Stack;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.I18nManager;

public class HistoryManager {

	private final Stack<IHistoryItem> history = new Stack<IHistoryItem>();
	private final I18nManager i18nManager;
	private TabletPresenter presenter;

	private static interface IHistoryItem {

		void activate();

		String getActionBarTitle();

		boolean isHome();

		int getActionBarIcon();
	}

	public HistoryManager(I18nManager i18nManager) {
		this.i18nManager = i18nManager;
	}

	public void setTabletPresenter(TabletPresenter presenter) {
		this.presenter = presenter;
	}

	public boolean isCurrentHome() {
		return history.peek().isHome();
	}

	public String getCurrentActionBarTitle() {
		return history.peek().getActionBarTitle();
	}

	public int getCurrentActionBarIcon() {
		return history.peek().getActionBarIcon();
	}

	public void mapAlbumMaybe(BaseAlbum album) {
		pushHistory(new MapAlbumHistoryItem(album));
	}

	public void mapMaybe() {
		pushHistory(new MapHistoryItem());
	}

	public void exploreArtistMaybe(BaseArtist artist) {
		pushHistory(new ArtistHistoryItem(artist));
	}

	public void exploreAllAlbumsMaybe() {
		if (history.size() != 1) {
			history.clear();
			pushHistory(new AllAlbumsHistoryItem());
		}
	}

	public void exploreTagMaybe(BaseTag tag) {
		pushHistory(new TagHistoryItem(tag));
	}

	public boolean popHistory() {
		if (history.size() < 2) {
			return false;
		}
		history.pop();
		history.peek().activate();
		return true;
	}

	private void pushHistory(IHistoryItem item) {
		if (history.isEmpty() || !item.equals(history.peek())) {
			history.push(item);
			item.activate();
		}
	}

	private class ArtistHistoryItem implements IHistoryItem {

		private final BaseArtist artist;

		public ArtistHistoryItem(BaseArtist artist) {
			this.artist = artist;
		}

		@Override
		public void activate() {
			presenter.exploreArtist(artist);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (artist == null ? 0 : artist.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ArtistHistoryItem other = (ArtistHistoryItem) obj;
			return artist.equals(other.artist);
		}

		@Override
		public String getActionBarTitle() {
			return i18nManager.getActionBarTitleArtist(artist);
		}

		@Override
		public boolean isHome() {
			return false;
		}

		@Override
		public int getActionBarIcon() {
			return R.drawable.d022_app_icon;
		}
	}

	private class TagHistoryItem implements IHistoryItem {

		private final BaseTag tag;

		public TagHistoryItem(BaseTag tag) {
			this.tag = tag;
		}

		@Override
		public void activate() {
			presenter.exploreTag(tag);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (tag == null ? 0 : tag.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			TagHistoryItem other = (TagHistoryItem) obj;
			return tag.equals(other.tag);
		}

		@Override
		public String getActionBarTitle() {
			return i18nManager.getActionBarTitleTags();
		}

		@Override
		public boolean isHome() {
			return false;
		}

		@Override
		public int getActionBarIcon() {
			return R.drawable.d022_app_icon;
		}
	}

	private class AllAlbumsHistoryItem implements IHistoryItem {

		@Override
		public void activate() {
			presenter.exploreAllAlbums();
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			return getClass() == obj.getClass();
		}

		@Override
		public String getActionBarTitle() {
			return i18nManager.getActionBarTitleAllAlbums();
		}

		@Override
		public boolean isHome() {
			return true;
		}

		@Override
		public int getActionBarIcon() {
			return R.drawable.d022_app_icon;
		}
	}

	private class MapHistoryItem implements IHistoryItem {

		@Override
		public void activate() {
			presenter.map();
		}

		@Override
		public String getActionBarTitle() {
			return i18nManager.getActionBarTitleMap();
		}

		@Override
		public boolean isHome() {
			return false;
		}

		@Override
		public int hashCode() {
			return 17;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			return getClass() == obj.getClass();
		}

		@Override
		public int getActionBarIcon() {
			return R.drawable.d013_map;
		}
	}

	private class MapAlbumHistoryItem implements IHistoryItem {

		private final BaseAlbum album;

		public MapAlbumHistoryItem(BaseAlbum album) {
			this.album = album;
		}

		@Override
		public void activate() {
			presenter.mapAlbum(album);
		}

		@Override
		public String getActionBarTitle() {
			return i18nManager.getActionBarTitleMap();
		}

		@Override
		public boolean isHome() {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (album == null ? 0 : album.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MapAlbumHistoryItem other = (MapAlbumHistoryItem) obj;
			if (album == null) {
				if (other.album != null) {
					return false;
				}
			} else if (!album.equals(other.album)) {
				return false;
			}
			return true;
		}

		@Override
		public int getActionBarIcon() {
			return R.drawable.d013_map;
		}
	}
}
