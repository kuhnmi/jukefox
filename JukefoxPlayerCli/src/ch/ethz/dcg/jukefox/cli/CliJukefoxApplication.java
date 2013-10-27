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
package ch.ethz.dcg.jukefox.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.naturalcli.ExecutionException;
import org.naturalcli.InvalidSyntaxException;
import org.naturalcli.NaturalCLI;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Log.LogLevel;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.commons.utils.SimpleMusicTaste;
import ch.ethz.dcg.jukefox.controller.player.CliPlayerController;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.data.cache.ImportStateListener;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.model.CollectionModelManager;
import ch.ethz.dcg.jukefox.model.PlayerModelManager;
import ch.ethz.dcg.jukefox.model.TagPlaylistGenerator;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.IReadOnlyPlaylist;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.Playlist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.EmptyPlaylistException;
import ch.ethz.dcg.jukefox.model.commons.NoNextSongException;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.model.player.PlayerState;

public class CliJukefoxApplication implements ImportStateListener, IOnPlayerStateChangeListener {

	private final static String TAG = CliJukefoxApplication.class.getSimpleName();

	private final static Integer MAX_NUMBER_OF_RESULTS = 100;

	private static CollectionModelManager collectionModel;
	private static PlayerModelManager playerModel;
	private static CliPlayerController playerController;
	private static LibraryImportManager libraryImportManager;
	private static ModelSettingsManager modelSettingsManager;
	private static DirectoryManager directoryManager;
	private static NaturalCLI commandLineInterface;

	private boolean isPlayerInitialised;
	private static Scanner scanner;

	private static boolean running = true;

	public static void main(String[] args) {

		CommandLineParser parser = new PosixParser();
		LogLevel logLevel = LogLevel.ERROR;
		try {
			CommandLine line = parser.parse(getCliOptions(), args);
			if (line.hasOption(VERBOSE_OPTION)) {
				logLevel = LogLevel.VERBOSE;
			}
		} catch (ParseException e1) {
			Log.e(TAG, "Unexpected exception: " + e1.getMessage());
		}

		printWelcome();

		Log.setLogLevel(logLevel);

		// Send logs async
		new Thread(new Runnable() {

			@Override
			public void run() {
				playerModel.getLogManager().sendLogs();
			}
		}).start();

		CliJukefoxApplication application = new CliJukefoxApplication();

		try {
			scanner = new Scanner(System.in);
			application.start();

			ImportState importState = libraryImportManager.getImportState();
			importState.addListener(application);
			//System.out.println(importState.getProgress().getStatusMessage());

			while (running) {
				String next = scanner.nextLine();
				if (next.isEmpty()) {
					continue;
				}
				try {
					application.getCommandLineInterface().execute(next);
				} catch (ExecutionException e) {
					System.out.println("Invalid input, please use following commands: ");
					application.getCommandLineInterface().execute("help");
				}

			}
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final String VERBOSE_OPTION = "verbose";

	public static Options getCliOptions() {
		Options options = new Options();
		options.addOption("v", VERBOSE_OPTION, false, "show verbose log output");
		return options;
	}

	public CliJukefoxApplication() {

		directoryManager = new DirectoryManager();
		initDirectories();
		collectionModel = new CollectionModelManager(directoryManager);
		libraryImportManager = collectionModel.getLibraryImportManager();
		playerModel = (PlayerModelManager) collectionModel.getPlayerModelManager(TAG);
		modelSettingsManager = collectionModel.getModelSettingsManager();
		playerController = new CliPlayerController(collectionModel, playerModel);
		playerController.addOnPlayerStateChangeListener(this);

		try {
			commandLineInterface = new NaturalCLI(PlayerCommandLineInterface.getCliCommands(this));
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			exit();
		}
	}

	public NaturalCLI getCommandLineInterface() {
		return commandLineInterface;
	}

	public void start() {

		try {
			libraryImportManager.doImportAsync(false, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isPlayerInitialised = true;
	}

	// -----=== Basic Player Functions ===-----

	public void play() {
		if (isPlayerInitialised) {
			playerController.play();
		} else {
			System.out.println("Please do 'init' first.");
		}
	}

	public void playTag(String tagName) {
		Collection<CompleteTag> compTags;
		CompleteTag tagToPlay = null;
		try {
			compTags = collectionModel.getTagProvider().getAllCompleteTags(MAX_NUMBER_OF_RESULTS);
			for (CompleteTag tag : compTags) {
				if (tag.getName().equals(tagName)) {
					tagToPlay = tag;
					break;
				}
			}
			// tag not found search approx. tag
			if (tagToPlay == null) {
				for (CompleteTag tag : compTags) {
					if (tag.getName().toLowerCase().equals(tagName.toLowerCase())) {
						tagToPlay = tag;
						break;
					}
				}
			}
			if (tagToPlay != null) {
				int size = 10;
				try {
					List<PlaylistSong<BaseArtist, BaseAlbum>> songsToAdd = collectionModel.getTagPlaylistGenerator()
							.generatePlaylist(tagToPlay, size,
									TagPlaylistGenerator.DEFAULT_SAMPLE_FACTOR);
					boolean first = true;
					playerController.clearPlaylist();
					for (PlaylistSong<BaseArtist, BaseAlbum> song : songsToAdd) {
						playerController.appendSongAtEnd(song);
						if (first) {
							playerController.play();
							first = false;
						}
					}
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
					return;
				}
			}
		} catch (DataUnavailableException e) {
			Log.w(TAG, "No tags are currently available.");
		}
	}

	public void playGenre(String genreName) {
		List<Genre> compGenres = collectionModel.getGenreProvider().getAllGenres();
		;
		Genre genreToPlay = null;

		for (Genre genre : compGenres) {
			if (genre.getName().equals(genreName)) {
				genreToPlay = genre;
				break;
			}
		}

		// genre not found search approx. genre
		if (genreToPlay == null) {
			for (Genre genre : compGenres) {
				if (genre.getName().toLowerCase().equals(genreName.toLowerCase())) {
					genreToPlay = genre;
					break;
				}
			}
		}

		if (genreToPlay != null) {
			List<BaseSong<BaseArtist, BaseAlbum>> baseSongsToAdd =
					collectionModel.getSongProvider().getAllBaseSongs(genreToPlay);
			boolean first = true;
			playerController.clearPlaylist();
			for (BaseSong<BaseArtist, BaseAlbum> baseSong : baseSongsToAdd) {
				playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(baseSong,
						SongSource.MANUALLY_SELECTED));
				if (first) {
					playerController.play();
					first = false;
				}
			}
		} else {
			System.out.println("Genre '" + genreName + "' was not found!");
		}
	}

	public void playSong(String songName) {
		BaseSong<BaseArtist, BaseAlbum> songToPlay = null;
		List<BaseSong<BaseArtist, BaseAlbum>> compSongs =
				collectionModel.getSongProvider().findBaseSongsBySearchString(songName, MAX_NUMBER_OF_RESULTS);
		if (compSongs.size() > 1) {
			// if there were more than one songs with matching name then the user has to chose
			int index = chooseSong(compSongs);
			if (index > 0) {
				songToPlay = compSongs.get(index - 1);
			} else {
				return;
			}
		} else if (compSongs.size() == 1) {
			// if there is exactly one matching song
			songToPlay = compSongs.get(0);
		}

		if (songToPlay != null) {
			playerController.clearPlaylist();
			playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(songToPlay,
					SongSource.MANUALLY_SELECTED));
			playerController.play();
		} else {
			System.out.println("Song '" + songName + "' was not found!");
		}
	}

	public void playAlbum(String albumName) {
		BaseAlbum albumToPlay = null;
		List<ListAlbum> compAlbums =
				collectionModel.getAlbumProvider().findListAlbumBySearchString(albumName, MAX_NUMBER_OF_RESULTS);

		if (compAlbums.size() > 1) {
			// if there were more than one albums with matching name then the user has to chose
			int index = choseAlbum(compAlbums);
			if (index > 0) {
				albumToPlay = compAlbums.get(index - 1);
			} else {
				return;
			}
		} else if (compAlbums.size() == 1) {
			// if there is exactly one matching album
			albumToPlay = compAlbums.get(0);
		}

		if (albumToPlay != null) {
			List<BaseSong<BaseArtist, BaseAlbum>> baseSongsToAdd =
					collectionModel.getSongProvider().getAllBaseSongs(albumToPlay);
			boolean first = true;
			playerController.clearPlaylist();
			for (BaseSong<BaseArtist, BaseAlbum> baseSong : baseSongsToAdd) {
				playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(baseSong,
						SongSource.MANUALLY_SELECTED));
				if (first) {
					playerController.play();
					first = false;
				}
			}
		} else {
			System.out.println("Album '" + albumName + "' was not found!");
		}
	}

	public void playArtist(String artistName) {
		BaseArtist artistToPlay = null;
		List<BaseArtist> compArtists =
				collectionModel.getArtistProvider().findBaseArtistBySearchString(artistName, MAX_NUMBER_OF_RESULTS);

		if (compArtists.size() > 1) {
			// if there were more than one artists with matching name then the user has to chose
			int index = choseArtist(compArtists);
			if (index > 0) {
				artistToPlay = compArtists.get(index - 1);
			} else {
				return;
			}
		} else if (compArtists.size() == 1) {
			// if there is exactly one matching artist
			artistToPlay = compArtists.get(0);
		}

		if (artistToPlay != null) {
			List<BaseSong<BaseArtist, BaseAlbum>> baseSongsToAdd =
					collectionModel.getSongProvider().getAllBaseSongs(artistToPlay);
			boolean first = true;
			playerController.clearPlaylist();
			for (BaseSong<BaseArtist, BaseAlbum> baseSong : baseSongsToAdd) {
				playerController.appendSongAtEnd(new PlaylistSong<BaseArtist, BaseAlbum>(baseSong,
						SongSource.MANUALLY_SELECTED));
				if (first) {
					playerController.play();
					first = false;
				}
			}
			try {
				playerController.playSongAtPosition(0);
			} catch (PlaylistPositionOutOfRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Artist '" + artistName + "' was not found!");
		}
	}

	public void pause() {
		if (isPlayerInitialised) {
			playerController.pause();
		} else {
			System.out.println("Please do 'init' first.");
		}
	}

	public void stop() {
		if (isPlayerInitialised) {
			playerController.stop();
		} else {
			System.out.println("Please do 'init' first.");
		}
	}

	public void next() {
		if (isPlayerInitialised) {
			try {
				playerController.playNext();
			} catch (EmptyPlaylistException e) {
				System.out.println("Playlist is empty.");
			} catch (NoNextSongException e) {
				System.out.println("No next song.");
			}
		} else {
			System.out.println("Please do 'init' first.");
		}
	}

	public void previous() {
		if (isPlayerInitialised) {
			try {
				playerController.playPrevious();
			} catch (EmptyPlaylistException e) {
				System.out.println("Playlist is empty.");
			} catch (NoNextSongException e) {
				System.out.println("No previous song.");
			}
		} else {
			System.out.println("Please do 'init' first.");
		}
	}

	public void exit() {
		playerController.stop();
		collectionModel.onTerminate();
		System.out.println("good bye...");
		running = false;
	}

	// -----=== Extended Player Functions ===-----

	public void showCurrentPlayMode() {
		PlayModeType playMode = playerController.getPlayMode().getPlayModeType();
		String nicePlayMode = playMode.toString().replaceAll("_", " ").toLowerCase();
		System.out.println("Current play mode is: " + nicePlayMode);
	}

	public void setPlayMode(String playModeName) {
		PlayModeType playModeToSet = null;

		for (PlayModeType playMode : PlayModeType.values()) {
			if (playMode.toString().toLowerCase().equals(playModeName.toLowerCase())) {
				playModeToSet = playMode;
				break;
			}
		}
		// play mode not found search approx. play mode
		if (playModeToSet == null) {
			for (PlayModeType playMode : PlayModeType.values()) {
				String approxPlayMode = playMode.toString().toLowerCase().replaceAll("_", " ");
				if (approxPlayMode.equals(playModeName.toLowerCase())) {
					playModeToSet = playMode;
					break;
				} else if (approxPlayMode.replaceAll(" ", "").equals(playModeName.toLowerCase())) {
					playModeToSet = playMode;
					break;
				}
			}
		}

		int artistAvoidance = 0; // TODO set to settings value for similar play mode

		if (playModeToSet != null) {
			playerController.setPlayMode(playModeToSet, artistAvoidance, Constants.SAME_SONG_AVOIDANCE_NUM);
			showCurrentPlayMode();
		} else {
			System.out.println("Play mode '" + playModeName + "' not found!");
			listPlayModes();
			int playModeValue = promtForOptions(PlayModeType.values().length);
			if (playModeValue > 0) {
				playerController.setPlayMode(PlayModeType.byValue(playModeValue - 1), artistAvoidance,
						Constants.SAME_SONG_AVOIDANCE_NUM);
				showCurrentPlayMode();
			} else {
				return;
			}
		}
	}

	public void listPlayModes() {
		System.out.println("All available play modes:");
		for (int i = 0; i < PlayModeType.values().length; i++) {
			String nicePlayMode = PlayModeType.values()[i].toString().replaceAll("_", " ").toLowerCase();
			if (PlayModeType.values()[i] == playerController.getPlayMode().getPlayModeType()) {
				// current playmode
				System.out.println((i + 1) + ": * " + nicePlayMode);
			} else {
				// Not the current playmode
				System.out.println((i + 1) + ":   " + nicePlayMode);
			}

		}
	}

	public void showCurrentPlaylist() {
		IReadOnlyPlaylist playlist = playerController.getCurrentPlaylist();
		try {
			int curPos = playerController.getCurrentSongIndex();
			int i = 0;
			for (PlaylistSong<BaseArtist, BaseAlbum> song : playlist.getSongList()) {
				if (i == curPos) {
					System.out.print("* ");
				} else {
					System.out.print("  ");
				}
				System.out.println(song.getArtist().getName() + " - " + song.getName());
				i++;
			}
		} catch (EmptyPlaylistException e) {
			System.out.println("Current playlist is empty!");
		}
	}

	public void clearPlaylist() {
		playerController.clearPlaylist();
	}

	public void savePlaylist(String playlistName) {
		IReadOnlyPlaylist playlist = playerController.getCurrentPlaylist();
		playerModel.getPlaylistManager().writePlaylistToFile(playlist, playlistName);
	}

	public void listPlaylistFiles() {
		File[] playlists = playerModel.getPlaylistManager().getPlaylistDirectory().listFiles();

		if (playlists.length > 0) {
			for (File file : playlists) {
				System.out.println(file.getName());
			}
		} else {
			System.out.println("No playlist was found!");
		}
	}

	public void loadPlaylist(String playlistName) {
		Playlist loadedPlaylist = null;
		try {
			loadedPlaylist = playerModel.getPlaylistManager().loadPlaylistFromFileByName(playlistName);
		} catch (DataUnavailableException e) {
			System.out.println("Playlist '" + playlistName + "' was not found!");
		}

		if (loadedPlaylist != null) {
			playerController.clearPlaylist();
			playerController.setPlaylist(loadedPlaylist);
			System.out.println("New playlist was successfully loaded.\n");
			playerController.play();
		} else {
			System.out.println("Can't load '" + playlistName + "'!");
		}
	}

	public void listTags() {
		Collection<CompleteTag> compTags;
		try {
			compTags = collectionModel.getTagProvider().getAllCompleteTags(MAX_NUMBER_OF_RESULTS);
			List<CompleteTag> tags = new ArrayList<CompleteTag>(compTags);
			Collections.sort(tags);
			for (CompleteTag tag : tags) {
				System.out.println(tag.getName());
			}
		} catch (DataUnavailableException e) {
			Log.w(TAG, "No tags are currently available.");
		}
	}

	public void listGenres() {
		List<Genre> genres = collectionModel.getGenreProvider().getAllGenres();
		if (genres.size() > 0) {
			for (Genre genre : genres) {
				System.out.println(genre.getName());
			}
		} else {
			System.out.println("No genres are currently available.");
		}
	}

	public void listArtists() {
		List<BaseArtist> artists = collectionModel.getArtistProvider().getAllArtists();
		if (artists.size() > 0) {
			for (BaseArtist artist : artists) {
				System.out.println(artist.getName());
			}
		} else {
			System.out.println("No artists are currently available.");
		}
	}

	public void listAlbums() {
		List<ListAlbum> albums = collectionModel.getAlbumProvider().getAllListAlbums();
		if (albums.size() > 0) {
			for (ListAlbum album : albums) {
				System.out.println(album.getName());
			}
		} else {
			System.out.println("No genres are currently available.");
		}
	}

	// -----=== Core Functions ===-----

	public void init(boolean clearDb, boolean reduced) {

		try {
			libraryImportManager.doImportAsync(clearDb, reduced);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isPlayerInitialised = true;
	}

	public void addLibraryPath(String path) {
		File libraryPath = new File(path);
		if (libraryPath.isDirectory()) {
			modelSettingsManager.addLibraryPath(path);
			System.out.println("Path successfully added: " + path);
		} else {
			System.out.println("Path is invalid: " + path);
		}

	}

	public void listLibraryPaths() {
		HashSet<String> paths = modelSettingsManager.getLibraryPaths();
		if (paths.isEmpty()) {
			System.out.println("No paths stored in settings.");
			return;
		}
		Iterator<String> iterator = paths.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}
	}

	public void delLibraryPath(String path) {
		if (modelSettingsManager.getLibraryPaths().contains(path)) {
			modelSettingsManager.removeLibraryPath(path);
			System.out.println("Path successfully removed: " + path);
		} else {
			System.out.println("Path is not in settings: " + path);
		}
	}

	// -----=== Debug Functions ===-----

	public void displayNumSongs() {
		try {
			System.out.println("Number of songs in Collection: " + collectionModel.getOtherDataProvider()
					.getNumberOfSongsWithCoordinates());
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
	}

	public void clearDb() {
		collectionModel.getDbAccessProvider().resetDatabase();
		System.out.println("DB is now cleared! Do init again...");
	}

	// ----------------------------------------------------------

	private void initDirectories() {
		if (directoryManager.isDirectoryMissing()) {
			try {
				directoryManager.deleteDirectories();
				directoryManager.createAllDirectories();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public void onAlbumCoversFetched() {

	}

	@Override
	public void onBaseDataCommitted() {

	}

	@Override
	public void onCoordinatesFetched() {

	}

	@Override
	public void onImportAborted(boolean hadChanges) {

	}

	@Override
	public void onImportCompleted(boolean hadChanges) {
		System.out.println("Finished importing library.");
	}

	@Override
	public void onImportProblem(Throwable e) {
	}

	@Override
	public void onImportStarted() {
		System.out.println("Importing library ...");
	}

	private int chooseSong(List<BaseSong<BaseArtist, BaseAlbum>> baseSongs) {
		System.out.println();
		for (int i = 0; i < baseSongs.size(); i++) {
			System.out.println((i + 1) + ": " + baseSongs.get(i).toString());
		}
		return promtForOptions(baseSongs.size());
	}

	private int choseArtist(List<BaseArtist> artists) {
		System.out.println();
		for (int i = 0; i < artists.size(); i++) {
			System.out.println((i + 1) + ": " + artists.get(i).toString());
		}
		return promtForOptions(artists.size());
	}

	private int choseAlbum(List<ListAlbum> albums) {
		System.out.println();
		for (int i = 0; i < albums.size(); i++) {
			System.out.println((i + 1) + ": " + albums.get(i).getArtists().get(0).toString() + " - " + albums.get(i)
					.getName());
		}
		return promtForOptions(albums.size());
	}

	/**
	 * Asks the user to enter an {@link Integer}.
	 * 
	 * @param maxNumberOfOptions
	 *            The upper bound for the options
	 * @return The chosen option as {@link Integer} or -1 if the result isn't valid
	 */
	private int promtForOptions(int maxNumberOfOptions) {
		int result = -1;
		System.out.println();

		// Try 3 times to get a valid result
		for (int i = 0; i < 3; i++) {
			System.out.print("Please chose one of the results from the list above: ");
			String next = scanner.nextLine();
			if (next.isEmpty()) {
				break;
			}
			int parsedInt;
			try {
				parsedInt = Integer.parseInt(next);
			} catch (NumberFormatException e) {
				System.out.println("The option has to be an integer.");
				continue;
			}

			// Check if the chosen option is possible
			if (parsedInt > 0 && parsedInt <= maxNumberOfOptions) {
				result = parsedInt;
				break;
			} else {
				System.out.println("The option has to be between 1 and " + maxNumberOfOptions + ".");
			}
		}

		if (result == -1) {
			System.out.println("No option was chosen.");
		}
		System.out.println();
		return result;
	}

	public static void printWelcome() {
		System.out.println(SplashLogo.getSplashLogoSmall());
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {
		// do nothing
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
		// do nothing		
	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		// do nothing
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		System.out.println(String.format("Now playing: %s [%dms]", song, song.getDuration()));
	}

	/**
	 * Seek to a specified position in the current song
	 * 
	 * @param position
	 *            The position to seek to. The string is interpreted as seconds except if a percentage sign is added at
	 *            the end (then interpreted as percentage of the duration of the current song).
	 */
	public void seektTo(String position) {
		if (position == null || position.length() < 1) {
			System.out.println("Could not parse position to seek to.");
			return;
		}
		int seekPos = -1;
		if (position.contains("%")) {
			String[] parts = position.split("%");
			if (parts.length < 1) {
				System.out.println("Could not parse position to seek to.");
				return;
			}
			seekPos = Integer.parseInt(parts[0]) * playerController.getDuration() / 100;
		} else {
			seekPos = Integer.parseInt(position) * 1000;
		}
		if (seekPos < 0 || seekPos > playerController.getDuration()) {
			System.out.println("Invalid seek to position: " + seekPos);
			return;
		}
		System.out.println("Seek to position: " + seekPos + "ms");
		playerController.seekTo(seekPos);
	}

	public void computeMusicTaste() {
		List<BaseSong<BaseArtist, BaseAlbum>> songs = collectionModel.getSongProvider().getAllBaseSongs();
		List<Pair<float[], Integer>> weightedPreferences = new LinkedList<Pair<float[], Integer>>();
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			try {
				weightedPreferences.add(new Pair<float[], Integer>(collectionModel.getSongCoordinatesProvider()
						.getSongCoordinates(song).getCoords(),
						1));
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
		}
		SimpleMusicTaste taste = new SimpleMusicTaste(weightedPreferences, 10);
		List<BaseSong<BaseArtist, BaseAlbum>> compSongs =
				collectionModel.getSongProvider().findBaseSongsBySearchString("No Way Back", MAX_NUMBER_OF_RESULTS);
		if (compSongs.size() > 0) {
			float[] coords;
			try {
				coords = collectionModel.getSongCoordinatesProvider()
						.getSongCoordinates(compSongs.get(0)).getCoords();
				Log.v(TAG, "No Way Back: " + taste.getRating(coords));
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
			float[] coords2 = { 0f, 1.90928e-16f, 0.0016195f, 0.00505543f, 0.000970416f, 0.0490976f, 1.69613e-31f,
					0.000645545f, 0.0131592f, 0.0600449f, 0.000892332f, 0.0457037f, 3.44343e-19f, 0.00308876f,
					0.0472867f, 0.0423707f, 0.000804705f, 0.00167689f, 0.14295f, 0.0189533f, 0.232698f,
					4.04022e-22f, 0.268841f, 0.00032636f, 0.00128004f, 0.00993646f, 0.0210824f, 0.000287654f,
					0.000712417f, 0.00114409f, 0.026492f, 0.0028783f };
			float[] coords3 = { 0.0154382f, 0.00956529f, 0.000183891f, 0.000583008f, 0.000417118f, 5.0891e-05f,
					0.000164999f, 0.000145732f, 0.000174763f, 0.0058261f, 0.000440863f, 0.0136763f, 5.6058e-05f,
					0.00475601f, 0.000676719f, 0.0221654f, 0.00376773f, 1.65927e-23f, 0.00260623f, 3.58366e-05f,
					0.00961327f, 0.772598f, 0.100499f, 0.0023948f, 6.62612e-05f, 0.000291621f, 6.03924e-16f,
					0.000621412f, 0.000808095f, 0.00492318f, 4.99822e-05f, 0.0274027f };
			float[] coords4 = { 8.11304e-05f, 0.0038187f, 6.27423e-05f, 0.0008956f, 0.000187512f, 0.0057181f,
					0.000118811f, 0.000657578f, 0.000154475f, 0.00423853f, 0.00151931f, 0.0390971f, 0.000978525f,
					4.48275e-05f, 0.000159531f, 0.0275912f, 0.00034222f, 0.00104841f, 0.0114363f, 8.21533e-05f,
					0.117015f, 0.000616639f, 0.00428872f, 0.000374882f, 0.0160043f, 0.00137019f, 5.90836e-05f,
					1.42985e-05f, 0.00044856f, 0.725169f, 0.0344517f, 0.00195414f };
			Log.v(TAG, "Wolfmother: " + taste.getRating(coords2));
			Log.v(TAG, "Nightwish: " + taste.getRating(coords3));
			Log.v(TAG, "Britney Spears: " + taste.getRating(coords4));
		}
	}

	public void showImportStatus() {
		ImportState status = collectionModel.getLibraryImportManager().getImportState();
		System.out.println(status.getProgress().toString());
	}
}
