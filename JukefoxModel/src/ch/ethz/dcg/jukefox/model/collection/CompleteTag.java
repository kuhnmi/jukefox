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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.ethz.dcg.jukefox.commons.Constants;

/**
 * Class to hold all informations about a tag
 * 
 * @author saaam
 * 
 */
public class CompleteTag extends BaseTag {

	private int meId;
	private Float variancePlsaProb;
	private Float variancePcaSpace;
	private Float meanPlsaProb;
	private Float meanPcaSpaceX;
	private Float meanPcaSpaceY;
	private float[] plsaCoords;
	private float[] plsaMeanSongCoords;
	private boolean isMapTag;

	public CompleteTag(int id, String name, int meId, Float variancePlsaProb, Float variancePcaSpace,
			Float meanPlsaProb, Float meanPcaSpaceX, Float meanPcaSpaceY, float[] plsaCoords, boolean isMapTag) {
		super(id, name);
		this.meId = meId;
		this.variancePlsaProb = variancePlsaProb;
		this.variancePcaSpace = variancePcaSpace;
		this.meanPlsaProb = meanPlsaProb;
		this.meanPcaSpaceX = meanPcaSpaceX;
		this.meanPcaSpaceY = meanPcaSpaceY;
		this.plsaCoords = plsaCoords;
		this.isMapTag = isMapTag;
		this.plsaMeanSongCoords = new float[Constants.DIM];
	}

	public boolean isMapTag() {
		return isMapTag;
	}

	public void setMapTag(boolean isMapTag) {
		this.isMapTag = isMapTag;
	}

	public int getMeId() {
		return meId;
	}

	public void setMeId(int meId) {
		this.meId = meId;
	}

	public Float getVariancePlsaProb() {
		return variancePlsaProb;
	}

	public void setVariancePlsaProb(Float variancePlsaProb) {
		this.variancePlsaProb = variancePlsaProb;
	}

	public Float getVariancePcaSpace() {
		return variancePcaSpace;
	}

	public void setVariancePcaSpace(Float variancePcaSpace) {
		this.variancePcaSpace = variancePcaSpace;
	}

	public Float getMeanPlsaProb() {
		return meanPlsaProb;
	}

	public void setMeanPlsaProb(Float meanPlsaProb) {
		this.meanPlsaProb = meanPlsaProb;
	}

	public Float getMeanPcaSpaceX() {
		return meanPcaSpaceX;
	}

	public void setMeanPcaSpaceX(Float meanPcaSpaceX) {
		this.meanPcaSpaceX = meanPcaSpaceX;
	}

	public Float getMeanPcaSpaceY() {
		return meanPcaSpaceY;
	}

	public void setMeanPcaSpaceY(Float meanPcaSpaceY) {
		this.meanPcaSpaceY = meanPcaSpaceY;
	}

	public float[] getPlsaCoords() {
		return plsaCoords;
	}

	public void setPlsaCoords(float[] plsaCoords) {
		this.plsaCoords = plsaCoords;
	}

	/**
	 * Constructor to read a tag information from a file stream
	 * 
	 * @param stream
	 * @throws Exception
	 */
	public CompleteTag(DataInputStream dis) throws IOException {
		super(0, null); // will immediately be overridden
		name = dis.readUTF();
		id = dis.readInt();

		variancePlsaProb = dis.readFloat();
		variancePcaSpace = dis.readFloat();
		meanPlsaProb = dis.readFloat();
		meanPcaSpaceX = dis.readFloat();
		meanPcaSpaceY = dis.readFloat();

		plsaCoords = new float[Constants.DIM];
		for (int i = 0; i < Constants.DIM; i++) {
			plsaCoords[i] = dis.readFloat();
		}

	}

	/**
	 * Write all important variable to a datastream
	 * 
	 * @param stream
	 *            stream to write in
	 * @throws IOException
	 */
	public void writeToStream(DataOutputStream dis) throws IOException {

		if (name == null) {
			name = "";
		}
		dis.writeUTF(name);

		dis.writeInt(id);

		dis.writeFloat(variancePlsaProb);
		dis.writeFloat(variancePcaSpace);
		dis.writeFloat(meanPlsaProb);
		dis.writeFloat(meanPcaSpaceX);
		dis.writeFloat(meanPcaSpaceY);

		for (int i = 0; i < Constants.DIM; i++) {
			dis.writeFloat(plsaCoords[i]);
		}

	}

	public float[] getPcaCoords() {
		return new float[] { meanPcaSpaceX, meanPcaSpaceY };
	}

	public float[] getPlsaMeanSongCoords() {
		return plsaMeanSongCoords;
	}

	public void setPlsaMeanSongCoords(float[] plsaMeanSongCoords) {
		this.plsaMeanSongCoords = plsaMeanSongCoords;
	}

}
