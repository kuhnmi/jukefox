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
import java.util.Iterator;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.LanguageHelper;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.io.IoUtils;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileFilter;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public class LibraryScanner extends AbstractLibraryScanner {

	private final static String TAG = LibraryScanner.class.getSimpleName();

	private static AudioFileFilter audioFileFilter = new AudioFileFilter();
	private static LanguageHelper languageHelper = new LanguageHelper();
	private HashMap<String, Integer> genreTable;

	public LibraryScanner(AbstractCollectionModelManager collectionModelManager, ImportState importState) {
		super(collectionModelManager, importState);
	}

	@Override
	public boolean reducedScan() throws DataUnavailableException {
		aborted = false;
		directoryBlackList = readDirectoryBlacklist();
		fileBlackList = readFileBlacklist();

		HashSet<String> dbPaths = otherDataProvider.getAllSongsPaths();
		HashMap<String, File> audioCollection;
		audioCollection = collectSongsFromLibraryFolders();
		return checkForChanges(dbPaths, audioCollection);
	}

	@Override
	public void scan() throws DataUnavailableException {

		scannedFiles = 0;
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
		boolean initialImport = dbSongs.size() == 0;
		HashMap<String, File> audioCollection;
		audioCollection = collectSongsFromLibraryFolders();
		processCollection(dbSongs, audioCollection, albumNamesToGroup, initialImport);

		for (ImportSong s : dbSongs.values()) {
			libraryChanges.addSongToRemove(s);
		}
	}

	private HashMap<String, File> collectSongsFromLibraryFolders() {
		HashSet<String> libraryPaths = modelSettingsManager.getLibraryPaths();
		Iterator<String> libraryFolders = libraryPaths.iterator();
		HashMap<String, File> audioCollection = new HashMap<String, File>();
		while (libraryFolders.hasNext()) {
			File folder = new File(libraryFolders.next());
			if (folder.exists()) {
				addFilesRecursively(folder, audioCollection);
			}
		}
		return audioCollection;
	}

	private void addFilesRecursively(File file, HashMap<String, File> audioCollection) {
		final File[] songs = file.listFiles(audioFileFilter);
		if (songs != null) {
			for (File song : songs) {
				if (pathIsBlacklisted(song.getAbsolutePath())) {
					continue;
				}
				if (song.isDirectory()) {
					addFilesRecursively(song, audioCollection);
				} else {
					audioCollection.put(song.getAbsolutePath(), song);
				}
			}
		}
	}

	private boolean checkForChanges(HashSet<String> dbPaths, HashMap<String, File> audioCollection) {

		Iterator<File> songs = audioCollection.values().iterator();
		while (songs.hasNext() && !aborted) {
			String path = songs.next().getAbsolutePath();
			if (!dbPaths.remove(path)) {
				Log.v(TAG, "reduced scan: new song path found: " + path);
				return true; // there is a change
			}
		}
		Log.v(TAG, "end of reduced scan: dbPaths.size(): " + dbPaths.size());
		return dbPaths.size() > 0;

	}

	@Override
	protected void processCollection(HashMap<String, ImportSong> dbSongs, HashMap<String, File> audioCollection,
			HashSet<String> albumNamesToGroup, boolean initialImport) {

		createGenreTable();
		int numSongs = audioCollection.size();
		Iterator<File> songs = audioCollection.values().iterator();
		int count = 0;
		while (songs.hasNext() && !aborted) {
			count++;

			ImportSong song = null;
			song = readSongInfoWithTagLibrary(songs.next());
			if (song == null) {
				continue;
			}
			updateSongGenreMap(song);
			replaceEmptyFieldsWithAlias(song);
			groupAlbumsIfNecessary(song, albumNamesToGroup);
			boolean earlyInsert = initialImport && scannedFiles < AbstractLibraryScanner.NUM_SONGS_EARLY_INSERT;
			getSongToAddRemoveAndChange(dbSongs, song, earlyInsert);

			if (initialImport && earlyInsertSongs.size() >= Math.min(numSongs,
					AbstractLibraryScanner.NUM_SONGS_EARLY_INSERT)) {
				// batch insert the songs
				try {
					modifyProvider.batchInsertSongs(earlyInsertSongs);
				} catch (DataWriteException e) {
					for (ImportSong importSong : earlyInsertSongs) {
						libraryChanges.addSongToAdd(importSong);
					}
				}
				earlyInsertSongs.clear();
			}

			scannedFiles++;
			// there are 3 steps (prescan, scan, commit) for the base data
			// import (thus 3*numSongs)
			importState.setBaseDataProgress(scannedFiles, 3 * numSongs, "Importing Song Nr: " + count + "/" + numSongs);

			// TODO do we need this in PC version
			if (scannedFiles % 500 == 0) {
				JoinableThread.sleepWithoutThrowing(10);
			}

		}

	}

	@Override
	protected void createGenreTable() {
		List<Genre> genreList = genreProvider.getAllGenres();
		genreTable = new HashMap<String, Integer>();
		for (Genre genre : genreList) {
			genreTable.put(genre.getName(), genre.getId());
		}
	}

	private void updateSongGenreMap(ImportSong song) {
		HashSet<Integer> genreIds = new HashSet<Integer>();
		for (String genre : song.getGenres()) {
			int genreId;
			if (genreTable.isEmpty() || !genreTable.containsKey(genre)) {
				try {
					genreId = modifyProvider.insertGenre(genre);
				} catch (DataWriteException e) {
					Log.e(TAG, "insert genre failed: " + genre);
					return;
				}
			} else {
				genreId = genreTable.get(genre);
			}
			genreIds.add(genreId);
		}
		libraryChanges.getCollectionSongGenreMap().put(song.getPath(), genreIds);
	}

	/*
	 * Added the suppression below to get rid of the two warnings that occur
	 * in relation assigning the lists returned by the Tag-class to
	 * typed references.
	 */
	@SuppressWarnings("unchecked")
	private ImportSong readSongInfoWithTagLibrary(File songPath) {

		ImportSong song = null;
		String name = null;
		String artist = null;
		String albumName = null;
		String path = songPath.getAbsolutePath();
		int duration;
		int track = 0;
		ContentProviderId cpId = null;
		List<TextId3Frame> genres;
		ImportAlbum album = null;

		try {
			Log.d(TAG, "Reading Tags of '" + songPath + "'.");

			// Needed suspend method because the read-method prints an exception in case one occurs instead
			// of throwing it again.
			IoUtils.suspendSysErr();
			AudioFile af = AudioFileIO.read(songPath);
			IoUtils.resumeSysErr();

			Tag tag = af.getTag();

			albumName = tag.getFirstAlbum();
			if (Utils.isNullOrEmpty(albumName, true)) {
				albumName = languageHelper.getUnknownAlbumAlias();
			}
			if (tag.hasField("TCMP")) { // iTunes compilation album marker field
				try {
					Log.v("TCMP", "TCMP tag field found: " + songPath.getAbsolutePath());
					Log.v(TAG, "TCMP tag field found: " + songPath.getAbsolutePath());
					String content = readFieldContent(tag, "TCMP");
					Log.v("TCMP", "content: " + content);
					Log.v(TAG, "content: " + content);
					if (!content.contains("0")) {
						album = new ImportAlbum(albumName, languageHelper.getAlbumArtistAlias());
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
			if (album == null && tag.hasField("TPE2")) {
				try {
					List<TextId3Frame> albumArtists = tag.get("TPE2");
					String albumArtistName = albumArtists.get(0).getContent();
					if (Utils.isNullOrEmpty(albumArtistName, true)) {
						albumArtistName = languageHelper.getUnknownArtistAlias();
					}
					album = new ImportAlbum(albumName, albumArtistName);
					Log.v(TAG, "Read TPE2: " + albumArtists.get(0));
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}

			name = tag.getFirstTitle();
			if (Utils.isNullOrEmpty(name, true)) {
				name = languageHelper.getUnknownTitleAlias();
			}

			artist = tag.getFirstArtist();

			if (album == null) {
				album = new ImportAlbum(albumName, artist);
			}

			try {
				track = Integer.parseInt(tag.getFirstTrack());
			} catch (Exception e) {
			}

			duration = (int) (af.getPreciseLength() * 1000);
			song = new ImportSong(name, album, artist, path, duration, track, cpId, null, new Date());

			try {
				genres = tag.getGenre();
				for (TextId3Frame genre : genres) {
					song.addGenre(genre.getContent());
				}
			} catch (Exception e) {
			}

			Log.v(TAG, "Read from tag " + song.getPath() + " : (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName() + " track: " + song.getTrack());

		} catch (Throwable e) {
			Log.w(TAG, e);
		}

		return song;
	}

	@Override
	public void clearData() {
		libraryChanges = null;
	}

}
