package edu.kit.aifb.orel.kbmanager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

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
	protected static final String OWL_NS="http://www.w3.org/2002/07/owl#";
    protected static final String XSD_NS="http://www.w3.org/2001/XMLSchema#";

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
		numberRanges.put(OWL_NS+"real",new NumberRange(null,null));
		numberRanges.put(OWL_NS+"rational",new NumberRange(null,null));
		numberRanges.put(XSD_NS+"decimal",new NumberRange(null,null));
		numberRanges.put(XSD_NS+"integer",new NumberRange(null,null));
		numberRanges.put(XSD_NS+"long",new NumberRange(Long.MIN_VALUE,Long.MAX_VALUE));
		numberRanges.put(XSD_NS+"int",new NumberRange(Integer.MIN_VALUE,Integer.MAX_VALUE));
		numberRanges.put(XSD_NS+"short",new NumberRange((int)Short.MIN_VALUE,(int)Short.MAX_VALUE));
		numberRanges.put(XSD_NS+"byte",new NumberRange((int)Byte.MIN_VALUE,(int)Byte.MAX_VALUE));
		numberRanges.put(XSD_NS+"nonNegativeInteger",new NumberRange(0,null));
		numberRanges.put(XSD_NS+"positiveInteger",new NumberRange(1,null));
		numberRanges.put(XSD_NS+"nonPositiveInteger",new NumberRange(null,0));
		numberRanges.put(XSD_NS+"negativeInteger",new NumberRange(null,-1));
		numberRanges.put(XSD_NS+"unsignedLong",new NumberRange(0,new BigInteger("18446744073709551615")));
		numberRanges.put(XSD_NS+"unsignedInt",new NumberRange(0,4294967295L));
		numberRanges.put(XSD_NS+"unsignedShort",new NumberRange(0,65535));
		numberRanges.put(XSD_NS+"unsignedByte",new NumberRange(0,255));
	}
	

	public static SimpleLiteral makeSimpleLiteral(OWLLiteral literal) {
		OWLDatatype type = literal.getDatatype();
		if (type == null) { // untyped literal, not supported yet
			return null;
		} else {
			String typeuri = type.getIRI().toString();
			if (numberRanges.containsKey(typeuri)) {
				typeuri = OWL_NS+"real";
			}
			System.out.println("Parsing \"" + literal.getLiteral() + "\"^^" + type.getIRI().toString());
			Object value = parseValue(literal.getLiteral(),type.getIRI().toString());
			if (value == null) {
				System.out.println("Failed.");				
				return null;
			} else {
				System.out.println("Success.");
				return new SimpleLiteral(value.toString(), value, typeuri);
			}
		}
	}
	
	protected static Object parseValue(String lexicalValue, String typeuri) {
		if (typeuri.equals(OWL_NS + "real")) {
			return null; // no lexical values for owl:real
		} else if (typeuri.equals(OWL_NS + "rational")) {
			return parseRational(lexicalValue);
		} else if (typeuri.equals(XSD_NS + "decimal")) {
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
