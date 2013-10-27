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

public enum AlbumStatus {
	COVER_UNCHECKED(1), CREATED_DEFAULT_COVER(2), DIRECTORY_COVER(3), CONTENT_PROVIDER_COVER(4), WEB_COVER(5), WEB_ERROR(
			6);

	private int value;

	private AlbumStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static AlbumStatus getStatusFromValue(int value) {
		switch (value) {
			case 1:
				return COVER_UNCHECKED;
			case 2:
				return CREATED_DEFAULT_COVER;
			case 3:
				return DIRECTORY_COVER;
			case 4:
				return CONTENT_PROVIDER_COVER;
			case 5:
				return WEB_COVER;
			case 6:
				return WEB_ERROR;
		}
		return null;
	}

	@Override
	public String toString() {
		switch (value) {
			case 1:
				return "COVER_UNCHECKED";
			case 2:
				return "CREATED_DEFAULT_COVER";
			case 3:
				return "DIRECTORY_COVER";
			case 4:
				return "CONTENT_PROVIDER_COVER";
			case 5:
				return "WEB_COVER";
			case 6:
				return "WEB_ERROR";
		}
		return null;
	}
}