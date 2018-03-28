package org.freeciv.servlet;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.context.EnvSqlConnection;
import org.freeciv.utils.Constants;
import org.freeciv.utils.QueryDesigner;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc
/**
 * Submit game results to Hall of Fame.
 *
 * URL: /deactivate_user
 */
public class HallOfFamePost extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(HallOfFamePost.class);

	/** The pattern validate alpha numeric. */
	private String PATTERN_VALIDATE_ALPHA_NUMERIC = "[0-9a-zA-Z \\.]*";

	/** The p. */
	private Pattern p = Pattern.compile(PATTERN_VALIDATE_ALPHA_NUMERIC);

	/** The Constant mapSrcImgPaths. */
	private static final String mapSrcImgPaths = "/var/lib/tomcat8/webapps/data/savegames/";

	/** The Constant mapDstImgPaths. */
	private static final String mapDstImgPaths = "/var/lib/tomcat8/webapps/data/mapimgs/";

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		logParams(request);

		String username = java.net.URLDecoder.decode(request.getParameter("username"), "UTF-8");
		String nation = java.net.URLDecoder.decode(request.getParameter("nation"), "UTF-8");
		String score = java.net.URLDecoder.decode(request.getParameter("score"), "UTF-8");
		String turn = java.net.URLDecoder.decode(request.getParameter("turn"), "UTF-8");
		String port = java.net.URLDecoder.decode(request.getParameter("port"), "UTF-8");
		String ipAddress = request.getHeader("X-Real-IP");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		if (username == null || username.length() <= 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username. Please try again with another username.");
			return;
		}

		if (!p.matcher(username).matches() || !p.matcher(score).matches() || !p.matcher(turn).matches()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid data submitted. ");
			return;
		}

		Connection conn = null;
		PreparedStatement ps0 = null;
		PreparedStatement ps1 = null;
		ResultSet rs = null;

		try {
			Thread.sleep(200);

			Context env = (Context) (new InitialContext().lookup(Constants.CONTEXT));
			DataSource ds = (DataSource) env.lookup(Constants.JDBC);
			conn = ds.getConnection();

			String idQuery = QueryDesigner.getMaxIdHallOfFame();
			ps0 = conn.prepareStatement(idQuery);
			rs = ps0.executeQuery();
			int newId = 1;
			while (rs.next()) {
				newId = rs.getInt(1) + 1;
			}

			File mapimg = new File(mapSrcImgPaths + "map-" + Integer.parseInt(port) + ".map.gif");
			if (mapimg.exists()) {
				FileUtils.moveFileToDirectory(mapimg, new File(mapDstImgPaths), true);
				File resultFile = new File(mapDstImgPaths + "map-" + Integer.parseInt(port) + ".map.gif");
				resultFile.renameTo(new File(mapDstImgPaths + newId + ".gif"));
			}

			String query = QueryDesigner.updateHallOfFame();
			ps1 = conn.prepareStatement(query);
			ps1.setString(1, username);
			ps1.setString(2, nation);
			ps1.setString(3, score);
			ps1.setString(4, turn);
			ps1.setString(5, ipAddress);
			ps1.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("ERROR!", e);
			response.setHeader("result", "error");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to submit to Hall of Fame: " + e);
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
