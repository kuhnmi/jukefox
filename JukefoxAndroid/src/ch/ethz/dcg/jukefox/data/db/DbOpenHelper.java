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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import ch.ethz.dcg.jukefox.commons.utils.Log;

public class DbOpenHelper extends SQLiteOpenHelper {

	private boolean wasOnCreate = false;
	private boolean wasOnUpdate = false;
	private int oldVersion;
	private int newVersion;

	public DbOpenHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		wasOnCreate = false;
		oldVersion = 0;
		newVersion = 0;
	}

	private final static String TAG = "DBOpenHelper";

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "DbOpenHelper.onCreate() called.");
		wasOnCreate = true;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v(TAG, "DbOpenHelper.onUpgrade() called.");
		wasOnUpdate = true;
		this.oldVersion = oldVersion;
		this.newVersion = newVersion;
	}

	public boolean wasOnCreate() {
		return wasOnCreate;
	}

	public boolean wasOnUpdate() {
		return wasOnUpdate;
	}

	public int getOldVersion() {
		return oldVersion;
	}

	public int getNewVersion() {
		return newVersion;
	}
}
