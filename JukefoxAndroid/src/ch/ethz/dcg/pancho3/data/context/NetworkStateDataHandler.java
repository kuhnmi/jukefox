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
package ch.ethz.dcg.pancho3.data.context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateDataHandler extends
		SensorDataHandler<String, NetworkInfo> {

	public static final String TAG = NetworkStateDataHandler.class
			.getSimpleName();

	public NetworkStateDataHandler() {

	}

	@Override
	public void onNewEvent(NetworkInfo event) {
		if (event == null) {
			return;
		}
//		Log.v(TAG, "NewNetwork Event Type:" + event.getType()
//				+ ", isConnected: " + event.isConnected());
		long time = System.currentTimeMillis();
		if (event.isConnected()) {
			int type = event.getType();
			if (type == ConnectivityManager.TYPE_WIFI) {
//				Context ctx = JukefoxApplication.getAppContext();
//				WifiManager wm = (WifiManager) ctx
//						.getSystemService(Context.WIFI_SERVICE);
//				try {
//					WifiInfo info = wm.getConnectionInfo();
//					String ssid = info.getSSID();
//					Log.v(TAG, "BSSID: " + info.getBSSID() + ", SSID: "
//							+ info.getSSID());
//					ssid = "<WLAN>" + ssid;
//					appendSensorData(ssid, time);
//				} catch (Exception e) {
//					Log.w(TAG, e);
//				}			
				appendSensorData("W", time);	// WIFI
			} else if (type == ConnectivityManager.TYPE_MOBILE) {
				appendSensorData("M", time); // Mobile
			} else {
				appendSensorData("U", time); // Unknown Connection
			}
		} else {
			appendSensorData("N", time);  // No connection
		}
	}

	@Override
	public String getMeanValue(int millisBack) {
		return getLatestValue();
	}

}
