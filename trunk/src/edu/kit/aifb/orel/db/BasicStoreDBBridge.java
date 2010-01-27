package edu.kit.aifb.orel.db;

import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

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
public class BasicStoreDBBridge {
	protected HashMap<String,PredicateDeclaration> predicates;
	protected HashMap<String,InferenceRuleDeclaration> inferencerules;
	protected HashMap<String,ArrayList<PreparedStatement>> inferencerulestmts;
	// keep prepared statements in a map to process them with less code
	protected HashMap<String,PreparedStatement> prepinsertstmts;
	protected HashMap<String,Integer> prepinsertstmtsizes;
	protected HashMap<String,PreparedStatement> prepcheckstmts;	
	
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
	protected PreparedStatement prelocids = null; // prelocate empty id rows
	protected ResultSet prelocatedids = null; // keys of prelocated ids
	protected HashMap<String,Integer> unwrittenids = null; // map of yet to be written ids (do not rely on "ids" cache until all is written!)
	
	public BasicStoreDBBridge(Connection connection) {
		this(connection,-1);
	}

	public BasicStoreDBBridge(Connection connection, int startid) {
		con = connection;
		//implement a least recently used cache for IDs:
		ids = new LinkedHashMap<String,Integer>(idcachesize,0.75f,true) {
			/// Anonymous inner class
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<String,Integer> eldest) {
			   return size() > idcachesize;
			}
		};
		final int expectedNumberOfPredicates = 15; 
		predicates = new HashMap<String,PredicateDeclaration>(expectedNumberOfPredicates);
		inferencerules     = new HashMap<String,InferenceRuleDeclaration>(30);
		inferencerulestmts = new HashMap<String,ArrayList<PreparedStatement>>(30);

		unwrittenids = new HashMap<String,Integer>(prelocsize);
		prepinsertstmts = new HashMap<String,PreparedStatement>(expectedNumberOfPredicates);
		prepinsertstmtsizes = new HashMap<String,Integer>(expectedNumberOfPredicates);
		prepcheckstmts = new HashMap<String,PreparedStatement>(expectedNumberOfPredicates);
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// unlikely
		}

		// find largest used id to start iteration:
		if (startid < 0) {
			try {
				Statement stmt = con.createStatement();
				ResultSet res = stmt.executeQuery("SELECT max(id) FROM ids");
				if (res.next()) {
					startid = res.getInt(1) + 1;
				} else {
					startid = 1;
				}
			} catch (SQLException e) {
				startid = 0; // fall back to AUTO INCREMENT
			}
		}
		curid = startid;
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
		Statement stmt = con.createStatement();
		Iterator<PredicateDeclaration> pit = predicates.values().iterator();
		while (pit.hasNext()) {
			stmt.execute("DROP TABLE IF EXISTS " + pit.next().getName());
		}
		stmt.execute("DROP TABLE IF EXISTS ids");
	}

	/**
	 * Delete the contents of the database but do not drop the tables we created.
	 * @throws SQLException
	 */
	public void clear(boolean onlyderived) throws SQLException {
		Statement stmt = con.createStatement();
		Iterator<PredicateDeclaration> pit = predicates.values().iterator();
		PredicateDeclaration pd;
		while (pit.hasNext()) {
			pd = pit.next();
			if ( (onlyderived == true) && (pd.isInferred()) )  {
				stmt.execute("DELETE FROM " + pd.getName() + " WHERE step!=0");
			} else {
				stmt.execute("TRUNCATE TABLE " + pd.getName());
			}
		}
		if (!onlyderived) {
			stmt.execute("TRUNCATE TABLE ids");
		}
	}
	
	/**
	 * Make sure all batch inserts are flushed, even if they are
	 * below the maximum size.
	 */
	public void close() throws SQLException {
		Iterator<String> inserttables = prepinsertstmts.keySet().iterator();
		PreparedStatement pstmt;
		String key;
		while (inserttables.hasNext()) {
			key = inserttables.next();
			pstmt = prepinsertstmts.get(key);
			pstmt.executeBatch();
			pstmt.close();
		}
		prepinsertstmts.clear();
		prepinsertstmtsizes.clear();
		if (makeids != null) makeids.executeBatch();
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM ids WHERE name=\"-\""); // delete any unused pre-allocated ids
		con.commit();
	}
	
	/* *** Basic data access *** */

	public void insertIdsToTable(String tablename, int id1) throws SQLException {
		insertIdsToTable(tablename,id1,-1,-1);
	}
	
	public void insertIdsToTable(String tablename, int id1, int id2) throws SQLException {
		insertIdsToTable(tablename,id1,id2,-1);
	}

	public void insertIdsToTable(String tablename, int id1, int id2, int id3) throws SQLException {
		PreparedStatement stmt = prepinsertstmts.get(tablename);
		if (stmt == null) {
			stmt = getPreparedInsertStatement(tablename);
			prepinsertstmts.put(tablename, stmt);
			prepinsertstmtsizes.put(tablename, 0);
		}
		if (id1 >= 0) stmt.setInt(1, id1);
		if (id2 >= 0) stmt.setInt(2, id2);
		if (id3 >= 0) stmt.setInt(3, id3);
		stmt.addBatch();
		int cursize = prepinsertstmtsizes.get(tablename)+1;
		if (cursize >= maxbatchsize) {
			stmt.executeBatch();
			prepinsertstmtsizes.put(tablename,0);
		} else {
			prepinsertstmtsizes.put(tablename,cursize);
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
	
	public boolean checkIdsInTable(String tablename, int id1, int id2) throws SQLException {
		return checkIdsInTable(tablename,id1,id2,-1);
	}

	public boolean checkIdsInTable(String tablename, int id1, int id2, int id3) throws SQLException {
		PreparedStatement stmt = prepcheckstmts.get(tablename);
		if (stmt == null) {
			stmt = getPreparedCheckStatement(tablename);
			prepinsertstmts.put(tablename, stmt);
			prepinsertstmtsizes.put(tablename, 0);
		}
		if (id1 >= 0) stmt.setInt(1, id1);
		if (id2 >= 0) stmt.setInt(2, id2);
		if (id3 >= 0) stmt.setInt(3, id3);
		ResultSet res = stmt.executeQuery();
		boolean result = (res.next());
		res.close();
		return result;
	}

	/// TODO: update to use predicate declarations
	protected PreparedStatement getPreparedCheckStatement(String tablename) throws SQLException {
		if (tablename.equals("sco")) {
			return con.prepareStatement("SELECT * FROM sco WHERE s_id=? AND o_id=? LIMIT 1");
		} else if (tablename.equals("sv")) {
			return null;
		} else if (tablename.equals("subconjunctionof")) {
			return null;
		} else if (tablename.equals("subpropertyof")) {
			return con.prepareStatement("SELECT * FROM subpropertyof WHERE s_id=? AND o_id=? LIMIT 1");
		} else if (tablename.equals("subpropertychain")) {
			return con.prepareStatement("SELECT * FROM subpropertychain WHERE s1_id=? AND s2_id=? AND o_id=? LIMIT 1");
		} else if (tablename.equals("subsomevalues")) {
			return null;
		} else if (tablename.equals("ids")) {
			return null;
		}
		return null;
	}

	/* *** Rule execution *** */
	
	public void registerInferenceRule(InferenceRuleDeclaration rd) {
		inferencerules.put(rd.getName(), rd);
		inferencerulestmts.put(rd.getName(), getInferenceRuleStatements(rd));
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
			System.err.println("Illegal call to rule " + rulename);
			return 0;
		}
		System.out.print("  Applying Rule " + rulename + " ... "); // debug
		PreparedStatement stmt = stmts.get(0);
		int result = 0;
		try {
			stmt.setInt(1, newstep);
			result = stmt.executeUpdate();
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println(" got " + result + " rows."); // debug
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
			System.err.println("Illegal call to rule " + rulename);
			return 0;
		}
		System.out.print("  Applying Rule " + rulename + " ... "); // debug
		PreparedStatement stmt = stmts.get(0);
		int result = 0;
		try {
			stmt.setInt(1, newstep);
			for (int i=0; i<params.length; i++) {
				stmt.setInt(i+2, params[i]);				
			}
			result = stmt.executeUpdate();
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println(" got " + result + " rows."); // debug
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
		ArrayList<PreparedStatement> stmts = inferencerulestmts.get(rulename);
		if ((stmts == null) || (stmts.size() == 0)) { // internal error, just print it
			System.err.println("Illegal call to rule " + rulename);
			return 0;
		}
		if (stmts.size() == 1) return runRule(rulename,max_cur_step+1); // no steps in body
		int result = 0;
		System.out.print("  Applying Rule " + rulename + " ... "); // debug
		try {
			int pos;
			PreparedStatement stmt;
			for (int i=1; i<stmts.size(); i++) {
				stmt = stmts.get(i);
				pos = 1;
				stmt.setInt(pos++, max_cur_step+1);
				for (int j=1; j<=i; j++) {
					stmt.setInt(pos++, min_cur_step);
				}
				stmt.setInt(pos++, max_cur_step);
				result = result + stmt.executeUpdate();
			}
		} catch (SQLException e) { // internal bug, just print the message
			System.err.println(e.toString());
		}
		System.out.println(" got " + result + " rows."); // debug
		return result;
	}

	/**
	 * Transform a rule declaration into a list of prepared SQL statements used to
	 * execute the rule. The first entry of the list is the version of the rule operating
	 * on all input data without additional step-based filters. All following statements
	 * correspond to variants of the rule where only certain combinations of steps are
	 * considered in the body, thus enabbling semi-naive evaluation.
	 * @param rd
	 * @return
	 */
	protected ArrayList<PreparedStatement> getInferenceRuleStatements(InferenceRuleDeclaration rd) {
		ArrayList<PreparedStatement> result = new ArrayList<PreparedStatement>();
		ArrayList<String> inferredTables = new ArrayList<String>();
		HashMap<String,ArrayList<String>> varequalities = new HashMap<String,ArrayList<String>>();
		HashMap<String,ArrayList<String>> constequalities = new HashMap<String,ArrayList<String>>();
		String from = "", insert = "", select = "", on = "", 
		       on_op = " WHERE ";  // use WHERE while there is just one table
		PredicateAtom pa;
		PredicateTerm pt;
		PredicateDeclaration pd;
		boolean hasParameterConstants = false; // constants of value "?" are set at application time;
		                                       // rules with such constants do not support semi-naive
		                                       // rewriting and can only be called when providing parameters
		
		// iterate over body to collect all variables and their variable->column mappings
		// also make join string for FROM here
		for (int i=0; i<rd.getBody().size(); i++) {
			pa = rd.getBody().get(i);
			pd = predicates.get(pa.getName());
			if (pd == null) continue; // ignore unknown predicates
			// build basic join string
			if (i>0) {
				from = from + " INNER JOIN ";
				on_op = " ON ";
			}
			from = from + pa.getName() + " AS t" + (i);
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
						if (pt.getValue().equals("0")) { // special handling: match all steps below 0
							constequalities.get(pt.getValue()).add( ">=t" + (i) + ".step" );
						} else {
							constequalities.get(pt.getValue()).add( "=t" + (i) + ".step" );
						}
					}
				}
			}
		}
		
		// make strings for INSERT and SELECT part
		insert = "INSERT IGNORE INTO " + rd.getHead().getName() + " (";
		pd = predicates.get(rd.getHead().getName()); // use "pd == null" to indicate that rule is broken
		for (int i=0; i<rd.getHead().getArguments().size(); i++) {
			if (i>0) insert = insert + ",";
			insert = insert + "f" + (i);
			pt = rd.getHead().getArguments().get(i);
			if (pt.isVariable()) {
				if (!select.equals("")) select = select + ",";
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
		if (pd == null) return result;
		if (pd.isInferred()) {
			insert = insert + ",step";
			select = select + ", ? AS step";
		}
		insert = insert + ") "; 
		select = "SELECT DISTINCT " + select;
		
		// make string for ON part (join conditions)
		Iterator<String> keyit = constequalities.keySet().iterator();
		String key;
		while (keyit.hasNext()) {
			key = keyit.next();
			for (int i=0; i<constequalities.get(key).size(); i++) {
				if (!on.equals("")) on = on + " AND ";
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
			for (int i=1; i<fields.size(); i++) {
				if (!on.equals("")) on = on + " AND ";
				on = on + fields.get(i) + "=" + fields.get((i+1)%fields.size());
			}
		}
	
		/// Now build the final rules ...
		// always make step-less version at index 0
		try {
			//System.out.println(rd.getName() + ":\n " + insert + select + " FROM " + from + on_op + on + "\n\n"); // DEBUG
			result.add(con.prepareStatement( insert + select + " FROM " + from + on_op + on));
		} catch (SQLException e) { // bug in the above code, just print it
			System.err.println(e.toString()); 
		}
		String sql;
		// other indices k hold semi-naive rule for the k-th inferred predicate 
		if ( (inferredTables.size() > 0) && (!hasParameterConstants) ) {
			// make rule variants for semi-naive evaluation			
			for (int i=0; i<inferredTables.size(); i++) {
				sql = insert + select + " FROM " + from + on_op + on;
				for (int j=0; j<=i; j++) {
					if (j<i) {
						sql = sql + " AND " + inferredTables.get(j) + ".step<?";
					} else {
						sql = sql + " AND " + inferredTables.get(j) + ".step>=? AND " + inferredTables.get(j) + ".step<=?";
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

	/* *** Id management *** */
	
	public int getID(OWLClassExpression description) throws SQLException {
		return getID(description.toString());
	}

	public int getID(OWLIndividual individual) throws SQLException {
		return getID(individual.toString());
	}
	
	public int getID(OWLObjectPropertyExpression property) throws SQLException {
		return getID(property.toString());
	}
	
	public int getID(String description) throws SQLException {
		int id = 0;
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
		} else { // id not available: find it in the DB or newly allocate it
			if (findid == null) findid = con.prepareStatement("SELECT id FROM ids WHERE name=? LIMIT 1");
			findid.setString(1, hash);
			ResultSet res = findid.executeQuery();
			if (res.next()) {
				id = res.getInt(1);
			} else {
				// check if we need to pre-allocate more ids (and commit recently created ids)
				if ( ( (curid>0) && (curid>maxid) ) || 
				     ( (curid==0) && ((prelocatedids == null) || (!prelocatedids.next())) ) ) {
					if (curid == 0) { // rely on AUTO INCREMENT
						if (prelocids == null) {
							String insertvals = "(NULL,\"-\")";
							for (int i=1; i<prelocsize; i++) {
								insertvals = insertvals.concat(",NULL,\"-\")");
							}
							prelocids = con.prepareStatement("INSERT INTO ids VALUES " + insertvals);//,Statement.RETURN_GENERATED_KEYS);
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
			}
			res.close();
			ids.put(hash,id);
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