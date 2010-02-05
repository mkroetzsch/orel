package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLOntology;


import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.kbmanager.BasicKBManager.InferenceResult;
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
	
	protected void registerCheckRules() {
		inferencerules = new ArrayList<String>();
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable
		
		// Recall that these rules are not recursively applied!
		// Many of these rules won't be used for querying, but we still 
		// use them to document the incompleteness in materializing derived
		// predicates.  
		rules.put("self-p", "?self(x,q) :- self(x,p), spo(p,q)");
		rules.put("sv-p",   "?sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		rules.put("sv-x",   "?sv(x,p,z) :- sco(x,y), sv(y,p,z)");
		rules.put("sv-px",  "?sv(x,q,z) :- sco(x,y), spo(p,q), sv(y,p,z)");
		rules.put("av-x",   "?av(x,p,y) :- sco(x,x1), av(x1,p,y)");
		rules.put("av-y",   "?av(x,p,y) :- av(x,p,y1), sco(y1,y)");
		rules.put("av-xy",  "?av(x,p,y) :- sco(x,x1), av(x1,p,y1), sco(y1,y)");
		
		// Unsound test: if x has a p to something, then it has a p to itself
		//storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString("test","?sco(x,y) :- subself(p,y), sv(x,p,z)"));
		
		// Rules without bodies not supported yet:
		//storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString("test","?self(x,x) :- "));
		
		// now register those rules:
		Iterator<String> nameit = rules.keySet().iterator();
		String name;
		while (nameit.hasNext()) {
			name = nameit.next();
			storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString(name,rules.get(name)));
		}
	}
	
	protected void registerInferenceRules() {
		inferencerules = new ArrayList<String>();
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable
		rules.put("spo", "spo(x,z) :- spo(x,y,0), spo(y,z)");
		rules.put("chain-spo1", "spoc(u,v2,w) :- spo(u,v1), spoc(v1,v2,w)");
		rules.put("chain-spo2", "spoc(v1,u,w) :- spo(u,v2), spoc(v1,v2,w)");
		
		//rules.put("del-ref",  "-sco(x,x) :- ");
		
		rules.put("sco",    "sco(x,z)  :- sco(x,y), sco(y,z)");
		rules.put("con",    "sco(x,z)  :- subconjunctionof(y1,y2,z), sco(x,y1), sco(x,y2)");
		rules.put("con r1", "sco(x,z)  :- subconjunctionof(x,y,z), sco(x,y)");
		rules.put("con r2", "sco(x,z)  :- subconjunctionof(y,x,z), sco(x,y)");
		rules.put("con r3", "sco(x,z)  :- subconjunctionof(x,x,z)");
		rules.put("subsome","sco(x,y)  :- sv(x,v,z), subsomevalues(v,z,y)");
		rules.put("subsome-p","sco(x,y) :- sv(x,v,z), spo(v,u), subsomevalues(u,z,y)");
		rules.put("chain",  "sv(x,w,z) :- sv(x,v1,y), sv(y,v2,z), spoc(v1,v2,w)");
		//rules.put("sv-x",   "sv(x,p,z) :- sco(x,y), sv(y,p,z)");
		rules.put("sv-y",   "sv(x,p,z) :- sv(x,p,y), sco(y,z)");
		//rules.put("sv-p",   "sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		
		rules.put("nom1", "sco(y,x) :- sco(x,y), nonempty(x), nominal(y)");
		rules.put("nom2", "nonempty(y) :- sco(x,y), nonempty(x)");
		rules.put("nom3", "nonempty(y) :- sv(x,v,y), nonempty(x)");
		
		rules.put("self-weak", "sv(x,p,x) :- self(x,p)");
		rules.put("self-ind",  "self(x,p) :- sv(x,p,x), nominal(x)");
		rules.put("self-x",    "self(x,p) :- sco(x,y), self(y,p)");
		//rules.put("self-p",    "self(x,q) :- self(x,p), spo(p,q)");
		rules.put("self-ind-sco",  "self(x,p) :- sco(x,y), sv(y,p,x), nominal(x)");
		rules.put("subself",   "sco(x,y)  :- self(x,p), subself(p,y)");
		
		rules.put("av-p", "av(x,q,y) :- spo(q,p), av(x,p,y)");
		rules.put("av1", "sco(y1,y2) :- nominal(v), sco(v,x1), sco(v,x2), sv(x1,p,y1), av(x2,p,y2)");
		rules.put("av2", "sco(y1,y2) :- nominal(v),            sco(v,x2), sv(v ,p,y1), av(x2,p,y2)");
		rules.put("av3", "sco(y1,y2) :- nominal(v), sco(v,x1),            sv(x1,p,y1), av(v ,p,y2)");
		rules.put("av4", "sco(y1,y2) :- nominal(v),                       sv(v ,p,y1), av(v ,p,y2)");
		
		rules.put("suball",     "sco(x,z) :- av(x,p,y), suballvalues(p,y,z)");
		rules.put("suball-sco", "sco(x,z) :- av(x,p,y1), sco(y1,y2), suballvalues(p,y2,z)");

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
			System.out.println("============");
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
	public InferenceResult checkEntailment(OWLOntology ontology) throws Exception {
		BasicKBLoader loader = new BasicKBLoader(storage);
		boolean loaded = loader.processOntology(ontology, BasicKBLoader.PREPARECHECK );
		materialize();
		registerCheckRules();
		// inconsistent ontologies entail anything
		if (storage.checkPredicateAssertion("nonempty",storage.getIDForNothing())) {
			return InferenceResult.YES;
		} else if (!loader.processOntology(ontology, BasicKBLoader.CHECK )) { // some supported axioms not entailed
			return InferenceResult.NO;
		} else if (loaded) { // all axioms supported and entailed 
			return InferenceResult.YES;
		} else { // some axioms not supported, all supported ones entailed
			return InferenceResult.DONTKNOW;
		}
	}

	/**
	 * Check if the loaded axioms are consistent.
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 */
	public InferenceResult checkConsistency() throws Exception {
		materialize();
		registerCheckRules();
		if (storage.checkPredicateAssertion("nonempty",storage.getIDForNothing())) {
			return InferenceResult.NO;
		} else {
			return InferenceResult.YES;
		}
	}
	
}
