package edu.kit.aifb.orel.kbmanager;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicKBLoader {
	protected StorageDriver storage;
	
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
		boolean result;
		if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
			result = processSubclassOf(((OWLSubClassOfAxiom) axiom).getSubClass(), ((OWLSubClassOfAxiom) axiom).getSuperClass(),todos);
		} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
			result = processEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getClassExpressions(),todos);
		} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
			result = processSubpropertyOf(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty(), ((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty(),todos);
		} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
			result = processSubpropertyChainOf(((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain(), ((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty(),todos);
		} else if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
			result = processClassAssertion( ((OWLClassAssertionAxiom) axiom).getIndividual(), ((OWLClassAssertionAxiom) axiom).getClassExpression(),todos); 
		} else if (axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION) {
			OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual> pa = ((OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual>) axiom);
			result = processPropertyAssertion( pa.getSubject(), pa.getProperty(), pa.getObject(),todos); 
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
			storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("sco",id1,id2);
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
			j = (i%(descs.length-1))+1;
			result = result && processSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j], todos);
		}
		return result;
	}

	protected boolean processSubpropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2, int todos) throws Exception {
		if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK) ) == 0 ) return true; // nothing to do
		boolean result = true;
		int pid1 = storage.getID(p1), pid2 = storage.getID(p2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("subpropertyof", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("subpropertyof", pid1, pid2);
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

	protected boolean processPropertyAssertion(OWLIndividual s, OWLObjectProperty p, OWLIndividual o, int todos) throws Exception {
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
	

	protected void createBodyFacts(int id, OWLIndividual i) throws Exception {
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
	}
	
	protected void createBodyFacts(int id, OWLClassExpression d) throws Exception {
		id = storage.getID(d); //test
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			createConjunctionBodyFacts(id, ((OWLObjectIntersectionOf) d).getOperands().toArray());
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
	
	protected void createConjunctionBodyFacts(int id, Object[] ops) throws Exception {
		// TODO maybe sort ops first to increase likeliness of finding the same sub-ops again
		if (ops.length <= 0) return;
		int oid1 = storage.getID((OWLClassExpression)ops[0]);
		createBodyFacts(oid1,(OWLClassExpression)ops[0]);
		if (ops.length == 2) {
			int oid2 = storage.getID((OWLClassExpression)ops[1]);
			storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			createBodyFacts(oid2,(OWLClassExpression)ops[1]);
		} else { // recursion
			String opsidstring = "IntersectionOf(";
			Object[] newops = new Object[ops.length-1];
			for (int i=1; i<ops.length; i++) {
				opsidstring = new String(opsidstring + " " + ops[i].toString());
				newops[i-1] = ops[i];
			}
			opsidstring = opsidstring + ')';
			int oid2 = storage.getID(opsidstring);
			storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			createConjunctionBodyFacts(oid2,newops);
		}
	}

	protected void createHeadFacts(int id, OWLIndividual i) throws Exception {
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
	}

	protected void createHeadFacts(int sid, OWLClassExpression d) throws Exception {
		sid = storage.getID(d); //test
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
		} else {// TODO: add more description types
			System.err.println("Unsupported head class expression: " + d.toString());
		}
	}

}
