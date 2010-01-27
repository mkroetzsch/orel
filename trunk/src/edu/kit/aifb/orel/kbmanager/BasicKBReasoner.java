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

public class BasicKBReasoner {
	protected StorageDriver storage;
	protected long timerepair = 0;
	protected long timetrans = 0;
	
	public BasicKBReasoner(StorageDriver storage) {
		this.storage = storage;
	}

	protected void registerInferenceRules() {
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customizable
		rules.put("prop-1", "subpropertyof(x,z) :- subpropertyof(x,y,0), subpropertyof(y,z)");
		rules.put("prop-2", "subpropertychain(u,v2,w) :- subpropertyof(u,v1), subpropertychain(v1,v2,w)");
		rules.put("prop-3", "subpropertychain(v1,u,w) :- subpropertyof(u,v2), subpropertychain(v1,v2,w)");
		
		rules.put("trans-repair1",  "sco(x,z)  :- sco(x,y,0), sco(y,z,?,?)");
		rules.put("trans-repair2",  "sco(x,z)  :- sco(x,y,?,?), sco(y,z,?,?)");
		
		rules.put("del-ref",  "-sco(x,x) :- sco(x,x)");
		
		rules.put("trans",  "sco(x,z)  :- sco(x,y,0), sco(y,z)");
		rules.put("E",      "sco(x,z)  :- subconjunctionof(y1,y2,z), sco(x,y1), sco(x,y2)");
		rules.put("E ref1", "sco(x,z)  :- subconjunctionof(x,y,z), sco(x,y)");
		rules.put("E ref2", "sco(x,z)  :- subconjunctionof(y,x,z), sco(x,y)");
		rules.put("E ref3", "sco(x,z)  :- subconjunctionof(x,x,z)");
		rules.put("F",      "sco(x,y)  :- sv(x,v,z), subsomevalues(v,z,y)");
		rules.put("G",      "sco(x,y)  :- sv(x,v,z), subpropertyof(v,u), subsomevalues(u,z,y)");
		rules.put("Hn",     "sv(x,w,z) :- sv(x,v1,y), sv(y,v2,z), subpropertychain(v1,v2,w)");
		//rules.put("I",      "sv(x,v,z) :- sco(x,y), sv(y,v,z)");
		rules.put("Jn",     "sv(x,v,z) :- sv(x,v,y), sco(y,z)");
		
		rules.put("Nom 1n", "sco(y,x) :- sco(x,y), nonempty(x), nominal(y)");
		rules.put("Nom 2n", "nonempty(y) :- sco(x,y), nonempty(x)");
		rules.put("Nom 3n", "nonempty(y) :- sv(x,v,y), nonempty(x)");
		
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
		System.out.println("Materialising property hierarchy ... ");
		materializePropertyHierarchy();
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
	
		sTime=System.currentTimeMillis();
		System.out.println("Starting iterative materialisation ... ");
		int affectedrows, auxarows;
		int maxstep=0,curstep_scotra=0,curstep_sco=0,curstep_nonsco=0, auxcurstep;
		while ( (maxstep>=curstep_scotra) || (maxstep>=curstep_sco) || (maxstep>=curstep_nonsco) ) {
			System.out.println("###");
			if (maxstep>=curstep_scotra) {
				System.out.println(" Materialising transitivity for step " + curstep_scotra + "... ");
				maxstep = materializeSubclassOfTransitivity(curstep_scotra);
				curstep_scotra = maxstep + 1; // for now we are done; only future results will matter to scotra
				System.out.println(" Done.");
			} else if (maxstep>=curstep_sco) {
				System.out.println(" Applying remaining SCO rules to results " + curstep_sco + " to " + maxstep + " ...");
				auxcurstep = maxstep+1; // remember where first bunch of results in this step was put
				affectedrows = storage.runRule("F",curstep_sco,maxstep);
				affectedrows = affectedrows + storage.runRule("G",curstep_sco,maxstep);
				// note that rules F and G cannot be affected by sco derivations, hence they are not iterated here 
				System.out.println(" Applying Rule E iteratively ... ");
				if (affectedrows > 0) maxstep++; // don't forget new scos rule E; they will no longer be new when we come here next time!
				auxarows = 1;
				while (auxarows > 0) {
					auxarows = storage.runRule("E",curstep_sco,maxstep) +
					           storage.runRule("E ref1",curstep_sco,maxstep) +
					           storage.runRule("E ref2",curstep_sco,maxstep) +
					           storage.runRule("E ref3",curstep_sco,maxstep);
					affectedrows = affectedrows + auxarows;
					curstep_sco = maxstep+1; // executed at least once, so we set this value even if no rules applied
					if (auxarows > 0 ) maxstep++;
				}
				if (affectedrows > 0) { // new sconl statements; update result of transitivity materialisation
					System.out.println(" Number of rows affected in above rules: " + affectedrows + ". Starting sco repair for steps " + auxcurstep + " to " + curstep_sco + " ... ");
					// Before moving the new facts downwards to become base facts, make sure that the remaining rules have seen them:
					affectedrows = storage.runRule("Hn",curstep_nonsco,curstep_sco);
					affectedrows = affectedrows + storage.runRule("Jn",curstep_nonsco,curstep_sco);
					curstep_nonsco = curstep_sco+1;
					// now do the actual repair (and increase of maxstep)
					maxstep = repairMaterializeSubclassOfTransitivity(auxcurstep,curstep_sco); // always increases step counter
					curstep_scotra = maxstep; // scotra can continue here
					System.out.println(" Done.");
				}
			} else { // this implies (maxstep>=curstep_nonsco)
				System.out.println(" Applying remaining non-SCO rules to results " + curstep_nonsco + " to " + maxstep + " ...");
				affectedrows = storage.runRule("Hn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + storage.runRule("Jn",curstep_nonsco,maxstep);
				curstep_nonsco = maxstep+1;
				if (affectedrows > 0) { // some other new statements, just increase step counter directly
					System.out.println(" Number of rows affected in above rules: " + affectedrows + ". Continue iteration.");
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
		}
		timetrans = timetrans + System.currentTimeMillis() - starttime;
		return curstep-1;
	}

	/**
	 * Materialize additional consequences of transitivity of subclassOf that would have been 
	 * obtained up to this step if all sco facts that have been inserted at the given step would have 
	 * been available as base facts. The operation performs enough steps to ensure that all those
	 * conclusions are obtained, so that the normal materialization can continue at the returned step
	 * value. The operation does not continue until staturation of the sco table w.r.t. transitivity
	 * -- it just restores the assumed completeness of facts that are found in sco up to step. 
	 * 
	 * After this "repair" operation, all facts of level -1 so as to be taken into account for future
	 * applications the transitivity rule.
	 * @param step
	 * @return new step counter
	 * @throws SQLException
	 */
	protected int repairMaterializeSubclassOfTransitivity(int min_cur_step, int max_cur_step) throws Exception {
		long starttime = System.currentTimeMillis();
		// repeat all sco transitivity iterations that happened so far, but only recompute results that
		// rely on the newly added facts
		//System.out.print("    ");
		int affectedrows, min_cstep=min_cur_step, max_cstep=max_cur_step;
		int[] params1 = new int[2];
		int[] params2 = new int[4];
		params2[0] = min_cur_step;
		params2[1] = max_cur_step;
		boolean prevaffected = true;
		for (int i=0; i<min_cur_step; i++) {
			// join new base facts with old level i facts:
			if (i == 0)	params2[2] = -1; else params2[2] = i; // include -1 for base case 
			params2[3] = i;
			affectedrows = storage.runRule("trans-repair2", max_cstep+1, params2 );
			if (prevaffected) {
				// joins with new level i facts only needed if new level i facts were added:
				params2[2] = min_cstep;
				params2[3] = max_cstep;
				affectedrows = affectedrows + storage.runRule("trans-repair2" , max_cstep+1, params2 );
				params1[0] = min_cstep;
				params1[1] = max_cstep;
				affectedrows = affectedrows + storage.runRule("trans-repair1" , max_cstep+1, params1 );
			}
			prevaffected = (affectedrows > 0);
			if (prevaffected) {
				max_cstep++;
				min_cstep = max_cstep;
				//System.out.print("(" + i + ":" + affectedrows + ")");
			} else {
				//System.out.print(".");
			}
		}
		//System.out.println(" Done.");
		// move the new facts down to the base level
		for (int i=min_cur_step; i<=max_cur_step; i++) {
			storage.changeStep("sco",i,-1);
		}
		timerepair = timerepair + System.currentTimeMillis() - starttime;
		return max_cstep+1;
	}
	
}
