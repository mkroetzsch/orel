package edu.kit.aifb.orel.kbmanager;

import java.util.Iterator;
import java.util.Set;
import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.client.LogWriter;
import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicKBLoader {
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	protected BasicExpressionVisitor expvisitor;
	protected BasicAxiomVisitor axiomvisitor;
	
	/// Flags for more fine-grained control of actions during axiom processing
	static public final int CHECK = 1;
	static public final int PREPARECHECK = 2;
	static public final int ASSERT = 4;
	static public final int PREPAREASSERT = 8;
	/// Combination of flags
	static public final int PREPARE = PREPAREASSERT | PREPARECHECK;
	static public final int WRITE = PREPARE | ASSERT;
	
	public BasicKBLoader(StorageDriver storage) {
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
		expvisitor = new BasicExpressionVisitor(BasicExpressionVisitor.Action.READ,storage);
		axiomvisitor = new BasicAxiomVisitor(storage,datafactory,expvisitor,todos);
		boolean writing = ( (todos & BasicKBLoader.WRITE) != 0 );
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
			if ( !curresult && ((todos & (BasicKBLoader.ASSERT | BasicKBLoader.PREPARE)) != 0) ) {
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
