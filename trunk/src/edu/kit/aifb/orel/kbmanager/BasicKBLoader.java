package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.storage.SimpleLiteral;
import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicKBLoader {
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	
	/// Flags for more fine-grained control of actions during axiom processing
	static protected final int CHECK = 1;
	static protected final int PREPARECHECK = 2;
	static protected final int ASSERT = 4;
	static protected final int PREPAREASSERT = 8;
	/// Combination of flags
	static protected final int PREPARE = PREPAREASSERT | PREPARECHECK;
	static protected final int WRITE = PREPARE | ASSERT;
	
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
		boolean writing = ( (todos & BasicKBLoader.WRITE) != 0 );
		if ( writing ) { // initialize bridge and prepare for bulk insert:
			storage.beginLoading();
		}
		// iterate over ontology to load all axioms:
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int count = 0;
		while( (result || writing) && axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			result = processLogicalAxiom(axiom, todos) && result;
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		System.out.println(" processed " + count + " axiom(s).");
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
			result = processSubclassOf(datafactory.getOWLThing(), datafactory.getOWLObjectHasSelf(((OWLReflexiveObjectPropertyAxiom)axiom).getProperty()),todos);
		} else if (axiom instanceof OWLIrreflexiveObjectPropertyAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {
			OWLObjectPropertyDomainAxiom pda = (OWLObjectPropertyDomainAxiom)axiom;
			result = processSubclassOf(datafactory.getOWLObjectSomeValuesFrom(pda.getProperty(), datafactory.getOWLThing()), pda.getDomain(),todos);
		} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
			result = processRange(((OWLObjectPropertyRangeAxiom)axiom).getProperty(),((OWLObjectPropertyRangeAxiom)axiom).getRange(),todos);
		} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
			result = processDisjointProperties(((OWLDisjointObjectPropertiesAxiom)axiom).getProperties(),todos);
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
			result = processSubclassOf( datafactory.getOWLObjectOneOf(pa.getSubject()) ,
			                            datafactory.getOWLObjectSomeValuesFrom(
					                       pa.getProperty(), datafactory.getOWLObjectOneOf(pa.getObject()) ), todos );
		} else if (axiom instanceof OWLSameIndividualAxiom) {
			result = processSameIndividuals(((OWLSameIndividualAxiom)axiom).getIndividuals(),todos);
		} else if (axiom instanceof OWLDifferentIndividualsAxiom) {
			result = processDifferentIndividuals(((OWLDifferentIndividualsAxiom)axiom).getIndividuals(),todos);
		} else if (axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) {
			OWLNegativeObjectPropertyAssertionAxiom pa = (OWLNegativeObjectPropertyAssertionAxiom) axiom;
			Set<OWLClassExpression> classes = new HashSet<OWLClassExpression>();
			classes.add( datafactory.getOWLObjectOneOf(pa.getSubject()) );
			classes.add( datafactory.getOWLObjectSomeValuesFrom(
					pa.getProperty(), datafactory.getOWLObjectOneOf(pa.getObject()) ) );
			result = processDisjointClasses(classes, todos);
		} else if (axiom instanceof OWLDataPropertyAssertionAxiom) {
			OWLDataPropertyAssertionAxiom pa = (OWLDataPropertyAssertionAxiom) axiom;
			result = processDataPropertyAssertion( pa.getSubject(), pa.getProperty(), pa.getObject(),todos);
		} else if (axiom instanceof OWLNegativeDataPropertyAssertionAxiom) {
			result = false; // TODO
			System.err.println("The following axiom is not supported: " + axiom + "\n");
		} else { // unknown logical axiom type
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
			//if ( (!c1.isOWLNothing()) && (!c2.isOWLThing()) ) { // no need to check if trivial
				result = storage.checkPredicateAssertion("sco",id1,id2);
			}
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			result = createBodyFacts(id1,c1,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			result = createHeadFacts(id2,c2,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
		}
		return result;
	}

	protected boolean processEquivalentClasses(Set<OWLClassExpression> descriptions, int todos) throws Exception {
		Object[] descs = descriptions.toArray();
		int j;
		boolean result = true;
		for (int i=0;i<descs.length;i++) {
			j = ((i+1)%descs.length);
			result = processSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j], todos) && result;
		}
		return result;
	}
	
	protected boolean processDisjointClasses(Set<OWLClassExpression> descriptions, int todos) throws Exception {
		Object[] descs = descriptions.toArray();
		boolean result = true;
		int botid = storage.getIDForNothing();
		int id1, id2;
		ArrayList<OWLClassExpression> ops;
		for (int i=0; i<descs.length; i++) {
			id1 = storage.getID((OWLClassExpression)descs[i]);
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
					// Note: auxiliary body facts created in outer loop (once per class)
					// (nothing to prepare for bottom)
					if ((todos & BasicKBLoader.PREPARECHECK)!=0) {
						id2 = storage.getID((OWLClassExpression)descs[j]);
						storage.makePredicateAssertion("sco", interid, id1);
						storage.makePredicateAssertion("sco", interid, id2);
					} else {
						result = createConjunctionBodyFacts(interid,ops) && result;
					}
				}
			}
			if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
				result = createBodyFacts(id1,(OWLClassExpression)descs[i],((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			}
		}
		return result;
	}

	protected boolean processSubObjectPropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2, int todos) throws Exception {
		boolean result = true;
		int pid1 = storage.getID(p1), pid2 = storage.getID(p2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("spo", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if (pid1 != pid2) { // no need to check if trivial
				result = storage.checkPredicateAssertion("spo", pid1, pid2);
			}
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			result = createPropertyFacts(pid1,p1) && result;
			result = createPropertyFacts(pid2,p2) && result;
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
				storage.makePredicateAssertion("spoc", pid1, pid2, pid);				
			}
			if ( (todos & BasicKBLoader.CHECK) != 0 ) {
				result = storage.checkPredicateAssertion("spoc", pid1, pid2, pid);
			}
			if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
				result = createPropertyFacts(pid1,chain.get(0)) && result;
				result = createPropertyFacts(pid2,chain.get(1)) && result;
			}
			return result;
		} else {
			// TODO recursion (prepare subchains even if todos don't have BasicKBLoader.ASSERT)
			return false;
		}
	}
	
	protected boolean processDisjointProperties(Set<OWLObjectPropertyExpression> properties, int todos) throws Exception {
		Object[] props = properties.toArray();
		boolean result = true;
		int pid1,pid2;
		for (int i=0;i<props.length;i++) {
			pid1 = storage.getID((OWLObjectPropertyExpression)props[i]);
			if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK)) != 0 ) {
				for (int j=i+1;j<props.length;j++) {
					pid2 = storage.getID((OWLObjectPropertyExpression)props[j]);
					if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
						storage.makePredicateAssertion("disjoint",pid1,pid2);
					}
					if ( (todos & BasicKBLoader.CHECK) != 0 ) {
						result = result && storage.checkPredicateAssertion("disjoint",pid1,pid2);
					}
				}
			}
			if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
				result = createPropertyFacts(pid1,(OWLObjectPropertyExpression)props[i]) && result;
			}
		}
		return result;
	}

	protected boolean processRange(OWLObjectPropertyExpression property, OWLClassExpression range, int todos) throws Exception {
		boolean result = true;
		int pid = storage.getID(property), oid = storage.getID(range);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("ran", pid, oid);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("ran", pid, oid);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			result = createPropertyFacts(pid,property) && result;
			result = createHeadFacts(oid,range,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
		}
		return result;
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
			result = createBodyFacts(id1,i,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			result = createHeadFacts(id2,c,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
		}
		return result;
	}

/*	protected boolean processObjectPropertyAssertion(OWLIndividual s, OWLObjectPropertyExpression p, OWLIndividual o, int todos) throws Exception {
		boolean result = true;
		int sid = storage.getID(s);
		int pid = storage.getID(p);
		result = createPropertyFacts( ...
		int oid = storage.getID(o);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("sv",sid,pid,oid);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("sv",sid,pid,oid);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			result = ... createBodyFacts(sid,s,((todos & BasicKBLoader.PREPARECHECK)!=0));
			createHeadFacts(oid,o,((todos & BasicKBLoader.PREPARECHECK)!=0));
		}
		return result;
	}*/

	protected boolean processDataPropertyAssertion(OWLIndividual s, OWLDataPropertyExpression p, OWLLiteral l, int todos) throws Exception {
		boolean result = true;
		SimpleLiteral sl = Literals.makeSimpleLiteral(l);
		if (sl == null) return false;
		int sid = storage.getID(s);
		int pid = storage.getID(p);
		int lid = storage.getID(sl);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("dsv",sid,pid,lid);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("dsv",sid,pid,lid);
		}
		if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
			result = createBodyFacts(sid,s,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			result = createHeadFacts(lid,sl,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
		}
		return result;
	}
	
	protected boolean processDifferentIndividuals(Set<OWLIndividual> individuals, int todos) throws Exception {
		Object[] inds = individuals.toArray();
		boolean result = true;
		int botid = storage.getIDForNothing();
		int oid1,oid2;
		ArrayList<OWLIndividual> ops;
		for (int i=0; i<inds.length; i++) {
			oid1 = storage.getID((OWLIndividual)inds[i]);
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
					// Note: auxiliary body facts created in outer loop (once per class)
					// (nothing to prepare for bottom)
					oid2 = storage.getID((OWLIndividual)inds[j]);
					if ((todos & BasicKBLoader.PREPARECHECK)!=0) {
						storage.makePredicateAssertion("sco",interid,oid1);
						storage.makePredicateAssertion("sco",interid,oid2);
					} else {
						storage.makePredicateAssertion("subconjunctionof",oid1,oid2,interid);
					}
				}
			}
			if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
				result = createBodyFacts(oid1,(OWLIndividual)inds[i],((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			}
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
			result = createBodyFacts(id1,i1,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
			result = createHeadFacts(id2,i2,((todos & BasicKBLoader.PREPARECHECK)!=0)) && result;
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
	

	/**
	 * Create auxiliary tuples for nominals appearing in body positions, where
	 * individuals are used to represent nominals. Note that by punning, there can
	 * be classes of the same IRI. The id generation for individuals therefore
	 * always encloses them in "ObjectOneOf" and generally ensures a unified
	 * id selection for all variants in which nominals can occur. 
	 * @param id
	 * @param i
	 * @throws Exception
	 */
	protected boolean createBodyFacts(int id, OWLIndividual i, boolean invert) throws Exception {
		if (invert) {
			return createHeadFacts(id,i,false);
		}
		createClassTautologies(id);
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
		return true;
	}

	protected boolean createBodyFacts(int id, SimpleLiteral sl, boolean invert) throws Exception {
		if (invert) {
			return createHeadFacts(id,sl,false);
		}
		storage.makePredicateAssertion("dnominal",id);
		storage.makePredicateAssertion("dnonempty",id);
		return true;
	}
	
	protected boolean createBodyFacts(int id, OWLClassExpression d, boolean invert) throws Exception {
		if (invert) {
			return createHeadFacts(id,d,false);
		}
		boolean result = true;
		createClassTautologies(id);
		if (d instanceof OWLClass) {
			// nothing special to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			ArrayList<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(((OWLObjectIntersectionOf) d).getOperands());
			Collections.sort(ops); // make sure that we have a defined order; cannot have random changes between prepare and check!
			result = createConjunctionBodyFacts(id, ops);
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = storage.getID(filler);
			storage.makePredicateAssertion("subsomevalues",pid,sid,id);
			result = createPropertyFacts(pid,((OWLObjectSomeValuesFrom)d).getProperty());
			result = createBodyFacts(sid,filler,false) && result;
		} else if (d instanceof OWLObjectHasValue) {
			int pid = storage.getID(((OWLObjectHasValue)d).getProperty());
			OWLIndividual value = ((OWLObjectHasValue)d).getValue();
			int sid = storage.getID(value);
			storage.makePredicateAssertion("subsomevalues",pid,sid,id);
			result = createPropertyFacts(pid,((OWLObjectHasValue)d).getProperty());
			result = createBodyFacts(sid,value,false) && result;
		} else if (d instanceof OWLObjectUnionOf) {
			Iterator<OWLClassExpression> opit = ((OWLObjectUnionOf)d).getOperands().iterator();
			OWLClassExpression op;
			int sid;
			while (opit.hasNext()) {
				op = opit.next();
				sid = storage.getID(op);
				storage.makePredicateAssertion("sco",sid,id);
				result = createBodyFacts(sid,op,false) && result;
			}
		} else if (d instanceof OWLObjectOneOf) {
			Set<OWLIndividual> inds = ((OWLObjectOneOf)d).getIndividuals();
			if (inds.size() == 1) {
				result = createBodyFacts(id,inds.iterator().next(),false);
			} else {
				result = createBodyFacts(id,((OWLObjectOneOf)d).asObjectUnionOf(),false);
			}
		} else if (d instanceof OWLObjectHasSelf) {
			int pid = storage.getID(((OWLObjectHasSelf)d).getProperty());
			storage.makePredicateAssertion("subself",pid,id);
			result = createPropertyFacts(pid,((OWLObjectHasSelf)d).getProperty());
		} else if (d instanceof OWLObjectAllValuesFrom){
			int pid = storage.getID(((OWLObjectAllValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectAllValuesFrom)d).getFiller();
			int sid = storage.getID(filler);
			storage.makePredicateAssertion("suballvalues",pid,sid,id);
			result = createPropertyFacts(pid,((OWLObjectAllValuesFrom)d).getProperty());
			result = createBodyFacts(sid,filler,false) && result;
		} else {// TODO: add more description types
			System.err.println("Unsupported body class expression: " + d.toString());
			result = false;
		}
		return result;
	}
	
	protected boolean createConjunctionBodyFacts(int id, List<OWLClassExpression> ops) throws Exception {
		if (ops.size() <= 0) return true;
		int oid1 = storage.getID(ops.get(0));
		boolean result = createBodyFacts(oid1,ops.get(0),false);
		if (ops.size() == 2) {
			int oid2 = storage.getID(ops.get(1));
			if ( (oid1 == oid2) || (ops.get(1).isOWLThing()) )  { // rare cases, let's not bother with those elsewhere
				storage.makePredicateAssertion("sco",oid1,id);
			} else if (ops.get(0).isOWLThing()) {
				storage.makePredicateAssertion("sco",oid2,id);
			} else {
				storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			}
			result = createBodyFacts(oid2,ops.get(1),false) && result;
		} else { // recursion
			ArrayList<OWLClassExpression> newops = new ArrayList<OWLClassExpression>(ops.size()-1);
			for (int i=1; i<ops.size(); i++) {
				newops.add(ops.get(i));
			}
			int oid2 = storage.getIDForNaryExpression(StorageDriver.OP_OBJECT_INTERSECTION, ops);
			createClassTautologies(oid2);
			if (ops.get(0).isOWLThing()) {
				storage.makePredicateAssertion("sco",oid2,id);
			} else {
				storage.makePredicateAssertion("subconjunctionof",oid1,oid2,id);
			}
			result = createConjunctionBodyFacts(oid2,newops) && result;
		}
		return result;
	}

	/**
	 * Create auxiliary tuples for nominals appearing in head positions, where
	 * individuals are used to represent nominals. Note that by punning, there can
	 * be classes of the same IRI. The id generation for individuals therefore
	 * always encloses them in "ObjectOneOf" and generally ensures a unified
	 * id selection for all variants in which nominals can occur. 
	 * @param id
	 * @param i
	 * @throws Exception
	 */
	protected boolean createHeadFacts(int id, OWLIndividual i, boolean invert) throws Exception {
		if (invert) {
			return createBodyFacts(id,i,false);
		}
		createClassTautologies(id);		
		storage.makePredicateAssertion("nominal",id);
		storage.makePredicateAssertion("nonempty",id);
		return true;
	}

	protected boolean createHeadFacts(int id, SimpleLiteral sl, boolean invert) throws Exception {
		if (invert) {
			return createBodyFacts(id,sl,false);
		}
		storage.makePredicateAssertion("dnominal",id);
		storage.makePredicateAssertion("dnonempty",id);
		List<String> typeuris = Literals.getDatatypeURIs(sl);
		for (int i=0; i<typeuris.size(); i++) {
			storage.makePredicateAssertion("dsco",id,storage.getIDForDatatypeURI(typeuris.get(i)));
		}
		return true;
	}

	protected boolean createHeadFacts(int sid, OWLClassExpression d, boolean invert) throws Exception {
		if (invert) {
			return createBodyFacts(sid,d,false);
		}
		boolean result = true;
		createClassTautologies(sid);
		if (d instanceof OWLClass) {
			// nothing special to do here
		} else if (d instanceof OWLObjectIntersectionOf){
			Iterator<OWLClassExpression> descit = ((OWLObjectIntersectionOf)d).getOperands().iterator();
			OWLClassExpression desc;
			int descid;
			while (descit.hasNext()) {
				desc = descit.next();
				descid = storage.getID(desc);
				storage.makePredicateAssertion("sco",sid,descid);
				result = createHeadFacts(descid,desc,false) && result;
			}
		} else if (d instanceof OWLObjectSomeValuesFrom){
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			result = createPropertyFacts(pid,((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int oid = storage.getID(filler);
			int auxid = storage.getSkolemID(d);
			storage.makePredicateAssertion("sv",sid,pid,auxid);
			storage.makePredicateAssertion("sco",auxid,oid);
			result = createHeadFacts(oid,filler,false) && result;
		} else if (d instanceof OWLObjectHasValue) {
			int pid = storage.getID(((OWLObjectHasValue)d).getProperty());
			result = createPropertyFacts(pid,((OWLObjectHasValue)d).getProperty());
			OWLIndividual value = ((OWLObjectHasValue)d).getValue();
			int auxid = storage.getSkolemID(d);
			int oid = storage.getID(value);
			storage.makePredicateAssertion("sv",sid,pid,auxid);
			storage.makePredicateAssertion("sco",auxid,oid);
			result = createHeadFacts(oid,value,false) && result;
		} else if (d instanceof OWLObjectOneOf) {
			Set<OWLIndividual> inds = ((OWLObjectOneOf) d).getIndividuals();
			if (inds.size() == 1) {
				result = createHeadFacts(sid,inds.iterator().next(),false);
			} else { // only unary nominals can occur in heads
				System.err.println("Unsupported head class expression: " + d.toString());
				result = false;
			}
		} else if (d instanceof OWLObjectHasSelf) {
			int pid = storage.getID(((OWLObjectHasSelf)d).getProperty());
			result = createPropertyFacts(pid,((OWLObjectHasSelf)d).getProperty());
			storage.makePredicateAssertion("self",sid,pid);
		} else if (d instanceof OWLObjectAllValuesFrom){
			int pid = storage.getID(((OWLObjectAllValuesFrom)d).getProperty());
			result = createPropertyFacts(pid,((OWLObjectAllValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectAllValuesFrom)d).getFiller();
			int oid = storage.getID(filler);
			storage.makePredicateAssertion("av",sid,pid,oid);
			result = createHeadFacts(oid,filler,false) && result;
		} else {// TODO: add more description types
			System.err.println("Unsupported head class expression: " + d.toString());
			result = false;
		}
		return result;
	}
	
	public void createClassTautologies(int id) throws Exception {
		storage.makePredicateAssertion("sco",id,id);
		storage.makePredicateAssertion("sco",id,storage.getIDForThing());
		storage.makePredicateAssertion("sco",storage.getIDForNothing(),id);
	}
	
	public boolean createPropertyFacts(int id, OWLObjectPropertyExpression p) throws Exception {
		storage.makePredicateAssertion("spo",id,id);
		if (p instanceof OWLObjectInverseOf) {
			return false; // currently not supported
		} else if ( p.isOWLTopObjectProperty() || p.isOWLBottomObjectProperty() ) {
			return false; // currently not supported
		} else {
			return true;
		}
	}

}
