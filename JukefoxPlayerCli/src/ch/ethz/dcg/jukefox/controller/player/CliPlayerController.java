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
package ch.ethz.dcg.jukefox.controller.player;

import ch.ethz.dcg.jukefox.controller.player.mediaplayer.MiniMediaPlayerWrapper;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.BasePlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.GaplessPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playbackcontroller.IPlaybackController;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.IPlaylistManager;
import ch.ethz.dcg.jukefox.controller.player.playlistmanager.PlaylistManager;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;

public class CliPlayerController extends AbstractPlayerController {

	public CliPlayerController(AbstractCollectionModelManager model,
			AbstractPlayerModelManager playerModel) {
		super(model, playerModel);
	}

	@Override
	protected IPlaybackController createPlaybackController(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel,
			IPlaylistManager playlistManager) {
		IPlaybackController player = null;

		MiniMediaPlayerWrapper mediaPlayer1 = new MiniMediaPlayerWrapper();

		//TODO Gapless is not usable... because MiniJavaPlayer does not supporting multiple instances:

		//Exception in thread "player-playback-thread" java.lang.IllegalArgumentException
		//at maryb.player.PlayerThread.openInputStream(PlayerThread.java:149)
		//at maryb.player.PlayerThread.run(PlayerThread.java:251)

		player = new BasePlaybackController(listenerInformer,
				collectionModel, playerModel, currentPlaylistManager,
				mediaPlayer1);

		// Gapless play
		MiniMediaPlayerWrapper mediaPlayer2 = new MiniMediaPlayerWrapper();
		player = new GaplessPlaybackController(listenerInformer, collectionModel, playerModel, playlistManager, 0,
				0, mediaPlayer1, mediaPlayer2);

		return player;
	}

	@Override
	protected IPlaylistManager createPlaylistManager(IPlaybackInfoBroadcaster listenerInformer,
			AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		return new PlaylistManager(collectionModel, playerModel, this, this);
	}

}
