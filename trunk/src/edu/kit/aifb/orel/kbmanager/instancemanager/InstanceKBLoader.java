package edu.kit.aifb.orel.kbmanager.instancemanager;

import java.util.Iterator;
import java.util.Set;
import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.client.LogWriter;
import edu.kit.aifb.orel.storage.StorageDriver;

public class InstanceKBLoader {
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	protected InstanceExpressionVisitor expvisitor;
	protected InstanceAxiomVisitor axiomvisitor;
	
	/*
	 * Flag to indicate the activity of reading the store to check for some
	 * conclusion. This operation returns "true" if the conclusion was found.   
	 */
	static public final int CHECK = 1;
	/*
	 * Flag to indicate the activity of writing auxiliary axioms to the store
	 * as needed for being able to check for some conclusion. This involves
	 * introducing auxiliary axioms for decomposing complex statements, but
	 * also the creation of "test individuals" that are needed for checking
	 * some axioms. This operation returns "true" if checking for entailment is
	 * supported for the given axiom.      
	 */
	static public final int PREPARECHECK = 2;
	/*
	 * Flag to indicate the activity of writing some axiom to the store. This
	 * operation returns "true" if the axiom (type) can be written. It is
	 * normally executed together with PREPAREASSERT.
	 */
	static public final int ASSERT = 4;
	/*
	 * Flag to indicate the activity of writing auxiliary axioms to the store
	 * as needed for being able to assert for some axiom. This involves
	 * introducing auxiliary axioms for decomposing complex statements. This
	 * operation returns "true" if checking for entailment is supported for the
	 * given axiom.      
	 */
	static public final int PREPAREASSERT = 8;
	/// Flag combination to indicate preparation activities.
	static public final int PREPARE = PREPAREASSERT | PREPARECHECK;
	/// Flag combination to indicate activiteis that write to the store.
	static public final int WRITE = PREPARE | ASSERT;
	
	public InstanceKBLoader(StorageDriver storage) {
		this.storage = storage;
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	public boolean processOntology(OWLOntology ontology, int todos) throws Exception {
		boolean result = true;
		datafactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		expvisitor = new InstanceExpressionVisitor(InstanceExpressionVisitor.Action.READ,storage);
		axiomvisitor = new InstanceAxiomVisitor(storage,datafactory,expvisitor,todos);
		boolean writing = ( (todos & InstanceKBLoader.WRITE) != 0 );
		if ( writing ) { // initialize bridge and prepare for bulk insert:
			storage.beginLoading();
		}
		// iterate over ontology to load all axioms:
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		boolean curresult;
		int count = 0;
		while ( (result || writing) && axiomiterator.hasNext() ) {
			axiom = axiomiterator.next();
			curresult = axiom.accept(axiomvisitor);
			if ( !curresult && ((todos & (InstanceKBLoader.ASSERT | InstanceKBLoader.PREPARE)) != 0) ) {
				LogWriter.get().printlnWarning("Unsupported axiom: " + axiom.toString());
			}
			result = curresult && result;
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		LogWriter.get().printlnNote(" processed " + count + " axiom(s).");
		if ( writing ) { // close, commit, and recompute indexes
			storage.endLoading();
		}
		return result;
	}

}
