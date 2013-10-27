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
package ch.ethz.dcg.jukefox.data;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import ch.ethz.dcg.jukefox.commons.Constants;

public class HttpHelper {

	public static DefaultHttpClient createHttpClientWithDefaultSettings() {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, Constants.CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, Constants.CONNECTION_TIMEOUT);
		DefaultHttpClient httpClient = new DefaultHttpClient(params);
		HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(2, true);
		httpClient.setHttpRequestRetryHandler(retryHandler);
		return httpClient;
	}

	public static boolean isNetworkException(Exception e) {
		if (e == null) {
			return false;
		}
		if (e instanceof SocketException) {
			return true;
		}
		if (e instanceof SocketTimeoutException) {
			return true;
		}
		if (e instanceof UnknownHostException) {
			return true;
		}
		return false;
	}

}
