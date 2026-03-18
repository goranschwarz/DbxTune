/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.central.utils.PasswordHashUtil;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Authenticated endpoint for self-service account settings.
 * Mapped to {@code GET|POST /api/user/settings}.
 *
 * <ul>
 *   <li>{@code GET  ?op=profile}       — return {@code {username, email}} for the current user</li>
 *   <li>{@code POST ?op=changePassword} — verify current password, store new BCrypt hash</li>
 *   <li>{@code POST ?op=changeEmail}    — validate and update the user's email address</li>
 * </ul>
 *
 * Authentication is enforced by Jetty; {@code req.getRemoteUser()} is always non-null here.
 */
public class UserSettingsServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// -------------------------------------------------------------------------
	// GET — profile
	// -------------------------------------------------------------------------

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		String op = StringUtil.nullToValue(req.getParameter("op"), "").trim();
		if (!"profile".equals(op))
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			new ObjectMapper().writeValue(resp.getWriter(), buildError("Unknown op: " + op));
			return;
		}

		String username = req.getRemoteUser();
		try
		{
			DbxCentralUser user = CentralPersistReader.hasInstance()
					? CentralPersistReader.getInstance().getDbxCentralUser(username)
					: null;

			ProfileResult pr = new ProfileResult();
			pr.username = username;
			pr.email    = (user != null) ? StringUtil.nullToValue(user.getEmail(), "") : "";
			new ObjectMapper().writeValue(resp.getWriter(), pr);
		}
		catch (SQLException e)
		{
			_logger.error("UserSettingsServlet.doGet: DB error for user '{}'.", username, e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			new ObjectMapper().writeValue(resp.getWriter(), buildError("Database error."));
		}
	}


	// -------------------------------------------------------------------------
	// POST — changePassword / changeEmail
	// -------------------------------------------------------------------------

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper om       = Helper.createObjectMapper();
		String       username = req.getRemoteUser();
		String       op       = StringUtil.nullToValue(req.getParameter("op"), "").trim();

		try
		{
			if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
			{
				om.writeValue(resp.getWriter(), buildError("Service not available."));
				return;
			}

			DbxCentralUser user = CentralPersistReader.getInstance().getDbxCentralUser(username);
			if (user == null)
			{
				om.writeValue(resp.getWriter(), buildError("User account not found."));
				return;
			}

			CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();
			if (writer == null)
			{
				om.writeValue(resp.getWriter(), buildError("Database writer not available."));
				return;
			}

			switch (op)
			{
				case "changePassword": handleChangePassword(req, resp, om, user, writer); break;
				case "changeEmail":    handleChangeEmail   (req, resp, om, user, writer); break;
				default:
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					om.writeValue(resp.getWriter(), buildError("Unknown op: " + op));
			}
		}
		catch (SQLException e)
		{
			_logger.error("UserSettingsServlet.doPost: DB error for user '{}'.", username, e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(resp.getWriter(), buildError("Database error."));
		}
	}


	// -------------------------------------------------------------------------
	// Handlers
	// -------------------------------------------------------------------------

	private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp,
	                                   ObjectMapper om, DbxCentralUser user,
	                                   CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String currentPassword  = StringUtil.nullToValue(req.getParameter("currentPassword"),  "");
		String newPassword      = StringUtil.nullToValue(req.getParameter("newPassword"),      "");
		String confirmPassword  = StringUtil.nullToValue(req.getParameter("confirmPassword"),  "");

		if (currentPassword.isEmpty())
		{
			om.writeValue(resp.getWriter(), buildError("Current password is required."));
			return;
		}
		if (!PasswordHashUtil.verifyPassword(currentPassword, user.getPassword()))
		{
			om.writeValue(resp.getWriter(), buildError("Current password is incorrect."));
			return;
		}
		if (newPassword.length() < 6)
		{
			om.writeValue(resp.getWriter(), buildError("New password must be at least 6 characters."));
			return;
		}
		if (!newPassword.equals(confirmPassword))
		{
			om.writeValue(resp.getWriter(), buildError("Passwords do not match."));
			return;
		}

		writer.updateDbxCentralUserPassword(user.getUsername(), PasswordHashUtil.hashPassword(newPassword));
		_logger.info("UserSettingsServlet: Password changed for user '{}'.", user.getUsername());
		om.writeValue(resp.getWriter(), buildOk("Password updated successfully."));
	}

	private void handleChangeEmail(HttpServletRequest req, HttpServletResponse resp,
	                                ObjectMapper om, DbxCentralUser user,
	                                CentralPersistWriterJdbc writer)
	throws IOException, SQLException
	{
		String email = StringUtil.nullToValue(req.getParameter("email"), "").trim();

		if (email.isEmpty() || !email.contains("@"))
		{
			om.writeValue(resp.getWriter(), buildError("A valid email address is required."));
			return;
		}

		// Check the email isn't already taken by someone else.
		DbxCentralUser existing = CentralPersistReader.getInstance().getDbxCentralUserByEmail(email);
		if (existing != null && !existing.getUsername().equalsIgnoreCase(user.getUsername()))
		{
			om.writeValue(resp.getWriter(), buildError("Email address '" + email + "' is already registered."));
			return;
		}

		writer.updateDbxCentralUserEmail(user.getUsername(), email);
		_logger.info("UserSettingsServlet: Email updated for user '{}'.", user.getUsername());
		om.writeValue(resp.getWriter(), buildOk("Email updated successfully."));
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

	@JsonPropertyOrder({"username", "email"})
	private static class ProfileResult
	{
		public String username;
		public String email;
	}

	private static ApiResult buildOk   (String message) { return new ApiResult(true,  message); }
	private static ApiResult buildError(String message) { return new ApiResult(false, message); }
}
