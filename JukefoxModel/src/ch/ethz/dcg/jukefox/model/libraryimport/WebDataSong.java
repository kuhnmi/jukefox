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

import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.CompleteArtist;
import ch.ethz.dcg.jukefox.model.collection.SongStatus;

public class WebDataSong {

	private int id;
	private String name;
	private Integer meId;
	private String meName;
	private float[] coords;
	private SongStatus status;
	private CompleteArtist artist;
	private int albumId;
	private AlbumStatus albumStatus;
	private boolean hasStatusChanged;

	public WebDataSong(int id, String name, Integer meId, String meName, float[] coords, SongStatus status,
			CompleteArtist artist, int albumId, AlbumStatus albumStatus) {
		super();
		this.id = id;
		this.name = name;
		this.meId = meId;
		this.meName = meName;
		this.coords = coords;
		this.status = status;
		this.artist = artist;
		this.albumId = albumId;
		this.albumStatus = albumStatus;
		hasStatusChanged = false;
	}

	public SongStatus getStatus() {
		return status;
	}

	public int getAlbumId() {
		return albumId;
	}

	public void setAlbumId(int albumId) {
		this.albumId = albumId;
	}

	public AlbumStatus getAlbumStatus() {
		return albumStatus;
	}

	public void setAlbumStatus(AlbumStatus albumStatus) {
		this.albumStatus = albumStatus;
	}

	public void setMeId(Integer meId) {
		this.meId = meId;
	}

	public void setStatus(SongStatus status) {
		if (this.status != status) {
			hasStatusChanged = true;
		}
		this.status = status;
	}

	public Integer getMeId() {
		return meId;
	}

	public void setMeId(int meId) {
		this.meId = meId;
	}

	public String getMeName() {
		return meName;
	}

	public void setMeName(String meName) {
		this.meName = meName;
	}

	public float[] getCoords() {
		return coords;
	}

	public void setCoords(float[] coords) {
		this.coords = coords;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CompleteArtist getArtist() {
		return artist;
	}

	public void setArtist(CompleteArtist artist) {
		this.artist = artist;
	}

	public boolean isHasStatusChanged() {
		return hasStatusChanged;
	}

}
