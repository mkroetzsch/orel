package edu.kit.aifb.orel.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;

import edu.kit.aifb.orel.client.Settings;
import edu.kit.aifb.orel.kbmanager.BasicKBManager;
import edu.kit.aifb.orel.storage.MySQLStorageDriver;
import edu.kit.aifb.orel.storage.StorageDriver;

public class RunTest {

	/**
	 * Class to run tests.
	 * @author Anees
	 */
	
	public static void main(String[] args) throws Exception {
		//set your own settings
		
		PrintStream fileStream=new PrintStream(new FileOutputStream("output.txt",true));
		PrintStream errorStream=new PrintStream(new FileOutputStream("error.txt",true));
		PrintStream orgStream=System.out;
		System.setErr(errorStream);
		System.setOut(fileStream);
		String configfile = "settings.cfg";
		Settings.load(configfile);
		StorageDriver storage = new MySQLStorageDriver(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
		BasicKBManager kbmanager = new BasicKBManager(storage);
		kbmanager.initialize();
		
		Test test=new Test("smallprofile-EL.rdf");
		test.test(kbmanager);
	}

}
