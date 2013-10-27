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

import java.util.Calendar;

public class DateTag {

	@SuppressWarnings("unused")
	private static final String TAG = DateTag.class.getSimpleName();
	private int id;
	private long from;
	private long to;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getFrom() {
		return from;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public long getTo() {
		return to;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public long getTime() {
		return (to + from) / 2;
	}

	public long getRange() {
		return to - from;
	}

	public static DateTag getDateTag(CompleteTag t) {
		String name = t.getName();
		int range;
		if (name.endsWith("s")) {
			name = name.substring(0, name.length() - 1);
			// Log.v(TAG, "name without trailing s: " + name);
		}
		if (!name.matches("\\d*")) {
			// Log.v(TAG, "name '" + name + "' is not numeric!");
			return null;
		}
		if (name.length() != 4 && name.length() != 2) {
			return null;
		}
		if (name.endsWith("0")) {
			range = 10;
		} else {
			range = 1;
		}
		if (name.length() == 2) {
			if (name.equals("00")) {
				name = "2000";
			} else {
				name = "19" + name;
			}
		}
		int year = Integer.parseInt(name);
		Calendar cal = Calendar.getInstance();
		cal.set(year, 1, 1);
		long time1 = cal.getTimeInMillis();
		cal.roll(Calendar.YEAR, range);
		long time2 = cal.getTimeInMillis();
		DateTag dateTag = new DateTag();
		dateTag.setFrom(time1);
		dateTag.setTo(time2);
		dateTag.setId(t.getId());
		return dateTag;
	}

}
