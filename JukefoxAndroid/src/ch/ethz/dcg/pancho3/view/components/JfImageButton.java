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
package ch.ethz.dcg.pancho3.view.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;

public class JfImageButton extends LinearLayout {

	private ImageView image;
	private TextView text;

	// Only available since API Level 11
	// public JfImageButton(Context context, AttributeSet attrs, int defStyle) {
	// super(context, attrs, defStyle);
	// init(context);
	// init(attrs);
	// }

	private void init(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.JfImageButton);
		Drawable d = a.getDrawable(R.styleable.JfImageButton_img_src);
		CharSequence text = a.getText(R.styleable.JfImageButton_text);
		setImageDrawable(d);
		setText(text);
	}

	public JfImageButton(Context context) {
		super(context);
		init(context);
	}

	public JfImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
		init(attrs);
	}

	public void init(Context context) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.jf_imagebutton, this);

		setOrientation(LinearLayout.HORIZONTAL);
		setBackgroundResource(R.drawable.d046_bg_button);
		setGravity(Gravity.CENTER_VERTICAL);

		image = (ImageView) findViewById(R.id.image);
		text = (TextView) findViewById(R.id.text);
	}

	public void setImageDrawable(Drawable d) {
		image.setImageDrawable(d);
	}

	public void setText(CharSequence text) {
		this.text.setText(text);
	}
}
