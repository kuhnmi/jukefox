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
package ch.ethz.dcg.pancho3.tablet.view.exploration;

import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ArtistChooserPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ArtistChooserPresenter.IArtistChooserView;
import ch.ethz.dcg.pancho3.tablet.view.lists.ArtistListAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;

/**
 * A popover fragment which lets the user select an artist and then hides
 * itself.
 */
public class ArtistChooserFragment extends Fragment implements IArtistChooserView {

	private PinnedHeaderListView listView;
	private ArtistChooserPresenter presenter;
	private ArtistListAdapter adapter;
	private View column;
	// Indicates whether this view has already been hidden again
	// (after it has been shown).
	// This is used so we don't pop too much off the back stack.
	// TODO: this should be done in the presenter
	private boolean hiddenAgain = false;
	private int offset;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		TabletFactory tabletFactory = ((TabletFactoryGetter) getActivity()).getTabletFactory();
		View view = inflater.inflate(R.layout.tablet_artistchooser, null);
		listView = (PinnedHeaderListView) view.findViewById(R.id.artistlist);
		column = view.findViewById(R.id.column);
		adapter = tabletFactory.createArtistListAdapter(listView);
		//	adapter.initUI();
		listView.setAdapter(adapter);
		listView.setPinnedHeaderAdapter(adapter);
		adapter.initUI();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				presenter.onArtistSelected(adapter.getItem(position));
			}
		});
		tabletFactory.getDragManager().registerArtistListViewDragging(listView, adapter);

		presenter = tabletFactory.getArtistChooserPresenter(this);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Clicking anywhere on the shaded out area closes this popover fragment.
				hide();
			}
		});

		offset = Math.round(-244 * inflater.getContext().getResources().getDisplayMetrics().density);

		getActivity().startActionMode(presenter);
		presenter.onViewFinishedInit();
		return view;
	}

	@Override
	public void hide() {
		if (!hiddenAgain) {
			hiddenAgain = true;
			column.animate().translationX(offset).
					alpha(100).setListener(new AnimatorListener() {

						@Override
						public void onAnimationStart(Animator animation) {
						}

						@Override
						public void onAnimationRepeat(Animator animation) {
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							presenter.onHideOverlay();
							getFragmentManager().popBackStack();
						}

						@Override
						public void onAnimationCancel(Animator animation) {
							presenter.onHideOverlay();
							getFragmentManager().popBackStack();
						}
					});
		}
	}

	@Override
	public void displayArtists(List<BaseArtist> artists) {
		adapter.setArtists(artists);
		adapter.initAdapter();
		adapter.notifyDataSetChanged();
	}

	@Override
	public void startAnimation() {
		// The newer API has a bug in honeycomb, this is why we use this one.
		ObjectAnimator.ofFloat(column, "translationX", 0.0f).start();
		ObjectAnimator.ofFloat(column, "alpha", 100).start();
	}
}
