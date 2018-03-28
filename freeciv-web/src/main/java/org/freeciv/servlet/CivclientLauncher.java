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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.context.EnvSqlConnection;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;
import java.util.Enumeration;

import javax.sql.*;
import javax.naming.*;

// TODO: Auto-generated Javadoc
/**
 * This class is responsible for finding an available freeciv-web server for clients based on information in the metaserver database.
 *
 * URL: /civclientlauncher
 */
public class CivclientLauncher extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(DeactivateUser.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		logParams(request);

		// Parse input parameters ...
		String action = request.getParameter("action");
		if (action == null) {
			action = "new";
		}

		String civServerPort = request.getParameter("civserverport");

		Connection conn = null;
		PreparedStatement ps1 = null;
		PreparedStatement ps0 = null;
		ResultSet rs0 = null;
		ResultSet rs1 = null;

		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			String gameType;
			switch (action) {
			case "new":
			case "load":
			case "observe":
			case "multi":
			case "hotseat":
			case "earthload":
				gameType = "singleplayer";
				break;
			case "pbem":
				gameType = "pbem";
				break;
			default:
				response.setHeader("result", "invalid port validation");
				LOGGER.info("Unable to find a valid Freeciv server to play on. Please try again later.");
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to find a valid Freeciv server to play on. Please try again later.");
				return;
			}

			if (StringUtils.isEmpty(civServerPort)) {
				// If the user requested a new game, then get host and port for an available
				// server from the metaserver DB, and use that one.
				String lookupQuery = QueryDesigner.getPort();
				ps0 = conn.prepareStatement(lookupQuery);
				ps0.setString(1, gameType);
				rs0 = ps0.executeQuery();
				if (rs0.next()) {
					civServerPort = Integer.toString(rs0.getInt(1));
				} else {
					LOGGER.info("No servers available for creating a new game on.");
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No servers available for creating a new game on.");
					return;
				}
			}

			/* Validate port */
			/*
			String validateQuery = QueryDesigner.checkPort();
			ps1 = conn.prepareStatement(validateQuery);
			if (StringUtils.isEmpty(civServerPort)) {
				LOGGER.info("Unable to find a valid Freeciv server to play on. Please try again later.");
				response.setHeader("result", "invalid port validation");
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to find a valid Freeciv server to play on. Please try again later.");
				return;
			}

			ps1.setInt(1, Integer.parseInt(civServerPort));
			rs1 = ps1.executeQuery();
			rs1.next();
			if (rs1.getInt(1) != 1) {
				LOGGER.info("Invalid input values to civclient.");
				response.setHeader("result", "invalid port validation");
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input values to civclient.");
				return;
			}
			*/

		} catch (Exception err) {
			LOGGER.error("SQL ERROR!", err);
			response.setHeader("result", err.getMessage());
			return;
		} finally {
			if (rs1 != null) {
				try {
					rs1.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (rs0 != null) {
				try {
					rs0.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (ps1 != null) {
				try {
					ps1.close();
				} catch (SQLException e) {
					LOGGER.error("ERROR!", e);
				}
			}
			if (ps0 != null) {
				try {
					ps0.close();
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

		response.setHeader("port", civServerPort);
		response.setHeader("result", "success");
		response.setHeader("action", action);
		response.getOutputStream().print("success");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		LOGGER.warn("This endpoint only supports the POST method.");
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This endpoint only supports the POST method.");
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
