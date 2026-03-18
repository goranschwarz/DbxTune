/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.central.utils.PasswordHashUtil;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Public endpoint for self-service user registration.
 * New accounts are always created with the {@code user} role.
 * Mapped to {@code POST /api/user/register}.
 */
public class UserRegistrationServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper om = Helper.createObjectMapper();

		String username        = StringUtil.nullToValue(req.getParameter("username"       ), "").trim();
		String email           = StringUtil.nullToValue(req.getParameter("email"          ), "").trim();
		String password        = StringUtil.nullToValue(req.getParameter("password"       ), "");
		String confirmPassword = StringUtil.nullToValue(req.getParameter("confirmPassword"), "");

		if (username.isEmpty())                          { om.writeValue(resp.getWriter(), buildError("Username is required."));                        return; }
		if (username.length() > 128)                     { om.writeValue(resp.getWriter(), buildError("Username must be 128 characters or less."));      return; }
		if (!email.contains("@"))                        { om.writeValue(resp.getWriter(), buildError("A valid email address is required."));            return; }
		if (password.length() < 6)                       { om.writeValue(resp.getWriter(), buildError("Password must be at least 6 characters."));      return; }
		if (!password.equals(confirmPassword))           { om.writeValue(resp.getWriter(), buildError("Passwords do not match."));                      return; }

		try
		{
			if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
			{
				resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				om.writeValue(resp.getWriter(), buildError("Service not available."));
				return;
			}

			if (CentralPersistReader.getInstance().getDbxCentralUser(username) != null)
			{
				om.writeValue(resp.getWriter(), buildError("Username '" + username + "' is already taken."));
				return;
			}

			if (CentralPersistReader.getInstance().getDbxCentralUserByEmail(email) != null)
			{
				om.writeValue(resp.getWriter(), buildError("Email address '" + email + "' is already registered."));
				return;
			}

			CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();
			if (writer == null)
			{
				resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				om.writeValue(resp.getWriter(), buildError("Database writer not available."));
				return;
			}

			writer.addDbxCentralUser(username, PasswordHashUtil.hashPassword(password), email, DbxCentralRealm.ROLE_USER);
			_logger.info("UserRegistrationServlet: New user registered: '" + username + "'.");
			om.writeValue(resp.getWriter(), buildOk("Account created. You can now log in."));
		}
		catch (SQLException e)
		{
			_logger.error("UserRegistrationServlet: Database error during registration for '" + username + "'.", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(resp.getWriter(), buildError("Registration failed due to a server error."));
		}
	}

	@JsonPropertyOrder({"success", "message"})
	private static class ApiResult
	{
		public boolean success;
		public String  message;
		ApiResult(boolean success, String message) { this.success = success; this.message = message; }
	}

	private ApiResult buildOk(String message)    { return new ApiResult(true,  message); }
	private ApiResult buildError(String message) { return new ApiResult(false, message); }
}
