package edu.kit.aifb.orel.kbmanager;

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

import edu.kit.aifb.orel.storage.SimpleLiteral;
import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicExpressionVisitor implements
		OWLClassExpressionVisitorEx<String>, OWLPropertyExpressionVisitorEx<String>,
		OWLIndividualVisitorEx<String>, OWLDataVisitorEx<String> {

	final public static String OP_NOTHING = "owl:Nothing";
	final public static String OP_THING = "owl:Thing";
	final public static String OP_OBJECT_INTERSECTION = "ObjectIntersectionOf";
	final public static String OP_OBJECT_UNION = "ObjectUnionOf";
	final public static String OP_OBJECT_ONE_OF = "ObjectOneOf";
	final public static String OP_DATA_ONE_OF = "DataOneOf";
	final public static String OP_OBJECT_SOME = "ObjectSomeValuesFrom";
	final public static String OP_DATA_SOME = "DataSomeValuesFrom";
	final public static String OP_OBJECT_SELF = "ObjectHasSelf";
	
	/**
	 * Defines what the visitor should do, if anything. The write actions 
	 * alter the storage, the read action only computes the key string. 
	 */
	static public enum Action {
	    READ,WRITEBODY,WRITEHEAD 
	}
	protected StorageDriver storage;
	protected Action action;
	
	public BasicExpressionVisitor(Action a, StorageDriver sd) {
		storage = sd;
		action = a;
	}
	
	public String visitAndAct(OWLClassExpression ce, Action a) {
		action = a;
		return ce.accept(this);
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
			createClassTautologies(storage.getID(result));
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
					createClassTautologies(oid);
			}
		} else if (action == Action.WRITEHEAD) {
			result = makeNAryExpressionKey(OP_OBJECT_INTERSECTION,opkeys,opkeys.size()-1,OP_NOTHING);
			sid1 = storage.getID(result);
			for (int i=0; i<opkeys.size(); i++) {
				oid = storage.getID(opkeys.get(i));
				storage.makePredicateAssertion("sco",sid1,oid);
			}
			createClassTautologies(sid1);
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
				storage.makePredicateAssertion("sco",sid,oid);
			}
			createClassTautologies(oid);
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
		// TODO Auto-generated method stub
		return null;
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
			createClassTautologies(oid);
			storage.makePredicateAssertion("subsomevalues",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(fillkey);
			createClassTautologies(sid);
			int auxid = storage.getID("C(" + sid + ")"); // auxiliary "Skolem" constant
			createClassTautologies(auxid); // TODO: is this needed?
			storage.makePredicateAssertion("sv",sid,pid,auxid);
			storage.makePredicateAssertion("sco",auxid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectAllValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
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
			createClassTautologies(oid);
			storage.makePredicateAssertion("subsomevalues",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(valuekey);
			createClassTautologies(sid);
			storage.makePredicateAssertion("sv",sid,pid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visit(OWLObjectHasSelf ce) {
		String propkey = ce.getProperty().accept(this);
		if (propkey == null) return null;
		String result = makeNAryExpressionKey(OP_OBJECT_SELF, propkey);
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int pid = storage.getID(propkey);
			int id = storage.getID(result);
			createClassTautologies(id);
			if (action == Action.WRITEBODY) {
				storage.makePredicateAssertion("subself",pid,id);
			} else {
				storage.makePredicateAssertion("self",id,pid);
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
			createClassTautologies(oid);
			storage.makePredicateAssertion("dsubsomevalues",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			// TODO Currently not supported 
			result = null;
//			int sid = storage.getID(result);
//			int pid = storage.getID(propkey);
//			int oid = storage.getID(fillkey);
//			createClassTautologies(sid);
//			int auxid = storage.getID("C(" + sid + ")"); // auxiliary "Skolem" constant
//			createClassTautologies(auxid); // TODO: is this needed?
//			storage.makePredicateAssertion("dsv",sid,pid,auxid);
//			storage.makePredicateAssertion("dsco",auxid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
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
			createClassTautologies(oid);
			storage.makePredicateAssertion("dsubsomevalues",pid,sid,oid);
		} else if (action == Action.WRITEHEAD) {
			int sid = storage.getID(result);
			int pid = storage.getID(propkey);
			int oid = storage.getID(valuekey);
			createClassTautologies(sid);
			storage.makePredicateAssertion("dsv",sid,pid,oid);
		}
		return result;
	}

	@Override
	public String visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String visit(OWLObjectProperty property) {
		if ( property.isOWLTopObjectProperty() || property.isOWLBottomObjectProperty() ) {
			return null;
		}
		String result = property.toString();
		int id = storage.getID(result); 
		storage.makePredicateAssertion("spo",id,id);
		return result;
	}

	@Override
	public String visit(OWLObjectInverseOf property) {
		// TODO Currently not supported
		return null;
	}

	@Override
	public String visit(OWLDataProperty property) {
		if ( property.isOWLTopDataProperty() || property.isOWLBottomDataProperty() ) {
			return null;
		}
		String result = property.toString();
		int id = storage.getID(result); 
		storage.makePredicateAssertion("dspo",id,id);
		return result;
	}
	
	@Override
	public String visit(OWLNamedIndividual individual) {
		String result = makeNAryExpressionKey(OP_OBJECT_ONE_OF, individual.toString());
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result);
			createClassTautologies(id);
			storage.makePredicateAssertion("nominal",id);
			storage.makePredicateAssertion("nonempty",id);
		}
		return result;
	}

	@Override
	public String visit(OWLAnonymousIndividual individual) {
		// TODO Currently not supported
		return null;
	}

	@Override
	public String visit(OWLDatatype node) {
		// TODO Currently not supported
		return null;
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
		// TODO Currently not supported
		return null;
	}

	@Override
	public String visit(OWLTypedLiteral node) {
		return visitSimpleLiteral(Literals.makeSimpleLiteral(node));
	}

	@Override
	public String visit(OWLStringLiteral node) {
		return visitSimpleLiteral(Literals.makeSimpleLiteral(node));
	}
	
	protected String visitSimpleLiteral(SimpleLiteral sl) {
		if (sl == null) return null;
		String result = makeNAryExpressionKey(OP_DATA_ONE_OF, sl.toString());
		if ( (action == Action.WRITEBODY) || (action == Action.WRITEHEAD) ) {
			int id = storage.getID(result);
			storage.makePredicateAssertion("dnominal",id);
			storage.makePredicateAssertion("dnonempty",id);
			if (action == Action.WRITEHEAD) {
				List<String> typeuris = Literals.getDatatypeURIs(sl);
				for (int i=0; i<typeuris.size(); i++) {
					storage.makePredicateAssertion("dsco",id,storage.getIDForDatatypeURI(typeuris.get(i)));
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
	
	public void createClassTautologies(int id) {
		try {
			storage.makePredicateAssertion("sco",id,id);
			storage.makePredicateAssertion("sco",id,storage.getIDForThing());
			storage.makePredicateAssertion("sco",storage.getIDForNothing(),id);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	public String makeNAryExpressionKey(String opname, List<String> operators, int maxindex, String emptyKey) {
		assert maxindex<operators.size();
		if (maxindex<0) return emptyKey;
		String oplist = "";
		for (int i=0; i<=maxindex; i++) {
			oplist += " " + operators.get(i);
		}
		return opname + "(" + oplist + " )";
	}
	
	public String makeNAryExpressionKey(String opname, String... operators) {
		String oplist = "";
		for (int i=0; i<operators.length; i++) {
			oplist += " " + operators[i];
		}
		return opname + "(" + oplist + " )";
	}

}
