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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.LanguageHelper;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.db.SqlJdbcDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.AlbumCoverFetcherListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.AlbumCoverFetcherThreadFactory;
import ch.ethz.dcg.jukefox.manager.libraryimport.GenreManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryScanner;

public class CollectionModelManager extends AbstractCollectionModelManager {

	public CollectionModelManager(DirectoryManager directoryManager) {
		super(directoryManager);
	}

	@Override
	protected IDbDataPortal createDbDataPortal() {
		return new SqlJdbcDbDataPortal(directoryManager);
	}

	@Override
	protected AbstractPlayerModelManager createPlayerModelManager(String name, IDbDataPortal dbDataPortal) {
		return new PlayerModelManager(this, name, dbDataPortal);
	}

	@Override
	protected LibraryImportManager createLibraryImportManager() {
		LibraryScanner libraryScanner = new LibraryScanner(this, importState);

		List<AlbumCoverFetcherListener> coverFetcherListeners = new LinkedList<AlbumCoverFetcherListener>();
		BlockingQueue<Integer> albumIdQueue = new LinkedBlockingQueue<Integer>();

		AlbumCoverFetcherThreadFactory aFactory = new AlbumCoverFetcherThreadFactory(this, albumIdQueue,
				coverFetcherListeners, importState);

		GenreManager genreManager = new GenreManager(this, importState);

		LibraryImportManager libraryImportManager = new LibraryImportManager(libraryScanner, aFactory, this,
				genreManager, importState);
		return libraryImportManager;
	}

	@Override
	public AbstractLanguageHelper getLanguageHelper() {
		return new LanguageHelper();
	}

}
