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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import ch.ethz.dcg.jukefox.data.db.LockHelper.Lock;

public class JdbcCursor implements ICursor {

	private ResultSet rs;
	private PreparedStatement prep;
	private final Lock lock;

	public JdbcCursor(ResultSet rs, PreparedStatement prep, Lock lock) {
		this.rs = rs;
		this.prep = prep;
		this.lock = lock; // Hold the lock until the cursor gets closed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#close()
	 */
	@Override
	public void close() {
		try {
			rs.close();
			prep.close();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
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
			Object o = rs.getObject(i + 1);
			return o == null; //rs.wasNull(); <-- not working?!?
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	// /* (non-Javadoc)
	// * @see ch.ethz.dcg.jukefox.db.ICursor#getCount()
	// */
	// @Override
	// public int getCount() {
	// try {
	// int rowCount;
	// int currentRow = rs.getRow();
	// rowCount = rs.last() ? rs.getRow() : 0;
	//
	// if (currentRow == 0)
	// rs.beforeFirst();
	// else
	// rs.absolute(currentRow);
	//
	// return rowCount;
	// } catch (SQLException e) {
	// throw new UncheckedSqlException(e);
	// }
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.db.ICursor#getString(int)
	 */
	@Override
	public String getString(int i) {
		try {
			// Android Cursor has zero-based index
			return rs.getString(i + 1);
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
			return rs.getInt(i + 1);
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
			return rs.getLong(i + 1);
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
			return rs.getFloat(i + 1);
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
			return rs.next();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	// /* (non-Javadoc)
	// * @see ch.ethz.dcg.jukefox.db.ICursor#moveToFirst()
	// */
	// @Override
	// public boolean moveToFirst() {
	// try {
	// return rs.first();
	// } catch (SQLException e) {
	// throw new UncheckedSqlException(e);
	// }
	// }

	@Override
	public int getColumnCount() {
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			return rsmd.getColumnCount();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}

	}

}
