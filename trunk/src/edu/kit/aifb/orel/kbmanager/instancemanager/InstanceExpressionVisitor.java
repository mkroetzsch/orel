package edu.kit.aifb.orel.kbmanager.instancemanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.kbmanager.Literals;
import edu.kit.aifb.orel.storage.SimpleLiteral;
import edu.kit.aifb.orel.storage.StorageDriver;

public class InstanceExpressionVisitor implements
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
	
	public InstanceExpressionVisitor(Action a, StorageDriver sd) {
		storage = sd;
		action = a;
	}
	
	public String visitAndAct(OWLClassExpression ce, Action a) {
		action = a;
		return ce.accept(this);
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
					storage.makePredicateAssertion("subcon",sid1,sid2,oid);
					createClassTautologies(oid,storage);
			}
		} else if (action == Action.WRITEHEAD) {
			result = makeNAryExpressionKey(OP_OBJECT_INTERSECTION,opkeys,opkeys.size()-1,OP_NOTHING);
			sid1 = storage.getID(result);
			for (int i=0; i<opkeys.size(); i++) {
				oid = storage.getID(opkeys.get(i));
				storage.makePredicateAssertion("subc",sid1,oid);
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
		int sid, oid;
		if (action == Action.WRITEBODY) {		
			result = makeNAryExpressionKey(OP_OBJECT_UNION,opkeys,opkeys.size()-1,OP_THING);
			oid = storage.getID(result);
			for (int i=0; i<opkeys.size(); i++) {
				sid = storage.getID(opkeys.get(i));
				storage.makePredicateAssertion("subc",sid,oid);
			}
			createClassTautologies(oid,storage);
		} else if (action == Action.WRITEHEAD) {
			result = null; // not supported in OWL EL or RL
		}  else {
			assert action == Action.READ;
			result = makeNAryExpressionKey(OP_OBJECT_UNION,opkeys,opkeys.size()-1,OP_THING);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectComplementOf ce) {
		// process subclass under inverted polarity
		String subkey;
		if (action == Action.WRITEBODY) {
			return null;
		} else if (action == Action.WRITEHEAD) {
			action = Action.WRITEBODY;
			subkey = ce.getOperand().accept(this);
			action = Action.WRITEHEAD;
		} else {
			subkey = ce.getOperand().accept(this);
		}
		if (subkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_COMPLEMENT,subkey);
		if (action == Action.WRITEHEAD) {
			int sid1 = storage.getID(result);
			int sid2 = storage.getID(subkey);
			storage.makePredicateAssertion("subcon",sid1,sid2,storage.getID(InstanceExpressionVisitor.OP_NOTHING));
			createClassTautologies(sid1,storage);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectSomeValuesFrom ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey = ce.getFiller().accept(this);
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_SOME,propkey,fillkey);
		if (action == Action.WRITEBODY) {
			int sid = storage.getID(fillkey);
			int pid = storage.getID(propkey);
			int oid = storage.getID(result);
			createClassTautologies(oid,storage);
			storage.makePredicateAssertion("subsome",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(fillkey);
			createClassTautologies(sid,storage);
			int auxid = storage.getID("C(" + result + ")"); // auxiliary "Skolem" constant
			createClassTautologies(auxid,storage); // TODO: is this needed?
			storage.makePredicateAssertion("supsome",sid,pid,auxid);
			storage.makePredicateAssertion("subc",auxid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectAllValuesFrom ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey = ce.getFiller().accept(this);
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_ALL,propkey,fillkey);
		if (action == Action.WRITEBODY) {
			return null; // not supported in EL or RL
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(fillkey);
			createClassTautologies(sid,storage);
			storage.makePredicateAssertion("supall",sid,pid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectHasValue ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String valuekey = ce.getValue().accept(this);
		if (valuekey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_SOME,propkey,valuekey);
		if (action == Action.WRITEBODY) {
			int sid = storage.getID(valuekey);
			int pid = storage.getID(propkey);
			int oid = storage.getID(result);
			createClassTautologies(oid,storage);
			storage.makePredicateAssertion("subsome",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(valuekey);
			createClassTautologies(sid,storage);
			storage.makePredicateAssertion("supsome",sid,pid,oid);
		}
		return result;
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
		if (action == Action.WRITEBODY) return null;
		if (ce.getCardinality() > 1) return null;
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey;
		if (action == Action.WRITEHEAD) {
			action = Action.WRITEBODY;
			fillkey = ce.getFiller().accept(this);
			action = Action.WRITEHEAD;
		} else {
			assert action == Action.READ;
			fillkey = ce.getFiller().accept(this);
		}
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_MAX,new Integer(ce.getCardinality()).toString(),propkey,fillkey);
		if (action == Action.WRITEHEAD) {
			int rid = storage.getID(result);
			int pid = storage.getID(propkey);
			int fillid = storage.getID(fillkey);
			createClassTautologies(rid,storage);
			if (ce.getCardinality() == 1) {
				storage.makePredicateAssertion("supfunc",rid,pid,fillid);
			} else {
				assert ce.getCardinality() == 0;
				String auxkey = makeNAryExpressionKey(OP_OBJECT_SOME,propkey,fillkey);
				int someid = storage.getID(auxkey);
				createClassTautologies(someid,storage);
				storage.makePredicateAssertion("subsome",pid,fillid,someid);
				storage.makePredicateAssertion("subcon",rid,someid,storage.getID(InstanceExpressionVisitor.OP_NOTHING));
			}
		}
		return result;
	}

	@Override
	public String visit(OWLObjectHasSelf ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_SELF, propkey);
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int pid = storage.getID(propkey);
			int id = storage.getID(result);
			createClassTautologies(id,storage);
			if (action == Action.WRITEBODY) {
				storage.makePredicateAssertion("subself",pid,id);
			} else {
				storage.makePredicateAssertion("supself",id,pid);
			}
		}
		return result;
	}

	@Override
	public String visit(OWLObjectOneOf ce) {
		String result;
		if (ce.getIndividuals().size() == 1) {
			result = ce.getIndividuals().iterator().next().accept(this);
		} else {
			if (action == Action.WRITEHEAD) {
				result = null;
			} else {
				result = ce.asObjectUnionOf().accept(this);
			}
		}
		return result;
	}

	@Override
	public String visit(OWLDataSomeValuesFrom ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey = ce.getFiller().accept(this);
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_SOME,propkey,fillkey);
		if (action == Action.WRITEBODY) {
			int sid = storage.getID(fillkey);
			int pid = storage.getID(propkey);
			int oid = storage.getID(result);
			createClassTautologies(oid,storage);
			storage.makePredicateAssertion("dsubsome",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			// NOTE: We load all types in RL/EL, and restrict certain inferences to EL datatypes later on.
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(fillkey);
			createClassTautologies(sid,storage);
			int auxid = storage.getID("C(" + result + ")"); // auxiliary "Skolem" constant
			createDatarangeTautologies(auxid,storage); // TODO: is this needed?
			storage.makePredicateAssertion("dsupsome",sid,pid,auxid);
			storage.makePredicateAssertion("dsubc",auxid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLDataAllValuesFrom ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey = ce.getFiller().accept(this);
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_ALL,propkey,fillkey);
		if (action == Action.WRITEBODY) {
			return null; // not supported in EL or RL
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(fillkey);
			createClassTautologies(sid,storage);
			storage.makePredicateAssertion("dsupall",sid,pid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLDataHasValue ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String valuekey = ce.getValue().accept(this);
		if (valuekey == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_SOME,propkey,valuekey);
		if (action == Action.WRITEBODY) {
			int sid = storage.getID(valuekey);
			int pid = storage.getID(propkey);
			int oid = storage.getID(result);
			createClassTautologies(oid,storage);
			storage.makePredicateAssertion("dsubsome",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(valuekey);
			createClassTautologies(sid,storage);
			storage.makePredicateAssertion("dsupsome",sid,pid,oid);
		}
		return result;
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
		if (action == Action.WRITEBODY) return null;
		if (ce.getCardinality() > 1) return null;
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String fillkey;
		if (action == Action.WRITEHEAD) {
			action = Action.WRITEBODY;
			fillkey = ce.getFiller().accept(this);
			action = Action.WRITEHEAD;
		} else {
			assert action == Action.READ;
			fillkey = ce.getFiller().accept(this);
		}
		if (fillkey == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_MAX,new Integer(ce.getCardinality()).toString(),propkey,fillkey);
		if (action == Action.WRITEHEAD) {
			int rid = storage.getID(result);
			int pid = storage.getID(propkey);
			int fillid = storage.getID(fillkey);
			createClassTautologies(rid,storage);
			if (ce.getCardinality() == 1) {
				storage.makePredicateAssertion("dsupfunc",rid,pid,fillid);
			} else {
				assert ce.getCardinality() == 0;
				String auxkey = makeNAryExpressionKey(OP_DATA_SOME,propkey,fillkey);
				int someid = storage.getID(auxkey);
				createClassTautologies(someid,storage);
				storage.makePredicateAssertion("dsubsome",pid,fillid,someid);
				storage.makePredicateAssertion("subcon",rid,someid,storage.getID(InstanceExpressionVisitor.OP_NOTHING));
			}
		}
		return result;
	}
	
	@Override
	public String visit(OWLObjectProperty property) {
		if ( property.isOWLTopObjectProperty() ) {
			return null;
		}
		String result = property.toString();
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result); 
			//storage.makePredicateAssertion("subp",id,id); // not needed (no support for property inferences right now) 
			storage.makePredicateAssertion("subp",storage.getID(OP_BOTTOM_OBJECT_PROPERTY),id);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectInverseOf property) {
		String propkey = property.getInverse().accept(this);
		if (propkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_INVERSE_OF, propkey);
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int pid = storage.getID(propkey);
			int id = storage.getID(result);
			storage.makePredicateAssertion("subinv",id,pid);
		}
		return result;
	}

	@Override
	public String visit(OWLDataProperty property) {
		if ( property.isOWLTopDataProperty() ) {
			return null;
		}
		String result = property.toString();
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result); 
			//storage.makePredicateAssertion("dsubp",id,id); // not needed (no support for property inferences right now)
			storage.makePredicateAssertion("dsubp",storage.getID(OP_BOTTOM_DATA_PROPERTY),id);
		}
		return result;
	}
	
	@Override
	public String visit(OWLNamedIndividual individual) {
		String result = makeNAryExpressionKey(OP_OBJECT_ONE_OF, individual.toString());
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result);
			createClassTautologies(id,storage);
			storage.makePredicateAssertion("name",id);
			storage.makePredicateAssertion("extant",id);
		}
		return result;
	}

	@Override
	public String visit(OWLAnonymousIndividual individual) {
		// TODO Handling of bnodes in OWL RL ontology entailment not clear yet
		// This code is only correct for bnodes in asserted ontologies, not in entailed ones 
		String result = makeNAryExpressionKey(OP_OBJECT_ONE_OF, individual.getID().getID());
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result);
			createClassTautologies(id,storage);
			storage.makePredicateAssertion("name",id);
			storage.makePredicateAssertion("extant",id);
		}
		return result;
	}

	@Override
	public String visit(OWLDatatype node) {
		if (Literals.isELRLType(node.getIRI().toString())) {
			return node.getIRI().toString();
		} else {
			return null;
		}
	}

	@Override
	public String visit(OWLDataComplementOf node) {
		// Not supported in OWL EL or RL
		return null;
	}

	@Override
	public String visit(OWLDataOneOf node) {
		String result;
		if (node.getValues().size() == 1) {
			result = node.getValues().iterator().next().accept(this);
		} else { // not supported in OLW EL or RL, though we could allow this in bodies
			result = null;
		}
		return result;
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
		return visitSimpleLiteral(Literals.makeSimpleLiteral(node), storage, action);
	}

	@Override
	public String visit(OWLStringLiteral node) {
		return visitSimpleLiteral(Literals.makeSimpleLiteral(node), storage, action);
	}
	
	public static String visitSimpleLiteral(SimpleLiteral sl, StorageDriver storage, Action action) {
		if (sl == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_ONE_OF, sl.toString());
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result);
			storage.makePredicateAssertion("dname",id);
			storage.makePredicateAssertion("dextant",id);
			createDatarangeTautologies(id,storage);
			if (action == Action.WRITEHEAD) {
				List<String> typeuris = Literals.getDatatypeURIs(sl);
				for (int i=0; i<typeuris.size(); i++) {
					storage.makePredicateAssertion("dsubc",id,storage.getID(typeuris.get(i)));
				}
			}
		}
		return result;
	}

	@Override
	public String visit(OWLFacetRestriction node) {
		// not supported in OWL EL or RL
		return null;
	}

	public static void createClassTautologies(int id, StorageDriver storage) {
		storage.makePredicateAssertion("inst",id,id);
		storage.makePredicateAssertion("inst",id,storage.getID(InstanceExpressionVisitor.OP_THING));
	}

	public static void createDatarangeTautologies(int id, StorageDriver storage) {
		storage.makePredicateAssertion("dinst",id,id);
		storage.makePredicateAssertion("dinst",id,storage.getID(Literals.TOP_DATATYPE));
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
