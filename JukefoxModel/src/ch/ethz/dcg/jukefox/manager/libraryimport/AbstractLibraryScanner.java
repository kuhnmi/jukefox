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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.libraryimport.ContentProviderId;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.GenreProvider;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileFilter;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public abstract class AbstractLibraryScanner {

	private final static String TAG = AbstractLibraryScanner.class.getSimpleName();
	protected static final int NUM_SONGS_EARLY_INSERT = 20;

	protected ModelSettingsManager modelSettingsManager;
	protected SongProvider songProvider;
	protected ModifyProvider modifyProvider;
	protected GenreProvider genreProvider;
	protected OtherDataProvider otherDataProvider;
	protected LibraryChanges libraryChanges;
	protected DirectoryManager directoryManager;
	protected AbstractLanguageHelper languageHelper;
	protected String[] directoryBlackList;
	protected HashSet<String> fileBlackList;
	protected ImportState importState;
	protected int scannedFiles = 0;
	protected boolean aborted;
	protected static AudioFileFilter audioFileFilter = new AudioFileFilter();
	protected HashMap<String, Integer> genreTable;

	public AbstractLibraryScanner(AbstractCollectionModelManager collectionModelManager, ImportState importState) {
		this.importState = importState;
		modelSettingsManager = collectionModelManager.getModelSettingsManager();
		modifyProvider = collectionModelManager.getModifyProvider();
		songProvider = collectionModelManager.getSongProvider();
		otherDataProvider = collectionModelManager.getOtherDataProvider();
		genreProvider = collectionModelManager.getGenreProvider();
		directoryManager = collectionModelManager.getDirectoryManager();
		languageHelper = collectionModelManager.getLanguageHelper();
		aborted = false;
	}

	public void abort() {
		aborted = true;
	}

	public boolean wasAborted() {
		return aborted;
	}

	public LibraryChanges getLibraryChanges() {
		return libraryChanges;
	}

	public abstract void clearData();

	public abstract void scan() throws DataUnavailableException;

	public abstract boolean reducedScan() throws DataUnavailableException;

	protected Set<ImportSong> earlyInsertSongs = new HashSet<ImportSong>();

	protected void getSongToAddRemoveAndChange(HashMap<String, ImportSong> dbSongs, ImportSong song, boolean earlyInsert) {
		ImportSong dbSong = dbSongs.get(song.getPath());

		if (dbSong == null) { // new song
			Log.v(TAG, "song to add found: " + song.getArtist() + " - " + song.getName() + " path: " + song.getPath());
			if (earlyInsert) {
				earlyInsertSongs.add(song);
				libraryChanges.addSongToChange(song);
			} else {
				libraryChanges.addSongToAdd(song);
			}
		} else { // we know the song already
			song.setJukefoxId(dbSong.getJukefoxId());
			if (!dbSong.equals(song)) {
				Log.v(TAG, "song to change found");
				Log.v(TAG, "dbSong: " + dbSong.getLogString());
				Log.v(TAG, "song: " + song.getLogString());
				libraryChanges.addSongToChange(song);
			} else {
				// add contentProviderId to idMap for all already existing songs if
				// exists
				if (song.getContentProviderId() != null) {
					libraryChanges.getContentProviderIdToJukefoxIdMap().put(song.getContentProviderId(),
							dbSong.getJukefoxId());
				}
			}
			// make sure, only the songs that need to be removed stay in the HashMap
			dbSongs.remove(song.getPath());
		}
	}

	@SuppressWarnings("unchecked")
	protected String readFieldContent(Tag tag, String id) {
		try {
			List<TagField> fields = tag.get(id);
			if (fields == null || fields.size() == 0) {
				return "";
			}
			try {
				TextId3Frame frame = (TextId3Frame) fields.get(0);
				return frame.getContent();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			// TODO: try to read GenericId3Frame? => how to get encoding??
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return "";

	}

	protected boolean isSongReadCorrectly(ImportSong song) {
		if (Utils.isNullOrEmpty(song.getArtist(), true)) {
			Log.v(TAG, "Song at " + song.getPath() + " is read incorrectly (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName());
			return false;
		}
		if (Utils.isNullOrEmpty(song.getAlbum().getName(), true)) {
			Log.v(TAG, "Song at " + song.getPath() + " is read incorrectly (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName());
			return false;
		}

		if (Utils.isNullOrEmpty(song.getName(), true)) {
			Log.v(TAG, "Song at " + song.getPath() + " is read incorrectly (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName());
			return false;
		}
		if (song.getDuration() == 0) {
			Log.v(TAG, "Song at " + song.getPath() + " is read incorrectly (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName());
			return false;
		}
		return isArtistValid(song);
	}

	protected boolean isArtistValid(ImportSong song) {
		String normalizedArtist = song.getArtist().trim().toLowerCase();
		if (normalizedArtist.equals("unknown") || normalizedArtist.equals("unknown artist")
				|| normalizedArtist.equals("<unknown>") || normalizedArtist.equals("")) {
			Log.v(TAG, "Song at " + song.getPath() + " is read incorrectly (artist: " + song.getArtist() + " album: "
					+ song.getAlbum().getName() + " title: " + song.getName());
			return false;
		}
		return true;
	}

	protected void replaceEmptyFieldsWithAlias(ImportSong song) {
		if (Utils.isNullOrEmpty(song.getName(), true)) {
			song.setName(languageHelper.getUnknownTitleAlias());
		}
		if (Utils.isNullOrEmpty(song.getAlbum().getName(), true)) {
			song.getAlbum().setName(languageHelper.getUnknownAlbumAlias());
		}
		if (Utils.isNullOrEmpty(song.getArtist(), true)) {
			song.setArtist(languageHelper.getUnknownArtistAlias());
		}
		if (song.getAlbum().getArtistNames().size() == 0) {
			song.getAlbum().addArtistName(song.getArtist());
		}
		if (song.getAlbum().getArtistNames().size() == 1) {
			if (Utils.isNullOrEmpty(song.getAlbum().getArtistNames().iterator().next(), true)) {
				song.getAlbum().getArtistNames().clear();
				song.getAlbum().addArtistName(song.getArtist());
			}
		}
	}

	protected void groupAlbumsIfNecessary(ImportSong song, HashSet<String> albumNamesToGroup) {
		if (albumNamesToGroup.contains(song.getAlbum().getName())) {
			song.getAlbum().getArtistNames().clear();

			song.getAlbum().addArtistName(languageHelper.getAlbumArtistAlias());
		}
	}

	protected String[] readDirectoryBlacklist() {

		File dirFile = directoryManager.getMusicDirectoriesBlacklistFile();

		if (!dirFile.exists()) {
			return new String[0];
		}

		FileInputStream fileInput = null;
		BufferedReader dirBuffReader = null;
		List<String> dirNames = new ArrayList<String>();
		try {
			fileInput = new FileInputStream(dirFile);
			dirBuffReader = new BufferedReader(new InputStreamReader(fileInput));

			String line = null;

			while ((line = dirBuffReader.readLine()) != null) {
				dirNames.add(line);
			}

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirBuffReader.close();
			} catch (Exception e) {
			}
		}

		String[] blacklist = new String[dirNames.size()];
		for (int i = 0; i < blacklist.length; i++) {
			blacklist[i] = dirNames.get(i);
			Log.v(TAG, "blacklist path " + blacklist[i]);
		}
		return blacklist;
	}

	protected HashSet<String> readFileBlacklist() {
		File dirFile = directoryManager.getMusicFilesBlacklistFile();

		if (!dirFile.exists()) {
			return new HashSet<String>();
		}

		FileInputStream fileInput = null;
		BufferedReader dirBuffReader = null;
		HashSet<String> fileNames = new HashSet<String>();
		try {
			fileInput = new FileInputStream(dirFile);
			dirBuffReader = new BufferedReader(new InputStreamReader(fileInput));

			String line = null;

			while ((line = dirBuffReader.readLine()) != null) {
				fileNames.add(line);
			}

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirBuffReader.close();
			} catch (Exception e) {
			}
		}

		return fileNames;
	}

	protected boolean pathIsBlacklisted(String path) {
		// Reject files from paths on the blacklist
		for (int i = 0; i < directoryBlackList.length; i++) {
			if (path.startsWith(directoryBlackList[i])) {
				// Log.v("Checking path true", song.getPath());
				return true;
			}
		}
		if (fileBlackList.contains(path)) {
			return true;
		}
		// Log.v("Checking path false", song.getPath());
		return false;
	}

	public void scanDirectory(File directory) throws DataUnavailableException {

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

		// Remove all songs that are not in the specified directory, as they should be ignored
		HashMap<String, ImportSong> dbSongsToConsider = new HashMap<String, ImportSong>();
		String dirPath = directory.getAbsolutePath();
		for (String path : dbSongs.keySet()) {
			if (path.startsWith(dirPath)) {
				dbSongsToConsider.put(path, dbSongs.get(path));
			}
		}

		// if db empty, already insert songs during scan, such that a first song
		// is available as soon as possible
		boolean initialImport = dbSongsToConsider.size() == 0;
		HashMap<String, File> audioCollection;
		audioCollection = collectSongsFromFolder(directory);
		processCollection(dbSongsToConsider, audioCollection, albumNamesToGroup, initialImport);

		for (ImportSong s : dbSongsToConsider.values()) {
			libraryChanges.addSongToRemove(s);
		}
	}

	private HashMap<String, File> collectSongsFromFolder(File directory) {
		HashMap<String, File> audioCollection = new HashMap<String, File>();
		if (directory.exists()) {
			addFilesRecursively(directory, audioCollection);
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

	protected void createGenreTable() {
		List<Genre> genreList = genreProvider.getAllGenres();
		genreTable = new HashMap<String, Integer>();
		for (Genre genre : genreList) {
			genreTable.put(genre.getName(), genre.getId());
		}
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
			AudioFile af = AudioFileIO.read(songPath);

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

			duration = af.getLength();
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
}