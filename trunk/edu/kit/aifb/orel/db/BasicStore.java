package edu.kit.aifb.orel.db;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.*;

import com.mysql.jdbc.*;



/**
 * Basic class to manage initialization, writing, and query answering from a
 * database.
 * 
 * @author Markus Krötzsch
 */
public class BasicStore {
	protected Connection con = null;
	
	// use prepared statements locally for batch updates and frequent reads
	protected PreparedStatement idsinsert = null;
	protected PreparedStatement scoinsert = null;
	protected PreparedStatement svinsert = null;
	protected PreparedStatement subconjunctionofinsert = null;
	protected PreparedStatement subpropertyofinsert = null;
	protected PreparedStatement subpropertychaininsert = null;
	protected PreparedStatement subsomevaluesinsert = null;
	protected PreparedStatement findid = null;
	protected PreparedStatement makeid = null;
	// cache ids locally
	protected HashMap ids = null;
	final protected int idcachesize = 100;
	
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
				", INDEX(s_id), INDEX(o_id), PRIMARY KEY (s_id,o_id))");
		stmt.execute("CREATE TABLE IF NOT EXISTS sv " +
				"( s_id " + idfieldtype + 
                ", p_id " + idfieldtype +
                ", o_id " + idfieldtype + 
                ", INDEX(s_id), INDEX(p_id,o_id), PRIMARY KEY (s_id,p_id,o_id) )");
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
	 * Load the content of some ontology to the database.   
	 * @param uristring
	 */
	public void loadOntology(String uristring) throws OWLOntologyCreationException,SQLException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		URI physicalURI=(new File(uristring)).toURI();
//		URI physicalURI= URI.create(uristring);
		OWLOntology ontology = manager.loadOntologyFromPhysicalURI(physicalURI);
		
		idsinsert = con.prepareStatement("INSERT IGNORE INTO ids VALUES (?,?)");
		scoinsert = con.prepareStatement("INSERT IGNORE INTO sco VALUES (?,?)");
		svinsert = con.prepareStatement("INSERT IGNORE INTO sv VALUES (?,?,?)");
		subconjunctionofinsert = con.prepareStatement("INSERT IGNORE INTO subconjunctionof VALUES (?,?,?)");
		subpropertyofinsert = con.prepareStatement("INSERT IGNORE INTO subpropertyof VALUES (?,?)");
		subpropertychaininsert = con.prepareStatement("INSERT IGNORE INTO subpropertychain VALUES (?,?,?)");
		subsomevaluesinsert = con.prepareStatement("INSERT IGNORE INTO subsomevalues VALUES (?,?,?)");
		findid = con.prepareStatement("SELECT id FROM ids WHERE name=? LIMIT 1");
		makeid = con.prepareStatement("INSERT INTO ids VALUES (NULL,?)");
		ids = new HashMap<String,Integer>(idcachesize);
		java.util.Set<OWLLogicalAxiom> axiomset = ontology.getLogicalAxioms();
		Iterator<OWLLogicalAxiom> axiomiterator = axiomset.iterator();
		OWLLogicalAxiom axiom;
		while(axiomiterator.hasNext()){
			axiom = axiomiterator.next();
			if (axiom.getAxiomType() == AxiomType.SUBCLASS) {
				loadSubclassOf(((OWLSubClassAxiom) axiom).getSubClass(), ((OWLSubClassAxiom) axiom).getSuperClass());
			} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
				loadEquivalentClasses(((OWLEquivalentClassesAxiom)axiom).getDescriptions());
			} else if (axiom.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY) {
				loadSubpropertyOf(((OWLObjectSubPropertyAxiom) axiom).getSubProperty(), ((OWLObjectSubPropertyAxiom) axiom).getSuperProperty());
			} else {
				System.err.println("The following axiom is not supported: " + axiom + "\n");
			}
		}
		manager.removeOntology(ontology.getURI());
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
		//testing: getID(c1.toString());
	}

	protected void loadEquivalentClasses(Set<OWLDescription> descriptions) {
		//System.err.println("Calling equivalent classes.");
	}

	protected void loadSubpropertyOf(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
		//System.err.println("Calling subproperty of.");
	}
	
	protected int getID(String description) throws SQLException {
		int id = 0;
		// TODO use our hash map as well for faster access
		findid.setString(1, description);
		ResultSet res = findid.executeQuery();
		if (res.next()) {
			id = res.getInt(1);
		} else {
			makeid.setString(1, description);
			makeid.executeUpdate();
			ResultSet keys = makeid.getGeneratedKeys();
			if (keys.next()) {
				id = keys.getInt(1);
			} // else we are really out of luck, return 0
			keys.close();
		}
		res.close();
		return id;
	}
}
