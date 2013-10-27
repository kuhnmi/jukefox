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

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId.Type;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public class AndroidLibraryScanner extends AbstractLibraryScanner {

	private final static String TAG = AndroidLibraryScanner.class.getSimpleName();

	private HashSet<String> readUsingTagLibrary;
	private HashMap<String, String> albumArtistMapping;
	private JukefoxApplication application;

	public AndroidLibraryScanner(AbstractCollectionModelManager collectionModelManager, ImportState importState,
			JukefoxApplication application) {
		super(collectionModelManager, importState);
		this.application = application;
	}

	@Override
	public boolean reducedScan() throws DataUnavailableException {
		aborted = false;
		directoryBlackList = readDirectoryBlacklist();
		fileBlackList = readFileBlacklist();

		HashSet<String> dbPaths = otherDataProvider.getAllSongsPaths();
		return processMediaProviderInfoReduced(dbPaths);
	}

	@Override
	public void scan() throws DataUnavailableException {

		scannedFiles = 0;

		// TODO: check MediaProvider.AudioColumns.DATA_MODIFIED und
		// MediaProvider.AudioColumns.DATA_ADDED to improve performance?
		aborted = false;
		libraryChanges = new LibraryChanges();

		directoryBlackList = readDirectoryBlacklist();
		fileBlackList = readFileBlacklist();
		HashSet<String> albumNamesToGroup;
		try {
			albumNamesToGroup = modelSettingsManager.getAlbumNamesToGroup();
		} catch (Exception e) {
			Log.w(TAG, e);
			albumNamesToGroup = new HashSet<String>(); // TODO: can we inform
			// the user??
		}

		// Read all paths currently in the db. During
		// processMediaProviderInfo, subtract all songs that are still
		// available from this list to keep only those that need to be
		// removed from the db.
		HashMap<String, ImportSong> dbSongs = songProvider.getAllImportSongs();

		// if db empty, already insert songs during scan, such that a first song
		// is available as soon as possible
		boolean earlyInsert = dbSongs.size() == 0;
		readAlbumArtistCandidates();
		processMediaProviderInfo(dbSongs, albumNamesToGroup, earlyInsert);

		for (ImportSong s : dbSongs.values()) {
			libraryChanges.addSongToRemove(s);
		}
	}

	private void readAlbumArtistCandidates() {

		String[] projection = new String[] { android.provider.MediaStore.Audio.AudioColumns.ALBUM,
				android.provider.MediaStore.Audio.AudioColumns.ARTIST,
				android.provider.MediaStore.Audio.AudioColumns.DATA };
		albumArtistMapping = new HashMap<String, String>();
		readUsingTagLibrary = new HashSet<String>();
		Cursor cur = null;
		try {
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			Log.v(TAG, "uri: " + uri);
			ContentResolver cr = application.getContentResolver();
			// cur = cr.query(uri, projection, null, null,
			// MediaStore.Audio.Media._ID);
			cur = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null,
					MediaStore.Audio.Media.TRACK);
			Log.v(TAG, "cur == null: " + (cur == null));
			int numSongs = cur.getCount();
			Log.v(TAG, "Total number of songs on device: " + numSongs);
			while (cur.moveToNext() && !aborted) {
				scannedFiles++;
				// there are 3 steps (prescan, scan, commit) for the base data
				// import (thus 3*numSongs)
				String artist = cur.getString(1);
				String album = cur.getString(0);
				String path = cur.getString(2);
				if (album != null) {
					importState.setBaseDataProgress(scannedFiles, 3 * numSongs, "Importing: " + album);
				}
				if (pathIsBlacklisted(path)) {
					continue;
				}
				if (album == null) {
					continue;
				}
				if (artist == null) {
					readUsingTagLibrary.add(album);
					continue;
				}
				// Log.v(TAG, "Prescan song at path: " + path);
				if (!readUsingTagLibrary.contains(album)) {
					if (albumArtistMapping.containsKey(album)) {
						if (!albumArtistMapping.get(album).equals(artist)) {
							readUsingTagLibrary.add(album);
							Log.v(TAG, "Added album to read with tag library: " + album);
						}
					} else {
						albumArtistMapping.put(album, artist);
					}
				}
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	private void processMediaProviderInfo(HashMap<String, ImportSong> dbSongs, HashSet<String> albumNamesToGroup,
			boolean earlyInsert) {
		String[] projection = new String[] { android.provider.MediaStore.Audio.AudioColumns.TITLE,
				android.provider.MediaStore.Audio.AudioColumns.ALBUM,
				android.provider.MediaStore.Audio.AudioColumns.ARTIST,
				android.provider.MediaStore.Audio.AudioColumns.DATA,
				android.provider.MediaStore.Audio.AudioColumns.DURATION,
				android.provider.MediaStore.Audio.AudioColumns.TRACK,
				android.provider.MediaStore.Audio.AudioColumns._ID };

		processMediaProviderInfo(projection, dbSongs, albumNamesToGroup, Type.EXTERNAL, earlyInsert);
		// processMediaProviderInfo(projection, dbSongs, Type.INTERNAL);
	}

	/**
	 * 
	 * @param dbPaths
	 * @return true if there are changes
	 */
	private boolean processMediaProviderInfoReduced(HashSet<String> dbPaths) {
		String[] projection = new String[] { android.provider.MediaStore.Audio.AudioColumns.DATA };
		boolean ret;
		ret = processMediaProviderInfoReduced(projection, dbPaths, Type.EXTERNAL);
		if (ret) {
			return true;
		}
		// ret = processMediaProviderInfoReduced(projection, dbPaths,
		// Type.INTERNAL);
		return ret;
	}

	private boolean processMediaProviderInfoReduced(String[] projection, HashSet<String> dbPaths, Type type) {
		Cursor cur = null;
		try {
			Uri uri;
			if (type == Type.EXTERNAL) {
				uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			} else {
				uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
			}
			Log.v(TAG, "uri: " + uri);
			ContentResolver cr = application.getContentResolver();
			cur = cr.query(uri, projection, null, null, MediaStore.Audio.Media._ID);

			Log.v(TAG, "cur == null: " + (cur == null));
			Log.v(TAG, "numPaths from dbProvider: " + cur.getCount());
			while (cur.moveToNext() && !aborted) {
				String path = cur.getString(0);
				if (pathIsBlacklisted(path)) {
					continue;
				}
				if (!dbPaths.remove(path)) {
					Log.v(TAG, "reduced scan: new song path found: " + path);
					return true; // there is a change
				}
			}
			// if there are paths remaining, we have a change => return true
			Log.v(TAG, "end of reduced scan: dbPaths.size(): " + dbPaths.size());
			return dbPaths.size() > 0;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

	}

	private void processMediaProviderInfo(String[] projection, HashMap<String, ImportSong> dbSongs,
			HashSet<String> albumNamesToGroup, Type type, boolean initialImport) {
		Cursor cur = null;
		try {
			Uri uri;
			if (type == Type.EXTERNAL) {
				uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			} else {
				uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
			}
			Log.v(TAG, "uri: " + uri);
			ContentResolver cr = application.getContentResolver();
			cur = cr.query(uri, projection, null, null, MediaStore.Audio.Media._ID);

			Log.v(TAG, "cur == null: " + (cur == null));
			int numSongs = cur.getCount();
			int count = 0;
			while (cur.moveToNext() && !aborted) {
				count++;
				boolean earlyInsert = initialImport && scannedFiles < Math.min(
						AbstractLibraryScanner.NUM_SONGS_EARLY_INSERT, numSongs);
				processSongInfoRow(cur, dbSongs, type, albumNamesToGroup, earlyInsert);

				scannedFiles++;
				// there are 3 steps (prescan, scan, commit) for the base data
				// import (thus 3*numSongs)
				importState.setBaseDataProgress(scannedFiles, 3 * numSongs, "Importing Song Nr: " + count + "/"
						+ numSongs);
				if (scannedFiles % 500 == 0) {
					JoinableThread.sleepWithoutThrowing(10);
				}
			}
			importState.setBaseDataProgress(scannedFiles, 3 * numSongs, "Got " + numSongs + " songs");
			return;
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
	}

	private void processSongInfoRow(Cursor cur, HashMap<String, ImportSong> dbSongs, Type type,
			HashSet<String> albumNamesToGroup, boolean earlyInsert) {

		ImportSong song = getSongFromCursor(cur, type);

		if (pathIsBlacklisted(song.getPath())) {
			// Ignore songs in blacklisted paths
			return;
		}

		if (!isSongReadCorrectly(song) || readUsingTagLibrary.contains(song.getAlbum().getName()) || isWmaFile(song)) {
			readSongInfoWithTagLibrary(song);
		}

		replaceEmptyFieldsWithAlias(song);
		groupAlbumsIfNecessary(song, albumNamesToGroup);

		getSongToAddRemoveAndChange(dbSongs, song, earlyInsert);

		if (earlyInsert) {
			try {
				modifyProvider.insertSong(song);
			} catch (DataWriteException e) {
				e.printStackTrace();
				libraryChanges.addSongToAdd(song);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void readSongInfoWithTagLibrary(ImportSong song) {
		try {
			// Log.v("Tag Reader", "Reading tags from " + song.getName());
			File f = new File(song.getPath());
			AudioFile af = AudioFileIO.read(f);
			Tag tag = af.getTag();
			ImportAlbum album = null;
			String albumName = tag.getFirstAlbum();
			if (AndroidUtils.isNullOrEmpty(albumName, true)) {
				// albumName = JukefoxApplication.unknownAlbumAlias;
				albumName = AndroidConstants.UNKOWN_ALBUM;
			}
			if (tag.hasField("TCMP")) { // iTunes compilation album marker field
				try {
					Log.v("TCMP", "TCMP tag field found: " + song.getPath());
					Log.v(TAG, "TCMP tag field found: " + song.getPath());
					String content = readFieldContent(tag, "TCMP");
					Log.v("TCMP", "content: " + content);
					Log.v(TAG, "content: " + content);
					if (!content.contains("0")) {
						// album = new ImportAlbum(albumName,
						// JukefoxApplication.albumArtistAlias);
						album = new ImportAlbum(albumName, AndroidConstants.ALBUM_ARTIST_ALIAS);
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
			if (album == null && tag.hasField("TPE2")) {
				try {
					List<TextId3Frame> albumArtists = tag.get("TPE2");
					String albumArtistName = albumArtists.get(0).getContent();
					if (AndroidUtils.isNullOrEmpty(albumArtistName, true)) {
						// albumArtistName =
						// JukefoxApplication.unknownArtistAlias;
						albumArtistName = AndroidConstants.UNKOWN_ARTIST;
					}
					album = new ImportAlbum(albumName, albumArtistName);
					Log.v(TAG, "Read TPE2: " + albumArtists.get(0));
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
			if (album != null) {
				song.setAlbum(album);
			}

			String title = tag.getFirstTitle();
			if (AndroidUtils.isNullOrEmpty(title, true)) {
				// title = JukefoxApplication.unknownTitleAlias;
				title = AndroidConstants.UNKOWN_TITLE;
			}
			song.setName(title);
			song.setArtist(tag.getFirstArtist());
			if (album == null) {
				album = new ImportAlbum(albumName, song.getArtist());
			}
			song.setAlbum(album);
			// Only try to read track if it was not read correctly before.
			if (song.getTrack() < 1) {
				int track = 0;
				try {
					track = Integer.parseInt(tag.getFirstTrack());
				} catch (Exception e) {

				}
				song.setTrack(track);
			}
			if (song.getDuration() < 1000) {
				song.setDuration(af.getLength());
			}
			Log.v(TAG, "Read from tag " + song.getPath() + " : (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName() + " track: " + song.getTrack());
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
	}

	private ImportSong getSongFromCursor(Cursor cur, Type type) {

		String name = cur.getString(0);
		String album = cur.getString(1);
		String artist = cur.getString(2);
		String path = cur.getString(3);
		int duration = cur.getInt(4);
		int track = cur.getInt(5);
		ContentProviderId cpId = new ContentProviderId(cur.getInt(6), type);
		// int track = 0;
		// ContentProviderId cpId = new ContentProviderId(cur.getInt(5), type);
		if (artist == null) {
			Log.v(TAG, "getSongFromCursor(): artist == null");
		}
		// Log.v(TAG, "artist: '" + artist + "'");

		ImportAlbum importAlbum = new ImportAlbum(album, artist);

		ImportSong song = new ImportSong(name, importAlbum, artist, path, duration, track, cpId, null, new Date());

		return song;

	}

	private boolean isWmaFile(ImportSong song) {
		String path = song.getPath().toLowerCase();
		if (path.endsWith(".wma")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void clearData() {
		libraryChanges = null;
		readUsingTagLibrary = null;
		albumArtistMapping = null;
	}

}
