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
package ch.ethz.dcg.jukefox.playmode.smartshuffle.agents;

import java.util.List;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SongVote;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;

/**
 * An agent in a first stage returns songs which should be considered for playing next and rates given songs by its
 * knowledge of how well it will be accepted by the user.
 */
public interface IAgent {

	/**
	 * Returns the type of this agent.
	 * 
	 * @return The agent type
	 */
	public AgentType getAgentType();

	/**
	 * Unique identifier for this agent.
	 * 
	 * @return The agent id
	 */
	public String getIdentifier();

	/**
	 * Returns a list of songs which the user could find interresting to listen to in the moment.
	 * 
	 * @param num
	 *            The number of songs which should be returned
	 * @return The songs the we are voting for
	 */
	public List<BaseSong<BaseArtist, BaseAlbum>> suggestSongs(int num);

	/**
	 * Returns the votes for the given songs. If we consider a song as very interresting to play at the moment the vote
	 * is set to 1 and it is set to -1 if we think it is not good at all.
	 * 
	 * @param songs
	 *            The songs which we should rate
	 * @return The rated songs
	 */
	public List<SongVote> vote(List<BaseSong<BaseArtist, BaseAlbum>> songs);
}