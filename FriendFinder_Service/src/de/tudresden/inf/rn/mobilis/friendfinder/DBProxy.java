package de.tudresden.inf.rn.mobilis.friendfinder;

import java.io.BufferedWriter;
import static java.nio.file.StandardCopyOption.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

import de.tudresden.inf.rn.mobilis.friendfinder.proxy.ClientLocation;

/**
 * this enclose all methods for accessing the database
 * for easy exchange of the db, there was a jdbc-driver used
 */
public class DBProxy {
	private final String TABLE_NODES = "nodes";
	private final String TABLE_TRACKS = "tracks";
	/**
	 * maximum error between two points to detect as the same one
	 */
	private final float error_bound = 20; // in meter
	
	private String db_name;

	private final static Logger LOG = Logger.getLogger(DBProxy.class
			.getCanonicalName());

	private Connection c = null;

	public DBProxy() {
		this.connect();
	}

	/**
	 * connect to the database
	 */
	public void connect() {
		try {
			db_name =  "friendfinder_db\\eet_"+System.currentTimeMillis()+".db";
			File origin = new File("friendfinder_db\\eet.db");
			if(origin.exists()) Files.copy(origin.toPath(), new File(db_name).toPath(), REPLACE_EXISTING);
			
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+db_name);
			LOG.info("open database successful");
		} catch (Exception e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}

	/**
	 * close the database-connection
	 */
	public void close() {
		if (c != null) {
			try {
				c.close();
			} catch (SQLException e) {
				LOG.severe("!EXCEPTION " + e.toString());
			}
		}
	}

	/************* insert ********************/
	/**
	 * insert some mock-data
	 */
	public void insertMock() {
		int key = insertTrack(1, System.currentTimeMillis());
		insertNode(51.2, 13.4, key, "on_bicycle", System.currentTimeMillis(),
				5.6f);
		insertNode(51.22, 13.42, key, "on_bicycle", System.currentTimeMillis(),
				5.6f);
	}
	/**
	 * insert a gpx-track
	 * @param gpx
	 */
	public void insertGPX(GPXTrack gpx) {
		int trkKey = this.insertTrack(1, gpx.time);
		ArrayList<GPXTrack.Trkpt> pts = gpx.getTrackPoints();
		for (GPXTrack.Trkpt pt : pts) {
			if (hasNode(pt.lat, pt.lon) == false) {
				this.insertNode(pt.lat, pt.lon, trkKey, pt.activity, pt.time,
						pt.speed);
			} else
				LOG.info("node already exists");
		}
	}
	/**
	 * insert one track with frequency and time
	 * @param frequency
	 * @param time
	 * @return the id of the inserted track
	 */
	public int insertTrack(int frequency, long time) {
		try {
			Statement stmt = c.createStatement();
			String sql = "INSERT INTO " + TABLE_TRACKS
					+ " (id, frequency, tr_time) VALUES (NULL, " + frequency
					+ ", " + time + ");";
			stmt.executeUpdate(sql);

			sql = "SELECT LAST_INSERT_ROWID()";
			ResultSet rs = stmt.executeQuery(sql);
			int key = 0;
			while (rs.next())
				key = rs.getInt(1);

			stmt.close();
			return key;
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
			return 0;
		}
	}
	/**
	 * insert one node
	 * @param cl
	 * @param track_fk
	 */
	public void insertNode(ClientLocation cl, int track_fk) {
		insertNode(cl.getLat(), cl.getLng(), track_fk, cl.getActivity(),
				cl.getTime(), cl.getSpeed());
	}
	/**
	 * insert one node
	 * @param lat
	 * @param lon
	 * @param track_fk
	 * @param activity
	 * @param time
	 * @param speed
	 */
	public void insertNode(double lat, double lon, int track_fk,
			String activity, long time, float speed) {
		try {
			Statement stmt = c.createStatement();
			String sql = "INSERT INTO " + TABLE_NODES
					+ " (id, lat, lon, track_fk, act, time, speed)"
					+ " VALUES (NULL, " + lat + "," + lon + "," + track_fk
					+ ",'" + activity + "'," + time + "," + speed + ");";
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}
	/**
	 * divide a track in two indepentend tracks with different track-ids
	 * @param trackId
	 * @param nodeId
	 */
	public void divideTrack(int trackId, int nodeId) {

		try {
			Statement stmt = c.createStatement();
			String sql = "SELECT * FROM " + TABLE_TRACKS + " WHERE id = "
					+ trackId;
			ResultSet rs = stmt.executeQuery(sql);
			long time = System.currentTimeMillis();
			int frequency = 1;
			if (rs.next()) {
				time = rs.getLong("tr_time");
				frequency = rs.getInt("frequency");
			}

			int newTrackId = insertTrack(frequency, time);

			sql = "UPDATE " + TABLE_NODES + " SET track_fk = " + newTrackId
					+ " WHERE track_fk = " + trackId + " AND id > " + nodeId;
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}
	
	/**
	 * clean up the db
	 * - delete short tracks
	 */
	public void cleanDB(){
		try {
			Statement stmt = c.createStatement();
			
			String sql = "DELETE FROM " + TABLE_NODES + " WHERE " + TABLE_NODES + ".track_fk IN "
					+ "( SELECT t.id FROM " + TABLE_NODES + " AS n INNER JOIN " + TABLE_TRACKS + " AS t"
					+ " ON n.track_fk = t.id"
					+ " GROUP BY t.id HAVING count(t.id) < 3 );";
			stmt.executeUpdate(sql);
			
			String sql2 = "DELETE FROM " + TABLE_TRACKS + " WHERE " + TABLE_TRACKS + ".id NOT IN (SELECT n.track_fk FROM " + TABLE_NODES + " AS n);";
			stmt.executeUpdate(sql2);
			stmt.close();
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}
	
	/**
	 * increase the frequency-field of a track
	 * @param trackId
	 */
	public void rateTrack(int trackId){
		try {
			Statement stmt = c.createStatement();
			String sql = "UPDATE " + TABLE_TRACKS + " SET frequency = frequency + 1 WHERE id = " + trackId;
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}

	/******************** select ***************/
	/**
	 * is there a track for the given coordinates in the db
	 * @param lat
	 * @param lon
	 * @return
	 */
	public Boolean hasNode(double lat, double lon) {
		try {
			Statement stmt = c.createStatement();
			String sql = "SELECT count(*) FROM " + TABLE_NODES
					+ " WHERE lat < " + (lat + error_bound) + " AND lat > "
					+ (lat - error_bound) + " AND lon < " + (lon + error_bound)
					+ " AND lon > " + (lon - error_bound) + ";";
			ResultSet rs = stmt.executeQuery(sql);
			LOG.info("count: " + rs.getInt(1));
			return (rs.getInt(1) > 0) ? true : false;
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
			return null;
		}
	}
	/**
	 * get all matching nodes which are in the range of error_bound from the given coordinates joined with the track-frequency
	 * @param lat
	 * @param lon
	 * @param acc
	 * @return
	 */
	public ArrayList<Node> getMatchingNodes(double lat, double lon, double acc) {
		ArrayList<Node> list = new ArrayList<Node>();

		double lat_er = (acc + error_bound) / 111000;
		double lat_min = lat - lat_er;
		double lat_max = lat + lat_er;
		double lon_er = (acc + error_bound)
				/ (Math.abs(Math.cos(Math.toRadians(lat))) * 111000);
		double lon_min = lon - lon_er;
		double lon_max = lon + lon_er;

		try {
			Statement stmt = c.createStatement();
			String sql = "SELECT n.id, n.lat, n.lon, n.track_fk, n.act, n.time, n.speed, t.frequency "
					+ "FROM "
					+ TABLE_NODES
					+ " AS n INNER JOIN "
					+ TABLE_TRACKS
					+ " AS t ON t.id = n.track_fk "
					+ " WHERE n.lat < "
					+ lat_max
					+ " AND n.lat > "
					+ lat_min
					+ " AND n.lon < "
					+ lon_max
					+ " AND n.lon > "
					+ lon_min
					+ " ORDER BY t.frequency DESC;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				list.add(new Node(rs.getInt("id"), rs.getDouble("lat"), rs
						.getDouble("lon"), rs.getInt("track_fk"), rs
						.getString("act"), rs.getLong("time"), rs
						.getFloat("speed"), rs.getInt("frequency")));
			}
			stmt.close();
			return list;
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
			return null;
		}
	}

	/**
	 * select one track
	 * @param trackId
	 * @return the found track as a gpx-track
	 */
	public GPXTrack selectTrack(int trackId) {
		GPXTrack gpx = new GPXTrack();
		gpx.trkName = Integer.toString(trackId);

		try {
			Statement stmt = c.createStatement();
			String sql = "SELECT * FROM " + TABLE_NODES + " WHERE track_fk = "
					+ trackId + " LIMIT 200;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				gpx.addTrackPoint(rs.getDouble("lat"), rs.getDouble("lon"),
						rs.getString("act"), rs.getFloat("speed"),
						rs.getLong("time"), 0, 0);
			}
			stmt.close();
			return gpx;

		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
			return null;
		}
	}

	/******************* other *****************/

	/**
	 * save database to a gpx-file with multiple trk-sections
	 */
	public void saveGPX() {
		int track_fk = 0;
		GPXTrack gpx = null;
		try {
			// Create file
			FileWriter fstream = new FileWriter(db_name + ".gpx");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("<gpx>");
		
			Statement stmt = c.createStatement();
			String sql = "SELECT n.id, n.lat, n.lon, n.track_fk, n.act, n.time, n.speed, t.frequency "
					+ "FROM " + TABLE_NODES	+ " AS n INNER JOIN " + TABLE_TRACKS + " AS t ON t.id = n.track_fk "
					+ " ORDER BY t.id ASC, n.id ASC;";
			ResultSet rs = stmt.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt("track_fk") != track_fk) {
					if (gpx != null)
						out.write(gpx.trackToXML(true));
					
					track_fk = rs.getInt("track_fk");
					gpx = new GPXTrack();
					gpx.trkName = Integer.toString(track_fk) + " (Rate: " + rs.getString("frequency") + ")";
					gpx.time = rs.getLong("time");
				}
				gpx.addTrackPoint(rs.getDouble("lat"), rs.getDouble("lon"),
						rs.getString("act"), rs.getFloat("speed"));
			}
			if (gpx != null)
				out.write(gpx.trackToXML(true));
			
			stmt.close();
			out.write("</gpx>");
			out.close();
			fstream.close();
			
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		} catch (Exception e) {
			System.err.println("Error: " + e.toString());
		}
	}

	/**
	 * print db to the console
	 */
	public void printData() {
		Statement stmt;
		try {
			stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE_NODES
					+ ";");
			StringBuilder out = new StringBuilder();
			out.append("\n" + TABLE_NODES + ":");
			while (rs.next()) {
				out.append("\n[" + rs.getInt("id") + ", " + rs.getDouble("lat")
						+ ", " + rs.getDouble("lon") + ", "
						+ rs.getInt("track_fk") + ", " + rs.getString("act")
						+ ", " + rs.getInt("time") + ", "
						+ rs.getFloat("speed") + "]");
			}

			rs = stmt.executeQuery("SELECT * FROM " + TABLE_TRACKS + ";");
			out.append("\n\n" + TABLE_TRACKS + ":");
			while (rs.next()) {
				out.append("\n[" + rs.getInt("id") + ", "
						+ rs.getInt("frequency") + ", " + rs.getLong("tr_time")
						+ "]");
			}
			rs.close();
			stmt.close();
			LOG.info(out.toString());
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}

	/**
	 * create tables if not exists
	 */
	public void createTables() {
		try {
			Statement stmt = c.createStatement();

			String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NODES
					+ "(id INTEGER PRIMARY KEY, " + " lat REAL NOT NULL, "
					+ " lon REAL NOT NULL, " + " track_fk INTEGER NOT NULL, "
					+ " act TEXT NOT NULL, " + " time INTEGER NOT NULL, "
					+ " speed REAL NOT NULL)";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE IF NOT EXISTS " + TABLE_TRACKS
					+ "(id INTEGER PRIMARY KEY, "
					+ " frequency INTEGER NOT NULL, "
					+ " tr_time INTEGER NOT NULL)";
			stmt.executeUpdate(sql);

			stmt.close();

			LOG.info("Table created successfully");
		} catch (SQLException e) {
			LOG.severe("!EXCEPTION " + e.toString());
		}
	}

	/**
	 * representation of a node with id and track-attributes
	 *
	 */
	public class Node {
		int id, track_fk, frequency;
		double lat, lon;
		String act;
		long time;
		float speed;

		public Node(int id, double lat, double lon, int track_fk, String act,
				long time, float speed, int frequency) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.track_fk = track_fk;
			this.act = act;
			this.time = time;
			this.speed = speed;
			this.frequency = frequency;
		}

		@Override
		public String toString() {
			return "Node: id " + id + ", trackFk " + track_fk + ", N" + lat
					+ " E" + lon;
		}

	}
}
