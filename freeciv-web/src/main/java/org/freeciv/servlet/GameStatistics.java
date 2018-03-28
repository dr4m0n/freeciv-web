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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.context.EnvSqlConnection;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * Lists: the number of running games, the number of single player games played, and the number of multi player games played.
 *
 * URL: /game/statistics
 */
public class GameStatistics extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(GameStatistics.class);

	/** The Constant HEADER_EXPIRES. */
	private static final String HEADER_EXPIRES = "Expires";

	/** The Constant CONTENT_TYPE. */
	private static final String CONTENT_TYPE = "application/json";

	/** The Constant INTERNAL_SERVER_ERROR. */
	private static final String INTERNAL_SERVER_ERROR = new JSONObject() //
			.put("statusCode", HttpServletResponse.SC_INTERNAL_SERVER_ERROR) //
			.put("error", "Internal server error.") //
			.toString();

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		logParams(request);

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			response.setContentType(CONTENT_TYPE);

			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			String query = QueryDesigner.getGameStats();

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			if (!rs.next()) {
				throw new Exception("Expected at least one row.");
			}

			JSONObject result = new JSONObject();
			result.put("ongoing", rs.getInt("ongoingGames"));
			JSONObject finishedGames = new JSONObject();
			finishedGames.put("singlePlayer", rs.getInt("totalSinglePlayerGames"));
			finishedGames.put("multiPlayer", rs.getInt("totalMultiPlayerGames"));
			result.put("finished", finishedGames);

			ZonedDateTime expires = ZonedDateTime.now(ZoneId.of("UTC")).plusHours(1);
			String rfc1123Expires = expires.format(DateTimeFormatter.RFC_1123_DATE_TIME);

			response.setHeader(HEADER_EXPIRES, rfc1123Expires);
			response.getOutputStream().print(result.toString());

		} catch (Exception e) {
			LOGGER.error("ERROR!", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getOutputStream().print(INTERNAL_SERVER_ERROR);
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
		LOGGER.info("request received!");
		Enumeration<String> params = request.getParameterNames();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			LOGGER.info(" * Parameter Name - " + paramName + ", Value - " + request.getParameter(paramName));
		}
	}	

}