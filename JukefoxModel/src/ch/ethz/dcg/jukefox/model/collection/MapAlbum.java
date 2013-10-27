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
 * Contains all information about an album to display it on a 2D map
 */
public class MapAlbum extends ListAlbum {

	private float[] gridCoords;
	private int color;

	public MapAlbum(int id, String name, float[] gridCoords, int color, List<BaseArtist> artists) {
		super(id, name, artists);
		this.gridCoords = gridCoords;
		setColor(color);
	}

	// public MapAlbum(MapAlbum album) {
	// super(album.getId(), album.getName(), album.getArtists());
	// this.color = album.color;
	// this.gridCoords = album.gridCoords;
	// }

	public MapAlbum(ListAlbum listAlbum) {
		super(listAlbum.getId(), listAlbum.getName(), listAlbum.getArtists());
	}

	public float[] getGridCoords() {
		return gridCoords;
	}

	public void setGridCoords(float[] gridCoords) {
		this.gridCoords = gridCoords;
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

	public void setColor(int color) {
		// if (color == Color.BLACK || color == 0) {
		// color = Color.GRAY;
		// }
		this.color = color;
	}

}
