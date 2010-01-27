package edu.kit.aifb.orel.kbmanager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.storage.StorageDriver;

public class BasicLeafKBReasoner {
	protected StorageDriver storage;
	protected long timerepair = 0;
	protected long timetrans = 0;
	
	public BasicLeafKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}

	protected void registerInferenceRules() {
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable
		rules.put("move-sco", "sco_nl(x,y)  :- sco(x,y,0)");
		rules.put("move-sv",  "sv_nl(x,v,y) :- sv(x,v,y,0)");
		
		rules.put("prop-1", "subpropertyof(x,z) :- subpropertyof(x,y,0), subpropertyof(y,z)");
		rules.put("prop-2", "subpropertychain(u,v2,w) :- subpropertyof(u,v1), subpropertychain(v1,v2,w)");
		rules.put("prop-3", "subpropertychain(v1,u,w) :- subpropertyof(u,v2), subpropertychain(v1,v2,w)");
		
		rules.put("trans",  "sco_nl(x,z)  :- sco_nl(x,y,0), sco_nl(y,z)");
		rules.put("trans-repair1",  "sco_nl(x,z)  :- sco_nl(x,y,0), sco_nl(y,z,?)");
		rules.put("trans-repair2",  "sco_nl(x,z)  :- sco_nl(x,y,?), sco_nl(y,z,?)");
		rules.put("E",      "sco_nl(x,z)  :- subconjunctionof(y1,y2,z), sco_nl(x,y1), sco_nl(x,y2)");
		rules.put("E ref1", "sco_nl(x,z)  :- subconjunctionof(x,y,z), sco_nl(x,y)");
		rules.put("E ref2", "sco_nl(x,z)  :- subconjunctionof(y,x,z), sco_nl(x,y)");
		rules.put("E ref3", "sco_nl(x,z)  :- subconjunctionof(x,x,z)");
		rules.put("F",      "sco_nl(x,y)  :- sv_nl(x,v,z), subsomevalues(v,z,y)");
		rules.put("G",      "sco_nl(x,y)  :- sv_nl(x,v,z), subpropertyof(v,u), subsomevalues(u,z,y)");
		rules.put("Hn",     "sv_nl(x,w,z) :- sv_nl(x,v1,y), sv_nl(y,v2,z), subpropertychain(v1,v2,w)");
		rules.put("Hl",     "sv_nl(x,w,z) :- sv_nl(x,v1,y), sv(y,v2,z), subpropertychain(v1,v2,w)");
		rules.put("I",      "sv_nl(x,v,z) :- sco_nl(x,y), sv_nl(y,v,z)");
		rules.put("Jn",     "sv_nl(x,v,z) :- sv_nl(x,v,y), sco(y,z)");
		rules.put("Jl",     "sv_nl(x,v,z) :- sv(x,v,y), sco(y,z)");
		rules.put("K",      "subconint(x,z,w) :- sco(x,y), subconjunctionof(y,z,w)");
		rules.put("L",      "subconint(x,z,w) :- sco(x,y), sco_nl(y,y1), subconjunctionof(y1,z,w)");
		rules.put("M",      "sco(x,w)     :- subconint(x,z,w), sco(x,z)");
		rules.put("N",      "sco(x,w)     :- subconint(x,z,w), sco(x,z1), sco_nl(z1,z)");
		rules.put("O",      "sco(x,y)     :- sv(x,v,z), subsomevalues(v,z,y)");
		rules.put("P",      "sco(x,y)     :- sv(x,v,z), subpropertyof(v,u), subsomevalues(u,z,y)");
		rules.put("Qn",     "sv(x,w,z)    :- sv(x,v1,y), sv_nl(y,v2,z), subpropertychain(v1,v2,w)");
		rules.put("Ql",     "sv(x,w,z)    :- sv(x,v1,y), sv(y,v2,z), subpropertychain(v1,v2,w)");
		rules.put("R",      "sv(x,v,z)    :- sco(x,y), sv_nl(y,v,z)");
		rules.put("Sn",     "sv(x,v,z)    :- sv(x,v,y), sco_nl(y,z)");
		rules.put("Sl",     "sv(x,v,z)    :- sv(x,v,y), sco(y,z)");
		
		rules.put("Nom 1n", "sco_nl(y,x) :- sco_nl(x,y), nonempty(x), nominal(y)");
		rules.put("Nom 1l", "sco_nl(y,x) :- sco(x,y), nonempty(x), nominal(y)");
		rules.put("Nom 2n", "nonempty(y) :- sco_nl(x,y), nonempty(x)");
		rules.put("Nom 2l", "nonempty(y) :- sco(x,y), nonempty(x)");
		rules.put("Nom 3n", "nonempty(y) :- sv_nl(x,v,y), nonempty(x)");
		rules.put("Nom 3l", "nonempty(y) :- sv(x,v,y), nonempty(x)");
		
		//rules.put("test", "subconint(x,z,w) :- sco(x,y), sco_nl(y,y1), subconjunctionof(y1,z,w)");
		
		// now register those rules:
		Iterator<String> nameit = rules.keySet().iterator();
		String name;
		while (nameit.hasNext()) {
			name = nameit.next();
			storage.registerInferenceRule(InferenceRuleDeclaration.buildFromString(name,rules.get(name)));
		}
	}
	
	/**
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws Exception {
		long sTime;
		registerInferenceRules();
		
		sTime=System.currentTimeMillis();
		System.out.println("Separating leafs ... ");
		separateLeafs();
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
		
		sTime=System.currentTimeMillis();
		System.out.println("Materialising property hierarchy ... ");
		materializePropertyHierarchy();
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
	
		sTime=System.currentTimeMillis();
		System.out.println("Starting iterative materialisation ... ");
		int affectedrows, auxarows;
		int maxstep=0,curstep_scotra=0,curstep_sco=0,curstep_nonsco=0;
		while ( (maxstep>=curstep_scotra) || (maxstep>=curstep_sco) || (maxstep>=curstep_nonsco) ) {
			System.out.println("###");
			if (maxstep>=curstep_scotra) {
				System.out.println("  Materialising transitivity for step " + curstep_scotra + "... ");
				maxstep = materializeSubclassOfTransitivity(curstep_scotra);
				curstep_scotra = maxstep + 1; // for now we are done; only future results will matter to scotra
				System.out.println("  Done.");
			} else if (maxstep>=curstep_sco) {
				System.out.println("  Applying remaining SCO rules to results " + curstep_sco + " to " + maxstep + " ...");
				affectedrows = storage.runRule("F",curstep_sco,maxstep);
				affectedrows = affectedrows + storage.runRule("G",curstep_sco,maxstep);
				System.out.println("  Applying Rule E iteratively ... ");
				auxarows = 1;
				while (auxarows > 0) {
					auxarows = storage.runRule("E",curstep_sco,maxstep) +
					           storage.runRule("E ref1",curstep_sco,maxstep) +
					           storage.runRule("E ref2",curstep_sco,maxstep) +
					           storage.runRule("E ref3",curstep_sco,maxstep);
					affectedrows = affectedrows + auxarows;
					curstep_sco = maxstep+1; // executed at least once, making sure that we do set this value even if no rules applied
					if (auxarows > 0 ) maxstep = maxstep+1;
				}
				System.out.println("(" + affectedrows + ")");
				if (affectedrows > 0) { // new sconl statements; update result of transitivity materialisation
					System.out.println("  Number of rows affected in above rules: " + affectedrows + ". Starting sco repair ... ");
					maxstep = repairMaterializeSubclassOfTransitivity(maxstep+1); // always increases step counter
					curstep_scotra = maxstep; // scotra can continue here
					System.out.println("  Done.");
				}
			} else { // this implies (maxstep>=curstep_nonsco)
				System.out.println("  Applying remaining non-SCO rules to results " + curstep_nonsco + " to " + maxstep + " ...");
				affectedrows = storage.runRule("Hn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Hl",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("I",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Jn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Jl",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("K",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("L",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("M",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("N",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("O",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("P",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Qn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Ql",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("P",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Sn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Sl",curstep_nonsco,maxstep);
				curstep_nonsco = maxstep+1;
				if (affectedrows > 0) { // some other new statements, just increase step counter directly
					System.out.println("  Number of rows affected in above rules: " + affectedrows + ". Continue iteration.");
					maxstep++;
					curstep_scotra = maxstep + 1; // the rules we have here are not relevant for scotra, so move curstep for this rule
				}
			}
			
		}
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
		System.out.println("Times used:");
		System.out.println("Sco transitivity materialisation total: " + timetrans);
		System.out.println("Sco repair total: " + timerepair);
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	@SuppressWarnings("unchecked")
	public boolean checkEntailment(OWLOntology ontology) throws Exception {
		materialize();
		// now check entailment of all axioms
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int id1,id2;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				id1 = storage.getID(((OWLSubClassOfAxiom) axiom).getSubClass());
				id2 = storage.getID(((OWLSubClassOfAxiom) axiom).getSuperClass());
				if ( (id1 != id2) && (!storage.checkIdsInTable("sco",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				Object[] descs = ((OWLEquivalentClassesAxiom)axiom).getClassExpressions().toArray();
				int j;
				for(int i=0;i<descs.length;i++){
					j=(i%(descs.length-1))+1;
					id1 = storage.getID((OWLClassExpression)descs[i]);
					id2 = storage.getID((OWLClassExpression)descs[j]);
					if ( (id1 != id2) && (!storage.checkIdsInTable("sco",id1,id2)) ) return false;
				}
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				id1 = storage.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty());
				id2 = storage.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty());
				if ( (id1 != id2) && (!storage.checkIdsInTable("subpropertyof",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				List<OWLObjectPropertyExpression> chain = ((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain();
				if (chain.size() == 2) {
					id1 = storage.getID(chain.get(0));
					id2 = storage.getID(chain.get(1));
					if (!storage.checkIdsInTable("subpropertychain",id1,id2,storage.getID(((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty()))) return false;
				} else {
					//return false;
					// TODO
				}
			} else if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
				//return false;
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
		}
		return true;
	}
	
	/**
	 * Method for separating leaf classes from other classes in the sco and sv table.
	 * The operation is idempotent.
	 * @throws SQLException
	 */
	protected void separateLeafs() throws Exception {
		int affectedrows;
		
		// do not perform the leaf distinction now
		affectedrows = storage.runRule("move-sco", 0); 
		if (affectedrows > 0) {
			storage.clear("sco",false);
		}
		affectedrows = storage.runRule("move-sv", 0);
		if (affectedrows > 0) {
			storage.clear("sv",false);
		}
		// begin with the subClassOf statements:
		/*affectedrows = stmt.executeUpdate( 
			"INSERT IGNORE INTO sco_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.o_id AS o_id, \"0\" AS step " +
			"FROM sco AS t1 INNER JOIN sco AS t2 ON t1.s_id=t2.o_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sco AS t1 INNER JOIN sco AS t2 ON t1.s_id=t2.o_id");
		}
		affectedrows = stmt.executeUpdate(
			"INSERT IGNORE INTO sco_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.o_id AS o_id, \"0\" AS step FROM " +
			"sco AS t1 INNER JOIN subconjunctionof AS t2 ON t1.s_id=t2.o_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sco AS t1 INNER JOIN subconjunctionof AS t2 ON t1.s_id=t2.o_id");
		}
		affectedrows = stmt.executeUpdate(
			"INSERT IGNORE INTO sco_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.o_id AS o_id, \"0\" AS step FROM " +
			"sco AS t1 INNER JOIN subsomevalues AS t2 ON t1.s_id=t2.o_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sco AS t1 INNER JOIN subsomevalues AS t2 ON t1.s_id=t2.o_id");
		}
		// now also take care of all other property statements:
		affectedrows = stmt.executeUpdate( // take advantage of pre-computed sco leafs:
			"INSERT IGNORE INTO sv_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.p_id AS p_id,t1.o_id AS o_id, \"0\" AS step FROM " +
			"sv AS t1 INNER JOIN sco_nl AS t2 ON t1.s_id=t2.s_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sv AS t1 INNER JOIN sco_nl AS t2 ON t1.s_id=t2.s_id");
		}
		// but still check the other tables, since not all non-leafs need to occur in sco table at all:
		affectedrows = stmt.executeUpdate(
			"INSERT IGNORE INTO sv_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.p_id AS p_id,t1.o_id AS o_id, \"0\" AS step FROM " +
			"sv AS t1 INNER JOIN subconjunctionof AS t2 ON t1.s_id=t2.o_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sv AS t1 INNER JOIN subconjunctionof AS t2 ON t1.s_id=t2.o_id");
		}
		affectedrows = stmt.executeUpdate(
			"INSERT IGNORE INTO sv_nl " + 
			"SELECT DISTINCT t1.s_id AS s_id,t1.p_id AS p_id,t1.o_id AS o_id, \"0\" AS step FROM " +
			"sv AS t1 INNER JOIN subsomevalues AS t2 ON t1.s_id=t2.o_id"
		);
		if (affectedrows > 0) {
			stmt.executeUpdate("DELETE t1.* FROM sv AS t1 INNER JOIN subsomevalues AS t2 ON t1.s_id=t2.o_id");
		}*/
	}
	
	protected void materializePropertyHierarchy() throws Exception {
		int i = 1;
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = storage.runRule("prop-1", i-1, i-1);
			i++;
		}
		storage.runRule("prop-2", i);
		storage.runRule("prop-3", i);
	}

	/**
	 * Materialize all consequences of Rule D (transitivity of subclassOf) starting 
	 * from the given step counter (inserting new results after this counter). The
	 * operation performs steps until no more results are obtained, and it returns the
	 * step counter of the last new results that have been added. Especially, the method
	 * returns an unchanged step counter if no facts were added.
	 * @param curstep The inference step whose results should be considered as "new" here.
	 * @return new step counter
	 * @throws SQLException
	 */
	protected int materializeSubclassOfTransitivity(int curstep) throws Exception {
		long starttime = System.currentTimeMillis();
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = storage.runRule("trans",curstep,curstep);
			curstep++;
			System.out.println("    Updated " + affectedrows + " rows.");
		}
		timetrans = timetrans + System.currentTimeMillis() - starttime;
		return curstep-1;
	}

	/**
	 * Materialize additional consequences of Rule D (transitivity of subclassOf) that would have been 
	 * obtained up to this step if all sco_nl facts that have been inserted at the given step would have 
	 * been available as base facts. The operation performs enough steps to ensure that all those
	 * conclusions are obtained, so that the normal materialization can continue at the returned step
	 * value. The operation does not continue until staturation of the sco_nl table w.r.t. Rule D -- it
	 * just restores the assumed completeness of facts that are found in sco_nl up to step. 
	 * 
	 * After this "repair" operation, all facts of level -1 so as to be taken into account for future
	 * applications of Rule D.
	 * @param step
	 * @return new step counter
	 * @throws SQLException
	 */
	protected int repairMaterializeSubclassOfTransitivity(int step) throws Exception {
		long starttime = System.currentTimeMillis();
		// repeat all sco_nl Rule D iterations that happened so far, but only recompute results that
		// rely on the newly added facts
		System.out.print("    ");
		int affectedrows, curstep=step;
		int[] params1 = new int[1];
		int[] params2 = new int[2];
		boolean prevaffected = true;
		for (int i=1; i<step; i++) {
			// join new base facts with old level i facts:
			params2[0] = step;
			params2[1] = i;
			affectedrows = storage.runRule("trans_repair2" , curstep+1, params2 );
			///rule_D_repair.executeUpdate();
			if (prevaffected) {
				// joins with new level i facts only needed if new level i facts were added:
				params2[0] = step;
				params2[1] = curstep;
				affectedrows = affectedrows + storage.runRule("trans_repair2" , curstep+1, params2 );
				params1[0] = i;
				affectedrows = affectedrows + storage.runRule("trans_repair1" , curstep+1, params1 );
			}
			prevaffected = (affectedrows > 0);
			if (prevaffected) {
				curstep++;
				System.out.print("(" + i + ":" + affectedrows + ")");
			} else {
				System.out.print(".");
			}
		}
		System.out.println(" Done.");
		// move the new facts down to the base level
		storage.changeStep("sco_nl",step,-1);
		step = curstep;
		timerepair = timerepair + System.currentTimeMillis() - starttime;
		return step;
	}
	
}
