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
package ch.ethz.dcg.jukefox.manager;

import ch.ethz.dcg.pancho3.commons.settings.AndroidSettings;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsEditor;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

/**
 * Manages the Android settings
 */
public class AndroidSettingsManager {

	private static AndroidSettings androidSettings = null;

	private static AndroidSettings getAndroidSettings() {
		if (androidSettings == null) {
			/*
			 * TODO: Is this the right context object?
			 */
			androidSettings = new AndroidSettings(JukefoxApplication.getAppContext());
		}
		return androidSettings;
	}

	/**
	 * Gets the Android settings editor
	 */
	public static ISettingsEditor getAndroidSettingsEditor() {
		return getAndroidSettings();
	}

	/**
	 * Gets the Android settings reader
	 */
	public static ISettingsReader getAndroidSettingsReader() {
		return getAndroidSettings();
	}

}
