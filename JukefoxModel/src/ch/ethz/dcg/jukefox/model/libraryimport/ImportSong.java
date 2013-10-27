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
package ch.ethz.dcg.jukefox.model.libraryimport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class ImportSong {

	private final static String TAG = ImportSong.class.getSimpleName();

	private String name;
	private String artist;
	private ImportAlbum album;
	private String path;
	private int duration;
	private int track;
	private ContentProviderId contentProviderId;
	private Integer jukefoxId;
	private List<String> genres;
	private Date importDate;

	public ImportSong(String name, ImportAlbum album, String artist, String path, int duration, int track,
			ContentProviderId contentProviderId, Integer jukefoxId, Date importDate) {
		setName(name);
		setArtist(artist);
		this.album = album;
		this.path = path;
		this.duration = duration;
		this.track = track;
		this.contentProviderId = contentProviderId;
		this.jukefoxId = jukefoxId;
		this.genres = new ArrayList<String>();
		this.importDate = importDate;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		try {
			ImportSong s = (ImportSong) o;
			if (name.equals(s.name) && album.equals(s.album) && artist.equals(s.artist) && path.equals(s.path)
					&& duration == s.duration && track == s.track) {
				return true;
			}
			return false;
		} catch (Exception e) {
			Log.w(TAG, e);
			Log.w(TAG, "returning result of super.equals()");
			return super.equals(o);
		}
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	public String getName() {
		return name;
	}

	public String getArtist() {
		return artist;
	}

	public ImportAlbum getAlbum() {
		return album;
	}

	public String getPath() {
		return path;
	}

	public int getDuration() {
		return duration;
	}

	public int getTrack() {
		return track;
	}

	public ContentProviderId getContentProviderId() {
		return contentProviderId;
	}

	public List<String> getGenres() {
		return genres;
	}

	public Integer getJukefoxId() {
		return jukefoxId;
	}

	public Date getImportDate() {
		return importDate;
	}

	public String getLogString() {
		StringBuilder sb = new StringBuilder();
		sb.append("title: " + name + ", artist: " + artist + "\n");
		sb.append("album: " + album.getLogString() + "\n");
		sb.append("path: " + path + ", duration: " + duration + ", track: " + track + "\n");
		sb.append("import timestamp: " + importDate.getTime());
		return sb.toString();
	}

	public void setName(String name) {
		this.name = name == null ? null : name.trim();
	}

	public void setArtist(String artist) {
		this.artist = artist == null ? null : artist.trim();
	}

	public void setAlbum(ImportAlbum album) {
		this.album = album;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public void setTrack(int track) {
		this.track = track;
	}

	public void setJukefoxId(Integer jukefoxId) {
		this.jukefoxId = jukefoxId;
	}

	public void addGenre(String genre) {
		genres.add(genre);
	}

	public void setImportDate(Date importDate) {
		this.importDate = importDate;
	}
}
