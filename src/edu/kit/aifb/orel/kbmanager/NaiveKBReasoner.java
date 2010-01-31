package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLOntology;


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
		
		rules.put("self-weak", "sv(x,p,x) :- self(x,p)");
		rules.put("self-prop", "self(x,q) :- self(x,p), subpropertyof(p,q)");
		rules.put("self-ind",  "self(x,p) :- sv(x,p,x), nominal(x)");
		rules.put("self-sub",  "sco(x,y)  :- self(x,p), subself(p,y)");

        //Self(p,x), Range(p,y) -> sCO(x,y)
		//sV(x,p,y), sV(x,p,z),atMostOne(p,z),Self(p,x) -> sCO(x,y)
        // (this should be dispensable as it goes beyond safety of atMostOne) 
        // Re (B4) Handling of EL range restrictions:
        //sV(x,p,y2), Range(p,y1), SubConjunctionOfFirst(y1,y2,z) -> sV(x,p,z)
        //(assuming that sV is "sCO-upward-saturated" in the last argument, 
        //also pending: symmetric case wrt. SubConjOfFirst)
		
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
			curstep++;
		}
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
		storage.dumpStatistics();
	}
	
	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	public boolean checkEntailment(OWLOntology ontology) throws Exception {
		BasicKBLoader loader = new BasicKBLoader(storage);
		loader.processOntology(ontology, BasicKBLoader.PREPARECHECK );
		materialize();
		// inconsistent ontologies entail anything
		if (storage.checkPredicateAssertion("nonempty",storage.getIDForNothing())) {
			return true;
		}
		return loader.processOntology(ontology, BasicKBLoader.CHECK );
	}
	
}
