package edu.kit.aifb.orel.storage;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;

/**
 * Interface for storage drivers in Orel.
 * TODO document members
 * @author Markus Krötzsch
 */
public interface StorageDriver {
	public void registerPredicate(PredicateDeclaration pd);
	
	public void initialize() throws Exception;
	
	public void drop() throws Exception;
	
	public void dumpStatistics();
	
	public void clear(boolean onlyderived) throws Exception;
	public void clear(String predicate, boolean onlyderived) throws Exception;
	
	public void commit() throws Exception;
	
	public void beginLoading();
	
	public void endLoading();
	
	public void makePredicateAssertion(String predicate, int... ids);
	public boolean checkPredicateAssertion(String predicate, int... ids);
	public int changeStep(String predicate, int oldstep, int newstep) throws Exception;
	public int getMaxStep();

	public void registerInferenceRule(InferenceRuleDeclaration rd);
	
	public int runRule(String rulename, int newstep);
	public int runRule(String rulename, int newstep, int[] params);
	public int runRule(String rulename, int min_cur_step, int max_cur_step);
	
	public int getIDForNothing();
	public int getIDForThing();
	public int getIDForTopDatatype();
	public int getIDForBottomDatatype();
	public int getID(String key);
}
