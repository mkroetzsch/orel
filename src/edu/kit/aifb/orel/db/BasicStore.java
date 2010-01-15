package edu.kit.aifb.orel.db;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.*;

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
		String idfieldtype = "INT NOT NULL";
		stmt.execute("CREATE TABLE IF NOT EXISTS ids " +
				"( id " + idfieldtype + " AUTO_INCREMENT" +
                ", name VARCHAR(255), PRIMARY KEY (id), INDEX(name))");
		stmt.execute("CREATE TABLE IF NOT EXISTS sco " +
				"( s_id " + idfieldtype + 
				", o_id " + idfieldtype +
				", step INT" + 
				", INDEX(step), INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id))");
		stmt.execute("CREATE TABLE IF NOT EXISTS sv " +
				"( s_id " + idfieldtype + 
                ", p_id " + idfieldtype +
                ", o_id " + idfieldtype +
                ", step INT" +
                ", INDEX(step), INDEX(s_id), INDEX(p_id,o_id), PRIMARY KEY (s_id,p_id,o_id) )");
		stmt.execute("CREATE TABLE IF NOT EXISTS subconjunctionof " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(s1_id,s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) )");
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertychain " +
				"( s1_id " + idfieldtype + 
                ", s2_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(s1_id,s2_id), INDEX(o_id), PRIMARY KEY (s1_id,s2_id,o_id) )");
		stmt.execute("CREATE TABLE IF NOT EXISTS subsomevalues " +
				"( p_id " + idfieldtype + 
                ", s_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(p_id,s_id), INDEX(o_id), PRIMARY KEY (p_id,s_id,o_id) )");
		stmt.execute("CREATE TABLE IF NOT EXISTS subpropertyof " +
				"( s_id " + idfieldtype + 
                ", o_id " + idfieldtype + 
                ", INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id) )");
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws SQLException {
		Statement stmt = con.createStatement();
		stmt.execute("DROP TABLE IF EXISTS ids");
		stmt.execute("DROP TABLE IF EXISTS sco");
		stmt.execute("DROP TABLE IF EXISTS sv");
		stmt.execute("DROP TABLE IF EXISTS subconjunctionof");
		stmt.execute("DROP TABLE IF EXISTS subpropertychain");
		stmt.execute("DROP TABLE IF EXISTS subsomevalues");
		stmt.execute("DROP TABLE IF EXISTS subpropertyof");
	}
	
	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clearDatabase() throws SQLException {
		Statement stmt = con.createStatement();
		stmt.execute("TRUNCATE TABLE ids");
		stmt.execute("TRUNCATE TABLE sco");
		stmt.execute("TRUNCATE TABLE sv");
		stmt.execute("TRUNCATE TABLE subconjunctionof");
		stmt.execute("TRUNCATE TABLE subpropertychain");
		stmt.execute("TRUNCATE TABLE subsomevalues");
		stmt.execute("TRUNCATE TABLE subpropertyof");
	}
	
	/**
	 * Load the content of some ontology to the database.   
	 * @param ontology
	 */
	public void loadOntology(OWLOntology ontology) throws SQLException {
		java.util.Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		bridge = new BasicStoreDBBridge(con);
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				loadSubclassOf(((OWLSubClassAxiom) axiom).getSubClass(), ((OWLSubClassAxiom) axiom).getSuperClass());
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				loadEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getDescriptions());
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				loadSubpropertyOf(((OWLObjectSubPropertyAxiom) axiom).getSubProperty(), ((OWLObjectSubPropertyAxiom) axiom).getSuperProperty());
			} else if (axiom.getAxiomType() == AxiomType.PROPERTY_CHAIN_SUB_PROPERTY) {
				loadSubpropertyChainOf(((OWLObjectPropertyChainSubPropertyAxiom) axiom).getPropertyChain(), ((OWLObjectPropertyChainSubPropertyAxiom) axiom).getSuperProperty());
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
		}
		bridge.close();
		bridge = null;
	}

	/**
	 * Compute all materialized statements on the database.  
	 */
	public void materialize() throws SQLException {
		// use with (newindex, index-1, index-2)
		//PreparedStatement rule1_1 = con.prepareStatement("INSERT IGNORE INTO sco (s_id, o_id, step) SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step FROM sco AS t1 INNER JOIN sco AS t2 ON t1.o_id=t2.s_id WHERE t1.step=? AND t2.step<=?");
		// use with (newindex, index-1, index-1)
		//PreparedStatement rule1_2 = con.prepareStatement("INSERT IGNORE INTO sco (s_id, o_id, step) SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step FROM sco AS t1 INNER JOIN sco AS t2 ON t1.o_id=t2.s_id WHERE t1.step<=? AND t2.step=?");
		// use with (newindex, index-1, index-2)
		PreparedStatement rule1_1 = con.prepareStatement("INSERT IGNORE INTO sco (s_id, o_id, step) SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step FROM sco AS t1 INNER JOIN sco AS t2 ON t1.o_id=t2.s_id AND t1.step=? AND t2.step<=?");
		// use with (newindex, index-1, index-1)
		PreparedStatement rule1_2 = con.prepareStatement("INSERT IGNORE INTO sco (s_id, o_id, step) SELECT DISTINCT t1.s_id AS s_id, t2.o_id AS o_id, ? AS step FROM sco AS t1 INNER JOIN sco AS t2 ON t1.o_id=t2.s_id AND t1.step<=? AND t2.step=?");		
		
		
		int i = 1;
		int affectedrows = 1;
		while (affectedrows != 0 ) {
			affectedrows = 0;
			rule1_1.setInt(1, i);
			rule1_1.setInt(2, i-1);
			rule1_1.setInt(3, i-2);
			affectedrows = affectedrows + rule1_1.executeUpdate();
			rule1_2.setInt(1, i);
			rule1_2.setInt(2, i-1);
			rule1_2.setInt(3, i-1);
			affectedrows = affectedrows + rule1_2.executeUpdate();
			System.out.println("Updated " + affectedrows + " rows.");
			i++;
		}
		
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
            con = DriverManager.getConnection("jdbc:mysql://" + dbserver + "/" + dbname + "?user=" + dbuser + "&password=" + dbpwd);
        } catch (SQLException e) { // TODO either do something useful or drop this catch block
            throw e;
        } catch (ClassNotFoundException e) {
        	System.err.println("Database driver not found:\n");
        	System.err.println(e.toString());
        }
	}

	protected void loadSubclassOf(OWLDescription c1, OWLDescription c2) throws SQLException {
		//System.err.println("Calling subclass of.");
		int id1 = bridge.getID(c1);
		int id2 = bridge.getID(c2);
		bridge.insertIdsToTable("sco",id1,id2);
		createBodyFacts(id1,c1);
		createHeadFacts(id2,c2);
	}

	protected void loadEquivalentClasses(Set<OWLDescription> descriptions) throws SQLException {
		Object[] descs = descriptions.toArray();
		int j;
		for(int i=0;i<descs.length;i++){
			j=(i%(descs.length-1))+1;
			loadSubclassOf((OWLDescription)descs[i],(OWLDescription)descs[j]);
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
	
	protected void createBodyFacts(int id, OWLDescription d) throws SQLException {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf) {
			Set<OWLDescription> ops = ((OWLObjectIntersectionOf) d).getOperands();
			Iterator<OWLDescription> opsit = ops.iterator();
			// TODO maybe sort ops first to increase likeliness of finding the same sub-ops again
			if (ops.size() == 2) {
				OWLDescription op1 = opsit.next(), op2 = opsit.next();
				int oid1 = bridge.getID(op1), oid2 = bridge.getID(op2);
				bridge.insertIdsToTable("subconjunctionof",oid1,oid2,id);
				createBodyFacts(oid1,op1);
				createBodyFacts(oid2,op2);
			} else { // recursion
				// TODO
			}
		} else if (d instanceof OWLObjectSomeRestriction) {
			int pid = bridge.getID(((OWLObjectSomeRestriction)d).getProperty());
			OWLDescription filler = ((OWLObjectSomeRestriction)d).getFiller();
			int sid = bridge.getID(filler);
			bridge.insertIdsToTable("subsomevalues",pid,sid,id);
			createBodyFacts(sid,filler);
		} // TODO: add more description types
	}

	protected void createHeadFacts(int sid, OWLDescription d) throws SQLException {
		if (d instanceof OWLClass) {
			// nothing to do here
		} else if (d instanceof OWLObjectIntersectionOf){
			Iterator<OWLDescription> descit = ((OWLObjectIntersectionOf)d).getOperands().iterator();
			OWLDescription desc;
			int descid;
			while (descit.hasNext()) {
				desc = descit.next();
				descid = bridge.getID(desc);
				bridge.insertIdsToTable("sco",sid,descid);
				createHeadFacts(descid,desc);
			}
		} else if (d instanceof OWLObjectSomeRestriction){
			int pid = bridge.getID(((OWLObjectSomeRestriction)d).getProperty());
			OWLDescription filler = ((OWLObjectSomeRestriction)d).getFiller();
			int oid = bridge.getID(filler);
			bridge.insertIdsToTable("sv",sid,pid,oid);
			createHeadFacts(oid,filler);
		}
	}
	

}
