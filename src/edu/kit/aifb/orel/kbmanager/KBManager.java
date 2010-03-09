package edu.kit.aifb.orel.kbmanager;

import org.semanticweb.owlapi.model.OWLOntology;

import edu.kit.aifb.orel.storage.StorageDriver;

public abstract class KBManager {
	static public enum InferenceResult {
	    YES, NO, DONTKNOW 
	}
	protected StorageDriver storage;
	
	public static KBManager getKBManager(String name, StorageDriver storage) {
		if (name.equals("ELRLManager")) {
			return new BasicKBManager(storage);
		} else {
			return null;
		}
	}
	
	public KBManager(StorageDriver storage) {
		this.storage = storage;
	}

	/**
	 * Prepare the storage for loading and reasoning by initializing the
	 * necessary structures (e.g. database tables) and contents (e.g. static
	 * knowledge about datatypes).
	 * @throws Exception
	 */
	public abstract void initialize() throws Exception;

	/**
	 * Load the given ontology into the storage. This will add to the existing
	 * content of the storage.
	 * @param ontology
	 * @return
	 * @throws Exception
	 */
	public abstract boolean loadOntology(OWLOntology ontology) throws Exception;

	/**
	 * Compute and store all logical consequences of the loaded ontology data
	 * for easier querying, or update any earlier materialization based on new
	 * results. 
	 * @throws Exception
	 */
	public abstract void materialize() throws Exception;

	/**
	 * Find out if the given ontology is entailed by the stored ontology data.
	 * This operation may change the storage contents, e.g. by materialization
	 * or by adding auxiliary facts, but it must not change the logical
	 * semantics of the storage contents with respect to any possible ontology.
	 * In other words, the storage contents must be a conservative extension of
	 * the existing content over the signature of all ontologies. 
	 * @param ontology
	 * @return
	 * @throws Exception
	 */
	public abstract InferenceResult checkEntailment(OWLOntology ontology) throws Exception;

	/**
	 * Find out if the loaded ontology data is logically consistent.
	 * This method may change the storage contents in the same was as
	 * checkEntailment().
	 * @return
	 * @throws Exception
	 */
	public abstract InferenceResult checkConsistency() throws Exception;

	/**
	 * Delete the contents of the store but keep it in an initialized state.
	 * @throws Exception
	 */
	public void clear(boolean onlyderived) throws Exception {
		if (onlyderived) {
			storage.clear(onlyderived);
		} else { // faster
			drop();
			initialize();
		}
	}

	/**
	 * Completely remove all generated storage structures for the given storage driver,
	 * for instance by deleting all created database tables and their contents.
	 * @throws Exception
	 */
	public void drop() throws Exception {
		storage.drop();
	}

}