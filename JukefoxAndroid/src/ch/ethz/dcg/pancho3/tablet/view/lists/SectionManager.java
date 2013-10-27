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
package ch.ethz.dcg.pancho3.tablet.view.lists;

import java.util.ArrayList;
import java.util.List;

public class SectionManager {

	private ArrayList<Integer> groupCounts = new ArrayList<Integer>();
	private ArrayList<String> groupTitles = new ArrayList<String>();

	private ArrayList<Integer> positionForSection = new ArrayList<Integer>();
	private ArrayList<Integer> sectionForPosition = new ArrayList<Integer>();

	public void createSections(List<? extends Object> objects) {
		groupCounts.clear();
		groupTitles.clear();
		positionForSection.clear();
		sectionForPosition.clear();
		char currentGroupChar = '\0';
		int currentGroupCount = 0;
		for (int i = 0; i < objects.size(); i++) {
			Object object = objects.get(i);
			char firstLetter = Character.toUpperCase(object.toString().charAt(0));
			if (firstLetter != currentGroupChar) {
				positionForSection.add(i);
				if (currentGroupCount != 0) {
					groupCounts.add(currentGroupCount);
					groupTitles.add(Character.toString(currentGroupChar));

				}
				currentGroupChar = firstLetter;
				currentGroupCount = 0;
			}
			sectionForPosition.add(positionForSection.size() - 1);
			currentGroupCount++;
		}
		if (currentGroupCount != 0) {
			groupCounts.add(currentGroupCount);
			groupTitles.add(Character.toString(currentGroupChar));
		}
	}

	public String getGroupTitle(int groupNumber) {
		return groupTitles.get(groupNumber);
	}

	public List<Integer> getGroupCounts() {
		return groupCounts;
	}
}
