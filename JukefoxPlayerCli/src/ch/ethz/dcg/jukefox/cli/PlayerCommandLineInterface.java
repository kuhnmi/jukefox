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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.naturalcli.Command;
import org.naturalcli.ICommandExecutor;
import org.naturalcli.InvalidSyntaxException;
import org.naturalcli.ParseResult;

/**
 * A command line interface for the player with the most common functions implemented
 */
public class PlayerCommandLineInterface {

	public static Set<Command> getCliCommands(final CliJukefoxApplication application) throws InvalidSyntaxException {

		// -----=== Basic Player Functions ===-----

		Command play =
				new Command(
						"play",
						"Start playback.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.play();
							}
						}
				);

		Command playTag =
				new Command(
						"play tag <name:string> ...",
						"Play a new created playlist using a tag.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String tag = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									tag = tag + " " + pr.getParameterValue(i);
								}
								application.playTag(tag);
							}
						}
				);

		Command playGenre =
				new Command(
						"play genre <name:string> ...",
						"Play a new created playlist using a genre.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String genre = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									genre = genre + " " + pr.getParameterValue(i);
								}
								application.playGenre(genre);
							}
						}
				);

		Command playSong =
				new Command(
						"play song <name:string> ...",
						"Play a new created playlist using a song.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String song = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									song = song + " " + pr.getParameterValue(i);
								}
								application.playSong(song);
							}
						}
				);

		Command playAlbum =
				new Command(
						"play album <name:string> ...",
						"Play a new created playlist using an album.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String album = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									album = album + " " + pr.getParameterValue(i);
								}
								application.playAlbum(album);
							}
						}
				);

		Command playArtist =
				new Command(
						"play artist <name:string> ...",
						"Play a new created playlist using an artist.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String artist = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									artist = artist + " " + pr.getParameterValue(i);
								}
								application.playArtist(artist);
							}
						}
				);

		Command pause =
				new Command(
						"pause",
						"Pause playback.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.pause();
							}
						}
				);

		Command stop =
				new Command(
						"stop",
						"Stop playback.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.stop();
							}
						}
				);

		Command next =
				new Command(
						"next",
						"Play next song.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.next();
							}
						}
				);

		Command previous =
				new Command(
						"previous",
						"Play previous song.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.previous();
							}
						}
				);

		Command exit =
				new Command(
						"exit",
						"Exit player.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.exit();
							}
						}
				);

		Command seekto =
				new Command(
						"seekto <position:string> ...",
						"Seek to a position in the current song.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String position = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									position = position + " " + pr.getParameterValue(i);
								}
								application.seektTo(position);
							}
						}
				);

		Command about =
				new Command(
						"about",
						"About this player.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								CliJukefoxApplication.printWelcome();
							}
						}
				);

		// -----=== Extended Player Functions ===-----

		Command showPlayMode =
				new Command(
						"show playmode",
						"Show the current playmode.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.showCurrentPlayMode();
							}
						}
				);

		Command setPlayMode =
				new Command(
						"set playmode <name:string> ...",
						"Set a new play mode type.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String playMode = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									playMode = playMode + " " + pr.getParameterValue(i);
								}
								application.setPlayMode(playMode);
							}
						}
				);

		Command listPlayModes =
				new Command(
						"list playmodes",
						"List all available playmodes.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listPlayModes();
							}
						}
				);

		Command showPlaylist =
				new Command(
						"show playlist",
						"Show the current playlist.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.showCurrentPlaylist();
							}
						}
				);

		Command clearPlaylist =
				new Command(
						"clear playlist",
						"Clear the current playlist.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.clearPlaylist();
							}
						}
				);

		Command savePlaylist =
				new Command(
						"save playlist <name:string> ...",
						"Save the current playlist.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String playlist = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									playlist = playlist + " " + pr.getParameterValue(i);
								}
								application.savePlaylist(playlist);
							}
						}
				);

		Command listPlaylists =
				new Command(
						"list playlists",
						"List all available playlist files.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listPlaylistFiles();
							}
						}
				);

		Command loadPlaylist =
				new Command(
						"load playlist <name:string> ...",
						"Load a playlist and play it.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String playlist = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									playlist = playlist + " " + pr.getParameterValue(i);
								}
								application.loadPlaylist(playlist);
							}
						}
				);

		Command listTags =
				new Command(
						"list tags",
						"List all available tags.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listTags();
							}
						}
				);

		Command listGenres =
				new Command(
						"list genres",
						"List all available genres.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listGenres();
							}
						}
				);

		Command listArtists =
				new Command(
						"list artists",
						"List all available artists.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listArtists();
							}
						}
				);

		Command listAlbums =
				new Command(
						"list albums",
						"List all available albums.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listAlbums();
							}
						}
				);

		// -----=== Core Functions ===-----

		Command init =
				new Command(
						"init [<scanType:integer>]",
						"Importing Library. (scanType: 0 [default] = noClearDb,reduced; 1 = clearDb,noReduced; 2 = noClearDb,noReduced)",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								Integer p0 = null;
								if (pr.getParameterCount() > 0) {
									p0 = (Integer) pr.getParameterValue(0);
								}
								if (p0 == null) {
									p0 = 0;
								}

								boolean clearDb = (p0 & 1) != 0;
								boolean noReduced = (p0 & (1 << 1)) != 0;

								application.init(clearDb, !noReduced);
							}
						}
				);

		Command showImportStatus =
				new Command(
						"show import status",
						"Display the status of the runnning import.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.showImportStatus();
							}
						}
				);

		Command addLibraryPath =
				new Command(
						"add path <name:string> ...",
						"Add a new library path (like C:\\User).",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String path = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									path = path + " " + pr.getParameterValue(i);
								}
								application.addLibraryPath(path);

							}
						}
				);

		Command listLibraryPaths =
				new Command(
						"list paths",
						"Lists all library paths.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.listLibraryPaths();
							}
						}
				);

		Command delLibraryPath =
				new Command(
						"del path <name:string> ...",
						"Add a library path (like C:\\User).",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								String path = pr.getParameterValue(0).toString();
								for (int i = 1; i < pr.getParameterCount(); i++) {
									path = path + " " + pr.getParameterValue(i);
								}
								application.delLibraryPath(path);
							}
						}
				);

		// -----=== Debug Functions ===-----

		Command numSongs =
				new Command(
						"numsongs",
						"Display the number of known songs.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.displayNumSongs();
							}
						}
				);

		Command clearDb =
				new Command(
						"clear db",
						"Clear the database.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.clearDb();
							}
						}
				);
		Command taste =
				new Command(
						"taste",
						"Compute the music taste",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								application.computeMusicTaste();
							}
						}
				);

		// --------------------------------------------------------------

		// Create the set of commands
		//Set<Command> cs = new HashSet<Command>();
		//cs.add(new HelpCommand(cs));
		final List<Command> cs = new ArrayList<Command>();

		// --- Basic Player Functions ---
		cs.add(play);
		cs.add(playTag);
		cs.add(playArtist);
		cs.add(playGenre);
		cs.add(playAlbum);
		cs.add(playSong);
		cs.add(pause);
		cs.add(stop);
		cs.add(next);
		cs.add(previous);
		cs.add(exit);
		cs.add(seekto);
		cs.add(about);

		// --- Extended Player Functions ---
		cs.add(showPlayMode);
		cs.add(setPlayMode);
		cs.add(listPlayModes);
		cs.add(showPlaylist);
		cs.add(clearPlaylist);
		cs.add(savePlaylist);
		cs.add(listPlaylists);
		cs.add(loadPlaylist);
		cs.add(listTags);
		cs.add(listGenres);
		cs.add(listArtists);
		cs.add(listAlbums);

		// --- Core Functions ---
		cs.add(init);
		cs.add(showImportStatus);
		cs.add(addLibraryPath);
		cs.add(listLibraryPaths);
		cs.add(delLibraryPath);

		// --- Debug Functions ---
		cs.add(numSongs);
		cs.add(clearDb);
		cs.add(taste);

		Command customHelp =
				new Command(
						"help",
						"Shows the commands help.",
						new ICommandExecutor() {

							@Override
							public void execute(ParseResult pr) {
								System.out.println();

								// calculate the longest command syntax
								int maxLength = 0;
								for (Command c : cs) {
									if (maxLength < c.getSyntax().toString().length()) {
										maxLength = c.getSyntax().toString().length();
									}
								}

								for (Command c : cs)
								{
									if (c.isHidden()) {
										continue;
									}
									System.out.println(String.format("%-" + (maxLength + 1) + "s| %s", c.getSyntax(),
											c.getHelp()));
								}
							}
						}
				);

		cs.add(customHelp);

		HashSet<Command> result = new HashSet<Command>(cs);
		return result;
	}
}
