package edu.kit.aifb.orel.kbmanager.instancemanager;

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
 * A simple reasoning implementation based on a naive saturation of the
 * knowledge based for a given set of inference rules.  
 * @author Markus Krötzsch
 */
public class InstanceKBReasoner {
	protected StorageDriver storage;
	//protected ArrayList<String> inferencerules = null;
	protected ArrayList<RuleInfo> ruleinfo = null;
	protected boolean hasCheckRules = false; 
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
	
	public InstanceKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}
	
	protected void registerCheckRules() {
		if (hasCheckRules) return;
		hasCheckRules = true;
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error-free and customizable
		
		// Recall that these rules are not recursively applied!
		// Many of these rules won't be used for querying, but we still 
		// use them to document the incompleteness in materializing derived
		// predicates.
		int bot = storage.getID(InstanceExpressionVisitor.OP_NOTHING);
		rules.put("inst-lbot",  "?inst(x,A) :- inst(x," + bot + ")");
		rules.put("dinst-lbot", "?dinst(x,A) :- inst(x," + bot + ")");
		rules.put("self-lbot",  "?self(x,R) :- inst(x," + bot + ")");
		rules.put("triple-lbot", "?triple(x,R,y) :- inst(x," + bot + ")");
		
		rules.put("inst-gbot",   "?inst(x,A) :- real(z), inst(z," + bot + ")");
		rules.put("dinst-gbot", "?dinst(x,A) :- real(z), inst(z," + bot + ")");
		rules.put("self-gbot",   "?self(x,R) :- real(z), inst(z," + bot + ")");
		rules.put("triple-gbot", "?triple(x,R,y) :- real(z), inst(z," + bot + ")");
		
		rules.put("inst-lbotd", "?inst(x,A) :- inst(x," + bot + ")");
		rules.put("self-lbotd", "?self(x,R) :- inst(x," + bot + ")");
		rules.put("triple-lbotd", "?triple(x,R,y) :- inst(x," + bot + ")");
		
		rules.put("inst-gbotd", "?inst(x,A) :- real(z), inst(z," + bot + ")");
		rules.put("self-gbotd", "?self(x,R) :- real(z), inst(z," + bot + ")");
		rules.put("triple-gbotd", "?triple(x,R,y) :- real(z), inst(z," + bot + ")");
		
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
		ruleinfo = new ArrayList<RuleInfo>();
		HashMap<String,String> rules = new HashMap<String,String>();
//		int top = storage.getIDForThing();
		int bot = storage.getID(InstanceExpressionVisitor.OP_NOTHING);
		int botprop = storage.getID(InstanceExpressionVisitor.OP_BOTTOM_OBJECT_PROPERTY);
//		int dtop = storage.getIDForTopDatatype();
		int dbot = storage.getID(Literals.BOTTOM_DATATYPE);
		int dbotprop = storage.getID(InstanceExpressionVisitor.OP_BOTTOM_DATA_PROPERTY);
		// make the rule declaration as readable as possible;
		// it is crucial to have this error-free and customizable
		
		// EL rules:
		rules.put("(1)", "self(x,R) :- triple(x,R,x), name(x)");
		rules.put("(2)", "inst(x,C) :- subc(A,C), inst(x,A)");
		rules.put("(3)", "inst(x,C) :- subcon(A,B,C), inst(x,A), inst(x,B)");
		rules.put("(4)", "inst(x,C) :- subsome(R,A,C), triple(x,R,y), inst(y,A)");
		rules.put("(5)", "triple(x,R,dRB) :- supsome(A,R,dRB), inst(x,A)");
		rules.put("(6')", "real(dRB) :- supsome(A,R,dRB), inst(x,A), real(x)");
			//rules.put("(6'')", "{A ⊑ ∃R.B} ∧ inst(x,A) ∧ leadsto(w,x) → leadsto(w,dRB)");
		rules.put("(7)", "inst(x,C) :- subself(R,C), self(x,R)");
		rules.put("(8)", "self(x,R) :- supself(A,R), inst(x,A)");
		rules.put("(9)", "triple(x,R,x) :- supself(A,R), inst(x,A)");
		rules.put("(10)", "triple(x,T,y) :- subp(R,T), triple(x,R,y)");
		rules.put("(11)", "self(x,T) :- subp(R,T), self(x,R)");
		rules.put("(12)", "triple(x,T,z) :- subchain(R,S,T), triple(x,R,y), triple(y,S,z)");
		rules.put("(13disj)", "inst(x," + bot + ") :- pdisjoint(R,S), triple(x,R,y), triple(x,S,y)");
		rules.put("(14disj)", "inst(x," + bot + ") :- pdisjoint(R,S), self(x,R), self(x,S)");
		// Equality theory:
		rules.put("(20)",   "inst(y,x) :- inst(x,y), name(y), real(x)");
		rules.put("(22'')", "name(x) :- inst(x,y), name(y)");
		rules.put("(24)",   "triple(x1,R,x2) :- triple(x1,R,y), inst(x2,y), name(y)");
		// RL rules:
		rules.put("(27)", "inst(y,B) :- supall(A,R,B), real(x), name(x), name(y), triple(x,R,y), inst(x,A)");
		rules.put("(28)", "inst(y1,y2) :- supfunc(A,R,B), real(x), name(x), name(y1), name(y2), inst(x,A), triple(x,R,y1), triple(x,R,y2), inst(y1,B), inst(y2,B), orel:distinct(y1,y2)");
		rules.put("(29)", "triple(y,S,x) :- subinv(R,S), real(x), name(x), name(y), triple(x,R,y)");
		// Support for top and bottom: (largely implemented via check rules)
		rules.put("(B2)", "inst(x," + bot + ") :- triple(x," + botprop + ",y)");
		
		// EL rules (datatype):
		rules.put("(2d)", "dinst(x,C) :- dsubc(A,C), dinst(x,A)");
		rules.put("(3d)", "dinst(x,C) :- dsubcon(A,B,C), dinst(x,A), dinst(x,B)");
		rules.put("(4d)", "inst(x,C) :- dsubsome(R,A,C), dtriple(x,R,y), dinst(y,A)");
		rules.put("(5d)", "dtriple(x,R,dRB) :- dsupsome(A,R,dRB), inst(x,A)");
		rules.put("(6'd)", "dreal(dRB) :- dsupsome(A,R,dRB), inst(x,A), real(x)");
			//rules.put("(6'')", "{A ⊑ ∃R.B} ∧ inst(x,A) ∧ leadsto(w,x) → leadsto(w,dRB)");
		rules.put("(10d)", "dtriple(x,T,y) :- dsubp(R,T), dtriple(x,R,y)");
		rules.put("(13ddisj)", "inst(x," + bot + ") :- dpdisjoint(R,S), dtriple(x,R,y), dtriple(x,S,y)");
		// Equality theory (datatype):
		rules.put("(20d)",   "dinst(y,x) :- dinst(x,y), dname(y), dreal(x)");
		rules.put("(22d'')", "dname(x) :- dinst(x,y), dname(y)");
		rules.put("(24d)",   "dtriple(x1,R,x2) :- dtriple(x1,R,y), dinst(x2,y), dname(y)");
		// RL rules (datatype):
		rules.put("(27d)", "dinst(y,B) :- dsupall(A,R,B), name(x), dname(y), dtriple(x,R,y), inst(x,A)");
		rules.put("(28d)", "dinst(y1,y2) :- dsupfunc(A,R,B), name(x), dname(y1), dname(y2), inst(x,A), dtriple(x,R,y1), dtriple(x,R,y2), dinst(y1,B), dinst(y2,B)");
		// Support for top and bottom (datatype): (largely implemented via check rules)
		rules.put("(B2d)", "dinst(x," + dbot + ") :- dtriple(x," + dbotprop + ",y)");
		
		// Rules for handling inferences that require assumptions of non-emptiness of some class:
		rules.put("(20G)", "rampant(x) :- inst(x,y), name(y), unreal(x)");
		rules.put("(27G)", "rampant(x) :- supall(A,R,B), unreal(x), name(x), name(y), triple(x,R,y), inst(x,A)");
		rules.put("(28G)", "rampant(x) :- supfunc(A,R,B), unreal(x), name(x), name(y1), name(y2), inst(x,A), triple(x,R,y1), triple(x,R,y2), inst(y1,B), inst(y2,B), orel:distinct(y1,y2)");
		rules.put("(29G)", "rampant(x) :- subinv(R,S), unreal(x), name(y), triple(x,R,y)");
		
		rules.put("(R1)", "rampant(x) :- rampant(y), unreal(x), inst(x,y)"); // ?
		rules.put("(R2)", "rampant(x) :- rampant(y), unreal(x), triple(x,R,y)");
		
		rules.put("(R3)", "winst(w,w,w) :- rampant(w)");
		rules.put("(R4)", "wname(w,w) :- rampant(w), name(w)"); // not needed if g is restricted to test individuals for which this must be inferrable anyway
		rules.put("(R5)", "winst(x,y,w) :- rampant(w), real(x), inst(x,y)"); // could be restricted to: g(w) ∧ real(x) → winst(x,x,w)
		rules.put("(R6)", "wname(x,w) :- rampant(w), real(x), name(x)");
		
		// EL rules with context:
		rules.put("(1W)", "wself(x,R,w) :- wtriple(x,R,x,w), wname(x,w)");
		rules.put("(2W)", "winst(x,C,w) :- subc(A,C), winst(x,A,w)");
		rules.put("(3W)", "winst(x,C,w) :- subcon(A,B,C), winst(x,A,w), winst(x,B,w)");
		rules.put("(4W)", "winst(x,C,w) :- subsome(R,A,C), wtriple(x,R,y,w), winst(y,A,w)");
		rules.put("(5W)", "wtriple(x,R,dRB,w) :- supsome(A,R,dRB), winst(x,A,w)");
		rules.put("(7W)", "winst(x,C,w) :- subself(R,C), wself(x,R,w)");
		rules.put("(8W)", "wself(x,R,w) :- supself(A,R), winst(x,A,w)");
		rules.put("(9W)", "wtriple(x,R,x,w) :- supself(A,R), winst(x,A,w)");
		rules.put("(10W)", "wtriple(x,T,y,w) :- subp(R,T), wtriple(x,R,y,w)");
		rules.put("(11W)", "wself(x,T,w) :- subp(R,T), wself(x,R,w)");
		rules.put("(12W)", "wtriple(x,T,z,w) :- subchain(R,S,T), wtriple(x,R,y,w), wtriple(y,S,z,w)");
		rules.put("(13Wdisj)", "winst(x," + bot + ",w) :- pdisjoint(R,S), wtriple(x,R,y,w), wtriple(x,S,y,w)");
		rules.put("(14Wdisj)", "winst(x," + bot + ",w) :- pdisjoint(R,S), wself(x,R,w), wself(x,S,w)");
		// Equality theory:
		rules.put("(20W)",   "winst(y,x,w) :- winst(x,y,w), wname(y,w)");
		rules.put("(22W'')", "wname(x,w) :- winst(x,y,w), wname(y,w)");
		rules.put("(24W)",   "wtriple(x1,R,x2,w) :- wtriple(x1,R,y,w), winst(x2,y,w), wname(y,w)");
		// RL rules:
		rules.put("(27W)", "winst(y,B,w) :- supall(A,R,B), wname(x,w), wname(y,w), wtriple(x,R,y,w), winst(x,A,w)");
		rules.put("(28W)", "winst(y1,y2,w) :- supfunc(A,R,B), wname(x,w), wname(y1,w), wname(y2,w), winst(x,A,w), wtriple(x,R,y1,w), wtriple(x,R,y2,w), winst(y1,B,w), winst(y2,B,w), orel:distinct(y1,y2)");
		rules.put("(29W)", "wtriple(y,S,x,w) :- subinv(R,S), wname(x,w), wname(y,w), wtriple(x,R,y,w)");
		// Support for top and bottom: (largely implemented via check rules)
		rules.put("(B2W)", "winst(x," + bot + ",w) :- wtriple(x," + botprop + ",y,w)");
		
		// TODO Code remaining rules for establishing completeness ...
		

		
		// now register those rules:
		Iterator<String> nameit = rules.keySet().iterator();
		String name;
		while (nameit.hasNext()) {
			name = nameit.next();
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
		InstanceKBLoader loader = new InstanceKBLoader(storage);
		boolean loaded = loader.processOntology(ontology, InstanceKBLoader.PREPARECHECK );
		materialize();
		registerCheckRules();
		// inconsistent ontologies entail everything
		if ( storage.checkPredicateAssertion("inst", storage.getID(InstanceExpressionVisitor.OP_THING), storage.getID(InstanceExpressionVisitor.OP_NOTHING)) ) {
			return InferenceResult.YES;
		} else if (!loaded) { // if checked axioms failed to load, neither YES nor NO are sure answers
			return InferenceResult.DONTKNOW;
		} else if (loader.processOntology(ontology, InstanceKBLoader.CHECK )) { // all axioms entailed
			return InferenceResult.YES;
		} else { // some supported axioms not entailed
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
		if ( storage.checkPredicateAssertion("inst", storage.getID(InstanceExpressionVisitor.OP_THING), storage.getID(InstanceExpressionVisitor.OP_NOTHING)) ) {
			return InferenceResult.NO;
		} else {
			return InferenceResult.YES;
		}
	}
	
}
