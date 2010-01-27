package edu.kit.aifb.orel.kbmanager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * Main class for maintaining a storage that uses the basic OWL EL/RL
 * predicate signature.
 * 
 * @author Markus Krötzsch
 */
public class BasicKBManager {
	protected StorageDriver storage = null;
	
	/**
	 * Constructor that also establishes a database connection, since this object cannot really work without a database.
	 * @param dbserver
	 * @param dbname
	 * @param dbuser
	 * @param dbpwd
	 * @throws Exception
	 */
	public BasicKBManager(StorageDriver storage) throws Exception {
		this.storage = storage;
		registerPredicates();
	}

	/**
	 * Ensure that the DB has the right tables, creating them if necessary.
	 */
	public void initialize() throws Exception {
		storage.initialize();
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws Exception {
		storage.drop();
	}
	
	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clearDatabase(boolean onlyderived) throws Exception {
		storage.clear(onlyderived);
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 */
	public void loadOntology(OWLOntology ontology) throws Exception {
		loadOntology(ontology, false);
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	public void loadOntology(OWLOntology ontology, boolean donotassert) throws Exception {
		BasicKBLoader loader = new BasicKBLoader(storage);
		loader.loadOntology(ontology, donotassert);
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	public boolean checkEntailment(OWLOntology ontology) throws Exception {
		loadOntology(ontology,true);
		BasicKBReasoner reasoner = new BasicKBReasoner(storage);
		return reasoner.checkEntailment(ontology);
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		BasicKBReasoner reasoner = new BasicKBReasoner(storage);
		reasoner.materialize();
	}
	

	protected void registerPredicates() {
		storage.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sco_nl",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv_nl",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subconint",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subpropertychain",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subpropertyof",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("nominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("nonempty",1,true,false) );
	}

}