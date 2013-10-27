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
package ch.ethz.dcg.jukefox.commons;

public class AndroidConstants {

	// -----=== COMMONS ===-----

	public static final int VIBRATION_DURATION = 25;
	public static final String DB_NAME = "museekDb";
	public static final boolean THROW_METHOD_STUB_EXCEPTIONS = false;
	public static final int SHARED_PREF_VERSION = 1;

	// -----=== SPECIFICATIONS ===-----

	public static final String PLAYLIST_EXT_INFO_PREFIX = "#EXTMUSEEKINFO";

	// TODO should be placed somewhere else
	public static final long DB_ACTIVATED_PLAY_LOG_SIZE = 2500;

	// versions
	public static final int DB_VERSION = 1;
	public static final int PLAY_LOG_VERSION = 4;

	// resource loader
	public final static int NUM_TAGS = 500;
	public final static int NUM_ARTISTS = 2000;

	// locals
	public static final String UNKOWN_TITLE = "&lt;unknown title&gt;";
	public static final String UNKOWN_ARTIST = "&lt;unknown artist&gt;";
	public static final String UNKOWN_ALBUM = "&lt;unknown album&gt;";
	public static final String ALBUM_ARTIST_ALIAS = "Various Artists";

	// -----=== HTTP CONNECTION ===-----

	public static final int CONNECTION_TIMEOUT = 20000;

	// -----=== PLAYER ===-----

	public static final String CURRENT_PLAYLIST_NAME = "onTheFly";

	public static final int COVER_SIZE_LOW_RES = 128;
	public static final int COVER_SIZE_HIGH_RES = 256;
	public static final String PLAYER_MODEL_NAME = "androidPlayModel";

}
