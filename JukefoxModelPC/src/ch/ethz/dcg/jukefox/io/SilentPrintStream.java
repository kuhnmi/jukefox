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
package ch.ethz.dcg.jukefox.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


class SilentPrintStream extends PrintStream {
	private static SilentOutputStream sos = new SilentOutputStream();
	
	public static final SilentPrintStream SILENT_PRINT_STREAM = new SilentPrintStream();

	protected SilentPrintStream() {
		super(sos);
	}
}

class SilentOutputStream extends OutputStream {
	@Override
	public void write(int b) throws IOException {
	}
	 
	@Override
	public void write(byte[] b) throws IOException {
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}
}