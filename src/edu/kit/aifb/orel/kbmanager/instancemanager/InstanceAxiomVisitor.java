package edu.kit.aifb.orel.kbmanager.instancemanager;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * OWLAxiomVisitorEx for processing the axioms of an ontology when used by
 * InstanceKBManager. Please see the documentation of that class for details
 * about the storage model.
 * 
 * The visitor can perform different reading or writing activities that are
 * relevant for loading and inferencing. The activites that are to be done
 * during one visit are defined by the variable todos which is a bitwise
 * disjunction of the flags InstanceKBLoader.CHECK, InstanceKBLoader.ASSERT,
 * InstanceKBLoader.PREPAREASSERT, and InstanceKBLoader.PREPARECHECK. See their
 * documentation for details.
 * 
 * All visiting methods return a boolean to indicate the outcome/success of the
 * performed actions. If multiple actions are performed, the result is their
 * conjunction.
 * @author Markus Krötzsch
 */
public class InstanceAxiomVisitor implements OWLAxiomVisitorEx<Boolean> {
	/// Integer for todo flags as defined in InstanceKBLoader.
	protected int todos;

	protected InstanceExpressionVisitor expvisitor;
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	
	public InstanceAxiomVisitor(StorageDriver sd, OWLDataFactory datafactory, InstanceExpressionVisitor expvisitor, int todos) {
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
		String key;
		for (int i=0; i<descs.length; i++) {
			for (int j=i+1; j<descs.length; j++) {
				key = expvisitor.visitAndAct(datafactory.getOWLObjectIntersectionOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j]),getVisitorBodyAction(todos));
				result = result && processSubClassOfAxiom(key, InstanceExpressionVisitor.OP_NOTHING);
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
		OWLObjectOneOf[] nominals = new OWLObjectOneOf[inds.length];
		for (int i=0; i<inds.length; i++) {
			nominals[i] = datafactory.getOWLObjectOneOf((OWLIndividual)inds[i]);
		}
		boolean result = true;
		String key;
		for (int i=0; i<inds.length; i++) {
			for (int j=i+1; j<inds.length; j++) {
				key = expvisitor.visitAndAct(datafactory.getOWLObjectIntersectionOf(nominals[i],nominals[j]),getVisitorBodyAction(todos));
				result = result && processSubClassOfAxiom(key, InstanceExpressionVisitor.OP_NOTHING);
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
		if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) { // currently no support for checking this
			return false;
		}
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
			if ( (todos & (InstanceKBLoader.ASSERT | InstanceKBLoader.CHECK)) != 0 ) {
				for (int j=i+1;j<props.length;j++) {
					pid2 = storage.getID(keys[j]);
					if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
						storage.makePredicateAssertion("dpdisjoint",pid1,pid2);
					}
					if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
						result = false;
					}
				}
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
		if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) { // currently no support for checking this
			return false;
		}
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
			if ( (todos & (InstanceKBLoader.ASSERT | InstanceKBLoader.CHECK)) != 0 ) {
				for (int j=i+1;j<props.length;j++) {
					pid2 = storage.getID(keys[j]);
					if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
						storage.makePredicateAssertion("pdisjoint",pid1,pid2);
					}
					if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
						result = false;
					}
				}
			}
		}
		return result;
	}

	@Override
	public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
		return processSubClassOfAxiom( datafactory.getOWLThing(),
                datafactory.getOWLObjectAllValuesFrom(axiom.getProperty(),axiom.getRange()) );
		// Do not support EL ranges for now (all ranges are interpreted under RL semantics)
//		boolean result = true;
//		String propkey = expvisitor.visitAndAct(axiom.getProperty(),getVisitorBodyAction(todos));
//		String rangekey = expvisitor.visitAndAct(axiom.getRange(),getVisitorHeadAction(todos));
//		if ( (propkey == null) || (rangekey == null) ) return false;
//		int pid = storage.getID(propkey), oid = storage.getID(rangekey);
//		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
//			storage.makePredicateAssertion("ran", pid, oid);				
//		}
//		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
//			result = storage.checkPredicateAssertion("ran", pid, oid);
//		}
//		return result;
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
		return processSubClassOfAxiom( datafactory.getOWLThing(),
                datafactory.getOWLDataAllValuesFrom(axiom.getProperty(),axiom.getRange()) );
		// Do not support EL ranges for now (all ranges are interpreted under RL semantics)
//		boolean result = true;
//		String propkey = expvisitor.visitAndAct(axiom.getProperty(),getVisitorBodyAction(todos));
//		String rangekey = expvisitor.visitAndAct(axiom.getRange(),getVisitorHeadAction(todos));
//		if ( (propkey == null) || (rangekey == null) ) return false;
//		int pid = storage.getID(propkey), oid = storage.getID(rangekey);
//		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
//			storage.makePredicateAssertion("dran", pid, oid);				
//		}
//		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
//			result = storage.checkPredicateAssertion("dran", pid, oid);
//		}
//		return result;
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
			if ( (todos & (InstanceKBLoader.ASSERT | InstanceKBLoader.CHECK) ) == 0 ) return true; // nothing to do
			boolean result = true;
			String pkey, pkey1, pkey2;
			pkey = expvisitor.visitAndAct(axiom.getSuperProperty(),getVisitorHeadAction(todos));
			pkey1 = expvisitor.visitAndAct(chain.get(0),getVisitorBodyAction(todos));
			pkey2 = expvisitor.visitAndAct(chain.get(1),getVisitorBodyAction(todos));
			if ( (pkey == null) || (pkey1 == null) || (pkey2 == null) ) return false;
			int pid = storage.getID(pkey), pid1 = storage.getID(pkey1), pid2 = storage.getID(pkey2);
			if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
				storage.makePredicateAssertion("subchain", pid1, pid2, pid);				
			}
			if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
				result = storage.checkPredicateAssertion("subchain", pid1, pid2, pid);
			}
			return result;
		} else {
			// TODO recursion (prepare subchains even if todos don't have InstanceKBLoader.ASSERT)
			return false;
		}
	}

	@Override
	public Boolean visit(OWLInverseObjectPropertiesAxiom axiom) {
		if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) { // currently no support for checking this
			return false;
		}		
		boolean result = true;
		String pkey1, pkey2;
		pkey1 = expvisitor.visitAndAct(axiom.getFirstProperty(),getVisitorBodyAction(todos));
		pkey2 = expvisitor.visitAndAct(axiom.getSecondProperty(),getVisitorBodyAction(todos));
		if ( (pkey1 == null) || (pkey2 == null) ) return false;
		int pid1 = storage.getID(pkey1), pid2 = storage.getID(pkey2);
		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("subinv", pid1, pid2);				
		}
		if ( (todos & InstanceKBLoader.CHECK) != 0 ) { // TODO support checks for subproperties
			result = false;
		}
		return result;		
	}

	@Override
	public Boolean visit(OWLHasKeyAxiom axiom) {
		// TODO Implement this method.
		return false;
	}

	@Override
	public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
		String typekey = expvisitor.visitAndAct(axiom.getDatatype(),getVisitorBodyAction(todos));
		String rangekey = expvisitor.visitAndAct(axiom.getDataRange(),getVisitorBodyAction(todos));
		if ( (typekey==null) || (rangekey==null) ) return false;
		// generate data for both polarities
		if (expvisitor.visitAndAct(axiom.getDataRange(),getVisitorHeadAction(todos)) == null) return false;
		
		boolean result = true;
		int keyid = storage.getID(typekey);
		int rangeid = storage.getID(rangekey);
		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
			storage.makePredicateAssertion("dsubc",keyid,rangeid);
			storage.makePredicateAssertion("dsubc",rangeid,keyid);
		}
		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
			result = storage.checkPredicateAssertion("dinst",keyid,rangeid) && storage.checkPredicateAssertion("dinst",rangeid,keyid);
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
		// TODO Should this method really exclude checks/assertions that are "clearly" tautologies? 
		boolean result = true;
		if ( (key1 == null) || (key2 == null) ) return false;
		int id1 = storage.getID(key1);
		int id2 = storage.getID(key2);
		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) { 
			// trivial cases not stored
			if (id1 != id2)	storage.makePredicateAssertion("subc",id1,id2);
		}
		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
			if ( (id1 != id2) && (!key1.equals(InstanceExpressionVisitor.OP_NOTHING)) && (!key2.equals(InstanceExpressionVisitor.OP_THING)) ) { // no need to check if trivial
				result = storage.checkPredicateAssertion("inst",id1,id2);
			}
		}
		return result;
	}

	protected Boolean processSubObjectPropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
		if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) { // currently no support for checking this
			return false;
		}
		boolean result = true;
		String key1 = expvisitor.visitAndAct(p1,getVisitorBodyAction(todos));
		String key2 = expvisitor.visitAndAct(p2,getVisitorHeadAction(todos));
		int pid1 = storage.getID(key1), pid2 = storage.getID(key2);
		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("subp", pid1, pid2);				
		}
		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
			// TODO Support checking for property entailments?
			result = false;
		}
		return result;
	}

	protected Boolean processSubDataPropertyOf(OWLDataPropertyExpression p1, OWLDataPropertyExpression p2) {
		if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) { // currently no support for checking this
			return false;
		}
		boolean result = true;
		String key1 = expvisitor.visitAndAct(p1,getVisitorBodyAction(todos));
		String key2 = expvisitor.visitAndAct(p2,getVisitorHeadAction(todos));
		int pid1 = storage.getID(key1), pid2 = storage.getID(key2);
		if ( (todos & InstanceKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("dsubp", pid1, pid2);				
		}
		if ( (todos & InstanceKBLoader.CHECK) != 0 ) {
			// TODO Support checking for property entailments?
			result = false;
		}
		return result;
	}

	protected InstanceExpressionVisitor.Action getVisitorBodyAction(int todos) {
		if ( (todos & InstanceKBLoader.PREPAREASSERT) != 0) {
			return InstanceExpressionVisitor.Action.WRITEBODY;
		} else if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) {
			return InstanceExpressionVisitor.Action.WRITEHEAD;
		} else {
			return InstanceExpressionVisitor.Action.READ; 
		}
	}
	
	protected InstanceExpressionVisitor.Action getVisitorHeadAction(int todos) {
		if ( (todos & InstanceKBLoader.PREPAREASSERT) != 0) {
			return InstanceExpressionVisitor.Action.WRITEHEAD;
		} else if ( (todos & InstanceKBLoader.PREPARECHECK) != 0 ) {
			return InstanceExpressionVisitor.Action.WRITEBODY;
		} else {
			return InstanceExpressionVisitor.Action.READ; 
		}
	}

}

