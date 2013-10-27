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
package ch.ethz.dcg.jukefox.controller.sharing;

import java.net.Socket;
import java.util.Random;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.sharing.messages.IJmspMessageListener;
import ch.ethz.dcg.jukefox.controller.sharing.messages.JmspMessageHandler;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspSessionId;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspSessionMessage;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspSessionMessage.SessionMessageAction;

public class JmspSessionManager implements ISocketsReadyListener, IJmspMessageListener {

	private static final String TAG = JmspSessionManager.class.getSimpleName();
	private Socket messageSocket;
	private Socket dataSocket;

	private JmspMessageHandler messageHandler;
	private JmspDataHandler dataHandler;

	private JmspSession session;

	public JmspSessionManager() {

	}

	public void setMessageSocket(Socket messageSocket) {
		this.messageSocket = messageSocket;
	}

	public void setDataSocket(Socket dataSocket) {
		this.dataSocket = dataSocket;
	}

	@Override
	public void onSocketsReady(Socket messageSocket, Socket dataSocket) {
		Log.v(TAG, "Got Sockets!");
		session = new JmspSession();
		setMessageSocket(messageSocket);
		setDataSocket(dataSocket);
		messageHandler = new JmspMessageHandler(messageSocket);
		messageHandler.addMessageListener(this);
		initiateSession();

	}

	private void initiateSession() {
		JmspSessionMessage msg = new JmspSessionMessage();
		msg.setMessageAction(SessionMessageAction.PROPOSE_SESSION_ID);
		JmspSessionId sessionId = new JmspSessionId(new Random().nextInt());
		msg.setSessionId(sessionId);
		messageHandler.sendSessionMessage(msg);
	}

	@Override
	public void onNewSessionMessage(JmspSessionMessage msg) {
		Log.v(TAG, "Got session msg with id: " + msg.getSessionId().getId());
	}
}
