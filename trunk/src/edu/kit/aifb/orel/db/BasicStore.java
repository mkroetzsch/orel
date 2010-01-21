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
                ", INDEX(step), INDEX(s_id), INDEX(p_id), INDEX(o_id), PRIMARY KEY (s_id,p_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS sv_nl " +
				"( s_id " + idfieldtype + 
                ", p_id " + idfieldtype +
                ", o_id " + idfieldtype +
                ", step INT" +
                ", INDEX(step), INDEX(s_id), INDEX(p_id), INDEX(o_id), PRIMARY KEY (s_id,p_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subconjunctionof " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ",  INDEX(s1_id), INDEX(s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subconint " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype +
                ", step INT" +
                ", INDEX(step), INDEX(s1_id), INDEX(s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertychain " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", step INT" +
                ", INDEX(s1_id), INDEX(s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subsomevalues " +
				"( p_id " + idfieldtype + 
                ", s_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(s_id), INDEX(p_id), INDEX(o_id), PRIMARY KEY (p_id,s_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertyof " +
				"( s_id " + idfieldtype + 
                ", o_id " + idfieldtype + 
                ", step INT" +
                ", INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id) ) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS nominal " +
				"( id " + idfieldtype + ", PRIMARY KEY (id)) " + engine);
		stmt.execute("CREATE TABLE IF NOT EXISTS nonempty " +
				"( id " + idfieldtype + 
				", step INT, PRIMARY KEY (id)) " + engine);
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
		stmt.execute("DROP TABLE IF EXISTS subconint");
		stmt.execute("DROP TABLE IF EXISTS subpropertychain");
		stmt.execute("DROP TABLE IF EXISTS subsomevalues");
		stmt.execute("DROP TABLE IF EXISTS subpropertyof");
		stmt.execute("DROP TABLE IF EXISTS nonempty");
		stmt.execute("DROP TABLE IF EXISTS nominal");
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
			stmt.execute("DELETE FROM nonempty WHERE step!=0");
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
			stmt.execute("TRUNCATE TABLE nonempty");
			stmt.execute("TRUNCATE TABLE nominal");
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
				System.out.print("  Applying Rule F ... ");
				affectedrows = runRuleFsconl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule G ... ");
				affectedrows = affectedrows + runRuleGsconl(curstep_sco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule E iteratively ... ");
				auxarows = 1;
				while (auxarows > 0) {
					auxarows = runRuleEsconl(curstep_sco,maxstep);
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
				System.out.print("  Applying Rule H ... ");
				affectedrows = runRuleHsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule I ... ");
				affectedrows = affectedrows + runRuleIsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule J ... ");
				affectedrows = affectedrows + runRuleJsvnl(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule K ... ");
				affectedrows = affectedrows + runRuleKsconint(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule L ... ");
				affectedrows = affectedrows + runRuleLsconint(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule M ... ");
				affectedrows = affectedrows + runRuleMscol(curstep_nonsco,maxstep);
				System.out.println("(" + affectedrows + ")");
				System.out.print("  Applying Rule N ... ");
				affectedrows = affectedrows + runRuleNscol(curstep_nonsco,maxstep);
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
		System.out.println("Times used:");
		System.out.println("Rule D: " + timeRuleD);
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
		System.out.println("Rule S: " + timeRuleS);
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
			createConjunctionBodyFacts(id, ((OWLObjectIntersectionOf) d).getOperands().toArray());
		} else if (d instanceof OWLObjectSomeValuesFrom) {
			int pid = bridge.getID(((OWLObjectSomeValuesFrom)d).getProperty());
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)d).getFiller();
			int sid = bridge.getID(filler);
			bridge.insertIdsToTable("subsomevalues",pid,sid,id);
			createBodyFacts(sid,filler);
		} // TODO: add more description types
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
		long starttime = System.currentTimeMillis();
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = runRuleDsconl(curstep);
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
		// Rule D (joining with base old facts), tail recursive, reflexivity avoiding, usage (i,i-initialstep-1) 
		final PreparedStatement rule_D = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM sco_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND t1.step<=0 AND t2.step=? WHERE t1.s_id!=t2.o_id"
		);
		// Rule D repair (joining with new base facts), tail recursive (using new base facts instead of old ones), reflexivity avoiding, usage (i,i-initialstep-1) or (i,i-1)
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
		System.out.print("    ");
		int affectedrows, curstep=step;
		boolean prevaffected = true;
		for (int i=1; i<step; i++) {
			// join new base facts with old level i facts:
			rule_D_repair.setInt(1, curstep+1);
			rule_D_repair.setInt(2, i);
			affectedrows = rule_D_repair.executeUpdate();
			if (prevaffected) {
				// joins with new level i facts only needed if new level i facts were added:
				rule_D_repair.setInt(1, curstep+1);
				rule_D_repair.setInt(2, curstep);				
				affectedrows = affectedrows + rule_D_repair.executeUpdate();
				rule_D.setInt(1, curstep+1);
				rule_D.setInt(2, i);
				affectedrows = affectedrows + rule_D.executeUpdate();
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
		addNewSCOFacts.setInt(1, step);
		addNewSCOFacts.executeUpdate();
		step = curstep;
		timerepair = timerepair + System.currentTimeMillis() - starttime;
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
		long starttime = System.currentTimeMillis();
		// Rule D, reflexivity avoiding, tail recursive, usage (step,curstep)
		final PreparedStatement rule_D = con.prepareStatement(
				"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
				"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
				"FROM sco_nl AS t1 INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND t1.step<=0 AND t2.step=? WHERE t1.s_id!=t2.o_id"
		);
		rule_D.setInt(1, curstep+1);
		rule_D.setInt(2, curstep);
		int result = rule_D.executeUpdate();
		timeRuleD = timeRuleD + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		
		int result = rule_E_1.executeUpdate() + rule_E_2.executeUpdate() + rule_E_ref1.executeUpdate() + rule_E_ref2.executeUpdate() + rule_E_ref12.executeUpdate();
		timeRuleE = timeRuleE + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
		// Rule F, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_F = con.prepareStatement(
			"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN subsomevalues AS t2 ON t1.step>=? AND t1.step<=? AND t1.p_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_F.setInt(1, max_cur_step+1);
		rule_F.setInt(2, min_cur_step);
		rule_F.setInt(3, max_cur_step);
		int result = rule_F.executeUpdate();
		timeRuleF = timeRuleF + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
		// Rule G, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_G = con.prepareStatement(
			"INSERT IGNORE INTO sco_nl (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv_nl AS t1 INNER JOIN subpropertyof AS tp ON t1.step>=? AND t1.step<=? AND t1.p_id=tp.s_id INNER JOIN subsomevalues AS t2 ON tp.o_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_G.setInt(1, max_cur_step+1);
		rule_G.setInt(2, min_cur_step);
		rule_G.setInt(3, max_cur_step);
		int result = rule_G.executeUpdate();
		timeRuleG = timeRuleG + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_H_NL_1.executeUpdate() + rule_H_NL_2.executeUpdate() + rule_H_L_1.executeUpdate() + rule_H_L_2.executeUpdate();
		timeRuleH = timeRuleH + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_I_1.executeUpdate() + rule_I_2.executeUpdate();
		timeRuleI = timeRuleI + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_J_NL_1.executeUpdate() + rule_J_NL_2.executeUpdate() + rule_J_L_1.executeUpdate() + rule_J_L_2.executeUpdate();
		timeRuleJ = timeRuleJ + System.currentTimeMillis() - starttime;
		return result;
	}

	/**
	 * Run Rule K:
	 * subConjIntermediate(x,z,w) :- subClassOfL(x,y), subConjunctionOf(y,z,w)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleKsconint(int min_cur_step, int max_cur_step) throws SQLException {
		long starttime = System.currentTimeMillis();
		// Rule K, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_K = con.prepareStatement(
			"INSERT IGNORE INTO subconint (s1_id, s2_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s1_id, tc.s2_id AS s2_id, tc.o_id AS o_id, ? AS step " +
			"FROM sco AS t1 INNER JOIN subconjunctionof AS tc ON t1.step>=? AND t1.step<=? AND t1.o_id=tc.s2_id"
		);
		rule_K.setInt(1, max_cur_step+1);
		rule_K.setInt(2, min_cur_step);
		rule_K.setInt(3, max_cur_step);
		int result = rule_K.executeUpdate();
		timeRuleK = timeRuleK + System.currentTimeMillis() - starttime;
		return result;
	}

	/**
	 * Run Rule L:
	 * subConjIntermediate(x,z,w) :- subClassOfL(x,y), subClassOfNL(y,y1), subConjunctionOf(y1,z,w)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleLsconint(int min_cur_step, int max_cur_step) throws SQLException {
		long starttime = System.currentTimeMillis();
		// Rule L
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_L_1 = con.prepareStatement(
			"INSERT IGNORE INTO subconint (s1_id, s2_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s1_id, tc.s2_id AS s2_id, tc.o_id AS o_id, ? AS step " +
			"FROM sco AS t1 INNER JOIN sco_nl AS t2 ON t1.step>=? AND t1.step<=? AND t1.o_id=t2.s_id " +
			"INNER JOIN subconjunctionof AS tc ON t2.o_id=tc.s2_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_L_2 = con.prepareStatement(
			"INSERT IGNORE INTO subconint (s1_id, s2_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s1_id, tc.s2_id AS s2_id, tc.o_id AS o_id, ? AS step " +
			"FROM sco AS t1 INNER JOIN sco_nl AS t2 ON t1.step<? AND t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id " +
			"INNER JOIN subconjunctionof AS tc ON t2.o_id=tc.s2_id"
		);
		rule_L_1.setInt(1, max_cur_step+1);
		rule_L_1.setInt(2, min_cur_step);
		rule_L_1.setInt(3, max_cur_step);
		rule_L_2.setInt(1, max_cur_step+1);
		rule_L_2.setInt(2, min_cur_step);
		rule_L_2.setInt(3, min_cur_step);
		rule_L_2.setInt(4, max_cur_step);
		int result = rule_L_1.executeUpdate() + rule_L_2.executeUpdate();
		timeRuleL = timeRuleL + System.currentTimeMillis() - starttime;
		return result;
	}

	/**
	 * Run Rule M:
	 * subClassOfL(x,w) :- subConjIntermediate(x,z,w), subClassOfL(x,z)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleMscol(int min_cur_step, int max_cur_step) throws SQLException {
		long starttime = System.currentTimeMillis();
		// Rule M, reflexivity avoiding
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_M_1 = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, tci.o_id AS o_id, ? AS step " +
			"FROM subconint AS tci INNER JOIN sco AS t1 ON tci.step>=? AND tci.step<=? AND tci.s1_id=t1.s_id AND tci.s2_id=t1.o_id " +
			"WHERE t1.s_id!=tci.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_M_2 = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, tci.o_id AS o_id, ? AS step " +
			"FROM subconint AS tci INNER JOIN sco AS t1 ON tci.step<? AND t1.step>=? AND t1.step<=? AND tci.s1_id=t1.s_id AND tci.s2_id=t1.o_id " +
			"WHERE t1.s_id!=tci.o_id"
		);
		rule_M_1.setInt(1, max_cur_step+1);
		rule_M_1.setInt(2, min_cur_step);
		rule_M_1.setInt(3, max_cur_step);
		rule_M_2.setInt(1, max_cur_step+1);
		rule_M_2.setInt(2, min_cur_step);
		rule_M_2.setInt(3, min_cur_step);
		rule_M_2.setInt(4, max_cur_step);
		int result = rule_M_1.executeUpdate() + rule_M_2.executeUpdate();
		timeRuleM = timeRuleM + System.currentTimeMillis() - starttime;
		return result;
	}

	/**
	 * Run Rule N:
	 * subClassOfL(x,w) :- subConjIntermediate(x,z,w), subClassOfL(x,z1), subClassOfNL(z1,z)
	 * @param min_cur_step the earliest derivations not considered as "current" in this rule yet  
	 * @param max_cur_step the latest derivations before this call
	 * @return number of computed results (affected rows)
	 * @throws SQLException
	 */
	protected int runRuleNscol(int min_cur_step, int max_cur_step) throws SQLException {
		long starttime = System.currentTimeMillis();
		// Rule N, reflexivity avoiding
		// Rule 1 of semi-naive evaluation, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_N_1 = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, tci.o_id AS o_id, ? AS step " +
			"FROM subconint AS tci INNER JOIN sco AS t1 ON tci.step>=? AND tci.step<=? AND tci.s1_id=t1.s_id " +
			"INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND tci.s2_id=t2.o_id WHERE t1.s_id!=tci.o_id"
		);
		// Rule 2 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_N_2 = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, tci.o_id AS o_id, ? AS step " +
			"FROM subconint AS tci INNER JOIN sco AS t1 ON tci.step<? AND t1.step>=? AND t1.step<=? AND tci.s1_id=t1.s_id " +
			"INNER JOIN sco_nl AS t2 ON t1.o_id=t2.s_id AND tci.s2_id=t2.o_id WHERE t1.s_id!=tci.o_id"
		);
		// Rule 3 of semi-naive evaluation, usage (step,min_cur_step,min_cur_step,min_cur_step,max_cur_step)
		final PreparedStatement rule_N_3 = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, tci.o_id AS o_id, ? AS step " +
			"FROM subconint AS tci INNER JOIN sco AS t1 ON tci.step<? AND t1.step<? AND tci.s1_id=t1.s_id " +
			"INNER JOIN sco_nl AS t2 ON t2.step>=? AND t2.step<=? AND t1.o_id=t2.s_id AND tci.s2_id=t2.o_id WHERE t1.s_id!=tci.o_id"
		);
		rule_N_1.setInt(1, max_cur_step+1);
		rule_N_1.setInt(2, min_cur_step);
		rule_N_1.setInt(3, max_cur_step);
		rule_N_2.setInt(1, max_cur_step+1);
		rule_N_2.setInt(2, min_cur_step);
		rule_N_2.setInt(3, min_cur_step);
		rule_N_2.setInt(4, max_cur_step);
		rule_N_3.setInt(1, max_cur_step+1);
		rule_N_3.setInt(2, min_cur_step);
		rule_N_3.setInt(3, min_cur_step);
		rule_N_3.setInt(4, min_cur_step);
		rule_N_3.setInt(5, max_cur_step);
		int result = rule_N_1.executeUpdate() + rule_N_2.executeUpdate() + rule_N_3.executeUpdate();
		timeRuleN = timeRuleN + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
		// Rule O, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_O = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN subsomevalues AS t2 ON t1.step>=? AND t1.step<=? AND t1.p_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_O.setInt(1, max_cur_step+1);
		rule_O.setInt(2, min_cur_step);
		rule_O.setInt(3, max_cur_step);
		int result = rule_O.executeUpdate();
		timeRuleO = timeRuleO + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
		// Rule P, reflexivity avoiding, usage (step,min_cur_step,max_cur_step)
		final PreparedStatement rule_P = con.prepareStatement(
			"INSERT IGNORE INTO sco (s_id, o_id, step) " +
			"SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step " +
			"FROM sv AS t1 INNER JOIN subpropertyof AS tp ON t1.step>=? AND t1.step<=? AND t1.p_id=tp.s_id INNER JOIN subsomevalues AS t2 ON tp.o_id=t2.p_id AND t1.o_id=t2.s_id AND t1.s_id!=t2.o_id"
		);
		rule_P.setInt(1, max_cur_step+1);
		rule_P.setInt(2, min_cur_step);
		rule_P.setInt(3, max_cur_step);
		int result = rule_P.executeUpdate();
		timeRuleP = timeRuleP + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_Q_NL_1.executeUpdate() + rule_Q_NL_2.executeUpdate() + rule_Q_L_1.executeUpdate() + rule_Q_L_2.executeUpdate();
		timeRuleQ = timeRuleQ + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_R_1.executeUpdate() + rule_R_2.executeUpdate();
		timeRuleR = timeRuleR + System.currentTimeMillis() - starttime;
		return result;
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
		long starttime = System.currentTimeMillis();
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
		int result = rule_S_NL_1.executeUpdate() + rule_S_NL_2.executeUpdate() + rule_S_L_1.executeUpdate() + rule_S_L_2.executeUpdate();
		timeRuleS = timeRuleS + System.currentTimeMillis() - starttime;
		return result;
	}
	

}
