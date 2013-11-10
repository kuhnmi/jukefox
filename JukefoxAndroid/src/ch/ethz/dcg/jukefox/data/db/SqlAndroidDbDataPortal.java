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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.AndroidLanguageHelper;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.LockHelper.Lock;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class SqlAndroidDbDataPortal extends SqlDbDataPortal<AndroidContentValues> {

	private final static String TAG = SqlAndroidDbDataPortal.class.getSimpleName();
	private JukefoxApplication application;

	private class AndroidSqlDbConnection implements ISqlDbConnection {

		private SQLiteDatabase db = null;

		@Override
		public boolean open() {
			if (isOpen()) {
				return true;
			}

			try {
				DbOpenHelper androidDbOpenHelper = new DbOpenHelper(JukefoxApplication.getAppContext(),
						AndroidConstants.DB_NAME, null, Constants.DB_VERSION);
				Log.v(TAG, "open() called");

				db = androidDbOpenHelper.getWritableDatabase();
				Log.v(TAG, "open() call returned");

				// now we have to catch up the onCreate() or onUpdate() functions
				if (androidDbOpenHelper.wasOnCreate()) {
					onUpgrade(0);
				} else if (androidDbOpenHelper.wasOnUpdate()) {
					int oldVersion = androidDbOpenHelper.getOldVersion();
					onUpgrade(oldVersion);
				}

				return true;
			} catch (Exception e) {
				Log.w(TAG, e);
				return false;
			}
		}

		@Override
		public boolean isOpen() {
			return (db != null) && db.isOpen();
		}

		@Override
		public void close() {
			if (!isOpen()) {
				return;
			}

			Lock lock = lockX();
			try {
				db.close();
				db = null;
			} catch (Exception e) {
				Log.w(TAG, e);
			} finally {
				lock.release();
			}
		}

		protected SQLiteDatabase getDatabase() {
			return db;
		}
	}

	// ----------------------------------------------------------------------------------------
	// CONSTRUCTORS (only first will be needed in Android)
	// ----------------------------------------------------------------------------------------

	public SqlAndroidDbDataPortal(DirectoryManager directoryManager, JukefoxApplication application) {
		super(directoryManager, new AndroidLanguageHelper());
		this.application = application;
	}

	public SqlAndroidDbDataPortal(DirectoryManager directoryManager, JukefoxApplication application, String dbUrl) {
		super(directoryManager, new AndroidLanguageHelper(), dbUrl);
		this.application = application;
	}

	public SqlAndroidDbDataPortal(DirectoryManager directoryManager, JukefoxApplication application,
			String dbUrl, String user, String password) {
		super(directoryManager, new AndroidLanguageHelper(), dbUrl, user, password);
		this.application = application;
	}

	@Override
	protected void init() {
	}

	@Override
	protected AndroidContentValues createContentValues() {
		return new AndroidContentValues();
	}

	@Override
	protected ISqlDbConnection createDbConnection() {
		return new AndroidSqlDbConnection();
	}

	@Override
	public AndroidSqlDbConnection getConnection() {
		return (AndroidSqlDbConnection) super.getConnection();
	}

	private SQLiteDatabase getDatabase() {
		return getConnection().getDatabase();
	}

	@Override
	protected void dropRegularTables() {
		List<String> statements = new LinkedList<String>();
		ICursor cur = null;
		try {
			cur = execSelect("SELECT type, name " +
					"FROM SQLITE_MASTER " +
					"WHERE ((type = 'table') OR (type = 'view')) " +
					"  AND (name != 'android_metadata') " +
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
				Log.v(TAG, "dropRegularTables: " + statement);
				execSQL(statement);
			}
			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	@Override
	public boolean deleteDatabase() {
		Lock lock = lockX();
		try {
			if (getConnection() != null) {
				close();
			}

			boolean deleted = application.deleteDatabase(AndroidConstants.DB_NAME);
			return deleted;
		} finally {
			lock.release();
		}
	}

	@Override
	public ICursor execSelect(String sql, String[] selectionArgs) {
		final Lock lock = lockS();
		try {
			final Cursor cursor = getDatabase().rawQuery(sql, selectionArgs);
			handleCursorLock(cursor, lock);
			return new AndroidCursor(cursor);
		} catch (SQLException e) {
			lock.release();
			throw new UncheckedSqlException(e);
		}
	}

	/**
	 * Adds an observer to the cursor, that keeps the given lock open until the cursor gets closed.
	 * 
	 * @param cursor
	 *            The cursor
	 * @param lock
	 *            The to be managed lock
	 */
	private void handleCursorLock(final Cursor cursor, final Lock lock) {
		// Listen for a closed event of the cursor
		cursor.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onInvalidated() {
				super.onInvalidated();
				if (cursor.isClosed()) {
					lock.release();
				}
			}
		});

	}

	@Override
	public long insertOrThrow(String table, AndroidContentValues values) {
		Lock lock = lockX();
		try {
			return getDatabase().insertOrThrow(table, null, values.getContentValues());
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	@Override
	protected void insertBatch(String table, BatchContentValues batchContentValues, boolean ignoreConflicts)
			throws UncheckedSqlException {
		beginTransaction();
		try {
			for (AndroidContentValues cv : batchContentValues.getContentValues()) {
				if (ignoreConflicts) {
					getDatabase().insertWithOnConflict(table, null, cv.getContentValues(),
							SQLiteDatabase.CONFLICT_IGNORE);
				} else {
					insertOrThrow(table, cv); // TODO create method insertOrThrowNoLock to not acquire a lock in every call here; maybe use insert blocks with multiple inserts in one call  
				}
			}
			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	};

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
		try {
			AndroidSqlDbConnection androidConnection = (AndroidSqlDbConnection) connection;
			androidConnection.getDatabase().execSQL(sql);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	@Override
	public int update(String table, AndroidContentValues values, String whereClause, String[] whereArgs) {
		Lock lock = lockX();
		try {
			return getDatabase().update(table, values.getContentValues(), whereClause, whereArgs);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	@Override
	protected void updateBatch(String table, BatchContentValues batchContentValues,
			BatchContentValues whereClauseContentValues)
			throws UncheckedSqlException {

		if (whereClauseContentValues.getContentValues().size() == 0) {
			return;
		}

		beginTransaction();
		try {
			String whereClause = getWhereCondition(whereClauseContentValues.getContentValues().get(0));
			//			Log.v(TAG, whereClause);
			for (Iterator<AndroidContentValues> itUp = batchContentValues.getContentValues().iterator(), itWhere = whereClauseContentValues
					.getContentValues().iterator(); itUp.hasNext();) {
				AndroidContentValues cv = itUp.next();
				AndroidContentValues cvWhere = itWhere.next();
				String[] whereArgs = getWhereArgs(cvWhere);
				update(table, cv, whereClause, whereArgs);
			}
			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	private String[] getWhereArgs(AndroidContentValues cvWhere) {
		Set<Entry<String, Object>> entrySet = cvWhere.getEntrySet();
		String[] a = new String[entrySet.size()];
		int counter = 0;
		for (Entry<String, Object> entry : entrySet) {
			a[counter] = DbUtils.formatQueryValue(entry.getValue());
			counter++;
		}
		return a;
	}

	@Override
	public int delete(String table, String whereClause, String[] whereArgs) {
		Lock lock = lockX();
		try {
			return getDatabase().delete(table, whereClause, whereArgs);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} finally {
			lock.release();
		}
	}

	/**
	 * Find all songs of which the title contains a certain term This method is used by Android to create a list which
	 * takes a Cursor as a listadapter
	 * 
	 * @param searchTerm
	 *            the term to search for in song titles
	 * @param maxResults
	 *            the maximal number of results that should be returned
	 * @return
	 */
	public Cursor findTitleBySearchStringCursor(String searchTerm,
			int maxResults) {
		String escapedSearchTerm = DbUtils.escapeString(searchTerm);

		String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
				"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +
				"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
				"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
				"WHERE (s." + TblSongs.NAME + " LIKE '% " + escapedSearchTerm + "%' " +
				"OR s." + TblSongs.NAME + " LIKE '" + escapedSearchTerm + "%' " +
				"OR alb." + TblAlbums.ALBUM_NAME + " LIKE '% " + escapedSearchTerm + "%' " +
				"OR alb." + TblAlbums.ALBUM_NAME + " LIKE '" + escapedSearchTerm + "%' " +
				"OR a." + TblArtists.NAME + " LIKE '% " + escapedSearchTerm + "%' " +
				"OR a." + TblArtists.NAME + " LIKE '" + escapedSearchTerm + "%') " +
				"LIMIT ?";

		final Lock lock = lockS();
		try {
			final Cursor cursor = getDatabase().rawQuery(sql, new String[] { "" + maxResults });
			handleCursorLock(cursor, lock);
			return cursor;
		} catch (SQLException e) {
			lock.release();
			throw new UncheckedSqlException(e);
		}
	}

}