package edu.kit.aifb.orel.test;

import java.io.File;

import org.semanticweb.owlapi.model.IRI;

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
		IRI physicalURI= IRI.create( (new File(System.getProperty("user.dir") + "/tests/rleltests.rdf")).toURI() );
		OWLWGTestCaseChecker test = new OWLWGTestCaseChecker(physicalURI,kbmanager);
		test.runTests("testresults.txt");
	}

}
