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

import org.apache.commons.codec.digest.Crypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * Deactivate a user account.
 *
 * URL: /deactivate_user
 */
public class DeactivateUser extends HttpServlet {

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

		String username = request.getParameter("username");
		String secure_password = java.net.URLDecoder.decode(request.getParameter("sha_password"), "UTF-8");

		if (username == null || username.length() <= 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username. Please try again with another username.");
			return;
		}

		Connection conn = null;
		PreparedStatement ps0 = null;
		PreparedStatement ps1 = null;
		ResultSet rs = null;

		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			// Salted, hashed password.
			String saltHashQuery = QueryDesigner.getPasswordAuth();
			ps1 = conn.prepareStatement(saltHashQuery);
			ps1.setString(1, username);
			rs = ps1.executeQuery();
			if (!rs.next()) {
				response.getOutputStream().print("Failed");
				return;
			} else {
				String hashedPasswordFromDB = rs.getString(1);
				if (hashedPasswordFromDB.equals(Crypt.crypt(secure_password, hashedPasswordFromDB))) {

					String query = QueryDesigner.updateAuthDeactivate();
					PreparedStatement ps = conn.prepareStatement(query);
					ps.setString(1, username);
					int no_updated = ps.executeUpdate();
					if (no_updated == 1) {
						response.getOutputStream().print("OK!");
					} else {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
						return;
					}
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
					return;
				}
			}
		} catch (Exception e) {
			LOGGER.error("ERROR!", e);
			response.setHeader("result", "error");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to login");
		} finally {
			if (rs != null) {
				try {
					rs.close();
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
			if (ps1 != null) {
				try {
					ps1.close();
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