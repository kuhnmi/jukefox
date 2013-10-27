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
package ch.ethz.dcg.jukefox.commons.utils;

public class AndroidLogPrinter implements ILogPrinter {

	public static final String TAG = AndroidLogPrinter.class.getSimpleName();

	@Override
	public void printAssert(String tag, String msg) {
		android.util.Log.e(tag + " WTF:", msg);
	}

	@Override
	public void printDebug(String tag, String msg) {
		android.util.Log.d(tag, msg);
	}

	@Override
	public void printError(String tag, String msg) {
		android.util.Log.e(tag, msg);
	}

	@Override
	public void printInfo(String tag, String msg) {
		android.util.Log.i(tag, msg);
	}

	@Override
	public void printVerbose(String tag, String msg) {
		android.util.Log.v(tag, msg);
	}

	@Override
	public void printWarning(String tag, String msg) {
		android.util.Log.w(tag, msg);
	}
}
