package edu.kit.aifb.orel.test;

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
		String configfile = "settings.cfg";
		Settings.load(configfile);
		StorageDriver storage = new MySQLStorageDriver(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
		BasicKBManager kbmanager = new BasicKBManager(storage);
		kbmanager.initialize();
		Test test=new Test("profile-EL.rdf");
		test.test(kbmanager);
	}

}
