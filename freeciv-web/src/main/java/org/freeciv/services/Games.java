package org.freeciv.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.context.EnvSqlConnection;
import org.freeciv.model.Game;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

// TODO: Auto-generated Javadoc
/**
 * The Class Games.
 */
public class Games {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(Games.class);

	/**
	 * Gets the multi player count.
	 *
	 * @return the multi player count
	 */
	public int getMultiPlayerCount() {
		String query;
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			connection = ds.getConnection();
			query = QueryDesigner.getMultiplayerCount();
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			rs.next();
			return rs.getInt("count");
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
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
		}
	}

	/**
	 * Gets the multi player games.
	 *
	 * @return the multi player games
	 */
	public List<Game> getMultiPlayerGames() {
		String query;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			query = QueryDesigner.getMultiplayerGames();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			List<Game> multiPlayerGames = new ArrayList<>();
			while (rs.next()) {
				Game game = new Game() //
						.setHost(rs.getString("host")) //
						.setPort(rs.getInt("port")) //
						.setVersion(rs.getString("version")) //
						.setPatches(rs.getString("patches")) //
						.setState(rs.getString("state")) //
						.setMessage(rs.getString("message")) //
						.setDuration(rs.getLong("duration")) //
						.setTurn(rs.getInt("turn")) //
						.setPlayers(rs.getInt("players"));
				multiPlayerGames.add(game);
			}
			return multiPlayerGames;
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
	 * Gets the single player count.
	 *
	 * @return the single player count
	 */
	public int getSinglePlayerCount() {
		String query;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			query = QueryDesigner.getSinglePlayerCount();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			rs.next();
			return rs.getInt("count");
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
	 * Gets the single player games.
	 *
	 * @return the single player games
	 */
	public List<Game> getSinglePlayerGames() {
		String query;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			query = QueryDesigner.getSinglePlayerGames();

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			List<Game> singlePlayerGames = new ArrayList<>();
			while (rs.next()) {
				Game game = new Game() //
						.setHost(rs.getString("host")) //
						.setPort(rs.getInt("port")) //
						.setVersion(rs.getString("version")) //
						.setPatches(rs.getString("patches")) //
						.setState(rs.getString("state")) //
						.setMessage(rs.getString("message")) //
						.setDuration(rs.getLong("duration")) //
						.setTurn(rs.getInt("turn")) //
						.setPlayers(rs.getInt("players")) //
						.setPlayer(rs.getString("player")) //
						.setFlag(rs.getString("flag"));

				singlePlayerGames.add(game);
			}
			return singlePlayerGames;
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
	 * Summary.
	 *
	 * @return the list
	 */
	public List<Game> summary() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();
			String query = QueryDesigner.getSummary();
			List<Game> multiplayerGames = new ArrayList<>();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			while (rs.next()) {
				multiplayerGames.add(new Game() //
						.setHost(rs.getString("host")) //
						.setPort(rs.getInt("port")) //
						.setVersion(rs.getString("version")) //
						.setPatches(rs.getString("patches")) //
						.setState(rs.getString("state")) //
						.setMessage(rs.getString("message")) //
						.setDuration(rs.getLong("duration")) //
						.setTurn(rs.getInt("turn")) //
						.setPlayers(rs.getInt("players")) //
				);
			}
			return multiplayerGames;
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

}
