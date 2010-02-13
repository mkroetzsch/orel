package edu.kit.aifb.orel.storage;

import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import edu.kit.aifb.orel.inferencing.InferenceRuleDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateAtom;
import edu.kit.aifb.orel.inferencing.PredicateDeclaration;
import edu.kit.aifb.orel.inferencing.PredicateTerm;

/**
 * Class for implementing a simple caching interface for DB access, 
 * using batch updates for writing and caches for reading.
 * 
 * For now, we replicate a lot of code here in a dumb fashion for
 * all the tables. Maybe we can reduce the code needed later on.
 * 
 * @author Markus Krötzsch
 */
public class MySQLStorageDriver implements StorageDriver {
	
	protected HashMap<String,PredicateDeclaration> predicates;
	protected HashMap<String,InferenceRuleDeclaration> inferencerules;
	protected HashMap<String,ArrayList<PreparedStatement>> inferencerulestmts;
	protected HashMap<String,Long> inferenceruleruntimes;
	// keep prepared statements in a map to process them with less code
	protected HashMap<String,PreparedStatement> prepinsertstmts;
	protected HashMap<String,Integer> prepinsertstmtsizes;
	protected HashMap<String,ArrayList<PreparedStatement>> prepcheckstmts;
	
	// true if we are in bulk loading, auto commit=off mode
	protected boolean loadmode = false;
	
	// the remaining fields are for ID management
	protected int curid = 0; // optionally do your own counting instead of using auto increment: faster but not multi-thread safe
	// (a value of 0 indicates that AUTO INCREMENT should be used)
	protected int maxid = -1; // maximum id that is currently reserved (only used for committing batches right now)
	
	protected int namefieldlength = 50; // maximal length of the VARCHAR in the ids table
	protected Connection con = null;
	protected MessageDigest digest = null;
	protected int maxbatchsize = 1000;

	// cache ids locally
	//protected HashMap<String,Integer> ids = null;
	protected LinkedHashMap<String,Integer> ids = null;
	final protected int idcachesize = 1000;
	final protected int prelocsize = 1000;
	protected PreparedStatement findid = null; // SELECT id by name
	protected PreparedStatement makeids = null; // UPDATE prelocated ids with their names
	protected PreparedStatement makeid = null; // directly insert a single id string using AUTO INCREMENT to get its key
	protected PreparedStatement prelocids = null; // prelocate empty id rows
	protected ResultSet prelocatedids = null; // keys of prelocated ids
	protected HashMap<String,Integer> unwrittenids = null; // map of yet to be written ids (do not rely on "ids" cache until all is written!)

	
	public MySQLStorageDriver(String dbserver, String dbname, String dbuser, String dbpwd) throws SQLException {
		connect(dbserver,dbname,dbuser,dbpwd);
		//implement a least recently used cache for IDs:
		resetCaches();
		final int expectedNumberOfPredicates = 15; 
		predicates = new HashMap<String,PredicateDeclaration>(expectedNumberOfPredicates);
		inferencerules     = new HashMap<String,InferenceRuleDeclaration>(30);
		inferencerulestmts = new HashMap<String,ArrayList<PreparedStatement>>(30);
		inferenceruleruntimes = new HashMap<String,Long>(30);

		prepinsertstmts = new HashMap<String,PreparedStatement>(expectedNumberOfPredicates);
		prepinsertstmtsizes = new HashMap<String,Integer>(expectedNumberOfPredicates);
		prepcheckstmts = new HashMap<String,ArrayList<PreparedStatement>>(expectedNumberOfPredicates);
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// unlikely
		}
	}
	
	/**
	 * Destroy and recreate any in memory caches that this object has. This is
	 * important if the DB is modified in violent ways during some run.
	 */
	protected void resetCaches() {
		ids = new LinkedHashMap<String,Integer>(idcachesize,0.75f,true) {
			/// Anonymous inner class
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<String,Integer> eldest) {
			   return size() > idcachesize;
			}
		};
		unwrittenids = new HashMap<String,Integer>(prelocsize);
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
	
	public void registerPredicate(PredicateDeclaration pd) {
		predicates.put(pd.getName(), pd);
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
		// additional non-predicate table
		stmt.execute("CREATE TABLE IF NOT EXISTS ids " +
				"( id " + idfieldtype + " AUTO_INCREMENT" +
                ", name VARCHAR(50), PRIMARY KEY (id), INDEX(name)) " + engine);
		Iterator<PredicateDeclaration> pit = predicates.values().iterator();
		PredicateDeclaration pd;
		String sql, indexes, primkey, sep;
		while (pit.hasNext()) {
			pd = pit.next();
			sql = "CREATE TABLE IF NOT EXISTS " + pd.getName() + " (";
			indexes = "";
			primkey = "";
			for (int i=0; i<pd.getFieldCount(); i++) {
				if (i>0) sep = ","; else sep = "";
				sql = sql + sep + "f" + (i) + " " + idfieldtype;
				indexes = indexes + ", INDEX(f" + (i) + ")";
				primkey = primkey + sep + "f" + (i);
			}
			if (pd.isInferred()) {
				sql = sql + ", step INT";
				indexes = indexes + ", INDEX(step)";
			}
			stmt.execute(sql + indexes + ", PRIMARY KEY (" + primkey + ") )" + engine);
		}
	}

	/**
	 * Delete all of our database tables and their contents.
	 */
	public void drop() throws SQLException {
		commit(); // use to clear prepared statements' caches
		Statement stmt = con.createStatement();
		Iterator<PredicateDeclaration> pit = predicates.values().iterator();
		while (pit.hasNext()) {
			stmt.execute("DROP TABLE IF EXISTS " + pit.next().getName());
		}
		stmt.execute("DROP TABLE IF EXISTS ids");
		resetCaches();
	}

	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clear(boolean onlyderived) throws SQLException {
		commit();
		Iterator<String> pit = predicates.keySet().iterator();
		String predicate;
		while (pit.hasNext()) {
			predicate = pit.next();
			clear(predicate,onlyderived);
		}
		if (!onlyderived) {
			Statement stmt = con.createStatement();
			stmt.execute("TRUNCATE TABLE ids");
		}
		resetCaches();
	}
	
	public void clear(String predicate, boolean onlyderived) throws SQLException {
		commit();
		Statement stmt = con.createStatement();
		PredicateDeclaration pd = predicates.get(predicate);
		if (pd == null) return; // unknown predicate
		if (onlyderived == true)  {
			if ( pd.isInferred() ) {
				stmt.execute("DELETE FROM " + pd.getName() + " WHERE step!=0");
			}
		} else {
			stmt.execute("TRUNCATE TABLE " + pd.getName());
		}
	}
	
	/**
	 * Really close all DB objects. Maybe relevant for longer runs; not
	 * currently used.
	 * @throws SQLException
	 */
	protected void close() throws SQLException {
		commit();
		Iterator<String> inserttables = prepinsertstmts.keySet().iterator();
		PreparedStatement pstmt;
		while (inserttables.hasNext()) {
			pstmt = prepinsertstmts.get(inserttables.next());
			pstmt.close();
		}
		prepinsertstmts.clear();
		prepinsertstmtsizes.clear();
		if (makeids != null) {
			makeids.close();
			makeids = null;
		}
	}

	/**
	 * Make sure all batch inserts are flushed, even if they are
	 * below the maximum size.
	 */
	public void commit() throws SQLException {
		Iterator<String> inserttables = prepinsertstmts.keySet().iterator();
		String key;
		while (inserttables.hasNext()) {
			key = inserttables.next();
			prepinsertstmts.get(key).executeBatch();
			prepinsertstmtsizes.put(key,0);
		}
		if (makeids != null) makeids.executeBatch();
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM ids WHERE name=\"-\""); // delete any unused pre-allocated ids
		if (loadmode) con.commit();
	}
	
	/**
	 * Debugging function to print all statistics gathered about some run. Maybe the
	 * architecture for this could become more intelligent in the future.
	 */
	public void dumpStatistics() {
		Iterator<String> statit = inferenceruleruntimes.keySet().iterator();
		String rulename;
		System.out.println("Times spent on inference rules:");
		while (statit.hasNext()) {
			rulename = statit.next();
			System.out.println("  Rule " + rulename + ": " + inferenceruleruntimes.get(rulename) + "ms.");
		}
	}
	
	/**
	 * Configure the store for loading large amounts of data more efficiently.
	 * Call endLoading() when the operation is completed.
	 */
	public void beginLoading() {
		// find largest used id to start iteration:
		if (curid <= 0) {
			try {
				Statement stmt = con.createStatement();
				ResultSet res = stmt.executeQuery("SELECT max(id) FROM ids");
				if (res.next()) {
					curid = res.getInt(1) + 1;
				} else {
					curid = 1;
				}
			} catch (SQLException e) {
				curid = 0; // fall back to AUTO INCREMENT
			}
		}
		try {
			con.setAutoCommit(false);
			//con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			//con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			enableKeys(false);
		} catch (SQLException e) {
			System.err.println(e.toString());
		}
		loadmode=true;
	}
	
	/**
	 * Reset to normal operation and possibly flush buffers after a large loading
	 * operation initiated with beginLoading().
	 */
	public void endLoading() {
		if (!loadmode) return;
		curid=0; // back to AUTO INCREMENT
		try {
			commit();
			con.setAutoCommit(true);
			enableKeys(true);
		} catch (SQLException e) {
			System.err.println(e.toString());
		}
		loadmode=false;
	}

	protected void enableKeys(boolean ek) throws SQLException {
		Statement stmt = con.createStatement();
		String action;
		if (ek) {
			action = " ENABLE KEYS";
		} else {
			action = " DISABLE KEYS";
		}
		Iterator<PredicateDeclaration> pit = predicates.values().iterator();
		PredicateDeclaration pd;
		while (pit.hasNext()) {
			pd = pit.next();
			stmt.execute("ALTER TABLE " + pd.getName() + action);
		}
	}
	
	/* *** Basic data access *** */

	public void makePredicateAssertion(String predicate, int... ids) throws SQLException {
		PreparedStatement stmt = prepinsertstmts.get(predicate);
		if (stmt == null) {
			stmt = getPreparedInsertStatement(predicate);
			prepinsertstmts.put(predicate, stmt);
			prepinsertstmtsizes.put(predicate, 0);
		}
		for (int i=0; i<ids.length; i++) {
			stmt.setInt(i+1, ids[i]);
		}
		stmt.addBatch();
		int cursize = prepinsertstmtsizes.get(predicate)+1;
		if (cursize >= maxbatchsize) {
			stmt.executeBatch();
			prepinsertstmtsizes.put(predicate,0);
		} else {
			prepinsertstmtsizes.put(predicate,cursize);
		}
	}
	
	protected PreparedStatement getPreparedInsertStatement(String tablename) throws SQLException {
		if (predicates.containsKey(tablename)) {
			PredicateDeclaration pd = predicates.get(tablename);
			String sql = "INSERT IGNORE INTO " + tablename + " VALUES (?";
			for (int i=1; i<pd.getFieldCount(); i++) {
				sql = sql.concat(",?");
			}
			if (pd.isInferred()) { // also set the default step counter:
				sql = sql.concat(",0");
			}
			sql = sql.concat(")");
			return con.prepareStatement(sql);
		} else { 
			return null;
		}
	}
	
	public boolean checkPredicateAssertion(String predicate, int... ids) throws SQLException {
		ArrayList<PreparedStatement> stmts = prepcheckstmts.get(predicate);
		if (stmts == null) {
			stmts = new ArrayList<PreparedStatement>(1);
			stmts.add(getPreparedCheckStatement(predicate));
			prepcheckstmts.put(predicate, stmts);
		}
		Iterator<PreparedStatement> stmtit = stmts.iterator();
		PreparedStatement stmt;
		boolean result = false;
		while (!result && stmtit.hasNext()) {
			stmt = stmtit.next();
			for (int i=0; i<ids.length; i++) {
				stmt.setInt(i+1, ids[i]);
			}
			ResultSet res = stmt.executeQuery();
			result = (res.next());
			res.close();
		}
		return result;
	}

	protected PreparedStatement getPreparedCheckStatement(String tablename) {
		if (predicates.containsKey(tablename)) {
			PredicateDeclaration pd = predicates.get(tablename);
			String sql = "SELECT * FROM " + tablename + " WHERE ";
			for (int i=0; i<pd.getFieldCount(); i++) {
				if (i>0) sql = sql + " AND ";
				sql = sql + "f" + (i) + "=?";
			}
			sql = sql + " LIMIT 1";
			try {
				return con.prepareStatement(sql);
			} catch (SQLException e) {
				System.err.println(e.toString());
				return null;
			}
		} else { 
			return null;
		}
	}
	
	public int changeStep(String predicate, int oldstep, int newstep) throws SQLException {
		Statement stmt = con.createStatement();
		//System.out.println("Changestep: " + oldstep + " -> " + newstep + " on " + predicate); // debug
		return stmt.executeUpdate("UPDATE " + predicate + " SET step=\"" + newstep + "\" WHERE step=\"" + oldstep + "\"");
	}

	/* *** Rule execution *** */
	
	public void registerInferenceRule(InferenceRuleDeclaration rd) {
		inferencerules.put(rd.getName(), rd);
		if (rd.getMode() == InferenceRuleDeclaration.MODE_CHECK) {
			String predicate = rd.getHead().getName();
			ArrayList<PreparedStatement> stmts = prepcheckstmts.get(predicate);
			if (stmts == null) {
				stmts = new ArrayList<PreparedStatement>(1);
				stmts.add(getPreparedCheckStatement(predicate));
				prepcheckstmts.put(predicate, stmts);
			}
			stmts.addAll(getInferenceRuleStatements(rd));
		} else {
			inferencerulestmts.put(rd.getName(), getInferenceRuleStatements(rd));
		}
	}

	/**
	 * Run the given rule on all existing preconditions, without filtering
	 * by step. The results are given the step value as defined by newstep.
	 * @param rulename
	 * @param newstep
	 * @return the number of new tuples that were found
	 */
	public int runRule(String rulename, int newstep) {
		ArrayList<PreparedStatement> stmts = inferencerulestmts.get(rulename);
		if ((stmts == null) || (stmts.size() == 0)) { // internal error, just print it
			System.err.println("Call to unknown rule " + rulename);
			return 0;
		}
		System.out.print("  Rule " + rulename + "(*) -> " + newstep + " ... "); // debug
		long sTime = System.currentTimeMillis();
		PreparedStatement stmt = stmts.get(0);
		int result = 0;
		try {
			if (inferencerules.get(rulename).getMode() != InferenceRuleDeclaration.MODE_RETRACT) {
				stmt.setInt(1, newstep);
			}
			result = stmt.executeUpdate();
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println("[" + result + "]"); // debug
		if (inferenceruleruntimes.containsKey(rulename)) {
			sTime = sTime - inferenceruleruntimes.get(rulename);
		}
		inferenceruleruntimes.put(rulename,new Long(System.currentTimeMillis() - sTime));
		return result;
	}

	/**
	 * Run the given rule on all existing preconditions, without filtering
	 * by step, but using the given parameters for setting the unspecified constants
	 * in the rule. The results are given the step value as defined by newstep.
	 * @param rulename
	 * @param newstep
	 * @return the number of new tuples that were found
	 */
	public int runRule(String rulename, int newstep, int[] params) {
		ArrayList<PreparedStatement> stmts = inferencerulestmts.get(rulename);
		if ((stmts == null) || (stmts.size() == 0)) { // internal error, just print it
			System.err.println("Call to unknown rule " + rulename);
			return 0;
		}
		System.out.print("  Rule " + rulename + "(*) -> " + newstep + " ... "); // debug
		long sTime = System.currentTimeMillis();
		PreparedStatement stmt = stmts.get(0);
		int result = 0, pos=1;
		try {
			if (inferencerules.get(rulename).getMode() != InferenceRuleDeclaration.MODE_RETRACT) {
				stmt.setInt(pos++, newstep);
			}
			for (int i=0; i<params.length; i++) {
				stmt.setInt(pos++, params[i]);				
			}
			result = stmt.executeUpdate();
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println("[" + result + "]"); // debug
		if (inferenceruleruntimes.containsKey(rulename)) {
			sTime = sTime - inferenceruleruntimes.get(rulename);
		}
		inferenceruleruntimes.put(rulename,new Long(System.currentTimeMillis() - sTime));
		return result;
	}
	
	/**
	 * Run the given rule for obtaining all results that require the use of
	 * some data with step values between min_cur_step and max_cur_step; the
	 * boundaries are included. Newly derived tuples are marked with the step
	 * value max_cur_step+1.
	 * @param rulename
	 * @param min_cur_step
	 * @param max_cur_step
	 * @return the number of new tuples that were found
	 */
	public int runRule(String rulename, int min_cur_step, int max_cur_step) {
		if (min_cur_step == 0) min_cur_step = -1; // make sure that sub-zero (late) base facts are considered in this case 
		ArrayList<PreparedStatement> stmts = inferencerulestmts.get(rulename);
		if ((stmts == null) || (stmts.size() == 0)) { // internal error, just print it
			System.err.println("Call to unknown rule " + rulename);
			return 0;
		}
		if (stmts.size() == 1) return runRule(rulename,max_cur_step+1); // no steps in body
		int result = 0;
		System.out.print("  Rule " + rulename + "(" + min_cur_step + "-" + max_cur_step + ") ... "); // debug
		long sTime = System.currentTimeMillis();
		try {
			int pos;
			PreparedStatement stmt;
			for (int i=1; i<stmts.size(); i++) {
				stmt = stmts.get(i);
				pos = 1;
				if (inferencerules.get(rulename).getMode() != InferenceRuleDeclaration.MODE_RETRACT) {
					stmt.setInt(pos++, max_cur_step+1);
				}
				for (int j=1; j<=i; j++) {
					stmt.setInt(pos++, min_cur_step);
				}
				stmt.setInt(pos++, max_cur_step);
				result = result + stmt.executeUpdate();
			}
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println("[" + result + "]"); // debug
		if (inferenceruleruntimes.containsKey(rulename)) {
			sTime = sTime - inferenceruleruntimes.get(rulename);
		}
		inferenceruleruntimes.put(rulename,new Long(System.currentTimeMillis() - sTime));
		return result;
	}

	/**
	 * Transform a rule declaration into a list of prepared SQL statements used to
	 * execute the rule. The first entry of the list is the version of the rule operating
	 * on all input data without additional step-based filters. All following statements
	 * correspond to variants of the rule where only certain combinations of steps are
	 * considered in the body, thus enabling semi-naive evaluation.
	 * @param rd
	 * @return
	 */
	protected ArrayList<PreparedStatement> getInferenceRuleStatements(InferenceRuleDeclaration rd) {
		ArrayList<PreparedStatement> result = new ArrayList<PreparedStatement>();
		ArrayList<String> inferredTables = new ArrayList<String>();
		HashMap<String,ArrayList<String>> varequalities = new HashMap<String,ArrayList<String>>();
		HashMap<String,ArrayList<String>> constequalities = new HashMap<String,ArrayList<String>>();
		String from = "", insert, select = "", on = "", 
		       on_op = " WHERE ";  // use WHERE while there is just one table
		PredicateTerm pt;
		boolean hasParameterConstants = false; // constants of value "?" are set at application time;
		                                       // rules with such constants do not support semi-naive
		                                       // rewriting and can only be called when providing parameters

		// iterate over body to collect all variables and their variable->column mappings
		// also make join string for FROM here
		for (int i=0; i<rd.getBody().size(); i++) {
			if (i>0) {
				from = from + " INNER JOIN ";
				on_op = " ON ";
			}
			from = from + prepareRuleBodyAtom(rd, rd.getBody().get(i), i, inferredTables, varequalities, constequalities);
		}
		
		// make strings for INSERT and SELECT part
		if (rd.getMode() == InferenceRuleDeclaration.MODE_RETRACT) { // DELETE
			insert = "DELETE t" + rd.getBody().size() + ".*";
			if (!from.equals("")) from = from + " INNER JOIN ";
			from = from + prepareRuleBodyAtom(rd, rd.getHead(), rd.getBody().size(), inferredTables, varequalities, constequalities);
		} else if (rd.getMode() == InferenceRuleDeclaration.MODE_CHECK) { // SELECT all (we just care about the non-zero count here)
			insert = "";
			select = "SELECT * ";
			// Note: we build the "on" string here instead of adding equalities to our HashMaps, since otherwise the order of ? in the statement would not be correct
			for (int i=0; i<rd.getHead().getArguments().size(); i++) {
				if (!on.equals("")) on = on + " AND "; else on = on_op + " ";
				pt = rd.getHead().getArguments().get(i);
				if (pt.isVariable()) {
					if (varequalities.containsKey(pt.getValue())) {
						on = on + varequalities.get(pt.getValue()).get(0) + "=?";
					} // else: no constraints on this variable, nothing to check
					/// FIXME this is unsound if some variable appears more than once in the head but not in the body
				} else {
					on = on + "?=\"" + pt.getValue() + "\"";
				}
			}
			hasParameterConstants = true; // never make stepped statements for checks
		} else { // INSERT SELECTed data 
			insert = "INSERT IGNORE INTO " + rd.getHead().getName() + " (";
			PredicateDeclaration pd = predicates.get(rd.getHead().getName()); // use "pd == null" to indicate that rule is broken
			for (int i=0; i<rd.getHead().getArguments().size(); i++) {
				if (i>0) insert = insert + ",";
				insert = insert + "f" + (i);
				pt = rd.getHead().getArguments().get(i);
				if (!select.equals("")) select = select + ",";
				if (pt.isVariable()) {
					if (varequalities.containsKey(pt.getValue())) {
						select = select + varequalities.get(pt.getValue()).get(0) + " AS f" + (i);
					} else { // else: unsafe rule, drop it
						System.err.println("Rule " + rd.getName() + " is unsafe. Ignoring it."); 
						pd = null;
					}
				} else {
					select = select + "\"" + pt.getValue() + "\" AS f" + (i);
				}
			}
			if (pd == null) {
				System.err.println("There was a problem registering rule " + rd.getName());
				return result;
			}
			if (pd.isInferred()) {
				insert = insert + ",step";
				select = select + ", ? AS step";
			}
			insert = insert + ") "; 
			select = "SELECT DISTINCT " + select;
		}
		
		// make string for ON part (join conditions)
		Iterator<String> keyit = constequalities.keySet().iterator();
		String key;
		while (keyit.hasNext()) {
			key = keyit.next();
			for (int i=0; i<constequalities.get(key).size(); i++) {
				if (!on.equals("")) on = on + " AND "; else on = on_op + " ";
				if (key.equals("?")) { // keep as parameter
					on = on + key + constequalities.get(key).get(i);
					hasParameterConstants = true;
				} else {
					on = on + "\"" + key + "\"" + constequalities.get(key).get(i);
				}
			}
		}
		Iterator<ArrayList<String>> fieldsit = varequalities.values().iterator();
		ArrayList<String> fields;
		while (fieldsit.hasNext()) {
			fields = fieldsit.next();
			for (int i=0; i<fields.size()-1; i++) {
				if (!on.equals("")) on = on + " AND "; else on = on_op + " ";
				on = on + fields.get(i) + "=" + fields.get(i+1);
			}
		}
	
		/// Now build the final rules ...
		// always make step-less version at index 0
		try {
			if (rd.getMode() == InferenceRuleDeclaration.MODE_CHECK) on = on + " LIMIT 1";
			//System.out.println(rd.getName() + ":\n " + insert + select + " FROM " + from + on + "\n\n"); // DEBUG
			result.add(con.prepareStatement( insert + select + " FROM " + from + on));
		} catch (SQLException e) { // bug in the above code, just print it
			System.err.println(e.toString()); 
		}
		String sql;
		// other indices k hold semi-naive rule for the k-th inferred predicate 
		if ( (inferredTables.size() > 0) && (!hasParameterConstants) ) {
			// make rule variants for semi-naive evaluation			
			for (int i=0; i<inferredTables.size(); i++) {
				sql = insert + select + " FROM " + from + on;
				if (on.equals("")) on_op = " WHERE "; else on_op = " AND ";
				for (int j=0; j<=i; j++) {
					if (j<i) {
						sql = sql + on_op + inferredTables.get(j) + ".step<?";
					} else {
						sql = sql + on_op + inferredTables.get(j) + ".step>=? AND " + inferredTables.get(j) + ".step<=?";
					}
				}
				//System.out.println(sql); // DEBUG
				try {
					result.add(con.prepareStatement(sql));
				} catch (SQLException e) { // bug in the above code, just print it
					System.err.println(e.toString()); 
				}
			}
		}		
		return result;
	}
	
	/**
	 * Helper function to process the i-th atom in the body of a rule described by rd. 
	 * @param rd
	 * @param i
	 * @param inferredTables
	 * @param varequalities
	 * @param constequalities
	 * @return
	 */
	protected String prepareRuleBodyAtom(InferenceRuleDeclaration rd, PredicateAtom pa, int i, 
			ArrayList<String> inferredTables, 
			HashMap<String,ArrayList<String>> varequalities,
			HashMap<String,ArrayList<String>> constequalities) {
		PredicateTerm pt;
		PredicateDeclaration pd;
		pd = predicates.get(pa.getName());
		if (pd == null) return ""; // ignore unknown predicates
		// collect inferred body tables unless "step" value is given explicitly
		if ( (pd.isInferred() && (pa.getArguments().size()==pd.getFieldCount()) )) {
			inferredTables.add("t" + (i));
		}
		// collect fields equal to each variable/constant
		for (int j=0; j<pa.getArguments().size(); j++) {
			pt = pa.getArguments().get(j);
			if (pt.isVariable()) {
				if (!varequalities.containsKey(pt.getValue())) {
					varequalities.put(pt.getValue(),new ArrayList<String>());
				}
				varequalities.get(pt.getValue()).add( "t" + (i) + ".f" + (j) );
			} else {
				if (!constequalities.containsKey(pt.getValue())) {
					constequalities.put(pt.getValue(),new ArrayList<String>());
				}
				if (j<pd.getFieldCount()) {
					constequalities.get(pt.getValue()).add( "=t" + (i) + ".f" + (j) );
				} else { // use the extra field for fixed step
					if (pa.getArguments().size() == pd.getFieldCount()+1) {
						if (pt.getValue().equals("0")) { // special handling: match all steps below 0
							constequalities.get(pt.getValue()).add( ">=t" + (i) + ".step" );
						} else {
							constequalities.get(pt.getValue()).add( "=t" + (i) + ".step" );
						}
					} else { // support up to two step parameters (outer bounds)  
						if (j==pd.getFieldCount()) {
							constequalities.get(pt.getValue()).add( "<=t" + (i) + ".step" );
						} else if (j==pd.getFieldCount()+1) {
							constequalities.get(pt.getValue()).add( ">=t" + (i) + ".step" );
						}
					}
				}
			}
		}
		return pa.getName() + " AS t" + (i);
	}

	/* *** Id management *** */
	
	/**
	 * Produce a canonical string name based on the given operator name and operand list.
	 * For commutative operators like ObjectIntersectionOf, the operands should be sorted
	 * before being passed to this method, so as to ensure a consistent representation.
	 * TODO Check if this is still used for sets of individuals by anybody. Otherwise 
	 * reduce to class expressions.
	 */
	protected String getCanonicalName(String opname, List<? extends OWLObject> operands) {
		String result = opname + "(";
		for (int i=0; i<operands.size(); i++) {
			if (operands.get(i) instanceof OWLIndividual) {
				result = result + " " + getCanonicalName((OWLIndividual)operands.get(i));
			} else if (operands.get(i) instanceof OWLClassExpression) {
				result = result + " " + getCanonicalName((OWLClassExpression)operands.get(i));
			}
		}
		return result + " )";
	}
	
	protected String getCanonicalName(OWLClassExpression description) {
		if (description.isOWLNothing()) {
			return StorageDriver.OP_NOTHING;
		} else if (description.isOWLThing()) {
			return StorageDriver.OP_THING;
		} else if (description instanceof OWLObjectOneOf) {
			Set<OWLIndividual> ops = ((OWLObjectOneOf) description).getIndividuals();
			if (ops.size() == 1) {
				return getCanonicalName(ops.iterator().next());
			} else {
				return getCanonicalName(((OWLObjectOneOf) description).asObjectUnionOf());
			}
		} else if (description instanceof OWLNaryBooleanClassExpression) {
			String opname;
			if (description instanceof OWLObjectIntersectionOf) {
				opname = StorageDriver.OP_OBJECT_INTERSECTION;
			} else if (description instanceof OWLObjectUnionOf) {
				opname = StorageDriver.OP_OBJECT_UNION;
			} else {
				System.err.println("Unsupported nary class expression " + description.toString());
				return description.toString();
			}
			ArrayList<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(((OWLNaryBooleanClassExpression) description).getOperands());
			Collections.sort(ops); // make sure that we have a defined order; cannot have random changes between prepare and check!
			return getCanonicalName(opname,ops);
		} else {
			return description.toString();
		}
	}
	
	protected String getCanonicalName(OWLIndividual individual) {
		// treat individuals like nominals
		return  StorageDriver.OP_OBJECT_ONE_OF + "(" + individual.toString() + ")";
	}
	
	/**
	 * Produce an id based on the given operator name and operand list.
	 * For commutative operators like ObjectIntersectionOf, the operands should be sorted
	 * before being passed to this method, so as to ensure a consistent representation.
	 */
	public int getIDForNaryExpression(String opname, List<? extends OWLObject> operands) {
		return getIDForString(getCanonicalName(opname, operands));
	}
	
	public int getID(OWLClassExpression description) {
		return getIDForString(getCanonicalName(description));
	}

	public int getID(OWLIndividual individual) {
		return getIDForString(getCanonicalName(individual));
	}
	
	public int getID(SimpleLiteral literal) {
		return getIDForString(literal.toString());
	}
	
	public int getID(OWLPropertyExpression<?,?> property) {
		return getIDForString(property.toString());
	}
	
	public int getIDForNothing() {
		return getIDForString(StorageDriver.OP_NOTHING);
	}

	public int getIDForThing() {
		return getIDForString(StorageDriver.OP_THING);
	}
	
	public int getIDForDatatypeURI(String uri) {
		return getIDForString(uri);
	}
	
	protected int getIDForString(String description) {
		int id = 0;
		//System.out.println("Getting id for " + description); // debug
		String hash;
		if (description.toCharArray().length < namefieldlength) { // try to keep names intact ...
			hash = description;
		} else { // ... but use a hash if the string is too long
			digest.update(description.getBytes());
			hash = "_" + getHex(digest.digest());
		}
		if (ids.containsKey(hash)) { // id in LRU cache
			id = ids.get(hash).intValue();
		} else if (unwrittenids.containsKey(hash)) { // id was created recently and is not written to disk yet
			id = unwrittenids.get(hash).intValue();
		} else try { // id not available: find it in the DB or newly allocate it
			if (findid == null) findid = con.prepareStatement("SELECT id FROM ids WHERE name=? LIMIT 1");
			findid.setString(1, hash);
			ResultSet res = findid.executeQuery();
			if (res.next()) {
				id = res.getInt(1);
			} else if (loadmode) { // use batch operations and write caching
				// check if we need to pre-allocate more ids (and commit recently created ids)
				if ( ( (curid>0) && (curid>maxid) ) || 
				     ( (curid==0) && ((prelocatedids == null) || (!prelocatedids.next())) ) ) {
					if (curid == 0) { // rely on AUTO INCREMENT
						if (prelocids == null) {
							String insertvals = "(NULL,\"-\")";
							for (int i=1; (i<prelocsize); i++) {
								insertvals = insertvals.concat(",(NULL,\"-\")");
							}
							prelocids = con.prepareStatement("INSERT INTO ids VALUES " + insertvals, Statement.RETURN_GENERATED_KEYS);
						}
						if (prelocatedids != null) prelocatedids.close();						
						prelocids.executeUpdate();
						prelocatedids = prelocids.getGeneratedKeys();
						prelocatedids.next();						
					} else { // simply increment ids yourself
						maxid = maxid + prelocsize;
					}
					if (makeids != null) { // in any case: batch write recently introduced ids
						makeids.executeBatch();
						unwrittenids.clear();
					}
					con.commit(); // needed to ensure that above SELECTs will be correct now that unwrittenids is empty again
				}
				// add a new id to the current batch and unwritten id cache
				if (curid == 0) {
					id = prelocatedids.getInt(1);
					if (makeids == null) makeids = con.prepareStatement("UPDATE ids SET name=? WHERE id=?");
					makeids.setInt(2,id);
					makeids.setString(1,hash);
				} else {
					id = curid++;
					if (makeids == null) makeids = con.prepareStatement("INSERT INTO ids VALUES (?,?)");
					makeids.setInt(1,id);
					makeids.setString(2,hash);					
				}
				unwrittenids.put(hash,id);
				makeids.addBatch();
			} else { // use slow single insert
				if (makeid == null) makeid = con.prepareStatement("INSERT INTO ids VALUES (NULL,?)", Statement.RETURN_GENERATED_KEYS);
				makeid.setString(1,hash);
				makeid.executeUpdate();
				prelocatedids = makeid.getGeneratedKeys();
				prelocatedids.next();
				id = prelocatedids.getInt(1);
			}
			res.close();
			ids.put(hash,id);
		} catch (SQLException e) { // should happen only on programming errors in above code
			System.err.println(e.toString());
			id = -1;
		}
		return id;
	}

	static final String HEXES = "0123456789ABCDEF";
	/**
	 * Convert a byte array to a string that shows its entries in Hex format.
	 * Based on code from http://www.rgagnon.com/javadetails/java-0596.html.
	 * @param raw
	 * @return
	 */
	protected static String getHex( byte [] raw ) {
		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
				.append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	  }

	
}