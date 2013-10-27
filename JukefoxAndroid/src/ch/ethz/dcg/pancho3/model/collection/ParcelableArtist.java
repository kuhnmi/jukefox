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
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;

/**
 * Smallest data type to represent an artist
 * 
 */
public class ParcelableArtist implements Parcelable, IBaseListItem, Comparable<ParcelableArtist> {

	private final int id;
	private final String name;

	public ParcelableArtist(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public ParcelableArtist(BaseArtist artist) {
		this.id = artist.getId();
		this.name = artist.getName();
	}

	public BaseArtist getBaseArtist() {
		return new BaseArtist(id, name);
	}

	public int getId() {
		return id;
	}

	public String getName() {
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

	}

	public static final Parcelable.Creator<ParcelableArtist> CREATOR = new Parcelable.Creator<ParcelableArtist>() {

		public ParcelableArtist createFromParcel(Parcel in) {
			return new ParcelableArtist(in);
		}

		public ParcelableArtist[] newArray(int size) {
			return new ParcelableArtist[size];
		}
	};

	private ParcelableArtist(Parcel in) {
		id = in.readInt();
		name = in.readString();
	}

	@Override
	public String getSortString(boolean ignorePrefix) {
		if (ignorePrefix) {
			if (name.toLowerCase().startsWith("the ")) {
				return name.substring(4).trim();
			}
		}
		return name;
	}

	@Override
	public String getSubTitle() {
		return "";
	}

	@Override
	public String getTitle() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * We compare alphabetically by name.
	 */
	@Override
	public int compareTo(ParcelableArtist another) {
		if (another == null) {
			return -1;
		}
		return this.name.compareTo(another.name);
	}

	@Override
	public int hashCode() {
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
		if (!(obj instanceof ParcelableArtist)) {
			return false;
		}
		ParcelableArtist other = (ParcelableArtist) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}
