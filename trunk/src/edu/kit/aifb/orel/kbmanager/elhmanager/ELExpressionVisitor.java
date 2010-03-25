package edu.kit.aifb.orel.kbmanager.elhmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDataVisitorEx;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLIndividualVisitorEx;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLStringLiteral;
import org.semanticweb.owlapi.model.OWLTypedLiteral;

import edu.kit.aifb.orel.kbmanager.Literals;
import edu.kit.aifb.orel.storage.StorageDriver;

public class ELExpressionVisitor implements
		OWLClassExpressionVisitorEx<String>, OWLPropertyExpressionVisitorEx<String>,
		OWLIndividualVisitorEx<String>, OWLDataVisitorEx<String> {

	final public static String OP_NOTHING = "owl:Nothing"; // "http://www.w3.org/2002/07/owl#Nothing";
	final public static String OP_THING = "owl:Thing";
	final public static String OP_OBJECT_INTERSECTION = "ObjectIntersectionOf";
	final public static String OP_OBJECT_UNION = "ObjectUnionOf";
	final public static String OP_OBJECT_ONE_OF = "ObjectOneOf";
	final public static String OP_DATA_ONE_OF = "DataOneOf";
	final public static String OP_OBJECT_SOME = "ObjectSomeValuesFrom";
	final public static String OP_DATA_SOME = "DataSomeValuesFrom";
	final public static String OP_OBJECT_SELF = "ObjectHasSelf";
	final public static String OP_OBJECT_COMPLEMENT = "ObjectComplementOf";
	final public static String OP_OBJECT_INVERSE_OF = "ObjectInverseOf";
	final public static String OP_OBJECT_ALL = "ObjectAllValuesFrom";
	final public static String OP_DATA_ALL = "DataAllValuesFrom";
	final public static String OP_OBJECT_MAX = "ObjectMaxCardinality";
	final public static String OP_DATA_MAX = "DataMaxCardinality";
	final public static String OP_BOTTOM_OBJECT_PROPERTY = "owl:bottomObjectProperty";
	final public static String OP_BOTTOM_DATA_PROPERTY = "owl:bottomDataProperty";
	
	/**
	 * Defines what the visitor should do, if anything. The write actions 
	 * alter the storage, the read action only computes the key string. 
	 */
	static public enum Action {
	    READ,WRITEBODY,WRITEHEAD 
	}
	protected StorageDriver storage;
	protected Action action;
	
	public ELExpressionVisitor(Action a, StorageDriver sd) {
		storage = sd;
		action = a;
	}
	
	public String visitAndAct(OWLClassExpression c, Action a) {
		action = a;
		return c.accept(this);
	}

	public String visitAndAct(OWLDataRange dr, Action a) {
		action = a;
		return dr.accept(this);
	}
	
	public String visitAndAct(OWLPropertyExpression<?,?> pe, Action a) {
		action = a;
		return pe.accept(this);
	}
	
	@Override
	public String visit(OWLClass ce) {
		String result;
		if (ce.isOWLThing()) {
			result = OP_THING;
		} else if (ce.isOWLNothing()) {
			result = OP_NOTHING;
		} else {
			result = ce.toString();
		}
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			createClassTautologies(storage.getID(result),storage);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectIntersectionOf ce) {
		ArrayList<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(ce.getOperands());
		ArrayList<String> opkeys = new ArrayList<String>(ops.size());
		String opkey;
		Collections.sort(ops); // sorting helps with re-use of ids and reduces randomness between runs
		for (int i=0; i<ops.size(); i++) {
			opkey = ops.get(i).accept(this);
			if (opkey == null) {
				return null;
			}
			opkeys.add(opkey);
		}
		String result;
		int sid1, sid2, oid;
		if (action == Action.WRITEBODY) {
			result = (ops.size()>0) ? opkeys.get(0) : OP_NOTHING;
			for (int i=1; i<ops.size(); i++) {
					sid1 = storage.getID(result);
					sid2 = storage.getID(opkeys.get(i));
					result = makeNAryExpressionKey(OP_OBJECT_INTERSECTION,opkeys,i,null); 
					oid  = storage.getID(result);
					storage.makePredicateAssertion("subconjunctionof",sid1,sid2,oid);
					createClassTautologies(oid,storage);
			}
		} else if (action == Action.WRITEHEAD) {
			result = makeNAryExpressionKey(OP_OBJECT_INTERSECTION,opkeys,opkeys.size()-1,OP_NOTHING);
			sid1 = storage.getID(result);
			for (int i=0; i<opkeys.size(); i++) {
				oid = storage.getID(opkeys.get(i));
				storage.makePredicateAssertion("sco",sid1,oid);
			}
			createClassTautologies(sid1,storage);
		} else {
			assert action == Action.READ;
			result = makeNAryExpressionKey(OP_OBJECT_INTERSECTION,opkeys,opkeys.size()-1,OP_NOTHING);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectUnionOf ce) {
		return null;
	}

	@Override
	public String visit(OWLObjectComplementOf ce) {
		return null;
	}

	@Override
	public String visit(OWLObjectSomeValuesFrom ce) {
		return null;
}

	@Override
	public String visit(OWLObjectAllValuesFrom ce) {
		return null;
	}

	@Override
	public String visit(OWLObjectHasValue ce) {
//		String propkey = ce.getProperty().accept(this);
//		if (propkey == null) return null;
//		String valuekey = ce.getValue().accept(this);
//		if (valuekey == null) return null;
//		String result = makeNAryExpressionKey(OP_OBJECT_SOME,propkey,valuekey);
//		if (action == Action.WRITEBODY) {
//			int sid = storage.getID(valuekey);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(result);
//			createClassTautologies(oid,storage);
//			storage.makePredicateAssertion("subsomevalues",pid,sid,oid);
//		} else if (action == Action.WRITEHEAD) {
//			int sid = storage.getID(result);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(valuekey);
//			createClassTautologies(sid,storage);
//			storage.makePredicateAssertion("sv",sid,pid,oid);
//		}
//		return result;
		return null;
	}

	@Override
	public String visit(OWLObjectMinCardinality ce) {
		// not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLObjectExactCardinality ce) {
		// not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLObjectMaxCardinality ce) {
		return null;
	}

	@Override
	public String visit(OWLObjectHasSelf ce) {
		return null;
	}

	@Override
	public String visit(OWLObjectOneOf ce) {
		return null;
}

	@Override
	public String visit(OWLDataSomeValuesFrom ce) {
//		String propkey = ce.getProperty().accept(this);
//		if (propkey == null) return null;
//		String fillkey = ce.getFiller().accept(this);
//		if (fillkey == null) return null;
//		String result = makeNAryExpressionKey(OP_DATA_SOME,propkey,fillkey);
//		if (action == Action.WRITEBODY) {
//			int sid = storage.getID(fillkey);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(result);
//			createClassTautologies(oid,storage);
//			storage.makePredicateAssertion("dsubsomevalues",pid,sid,oid);
//		} else if (action == Action.WRITEHEAD) {
//			// NOTE: We load all types in RL/EL, and restrict certain inferences to EL datatypes later on.
//			int sid = storage.getID(result);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(fillkey);
//			createClassTautologies(sid,storage);
//			int auxid = storage.getID("C(" + result + ")"); // auxiliary "Skolem" constant
//			createDatarangeTautologies(auxid,storage); // TODO: is this needed?
//			storage.makePredicateAssertion("dsv",sid,pid,auxid);
//			storage.makePredicateAssertion("dsco",auxid,oid);
//		}
		return null;
	}

	@Override
	public String visit(OWLDataAllValuesFrom ce) {
		return null;
}

	@Override
	public String visit(OWLDataHasValue ce) {
		return null;
//		String propkey = ce.getProperty().accept(this);
//		if (propkey == null) return null;
//		String valuekey = ce.getValue().accept(this);
//		if (valuekey == null) return null;
//		String result = makeNAryExpressionKey(OP_DATA_SOME,propkey,valuekey);
//		if (action == Action.WRITEBODY) {
//			int sid = storage.getID(valuekey);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(result);
//			createClassTautologies(oid,storage);
//			storage.makePredicateAssertion("dsubsomevalues",pid,sid,oid);
//		} else if (action == Action.WRITEHEAD) {
//			int sid = storage.getID(result);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(valuekey);
//			createClassTautologies(sid,storage);
//			storage.makePredicateAssertion("dsv",sid,pid,oid);
//		}
//		return result;
	}

	@Override
	public String visit(OWLDataMinCardinality ce) {
		// not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLDataExactCardinality ce) {
		// not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLDataMaxCardinality ce) {
		return null;
	}
	
	@Override
	public String visit(OWLObjectProperty property) {
		if ( property.isOWLTopObjectProperty() ) {
			return null;
		}
		String result = property.toString();
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result); 
			storage.makePredicateAssertion("spo",id,id);
			storage.makePredicateAssertion("spo",storage.getID(OP_BOTTOM_OBJECT_PROPERTY),id);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectInverseOf property) {
		return null;
	}

	@Override
	public String visit(OWLDataProperty property) {
	return null;
	}
	
	@Override
	public String visit(OWLNamedIndividual individual) {
		return null;
	}

	@Override
	public String visit(OWLAnonymousIndividual individual) {
		return null;
	}

	@Override
	public String visit(OWLDatatype node) {
		return null;
	}

	@Override
	public String visit(OWLDataComplementOf node) {
		// Not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLDataOneOf node) {
		return null;
	}

	@Override
	public String visit(OWLDataIntersectionOf node) {
		// TODO Currently not supported
		return null;
	}

	@Override
	public String visit(OWLDataUnionOf node) {
		// Not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLDatatypeRestriction node) {
		// Not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLTypedLiteral node) {
		return null;
	}

	@Override
	public String visit(OWLStringLiteral node) {
		return null;
	}
	
//	public static String visitSimpleLiteral(SimpleLiteral sl, StorageDriver storage, Action action) {
//		if (sl == null) return null;
//		String result = makeNAryExpressionKey(OP_DATA_ONE_OF, sl.toString());
//		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
//			int id = storage.getID(result);
//			storage.makePredicateAssertion("dnominal",id);
//			storage.makePredicateAssertion("dnonempty",id);
//			createDatarangeTautologies(id,storage);
//			if (action == Action.WRITEHEAD) {
//				List<String> typeuris = Literals.getDatatypeURIs(sl);
//				for (int i=0; i<typeuris.size(); i++) {
//					storage.makePredicateAssertion("dsco",id,storage.getID(typeuris.get(i)));
//				}
//			}
//		}
//		return result;
//	}

	@Override
	public String visit(OWLFacetRestriction node) {
		// not supported in OWL EL or RL
		return null;
	}
	
	public static void createClassTautologies(int id, StorageDriver storage) {
		storage.makePredicateAssertion("sco",id,id);
		storage.makePredicateAssertion("sco",id,storage.getID(OP_THING));
		storage.makePredicateAssertion("sco",storage.getID(OP_NOTHING),id);
	}

	public static void createDatarangeTautologies(int id, StorageDriver storage) {
		storage.makePredicateAssertion("dsco",id,id);
		storage.makePredicateAssertion("dsco",id,storage.getID(Literals.TOP_DATATYPE));
		storage.makePredicateAssertion("dsco",storage.getID(Literals.BOTTOM_DATATYPE),id);
	}
	
	public static String makeNAryExpressionKey(String opname, List<String> operators, int maxindex, String emptyKey) {
		assert maxindex<operators.size();
		if (maxindex<0) return emptyKey;
		String oplist = "";
		for (int i=0; i<=maxindex; i++) {
			oplist += " " + operators.get(i);
		}
		return opname + "(" + oplist + " )";
	}
	
	public static String makeNAryExpressionKey(String opname, String... operators) {
		String oplist = "";
		for (int i=0; i<operators.length; i++) {
			oplist += " " + operators[i];
		}
		return opname + "(" + oplist + " )";
	}

	

}
