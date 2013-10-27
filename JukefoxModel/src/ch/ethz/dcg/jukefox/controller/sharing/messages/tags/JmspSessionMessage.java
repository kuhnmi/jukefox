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
package ch.ethz.dcg.jukefox.controller.sharing.messages.tags;

import java.io.PrintWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class JmspSessionMessage extends JmspMessage {

	public enum SessionMessageAction {
		PROPOSE_SESSION_ID("propose"), ACCEPT_SESSION_ID("accept"), CLOSE_SESSION("close");

		public String tag;

		private SessionMessageAction(String tag) {
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		}
	}

	private SessionMessageAction messageAction;

	public JmspSessionMessage() {
	}

	public SessionMessageAction getMessageAction() {
		return messageAction;
	}

	public void setMessageAction(SessionMessageAction messageAction) {
		this.messageAction = messageAction;
	}

	@Override
	public String getTagString() {
		return JmspMessageType.SESSION.getTag();
	}

	@Override
	public JmspTag startElement(String namespaceURI, String localName, String qName, Attributes atts,
			JmspTag lastOpenedTag) throws SAXException {
		if (qName.equals(getTagString())) {
			parentTag = lastOpenedTag;
			parseAttributes(atts);
			return this;
		} else if (qName.equals(JmspSessionId.TAG_STRING)) {
			sessionId = new JmspSessionId();
			return sessionId.startElement(namespaceURI, localName, qName, atts, lastOpenedTag);
		} else {
			throw new SAXException();
		}
	}

	private void parseAttributes(Attributes atts) {
		String action = atts.getValue("action");
		if (action.equals(SessionMessageAction.PROPOSE_SESSION_ID.getTag())) {
			messageAction = SessionMessageAction.PROPOSE_SESSION_ID;
		} else if (action.equals(SessionMessageAction.ACCEPT_SESSION_ID.getTag())) {
			messageAction = SessionMessageAction.ACCEPT_SESSION_ID;
		} else if (action.equals(SessionMessageAction.CLOSE_SESSION.getTag())) {
			messageAction = SessionMessageAction.CLOSE_SESSION;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		// Ignore text
	}

	@Override
	public void writeTag(PrintWriter output) {
		printOpenTag(output, getTagString() + " action=\"" + messageAction.getTag() + "\"", true);
		if (sessionId != null) {
			sessionId.writeTag(output);
		}
		printCloseTag(output, getTagString(), true);
	}

}
