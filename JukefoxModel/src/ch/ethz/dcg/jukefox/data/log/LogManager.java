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
package ch.ethz.dcg.jukefox.data.log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import ch.ethz.dcg.jukefox.commons.AbstractLanguageHelper;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.HttpHelper;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.model.providers.LogProvider;

public class LogManager {

	private final static String TAG = LogManager.class.getSimpleName();

	private final LogProvider logProvider;
	private final AbstractLanguageHelper languageHelper;
	private final ModelSettingsManager modelSettingsManager;

	private LinkedList<ILogEntry> outstandingWrites = new LinkedList<ILogEntry>();

	public LogManager(LogProvider logProvider, AbstractLanguageHelper languageHelper,
			ModelSettingsManager modelSettingsManager) {
		this.logProvider = logProvider;
		this.languageHelper = languageHelper;
		this.modelSettingsManager = modelSettingsManager;
	}

	/**
	 * Adds the given log entry to the log queue. A call to {@link #sendLogs()} will then push the logs to the server.
	 * 
	 * @param logEntry
	 *            The log entry
	 */
	public void addLogEntry(ILogEntry logEntry) {
		/* FIXME @sämy fix this implementation... if (!modelSettingsManager.isHelpImproveJukefox()) {
			return;
		}*/

		// Write it async to the db, to not delay the caller
		synchronized (outstandingWrites) {
			outstandingWrites.add(logEntry);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Write all existing logEntries to the db
				while (true) {
					ILogEntry logEntry;
					synchronized (outstandingWrites) {
						if (outstandingWrites.size() > 0) {
							logEntry = outstandingWrites.poll();
						} else {
							return;
						}
					}

					logProvider.writeLogEntry(logEntry);
				}
			}
		}).start();
	}

	/**
	 * Sends the stored logs to the log server.
	 */
	public void sendLogs() {
		/* FIXME @Sämy: if (!modelSettingsManager.isHelpImproveJukefox()) {
			return;
		}*/

		// Read the log entries
		String meId = languageHelper.getUniqueId();
		List<PackedLogEntry> logEntries = logProvider.getLogEntryStrings();
		int maxLogEntryId = 0;

		if (logEntries.size() == 0) {
			// Nothing to do
			return;
		}

		// Pack them to one request
		StringBuffer sb = new StringBuffer();
		sb.append(meId + '\n'); // First line: meId

		for (PackedLogEntry logEntry : logEntries) {
			sb.append(logEntry.getPacked());
			sb.append('\n');

			if (logEntry.getDbLogEntryId() > maxLogEntryId) {
				maxLogEntryId = logEntry.getDbLogEntryId();
			}
		}
		String requestStr = sb.toString();

		// Send it to the server
		DefaultHttpClient httpClient = HttpHelper.createHttpClientWithDefaultSettings();
		HttpPost httpPost = new HttpPost(Constants.FORMAT_LOG2_URL);

		try {
			httpPost.setEntity(new StringEntity(requestStr));

			// Execute HTTP Post Request
			HttpResponse response = httpClient.execute(httpPost);
			String serverReply = EntityUtils.toString(response.getEntity());

			if ("OK".equals(serverReply)) {
				logProvider.removeLogEntriesOlderThan(maxLogEntryId);
			} else {
				Log.w(TAG, "Could not send the log to the server: " + serverReply);
			}

		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, "Could not build the log to the server package: " + e.getMessage());
		} catch (ClientProtocolException e) {
			Log.w(TAG, "Could not send the log to the server: " + e.getMessage());
		} catch (IOException e) {
			Log.w(TAG, "Could not send the log to the server: " + e.getMessage());
		}
	}
}
