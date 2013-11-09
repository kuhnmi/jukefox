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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class ImportStatistics {

	public static final String TAG = ImportStatistics.class.getSimpleName();

	private String hash;
	private String jukefoxVersion;
	private String phoneModel;
	private String androidVersion;
	private boolean clearDb;
	private boolean reduced;
	private boolean hadChanges;
	private long startTime;
	private long endTime;
	private int numberOfStartedImports;
	private int numberOfCompletedImports;
	private int numberOfSongs;
	private int numberOfSongsWithCoords;

	private Throwable throwable;

	public void setHash(String hash) {
		this.hash = hash;
	}

	public void setJukefoxVersion(String jukefoxVersion) {
		this.jukefoxVersion = jukefoxVersion;
	}

	public void setPhoneModel(String phoneModel) {
		this.phoneModel = phoneModel;
	}

	public void setAndroidVersion(String androidVersion) {
		this.androidVersion = androidVersion;
	}

	public void setClearDb(boolean clearDb) {
		this.clearDb = clearDb;
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public void setHadChanges(boolean hadChanges) {
		this.hadChanges = hadChanges;
	}

	public void setNumberOfStartedImports(int numberOfStartedImports) {
		this.numberOfStartedImports = numberOfStartedImports;
	}

	public void setNumberOfCompletedImports(int numberOfCompletedImports) {
		this.numberOfCompletedImports = numberOfCompletedImports;
	}

	public void setNumberOfSongs(int numberOfSongs) {
		this.numberOfSongs = numberOfSongs;
	}

	public void setNumberOfSongsWithCoords(int numberOfSongsWithCoords) {
		this.numberOfSongsWithCoords = numberOfSongsWithCoords;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public boolean hadChanges() {
		return hadChanges;
	}

	public boolean isReduced() {
		return reduced;
	}

	public String getStatsString() {
		StringBuilder sb = new StringBuilder();

		sb.append("hash: " + hash + "\n");
		sb.append("jukefox version: " + jukefoxVersion + "\n");
		sb.append("phoneModel: " + phoneModel + "\n");
		sb.append("android version: " + androidVersion + "\n");
		sb.append("clearDb: " + clearDb + "\n");
		sb.append("reduced: " + reduced + "\n");
		sb.append("hadChanges: " + hadChanges + "\n");
		sb.append("startTime: " + startTime + "\n");
		sb.append("endTime: " + endTime + "\n");
		sb.append("time: " + (endTime - startTime) + "\n");
		sb.append("number of started imports: " + numberOfStartedImports + "\n");
		sb.append("number of completed imports: " + numberOfCompletedImports + "\n");
		sb.append("number of songs: " + numberOfSongs + "\n");
		sb.append("number of songs with coords: " + numberOfSongsWithCoords + "\n");
		if (throwable != null) {
			sb.append("exception message: " + throwable.getMessage() + "\n");
			StackTraceElement[] trace = throwable.getStackTrace();
			for (StackTraceElement el : trace) {
				sb.append("   " + el.toString() + "\n");
			}
		}
		String statsString = sb.toString();
		Log.v(TAG, statsString);
		return statsString;
	}

	public void setThrowable(Throwable e) {
		this.throwable = e;
	}

}
