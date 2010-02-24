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
		//rules.put("self-p", "?self(x,q) :- self(x,p), spo(p,q)");
		//rules.put("sv-p",   "?sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		//rules.put("sv-x",   "?sv(x,p,z) :- sco(x,y), sv(y,p,z)");
		//rules.put("sv-px",  "?sv(x,q,z) :- sco(x,y), spo(p,q), sv(y,p,z)");
		//rules.put("av-x",   "?av(x,p,y) :- sco(x,x1), av(x1,p,y)");
		//rules.put("av-y",   "?av(x,p,y) :- av(x,p,y1), sco(y1,y)");
		//rules.put("av-xy",  "?av(x,p,y) :- sco(x,x1), av(x1,p,y1), sco(y1,y)");
		
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
		int top = storage.getIDForThing();
		int bot = storage.getIDForNothing();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable

        /// spoc
        rules.put("spocp", "spoc(p',q',r) :- spo(p',p), spo(q',q), spoc(p,q,r)");
        /// spo
        //TODO: property(p) --> spo(p,p)
        rules.put("spo",            "spo(p,r) :- spo(p,q), spo(q,r)");
        rules.put("sposelfspocdom", "spo(q,r) :- subsomevalues(q," + top + ",x), sco(x,y), self(y,p), spoc(p,q,r)");
        rules.put("sposelfspocran", "spo(p,r) :- ran(p,x), sco(x,y), self(y,q), spoc(p,q,r)");
        /// sco
        // NOTE: we create sco(x,top), sco(bot,x), and sco(x,x) at load time
        rules.put("sco",         "sco(x,z) :- sco(x,y), sco(y,z)"); 
        rules.put("con",         "sco(x,z) :- sco(x,y1), sco(x,y2), subconjunctionof(y1,y2,z)");
        rules.put("svr",         "sco(x,z) :- sv(x,p,y), sco(y,y'), subsomevalues(p,y',z)");
        rules.put("svr-self",    "sco(x,z) :- self(x,p), sco(x,y'), subsomevalues(p,y',z)");
        rules.put("scoeq",       "sco(y,x) :- nonempty(x), nominal(y), sco(x,y)");
        rules.put("selfran",     "sco(x,y) :- self(x,p), ran(p,y)"); // this is "scoran-self"
        rules.put("selfsubself", "sco(x,y) :- self(x,p), subself(p,y)");
        rules.put("scoran",      "sco(x,y) :- sv(x,p,y), ran(p,y)");
        /// svp
        rules.put("svp",          "sv(x,q,y) :- sv(x,p,y), spo(p,q)");
        rules.put("svspoc",       "sv(x,r,z) :- spoc(p,q,r), sv(x,p,x'), sco(x',z'), sv(z',q,z)");
        rules.put("svspoc-self1", "sv(x,r,z) :- spoc(p,q,r), self(x,p), sco(x,z'), sv(z',q,z)");
        rules.put("svspoc-self2", "sv(x,r,y) :- spoc(p,q,r), sv(x,p,y), sco(y,z), self(z,q)");
                //rules.put("svspoc-self3", "sv(x,r,z) :- spoc(p,q,r), self(x,p), sco(x,z), self(z,q)"); // reincarnated as "svselfspoc"
                //rules.put("svself",       "sv(x,p,x) :- sco(x,y), self(y,p)"); // this rule would violate the assumptions on sv (referring to aux. ids only)
        /// ran
        rules.put("ranp",   "ran(q,x) :- ran(p,x), spo(q,p)");
        rules.put("ransco", "ran(p,y) :- ran(p,x), sco(x,y)");
        rules.put("rancon", "ran(p,z) :- ran(p,x), ran(p,y), subconjunctionof(x,y,z)"); 
        /// nonempty
        rules.put("nenom",  "nonempty(x) :- nominal(x)");
        rules.put("nesco",  "nonempty(y) :- nonempty(x), sco(x,y)");
        /// self
        rules.put("selfp",        "self(x,q) :- self(x,p), spo(p,q)"); // subsumes "svp-self"
        rules.put("svselfspoc",   "self(x,r) :- sco(x,y1), sco(x,y2), self(y1,p), self(y2,q), spoc(p,q,r)");
        rules.put("selfnom",      "self(x,p) :- nominal(x), sco(x,y), sv(y,p,z), sco(z,x)");
        rules.put("selfnom-self", "self(x,p) :- nominal(x), sco(x,y), self(y,p), sco(y,x)");

		// <<<Role Disjointness:>>>
        rules.put("disnom",  "sco(x," + bot + ") :- disjoint(v,w), nominal(y), sco(x,x1), sco(x,x2), sv(x1,v,x1), sv(x2,w,x2), sco(x1,y), sco(x2,y)");
        rules.put("disspo",  "sco(y," + bot + ") :- sco(y,x), sv(x,u,x), disjoint(v,w), spo(u,v), spo(u,w)");
        rules.put("disself", "sco(x," + bot + ") :- disjoint(v,w), sco(x,y1), sco(x,y2), self(y1,v), self(y2,w)");

		rules.put("dissub1", "disjoint(p,q) :- disjoint(p1,q1), spo(p,p1), spo(q,q1)");
		rules.put("dissub2", "disjoint(p,q) :- subsomevalues(p," + top + ",x), subsomevalues(q," + top + ",y), sco(x,x'), sco(y,y'), subconjunctionof(x',y',z'), sco(z'," + bot + ")");
		rules.put("dissub3", "disjoint(p,q) :- ran(p,x), ran(q,y), sco(x,x'), sco(y,y'), subconjunctionof(x',y',z'), sco(z'," + bot + ")");
		rules.put("dissym",  "disjoint(p,q) :- disjoint(q,p)");
		
		//// Datatype property versions of rules below:
        /// dspoc [not existing in OWL 2] 
        /// dspo
        //TODO: property(p) --> spo(p,p)
        rules.put("dspo",           "dspo(p,r) :- dspo(p,q), dspo(q,r)");
        /// dsco
        // NOTE: we create sco(x,top), sco(bot,x), and sco(x,x) at load time
        rules.put("dsco",         "dsco(x,z) :- dsco(x,y), dsco(y,z)"); 
        rules.put("dcon",         "dsco(x,z) :- dsco(x,y1), dsco(x,y2), dsubconjunctionof(y1,y2,z)");
        rules.put("dsvr",         "dsco(x,z) :- dsv(x,p,y), dsco(y,y'), dsubsomevalues(p,y',z)");
        rules.put("dscoeq",       "dsco(y,x) :- dnonempty(x), dnominal(y), dsco(x,y)");
        rules.put("dscoran",      "dsco(x,y) :- dsv(x,p,y), dran(p,y)");
        /// dsvp
        rules.put("dsvp",          "dsv(x,q,y) :- dsv(x,p,y), dspo(p,q)");
        /// dran
        rules.put("dranp",   "dran(q,x) :- dran(p,x), dspo(q,p)");
        rules.put("dransco", "dran(p,y) :- dran(p,x), dsco(x,y)");
        rules.put("drancon", "dran(p,z) :- dran(p,x), dran(p,y), dsubconjunctionof(x,y,z)"); 
        /// dnonempty
        rules.put("dnenom",  "dnonempty(x) :- dnominal(x)");
        rules.put("dnesco",  "dnonempty(y) :- dnonempty(x), dsco(x,y)");
        /// dself [not existing in OWL 2]

		// <<<Role Disjointness:>>>
        // TODO
		
		/* old rule set below
		 
		// rules with "top" extension account for the lack of sco(x,top) and nonempty(top)
		
		rules.put("spo", "spo(x,z) :- spo(x,y,0), spo(y,z)");
		rules.put("chain-spo1", "spoc(u,v2,w) :- spo(u,v1), spoc(v1,v2,w)");
		rules.put("chain-spo2", "spoc(v1,u,w) :- spo(u,v2), spoc(v1,v2,w)");
		
		//rules.put("del-ref",  "-sco(x,x) :- ");
		
		rules.put("sco",    "sco(x,z)  :- sco(x,y), sco(y,z)");
		rules.put("con",    "sco(x,z)  :- subconjunctionof(y1,y2,z), sco(x,y1), sco(x,y2)");
		rules.put("con r1", "sco(x,z)  :- subconjunctionof(x,y,z), sco(x,y)");
		rules.put("con r2", "sco(x,z)  :- subconjunctionof(y,x,z), sco(x,y)");
		rules.put("con-top1","sco(y,z) :- subconjunctionof(x,y,z), sco(" + top + ",x)");
		rules.put("con-top1","sco(x,z) :- subconjunctionof(x,y,z), sco(" + top + ",y)");
		//rules.put("con r3", "sco(x,z)  :- subconjunctionof(x,x,z)"); // taken into account during loading now
		rules.put("subsome",  "sco(x,y) :- sv(x,v,z), subsomevalues(v,z,y)");
		rules.put("subsome-p","sco(x,y) :- sv(x,v,z), spo(v,u), subsomevalues(u,z,y)");
		rules.put("chain",   "sv(x,w,z) :- sv(x,v1,y), sv(y,v2,z), spoc(v1,v2,w)");
		//rules.put("sv-x",  "sv(x,p,z) :- sco(x,y), sv(y,p,z)");
		rules.put("sv-y",    "sv(x,p,z) :- sv(x,p,y), sco(y,z)");
		rules.put("sv-top", "sv(x,p," + top + ") :- sv(x,p,y)");
		//rules.put("sv-p",   "sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		
		rules.put("nom1", "sco(y,x) :- sco(x,y), nonempty(x), nominal(y)");
		rules.put("nom2", "nonempty(y) :- sco(x,y), nonempty(x)");
		rules.put("nom3", "nonempty(y) :- sv(x,v,y), nonempty(x)");
		rules.put("nom2-top", "nonempty(y) :- sco(" + top + ",y)");
		rules.put("nom3-top", "nonempty(y) :- sv(" + top + ",v,y)");
		
		// below rules still need "top" extension
		
		rules.put("self-weak", "sv(x,p,x) :- self(x,p)");
		rules.put("self-ind",  "self(x,p) :- sv(x,p,x), nominal(x)");
		rules.put("self-x",    "self(x,p) :- sco(x,y), self(y,p)");
		//rules.put("self-p",    "self(x,q) :- self(x,p), spo(p,q)");
		rules.put("self-ind-sco",  "self(x,p) :- sco(x,y), sv(y,p,x), nominal(x)");
		rules.put("subself",   "sco(x,y)  :- self(x,p), subself(p,y)");
		
		rules.put("av-p", "av(x,q,y) :- spo(q,p), av(x,p,y)");
		rules.put("av1", "sco(y1,y2) :- nominal(v), sco(v,x1), sco(v,x2), sv(x1,p,y1), av(x2,p,y2), nominal(y1)");
		rules.put("av2", "sco(y1,y2) :- nominal(v),            sco(v,x2), sv(v ,p,y1), av(x2,p,y2), nominal(y1)");
		rules.put("av3", "sco(y1,y2) :- nominal(v), sco(v,x1),            sv(x1,p,y1), av(v ,p,y2), nominal(y1)");
		rules.put("av4", "sco(y1,y2) :- nominal(v),                       sv(v ,p,y1), av(v ,p,y2), nominal(y1)");
		
		rules.put("suball",     "sco(x,z) :- av(x,p,y), suballvalues(p,y,z)");
		rules.put("suball-sco", "sco(x,z) :- av(x,p,y1), sco(y1,y2), suballvalues(p,y2,z)");*/
		
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
		} else if (!loaded) { // if checked axioms failed to load, neither YES nor NO are sure answers
			return InferenceResult.DONTKNOW;
		} else if (loader.processOntology(ontology, BasicKBLoader.CHECK )) { // some supported axioms not entailed
			return InferenceResult.YES;
		} else { 
			return InferenceResult.NO;
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
