package edu.kit.aifb.orel.kbmanager.elhmanager;

import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLOntology;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.kbmanager.BasicExpressionVisitor;
import edu.kit.aifb.orel.kbmanager.BasicKBLoader;
import edu.kit.aifb.orel.kbmanager.KBManager;
import edu.kit.aifb.orel.kbmanager.NaiveKBReasoner;
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
		storage.initialize();
		int thing = storage.getIDForThing(), nothing = storage.getIDForNothing(); 
		storage.makePredicateAssertion("sco",thing,thing);
		storage.makePredicateAssertion("sco",nothing,nothing);
		storage.makePredicateAssertion("sco",nothing,thing);
		storage.makePredicateAssertion("subconjunctionof",thing,nothing,nothing);
		int botobjprop = storage.getID(BasicExpressionVisitor.OP_BOTTOM_OBJECT_PROPERTY); 
		storage.makePredicateAssertion("spo",botobjprop,botobjprop);
		storage.commit();
		
	}

	@Override
	public boolean loadOntology(OWLOntology ontology) throws Exception {
		boolean result = true;
		BasicKBLoader loader = new BasicKBLoader(storage);
		// TODO Do we need to guard against input loops (cyclic imports)?
		Iterator<OWLOntology> ontit = ontology.getDirectImports().iterator();
		while (ontit.hasNext()) {
			result = loadOntology(ontit.next()) && result;
		}
		return loader.processOntology(ontology, (BasicKBLoader.PREPAREASSERT | BasicKBLoader.ASSERT) ) && result;
	}

	@Override
	public void materialize() throws Exception {
		ELKBReasoner reasoner = new ELKBReasoner(storage);
		reasoner.materialize();
		
	}
	
	private void registerPredicates() {
		storage.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("spo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("spoc",3,true,false) );
	}



}
