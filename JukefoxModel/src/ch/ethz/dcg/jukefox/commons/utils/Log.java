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
package ch.ethz.dcg.jukefox.commons.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;

public class Log {

	public enum LogLevel {
		VERBOSE(2), DEBUG(3), INFO(4), WARN(5), ERROR(6), ASSERT(7);

		private final int value;

		LogLevel(int value) {
			this.value = value;
		}

		public final int value() {
			return value;
		}

		public static final LogLevel byValue(int value) {
			switch (value) {
				case 2:
					return LogLevel.VERBOSE;
				case 3:
					return LogLevel.DEBUG;
				case 4:
					return LogLevel.INFO;
				case 5:
					return LogLevel.WARN;
				case 6:
					return LogLevel.ERROR;
				case 7:
					return LogLevel.ASSERT;
				default:
					return LogLevel.VERBOSE;
			}
		}
	}

	private static long lastLogFileCheckTime = 0;
	public static final int LOG_FILE_CHECK_INTERVALL = 1000 * 60 * 60; // Millisecods
	public static final int MAX_LOG_FILE_LENGTH = 150 * 1000; // Size in bytes
	private static final String TAG = Log.class.getSimpleName();
	public static int currentLogFileNr = getCurrentLogFileNr();
	private static String logFileBasePath = null;
	private static ModelSettingsManager modelSettingsManager = null;

	private static ILogPrinter logPrinter = null;

	// don't change this value. It is changed as soon
	private static boolean logToFile = false;
	private static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy hh:mm:ss");

	private static LogLevel logLevel = LogLevel.VERBOSE;

	public static void createLogPrinter(ILogPrinter logPrinter) {
		Log.logPrinter = logPrinter;
	}

	public static void setLogLevel(LogLevel logLevel) {
		Log.logLevel = logLevel;
	}

	public static void setModelSettingsManager(ModelSettingsManager modelSettingsManager) {
		Log.modelSettingsManager = modelSettingsManager;
		logToFile = modelSettingsManager.isLogFileEnabled();
	}

	public static void setLogFileBasePath(String path) {
		logFileBasePath = path;
	}

	private static void createDefaultLogPrinter() {
		createLogPrinter(new SystemOutLogPrinter());
	}

	public static void setLogToFile(boolean logToFile) {
		Log.logToFile = logToFile;
	}

	private static int getCurrentLogFileNr() {
		if (modelSettingsManager == null) {
			return 1;
		}
		return modelSettingsManager.getCurrentLogFileNumber();
	}

	private static String getCurrentTimeFormatted() {
		long time = System.currentTimeMillis();
		return format.format(new Date(time)) + "; " + time;
	}

	public static void d(String tag, String msg) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (msg == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.DEBUG.value()) {
			logPrinter.printDebug(tag, msg);
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; D; " + tag, msg);
		}
	}

	public static void v(String tag, String msg) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (msg == null) {
			return;
		}
		// android.util.Log.v(TAG, "logToFile: " + logToFile + ", LOG_LEVEL: " +
		// LOG_LEVEL + ", verbose-level: " + android.util.Log.VERBOSE);
		if (logToFile || logLevel.value() <= LogLevel.VERBOSE.value()) {
			logPrinter.printVerbose(tag, msg);
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; V; " + tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (msg == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.WARN.value()) {
			logPrinter.printWarning(tag, msg);
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; W; " + tag, msg);
		}
	}

	public static void w(String tag, Exception e) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (e == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.WARN.value()) {
			logPrinter.printWarning(tag, getStackTraceString(e));
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; W; " + tag, e);
		}
	}

	public static void w(String tag, Throwable e) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (e == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.WARN.value()) {
			logPrinter.printWarning(tag, getStackTraceString(e));
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; W; " + tag, e);
		}
	}

	public static void w(String tag, Error e) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (e == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.WARN.value()) {
			logPrinter.printWarning(tag, getStackTraceString(e));
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; W; " + tag, e);
		}
	}

	public static void e(String tag, String msg) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (msg == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.ERROR.value()) {
			logPrinter.printError(tag, msg);
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; E; " + tag, msg);
		}
	}

	public static void i(String tag, String msg) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		if (msg == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.INFO.value()) {
			logPrinter.printInfo(tag, msg);
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; I; " + tag, msg);
		}
	}

	public static void wtf(String tag, Exception e) {
		if (logPrinter == null) {
			createDefaultLogPrinter();
		}
		// TODO: send back to server?
		if (e == null) {
			return;
		}
		if (logToFile || logLevel.value() <= LogLevel.ERROR.value()) {
			logPrinter.printAssert(tag, getStackTraceString(e));
		}
		if (logToFile) {
			writeToLogFile(getCurrentTimeFormatted() + "; WTF; " + tag, e);
		}
	}

	public static void writeExceptionToErrorFile(String tag, String msg, Exception e) {
		DataOutputStream dout = null;
		try {
			File f = getLogFile(currentLogFileNr);
			// Check if we have a valid log file path
			if (f == null) {
				return;
			}
			dout = getOutputStream(f);
			dout.writeBytes(tag + "; ");
			dout.writeBytes("msg: " + msg + "\n");
			for (StackTraceElement el : e.getStackTrace()) {
				dout.writeBytes(el.getFileName() + "(" + el.getClassName() + "." + el.getMethodName() + " (): "
						+ el.getLineNumber() + ")");
			}
			PrintStream pr = new PrintStream(dout);
			e.printStackTrace(pr);
		} catch (Exception e2) {
			System.out.println("Jukefox Error Log: " + "\n" + getStackTraceString(e2));
		} finally {
			try {
				if (dout != null) {
					dout.close();
				}
			} catch (Exception e3) {
				System.out.println("Jukefox Error Log: " + "\n" + getStackTraceString(e3));
			}
		}
	}

	private static void writeToLogFile(String tag, String msg) {
		DataOutputStream dout = null;
		try {
			File f = getLogFile(currentLogFileNr);
			// Check if we have a valid log file path
			if (f == null) {
				return;
			}
			dout = getOutputStream(f);
			dout.writeBytes(tag + "; ");
			dout.writeBytes(msg + "\n");
		} catch (Exception e2) {
			System.out.println("Jukefox LogCat: " + "\n" + getStackTraceString(e2));
		} finally {
			try {
				if (dout != null) {
					dout.close();
				}
				if (System.currentTimeMillis() - lastLogFileCheckTime > LOG_FILE_CHECK_INTERVALL) {
					checkLogFile();
				}
			} catch (Exception e3) {
				Log.w("Jukefox LogCat", e3);
			}
			;
		}
	}

	// private static void writeToLogFile(String tag, Exception e) {
	// DataOutputStream dout = null;
	// try {
	// dout = getOutputStream(getLogFileName());
	// dout.writeBytes(tag + "\n");
	// dout.writeBytes("msg: " + e.getMessage() + "\n");
	// PrintStream pr = new PrintStream(dout);
	// e.printStackTrace(pr);
	// } catch (Exception e2) {
	// android.util.Log.w("Jukefox LogCat", e2);
	// } finally {
	// try {
	// if (dout != null) {
	// dout.close();
	// }
	// if (System.currentTimeMillis() - lastLogFileCheckTime >
	// LOG_FILE_CHECK_INTERVALL) {
	// checkLogFile();
	// }
	// } catch (Exception e3) {
	// android.util.Log.w("Jukefox LogCat", e3);
	// }
	// ;
	// }
	// }
	//
	// private static void writeToLogFile(String tag, Error e) {
	// DataOutputStream dout = null;
	// try {
	// dout = getOutputStream(getLogFileName());
	// dout.writeBytes(tag + "\n");
	// dout.writeBytes("msg: " + e.getMessage() + "\n");
	// PrintStream pr = new PrintStream(dout);
	// e.printStackTrace(pr);
	// } catch (Exception e2) {
	// android.util.Log.w("Jukefox LogCat", e2);
	// } finally {
	// try {
	// if (dout != null) {
	// dout.close();
	// }
	// if (System.currentTimeMillis() - lastLogFileCheckTime >
	// LOG_FILE_CHECK_INTERVALL) {
	// checkLogFile();
	// }
	// } catch (Exception e3) {
	// android.util.Log.w("Jukefox LogCat", e3);
	// }
	// }
	// }

	private static void writeToLogFile(String tag, Throwable e) {
		DataOutputStream dout = null;
		try {
			File f = getLogFile(currentLogFileNr);
			// Check if we have a valid log file path
			if (f == null) {
				return;
			}
			dout = getOutputStream(f);
			dout.writeBytes(tag + "; ");
			dout.writeBytes("msg: " + e.getMessage() + "\n");

			PrintStream pr = new PrintStream(dout);
			e.printStackTrace(pr);
		} catch (Exception e2) {
			System.out.println("Jukefox LogCat: " + "\n" + getStackTraceString(e2));
		} finally {
			try {
				if (dout != null) {
					dout.close();
				}
				if (System.currentTimeMillis() - lastLogFileCheckTime > LOG_FILE_CHECK_INTERVALL) {
					checkLogFile();
				}
			} catch (Exception e3) {
				System.out.println("Jukefox LogCat: " + "\n" + getStackTraceString(e3));
			}
			;
		}
	}

	private synchronized static void checkLogFile() {
		lastLogFileCheckTime = System.currentTimeMillis();
		File f = getLogFile(currentLogFileNr);
		if (f == null) {
			return;
		}
		if (f.length() > MAX_LOG_FILE_LENGTH) {
			int nextLogFileNr = currentLogFileNr == 1 ? 2 : 1;
			// FIXME: neu im Directorymanager machen!
			File f2 = getLogFile(nextLogFileNr);
			if (f2 != null) {
				if (!f2.delete()) {
					Log.w(TAG, "Could not delete Log file!: " + f2.getAbsolutePath());
				}
			}
			Log.setCurrentLogFileNumber(nextLogFileNr);
			// FIXME: neu im Directorymanager machen!
			createNewLogFile(getLogFile(nextLogFileNr));
		}
	}

	private static void createNewLogFile(File file) {
		writeToLogFile("Log", "Start time: " + System.currentTimeMillis());
		String settings = "";
		if (modelSettingsManager != null) {
			settings = modelSettingsManager.getLogString();
		}
		try {
			// writeToLogFile(TAG, "Jukefox version: " +
			// AndroidUtils.getVersionName());
			// writeToLogFile(TAG, "Android version: " +
			// AndroidUtils.getAndroidVersionName());
			// writeToLogFile(TAG, "Phone model: " + AndroidUtils.getModel());
			writeToLogFile(TAG, settings);
		} catch (Throwable t) {
			Log.w(TAG, t);
		}

	}

	private static DataOutputStream getOutputStream(File logCat) throws FileNotFoundException {
		DataOutputStream dout;
		FileOutputStream fout = new FileOutputStream(logCat, true);
		dout = new DataOutputStream(fout);
		return dout;
	}

	private static File getLogFile(int nr) {
		// FIXME: neu im Directorymanager machen!
		if (logFileBasePath == null) {
			return null;
		}
		return new File(logFileBasePath + nr + ".txt");
	}

	private static void setCurrentLogFileNumber(int newLogFileNumber) {
		currentLogFileNr = newLogFileNumber;
		if (modelSettingsManager == null) {
			return;
		}
		modelSettingsManager.setCurrentLogFileNumber(currentLogFileNr);
	}

	public static void printStackTrace(String tag, StackTraceElement[] stackTrace) {
		if (modelSettingsManager == null || !modelSettingsManager.isLogFileEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement el : stackTrace) {
			sb.append("--- " + el.getFileName() + "(" + el.getClassName() + "." + el.getMethodName() + " (): "
					+ el.getLineNumber() + ")\n");
		}
		Log.v(tag, sb.toString());
	}

	private static String getStackTraceString(Throwable tr) {
		if (tr == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		tr.printStackTrace(pw);
		return sw.toString();
	}
}
