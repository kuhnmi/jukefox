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
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ethz.dcg.jukefox.commons.utils;

/**
 * Container to ease passing around a tuple of two objects. This object provides
 * a sensible implementation of equals(), returning true if equals() is true on
 * each of the contained objects.
 */
public class Pair<F, S> {

	public final F first;
	public final S second;

	/**
	 * Constructor for a Pair. If either are null then equals() and hashCode()
	 * will throw a NullPointerException.
	 * 
	 * @param first
	 *            the first object in the Pair
	 * @param second
	 *            the second object in the pair
	 */
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Checks the two objects for equality by delegating to their respective
	 * equals() methods.
	 * 
	 * @param o
	 *            the Pair to which this one is to be checked for equality
	 * @return true if the underlying objects of the Pair are both considered
	 *         equals()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Pair<?, ?>)) {
			return false;
		}
		final Pair<F, S> other;
		try {
			other = (Pair<F, S>) o;
		} catch (ClassCastException e) {
			return false;
		}
		return first.equals(other.first) && second.equals(other.second);
	}

	/**
	 * Compute a hash code using the hash codes of the underlying objects
	 * 
	 * @return a hashcode of the Pair
	 */
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + first.hashCode();
		result = 31 * result + second.hashCode();
		return result;
	}

	/**
	 * Convenience method for creating an appropriately typed pair.
	 * 
	 * @param a
	 *            the first object in the Pair
	 * @param b
	 *            the second object in the pair
	 * @return a Pair that is templatized with the types of a and b
	 */
	public static <A, B> Pair<A, B> create(A a, B b) {
		return new Pair<A, B>(a, b);
	}
}
