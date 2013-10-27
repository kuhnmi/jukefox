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

import java.util.HashMap;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.cache.PreloadedDataManager;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.manager.ResourceLoaderManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.AlbumProvider;
import ch.ethz.dcg.jukefox.model.providers.ArtistProvider;
import ch.ethz.dcg.jukefox.model.providers.CollectionPlaylistProvider;
import ch.ethz.dcg.jukefox.model.providers.DbAccessProvider;
import ch.ethz.dcg.jukefox.model.providers.GenreProvider;
import ch.ethz.dcg.jukefox.model.providers.ModifyProvider;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongCoordinatesProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.providers.TagProvider;

/**
 * This class manages all collection model providers and changes in the model.
 */
public abstract class AbstractCollectionModelManager {

	private static final String TAG = AbstractCollectionModelManager.class.getSimpleName();

	private HashMap<String, AbstractPlayerModelManager> playerModelManagers;

	// ---===== Data Manages & Other =====---

	private final PreloadedDataManager preloadedDataManager;
	protected final IDbDataPortal dbDataPortal;
	private final ResourceLoaderManager resourceLoaderManager;
	protected final ImportState importState;
	private final TagPlaylistGenerator tagPlaylistGenerator;
	protected final DirectoryManager directoryManager;
	protected final ModelSettingsManager modelSettingsManager;

	// ---===== Model Providers =====---

	private final AlbumProvider albumProvider;
	private final ArtistProvider artistProvider;
	private final DbAccessProvider dbAccessProvider;
	private final GenreProvider genreProvider;
	private final OtherDataProvider otherDataProvider;
	private final CollectionPlaylistProvider playlistProvider;
	private final SongProvider songProvider;
	private final SongCoordinatesProvider songCoordinatesProvider;
	private final TagProvider tagProvider;
	private final ModifyProvider modifyProvider;

	private final LibraryImportManager libraryImportManager;

	/**
	 * Creates a new instance of {@link AbstractCollectionModelManager}
	 */
	protected AbstractCollectionModelManager(DirectoryManager directoryManager) {
		this.directoryManager = directoryManager;
		modelSettingsManager = new ModelSettingsManager(directoryManager);
		playerModelManagers = new HashMap<String, AbstractPlayerModelManager>();
		importState = new ImportState();
		this.dbDataPortal = createDbDataPortal();

		resourceLoaderManager = new ResourceLoaderManager(dbDataPortal, directoryManager);
		preloadedDataManager = new PreloadedDataManager(dbDataPortal, resourceLoaderManager, importState,
				modelSettingsManager, directoryManager);
		try {
			preloadedDataManager.loadData();
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}

		songProvider = new SongProvider(dbDataPortal, preloadedDataManager);
		songCoordinatesProvider = new SongCoordinatesProvider(dbDataPortal, preloadedDataManager);

		tagPlaylistGenerator = new TagPlaylistGenerator(songCoordinatesProvider, songProvider);

		albumProvider = new AlbumProvider(dbDataPortal, preloadedDataManager);
		artistProvider = new ArtistProvider(dbDataPortal);
		dbAccessProvider = new DbAccessProvider(dbDataPortal);
		genreProvider = new GenreProvider(dbDataPortal);
		otherDataProvider = new OtherDataProvider(dbDataPortal, preloadedDataManager);
		playlistProvider = new CollectionPlaylistProvider(dbDataPortal);
		tagProvider = new TagProvider(dbDataPortal, preloadedDataManager);
		modifyProvider = new ModifyProvider(dbDataPortal, preloadedDataManager, directoryManager);

		libraryImportManager = createLibraryImportManager();
	}

	/**
	 * Gets a {@link AbstractPlayerModelManager} with the given name
	 * 
	 * @param name
	 *            The name ({@link String}) of the returned {@link AbstractPlayerModelManager}. All characters are
	 *            automatically converted into lower case.
	 * @return A {@link AbstractPlayerModelManager} with the given name
	 */
	public AbstractPlayerModelManager getPlayerModelManager(String name) {
		AbstractPlayerModelManager result = null;
		name = name.toLowerCase();

		if (playerModelManagers.containsKey(name)) {
			result = playerModelManagers.get(name);
		} else {
			result = createPlayerModelManager(name, dbDataPortal);
			playerModelManagers.put(name, result);
		}

		return result;
	}

	public IDbDataPortal getDbDataPortal() {
		return dbDataPortal;
	}

	/**
	 * Gets the library import manager
	 */
	public LibraryImportManager getLibraryImportManager() {
		return libraryImportManager;
	}

	/**
	 * Returns the directory manager
	 */
	public DirectoryManager getDirectoryManager() {
		return directoryManager;
	}

	/**
	 * Gets the {@link ResourceLoaderManager}
	 */
	public ResourceLoaderManager getResourceLoaderManager() {
		return resourceLoaderManager;
	}

	/**
	 * Gets the {@link ModelSettingsManager}
	 */
	public ModelSettingsManager getModelSettingsManager() {
		return modelSettingsManager;
	}

	/**
	 * Gets the tag playlist generator
	 */
	public TagPlaylistGenerator getTagPlaylistGenerator() {
		return tagPlaylistGenerator;
	}

	/**
	 * Gets the language helper
	 */
	public abstract AbstractLanguageHelper getLanguageHelper();

	// -----=== Provider Getter ===-----

	/**
	 * Gets the {@link AlbumProvider}
	 */
	public AlbumProvider getAlbumProvider() {
		return albumProvider;
	}

	/**
	 * Gets the {@link ArtistProvider}
	 */
	public ArtistProvider getArtistProvider() {
		return artistProvider;
	}

	/**
	 * Gets the {@link DbAccessProvider}
	 */
	public DbAccessProvider getDbAccessProvider() {
		return dbAccessProvider;
	}

	/**
	 * Gets the {@link GenreProvider}
	 */
	public GenreProvider getGenreProvider() {
		return genreProvider;
	}

	/**
	 * Gets the {@link OtherDataProvider}
	 */
	public OtherDataProvider getOtherDataProvider() {
		return otherDataProvider;
	}

	/**
	 * Gets the {@link CollectionPlaylistProvider}
	 */
	public CollectionPlaylistProvider getPlaylistProvider() {
		return playlistProvider;
	}

	/**
	 * Gets the {@link SongProvider}
	 */
	public SongProvider getSongProvider() {
		return songProvider;
	}

	/**
	 * Gets the {@link SongCoordinatesProvider}
	 */
	public SongCoordinatesProvider getSongCoordinatesProvider() {
		return songCoordinatesProvider;
	}

	/**
	 * Gets the {@link TagProvider}
	 */
	public TagProvider getTagProvider() {
		return tagProvider;
	}

	/**
	 * Gets the {@link ModifyProvider}
	 */
	public ModifyProvider getModifyProvider() {
		return modifyProvider;
	}

	public void onTerminate() {
		for (AbstractPlayerModelManager pmm : playerModelManagers.values()) {
			pmm.onTerminate();
		}
		// Never forget to do this!
		dbAccessProvider.close();
	}

	/**
	 * Creates a new data base data portal.
	 */
	protected abstract IDbDataPortal createDbDataPortal();

	/**
	 * Creates a new {@link AbstractPlayerModelManager} with the given name
	 * 
	 * @param name
	 *            The name ({@link String}) of the returned {@link AbstractPlayerModelManager}
	 * @param dbDataPortal
	 *            The database data portal which will be used
	 * @return A new created {@link AbstractPlayerModelManager} with the given name
	 */
	protected abstract AbstractPlayerModelManager createPlayerModelManager(String name, IDbDataPortal dbDataPortal);

	/**
	 * Creates a new {@link LibraryImportManager}
	 */
	protected abstract LibraryImportManager createLibraryImportManager();

}
