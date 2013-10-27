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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspMessage;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspSessionMessage;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspTag;
import ch.ethz.dcg.jukefox.controller.sharing.messages.tags.JmspMessage.JmspMessageType;

public class JmspMessageParser {

	public static final String TAG = JmspMessageParser.class.getSimpleName();

	private IJmspMessageListener listener;

	public JmspMessageParser(IJmspMessageListener listener) {
		this.listener = listener;
	}

	public void parseInput(BufferedReader input) throws SAXException, IOException {

		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader();

			DataHandler dataHandler = new DataHandler();
			xr.setContentHandler(dataHandler);

			xr.parse(new InputSource(input));

		} catch (ParserConfigurationException pce) {
			Log.e("SAX XML", pce.toString());
		}
	}

	public class DataHandler extends DefaultHandler {

		private JmspTag lastOpenedTag;
		private JmspMessageType lastOpenedMsgType;
		private JmspMessage currentMessage;

		/**
		 * This gets called when the xml document is first opened
		 * 
		 * @throws SAXException
		 */
		@Override
		public void startDocument() throws SAXException {

		}

		/**
		 * Called when it's finished handling the document
		 * 
		 * @throws SAXException
		 */
		@Override
		public void endDocument() throws SAXException {

		}

		/**
		 * This gets called at the start of an element. Here we're also setting the booleans to true if it's at that
		 * specific tag. (so we know where we are)
		 * 
		 * @param namespaceURI
		 * @param localName
		 * @param qName
		 * @param atts
		 * @throws SAXException
		 */
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
				throws SAXException {
			System.out.println("StartTag: " + qName);

			if (qName.equals(JmspMessageType.SESSION.getTag())) {
				currentMessage = new JmspSessionMessage();
				lastOpenedMsgType = JmspMessageType.SESSION;
				lastOpenedTag = currentMessage.startElement(namespaceURI, qName, qName, atts, null);
				return;
			}

			if (lastOpenedTag == null) {
				Log.w(TAG, "Root tag of a xml must be a message tag and not: " + qName);
				return;
			}

			lastOpenedTag = lastOpenedTag.startElement(namespaceURI, localName, qName, atts, lastOpenedTag);

		}

		/**
		 * Called at the end of the element. Setting the booleans to false, so we know that we've just left that tag.
		 * 
		 * @param namespaceURI
		 * @param localName
		 * @param qName
		 * @throws SAXException
		 */
		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			System.out.println("EndTag: " + qName);

			if (lastOpenedTag == null) {
				Log.w(TAG, "Root tag of a xml must be a message tag and not: " + qName);
				return;
			}

			lastOpenedTag = lastOpenedTag.endElement(namespaceURI, localName, qName);

			if (qName.equals(JmspMessageType.SESSION.getTag())) {
				if (lastOpenedMsgType == JmspMessageType.SESSION) {
					listener.onNewSessionMessage((JmspSessionMessage) currentMessage);
				}
				currentMessage = null;
			}
		}

		/**
		 * Calling when we're within an element. Here we're checking to see if there is any content in the tags that
		 * we're interested in and populating it in the Config object.
		 * 
		 * @param ch
		 * @param start
		 * @param length
		 */
		@Override
		public void characters(char ch[], int start, int length) {
			//			String chars = new String(ch, start, length);
			//			chars = chars.trim();
			if (lastOpenedTag != null) {
				lastOpenedTag.characters(ch, start, length);
			}
		}
	}

}
