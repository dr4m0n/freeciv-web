/**
 * 
 */
package org.freeciv.utils;

/**
 * @author eolaso
 *
 */
public class QueryDesigner {

	/**
	 * Private Constructor
	 */
	private QueryDesigner() {

	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getMultiplayerCount() {
		return "SELECT COUNT(*) AS count " //
				+ " FROM servers s " //
				+ " WHERE type IN ('multiplayer', 'longturn') " //
				+ "	AND ( " //
				+ "	state = 'Running' OR " //
				+ "	(state = 'Pregame' " //
				+ "	AND CONCAT(s.host ,':',s.port) IN ( " //
				+ " SELECT hostport FROM players WHERE type <> 'A.I.') )" //
				+ "	)";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getMultiplayerGames() {
		return "SELECT host, port, version, patches, state, message, " //
				+ " DATEDIFF(SECOND, CURRENT_TIMESTAMP, stamp) AS duration, " //
				+ "	(SELECT COUNT(*) " //
				+ "	FROM players " //
				+ "	WHERE type = 'Human' " //
				+ "	AND hostport = CONCAT(s.host ,':',s.port) " //
				+ "	) AS players," //
				+ "	(SELECT value " //
				+ "	FROM variables " //
				+ "	WHERE name = 'turn' " //
				+ "	AND hostport = CONCAT(s.host ,':',s.port)" //
				+ "	) AS turn " //
				+ " FROM servers s " //
				+ " WHERE type IN ('multiplayer', 'longturn') OR message LIKE '%Multiplayer%' " //
				+ " ORDER BY humans DESC, state DESC";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getSinglePlayerCount() {
		return "SELECT COUNT(*) AS count " //
				+ " FROM servers s " //
				+ " WHERE type = 'singleplayer' AND state = 'Running'";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getSinglePlayerGames() {
		return " SELECT host, port, version, patches, state, message, " //
				+ "	DATEDIFF(SECOND, CURRENT_TIMESTAMP, stamp) AS duration, " //
				+ "	ISNULL(" //
				+ "	(SELECT TOP 1 [user] FROM players p WHERE p.hostport = CONCAT(s.host ,':',s.port) AND p.type = 'Human')," //
				+ "	'none') AS player, " //
				+ "	ISNULL(" //
				+ "	(SELECT TOP 1 flag FROM players p WHERE p.hostport = CONCAT(s.host ,':',s.port) AND p.type = 'Human'), " //
				+ "	'none') AS flag, " //
				+ "	(SELECT value " //
				+ "	FROM variables " //
				+ "	WHERE name = 'turn' " //
				+ "	AND hostport = CONCAT(s.host ,':',s.port)) + 0 AS turn, " //
				+ "	(SELECT COUNT(*) " //
				+ "	FROM players " //
				+ "	WHERE hostport = CONCAT(s.host ,':',s.port)) AS players, " //
				+ "	(SELECT value " //
				+ "	FROM variables " //
				+ "	WHERE name = 'turn' " //
				+ "	AND hostport = CONCAT(s.host ,':',s.port)) + 0 AS turnsort " //
				+ "	FROM servers s " //
				+ " WHERE type = 'singleplayer' " //
				+ " AND state = 'Running' " //
				+ " AND message NOT LIKE '%Multiplayer%' " //
				+ " ORDER BY turnsort DESC";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getSummary() {
		return "" //
				+ " ( " //
				+ " SELECT host, port, version, patches, state, message, " //
				+ " DATEDIFF(SECOND, CURRENT_TIMESTAMP, stamp) AS duration, " //
				+ " ( SELECT value " //
				+ " FROM variables " //
				+ " WHERE name = 'turn' AND hostport = CONCAT(s.host, ':', s.port) " //
				+ " ) AS turn, " //
				+ " ( SELECT COUNT(*) FROM players WHERE type = 'Human' AND hostport = CONCAT(s.host, ':', s.port)) AS players " //
				+ " FROM servers s " //
				+ " WHERE message NOT LIKE '%Private%' AND type IN ('multiplayer', 'longturn') AND state = 'Running' " //
				+ " ) " //
				+ " UNION " //
				+ " ( " //
				+ " SELECT TOP 1 host, port, version, patches, state, message, " //
				+ " DATEDIFF(SECOND, CURRENT_TIMESTAMP, stamp) AS duration, " //
				+ " ( SELECT value " //
				+ " FROM variables " //
				+ " WHERE message NOT LIKE '%Private%' AND name = 'turn' AND hostport = CONCAT(s.host, ':', s.port) " //
				+ " ) AS turn, " //
				+ " ( SELECT COUNT(*) FROM players WHERE type = 'Human' AND hostport = CONCAT(s.host, ':', s.port)) AS players " //
				+ " FROM servers s " //
				+ " WHERE message NOT LIKE '%Private%' " //
				+ " AND type IN ('multiplayer', 'longturn') " //
				+ " AND state = 'Pregame' " //
				+ " AND CONCAT(s.host ,':',s.port) IN (SELECT hostport FROM players WHERE type <> 'A.I.') " //
				+ " ) " //
				+ " UNION " //
				+ " ( " //
				+ " SELECT TOP 2 host, port, version, patches, state, message, " //
				+ " DATEDIFF(SECOND, CURRENT_TIMESTAMP, stamp), " //
				+ " ( SELECT value " //
				+ " FROM variables " //
				+ " WHERE name = 'turn' AND hostport = CONCAT(s.host, ':', s.port) " //
				+ " ) AS turn, " //
				+ " ( SELECT COUNT(*) FROM players WHERE type = 'Human' AND hostport = CONCAT(s.host, ':', s.port)) AS players " //
				+ " FROM servers s " //
				+ " WHERE type IN ('multiplayer', 'longturn') AND state = 'Pregame' " //
				+ " ) ";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getPlayByEmailWinners() {
		return "SELECT TOP 20 winner, r.playerOne, r.playerTwo, endDate, "
				+ " ( SELECT COUNT(*) FROM game_results r2 WHERE r2.winner = r.playerOne) AS winsByPlayerOne, " //
				+ " ( SELECT COUNT(*) FROM game_results r3 WHERE r3.winner = r.playerTwo) AS winsByPlayerTwo " //
				+ " FROM game_results r " //
				+ " ORDER BY id DESC";
	}

	/**
	 * @return
	 */
	public static String getPlayByEmailTopPlayers() {
		return " SELECT TOP 10 winner AS player, COUNT(winner) AS wins " //
				+ " FROM game_results " //
				+ " GROUP BY winner " //
				+ " ORDER BY wins DESC";
	}

	/**
	 * @return
	 */
	public static String getPlayedGamesByType() {
		return " SELECT DISTINCT statsDate AS date, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '0') AS webSinglePlayer, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '1') AS webMultiPlayer, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '2') AS webPlayByEmail, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '4') AS webHotseat, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '3') AS desktopMultiplayer, " //
				+ " (SELECT gameCount FROM games_played_stats WHERE statsDate = date AND gameType = '5') AS webSinglePlayer3D " //
				+ "FROM games_played_stats";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getHallOfFameList() {
		return "SELECT TOP 500 " //
				+ " id, username, nation, score, end_turn, end_date, " //
				+ " (select sum(s.score) from hall_of_fame s where s.username = a.username) as total_score " //
				+ " FROM hall_of_fame a " //
				+ " ORDER BY score DESC";
	}

	/**
	 * @return
	 */
	public static String checkPort() {
		return "SELECT COUNT(*) " //
				+ " FROM servers " //
				+ " WHERE port = ?";
	}

	/**
	 * @return
	 */
	public static String getPort() {
		return "SELECT TOP 1 port " //
				+ " FROM servers " //
				+ " WHERE state = 'Pregame' AND type = ? AND humans = '0' " //
				+ "ORDER BY RAND()";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getPasswordAuth() {
		return "SELECT TOP 1 secure_hashed_password " //
				+ " FROM auth " //
				+ " WHERE LOWER(username) = LOWER(?) " //
				+ "	AND activated = '1'";

	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String checkAuth() {
		return "SELECT username, activated " //
				+ "FROM auth " //
				+ "WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)";
	}

	/**
	 * @return
	 */
	public static String updateGameStats() {
		return "INSERT INTO " //
				+ " games_played_stats (statsDate, gameType, gameCount) " //
				+ " VALUES (CURDATE(), ?, 1) " //
				+ " ON DUPLICATE KEY UPDATE gameCount = gameCount + 1";
	}

	/**
	 * @return
	 */
	public static String getServers() {
		return "SELECT * " //
				+ " FROM servers " //
				+ " WHERE host = ? AND port = ?";
	}

	/**
	 * @return
	 */
	public static String getPlayers() {
		return "SELECT * " //
				+ " FROM players " //
				+ " WHERE hostport = ? " //
				+ " ORDER BY name";
	}

	/**
	 * @return
	 */
	public static String getVariables() {
		return "SELECT * " //
				+ " FROM variables " //
				+ " WHERE hostport = ? " //
				+ " ORDER BY name";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getGameStats() {
		return "SELECT " //
				+ "	(SELECT COUNT(*) FROM servers WHERE state = 'Running') AS ongoingGames, " //
				+ "	(SELECT SUM(gameCount) FROM games_played_stats WHERE gametype IN (0, 5)) AS totalSinglePlayerGames, " //
				+ "	(SELECT SUM(gameCount) FROM games_played_stats WHERE gametype IN (1, 2)) AS totalMultiPlayerGames";
	}

	/**
	 * @return
	 */
	public static String updateHallOfFame() {
		return "INSERT INTO " //
				+ " hall_of_fame (username, nation, score, end_turn, end_date, ip) " //
				+ " VALUES (?, ?, ?, ?, " + getNow() + ", ?)";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getMaxIdHallOfFame() {
		return "SELECT MAX(id) " //
				+ " FROM hall_of_fame ";
	}

	/**
	 * @return
	 */
	public static String insertAuth() {
		return "INSERT INTO " //
				+ " auth (username, email, secure_hashed_password, activated, ip) " //
				+ " VALUES (?, ?, ?, ?, ?)";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getTopAuth() {
		return "SELECT TOP 1 username " //
				+ " FROM auth " //
				+ " WHERE activated='1' " //
				+ " AND id >= (SELECT FLOOR(MAX(id) * RAND()) FROM auth) " //
				+ " ORDER BY id;";
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getServerStats() {
		if (Constants.DATABASETYPE.equals("sqlserver")) {
			return "SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	WHERE type = 'singleplayer' " //
					+ "	AND state = 'Pregame' " //
					+ "	AND humans = '0' " //
					+ "	AND stamp >= DATEADD(MINUTE, -1, GETDATE()) " //
					+ "	UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	WHERE type = 'multiplayer' " //
					+ "	AND state = 'Pregame' " //
					+ "	AND stamp >= DATEADD(MINUTE, -1, GETDATE()) " //
					+ "	UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	WHERE type = 'pbem' " //
					+ "	AND state = 'Pregame' " //
					+ "	AND stamp >= DATEADD(MINUTE, -1, GETDATE())";
		} else if (Constants.DATABASETYPE.equals("mysqlserver")) {
			return "SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ " UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	WHERE type = 'singleplayer' " //
					+ " AND state = 'Pregame' " //
					+ " AND humans = '0' " //
					+ " AND stamp >= DATE_SUB(NOW(), INTERVAL 1 MINUTE) " //
					+ " UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ "	WHERE type = 'multiplayer' " //
					+ "	AND state = 'Pregame' " //
					+ "	AND stamp >= DATE_SUB(NOW(), INTERVAL 1 MINUTE) " //
					+ " UNION ALL " //
					+ "	SELECT COUNT(*) AS count " //
					+ "	FROM servers " //
					+ " WHERE type = 'pbem' " //
					+ " AND state = 'Pregame' " //
					+ " AND stamp >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)";
		} else {
			return null;
		}
	}

	/**
	 * 
	 * info : checked correctly against azure sqleditor
	 * 
	 * @return
	 */
	public static String getGoogleAuth() {
		return "SELECT subject, activated, username " //
				+ " FROM google_auth " //
				+ " WHERE LOWER(username) = LOWER(?) OR subject = ?";
	}

	/**
	 * @return
	 */
	public static String insertGoogleAuth() {
		return "INSERT INTO " //
				+ " google_auth (username, subject, email, activated, ip) " //
				+ " VALUES (?, ?, ?, ?, ?)";
	}

	public static String updateGoogleAuth() {
		return "UPDATE google_auth SET ip = ? " //
				+ " WHERE LOWER(username) = ?";
	}

	/**
	 * @return
	 */
	public static String getAuthStats() {
		return "SELECT COUNT(*) FROM auth";
	}

	/**
	 * @return
	 */
	public static String getAuth() {
		return "SELECT username " //
				+ " FROM auth " //
				+ " WHERE email = ?";
	}

	/**
	 * @return
	 */
	public static String updateAuthChangePassword() {
		return "UPDATE auth SET secure_hashed_password = ? " //
				+ " WHERE email = ? " //
				+ " AND activated = 1";
	}

	/**
	 * @return
	 */
	public static String updateAuthDeactivate() {
		return "UPDATE auth SET activated = '0' " + //
				" WHERE username = ? ";
	}

	/**
	 * @return
	 */
	public static String getServerCount() {
		return "SELECT COUNT(*) " //
				+ " FROM servers " //
				+ " WHERE host = ? AND port = ?";
	}

	/**
	 * @return
	 */
	public static String deleteServer() {
		return "DELETE FROM servers WHERE host = ? AND port = ?";
	}

	/**
	 * @return
	 */
	public static String deleteVariable() {
		return "DELETE FROM variables WHERE hostport = ?";
	}

	/**
	 * @return
	 */
	public static String deletePlayer() {
		return "DELETE FROM players WHERE hostport = ?";
	}

	/**
	 * @return
	 */
	public static String insertVariable() {
		return "INSERT INTO " //
				+ " variables (hostport, name, value) " //
				+ " VALUES (?, ?, ?)";
	}

	/**
	 * @return
	 */
	public static String insertPlayer() {
		return "INSERT INTO " //
				+ " players (hostport, [user], name, nation, flag, type, host) " //
				+ " VALUES (?, ?, ?, ?, ?, ?, ?)";
	}

	/**
	 * @return
	 */
	public static String updateServer() {
		return "UPDATE servers SET " //
				+ " available = 0, humans = -1, stamp = CURRENT_TIMESTAMP " //
				+ " WHERE host = ? AND port = ?";
	}

	/**
	 * @param typeDDBB
	 * @return
	 */
	private static String getNow() {
		if (Constants.DATABASETYPE.equals("sqlserver")) {
			return "GETUTCDATE()";
		} else if (Constants.DATABASETYPE.equals("mysqlserver")) {
			return "NOW()";
		} else {
			return null;
		}
	}

}
