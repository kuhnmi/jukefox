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

import android.database.Cursor;
import android.database.SQLException;

public class AndroidCursor implements ICursor {

	private Cursor cur;

	public AndroidCursor(Cursor cur) {
		this.cur = cur;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#close()
	 */
	@Override
	public void close() {
		try {
			cur.close();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#isNull(int)
	 */
	@Override
	public boolean isNull(int i) {
		try {
			return cur.isNull(i);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getString(int)
	 */
	@Override
	public String getString(int i) {
		try {
			// Android Cursor has zero-based index
			return cur.getString(i);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getInt(int)
	 */
	@Override
	public int getInt(int i) {
		try {
			// Android Cursor has zero-based index
			return cur.getInt(i);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getLong(int)
	 */
	@Override
	public long getLong(int i) {
		try {
			// Android Cursor has zero-based index
			return cur.getLong(i);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getFloat(int)
	 */
	@Override
	public float getFloat(int i) {
		try {
			// Android Cursor has zero-based index
			return cur.getFloat(i);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#moveToNext()
	 */
	@Override
	public boolean moveToNext() {
		try {
			return cur.moveToNext();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		try {
			return cur.getColumnCount();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}

	}

}
