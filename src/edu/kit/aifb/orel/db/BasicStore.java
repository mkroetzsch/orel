package edu.kit.aifb.orel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.*;

/**
 * Basic class to manage initialization, writing, and query answering from a
 * database.
 * 
 * @author Markus Krötzsch
 */
public class BasicStore {
	protected Connection con = null;
	protected BasicStoreDBBridge bridge = null;

	
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
		Statement stmt = con.createStatement();
		//String engine = "ENGINE = InnoDB"; // tends to be slow
		String engine = "ENGINE = MyISAM";
		//String engine = "ENGINE = Memory";
		String idfieldtype = "INT NOT NULL";
		stmt.execute("CREATE TABLE IF NOT EXISTS ids " +
				"( id " + idfieldtype + " AUTO_INCREMENT" +
                ", name VARCHAR(50), PRIMARY KEY (id), INDEX(name)) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS sco " +
				"( s_id " + idfieldtype + 
				", o_id " + idfieldtype +
				", step INT" + 
				", INDEX(step), INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id)) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS sco_nl " +
				"( s_id " + idfieldtype + 
				", o_id " + idfieldtype +
				", step INT" + 
				", INDEX(step), INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id)) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS sv " +
				"( s_id " + idfieldtype + 
                ", p_id " + idfieldtype +
                ", o_id " + idfieldtype +
                ", step INT" +
                ", INDEX(step), INDEX(s_id), INDEX(p_id,o_id), PRIMARY KEY (s_id,p_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS sv_nl " +
				"( s_id " + idfieldtype + 
                ", p_id " + idfieldtype +
                ", o_id " + idfieldtype +
                ", step INT" +
                ", INDEX(step), INDEX(s_id), INDEX(p_id,o_id), PRIMARY KEY (s_id,p_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subconjunctionof " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(s1_id,s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertychain " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", step INT" +
                ", INDEX(s1_id,s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subsomevalues " +
				"( p_id " + idfieldtype + 
                ", s_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(p_id,s_id), INDEX(o_id), PRIMARY KEY (p_id,s_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertyof " +
				"( s_id " + idfieldtype + 
                ", o_id " + idfieldtype + 
                ", step INT" +
                ", INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id) ) " + engine);
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws SQLException {
		Statement stmt = con.createStatement();
		stmt.execute("DROP TABLE IF EXISTS ids");
		stmt.execute("DROP TABLE IF EXISTS sco");
		stmt.execute("DROP TABLE IF EXISTS sco_nl");
		stmt.execute("DROP TABLE IF EXISTS sv");
		stmt.execute("DROP TABLE IF EXISTS sv_nl");
		stmt.execute("DROP TABLE IF EXISTS subconjunctionof");
		stmt.execute("DROP TABLE IF EXISTS subpropertychain");
		stmt.execute("DROP TABLE IF EXISTS subsomevalues");
		stmt.execute("DROP TABLE IF EXISTS subpropertyof");
	}
	
	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clearDatabase(boolean onlyderived) throws SQLException {
		Statement stmt = con.createStatement();
		if (onlyderived == true) {
			stmt.execute("DELETE FROM sco WHERE step!=0");
			stmt.execute("DELETE FROM sco_nl WHERE step!=0");
			stmt.execute("DELETE FROM sv WHERE step!=0");
			stmt.execute("DELETE FROM sv_nl WHERE step!=0");
			stmt.execute("DELETE FROM subpropertychain WHERE step!=0");
			stmt.execute("DELETE FROM subpropertyof WHERE step!=0");
		} else {
			stmt.execute("TRUNCATE TABLE ids");
			stmt.execute("TRUNCATE TABLE sco");
			stmt.execute("TRUNCATE TABLE sco_nl");
			stmt.execute("TRUNCATE TABLE sv");
			stmt.execute("TRUNCATE TABLE sv_nl");
			stmt.execute("TRUNCATE TABLE subconjunctionof");
			stmt.execute("TRUNCATE TABLE subpropertychain");
			stmt.execute("TRUNCATE TABLE subsomevalues");
			stmt.execute("TRUNCATE TABLE subpropertyof");
		}
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 */
	@SuppressWarnings("unchecked")
	public void loadOntology(OWLOntology ontology) throws SQLException {
		// prepare DB for bulk insert:
		con.setAutoCommit(false);
		//con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		//con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		enableKeys(false);
		// find largest used id to start iteration:
		Statement stmt = con.createStatement();
		ResultSet res = stmt.executeQuery("SELECT max(id) FROM ids");
		int startid;
		if (res.next()) {
			startid = res.getInt(1) + 1;
		} else {
			startid = 1;
		}
		bridge = new BasicStoreDBBridge(con,startid);
		// iterate over ontology to load all axioms:
		java.util.Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		int count = 0;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				loadSubclassOf(((OWLSubClassOfAxiom) axiom).getSubClass(), ((OWLSubClassOfAxiom) axiom).getSuperClass());
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				loadEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getClassExpressions());
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				loadSubpropertyOf(((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSubProperty(), ((OWLSubPropertyAxiom<OWLObjectProperty>) axiom).getSuperProperty());
			} else if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF) {
				loadSubpropertyChainOf(((OWLSubPropertyChainOfAxiom) axiom).getPropertyChain(), ((OWLSubPropertyChainOfAxiom) axiom).getSuperProperty());
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
			count++;
			if (count % 100  == 0 ) System.out.print(".");
		}
		System.out.println(" loaded " + count + " axioms.");
		// close, commit, and recompute indexes
		bridge.close();
		con.setAutoCommit(true);
		enableKeys(true);
		bridge = null;
	}

	/**
	 * Compute all materialized statements on the database.  
	 */
	public void materialize() throws SQLException {
		long sTime;
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
		int affectedrows; //, newstep=0, step = -1; // start iteration
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
				System.out.print("  Applying Rule E ... ");
				affectedrows = runRuleEsconl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule F ... ");
				affectedrows = affectedrows + runRuleFsconl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule G ... ");
				affectedrows = affectedrows + runRuleGsconl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				curstep_sco = maxstep+1;
				if (affectedrows > 0) { // new sconl statements; update result of transitivity materialisation
					System.out.println("  Number of rows affected in above rules: " + affectedrows + ". Starting sco repair ... ");
					maxstep = repairMaterializeSubclassOfTransitivity(maxstep+1); // always increases step counter
					curstep_scotra = maxstep; // scotra can continue here
					System.out.println("  Done.");
				}
			} else { // this implies (maxstep>=curstep_nonsco)
				System.out.println("  Applying remaining non-SCO rules to results " + curstep_nonsco + " to " + maxstep + " ...");
				System.out.print("  Applying Rule H ... ");
				affectedrows = runRuleHsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule I ... ");
				affectedrows = affectedrows + runRuleIsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule J ... ");
				affectedrows = affectedrows + runRuleJsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule O ... ");
				affectedrows = affectedrows + runRuleOscol(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule P ... ");
				affectedrows = affectedrows + runRulePscol(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule Q ... ");
				affectedrows = affectedrows + runRuleQsvl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule R ... ");
				affectedrows = affectedrows + runRuleRsvl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule S ... ");
				affectedrows = affectedrows + runRuleSsvl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				curstep_nonsco = maxstep+1;
				if (affectedrows > 0) { // some other new statements, just increase step counter directly
					System.out.println("  Number of rows affected in above rules: " + affectedrows + ". Continue iteration.");
					maxstep++;
					curstep_scotra = maxstep + 1; // the rules we have here are not relevant for scotra, so move curstep for this rule
				}
			}
			
		}
		System.out.println("Done in " + (System.currentTimeMillis() - sTime) + "ms.");
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

	protected void loadSubclassOf(OWLClassExpression c1, OWLClassExpression c2) throws SQLException {
		//System.err.println("Calling subclass of.");
		int id1 = bridge.getID(c1);
		int id2 = bridge.getID(c2);
		bridge.insertIdsToTable("sco",id1,id2);
		createBodyFacts(id1,c1);
		createHeadFacts(id2,c2);
	}

	protected void loadEquivalentClasses(Set<OWLClassExpression> descriptions) throws SQLException {
		Object[] descs = descriptions.toArray();
		int j;
		for(int i=0;i<descs.length;i++){
			j=(i%(descs.length-1))+1;
			loadSubclassOf((OWLClassExpression)descs[i],(OWLClassExpression)descs[j]);
		}
	}

	protected void loadSubpropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) throws SQLException {
		int pid1 = bridge.getID(p1), pid2 = bridge.getID(p2);
		bridge.insertIdsToTable("subpropertyof", pid1, pid2);
	}
	
	protected void loadSubpropertyChainOf(List<OWLObjectPropertyExpression> chain, OWLObjectPropertyExpression p) throws SQLException {
		if (chain.size() == 2) {
			int pid = bridge.getID(p), pid1 = bridge.getID(chain.get(0)), pid2 = bridge.getID(chain.get(1));
			bridge.insertIdsToTable("subpropertychain", pid1, pid2, pid);
		} else {
			// TODO recursion
		}
	}
	
	protected void createBodyFacts(int id, OWLClassExpression d) throws SQLException {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			Set<OWLClassExpression> ops = ((OWLObjectIntersectionOf) d).getOperands();
			Iterator<OWLClassExpression> opsit = ops.iterator();
			// TODO maybe sort ops first to increase likeliness of finding the same sub-ops again
			if (ops.size() == 2) {
				OWLClassExpression op1 = opsit.next(), op2 = opsit.next();
				int oid1 = bridge.getID(op1), oid2 = bridge.getID(op2);
				bridge.insertIdsToTable("subconjunctionof",oid1,oid2,id);
				createBodyFacts(oid1,op1);
				createBodyFacts(oid2,op2);
			} else { // recursion
				// TODO
			}
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = bridge.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = bridge.getID(filler);
			bridge.insertIdsToTable("subsomevalues",pid,sid,id);
			createBodyFacts(sid,filler);
		} // TODO: add more description types
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
		// begin with the subClassOf statements:
		affectedrows = stmt.executeUpdate( 
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
		}
	}
	
	protected void materializePropertyHierarchy() throws SQLException {
		// Rule A: subPropertyOf(x,z) :- subPropertyOf(x,y), subPropertyOf(x,z)
		// tail recursive, usage: (i,i-1)		
		PreparedStatement rule_A = con.prepareStatement(
				"INSERT IGNORE INTO subpropertyof (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM subpropertyof AS t1 INNER JOIN subpropertyof AS t2 ON t1.o_id=t2.s_id AND t1.step=0 AND t2.step=?");
		
		int i = 1;
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			rule_A.setInt(1, i);
			rule_A.setInt(2, i-1);
			affectedrows = rule_A.executeUpdate();
			i++;
		}

		Statement stmt = con.createStatement();
		// Rule B: subPropertyChainOf(u,v2,w) :- subPropertyChainOf(v1,v2,w), subPropertyOf(u,v1)
		stmt.executeUpdate(
			"INSERT IGNORE INTO subpropertychain (s1_id, s2_id, o_id, step) " +
			"SELECT DISTINCT t2.s_id AS s1_id, t1.s2_id AS s2_id, t1.o_id AS o_id, \"1\" AS step " +
			"FROM subpropertychain AS t1 INNER JOIN subpropertyof AS t2 ON t2.o_id=t1.s1_id"
		);
		// Rule C: subPropertyChainOf(v1,u,w) :- subPropertyChainOf(v1,v2,w), subPropertyOf(u,v2)
		stmt.executeUpdate(
			"INSERT IGNORE INTO subpropertychain (s1_id, s2_id, o_id, step) " +
			"SELECT DISTINCT t1.s1_id AS s1_id, t2.s_id AS s2_id, t1.o_id AS o_id, \"1\" AS step " +
			"FROM subpropertychain AS t1 INNER JOIN subpropertyof AS t2 ON t2.o_id=t1.s2_id"
		);
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
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = runRuleDsconl(curstep);
			curstep++;
			System.out.println("    Updated " + affectedrows + " rows.");
		}
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
		// Rule D, tail recursive, reflexivity avoiding, usage (i,i-initialstep-1) 
		final PreparedStatement rule_D = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM sco_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND t1.step<=0 AND t2.step=? WHERE t1.s_id!=t2.o_id"
		);
		// Rule D repair, tail recursive (using new base facts instead of old ones), reflexivity avoiding, usage (i,i-initialstep-1) or (i,i-1)
		final PreparedStatement rule_D_repair = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM sco_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND t1.step=\"" + step + 
					"\" AND t2.step=? WHERE t1.s_id!=t2.o_id"
		);
		
		final PreparedStatement addNewSCOFacts = con.prepareStatement(
				"UPDATE sco_nl SET step=\"-1\" WHERE step=?"
		);
		
		// repeat all sco_nl Rule D iterations that happened so far, but only recompute results that
		// rely on the newly added facts
		int affectedrows = 0;
		for (int i=1; i<step; i++) {
			rule_D.setInt(1, step+i);
			rule_D.setInt(2, i);
			affectedrows = rule_D.executeUpdate();
			rule_D_repair.setInt(1, step+i);
			rule_D_repair.setInt(2, i);
			affectedrows = affectedrows + rule_D_repair.executeUpdate();
			rule_D_repair.setInt(1, step+i);
			rule_D_repair.setInt(2, step+i-1);
			affectedrows = affectedrows + rule_D_repair.executeUpdate();
			System.out.println("    Updated " + affectedrows + " rows.");
		}
		// move the new facts down to the base level
		addNewSCOFacts.setInt(1, step);
		addNewSCOFacts.executeUpdate();
		step = 2*step-1;
		return step;
	}

	/**
	 * Run Rule D:
	 * subClassOfNL(x,z) :- subClassOfNL(x,y), subClassOfNL(y,z)
	 * @param step
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleDsconl(int curstep) throws SQLException {
		// Rule D, reflexivity avoiding, tail recursive, usage (step,curstep)
		final PreparedStatement rule_D = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM sco_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND t1.step<=0 AND t2.step=? WHERE t1.s_id!=t2.o_id"
		);
		rule_D.setInt(1, curstep+1);
		rule_D.setInt(2, curstep);
		return rule_D.executeUpdate();
	}

	/**
	 * Run Rule E:
	 * subClassOfNL(x,z) :- subConjunctionOf(y1,y2,z), subClassOfNL(x,y1), subClassOfNL(x,y2)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleEsconl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule E, reflexivity avoiding, usage (i)
		// Semi-naive evaluation: do not consider all sco_nl rows, but only recently derived ("new") ones
		
		// Rule 1 of semi-naive evaluation; usage (step, min_cur_step, max_cur_step)
		final PreparedStatement rule_E_1 = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, tc.o_id AS o_id, ? AS step " +
				"FROM subconjunctionof AS tc INNER JOIN sco_nl AS t1 ON t1.step>=? AND t1.step<=? AND tc.s1_id=t1.o_id " +
				"INNER JOIN sco_nl AS t2 ON t1.s_id=t2.s_id AND t2.o_id=tc.s2_id WHERE tc.o_id!=t1.s_id"
		);
		// Rule 2 of semi-naive evaluation; usage (step, min_cur_step, min_cur_step, max_cur_step)
		final PreparedStatement rule_E_2 = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, tc.o_id AS o_id, ? AS step " +
				"FROM subconjunctionof AS tc INNER JOIN sco_nl AS t1 ON t1.step<? AND tc.s1_id=t1.o_id " +
				"INNER JOIN sco_nl AS t2 ON t2.step>=? AND t2.step<=? AND t1.s_id=t2.s_id AND t2.o_id=tc.s2_id WHERE tc.o_id!=t1.s_id"
		);
		
		// The extra ref rules to simulate reflexivity statements in sco_nl that are not stored explicitly
		// Case 1: subClassOf(x,z) :- subClassOf(x,y), subConjunctionOf(x,y,z)
		// usage (step, min_cur_step, max_cur_step)
		final PreparedStatement rule_E_ref1 = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t.s_id AS s_id, tc.o_id AS o_id, ? AS step " +
				"FROM subconjunctionof AS tc INNER JOIN sco_nl AS t ON t.step>=? AND t.step<=? AND tc.s1_id=t.o_id AND tc.s2_id=t.s_id WHERE tc.o_id!=t.s_id"
		);
		// Case 2: subClassOf(x,z) :- subClassOf(x,y), subConjunctionOf(y,x,z)
		// usage (step, min_cur_step, max_cur_step)
		final PreparedStatement rule_E_ref2 = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t.s_id AS s_id, tc.o_id AS o_id, ? AS step " +
				"FROM subconjunctionof AS tc INNER JOIN sco_nl AS t ON t.step>=? AND t.step<=? AND tc.s2_id=t.o_id AND tc.s1_id=t.s_id WHERE tc.o_id!=t.s_id"
		);
		// Case 3: subClassOf(x,z) :- subConjunctionOf(x,x,z)
		// TODO We may want to do this only once, given that there are no inferred subConjunctionOf statements to be considered
		final PreparedStatement rule_E_ref12 = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT tc.s1_id AS s_id, tc.o_id AS o_id, ? AS step " +
				"FROM subconjunctionof AS tc WHERE tc.s1_id=tc.s2_id AND tc.o_id!=tc.s1_id"
		);
		rule_E_1.setInt(1, max_cur_step+1);
		rule_E_1.setInt(2, min_cur_step);
		rule_E_1.setInt(3, max_cur_step);
		rule_E_2.setInt(1, max_cur_step+1);
		rule_E_2.setInt(2, min_cur_step);
		rule_E_2.setInt(3, min_cur_step);
		rule_E_2.setInt(4, max_cur_step);
		rule_E_ref1.setInt(1, max_cur_step+1);
		rule_E_ref1.setInt(2, min_cur_step);
		rule_E_ref1.setInt(3, max_cur_step);
		rule_E_ref2.setInt(1, max_cur_step+1);
		rule_E_ref2.setInt(2, min_cur_step);
		rule_E_ref2.setInt(3, max_cur_step);
		rule_E_ref12.setInt(1, max_cur_step+1);
		return rule_E_1.executeUpdate() + rule_E_2.executeUpdate() + rule_E_ref1.executeUpdate() + rule_E_ref2.executeUpdate() + rule_E_ref12.executeUpdate();
	}
	
	/**
	 * Run Rule F:
	 * subClassOfNL(x,y) :- someValuesOfNL(x,v,z), subSomeValuesOf(v,z,y)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleFsconl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule F, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_F = con.prepareStatement(
			"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN subsomevalues AS t2 ON t1.step>=? AND t1.step<=? AND t1.p_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_F.setInt(1, max_cur_step+1);
		rule_F.setInt(2, min_cur_step);
		rule_F.setInt(3, max_cur_step);
		return rule_F.executeUpdate();
	}
	
	/**
	 * Run Rule G:
	 * subClassOfNL(x,y) :- someValuesOfNL(x,v,z), subPropertyOf(v,u), subSomeValuesOf(u,z,y)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleGsconl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule G, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_G = con.prepareStatement(
			"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN subpropertyof AS tp ON t1.step>=? AND t1.step<=? AND t1.p_id=tp.s_id INNER JOIN subsomevalues AS t2 ON tp.o_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_G.setInt(1, max_cur_step+1);
		rule_G.setInt(2, min_cur_step);
		rule_G.setInt(3, max_cur_step);
		return rule_G.executeUpdate();
	}

	/**
	 * Run Rule H:
	 * svNL(x,w,z) :- svNL(x,v1,y), sv(y,v2,z), subPropertyChain(v1,v2,w)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleHsvnl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule H for NL case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_H_NL_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv_nl AS t1 ON t1.step>=? AND t1.step<=? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv_nl AS t2 ON t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_H_NL_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv_nl AS t1 ON t1.step<? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv_nl AS t2 ON t2.step>=? AND t2.step<=? AND t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule H for L case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_H_L_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv_nl AS t1 ON t1.step>=? AND t1.step<=? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv AS t2 ON t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_H_L_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv_nl AS t1 ON t1.step<? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv AS t2 ON t2.step>=? AND t2.step<=? AND t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_H_NL_1.setInt(1, max_cur_step+1);
		rule_H_NL_1.setInt(2, min_cur_step);
		rule_H_NL_1.setInt(3, max_cur_step);
		rule_H_NL_2.setInt(1, max_cur_step+1);
		rule_H_NL_2.setInt(2, min_cur_step);
		rule_H_NL_2.setInt(3, min_cur_step);
		rule_H_NL_2.setInt(4, max_cur_step);
		rule_H_L_1.setInt(1, max_cur_step+1);
		rule_H_L_1.setInt(2, min_cur_step);
		rule_H_L_1.setInt(3, max_cur_step);
		rule_H_L_2.setInt(1, max_cur_step+1);
		rule_H_L_2.setInt(2, min_cur_step);
		rule_H_L_2.setInt(3, min_cur_step);
		rule_H_L_2.setInt(4, max_cur_step);
		return rule_H_NL_1.executeUpdate() + rule_H_NL_2.executeUpdate() + rule_H_L_1.executeUpdate() + rule_H_L_2.executeUpdate();
	}

	/**
	 * Run Rule I:
	 * svNL(x,v,z) :- subClassOfNL(x,y), svNL(y,v,z)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleIsvnl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule I
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_I_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sco_nl AS t1 INNER JOIN sv_nl AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_I_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sco_nl AS t1 INNER JOIN sv_nl AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		rule_I_1.setInt(1, max_cur_step+1);
		rule_I_1.setInt(2, min_cur_step);
		rule_I_1.setInt(3, max_cur_step);
		rule_I_2.setInt(1, max_cur_step+1);
		rule_I_2.setInt(2, min_cur_step);
		rule_I_2.setInt(3, min_cur_step);
		rule_I_2.setInt(4, max_cur_step);
		return rule_I_1.executeUpdate() + rule_I_2.executeUpdate();
	}

	/**
	 * Run Rule J:
	 * svNL(x,v,z) :- sv(x,v,y), subClassOfNL(y,x)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleJsvnl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule J for NL case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_J_NL_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_J_NL_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule J for L case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_J_L_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN sco AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_J_L_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv_nl (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN sco AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		rule_J_NL_1.setInt(1, max_cur_step+1);
		rule_J_NL_1.setInt(2, min_cur_step);
		rule_J_NL_1.setInt(3, max_cur_step);
		rule_J_NL_2.setInt(1, max_cur_step+1);
		rule_J_NL_2.setInt(2, min_cur_step);
		rule_J_NL_2.setInt(3, min_cur_step);
		rule_J_NL_2.setInt(4, max_cur_step);
		rule_J_L_1.setInt(1, max_cur_step+1);
		rule_J_L_1.setInt(2, min_cur_step);
		rule_J_L_1.setInt(3, max_cur_step);
		rule_J_L_2.setInt(1, max_cur_step+1);
		rule_J_L_2.setInt(2, min_cur_step);
		rule_J_L_2.setInt(3, min_cur_step);
		rule_J_L_2.setInt(4, max_cur_step);
		return rule_J_NL_1.executeUpdate() + rule_J_NL_2.executeUpdate() + rule_J_L_1.executeUpdate() + rule_J_L_2.executeUpdate();
	}

	/**
	 * Run Rule O:
	 * subClassOfL(x,y) :- someValuesOfL(x,v,z), subSomeValuesOf(v,z,y)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleOscol(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule O, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_O = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN subsomevalues AS t2 ON t1.step>=? AND t1.step<=? AND t1.p_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_O.setInt(1, max_cur_step+1);
		rule_O.setInt(2, min_cur_step);
		rule_O.setInt(3, max_cur_step);
		return rule_O.executeUpdate();
	}
	
	/**
	 * Run Rule P:
	 * subClassOfL(x,y) :- someValuesOfL(x,v,z), subPropertyOf(v,u), subSomeValuesOf(u,z,y)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRulePscol(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule P, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_P = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN subpropertyof AS tp ON t1.step>=? AND t1.step<=? AND t1.p_id=tp.s_id INNER JOIN subsomevalues AS t2 ON tp.o_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_P.setInt(1, max_cur_step+1);
		rule_P.setInt(2, min_cur_step);
		rule_P.setInt(3, max_cur_step);
		return rule_P.executeUpdate();
	}

	/**
	 * Run Rule Q:
	 * svL(x,w,z) :- svL(x,v1,y), sv(y,v2,z), subPropertyChain(v1,v2,w)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleQsvl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule Q for NL case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_Q_NL_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv AS t1 ON t1.step>=? AND t1.step<=? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv_nl AS t2 ON t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_Q_NL_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv AS t1 ON t1.step<? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv_nl AS t2 ON t2.step>=? AND t2.step<=? AND t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule Q for L case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_Q_L_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv AS t1 ON t1.step>=? AND t1.step<=? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv AS t2 ON t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_Q_L_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, sc.o_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM subpropertychain AS sc INNER JOIN sv AS t1 ON t1.step<? AND t1.p_id=sc.s1_id " +
			"INNER JOIN sv AS t2 ON t2.step>=? AND t2.step<=? AND t2.s_id=t1.o_id AND t2.p_id=sc.s2_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_Q_NL_1.setInt(1, max_cur_step+1);
		rule_Q_NL_1.setInt(2, min_cur_step);
		rule_Q_NL_1.setInt(3, max_cur_step);
		rule_Q_NL_2.setInt(1, max_cur_step+1);
		rule_Q_NL_2.setInt(2, min_cur_step);
		rule_Q_NL_2.setInt(3, min_cur_step);
		rule_Q_NL_2.setInt(4, max_cur_step);
		rule_Q_L_1.setInt(1, max_cur_step+1);
		rule_Q_L_1.setInt(2, min_cur_step);
		rule_Q_L_1.setInt(3, max_cur_step);
		rule_Q_L_2.setInt(1, max_cur_step+1);
		rule_Q_L_2.setInt(2, min_cur_step);
		rule_Q_L_2.setInt(3, min_cur_step);
		rule_Q_L_2.setInt(4, max_cur_step);
		return rule_Q_NL_1.executeUpdate() + rule_Q_NL_2.executeUpdate() + rule_Q_L_1.executeUpdate() + rule_Q_L_2.executeUpdate();
	}

	/**
	 * Run Rule R:
	 * svL(x,v,z) :- subClassOfL(x,y), svNL(y,v,z)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleRsvl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule R
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_R_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sco AS t1 INNER JOIN sv_nl AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_R_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sco AS t1 INNER JOIN sv_nl AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		rule_R_1.setInt(1, max_cur_step+1);
		rule_R_1.setInt(2, min_cur_step);
		rule_R_1.setInt(3, max_cur_step);
		rule_R_2.setInt(1, max_cur_step+1);
		rule_R_2.setInt(2, min_cur_step);
		rule_R_2.setInt(3, min_cur_step);
		rule_R_2.setInt(4, max_cur_step);
		return rule_R_1.executeUpdate() + rule_R_2.executeUpdate();
	}

	/**
	 * Run Rule S:
	 * svL(x,v,z) :- svL(x,v,y), subClassOf(y,x)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleSsvl(int min_cur_step, int max_cur_step) throws SQLException {
		// Rule S for NL case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_S_NL_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN sco_nl AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_S_NL_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN sco_nl AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule S for L case
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_S_L_1 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN sco AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_S_L_2 = con.prepareStatement(
			"INSERT IGNORE INTO sv (s_id, p_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t1.p_id AS p_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN sco AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id"
		);
		rule_S_NL_1.setInt(1, max_cur_step+1);
		rule_S_NL_1.setInt(2, min_cur_step);
		rule_S_NL_1.setInt(3, max_cur_step);
		rule_S_NL_2.setInt(1, max_cur_step+1);
		rule_S_NL_2.setInt(2, min_cur_step);
		rule_S_NL_2.setInt(3, min_cur_step);
		rule_S_NL_2.setInt(4, max_cur_step);
		rule_S_L_1.setInt(1, max_cur_step+1);
		rule_S_L_1.setInt(2, min_cur_step);
		rule_S_L_1.setInt(3, max_cur_step);
		rule_S_L_2.setInt(1, max_cur_step+1);
		rule_S_L_2.setInt(2, min_cur_step);
		rule_S_L_2.setInt(3, min_cur_step);
		rule_S_L_2.setInt(4, max_cur_step);
		return rule_S_NL_1.executeUpdate() + rule_S_NL_2.executeUpdate() + rule_S_L_1.executeUpdate() + rule_S_L_2.executeUpdate();
	}
	

}
