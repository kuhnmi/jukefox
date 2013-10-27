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
package ch.ethz.dcg.jukefox.model.collection;

/**
 * Smallest data type to represent a song
 * 
 */
public class BaseSong<ArtistType extends BaseArtist, AlbumType extends BaseAlbum> implements IBaseListItem,
		Comparable<BaseSong<ArtistType, AlbumType>> {

	private int id;
	private String name;
	private ArtistType artist;
	private AlbumType album;
	private IRating rating;
	private int duration;

	public BaseSong(int id, String name, ArtistType artist, AlbumType album, int duration) {
		assert duration > 0 : "Song " + name + " has duration 0!";

		this.id = id;
		this.name = name;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
	}

	@Override
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ArtistType getArtist() {
		return artist;
	}

	public AlbumType getAlbum() {
		return album;
	}

	/**
	 * Returns the duration.
	 * 
	 * @return The duration in ms
	 */
	public int getDuration() {
		return duration;
	}

	/*public void setDuration(int duration) {
		this.duration = duration;
	}*/

	@Override
	public String toString() {
		return getArtist().getName() + " - " + getAlbum().getName() + " - " + getTitle();
	}

	@Override
	/**
	 * always returns the song title
	 */
	public String getSortString(boolean ignorePrefix) {
		return getName();
	}

	@Override
	public String getSubTitle() {
		return getArtist().getName();
	}

	@Override
	public String getTitle() {
		return getName();
	}

	public IRating getRating() {
		return rating;
	}

	public void setRating(IRating rating) {
		this.rating = rating;
	}

	@Override
	public int hashCode() {
		// kuhnmi: why computed like this? Seems to be used in conjunction with
		// Pair.hashCode()
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		if (!(obj instanceof BaseSong<?, ?>)) {
			return false;
		}
		BaseSong<?, ?> other = (BaseSong<?, ?>) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

	/**
	 * We compare alphabetically by name.
	 */
	@Override
	public int compareTo(BaseSong<ArtistType, AlbumType> another) {
		return this.name.compareTo(another.name);
	}
}
