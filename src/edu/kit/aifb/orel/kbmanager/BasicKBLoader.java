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
	
	public BasicKBLoader(StorageDriver storage) {
		this.storage = storage;
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	@SuppressWarnings("unchecked")
	public void loadOntology(OWLOntology ontology, boolean donotassert) throws Exception {
		// initialize bridge and prepare for bulk insert:
		storage.beginLoading();

		// iterate over ontology to load all axioms:
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int count = 0;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				loadSubclassOf(((OWLSubClassOfAxiom) axiom).getSubClass(), ((OWLSubClassOfAxiom) axiom).getSuperClass(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				loadEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getClassExpressions(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				loadSubpropertyOf(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty(), ((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				loadSubpropertyChainOf(((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain(), ((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
				loadClassAssertion( ((OWLClassAssertionAxiom) axiom).getIndividual(), ((OWLClassAssertionAxiom) axiom).getClassExpression(), donotassert); 
			} else if (axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION) {
				OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual> pa = ((OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual>) axiom);
				loadPropertyAssertion( pa.getSubject(), pa.getProperty(), pa.getObject(), donotassert); 
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		System.out.println(" loaded " + count + " axioms.");
		// close, commit, and recompute indexes
		storage.endLoading();
	}

	protected void loadSubclassOf(OWLClassExpression c1, OWLClassExpression c2, boolean donotassert) throws Exception {
		//System.err.println("Calling subclass of.");
		int id1 = storage.getID(c1);
		int id2 = storage.getID(c2);
		if (donotassert == false) {
			storage.insertIdsToTable("sco",id1,id2);
		}
		createBodyFacts(id1,c1);
		createHeadFacts(id2,c2);
	}

	protected void loadEquivalentClasses(Set<OWLClassExpression> descriptions, boolean donotassert) throws Exception {
		Object[] descs = descriptions.toArray();
		int j;
		for(int i=0;i<descs.length;i++){
			j=(i%(descs.length-1))+1;
			loadSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j], donotassert);
		}
	}

	protected void loadSubpropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2, boolean donotassert) throws Exception {
		if (donotassert) return;
		int pid1 = storage.getID(p1), pid2 = storage.getID(p2);
		storage.insertIdsToTable("subpropertyof", pid1, pid2);
	}
	
	protected void loadSubpropertyChainOf(List<OWLObjectPropertyExpression> chain, OWLObjectPropertyExpression p, boolean donotassert) throws Exception {
		if (chain.size() == 2) {
			if (donotassert) return;
			int pid = storage.getID(p), pid1 = storage.getID(chain.get(0)), pid2 = storage.getID(chain.get(1));
			storage.insertIdsToTable("subpropertychain", pid1, pid2, pid);
		} else {
			// TODO recursion (even if donotassert==true we need to assert the subchains here)
		}
	}

	protected void loadClassAssertion(OWLIndividual i, OWLClassExpression c, boolean donotassert) throws Exception {
		//System.err.println("Calling subclass of.");
		int id1 = storage.getID(i);
		int id2 = storage.getID(c);
		if (donotassert == false) {
			storage.insertIdsToTable("sco",id1,id2);
		}
		createBodyFacts(id1,i);
		createHeadFacts(id2,c);
	}

	protected void loadPropertyAssertion(OWLIndividual s, OWLObjectProperty p, OWLIndividual o, boolean donotassert) throws Exception {
		//System.err.println("Calling subclass of.");
		int sid = storage.getID(s);
		int pid = storage.getID(p);
		int oid = storage.getID(o);
		if (donotassert == false) {
			storage.insertIdsToTable("sv",sid,pid,oid);
		}
		createBodyFacts(sid,s);
		createHeadFacts(oid,o);
	}
	
	protected void createBodyFacts(int id, OWLIndividual i) throws Exception {
		storage.insertIdsToTable("nominal",id);
		storage.insertIdsToTable("nonempty",id);
	}
	
	protected void createBodyFacts(int id, OWLClassExpression d) throws Exception {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			createConjunctionBodyFacts(id, ((OWLObjectIntersectionOf) d).getOperands().toArray());
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = storage.getID(filler);
			storage.insertIdsToTable("subsomevalues",pid,sid,id);
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
			storage.insertIdsToTable("subconjunctionof",oid1,oid2,id);
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
			storage.insertIdsToTable("subconjunctionof",oid1,oid2,id);
			createConjunctionBodyFacts(oid2,newops);
		}
	}

	protected void createHeadFacts(int id, OWLIndividual i) throws Exception {
		storage.insertIdsToTable("nominal",id);
		storage.insertIdsToTable("nonempty",id);
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
				storage.insertIdsToTable("sco",sid,descid);
				createHeadFacts(descid,desc);
			}
		} else if (d instanceof OWLObjectSomeValuesFrom){
			int pid = storage.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int oid = storage.getID(filler);
			storage.insertIdsToTable("sv",sid,pid,oid);
			createHeadFacts(oid,filler);
		} else {// TODO: add more description types
			System.err.println("Unsupported head class expression: " + d.toString());
		}
	}

}
