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

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.central.utils.PasswordHashUtil;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Public endpoint (whitelisted from mandatory-login) for self-registration requests.
 * Mapped to {@code /api/user/request-access}.
 *
 * <p>POST parameters: {@code email}, {@code fullName}, {@code reason} (optional).
 * Creates a row in DbxCentralUsers with Status=PENDING_APPROVAL and notifies admins by email.
 */
public class RequestAccessServlet extends HttpServlet
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

		String email    = StringUtil.nullToValue(req.getParameter("email"   ), "").trim();
		String fullName = StringUtil.nullToValue(req.getParameter("fullName"), "").trim();
		String reason   = StringUtil.nullToValue(req.getParameter("reason"  ), "").trim();

		if (email.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), new ApiResult(false, "Parameter 'email' is required."));
			return;
		}
		if (!email.contains("@"))
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(resp.getWriter(), new ApiResult(false, "Invalid email address."));
			return;
		}

		if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), new ApiResult(false, "Service not available."));
			return;
		}

		CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();
		if (writer == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(resp.getWriter(), new ApiResult(false, "Database writer not available."));
			return;
		}

		try
		{
			// Email is stored as the UserName PK — check for duplicates on the PK directly
			String username = email;
			if (CentralPersistReader.getInstance().getDbxCentralUser(username) != null)
			{
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
				om.writeValue(resp.getWriter(), new ApiResult(false, "An account with this email already exists."));
				return;
			}

			// Non-loginable placeholder password; account requires admin approval before login is possible
			String placeholderPwd = PasswordHashUtil.hashPassword("self-reg:" + email);

			boolean requireApproval = DbxTuneCentral.isNewAccountRequireApproval();
			int status = requireApproval
					? DbxCentralUser.UserStatus.PENDING_APPROVAL.getBit()
					: DbxCentralUser.UserStatus.ACTIVE.getBit();

			writer.addDbxCentralUser(
					username, placeholderPwd, email, DbxCentralRealm.ROLE_USER,
					status, "self-registration",
					StringUtil.hasValue(fullName) ? fullName : null,
					StringUtil.hasValue(reason)   ? reason   : null);

			_logger.info("RequestAccessServlet: new self-registration request from email='{}', username='{}', requireApproval={}", email, username, requireApproval);

			if (requireApproval)
				NewAccountNotifier.sendAdminNotification(username, fullName, email, "self-registration", reason);

			String msg = requireApproval
					? "Request submitted. An admin will review your request."
					: "Account created. You can now log in.";
			om.writeValue(resp.getWriter(), new ApiResult(true, msg));
		}
		catch (SQLException ex)
		{
			_logger.error("RequestAccessServlet: DB error", ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(resp.getWriter(), new ApiResult(false, "Database error: " + ex.getMessage()));
		}
	}

	@JsonPropertyOrder({"success", "message"})
	private static class ApiResult
	{
		public boolean success;
		public String  message;

		ApiResult(boolean success, String message)
		{
			this.success = success; 
			this.message = message; 
		}
	}
}
