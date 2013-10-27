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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

	public static final String TAG = SeekBarPreference.class.getSimpleName();

	public static int maximum = 5;
	public static int interval = 1;

	private float oldValue = 1;
	private TextView monitorBox;

	public SeekBarPreference(Context context) {
		super(context);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		LinearLayout layout = new LinearLayout(getContext());

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.gravity = Gravity.LEFT;
		params1.weight = 1.0f;

		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.WRAP_CONTENT);
		params2.gravity = Gravity.RIGHT;

		LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT);
		params3.gravity = Gravity.CENTER;

		layout.setPadding(15, 5, 10, 5);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		TextView view = new TextView(getContext());
		view.setText(getTitle());
		view.setTextSize(18);
		view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		view.setGravity(Gravity.LEFT);
		view.setLayoutParams(params1);

		SeekBar bar = new SeekBar(getContext());
		bar.setMax(maximum);
		bar.setProgress((int) this.oldValue);
		bar.setLayoutParams(params2);
		bar.setOnSeekBarChangeListener(this);

		this.monitorBox = new TextView(getContext());
		this.monitorBox.setTextSize(12);
		this.monitorBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
		this.monitorBox.setLayoutParams(params3);
		this.monitorBox.setPadding(2, 5, 0, 0);
		this.monitorBox.setText(bar.getProgress() + "");

		layout.addView(view);
		layout.addView(bar);
		layout.addView(this.monitorBox);
		layout.setId(android.R.id.widget_frame);

		return layout;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		progress = Math.round((float) progress / interval) * interval;

		if (!callChangeListener(progress)) {
			seekBar.setProgress((int) this.oldValue);
			return;
		}

		// kuhnmi 3.12.2011: avoid endless-loop/stack-overflow.
		if (this.oldValue == progress) {
			return;
		}

		seekBar.setProgress(progress);
		this.oldValue = progress;
		this.monitorBox.setText(progress + "");
		updatePreference(progress);

		notifyChanged();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {

		int dValue = ta.getInt(index, 50);

		return validateValue(dValue);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		int temp = restoreValue ? getPersistedInt(50) : (Integer) defaultValue;

		if (!restoreValue) {
			persistInt(temp);
		}

		this.oldValue = temp;
	}

	private int validateValue(int value) {

		if (value > maximum) {
			value = maximum;
		} else if (value < 0) {
			value = 0;
		} else if (value % interval != 0) {
			value = Math.round((float) value / interval) * interval;
		}

		return value;
	}

	private void updatePreference(int newValue) {

		SharedPreferences.Editor editor = getEditor();
		editor.putInt(getKey(), newValue);
		editor.commit();
	}
}
