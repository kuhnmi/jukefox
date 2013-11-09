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
package ch.ethz.dcg.pancho3.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.ImportedPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;

/**
 * Imports playlists that were not saved by jukefox
 * 
 * @author kuhnmi
 * 
 */
public class PlaylistImporter {

	private final static String TAG = PlaylistImporter.class.getSimpleName();

	public static class PlaylistInfo {

		protected final String path;
		protected final String name;
		protected final Date dateModified;
		protected final Date dateAdded;

		public PlaylistInfo(String path, String name, Date dateModified, Date dateAdded) {
			this.path = path;
			this.name = name;
			this.dateModified = dateModified;
			this.dateAdded = dateAdded;
		}

		public String getPath() {
			return path;
		}

		public String getName() {
			return name;
		}

		public Date getDateModified() {
			return dateModified;
		}

		public Date getDateAdded() {
			return dateAdded;
		}

		@Override
		public String toString() {
			return path;
		}
	}

	private AndroidCollectionModelManager collectionModel;

	public PlaylistImporter(AndroidCollectionModelManager collectionModel) {
		this.collectionModel = collectionModel;
	}

	public List<PlaylistInfo> getPlaylists() {
		String[] projection = new String[] { MediaStore.Audio.PlaylistsColumns.NAME,
				MediaStore.Audio.PlaylistsColumns.DATA, MediaStore.Audio.PlaylistsColumns.DATE_ADDED,
				MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED, };
		ContentResolver cr = JukefoxApplication.getAppContext().getContentResolver();
		List<PlaylistInfo> playlists = new ArrayList<PlaylistInfo>();
		playlists.addAll(getPlaylists(projection, cr, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI));
		Log.v(TAG, "Number of external playlists found: " + playlists.size());
		try {
			playlists.addAll(getPlaylists(projection, cr, MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI));
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
		Log.v(TAG, "Number of playlists (internal + external) found: " + playlists.size());
		return playlists;
	}

	private List<PlaylistInfo> getPlaylists(String[] projection, ContentResolver cr, Uri uri) {
		List<PlaylistInfo> playlists = new ArrayList<PlaylistImporter.PlaylistInfo>();
		Cursor cur = null;
		try {
			cur = cr.query(uri, projection, null, null, null);
			while (cur.moveToNext()) {
				String name = cur.getString(0);
				String data = cur.getString(1);
				if (!isValidFile(data)) {
					continue;
				}
				Date dateAdded = null;
				Date dateModified = null;
				try {
					String dateAddedStr = cur.getString(2);
					String dateModifiedStr = cur.getString(3);
					dateAdded = new Date(dateAddedStr);
					dateModified = new Date(dateModifiedStr);
				} catch (Exception e) {
					Log.w(TAG, e);
				}
				playlists.add(new PlaylistInfo(data, name, dateModified, dateAdded));
			}
			return playlists;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private boolean isValidFile(String data) {
		if (data == null) {
			return false;
		}
		File file = new File(data);
		return file.exists() && !file.isDirectory();
	}

	public ImportedPlaylist parse(PlaylistInfo info) throws IOException {
		Log.v(TAG, "parse");
		String parentPath = getParentPathOrRoot(info.getPath());
		ImportedPlaylist playlist = new ImportedPlaylist();
		playlist.setPlaylistName(info.getName());
		playlist.setPath(info.getPath());
		playlist.setDateAdded(info.getDateAdded());
		parsePlaylistFile(playlist, info.getPath(), parentPath);
		return playlist;
	}

	private String getParentPathOrRoot(String path) {
		String parentPath = new File(path).getParent();
		if (parentPath == null) { // root (TODO: correct?)
			parentPath = "/";
		} else {
			parentPath = new File(parentPath).getAbsolutePath();
		}
		return parentPath;
	}

	private void parsePlaylistFile(ImportedPlaylist playlist, String path, String parentPath)
			throws FileNotFoundException, IOException {
		FileReader fileReader = new FileReader(path);
		BufferedReader br = new BufferedReader(fileReader);
		Log.v(TAG, "reading playlist at: " + path);
		try {
			String line;
			while ((line = br.readLine()) != null) {
				try {
					Log.v(TAG, "parsing line: " + line);
					String convertedLine = convertLine(playlist, parentPath, line);
					// Log.v(TAG, "converted: " + convertedLine);
					if (convertedLine == null) {
						continue;
					}
					if (!AndroidUtils.fileExists(convertedLine)) {
						playlist.incInvalidSongPathCnt();
						continue;
					}
					PlaylistSong<BaseArtist, BaseAlbum> song = collectionModel.getSongProvider().getSongForPath(
							convertedLine, true);
					if (song == null) {

						// Check if song path is misspelled (case - Windows
						// systems ignore this)
						song = collectionModel.getSongProvider().getSongForPath(convertedLine, false);
					}
					if (song == null) {
						Log.v(TAG, "no song found for path: " + convertedLine);
						playlist.incInvalidSongPathCnt();
						continue;
					}
					playlist.appendSongAtEnd(song);
				} catch (Exception e) {
					e.printStackTrace();
					playlist.incUnrecognizedLineCnt();
				}
			}
		} finally {
			br.close();
			fileReader.close();
		}
	}

	private String convertLine(ImportedPlaylist playlist, String parentPath, String line) {
		if (line == null) {
			playlist.incUnrecognizedLineCnt();
			return null;
		}
		if (line.trim().equals("") || line.trim().startsWith("#")) {
			return null;
		}
		if (line.trim().startsWith("http://")) {
			playlist.incUrlCnt();
			return null; // url
		}
		if (line.trim().substring(1, 3).equals(":\\")) {
			playlist.incWindowsPathCnt();
			return null; // Windows absolute path
		}
		String[] tokens = line.split("#");
		if (tokens[0].trim().endsWith(".m3u")) {
			playlist.incEmbeddedPlaylistCnt();
		}

		if (line.trim().startsWith("/")) {
			// absolute path => should also work on jukefox
			return line.trim();
		}
		if (line.trim().startsWith("../")) {
			while (line.trim().startsWith("../")) {
				parentPath = getParentPathOrRoot(parentPath);
				line = line.trim().substring(3).trim();
			}
			return parentPath + "/" + line;
		}
		if (line.trim().startsWith("./")) {
			return parentPath + "/" + line.substring(2);
		}
		return parentPath + "/" + line.trim();
	}
}
