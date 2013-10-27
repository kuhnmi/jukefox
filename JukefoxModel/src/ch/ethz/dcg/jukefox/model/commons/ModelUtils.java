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
package ch.ethz.dcg.jukefox.model.commons;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class ModelUtils {

	public static boolean equals(float[] p1, float[] p2) {
		if (p1 == null) {
			return p2 == null;
		}
		if (p2 == null) {
			return false;
		}
		if (p1.length != p2.length) {
			return false;
		}
		for (int i = 0; i < p1.length; i++) {
			if (p1[i] != p2[i]) {
				return false;
			}
		}
		return true;
	}

	public static int getHashCode(float[] p) {
		if (p == null) {
			return 0;
		}
		int hash = 0;
		for (Float f : p) {
			hash ^= f.hashCode();
		}
		return hash;
	}

	public static List<String> listFilesRecursive(String startPath, FilenameFilter filter) {
		List<String> files = new ArrayList<String>();
		File start = new File(startPath);
		addFilesRecursive(files, start, filter);
		return files;
	}

	public static void addFilesRecursive(List<String> files, File location, FilenameFilter filter) {
		if (!location.exists()) {
			return;
		}

		if (!location.isDirectory()) {
			if (filter.accept(location.getParentFile(), location.getName())) {
				files.add(location.getAbsolutePath());
			}
		}

		// we are in a directory => add all files matching filter and then
		// recursively add all files in subdirectories
		File[] tmp = location.listFiles(filter);
		if (tmp != null) {
			for (File file : tmp) {
				files.add(file.getAbsolutePath());
			}
		}

		File[] dirs = location.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		if (dirs == null) {
			return;
		}
		for (File dir : dirs) {
			addFilesRecursive(files, dir, filter);
		}
	}

}
