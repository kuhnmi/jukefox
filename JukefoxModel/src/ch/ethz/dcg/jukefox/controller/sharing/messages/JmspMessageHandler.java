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
package ch.ethz.dcg.jukefox.controller.sharing.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;

import org.xml.sax.SAXException;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspSessionMessage;

public class JmspMessageHandler implements IJmspMessageListener {

	public static final String TAG = JmspMessageHandler.class.getSimpleName();

	private BufferedReader input;
	private PrintWriter output;
	private Socket messageSocket;

	private LinkedList<IJmspMessageListener> listeners;
	private JmspMessageParser messageParser;

	public JmspMessageHandler(Socket messageSocket) {

		listeners = new LinkedList<IJmspMessageListener>();

		try {
			this.messageSocket = messageSocket;
			input = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));
			output = new PrintWriter(messageSocket.getOutputStream(), true);
		} catch (IOException e) {
			Log.w(TAG, e);
		}

		startMessageParser();

	}

	private void startMessageParser() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				messageParser = new JmspMessageParser(JmspMessageHandler.this);
				try {
					messageParser.parseInput(input);
				} catch (SAXException e) {
					Log.w(TAG, e);
				} catch (IOException e) {
					Log.w(TAG, e);
				}
			}
		}).start();
	}

	public void closeSocket() {
		try {
			messageSocket.close();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}

	public void addMessageListener(IJmspMessageListener listener) {
		if (listener == null) {
			return;
		}
		listeners.add(listener);
	}

	public void removeMessageListener(IJmspMessageListener listener) {
		if (listener == null) {
			return;
		}
		listeners.remove(listener);
	}

	public void sendSessionMessage(JmspSessionMessage msg) {
		msg.writeTag(output);
		output.flush();
		//		PrintWriter output2 = new PrintWriter(System.out);
		//		msg.writeTag(output2);
		//		output2.flush();

	}

	@Override
	public void onNewSessionMessage(JmspSessionMessage msg) {
		for (IJmspMessageListener listener : listeners) {
			listener.onNewSessionMessage(msg);
		}
	}

}
