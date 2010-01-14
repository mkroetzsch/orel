package edu.kit.aifb.orel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.mysql.jdbc.*;



/**
 * Basic class to manage initialization, writing, and query answering from a
 * database.
 * 
 * @author Markus Krötzsch
 */
public class BasicStore {
	protected Connection con = null;
	
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
				"( id " + idfieldtype + 
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
}
