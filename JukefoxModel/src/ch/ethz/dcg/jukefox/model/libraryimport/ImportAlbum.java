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
package ch.ethz.dcg.jukefox.model.libraryimport;

import java.util.HashSet;

import ch.ethz.dcg.jukefox.data.db.DbUtils;

public class ImportAlbum {

	private final static String TAG = ImportAlbum.class.getSimpleName();

	private String name;
	private final HashSet<String> artistNames = new HashSet<String>();

	public ImportAlbum(String name, String artist) {
		setName(name);
		addArtistName(artist);
	}

	public ImportAlbum(String name) {
		setName(name);
	}

	public String getName() {
		return name;
	}

	public HashSet<String> getArtistNames() {
		return artistNames;
	}

	public void addArtistName(String name) {
		if (name == null || name.trim().length() == 0) {
			return;
		}
		artistNames.add(name.trim());
	}

	//	@Override
	//	public boolean equals(Object o) {
	//		if (o == null) {
	//			return false;
	//		}
	//		try {
	//			ImportAlbum a = (ImportAlbum) o;
	//			if (name == null) {
	//				if (a.name != null) {
	//					return false;
	//				}
	//				if (artistNames.equals(a.getArtistNames())) {
	//					return true;
	//				}
	//			} else {
	//				if (name.equals(a.name) && artistNames.equals(a.getArtistNames())) {
	//					return true;
	//				}
	//			}
	//			return false;
	//			// if (!name.equals(a.name)
	//			// || artistNames.size() != a.getArtistNames().size()) {
	//			// return false;
	//			// }
	//			// for (String artistName : a.getArtistNames()) {
	//			// if (!artistNames.contains(artistName)) {
	//			// return false;
	//			// }
	//			// }
	//			// return true;
	//		} catch (Exception e) {
	//			Log.w(TAG, e);
	//			Log.w(TAG, "returning result of super.equals()");
	//			return super.equals(o);
	//		}
	//	}

	public String getLogString() {
		return "album: " + name + ", artists: " + DbUtils.getValuesString(artistNames, "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artistNames == null) ? 0 : artistNames.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ImportAlbum other = (ImportAlbum) obj;
		if (artistNames == null) {
			if (other.artistNames != null) {
				return false;
			}
		} else if (!artistNames.equals(other.artistNames)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public void setName(String name) {
		this.name = name == null ? null : name.trim();
	}

}
