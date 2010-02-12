package edu.kit.aifb.orel.storage;

import java.util.List;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;

/**
 * Interface for storage drivers in Orel.
 * TODO document members
 * @author Markus Krötzsch
 */
public interface StorageDriver {
	final public static String OP_NOTHING = "owl:Nothing";
	final public static String OP_THING = "owl:Thing";
	final public static String OP_OBJECT_INTERSECTION = "ObjectIntersectionOf";
	final public static String OP_OBJECT_UNION = "ObjectUnionOf";
	final public static String OP_OBJECT_ONE_OF = "ObjectOneOf";

	public void registerPredicate(PredicateDeclaration pd);
	
	public void initialize() throws Exception;
	
	public void drop() throws Exception;
	
	public void dumpStatistics();
	
	public void clear(boolean onlyderived) throws Exception;
	public void clear(String predicate, boolean onlyderived) throws Exception;
	
	public void commit() throws Exception;
	
	
	public void beginLoading();
	
	public void endLoading();
	
	public void makePredicateAssertion(String predicate, int... ids) throws Exception;
	public boolean checkPredicateAssertion(String predicate, int... ids) throws Exception;
	public int changeStep(String predicate, int oldstep, int newstep) throws Exception;

	public void registerInferenceRule(InferenceRuleDeclaration rd);
	
	public int runRule(String rulename, int newstep);
	public int runRule(String rulename, int newstep, int[] params);
	public int runRule(String rulename, int min_cur_step, int max_cur_step);
	
	public int getIDForNaryExpression(String opname, List<? extends OWLObject> operands);
	public int getIDForNothing();
	public int getIDForThing();
	public int getIDForDatatypeURI(String uri);
	public int getID(OWLClassExpression description);
	public int getID(OWLIndividual individual);
	public int getID(SimpleLiteral literal);
	public int getID(OWLPropertyExpression<?,?> property);
}
