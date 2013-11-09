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

import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.data.context.IContextProvider;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.log.LogManager;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLog;
import ch.ethz.dcg.jukefox.model.providers.LogProvider;
import ch.ethz.dcg.jukefox.model.providers.PlayerPlaylistManager;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.model.rating.RatingHelper;

/**
 * This class manages all player specific providers and the current profile.
 */
public abstract class AbstractPlayerModelManager {

	private final int id;

	private final IContextProvider contextProvider;
	private final LogManager logManager;
	private final PlayerPlaylistManager playlistManager;
	private final PlayLog playLog;
	private final RatingHelper ratingHelper;

	private final LogProvider logProvider;
	private final StatisticsProvider statisticsProvider;

	/**
	 * Creates a new instance of {@link AbstractPlayerModelManager}
	 */
	public AbstractPlayerModelManager(AbstractCollectionModelManager collectionModelManager, String name,
			IDbDataPortal dbDataPortal) {
		try {
			id = dbDataPortal.insertOrGetPlayerModelId(name);
		} catch (DataWriteException e) {
			throw new RuntimeException(e);
		}

		contextProvider = createContextProvider();

		logProvider = new LogProvider(id, dbDataPortal.getLogHelper());
		statisticsProvider = new StatisticsProvider(id, dbDataPortal);

		logManager = new LogManager(logProvider, collectionModelManager.getLanguageHelper(),
				collectionModelManager.getModelSettingsManager());
		ratingHelper = new RatingHelper(id, dbDataPortal, collectionModelManager.getSongProvider(),
				collectionModelManager.getOtherDataProvider(), logManager);
		playLog = new PlayLog(contextProvider, id, collectionModelManager.getSongProvider(),
				collectionModelManager.getTagProvider(), collectionModelManager.getTagPlaylistGenerator(),
				dbDataPortal, collectionModelManager.getModelSettingsManager(), ratingHelper);
		playlistManager = new PlayerPlaylistManager(dbDataPortal, name, collectionModelManager.getDirectoryManager());
	}

	/**
	 * Gets the id of this player model manager
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gets the {@link PlayLog}
	 */
	public PlayLog getPlayLog() {
		return playLog;
	}

	/**
	 * Gets the {@link RatingHelper}.
	 * 
	 * @return The rating helper
	 */
	public RatingHelper getRatingHelper() {
		return ratingHelper;
	}

	/**
	 * Gets the {@link LogProvider}
	 */
	public LogProvider getLogProvider() {
		return logProvider;
	}

	/**
	 * Gets the {@link StatisticsProvider}
	 */
	public StatisticsProvider getStatisticsProvider() {
		return statisticsProvider;
	}

	/**
	 * Gets the {@link IContextProvider}
	 */
	public IContextProvider getContextProvider() {
		return contextProvider;
	}

	/**
	 * Creates a new context provider
	 */
	protected abstract IContextProvider createContextProvider();

	/**
	 * 
	 */
	public PlayerPlaylistManager getPlaylistManager() {
		return playlistManager;
	}

	/**
	 * Returns the {@link LogManager}
	 */
	public LogManager getLogManager() {
		return logManager;
	}

	/**
	 * Is called before the CollectionModel and the PlayerModel are destroyed
	 */
	public void onTerminate() {

	}
}
