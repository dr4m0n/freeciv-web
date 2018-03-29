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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.sql.*;
import java.util.Properties;
import javax.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

import javax.naming.*;

// TODO: Auto-generated Javadoc
/**
 * Sign in with a Google Account URL: /token_signin
 *
 * https://developers.google.com/identity/sign-in/web/backend-auth
 */
public class TokenSignin extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(TokenSignin.class);

	/** The Constant jacksonFactory. */
	private static final JacksonFactory jacksonFactory = new JacksonFactory();

	/** The google signin key. */
	private String google_signin_key;

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
			google_signin_key = prop.getProperty("google-signin-client-key");
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

		if (Constants.VALIDATING) {
			// will continue
		} else {
			try {
				response.getOutputStream().print("winner");
			} catch (Exception e) {
				LOGGER.error("ERROR!", e);
			}
			return;
		}

		String idtoken = request.getParameter("idtoken");
		String username = request.getParameter("username");
		Connection conn = null;
		PreparedStatement ps1 = null;
		PreparedStatement ps0 = null;
		ResultSet rs = null;
		String ipAddress = request.getHeader("X-Real-IP");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		try {
			LOGGER.info("google_signin_key=" + google_signin_key);

			if (google_signin_key == null || "".equals(google_signin_key.trim())) {

				try {
					response.getOutputStream().print("Invalid Key");
				} catch (Exception e) {
					LOGGER.error("ERROR!", e);
				}
				return;

			} else {
				// will continue
			}

			List<String> singletonList = Collections.singletonList(google_signin_key);
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), jacksonFactory)
					.setAudience(singletonList).build();
			if (verifier == null) {

				LOGGER.warn("verifier is null. Is this correct? Is this desirable?");

			} else {

				GoogleIdToken idToken = verifier.verify(idtoken);

				if (idToken == null) {

					LOGGER.warn("idToken is null. Is this correct? Is this desirable?");

				} else {

					Payload payload = idToken.getPayload();
					String userId = getUserId(payload);
					String email = getEmail(payload);
					boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
					if (!emailVerified) {
						response.getOutputStream().print("Email not verified");
						return;
					}

					Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
					DataSource ds = (DataSource) env.lookup(Constants.JDBC);
					conn = ds.getConnection();

					// 1. Check if username and userId is already stored in the database,
					// and check if the username and userId matches.
					String authQuery = QueryDesigner.getGoogleAuth();
					ps1 = conn.prepareStatement(authQuery);
					ps1.setString(1, username);
					ps1.setString(2, userId);
					rs = ps1.executeQuery();
					if (!rs.next()) {
						// if username or subject not found, then a new user.
						String query = QueryDesigner.insertGoogleAuth();
						ps0 = conn.prepareStatement(query);
						ps0.setString(1, username.toLowerCase());
						ps0.setString(2, userId);
						ps0.setString(3, email);
						ps0.setInt(4, 1);
						ps0.setString(5, ipAddress);
						ps0.executeUpdate();
						response.getOutputStream().print(userId);
					} else {
						String dbSubject = rs.getString(1);
						int dbActivated = rs.getInt(2);
						String Username = rs.getString(3);

						if (dbSubject != null && dbSubject.equalsIgnoreCase(userId) && dbActivated == 1 && username.equalsIgnoreCase(Username)) {
							// if username and userId matches, then login OK!
							response.getOutputStream().print(userId);
							String query = QueryDesigner.insertGoogleAuth();
							PreparedStatement preparedStatement = conn.prepareStatement(query);
							preparedStatement.setString(1, ipAddress);
							preparedStatement.setString(2, username.toLowerCase());
							preparedStatement.executeUpdate();
						} else {
							// if username and userId doesn't match, then login not OK!
							response.getOutputStream().print("Failed");
						}
					}
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

	/**
	 * @param payload
	 * @return
	 */
	private String getUserId(Payload payload) {
		String userId = payload.getSubject();
		if (userId == null) {
			userId = "";
		}
		return userId;
	}

	/**
	 * @param payload
	 * @return
	 */
	private String getEmail(Payload payload) {
		String email = payload.getEmail();
		if (email == null) {
			email = "";
		}
		return email;
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
