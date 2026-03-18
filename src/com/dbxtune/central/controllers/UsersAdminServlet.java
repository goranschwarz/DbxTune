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
import java.util.List;

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
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.central.utils.PasswordHashUtil;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Admin-only servlet for user management.
 * Mapped to {@code /admin/users}, protected by the existing {@code /admin/*} security constraint.
 *
 * <p>Operations via query parameter {@code op}:
 * <ul>
 *   <li>{@code roles}         — list available roles from {@link DbxCentralRealm.Role} enum</li>
 *   <li>{@code list}          — list all users (password omitted)</li>
 *   <li>{@code add}           — add a user: username, email, password, roles</li>
 *   <li>{@code updateRoles}   — update roles: username, roles</li>
 *   <li>{@code resetPassword} — reset password: username, password</li>
 *   <li>{@code delete}        — delete user: username (cannot delete 'admin')</li>
 * </ul>
 */
public class UsersAdminServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		String       op     = StringUtil.nullToValue(req.getParameter("op"), "").trim();
		ObjectMapper om     = Helper.createObjectMapper();

		if (op.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), buildError("Parameter 'op' is required."));
			return;
		}

		if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), buildError("Service not available."));
			return;
		}

		CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();

		try
		{
			switch (op)
			{
				case "roles":         handleRoles        (req, resp, om        ); break;
				case "list":          handleList         (req, resp, om        ); break;
				case "add":           handleAdd          (req, resp, om, writer); break;
				case "updateRoles":   handleUpdateRoles  (req, resp, om, writer); break;
				case "resetPassword": handleResetPassword(req, resp, om, writer); break;
				case "delete":        handleDelete       (req, resp, om, writer); break;
				default:
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					om.writeValue(resp.getWriter(), buildError("Unknown operation: '" + op + "'."));
					break;
			}
		}
		catch (SQLException e)
		{
			_logger.error("UsersAdminServlet: Database error for op='" + op + "'.", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(resp.getWriter(), buildError("Database error: " + e.getMessage()));
		}
	}

	/** Also accept POST so the UI can use either method. */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		doGet(req, resp);
	}

	// -------------------------------------------------------------------------
	// Operation handlers
	// -------------------------------------------------------------------------

	private void handleRoles(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om)
	throws IOException
	{
		ArrayNode arr = om.createArrayNode();
		for (String role : DbxCentralRealm.Role.getAllRoleNames())
			arr.add(role);
		om.writeValue(resp.getWriter(), arr);
	}

	private void handleList(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om)
	throws IOException, SQLException
	{
		List<DbxCentralUser> users = CentralPersistReader.getInstance().getDbxCentralUsers();
		ArrayNode arr = om.createArrayNode();
		for (DbxCentralUser u : users)
		{
			ObjectNode node = om.createObjectNode();
			node.put("username", u.getUsername());
			node.put("email",    u.getEmail());
			node.put("roles",    String.join(",", u.getRoles()));
			arr.add(node);
		}
		om.writeValue(resp.getWriter(), arr);
	}

	private void handleAdd(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om, CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String username = StringUtil.nullToValue(req.getParameter("username"), "").trim();
		String email    = StringUtil.nullToValue(req.getParameter("email"   ), "").trim();
		String password = StringUtil.nullToValue(req.getParameter("password"), "");
		String roles    = StringUtil.nullToValue(req.getParameter("roles"   ), "").trim();

		if (username.isEmpty() || email.isEmpty() || password.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), buildError("Parameters 'username', 'email', and 'password' are required."));
			return;
		}
		if (roles.isEmpty())
			roles = DbxCentralRealm.ROLE_USER;

		if (CentralPersistReader.getInstance().getDbxCentralUser(username) != null)
		{
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			om.writeValue(resp.getWriter(), buildError("Username '" + username + "' already exists."));
			return;
		}
		if (writer == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), buildError("Database writer not available."));
			return;
		}

		writer.addDbxCentralUser(username, PasswordHashUtil.hashPassword(password), email, roles);
		_logger.info("UsersAdminServlet.handleAdd: Admin added user '" + username + "' with roles '" + roles + "'.");
		om.writeValue(resp.getWriter(), buildOk("User '" + username + "' added successfully."));
	}

	private void handleUpdateRoles(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om, CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String username = StringUtil.nullToValue(req.getParameter("username"), "").trim();
		String roles    = StringUtil.nullToValue(req.getParameter("roles"   ), "").trim();

		if (username.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), buildError("Parameter 'username' is required."));
			return;
		}
		if (roles.isEmpty())
			roles = DbxCentralRealm.ROLE_USER;
		if (writer == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), buildError("Database writer not available."));
			return;
		}

		writer.updateDbxCentralUserRoles(username, roles);
		_logger.info("UsersAdminServlet.handleUpdateRoles: Roles for '" + username + "' updated to '" + roles + "'.");
		om.writeValue(resp.getWriter(), buildOk("Roles for user '" + username + "' updated to '" + roles + "'."));
	}

	private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om, CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String username = StringUtil.nullToValue(req.getParameter("username"), "").trim();
		String password = StringUtil.nullToValue(req.getParameter("password"), "");

		if (username.isEmpty() || password.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), buildError("Parameters 'username' and 'password' are required."));
			return;
		}
		if (writer == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), buildError("Database writer not available."));
			return;
		}

		writer.updateDbxCentralUserPassword(username, PasswordHashUtil.hashPassword(password));
		_logger.info("UsersAdminServlet.handleResetPassword: Password reset for '" + username + "'.");
		om.writeValue(resp.getWriter(), buildOk("Password for user '" + username + "' has been reset."));
	}

	private void handleDelete(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om, CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String username = StringUtil.nullToValue(req.getParameter("username"), "").trim();

		if (username.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), buildError("Parameter 'username' is required."));
			return;
		}
		if ("admin".equalsIgnoreCase(username))
		{
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			om.writeValue(resp.getWriter(), buildError("The built-in 'admin' account cannot be deleted."));
			return;
		}
		if (writer == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), buildError("Database writer not available."));
			return;
		}

		writer.deleteDbxCentralUser(username);
		_logger.info("UsersAdminServlet.handleDelete: Admin deleted user '" + username + "'.");
		om.writeValue(resp.getWriter(), buildOk("User '" + username + "' has been deleted."));
	}

	// -------------------------------------------------------------------------
	// Response helpers
	// -------------------------------------------------------------------------

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
