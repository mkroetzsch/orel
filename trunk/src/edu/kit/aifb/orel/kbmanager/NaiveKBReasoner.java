package edu.kit.aifb.orel.kbmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLOntology;


import edu.kit.aifb.orel.client.LogWriter;
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
	//protected ArrayList<String> inferencerules = null;
	protected ArrayList<RuleInfo> ruleinfo = null;
	protected class RuleInfo implements Comparable<RuleInfo> {
		public int laststep;
		public String name;
		protected int lastaffected;
		protected int runs = 0;
		protected int successfulruns = 0;
		public RuleInfo (String name) {
			this.name = name;
			reset();
		}
		public void reset() {
			laststep = -1;
			lastaffected = 0;
		}
		public int compareTo(RuleInfo ri) {
			if (this.lastaffected > ri.lastaffected) {
				return -1;
			} else if (this.lastaffected < ri.lastaffected) {
				return 1;
			} else {
				float rate1 = (runs>0) ? (successfulruns/runs) : 0;
				float rate2 = (ri.runs>0) ? (ri.successfulruns/ri.runs) : 0;
				if (rate1 > rate2) {
					return -1;
				} else if (rate1 < rate2) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		public int getLastAffected() {
			return lastaffected;
		}
		public void setLastAffected(int n) {
			runs++;
			if (n>0) successfulruns++;
			lastaffected = n;
		}
	}
	
	public NaiveKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}
	
	protected void registerCheckRules() {
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
		if (ruleinfo != null) return; // do not do this multiple times in one run
//		inferencerules = new ArrayList<String>();
		ruleinfo = new ArrayList<RuleInfo>();
		HashMap<String,String> rules = new HashMap<String,String>();
		int top = storage.getIDForThing();
		int bot = storage.getIDForNothing();
		int dtop = storage.getIDForTopDatatype();
		int dbot = storage.getIDForBottomDatatype();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable

		/// spoc
		rules.put("spocp", "spoc(p',q',r) :- spo(p',p), spo(q',q), spoc(p,q,r)");
		/// spo
		// NOTE: spo(p,p) is created at load time
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
		rules.put("scoran",      "sco(y,z) :- sv(x,p,y), ran(p,z)");
		/// sv
		rules.put("svp",          "sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		rules.put("svspoc",       "sv(x,r,z) :- spoc(p,q,r), sv(x,p,x'), sco(x',z'), sv(z',q,z)");
		rules.put("svspoc-self1", "sv(x,r,z) :- spoc(p,q,r), self(x,p), sco(x,z'), sv(z',q,z)");
		rules.put("svspoc-self2", "sv(x,r,y) :- spoc(p,q,r), sv(x,p,y), sco(y,z), self(z,q)");
		rules.put("sv-self-nom",  "sv(x,p,x) :- sco(x,y), self(y,p), nominal(x)");
		rules.put("absnom1",     "sv(x',p,y) :- sco(x',x), sv(x,p,y), nominal(x')"); 
		rules.put("absnom2",     "sv(x,p,y') :- sv(x,p,y), sco(y,y'), nominal(y')"); 

			//rules.put("svspoc-self3", "sv(x,r,z) :- spoc(p,q,r), self(x,p), sco(x,z), self(z,q)"); // reincarnated as "svselfspoc"
			//rules.put("svself",       "sv(x,p,x) :- sco(x,y), self(y,p)"); // this rule would violate the assumptions on sv (referring to aux. ids only)
		/// ran
		rules.put("ranp",   "ran(q,x) :- ran(p,x), spo(q,p)");
		rules.put("ransco", "ran(p,y) :- ran(p,x), sco(x,y)");
		rules.put("rancon", "ran(p,z) :- ran(p,x), ran(p,y), subconjunctionof(x,y,z)"); 
		/// nonempty
		rules.put("nenom",  "nonempty(x) :- nominal(x)");
		rules.put("nesco",  "nonempty(y) :- nonempty(x), sco(x,y)");
		rules.put("nesv",   "nonempty(y) :- nonempty(x), sv(x,p,y)");
		/// self
		rules.put("selfp",        "self(x,q) :- self(x,p), spo(p,q)"); // subsumes "svp-self"
		rules.put("svselfspoc",   "self(x,r) :- sco(x,y1), sco(x,y2), self(y1,p), self(y2,q), spoc(p,q,r)");
		rules.put("selfnom",      "self(x,p) :- nominal(x), sco(x,y), sv(y,p,z), sco(z,x)");
		rules.put("selfnom-self", "self(x,p) :- nominal(x), sco(x,y), self(y,p), sco(y,x)");

		// <<<Role Disjointness:>>>
		rules.put("disnom1", "sco(x," + bot + ") :- disjoint(v,w), nominal(y), sco(x,x1), sco(x,x2), sv(x1,v,x1'), sv(x2,w,x2'), sco(x1',y), sco(x2',y)");
		rules.put("disnom2-aux", "disjointaux(v,x,y1) :- disjoint(v,w), nonempty(x), sco(x,x1), sv(x1,v,x1'), sco(x1',y1), nominal(y1)");
		rules.put("disnom2", "sco(z," + bot + ") :- disjoint(v,w), disjointaux(v,x,y1), disjointaux(w,x,y2), sco(z,y1), sco(z,y2)");
		rules.put("disnomran1", "ran(p," + bot + ") :- disjoint(v,w), nominal(y), ran(p,x1), ran(p,x2), sv(x1,v,x1'), sv(x2,w,x2'), sco(x1',y), sco(x2',y)");
		rules.put("disnomran2", "ran(p," + dbot + ") :- disjoint(v,w), disjointaux(v,x,y1), disjointaux(w,x,y2), ran(p,y1), ran(p,y2)");
		rules.put("disspo",  "sco(y," + bot + ") :- sco(y,x), sv(x,u,x'), disjoint(v,w), spo(u,v), spo(u,w)");
		rules.put("disself", "sco(x," + bot + ") :- disjoint(v,w), sco(x,y1), sco(x,y2), self(y1,v), self(y2,w)");
		
		rules.put("dissub1", "disjoint(p,q) :- disjoint(p1,q1), spo(p,p1), spo(q,q1)");
		rules.put("dissub2", "disjoint(p,q) :- subsomevalues(p," + top + ",x), subsomevalues(q," + top + ",y), sco(x,x'), sco(y,y'), subconjunctionof(x',y',z'), sco(z'," + bot + ")");
		rules.put("dissub3", "disjoint(p,q) :- ran(p,x), ran(q,y), sco(x,x'), sco(y,y'), subconjunctionof(x',y',z'), sco(z'," + bot + ")");
		rules.put("dissym",  "disjoint(p,q) :- disjoint(q,p)");
		
		// <<<Inverse roles>>>
		// Support for inverse is known to be incomplete in this calculus; it is not clear yet if the new intentional Orel calculus can have a practical support for complete inverseof reasoning
		rules.put("svinv",   "sv(y,q,x) :- inverseof(p,q), sv(x,p,y), nominal(x), nominal(y)"); 
		rules.put("selfinv", "self(x,q) :- self(x,p), inverseof(p,q)"); 
		rules.put("spoinv1", "spo(p,r)  :- inverseof(p,q), inverseof(q,r)");
		rules.put("spoinv2", "spo(p,q)  :- spo(p',q'), inverseof(p,p'), inverseof(q,q')");
		rules.put("disjinv", "disjoint(p,q)  :- disjoint(p',q'), inverseof(p,p'), inverseof(q,q')"); 
		rules.put("spocinv", "spoc(p,q,r)    :- spoc(q',p',r'), inverseof(p,p'), inverseof(q,q'), inverseof(r,r')"); 
		rules.put("invinv",  "inverseof(p,q) :- inverseof(q,p)"); 
		rules.put("invnom",  "inverseof(p,q) :- nominal(x), nominal(y), ran(p,x1), sco(x1,x), subsomevalues(p," + top + ", y1), sco(y1,y), ran(q,x2), sco(x2,x), subsomevalues(q," + top + ", y2), sco(y2,y)");
		rules.put("spoinv",  "spo(p,q) :- spo(p',q'), inverseof(p,p'), inverseof(q,q')");
		
		// <<<Other RL stuff>>>
		//rules.put("svpnomleft",   "sv(x,q,y) :- nominal(x), sco(x,x'), sv(x',p,y)");
		//rules.put("svpnomright",  "sv(x,q,y) :- sv(x,p,y'), sco(y',y), nominal(y)");

		rules.put("avsco1",        "av(x,p,y)     :- sco(x,z), av(z,p,y)");
		rules.put("avspo",         "av(x,q,y)     :- spo(q,p), av(x,p,y)");
		rules.put("avsco2",        "av(x,p,y)     :- av(x,p,z), sco(z,y)");
		rules.put("avran",         "av(" + top + ",p,x)   :- ran(p,x)");
		rules.put("avsubsome",     "av(x,q,y)     :- subsomevalues(p,x,y), inverseof(p,q)");
		rules.put("avselfatmost",  "av(x,p,x)     :- self(x,p), atmostone(x,p,x)");
		rules.put("avsvnomatmost", "av(x,p,y)     :- sv(x,p,y), nominal(y), atmostone(x,p," + top + ")");
		rules.put("avbotrole",     "av(" + top + ",p," + bot + ") :- disjoint(p,p)");
		rules.put("funcbackwards-aux", "atmostoneaux(p,y,x1,y1) :- atmostone(x,p,y), nominal(x1), nominal(y1), sv(x1,p,y1), sco(y1,y), sco(x1,x)");
		rules.put("funcbackwards", "sco(x'," + bot + ") :- sco(x',x1), sco(x',x2), atmostoneaux(p,y,x1,y1), atmostoneaux(p,y,x2,y2), subconjunctionof(y1,y2,z), sco(z," + bot + ")");
		
		rules.put("scoavsafe",     "sco(y,z)   :- sv(x,p,y), nominal(x), nominal(y), sco(x,w), av(w,p,z)");
		rules.put("scoatmostsafe", "sco(w1,w2) :- atmostone(x,p,y), nominal(x), sv(x,p,w1), nominal(w1), sco(w1,y), sv(x,p,w2), nominal(w2), sco(w2,y)"); 
		
		rules.put("amosco1",     "atmostone(x,p,y)                     :- sco(x,z), atmostone(z,p,y)");
		rules.put("amosco2",     "atmostone(x,p,y)                     :- atmostone(x,p,z), sco(y,z)");
		rules.put("amospo",      "atmostone(x,p,y)                     :- atmostone(x,q,y), spo(p,q)");
		//rules.put("amonom",      "atmostone(x,p,y)                     :- nominal(y)");    // unsafe, but only needed for output
		rules.put("amoav",       "atmostone(x,p," + top + ")           :- av(x,p,y), atmostone(x,p,y)");
		rules.put("amobotrole",  "atmostone(" + top + ",p," + top + ") :- disjoint(p,p)");
		
		
		//// Datatype property versions of rules below:
		/// dspoc [not existing in OWL 2] 
		/// dspo
		// NOTE: dspo(p,p) is created at load time
		rules.put("dspo",           "dspo(p,r) :- dspo(p,q), dspo(q,r)");
		/// dsco
		rules.put("dclash",  "dnonempty(" + dbot + ") :- dnominal(x), dnominal(y), dsco(x,y), orel:distinct(x,y)");
		rules.put("dsco",    "dsco(x,z) :- dsco(x,y), dsco(y,z)"); 
		rules.put("dcon",    "dsco(x,z) :- dsco(x,y1), dsco(x,y2), dsubconjunctionof(y1,y2,z)");
		rules.put("dsvr-el", "dsco(x,z) :- dsv(x,p,y), dsco(y,y1), eltype(y1), dsubsomevalues(p,y1,z)");
		rules.put("dsvr-rl", "dsco(x,z) :- dsv(x,p,y), dsco(y,y1), nominal(y1), dsco(y1,y2), dsubsomevalues(p,y2,z)");
		rules.put("dscoeq",  "dsco(y,x) :- dnonempty(x), dnominal(y), dsco(x,y)");
		rules.put("dscoran", "dsco(y,z) :- dsv(x,p,y), dran(p,z)");
		/// eltype
		rules.put("eltypesco", "eltype(y) :- eltype(x), dsco(x,y)");
		rules.put("eltypecon", "eltype(y) :- eltype(x1), eltype(x2), dsubconjunctionof(x1,x2,y)");
		/// dsv
		rules.put("dsvp",      "dsv(x,q,y) :- dsv(x,p,y), dspo(p,q)");
		rules.put("dabsnom1", "dsv(x',p,y) :- sco(x',x), dsv(x,p,y), nominal(x')"); 
		rules.put("dabsnom2", "dsv(x,p,y') :- dsv(x,p,y), sco(y,y'), dnominal(y')");
		/// dran
		rules.put("dranp",   "dran(q,x) :- dran(p,x), dspo(q,p)");
		rules.put("dransco", "dran(p,y) :- dran(p,x), dsco(x,y)");
		rules.put("drancon", "dran(p,z) :- dran(p,x), dran(p,y), dsubconjunctionof(x,y,z)"); 
		/// dnonempty
		rules.put("dnenom",  "dnonempty(x) :- dnominal(x)");
		rules.put("dnesco",  "dnonempty(y) :- dnonempty(x), dsco(x,y)");
		rules.put("dnesv",   "dnonempty(y) :- nonempty(x), dsv(x,p,y)");
		/// dself [not existing in OWL 2]

		// <<<Role Disjointness:>>>
		rules.put("ddisnom1", "sco(x," + bot + ")   :- ddisjoint(v,w), dnominal(y), sco(x,x1), sco(x,x2), dsv(x1,v,x1'), dsv(x2,w,x2'), dsco(x1',y), dsco(x2',y)");
		rules.put("ddisnom2-aux", "ddisjointaux(v,x,y1) :- ddisjoint(v,w), nonempty(x), sco(x,x1), dsv(x1,v,x1'), dsco(x1',y1), dnominal(y1)");
		rules.put("ddisnom2", "dsco(z," + dbot + ") :- ddisjoint(v,w), ddisjointaux(v,x,y1), ddisjointaux(w,x,y2), dsco(z,y1), dsco(z,y2)");
		rules.put("ddisnomran1", "ran(p," + bot + ")   :- ddisjoint(v,w), nominal(y), ran(p,x1), ran(p,x2), dsv(x1,v,x1'), dsv(x2,w,x2'), dsco(x1',y), dsco(x2',y)");
		rules.put("ddisnomran2", "dran(p," + dbot + ") :- ddisjoint(v,w), ddisjointaux(v,x,y1), ddisjointaux(w,x,y2), dran(p,y1), dran(p,y2)");
		rules.put("ddisspo",  "sco(y," + bot + ") :- sco(y,x), dsv(x,u,x'), ddisjoint(v,w), dspo(u,v), dspo(u,w)");
		
		rules.put("ddissub1", "ddisjoint(p,q) :- ddisjoint(p1,q1), dspo(p,p1), dspo(q,q1)");
		rules.put("ddissub2", "ddisjoint(p,q) :- dsubsomevalues(p," + dtop + ",x), dsubsomevalues(q," + dtop + ",y), dsco(x,x'), dsco(y,y'), dsubconjunctionof(x',y',z'), dsco(z'," + dbot + ")");
		rules.put("ddissub3", "ddisjoint(p,q) :- dran(p,x), dran(q,y), dsco(x,x'), dsco(y,y'), dsubconjunctionof(x',y',z'), sco(z'," + dbot + ")");
		rules.put("ddissym",  "ddisjoint(p,q) :- ddisjoint(q,p)");
		
		rules.put("davsco1",        "dav(x,p,y)     :- sco(x,z), dav(z,p,y)");
		rules.put("davspo",         "dav(x,q,y)     :- dspo(q,p), dav(x,p,y)");
		rules.put("davsco2",        "dav(x,p,y)     :- dav(x,p,z), dsco(z,y)");
		rules.put("davran",         "dav(" + top + ",p,x)   :- dran(p,x)");
		rules.put("davsvnomatmost", "dav(x,p,y)     :- dsv(x,p,y), dnominal(y), datmostone(x,p," + dtop + ")");
		rules.put("davbotrole",     "dav(" + top + ",p," + dbot + ") :- ddisjoint(p,p)");
		rules.put("dfuncbackwards-aux", "datmostoneaux(p,y,x1,y1) :- datmostone(x,p,y), nominal(x1), dnominal(y1), dsv(x1,p,y1), dsco(y1,y), sco(x1,x)");
		rules.put("dfuncbackwards", "sco(x'," + bot + ") :- sco(x',x1), sco(x',x2), datmostoneaux(p,y,x1,y1), datmostoneaux(p,y,x2,y2), dsubconjunctionof(y1,y2,z), dsco(z," + dbot + ")");
		
		rules.put("dscoavsafe",     "dsco(y,z)   :- dsv(x,p,y), nominal(x), dnominal(y), sco(x,w), dav(w,p,z)");
		rules.put("dscoatmostsafe", "dsco(w1,w2) :- nominal(x), datmostone(x,p,y), dsv(x,p,w1), dnominal(w1), dsv(x,p,w2), dnominal(w2), dsco(w1,y), dsco(w2,y)"); 
		
		rules.put("damosco1",     "datmostone(x,p,y)                     :- sco(x,z), datmostone(z,p,y)");
		rules.put("damosco2",     "datmostone(x,p,y)                     :- datmostone(x,p,z), dsco(y,z)");
		rules.put("damospo",      "datmostone(x,p,y)                     :- datmostone(x,q,y), dspo(p,q)");
		//rules.put("damonom",      "datmostone(x,p,y)                     :- dnominal(y)");    // unsafe, but only needed for output
		rules.put("damoav",       "datmostone(x,p," + dtop + ")           :- dav(x,p,y), datmostone(x,p,y)");
		rules.put("damobotrole",  "datmostone(" + top + ",p," + dtop + ") :- ddisjoint(p,p)");
		
		// now register those rules:
		Iterator<String> nameit = rules.keySet().iterator();
		String name;
		while (nameit.hasNext()) {
			name = nameit.next();
			//inferencerules.add(name);
			ruleinfo.add(new RuleInfo(name));
			storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString(name,rules.get(name)));
		}
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		long sTime=System.currentTimeMillis();
		registerInferenceRules();
		for (int i=0; i<ruleinfo.size(); i++) {
			ruleinfo.get(i).reset();
		}
		int affectedrows = 1, curstep = storage.getMaxStep();
		while (affectedrows > 0) {
			LogWriter.get().printlnDebug("============");
			Collections.sort(ruleinfo);
			affectedrows = 0;
			for (int i=0; i<ruleinfo.size(); i++) {
				affectedrows = affectedrows + runRule(ruleinfo.get(i),curstep);
				if (affectedrows != 0) i=ruleinfo.size();
			}
			curstep++;
		}
		LogWriter.get().printlnNote("Completed materialization in " + (System.currentTimeMillis() - sTime) + "ms.");
		storage.dumpStatistics();
	}
	
	protected int runRule(RuleInfo ri, int curstep) {
		int affectedrows = storage.runRule(ri.name,ri.laststep+1,curstep);
		ri.laststep = curstep;
		ri.setLastAffected(affectedrows);
		return affectedrows;
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
		if ( storage.checkPredicateAssertion("nonempty",storage.getIDForNothing()) || 
			 storage.checkPredicateAssertion("dnonempty",storage.getIDForBottomDatatype())) {
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
		if ( storage.checkPredicateAssertion("nonempty",storage.getIDForNothing()) || 
			 storage.checkPredicateAssertion("dnonempty",storage.getIDForBottomDatatype()) ) {
			return InferenceResult.NO;
		} else {
			return InferenceResult.YES;
		}
	}
	
}
