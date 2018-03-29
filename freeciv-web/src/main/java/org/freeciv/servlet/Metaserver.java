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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.JSONObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

// TODO: Auto-generated Javadoc
/**
 * Displays the multiplayer games
 * 
 * URL: /meta/metaserver.
 */
@MultipartConfig
public class Metaserver extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(Metaserver.class);

	/** The Constant CONTENT_TYPE. */
	private static final String CONTENT_TYPE = "application/json";

	/** The Constant INTERNAL_SERVER_ERROR. */
	private static final String INTERNAL_SERVER_ERROR = new JSONObject() //
			.put("statusCode", HttpServletResponse.SC_INTERNAL_SERVER_ERROR) //
			.put("error", "Internal server error.") //
			.toString();

	/** The Constant FORBIDDEN. */
	private static final String FORBIDDEN = new JSONObject() //
			.put("statusCode", HttpServletResponse.SC_FORBIDDEN) //
			.put("error", "Forbidden.") //
			.toString();

	/** The Constant BAD_REQUEST. */
	private static final String BAD_REQUEST = new JSONObject() //
			.put("statusCode", HttpServletResponse.SC_BAD_REQUEST) //
			.put("error", "Bad Request.") //
			.toString();

	/** The Constant SERVER_COLUMNS. */
	private static final List<String> SERVER_COLUMNS = new ArrayList<>();

	static {
		SERVER_COLUMNS.add("version");
		SERVER_COLUMNS.add("patches");
		SERVER_COLUMNS.add("capability");
		SERVER_COLUMNS.add("state");
		SERVER_COLUMNS.add("ruleset");
		SERVER_COLUMNS.add("message");
		SERVER_COLUMNS.add("type");
		SERVER_COLUMNS.add("available");
		SERVER_COLUMNS.add("humans");
		SERVER_COLUMNS.add("serverid");
		SERVER_COLUMNS.add("host");
		SERVER_COLUMNS.add("port");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("resource")
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		logParams(request);

		String localAddr = request.getLocalAddr();
		String remoteAddr = request.getRemoteAddr();

		if ((localAddr == null) || (remoteAddr == null) || !localAddr.equals(remoteAddr)) {
			response.setContentType(CONTENT_TYPE);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getOutputStream().print(FORBIDDEN);
			return;
		}

		String serverIsStopping = request.getParameter("bye");
		String sHost = request.getParameter("host");
		String sPort = request.getParameter("port");
		String dropPlayers = request.getParameter("dropplrs");

		List<String> sPlUser = request.getParameterValues("plu[]") == null ? null : Arrays.asList(request.getParameterValues("plu[]"));
		List<String> sPlName = request.getParameterValues("pll[]") == null ? null : Arrays.asList(request.getParameterValues("pll[]"));
		List<String> sPlNation = request.getParameterValues("pln[]") == null ? null : Arrays.asList(request.getParameterValues("pln[]"));
		List<String> sPlFlag = request.getParameterValues("plf[]") == null ? null : Arrays.asList(request.getParameterValues("plf[]"));
		List<String> sPlType = request.getParameterValues("plt[]") == null ? null : Arrays.asList(request.getParameterValues("plt[]"));
		List<String> sPlHost = request.getParameterValues("plh[]") == null ? null : Arrays.asList(request.getParameterValues("plh[]"));
		List<String> variableNames = request.getParameterValues("vn[]") == null ? null : Arrays.asList(request.getParameterValues("vn[]"));
		List<String> variableValues = request.getParameterValues("vv[]") == null ? null : Arrays.asList(request.getParameterValues("vv[]"));

		Map<String, String> serverVariables = new HashMap<>();
		for (String serverParameter : SERVER_COLUMNS) {
			String parameter = request.getParameter(serverParameter);
			if (parameter != null) {
				serverVariables.put(serverParameter, parameter);
			}
		}

		// Data validation
		String query;
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
			response.setContentType(CONTENT_TYPE);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getOutputStream().print(BAD_REQUEST);
			return;
		}

		String hostPort = sHost + ':' + sPort;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			if (serverIsStopping != null) {
				query = QueryDesigner.deleteServer();
				ps = conn.prepareStatement(query);
				ps.setString(1, sHost);
				ps.setInt(2, port);
				ps.executeUpdate();

				query = QueryDesigner.deleteVariable();
				ps = conn.prepareStatement(query);
				ps.setString(1, hostPort);
				ps.executeUpdate();

				query = QueryDesigner.deletePlayer();
				ps = conn.prepareStatement(query);
				ps.setString(1, hostPort);
				ps.executeUpdate();
				return;
			}

			boolean isSettingPlayers = (sPlUser != null) && !sPlUser.isEmpty() //
					&& (sPlName != null) && !sPlName.isEmpty() //
					&& (sPlNation != null) && !sPlNation.isEmpty() //
					&& (sPlFlag != null) && !sPlFlag.isEmpty() //
					&& (sPlType != null) && !sPlType.isEmpty() //
					&& (sPlHost != null) && !sPlHost.isEmpty();

			if (isSettingPlayers || (dropPlayers != null)) {
				query = QueryDesigner.deletePlayer();
				ps = conn.prepareStatement(query);
				ps.setString(1, hostPort);
				ps.executeUpdate();

				if (dropPlayers != null) {
					query = QueryDesigner.updateServer();
					ps = conn.prepareStatement(query);
					ps.setString(1, sHost);
					ps.setInt(2, port);
					ps.executeUpdate();
				}

				if (isSettingPlayers) {

					query = QueryDesigner.insertPlayer();
					ps = conn.prepareStatement(query);
					try {
						for (int i = 0; i < sPlUser.size(); i++) {
							ps.setString(1, hostPort);
							ps.setString(2, sPlUser.get(i));
							ps.setString(3, sPlName.get(i));
							ps.setString(4, sPlNation.get(i));
							ps.setString(5, sPlFlag.get(i));
							ps.setString(6, sPlType.get(i));
							ps.setString(7, sPlHost.get(i));
							ps.executeUpdate();
						}
					} catch (IndexOutOfBoundsException e) {
						LOGGER.error("ERROR!", e);
						response.setContentType(CONTENT_TYPE);
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.getOutputStream().print(BAD_REQUEST);
						return;
					}
				}
			}

			// delete this variables that this server might have already set
			String sql = QueryDesigner.deleteVariable();
			ps = conn.prepareStatement(sql);
			ps.setString(1, hostPort);
			ps.executeUpdate();

			if ((!variableNames.isEmpty()) && (!variableValues.isEmpty())) {
				query = QueryDesigner.insertVariable();
				ps = conn.prepareStatement(query);
				try {
					for (int i = 0; i < variableNames.size(); i++) {
						ps.setString(1, hostPort);
						ps.setString(2, variableNames.get(i));
						ps.setString(3, variableValues.get(i));
						ps.executeUpdate();
					}
				} catch (IndexOutOfBoundsException e) {
					LOGGER.error("ERROR!", e);
					response.setContentType(CONTENT_TYPE);
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getOutputStream().print(BAD_REQUEST);
					return;
				}

			}

			query = QueryDesigner.getServerCount();
			ps = conn.prepareStatement(query);
			ps.setString(1, sHost);
			ps.setInt(2, port);
			rs = ps.executeQuery();

			boolean serverExists = rs.next() && (rs.getInt(1) == 1);

			List<String> setServerVariables = new ArrayList<>(serverVariables.keySet());
			StringBuilder queryBuilder = new StringBuilder();
			if (serverExists) {
				queryBuilder.append(" UPDATE servers SET ");
				for (String parameter : setServerVariables) {
					queryBuilder.append(parameter).append(" = ?, ");
				}
				queryBuilder.append(" stamp = CURRENT_TIMESTAMP ");
				queryBuilder.append(" WHERE host = ? AND port = ?");
			} else {
				queryBuilder = new StringBuilder("INSERT INTO servers ( stamp ");
				for (String parameter : setServerVariables) {
					queryBuilder.append(", ").append(parameter);
				}
				queryBuilder.append(" ) VALUES ( CURRENT_TIMESTAMP ");
				for (String parameter : setServerVariables) {
					queryBuilder.append(", ?");
				}
				queryBuilder.append(" )");
			}

			query = queryBuilder.toString();
			ps = conn.prepareStatement(query);
			int i = 1;
			for (String parameter : setServerVariables) {
				switch (parameter) {
				case "port":
				case "available":
					ps.setInt(i++, Integer.parseInt(serverVariables.get(parameter)));
					break;
				default:
					ps.setString(i++, serverVariables.get(parameter));
					break;
				}
			}
			if (serverExists) {
				ps.setString(i++, sHost);
				ps.setInt(i++, port);
			}
			ps.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("ERROR!", e);
			response.setContentType(CONTENT_TYPE);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getOutputStream().print(ExceptionUtils.getStackTrace(e));
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
	 * @param request
	 */
	protected void logParams(HttpServletRequest request) {
		LOGGER.info("request received! (loggin has been hidden for now)");
		Enumeration<String> params = request.getParameterNames();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			LOGGER.trace(" * Parameter Name - " + paramName + ", Value - " + request.getParameter(paramName));
		}
	}

}