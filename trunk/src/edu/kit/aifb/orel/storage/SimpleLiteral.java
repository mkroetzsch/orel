package edu.kit.aifb.orel.storage;

/**
 * The simplest possible literal container. It is assumed that this literal
 * is always used with a normalized (canonical) lexical value. Typically, the
 * given datatype is the most general one that the literal belongs to, e.g. it.
 * is owl_real for any numerical type. The valueString is not the lexical value
 * but rather a canonical string version of the semantic value. For example, it 
 * the same string "1" would be used for "1"^^xsd:int and "2/2"^^owl:rational,
 * even though "1" is not in the lexical space of owl:rational.
 * @author Markus Krötzsch
 */
public class SimpleLiteral {
	String valueString;
	String datatypeURI;
	Object value;
	
	public SimpleLiteral(String valueString, Object value, String datatypeURI) {
		this.valueString = valueString;
		this.value = value;
		this.datatypeURI = datatypeURI;
	}
	
	public String getDatatypeURI() {
		return datatypeURI;
	}
	
	public String getValueString() {
		return valueString;
	}
	
	public Object getValue() {
		return value;
	}
	
	/**
	 * Create a string representation for this value that resembles the representation
	 * used in Turtle. However, we do not care about escaping issues here. Since we
	 * use the value string to uniquely identify the (semantic) value, it may not even
	 * be a valid lexical value for the datatype.
	 */
	public String toString() {
		return '"' + valueString + '"' + "^^" + datatypeURI; 
	}

}
