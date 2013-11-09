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
package ch.ethz.dcg.jukefox.model;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.AndroidLanguageHelper;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.db.SqlAndroidDbDataPortal;
import ch.ethz.dcg.jukefox.manager.AndroidDirectoryManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.AlbumCoverFetcherListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.AlbumCoverFetcherThreadFactory;
import ch.ethz.dcg.jukefox.manager.libraryimport.AndroidGenreManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.AndroidLibraryScanner;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.manager.model.CursorProvider;
import ch.ethz.dcg.jukefox.manager.model.albumart.AlbumArtProvider;
import ch.ethz.dcg.pancho3.model.AndroidApplicationStateManager;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.model.PlaylistImporter;

public class AndroidCollectionModelManager extends AbstractCollectionModelManager {

	private AlbumArtProvider albumArtProvider;
	private AndroidApplicationStateManager applicationStateManager;
	private PlaylistImporter playlistImporter;
	private JukefoxApplication application;
	private CursorProvider cursorProvider;

	/**
	 * Creates a new instance of {@link AndroidCollectionModelManager}
	 */
	public AndroidCollectionModelManager(JukefoxApplication application, AndroidDirectoryManager directoryManager) {
		super(directoryManager);
		this.application = application;
		albumArtProvider = new AlbumArtProvider(dbDataPortal);
		applicationStateManager = new AndroidApplicationStateManager(this);
		playlistImporter = new PlaylistImporter(this);
		cursorProvider = new CursorProvider((SqlAndroidDbDataPortal) dbDataPortal);
	}

	@Override
	protected IDbDataPortal createDbDataPortal() {
		return new SqlAndroidDbDataPortal(directoryManager, JukefoxApplication.getInstance());
	}

	@Override
	protected AbstractPlayerModelManager createPlayerModelManager(String name, IDbDataPortal dbDataPortal) {
		return new AndroidPlayerModelManager(this, name, dbDataPortal);
	}

	public AndroidApplicationStateManager getApplicationStateManager() {
		return applicationStateManager;
	}

	public AlbumArtProvider getAlbumArtProvider() {
		return albumArtProvider;
	}

	public PlaylistImporter getPlaylistImporter() {
		return playlistImporter;
	}

	public CursorProvider getCursorProvider() {
		return cursorProvider;
	}

	@Override
	protected LibraryImportManager createLibraryImportManager() {

		AndroidLibraryScanner libraryScanner = new AndroidLibraryScanner(this, importState, JukefoxApplication
				.getInstance());
		List<AlbumCoverFetcherListener> coverFetcherListeners = new LinkedList<AlbumCoverFetcherListener>();
		AlbumCoverFetcherThreadFactory aFactory = new AlbumCoverFetcherThreadFactory(this,
				coverFetcherListeners, importState, JukefoxApplication.getInstance());

		AndroidGenreManager genreManager = new AndroidGenreManager(this, importState, JukefoxApplication.getInstance()
				.getContentResolver());
		LibraryImportManager libraryImportManager = new LibraryImportManager(libraryScanner, aFactory, this,
				genreManager, importState);
		return libraryImportManager;
	}

	@Override
	public AbstractLanguageHelper getLanguageHelper() {
		return new AndroidLanguageHelper();
	}
}
