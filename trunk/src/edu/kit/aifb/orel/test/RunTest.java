package edu.kit.aifb.orel.test;

import java.io.IOException;
import java.net.URISyntaxException;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import edu.kit.aifb.orel.client.Settings;
import edu.kit.aifb.orel.db.BasicStore;

public class RunTest {

	/**
	 * Class to run tests.
	 * @author Anees
	 */
	
	public static void main(String[] args) throws Exception {
		//set your own settings
		String configfile = "mysettings.cfg";
		Settings.load(configfile);
		BasicStore store=new BasicStore(Settings.getDBServer(),Settings.getDBName(),Settings.getDBUser(),Settings.getDBPassword());
		store.initialize();
		Test test=new Test("syntax-dl.rdf");
		OWLOntology premise=test.getPremiseOntology();
		OWLOntology conclusion=test.getConclusionOntology();
		store.loadOntology(premise);
		if(store.checkEntailment(conclusion)){
			System.out.println("Entailed");
		}
		else{
			System.out.println("Not Entailed");
		}
	}

}
