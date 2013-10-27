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
package ch.ethz.dcg.pancho3.model.collection;

import android.os.Parcel;
import android.os.Parcelable;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.jukefox.model.collection.IRating;

/**
 * Smallest data type to represent a song
 * 
 */
public class ParcelableSong implements Parcelable, IBaseListItem, Comparable<ParcelableSong> {

	private int id;
	private String name;
	private ParcelableArtist artist;
	private ParcelableAlbum album;
	private IRating rating;
	private int duration;

	public ParcelableSong(int id, String name, ParcelableArtist artist, ParcelableAlbum album, int duration) {
		assert duration > 0;

		this.id = id;
		this.name = name;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
	}

	public ParcelableSong(BaseSong<BaseArtist, BaseAlbum> song) {
		this.id = song.getId();
		this.name = song.getName();
		this.artist = new ParcelableArtist(song.getArtist());
		this.album = new ParcelableAlbum(song.getAlbum());
		this.duration = song.getDuration();
	}

	public BaseSong<BaseArtist, BaseAlbum> getBaseSong() {
		return new BaseSong<BaseArtist, BaseAlbum>(id, name, artist.getBaseArtist(), album.getBaseAlbum(), duration);
	}

	@Override
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ParcelableArtist getArtist() {
		return artist;
	}

	public ParcelableAlbum getAlbum() {
		return album;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(name);
		// dest.writeParcelable(artist, 0);
		// dest.writeParcelable(album, 0);
		dest.writeInt(artist.getId());
		dest.writeString(artist.getName());
		dest.writeInt(album.getId());
		dest.writeString(album.getName());
		dest.writeInt(duration);
	}

	public static final Parcelable.Creator<ParcelableSong> CREATOR = new Parcelable.Creator<ParcelableSong>() {

		@Override
		public ParcelableSong createFromParcel(Parcel in) {
			return new ParcelableSong(in);
		}

		@Override
		public ParcelableSong[] newArray(int size) {
			return new ParcelableSong[size];
		}
	};

	private ParcelableSong(Parcel in) {
		id = in.readInt();
		name = in.readString();
		// artist = in.readParcelable(null);
		// album = in.readParcelable(null);
		artist = new ParcelableArtist(in.readInt(), in.readString());
		album = new ParcelableAlbum(in.readInt(), in.readString());
		duration = in.readInt();
	}

	@Override
	/**
	 * always returns the song title
	 */
	public String getSortString(boolean ignorePrefix) {
		return name;
	}

	@Override
	public String getSubTitle() {
		return artist.getName();
	}

	@Override
	public String getTitle() {
		return name;
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
		if (!(obj instanceof ParcelableSong)) {
			return false;
		}
		ParcelableSong other = (ParcelableSong) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

	/**
	 * We compare alphabetically by name.
	 */
	@Override
	public int compareTo(ParcelableSong another) {
		return this.name.compareTo(another.name);
	}
}
