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
 * Complete information about a song
 * 
 */
public class CompleteSong<ArtistType extends BaseArtist, AlbumType extends BaseAlbum, TagType extends BaseTag> extends
		BaseSong<ArtistType, AlbumType> {

	private final String path;
	private final int musicexplorerId;
	private final int track;
	private final List<Genre> genres;
	private final TagType mostProbTag;
	private final float[] coords;
	private final float[] coordsPca2D;

	public CompleteSong(int id, String name, ArtistType artist, AlbumType album, String path, int musicexplorerId,
			int track, List<Genre> genres, TagType mostProbTag, int duration, float[] coords, float[] coordsPca2D) {
		super(id, name, artist, album, duration);
		this.path = path;
		this.musicexplorerId = musicexplorerId;
		this.track = track;
		this.genres = genres;
		this.mostProbTag = mostProbTag;
		this.coords = coords;
		this.coordsPca2D = coordsPca2D;
	}

	public String getPath() {
		return path;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public int getMusicexplorerId() {
		return musicexplorerId;
	}

	public int getTrack() {
		return track;
	}

	public List<Genre> getGenres() {
		return genres;
	}

	public TagType getMostProbTag() {
		return mostProbTag;
	}

	public float[] getCoords() {
		return coords;
	}

	public float[] getCoordsPca2D() {
		return coordsPca2D;
	}

}
