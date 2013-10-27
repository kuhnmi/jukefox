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

import java.io.File;

public class Constants {

	// -----=== COMMONS ===-----

	public static final String FS = File.separator;
	public static final int DIM = 32;
	public static final long ONE_DAY = 1000 * 60 * 60 * 24;
	public static final int SAME_SONG_AVOIDANCE_NUM = 50;

	// -----=== SPECIFICATIONS ===-----

	public static final String PLAYLIST_EXT_INFO_PREFIX = "#EXTMUSEEKINFO";
	public static final long DB_ACTIVATED_PLAY_LOG_SIZE = 2500;

	// versions
	public static final int DB_VERSION = 10;
	public static final int PLAY_LOG_VERSION = 5;

	// resource loader
	public final static int NUM_TAGS = 500;
	public final static int NUM_ARTISTS = 2000;

	// locals
	public static final String UNKOWN_TITLE = "Unknown Title";
	public static final String UNKOWN_ARTIST = "Unknown Artist";
	public static final String UNKOWN_ALBUM = "Unknown Album";
	public static final String ALBUM_ARTIST_ALIAS = "Various Artists";

	// -----=== HTTP CONNECTION ===-----

	public static final int CONNECTION_TIMEOUT = 20000;

	public static final String SERVICES_BASE_URL = "http://jukefox.org/services/3/";
	public static final String HELP_URL = "http://www.jukefox.org/index.php/help";
	public static final String FORMAT_IMAGE_URL_REQUEST_PER_SONG = SERVICES_BASE_URL + "getImageURL.php?titleId=%s";
	public static final String FORMAT_IMAGE_URL_REQUEST_PER_ALBUM = SERVICES_BASE_URL
			+ "getImageURLByAlbum.php?artistId=%s&album=%s";
	public static final String FORMAT_PLAY_LOG_URL = SERVICES_BASE_URL + "extPlayLog.php";
	public static final String FORMAT_IMPORT_STATS_URL = SERVICES_BASE_URL + "importStats.php";
	public static final String FORMAT_LOG2_URL = SERVICES_BASE_URL + "log2/log2.php";

	public static final String FORMAT_COORDS_REQUEST_PACKAGE_NOXML = SERVICES_BASE_URL
			+ "getCoords.php?useLastFm=true&";
	public static final String FORMAT_IMAGE_URL_REQUEST_NO_XML = SERVICES_BASE_URL + "getImageURL.php?titleId=%s";
	public static final String FORMAT_COORD_VERSION_REQUEST = SERVICES_BASE_URL + "getCoordVersion.php";
	public static final String FORMAT_LOG_URL = SERVICES_BASE_URL + "log.php";
	public static final String FORMAT_ERROR_LOG_URL = SERVICES_BASE_URL + "errorLog.php";
	public static final String MUSICEXPLORER_EMAIL = "musicexplorer@tik.ee.ethz.ch";

	// -----=== STATISTICS ===-----

	/**
	 * Minimum weight that we consider it as interresting. Weights below this value can be dropped since they most
	 * likely get outvoted anyway.
	 */
	public static final double INTERRESTING_WEIGHT = 0.01d;
}
