package edu.kit.aifb.orel.inferencing;

/**
 * A simple class for describing the signature of one (meta) predicate used
 * in inferencing. When an RDBMS backend is used, predicates correspond
 * to tables, but this is not essential on higher levels.
 * 
 * Predicates use position-based addressing of arguments for now, and
 * all fields are assumed to have the same type (internal integer ID). 
 * @author Markus Krötzsch
 */
public class PredicateDeclaration {
	protected String name;
	protected int fieldCount;
	protected boolean isInferred;
	protected boolean inMemory;
	
	public PredicateDeclaration(String name, int fields, boolean isInferred, boolean inMemory) {
		this.name = name;
		this.fieldCount = fields;
		this.isInferred = isInferred;
		this.inMemory = inMemory;
	}
	
	public String getName() {
		return name;
	}
	
	public int getFieldCount() {
		return fieldCount;
	}
	
	public boolean isInferred() {
		return isInferred;
	}
	
	public boolean isInMemory() {
		return inMemory;
	}
	
}
