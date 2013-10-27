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

import java.util.List;

/**
 * Complete information about an album
 * 
 */
public class CompleteAlbum extends BaseAlbum {

	private float[] coordsPCA2D;
	private int color;
	private List<BaseArtist> artists;
	private List<BaseSong<BaseArtist, BaseAlbum>> songs;
	private AlbumStatus albumCoverStatus;

	public CompleteAlbum(int id, String name, float[] coordsPCA2D, int color, List<BaseArtist> artists,
			List<BaseSong<BaseArtist, BaseAlbum>> songs, AlbumStatus albumCoverStatus) {
		super(id, name);

		this.coordsPCA2D = coordsPCA2D;
		this.color = color;
		this.songs = songs;
		this.artists = artists;
		this.albumCoverStatus = albumCoverStatus;

	}

	public AlbumStatus getAlbumCoverStatus() {
		return albumCoverStatus;
	}

	public CompleteAlbum(BaseAlbum album, float[] coordsPCA2D, int color, List<BaseArtist> artists,
			List<BaseSong<BaseArtist, BaseAlbum>> songs, AlbumStatus albumCoverStatus) {
		super(album.getId(), album.getName());

		this.coordsPCA2D = coordsPCA2D;
		this.color = color;
		this.songs = songs;
		this.artists = artists;
		this.albumCoverStatus = albumCoverStatus;
	}

	public List<BaseArtist> getArtists() {
		return artists;
	}

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongs() {
		return songs;
	}

	public float[] getCoordsPCA2D() {
		return coordsPCA2D;
	}

	public int getColor() {
		return color;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
