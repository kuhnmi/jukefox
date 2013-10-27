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

import android.content.res.Resources;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.pancho3.R;

public class I18nManager {

	private static final String REPLACE_TOKEN = "{0}";
	private final Resources resources;

	public I18nManager(Resources resources) {
		this.resources = resources;
	}

	public String getNumberOfSongsText(int numberOfSongs) {
		return numberOfSongs == 1 ? resources.getString(R.string._1_song) :
				resources.getString(R.string.x_songs).replace(REPLACE_TOKEN,
						Integer.toString(numberOfSongs));
	}

	public String getNumberOfAlbumsText(int numberOfAlbums) {
		return numberOfAlbums == 1 ? resources.getString(R.string._1_album) :
				resources.getString(R.string.x_albums).replace(REPLACE_TOKEN,
						Integer.toString(numberOfAlbums));
	}

	public String getExploreArtistText(BaseArtist artist) {
		return resources.getString(R.string.explore_artist).
				replace(REPLACE_TOKEN, artist.getTitle());
	}

	public String getMapAlbumText(BaseAlbum album) {
		return resources.getString(R.string.map_album).replace(REPLACE_TOKEN, album.getName());
	}

	public String getDisplayNumberOfAlbumsText(int numberOfAlbums) {
		return numberOfAlbums == 1 ? resources.getString(R.string.display_1_album) :
				resources.getString(R.string.display_x_albums).
						replace(REPLACE_TOKEN, Integer.toString(numberOfAlbums));
	}

	public String getDisplayNumberOfSongsText(int numberOfSongs) {
		return numberOfSongs == 1 ? resources.getString(R.string.display_1_song) :
				resources.getString(R.string.display_x_songs).
						replace(REPLACE_TOKEN, Integer.toString(numberOfSongs));
	}

	public String getActionBarTitleAllAlbums() {
		return resources.getString(R.string.all_albums);
	}

	public String getActionBarTitleTags() {
		return resources.getString(R.string.exploring_tags);
	}

	public String getActionBarTitleArtist(BaseArtist artist) {
		return artist.getName();
	}

	public String getActionBarTitleMap() {
		return resources.getString(R.string.music_map);
	}
}
