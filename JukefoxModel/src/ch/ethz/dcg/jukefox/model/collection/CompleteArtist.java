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

public class CompleteArtist extends BaseArtist {

	private Integer meId;
	private String meName;
	private float[] coords;

	public CompleteArtist(int id, String name, Integer meId, String meName, float[] coords) {
		super(id, name);
		this.meId = meId;
		this.meName = meName;
		this.coords = coords;
	}

	public CompleteArtist(BaseArtist baseArtist, Integer meId, String meName, float[] coords) {
		super(baseArtist.getId(), baseArtist.getName());
		this.meId = meId;
		this.meName = meName;
		this.coords = coords;
	}

	public int getMeId() {
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

}
