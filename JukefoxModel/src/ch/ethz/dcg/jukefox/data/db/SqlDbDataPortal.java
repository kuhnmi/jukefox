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
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.cache.PreloadedSongInfo;
import ch.ethz.dcg.jukefox.data.context.AbstractContextResult;
import ch.ethz.dcg.jukefox.data.db.LockHelper.Lock;
import ch.ethz.dcg.jukefox.data.db.LockHelper.LockType;
import ch.ethz.dcg.jukefox.data.db.TransactionHelper.TransactionType;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.AbstractAlbumCoverFetcherThread.AlbumFetcherResult;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.SongStatus;
import ch.ethz.dcg.jukefox.model.collection.statistics.CollectionProperties;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap;
import ch.ethz.dcg.jukefox.model.libraryimport.GenreSongMap.GenreSongEntry;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.WebDataSong;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLogEntry;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLogSendEntity;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.NextSongCalculationThread;

/**
 * Abstract for all SQLDatabase implementations
 */
public abstract class SqlDbDataPortal<ContentValues extends IContentValues> implements IDbDataPortal {

	private final static String TAG = SqlDbDataPortal.class.getSimpleName();

	protected final DirectoryManager directoryManager;
	protected final AbstractLanguageHelper languageHelper;

	protected final String DB_URL;
	protected final String DB_USERNAME;
	protected final String DB_PASSWORD;

	private final ISqlDbConnection defaultConnection;
	private final ISqlDbConnection transactionConnection;

	private final LockHelper lockHelper;
	private final TransactionHelper transactionHelper;

	private final IDbLogHelper logDbHelper;
	private final IDbStatisticsHelper statisticsDbHelper;

	protected final static String NAMESPACE_COLLECTION_PROPERTIES = "statistics.collection_properties";
	protected final static String CP_AVG_SONG_DISTANCE = "average_song_distance";
	protected final static String CP_SONG_DISTANCE_STD_DEVIATION = "song_distance_standard_deviation";

	/**
	 * This class maintains a set of ContentValues suited to be inserted in a batch operation at once. All ContentValues
	 * within an instance have to have exactly the same fields set. The list of fields is given in the constructor and
	 * then the put-methods of the instance have to be used to set the value of each field given in the constructor.
	 * Then calling {@link #saveContentValues()} commits the current values to the permanent list.
	 * 
	 * @see SqlDbDataPortal#updateBatch(String, BatchContentValues, BatchContentValues)
	 * @see SqlDbDataPortal#insertBatch(String, BatchContentValues, boolean)
	 * 
	 * @author langnert
	 */
	protected class BatchContentValues {

		private Set<String> fieldNames;
		private List<ContentValues> contentValues;
		private ContentValues currentContentValues;

		public BatchContentValues(String[] fieldNames) {
			this(new HashSet<String>(Arrays.asList(fieldNames)));
		}

		public BatchContentValues(Set<String> fieldNames) {
			this.fieldNames = fieldNames;
			this.contentValues = new LinkedList<ContentValues>();
			this.currentContentValues = createContentValues();
		}

		/**
		 * Validates whether the all fields of this instance have been filled with values and then saves the current
		 * values permanently.
		 */
		public void saveContentValues() {
			if (currentContentValues.size() != fieldNames.size()) {
				throw new IllegalStateException(
						"Size of the ContentValues to be saved does not match field count.");
			}

			// validate all keys
			for (ContentValue cv : currentContentValues) {
				if (!fieldNames.contains(cv.getKey())) {
					throw new IllegalArgumentException(
							"The field " + cv.getKey() + " is not valid for this BatchContentValues.");
				}
			}

			// validation ok, so add it
			contentValues.add(this.currentContentValues);
			this.currentContentValues = createContentValues();
		}

		public void put(String key, String value) {
			currentContentValues.put(key, value);
		}

		public void put(String key, Float value) {
			currentContentValues.put(key, value);
		}

		public void put(String key, Integer value) {
			currentContentValues.put(key, value);
		}

		public void put(String key, Double value) {
			currentContentValues.put(key, value);
		}

		public void put(String key, Boolean value) {
			currentContentValues.put(key, value);
		}

		public void put(String key, Long value) {
			currentContentValues.put(key, value);
		}

		public void addContentValues(ContentValues cvs) {
			this.currentContentValues = cvs;
			saveContentValues();
		}

		public List<ContentValues> getContentValues() {
			return contentValues;
		}

		@Override
		public String toString() {
			return getContentValues().toString();
		}

		public Set<String> getFieldNames() {
			return new HashSet<String>(fieldNames);
		}

		public int size() {
			return getContentValues().size();
		}
	}

	/**
	 * Just a helper to represent db connections.
	 */
	public interface ISqlDbConnection {

		/**
		 * @see #open()
		 */
		public boolean open();

		/**
		 * @see IDbDataPortal#isOpen()
		 */
		public boolean isOpen();

		/**
		 * @see IDbDataPortal#close()
		 */
		public void close();
	}

	// ----------------------------------------------------------------------------------------
	// CONSTRUCTORS
	// ----------------------------------------------------------------------------------------

	public SqlDbDataPortal(DirectoryManager directoryManager, AbstractLanguageHelper languageHelper) {
		this(directoryManager, languageHelper, directoryManager.getDataBaseConnectionString(),
				null, null);
	}

	public SqlDbDataPortal(DirectoryManager directoryManager, AbstractLanguageHelper languageHelper, String dbUrl) {
		this(directoryManager, languageHelper, dbUrl, null, null);
	}

	public SqlDbDataPortal(DirectoryManager directoryManager, AbstractLanguageHelper languageHelper, String dbUrl,
			String user, String password) {
		this.languageHelper = languageHelper;
		this.directoryManager = directoryManager;

		lockHelper = new LockHelper(this);
		transactionHelper = new TransactionHelper(this);

		logDbHelper = createLogDbHelper();
		statisticsDbHelper = createStatisticsDbHelper();

		DB_URL = dbUrl;
		DB_USERNAME = user;
		DB_PASSWORD = password;

		init();

		defaultConnection = createDbConnection();
		transactionConnection = createDbConnection();

		open();
	}

	/**
	 * Called before the database is opened the first time. Initialize your environment in here, since the database will
	 * typically be opened in the constructor (and therefore before your initialization phase in yours).
	 */
	protected abstract void init();

	/**
	 * Returns a new {@link IDbLogHelper}-instance. If you want to extend {@link DbLogHelper} then overwrite this method
	 * and return the extended class instance.
	 * 
	 * @return The instance
	 */
	protected IDbLogHelper createLogDbHelper() {
		return new DbLogHelper<ContentValues>(this);
	}

	/**
	 * Returns a new {@link IDbStatisticsHelper}-instance. If you want to extend {@link DbStatisticsHelper} then
	 * overwrite this method and return the extended class instance.
	 * 
	 * @return The instance
	 */
	protected IDbStatisticsHelper createStatisticsDbHelper() {
		return new DbStatisticsHelper<ContentValues>(this);
	}

	@Override
	public final IDbLogHelper getLogHelper() {
		return logDbHelper;
	}

	@Override
	public final IDbStatisticsHelper getStatisticsHelper() {
		return statisticsDbHelper;
	}

	// ----------------------------------------------------------------------------------------
	// LOCK HELPER
	// ----------------------------------------------------------------------------------------

	/**
	 * @see #lockS(ISqlDbConnection)
	 */
	public final Lock lockS() {
		return lockS(getConnection());
	}

	/**
	 * Acquires a shared lock.
	 * 
	 * @see LockHelper
	 * @see LockHelper#lock(LockType, ISqlDbConnection)
	 */
	public final Lock lockS(ISqlDbConnection connection) {
		return lockHelper.lock(LockType.SHARED, connection);
	}

	/**
	 * @see #lockR(ISqlDbConnection)
	 */
	public final Lock lockR() {
		return lockR(getConnection());
	}

	/**
	 * Acquires a reserved lock.
	 * 
	 * @see LockHelper
	 * @see LockHelper#lock(LockType, ISqlDbConnection)
	 */
	public final Lock lockR(ISqlDbConnection connection) {
		return lockHelper.lock(LockType.RESERVED, connection);
	}

	/**
	 * @see #lockX(ISqlDbConnection)
	 */
	public final Lock lockX() {
		return lockX(getConnection());
	}

	/**
	 * Acquires a exclusive lock.
	 * 
	 * @see LockHelper
	 * @see LockHelper#lock(LockType, ISqlDbConnection)
	 */
	public final Lock lockX(ISqlDbConnection connection) {
		return lockHelper.lock(LockType.EXCLUSIVE, connection);
	}

	// ----------------------------------------------------------------------------------------
	// TRANSACTION HELPER
	// ----------------------------------------------------------------------------------------

	@Override
	public final void beginTransaction() {
		transactionHelper.beginTransaction();
	}

	@Override
	public final void beginExclusiveTransaction() {
		transactionHelper.beginExclusiveTransaction();
	}

	@Override
	public final boolean inTransaction() {
		return transactionHelper.inTransaction();
	}

	/**
	 * @see TransactionHelper#getTransactionType()
	 */
	public final TransactionType getTransactionType() {
		return transactionHelper.getTransactionType();
	}

	@Override
	public final void setTransactionSuccessful() {
		transactionHelper.setTransactionSuccessful();
	}

	@Override
	public final void endTransaction() {
		transactionHelper.endTransaction();
	}

	// ----------------------------------------------------------------------------------------
	// CONNECTION
	// ----------------------------------------------------------------------------------------

	protected abstract ISqlDbConnection createDbConnection();

	/**
	 * Returns the appropriate connection. If the current thread is in a transaction, we return the transaction
	 * connection. Otherwise the default connection gets returned.
	 * 
	 * @return The connection to the database
	 */
	public ISqlDbConnection getConnection() {
		if (inTransaction()) {
			return getTransactionConnection();
		} else {
			return getDefaultConnection();
		}
	}

	public ISqlDbConnection getDefaultConnection() {
		return defaultConnection;
	}

	public ISqlDbConnection getTransactionConnection() {
		return transactionConnection;
	}

	/**
	 * Opens the connection to the database.
	 * 
	 * @return True, if successful.
	 */
	protected final boolean open() {
		Log.v(TAG, "defaultConnection: " + defaultConnection + ", transactionConnection: " + transactionConnection);
		if (!transactionConnection.open()) {
			return false;
		}
		if (!defaultConnection.open()) {
			transactionConnection.close();
			return false;
		}
		return true;
	}

	@Override
	public final boolean isOpen() {
		boolean isOpen = true;
		isOpen &= (defaultConnection != null) && defaultConnection.isOpen();
		isOpen &= (transactionConnection != null) && transactionConnection.isOpen();

		return isOpen;
	}

	@Override
	public final void close() {
		Lock lock = lockX();
		try {
			defaultConnection.close();
			transactionConnection.close();
		} finally {
			lock.release();
		}
	}

	// ----------------------------------------------------------------------------------------
	// ABSTRACTS
	// ----------------------------------------------------------------------------------------

	protected abstract ContentValues createContentValues();

	/**
	 * Removes all tables which are created in {@link SqlDbDataPortal#onCreate()}. However, backup tables will survive
	 * this call.
	 */
	protected abstract void dropRegularTables();

	/**
	 * Abstract method for all Selects.
	 * 
	 * @param sql
	 *            the SQL query
	 * @param selectionArgs
	 *            You may include ?s in where clause in the query, which will be replaced by the values from
	 *            selectionArgs. The values will be bound as Strings.
	 * @throws UncheckedSqlException
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public abstract ICursor execSelect(String sql, String[] selectionArgs) throws UncheckedSqlException;

	/**
	 * Abstract method for inserting a row into the database.
	 * 
	 * @param table
	 *            the table to insert the row into
	 * @param values
	 *            this map contains the initial column values for the row. The keys should be the column names and the
	 *            values the column values
	 * @throws UncheckedSqlException
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public abstract long insertOrThrow(String table, ContentValues values) throws UncheckedSqlException;

	/**
	 * Inserts the rows corresponding to the ContentValues in the given {@link BatchContentValues} as fast as possible.
	 * 
	 * @param table
	 *            The table to insert the rows.
	 * @param batchContentValues
	 *            Contains the ContentValues that should be inserted in a batch as fast as possible.
	 * @param ignoreConflicts
	 *            Indicates whether potential conflicts while inserting should be silently ignored.
	 * 
	 * @throws UncheckedSqlException
	 */
	protected abstract void insertBatch(String table, BatchContentValues batchContentValues, boolean ignoreConflicts)
			throws UncheckedSqlException;

	/**
	 * Abstract for all Updates
	 * 
	 * @param table
	 *            The table to insert the row into
	 * @param values
	 *            this map contains the initial column values for the row. The keys should be the column names and the
	 *            values the column values
	 * @param whereClause
	 *            the optional WHERE clause to apply when updating. Passing null will update all rows.
	 * @throws UncheckedSqlException
	 * @return the number of rows affected
	 */
	public abstract int update(String table, ContentValues values, String whereClause, String[] whereArgs)
			throws UncheckedSqlException;

	/**
	 * Updates the rows corresponding to the given content values in a batch as fast as possible.
	 * 
	 * @param table
	 *            The table which to update.
	 * @param batchContentValues
	 *            Contains the key-value-pairs to be updated.
	 * @param whereClauseContentValues
	 *            Contains the key-value-pairs that restrict the elements to be updated. There is a one-to-one
	 *            correspondence between the entries in <code>batchContentValues</code> and the entries in
	 *            <code>whereClauseContentValues</code>, i.e. each entry in the latter specify which entry should be
	 *            updated with the values in the former.
	 * @throws UncheckedSqlException
	 */
	protected abstract void updateBatch(String table, BatchContentValues batchContentValues,
			BatchContentValues whereClauseContentValues)
			throws UncheckedSqlException;

	protected String getWhereCondition(ContentValues cvs) {
		StringBuilder sql = new StringBuilder();
		for (ContentValue cv : cvs) {
			sql.append(cv.getKey()).append(" =? AND ");
		}

		sql.delete(sql.length() - 5, sql.length());

		return sql.toString();
	}

	/**
	 * Abstract for all Deletes
	 * 
	 * @param table
	 *            the table to delete from
	 * @param whereClause
	 *            the optional WHERE clause to apply when deleting. Passing null will delete all rows.
	 * @throws UncheckedSqlException
	 * @return the number of rows affected
	 */
	public abstract int delete(String table, String whereClause, String[] whereArgs) throws UncheckedSqlException;

	/**
	 * Abstract for all Query e.g Deletes, Updates
	 */
	public abstract void execSQL(String sql) throws UncheckedSqlException;

	/**
	 * Abstract for some rare queries, which explicitely do not want a lock to be created. (i.e. transaction creation)
	 * 
	 * @param connection
	 *            On which connection we should operate
	 * @see #execSQL(String)
	 */
	public abstract void execSQLNoLock(String sql, ISqlDbConnection connection) throws UncheckedSqlException;

	// ----------------------------------------------------------------------------------------
	// IMPLEMENTATIONS
	// ----------------------------------------------------------------------------------------

	private ArtistIdGenerator artistIdGenerator;

	protected ArtistIdGenerator getArtistIdGenerator() {
		if (artistIdGenerator == null) {
			artistIdGenerator = new ArtistIdGenerator(this);
		}

		return artistIdGenerator;
	}

	@Override
	public void resetDatabase() {
		// execSQL("DELETE FROM " + TblSongs.TBL_NAME);
		// execSQL("DELETE FROM " + TblArtists.TBL_NAME + " WHERE "
		// + TblArtists.IS_FAMOUS_ARTIST + " = 0");
		// execSQL("DELETE FROM " + TblAlbums.TBL_NAME);
		// execSQL("DELETE FROM " + TblGenres.TABLE_NAME);
		// // execSQL("DELETE FROM " + TblTags.TBL_NAME);
		// execSQL("DELETE FROM " + TblArtistSets.TBL_NAME);
		// execSQL("DELETE FROM " + TblSongGenres.TABLE_NAME);
		// execSQL("DELETE FROM " + TblSongCoords.TBL_NAME);
		// execSQL("DELETE FROM " + TblArtistCoords.TBL_NAME + " WHERE "
		// + TblArtistCoords.ARTIST_ID + " NOT IN (SELECT "
		// + TblArtists.ARTIST_ID + " FROM " + TblArtists.TBL_NAME
		// + " WHERE " + TblArtists.IS_FAMOUS_ARTIST + " = 0" + ")");
		// execSQL("DELETE FROM " + TblPlayLog.TBL_NAME);
		// boolean deleted = application.deleteDatabase(Constants.DB_NAME);
		// FIXME do we need to clean this tables?

		backupDurableData();

		onUpgrade(0); // Recreate the database from scratch
		Log.v(TAG, "clearDb: database cleared");
	}

	/**
	 * Moves data which should survive a database reset into backup tables.<br/>
	 * Please note, that the backup table names must begin with "backup_".
	 */
	private void backupDurableData() {
		statisticsDbHelper.backupStatisticsData();
		statisticsDbHelper.backupRatingData();
	}

	// ----------------------------------------------------------------------------------------
	// SELECTS
	// ----------------------------------------------------------------------------------------

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getAllSongs() {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID;

			cur = execSelect(sql, new String[] {});
			while (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
				songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album, cur
						.getInt(6)));
			}

			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public BaseSong<BaseArtist, BaseAlbum> getBaseSongById(int randomId) throws DataUnavailableException {
		Set<Integer> ids = new HashSet<Integer>(1);
		ids.add(randomId);
		return batchGetBaseSongByIds(ids).get(0);
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> batchGetBaseSongByIds(Set<Integer> randomIds)
			throws DataUnavailableException {
		ICursor cur = null;
		try {
			StringBuffer ids = new StringBuffer(randomIds.size() * 4); // I assume an average id length of 3 chars plus the comma
			for (int id : randomIds) {
				ids.append(id).append(",");
			}
			ids.deleteCharAt(ids.length() - 1); // remove last comma

			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.SONG_ID + " IN (%s)";

			cur = execSelect(String.format(sql, ids.toString()), null);

			List<BaseSong<BaseArtist, BaseAlbum>> ret = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(randomIds.size());
			while (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
				ret.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
						cur.getInt(6)));
			}

			if (ret.size() != randomIds.size()) {
				throw new DataUnavailableException("Not all ids found!");
			} else {
				return ret;
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public HashMap<String, ImportSong> getAllSongsForImport() throws DataUnavailableException {

		HashMap<String, ImportSong> pathToSongMap = new HashMap<String, ImportSong>();

		ICursor cur = null;
		try {
			String sql = "SELECT " + "s." + TblSongs.SONG_ID + ", " + "s." + TblSongs.NAME + ", ar." + TblArtists.NAME + ", " +
					"  al." + TblAlbums.ALBUM_NAME + ", al." + TblAlbums.ARTIST_SET_ID + ", s." + TblSongs.DATA + ", " +
					"  s." + TblSongs.DURATION + ", s." + TblSongs.TRACK_NR + ", s." + TblSongs.IMPORT_TIMESTAMP + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"  JOIN " + TblArtists.TBL_NAME + " AS ar ON s." + TblSongs.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " " +
					"  JOIN " + TblAlbums.TBL_NAME + " AS al ON s." + TblSongs.ALBUM_ID + " = al." + TblAlbums.ALBUM_ID;

			// Log.v(TAG, "getAllSongsForImport: sql: " + sql);
			cur = execSelect(sql, null);
			if (cur == null) {
				throw new DataUnavailableException("cur == null");
			}
			while (cur.moveToNext()) {
				ImportSong importSong = getImportSongFromCursor(cur);
				pathToSongMap.put(importSong.getPath(), importSong);
			}
			return pathToSongMap;
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * Creates an {@link ImportSong} instance out of the given cursor.<br/>
	 * Ensurethe first columns are {@link TblSongs#SONG_ID}, {@link TblSongs#NAME}, {@link TblArtists#NAME},
	 * {@link TblAlbums#ALBUM_NAME}, {@link TblAlbums#ARTIST_SET_ID}, {@link TblSongs#DATA}, {@link TblSongs#DURATION},
	 * {@link TblSongs#TRACK_NR}, {@link TblSongs#IMPORT_TIMESTAMP}.
	 * 
	 * @param cur
	 * @return
	 */
	private ImportSong getImportSongFromCursor(ICursor cur) {
		Integer jukefoxId = cur.getInt(0);
		String name = cur.getString(1);
		String artist = cur.getString(2);
		String albumName = cur.getString(3);
		int albumArtistSetId = cur.getInt(4);
		String path = cur.getString(5);
		int duration = cur.getInt(6);
		int track = cur.getInt(7);
		Date importDate = new Date(cur.getInt(8));
		ContentProviderId cpId = null;

		if (Utils.isNullOrEmpty(name, true)) {
			name = languageHelper.getUnknownTitleAlias();
		}
		if (Utils.isNullOrEmpty(albumName, true)) {
			albumName = languageHelper.getUnknownAlbumAlias();
		}
		if (Utils.isNullOrEmpty(artist, true)) {
			artist = languageHelper.getUnknownArtistAlias();
		}

		ImportAlbum importAlbum = getImportAlbum(albumName, albumArtistSetId);
		return new ImportSong(name, importAlbum, artist, path, duration, track, cpId, jukefoxId, importDate);
	}

	private ImportAlbum getImportAlbum(String albumName, int albumArtistSetId) {
		ICursor cur = null;
		try {
			String sql = "SELECT a." + TblArtists.NAME + " FROM " + TblArtistSets.TBL_NAME + " aset JOIN "
					+ TblArtists.TBL_NAME + " a ON aset." + TblArtistSets.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " WHERE aset." + TblArtistSets.ARTIST_SET_ID + " = ?";

			// Log.v(TAG, "getImportAlbum: sql: " + sql);

			cur = execSelect(sql, new String[] { "" + albumArtistSetId });

			ImportAlbum album = new ImportAlbum(albumName);
			while (cur.moveToNext()) {
				album.addArtistName(cur.getString(0));
			}
			return album;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getSongListForAlbum(BaseAlbum album) {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"WHERE s." + TblSongs.ALBUM_ID + "=? " +
					"ORDER BY s." + TblSongs.TRACK_NR + " ASC, " + TblSongs.DATA + " ASC";

			cur = execSelect(sql, new String[] { "" + album.getId() });

			while (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album, cur
						.getInt(4)));
			}
			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongListForPaths(List<String> paths) {
		List<PlaylistSong<BaseArtist, BaseAlbum>> songs = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		for (String path : paths) {
			PlaylistSong<BaseArtist, BaseAlbum> song;
			try {
				song = getSongForPath(path);
			} catch (DataUnavailableException e) {
				song = null;
			}
			if (song != null) {
				songs.add(song);
			}
		}
		return songs;
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getSongListForIds(Vector<KdTreePoint<Integer>> points) {
		ArrayList<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.SONG_ID + "=?";

			for (KdTreePoint<Integer> point : points) {
				try {
					cur = execSelect(sql, new String[] { "" + point.getID() });
					if (cur == null) {
						Log.w(TAG, "DB cursor is null!");
						return songs;
					}
					if (cur.moveToNext()) {
						BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
						BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
						songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
								cur.getInt(6)));
					}
				} finally {
					if (cur != null) {
						cur.close();
						cur = null;
					}
				}
			}

			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>> getSongListForIds2(
			Vector<KdTreePoint<Integer>> points) {
		ArrayList<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>> songs = new ArrayList<Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.SONG_ID + "=?";

			for (KdTreePoint<Integer> point : points) {
				try {
					cur = execSelect(sql, new String[] { "" + point.getID() });
					if (cur == null) {
						Log.w(TAG, "DB cursor is null!");
						return songs;
					}
					if (cur.moveToNext()) {
						BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
						BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
						songs.add(new Pair<BaseSong<BaseArtist, BaseAlbum>, KdTreePoint<Integer>>(
								new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album, cur
										.getInt(6)),
								point));
					}
				} finally {
					if (cur != null) {
						cur.close();
						cur = null;
					}
				}
			}

			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getSongForPath(String path) throws DataUnavailableException {
		return getSongForPath(path, true);
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getSongForPath(String path, boolean caseSensitive)
			throws DataUnavailableException {
		String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
				"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
				"FROM " + TblSongs.TBL_NAME + " AS s " +
				"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
				"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
				"WHERE s." + TblSongs.DATA + "=?";
		ICursor cur = null;
		try {
			cur = execSelect(sql, new String[] { path });
			if (cur == null) {
				Log.w(TAG, "getSongForPath: DB cursor is null!");
				throw new DataUnavailableException();
			}
			if (!cur.moveToNext()) {
				throw new DataUnavailableException();
			}
			BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
			BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
			return new PlaylistSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
					SongSource.MANUALLY_SELECTED, cur.getInt(6));
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForArtist(BaseArtist artist) {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ALBUM_ID + ", " +
					"alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.ARTIST_ID + "=? " +
					"ORDER BY s." + TblSongs.ALBUM_ID + ",s." + TblSongs.TRACK_NR + " ASC";

			cur = execSelect(sql, new String[] { "" + artist.getId() });
			while (cur.moveToNext()) {
				BaseAlbum album = new BaseAlbum(cur.getInt(2), cur.getString(3));
				songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
						cur.getInt(4)));
			}

			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> getSongsForGenre(Genre genre) {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON s." + TblSongs.SONG_ID + "=sg." + TblSongGenres.SONG_ID + " " +
					"WHERE sg." + TblSongGenres.GENRE_ID + "=?";

			cur = execSelect(sql, new String[] { "" + genre.getId() });
			while (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
				songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
						cur.getInt(6)));
			}

			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * returns songs that have one of the status specified in statuses or the according album has one of the statuses
	 * specified in albumStatuses
	 */
	@Override
	public List<WebDataSong> getWebDataSongsForStatus(SongStatus[] statuses, AlbumStatus[] albumStatuses) {
		List<WebDataSong> songs = new ArrayList<WebDataSong>();
		if (statuses == null || statuses.length == 0) {
			return songs;
		}
		ICursor cur = null;
		try {
			// TODO: also read meIds, coords, etc.?
			StringBuffer buffer = new StringBuffer();
			buffer.append("SELECT " + "s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.SONG_STATUS
					+ ", ar." + TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", al."
					+ TblAlbums.ALBUM_STATUS + " FROM " + TblSongs.TBL_NAME + " s JOIN " + TblArtists.TBL_NAME
					+ " ar ON s." + TblSongs.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " JOIN "
					+ TblAlbums.TBL_NAME + " al ON s." + TblSongs.ALBUM_ID + " = al." + TblAlbums.ALBUM_ID
					+ " WHERE s." + TblSongs.SONG_STATUS + " = ?");

			for (int i = 1; i < statuses.length; i++) {
				buffer.append(" OR s." + TblSongs.SONG_STATUS + " = ?");
			}

			buffer.append(" OR al." + TblAlbums.ALBUM_STATUS + "= ?");

			for (int i = 1; i < statuses.length; i++) {
				buffer.append(" OR al." + TblAlbums.ALBUM_STATUS + " = ?");
			}

			buffer.append(" ORDER BY s." + TblSongs.ALBUM_ID);

			String sql = buffer.toString();

			Log.v(TAG, "getWebDataSongsForStatus(SongStatus[] statuses, AlbumStatus[] albumStatuses) => sql: " + sql);

			String[] selectionArgs = new String[statuses.length + albumStatuses.length];
			for (int i = 0; i < statuses.length; i++) {
				selectionArgs[i] = Integer.toString(statuses[i].getValue());
			}
			for (int i = 0; i < albumStatuses.length; i++) {
				selectionArgs[statuses.length + i] = Integer.toString(albumStatuses[i].getValue());
			}
			cur = execSelect(sql, selectionArgs);
			if (cur == null) {
				// TODO again?
				throw new UncheckedSqlException("cur == null");
			}
			while (cur.moveToNext()) {
				int songId = cur.getInt(0);
				String title = cur.getString(1);
				SongStatus status = SongStatus.getStatusForValue(cur.getInt(2));
				int artistId = cur.getInt(3);
				String artistName = cur.getString(4);
				int albumId = cur.getInt(5);
				AlbumStatus albumStatus = AlbumStatus.getStatusFromValue(cur.getInt(6));
				// TODO: later also read artistStatus...
				CompleteArtist artist = new CompleteArtist(artistId, artistName, null, null, null);
				WebDataSong webDataSong = new WebDataSong(songId, title, null, null, null, status, artist, albumId,
						albumStatus);
				songs.add(webDataSong);
			}
			return songs;
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
			return new ArrayList<WebDataSong>();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseArtist> getAllArtists() {
		// Log.v(TAG, "begin getAllArtists()");
		List<BaseArtist> artists = new ArrayList<BaseArtist>();
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtists.ARTIST_ID + ", " + TblArtists.NAME + " FROM " + TblArtists.TBL_NAME
					+ " WHERE " + TblArtists.IS_IN_COLLECTION + " = ?";

			cur = execSelect(sql, new String[] { "1" });

			while (cur.moveToNext()) {
				artists.add(new BaseArtist(cur.getInt(0), cur.getString(1)));
			}
			return artists;
		} finally {
			if (cur != null) {
				cur.close();
			}
			// Log.v(TAG, "end getAllArtists()");
		}
	}

	@Override
	public CompleteArtist getCompleteArtist(BaseArtist baseArtist) throws DataUnavailableException {
		ICursor cur = null;
		CompleteArtist artist = null;
		float[] coords = new float[Constants.DIM];
		try {
			String sql = "SELECT a." + TblArtists.ME_ARTIST_ID + ", a." + TblArtists.ME_NAME + ", "
					+ DbUtils.getCoordString("ac." + TblArtistCoords.COORD_PREFIX) + " FROM " + TblArtists.TBL_NAME
					+ " AS a" + " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID
					+ " = ac." + TblArtistCoords.ME_ARTIST_ID + " WHERE a." + TblArtists.ARTIST_ID + "=?";

			cur = execSelect(sql, new String[] { "" + baseArtist.getId() });

			if (cur.moveToNext()) {
				int meId = cur.getInt(0);
				String meName = cur.getString(1);
				if (!cur.isNull(2)) {
					// artist coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(2 + i);
					}
				}
				artist = new CompleteArtist(baseArtist, meId, meName, coords);
				return artist;
			} else {
				throw new DataUnavailableException(); // TODO: change to more
				// meaningful exception
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseArtist> getArtistsForGenre(Genre genre) {
		Log.v(TAG, "Get Artist for " + genre.getName());
		List<BaseArtist> artists = new ArrayList<BaseArtist>();
		ICursor cur = null;
		try {
			String sql = "SELECT DISTINCT a." + TblArtists.ARTIST_ID + ", a." + TblArtists.NAME + " FROM "
					+ TblArtists.TBL_NAME + " AS a JOIN " + TblSongs.TBL_NAME + " AS s ON a." + TblArtists.ARTIST_ID
					+ "=s." + TblSongs.ARTIST_ID + " JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON s."
					+ TblSongs.SONG_ID + "=sg." + TblSongGenres.SONG_ID + " WHERE sg." + TblSongGenres.GENRE_ID + "=?";

			// Log.v(TAG, sql);

			cur = execSelect(sql, new String[] { "" + genre.getId() });

			while (cur.moveToNext()) {
				artists.add(new BaseArtist(cur.getInt(0), cur.getString(1)));
			}

			Log.v(TAG, "Number of artists: " + artists.size());
			return artists;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public CompleteAlbum getCompleteAlbum(BaseAlbum album) throws DataUnavailableException {
		return getCompleteAlbumById(album.getId());
	}

	@Override
	public CompleteAlbum getCompleteAlbumById(int albumId) throws DataUnavailableException {
		ICursor cur = null;
		List<BaseArtist> artists = null;
		try {
			String sql = "SELECT " + TblAlbums.ALBUM_ID + ", " + TblAlbums.ALBUM_NAME + ", " + TblAlbums.ARTIST_SET_ID
					+ ", " + TblAlbums.PCA_COORDS_X + ", " + TblAlbums.PCA_COORDS_Y + ", " + TblAlbums.COLOR + ", "
					+ TblAlbums.ALBUM_STATUS + " FROM " + TblAlbums.TBL_NAME + " WHERE " + TblAlbums.ALBUM_ID + "=?";

			cur = execSelect(sql, new String[] { "" + albumId });
			if (!cur.moveToNext()) {
				throw new DataUnavailableException(); // TODO: change to more
				// meaningful exception
			}
			String albumName = cur.getString(1);
			artists = getArtistsForArtistSetId(cur.getInt(2));
			float[] coordsPca2D = new float[] { cur.getFloat(3), cur.getFloat(4) };
			int color = cur.getInt(5);
			AlbumStatus status = AlbumStatus.getStatusFromValue(cur.getInt(6));
			BaseAlbum baseAlbum = new BaseAlbum(albumId, albumName);
			List<BaseSong<BaseArtist, BaseAlbum>> songs = getSongListForAlbum(baseAlbum);
			return new CompleteAlbum(albumId, albumName, coordsPca2D, color, artists, songs, status);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<ListAlbum> getAllAlbumsAsListAlbums() {
		// Log.v(TAG, "begin getAllAlbumsAsListAlbums()");
		ICursor cur = null;
		try {
			List<ListAlbum> albums = new ArrayList<ListAlbum>();
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " ORDER BY al." + TblAlbums.ALBUM_ID;

			cur = execSelect(sql, new String[] {});

			List<BaseArtist> artists = null;
			int lastAlbumId = -1;

			while (cur.moveToNext()) {
				if (cur.getInt(0) != lastAlbumId) {
					artists = new ArrayList<BaseArtist>();
					albums.add(new ListAlbum(cur.getInt(0), cur.getString(1), artists));
					lastAlbumId = cur.getInt(0);
				}
				artists.add(new BaseArtist(cur.getInt(2), cur.getString(3)));
			}
			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
			// Log.v(TAG, "end getAllAlbumsAsListAlbums()");
		}
	}

	@Override
	public List<BaseAlbum> getAllAlbumsWithoutAlbumArt() {
		List<BaseAlbum> albums = new ArrayList<BaseAlbum>();
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblAlbums.ALBUM_ID + ", " + TblAlbums.ALBUM_NAME + " FROM " + TblAlbums.TBL_NAME
					+ " WHERE " + TblAlbums.HIGH_RES_COVER_PATH + " IS NULL OR " + TblAlbums.LOW_RES_COVER_PATH
					+ " IS NULL";

			cur = execSelect(sql, new String[] {});

			while (cur.moveToNext()) {
				albums.add(new BaseAlbum(cur.getInt(0), cur.getString(1)));
			}
			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<ListAlbum> getAllAlbumsForArtist(BaseArtist artist, boolean includeCompilations) {
		ICursor cur = null;
		HashSet<Integer> albumIds = new HashSet<Integer>();
		try {
			List<ListAlbum> albums = new ArrayList<ListAlbum>();
			String sql = "SELECT DISTINCT al." + TblAlbums.ALBUM_ID + " FROM " + TblArtistSets.TBL_NAME + " aset "
					+ "JOIN " + TblAlbums.TBL_NAME + " al ON aset." + TblArtistSets.ARTIST_SET_ID + " = al."
					+ TblAlbums.ARTIST_SET_ID + " WHERE aset." + TblArtistSets.ARTIST_ID + "=" + artist.getId();

			// Log.v(TAG, sql);
			cur = execSelect(sql, new String[] {});
			if (cur == null) {
				Log.w(TAG, "DB cursor is null!");
				return albums;
			}
			while (cur.moveToNext()) {
				// Log.v(TAG, "getAlbumsForArtist() id: " + cur.getInt(0));
				try {
					int albumId = cur.getInt(0);
					albumIds.add(albumId);
					albums.add(getListAlbum(albumId));
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}

			if (includeCompilations) {
				if (cur != null) {
					cur.close();
				}
				String sql2 = "SELECT DISTINCT " + TblSongs.ALBUM_ID + " FROM " + TblSongs.TBL_NAME + " WHERE "
						+ TblSongs.ARTIST_ID + "=" + artist.getId();

				// Log.v(TAG, sql2);
				cur = execSelect(sql2, new String[] {});

				while (cur.moveToNext()) {
					// Log.v(TAG, "getAlbumsForArtist() id: " + cur.getInt(0));
					try {
						int albumId = cur.getInt(0);
						if (!albumIds.contains(albumId)) {
							albums.add(getListAlbum(cur.getInt(0)));
						}
					} catch (Exception e) {
						Log.w(TAG, e);
					}
				}
			}

			// if (includeCompilations) {
			// List compilationAlbums = getCompliationAlbumsWithArtist(artist);
			// }

			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	public ListAlbum getListAlbum(int albumId) throws DataUnavailableException {
		ICursor cur = null;
		List<BaseArtist> artists = null;
		try {
			String sql = "SELECT " + TblAlbums.ALBUM_NAME + ", " + TblAlbums.ARTIST_SET_ID + ", "
					+ TblAlbums.PCA_COORDS_X + ", " + TblAlbums.PCA_COORDS_Y + ", " + TblAlbums.COLOR + ", "
					+ TblAlbums.ALBUM_STATUS + " FROM " + TblAlbums.TBL_NAME + " WHERE " + TblAlbums.ALBUM_ID + "=?";

			cur = execSelect(sql, new String[] { "" + albumId });
			if (!cur.moveToNext()) {
				throw new DataUnavailableException(); // TODO: change to more
				// meaningful exception
			}
			String albumName = cur.getString(0);
			artists = getArtistsForArtistSetId(cur.getInt(1));
			return new ListAlbum(albumId, albumName, artists);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private List<BaseArtist> getArtistsForArtistSetId(int artistSetId) {
		List<BaseArtist> artists = new ArrayList<BaseArtist>();
		ICursor cur = null;
		try {
			String sql = "SELECT arts." + TblArtistSets.ARTIST_ID + ", a." + TblArtists.NAME + " FROM "
					+ TblArtistSets.TBL_NAME + " AS arts JOIN " + TblArtists.TBL_NAME + " AS a ON arts."
					+ TblArtistSets.ARTIST_ID + "=a." + TblArtists.ARTIST_ID + " WHERE arts."
					+ TblArtistSets.ARTIST_SET_ID + "=?";

			cur = execSelect(sql, new String[] { "" + artistSetId });

			while (cur.moveToNext()) {
				artists.add(new BaseArtist(cur.getInt(0), cur.getString(1)));
			}
			return artists;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<ListAlbum> getAlbumsForGenre(Genre genre) {
		Log.v(TAG, "Get Albums for " + genre.getName());
		ICursor cur = null;
		try {
			List<ListAlbum> albums = new ArrayList<ListAlbum>();
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " JOIN " + TblSongs.TBL_NAME
					+ " AS s ON al." + TblAlbums.ALBUM_ID + "=s." + TblSongs.ALBUM_ID + " JOIN "
					+ TblSongGenres.TABLE_NAME + " AS sg ON s." + TblSongs.SONG_ID + "=sg." + TblSongGenres.SONG_ID
					+ " WHERE sg." + TblSongGenres.GENRE_ID + "=? ORDER BY al." + TblAlbums.ALBUM_ID + " ASC";

			// Log.v(TAG, sql);

			cur = execSelect(sql, new String[] { "" + genre.getId() });

			List<BaseArtist> artists = null;
			int lastAlbumId = -1;

			while (cur.moveToNext()) {
				if (cur.getInt(0) != lastAlbumId) {
					artists = new ArrayList<BaseArtist>();
					albums.add(new ListAlbum(cur.getInt(0), cur.getString(1), artists));
					lastAlbumId = cur.getInt(0);
				}
				artists.add(new BaseArtist(cur.getInt(2), cur.getString(3)));
			}
			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public BaseSong<BaseArtist, BaseAlbum> getBaseSongByMusicExplorerId(int meId) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.ME_SONG_ID + "=?";

			cur = execSelect(sql, new String[] { Integer.toString(meId) });
			if (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
				return new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
						cur.getInt(6));
			}
			throw new DataUnavailableException();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public BaseSong<BaseArtist, BaseAlbum> getArbitrarySongInTimeRange(int profileId, long fromTimestamp,
			long toTimestamp) throws DataUnavailableException {
		ICursor cur = null;
		try {

			String sql = "SELECT " + TblPlayLog.ME_SONG_ID + " FROM " + TblPlayLog.TBL_NAME + " WHERE "
					+ TblPlayLog.SKIPPED + " = 0 AND " + TblPlayLog.ME_SONG_ID + " IS NOT NULL AND "
					+ TblPlayLog.TIMESTAMP + " > " + fromTimestamp + " AND " + TblPlayLog.TIMESTAMP + " < "
					+ toTimestamp + " AND " + TblPlayLog.PROFILE_ID + " = " + profileId;

			Log.v(TAG, "sql: " + sql);

			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				Log.v(TAG, "getArbitrarySongInTimeRange: No song found for time period " + new Date(fromTimestamp)
						+ " to " + new Date(toTimestamp));
				return null;
			}

			// Log.v(TAG, "Number of songs in period: " + cur.getCount());
			// getCount() can no longer be used because of JDBC

			do {
				try {
					int meId = cur.getInt(0);
					BaseSong<BaseArtist, BaseAlbum> song = getBaseSongByMusicExplorerId(meId);
					return song;
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}
			} while (cur.moveToNext());
			Log.v(TAG, "could not find a valid song... returning null!");
			return null;

		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public BaseSong<BaseArtist, BaseAlbum> getSongCloseToTimeRange(int profileId, long fromTimestamp, long toTimestamp,
			float toleranceRange, float toleranceGlobal) throws DataUnavailableException {
		PlayLogEntry before = getLastPlaylogEntryBefore(profileId, fromTimestamp, true, true);
		PlayLogEntry after = getFirstPlaylogEntryAfter(profileId, toTimestamp, true, true);
		if (before == null && after == null) {
			return null;
		}
		try {
			if (before == null) {
				return getBaseSongByMusicExplorerId(after.getMeId());
			}
			if (after == null) {
				return getBaseSongByMusicExplorerId(before.getMeId());
			}
			long beforeDiff = fromTimestamp - before.getUtcTime();
			long afterDiff = after.getUtcTime() - toTimestamp;

			if (beforeDiff < afterDiff) {
				if (!isInTolerance(fromTimestamp, toTimestamp, beforeDiff, toleranceRange, toleranceGlobal)) {
					Log.v(TAG, "beforeDiff outside tolerance: " + beforeDiff);
					return null;
				}
				return getBaseSongByMusicExplorerId(before.getMeId());
			}
			if (!isInTolerance(fromTimestamp, toTimestamp, afterDiff, toleranceRange, toleranceGlobal)) {
				Log.v(TAG, "afterDiff outside tolerance: " + afterDiff);
				return null;
			}
			return getBaseSongByMusicExplorerId(after.getMeId());
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		return null;
	}

	private PlayLogEntry getLastPlaylogEntryBefore(int profileId, long timestamp, boolean unskippedOnly,
			boolean withMeIdOnly) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblPlayLog.ME_SONG_ID + "," + TblPlayLog.TIMESTAMP + ", " + TblPlayLog.SKIPPED
					+ " FROM " + TblPlayLog.TBL_NAME + " WHERE " + TblPlayLog.TIMESTAMP + " < " + timestamp + " AND "
					+ TblPlayLog.PROFILE_ID + " = " + profileId;
			if (unskippedOnly) {
				sql += " AND " + TblPlayLog.SKIPPED + " = 0";
			}
			if (withMeIdOnly) {
				sql += " AND " + TblPlayLog.ME_SONG_ID + " IS NOT NULL";
				sql += " AND " + TblPlayLog.ME_SONG_ID + " <> 0";
			}
			sql += " ORDER BY " + TblPlayLog.TIMESTAMP + " DESC LIMIT 1";

			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				Log.v(TAG, "No song found before " + new Date(timestamp));
				return null;
			}
			PlayLogEntry pe = new PlayLogEntry();
			if (!cur.isNull(0) && cur.getInt(0) != 0) {
				pe.setMeId(cur.getInt(0));
			}
			pe.setUtcTime(cur.getLong(1));
			pe.setSkipped(cur.getInt(2) == 0 ? false : true);
			return pe;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private PlayLogEntry getFirstPlaylogEntryAfter(int profileId, long timestamp, boolean unskippedOnly,
			boolean withMeIdOnly) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblPlayLog.ME_SONG_ID + "," + TblPlayLog.TIMESTAMP + ", " + TblPlayLog.SKIPPED
					+ " FROM " + TblPlayLog.TBL_NAME + " WHERE " + TblPlayLog.TIMESTAMP + " > " + timestamp + " AND "
					+ TblPlayLog.PROFILE_ID + " = " + profileId;
			if (unskippedOnly) {
				sql += " AND " + TblPlayLog.SKIPPED + " = 0";
			}
			if (withMeIdOnly) {
				sql += " AND " + TblPlayLog.ME_SONG_ID + " IS NOT NULL";
				sql += " AND " + TblPlayLog.ME_SONG_ID + " <> 0";
			}
			sql += " ORDER BY " + TblPlayLog.TIMESTAMP + " ASC LIMIT 1";

			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				Log.v(TAG, "No song found before " + new Date(timestamp));
				return null;
			}
			PlayLogEntry pe = new PlayLogEntry();
			if (!cur.isNull(0) && cur.getInt(0) != 0) {
				pe.setMeId(cur.getInt(0));
			}
			pe.setUtcTime(cur.getLong(1));
			pe.setSkipped(cur.getInt(2) == 0 ? false : true);
			return pe;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private boolean isInTolerance(long fromTimestamp, long toTimestamp, long diff, float toleranceRange,
			float toleranceGlobal) {
		long range = toTimestamp - fromTimestamp;
		if (diff < Constants.ONE_DAY) {
			return true;
		}
		if ((float) diff / range < toleranceRange) {
			return true;
		}
		long now = System.currentTimeMillis();
		long mean = (toTimestamp + fromTimestamp) / 2;
		if ((float) diff / (now - mean) < toleranceGlobal) {
			return true;
		}
		return false;
	}

	@Override
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongsForTimeRange(int profileId, long fromTimestamp,
			long toTimestamp, int number) {

		Log.v(TAG, "getSongsForTimeRange: from: " + fromTimestamp + "(" + new Timestamp(fromTimestamp) + ")");
		Log.v(TAG, "getSongsForTimeRange: from: " + toTimestamp + "(" + new Timestamp(toTimestamp) + ")");

		ICursor cur = null;
		try {

			String sql = "SELECT " + TblPlayLog.ME_SONG_ID + " FROM " + TblPlayLog.TBL_NAME + " WHERE "
					+ TblPlayLog.SKIPPED + " = 0 AND " + TblPlayLog.ME_SONG_ID + " IS NOT NULL AND "
					+ TblPlayLog.TIMESTAMP + " > " + fromTimestamp + " AND " + TblPlayLog.TIMESTAMP + " < "
					+ toTimestamp + " AND " + TblPlayLog.PROFILE_ID + " = " + profileId + " LIMIT " + number;

			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				Log
						.v(TAG, "No songs found for time period " + new Date(fromTimestamp) + " to " + new Date(
								toTimestamp));
				return null;
			}

			// Log.v(TAG, "Number of songs in period: " + cur.getCount());
			int num = 0;

			List<PlaylistSong<BaseArtist, BaseAlbum>> songs = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
			do {
				num++;
				try {
					int meId = cur.getInt(0);
					Log.v(TAG, "try to get song with meId " + meId);
					songs.add(new PlaylistSong<BaseArtist, BaseAlbum>(getBaseSongByMusicExplorerId(meId),
							SongSource.TIME_BASED));
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}
			} while (cur.moveToNext());

			Log.v(TAG, "Number of songs in period: " + num);
			return songs;

		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<Genre> getAllGenres() {
		Log.v(TAG, "Fetching all genres");
		List<Genre> genres = new ArrayList<Genre>();
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblGenres.GENRE_ID + ", " + TblGenres.NAME + " FROM " + TblGenres.TBL_NAME;

			cur = execSelect(sql, new String[0]);

			while (cur.moveToNext()) {
				genres.add(new Genre(cur.getInt(0), cur.getString(1)));
			}

			return genres;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<Pair<Genre, Integer>> getGenresForArtist(BaseArtist artist) throws DataUnavailableException {
		List<Pair<Genre, Integer>> genres = new ArrayList<Pair<Genre, Integer>>();
		ICursor cur = null;
		try {
			String sql = "SELECT count(*), g." + TblGenres.GENRE_ID + ", g." + TblGenres.NAME + " FROM "
					+ TblSongs.TBL_NAME + " AS s JOIN " + TblSongGenres.TABLE_NAME + " AS sg ON sg."
					+ TblSongGenres.SONG_ID + "=s." + TblSongs.SONG_ID + " JOIN " + TblGenres.TBL_NAME + " AS g ON g."
					+ TblGenres.GENRE_ID + "=sg." + TblSongGenres.GENRE_ID + " WHERE s." + TblSongs.ARTIST_ID
					+ "=? GROUP BY g." + TblGenres.GENRE_ID;

			Log.v(TAG, sql);
			cur = execSelect(sql, new String[] { "" + artist.getId() });
			if (cur == null) {
				throw new DataUnavailableException();
			}
			// Log.v(TAG, "Cursor size: " + cur.getCount());
			int num = 0;
			while (cur.moveToNext()) {
				num++;
				genres.add(new Pair<Genre, Integer>(new Genre(cur.getInt(1), cur.getString(2)), Integer.valueOf(cur
						.getInt(0))));
			}

			Log.v(TAG, "Cursor size: " + num);
			return genres;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public GenreSongMap getGenreSongMappings() throws DataUnavailableException {
		ICursor cur = null;
		try {
			GenreSongMap genreSongMap = new GenreSongMap();
			String sql = "SELECT " + TblSongGenres.GENRE_ID + ", " + TblSongGenres.SONG_ID + " FROM "
					+ TblSongGenres.TABLE_NAME;
			cur = execSelect(sql, null);

			while (cur.moveToNext()) {
				genreSongMap.put(cur.getInt(0), cur.getInt(1));
			}
			return genreSongMap;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public String getSongPath(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblSongs.DATA + " FROM " + TblSongs.TBL_NAME + " WHERE " + TblSongs.SONG_ID
					+ " = ?";
			cur = execSelect(sql, new String[] { "" + song.getId() });
			if (!cur.moveToNext()) {
				return null;
			}
			return cur.getString(0);
		} catch (UncheckedSqlException e) {
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public HashMap<String, Integer> getSongPathToIdMapping() throws DataUnavailableException {
		ICursor cur = null;
		try {
			HashMap<String, Integer> songPathIdMap = new HashMap<String, Integer>();
			// TODO: what about TblSongs.IS_IGNORED??
			String sql = "SELECT " + TblSongs.SONG_ID + ", " + TblSongs.DATA + " FROM " + TblSongs.TBL_NAME;

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int songId = cur.getInt(0);
				String songPath = cur.getString(1);
				songPathIdMap.put(songPath, songId);
			}
			return songPathIdMap;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public HashSet<String> getAllSongsPaths() throws DataUnavailableException {
		ICursor cur = null;
		try {
			HashSet<String> paths = new HashSet<String>();
			// TODO: what about TblSongs.IS_IGNORED??
			String sql = "SELECT " + TblSongs.DATA + " FROM " + TblSongs.TBL_NAME;

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				String path = cur.getString(0);
				paths.add(path);
			}
			return paths;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<String> getSongPathsForAlbumName(String name) {
		ICursor cur = null;
		try {
			ArrayList<String> paths = new ArrayList<String>();
			// TODO: what about TblSongs.IS_IGNORED??
			String sql = "SELECT " + TblSongs.DATA + " FROM " + TblSongs.TBL_NAME + " s JOIN " + TblAlbums.TBL_NAME
					+ " a ON s." + TblSongs.ALBUM_ID + " = a." + TblAlbums.ALBUM_ID + " WHERE a."
					+ TblAlbums.ALBUM_NAME + " = '" + name + "'";

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				String path = cur.getString(0);
				paths.add(path);
			}
			return paths;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public String getAlbumArtPath(BaseAlbum album, boolean lowRes) throws DataUnavailableException {

		ICursor cur = null;
		try {
			String sql = null;
			if (lowRes) {
				sql = "SELECT " + TblAlbums.LOW_RES_COVER_PATH + " FROM " + TblAlbums.TBL_NAME + " WHERE "
						+ TblAlbums.ALBUM_ID + "=?";
			} else {
				sql = "SELECT " + TblAlbums.HIGH_RES_COVER_PATH + " FROM " + TblAlbums.TBL_NAME + " WHERE "
						+ TblAlbums.ALBUM_ID + "=?";
			}

			cur = execSelect(sql, new String[] { "" + album.getId() });

			if (cur.moveToNext()) {
				return cur.getString(0);
			} else {
				throw new DataUnavailableException(); // TODO: change to more
				// meaningful exception
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<SongCoords> getSongCoords(boolean includeSongWithoutCoords) {
		// TODO: merge with getSongCoordsAndPcaCoords? (pcaCoords are cheap
		// here...)
		ICursor cur = null;
		try {
			String acPrefix = "ac." + TblArtistCoords.COORD_PREFIX;
			String scPrefix = "sc." + TblSongCoords.COORD_PREFIX;
			List<SongCoords> list = new ArrayList<SongCoords>();
			String sql = "SELECT s." + TblSongs.SONG_ID + ", " + DbUtils.getCoordString(scPrefix) + ", "
					+ DbUtils.getCoordString(acPrefix) + " FROM " + TblSongs.TBL_NAME + " s LEFT JOIN "
					+ TblSongCoords.TBL_NAME + " sc ON s." + TblSongs.SONG_ID + " = sc." + TblSongCoords.SONG_ID
					+ " LEFT JOIN " + TblArtists.TBL_NAME + " a ON s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID + " = ac." + TblArtistCoords.ME_ARTIST_ID;
			// Log.v(TAG, "getSongCoords(): " + sql);
			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				float[] coords = null;
				if (!cur.isNull(1)) {
					// song coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(1 + i);
					}
				} else if (!cur.isNull(Constants.DIM + 1)) {
					// artist coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(Constants.DIM + 1 + i);
					}
				}
				if (!includeSongWithoutCoords && coords == null) {
					continue;
				}
				SongCoords sc = new SongCoords(id, coords);
				list.add(sc);
			}
			return list;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public float[] getCoordsForSongById(int songId) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String acPrefix = "ac." + TblArtistCoords.COORD_PREFIX;
			String scPrefix = "sc." + TblSongCoords.COORD_PREFIX;
			String sql = "SELECT s." + TblSongs.SONG_ID + ", " + DbUtils.getCoordString(scPrefix) + ", "
					+ DbUtils.getCoordString(acPrefix) + " FROM " + TblSongs.TBL_NAME + " s LEFT JOIN "
					+ TblSongCoords.TBL_NAME + " sc ON s." + TblSongs.SONG_ID + " = sc." + TblSongCoords.SONG_ID
					+ " LEFT JOIN " + TblArtists.TBL_NAME + " a ON s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID + " = ac."
					+ TblArtistCoords.ME_ARTIST_ID + " WHERE s." + TblSongs.SONG_ID + "=?";
			//			Log.v(TAG, "getSongCoords(): " + sql + " " + songId);
			cur = execSelect(sql, new String[] { Integer.toString(songId) });
			if (cur.moveToNext()) {
				float[] coords = null;
				if (cur.getColumnCount() > 1) {
					if (!cur.isNull(1)) {
						// song coords available
						coords = new float[Constants.DIM];
						for (int i = 0; i < Constants.DIM; i++) {
							coords[i] = cur.getFloat(1 + i);
						}
					} else if (!cur.isNull(Constants.DIM + 1)) {
						// artist coords available
						coords = new float[Constants.DIM];
						for (int i = 0; i < Constants.DIM; i++) {
							coords[i] = cur.getFloat(Constants.DIM + 1 + i);
						}
					}
					if (coords != null) {
						return coords;
					}
				}
			}
			throw new DataUnavailableException("No coordinates are available for this song");
		} catch (Exception e) {
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public float[] getCoordsForAlbum(BaseAlbum album) throws DataUnavailableException {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = getSongListForAlbum(album);
		float[] coords = null;
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			try {
				SongCoords sc = getSongCoordsById(song.getId());
				coords = sc.getCoords();
				if (coords != null) {
					break;
				}
			} catch (DataUnavailableException e) {
				// Ignore it because we can look at other songs
				// We throw an exception later if we found no coords
			}
		}
		if (coords == null) {
			throw new DataUnavailableException();
		}
		return coords;
	}

	@Override
	public boolean hasArtistCoords(int meArtistId) {
		ICursor cur = null;
		try {
			String sql = "SELECT count(*) FROM " + TblArtistCoords.TBL_NAME + " WHERE " + TblArtistCoords.ME_ARTIST_ID
					+ " = ?";
			cur = execSelect(sql, new String[] { Integer.toString(meArtistId) });
			cur.moveToNext();
			return cur.getInt(0) != 0;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public SongCoords getSongCoordsById(Integer songId) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String acPrefix = "ac." + TblArtistCoords.COORD_PREFIX;
			String scPrefix = "sc." + TblSongCoords.COORD_PREFIX;
			String sql = "SELECT s." + TblSongs.SONG_ID + ", " + DbUtils.getCoordString(scPrefix) + ", "
					+ DbUtils.getCoordString(acPrefix) + " FROM " + TblSongs.TBL_NAME + " s LEFT JOIN "
					+ TblSongCoords.TBL_NAME + " sc ON s." + TblSongs.SONG_ID + " = sc." + TblSongCoords.SONG_ID
					+ " LEFT JOIN " + TblArtists.TBL_NAME + " a ON s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID + " = ac."
					+ TblArtistCoords.ME_ARTIST_ID + " WHERE s." + TblSongs.SONG_ID + "=" + songId;
			cur = execSelect(sql, null);
			if (cur.moveToNext()) {
				int id = cur.getInt(0);
				float[] coords = null;
				if (!cur.isNull(1)) {
					// song coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(1 + i);
					}
				} else if (!cur.isNull(Constants.DIM + 1)) {
					// artist coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(Constants.DIM + 1 + i);
					}
				}
				return new SongCoords(id, coords);
			} else {
				throw new DataUnavailableException();
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<SongCoords> getSongCoordsById(List<Integer> songIds) {
		ICursor cur = null;
		ArrayList<SongCoords> songCoords = new ArrayList<SongCoords>();
		if (songIds.size() == 0) {
			return songCoords;
		}

		try {

			String acPrefix = "ac." + TblArtistCoords.COORD_PREFIX;
			String scPrefix = "sc." + TblSongCoords.COORD_PREFIX;
			String query = "SELECT s." + TblSongs.SONG_ID + ", " + DbUtils.getCoordString(scPrefix) + ", " + DbUtils
					.getCoordString(acPrefix) + " FROM " + TblSongs.TBL_NAME + " s LEFT JOIN " + TblSongCoords.TBL_NAME + " sc ON s." +
					TblSongs.SONG_ID + " = sc." + TblSongCoords.SONG_ID
					+ " LEFT JOIN " + TblArtists.TBL_NAME + " a ON s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID + " = ac." + TblArtistCoords.ME_ARTIST_ID
					+ " WHERE s." + TblSongs.SONG_ID + " IN (";

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(query);
			int lastElementPos = songIds.size() - 1;
			int count = 0;
			for (Integer songId : songIds) {
				stringBuilder.append(songId);
				if (count != lastElementPos) {
					stringBuilder.append(",");
				} else {
					stringBuilder.append(")");
				}
				count++;
			}
			String sql = stringBuilder.toString();

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				float[] coords = null;
				if (!cur.isNull(1)) {
					// song coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(1 + i);
					}
					songCoords.add(new SongCoords(id, coords));

				} else if (!cur.isNull(Constants.DIM + 1)) {
					// artist coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(Constants.DIM + 1 + i);
					}
				}
			}
			return songCoords;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * Should only be called by the PreloadedDataManager If you need tags get it from the preloaded data
	 * 
	 * @return
	 */
	@Override
	public HashMap<Integer, CompleteTag> getCompleteTags(boolean onlyRelevantTags) throws DataUnavailableException {
		ICursor cur = null;
		try {
			HashMap<Integer, CompleteTag> tags = new HashMap<Integer, CompleteTag>();
			String sql = "SELECT " + TblTags.TAG_ID + ", " + TblTags.ME_TAG_ID + ", " + TblTags.NAME + ", "
					+ TblTags.MEAN_PLSA_PROB + ", " + TblTags.MEAN_PCA_SPACE_X + ", " + TblTags.MEAN_PCA_SPACE_Y + ", "
					+ TblTags.VARIANCE_PLSA_PROB + ", " + TblTags.VARIANCE_PCA_SPACE + ", " + TblTags.IS_MAP_TAG + ", "
					+ DbUtils.getCoordString(TblTags.COORD_PREFIX) + " FROM " + TblTags.TBL_NAME;
			if (onlyRelevantTags) {
				sql += " WHERE " + TblTags.IS_RELEVANT + "=1";
			}
			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				int meId = cur.getInt(1);
				String name = cur.getString(2);
				Float meanPlsaProb = cur.isNull(3) ? null : cur.getFloat(3);
				Float meanPcaSpaceX = cur.isNull(4) ? null : cur.getFloat(4);
				Float meanPcaSpaceY = cur.isNull(5) ? null : cur.getFloat(5);
				Float varPlsaProb = cur.isNull(6) ? null : cur.getFloat(6);
				Float varPcaSpace = cur.isNull(7) ? null : cur.getFloat(7);
				int isMapTag = cur.isNull(8) ? 0 : cur.getInt(8);
				float[] coords = null;
				if (!cur.isNull(9)) {
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(9 + i);
					}
				}
				tags.put(id, new CompleteTag(id, name, meId, varPlsaProb, varPcaSpace, meanPlsaProb, meanPcaSpaceX,
						meanPcaSpaceY, coords, isMapTag == 1));
			}
			return tags;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public CompleteTag getCompleteTagById(int tagId) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblTags.TAG_ID + ", " + TblTags.ME_TAG_ID + ", " + TblTags.NAME + ", "
					+ TblTags.MEAN_PLSA_PROB + ", " + TblTags.MEAN_PCA_SPACE_X + ", " + TblTags.MEAN_PCA_SPACE_Y + ", "
					+ TblTags.VARIANCE_PLSA_PROB + ", " + TblTags.VARIANCE_PCA_SPACE + ", " + TblTags.IS_MAP_TAG + ", "
					+ DbUtils.getCoordString(TblTags.COORD_PREFIX) + " FROM " + TblTags.TBL_NAME + " WHERE "
					+ TblTags.TAG_ID + "=" + tagId;
			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				return null;
			}
			int id = cur.getInt(0);
			int meId = cur.getInt(1);
			String name = cur.getString(2);
			Float meanPlsaProb = cur.isNull(3) ? null : cur.getFloat(3);
			Float meanPcaSpaceX = cur.isNull(4) ? null : cur.getFloat(4);
			Float meanPcaSpaceY = cur.isNull(5) ? null : cur.getFloat(5);
			Float varPlsaProb = cur.isNull(6) ? null : cur.getFloat(6);
			Float varPcaSpace = cur.isNull(7) ? null : cur.getFloat(7);
			int isMapTag = cur.isNull(8) ? 0 : cur.getInt(8);
			float[] coords = null;
			if (!cur.isNull(9)) {
				coords = new float[Constants.DIM];
				for (int i = 0; i < Constants.DIM; i++) {
					coords[i] = cur.getFloat(9 + i);
				}
			}
			return new CompleteTag(id, name, meId, varPlsaProb, varPcaSpace, meanPlsaProb, meanPcaSpaceX,
					meanPcaSpaceY, coords, isMapTag == 1);

		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public Integer getTagId(String tagName, boolean onlyRelevantTags) throws DataUnavailableException {
		ICursor cur = null;
		try {
			tagName = DbUtils.escapeString(tagName);
			String sql = "SELECT " + TblTags.TAG_ID + " FROM " + TblTags.TBL_NAME + " WHERE " + TblTags.NAME + " = '"
					+ tagName + "'";
			if (onlyRelevantTags) {
				sql += " AND " + TblTags.IS_RELEVANT + "=1";
			}
			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				return null;
			}
			return cur.getInt(0);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public List<MapTag> getMapTags() {
		ICursor cur = null;
		try {
			List<MapTag> tags = new ArrayList<MapTag>();
			String sql = "SELECT " + TblTags.TAG_ID + ", " + TblTags.NAME + ", " + TblTags.MEAN_PCA_SPACE_X + ", "
					+ TblTags.MEAN_PCA_SPACE_Y + ", " + TblTags.VARIANCE_PCA_SPACE + ", "
					+ DbUtils.getCoordString(TblTags.COORD_PREFIX) + " FROM " + TblTags.TBL_NAME + " WHERE "
					+ TblTags.IS_MAP_TAG + "=1";
			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				String name = cur.getString(1);
				Float meanPcaSpaceX = cur.isNull(2) ? 0 : cur.getFloat(2);
				Float meanPcaSpaceY = cur.isNull(3) ? 0 : cur.getFloat(3);
				float varPcaSpace = cur.isNull(4) ? 0f : cur.getFloat(4);
				float[] coords = null;
				if (!cur.isNull(5)) {
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(5 + i);
					}
				}
				float[] means = new float[] { meanPcaSpaceX, meanPcaSpaceY };
				MapTag tag = new MapTag(id, name, means, varPcaSpace);
				tags.add(tag);
			}
			return tags;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<MapTag> getHighestVarianceTags(int numTags) {
		ICursor cur = null;
		try {
			List<MapTag> tags = new ArrayList<MapTag>();
			String sql = "SELECT " + TblTags.TAG_ID + ", " + TblTags.NAME + ", " + TblTags.MEAN_PCA_SPACE_X + ", "
					+ TblTags.MEAN_PCA_SPACE_Y + ", " + TblTags.VARIANCE_PCA_SPACE + " FROM " + TblTags.TBL_NAME
					+ " WHERE " + TblTags.IS_RELEVANT + "=1 ORDER BY " + TblTags.VARIANCE_PLSA_PROB + " DESC LIMIT "
					+ numTags;

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				String name = cur.getString(1);
				Float meanPcaSpaceX = cur.isNull(2) ? 0 : cur.getFloat(2);
				Float meanPcaSpaceY = cur.isNull(3) ? 0 : cur.getFloat(3);
				float varPcaSpace = cur.isNull(4) ? 0 : cur.getFloat(4);
				float[] means = new float[] { meanPcaSpaceX, meanPcaSpaceY };
				MapTag tag = new MapTag(id, name, means, varPcaSpace);
				tags.add(tag);
			}
			return tags;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void logArtistSetTable() {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtistSets.ARTIST_SET_ID + "," + TblArtistSets.ARTIST_ID + " FROM "
					+ TblArtistSets.TBL_NAME;
			Log.v(TAG, "getAlbumsForArtist() 1");
			cur = execSelect(sql, new String[] {});

			// Log.v(TAG, "TblArtistSets num entries: " + cur.getCount());
			int num = 0;
			while (cur.moveToNext()) {
				num++;
				Log.v(TAG, "set id: " + cur.getInt(0) + ", artist id: " + cur.getInt(1));

			}
			Log.v(TAG, "TblArtistSets num entries: " + num);
			Log.v(TAG, "getAlbumsForArtist() 3");
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<MapAlbum> getAllMapAlbums() {
		ICursor cur = null;
		try {
			List<MapAlbum> albums = new ArrayList<MapAlbum>();
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", al."
					+ TblAlbums.PCA_COORDS_X + ", al." + TblAlbums.PCA_COORDS_Y + ", al." + TblAlbums.COLOR + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " WHERE " + TblAlbums.PCA_COORDS_X
					+ " IS NOT NULL";

			cur = execSelect(sql, new String[] {});

			List<BaseArtist> artists = null;
			int lastAlbumId = -1;

			while (cur.moveToNext()) {
				// Log.v(TAG, "getAllMapAlbums() id: " + cur.getInt(0)
				// + ", name: " + cur.getString(1) + ", artist: "
				// + cur.getString(6));
				if (cur.getInt(0) != lastAlbumId) {
					artists = new ArrayList<BaseArtist>();
					albums.add(new MapAlbum(cur.getInt(0), cur.getString(1), new float[] { cur.getFloat(2),
							cur.getFloat(3) }, cur.getInt(4), artists));
					lastAlbumId = cur.getInt(0);
					// Log.v(TAG, "Returned map album with color " +
					// Integer.toHexString(albums.get(albums.size()-1).getColor()));
				}
				artists.add(new BaseArtist(cur.getInt(5), cur.getString(6)));
			}
			Log.v(TAG, "Returned " + albums.size() + " map albums");
			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public MapAlbum getMapAlbum(BaseAlbum album) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", al."
					+ TblAlbums.PCA_COORDS_X + ", al." + TblAlbums.PCA_COORDS_Y + ", al." + TblAlbums.COLOR + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " WHERE " + TblAlbums.ALBUM_ID + "="
					+ album.getId() + " AND " + TblAlbums.PCA_COORDS_X + " IS NOT NULL";

			cur = execSelect(sql, new String[] {});

			List<BaseArtist> artists = null;
			MapAlbum mapAlbum = null;

			while (cur.moveToNext()) {
				if (artists == null) {
					artists = new ArrayList<BaseArtist>();
					mapAlbum = new MapAlbum(cur.getInt(0), cur.getString(1), new float[] { cur.getFloat(2),
							cur.getFloat(3) }, cur.getInt(4), artists);
				}
				artists.add(new BaseArtist(cur.getInt(5), cur.getString(6)));
			}
			if (artists == null) {
				throw new DataUnavailableException();
			}
			return mapAlbum;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public MapAlbum getMapAlbumBySong(BaseSong<? extends BaseArtist, ? extends BaseAlbum> song)
			throws DataUnavailableException {
		if (song == null) {
			return null;
		}
		ICursor cur = null;
		try {
			MapAlbum album = null;
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", al."
					+ TblAlbums.PCA_COORDS_X + ", al." + TblAlbums.PCA_COORDS_Y + ", al." + TblAlbums.COLOR + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " JOIN " + TblSongs.TBL_NAME
					+ " s ON al." + TblAlbums.ALBUM_ID + " = s." + TblSongs.ALBUM_ID + " WHERE " + TblSongs.SONG_ID
					+ "=" + song.getId();

			cur = execSelect(sql, new String[] {});

			List<BaseArtist> artists = null;
			int lastAlbumId = -1;

			while (cur.moveToNext()) {
				if (cur.getInt(0) != lastAlbumId) {
					artists = new ArrayList<BaseArtist>();
					album = new MapAlbum(cur.getInt(0), cur.getString(1), new float[] { cur.getFloat(2),
							cur.getFloat(3) }, cur.getInt(4), artists);
					lastAlbumId = cur.getInt(0);
				}
				artists.add(new BaseArtist(cur.getInt(5), cur.getString(6)));
			}
			return album;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public int getRandomSongId() throws DataUnavailableException {
		ICursor cur = null;
		try {
			int[] bounds = getSongIdBounds();
			Log.v(TAG, "b1: " + bounds[0] + " b2: " + bounds[1]);
			int diff = bounds[1] - bounds[0];
			if (diff < 1) {
				throw new DataUnavailableException();
			}

			Random random = RandomProvider.getRandom();
			int rnd = random.nextInt(diff);
			rnd += bounds[0];

			String sql = "SELECT " + TblSongs.SONG_ID + " FROM " + TblSongs.TBL_NAME + " WHERE " + TblSongs.SONG_ID
					+ " >= " + rnd;

			cur = execSelect(sql, null);
			if (cur.moveToNext()) {
				return cur.getInt(0);
			}
			throw new DataUnavailableException();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private int getSongId(ImportSong song) throws DataUnavailableException {
		ICursor cur = null;
		try {

			String sql = String.format("SELECT %s FROM %s WHERE %s=?;", TblSongs.SONG_ID, TblSongs.TBL_NAME,
					TblSongs.DATA);

			cur = execSelect(sql, new String[] { song.getPath() });
			if (cur.moveToNext()) {
				return cur.getInt(0);
			}
			throw new DataUnavailableException();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private int[] getSongIdBounds() throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT min(" + TblSongs.SONG_ID + "), max(" + TblSongs.SONG_ID + ") FROM "
					+ TblSongs.TBL_NAME;

			cur = execSelect(sql, null);
			if (cur.moveToNext()) {
				int[] bounds = new int[] { cur.getInt(0), cur.getInt(1) };
				return bounds;
			}
			throw new DataUnavailableException();
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<Integer> getSongIdsForAlbum(int albumId) {

		ICursor cur = null;
		try {
			List<Integer> list = new ArrayList<Integer>();
			String sql = "SELECT " + TblSongs.SONG_ID + " FROM " + TblSongs.TBL_NAME + " WHERE " + TblSongs.ALBUM_ID
					+ " = ?";
			// Log.v(TAG, "getSongIdsForAlbum(): " + sql);
			cur = execSelect(sql, new String[] { Integer.toString(albumId) });
			while (cur.moveToNext()) {
				list.add(cur.getInt(0));
			}
			return list;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public Integer getMusicExplorerArtistId(BaseArtist artist) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtists.ME_ARTIST_ID + " FROM " + TblArtists.TBL_NAME + " WHERE "
					+ TblArtists.ARTIST_ID + "=?";
			cur = execSelect(sql, new String[] { "" + artist.getId() });

			if (cur.moveToNext()) {
				if (cur.isNull(0)) {
					return null;
				}
				return Integer.valueOf(cur.getInt(0));
			} else {
				throw new DataUnavailableException("No data was found.");
			}
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<PreloadedSongInfo> getPreloadedSongInfo() {
		ICursor cur = null;
		try {
			String acPrefix = "ac." + TblArtistCoords.COORD_PREFIX;
			String scPrefix = "sc." + TblSongCoords.COORD_PREFIX;
			List<PreloadedSongInfo> list;
			list = new ArrayList<PreloadedSongInfo>();
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.SONG_PCA_X + ", s." + TblSongs.SONG_PCA_Y
					+ ", " + DbUtils.getCoordString(scPrefix) + ", " + DbUtils.getCoordString(acPrefix) + " FROM "
					+ TblSongs.TBL_NAME + " s LEFT JOIN " + TblSongCoords.TBL_NAME + " sc ON s." + TblSongs.SONG_ID
					+ " = sc." + TblSongCoords.SONG_ID
					+ " LEFT JOIN " + TblArtists.TBL_NAME + " a ON s." + TblSongs.ARTIST_ID + " = a." + TblArtists.ARTIST_ID
					+ " LEFT JOIN " + TblArtistCoords.TBL_NAME + " ac ON a." + TblArtists.ME_ARTIST_ID + " = ac." + TblArtistCoords.ME_ARTIST_ID;
			Log.v(TAG, "getSongCoordsAndPcaCoords(): " + sql);
			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				float[] pcaCoords = null;
				if (!cur.isNull(1)) {
					pcaCoords = new float[2];
					pcaCoords[0] = cur.getFloat(1);
					pcaCoords[1] = cur.getFloat(2);
				}
				float[] coords = null;
				if (!cur.isNull(3)) {
					// song coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(3 + i);
					}
				} else if (!cur.isNull(Constants.DIM + 3)) {
					// artist coords available
					coords = new float[Constants.DIM];
					for (int i = 0; i < Constants.DIM; i++) {
						coords[i] = cur.getFloat(Constants.DIM + 3 + i);
					}
				}

				PreloadedSongInfo sc = new PreloadedSongInfo(id, coords, pcaCoords);
				list.add(sc);
			}
			return list;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	@Override
	public Integer getMusicExplorerIdForSong(BaseSong<BaseArtist, BaseAlbum> song) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblSongs.ME_SONG_ID + " FROM " + TblSongs.TBL_NAME + " WHERE " + TblSongs.SONG_ID
					+ "=?";
			cur = execSelect(sql, new String[] { "" + song.getId() });

			if (cur.moveToNext()) {
				if (cur.isNull(0)) {
					return null;
				}
				return Integer.valueOf(cur.getInt(0));
			}
			return null;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public PlayLogSendEntity getPlayLogString(int profileId, int playLogVersion, int coordinateVersion, long lastSentId)
			throws DataUnavailableException {

		PlayLogSendEntity sendEntity = new PlayLogSendEntity();
		String log = "";

		ICursor c = null;
		try {

			String sql = "SELECT " + TblPlayLog.PLAY_LOG_ID + "," + TblPlayLog.TIMESTAMP + ","
					+ TblPlayLog.TIME_ZONE_OFFSET + ", " + TblPlayLog.ME_ARTIST_ID + "," + TblPlayLog.ME_SONG_ID + ","
					+ TblPlayLog.PLAY_MODE + "," + TblPlayLog.SKIPPED + "," + TblPlayLog.SONG_SOURCE + ","
					+ TblPlayLog.CONTEXT + " FROM " + TblPlayLog.TBL_NAME + " WHERE " + TblPlayLog.PLAY_LOG_ID + " > "
					+ lastSentId + " AND " + TblPlayLog.PROFILE_ID + " = " + profileId;
			StringBuilder logString = new StringBuilder();

			// Write UserId into String
			String uid = languageHelper.getUniqueId();

			Log.v(TAG, "UID: " + uid);

			logString.append("UID;" + uid + ";PLAYLOG_VERSION;" + playLogVersion + ";VERSION;" + coordinateVersion
					+ "#");

			c = execSelect(sql, null);
			int columnnumbers = c.getColumnCount();
			if (!c.moveToNext()) {
				Log.v(TAG, "No new log entries exist since logid: " + lastSentId);
				return null;
			}

			// Log.v(TAG, "Number of Entries: " + c.getCount()
			// + " Number of Columns: " + columnnumbers);
			int num = 0;

			do {

				if (num != 0) {
					logString.append("#");
				}
				num++;

				sendEntity.lastId = c.getInt(0);

				for (int j = 1; j < columnnumbers; j++) {
					logString.append(c.getString(j));
					if (!(j == columnnumbers - 1)) {
						logString.append(";");
					}
				}
			} while (c.moveToNext());

			Log.v(TAG, "Number of Entries: " + num + " Number of Columns: " + columnnumbers);

			log = logString.toString();

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		sendEntity.logString = log;

		return sendEntity;
	}

	@Override
	public List<ListAlbum> findAlbumBySearchString(String searchTerm, int maxResults) {
		ICursor cur = null;
		try {
			String escapedSearchTerm = DbUtils.escapeString(searchTerm);
			List<ListAlbum> albums = new ArrayList<ListAlbum>();
			String sql = "SELECT al." + TblAlbums.ALBUM_ID + ", al." + TblAlbums.ALBUM_NAME + ", ar."
					+ TblArtists.ARTIST_ID + ", ar." + TblArtists.NAME + " FROM " + TblAlbums.TBL_NAME + " al JOIN "
					+ TblArtistSets.TBL_NAME + " aset ON al." + TblAlbums.ARTIST_SET_ID + " = aset."
					+ TblArtistSets.ARTIST_SET_ID + " JOIN " + TblArtists.TBL_NAME + " ar ON aset."
					+ TblArtistSets.ARTIST_ID + " = ar." + TblArtists.ARTIST_ID + " WHERE al." + TblAlbums.ALBUM_NAME
					+ " LIKE '%" + escapedSearchTerm + "%' LIMIT ?";

			cur = execSelect(sql, new String[] { "" + maxResults });

			List<BaseArtist> artists = null;
			int lastAlbumId = -1;

			while (cur.moveToNext()) {
				if (cur.getInt(0) != lastAlbumId) {
					artists = new ArrayList<BaseArtist>();
					albums.add(new ListAlbum(cur.getInt(0), cur.getString(1), artists));
					lastAlbumId = cur.getInt(0);
				}
				artists.add(new BaseArtist(cur.getInt(2), cur.getString(3)));
			}
			return albums;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseArtist> findArtistBySearchString(String searchTerm, int maxResults) {
		List<BaseArtist> artists = new ArrayList<BaseArtist>();
		ICursor cur = null;
		try {
			String escapedSearchTerm = DbUtils.escapeString(searchTerm);
			String sql = "SELECT " + TblArtists.ARTIST_ID + ", " + TblArtists.NAME + " FROM " + TblArtists.TBL_NAME
					+ " WHERE " + TblArtists.IS_IN_COLLECTION + " = 1" + " AND " + TblArtists.NAME + " LIKE '%"
					+ escapedSearchTerm + "%' LIMIT ?";

			cur = execSelect(sql, new String[] { "" + maxResults });

			while (cur.moveToNext()) {
				artists.add(new BaseArtist(cur.getInt(0), cur.getString(1)));
			}
			return artists;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseArtist> findFamousArtistBySearchString(String searchTerm, int maxResults) {
		List<BaseArtist> artists = new ArrayList<BaseArtist>();
		ICursor cur = null;
		try {
			String escapedSearchTerm = DbUtils.escapeString(searchTerm);
			String sql = "SELECT " + TblArtists.ARTIST_ID + ", " + TblArtists.NAME + " FROM " + TblArtists.TBL_NAME
					+ " WHERE " + TblArtists.IS_FAMOUS_ARTIST + " = 1" + " AND " + TblArtists.NAME + " LIKE '%"
					+ escapedSearchTerm + "%' LIMIT ?";

			cur = execSelect(sql, new String[] { "" + maxResults });

			while (cur.moveToNext()) {
				artists.add(new BaseArtist(cur.getInt(0), cur.getString(1)));
			}
			return artists;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> findTitleBySearchString(String searchTerm, int maxResults) {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		ICursor cur = null;
		try {
			String escapedSearchTerm = DbUtils.escapeString(searchTerm);
			String sql = "SELECT s." + TblSongs.SONG_ID + ", s." + TblSongs.NAME + ", s." + TblSongs.ARTIST_ID + ", " +
					"a." + TblArtists.NAME + ", s." + TblSongs.ALBUM_ID + ", alb." + TblAlbums.ALBUM_NAME + ", s." + TblSongs.DURATION + " " +
					"FROM " + TblSongs.TBL_NAME + " AS s " +
					"JOIN " + TblArtists.TBL_NAME + " AS a ON a." + TblArtists.ARTIST_ID + "=s." + TblSongs.ARTIST_ID + " " +
					"JOIN " + TblAlbums.TBL_NAME + " AS alb ON s." + TblSongs.ALBUM_ID + "=alb." + TblAlbums.ALBUM_ID + " " +
					"WHERE s." + TblSongs.NAME + " LIKE '%" + escapedSearchTerm + "%' " +
					"LIMIT ?";

			cur = execSelect(sql, new String[] { "" + maxResults });
			if (cur == null) {
				Log.w(TAG, "DB cursor is null!");
				return songs;
			}
			while (cur.moveToNext()) {
				BaseArtist artist = new BaseArtist(cur.getInt(2), cur.getString(3));
				BaseAlbum album = new BaseAlbum(cur.getInt(4), cur.getString(5));
				songs.add(new BaseSong<BaseArtist, BaseAlbum>(cur.getInt(0), cur.getString(1), artist, album,
						cur.getInt(6)));
			}
			return songs;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void printPlayLog(int profileId) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblPlayLog.ME_SONG_ID + "," + TblPlayLog.TIMESTAMP + "," + TblPlayLog.SKIPPED
					+ " FROM " + TblPlayLog.TBL_NAME + " WHERE " + TblPlayLog.PROFILE_ID + " = " + profileId;

			cur = execSelect(sql, null);
			while (cur.moveToNext()) {
				Integer meId = cur.isNull(0) ? null : cur.getInt(0);
				long timestamp = cur.getLong(1);
				int skipped = cur.getInt(2);
				Date date = new Date(timestamp);
				Log.v(TAG, "id: " + meId + ", timestamp: " + timestamp + ", date: " + date + ", skipped: " + skipped);
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public boolean isSongInRecentHistory(int playerModelId, BaseSong<BaseArtist, BaseAlbum> baseSong,
			int equalSongAvoidanceNumber)
			throws DataUnavailableException {

		boolean result = false;

		ICursor c = null;
		try {

			String sql =
					"SELECT " + TblPlayLog.SONG_ID +
							" FROM " + TblPlayLog.TBL_NAME +
							" WHERE " + TblPlayLog.PROFILE_ID + " = " + playerModelId +
							" ORDER BY " + TblPlayLog.TIMESTAMP + " DESC " +
							" LIMIT " + equalSongAvoidanceNumber;

			c = execSelect(sql, null);

			if (!c.moveToNext()) {
				Log.v(TAG, "No logs detected!");
				throw new DataUnavailableException("No logs detected!");
			}

			do {
				result |= c.getInt(0) == baseSong.getId();
			} while (c.moveToNext());

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {
				}
			}
		}

		return result;
	}

	@Override
	public boolean isArtistInRecentHistory(int playerModelId, BaseArtist baseArtist,
			int similarArtistAvoidanceNumber)
			throws DataUnavailableException {

		boolean result = false;

		ICursor c = null;
		try {

			String sql =
					"SELECT " + TblPlayLog.ARTIST_ID +
							" FROM " + TblPlayLog.TBL_NAME +
							" WHERE " + TblPlayLog.PROFILE_ID + " = " + playerModelId +
							" ORDER BY " + TblPlayLog.TIMESTAMP + " DESC " +
							" LIMIT " + similarArtistAvoidanceNumber;

			c = execSelect(sql, null);

			if (!c.moveToNext()) {
				Log.v(TAG, "No logs detected!");
				throw new DataUnavailableException("No logs detected!");
			}

			do {
				result |= c.getInt(0) == baseArtist.getId();
			} while (c.moveToNext());

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {
				}
			}
		}

		return result;
	}

	// ----------------------------------------------------------------------------------------
	// INSERTS
	// ----------------------------------------------------------------------------------------

	@Override
	public int insertSong(ImportSong s) throws DataWriteException {
		// Log.v(TAG, "insert song: " + s.getName());
		int artistId = insertArtist(s.getArtist());
		// Log.v(TAG, "insertSong: artistId: " + artistId);

		HashSet<String> albumArtistNames = s.getAlbum().getArtistNames();
		HashSet<Integer> albumArtistIds = insertArtists(albumArtistNames);
		ArtistSet albumArtistSet = insertArtistSet(albumArtistIds);

		String albumName = s.getAlbum().getName();
		Integer albumId = getAlbumId(albumName, albumArtistSet.getArtistSetId());
		if (albumId == null) {
			albumId = insertAlbum(albumName, albumArtistSet.getArtistSetId());
		}

		return insertSong(s, albumId, artistId);
	}

	@Override
	public void batchInsertSongs(Set<ImportSong> songs) {
		beginTransaction();

		try {
			// ARTISTS
			Set<String> artistNames = new HashSet<String>();
			Set<ImportAlbum> albums = new HashSet<ImportAlbum>();

			for (ImportSong song : songs) {
				String songArtistName = song.getArtist();
				ImportAlbum album = song.getAlbum();
				albums.add(album);

				artistNames.add(songArtistName);
				for (String artistName : album.getArtistNames()) {
					artistNames.add(artistName);
				}
			}
			Map<String, Integer> artistIdMap = batchInsertArtists(artistNames);
			assert artistIdMap.size() == artistNames.size();

			// ARTIST-SETS
			// TODO: ensure thread safety or/and use IdGenerator
			int maxArtistSetId = getMaxArtistSetId();

			String[] fieldNames = new String[] {
					TblArtistSets.ARTIST_SET_ID,
					TblArtistSets.ARTIST_ID,
					TblArtistSets.ARTIST_SET_HASH
			};
			BatchContentValues artistSetsContentValues = new BatchContentValues(fieldNames);

			Map<ImportAlbum, Integer> albumToArtistSetIdMap = new HashMap<ImportAlbum, Integer>();
			Map<Set<Integer>, Integer> artistSet2artistSetIdMap = new HashMap<Set<Integer>, Integer>();
			//		songloop: for (ImportSong song : songs) {
			albumloop: for (ImportAlbum album : albums) {
				Set<Integer> artistIds = new HashSet<Integer>();
				for (String artistName : album.getArtistNames()) {
					Integer e = artistIdMap.get(artistName);
					assert e != null;
					artistIds.add(e);
				}
				int hash = DbUtils.getArtistSetHash(artistIds);

				// check if artist set is already scheduled to be inserted and skip in this case
				if (artistSet2artistSetIdMap.containsKey(artistIds)) {
					albumToArtistSetIdMap.put(album, artistSet2artistSetIdMap.get(artistIds));
					continue albumloop;
				}

				List<ArtistSet> artistSets = getArtistSetsForHash(hash);

				// check if artist set already exists in DB and skip in this case
				for (ArtistSet as : artistSets) {
					if (as.getArtistIds().equals(artistIds)) {
						// ArtistSet does already exist in DB
						albumToArtistSetIdMap.put(album, as.getArtistSetId());
						artistSet2artistSetIdMap.put(artistIds, as.getArtistSetId());
						continue albumloop;
					}
				}

				for (int artistId : artistIds) {
					artistSetsContentValues.put(TblArtistSets.ARTIST_SET_ID, maxArtistSetId + 1);
					artistSetsContentValues.put(TblArtistSets.ARTIST_ID, artistId);
					artistSetsContentValues.put(TblArtistSets.ARTIST_SET_HASH, hash);
					artistSetsContentValues.saveContentValues();
					albumToArtistSetIdMap.put(album, maxArtistSetId + 1);
					artistSet2artistSetIdMap.put(artistIds, maxArtistSetId + 1);
				}

				maxArtistSetId++;
			}
			if (artistSetsContentValues.size() > 0) {
				insertBatch(TblArtistSets.TBL_NAME, artistSetsContentValues, false);
			}

			artistSetsContentValues = null;

			// ALBUMS
			String[] albumFields = new String[] {
					TblAlbums.ALBUM_NAME,
					TblAlbums.ARTIST_SET_ID,
					TblAlbums.ALBUM_STATUS,
			};
			BatchContentValues albumBCVs = new BatchContentValues(albumFields);
			for (ImportAlbum album : albums) {
				Integer artistSetId = albumToArtistSetIdMap.get(album);
				assert artistSetId != null : album.getName();

				albumBCVs.put(TblAlbums.ALBUM_NAME, album.getName());
				albumBCVs.put(TblAlbums.ARTIST_SET_ID, artistSetId);
				albumBCVs.put(TblAlbums.ALBUM_STATUS, AlbumStatus.COVER_UNCHECKED.getValue());
				albumBCVs.saveContentValues();
			}
			insertBatch(TblAlbums.TBL_NAME, albumBCVs, true);

			// SONGS
			String[] songFields = new String[] {
					TblSongs.ALBUM_ID,
					TblSongs.ARTIST_ID,
					TblSongs.DATA,
					TblSongs.DURATION,
					TblSongs.NAME,
					TblSongs.SONG_STATUS,
					TblSongs.TRACK_NR,
					TblSongs.IMPORT_TIMESTAMP
			};
			BatchContentValues songBCVs = new BatchContentValues(songFields);
			for (ImportSong song : songs) {
				// find album
				Integer albumId = getAlbumId(song.getAlbum().getName(), albumToArtistSetIdMap.get(song.getAlbum())); // TODO get these ids in one shot. This takes forever!
				assert albumId != null;
				songBCVs.put(TblSongs.ALBUM_ID, albumId);
				songBCVs.put(TblSongs.ARTIST_ID, artistIdMap.get(song.getArtist()));
				songBCVs.put(TblSongs.DATA, song.getPath());
				songBCVs.put(TblSongs.DURATION, song.getDuration());
				songBCVs.put(TblSongs.NAME, song.getName());
				songBCVs.put(TblSongs.SONG_STATUS, SongStatus.BASE_DATA.getValue());
				songBCVs.put(TblSongs.TRACK_NR, song.getTrack());
				songBCVs.put(TblSongs.IMPORT_TIMESTAMP, song.getImportDate().getTime());
				songBCVs.saveContentValues();
			}
			insertBatch(TblSongs.TBL_NAME, songBCVs, false);

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}

		// get song IDS
		for (ImportSong song : songs) {
			try {
				song.setJukefoxId(getSongId(song));
			} catch (DataUnavailableException e) {
				Log.w(TAG, "Could not acquire ID of song " + song.getPath());
			}
		}
	}

	private Map<String, Integer> getArtistIdMap(Set<String> artistNames) {
		ICursor cursor = null;
		try {

			if (artistNames.size() == 0) {
				return new HashMap<String, Integer>();
			}

			// TODO fix this. Strings are not properly escaped in the IN clause.
			String sql = String.format("SELECT %s, %s FROM %s WHERE %s IN (", TblArtists.ARTIST_ID, TblArtists.NAME,
					TblArtists.TBL_NAME, TblArtists.NAME);
			StringBuilder query = new StringBuilder(sql);

			for (String artistName : artistNames) {
				query.append(DbUtils.formatQueryValue(DbUtils.escapeString(artistName))).append(", ");
			}
			query.delete(query.length() - 2, query.length());
			query.append(");");
			sql = query.toString();
			Log.v(TAG, sql);
			cursor = execSelect(sql, null);

			Map<String, Integer> artistIdMap = new HashMap<String, Integer>();
			while (cursor.moveToNext()) {
				artistIdMap.put(cursor.getString(1), cursor.getInt(0));
			}

			return artistIdMap;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private Map<String, Integer> batchInsertArtists(Set<String> artistNames) {
		// find out which of the artists to be inserted are already in the db		
		Map<String, Integer> artistIdMap = getArtistIdMap(artistNames);

		// all the artists in artistIdMap do not have to be inserted since they are already existing in the db.
		// However we have to set inCollection to true.
		String[] insertFieldNames = new String[] {
				TblArtists.ARTIST_ID,
				TblArtists.NAME,
				TblArtists.IS_FAMOUS_ARTIST,
				TblArtists.IS_IN_COLLECTION,
		};

		BatchContentValues artistsToInsert = new BatchContentValues(insertFieldNames);
		BatchContentValues artistsIds = new BatchContentValues(new String[] { TblArtists.ARTIST_ID });
		BatchContentValues artistsInCollection = new BatchContentValues(new String[] { TblArtists.IS_IN_COLLECTION });

		for (String artistName : artistNames) {
			if (!artistIdMap.containsKey(artistName)) {
				// the artist is not in the db so we have to add it later on
				int nextId = getArtistIdGenerator().nextId();
				artistsToInsert.put(TblArtists.ARTIST_ID, nextId);
				artistsToInsert.put(TblArtists.NAME, artistName);
				artistsToInsert.put(TblArtists.IS_FAMOUS_ARTIST, 0);
				artistsToInsert.put(TblArtists.IS_IN_COLLECTION, 1);
				artistsToInsert.saveContentValues();
				artistIdMap.put(artistName, nextId);
			} else {
				artistsIds.put(TblArtists.ARTIST_ID, artistIdMap.get(artistName));
				artistsIds.saveContentValues();

				artistsInCollection.put(TblArtists.IS_IN_COLLECTION, 1);
				artistsInCollection.saveContentValues();
			}
		}

		// Now we insert all new artists
		if (artistsToInsert.size() > 0) {
			insertBatch(TblArtists.TBL_NAME, artistsToInsert, false);
		}

		// and mark all already existing ones to be InCollection
		if (artistsInCollection.size() > 0) {
			updateBatch(TblArtists.TBL_NAME, artistsInCollection, artistsIds);
		}

		return artistIdMap;
	}

	private Integer getAlbumId(String albumName, int artistSetId) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblAlbums.ALBUM_ID + " FROM " + TblAlbums.TBL_NAME + " WHERE "
					+ TblAlbums.ARTIST_SET_ID + " = ? AND " + TblAlbums.ALBUM_NAME + " = ?";
			cur = execSelect(sql, new String[] { "" + artistSetId, albumName });
			if (!cur.moveToNext()) {
				return null;
			}
			return cur.getInt(0);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void batchUpdateWebData(Set<WebDataSong> songs) {
		String[] fieldNames = new String[] {
				TblSongs.ME_SONG_ID,
				TblSongs.ME_NAME,
				TblSongs.SONG_STATUS
		};
		BatchContentValues bcvs = new BatchContentValues(fieldNames);
		BatchContentValues whereBcvs = new BatchContentValues(new String[] { TblSongs.SONG_ID });

		for (WebDataSong song : songs) {
			if (song.getStatus() == SongStatus.WEB_DATA_OK) {
				updateWebDataArtist(song.getArtist());
				replaceSongCoords(song);
			}
			bcvs.put(TblSongs.ME_SONG_ID, song.getMeId());
			bcvs.put(TblSongs.ME_NAME, song.getMeName());
			bcvs.put(TblSongs.SONG_STATUS, song.getStatus().getValue());
			bcvs.saveContentValues();

			whereBcvs.put(TblSongs.SONG_ID, song.getId());
			whereBcvs.saveContentValues();
		}
		updateBatch(TblSongs.TBL_NAME, bcvs, whereBcvs);
	}

	@Override
	public void batchUpdateAlbumCovers(Set<AlbumFetcherResult> albumCovers) {

		String[] fieldNames = new String[] {
				TblAlbums.HIGH_RES_COVER_PATH,
				TblAlbums.LOW_RES_COVER_PATH,
				TblAlbums.COLOR,
				TblAlbums.ALBUM_STATUS
		};
		BatchContentValues bcvs = new BatchContentValues(fieldNames);
		BatchContentValues whereBcvs = new BatchContentValues(new String[] { TblAlbums.ALBUM_ID });

		for (AlbumFetcherResult result : albumCovers) {
			bcvs.put(TblAlbums.HIGH_RES_COVER_PATH, result.getHighResPath());
			bcvs.put(TblAlbums.LOW_RES_COVER_PATH, result.getLowResPath());
			bcvs.put(TblAlbums.COLOR, result.getColor());
			bcvs.put(TblAlbums.ALBUM_STATUS, result.getStatus().getValue());
			bcvs.saveContentValues();
			whereBcvs.put(TblAlbums.ALBUM_ID, result.getAlbumId());
			whereBcvs.saveContentValues();
		}
		updateBatch(TblAlbums.TBL_NAME, bcvs, whereBcvs);
	}

	@Override
	public void batchInsertTags(List<CompleteTag> tags) {

		String[] fieldNames = new String[2 + Constants.DIM];
		fieldNames[0] = TblTags.ME_TAG_ID;
		fieldNames[1] = TblTags.NAME;
		for (int i = 0; i < Constants.DIM; i++) {
			fieldNames[i + 2] = TblTags.COORD_PREFIX + i;
		}

		BatchContentValues bcvs = new BatchContentValues(fieldNames);

		for (CompleteTag tag : tags) {
			bcvs.put(TblTags.ME_TAG_ID, tag.getMeId());
			bcvs.put(TblTags.NAME, tag.getName());
			float[] plsaCoords = tag.getPlsaCoords();
			for (int i = 0; i < Constants.DIM; i++) {
				bcvs.put(TblTags.COORD_PREFIX + i, plsaCoords[0]);
			}
			bcvs.saveContentValues();
		}

		insertBatch(TblTags.TBL_NAME, bcvs, false);

		for (CompleteTag tag : tags) {
			try {
				tag.setId(getTagId(tag.getName(), false));
			} catch (DataUnavailableException e) {
				e.printStackTrace();
			}
		}
	}

	private HashSet<Integer> insertArtists(Set<String> artistNames) throws DataWriteException {
		HashSet<Integer> artistIds = new HashSet<Integer>();
		for (String artistName : artistNames) {
			int artistId = insertArtist(artistName);
			artistIds.add(artistId);
		}
		return artistIds;
	}

	private ArtistSet insertArtistSet(HashSet<Integer> artistIds) {
		int hash = DbUtils.getArtistSetHash(artistIds);
		List<ArtistSet> artistSets = getArtistSetsForHash(hash);

		// Log.v(TAG, "number of artist sets for hash " + hash + ": "
		// + artistSets.size());
		for (ArtistSet as : artistSets) {
			if (as.getArtistIds().equals(artistIds)) {
				return as;
			}
		}
		if (artistSets.size() > 0) {
			Log.v(TAG, "very unlikely to happen...");
			for (ArtistSet as : artistSets) {
				Log.v(TAG, "artistSetId: " + as.getArtistSetId());
			}
		}
		// TODO: ensure thread safety?
		int artistSetId = getMaxArtistSetId() + 1;
		for (int artistId : artistIds) {
			insertArtistSetEntry(artistSetId, hash, artistId);
		}
		return new ArtistSet(artistSetId, artistIds);
	}

	private List<ArtistSet> getArtistSetsForHash(int hash) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtistSets.ARTIST_SET_ID + ", " + TblArtistSets.ARTIST_ID + " FROM "
					+ TblArtistSets.TBL_NAME + " WHERE " + TblArtistSets.ARTIST_SET_HASH + " = ? ORDER BY "
					+ TblArtistSets.ARTIST_SET_ID;

			cur = execSelect(sql, new String[] { "" + hash });
			LinkedList<ArtistSet> artistSets = new LinkedList<ArtistSet>();
			int lastSetId = -1;
			HashSet<Integer> ids = null;
			while (cur.moveToNext()) {
				if (lastSetId != cur.getInt(0)) {
					ids = new HashSet<Integer>();
					lastSetId = cur.getInt(0);
					artistSets.add(new ArtistSet(lastSetId, ids));
				}
				ids.add(cur.getInt(1));
			}
			return artistSets;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private int getMaxArtistSetId() {
		String sql = "SELECT max(" + TblArtistSets.ARTIST_SET_ID + ") FROM " + TblArtistSets.TBL_NAME;
		ICursor cur = null;
		try {
			cur = execSelect(sql, null);
			if (!cur.moveToNext()) {
				return 0;
			}
			return cur.getInt(0);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private void insertArtistSetEntry(int artistSetId, int hash, int artistId) {
		ContentValues cv = createContentValues();
		cv.put(TblArtistSets.ARTIST_SET_ID, artistSetId);
		cv.put(TblArtistSets.ARTIST_SET_HASH, hash);
		cv.put(TblArtistSets.ARTIST_ID, artistId);
		insertOrThrow(TblArtistSets.TBL_NAME, cv);
	}

	private int insertAlbum(String albumName, int artistSetId) {
		ContentValues cv = createContentValues();
		cv.put(TblAlbums.ALBUM_NAME, albumName);
		cv.put(TblAlbums.ARTIST_SET_ID, artistSetId);
		cv.put(TblAlbums.ALBUM_STATUS, AlbumStatus.COVER_UNCHECKED.getValue());
		long id = insertOrThrow(TblAlbums.TBL_NAME, cv);
		return (int) id;
	}

	private int insertSong(ImportSong s, int albumId, int artistId) throws DataWriteException {
		try {
			if (s.getPath() == null) {
				Log.v(TAG, "path == null");
			}
			ContentValues cv = createContentValues();
			cv.put(TblSongs.ALBUM_ID, albumId);
			cv.put(TblSongs.ARTIST_ID, artistId);
			cv.put(TblSongs.DATA, s.getPath());
			cv.put(TblSongs.DURATION, s.getDuration());
			cv.put(TblSongs.NAME, s.getName());
			cv.put(TblSongs.SONG_STATUS, SongStatus.BASE_DATA.getValue());
			cv.put(TblSongs.TRACK_NR, s.getTrack());
			cv.put(TblSongs.IMPORT_TIMESTAMP, s.getImportDate().getTime());
			long id = insertOrThrow(TblSongs.TBL_NAME, cv);
			return (int) id;
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private int insertArtist(String name) throws DataWriteException {
		try {
			// Log.v(TAG, "insertArtist: " + name);

			int id = getArtistId(name);

			if (id != -1) {
				// artist already exists. make sure it is marked as being part
				// of the collection
				String sql = "UPDATE " + TblArtists.TBL_NAME + " SET " + TblArtists.IS_IN_COLLECTION + " = 1 WHERE "
						+ TblArtists.ARTIST_ID + " = " + id;
				execSQL(sql);
			}
			if (id == -1) {
				ContentValues cv = createContentValues();
				cv.put(TblArtists.NAME, name);
				cv.put(TblArtists.IS_FAMOUS_ARTIST, 0);
				cv.put(TblArtists.IS_IN_COLLECTION, true);
				id = (int) insertOrThrow(TblArtists.TBL_NAME, cv);
			}
			if (id == -1) {
				throw new DataWriteException("error inserting artist with name " + name);
			}
			return id;
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		} catch (DataUnavailableException e) {
			// workaround for getArtistId Exception
			throw new DataWriteException(e);
		}
	}

	private int getArtistId(String name) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtists.ARTIST_ID + " FROM " + TblArtists.TBL_NAME + " WHERE "
					+ TblArtists.NAME + " LIKE ?";

			// Log.v(TAG, "sql: " + sql);
			cur = execSelect(sql, new String[] { name });
			if (!cur.moveToNext()) {
				return -1;
			}
			int id = cur.getInt(0);
			// Log.v(TAG, "getArtistId() => returning " + id);
			return id;
		} catch (UncheckedSqlException e) {
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void insertSongGenreMapping(int genreId, int songId) throws DataWriteException {
		try {
			ContentValues cv = createContentValues();
			cv.put(TblSongGenres.GENRE_ID, genreId);
			cv.put(TblSongGenres.SONG_ID, songId);
			int id = (int) insertOrThrow(TblSongGenres.TABLE_NAME, cv);

			if (id == -1) {
				throw new DataWriteException("error inserting song genre mapping: " + songId + "=> " + genreId);
			}
		} catch (UncheckedSqlException e) {
			throw new DataWriteException("error inserting song genre mapping: " + songId + "=> " + genreId);
		}
	}

	@Override
	public void batchInsertSongGenreMappings(GenreSongMap newMappings) {
		String[] fieldNames = new String[] {
				TblSongGenres.GENRE_ID,
				TblSongGenres.SONG_ID
		};
		BatchContentValues bcvs = new BatchContentValues(fieldNames);

		for (GenreSongEntry gse : newMappings.getAll()) {
			bcvs.put(TblSongGenres.GENRE_ID, gse.getGenreId());
			bcvs.put(TblSongGenres.SONG_ID, gse.getSongId());
			bcvs.saveContentValues();
		}
		insertBatch(TblSongGenres.TABLE_NAME, bcvs, true);
	}

	@Override
	public void insertArtistCoords(int meArtistId, float[] coords) throws DataWriteException {
		try {
			ContentValues cv = createContentValues();
			cv.put(TblArtistCoords.ME_ARTIST_ID, meArtistId);
			fillCoordsContentValues(cv, TblArtistCoords.COORD_PREFIX, coords);

			// TODO When this method is called for a famous artist, the
			// coordinates may/are (?) already existing
			// in the database and therefore insertOrThrow will log an error
			// which is not was is supposed to happen.
			insertOrThrow(TblArtistCoords.TBL_NAME, cv);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void insertAlbumArtInfo(BaseAlbum album, String highResPath, String lowResPath, int color, AlbumStatus status)
			throws DataWriteException {
		try {
			String where = "" + TblAlbums.ALBUM_ID + "=?";
			ContentValues cv = createContentValues();
			cv.put(TblAlbums.HIGH_RES_COVER_PATH, highResPath);
			cv.put(TblAlbums.LOW_RES_COVER_PATH, lowResPath);
			cv.put(TblAlbums.COLOR, color);
			update(TblAlbums.TBL_NAME, cv, where, new String[] { "" + album.getId() });
			cv.put(TblAlbums.ALBUM_STATUS, status.getValue());
			update(TblAlbums.TBL_NAME, cv, where, new String[] { "" + album.getId() });
			// TODO does only throw from update
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public int insertGenre(String name) throws DataWriteException {
		try {
			int id = getGenreIdByName(name);
			if (id != -1) {
				// genre already exists
				return id;
			}
			if (id == -1) {
				ContentValues cv = createContentValues();
				cv.put(TblGenres.NAME, name);
				id = (int) insertOrThrow(TblGenres.TBL_NAME, cv);
			}
			if (id == -1) {
				throw new DataWriteException("error inserting genre: " + name);
			}
			return id;
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private int getGenreIdByName(String name) {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblGenres.GENRE_ID + " FROM " + TblGenres.TBL_NAME + " WHERE " + TblGenres.NAME
					+ "=?";
			cur = execSelect(sql, new String[] { name });

			if (cur.moveToNext()) {
				return cur.getInt(0);
			} else {
				return -1;
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public long writePlayLogEntry(int profileId, PlaylistSong<BaseArtist, BaseAlbum> song, long utcTime,
			int timeZoneOffset, int dayOfWeek, int hourOfDay, boolean skip, int playMode,
			AbstractContextResult contextData, int playbackPosition) throws DataWriteException {
		long id = 0;
		int meSongId = 0;
		int meArtistId = 0;

		// Get MusicExplorer Ids
		ICursor cur = null;
		try {
			String sql = "SELECT s." + TblSongs.ME_SONG_ID + ", a." + TblArtists.ME_ARTIST_ID + " FROM "
					+ TblSongs.TBL_NAME + " AS s " + " JOIN " + TblArtists.TBL_NAME + " AS a ON s."
					+ TblSongs.ARTIST_ID + "=a." + TblArtists.ARTIST_ID + " WHERE s." + TblSongs.SONG_ID + "="
					+ song.getId();

			cur = execSelect(sql, new String[] {});

			if (cur != null && cur.moveToNext()) {
				meSongId = cur.getInt(0);
				meArtistId = cur.getInt(1);
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		Log.v(TAG, "writeLogEntry: meSongId: " + meSongId + ", meArtistId: " + meArtistId);

		// Write Entry
		try {

			int skipped = 0;
			if (skip) {
				skipped = 1;
			}

			ContentValues initialValues = createContentValues();
			initialValues.put(TblPlayLog.PROFILE_ID, profileId);
			initialValues.put(TblPlayLog.TIMESTAMP, utcTime);
			initialValues.put(TblPlayLog.TIME_ZONE_OFFSET, timeZoneOffset);
			initialValues.put(TblPlayLog.HOUR, hourOfDay);
			initialValues.put(TblPlayLog.DAY, dayOfWeek);
			initialValues.put(TblPlayLog.SONG_ID, song.getId());
			initialValues.put(TblPlayLog.ARTIST_ID, song.getArtist().getId());
			initialValues.put(TblPlayLog.ME_SONG_ID, meSongId);
			initialValues.put(TblPlayLog.ME_ARTIST_ID, meArtistId);
			initialValues.put(TblPlayLog.PLAY_MODE, playMode);
			initialValues.put(TblPlayLog.SKIPPED, skipped);
			initialValues.put(TblPlayLog.SONG_SOURCE, song.getSongSource().value());
			initialValues.put(TblPlayLog.CONTEXT, contextData.createDbString());
			initialValues.put(TblPlayLog.PLAYBACK_POSITION, playbackPosition);

			id = insertOrThrow(TblPlayLog.TBL_NAME, initialValues);

			Log.d(TAG, "Inserted play log id " + id);

			// TODO maxrow should be the maxrow per Profile now
			long maxrow = Constants.DB_ACTIVATED_PLAY_LOG_SIZE;
			if (id > maxrow) {
				String sql = "DELETE FROM " + TblPlayLog.TBL_NAME + " WHERE " + TblPlayLog.PLAY_LOG_ID + " < "
						+ (id - maxrow);
				execSQL(sql);
			}
		} catch (Exception e) {
			Log.w(TAG, e);
			throw new DataWriteException(e);
		}

		return id;
	}

	@Override
	public int insertTag(int meId, String name, float[] coords) throws DataWriteException {
		try {
			ContentValues cv = createContentValues();
			cv.put(TblTags.ME_TAG_ID, meId);
			cv.put(TblTags.NAME, name);
			fillCoordsContentValues(cv, TblTags.COORD_PREFIX, coords);
			long id = insertOrThrow(TblTags.TBL_NAME, cv);
			return (int) id;
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public int insertOrGetPlayerModelId(String name) throws DataWriteException {
		try {
			int id = getPlayerModelId(name);
			if (id != -1) {
				// player model already exists
				return id;
			}
			if (id == -1) {
				ContentValues cv = createContentValues();
				cv.put(TblPlayerModel.NAME, name);
				id = (int) insertOrThrow(TblPlayerModel.TBL_NAME, cv);
			}
			if (id == -1) {
				throw new DataWriteException("error inserting player model: " + name);
			}
			return id;
		} catch (UncheckedSqlException uncheckedSqlException) {
			throw new DataWriteException(uncheckedSqlException);
		} catch (DataUnavailableException e) {
			throw new DataWriteException(e);
		}
	}

	/**
	 * Gets the player model id ({@link Integer}) of the given name or -1 if it doesn't exist
	 */
	private int getPlayerModelId(String name) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblPlayerModel.ID + " FROM " + TblPlayerModel.TBL_NAME + " WHERE "
					+ TblPlayerModel.NAME + " = ?";
			cur = execSelect(sql, new String[] { name });
			if (cur.moveToNext()) {
				return cur.getInt(0);
			} else {
				return -1;
			}
		} catch (UncheckedSqlException e) {
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				try {
					cur.close();
				} catch (Exception e1) {
				}
			}
		}
	}

	// *** Collection statistics *** //

	@Override
	public CollectionProperties getCollectionProperties() {
		CollectionProperties cp = new CollectionProperties();

		try {
			double avgSongDistance = getKeyValueDouble(NAMESPACE_COLLECTION_PROPERTIES, CP_AVG_SONG_DISTANCE);
			cp.setAverageSongDistance(avgSongDistance);
		} catch (DataUnavailableException e) {
			// Just ignore it. Will be handled in the getters of CollectionProperties.
		}
		try {
			double songDistanceStdDeviation = getKeyValueDouble(NAMESPACE_COLLECTION_PROPERTIES,
					CP_SONG_DISTANCE_STD_DEVIATION);
			cp.setSongDistanceStdDeviation(songDistanceStdDeviation);
		} catch (DataUnavailableException e) {
			// Just ignore it. Will be handled in the getters of CollectionProperties.
		}

		return cp;
	}

	@Override
	public String getKeyValue(String namespace, String key) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblKeyValue.VALUE + " " +
					"FROM " + TblKeyValue.TBL_NAME + " " +
					"WHERE (" + TblKeyValue.NAMESPACE + " = ?) " +
					"  AND (" + TblKeyValue.KEY + " = ?) ";
			cur = execSelect(sql, new String[] { namespace, key });

			if (cur.moveToNext()) {
				return cur.getString(0);
			} else {
				throw new DataUnavailableException();
			}
		} catch (UncheckedSqlException e) {
			throw new DataUnavailableException(e);
		} finally {
			if (cur != null) {
				try {
					cur.close();
				} catch (Exception e1) {
				}
			}
		}
	}

	@Override
	public int getKeyValueInt(String namespace, String key) throws DataUnavailableException {
		return Integer.parseInt(getKeyValue(namespace, key));
	}

	@Override
	public double getKeyValueDouble(String namespace, String key) throws DataUnavailableException {
		return Double.parseDouble(getKeyValue(namespace, key));
	}

	// ----------------------------------------------------------------------------------------
	// UPDATES
	// ----------------------------------------------------------------------------------------

	@Override
	public void batchInsertFamousArtists(int[] meIds, String[] names, float[][] coords)
			throws DataWriteException {

		String[] artistFields = new String[] {
				TblArtists.ME_ARTIST_ID,
				TblArtists.NAME,
				TblArtists.ME_NAME,
				TblArtists.IS_FAMOUS_ARTIST
		};

		String[] coordsFields = new String[Constants.DIM + 1];
		coordsFields[0] = TblArtistCoords.ME_ARTIST_ID;
		for (int i = 1; i < coordsFields.length; i++) {
			coordsFields[i] = TblArtistCoords.COORD_PREFIX + String.valueOf(i - 1);
		}

		BatchContentValues artistBCVs = new BatchContentValues(artistFields);
		BatchContentValues coordsBCVs = new BatchContentValues(coordsFields);

		for (int i = 0; i < meIds.length; i++) {
			int meId = meIds[i];
			String name = names[i];
			float[] coord = coords[i];

			artistBCVs.put(TblArtists.ME_ARTIST_ID, meId);
			artistBCVs.put(TblArtists.NAME, name);
			artistBCVs.put(TblArtists.ME_NAME, name);
			artistBCVs.put(TblArtists.IS_FAMOUS_ARTIST, true);
			artistBCVs.saveContentValues();

			coordsBCVs.put(TblArtistCoords.ME_ARTIST_ID, meId);

			for (int j = 0; j < coord.length; j++) {
				coordsBCVs.put(TblArtistCoords.COORD_PREFIX + String.valueOf(j), coord[j]);
			}
			coordsBCVs.saveContentValues();
		}
		insertBatch(TblArtists.TBL_NAME, artistBCVs, true);
		insertBatch(TblArtistCoords.TBL_NAME, coordsBCVs, true);
	}

	@Override
	public int replaceFamousArtist(int meId, String name, String meName) throws DataWriteException {
		int artistId;
		try {
			artistId = getArtistId(name);

			if (artistId == -1) {
				artistId = insertFamousArtist(artistId, meId, name, meName);
			} else {
				updateArtistToFamous(artistId, meId, meName);
			}
			return artistId;
		} catch (DataUnavailableException e) {
			throw new DataWriteException(e);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private void updateArtistToFamous(int artistId, int meId, String meName) {
		ContentValues cv = createContentValues();
		cv.put(TblArtists.ME_ARTIST_ID, meId);
		cv.put(TblArtists.ME_NAME, meName);
		cv.put(TblArtists.IS_FAMOUS_ARTIST, true);
		update(TblArtists.TBL_NAME, cv, TblArtists.ARTIST_ID + " = " + artistId, null);
		// Log.v(TAG, "updated artist to famous: " + artistId);
	}

	private int insertFamousArtist(int artistId, int meId, String name, String meName) throws DataWriteException {
		try {
			ContentValues cv = createContentValues();
			cv.put(TblArtists.ME_ARTIST_ID, meId);
			cv.put(TblArtists.NAME, name);
			cv.put(TblArtists.ME_NAME, meName);
			cv.put(TblArtists.IS_FAMOUS_ARTIST, true);

			// for some reason, replace does not work as expected (seems to
			// always create a new row...)			
			long id = insertOrThrow(TblArtists.TBL_NAME, cv);
			// Log.v(TAG, "insert famous artist, id: " + id);
			return (int) id;
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}

	}

	@Override
	public void updateUnusedArtists() throws DataWriteException {
		try {
			String sql = "UPDATE " + TblArtists.TBL_NAME + " SET " + TblArtists.IS_IN_COLLECTION + "=0 WHERE "
					+ TblArtists.ARTIST_ID + " IN(SELECT " + TblArtists.TBL_NAME + "." + TblArtists.ARTIST_ID
					+ " FROM " + TblArtists.TBL_NAME + " LEFT JOIN " + TblSongs.TBL_NAME + " ON " + TblArtists.TBL_NAME
					+ "." + TblArtists.ARTIST_ID + " = " + TblSongs.TBL_NAME + "." + TblSongs.ARTIST_ID + " WHERE "
					+ TblSongs.TBL_NAME + "." + TblSongs.SONG_ID + " IS NULL)";
			// Log.v(TAG, "sql: " + sql);
			long startTime = System.currentTimeMillis();
			execSQL(sql);
			long endTime = System.currentTimeMillis();
			Log.v(TAG, "remove unused artist: time: " + (endTime - startTime));
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void updateWebDataSong(WebDataSong song) throws DataWriteException {
		try {
			if (song.getStatus() == SongStatus.WEB_DATA_OK) {
				updateWebDataArtist(song.getArtist());
				replaceSongCoords(song);
			}
			ContentValues cv = createContentValues();
			cv.put(TblSongs.ME_SONG_ID, song.getMeId());
			cv.put(TblSongs.ME_NAME, song.getMeName());
			cv.put(TblSongs.SONG_STATUS, song.getStatus().getValue());
			update(TblSongs.TBL_NAME, cv, TblSongs.SONG_ID + " = " + song.getId(), null);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private void replaceSongCoords(WebDataSong song) {
		if (song.getCoords() == null) {
			// TODO: or delete row?
			return;
		}
		ContentValues cv = createContentValues();
		cv.put(TblSongCoords.SONG_ID, song.getId());
		fillCoordsContentValues(cv, TblSongCoords.COORD_PREFIX, song.getCoords());
		if (songHasCoords(song.getId())) {
			update(TblSongCoords.TBL_NAME, cv, TblSongCoords.SONG_ID + "=" + song.getId(), null);
		} else {
			insertOrThrow(TblSongCoords.TBL_NAME, cv);
		}
	}

	private boolean songHasCoords(int songId) {

		ICursor cur = null;
		try {
			String sql = "SELECT " + TblSongCoords.SONG_ID + " FROM " + TblSongCoords.TBL_NAME + " WHERE "
					+ TblSongCoords.SONG_ID + "=" + songId;

			cur = execSelect(sql, null);

			if (cur.moveToNext()) {
				return true;
			} else {
				return false;
			}

		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private boolean artistHasCoords(CompleteArtist artist) {
		int artistMeId = artist.getMeId();
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblArtistCoords.ME_ARTIST_ID + " FROM " + TblArtistCoords.TBL_NAME + " WHERE "
					+ TblArtistCoords.ME_ARTIST_ID + "=" + artistMeId;

			cur = execSelect(sql, null);

			if (cur.moveToNext()) {
				return true;
			} else {
				return false;
			}

		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private void updateWebDataArtist(CompleteArtist artist) {
		replaceArtistCoords(artist);
		ContentValues cv = createContentValues();
		cv.put(TblArtists.ME_ARTIST_ID, artist.getMeId());
		cv.put(TblArtists.ME_NAME, artist.getMeName());
		update(TblArtists.TBL_NAME, cv, TblArtists.ARTIST_ID + " = " + artist.getId(), null);
	}

	private void replaceArtistCoords(CompleteArtist artist) {
		if (artist.getCoords() == null) {
			// TODO: or delete row??
			return;
		}
		ContentValues cv = createContentValues();
		cv.put(TblArtistCoords.ME_ARTIST_ID, artist.getMeId());
		fillCoordsContentValues(cv, TblArtistCoords.COORD_PREFIX, artist.getCoords());
		if (artistHasCoords(artist)) {
			update(TblArtistCoords.TBL_NAME, cv, TblArtistCoords.ME_ARTIST_ID + "=" + artist.getMeId(), null);
		} else {
			insertOrThrow(TblArtistCoords.TBL_NAME, cv);
		}
	}

	@Override
	public void updateSongsPcaCoords(HashMap<Integer, float[]> songPcaCoords) throws DataWriteException {
		try {
			for (Entry<Integer, float[]> e : songPcaCoords.entrySet()) {
				updateSongPcaCoords(e.getKey(), e.getValue());
			}
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private void updateSongPcaCoords(Integer songId, float[] pcaCoords) {
		ContentValues cv = createContentValues();
		cv.put(TblSongs.SONG_PCA_X, pcaCoords[0]);
		cv.put(TblSongs.SONG_PCA_Y, pcaCoords[1]);
		update(TblSongs.TBL_NAME, cv, TblSongs.SONG_ID + "=" + songId, null);
	}

	@Override
	public void updateMapAlbumsPcaCoords(Collection<MapAlbum> mapAlbums) throws DataWriteException {
		try {
			for (MapAlbum album : mapAlbums) {
				updateMapAlbumPcaCoords(album);
			}
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}

	}

	private void updateMapAlbumPcaCoords(MapAlbum album) {
		ContentValues cv = createContentValues();
		cv.put(TblAlbums.PCA_COORDS_X, album.getGridCoords()[0]);
		cv.put(TblAlbums.PCA_COORDS_Y, album.getGridCoords()[1]);
		update(TblAlbums.TBL_NAME, cv, TblAlbums.ALBUM_ID + "=" + album.getId(), null);

		// Log.v(TAG, "grid coords of album " + album.getName() + "("
		// + album.getId() + ") updated.");
	}

	@Override
	public void setRelevantTags(Collection<CompleteTag> relevantTags) throws DataWriteException {

		try {
			beginTransaction();
			ContentValues cv = createContentValues();
			cv.put(TblTags.IS_RELEVANT, 0);
			update(TblTags.TBL_NAME, cv, "", null);
			for (CompleteTag tag : relevantTags) {
				cv = createContentValues();
				cv.put(TblTags.IS_RELEVANT, 1);
				cv.put(TblTags.MEAN_PCA_SPACE_X, tag.getMeanPcaSpaceX());
				cv.put(TblTags.MEAN_PCA_SPACE_Y, tag.getMeanPcaSpaceY());
				cv.put(TblTags.MEAN_PLSA_PROB, tag.getMeanPlsaProb());
				cv.put(TblTags.VARIANCE_PCA_SPACE, tag.getVariancePcaSpace());
				cv.put(TblTags.VARIANCE_PLSA_PROB, tag.getVariancePlsaProb());
				cv.put(TblTags.IS_MAP_TAG, tag.isMapTag());
				update(TblTags.TBL_NAME, cv, TblTags.TAG_ID + "=" + tag.getId(), null);
				// Log.v(TAG, "updating tag: " + tag.getName() + "; x: "
				// + tag.getMeanPcaSpaceX() + ", y: " + tag.getMeanPcaSpaceY()
				// + ", isMapTag: " + tag.isMapTag());
			}
			setTransactionSuccessful();
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		} finally {
			endTransaction();
		}
	}

	@Override
	public int getMaximumValue(String tblName, String columnName) throws DataUnavailableException {
		ICursor cur = null;
		try {
			String sql = "SELECT MAX(" + columnName + ") FROM " + tblName;
			cur = execSelect(sql, new String[0]);

			if (cur == null) {
				throw new DataUnavailableException();
			}
			cur.moveToNext();
			return cur.getInt(0);
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void setCollectionProperties(CollectionProperties properties) throws DataWriteException {
		beginTransaction();
		try {
			if (properties.hasAverageSongDistance()) {
				setKeyValue(NAMESPACE_COLLECTION_PROPERTIES, CP_AVG_SONG_DISTANCE,
						properties.getAverageSongDistanceSafe());
			}
			if (properties.hasSongDistanceStdDeviation()) {
				setKeyValue(NAMESPACE_COLLECTION_PROPERTIES, CP_SONG_DISTANCE_STD_DEVIATION,
						properties.getSongDistanceStdDeviationSafe());
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	@Override
	public void setKeyValue(String namespace, String key, String value) throws DataWriteException {
		try {
			ContentValues cv = createContentValues();
			cv.put(TblKeyValue.VALUE, value);

			try {
				getKeyValue(namespace, key);

				String where = "(" + TblKeyValue.NAMESPACE + " = ?) AND (" + TblKeyValue.KEY + " = ?)";
				update(TblKeyValue.TBL_NAME, cv, where, new String[] { namespace, key });
			} catch (DataUnavailableException e) {
				cv.put(TblKeyValue.NAMESPACE, namespace);
				cv.put(TblKeyValue.KEY, key);
				insertOrThrow(TblKeyValue.TBL_NAME, cv);
			}

		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void setKeyValue(String namespace, String key, int value) throws DataWriteException {
		setKeyValue(namespace, key, Integer.toString(value));
	}

	@Override
	public void setKeyValue(String namespace, String key, double value) throws DataWriteException {
		setKeyValue(namespace, key, Double.toString(value));
	}

	// ----------------------------------------------------------------------------------------
	// DELETES
	// ----------------------------------------------------------------------------------------

	@Override
	public void removeSongById(int jukefoxId) throws DataWriteException {
		try {
			// Log.v(TAG, "removeSongById: id: " + jukefoxId);
			String sql = "DELETE FROM " + TblSongs.TBL_NAME + " WHERE " + TblSongs.SONG_ID + " = " + jukefoxId;
			execSQL(sql);
			sql = "DELETE FROM " + TblSongGenres.TABLE_NAME + " WHERE " + TblSongs.SONG_ID + " = " + jukefoxId;
			execSQL(sql);
			removeSongCoordsById(jukefoxId); // TODO: always necessary?
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
			throw new DataWriteException(e);
		}
	}

	private void removeSongCoordsById(int jukefoxId) {
		String sql = "DELETE FROM " + TblSongCoords.TBL_NAME + " WHERE " + TblSongCoords.SONG_ID + " = " + jukefoxId;
		execSQL(sql);
	}

	@Override
	public void removeUnusedAlbums() throws DataWriteException {
		try {
			removeUnusedAlbumArt();

			String sql = "DELETE FROM " + TblAlbums.TBL_NAME + " WHERE " + TblAlbums.ALBUM_ID + " IN(SELECT "
					+ TblAlbums.TBL_NAME + "." + TblAlbums.ALBUM_ID + " FROM " + TblAlbums.TBL_NAME + " LEFT JOIN "
					+ TblSongs.TBL_NAME + " ON " + TblAlbums.TBL_NAME + "." + TblAlbums.ALBUM_ID + " = "
					+ TblSongs.TBL_NAME + "." + TblSongs.ALBUM_ID + " WHERE " + TblSongs.TBL_NAME + "."
					+ TblSongs.SONG_ID + " IS NULL)";
			// Log.v(TAG, "sql: " + sql);
			long startTime = System.currentTimeMillis();
			execSQL(sql);
			long endTime = System.currentTimeMillis();
			Log.v(TAG, "remove unused albums: time: " + (endTime - startTime));
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}

	}

	private void removeUnusedAlbumArt() {
		ICursor cur = null;
		try {
			String sql = "SELECT " + TblAlbums.TBL_NAME + "." + TblAlbums.LOW_RES_COVER_PATH + ", "
					+ TblAlbums.TBL_NAME + "." + TblAlbums.HIGH_RES_COVER_PATH + " FROM " + TblAlbums.TBL_NAME
					+ " LEFT JOIN " + TblSongs.TBL_NAME + " ON " + TblAlbums.TBL_NAME + "." + TblAlbums.ALBUM_ID
					+ " = " + TblSongs.TBL_NAME + "." + TblSongs.ALBUM_ID + " WHERE " + TblSongs.TBL_NAME + "."
					+ TblSongs.SONG_ID + " IS NULL";
			cur = execSelect(sql, new String[] {});
			if (cur.moveToNext()) {
				do {
					String path = cur.getString(0);
					if (path != null && path.startsWith(directoryManager.getAlbumCoverDirectory()
							.getAbsolutePath())) {
						File f = new File(path);
						if (!f.delete()) {
							Log.w(TAG, "Could not delete file: " + f.getAbsolutePath());
						}
					}
					path = cur.getString(1);
					if (path != null && path.startsWith(directoryManager.getAlbumCoverDirectory()
							.getAbsolutePath())) {
						File f = new File(path);
						if (!f.delete()) {
							Log.w(TAG, "Could not delete file: " + f.getAbsolutePath());
						}
					}
				} while (cur.moveToNext());
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	@Override
	public void removeObsoleteGenres() throws DataWriteException {
		try {
			Log.v(TAG, "Remove obsolete genres");
			String sql = "DELETE FROM " + TblGenres.TBL_NAME + " WHERE " + TblGenres.GENRE_ID + " IN (SELECT ge."
					+ TblGenres.GENRE_ID + " FROM " + TblGenres.TBL_NAME + " AS ge LEFT JOIN "
					+ TblSongGenres.TABLE_NAME + " AS sg ON ge." + TblGenres.GENRE_ID + "=sg." + TblSongGenres.GENRE_ID
					+ " WHERE sg." + TblSongGenres.GENRE_ID + " IS NULL)";
			Log.v(TAG, sql);
			execSQL(sql);
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
			throw new DataWriteException(e);
		}
	}

	@Override
	public void deleteGenreSongMapping(int genreId, int songId) throws DataWriteException {
		try {
			String where = TblSongGenres.GENRE_ID + "=? AND " + TblSongGenres.SONG_ID + "=?";
			delete(TblSongGenres.TABLE_NAME, where, new String[] { "" + genreId, "" + songId });

		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void emptyArtistsTable() throws DataWriteException {
		try {
			emptyTable(TblArtists.TBL_NAME, false);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void emptyTagsTable() throws DataWriteException {
		try {
			emptyTable(TblTags.TBL_NAME, false);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void deleteTagTable() throws DataWriteException {
		try {
			emptyTable(TblTags.TBL_NAME, true);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	@Override
	public void emptyArtistCoordsTable() throws DataWriteException {
		try {
			emptyTable(TblArtistCoords.TBL_NAME, false);
		} catch (UncheckedSqlException e) {
			throw new DataWriteException(e);
		}
	}

	private void emptyTable(String tblName, boolean resetAutoInc) {
		String sql = "DELETE FROM " + tblName;
		execSQL(sql);
		if (resetAutoInc) {
			resetAutoIncrement(tblName);
		}
	}

	private void resetAutoIncrement(String tblName) {
		String sql = "UPDATE SQLITE_SEQUENCE SET seq = 0 WHERE name = '" + tblName + "'";
		execSQL(sql);
	}

	private void fillCoordsContentValues(ContentValues cv, String coordPrefix, float[] coords) {
		for (int i = 0; i < Constants.DIM; i++) {
			cv.put(coordPrefix + i, coords[i]);
		}
	}

	// ----------------------------------------------------------------------------------------
	// TABLE DROP/CREATE/UPDATE FUNCTIONS
	// ----------------------------------------------------------------------------------------

	protected void onCreate() {
		try {
			Log.i(TAG, "onCreate called.");
			execSQL(TblSongs.getCreateSql());
			execSQL(TblArtists.getCreateSql());
			execSQL(TblAlbums.getCreateSql());
			execSQL(TblGenres.getCreateSql());
			execSQL(TblTags.getCreateSql());
			execSQL(TblArtistSets.getCreateSql());
			execSQL(TblSongGenres.getCreateSql());
			execSQL(TblSongCoords.getCreateSql());
			execSQL(TblArtistCoords.getCreateSql());
			execSQL(TblPlayLog.getCreateSql());

		} catch (Exception e) {
			Log.w(TAG, e);
			Log.v(TAG, "db creation failed, deleting all tables");
			try {
				dropRegularTables();
			} catch (Exception e2) {
				Log.w(TAG, e2);
			}
		}
	}

	/**
	 * Takes care of updating the database to the newest version. If the database is new or cleared, call this method
	 * with oldVersion set to 0.
	 * 
	 * @param oldVersion
	 */
	protected void onUpgrade(int oldVersion) {
		Log.v(TAG, "onUpgrade()");

		boolean successful = false;
		beginTransaction();
		try {
			if (oldVersion < 3) {
				dropRegularTables(); // Remove all tables
				onCreate(); // Recreate them
			}
			if (oldVersion < 4) {
				createMissingTables4_before();
				adaptTblSongs4();
				adaptTblPlayLog4();
			}
			if (oldVersion < 5) {
				adaptTblPlayLog5();
				createMissingTables5_after();
			}
			if (oldVersion < 6) {
				adaptTblSongs6();
				adaptViewRating6();
				adaptArtistCoords6();
			}
			if (oldVersion < 7) {
				createTblKeyValue7();
				createTblRating7();
				createRatingsFromPlayLog7();
			}
			if (oldVersion < 8) {
				createTblLogEntry8();
			}
			if (oldVersion < 9) {
				adaptTblRating9();
				dropViewRating9();
			}
			if (oldVersion < 10) {
				removeWrongRatingAndPlayLogEntries10();
			}

			setTransactionSuccessful();
			successful = true;
		} catch (UncheckedSqlException e) {
			Log.w(TAG, "Failed to update the database!");
			Log.w(TAG, e);
			// TODO: set the db-version to oldVersion, since the db-update did not take effect
		} finally {
			endTransaction();
		}

		if (!successful) {
			Log.w(TAG, "Recreating the database...");
			resetDatabase();
		}
	}

	private void createMissingTables4_before() {
		Log.v(TAG, "creating missing tables...");
		execSQL(TblPlayerModel.getCreateSql());
		execSQL(TblSongStatistics.getCreateSql());
	}

	private void adaptTblPlayLog4() {
		Log.v(TAG, "adapting TblPlayLog ...");
		for (String sql : TblPlayLog.getConvertTblPlayLogQueries4()) {
			execSQL(sql);
		}
	}

	private void adaptTblSongs4() {
		Log.v(TAG, "adapting TblSongs ...");
		for (String sql : TblSongs.getConvertTblSongsQueries4()) {
			execSQL(sql);
		}
	}

	@SuppressWarnings("deprecation")
	private void createMissingTables5_after() {
		Log.v(TAG, "creating missing tables...");
		execSQL(ViewRating.getCreateSql());
	}

	private void adaptTblPlayLog5() {
		Log.v(TAG, "adapting TblPlayLog...");
		for (String sql : TblPlayLog.getConvertTblPlayLogQueries5()) {
			execSQL(sql);
		}
	}

	private void adaptTblSongs6() {
		Log.v(TAG, "adapting TblSongs...");
		for (String sql : TblSongs.getConvertTblSongsQueries6()) {
			execSQL(sql);
		}
	}

	@SuppressWarnings("deprecation")
	private void adaptViewRating6() {
		Log.v(TAG, "adapting ViewRating...");
		for (String sql : ViewRating.getUpdateTo6()) {
			execSQL(sql);
		}
	}

	private void adaptArtistCoords6() {
		Log.v(TAG, "adapting TblArtistCoords ...");
		execSQL(TblArtistCoords.getAddMeArtistIdColumnString());
		replaceArtistIds6();
		for (String sql : TblArtistCoords.getRemoveArtistIdColumnString()) {
			execSQL(sql);
		}
		Log.v(TAG, "adapted TblArtistCoords ...");
	}

	private void replaceArtistIds6() {
		try {
			Log.v(TAG, "exchanging TblArtistCoords ...");
			ICursor cur = null;
			try {
				String sql = "SELECT " + TblArtists.ARTIST_ID + "," + TblArtists.ME_ARTIST_ID + " FROM " + TblArtists.TBL_NAME;
				cur = execSelect(sql, new String[] {});
				List<Pair<Integer, Integer>> artists = new LinkedList<Pair<Integer, Integer>>();

				while (cur.moveToNext()) {
					artists.add(new Pair<Integer, Integer>(cur.getInt(0), cur.getInt(1)));
				}

				if (cur != null) {
					cur.close();
					cur = null;
				}
				beginTransaction();
				try {
					for (Pair<Integer, Integer> artist : artists) {
						ContentValues cv = createContentValues();
						cv.put(TblArtistCoords.ME_ARTIST_ID, artist.second);
						update(TblArtists.TBL_NAME, cv, TblArtistCoords.ARTIST_ID + " = " + artist.first, null);
					}
					setTransactionSuccessful();
				} finally {
					endTransaction();
				}

			} finally {
				if (cur != null) {
					cur.close();
				}
			}
			Log.v(TAG, "exchanging TblArtistCoords ...");
		} catch (UncheckedSqlException e) {
			Log.w(TAG, e);
		}
	}

	private void createTblKeyValue7() {
		Log.v(TAG, "Creating table " + TblKeyValue.TBL_NAME + " ...");

		execSQL(TblKeyValue.getCreateSql());
	}

	private void createTblRating7() {
		Log.v(TAG, "Creating table " + TblRating.TBL_NAME + " ...");

		execSQL(TblRating.getCreateSql7());
	}

	private void createRatingsFromPlayLog7() {
		Log.v(TAG, "Importing ratings from playlog ...");

		List<String> sqls = TblRating.getCreateRatingsFromPlayLog7Sql();
		for (String sql : sqls) {
			execSQL(sql);
		}
	}

	private void createTblLogEntry8() {
		Log.v(TAG, "Creating table " + TblLogEntry.TBL_NAME + " ...");

		execSQL(TblLogEntry.getCreateSql8());
	}

	private void adaptTblRating9() {
		Log.v(TAG, "adapting TblRating ...");

		List<String> sqls = TblRating.getUpdateTo9();
		for (String sql : sqls) {
			execSQL(sql);
		}
	}

	@SuppressWarnings("deprecation")
	private void dropViewRating9() {
		Log.v(TAG, "Dropping ViewRating ...");

		List<String> sqls = ViewRating.getDrop9();
		for (String sql : sqls) {
			execSQL(sql);
		}
	}

	/**
	 * There was a bug before v10, that fake playlog- and rating data was written async in
	 * {@link NextSongCalculationThread}. This led to entries in the db that survived. We remove them here.
	 */
	private void removeWrongRatingAndPlayLogEntries10() {
		// Remove the playlog entries
		execSQL("DELETE FROM tblPlayLog " +
				"WHERE (playLogId IN (" +
				"  SELECT DISTINCT playLogId " +
				"  FROM tblPlayLog AS p " +
				"    JOIN tblRating AS r ON (r.timestamp = p.timestamp) " +
				"  WHERE (CAST ((r.rating * 1000) AS INT) / 1000.0 = 0.639) " + // positive prediction, cut after 3 decimal places
				"     OR (CAST ((r.rating * 1000) AS INT) / 1000.0 = -0.680)" + // negative prediction, cut after 3 decimal places
				"));");
		// Remove the rating entries
		execSQL("DELETE FROM tblRating " +
				"WHERE (CAST ((rating * 1000) AS INT) / 1000.0 = 0.639) " + // positive prediction, cut after 3 decimal places
				"   OR (CAST ((rating * 1000) AS INT) / 1000.0 = -0.680)"); // negative prediction, cut after 3 decimal places
	}
}
