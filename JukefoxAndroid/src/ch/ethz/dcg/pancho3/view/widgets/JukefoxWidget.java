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
package ch.ethz.dcg.pancho3.view.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;

public abstract class JukefoxWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.v(getTag(), "widget onUpdate called.");
		initWidget(context, appWidgetManager, appWidgetIds);
	}

	private void initWidget(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				int counter = 0;
				while (!AndroidUtils.isSdCardOk() && counter < 1000) {
					try {
						JoinableThread.sleep(5000);
					} catch (InterruptedException e) {
						Log.w(getTag(), e);
					}
					counter++;
				}
				if (counter < 1000) {
					Intent updateIntent = new Intent(context, PlayerService.class);
					Log.v(getTag(), "Launching intent with action: " + getWidgetInitIntentAction());
					updateIntent.setAction(getWidgetInitIntentAction());
					context.startService(updateIntent);

					updateAppWidget(context, appWidgetManager, appWidgetIds);
				}
			}

		});
		t.start();
	}

	public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayout());
		// if (appWidgetIds != null) {
		// appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
		// } else {
		ComponentName cn = new ComponentName(context, getWidgetClass());
		appWidgetManager.updateAppWidget(cn, remoteViews);
		// }
	}

	public abstract int getWidgetLayout();

	public abstract String getWidgetInitIntentAction();

	public abstract String getTag();

	public abstract Class<? extends JukefoxWidget> getWidgetClass();

	@Override
	public void onReceive(Context context, Intent intent) {
		// v1.5 fix that doesn't call onDelete Action
		Log.v(getTag(), "intent received: " + intent.getAction());
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			Log.v(getTag(), "Widget deleted");
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
			Log.v(getTag(), "Widget configuration changed");
			AppWidgetManager awm = AppWidgetManager.getInstance(context);
			initWidget(context, awm, null);
		} else {
			Log.v(getTag(), "Widget configuration changed");
			AppWidgetManager awm = AppWidgetManager.getInstance(context);
			initWidget(context, awm, null);
			super.onReceive(context, intent);
		}
	}

	@Override
	public void onEnabled(Context context) {
		Log.v(getTag(), "appWidget enabled.");
		super.onEnabled(context);
	}

}
