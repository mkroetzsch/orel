package edu.kit.aifb.orel.kbmanager.elhmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLOntology;


import edu.kit.aifb.orel.client.LogWriter;
import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.kbmanager.Literals;
import edu.kit.aifb.orel.kbmanager.KBManager.InferenceResult;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * An optimized Reasoner for ELH
 *   
 * @author Anees ul Mehdi
 */
public class ELKBReasoner {
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
	
	public ELKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}
	
	protected void registerCheckRules() {
		HashMap<String,String> rules = new HashMap<String,String>();
	
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
	
		// NOTE: spo(p,p) is created at load time
		rules.put("spo",            "spo(p,r) :- spo(p,q), spo(q,r)");
	
		rules.put("sco",         "sco(x,z) :- sco(x,y), sco(y,z)"); 
		rules.put("con",         "sco(x,z) :- sco(x,y1), sco(x,y2), subconjunctionof(y1,y2,z)");
		rules.put("svr",         "sco(x,z) :- sv(x,p,y), sco(y,y'), subsomevalues(p,y',z)");

		rules.put("svp",          "sv(x,q,y) :- sv(x,p,y), spo(p,q)");
		rules.put("svspoc",       "sv(x,r,z) :- spoc(p,q,r), sv(x,p,x'), sco(x',z'), sv(z',q,z)");
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
		ELKBLoader loader = new ELKBLoader(storage);
		boolean loaded = loader.processOntology(ontology, ELKBLoader.PREPARECHECK );
		materialize();
		registerCheckRules();
		// inconsistent ontologies entail anything
		if ( storage.checkPredicateAssertion("nonempty",storage.getID(ELExpressionVisitor.OP_NOTHING)) || 
			 storage.checkPredicateAssertion("dnonempty",storage.getID(Literals.BOTTOM_DATATYPE))) {
			return InferenceResult.YES;
		} else if (!loaded) { // if checked axioms failed to load, neither YES nor NO are sure answers
			return InferenceResult.DONTKNOW;
		} else if (loader.processOntology(ontology, ELKBLoader.CHECK )) { // some supported axioms not entailed
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
		if ( storage.checkPredicateAssertion("nonempty",storage.getID(ELExpressionVisitor.OP_NOTHING)) || 
			 storage.checkPredicateAssertion("dnonempty",storage.getID(Literals.BOTTOM_DATATYPE)) ) {
			return InferenceResult.NO;
		} else {
			return InferenceResult.YES;
		}
	}
	
}
