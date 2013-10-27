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
package ch.ethz.dcg.pancho3.view.youtube;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class Query {

	private final static String TAG = Query.class.getSimpleName();
	private DefaultHttpClient httpClient;
	public static final int CONNECTION_TIMEOUT = 20000;
	public String artist;
	public String song;
	private static final String VIDEO_FEED = "http://gdata.youtube.com/feeds/mobile/videos";

	public Query(String artist, String song) {

		this.artist = artist;
		this.song = song;
		initializeHttpClient();
	}
	
	public Query(String artist){
		this.artist = artist;
		this.song = "";
		initializeHttpClient();
	}

	private void initializeHttpClient() {
		// Initialize Http client
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
		httpClient = new DefaultHttpClient(params);

		HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(
				2, true);
		httpClient.setHttpRequestRetryHandler(retryHandler);
	}

	public String getQueryURL() throws UnsupportedEncodingException {
		return VIDEO_FEED + "?" + "q=" + URLEncoder.encode(artist, "UTF-8")
				+ "+" + URLEncoder.encode(song, "UTF-8")
				+ "+live&max-results=20&category=Music&v=2";
	}

	public String getVideosFromYouTubeServer() throws Exception {
		String urlStr = getQueryURL();
		Log.v(TAG, urlStr);
		InputStream is = null;
		StringBuilder contents = new StringBuilder();

		try {
			is = getVideosInputStreamFromUrl(urlStr);
			BufferedReader bufread = new BufferedReader(new InputStreamReader(
					is));
			try {
				String url = null;
				while (( url = bufread.readLine()) != null){
			          contents.append(url);
			          contents.append(System.getProperty("url.separator"));
				 }
		      }
		      finally {
		        bufread.close();
		      }
		    }
		    catch (Exception ex){
		      Log.w(TAG, ex);
		    }
		

			return contents.toString();

	}

	public InputStream getVideosInputStreamFromUrl(String urlStr) throws Exception {
	//	String urlStr = getQueryURL();
		Log.v(TAG, urlStr);
		HttpGet httpGet = new HttpGet(urlStr);
		Log.v(TAG, "httpGet has been created");
		HttpResponse httpResp = httpClient.execute(httpGet);
		Log.v(TAG, "httpResp has been created");
		HttpEntity httpEntity = httpResp.getEntity();
		Log.v(TAG, "httpEntity has been created");
		InputStream is = httpEntity.getContent();
		Log.v(TAG, "InputStream has been created: "+is);
		return is;
	}

}
