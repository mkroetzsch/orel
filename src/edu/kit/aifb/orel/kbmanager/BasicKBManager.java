package edu.kit.aifb.orel.kbmanager;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * Main class for maintaining a storage that uses the basic OWL EL/RL
 * predicate signature.
 * 
 * @author Markus Krötzsch
 */
public class BasicKBManager {
	static public enum InferenceResult {
	    YES, NO, DONTKNOW 
	}
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
		// the fundamental truths of DL reasoning: 
		int thing = storage.getIDForThing(), nothing = storage.getIDForNothing();
		storage.makePredicateAssertion("sco",thing,thing);
		storage.makePredicateAssertion("sco",nothing,nothing);
		storage.makePredicateAssertion("sco",nothing,thing);
		storage.makePredicateAssertion("nonempty",thing);
		storage.commit();
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
		if (onlyderived) {
			storage.clear(onlyderived);
		} else { // faster
			drop();
			initialize();
		}
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	public boolean loadOntology(OWLOntology ontology) throws Exception {
		BasicKBLoader loader = new BasicKBLoader(storage);
		return loader.processOntology(ontology, (BasicKBLoader.PREPAREASSERT | BasicKBLoader.ASSERT) );
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	public InferenceResult checkEntailment(OWLOntology ontology) throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		return reasoner.checkEntailment(ontology);
	}
	
	/**
	 * Check if the loaded axioms are consistent.
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 */
	public InferenceResult checkConsistency() throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		return reasoner.checkConsistency();
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		reasoner.materialize();
	}

	protected void registerPredicates() {
		storage.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("rsco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("drsco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ran",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dran",2,true,false) );

		storage.registerPredicate( new PredicateDeclaration("self",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subself",2,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("suballvalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("spoc",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("spo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("nominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dnominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("nonempty",1,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dnonempty",1,true,false) );
		
		storage.registerPredicate( new PredicateDeclaration("av",3,true,false) );
	}

}
