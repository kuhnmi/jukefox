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
package ch.ethz.dcg.jukefox.model.collection;

import java.util.Date;

// TODO: make the parsing part of this class a PlaylistParser that is 
// instantiated (in the model) when the user clicks import playlist
public class ImportedPlaylist extends Playlist {

	@SuppressWarnings("unused")
	private final static String TAG = ImportedPlaylist.class.getSimpleName();

	private String path;
	private Date dateAdded;
	private Date dateModified;

	private int unrecognizedLineCnt;
	private int invalidSongPathCnt;
	private int urlCnt;
	private int embeddedPlaylistCnt;
	private int windowsPathCnt;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Date getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}

	public Date getDateModified() {
		return dateModified;
	}

	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}

	public void incInvalidSongPathCnt() {
		invalidSongPathCnt++;
	}

	public void incUnrecognizedLineCnt() {
		unrecognizedLineCnt++;
	}

	public void incUrlCnt() {
		urlCnt++;
	}

	public void incWindowsPathCnt() {
		windowsPathCnt++;
	}

	public void incEmbeddedPlaylistCnt() {
		embeddedPlaylistCnt++;
	}

	public int getErrorCount() {
		return embeddedPlaylistCnt + windowsPathCnt + urlCnt + unrecognizedLineCnt + invalidSongPathCnt;
	}

	public int getUrlCnt() {
		return urlCnt;
	}

	public int getWindowsPathCnt() {
		return windowsPathCnt;
	}

	public int getInvalidSongPathCnt() {
		return invalidSongPathCnt;
	}

	public int getEmbeddedPlaylistCnt() {
		return embeddedPlaylistCnt;
	}

	public int getUnrecognizedLineCnt() {
		return unrecognizedLineCnt;
	}
}
