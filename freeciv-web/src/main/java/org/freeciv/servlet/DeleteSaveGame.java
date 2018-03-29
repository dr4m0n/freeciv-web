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
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;

// TODO: Auto-generated Javadoc
/**
 * Deletes a savegame.
 *
 * URL: /deletesavegame
 */
public class DeleteSaveGame extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(DeleteSaveGame.class);

	/** The Constant SAVEGAME_EXTENSION. */
	private static final String SAVEGAME_EXTENSION = ".sav.xz";

	/** The pattern validate alpha numeric. */
	private String PATTERN_VALIDATE_ALPHA_NUMERIC = "[0-9a-zA-Z\\.]*";

	/** The p. */
	private Pattern p = Pattern.compile(PATTERN_VALIDATE_ALPHA_NUMERIC);

	/** The savegame directory. */
	private String savegameDirectory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/WEB-INF/config.properties"));
			savegameDirectory = prop.getProperty("savegame_dir");
		} catch (IOException e) {
			LOGGER.error("ERROR!", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		logParams(request);

		String username = request.getParameter("username");
		String savegame = request.getParameter("savegame");
		String secure_password = java.net.URLDecoder.decode(request.getParameter("sha_password"), "UTF-8");

		if (!p.matcher(username).matches() || username.toLowerCase().equals("pbem")) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid username");
			return;
		}
		if (savegame == null || savegame.length() > 100 || savegame.contains(".") || savegame.contains("/") || savegame.contains("\\")) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid savegame");
			return;
		}

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			// Salted, hashed password.
			String saltHashQuery = QueryDesigner.getPasswordAuth();
			ps = conn.prepareStatement(saltHashQuery);
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (!rs.next()) {
				response.getOutputStream().print("Failed");
				return;
			} else {
				String hashedPasswordFromDB = rs.getString(1);
				if (hashedPasswordFromDB == null || secure_password == null) {
					response.getOutputStream().print("Failed auth when deleting.");
					return;
				}
				if (hashedPasswordFromDB.equals(Crypt.crypt(secure_password, hashedPasswordFromDB))) {
					// Login OK!
				} else {
					response.getOutputStream().print("Failed");
					return;
				}
			}

		} catch (Exception err) {
			LOGGER.error("ERROR!", err);
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

		try {
			if (savegame.equals("ALL")) {
				File folder = new File(savegameDirectory + "/" + username);
				if (!folder.exists()) {
					response.getOutputStream().print("Error!");
				} else {
					for (File savegameFile : folder.listFiles()) {
						if (savegameFile.exists() && savegameFile.isFile() && savegameFile.getName().endsWith(SAVEGAME_EXTENSION)) {
							Files.delete(savegameFile.toPath());
						}
					}
				}
			} else {
				File savegameFile = new File(savegameDirectory + username + "/" + savegame + SAVEGAME_EXTENSION);
				if (savegameFile.exists() && savegameFile.isFile() && savegameFile.getName().endsWith(SAVEGAME_EXTENSION)) {
					Files.delete(savegameFile.toPath());
				}
			}
		} catch (Exception err) {
			LOGGER.error("ERROR!", err);
			response.setHeader("result", "error");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR");
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