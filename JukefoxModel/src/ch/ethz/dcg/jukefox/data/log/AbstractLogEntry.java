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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.IAgent;

public abstract class AbstractLogEntry implements ILogEntry {

	private final static int VERSION = 1;

	private Date timestamp = null;

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	protected void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String getPacked() {
		return String.format("%d|%d|%s|%d|%s", // version|timestamp|type-ident|type-version|type-log  (synchronize this with PackedLogEntry!)
				VERSION, getTimestamp().getTime(), getTypeIdent(), getTypeVersion(), packYourStuff());
	}

	protected <K, V> String getPackedMap(Map<K, V> map, Formatter<K> keyFormatter, Formatter<V> valueFormatter) {
		if (map == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			sb.append(String.format(
					"%s=%s;",
					keyFormatter.format(entry.getKey()), valueFormatter.format(entry.getValue())));
		}
		sb.deleteCharAt(sb.length() - 1); // Remove last ';'

		return sb.toString();
	}

	/**
	 * Returns the unique log type identifier. This helps the server side to know how to decode the package.
	 * 
	 * @return The log type identifier
	 */
	protected abstract String getTypeIdent();

	/**
	 * Returns the version of this log entry. Gets increased every time the output format of
	 * {@link ILogEntry#createPackage} changes.
	 * 
	 * @return
	 */
	public abstract int getTypeVersion();

	/**
	 * Write your values (and only yours!) into a string.
	 * 
	 * @return The encoded values
	 */
	protected abstract String packYourStuff();

	/**
	 * Builder class for an {@link AbstractLogEntry}. Create the instance with this builder to ensure, all required
	 * fields are set. When building is done call {@link #build()} to get the instance.<br/>
	 * Please ensure, that you call {@link #init(AbstractLogEntry, Builder)} in the constructor of your builder
	 * implementation.
	 * 
	 * @param <L>
	 *            The log-entry class
	 * @param <B>
	 *            The builder class
	 */
	public static abstract class Builder<L extends AbstractLogEntry, B extends Builder<?, ?>> {

		private L instance;
		private B builderInstance;

		protected void init(L instance, B builderInstance) {
			this.instance = instance;
			this.builderInstance = builderInstance;

			instance.setTimestamp(new Date()); // Set it to now
		}

		protected L getInstance() {
			return instance;
		}

		public B setTimestamp(Date timestamp) {
			instance.setTimestamp(timestamp);
			return builderInstance;
		}

		/**
		 * Checks if all the fields are set in {@link #instance} and returns it afterwards.<br/>
		 * Please ensure, that all fields have <code>protected</code> visibility and the name of fields which do not
		 * need to be set end with <code>Opt</code>.
		 * 
		 * @return The instance
		 */
		public L build() {
			Field[] fields = getInstance().getClass().getDeclaredFields();
			for (Field field : fields) {
				try {
					if (field.getName().endsWith("Opt")) {
						continue;
					}
					assert field.get(instance) != null : "You did not set the required field " + field.getName();
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}

			return instance;
		}
	}

	protected interface Formatter<T> {

		public String format(T value);
	}

	protected class FloatFormatter implements Formatter<Float> {

		@Override
		public String format(Float value) {
			return String.format("%.2f", value);
		}
	}

	protected class DoubleFormatter implements Formatter<Double> {

		@Override
		public String format(Double value) {
			return String.format("%.2f", value);
		}
	}

	protected class AgentFormatter implements Formatter<IAgent> {

		@Override
		public String format(IAgent value) {
			return value.getIdentifier();
		}
	}

	protected class ListFormatter<T> implements Formatter<List<T>> {

		private final Formatter<T> itemFormatter;

		public ListFormatter(Formatter<T> itemFormatter) {
			this.itemFormatter = itemFormatter;
		}

		@Override
		public String format(List<T> list) {
			StringBuffer sb = new StringBuffer();
			for (T item : list) {
				sb.append(itemFormatter.format(item));
				sb.append(':');
			}
			sb.deleteCharAt(sb.length() - 1); // remove last ':'

			return sb.toString();
		}
	}
}
