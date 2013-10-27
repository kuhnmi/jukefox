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
package ch.ethz.dcg.pancho3.view.commons;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.manager.libraryimport.ImportProgressListener;
import ch.ethz.dcg.jukefox.manager.libraryimport.Progress;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.IReadOnlyAndroidApplicationState;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class StatusInfo implements ImportProgressListener {

	public static final int MIN_UPDATE_INTERVAL = 1000;

	private static final String TAG = StatusInfo.class.getSimpleName();

	private final View info;
	private boolean dismissed;
	private ProgressBar progressBar = null;
	private TextView statusText;
	private TextView detailStatusText;
	private long lastTimeUpdated = 0;
	private IReadOnlyAndroidApplicationState state;
	private Handler handler;
	private Timer secondaryProgressTimer;
	private int secondaryProgress = 0;

	private StatusInfo(View info) {
		this.info = info;
		this.dismissed = false;
	}

	public static StatusInfo showInfo(Activity activity, String text, IReadOnlyAndroidApplicationState state) {
		View info = View.inflate(activity, R.layout.statusinfo, null);
		final StatusInfo statusInfo = new StatusInfo(info);
		statusInfo.setProgressBar((ProgressBar) info.findViewById(R.id.progressBar));
		statusInfo.setDetailStatusText((TextView) info.findViewById(R.id.detailStatusText));
		statusInfo.getProgressBar().setVisibility(View.VISIBLE);
		initializeStatusInfo(activity, text, info, statusInfo);
		Progress progress = state.getImportProgress();
		statusInfo.updateProgress(progress);
		statusInfo.setState(state);
		state.addImportProgressListener(statusInfo);
		startSecondaryProgressTimer(statusInfo);
		if (progress.isImportFinished()) {
			statusInfo.dismiss();
		}
		return statusInfo;
	}

	public int incrementAndReturnSecondaryProgress() {
		int progress = getProgressBar().getProgress();
		if (progress != 0) {
			secondaryProgress = (secondaryProgress + progress / 10) % progress;
		} else {
			secondaryProgress = 0;
		}
		return secondaryProgress;
	}

	public Timer getSecondaryProgressTimer() {
		return secondaryProgressTimer;
	}

	public void setSecondaryProgressTimer(Timer secondaryProgressTimer) {
		this.secondaryProgressTimer = secondaryProgressTimer;
	}

	private static void startSecondaryProgressTimer(final StatusInfo statusInfo) {
		final ProgressBar progress = statusInfo.getProgressBar();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				JukefoxApplication.getHandler().post(new Runnable() {

					@Override
					public void run() {
						progress.setSecondaryProgress(statusInfo.incrementAndReturnSecondaryProgress());
					}

				});
			}

		}, 500, 500);
		statusInfo.setSecondaryProgressTimer(timer);
	}

	public IReadOnlyAndroidApplicationState getState() {
		return state;
	}

	public void setState(IReadOnlyAndroidApplicationState state) {
		this.state = state;
	}

	public static StatusInfo showInfo(Activity activity, String text) {
		View info = View.inflate(activity, R.layout.statusinfo, null);
		final StatusInfo statusInfo = new StatusInfo(info);
		initializeStatusInfo(activity, text, info, statusInfo);
		return statusInfo;
	}

	private static void initializeStatusInfo(final Activity activity, String text, final View info,
			final StatusInfo statusInfo) {
		statusInfo.handler = JukefoxApplication.getHandler();
		statusInfo.setStatusText((TextView) info.findViewById(R.id.statusText));
		statusInfo.getStatusText().setText(text);
		View bar = info.findViewById(R.id.statusInfoBar);
		bar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				statusInfo.dismiss();
			}
		});
		JukefoxApplication.getHandler().post(new Runnable() {

			@Override
			public void run() {
				activity.addContentView(info, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			}
		});
		return;
	}

	public synchronized void dismiss() {
		if (dismissed) {
			return;
		}
		Log.v(TAG, "Dismiss: state == null: " + (state == null));
		if (state != null) {
			// new thread to avoid concurrent modification exception in
			// onProgressChanged
			JoinableThread t = new JoinableThread(new Runnable() {

				@Override
				public void run() {
					state.removeImportProgressListener(StatusInfo.this);
				}
			});
			t.start();
		}
		cancelSecondaryProgressTimer();
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (!dismissed) {
					ViewGroup vg = (ViewGroup) info.getParent();
					if (vg != null) {
						vg.removeView(info);
					}
					dismissed = true;
				}
			}
		});
	}

	private void cancelSecondaryProgressTimer() {
		if (secondaryProgressTimer != null) {
			secondaryProgressTimer.cancel();
		}
	}

	public boolean isDismissed() {
		return dismissed;
	}

	@Override
	public void onProgressChanged(final Progress progress) {
		updateProgress(progress);
	}

	private void updateProgress(final Progress progress) {
		final long currentTime = System.currentTimeMillis();
		if (currentTime - lastTimeUpdated > MIN_UPDATE_INTERVAL) {
			handler.post(new Runnable() {

				@Override
				public void run() {
					getProgressBar().setMax(progress.getOverallMaxProgress());
					getProgressBar().setProgress(progress.getOverallProgress());
					getDetailStatusText().setText(progress.getStatusMessage());
					progress.logProgress();
					// getStatusText().setText(progress.getStatusMessage());
					lastTimeUpdated = currentTime;
				}

			});
		}
		if (progress.isImportFinished()) {
			// Log.v(TAG, "import Finished");
			dismiss();
		} else {
			// Log.v(TAG, "import not yet Finished");
		}
	}

	public void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public ProgressBar getProgressBar() {
		return progressBar;
	}

	public TextView getStatusText() {
		return statusText;
	}

	public void setStatusText(TextView statusText) {
		this.statusText = statusText;
	}

	private void setDetailStatusText(TextView detailStatusText) {
		this.detailStatusText = detailStatusText;
	}

	public TextView getDetailStatusText() {
		return detailStatusText;
	}
}
