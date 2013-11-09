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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.LanguageHelper;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.db.LockHelper.Lock;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;

public class SqlJdbcDbDataPortal extends SqlDbDataPortal<JdbcContentValues> {

	private final static String TAG = SqlJdbcDbDataPortal.class.getSimpleName();

	private class JdbcSqlDbConnection implements ISqlDbConnection {

		private Connection db = null;

		@Override
		public boolean open() {
			if (isOpen()) {
				return true;
			}

			try {
				openOrCreateDatabase();

				int version = getVersion();
				if (version != Constants.DB_VERSION) {
					beginTransaction();
					try {
						onUpgrade(version);

						setVersion(Constants.DB_VERSION);
						setTransactionSuccessful();
					} finally {
						endTransaction();
					}
				}

				return true;
			} catch (Exception e) {
				Log.w(TAG, e);
				return false;
			}
		}

		private void openOrCreateDatabase() {
			Log.d(TAG, "openOrCreateDatabase()");
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (java.lang.ClassNotFoundException e) {
				Log.w(TAG, e);
				throw new UncheckedSqlException(e);
			}

			try {
				db = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
			} catch (SQLException e) {
				Log.w(TAG, e);
				throw new UncheckedSqlException(e);
			}
		}

		@Override
		public boolean isOpen() {
			try {
				return (db != null) && !db.isClosed();
			} catch (SQLException e) {
				return false;
			}
		}

		@Override
		public void close() {
			if (!isOpen()) {
				return;
			}

			Lock lock = lockX();
			try {
				db.close();
			} catch (SQLException e) {
				Log.w(TAG, e);
			} finally {
				lock.release();
			}
		}

		protected Connection getDatabase() {
			return db;
		}
	}

	public SqlJdbcDbDataPortal(DirectoryManager directoryManager) {
		super(directoryManager, new LanguageHelper());
	}

	public SqlJdbcDbDataPortal(DirectoryManager directoryManager, String dbUrl) {
		super(directoryManager, new LanguageHelper(), dbUrl);
	}

	public SqlJdbcDbDataPortal(DirectoryManager directoryManager, String dbUrl, String user, String password) {
		super(directoryManager, new LanguageHelper(), dbUrl, user, password);
	}

	@Override
	protected void init() {
	}

	@Override
	protected JdbcContentValues createContentValues() {
		return new JdbcContentValues();
	}

	@Override
	protected ISqlDbConnection createDbConnection() {
		return new JdbcSqlDbConnection();
	}

	@Override
	public JdbcSqlDbConnection getConnection() {
		return (JdbcSqlDbConnection) super.getConnection();
	}

	private Connection getDatabase() {
		return getConnection().getDatabase();
	}

	private void ensureDbIsOpen() {
		if (!getConnection().isOpen()) {
			if (!open()) {
				throw new UncheckedSqlException("couldn't open db");
			}
		}
	}

	@Override
	protected void dropRegularTables() {
		Lock lock = lockX();
		try {
			List<String> statements = new ArrayList<String>();
			ICursor cur = null;
			try {
				cur = execSelect("SELECT type, name " +
						"FROM SQLITE_MASTER " +
						"WHERE ((type = 'table') OR (type = 'view')) " +
						"  AND (name != 'sqlite_sequence') " +
						"  AND (NOT name LIKE 'backup_%') ", null);

				while (cur.moveToNext()) {
					if ("table".equals(cur.getString(0))) {
						statements.add("DROP TABLE " + cur.getString(1));
					} else {
						statements.add("DROP VIEW " + cur.getString(1));
					}
				}
			} finally {
				if (cur != null) {
					cur.close();
				}
			}

			beginTransaction();
			try {
				for (String statement : statements) {
					execSQL(statement);
				}
				setTransactionSuccessful();
			} finally {
				endTransaction();
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public boolean deleteDatabase() {
		Lock lock = lockX();
		try {
			close();

			File dbfile = directoryManager.getDataBaseFile();
			dbfile.delete();

			return true;
		} catch (RuntimeException e) {
			Log.w(TAG, e);
			return false;
		} finally {
			lock.release();
		}
	}

	@Override
	public ICursor execSelect(String sql, String[] selectionArgs) {
		ensureDbIsOpen();
		Lock lock = lockS();
		try {
			// prep = conn.prepareStatement(sql,
			// ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			PreparedStatement prep = getDatabase().prepareStatement(sql);
			// Bind the values
			if (selectionArgs != null) {
				for (int i = 0; i < selectionArgs.length; i++) {
					prep.setObject(i + 1, selectionArgs[i]);
				}
			}

			// Run the program and then cleanup
			ResultSet rs = prep.executeQuery();
			return new JdbcCursor(rs, prep, lock); // rs & prep are closed in here
		} catch (SQLException e) {
			lock.release();
			throw new UncheckedSqlException(e);
		}
	}

	/**
	 * Returns a SQL INSERT statement that is suitable to insert the given content values into the given table. If
	 * <code>replace</code> is true, the statement will read "INSERT OR REPLACE".
	 * 
	 * @param table
	 *            The name of the table where the values are to be inserted.
	 * @param contentValues
	 *            the values to be inserted
	 * @param replace
	 *            A boolean indicating whether the inserting conflicting rows should be silently ignored.
	 * @return the SQL INSERT statement that is suitable to insert the given content values into the given table.
	 */
	protected String getInsertStatement(String table, JdbcContentValues contentValues, boolean ignoreConflicts) {
		// Measurements show most sql lengths <= 152
		StringBuilder sql = new StringBuilder(152);
		sql.append("INSERT");
		if (ignoreConflicts) {
			sql.append(" OR IGNORE");
		}
		sql.append(" INTO ");
		sql.append(table);
		sql.append(" ");
		// Measurements show most values lengths < 40
		StringBuilder values = new StringBuilder(40);

		Set<Map.Entry<String, Object>> entrySet = null;

		if (contentValues != null && contentValues.size() > 0) {
			entrySet = contentValues.valueSet();
			Iterator<Map.Entry<String, Object>> entriesIter = entrySet.iterator();
			sql.append('(');

			boolean needSeparator = false;
			while (entriesIter.hasNext()) {
				if (needSeparator) {
					sql.append(", ");
					values.append(", ");
				}
				needSeparator = true;
				Map.Entry<String, Object> entry = entriesIter.next();
				sql.append(entry.getKey());
				values.append('?');
			}

			sql.append(')');
		} else {
			throw new IllegalArgumentException("Empty or null ContentValues");
		}

		sql.append(" VALUES(");
		sql.append(values);
		sql.append(");");

		String query = sql.toString();

		return query;
	}

	private String getUpdateStatement(String table, JdbcContentValues contentValues,
			JdbcContentValues whereClauseContentValues) {
		if (contentValues == null || contentValues.size() == 0) {
			throw new IllegalArgumentException("Empty values");
		}

		StringBuilder sql = new StringBuilder(120);
		sql.append("UPDATE ");

		sql.append(table);
		sql.append(" SET ");

		for (ContentValue cv : contentValues) {
			sql.append(cv.getKey()).append("=?").append(", ");
		}
		sql.delete(sql.length() - 2, sql.length());

		sql.append(" WHERE ");
		sql.append(getWhereCondition(whereClauseContentValues));
		sql.append(";");

		return sql.toString();
	}

	/**
	 * Binds the variables in <code>fieldNamesToBind</code> in the given prepared statement with the given content
	 * values.
	 * 
	 * @param prep
	 *            The {@link PreparedStatement} whose values should be bound.
	 * @param contentValues
	 *            The values to bind.
	 */
	protected void bindPreparedStatement(PreparedStatement prep, JdbcContentValues contentValues) {
		bindPreparedStatement(prep, contentValues, 0);
	}

	/**
	 * Binds the variables in <code>fieldNamesToBind</code> in the given prepared statement with the given content
	 * values.
	 * 
	 * @param prep
	 *            The {@link PreparedStatement} whose values should be bound.
	 * @param contentValues
	 *            The values to bind.
	 * @param alreadyBoundNumber
	 *            This is usually 0 unless you want to bind more values to a {@link PreparedStatement} to which you have
	 *            already bound a few. In this case, this parameter should be the number of values you have already
	 *            bound prior to calling this method again.
	 */
	protected void bindPreparedStatement(PreparedStatement prep, JdbcContentValues contentValues, int alreadyBoundNumber) {
		try {
			// TODO take proper care of the types of the values in the PreparedStatement. Strings should be set with setString(..) and so on.

			// Bind the values
			int i = alreadyBoundNumber + 1;
			for (ContentValue cv : contentValues) {
				if (cv.getValue() == null) {
					prep.setObject(i, cv.getValue());
				} else if (cv.getValue().getClass().equals(Float.class)) {
					prep.setFloat(i, (Float) cv.getValue());
				} else if (cv.getValue().getClass().equals(String.class)) {
					prep.setString(i, (String) cv.getValue());
				} else {
					prep.setObject(i, cv.getValue());
				}
				i++;
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	@Override
	protected synchronized void insertBatch(String table, BatchContentValues batchContentValues, boolean ignoreConflicts)
			throws UncheckedSqlException {
		ensureDbIsOpen();

		if (batchContentValues.getContentValues().size() == 0) {
			Log.v(TAG, "Empty batch content values => returning");
			return;
		}

		beginTransaction();
		String query = getInsertStatement(table, batchContentValues.getContentValues().get(0), ignoreConflicts);
		try {
			PreparedStatement prep = getDatabase().prepareStatement(query);
			try {
				for (JdbcContentValues cv : batchContentValues.getContentValues()) {
					bindPreparedStatement(prep, cv);
					prep.addBatch();
				}
			} finally {
				prep.executeBatch();

				if (prep != null) {
					prep.close();
				}

				setTransactionSuccessful();
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			endTransaction();
		}
	}

	@Override
	protected synchronized void updateBatch(String table, BatchContentValues updateContentValues,
			BatchContentValues whereClauseContentValues)
			throws UncheckedSqlException {

		assert updateContentValues.size() == whereClauseContentValues.size();

		if (updateContentValues.getContentValues().size() == 0) {
			return;
		}

		ensureDbIsOpen();
		beginTransaction();
		try {
			String query = getUpdateStatement(table, updateContentValues.getContentValues().get(0),
					whereClauseContentValues
							.getContentValues().get(0));

			PreparedStatement prep = getDatabase().prepareStatement(query);
			try {
				for (Iterator<JdbcContentValues> itUp = updateContentValues.getContentValues().iterator(), itWhere = whereClauseContentValues
						.getContentValues().iterator(); itUp.hasNext();) {
					JdbcContentValues cv = itUp.next();
					JdbcContentValues cvWhere = itWhere.next();
					bindPreparedStatement(prep, cv);
					bindPreparedStatement(prep, cvWhere, cv.size());
					prep.addBatch();
				}
			} finally {
				prep.executeBatch();
				if (prep != null) {
					prep.close();
				}

				setTransactionSuccessful();
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			endTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.database.sqlite.SQLiteDatabase
	 */
	@Override
	public long insertOrThrow(String table, JdbcContentValues contentValues) {
		ensureDbIsOpen();
		Lock lock = lockX();
		String query = getInsertStatement(table, contentValues, false);
		try {
			PreparedStatement prep = getDatabase().prepareStatement(query);
			ResultSet rs = null;
			try {
				bindPreparedStatement(prep, contentValues);

				prep.executeUpdate();

				rs = prep.getGeneratedKeys();
				Long lastInsertRow = null;
				if (rs.next()) {
					lastInsertRow = rs.getLong(1);
				} else {
					lastInsertRow = -1L;
					Log.e(TAG, "Error inserting " + contentValues + " using " + query);
				}
				return lastInsertRow;
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (prep != null) {
					prep.close();
				}
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	@Override
	public void execSQL(String sql) {
		Lock lock = lockX();
		try {
			execSQLNoLock(sql, getConnection());
		} finally {
			lock.release();
		}
	}

	@Override
	public void execSQLNoLock(String sql, ISqlDbConnection connection) {
		ensureDbIsOpen();
		Statement stmt = null;
		try {
			try {
				stmt = ((JdbcSqlDbConnection) connection).getDatabase().createStatement();
				stmt.execute(sql);
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	@Override
	public int update(String table, JdbcContentValues values, String whereClause, String[] whereArgs) {
		ensureDbIsOpen();
		Lock lock = lockX();
		StringBuilder sql = new StringBuilder(120);
		try {
			if (values == null || values.size() == 0) {
				throw new IllegalArgumentException("Empty values");
			}

			sql.append("UPDATE ");

			sql.append(table);
			sql.append(" SET ");

			Set<Map.Entry<String, Object>> entrySet = values.valueSet();
			Iterator<Map.Entry<String, Object>> entriesIter = entrySet.iterator();

			while (entriesIter.hasNext()) {
				Map.Entry<String, Object> entry = entriesIter.next();
				sql.append(entry.getKey());
				sql.append("=?");
				if (entriesIter.hasNext()) {
					sql.append(", ");
				}
			}

			if (!Utils.isNullOrEmpty(whereClause, false)) {
				sql.append(" WHERE ");
				sql.append(whereClause);
			}

			PreparedStatement prep = null;
			try {
				prep = getDatabase().prepareStatement(sql.toString());

				// Bind the values
				int size = entrySet.size();
				entriesIter = entrySet.iterator();
				int bindArg = 1;
				for (int i = 0; i < size; i++) {
					Map.Entry<String, Object> entry = entriesIter.next();

					// this is a workaround for Exception: unexpected param
					// type: class java.lang.Float
					if (entry.getValue() instanceof Float) {
						prep.setFloat(i + 1, (Float) entry.getValue());
					} else {
						prep.setObject(i + 1, entry.getValue());
					}
					bindArg++;
				}

				if (whereArgs != null) {
					size = whereArgs.length;
					for (int i = 0; i < size; i++) {
						prep.setString(bindArg, whereArgs[i]);
						bindArg++;
					}
				}

				// Run the program and then cleanup
				int numChangedRows = prep.executeUpdate();
				return numChangedRows;
			} finally {
				if (prep != null) {
					prep.close();
				}
			}
		} catch (SQLException e) {
			Log.e(TAG, "Error updating " + values + " using " + sql);
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	@Override
	public int delete(String table, String whereClause, String[] whereArgs) {
		ensureDbIsOpen();
		Lock lock = lockX();
		PreparedStatement prep = null;
		try {
			prep = getDatabase().prepareStatement("DELETE FROM " + table
					+ (!Utils.isNullOrEmpty(whereClause, false) ? " WHERE " + whereClause : ""));
			if (whereArgs != null) {
				for (int i = 0; i < whereArgs.length; i++) {
					prep.setObject(i + 1, whereArgs[i]);
				}
			}
			int numChangedRows = prep.executeUpdate();
			return numChangedRows;
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
			try {
				if (prep != null) {
					prep.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	//	
	//	@Override
	//	public void batchInsertFamousArtists(int[] ids, String[] names, float[][] coords) throws DataWriteException {
	//		synchronized (this) {
	//			String sql = "INSERT OR REPLACE INTO " + TblArtists.TBL_NAME + "(" + TblArtists.NAME + ", " + TblArtists.ME_ARTIST_ID + ", " + TblArtists.ME_NAME + ", " + TblArtists.IS_FAMOUS_ARTIST + ")" + " VALUES (?, ?, ?, ?);";
	//			PreparedStatement stat = null;
	//			try {
	//				stat = conn.prepareStatement(sql);
	//				for (int i = 0; i < ids.length; i++) {
	//					stat.setString(1, names[i]);
	//					stat.setInt(2, ids[i]);
	//					stat.setString(3, names[i]);
	//					stat.setBoolean(4, true);
	//					stat.addBatch();					
	//				}
	//				stat.executeBatch();
	//				
	//				ResultSet generatedKeys = stat.getGeneratedKeys();
	//				int[] newKeys = new int[ids.length];
	//				
	//				// TODO unfortunately, the generatedKeys array does only contain the key generated by the last batch statement and not all of them
	//				// hence it is for now not possible to insert the corresponding artist coords in an efficient way.
	//				int k = 0;
	//				while (generatedKeys.next()) {
	//					newKeys[k++] = generatedKeys.getInt(1);					
	//				}
	//				stat.close();
	//				
	//				StringBuilder sb = new StringBuilder();
	//				sb.append("INSERT OR REPLACE INTO " + TblArtistCoords.TBL_NAME + " VALUES (");
	//				sb.append("?, ");
	//				for (int j = 0; j < 30; j++) {
	//					sb.append("?, ");
	//				}
	//				sb.append("?);");
	//				stat = conn.prepareStatement(sb.toString());
	//				
	//				for (int i = 0; i < ids.length; i++) {
	//					stat.setInt(1, ids[i]);
	//					for (int j = 0; j < 31; j++) {
	//						stat.setFloat(j+2, coords[i][j]);
	//					}
	//					stat.addBatch();
	//				}
	//				stat.executeBatch();
	//				stat.close();
	//				
	//			} catch (SQLException sqle) {
	//				throw new DataWriteException(sqle);
	//			} finally {
	//				if (stat != null) {
	//					try {
	//						stat.close();
	//					} catch (SQLException e) {
	//						e.printStackTrace();
	//					}
	//				}
	//			}
	//		}
	//	}

	/**
	 * Gets the database version.
	 * 
	 * @return the database version
	 */
	private int getVersion() {
		Lock lock = lockS();
		try {
			Statement stmt = null;
			ResultSet rs = null;
			long version;
			try {
				Connection db = getDatabase();
				if (db == null && ((JdbcSqlDbConnection) getDefaultConnection()).getDatabase() == null) {
					// we are in the startup case (transaction connection is opened first and tries to read the version). 
					// TODO(kuhnmi): find a better solution for that...
					db = ((JdbcSqlDbConnection) getTransactionConnection()).getDatabase();
				}
				stmt = db.createStatement();
				rs = stmt.executeQuery("PRAGMA user_version;");
				if (rs.next()) {
					version = rs.getLong(1);
				} else {
					throw new SQLException("Error PRAGMA user_version");
				}
				return (int) version;
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	/**
	 * Sets the database version.
	 * 
	 * @param version
	 *            the new database version
	 */
	private void setVersion(int version) {
		execSQL("PRAGMA user_version = " + version);
	}

}