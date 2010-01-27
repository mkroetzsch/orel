package edu.kit.aifb.orel.storage;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

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
	
	public void clear(boolean onlyderived) throws Exception;
	public void clear(String predicate, boolean onlyderived) throws Exception;
	
	
	public void beginLoading();
	
	public void endLoading();
	
	public void insertIdsToTable(String tablename, int id1) throws Exception;
	public void insertIdsToTable(String tablename, int id1, int id2) throws Exception;
	public void insertIdsToTable(String tablename, int id1, int id2, int id3) throws Exception;
	
	public boolean checkIdsInTable(String tablename, int id1, int id2) throws Exception;
	public boolean checkIdsInTable(String tablename, int id1, int id2, int id3) throws Exception;
	public int changeStep(String predicate, int oldstep, int newstep) throws Exception;

	public void registerInferenceRule(InferenceRuleDeclaration rd);
	
	public int runRule(String rulename, int newstep);
	public int runRule(String rulename, int newstep, int[] params);
	public int runRule(String rulename, int min_cur_step, int max_cur_step);
	
	public int getID(OWLClassExpression description) throws Exception;
	public int getID(OWLIndividual individual) throws Exception;
	public int getID(OWLObjectPropertyExpression property) throws Exception;
	public int getID(String description) throws Exception;
}