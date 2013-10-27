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
package ch.ethz.dcg.pancho3.view.webpublisher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.AndroidPlayerController;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IOnPlaylistStateChangeListener;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.playmode.IPlayMode;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

import com.googlecode.ascrblr.api.scrobbler.AudioscrobblerService;
import com.googlecode.ascrblr.api.scrobbler.TrackInfo;
import com.googlecode.ascrblr.api.scrobbler.TrackInfo.SourceType;
import com.googlecode.ascrblr.api.util.AuthenticationException;
import com.googlecode.ascrblr.api.util.ServiceException;
import com.googlecode.ascrblr.api.util.SessionExpiredException;

public class AudioScrobbler implements IOnPlayerStateChangeListener, IOnPlaylistStateChangeListener,
		OnSharedPreferenceChangeListener {

	public static final String ANSWER_OK = "OK";
	public static final String ANSWER_BAD_SESSION = "BADSESSION";
	private final static String TAG = AudioScrobbler.class.getSimpleName();
	private AudioscrobblerService service;
	private boolean credentialsAreSet = false;
	//	private TrackInfo track = null;
	private List<TrackInfo> bufferedTracks = null;
	/** Number of track titles to collect before sending them (0 is store all) **/
	private int numTracksToBuffer = 1;
	private boolean paused = false;
	private ISettingsReader settings;
	private IAudioScrobblerListener listener;
	private AndroidPlayerController controller;
	/** Maps the songId to the track object **/
	private HashMap<Integer, TrackInfo> tracks;

	public AudioScrobbler(IAudioScrobblerListener listener, AndroidPlayerController controller) {

		this.controller = controller;
		this.settings = AndroidSettingsManager.getAndroidSettingsReader();
		this.listener = listener;

		tracks = new HashMap<Integer, TrackInfo>();

		// Instantiating service (default protocol version is 1.2)
		service = new AudioscrobblerService("1.2");
		bufferedTracks = new ArrayList<TrackInfo>();
		readBufferFromFile();

		readSettings();

		settings.addSettingsChangeListener(this);

		init();
	}

	private void init() {
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				//				JukefoxApplication.getCollectionModel().getApplicationStateManager().getApplicationStateReader()
				//						.waitForPlaybackFunctionality();
				controller.addOnPlaylistStateChangeListener(AudioScrobbler.this);
				controller.addOnPlayerStateChangeListener(AudioScrobbler.this);
				BaseSong<BaseArtist, BaseAlbum> song;
				try {
					song = controller.getCurrentSong();
					setTrack(song);
				} catch (EmptyPlaylistException e) {
					Log.w(TAG, e);
				}
			}
		});
		t.start();
	}

	public void readSettings() {
		numTracksToBuffer = settings.getScrobbleInterval();
		paused = settings.isScrobblingPaused();
		setCredentials(settings.getLastFmUserName(), settings.getLastFmPassword());
		Log.v(TAG, "set scrobble credentials");
	}

	public boolean goodConnection() {
		boolean ret = true;
		try {
			service.testConnection();
		} catch (Exception e) {
			Log.w(TAG, e);
			ret = false;
		}
		return ret;
	}

	public boolean isInitialized() {
		return credentialsAreSet;
	}

	private void reHandshake() {
		setCredentials(settings.getLastFmUserName(), settings.getLastFmPassword());
	}

	public void setCredentials(String username, String password) {
		try {
			// Set credentials (initializes handshake to obtain session)
			// Log.v("Set scrobbler credentials to", "username: " + username +
			// ", password: " + password);
			service.setCredentials(username, password);
			credentialsAreSet = true;
			if (username == null || password == null || username.length() == 0 || password.length() == 0) {
				credentialsAreSet = false;
				Log.v(TAG, "credentials unset");
				return;
			}
			Log.v(TAG, "credentials set to: " + username + ", <some pwd>");
		} catch (ServiceException e) {
			Log.w(TAG, e);
			credentialsAreSet = false;
		} catch (Exception e) {
			Log.w(TAG, "unable to connect!");
			credentialsAreSet = false;
			Log.w(TAG, e);
		}

	}

	public synchronized void scrobbleSubmitAsync(BaseSong<BaseArtist, BaseAlbum> song) {

		if (!tracks.containsKey(song.getId())) {
			return;
		}
		final TrackInfo trackToSubmit = tracks.get(song.getId());

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				scrobbleSubmit(trackToSubmit);
			}

		});
		t.start();
	}

	public synchronized void scrobbleSubmit(TrackInfo trackToSubmit) {
		Log.v(TAG, "Scrobble Submit: credentials set: " + "" + credentialsAreSet);
		bufferedTracks.add(trackToSubmit);
		boolean authError = false;
		if (credentialsAreSet && !paused && bufferedTracks.size() >= numTracksToBuffer) {

			Log.v(TAG, "" + bufferedTracks.size() + " " + numTracksToBuffer);

			List<TrackInfo> sent = new ArrayList<TrackInfo>(bufferedTracks.size());

			for (TrackInfo track : bufferedTracks) {

				authError = serviceScrobbleSubmit(authError, track);

				if (service.postURL != null) {
					Log.v(TAG, "PostURL: " + service.postURL);

					// Create a new HttpClient and Post Header
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(service.postURL);

					try {

						// Execute HTTP Post Request
						HttpResponse response = httpclient.execute(httppost);
						InputStream is = response.getEntity().getContent();
						DataInputStream din = new DataInputStream(is);
						String responseState = din.readLine();
						Log.v(TAG, "HttpPost Response: " + responseState);
						if (responseState.equals(ANSWER_OK)) {
							sent.add(track);
						} else if (responseState.equals(ANSWER_BAD_SESSION)) {
							setCredentials(settings.getLastFmUserName(), settings.getLastFmPassword());
							authError = serviceScrobbleSubmit(authError, track);
							HttpPost httppost2 = new HttpPost(service.postURL);
							HttpResponse response2 = httpclient.execute(httppost2);
							InputStream is2 = response2.getEntity().getContent();
							DataInputStream din2 = new DataInputStream(is2);
							String responseState2 = din2.readLine();
							Log.v(TAG, "HttpPost Response: " + responseState2);
							if (responseState.equals(ANSWER_OK)) {
								sent.add(track);
							}
							Log.v(TAG, "PostURL: " + service.postURL);
						}
					} catch (ClientProtocolException e) {
						Log.w(TAG, e);
					} catch (IOException e) {
						Log.w(TAG, e);
					}
				}
			}

			for (TrackInfo ti : sent) {
				bufferedTracks.remove(ti);
			}

			if (authError && listener != null) {
				listener.onAuthenticationFailed();
			}
		} else if (!credentialsAreSet) {
			broadcastScrobbleSubmit();
		}
	}

	private boolean serviceScrobbleSubmit(boolean authError, TrackInfo track) {
		for (int i = 0; i < 2; i++) {
			try {

				// Submit the track information after the minimal amount of time
				// which is at least 31sec. or half the track length.
				if (track != null) {
					service.submit(track);
					// track = null;
				}
				break;

			} catch (SessionExpiredException e) {
				Log.w(TAG, e);
				reHandshake();
			} catch (AuthenticationException e) {
				// do some error handling here
				Log.w(TAG, "unable to scrobble ");
				Log.w(TAG, e);
				authError = true;
			} catch (ServiceException e) {
				// do some error handling here
				Log.w(TAG, "unable to scrobble ");
				Log.w(TAG, e);
			} catch (Exception e) {
				Log.w(TAG, "unable to scrobble ");
				Log.w(TAG, e);
			}
		}
		return authError;
	}

	private void broadcastScrobbleSubmit() {
		Intent i = new Intent("fm.last.android.playbackcomplete");
		JukefoxApplication.getAppContext().sendBroadcast(i);
		Log.v(TAG, "broadcasted scrobble submit intent");
	}

	public void setTrack(BaseSong<BaseArtist, BaseAlbum> song) {
		String artist = song.getArtist().getName();
		String album = song.getAlbum().getName();
		String title = song.getTitle();
		Integer id = song.getId();
		Log.v(TAG, "Set Track: " + artist + " - " + title);
		TrackInfo track = new TrackInfo(artist, title);
		track.setStartTime(Long.toString(System.currentTimeMillis()));
		track.setSource(SourceType.E);
		track.setAlbum(album);
		tracks.put(id, track);
		if (!credentialsAreSet) {
			broadcastSetTrack(track);
			Log.v(TAG, "broadcasted scrobble set track intent");
		}
	}

	private void broadcastSetTrack(TrackInfo track) {
		Intent i = new Intent("fm.last.android.metachanged");
		i.putExtra("artist", track.getArtist());
		i.putExtra("album", track.getAlbum());
		i.putExtra("track", track.getTrack());
		i.putExtra("duration", track.getLength());
		JukefoxApplication.getAppContext().sendBroadcast(i);
	}

	public synchronized void scrobbleNotifyAsync(final TrackInfo track) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				scrobbleNotify(track);
			}

		});
		t.start();
	}

	public void scrobbleNotify(TrackInfo track) {
		if (credentialsAreSet && numTracksToBuffer <= 1 && !paused) {
			for (int i = 0; i < 2; i++) {
				try {

					if (track != null) {
						// Notify the service which track you are currently
						// playing...
						service.notifyNew(track);
					}
					break;

				} catch (SessionExpiredException e) {
					Log.w(TAG, e);
					reHandshake();
				} catch (ServiceException e) {
					// do some error handling here
					Log.w(TAG, "unable to scrobble " + track.getArtist() + " - " + track.getTrack());
					Log.w(TAG, e);
				} catch (IOException e) {
					Log.w(TAG, "unable to scrobble " + track.getArtist() + " - " + track.getTrack());
					Log.w(TAG, e);
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	}

	public void writeBufferToFile() {
		if (numTracksToBuffer > 1) {
			File f = JukefoxApplication.getDirectoryManager().getScrobbleBufferFile();
			FileOutputStream fout = null;
			OutputStreamWriter outStream = null;
			try {
				fout = new FileOutputStream(f, false);
				outStream = new OutputStreamWriter(fout);
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
			}
			for (TrackInfo ti : bufferedTracks) {
				try {
					outStream.write(ti.getArtist() + "\n");
					outStream.write(ti.getTrack() + "\n");
					outStream.write("" + ti.getStartTime() + "\n");
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
			try {
				outStream.close();
			} catch (Exception e) {
			}
			bufferedTracks.clear();
		}
	}

	private void readBufferFromFile() {
		File f = JukefoxApplication.getDirectoryManager().getScrobbleBufferFile();
		FileInputStream fin = null;
		if (f == null || !f.exists() || !f.canRead()) {
			return;
		}
		try {
			fin = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			Log.w(TAG, e);
		}
		if (fin == null) {
			return;
		}
		DataInputStream din = new DataInputStream(fin);
		if (din == null) {
			return;
		}
		String artist = null;
		String track = null;
		String time = null;
		try {
			artist = din.readLine();
			if (artist != null) {
				track = din.readLine();
				time = din.readLine();
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		while (artist != null && track != null && time != null) {
			try {
				TrackInfo ti = new TrackInfo(artist, track);
				ti.setStartTime(time);
				ti.setSource(SourceType.E);
				bufferedTracks.add(ti);

				track = din.readLine();
				time = din.readLine();
				artist = din.readLine();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Context ctx = JukefoxApplication.getAppContext();
		String key1 = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_SCROBBLE_USERNAME);
		String key2 = ctx.getString(ch.ethz.dcg.pancho3.R.string.KEY_SCROBBLE_PWD);

		if (!key.equals(key1) && !key.equals(key2)) {
			return;
		}

		readSettings();
	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		scrobbleSubmitAsync(song);
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
		//		scrobbleSubmitAsync(); // Don't scrobble if song was not completed
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		setTrack(song);
		scrobbleNotifyAsync(tracks.get(song.getId()));
	}

	@Override
	public void onPlayModeChanged(IPlayMode newPlayMode) {

	}

	@Override
	public void onPlaylistChanged(IReadOnlyPlaylist newPlaylist) {

	}

	public void onDestroy() {
		settings.removeSettingsChangeListener(this);
	}

	@Override
	public void onCurrentSongChanged(PlaylistSong<BaseArtist, BaseAlbum> newSong) {

	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {

	}
}