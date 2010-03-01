package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicAxiomVisitor implements OWLAxiomVisitorEx<Boolean> {
	/// Integer for todo flags as defined in BasicKBLoader.
	protected int todos;

	protected BasicExpressionVisitor expvisitor;
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	
	public BasicAxiomVisitor(StorageDriver sd, OWLDataFactory datafactory, BasicExpressionVisitor expvisitor, int todos) {
		storage = sd;
		this.todos = todos;
		this.expvisitor = expvisitor;
		this.datafactory = datafactory;
	}

	@Override
	public Boolean visit(OWLSubClassOfAxiom axiom) {
		return processSubClassOfAxiom(axiom.getSubClass(),axiom.getSuperClass());
	}
	
	@Override
	public Boolean visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLObjectIntersectionOf(
		                                   datafactory.getOWLObjectOneOf(axiom.getSubject()),
		                                   datafactory.getOWLObjectSomeValuesFrom(axiom.getProperty(), datafactory.getOWLObjectOneOf(axiom.getObject()) )),
		                              datafactory.getOWLNothing() );
	}

	@Override
	public Boolean visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		return visit(datafactory.getOWLDisjointObjectPropertiesAxiom(
				datafactory.getOWLObjectInverseOf(axiom.getProperty()),axiom.getProperty()));
	}

	@Override
	public Boolean visit(OWLReflexiveObjectPropertyAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLThing(), datafactory.getOWLObjectHasSelf(axiom.getProperty()));
	}

	@Override
	public Boolean visit(OWLDisjointClassesAxiom axiom) {
		Object[] descs = axiom.getClassExpressions().toArray();
		boolean result = true;
		int botid = storage.getIDForNothing();
		String[] keys = new String[descs.length];
		for (int i=0; i<descs.length; i++) {
			keys[i] = expvisitor.visitAndAct((OWLClassExpression)descs[i],getVisitorBodyAction(todos));
			if (keys[i] == null) return false;
		}
		int id1, id2, conid;
		String conkey;
		ArrayList<String> ops;
		for (int i=0; i<descs.length; i++) {
			id1 = storage.getID(keys[i]);
			for (int j=i+1; j<descs.length; j++) {
				// TODO: the way how auxiliary conjunctions are created here is not optimal:
				// more of the work should be done inside BasicExpressionsVisitor.
				// See visit(OWLDifferentIndividualsAxiom) for an example of doing it in a cleaner way.
				ops = new ArrayList<String>(2);
				ops.add(keys[i]);
				ops.add(keys[j]);
				Collections.sort(ops);
				conkey = BasicExpressionVisitor.makeNAryExpressionKey("ObjectIntersection",ops,ops.size()-1,null);
				conid = storage.getID(conkey);
				if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
					storage.makePredicateAssertion("sco",conid,botid);
				}
				if ( (todos & BasicKBLoader.CHECK) != 0 ) {
					result = result && storage.checkPredicateAssertion("sco",conid,botid);
				}
				if ( (todos & BasicKBLoader.PREPARE) != 0 ) {
					BasicExpressionVisitor.createClassTautologies(conid,storage);
					id2 = storage.getID(keys[j]);
					// Note: auxiliary body facts were created in the earlier loop above
					if ((todos & BasicKBLoader.PREPARECHECK)!=0) {
						storage.makePredicateAssertion("sco",conid,id1);
						storage.makePredicateAssertion("sco",conid,id2);
					} else {
						storage.makePredicateAssertion("subconjunctionof",id1,id2,conid);
					}
				}
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDataPropertyDomainAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLDataSomeValuesFrom(axiom.getProperty(), datafactory.getTopDatatype()), axiom.getDomain());
	}

	@Override
	public Boolean visit(OWLObjectPropertyDomainAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLObjectSomeValuesFrom(axiom.getProperty(), datafactory.getOWLThing()), axiom.getDomain());
	}

	@Override
	public Boolean visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		Object[] props = axiom.getProperties().toArray();
		int j;
		boolean result = true;
		for (int i=0;i<props.length;i++) {
			j = ((i+1)%props.length);
			result = result && processSubObjectPropertyOf((OWLObjectPropertyExpression)props[i],(OWLObjectPropertyExpression)props[j]);
		}
		return result;
	}

	@Override
	public Boolean visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLObjectIntersectionOf(
		                                  datafactory.getOWLObjectOneOf(axiom.getSubject()),
		                                  datafactory.getOWLDataSomeValuesFrom(axiom.getProperty(), datafactory.getOWLDataOneOf(axiom.getObject()) )),
		                              datafactory.getOWLNothing() );
	}

	@Override
	public Boolean visit(OWLDifferentIndividualsAxiom axiom) {
		Object[] inds = axiom.getIndividuals().toArray();
		//String[] keys = new String[inds.length];
		OWLObjectOneOf[] nominals = new OWLObjectOneOf[inds.length];
		for (int i=0; i<inds.length; i++) {
			nominals[i] = datafactory.getOWLObjectOneOf((OWLIndividual)inds[i]);
			//keys[i] = expvisitor.visitAndAct(nominals[i],getVisitorBodyAction(todos));
		}
		boolean result = true;
		String key;
		for (int i=0; i<inds.length; i++) {
			for (int j=i+1; j<inds.length; j++) {
				key = expvisitor.visitAndAct(datafactory.getOWLObjectIntersectionOf(nominals[i],nominals[j]),getVisitorBodyAction(todos));
				result = result && processSubClassOfAxiom(key, BasicExpressionVisitor.OP_NOTHING);
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
		Object[] props = axiom.getProperties().toArray();
		boolean result = true;
		String[] keys = new String[props.length];
		for (int i=0; i<props.length; i++) {
			keys[i] = expvisitor.visitAndAct((OWLDataPropertyExpression)props[i],getVisitorBodyAction(todos));
			if (keys[i] == null) return false;
		}
		int pid1,pid2;
		for (int i=0;i<props.length;i++) {
			pid1 = storage.getID(keys[i]);
			if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK)) != 0 ) {
				for (int j=i+1;j<props.length;j++) {
					pid2 = storage.getID(keys[j]);
					if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
						storage.makePredicateAssertion("ddisjoint",pid1,pid2);
					}
					if ( (todos & BasicKBLoader.CHECK) != 0 ) {
						result = result && storage.checkPredicateAssertion("ddisjoint",pid1,pid2);
					}
				}
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
		Object[] props = axiom.getProperties().toArray();
		boolean result = true;
		String[] keys = new String[props.length];
		for (int i=0; i<props.length; i++) {
			keys[i] = expvisitor.visitAndAct((OWLObjectPropertyExpression)props[i],getVisitorBodyAction(todos));
			if (keys[i] == null) return false;
		}
		int pid1,pid2;
		for (int i=0;i<props.length;i++) {
			pid1 = storage.getID(keys[i]);
			if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK)) != 0 ) {
				for (int j=i+1;j<props.length;j++) {
					pid2 = storage.getID(keys[j]);
					if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
						storage.makePredicateAssertion("disjoint",pid1,pid2);
					}
					if ( (todos & BasicKBLoader.CHECK) != 0 ) {
						result = result && storage.checkPredicateAssertion("disjoint",pid1,pid2);
					}
				}
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
		boolean result = true;
		String propkey = expvisitor.visitAndAct(axiom.getProperty(),getVisitorBodyAction(todos));
		String rangekey = expvisitor.visitAndAct(axiom.getRange(),getVisitorHeadAction(todos));
		if ( (propkey == null) || (rangekey == null) ) return false;
		int pid = storage.getID(propkey), oid = storage.getID(rangekey);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("ran", pid, oid);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("ran", pid, oid);
		}
		return result;
	}

	@Override
	public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
		return processSubClassOfAxiom( datafactory.getOWLObjectOneOf(axiom.getSubject()),
		                               datafactory.getOWLObjectSomeValuesFrom(axiom.getProperty(),datafactory.getOWLObjectOneOf(axiom.getObject())) );
	}

	@Override
	public Boolean visit(OWLFunctionalObjectPropertyAxiom axiom) {
		return visit(datafactory.getOWLSubClassOfAxiom(datafactory.getOWLThing(), 
				datafactory.getOWLObjectMaxCardinality(1, axiom.getProperty(), datafactory.getOWLThing())));
	}

	@Override
	public Boolean visit(OWLSubObjectPropertyOfAxiom axiom) {
		return processSubObjectPropertyOf(axiom.getSubProperty(),axiom.getSuperProperty());
	}

	@Override
	public Boolean visit(OWLDisjointUnionAxiom axiom) {
		// Not allowed in OWL EL or RL
		return false;
	}

	@Override
	public Boolean visit(OWLDeclarationAxiom axiom) {
		// Only logical axioms are considered
		return false;
	}

	@Override
	public Boolean visit(OWLAnnotationAssertionAxiom axiom) {
		// This visitor only cares about logical axioms
		return false;
	}

	@Override
	public Boolean visit(OWLSymmetricObjectPropertyAxiom axiom) {
		return processSubObjectPropertyOf(datafactory.getOWLObjectInverseOf(axiom.getProperty()), axiom.getProperty());
	}

	@Override
	public Boolean visit(OWLDataPropertyRangeAxiom axiom) {
		boolean result = true;
		String propkey = expvisitor.visitAndAct(axiom.getProperty(),getVisitorBodyAction(todos));
		String rangekey = expvisitor.visitAndAct(axiom.getRange(),getVisitorHeadAction(todos));
		if ( (propkey == null) || (rangekey == null) ) return false;
		int pid = storage.getID(propkey), oid = storage.getID(rangekey);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("dran", pid, oid);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("dran", pid, oid);
		}
		return result;
	}

	@Override
	public Boolean visit(OWLFunctionalDataPropertyAxiom axiom) {
		return visit(datafactory.getOWLSubClassOfAxiom(datafactory.getOWLThing(), 
				datafactory.getOWLDataMaxCardinality(1, axiom.getProperty(), datafactory.getTopDatatype())));
	}

	@Override
	public Boolean visit(OWLEquivalentDataPropertiesAxiom axiom) {
		Object[] props = axiom.getProperties().toArray();
		int j;
		boolean result = true;
		for (int i=0;i<props.length;i++) {
			j = ((i+1)%props.length);
			result = result && processSubDataPropertyOf((OWLDataPropertyExpression)props[i],(OWLDataPropertyExpression)props[j]);
		}
		return result;
	}

	@Override
	public Boolean visit(OWLClassAssertionAxiom axiom) {
		return processSubClassOfAxiom( datafactory.getOWLObjectOneOf(axiom.getIndividual()),axiom.getClassExpression() );
	}

	@Override
	public Boolean visit(OWLEquivalentClassesAxiom axiom) {
		Object[] descs = axiom.getClassExpressions().toArray();
		int j;
		boolean result = true;
		for (int i=0;i<descs.length;i++) {
			j = ((i+1)%descs.length);
			result = processSubClassOfAxiom((OWLClassExpression)descs[i],(OWLClassExpression)descs[j]) && result;
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDataPropertyAssertionAxiom axiom) {
		return processSubClassOfAxiom( datafactory.getOWLObjectOneOf(axiom.getSubject()),
		                               datafactory.getOWLDataSomeValuesFrom(axiom.getProperty(), datafactory.getOWLDataOneOf(axiom.getObject())) );
	}

	@Override
	public Boolean visit(OWLTransitiveObjectPropertyAxiom axiom) {
		ArrayList<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>(2);
		chain.add(axiom.getProperty());
		chain.add(axiom.getProperty());
		return visit(datafactory.getOWLSubPropertyChainOfAxiom(chain, axiom.getProperty()));
	}

	@Override
	public Boolean visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		return processSubClassOfAxiom(datafactory.getOWLObjectHasSelf(axiom.getProperty()), datafactory.getOWLNothing());
	}

	@Override
	public Boolean visit(OWLSubDataPropertyOfAxiom axiom) {
		return processSubDataPropertyOf(axiom.getSubProperty(),axiom.getSuperProperty()); 
	}

	@Override
	public Boolean visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		return visit(datafactory.getOWLSubClassOfAxiom(datafactory.getOWLThing(), 
				datafactory.getOWLObjectMaxCardinality(1, datafactory.getOWLObjectInverseOf(axiom.getProperty()), datafactory.getOWLThing())));
	}

	@Override
	public Boolean visit(OWLSameIndividualAxiom axiom) {
		Object[] inds = axiom.getIndividuals().toArray();
		int j;
		boolean result = true;
		for (int i=0;i<inds.length;i++) {
			j = ((i+1)%inds.length);
			result = result && processSubClassOfAxiom(datafactory.getOWLObjectOneOf((OWLIndividual)inds[i]),datafactory.getOWLObjectOneOf((OWLIndividual)inds[j]));
		}
		return result;
	}

	@Override
	public Boolean visit(OWLSubPropertyChainOfAxiom axiom) {
		List<OWLObjectPropertyExpression> chain = axiom.getPropertyChain();
		if (chain.size() == 2) {
			if ( (todos & (BasicKBLoader.ASSERT | BasicKBLoader.CHECK) ) == 0 ) return true; // nothing to do
			boolean result = true;
			String pkey, pkey1, pkey2;
			pkey = expvisitor.visitAndAct(axiom.getSuperProperty(),getVisitorHeadAction(todos));
			pkey1 = expvisitor.visitAndAct(chain.get(0),getVisitorBodyAction(todos));
			pkey2 = expvisitor.visitAndAct(chain.get(1),getVisitorBodyAction(todos));
			if ( (pkey == null) || (pkey1 == null) || (pkey2 == null) ) return false;
			int pid = storage.getID(pkey), pid1 = storage.getID(pkey1), pid2 = storage.getID(pkey2);
			if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
				storage.makePredicateAssertion("spoc", pid1, pid2, pid);				
			}
			if ( (todos & BasicKBLoader.CHECK) != 0 ) {
				result = storage.checkPredicateAssertion("spoc", pid1, pid2, pid);
			}
			return result;
		} else {
			// TODO recursion (prepare subchains even if todos don't have BasicKBLoader.ASSERT)
			return false;
		}
	}

	@Override
	public Boolean visit(OWLInverseObjectPropertiesAxiom axiom) {
		boolean result = true;
		String pkey1, pkey2;
		pkey1 = expvisitor.visitAndAct(axiom.getFirstProperty(),getVisitorBodyAction(todos));
		pkey2 = expvisitor.visitAndAct(axiom.getSecondProperty(),getVisitorBodyAction(todos));
		if ( (pkey1 == null) || (pkey2 == null) ) return false;
		int pid1 = storage.getID(pkey1), pid2 = storage.getID(pkey2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("inverseof", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("inverseof", pid1, pid2);
		}
		return result;		
	}

	@Override
	public Boolean visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
		String typekey = expvisitor.visitAndAct(axiom.getDatatype(),getVisitorBodyAction(todos));
		String rangekey = expvisitor.visitAndAct(axiom.getDataRange(),getVisitorBodyAction(todos));
		if ( (typekey==null) || (rangekey==null) ) return null;
		// generate data for both polarities
		if (expvisitor.visitAndAct(axiom.getDataRange(),getVisitorHeadAction(todos)) == null) return null;
		
		boolean result = true;
		int keyid = storage.getID(typekey);
		int rangeid = storage.getID(rangekey);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("dsco",keyid,rangeid);
			storage.makePredicateAssertion("dsco",rangeid,keyid);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("dsco",keyid,rangeid) && storage.checkPredicateAssertion("dsco",rangeid,keyid);
		}
		return result;
	}

	@Override
	public Boolean visit(SWRLRule rule) {
		// No support for rules at the moment
		return false;
	}

	@Override
	public Boolean visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		// Annotations are not considered in Orel
		return false;
	}

	@Override
	public Boolean visit(OWLAnnotationPropertyDomainAxiom axiom) {
		// Annotations are not considered in Orel
		return false;
	}

	@Override
	public Boolean visit(OWLAnnotationPropertyRangeAxiom axiom) {
		// Annotations are not considered in Orel
		return false;
	}

	protected Boolean processSubClassOfAxiom(OWLClassExpression c1, OWLClassExpression c2) {
		String key1 = expvisitor.visitAndAct(c1,getVisitorBodyAction(todos));
		String key2 = expvisitor.visitAndAct(c2,getVisitorHeadAction(todos));
		return processSubClassOfAxiom(key1,key2);
	}
		
	protected Boolean processSubClassOfAxiom(String key1, String key2) {
		boolean result = true;
		if ( (key1 == null) || (key2 == null) ) return false;
		int id1 = storage.getID(key1);
		int id2 = storage.getID(key2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) { 
			// trivial cases not stored
			if (id1 != id2)	storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if ( (id1 != id2) && (!key1.equals(BasicExpressionVisitor.OP_NOTHING)) && (!key2.equals(BasicExpressionVisitor.OP_THING)) ) { // no need to check if trivial
				result = storage.checkPredicateAssertion("sco",id1,id2);
			}
		}
		return result;
	}

	protected Boolean processSubObjectPropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
		boolean result = true;
		String key1 = expvisitor.visitAndAct(p1,getVisitorBodyAction(todos));
		String key2 = expvisitor.visitAndAct(p2,getVisitorHeadAction(todos));
		int pid1 = storage.getID(key1), pid2 = storage.getID(key2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("spo", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if (pid1 != pid2) { // no need to check if trivial
				result = storage.checkPredicateAssertion("spo", pid1, pid2);
			}
		}
		return result;
	}

	protected Boolean processSubDataPropertyOf(OWLDataPropertyExpression p1, OWLDataPropertyExpression p2) {
		boolean result = true;
		String key1 = expvisitor.visitAndAct(p1,getVisitorBodyAction(todos));
		String key2 = expvisitor.visitAndAct(p2,getVisitorHeadAction(todos));
		int pid1 = storage.getID(key1), pid2 = storage.getID(key2);
		if ( (todos & BasicKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("dspo", pid1, pid2);				
		}
		if ( (todos & BasicKBLoader.CHECK) != 0 ) {
			if (pid1 != pid2) { // no need to check if trivial
				result = storage.checkPredicateAssertion("dspo", pid1, pid2);
			}
		}
		return result;
	}

	protected BasicExpressionVisitor.Action getVisitorBodyAction(int todos) {
		if ( (todos & BasicKBLoader.PREPAREASSERT) != 0) {
			return BasicExpressionVisitor.Action.WRITEBODY;
		} else if ( (todos & BasicKBLoader.PREPARECHECK) != 0 ) {
			return BasicExpressionVisitor.Action.WRITEHEAD;
		} else {
			return BasicExpressionVisitor.Action.READ; 
		}
	}
	
	protected BasicExpressionVisitor.Action getVisitorHeadAction(int todos) {
		if ( (todos & BasicKBLoader.PREPAREASSERT) != 0) {
			return BasicExpressionVisitor.Action.WRITEHEAD;
		} else if ( (todos & BasicKBLoader.PREPARECHECK) != 0 ) {
			return BasicExpressionVisitor.Action.WRITEBODY;
		} else {
			return BasicExpressionVisitor.Action.READ; 
		}
	}

}
