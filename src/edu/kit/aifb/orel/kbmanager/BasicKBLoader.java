package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicKBLoader {
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	
	/// Flags for more fine-grained control of actions during axiom processing
	static protected final int CHECK = 1;
	static protected final int PREPARE = 2;
	static protected final int ASSERT = 4;
	
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
		boolean writing = ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.PREPARE)) != 0 );
		if ( writing ) { // initialize bridge and prepare for bulk insert:
			storage.beginLoading();
		}
		// iterate over ontology to load all axioms:
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		int count = 0;
		while( (result || writing) && axiomiterator.hasNext()){
			result = result && processLogicalAxiom(axiomiterator.next(), todos );
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		System.out.println(" loaded " + count + " axioms.");
		if ( writing ) { // close, commit, and recompute indexes
			storage.endLoading();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean processLogicalAxiom(OWLLogicalAxiom axiom, int todos) throws Exception {
		//System.out.println("Processing axiom " + axiom.toString()); // debug
		boolean result;
		if (axiom instanceof OWLSubClassOfAxiom) {
			result = processSubclassOf(((OWLSubClassOfAxiom) axiom).getSubClass(), ((OWLSubClassOfAxiom) axiom).getSuperClass(),todos);
		} else if (axiom instanceof OWLEquivalentClassesAxiom) {
			result = processEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getClassExpressions(),todos);
		} else if (axiom instanceof OWLDisjointClassesAxiom) {
			result = processDisjointClasses(((OWLDisjointClassesAxiom)axiom).getClassExpressions(),todos);
		} else if (axiom instanceof OWLDisjointUnionAxiom) {
			result = false; // no disjoint unions in OWL RL/EL
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
			result = processSubObjectPropertyOf(((OWLSubObjectPropertyOfAxiom) axiom).getSubProperty(), ((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty(),todos);
		} else if (axiom instanceof OWLSubPropertyChainOfAxiom) {
			result = processSubpropertyChainOf(((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain(), ((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty(),todos);
		} else if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) {	
			result = processEquivalentObjectProperties(((OWLEquivalentObjectPropertiesAxiom) axiom).getProperties(),todos);
		} else if (axiom instanceof OWLInverseObjectPropertiesAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLFunctionalObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLInverseFunctionalObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLSymmetricObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLAsymmetricObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLTransitiveObjectPropertyAxiom) {
			ArrayList<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>(2);
			OWLObjectPropertyExpression p = ((OWLTransitiveObjectPropertyAxiom)axiom).getProperty();
			chain.add(p);
			chain.add(p);
			result = processSubpropertyChainOf(chain, p, todos);
		} else if (axiom instanceof OWLReflexiveObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLIrreflexiveObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLSubDataPropertyOfAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLFunctionalDataPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLDataPropertyDomainAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLDataPropertyRangeAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLDisjointDataPropertiesAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLHasKeyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLClassAssertionAxiom) {
			result = processClassAssertion( ((OWLClassAssertionAxiom) axiom).getIndividual(), ((OWLClassAssertionAxiom) axiom).getClassExpression(),todos); 
		} else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
			OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;
			result = processPropertyAssertion( pa.getSubject(), pa.getProperty(), pa.getObject(),todos); 
		} else if (axiom instanceof OWLSameIndividualAxiom) {
			result = processSameIndividuals(((OWLSameIndividualAxiom)axiom).getIndividuals(),todos);
		} else if (axiom instanceof OWLDifferentIndividualsAxiom) {
			result = processDifferentIndividuals(((OWLDifferentIndividualsAxiom)axiom).getIndividuals(),todos);
		} else if (axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) {
			OWLNegativeObjectPropertyAssertionAxiom pa = (OWLNegativeObjectPropertyAssertionAxiom) axiom;
			Set<OWLClassExpression> classes = new HashSet<OWLClassExpression>();
			classes.add( datafactory.getOWLObjectOneOf(pa.getObject()) );
			classes.add( datafactory.getOWLObjectSomeValuesFrom(
					pa.getProperty(), datafactory.getOWLObjectOneOf(pa.getSubject()) ) );
			result = processDisjointClasses(classes, todos);
		} else if (axiom instanceof OWLDataPropertyAssertionAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLNegativeDataPropertyAssertionAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else {
			result = false;
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		}
		return result;
	}

	protected boolean processSubclassOf(OWLClassExpression c1, OWLClassExpression c2, int todos) throws Exception {
		boolean result = true;
		int id1 = storage.getID(c1);
		int id2 = storage.getID(c2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) { 
			// trivial cases not stored
			if (id1 != id2)	storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if ( (id1 != id2) && (!c1.isOWLNothing()) && (!c2.isOWLThing()) ) { // no need to check if trivial
				result = storage.checkPredicateAssertion("sco",id1,id2);
			}
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			createBodyFacts(id1,c1);
			createHeadFacts(id2,c2);
		}
		return result;
	}

	protected boolean processEquivalentClasses(Set<OWLClassExpression> descriptions, int todos) throws Exception {
		Object[] descs = descriptions.toArray();
		int j;
		boolean result = true;
		for (int i=0;i<descs.length;i++) {
			j = ((i+1)%descs.length);
			result = result && processSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j], todos);
		}
		return result;
	}
	
	protected boolean processDisjointClasses(Set<OWLClassExpression> descriptions, int todos) throws Exception {
		Object[] descs = descriptions.toArray();
		boolean result = true;
		int botid = storage.getIDForNothing();
		ArrayList<OWLClassExpression> ops;
		for (int i=0; i<descs.length; i++) {
			for (int j=i+1; j<descs.length; j++) {
				ops = new ArrayList<OWLClassExpression>(2);
				ops.add((OWLClassExpression)descs[i]);
				ops.add((OWLClassExpression)descs[j]);
				Collections.sort(ops);
				int interid = storage.getIDForNaryExpression(StorageDriver.OP_OBJECT_INTERSECTION, ops);
				if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
					storage.makePredicateAssertion("sco",interid,botid);
				}
				if ( (todos & BasicKBLoader.CHECK) != 0 ) {
					result = result && storage.checkPredicateAssertion("sco",interid,botid);
				}
				if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
					// (nothing to prepare for bottom)
					createConjunctionBodyFacts(interid,ops);
				}
			}
		}
		return result;
	}

	protected boolean processSubObjectPropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2, int todos) throws Exception {
		if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK) ) == 0 ) return true; // nothing to do
		boolean result = true;
		int pid1 = storage.getID(p1), pid2 = storage.getID(p2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("subpropertyof", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if (pid1 != pid2) { // no need to check if trivial
				result = storage.checkPredicateAssertion("subpropertyof", pid1, pid2);
			}
		}
		return result;
	}

	protected boolean processEquivalentObjectProperties(Set<OWLObjectPropertyExpression> properties, int todos) throws Exception {
		Object[] props = properties.toArray();
		int j;
		boolean result = true;
		for (int i=0;i<props.length;i++) {
			j = ((i+1)%props.length);
			result = result && processSubObjectPropertyOf((OWLObjectPropertyExpression)props[i],(OWLObjectPropertyExpression)props[j], todos);
		}
		return result;
	}
	
	protected boolean processSubpropertyChainOf(List<OWLObjectPropertyExpression> chain, OWLObjectPropertyExpression p, int todos) throws Exception {
		if (chain.size() == 2) {
			if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK) ) == 0 ) return true; // nothing to do
			boolean result = true;
			int pid = storage.getID(p), pid1 = storage.getID(chain.get(0)), pid2 = storage.getID(chain.get(1));
			if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
				storage.makePredicateAssertion("subpropertychain", pid1, pid2, pid);				
			}
			if ( (todos & BasicKBLoader.CHECK) != 0 ) {
				result = storage.checkPredicateAssertion("subpropertychain", pid1, pid2, pid);
			}
			return result;
		} else {
			// TODO recursion (prepare subchains even if todos don't have BasicKBLoader.ASSERT)
			return true;
		}
	}

	protected boolean processClassAssertion(OWLIndividual i, OWLClassExpression c, int todos) throws Exception {
		boolean result = true;
		int id1 = storage.getID(i);
		int id2 = storage.getID(c);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			createBodyFacts(id1,i);
			createHeadFacts(id2,c);
		}
		return result;
	}

	protected boolean processPropertyAssertion(OWLIndividual s, OWLObjectPropertyExpression p, OWLIndividual o, int todos) throws Exception {
		boolean result = true;
		int sid = storage.getID(s);
		int pid = storage.getID(p);
		int oid = storage.getID(o);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("sv",sid,pid,oid);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("sv",sid,pid,oid);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			createBodyFacts(sid,s);
			createHeadFacts(oid,o);
		}
		return result;
	}
	
	protected boolean processSameIndividuals(OWLIndividual i1, OWLIndividual i2, int todos) throws Exception {
		boolean result = true;
		int id1 = storage.getID(i1);
		int id2 = storage.getID(i2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			createBodyFacts(id1,i1);
			createHeadFacts(id2,i2);
		}
		return result;
	}

	protected boolean processDifferentIndividuals(Set<OWLIndividual> individuals, int todos) throws Exception {
		Object[] inds = individuals.toArray();
		boolean result = true;
		int botid = storage.getIDForNothing();
		ArrayList<OWLIndividual> ops;
		for (int i=0; i<inds.length; i++) {
			int oid1 = storage.getID((OWLIndividual)inds[i]);
			for (int j=i+1; j<inds.length; j++) {
				ops = new ArrayList<OWLIndividual>(2);
				ops.add((OWLIndividual)inds[i]);
				ops.add((OWLIndividual)inds[j]);
				Collections.sort(ops);
				int interid = storage.getIDForNaryExpression(StorageDriver.OP_OBJECT_INTERSECTION, ops);
				if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
					storage.makePredicateAssertion("sco",interid,botid);
				}
				if ( (todos & BasicKBLoader.CHECK) != 0 ) {
					result = result && storage.checkPredicateAssertion("sco",interid,botid);
				}
				if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
					// (nothing to prepare for bottom)
					int oid2 = storage.getID((OWLIndividual)inds[j]);
					storage.makePredicateAssertion("subconjunctionof",oid1,oid2,interid);
				}
			}
			createBodyFacts(oid1,(OWLIndividual)inds[i]);
		}
		return result;
	}
	
	protected boolean processSameIndividuals(Set<OWLIndividual> individuals, int todos) throws Exception {
		Object[] inds = individuals.toArray();
		int j;
		boolean result = true;
		for (int i=0;i<inds.length;i++) {
			j = ((i+1)%inds.length);
			result = result && processSameIndividuals((OWLIndividual)inds[i],(OWLIndividual)inds[j], todos);
		}
		return result;
	}
	

	protected void createBodyFacts(int id, OWLIndividual i) throws Exception {
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
	}
	
	protected void createBodyFacts(int id, OWLClassExpression d) throws Exception {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			ArrayList<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(((OWLObjectIntersectionOf) d).getOperands());
			Collections.sort(ops); // make sure that we have a defined order; cannot have random changes between prepare and check!
			createConjunctionBodyFacts(id, ops);
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = storage.getID(filler);
			storage.makePredicateAssertion("subsomevalues",pid,sid,id);
			createBodyFacts(sid,filler);
		} else {// TODO: add more description types
			System.err.println("Unsupported body class expression: " + d.toString());
		}
	}
	
	protected void createConjunctionBodyFacts(int id, List<OWLClassExpression> ops) throws Exception {
		if (ops.size() <= 0) return;
		int oid1 = storage.getID(ops.get(0));
		createBodyFacts(oid1,ops.get(0));
		if (ops.size() == 2) {
			int oid2 = storage.getID(ops.get(1));
			storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			createBodyFacts(oid2,ops.get(1));
		} else { // recursion
			ArrayList<OWLClassExpression> newops = new ArrayList<OWLClassExpression>(ops.size()-1);
			for (int i=1; i<ops.size(); i++) {
				newops.add(ops.get(i));
			}
			int oid2 = storage.getIDForNaryExpression(StorageDriver.OP_OBJECT_INTERSECTION, ops);
			storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			createConjunctionBodyFacts(oid2,newops);
		}
	}

	protected void createHeadFacts(int id, OWLIndividual i) throws Exception {
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
	}

	protected void createHeadFacts(int sid, OWLClassExpression d) throws Exception {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf){
			Iterator<OWLClassExpression> descit = ((OWLObjectIntersectionOf)d).getOperands().iterator();
			OWLClassExpression desc;
			int descid;
			while (descit.hasNext()) {
				desc = descit.next();
				descid = storage.getID(desc);
				storage.makePredicateAssertion("sco",sid,descid);
				createHeadFacts(descid,desc);
			}
		} else if (d instanceof OWLObjectSomeValuesFrom){
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int oid = storage.getID(filler);
			storage.makePredicateAssertion("sv",sid,pid,oid);
			createHeadFacts(oid,filler);
		} else if (d instanceof OWLObjectOneOf) {
			// TODO
			
		} else {// TODO: add more description types
			System.err.println("Unsupported head class expression: " + d.toString());
		}
	}

}
