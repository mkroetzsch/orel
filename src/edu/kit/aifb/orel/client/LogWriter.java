package edu.kit.aifb.orel.client;

public abstract class LogWriter {
	public static final int LEVEL_ERROR = 3;
	public static final int LEVEL_WARNING = 2;
	public static final int LEVEL_NOTE = 1;
	public static final int LEVEL_DEBUG = 0;
	protected static LogWriter mainlogger;
	
	static public void set(LogWriter logger) {
		mainlogger = logger;
	}
	static public LogWriter get() {
		return mainlogger;
	}	
	
	public abstract void print(String message, int errorlevel);
	public abstract void println(String message, int errorlevel);
	
	public void printError(String message) {
		print(message,LEVEL_ERROR);
	}
	public void printlnError(String message) {
		println(message,LEVEL_ERROR);
	}
	public void printWarning(String message) {
		print(message,LEVEL_WARNING);
	}
	public void printlnWarning(String message) {
		println(message,LEVEL_WARNING);
	}
	public void printNote(String message) {
		print(message,LEVEL_NOTE);
	}
	public void printlnNote(String message) {
		println(message,LEVEL_NOTE);
	}
	public void printDebug(String message) {
		print(message,LEVEL_DEBUG);
	}
	public void printlnDebug(String message) {
		println(message,LEVEL_DEBUG);
	}
}
