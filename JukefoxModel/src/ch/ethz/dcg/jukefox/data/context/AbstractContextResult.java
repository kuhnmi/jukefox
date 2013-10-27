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
package ch.ethz.dcg.jukefox.data.context;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;

public abstract class AbstractContextResult {

	private static final String TAG = AbstractContextResult.class.getSimpleName();

	public static final String PLATFORM = "pl";
	public static final String ANDROID = "ad";
	public static final String PC = "pc";

	public static final String LIGHT = "li";
	public static final String PROXIMITY = "pr";
	public static final String ACCELERATION_ENERGY = "ae";
	public static final String ORIENTATION = "or";
	public static final String ORIONTATION_CHANGE = "oc";
	public static final String NETWORK_STATE = "ns";

	private Float light = null;
	private Float proximity = null;
	private Float accelerationEnergy = null;
	private Float orientationChange = null;
	private Integer orientation = null;
	private String networkState = null;

	public Float getOrientationChange() {
		return orientationChange;
	}

	public void setOrientationChange(Float orientationChange) {
		this.orientationChange = orientationChange;
	}

	public Float getLight() {
		return light;
	}

	public void setLight(Float light) {
		this.light = light;
	}

	public Float getProximity() {
		return proximity;
	}

	public void setProximity(Float proximity) {
		this.proximity = proximity;
	}

	public Float getAccelerationEnergy() {
		return accelerationEnergy;
	}

	public void setAccelerationEnergy(Float accelerationEnergy) {
		this.accelerationEnergy = accelerationEnergy;
	}

	public Integer getOrientation() {
		return orientation;
	}

	public void setOrientation(Integer orientation) {
		this.orientation = orientation;
	}

	public String getNetworkState() {
		return networkState;
	}

	public void setNetworkState(String networkState) {
		this.networkState = networkState;
	}

	/**
	 * Reads a dbString ({@link String}) and parse it to the correct values and
	 * set them
	 */
	public void parseDbString(String dbString) {
		// Normalize String
		dbString = dbString.trim();

		String name, value;
		int indexOfValue, indexOfNextName;

		// If something goes terrible wrong, this for-loop does maximal
		// dbString.length() loops
		for (int i = 0; i < dbString.length(); i++) {
			indexOfValue = dbString.indexOf("=");
			if (indexOfValue <= 0) {
				Log.w(TAG, "Couldn't parse: '" + dbString + "'. Missing '='!");
				break;
			}
			name = dbString.substring(0, dbString.indexOf("="));

			indexOfNextName = dbString.indexOf(";");
			if (indexOfNextName <= 0) {
				indexOfNextName = dbString.length();
			}

			value = dbString.substring(dbString.indexOf("=") + 1, indexOfNextName);

			parseString(name, value);

			if (indexOfNextName == dbString.length()) {
				break;
			} else {
				dbString = dbString.substring(indexOfNextName + 1);
			}
		}
	}

	private void parseString(String name, String value) {
		if (Utils.isNullOrEmpty(name, true) || Utils.isNullOrEmpty(value, true)) {
			return;
		} else if (name.compareTo(AbstractContextResult.ACCELERATION_ENERGY) == 0) {
			accelerationEnergy = Float.parseFloat(value);
		} else if (name.compareTo(AbstractContextResult.LIGHT) == 0) {
			light = Float.parseFloat(value);
		} else if (name.compareTo(AbstractContextResult.NETWORK_STATE) == 0) {
			networkState = value;
		} else if (name.compareTo(AbstractContextResult.ORIENTATION) == 0) {
			orientation = Integer.parseInt(value);
		} else if (name.compareTo(AbstractContextResult.ORIONTATION_CHANGE) == 0) {
			orientationChange = Float.parseFloat(value);
		} else if (name.compareTo(AbstractContextResult.PROXIMITY) == 0) {
			proximity = Float.parseFloat(value);
		} else if (name.compareTo(AbstractContextResult.PLATFORM) == 0) {
			// do nothing
		} else {
			Log.w(TAG, "Couldn't parse: '" + name + "' with value '" + value + "'");
		}
	}

	/**
	 * Creates a {@link String} which contains all necessary informations of
	 * this object
	 */
	public abstract String createDbString();

	protected void addAttribute(StringBuilder dbString, String name, Object value) {
		addAttribute(dbString, name, value, false);
	}

	protected void addAttribute(StringBuilder dbString, String name, Object value, boolean isFirst) {
		if (!isFirst) {
			dbString.append(";");
		}

		dbString.append(name);
		dbString.append("=");

		if (value != null) {
			dbString.append(value.toString());
		}
	}
}
