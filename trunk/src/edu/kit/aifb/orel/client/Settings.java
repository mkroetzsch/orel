package edu.kit.aifb.orel.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
	 */
	static public void load(String fileurl) throws IOException,URISyntaxException {
		Properties props = new Properties();
		URI fileuri = new URI(fileurl);
		URI workingdir = new File(System.getProperty("user.dir")).toURI();
		System.out.println("Trying to load configuration from " + fileuri + ".");
		FileInputStream configfile = new FileInputStream(new File(workingdir.getPath().toString()+fileuri));
		if (configfile == null) throw new IOException("Config file '" + fileurl + "' not found.");
		props.load(configfile);
		System.out.println("Configuration loaded.");
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
