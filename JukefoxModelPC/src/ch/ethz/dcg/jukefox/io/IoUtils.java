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

import java.io.PrintStream;

public class IoUtils {
	
	private static PrintStream sysout = System.out;
	private static PrintStream syserr = System.err;
	
	/**
	 * Sets the "standard" output stream to a dummy instance that swallows all
	 * incoming data. Call {@link #resumeSysOut()} to switch back to the normal one again.
	 */
	public static void suspendSysOut() {
		System.setOut(SilentPrintStream.SILENT_PRINT_STREAM);		
	}
	
	/**
	 * Sets the "standard" error stream to a dummy instance that swallows all
	 * incoming data. Call {@link #resumeSysOut()} to switch back to the normal one again.
	 */
	public static void suspendSysErr() {
		System.setErr(SilentPrintStream.SILENT_PRINT_STREAM);
	}
	
	/**
	 * Resumes the "standard" output stream after a call to {@link #suspendSysOut()}.
	 */
	public static void resumeSysOut() {
		System.setOut(sysout);	
	}
	
	/**
	 * Resumes the "standard" error stream after a call to {@link #suspendSysErr()}.
	 */
	public static void resumeSysErr() {
		System.setErr(syserr);	
	}	
}
