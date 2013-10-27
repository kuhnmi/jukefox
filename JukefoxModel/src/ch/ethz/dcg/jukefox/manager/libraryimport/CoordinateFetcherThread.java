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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.HttpHelper;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.AlbumStatus;
import ch.ethz.dcg.jukefox.model.collection.SongStatus;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.libraryimport.WebDataSong;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;

public class CoordinateFetcherThread extends JoinableThread {

	private final static String TAG = CoordinateFetcherThread.class.getSimpleName();

	private final static int ANSWER_OK = 0;
	private final static int ANSWER_ARTIST_APPR = 1;
	private final static int ANSWER_TITLE_APPR = 2;
	private final static int ANSWER_ARTIST_AND_TITLE_APPR = 3;
	private final static int ANSWER_ARTIST_AND_TITLE_APPR2 = 4;
	private final static int ANSWER_ARTIST_COORDS_FOUND = 5;
	private final static int ANSWER_ARTIST_COORDS_FOUND_APPR = 6;
	private final static int ANSWER_ARTIST_NOT_FOUND = -1;
	// public final static int ANSWER_TITLE_NOT_FOUND = -2;
	private final static int ANSWER_COORDS_NOT_FOUND = -3;
	private final static int ANSWER_GENERAL_ERROR = -4;

	private final static SongStatus[] REQ_STATUSES = new SongStatus[] { SongStatus.BASE_DATA, SongStatus.WEB_DATA_ERROR };

	private final static AlbumStatus[] REQ_ALBUM_STATUSES = new AlbumStatus[] { AlbumStatus.COVER_UNCHECKED,
			AlbumStatus.WEB_ERROR };

	private final static SongStatus[] REQ_STATUSES_REDUCED = new SongStatus[] { SongStatus.BASE_DATA };

	private final static AlbumStatus[] REQ_ALBUM_STATUSES_REDUCED = new AlbumStatus[] { AlbumStatus.COVER_UNCHECKED };

	private ModifyProvider modifyProvider;
	private SongProvider songProvider;
	private AbstractLanguageHelper languageHelper;

	private DefaultHttpClient httpClient;
	private JoinableThread dbWriterThread;
	private boolean writeAborted;
	private boolean hasChanges;
	private List<CoordinateFetcherListener> listeners;
	private ImportState importState;

	private boolean reduced; // reduced import does not consider web-errors.

	/**
	 * keeps track of the last album id in a request package, such that completed albums can be passed on to cover
	 * loading thread
	 */
	private int lastAlbumId;
	private AlbumStatus lastAlbumStatus;
	private BlockingQueue<Integer> outQueue;
	private ICoordinateFetcherConsumer consumer;

	public CoordinateFetcherThread(AbstractCollectionModelManager collectionModelManager,
			ICoordinateFetcherConsumer consumer, List<CoordinateFetcherListener> listeners, ImportState importState) {
		this.modifyProvider = collectionModelManager.getModifyProvider();
		this.songProvider = collectionModelManager.getSongProvider();
		this.languageHelper = collectionModelManager.getLanguageHelper();
		this.consumer = consumer;
		this.outQueue = consumer.getQueue();
		this.listeners = listeners;
		this.importState = importState;

		httpClient = HttpHelper.createHttpClientWithDefaultSettings();
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public void addListener(CoordinateFetcherListener listener) {
		listeners.add(listener);
	}

	@Override
	public void run() {
		int fetched = 0;
		hasChanges = false;
		abortPendingWrites();
		writeAborted = false;
		List<WebDataSong> songs = null;
		try {

			if (reduced) {
				songs = songProvider.getWebDataSongsForStatus(REQ_STATUSES_REDUCED, REQ_ALBUM_STATUSES_REDUCED);
			} else {
				songs = songProvider.getWebDataSongsForStatus(REQ_STATUSES, REQ_ALBUM_STATUSES);
			}

			// required for progress bar
			consumer.setNumberOfAlbums(getNumberOfAlbums(songs));

			Log.v(TAG, "retrieved web data songs. size: " + songs.size());
			if (songs.size() != 0) {
				lastAlbumId = songs.get(0).getAlbumId();
				lastAlbumStatus = songs.get(0).getAlbumStatus();
			}
			Set<WebDataSong> songsToUpdate = new HashSet<WebDataSong>();
			while (songs.size() > 0) {
				if (importState.shouldAbortImport()) {
					importState.setCoordinatesProgress(1, 1, "fetched song similarity infos");
					return;
				}
				fetched++;
				if (songs.size() > 0) {
					importState.setCoordinatesProgress(fetched, fetched + songs.size(),
							"Fetching similarity info for: " + songs.get(0).getName());
				}
				CoordinateRequestPackage requestPackage = null;
				try {
					//					Log.v(TAG, "request package");
					requestPackage = getRequestPackage(songs);
					//					Log.v(TAG, "get data from server");
					List<WebDataSong> serverResponse = getDataFromServer(requestPackage);
					songsToUpdate.addAll(serverResponse);
					outQueue.addAll(requestPackage.getCompleteAlbumIds());
				} catch (Exception e) {
					Log.w(TAG, e);
					if (requestPackage != null) {
						Log.v(TAG, "Adding request package to outqueue: " + requestPackage.getCompleteAlbumIds().size());
						outQueue.addAll(requestPackage.getCompleteAlbumIds());
					}
					if (HttpHelper.isNetworkException(e)) {
						throw e;
					}
					Log.w(TAG, e);
				}
			}

			// write changes to DB
			modifyProvider.batchUpdateWebData(songsToUpdate);
			for (WebDataSong s : songs) {
				if (s.isHasStatusChanged()) {
					hasChanges = true;
					informListenersChangeDetected();
					break;
				}
			}

		} catch (Exception e) {
			Log.w(TAG, e);
			if (songs != null) {
				Log.v(TAG, "writing remaining songs to outqueue: " + songs.size());
				writeRemainingSongsToOutQueue(songs);
			}
		}
		importState.setCoordinatesProgress(1, 1, "fetched song similarity infos");
		// informListenersCompleted();
	}

	private int getNumberOfAlbums(List<WebDataSong> songs) {
		int albumId = -1;
		int cnt = 0;
		for (WebDataSong s : songs) {
			if (s.getAlbumId() != albumId) {
				cnt++;
				albumId = s.getAlbumId();
			}
		}
		return cnt;
	}

	private void writeRemainingSongsToOutQueue(List<WebDataSong> songs) {
		while (songs.size() > 0) {
			CoordinateRequestPackage requestPackage = null;
			try {
				requestPackage = getRequestPackage(songs);
				outQueue.addAll(requestPackage.getCompleteAlbumIds());
			} catch (Exception e) {
				Log.w(TAG, e);
				try {
					if (requestPackage != null) {
						outQueue.addAll(requestPackage.getCompleteAlbumIds());
					}
				} catch (Exception e2) {
					Log.w(TAG, e2);
				}
			}
		}
	}

	//	private void writeToDb(final List<WebDataSong> songs, final List<Integer> completeAlbumIds) {
	//		waitForDbWriterThread();
	//		dbWriterThread = new JoinableThread(new Runnable() {
	//
	//			@Override
	//			public void run() {
	//				for (WebDataSong s : songs) {
	//					try {
	//						if (writeAborted) {
	//							break;
	//						}
	//						if (s.isHasStatusChanged()) {
	//							hasChanges = true;
	//							informListenersChangeDetected();
	//						}
	//						modifyProvider.updateWebDataSong(s);
	//					} catch (Exception e) {
	//						Log.w(TAG, e);
	//						// if something goes wrong, we will try it again later,
	//						// anyways... thus, just log the exception...
	//					}
	//				}
	//				outQueue.addAll(completeAlbumIds);
	//			}
	//
	//		});
	//		dbWriterThread.start();
	//	}

	private void informListenersChangeDetected() {
		for (CoordinateFetcherListener l : listeners) {
			l.onCoordinateFetcherChangeDetected();
		}
	}

	// private void informListenersCompleted() {
	// for (CoordinateFetcherListener l: listeners) {
	// l.onCoordinateFetcherCompleted();
	// }
	// }

	private void abortPendingWrites() {
		writeAborted = true;
		waitForDbWriterThread();
	}

	private void waitForDbWriterThread() {
		if (dbWriterThread == null) {
			return;
		}
		try {
			dbWriterThread.realJoin();
		} catch (InterruptedException e) {
			Log.w(TAG, e);
		}
	}

	private List<WebDataSong> getDataFromServer(CoordinateRequestPackage coordReqPackage) throws Exception {

		// songs are removed from the list and added to the package
		List<WebDataSong> ret;
		try {
			ret = getDataFromServerByPackage(coordReqPackage);
		} catch (Exception e) {
			if (HttpHelper.isNetworkException(e) && !(e instanceof SocketTimeoutException)) {
				throw e;
			}
			// the package failed... let's try them one by one...
			// TODO: do we need to "clean" the songs first...?
			ret = getDataFromServerBySingleRequests(coordReqPackage);
		}
		// these were marked as "server data missing" before.
		ret.addAll(coordReqPackage.getUnrequestedSongs());
		return ret;

	}

	private List<WebDataSong> getDataFromServerBySingleRequests(CoordinateRequestPackage coordReqPackage)
			throws Exception {
		List<WebDataSong> ret = new LinkedList<WebDataSong>();
		for (WebDataSong s : coordReqPackage.getSongs()) {
			try {
				ret.add(getSingleSongDataFromServer(s));
			} catch (Exception e) {
				if (HttpHelper.isNetworkException(e)) {
					throw e;
				}
				Log.w(TAG, e);
				Log.writeExceptionToErrorFile(TAG, e.getMessage(), e);
				s.setStatus(SongStatus.WEB_DATA_ERROR);
				ret.add(s);
			}
		}
		return ret;
	}

	private WebDataSong getSingleSongDataFromServer(WebDataSong song) throws IOException {
		InputStream is = null;
		BufferedReader br = null;
		try {
			String url = getBaseUrl();
			url += getUrlChunkForSong(song, 0);
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResp = httpClient.execute(httpGet);

			HttpEntity httpEntity = httpResp.getEntity();
			is = httpEntity.getContent();
			br = new BufferedReader(new InputStreamReader(is));
			processSong(br, song);
			return song;
		} catch (NumberFormatException ne) {
			Log.w(TAG, "Song lost due to NumberFormatException");
			Log.w(TAG, ne);
			String content = Utils.readBufferToString(br);
			Log.writeExceptionToErrorFile(TAG, "Coordinate package lost due " + "to NumberFormatException: " + content,
					ne);
			throw ne;
		} finally {
			if (br != null) {
				br.close();
			}
			if (is != null) {
				is.close();
			}
		}
	}

	private List<WebDataSong> getDataFromServerByPackage(CoordinateRequestPackage coordReqPackage) throws IOException {
		InputStream is = null;
		BufferedReader br = null;
		try {
			HttpGet httpGet = new HttpGet(coordReqPackage.getUrl());
			HttpResponse httpResp = httpClient.execute(httpGet);

			List<WebDataSong> webDataSongs = new LinkedList<WebDataSong>();

			HttpEntity httpEntity = httpResp.getEntity();
			is = httpEntity.getContent();
			br = new BufferedReader(new InputStreamReader(is));
			for (WebDataSong s : coordReqPackage.getSongs()) {
				processSong(br, s);
				webDataSongs.add(s);
				// Doesn't help to catch IOException here, as we do not know
				// where to proceed in the response
			}
			return webDataSongs;

		} catch (NumberFormatException ne) {
			Log.w(TAG, "Coordinate package lost due to NumberFormatException");
			Log.w(TAG, ne);
			String content = Utils.readBufferToString(br);
			Log.writeExceptionToErrorFile(TAG, "Coordinate package lost due " + "to NumberFormatException: " + content,
					ne);
			throw ne;
		} finally {
			if (br != null) {
				br.close();
			}
			if (is != null) {
				is.close();
			}
		}

	}

	private void processSong(BufferedReader br, WebDataSong song) throws IOException {
		String line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int code = Integer.parseInt(line);

		switch (code) {
			case ANSWER_OK: // the default case, i.e. everything was found as is
				processDefaultSong(br, song);
				break;
			case ANSWER_ARTIST_AND_TITLE_APPR:
			case ANSWER_ARTIST_AND_TITLE_APPR2:
				processArtistAndTitleApproxSong(br, song);
				break;
			case ANSWER_ARTIST_APPR:
				processArtistApproxSong(br, song);
				break;
			case ANSWER_TITLE_APPR:
				processTitleApproxSong(br, song);
				break;
			case ANSWER_ARTIST_NOT_FOUND:
				song.setStatus(SongStatus.WEB_DATA_MISSING);
				break;
			case ANSWER_ARTIST_COORDS_FOUND:
			case ANSWER_ARTIST_COORDS_FOUND_APPR:
				processArtistCoordsWebDataSong(br, song);
				break;
			case ANSWER_COORDS_NOT_FOUND:
				Log.w("getCoordinates",
						"No coordinates found for: " + song.getArtist().getName() + " - " + song.getName());
				song.setStatus(SongStatus.WEB_DATA_MISSING);
				break;
			case ANSWER_GENERAL_ERROR:
			default:
				Log.w(TAG,
						"Unknown error while getting coordinates for: " + song.getArtist().getName() + " - "
								+ song.getName());
				song.setStatus(SongStatus.WEB_DATA_ERROR);
		}
	}

	private void processDefaultSong(BufferedReader br, WebDataSong song) throws IOException {
		String line = br.readLine();
		int meArtistId = Integer.parseInt(line);
		line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int meSongId = Integer.parseInt(line);
		float[] coords = getCoords(br);
		// TODO: Check if we already know the artist coords. If not, retrieve
		// them from server.
		song.getArtist().setMeId(meArtistId);
		song.getArtist().setMeName(song.getArtist().getName());
		song.setMeId(meSongId);
		song.setMeName(song.getName());
		song.setCoords(coords);
		song.setStatus(SongStatus.WEB_DATA_OK);
	}

	public void processArtistAndTitleApproxSong(BufferedReader br, WebDataSong song) throws IOException {
		String meArtistName = Utils.replaceXmlEntities(br.readLine());
		String meTitle = Utils.replaceXmlEntities(br.readLine());
		String line = br.readLine();
		int meArtistId = Integer.parseInt(line);
		line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int meSongId = Integer.parseInt(line);
		float[] coords = getCoords(br);
		song.getArtist().setMeId(meArtistId);
		song.getArtist().setMeName(meArtistName);
		song.setMeId(meSongId);
		song.setMeName(meTitle);
		song.setCoords(coords);
		song.setStatus(SongStatus.WEB_DATA_OK);
	}

	public void processArtistApproxSong(BufferedReader br, WebDataSong song) throws IOException {
		String meArtistName = Utils.replaceXmlEntities(br.readLine());
		String line = br.readLine();
		int meArtistId = Integer.parseInt(line);
		line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int meSongId = Integer.parseInt(line);
		float[] coords = getCoords(br);
		song.getArtist().setMeId(meArtistId);
		song.getArtist().setMeName(meArtistName);
		song.setMeId(meSongId);
		song.setMeName(song.getName());
		song.setCoords(coords);
		song.setStatus(SongStatus.WEB_DATA_OK);
	}

	public void processTitleApproxSong(BufferedReader br, WebDataSong song) throws IOException {
		String meTitle = Utils.replaceXmlEntities(br.readLine());
		String line = br.readLine();
		int meArtistId = Integer.parseInt(line);
		line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int meSongId = Integer.parseInt(line);
		float[] coords = getCoords(br);
		song.getArtist().setMeId(meArtistId);
		song.getArtist().setMeName(song.getArtist().getName());
		song.setMeId(meSongId);
		song.setMeName(meTitle);
		song.setCoords(coords);
		song.setStatus(SongStatus.WEB_DATA_OK);
	}

	public void processArtistCoordsWebDataSong(BufferedReader br, WebDataSong song) throws IOException {
		String meArtistName = Utils.replaceXmlEntities(br.readLine());
		String line = br.readLine();
		if (line == null) {
			Log.w(TAG, "Could not read line from BufferedReader");
			song.setStatus(SongStatus.WEB_DATA_ERROR);
			return;
		}
		int meArtistId = Integer.parseInt(line);
		float[] artistCoords = getCoords(br);
		song.getArtist().setMeId(meArtistId);
		song.getArtist().setMeName(meArtistName);
		song.getArtist().setCoords(artistCoords);
		song.setStatus(SongStatus.WEB_DATA_OK);
	}

	// public WebDataSong getEmptyWebDataSong(WebDataSong song)
	// throws IOException {
	//
	// CompleteArtist artist = new CompleteArtist(song.getArtist(), null,
	// null, null);
	// WebDataAlbum album = new WebDataAlbum(song.getAlbum(), -1, null);
	// return new WebDataSong(song, artist, album, null, null, null);
	// }

	private float[] getCoords(BufferedReader bufread) throws IOException {
		String line;
		float[] coords;
		coords = new float[Constants.DIM];
		for (int i = 0; i < Constants.DIM; i++) {
			line = bufread.readLine();
			if (line == null) {
				Log.w(TAG, "Could not read line from BufferedReader");
				throw new IOException();
			}
			coords[i] = Float.parseFloat(line);
		}
		return coords;
	}

	private CoordinateRequestPackage getRequestPackage(List<WebDataSong> songs) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getBaseUrl());
		ArrayList<WebDataSong> songsInPackage = new ArrayList<WebDataSong>();
		ArrayList<WebDataSong> unrequestedSongs = new ArrayList<WebDataSong>();
		ArrayList<Integer> completeAlbumIdsInPackage = new ArrayList<Integer>();
		while (songsInPackage.size() < WebDataFetcher.COORDINATE_PACKAGE_SIZE && songs.size() > 0) {
			try {
				WebDataSong s = songs.remove(0);
				// Log.v(TAG,
				// "Adding song to request package with song status: " +
				// s.getStatus() + " and album status: " + s.getAlbumStatus());
				if (s.getAlbumId() != lastAlbumId) {
					if (reduced) {
						for (int i = 0; i < REQ_ALBUM_STATUSES_REDUCED.length; i++) {
							if (lastAlbumStatus == REQ_ALBUM_STATUSES_REDUCED[i]) {
								completeAlbumIdsInPackage.add(lastAlbumId);
							}
						}
					} else {
						for (int i = 0; i < REQ_ALBUM_STATUSES.length; i++) {
							if (lastAlbumStatus == REQ_ALBUM_STATUSES[i]) {
								completeAlbumIdsInPackage.add(lastAlbumId);
							}
						}
					}
					lastAlbumId = s.getAlbumId();
					lastAlbumStatus = s.getAlbumStatus();
				}
				if (!requiresRequest(s.getStatus())) {
					unrequestedSongs.add(s);
				}
				String urlChunkForSong = getUrlChunkForSong(s, songsInPackage.size());
				if (urlChunkForSong == null) {
					s.setStatus(SongStatus.WEB_DATA_MISSING);
					unrequestedSongs.add(s);
					continue;
				}
				urlBuilder.append(urlChunkForSong);
				songsInPackage.add(s);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
		if (songs.size() == 0) {
			if (reduced) {
				for (int i = 0; i < REQ_ALBUM_STATUSES_REDUCED.length; i++) {
					if (lastAlbumStatus == REQ_ALBUM_STATUSES_REDUCED[i]) {
						completeAlbumIdsInPackage.add(lastAlbumId);
					}
				}
			} else {
				for (int i = 0; i < REQ_ALBUM_STATUSES.length; i++) {
					if (lastAlbumStatus == REQ_ALBUM_STATUSES[i]) {
						completeAlbumIdsInPackage.add(lastAlbumId);
					}
				}
			}
		}
		String url = urlBuilder.toString();
		return new CoordinateRequestPackage(url, songsInPackage, unrequestedSongs, completeAlbumIdsInPackage);
	}

	private boolean requiresRequest(SongStatus status) {
		for (SongStatus s : REQ_STATUSES) {
			if (s == status) {
				return true;
			}
		}
		return false;
	}

	private String getBaseUrl() {
		StringBuilder sb = new StringBuilder();
		sb.append(Constants.FORMAT_COORDS_REQUEST_PACKAGE_NOXML);
		sb.append("hash=");
		// TODO
		// sb.append(JukefoxApplication.getUniqueId());
		sb.append(languageHelper.getUniqueId());
		return sb.toString();
	}

	private String getUrlChunkForSong(WebDataSong s, int idx) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		String artist = s.getArtist().getName();
		if (artist == null) {
			return null;
		}
		sb.append("&artist[" + idx + "]=" + URLEncoder.encode(artist, "UTF-8"));
		String title = s.getName();
		if (title == null) {
			title = languageHelper.getUnknownTitleAlias();
		}
		sb.append("&title[" + idx + "]=" + URLEncoder.encode(title, "UTF-8"));
		return sb.toString();
	}

	public boolean hasChanges() {
		return hasChanges;
	}

	public void joinIncludingInnerThread() {
		try {
			this.realJoin();
			if (dbWriterThread != null) {
				dbWriterThread.realJoin();
			}
		} catch (InterruptedException e) {
			Log.w(TAG, e);
		}
	}

}
