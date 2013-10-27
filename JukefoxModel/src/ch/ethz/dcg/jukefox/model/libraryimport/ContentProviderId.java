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

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class ContentProviderId {

	private final static String TAG = ContentProviderId.class.getSimpleName();

	public enum Type {
		INTERNAL, EXTERNAL;
	}

	private final int id;
	private final Type type;

	public ContentProviderId(int id, Type type) {
		this.id = id;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		if (type == Type.EXTERNAL) {
			return id;
		}
		return -id;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		try {
			ContentProviderId cpId = (ContentProviderId) o;
			return id == cpId.id && type == cpId.type;
		} catch (Exception e) {
			Log.w(TAG, e);
			Log.w(TAG, "returning result of super.equals()");
			return super.equals(o);
		}
	}

}
