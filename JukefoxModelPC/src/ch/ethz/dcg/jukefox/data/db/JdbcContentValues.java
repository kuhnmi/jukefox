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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class JdbcContentValues extends AbstractContentValues {

	private HashMap<String, Object> hm;

	public JdbcContentValues() {
		hm = new HashMap<String, Object>();
	}

	public JdbcContentValues(HashMap<String, Object> hm) {
		this.hm = hm;
	}

	public Set<Entry<String, Object>> valueSet() {
		return hm.entrySet();
	}

	@Override
	public void put(String key, String value) {
		hm.put(key, value);
	}

	@Override
	public void put(String key, Float value) {
		hm.put(key, value);
	}

	@Override
	public void put(String key, Integer value) {
		hm.put(key, value);
	}

	@Override
	public void put(String key, Double value) {
		hm.put(key, value);
	}

	@Override
	public void put(String key, Boolean value) {
		hm.put(key, value);
	}

	@Override
	public void put(String key, Long value) {
		hm.put(key, value);
	}

	@Override
	public Object get(String key) {
		return hm.get(key);
	}

	@Override
	protected Set<Entry<String, Object>> getEntrySet() {
		return hm.entrySet();
	}

}
