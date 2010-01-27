package edu.kit.aifb.orel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateAtom;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateTerm;

/**
 * Basic class to manage initialization, writing, and query answering from a
 * database.
 * 
 * @author Markus Krötzsch
 */
public class BasicStore {
	protected Connection con = null;
	protected BasicStoreDBBridge bridge = null;
	
	protected long timeRuleA = 0;
	protected long timeRuleB = 0;
	protected long timeRuleC = 0;
	protected long timeRuleD = 0;
	protected long timeRuleE = 0;
	protected long timeRuleF = 0;
	protected long timeRuleG = 0;
	protected long timeRuleH = 0;
	protected long timeRuleI = 0;
	protected long timeRuleJ = 0;
	protected long timeRuleK = 0;
	protected long timeRuleL = 0;
	protected long timeRuleM = 0;
	protected long timeRuleN = 0;
	protected long timeRuleO = 0;
	protected long timeRuleP = 0;
	protected long timeRuleQ = 0;
	protected long timeRuleR = 0;
	protected long timeRuleS = 0;
	protected long timerepair = 0;
	protected long timetrans = 0;

	
	/**
	 * Constructor that also establishes a database connection, since this object cannot really work without a database.
	 * @param dbserver
	 * @param dbname
	 * @param dbuser
	 * @param dbpwd
	 * @throws Exception
	 */
	public BasicStore(String dbserver, String dbname, String dbuser, String dbpwd) throws Exception {
		connect(dbserver,dbname,dbuser,dbpwd);
	}

	/**
	 * Ensure that the DB has the right tables, creating them if necessary.
	 */
	public void initialize() throws SQLException {
		getStoreBridge().initialize();
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws SQLException {
		getStoreBridge().drop();
	}
	
	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clearDatabase(boolean onlyderived) throws SQLException {
		getStoreBridge().clear(onlyderived);
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 */
	public void loadOntology(OWLOntology ontology) throws SQLException {
		loadOntology(ontology, false);
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 * @param donotassert if true then only load the relevant subexpressions without asserting the axioms 
	 */
	@SuppressWarnings("unchecked")
	public void loadOntology(OWLOntology ontology, boolean donotassert) throws SQLException {
		// prepare DB for bulk insert:
		con.setAutoCommit(false);
		//con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		//con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		enableKeys(false);
		getStoreBridge(); // initialize bridge

		// iterate over ontology to load all axioms:
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int count = 0;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				loadSubclassOf(((OWLSubClassOfAxiom) axiom).getSubClass(), ((OWLSubClassOfAxiom) axiom).getSuperClass(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				loadEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getClassExpressions(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				loadSubpropertyOf(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty(), ((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				loadSubpropertyChainOf(((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain(), ((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty(),donotassert);
			} else if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
				loadClassAssertion( ((OWLClassAssertionAxiom) axiom).getIndividual(), ((OWLClassAssertionAxiom) axiom).getClassExpression(), donotassert); 
			} else if (axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION) {
				OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual> pa = ((OWLPropertyAssertionAxiom<OWLObjectProperty,OWLIndividual>) axiom);
				loadPropertyAssertion( pa.getSubject(), pa.getProperty(), pa.getObject(), donotassert); 
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		System.out.println(" loaded " + count + " axioms.");
		// close, commit, and recompute indexes
		closeStoreBridge();
		con.setAutoCommit(true);
		enableKeys(true);
	}

	/**
	 * Check if the given ontology is entailed by the loaded axioms (return true or false).
	 * Unsupported axioms will be ignored, and the result will be as if they had not been given.   
	 * @param ontology
	 */
	@SuppressWarnings("unchecked")
	public boolean checkEntailment(OWLOntology ontology) throws SQLException {
		loadOntology(ontology,true);
		materialize();
		// (bridge was closed earlier; maybe do this more efficiently)
		getStoreBridge();
		// now check entailment of all axioms
		Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int id1,id2;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				id1 = bridge.getID(((OWLSubClassOfAxiom) axiom).getSubClass());
				id2 = bridge.getID(((OWLSubClassOfAxiom) axiom).getSuperClass());
				if ( (id1 != id2) && (!bridge.checkIdsInTable("sco",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				Object[] descs = ((OWLEquivalentClassesAxiom)axiom).getClassExpressions().toArray();
				int j;
				for(int i=0;i<descs.length;i++){
					j=(i%(descs.length-1))+1;
					id1 = bridge.getID((OWLClassExpression)descs[i]);
					id2 = bridge.getID((OWLClassExpression)descs[j]);
					if ( (id1 != id2) && (!bridge.checkIdsInTable("sco",id1,id2)) ) return false;
				}
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				id1 = bridge.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty());
				id2 = bridge.getID(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty());
				if ( (id1 != id2) && (!bridge.checkIdsInTable("subpropertyof",id1,id2)) ) return false;
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				List<OWLObjectPropertyExpression> chain = ((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain();
				if (chain.size() == 2) {
					id1 = bridge.getID(chain.get(0));
					id2 = bridge.getID(chain.get(1));
					if (!bridge.checkIdsInTable("subpropertychain",id1,id2,bridge.getID(((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty()))) return false;
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
	 * Compute all materialized statements on the database.
	 */
	public void materialize() throws SQLException {
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
				affectedrows = bridge.runRule("F",curstep_sco,maxstep);
				affectedrows = affectedrows + bridge.runRule("G",curstep_sco,maxstep);
				System.out.println("  Applying Rule E iteratively ... ");
				auxarows = 1;
				while (auxarows > 0) {
					auxarows = bridge.runRule("E",curstep_sco,maxstep) +
					           bridge.runRule("E ref1",curstep_sco,maxstep) +
					           bridge.runRule("E ref2",curstep_sco,maxstep) +
					           bridge.runRule("E ref3",curstep_sco,maxstep);
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
				affectedrows = bridge.runRule("Hn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Hl",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("I",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Jn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Jl",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("K",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("L",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("M",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("N",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("O",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("P",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Qn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Ql",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("P",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Sn",curstep_nonsco,maxstep);
				affectedrows = affectedrows + bridge.runRule("Sl",curstep_nonsco,maxstep);
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
		/*System.out.println("Rule D: " + timeRuleD);
		System.out.println("Rule E: " + timeRuleE);
		System.out.println("Rule F: " + timeRuleF);
		System.out.println("Rule G: " + timeRuleG);
		System.out.println("Rule H: " + timeRuleH);
		System.out.println("Rule I: " + timeRuleI);
		System.out.println("Rule J: " + timeRuleJ);
		System.out.println("Rule K: " + timeRuleK);
		System.out.println("Rule L: " + timeRuleL);
		System.out.println("Rule M: " + timeRuleM);
		System.out.println("Rule N: " + timeRuleN);
		System.out.println("Rule O: " + timeRuleO);
		System.out.println("Rule P: " + timeRuleP);
		System.out.println("Rule Q: " + timeRuleQ);
		System.out.println("Rule R: " + timeRuleR);
		System.out.println("Rule S: " + timeRuleS);*/
		System.out.println("Sco transitivity materialisation total: " + timetrans);
		System.out.println("Sco repair total: " + timerepair);
	}

	/**
	 * Establish a connection to the database.
	 * @param dbserver
	 * @param dbname
	 * @param dbuser
	 * @param dbpwd
	 * @throws Exception
	 */
	protected void connect(String dbserver, String dbname, String dbuser, String dbpwd) throws SQLException {
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            con = DriverManager.getConnection("jdbc:mysql://" + dbserver + "/" + dbname + "?user=" + dbuser + "&password=" + dbpwd + "&rewriteBatchedStatements=true");
        } catch (SQLException e) { // TODO either do something useful or drop this catch block
            throw e;
        } catch (ClassNotFoundException e) {
        	System.err.println("Database driver not found:\n");
        	System.err.println(e.toString());
        }
	}

	protected void loadSubclassOf(OWLClassExpression c1, OWLClassExpression c2, boolean donotassert) throws SQLException {
		//System.err.println("Calling subclass of.");
		int id1 = bridge.getID(c1);
		int id2 = bridge.getID(c2);
		if (donotassert == false) {
			bridge.insertIdsToTable("sco",id1,id2);
		}
		createBodyFacts(id1,c1);
		createHeadFacts(id2,c2);
	}

	protected void loadEquivalentClasses(Set<OWLClassExpression> descriptions, boolean donotassert) throws SQLException {
		Object[] descs = descriptions.toArray();
		int j;
		for(int i=0;i<descs.length;i++){
			j=(i%(descs.length-1))+1;
			loadSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j], donotassert);
		}
	}

	protected void loadSubpropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2, boolean donotassert) throws SQLException {
		if (donotassert) return;
		int pid1 = bridge.getID(p1), pid2 = bridge.getID(p2);
		bridge.insertIdsToTable("subpropertyof", pid1, pid2);
	}
	
	protected void loadSubpropertyChainOf(List<OWLObjectPropertyExpression> chain, OWLObjectPropertyExpression p, boolean donotassert) throws SQLException {
		if (chain.size() == 2) {
			if (donotassert) return;
			int pid = bridge.getID(p), pid1 = bridge.getID(chain.get(0)), pid2 = bridge.getID(chain.get(1));
			bridge.insertIdsToTable("subpropertychain", pid1, pid2, pid);
		} else {
			// TODO recursion (even if donotassert==true we need to assert the subchains here)
		}
	}

	protected void loadClassAssertion(OWLIndividual i, OWLClassExpression c, boolean donotassert) throws SQLException {
		//System.err.println("Calling subclass of.");
		int id1 = bridge.getID(i);
		int id2 = bridge.getID(c);
		if (donotassert == false) {
			bridge.insertIdsToTable("sco",id1,id2);
		}
		createBodyFacts(id1,i);
		createHeadFacts(id2,c);
	}

	protected void loadPropertyAssertion(OWLIndividual s, OWLObjectProperty p, OWLIndividual o, boolean donotassert) throws SQLException {
		//System.err.println("Calling subclass of.");
		int sid = bridge.getID(s);
		int pid = bridge.getID(p);
		int oid = bridge.getID(o);
		if (donotassert == false) {
			bridge.insertIdsToTable("sv",sid,pid,oid);
		}
		createBodyFacts(sid,s);
		createHeadFacts(oid,o);
	}
	
	protected void createBodyFacts(int id, OWLIndividual i) throws SQLException {
		bridge.insertIdsToTable("nominal",id);
		bridge.insertIdsToTable("nonempty",id);
	}
	
	protected void createBodyFacts(int id, OWLClassExpression d) throws SQLException {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			createConjunctionBodyFacts(id, ((OWLObjectIntersectionOf) d).getOperands().toArray());
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = bridge.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = bridge.getID(filler);
			bridge.insertIdsToTable("subsomevalues",pid,sid,id);
			createBodyFacts(sid,filler);
		} else {// TODO: add more description types
			System.err.println("Unsupported body class expression: " + d.toString());
		}
	}
	
	protected void createConjunctionBodyFacts(int id, Object[] ops) throws SQLException {
		// TODO maybe sort ops first to increase likeliness of finding the same sub-ops again
		if (ops.length <= 0) return;
		int oid1 = bridge.getID((OWLClassExpression)ops[0]);
		createBodyFacts(oid1,(OWLClassExpression)ops[0]);
		if (ops.length == 2) {
			int oid2 = bridge.getID((OWLClassExpression)ops[1]);
			bridge.insertIdsToTable("subconjunctionof",oid1,oid2,id);
			createBodyFacts(oid2,(OWLClassExpression)ops[1]);
		} else { // recursion
			String opsidstring = "IntersectionOf(";
			Object[] newops = new Object[ops.length-1];
			for (int i=1; i<ops.length; i++) {
				opsidstring = new String(opsidstring + " " + ops[i].toString());
				newops[i-1] = ops[i];
			}
			opsidstring = opsidstring + ')';
			int oid2 = bridge.getID(opsidstring);
			bridge.insertIdsToTable("subconjunctionof",oid1,oid2,id);
			createConjunctionBodyFacts(oid2,newops);
		}
	}

	protected void createHeadFacts(int id, OWLIndividual i) throws SQLException {
		bridge.insertIdsToTable("nominal",id);
		bridge.insertIdsToTable("nonempty",id);
	}

	protected void createHeadFacts(int sid, OWLClassExpression d) throws SQLException {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf){
			Iterator<OWLClassExpression> descit = ((OWLObjectIntersectionOf)d).getOperands().iterator();
			OWLClassExpression desc;
			int descid;
			while (descit.hasNext()) {
				desc = descit.next();
				descid = bridge.getID(desc);
				bridge.insertIdsToTable("sco",sid,descid);
				createHeadFacts(descid,desc);
			}
		} else if (d instanceof OWLObjectSomeValuesFrom){
			int pid = bridge.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int oid = bridge.getID(filler);
			bridge.insertIdsToTable("sv",sid,pid,oid);
			createHeadFacts(oid,filler);
		} else {// TODO: add more description types
			System.err.println("Unsupported head class expression: " + d.toString());
		}
	}
	
	protected void enableKeys(boolean ek) throws SQLException {
		Statement stmt = con.createStatement();
		String action;
		if (ek) {
			action = "ENABLE KEYS";
		} else {
			action = "DISABLE KEYS";
		}
		stmt.execute("ALTER TABLE sco " + action);
		stmt.execute("ALTER TABLE sv " + action);
		stmt.execute("ALTER TABLE subconjunctionof " + action);
		stmt.execute("ALTER TABLE subpropertychain " + action);
		stmt.execute("ALTER TABLE subsomevalues " + action);
		stmt.execute("ALTER TABLE subpropertyof " + action);
	}
	
	/**
	 * Method for separating leaf classes from other classes in the sco and sv table.
	 * The operation is idempotent.
	 * @throws SQLException
	 */
	protected void separateLeafs() throws SQLException {
		int affectedrows;
		Statement stmt = con.createStatement();
		// do not perform the leaf distinction now
		affectedrows = stmt.executeUpdate("INSERT IGNORE INTO sco_nl SELECT * FROM sco");
		if (affectedrows > 0) {
			stmt.executeUpdate("TRUNCATE TABLE sco");
		}
		affectedrows = stmt.executeUpdate("INSERT IGNORE INTO sv_nl SELECT * FROM sv");
		if (affectedrows > 0) {
			stmt.executeUpdate("TRUNCATE TABLE sv");
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
	
	protected void materializePropertyHierarchy() throws SQLException {
		int i = 1;
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = bridge.runRule("prop-1", i-1, i-1);
			i++;
		}
		bridge.runRule("prop-2", i);
		bridge.runRule("prop-3", i);
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
	protected int materializeSubclassOfTransitivity(int curstep) throws SQLException {
		long starttime = System.currentTimeMillis();
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = bridge.runRule("trans",curstep,curstep);
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
	protected int repairMaterializeSubclassOfTransitivity(int step) throws SQLException {
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
			affectedrows = bridge.runRule("trans_repair2" , curstep+1, params2 );
			///rule_D_repair.executeUpdate();
			if (prevaffected) {
				// joins with new level i facts only needed if new level i facts were added:
				params2[0] = step;
				params2[1] = curstep;
				affectedrows = affectedrows + bridge.runRule("trans_repair2" , curstep+1, params2 );
				params1[0] = i;
				affectedrows = affectedrows + bridge.runRule("trans_repair1" , curstep+1, params1 );
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
		final PreparedStatement addNewSCOFacts = con.prepareStatement(
				"UPDATE sco_nl SET step=\"-1\" WHERE step=?"
		);
		addNewSCOFacts.setInt(1, step);
		addNewSCOFacts.executeUpdate();
		step = curstep;
		timerepair = timerepair + System.currentTimeMillis() - starttime;
		return step;
	}


	protected BasicStoreDBBridge getStoreBridge() {
		if (bridge == null) {
			bridge = new BasicStoreDBBridge(con);
			bridge.registerPredicate( new PredicateDeclaration("sco",2,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("sco_nl",2,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("sv",3,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("sv_nl",3,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("subconjunctionof",3,false,false) );
			bridge.registerPredicate( new PredicateDeclaration("subconint",3,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("subsomevalues",3,false,false) );
			bridge.registerPredicate( new PredicateDeclaration("subpropertychain",3,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("subpropertyof",2,true,false) );
			bridge.registerPredicate( new PredicateDeclaration("nominal",1,false,false) );
			bridge.registerPredicate( new PredicateDeclaration("nonempty",1,true,false) );
		}
		return bridge;
	}

	protected void registerInferenceRules() {
		getStoreBridge(); // make sure we have the bridge
		HashMap<String,String> rules = new HashMap<String,String>();
		// make the rule declaration as readable as possible;
		// it is crucial to have this error free and customisable
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
			bridge.registerInferenceRule(InferenceRuleDeclaration.buildFromString(name,rules.get(name)));
		}
	}
	
	public void closeStoreBridge() throws SQLException {
		if (bridge != null) {
			bridge.close();
			bridge = null;
		}
	}

}
