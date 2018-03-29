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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;
import java.util.Properties;

import javax.sql.*;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

import javax.naming.*;

// TODO: Auto-generated Javadoc
/**
 * Creates a new play by email user account.
 *
 * URL: /create_pbem_user
 */
public class NewPBEMUser extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(NewPBEMUser.class);

	/** The Constant ACTIVATED. */
	private static final int ACTIVATED = 1;

	/** The captcha secret. */
	private String captchaSecret;

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
			captchaSecret = prop.getProperty("captcha_secret");
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

		String username = java.net.URLDecoder.decode(request.getParameter("username"), "UTF-8");
		String password = java.net.URLDecoder.decode(request.getParameter("password"), "UTF-8");
		String email = java.net.URLDecoder.decode(request.getParameter("email").replace("+", "%2B"), "UTF-8");
		String captcha = java.net.URLDecoder.decode(request.getParameter("captcha"), "UTF-8");

		String ipAddress = request.getHeader("X-Real-IP");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		if (password == null || password.length() <= 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password. Please try again with another password.");
			return;
		}
		if (username == null || username.length() <= 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username. Please try again with another username.");
			return;
		}
		if (email == null || email.length() <= 4) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid e-mail address. Please try again with another username.");
			return;
		}
		HttpClient client = HttpClientBuilder.create().build();
		String captchaUrl = "https://www.google.com/recaptcha/api/siteverify";
		HttpPost post = new HttpPost(captchaUrl);

		List<NameValuePair> urlParameters = new ArrayList<>();
		urlParameters.add(new BasicNameValuePair("secret", captchaSecret));
		urlParameters.add(new BasicNameValuePair("response", captcha));
		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		if (!captchaSecret.contains("secret goes here")) {
			/*
			 * Validate captcha against google api. skip validation for localhost where captcha_secret still has default value.
			 */
			HttpResponse captchaResponse = client.execute(post);
			InputStream in = captchaResponse.getEntity().getContent();
			String body = IOUtils.toString(in, "UTF-8");
			if (!(body.contains("success") && body.contains("true"))) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Captcha failed!");
				return;
			}
		}

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			Thread.sleep(300);

			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			String query = QueryDesigner.insertAuth();
			ps = conn.prepareStatement(query);
			ps.setString(1, username.toLowerCase());
			ps.setString(2, email);
			ps.setString(3, Crypt.crypt(password));
			ps.setInt(4, ACTIVATED);
			ps.setString(5, ipAddress);
			ps.executeUpdate();

		} catch (Exception err) {
			LOGGER.error("ERROR!", err);
			response.setHeader("result", "error");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to create user: " + err);
		} finally {
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