package edu.kit.aifb.orel.kbmanager.elhmanager;

import org.semanticweb.owlapi.model.OWLOntology;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.kbmanager.KBManager;
import edu.kit.aifb.orel.kbmanager.KBManager.InferenceResult;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * @author Anees ul Mehdi
 *
 */
public class ELKBManager extends KBManager{

	public ELKBManager(StorageDriver storage) {
		super(storage);
		registerPredicates();
		
	}

	@Override
	public InferenceResult checkConsistency() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InferenceResult checkEntailment(OWLOntology ontology)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean loadOntology(OWLOntology ontology) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void materialize() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void registerPredicates() {
		storage.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("av",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dav",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("atmostone",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("datmostone",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("atmostoneaux",4,true,false) );
		storage.registerPredicate( new PredicateDeclaration("datmostoneaux",4,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ran",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dran",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dsubconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dsubsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("spo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dspo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("disjoint",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ddisjoint",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("disjointaux",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ddisjointaux",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("nominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dnominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("nonempty",1,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dnonempty",1,true,false) );
		
		storage.registerPredicate( new PredicateDeclaration("eltype",1,true,false) );
		
		storage.registerPredicate( new PredicateDeclaration("spoc",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("self",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subself",2,false,false) );
		storage.registerPredicate( new PredicateDeclaration("inverseof",2,true,false) );
	}



}
