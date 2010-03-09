package edu.kit.aifb.orel.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Class for managing global settings such as the DB connection information.
 * @author Markus Krötzsch
 */
public class Settings {

	static protected String dbuser = "";
	static protected String dbserver = "";
	static protected String dbname = "";
	static protected String dbpassword = "";
	static protected String kbmanager = "";

	/**
	 * Load the configuration from a file.
	 */
	static public void load(String fileurl) throws URISyntaxException {
		Properties props = new Properties();
		URI fileuri = new URI(fileurl);
		URI workingdir = new File(System.getProperty("user.dir")).toURI();
		LogWriter.get().printlnNote("Trying to load configuration from " + fileuri + ".");
		try {
			FileInputStream configfile = new FileInputStream(new File(workingdir.getPath().toString()+fileuri));
			props.load(configfile);
			LogWriter.get().printlnNote("Configuration loaded.");
		} catch (FileNotFoundException e) {
			LogWriter.get().printlnError("Configuration file " + fileurl + "' not found:\n" +
			                             "  Please specify the correct location of your configuration file, or create a configuration file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Settings.dbuser = props.getProperty("dbuser","");
		Settings.dbpassword = props.getProperty("dbpassword","");
		Settings.dbserver = props.getProperty("dbserver","");
		Settings.dbname = props.getProperty("dbname","");
		Settings.kbmanager = props.getProperty("kbmanager","ELRLManager");
	}
	
	static public String getDBPassword() {
		return Settings.dbpassword;
	}

	static public String getDBUser() {
		return Settings.dbuser;
	}

	static public String getDBName() {
		return Settings.dbname;
	}

	static public String getDBServer() {
		return Settings.dbserver;
	}
	
	static public String getKBManager() {
		return Settings.kbmanager;
	}
}
