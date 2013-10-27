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

public enum SongStatus {
	BASE_DATA(1), WEB_DATA_OK(2), WEB_DATA_MISSING(3), WEB_DATA_ERROR(4);

	private int value;

	private SongStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static SongStatus getStatusForValue(int value) {
		switch (value) {
			case 1:
				return BASE_DATA;
			case 2:
				return WEB_DATA_OK;
			case 3:
				return WEB_DATA_MISSING;
			case 4:
				return WEB_DATA_ERROR;
		}
		return null;
	}

	@Override
	public String toString() {
		switch (value) {
			case 1:
				return "BASE_DATA";
			case 2:
				return "WEB_DATA_OK";
			case 3:
				return "WEB_DATA_MISSING";
			case 4:
				return "WEB_DATA_ERROR";
		}
		return null;
	}
}
