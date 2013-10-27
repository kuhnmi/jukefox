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
package ch.ethz.dcg.pancho3.view.youtube;

import java.io.StringReader;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import android.util.Log;

public class SAXParser extends DefaultHandler {

	private final static String TAG = SAXParser.class.getSimpleName();
	private ArrayList<String> videoIds;
	private int i = 0;
	private final static String syndication = "Syndication of this video was restricted by its owner";
//	private char[] stringArray;
	private String m;
	
	public SAXParser() {
		super();
		videoIds = new ArrayList<String>();
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
	//	stringArray = syndication.toCharArray();
		m = new String();
	}

	public ArrayList<String> getParseResult(String res) throws Exception {
		XMLReader xr = XMLReaderFactory.createXMLReader();
		SAXParser handler = new SAXParser();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);
		xr.parse(new InputSource(new StringReader(res)));
		
		return handler.getVideoIds();
	
	}

	public ArrayList<String> getVideoIds() {
		return videoIds;
	}

	public void startDocument() {
		Log.v(TAG, "Start document");
	}

	public void endDocument() {
		Log.v(TAG, "End document");
	}

	public void characters(char ch[], int start, int length) {
		System.out.print("Characters:    \"");
		String n = new String(ch);
	//	if (ch[0] == 't' && ch[1] == 'a' && ch[2] == 'g' && ch[3] == ':') {
		if (n.contains("tag:youtube.com,2008:video:")){
			if (i > 0) {
				char[] var = new char[11];
				for(int j = 27; j < 38; j++){
					var[j-27] = ch[j];	
				}
				String s = new String(var);
				this.videoIds.add(s);
				m = s;
				
			//	Log.v(TAG, ""+s);
			}
			i++;
		}
		
		if (isNotAllowed(n)){
			videoIds.remove(m);
		}
		

	}

	private boolean isNotAllowed(String n) {
		if(n.contains(syndication)){
			return true;
		}
		return false;
	}
}
