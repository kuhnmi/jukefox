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
package ch.ethz.dcg.jukefox.data.db;

import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;

public class AndroidContentValues extends AbstractContentValues {

	private ContentValues cv;

	public AndroidContentValues() {
		super();
		this.cv = new ContentValues();
	}

	public AndroidContentValues(ContentValues cv) {
		super();
		this.cv = cv;
	}

	public ContentValues getContentValues() {
		return cv;
	}

	@Override
	public void put(String key, String value) {
		cv.put(key, value);
	}

	@Override
	public void put(String key, Float value) {
		cv.put(key, value);
	}

	@Override
	public void put(String key, Integer value) {
		cv.put(key, value);
	}

	@Override
	public void put(String key, Double value) {
		cv.put(key, value);
	}

	@Override
	public void put(String key, Boolean value) {
		cv.put(key, value);
	}

	@Override
	public void put(String key, Long value) {
		cv.put(key, value);
	}

	@Override
	public Object get(String key) {
		return cv.get(key);
	}

	@Override
	protected Set<Entry<String, Object>> getEntrySet() {
		return cv.valueSet();
	}
}
