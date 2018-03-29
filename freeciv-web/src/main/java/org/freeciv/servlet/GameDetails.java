/*******************************************************************************
 * Freeciv-web - the web version of Freeciv. http://play.freeciv.org/
 * Copyright (C) 2009-2017 The Freeciv-web project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.freeciv.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

// TODO: Auto-generated Javadoc
/**
 * Displays detailed information about a specific game
 * 
 * URL: /meta/game-details.
 */
@MultipartConfig
public class GameDetails extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(GameDetails.class);

	/**
	 * The Class PlayerSummary.
	 */
	public class PlayerSummary {

		/** The flag. */
		private String flag;

		/** The name. */
		private String name;

		/** The nation. */
		private String nation;

		/** The user. */
		private String user;

		/** The type. */
		private String type;

		/**
		 * Gets the flag.
		 *
		 * @return the flag
		 */
		public String getFlag() {
			return flag;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gets the nation.
		 *
		 * @return the nation
		 */
		public String getNation() {
			return nation;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Gets the user.
		 *
		 * @return the user
		 */
		public String getUser() {
			return user;
		}
	}

	/**
	 * The Class VariableSummary.
	 */
	public class VariableSummary {

		/** The name. */
		private String name;

		/** The value. */
		private String value;

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		logParams(request);

		String sHost = request.getParameter("host");
		String sPort = request.getParameter("port");

		int port;
		try {
			if (sPort == null) {
				throw new IllegalArgumentException("Port must be supplied.");
			}
			port = Integer.parseInt(sPort);
			if ((port < 1024) || (port > 65535)) {
				throw new IllegalArgumentException("Invalid port supplied. Expected a number between 1024 and 65535");
			}
			if (sHost == null) {
				throw new IllegalArgumentException("Host parameter is required to perform this request.");
			}
		} catch (IllegalArgumentException e) {
			LOGGER.error("ERROR!", e);
			RequestDispatcher rd = request.getRequestDispatcher("game-details.jsp");
			rd.forward(request, response);
			return;
		}

		String hostPort = sHost + ':' + sPort;
		String query;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			query = QueryDesigner.getServers();

			ps = conn.prepareStatement(query);
			ps.setString(1, sHost);
			ps.setInt(2, port);
			rs = ps.executeQuery();
			if (rs.next()) {
				request.setAttribute("version", rs.getString("version"));
				request.setAttribute("patches", rs.getString("patches"));
				request.setAttribute("capability", rs.getString("capability"));
				request.setAttribute("state", rs.getString("state"));
				request.setAttribute("ruleset", rs.getString("ruleset"));
				request.setAttribute("serverid", rs.getString("serverid"));
				request.setAttribute("port", port);
				request.setAttribute("host", sHost);
			} else {
				RequestDispatcher rd = request.getRequestDispatcher("game-information.jsp");
				rd.forward(request, response);
				return;
			}

			query = QueryDesigner.getPlayers();
			ps = conn.prepareStatement(query);
			ps.setString(1, hostPort);
			rs = ps.executeQuery();
			List<PlayerSummary> players = new ArrayList<>();
			while (rs.next()) {
				PlayerSummary player = new PlayerSummary();
				player.flag = rs.getString("flag");
				player.name = rs.getString("name");
				player.nation = rs.getString("nation");
				player.user = rs.getString("user");
				player.type = rs.getString("type");
				players.add(player);
			}
			request.setAttribute("players", players);

			query = QueryDesigner.getVariables();
			ps = conn.prepareStatement(query);
			ps.setString(1, hostPort);
			rs = ps.executeQuery();
			List<VariableSummary> variables = new ArrayList<>();
			while (rs.next()) {
				VariableSummary variable = new VariableSummary();
				variable.name = rs.getString("name");
				variable.value = rs.getString("value");
				variables.add(variable);
			}
			request.setAttribute("variables", variables);

		} catch (Exception err) {
			LOGGER.error("ERROR!", err);
			request.removeAttribute("state");
			RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp/game/details.jsp");
			rd.forward(request, response);
			return;
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

		RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp/game/details.jsp");
		rd.forward(request, response);
	}

	/**
	 * @param request
	 */
	protected void logParams(HttpServletRequest request) {
		LOGGER.info("request received!");
		Enumeration<String> params = request.getParameterNames();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			LOGGER.info(" * Parameter Name - " + paramName + ", Value - " + request.getParameter(paramName));
		}
	}

}