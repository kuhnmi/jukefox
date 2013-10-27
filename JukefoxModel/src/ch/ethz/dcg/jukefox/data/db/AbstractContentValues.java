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
package ch.ethz.dcg.jukefox.data.db;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.MethodNotImplementedException;

public abstract class AbstractContentValues implements IContentValues {

	private static class _Iterator implements Iterator<ContentValue> {

		private Iterator<Entry<String, Object>> entrySetIterator;

		public _Iterator(AbstractContentValues cv) {
			this.entrySetIterator = cv.getEntrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return entrySetIterator.hasNext();
		}

		@Override
		public ContentValue next() {
			Entry<String, Object> next = entrySetIterator.next();
			return new ContentValue(next.getKey(), next.getValue());
		}

		@Override
		public void remove() {
			throw new MethodNotImplementedException();
		}
	}

	protected abstract Set<Entry<String, Object>> getEntrySet();

	@Override
	public Iterator<ContentValue> iterator() {
		return new _Iterator(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName()).append(": { ");

		for (ContentValue value : this) {
			builder.append(value.getKey()).append(" => ").append(value.getValue()).append(", ");
		}
		builder.delete(builder.lastIndexOf(", "), builder.length());
		builder.append(" }");

		return builder.toString();
	}

	@Override
	public int size() {
		return getEntrySet().size();
	}
}
