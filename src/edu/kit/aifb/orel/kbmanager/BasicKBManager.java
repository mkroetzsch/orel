package edu.kit.aifb.orel.kbmanager;

import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.storage.SimpleLiteral;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * Main class for maintaining a storage that uses the basic OWL EL/RL
 * predicate signature.
 * 
 * @author Markus Krötzsch
 */
public class BasicKBManager {
	static public enum InferenceResult {
	    YES, NO, DONTKNOW 
	}
	protected StorageDriver storage;
	
	/**
	 * Constructor that also establishes a database connection, since this object cannot really work without a database.
	 * @param dbserver
	 * @param dbname
	 * @param dbuser
	 * @param dbpwd
	 * @throws Exception
	 */
	public BasicKBManager(StorageDriver storage) throws Exception {
		this.storage = storage;
		registerPredicates();
	}

	/**
	 * Ensure that the DB has the right tables, creating them if necessary.
	 */
	public void initialize() throws Exception {
		storage.initialize();
		// the fundamental truths of DL reasoning: 
		int thing = storage.getIDForThing(), nothing = storage.getIDForNothing(); 
		storage.makePredicateAssertion("sco",thing,thing);
		storage.makePredicateAssertion("sco",nothing,nothing);
		storage.makePredicateAssertion("sco",nothing,thing);
		//storage.makePredicateAssertion("subconjunctionof",nothing,thing,nothing);
		storage.makePredicateAssertion("subconjunctionof",thing,nothing,nothing);
		storage.makePredicateAssertion("nonempty",thing);
		
	    int toptype = storage.getIDForTopDatatype(), bottype = storage.getIDForBottomDatatype();
		storage.makePredicateAssertion("dsco",toptype,toptype);
		storage.makePredicateAssertion("dsco",bottype,bottype);
		storage.makePredicateAssertion("dsco",bottype,toptype);
		//storage.makePredicateAssertion("dsubconjunctionof",bottype,toptype,bottype);
		storage.makePredicateAssertion("dsubconjunctionof",toptype,bottype,bottype);
		storage.makePredicateAssertion("dnonempty",toptype);
		int owlReal = storage.getID(Literals.OWL_real);
		int owlRational = storage.getID(Literals.OWL_rational);
		int xsdDecimal = storage.getID(Literals.XSD_decimal);
		int xsdInteger = storage.getID(Literals.XSD_integer);
		int xsdNonNegativeInteger = storage.getID(Literals.XSD_nonNegativeInteger);
		int xsdNegativeInteger = storage.getID(Literals.XSD_negativeInteger);
		int xsdNonPositiveInteger = storage.getID(Literals.XSD_nonPositiveInteger);
		int xsdPositiveInteger = storage.getID(Literals.XSD_positiveInteger);
		int xsdLong = storage.getID(Literals.XSD_long);
		int xsdInt = storage.getID(Literals.XSD_int);
		int xsdShort = storage.getID(Literals.XSD_short);
		int xsdByte = storage.getID(Literals.XSD_byte);
		int xsdUnsignedLong = storage.getID(Literals.XSD_unsignedLong);
		int xsdUnsignedInt = storage.getID(Literals.XSD_unsignedInt);
		int xsdUnsignedShort = storage.getID(Literals.XSD_unsignedShort);
		int xsdUnsignedByte = storage.getID(Literals.XSD_unsignedByte);
		SimpleLiteral zero = Literals.makeSimpleLiteral("0",Literals.XSD_byte);
		String zerokey = BasicExpressionVisitor.visitSimpleLiteral(zero, storage, BasicExpressionVisitor.Action.WRITEHEAD);
		int zeroid = storage.getID(zerokey);
		
		storage.makePredicateAssertion("dsco",owlRational,owlReal);
		storage.makePredicateAssertion("dsco",xsdDecimal,owlRational);
		storage.makePredicateAssertion("dsco",xsdInteger,xsdDecimal);
		storage.makePredicateAssertion("dsco",xsdNonNegativeInteger,xsdInteger);
		storage.makePredicateAssertion("dsco",xsdPositiveInteger,xsdNonNegativeInteger);
		storage.makePredicateAssertion("dsco",xsdNonPositiveInteger,xsdInteger);
		storage.makePredicateAssertion("dsco",xsdNegativeInteger,xsdNonPositiveInteger);
		storage.makePredicateAssertion("dsco",xsdLong,xsdInteger);
		storage.makePredicateAssertion("dsco",xsdInt,xsdLong);
		storage.makePredicateAssertion("dsco",xsdShort,xsdInt);
		storage.makePredicateAssertion("dsco",xsdByte,xsdShort);
		storage.makePredicateAssertion("dsco",xsdUnsignedLong,xsdNonNegativeInteger);
		storage.makePredicateAssertion("dsco",xsdUnsignedInt,xsdUnsignedLong);
		storage.makePredicateAssertion("dsco",xsdUnsignedShort,xsdUnsignedInt);
		storage.makePredicateAssertion("dsco",xsdUnsignedByte,xsdUnsignedShort);
		storage.makePredicateAssertion("dsubconjunctionof",xsdLong,xsdNonNegativeInteger,xsdUnsignedLong);
		storage.makePredicateAssertion("dsubconjunctionof",xsdInt,xsdUnsignedLong,xsdUnsignedInt);
		storage.makePredicateAssertion("dsubconjunctionof",xsdShort,xsdUnsignedInt,xsdUnsignedShort);
		storage.makePredicateAssertion("dsubconjunctionof",xsdByte,xsdUnsignedShort,xsdUnsignedByte);
		storage.makePredicateAssertion("dsubconjunctionof",xsdNonNegativeInteger,xsdNegativeInteger,bottype);
		storage.makePredicateAssertion("dsubconjunctionof",xsdNonPositiveInteger,xsdPositiveInteger,bottype);
		storage.makePredicateAssertion("dsubconjunctionof",xsdNonPositiveInteger,xsdNonNegativeInteger,zeroid);
		
		List<String> eltypes = Literals.getELDatatypeURIs();
		for (int i=0; i<eltypes.size(); i++) {
			storage.makePredicateAssertion("eltype",storage.getID(eltypes.get(i)));
		}
		List<String> numerictypes = Literals.getNumericDatatypeURIs(), othertypes = Literals.getOtherDatatypeURIs();
		for (int i=0; i<numerictypes.size(); i++) {
			int ntypeid = storage.getID(numerictypes.get(i));
			BasicExpressionVisitor.createDatarangeTautologies(ntypeid,storage);
			storage.makePredicateAssertion("dnonempty",ntypeid);
			for (int j=0; j<othertypes.size(); j++) {
				int otypeid = storage.getID(othertypes.get(j));
				storage.makePredicateAssertion("dsubconjunctionof",ntypeid,otypeid,bottype);
			}
		}
		for (int i=0; i<othertypes.size(); i++) {
			int otypeid1 = storage.getID(othertypes.get(i));
			BasicExpressionVisitor.createDatarangeTautologies(otypeid1,storage);
			storage.makePredicateAssertion("dnonempty",otypeid1);
			for (int j=i+1; j<othertypes.size(); j++) {
				int otypeid2 = storage.getID(othertypes.get(j));
				storage.makePredicateAssertion("dsubconjunctionof",otypeid1,otypeid2,bottype);
			}
		}
		storage.commit();
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws Exception {
		storage.drop();
	}
	
	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clearDatabase(boolean onlyderived) throws Exception {
		if (onlyderived) {
			storage.clear(onlyderived);
		} else { // faster
			drop();
			initialize();
		}
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	public boolean loadOntology(OWLOntology ontology) throws Exception {
		boolean result = true;
		BasicKBLoader loader = new BasicKBLoader(storage);
		// TODO Do we need to guard against input loops (cyclic imports)?
		Iterator<OWLOntology> ontit = ontology.getDirectImports().iterator();
		while (ontit.hasNext()) {
			result = loadOntology(ontit.next()) && result;
		}
		return loader.processOntology(ontology, (BasicKBLoader.PREPAREASSERT | BasicKBLoader.ASSERT) ) && result;
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	public InferenceResult checkEntailment(OWLOntology ontology) throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		return reasoner.checkEntailment(ontology);
	}
	
	/**
	 * Check if the loaded axioms are consistent.
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 */
	public InferenceResult checkConsistency() throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		return reasoner.checkConsistency();
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		NaiveKBReasoner reasoner = new NaiveKBReasoner(storage);
		reasoner.materialize();
	}

	protected void registerPredicates() {
		storage.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsco",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dsv",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("av",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dav",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("atmostone",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("datmostone",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("atmostoneaux",4,true,false) );
		storage.registerPredicate( new PredicateDeclaration("datmostoneaux",4,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ran",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dran",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dsubconjunctionof",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dsubsomevalues",3,false,false) );
		storage.registerPredicate( new PredicateDeclaration("spo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dspo",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("disjoint",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ddisjoint",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("disjointaux",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("ddisjointaux",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("nominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("dnominal",1,false,false) );
		storage.registerPredicate( new PredicateDeclaration("nonempty",1,true,false) );
		storage.registerPredicate( new PredicateDeclaration("dnonempty",1,true,false) );
		
		storage.registerPredicate( new PredicateDeclaration("eltype",1,true,false) );
		
		storage.registerPredicate( new PredicateDeclaration("spoc",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("self",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("subself",2,false,false) );
		storage.registerPredicate( new PredicateDeclaration("inverseof",2,true,false) );
	}

}
