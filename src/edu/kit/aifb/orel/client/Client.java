package edu.kit.aifb.orel.client;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.kit.aifb.orel.kbmanager.BasicKBManager;
import edu.kit.aifb.orel.kbmanager.BasicKBManager.InferenceResult;
import edu.kit.aifb.orel.storage.MySQLStorageDriver;
import edu.kit.aifb.orel.storage.StorageDriver;
import edu.kit.aifb.orel.test.OWLWGTestCaseChecker;

public class Client {
	protected static StorageDriver storage;
	protected static BasicKBManager kbmanager; 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// parse command line arguments
		// supported arguments:  
		// <mode> -- one of "load", "materialize", "init", "clear", "clearall", "checkentailment", "checkconsistency", "runtests"
		// -c <configfile> -- URL of configuration file
		// -o <outputfile> -- URL of outputfile file if relevant to the chosen operation
		int i = 0;
		String arg;
		String operation = "", inputfile = "", configfile = "./settings.cfg", outputfile = "";
		LogWriter.set(new SystemLogWriter(LogWriter.LEVEL_NOTE, LogWriter.LEVEL_WARNING));
		// add more intelligence to those checks later		
		while (i < args.length) {
			arg = args[i++];
			if (arg.equals("-c") || arg.equals("--config")) {
				if (i < args.length) {
					configfile = args[i++];
				} else {
					LogWriter.get().printlnError(arg + " requires a filename of the configuration file");
				}
			} else if (arg.equals("-v") || arg.equals("--verbose")) {
				LogWriter.set(new SystemLogWriter(LogWriter.LEVEL_DEBUG, LogWriter.LEVEL_WARNING));
			} else if (arg.equals("-q") || arg.equals("--quiet")) {
				LogWriter.set(new SystemLogWriter(LogWriter.LEVEL_ERROR, LogWriter.LEVEL_ERROR));
			} else if (arg.equals("-o") || arg.equals("--output")) {
				if (i < args.length) {
					outputfile = args[i++];
				} else {
					LogWriter.get().printlnError(arg + " requires a filename of the output file");
				}
			} else if (arg.startsWith("-")) {
				System.err.println("Unknown option " + arg);
			} else if ( (arg.equals("load")) || arg.equals("checkentailment") || arg.equals("runtests")) {
				operation = arg;
				if (i < args.length) {
					inputfile = args[i++];
				} else {
					LogWriter.get().printlnError(arg + " requires a filename of the input file");
				}
			} else if (arg.equals("materialize") || arg.equals("init") || arg.equals("drop") || 
					   arg.equals("clear") || arg.equals("clearall") || arg.equals("checkconsistency") ) {
				operation = arg;
			} else {
				LogWriter.get().printlnError("Unknown command " + arg);
			}
		}

		if ( operation.equals("") ) {
			LogWriter.get().printNote("No operation given. Usage:\n orel.sh <command> [<inputfile>] [-c <configfile>] [-o <ouptutfile>] \n" +
					           " <command>       : one of \"load\", \"materialize\", \"init\", \"drop\", \"clear\", \"clearall\", \"checkentailment\", \"checkconsistency\", \"runtests\"\n" +
					           "                   where \"load\", \"checkentailment\", and \"runtests\" must be followed by an input ontology URI\n" +
					           " -c <configfile> : path to the configuration file\n" +
					           " -o <outputfile> : name of the output file, if relevant to the current operation\n" +
					           " -v              : increase verbosity to show debug outputs \n" +
					           " -q              : be quiet amd report only errors \n");
			LogWriter.get().printlnNote("Exiting.");
			return;
		}

		try {
			Settings.load(configfile);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if ( Settings.getDBServer().equals("") || Settings.getDBUser().equals("") || Settings.getDBName().equals("") ) {
			LogWriter.get().printlnError("Insufficient database configuration.\n  See INSTALL on how to configure Orel.");
			return;
		}

		long sTime=System.currentTimeMillis();
		try {
			storage = new MySQLStorageDriver(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
			kbmanager = new BasicKBManager(storage);
			if (operation.equals("init")) {
				LogWriter.get().printlnNote("Initialising store ... ");
				kbmanager.initialize();
			} else if (operation.equals("drop")) {
				LogWriter.get().printlnNote("Deleting all database tables ...");
				Thread.sleep(1000);
				LogWriter.get().printlnNote("(CTRL+C to abort within the next 3 seconds!)");
				Thread.sleep(3000);
				kbmanager.drop();
			} else if (operation.equals("clear")) {
				LogWriter.get().printlnNote("Deleting derived database content ...");
				Thread.sleep(1000);
				LogWriter.get().printlnNote("(CTRL+C to abort within the next 3 seconds!)");
				Thread.sleep(3000);
				kbmanager.clearDatabase(true);
			} else if (operation.equals("clearall")) {
				LogWriter.get().printlnNote("Deleting ALL database content ...");
				Thread.sleep(1000);
				LogWriter.get().printlnNote("(CTRL+C to abort within the next 3 seconds!)");
				Thread.sleep(3000);
				kbmanager.clearDatabase(false);
			} else if (operation.equals("load")) {
				LogWriter.get().printlnNote("Loading ontology ...");
				if (inputfile.equals("")) {
					LogWriter.get().printlnError("Please provide the URI of the input ontology using the parameter -i.");
					return;
				}
				long loadsTime = System.currentTimeMillis();
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				IRI physicalURI = IRI.create(inputfile);
				OWLOntology ontology = manager.loadOntologyFromOntologyDocument(physicalURI);
				long loadeTime = System.currentTimeMillis();
				LogWriter.get().printlnNote("Ontology loaded in " + (loadeTime-loadsTime) + " ms.");
				LogWriter.get().printlnNote("Storing ontology ...");
				loadsTime = System.currentTimeMillis();
				boolean success = kbmanager.loadOntology(ontology);
				loadeTime = System.currentTimeMillis();
				if (!success) LogWriter.get().printlnWarning("Some features in the ontology are not (yet) supported by Orel and have been ignored.");
				LogWriter.get().printlnNote("Ontology stored in " + (loadeTime-loadsTime) + " ms.");
				manager.removeOntology(ontology);
			} else if (operation.equals("runtests")) {
				LogWriter.get().printlnNote("Loading and executing OWL test cases ...");
				if (inputfile.equals("")) {
					LogWriter.get().printlnError("Please provide the URI of the input ontology using the parameter -i.");
					return;
				}
				if (outputfile.equals("")) {
					outputfile = "testresults.txt";
				}
				IRI physicalURI = IRI.create(inputfile);
				OWLWGTestCaseChecker testchecker = new OWLWGTestCaseChecker(physicalURI,kbmanager);
				testchecker.runTests(outputfile);
				LogWriter.get().printlnNote("\n Test results written to file " + outputfile + ".");
			} else if (operation.equals("checkentailment")) {
				LogWriter.get().printlnNote("Checking entailment of ontology ...");
				if (inputfile.equals("")) {
					LogWriter.get().printlnError("Please provide the URI of the input ontology using the parameter -i.");
					return;
				}
				long loadsTime=System.currentTimeMillis();
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				//URI physicalURI=(new File(uristring)).toURI();
				IRI physicalURI = IRI.create(inputfile);
				OWLOntology ontology = manager.loadOntologyFromOntologyDocument(physicalURI);
				long loadeTime = System.currentTimeMillis();
				LogWriter.get().printlnNote("Ontology loaded in " + (loadeTime-loadsTime) + " ms.");
				LogWriter.get().printlnNote("Processing ontology ...");
				loadsTime = System.currentTimeMillis();
				InferenceResult result = kbmanager.checkEntailment(ontology);
				if (result == InferenceResult.YES) {
					LogWriter.get().printlnNote("Ontology is entailed.");
				} else if (result == InferenceResult.NO) {
					LogWriter.get().printlnNote("Ontology is not entailed.");
				} else {
					LogWriter.get().printlnNote("It could not be decided if the ontology is entailed, since the ontology contains unsupported features.");
				}
				loadeTime=System.currentTimeMillis();
				LogWriter.get().printlnNote("Ontology processed in " + (loadeTime-loadsTime) + " ms.");
				manager.removeOntology(ontology);
			} else if (operation.equals("checkconsistency")) {
				LogWriter.get().printlnNote("Checking consistency of loaded ontology ...");
				InferenceResult result = kbmanager.checkConsistency();
				if (result == InferenceResult.YES) {
					LogWriter.get().printlnNote("Ontology is consistent.");
				} else if (result == InferenceResult.NO) {
					LogWriter.get().printlnNote("Ontology is not consistent.");
				} else {
					LogWriter.get().printlnNote("It could not be decided if the ontology is consistent, since the ontology contains unsupported features.");
				}
			} else if (operation.equals("materialize")) {
				LogWriter.get().printlnNote("Materialising consequences ...");
				kbmanager.materialize();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		LogWriter.get().printlnNote("Done in " + (System.currentTimeMillis()-sTime) + " ms.\n");
	}

}
