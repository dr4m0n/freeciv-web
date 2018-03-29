package org.freeciv.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

// TODO: Auto-generated Javadoc
/**
 * The Class Statistics.
 */
public class Statistics {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(Statistics.class);

	/**
	 * Gets the play by email winners.
	 *
	 * @return the play by email winners
	 */
	public List<Map<String, String>> getPlayByEmailWinners() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env;
			env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			String query = QueryDesigner.getPlayByEmailWinners();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			List<Map<String, String>> result = new ArrayList<>();
			while (rs.next()) {
				Map<String, String> record = new HashMap<>();
				record.put("winner", rs.getString("winner"));
				record.put("playerOne", rs.getString("playerOne"));
				record.put("playerTwo", rs.getString("playerTwo"));
				record.put("endDate", rs.getDate("endDate") + "");
				record.put("winsByPlayerOne", rs.getInt("winsByPlayerOne") + "");
				record.put("winsByPlayerTwo", rs.getString("winsByPlayerTwo") + "");
				result.add(record);
			}
			return result;
		} catch (SQLException e) {
			LOGGER.error("ERROR!", e);
			throw new RuntimeException(e);
		} catch (NamingException e) {
			LOGGER.error("ERROR!", e);
			throw new RuntimeException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
		}
	}

	/**
	 * Gets the played games by type.
	 *
	 * @return the played games by type
	 */
	public List<Map<String, Object>> getPlayedGamesByType() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			String query = QueryDesigner.getPlayedGamesByType();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			List<Map<String, Object>> result = new ArrayList<>();
			while (rs.next()) {
				Map<String, Object> item = new HashMap<>();
				item.put("date", rs.getDate("date"));
				item.put("webSinglePlayer", rs.getInt("webSinglePlayer"));
				item.put("webMultiPlayer", rs.getInt("webMultiPlayer"));
				item.put("webPlayByEmail", rs.getInt("webPlayByEmail"));
				item.put("webHotseat", rs.getInt("webHotseat"));
				item.put("desktopMultiplayer", rs.getInt("desktopMultiplayer"));
				item.put("webSinglePlayer3D", rs.getInt("webSinglePlayer3D"));
				result.add(item);
			}
			return result;
		} catch (Exception err) {
			LOGGER.error("ERROR!", err);
			throw new RuntimeException(err);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
		}
	}

	/**
	 * Gets the hall of fame list.
	 *
	 * @return the hall of fame list
	 */
	public List<Map<String, Object>> getHallOfFameList() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			String query = QueryDesigner.getHallOfFameList();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			List<Map<String, Object>> result = new ArrayList<>();
			int num = 1;
			while (rs.next()) {
				Map<String, Object> item = new HashMap<>();
				item.put("position", num);
				String username = rs.getString("username");
				if (username.length() >= 20)
					username = username.substring(0, 19) + "..";
				item.put("username", username);
				item.put("nation", rs.getString("nation"));
				item.put("score", rs.getString("score"));
				item.put("end_turn", rs.getString("end_turn"));
				item.put("end_date", rs.getString("end_date"));
				item.put("total_score", rs.getString("total_score"));
				item.put("id", Integer.parseInt(rs.getString("id")));
				result.add(item);
				num++;
			}
			return result;
		} catch (Exception e) {
			LOGGER.error("ERROR!", e);
			throw new RuntimeException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
		}
	}

}
