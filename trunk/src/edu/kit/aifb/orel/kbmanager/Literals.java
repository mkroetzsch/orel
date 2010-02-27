package edu.kit.aifb.orel.kbmanager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;

import edu.kit.aifb.orel.storage.SimpleLiteral;

/**
 * Static class for processing literals of supported datatypes. 
 * 
 * Some datatype handling code is based on code from the HermiT reasoner,
 * copyright 2008--2010 by the Oxford University Computing Laboratory,
 * published under the GNU Lesser General Public License v3.
 * @author Markus Krötzsch
 */
public class Literals {
	public static final String OWL_NS="http://www.w3.org/2002/07/owl#";
	public static final String XSD_NS="http://www.w3.org/2001/XMLSchema#";
	public static final String RDF_NS="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_NS="http://www.w3.org/2000/01/rdf-schema#";
	
	public static final String RDFS_Literal = RDFS_NS+"Literal";
	public static final String RDF_PlainLiteral = RDF_NS+"PlainLiteral";
	public static final String RDF_XMLLiteral = RDF_NS+"XMLLiteral";
	public static final String OWL_real = OWL_NS+"real";
	public static final String OWL_rational = OWL_NS+"rational";
	public static final String XSD_decimal = XSD_NS+"decimal";
	public static final String XSD_integer = XSD_NS+"integer";
	public static final String XSD_nonNegativeInteger = XSD_NS+"nonNegativeInteger";
	public static final String XSD_negativeInteger = XSD_NS+"negativeInteger";
	public static final String XSD_nonPositiveInteger = XSD_NS+"nonPositiveInteger";
	public static final String XSD_positiveInteger = XSD_NS+"positiveInteger";
	public static final String XSD_long = XSD_NS+"long";
	public static final String XSD_int = XSD_NS+"int";
	public static final String XSD_short = XSD_NS+"short";
	public static final String XSD_byte = XSD_NS+"byte";
	public static final String XSD_unsignedLong = XSD_NS+"unsignedLong";
	public static final String XSD_unsignedInt = XSD_NS+"unsignedInt";
	public static final String XSD_unsignedShort = XSD_NS+"unsignedShort";
	public static final String XSD_unsignedByte = XSD_NS+"unsignedByte";
	public static final String XSD_string = XSD_NS+"string";
	public static final String XSD_normalizedString = XSD_NS+"normalizedString";
	public static final String XSD_token = XSD_NS+"token";
	public static final String XSD_language = XSD_NS+"language";
	public static final String XSD_Name = XSD_NS+"Name";
	public static final String XSD_NCName = XSD_NS+"NCName";
	public static final String XSD_NMTOKEN = XSD_NS+"NMTOKEN";
	public static final String XSD_boolean = XSD_NS+"boolean";
	public static final String XSD_hexBinary = XSD_NS+"hexBinary";
	public static final String XSD_base64Binary = XSD_NS+"base64Binary";
	public static final String XSD_anyURI = XSD_NS+"anyURI";
	public static final String XSD_dateTime = XSD_NS+"dateTime";
	public static final String XSD_dateTimeStamp = XSD_NS+"dateTimeStamp";
    
    public static final String TOP_DATATYPE=RDFS_Literal;
    public static final String BOTTOM_DATATYPE="http://owl.semanticweb.org/NoLiteral";

	static class NumberRange {
		public Number glb; // greatest lower bound
		public Number lub; // least upper bound
		public NumberRange(Number greatestLowerBound, Number leastUpperBound) {
			glb = greatestLowerBound;
			lub = leastUpperBound;
		}
	}
	static protected final HashMap<String,NumberRange> numberRanges = new HashMap<String,NumberRange>();
	static {
		numberRanges.put(OWL_real,new NumberRange(null,null));
		numberRanges.put(OWL_rational,new NumberRange(null,null));
		numberRanges.put(XSD_decimal,new NumberRange(null,null));
		numberRanges.put(XSD_integer,new NumberRange(null,null));
		numberRanges.put(XSD_long,new NumberRange(Long.MIN_VALUE,Long.MAX_VALUE));
		numberRanges.put(XSD_int,new NumberRange(Integer.MIN_VALUE,Integer.MAX_VALUE));
		numberRanges.put(XSD_short,new NumberRange((int)Short.MIN_VALUE,(int)Short.MAX_VALUE));
		numberRanges.put(XSD_byte,new NumberRange((int)Byte.MIN_VALUE,(int)Byte.MAX_VALUE));
		numberRanges.put(XSD_nonNegativeInteger,new NumberRange(0,null));
		numberRanges.put(XSD_positiveInteger,new NumberRange(1,null));
		numberRanges.put(XSD_nonPositiveInteger,new NumberRange(null,0));
		numberRanges.put(XSD_negativeInteger,new NumberRange(null,-1));
		numberRanges.put(XSD_unsignedLong,new NumberRange(0,new BigInteger("18446744073709551615")));
		numberRanges.put(XSD_unsignedInt,new NumberRange(0,4294967295L));
		numberRanges.put(XSD_unsignedShort,new NumberRange(0,65535));
		numberRanges.put(XSD_unsignedByte,new NumberRange(0,255));
	}
	static protected final HashSet<String> elNumberTypes = new HashSet<String>();
	static {
		elNumberTypes.add(OWL_real);
		elNumberTypes.add(OWL_rational);
		elNumberTypes.add(XSD_decimal);
		elNumberTypes.add(XSD_integer);
		elNumberTypes.add(XSD_nonNegativeInteger);
	}
	static protected final HashSet<String> elOtherTypes = new HashSet<String>();
	static {
		elOtherTypes.add(RDF_PlainLiteral);
		elOtherTypes.add(RDF_XMLLiteral);
		elOtherTypes.add(XSD_string);
		elOtherTypes.add(XSD_normalizedString);
		elOtherTypes.add(XSD_token);
		elOtherTypes.add(XSD_Name);
		elOtherTypes.add(XSD_NCName);
		elOtherTypes.add(XSD_NMTOKEN);
		elOtherTypes.add(XSD_hexBinary);
		elOtherTypes.add(XSD_base64Binary);
		elOtherTypes.add(XSD_anyURI);
		elOtherTypes.add(XSD_dateTime);
		elOtherTypes.add(XSD_dateTimeStamp);
	}
	static protected final HashSet<String> rlNumberTypes = new HashSet<String>();
	static {
		rlNumberTypes.add(XSD_decimal);
		rlNumberTypes.add(XSD_integer);
		rlNumberTypes.add(XSD_nonNegativeInteger);
		rlNumberTypes.add(XSD_negativeInteger);
		rlNumberTypes.add(XSD_nonPositiveInteger);
		rlNumberTypes.add(XSD_positiveInteger);
		rlNumberTypes.add(XSD_long);
		rlNumberTypes.add(XSD_int);
		rlNumberTypes.add(XSD_short);
		rlNumberTypes.add(XSD_byte);
		rlNumberTypes.add(XSD_unsignedLong);
		rlNumberTypes.add(XSD_unsignedInt);
		rlNumberTypes.add(XSD_unsignedShort);
		rlNumberTypes.add(XSD_unsignedByte);
	}
	static protected final HashSet<String> rlOtherTypes = new HashSet<String>();
	static {
		rlOtherTypes.add(RDF_PlainLiteral);
		rlOtherTypes.add(RDF_XMLLiteral);
		rlOtherTypes.add(XSD_string);
		rlOtherTypes.add(XSD_normalizedString);
		rlOtherTypes.add(XSD_token);
		rlOtherTypes.add(XSD_language);
		rlOtherTypes.add(XSD_Name);
		rlOtherTypes.add(XSD_NCName);
		rlOtherTypes.add(XSD_NMTOKEN);
		rlOtherTypes.add(XSD_boolean);
		rlOtherTypes.add(XSD_hexBinary);
		rlOtherTypes.add(XSD_base64Binary);
		rlOtherTypes.add(XSD_anyURI);
		rlOtherTypes.add(XSD_dateTime);
		rlOtherTypes.add(XSD_dateTimeStamp);
	}

	public static SimpleLiteral makeSimpleLiteral(OWLLiteral literal) {
		OWLDatatype type = literal.getDatatype();
		if (type == null) { // untyped literal, not supported yet
			if (literal.getLang() == null) {
				return new SimpleLiteral(literal.getLiteral(), literal.getLiteral(), null);
			} else {
				String value = literal.getLiteral() + '@' + literal.getLang();
				return new SimpleLiteral(value, value, null);
			}
		} else {
			return makeSimpleLiteral(literal.getLiteral(),type.getIRI().toString());
		}
	}
	
	public static SimpleLiteral makeSimpleLiteral(String lexicalValue, String datatype) {
		if ( (datatype==null) || datatype.equals("") ) {
			throw new IllegalArgumentException("Method can only be used for typed literals.");
		}
		Object value = parseValue(lexicalValue,datatype);
		if (value == null) {
			return null;
		} else {
			if (numberRanges.containsKey(datatype)) {
				datatype = OWL_NS+"real";
			}
			return new SimpleLiteral(value.toString(),value,datatype);
		}
	}
	
	public static List<String> getDatatypeURIs(SimpleLiteral sl) {
		ArrayList<String> result = new ArrayList<String>();
		result.add(RDFS_Literal);
		if (sl.getDatatypeURI() == null) {
			return result;
		} if (sl.getDatatypeURI().equals(OWL_real)) {
			// TODO: case for rational numbers needed too
			if (sl.getValue() instanceof BigDecimal) {
				result.add(OWL_real);
				result.add(OWL_rational);
				result.add(XSD_decimal);
			} else {
				Iterator<String> rangeit = numberRanges.keySet().iterator();
				Number value = (Number)sl.getValue();
				while (rangeit.hasNext()) {
					String typeuri = rangeit.next();
					if ( ( (numberRanges.get(typeuri).glb == null) || 
						       (compare(numberRanges.get(typeuri).glb,value) <= 0) ) &&
						     ( (numberRanges.get(typeuri).lub == null) ||
								       (compare(numberRanges.get(typeuri).lub,value) >= 0) ) ) {
						result.add(typeuri);
					}
				}
			}
		} else {
			result.add(sl.getDatatypeURI());
		}
		return result;
	}
	
	public static List<String> getELDatatypeURIs() {
		List<String> result = new ArrayList<String>();
		result.addAll(elNumberTypes);
		result.addAll(elOtherTypes);
		result.add(RDFS_Literal);
		return result;
	}

	public static List<String> getRLDatatypeURIs() {
		List<String> result = new ArrayList<String>();
		result.addAll(rlNumberTypes);
		result.addAll(rlOtherTypes);
		result.add(RDFS_Literal);
		return result;
	}

	public static List<String> getNumericDatatypeURIs() {
		HashSet<String> result = new HashSet<String>(); // use a set for avoiding duplicates
		result.addAll(elNumberTypes);
		result.addAll(rlNumberTypes);
		return new ArrayList<String>(result);
	}
	
	public static List<String> getOtherDatatypeURIs() {
		return new ArrayList<String>(rlOtherTypes);
	}
	
	public static boolean isELRLType(String datatypeuri) {
		return ( datatypeuri.equals(RDFS_Literal) || elNumberTypes.contains(datatypeuri) || rlNumberTypes.contains(datatypeuri)  || rlOtherTypes.contains(datatypeuri) );
	}
	
	protected static Object parseValue(String lexicalValue, String typeuri) {
		if (typeuri.equals(OWL_real)) {
			return null; // no lexical values for owl:real
		} else if (typeuri.equals(OWL_rational)) {
			return parseRational(lexicalValue);
		} else if (typeuri.equals(XSD_decimal)) {
			return parseDecimal(lexicalValue);
		} else if (numberRanges.containsKey(typeuri)) { // integer number
			Number result = parseInteger(lexicalValue);
			if ( ( (numberRanges.get(typeuri).glb != null) && 
			       (compare(numberRanges.get(typeuri).glb,result) == 1) ) ||
			     ( (numberRanges.get(typeuri).lub != null) && 
					       (compare(numberRanges.get(typeuri).lub,result) == -1) ) ) {
				return null; // out of the datatype's range
			} else {
				return result;
			}
		} else { // TODO: checks for other types needed as well
			return lexicalValue;
		}
	}
	
	protected static Number parseRational(String lexicalValue) {
		int divideIndex = lexicalValue.indexOf('/');
		if (divideIndex == -1) return null; // string does not contain /.
		BigInteger numerator = new BigInteger(lexicalValue.substring(0,divideIndex));
		BigInteger denominator = new BigInteger(lexicalValue.substring(divideIndex+1));
		if (denominator.compareTo(BigInteger.ZERO) <= 0) return null; // invalid denominator
		BigInteger gcd=numerator.gcd(denominator);
		numerator=numerator.divide(gcd);
		denominator=denominator.divide(gcd);
		if (denominator.equals(BigInteger.ONE)) {
			int numeratorBitCount=numerator.bitCount();
			if (numeratorBitCount<=32) {
				return numerator.intValue();
			} else if (numeratorBitCount<=64) {
				return numerator.longValue();
			} else {
				return numerator;
			}
		} else {
			try {
				return new BigDecimal(numerator).divide(new BigDecimal(denominator));
			}
			catch (ArithmeticException e) { // TODO: use proper rational type
				//return numerator.toString() + "/" + denominator.toString();
				return null;
			}
		}
	}
	
	protected static Number parseDecimal(String lexicalValue) {
		BigDecimal decimal = new BigDecimal(lexicalValue);
        try {
            return decimal.intValueExact();
        }
        catch (ArithmeticException e) {
        }
        try {
            return decimal.longValueExact();
        }
        catch (ArithmeticException e) {
        }
        try {
            return decimal.toBigIntegerExact();
        }
        catch (ArithmeticException e) {
        }
        return decimal.stripTrailingZeros();
	}
	
	protected static Number parseInteger(String lexicalValue) {
        try {
            return Integer.parseInt(lexicalValue);
        }
        catch (NumberFormatException e) {
        }
        try {
            return Long.parseLong(lexicalValue);
        }
        catch (NumberFormatException e) {
        }
        try {
        	return new BigInteger(lexicalValue);
        } catch (NumberFormatException e) {
        	return null;
        } 
	}
	
	protected static int compare(Number n1, Number n2) {
		if (n1.equals(n2)) {
			return 0;
		} else if ( (n1 instanceof BigInteger) || (n2 instanceof BigInteger) ) {
			BigInteger big1, big2;
			if (n1 instanceof BigInteger) {
				big1 = (BigInteger)n1;
			} else {
				big1 = BigInteger.valueOf(n1.longValue());
			}
			if (n2 instanceof BigInteger) {
				big2 = (BigInteger)n2;
			} else {
				big2 = BigInteger.valueOf(n2.longValue());
			}
			return big1.compareTo(big2);
		} else {
			long l1 = n1.longValue();
            long l2 = n2.longValue();
            return l1<l2 ? -1 : (l1==l2 ? 0 : 1);
		}
	}
	
}
