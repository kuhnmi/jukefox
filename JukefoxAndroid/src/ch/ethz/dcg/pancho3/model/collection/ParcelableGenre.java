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
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;

/**
 * Complete information about a genre
 * 
 */
public class ParcelableGenre implements Parcelable, IBaseListItem {

	int id;
	String name;

	public ParcelableGenre(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public ParcelableGenre(Genre genre) {
		this.id = genre.getId();
		this.name = genre.getName();
	}

	public Genre getGenre() {
		return new Genre(id, name);
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

	public static final Parcelable.Creator<ParcelableGenre> CREATOR = new Parcelable.Creator<ParcelableGenre>() {

		public ParcelableGenre createFromParcel(Parcel in) {
			return new ParcelableGenre(in);
		}

		public ParcelableGenre[] newArray(int size) {
			return new ParcelableGenre[size];
		}
	};

	private ParcelableGenre(Parcel in) {
		id = in.readInt();
		name = in.readString();
	}

	@Override
	/**
	 * always returns the genre name
	 */
	public String getSortString(boolean ignorePrefix) {
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
		if (!(obj instanceof ParcelableGenre)) {
			return false;
		}
		ParcelableGenre other = (ParcelableGenre) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}
