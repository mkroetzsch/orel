package edu.kit.aifb.orel.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class for managing global settings such as the DB connection information.
 * @author markus
 */
public class Settings {

	static protected String dbuser = "";
	static protected String dbserver = "";
	static protected String dbname = "";
	static protected String dbpassword = "";

	/**
	 * Load the configuration from a file.
	 * TODO Implement simple file reading instead of using fixed values. 
	 */
	static public void load(String fileurl) throws IOException {
		Properties props = new Properties();
		InputStream configfile = Settings.class.getClassLoader().getResourceAsStream(fileurl);
		if (configfile == null) throw new IOException("Config file '" + fileurl + "' not found.");
		props.load(configfile);
		Settings.dbuser = props.getProperty("dbuser","");
		Settings.dbpassword = props.getProperty("dbpassword","");
		Settings.dbserver = props.getProperty("dbserver","");
		Settings.dbname = props.getProperty("dbname","");
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
}
