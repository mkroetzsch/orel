package edu.kit.aifb.orel.kbmanager.instancemanager;

import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;

import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.kbmanager.KBManager;
import edu.kit.aifb.orel.kbmanager.Literals;
import edu.kit.aifb.orel.storage.SimpleLiteral;
import edu.kit.aifb.orel.storage.StorageDriver;

/**
 * KBManager class that implements a data-driven inferencing approach. Only
 * statements that refer to instances are computed during reasoning, while all
 * schema-level axioms are static data. To check for entailment of schema-level
 * axioms, new "test individuals" are introduced, reducing the task to instance
 * derivations. Care is taken to not modify the semantics of the stored data
 * when performing such tests.
 * 
 * To obtain cheap test individuals, the manager uses the internal IDs of a
 * class as the ID of a anonymous test individual that belongs to that class.
 * To avoid confusion with the IDs of real individuals that have the same URI
 * as some class (punning), the IDs assigned to real individuals are based on
 * singleton classes (ObjectOneOf) that contain only that individual. For
 * example, the ID for a class ":A" is based on ":A" and the same ID will be
 * used for the associated test individual, but a real individual with called
 * ":A" would get the ID of "ObjectOneOf( :A )" instead. Checking that a class
 * C is a subclass of D is implemented by checking whether the ID of C is an
 * instance of the ID of D, so class assertions and singleton subclasses lead
 * to the same check -- as required.
 * 
 * In addition, the manager distinguishes individuals that necessarily exist
 * from test individuals that are only introduced for checking schema
 * entailments. For the latter, the reasoner allows for the possibility that
 * they may not really exist: it just simulates the effects that their
 * existence would have, but it does not conclude a global contradiction if
 * the class that contains the test individual turns out to be incoherent.
 * The manager uses a unary predicate "is" for marking individuals that really
 * exist. So in the above example, the individual with ID "ObjectOneOf( :A )"
 * is real, while the individual ":A" is not.
 * @author Markus Krötzsch
 */
public class InstanceKBManager extends KBManager {

	/**
	 * Constructor that also establishes a database connection, since this
	 * object cannot really work without a database.
	 */
	public InstanceKBManager(StorageDriver storage) {
		super(storage);
		registerPredicates();
	}

	/**
	 * Ensure that the DB has the right tables, creating them if necessary.
	 */
	@Override
	public void initialize() throws Exception {
		storage.initialize();
		int thing = storage.getIDForThing(), //nothing = storage.getIDForNothing(),
			toptype = storage.getIDForTopDatatype(), bottype = storage.getIDForBottomDatatype();
		// use thing as the default instance of itself (punning):
		storage.makePredicateAssertion("inst",thing,thing);
		storage.makePredicateAssertion("extant",thing);
		storage.makePredicateAssertion("dinst",toptype,toptype);
		storage.makePredicateAssertion("dextant",toptype);

		// axiomatize basic datatype dependencies:
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
		String zerokey = InstanceExpressionVisitor.visitSimpleLiteral(zero, storage, InstanceExpressionVisitor.Action.WRITEHEAD);
		int zeroid = storage.getID(zerokey);
		
		storage.makePredicateAssertion("dsubc",owlRational,owlReal);
		storage.makePredicateAssertion("dsubc",xsdDecimal,owlRational);
		storage.makePredicateAssertion("dsubc",xsdInteger,xsdDecimal);
		storage.makePredicateAssertion("dsubc",xsdNonNegativeInteger,xsdInteger);
		storage.makePredicateAssertion("dsubc",xsdPositiveInteger,xsdNonNegativeInteger);
		storage.makePredicateAssertion("dsubc",xsdNonPositiveInteger,xsdInteger);
		storage.makePredicateAssertion("dsubc",xsdNegativeInteger,xsdNonPositiveInteger);
		storage.makePredicateAssertion("dsubc",xsdLong,xsdInteger);
		storage.makePredicateAssertion("dsubc",xsdInt,xsdLong);
		storage.makePredicateAssertion("dsubc",xsdShort,xsdInt);
		storage.makePredicateAssertion("dsubc",xsdByte,xsdShort);
		storage.makePredicateAssertion("dsubc",xsdUnsignedLong,xsdNonNegativeInteger);
		storage.makePredicateAssertion("dsubc",xsdUnsignedInt,xsdUnsignedLong);
		storage.makePredicateAssertion("dsubc",xsdUnsignedShort,xsdUnsignedInt);
		storage.makePredicateAssertion("dsubc",xsdUnsignedByte,xsdUnsignedShort);
		storage.makePredicateAssertion("dsubcon",xsdLong,xsdNonNegativeInteger,xsdUnsignedLong);
		storage.makePredicateAssertion("dsubcon",xsdInt,xsdUnsignedLong,xsdUnsignedInt);
		storage.makePredicateAssertion("dsubcon",xsdShort,xsdUnsignedInt,xsdUnsignedShort);
		storage.makePredicateAssertion("dsubcon",xsdByte,xsdUnsignedShort,xsdUnsignedByte);
		storage.makePredicateAssertion("dsubcon",xsdNonNegativeInteger,xsdNegativeInteger,bottype);
		storage.makePredicateAssertion("dsubcon",xsdNonPositiveInteger,xsdPositiveInteger,bottype);
		storage.makePredicateAssertion("dsubcon",xsdNonPositiveInteger,xsdNonNegativeInteger,zeroid);
		
		List<String> eltypes = Literals.getELDatatypeURIs();
		for (int i=0; i<eltypes.size(); i++) {
			storage.makePredicateAssertion("eltype",storage.getID(eltypes.get(i)));
		}
		List<String> numerictypes = Literals.getNumericDatatypeURIs(), othertypes = Literals.getOtherDatatypeURIs();
		for (int i=0; i<numerictypes.size(); i++) {
			int ntypeid = storage.getID(numerictypes.get(i));
			InstanceExpressionVisitor.createDatarangeTautologies(ntypeid,storage);
			storage.makePredicateAssertion("dinst",ntypeid,ntypeid);
			storage.makePredicateAssertion("dextant",ntypeid);
			for (int j=0; j<othertypes.size(); j++) {
				int otypeid = storage.getID(othertypes.get(j));
				storage.makePredicateAssertion("dsubcon",ntypeid,otypeid,bottype);
			}
		}
		for (int i=0; i<othertypes.size(); i++) {
			int otypeid1 = storage.getID(othertypes.get(i));
			InstanceExpressionVisitor.createDatarangeTautologies(otypeid1,storage);
			storage.makePredicateAssertion("dinst",otypeid1,otypeid1);
			storage.makePredicateAssertion("dextant",otypeid1);
			for (int j=i+1; j<othertypes.size(); j++) {
				int otypeid2 = storage.getID(othertypes.get(j));
				storage.makePredicateAssertion("dsubcon",otypeid1,otypeid2,bottype);
			}
		}
		storage.commit();
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	@Override
	public boolean loadOntology(OWLOntology ontology) throws Exception {
		boolean result = true;
		InstanceKBLoader loader = new InstanceKBLoader(storage);
		// TODO Do we need to guard against input loops (cyclic imports)?
		Iterator<OWLOntology> ontit = ontology.getDirectImports().iterator();
		while (ontit.hasNext()) {
			result = loadOntology(ontit.next()) && result;
		}
		return loader.processOntology(ontology, (InstanceKBLoader.PREPAREASSERT | InstanceKBLoader.ASSERT) ) && result;
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	@Override
	public InferenceResult checkEntailment(OWLOntology ontology) throws Exception {
		InstanceKBReasoner reasoner = new InstanceKBReasoner(storage);
		return reasoner.checkEntailment(ontology);
	}
	
	/**
	 * Check if the loaded axioms are consistent.
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 */
	@Override
	public InferenceResult checkConsistency() throws Exception {
		InstanceKBReasoner reasoner = new InstanceKBReasoner(storage);
		return reasoner.checkConsistency();
	}

	/**
	 * Compute all materialized statements on the database.
	 */
	@Override
	public void materialize() throws Exception {
		InstanceKBReasoner reasoner = new InstanceKBReasoner(storage);
		reasoner.materialize();
	}

	protected void registerPredicates() {
		// inferred predicates
		storage.registerPredicate( new PredicateDeclaration("inst",2,true,false) ); // inst(a,C): C(a)
		storage.registerPredicate( new PredicateDeclaration("dinst",2,true,false) );
		storage.registerPredicate( new PredicateDeclaration("self",2,true,false) ); // self(a,R): ∃R.Self(a)
		storage.registerPredicate( new PredicateDeclaration("triple",3,true,false) ); // triple(a,R,b): R(a,b)
		storage.registerPredicate( new PredicateDeclaration("dtriple",3,true,false) );
		storage.registerPredicate( new PredicateDeclaration("extant",1,true,false) ); // extant(a): the element a necessarily exists
		storage.registerPredicate( new PredicateDeclaration("dextant",1,true,false) );
		// auxiliary predicates
		storage.registerPredicate( new PredicateDeclaration("name",1,false,false) ); // name(a): the element a is named
		storage.registerPredicate( new PredicateDeclaration("dname",1,false,false) );
		// axiom predicates
		/// TBox (classes)
		storage.registerPredicate( new PredicateDeclaration("subc",2,false,false) ); // subc(A,C): A ⊑ C
		storage.registerPredicate( new PredicateDeclaration("subcon",3,false,false) ); // subcon(A,B,C): A ⊓ B ⊑ C
		storage.registerPredicate( new PredicateDeclaration("subsome",3,false,false) ); // subsome(R,A,C): ∃R.A ⊑ C
		storage.registerPredicate( new PredicateDeclaration("supsome",3,false,false) ); // supsome(A,R,C): A ⊑ ∃R.C
		storage.registerPredicate( new PredicateDeclaration("subself",2,false,false) ); // subself(R,C): ∃R.Self ⊑ C
		storage.registerPredicate( new PredicateDeclaration("supself",2,false,false) ); // supself(A,R): A ⊑ ∃R.Self
		storage.registerPredicate( new PredicateDeclaration("supall",3,false,false) ); // supall(A,R,C): A ⊑ ∀R.C
		storage.registerPredicate( new PredicateDeclaration("supfunc",3,false,false) ); // supfunc(A,R,C): A ⊑ ≤1R.B
		/// TBox (datatypes)
		storage.registerPredicate( new PredicateDeclaration("dsubc",2,false,false) ); // dsubc(D,E): D ⊑ E
		storage.registerPredicate( new PredicateDeclaration("dsubcon",3,false,false) ); // dsubcon(D,E,F): D ⊓ E ⊑ F
		storage.registerPredicate( new PredicateDeclaration("dsubsome",3,false,false) ); // dsubsome(P,D,C): ∃P.D ⊑ C
		storage.registerPredicate( new PredicateDeclaration("dsupsome",3,false,false) ); // dsupsome(A,P,D): A ⊑ ∃P.D
		storage.registerPredicate( new PredicateDeclaration("dsupall",3,false,false) ); // dsupall(A,P,D): A ⊑ ∀P.D
		storage.registerPredicate( new PredicateDeclaration("dsupfunc",3,false,false) ); // dsupfunc(A,P,D): A ⊑ ≤1P.D
		
		/// RBox
		storage.registerPredicate( new PredicateDeclaration("subp",2,false,false) ); // subp(R,S): R ⊑ S
		storage.registerPredicate( new PredicateDeclaration("dsubp",2,false,false) ); // subp(P,Q): P ⊑ Q (DataProperties)
		storage.registerPredicate( new PredicateDeclaration("subinv",2,false,false) ); // subinv(R,S): Inv(R) ⊑ S
		storage.registerPredicate( new PredicateDeclaration("subchain",3,false,false) ); // subchain(R,S,T): R ◦ S ⊑ T
		//storage.registerPredicate( new PredicateDeclaration("subpcon",3,false,false) ); // subrcon(R,S,T): R ⊓ S ⊑ T
		// only support one special case of role conjunctions
		storage.registerPredicate( new PredicateDeclaration("pdisjoint",2,false,false) ); // pdisjoint(R,S): R ⊓ S ⊑ bottom-role
		storage.registerPredicate( new PredicateDeclaration("dpdisjoint",2,false,false) ); // pdisjoint(R,S): P ⊓ Q ⊑ bottom-role (DataProperties)
		
		storage.registerPredicate( new PredicateDeclaration("eltype",1,true,false) );
	}

	
}
