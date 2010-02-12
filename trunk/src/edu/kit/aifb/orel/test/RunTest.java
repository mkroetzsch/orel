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
		String configfile = "settings.cfg";
		Settings.load(configfile);
		StorageDriver storage = new MySQLStorageDriver(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
		BasicKBManager kbmanager = new BasicKBManager(storage);
		kbmanager.initialize();
		Test test = new Test("tests/rleltests.rdf");
		test.test(kbmanager);
	}

}
