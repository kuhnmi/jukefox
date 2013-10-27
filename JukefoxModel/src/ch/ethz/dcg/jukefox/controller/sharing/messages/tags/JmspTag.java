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

public abstract class JmspTag {

	protected JmspTag parentTag;

	public abstract String getTagString();

	public JmspTag startElement(String namespaceURI, String localName, String qName, Attributes atts,
			JmspTag lastOpenedTag) throws SAXException {
		if (localName.equals(getTagString())) {
			parentTag = lastOpenedTag;
			return this;
		} else {
			throw new SAXException();
		}
	}

	public JmspTag endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (localName.equals(getTagString())) {
			return parentTag;
		} else {
			throw new SAXException();
		}
	}

	public abstract void writeTag(PrintWriter output);

	public void characters(char ch[], int start, int length) {

	}

	protected void printOpenTag(PrintWriter output, String tag, boolean lineBreak) {
		if (lineBreak) {
			output.println("<" + tag + ">");
		} else {
			output.print("<" + tag + ">");
		}
	}

	protected void printCloseTag(PrintWriter output, String tag, boolean lineBreak) {
		if (lineBreak) {
			output.println("</" + tag + ">");
		} else {
			output.print("</" + tag + ">");
		}
	}
}
