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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.commons.JukefoxNotificationManager;

public class ImportService extends Service {

	public static final String TAG = ImportService.class.getSimpleName();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		JukefoxNotificationManager nm = JukefoxApplication.getNotificationManager();
		startForeground(nm.getImportNotificationId(), nm.getImportNotification());
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v(TAG, "onStart()");
		executeIntentAction(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand()");
		executeIntentAction(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return Service.START_STICKY;
	}

	private void executeIntentAction(final Intent intent) {
		if (intent == null || intent.getAction() == null) {
			Log.v(TAG, "executeIntentAction: no intent or no intent action");
			return;
		}
		Log.v(TAG, "executeIntentAction: " + intent.getAction());

		JoinableThread executeAction = new JoinableThread(new Runnable() {

			@Override
			public void run() {

			}

		});
		executeAction.start();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy()");
		// playerController.onServiceDestroy();
		super.onDestroy();
	}

}
