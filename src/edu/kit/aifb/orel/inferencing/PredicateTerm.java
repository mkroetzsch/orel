package edu.kit.aifb.orel.inferencing;

/**
 * Represent a term, i.e. a constant or variable, that is used in
 * a predicate atom in a rule of inference.
 * @author Markus Krötzsch
 */
public class PredicateTerm {
	protected String value;
	protected boolean isVariable;

	public PredicateTerm(String value, boolean isVariable) {
		this.value = value;
		this.isVariable = isVariable;
	}
	
	public String getValue() {
		return value;
	}
	
	public boolean isVariable() {
		return isVariable;
	}
}
