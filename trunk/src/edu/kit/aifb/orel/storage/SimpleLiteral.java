package edu.kit.aifb.orel.storage;

/**
 * The simplest possible literal container. It is assumed that this literal
 * is always used with a normalized (canonical) lexical value. Typically, the
 * given datatype is the most specific one that the literal belongs to, i.e.
 * the intersection of all datatypes it belongs to. Since not all intersections
 * of relevant XML Schema types have XSD URIs, some datatype URIs used here are 
 * specific to Orel.
 * @author Markus Krötzsch
 */
public class SimpleLiteral {
	String lexicalValue;
	String datatypeURI;
	
	public SimpleLiteral(String canonicalValue, String datatypeURI) {
		this.lexicalValue = canonicalValue;
		this.datatypeURI = datatypeURI;
	}
	
	public String getDatatypeURI() {
		return datatypeURI;
	}
	
	public String getLexicalValue() {
		return lexicalValue;
	}
	
	/**
	 * Create a string representation for this value that resembles the representation
	 * used in Turtle. However, we do not care about escaping issues here.
	 */
	public String toString() {
		return '"' + lexicalValue + '"' + "^^" + datatypeURI; 
	}

}
