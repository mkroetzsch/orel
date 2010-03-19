package edu.kit.aifb.orel.kbmanager.elhmanager;

import java.util.ArrayList;
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
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
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

import edu.kit.aifb.orel.kbmanager.BasicExpressionVisitor;
import edu.kit.aifb.orel.storage.StorageDriver;

public class ELAxiomVisitor implements OWLAxiomVisitorEx<Boolean> {
	/// Integer for todo flags as defined in BasicKBLoader.
	protected int todos;

	protected ELExpressionVisitor expvisitor;
	protected StorageDriver storage;
	protected OWLDataFactory datafactory;
	
	public ELAxiomVisitor(StorageDriver sd, OWLDataFactory datafactory, ELExpressionVisitor expvisitor, int todos) {
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
		return false;
	}

	@Override
	public Boolean visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLReflexiveObjectPropertyAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLDisjointClassesAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLDataPropertyDomainAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLObjectPropertyDomainAxiom axiom) {
		return false;
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
		return false;
	}

	@Override
	public Boolean visit(OWLDifferentIndividualsAxiom axiom) {
		return false;
}

	@Override
	public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
		return processSubClassOfAxiom( datafactory.getOWLObjectOneOf(axiom.getSubject()),
		                               datafactory.getOWLObjectSomeValuesFrom(axiom.getProperty(),datafactory.getOWLObjectOneOf(axiom.getObject())) );
	}

	@Override
	public Boolean visit(OWLFunctionalObjectPropertyAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLSubObjectPropertyOfAxiom axiom) {
		return processSubObjectPropertyOf(axiom.getSubProperty(),axiom.getSuperProperty());
	}

	@Override
	public Boolean visit(OWLDisjointUnionAxiom axiom) {
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
		return false;
	}

	@Override
	public Boolean visit(OWLDataPropertyRangeAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLFunctionalDataPropertyAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLEquivalentDataPropertiesAxiom axiom) {
		return false;
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
		return false;
	}

	@Override
	public Boolean visit(OWLSubDataPropertyOfAxiom axiom) {
		return false; 
	}

	@Override
	public Boolean visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLSameIndividualAxiom axiom) {
		return false;
	}

	@Override
	public Boolean visit(OWLSubPropertyChainOfAxiom axiom) {
		List<OWLObjectPropertyExpression> chain = axiom.getPropertyChain();
		if (chain.size() == 2) {
			if ( (todos & (ELKBLoader.ASSERT | ELKBLoader.CHECK) ) == 0 ) return true; // nothing to do
			boolean result = true;
			String pkey, pkey1, pkey2;
			pkey = expvisitor.visitAndAct(axiom.getSuperProperty(),getVisitorHeadAction(todos));
			pkey1 = expvisitor.visitAndAct(chain.get(0),getVisitorBodyAction(todos));
			pkey2 = expvisitor.visitAndAct(chain.get(1),getVisitorBodyAction(todos));
			if ( (pkey == null) || (pkey1 == null) || (pkey2 == null) ) return false;
			int pid = storage.getID(pkey), pid1 = storage.getID(pkey1), pid2 = storage.getID(pkey2);
			if ( (todos & ELKBLoader.ASSERT) != 0 ) {
				storage.makePredicateAssertion("spoc", pid1, pid2, pid);				
			}
			if ( (todos & ELKBLoader.CHECK) != 0 ) {
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
		return false;
}

	@Override
	public Boolean visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
		return false;
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
		if ( (todos & ELKBLoader.ASSERT) != 0 ) { 
			// trivial cases not stored
			if (id1 != id2)	storage.makePredicateAssertion("sco",id1,id2);
		}
		if ( (todos & ELKBLoader.CHECK) != 0 ) {
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
		if ( (todos & ELKBLoader.ASSERT) != 0 ) {
			// trivial cases not stored
			if (pid1 != pid2) storage.makePredicateAssertion("spo", pid1, pid2);				
		}
		if ( (todos & ELKBLoader.CHECK) != 0 ) {
			if (pid1 != pid2) { // no need to check if trivial
				result = storage.checkPredicateAssertion("spo", pid1, pid2);
			}
		}
		return result;
	}

//	protected Boolean processSubDataPropertyOf(OWLDataPropertyExpression p1, OWLDataPropertyExpression p2) {
//		boolean result = true;
//		String key1 = expvisitor.visitAndAct(p1,getVisitorBodyAction(todos));
//		String key2 = expvisitor.visitAndAct(p2,getVisitorHeadAction(todos));
//		int pid1 = storage.getID(key1), pid2 = storage.getID(key2);
//		if ( (todos & ELKBLoader.ASSERT) != 0 ) {
//			// trivial cases not stored
//			if (pid1 != pid2) storage.makePredicateAssertion("dspo", pid1, pid2);				
//		}
//		if ( (todos & ELKBLoader.CHECK) != 0 ) {
//			if (pid1 != pid2) { // no need to check if trivial
//				result = storage.checkPredicateAssertion("dspo", pid1, pid2);
//			}
//		}
//		return result;
//	}

	protected ELExpressionVisitor.Action getVisitorBodyAction(int todos) {
		if ( (todos & ELKBLoader.PREPAREASSERT) != 0) {
			return ELExpressionVisitor.Action.WRITEBODY;
		} else if ( (todos & ELKBLoader.PREPARECHECK) != 0 ) {
			return ELExpressionVisitor.Action.WRITEHEAD;
		} else {
			return ELExpressionVisitor.Action.READ; 
		}
	}
	
	protected ELExpressionVisitor.Action getVisitorHeadAction(int todos) {
		if ( (todos & ELKBLoader.PREPAREASSERT) != 0) {
			return ELExpressionVisitor.Action.WRITEHEAD;
		} else if ( (todos & ELKBLoader.PREPARECHECK) != 0 ) {
			return ELExpressionVisitor.Action.WRITEBODY;
		} else {
			return ELExpressionVisitor.Action.READ; 
		}
	}

}
