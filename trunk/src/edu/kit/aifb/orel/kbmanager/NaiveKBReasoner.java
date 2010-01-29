package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * A simple reasoning implementation based on a naive saturation of the
 * knowledge based for a given set of inference rules.  
 * @author Markus Krötzsch
 */
public class NaiveKBReasoner {
	protected StorageDriver storage;
	protected ArrayList<String> inferencerules;
	
	public NaiveKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}
	
	protected void registerInferenceRules() {
		inferencerules = new ArrayList<String>();
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable
		rules.put("prop-1", "subpropertyof(x,z) :- subpropertyof(x,y,0), subpropertyof(y,z)");
		rules.put("prop-2", "subpropertychain(u,v2,w) :- subpropertyof(u,v1), subpropertychain(v1,v2,w)");
		rules.put("prop-3", "subpropertychain(v1,u,w) :- subpropertyof(u,v2), subpropertychain(v1,v2,w)");
		
		//rules.put("del-ref",  "-sco(x,x) :- ");
		
		rules.put("trans",  "sco(x,z)  :- sco(x,y), sco(y,z)");
		rules.put("E",      "sco(x,z)  :- subconjunctionof(y1,y2,z), sco(x,y1), sco(x,y2)");
		rules.put("E ref1", "sco(x,z)  :- subconjunctionof(x,y,z), sco(x,y)");
		rules.put("E ref2", "sco(x,z)  :- subconjunctionof(y,x,z), sco(x,y)");
		rules.put("E ref3", "sco(x,z)  :- subconjunctionof(x,x,z)");
		rules.put("F",      "sco(x,y)  :- sv(x,v,z), subsomevalues(v,z,y)");
		rules.put("G",      "sco(x,y)  :- sv(x,v,z), subpropertyof(v,u), subsomevalues(u,z,y)");
		rules.put("Hn",     "sv(x,w,z) :- sv(x,v1,y), sv(y,v2,z), subpropertychain(v1,v2,w)");
		rules.put("Jn",     "sv(x,v,z) :- sv(x,v,y), sco(y,z)");
		
		rules.put("Nom 1n", "sco(y,x) :- sco(x,y), nonempty(x), nominal(y)");
		rules.put("Nom 2n", "nonempty(y) :- sco(x,y), nonempty(x)");
		rules.put("Nom 3n", "nonempty(y) :- sv(x,v,y), nonempty(x)");
		
		// now register those rules:
		Iterator<String> nameit = rules.keySet().iterator();
		String name;
		while (nameit.hasNext()) {
			name = nameit.next();
			inferencerules.add(name);
			storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString(name,rules.get(name)));
		}
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		long sTime=System.currentTimeMillis();
		registerInferenceRules();
		int affectedrows = 1, curstep = 0;
		while (affectedrows > 0) {
			affectedrows = 0;
			for (int i=0; i<inferencerules.size(); i++) {
				affectedrows = affectedrows + storage.runRule(inferencerules.get(i),curstep,curstep);
			}
		}
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
		storage.dumpStatistics();
	}
	
	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	@SuppressWarnings("unchecked")
	public boolean checkEntailment(OWLOntology ontology) throws Exception {
		materialize();
		// now check entailment of all axioms
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int id1,id2;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				id1 = storage.getID(((OWLSubClassOfAxiom) axiom).getSubClass());
				id2 = storage.getID(((OWLSubClassOfAxiom) axiom).getSuperClass());
				if ( (id1 != id2) && (!storage.checkIdsInTable("sco",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				Object[] descs = ((OWLEquivalentClassesAxiom)axiom).getClassExpressions().toArray();
				int j;
				for(int i=0;i<descs.length;i++){
					j=(i%(descs.length-1))+1;
					id1 = storage.getID((OWLClassExpression)descs[i]);
					id2 = storage.getID((OWLClassExpression)descs[j]);
					if ( (id1 != id2) && (!storage.checkIdsInTable("sco",id1,id2)) ) return false;
				}
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				id1 = storage.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty());
				id2 = storage.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty());
				if ( (id1 != id2) && (!storage.checkIdsInTable("subpropertyof",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				List<OWLObjectPropertyExpression> chain = ((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain();
				if (chain.size() == 2) {
					id1 = storage.getID(chain.get(0));
					id2 = storage.getID(chain.get(1));
					if (!storage.checkIdsInTable("subpropertychain",id1,id2,storage.getID(((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty()))) return false;
				} else {
					//return false;
					// TODO
				}
			} else if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
				//return false;
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
		}
		return true;
	}
	
}
