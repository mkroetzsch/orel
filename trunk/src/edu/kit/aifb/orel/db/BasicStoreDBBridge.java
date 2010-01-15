package edu.kit.aifb.orel.db;

import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;

/**
 * Class for implementing a simple caching interface for DB access, 
 * using batch updates for writing and caches for reading.
 * 
 * For now, we replicate a lot of code here in a dumb fashion for
 * all the tables. Maybe we can reduce the code needed later on.
 * 
 * @author markus
 */
public class BasicStoreDBBridge {
	protected int namefieldlength = 100;
	
	protected Connection con = null;
	protected MessageDigest digest = null;
	protected int maxbatchsize = 1000;

	// cache ids locally
	protected HashMap<String,Integer> ids = null;
	final protected int idcachesize = 1000;
	protected PreparedStatement findid = null;
	protected PreparedStatement makeids = null;
	protected PreparedStatement prelocids = null;
	protected ResultSet prelocatedids = null;
	
	// keep prepared statements in a map to process them with less code
	protected HashMap<String,PreparedStatement> prepstmts;
	protected HashMap<String,Integer> prepstmtsizes;

	public BasicStoreDBBridge(Connection connection) {
		con = connection;
		ids = new HashMap<String,Integer>(idcachesize);
		prepstmts = new HashMap<String,PreparedStatement>(idcachesize);
		prepstmtsizes = new HashMap<String,Integer>(idcachesize);
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// unlikely
		}
	}
	
	/**
	 * Make sure all batch inserts are flushed, even if they are
	 * below the maximum size.
	 */
	public void close() throws SQLException {
		Iterator<String> inserttables = prepstmts.keySet().iterator();
		PreparedStatement pstmt;
		String key;
		while (inserttables.hasNext()) {
			key = inserttables.next();
			pstmt = prepstmts.get(key);
			pstmt.executeBatch();
			pstmt.close();
		}
		prepstmts.clear();
		prepstmtsizes.clear();
		if (makeids != null) makeids.executeBatch();
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM ids WHERE name=\"-\""); // delete any unused pre-allocated ids
	}
	
	public void insertIdsToTable(String tablename, int id1, int id2) throws SQLException {
		insertIdsToTable(tablename,id1,id2,-1);
	}

	public void insertIdsToTable(String tablename, int id1, int id2, int id3) throws SQLException {
		PreparedStatement stmt = prepstmts.get(tablename);
		if (stmt == null) {
			stmt = getPreparedInsertStatement(tablename);
			prepstmts.put(tablename, stmt);
			prepstmtsizes.put(tablename, 0);
		}
		if (id1 >= 0) stmt.setInt(1, id1);
		if (id2 >= 0) stmt.setInt(2, id2);
		if (id3 >= 0) stmt.setInt(3, id3);
		stmt.addBatch();
		int cursize = prepstmtsizes.get(tablename)+1;
		if (cursize >= maxbatchsize) {
			stmt.executeBatch();
			prepstmtsizes.put(tablename,0);
		} else {
			prepstmtsizes.put(tablename,cursize);
		}
	}
	
	protected PreparedStatement getPreparedInsertStatement(String tablename) throws SQLException {
		if (tablename.equals("sco")) {
			return con.prepareStatement("INSERT IGNORE INTO sco VALUES (?,?,0)");
		} else if (tablename.equals("sv")) {
			return con.prepareStatement("INSERT IGNORE INTO sv VALUES (?,?,?,0)");
		} else if (tablename.equals("subconjunctionof")) {
			return con.prepareStatement("INSERT IGNORE INTO subconjunctionof VALUES (?,?,?)");
		} else if (tablename.equals("subpropertyof")) {
			return con.prepareStatement("INSERT IGNORE INTO subpropertyof VALUES (?,?)");
		} else if (tablename.equals("subpropertychain")) {
			return con.prepareStatement("INSERT IGNORE INTO subpropertychain VALUES (?,?,?)");
		} else if (tablename.equals("subsomevalues")) {
			return con.prepareStatement("INSERT IGNORE INTO subsomevalues VALUES (?,?,?)");
		} else if (tablename.equals("ids")) {
			return con.prepareStatement("INSERT IGNORE INTO ids VALUES (?,?)");
		}
		return null;
	}
	
	public int getID(OWLDescription description) throws SQLException {
		return getID(description.toString());
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
		if (ids.containsKey(hash)) {
			id = ids.get(hash).intValue();
		} else {
			if (findid == null) findid = con.prepareStatement("SELECT id FROM ids WHERE name=? LIMIT 1");
			findid.setString(1, hash);
			ResultSet res = findid.executeQuery();
			if (res.next()) {
				id = res.getInt(1);
			} else {
				if ((prelocatedids == null) || (!prelocatedids.next())) {
					if (prelocids == null) {
						String insertvals = "(NULL,\"-\")";
						for (int i=0; i<100; i++) {
							insertvals = insertvals.concat(",(NULL,\"-\")");
						}
						prelocids = con.prepareStatement("INSERT INTO ids VALUES " + insertvals);
					}
					if (prelocatedids != null) prelocatedids.close();
					if (makeids != null) makeids.executeBatch();
					prelocids.executeUpdate();
					prelocatedids = prelocids.getGeneratedKeys();
					prelocatedids.next();
				}
				id = prelocatedids.getInt(1);
				if (makeids == null) makeids = con.prepareStatement("UPDATE ids SET name=? WHERE id=?");
				makeids.setInt(2,id);
				makeids.setString(1,hash);
				makeids.addBatch();
				/*if (makeid == null) makeid = con.prepareStatement("INSERT INTO ids VALUES (NULL,?)");
				makeid.setString(1, hash);
				makeid.executeUpdate();
				ResultSet keys = makeid.getGeneratedKeys();
				if (keys.next()) {
					id = keys.getInt(1);
				} // else we are really out of luck, return 0
				keys.close();*/
			}
			res.close();
			if (ids.size() >= idcachesize) ids.clear(); // violent cache management but better than leaking memory
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
