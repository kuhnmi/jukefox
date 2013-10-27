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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;

public class LibraryImportBlacklistManager {

	private static final String TAG = LibraryImportBlacklistManager.class.getSimpleName();

	public static void appendFileBlacklistPath(DirectoryManager directoryManager, String path) {
		File dirFile = directoryManager.getMusicFilesBlacklistFile();
		FileOutputStream fileOutput = null;
		DataOutputStream dirStream = null;
		try {

			fileOutput = new FileOutputStream(dirFile, true);
			dirStream = new DataOutputStream(fileOutput);
			dirStream.writeBytes(path + "\n");

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirStream.close();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}
}
