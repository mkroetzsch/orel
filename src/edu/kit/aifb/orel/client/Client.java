package edu.kit.aifb.orel.client;

import java.net.URI;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.kit.aifb.orel.db.BasicStore;

public class Client {
	protected static BasicStore store;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// parse command line arguments
		// supported arguments:  
		// <mode> -- one of "load", "materialize", "init", "clear"
		// -c <configfile> -- URL of configuration file
		int i = 0;
		String arg;
		String operation = "", inputfile = "", configfile = "./settings.cfg";
		// add more intelligence to those checks later		
		while (i < args.length) {
			arg = args[i++];
			if (arg.equals("-c") || arg.equals("--config")) {
				if (i < args.length) {
					configfile = args[i++];
				} else {
					System.err.println(arg + " requires a filename of the configuration file");
				}
			} else if (arg.startsWith("-")) {
				System.err.println("Unknown option " + arg);
			} else if (arg.equals("load")) {
				operation = arg;
				if (i < args.length) {
					inputfile = args[i++];
				} else {
					System.err.println(arg + " requires a filename of the input file");
				}
			} else if (arg.equals("materialize") || arg.equals("init") || arg.equals("drop") || arg.equals("clear")) {
				operation = arg;
			} else {
				System.err.println("Unknown command " + arg);
			}
		}

		if ( operation.equals("") ) {
			System.out.println("No operation given. Usage:\n orel.sh <command> -c <configfile> -i <inputfile>\n" +
					           " <command>       : one of \"load\", \"materialize\", \"init\", \"drop\"\n" +
					           "                   where \"load\" must be followed by an input ontology URI\n" +
					           " -c <configfile> : path to the configuration file\n");
			System.out.println("Exiting.");
			return;
		}

		try {
			Settings.load(configfile);
		} catch (Exception e) {
			System.err.println(e.toString());
			return;
		}
		if ( Settings.getDBServer().equals("") || Settings.getDBUser().equals("") || Settings.getDBName().equals("") ) {
			System.err.println("Insufficient database configuration.\nPlease be sure to specify at leat 'dbname', dbuser' and 'dbserver' in your local configuration.");
			return;
		}

		long sTime=System.currentTimeMillis();
		try {
			Client.store = new BasicStore(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
			if (operation.equals("init")) {
				System.out.println("Initialising store ... ");
				Client.store.initialize();
			} else if (operation.equals("drop")) {
				System.out.println("Deleting all database tables ...");
				Thread.sleep(1000);
				System.out.println("(CTRL+C to abort within the next 5 seconds!)");
				Thread.sleep(5000);
				Client.store.drop();
			} else if (operation.equals("clear")) {
				System.out.println("Deleting all database content ...");
				Thread.sleep(1000);
				System.out.println("(CTRL+C to abort within the next 3 seconds!)");
				Thread.sleep(3000);
				Client.store.clearDatabase();
			} else if (operation.equals("load")) {
				System.out.println("Loading ontology ...");
				if (inputfile.equals("")) {
					System.err.println("Please provide the URI of the input ontology using the parameter -i.");
					return;
				}
				long loadsTime=System.currentTimeMillis();
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				//URI physicalURI=(new File(uristring)).toURI();
				URI physicalURI= URI.create(inputfile);
				OWLOntology ontology = manager.loadOntologyFromPhysicalURI(physicalURI);
				long loadeTime=System.currentTimeMillis();
				System.out.println("Ontology loaded in " + (loadeTime-loadsTime) + " ms.");
				System.out.println("Storing ontology ...");
				loadsTime=System.currentTimeMillis();
				Client.store.loadOntology(ontology);
				loadeTime=System.currentTimeMillis();
				System.out.println("Ontology stored in " + (loadeTime-loadsTime) + " ms.");
				manager.removeOntology(ontology);
			} else if (operation.equals("materialize")) {
				System.out.println("Materialising consequences ...");
				Client.store.materialize();
			}
		} catch (Exception e) {
			System.err.println(e.toString());
			return;
		}
		long eTime=System.currentTimeMillis();
		System.out.println("Done in " + (eTime-sTime) + " ms.\n");
	}

}
