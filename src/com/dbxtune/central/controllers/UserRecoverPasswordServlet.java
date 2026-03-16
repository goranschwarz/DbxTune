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
import java.util.UUID;

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
import com.dbxtune.utils.MailUtil;
import com.dbxtune.utils.MailUtil.MailConfig;
import com.dbxtune.utils.MailUtil.MailException;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Public endpoint: sends a temporary password to the user's registered email address.
 * Mapped to {@code POST /api/user/forgot-password}.
 *
 * <ul>
 *   <li>User not found        - generic success (never reveal whether an address is registered)</li>
 *   <li>SMTP not configured   - error "Email service is not configured..."</li>
 *   <li>Send failure          - error "Failed to send recovery email..."</li>
 * </ul>
 */
public class UserRecoverPasswordServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Shown for both "user not found" and "email sent" -- never reveals whether an address is registered. */
	private static final String MSG_GENERIC_OK =
			"If that email address is registered, a temporary password has been sent.";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper om    = Helper.createObjectMapper();
		String       email = StringUtil.nullToValue(req.getParameter("email"), "").trim();

		if (email.isEmpty() || !email.contains("@"))
		{
			om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK));
			return;
		}

		try
		{
			if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
			{
				om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK));
				return;
			}

			// Check SMTP config BEFORE looking up the user -- avoids leaking user existence
			// via timing differences or different error responses.
			MailConfig mailConfig = MailUtil.autoDiscoverConfig();
			if (mailConfig == null)
			{
				_logger.warn("UserRecoverPasswordServlet: No SMTP configuration found (tried prefixes: {}). Cannot send recovery email.",
						MailUtil.AUTO_DISCOVER_PREFIXES);
				om.writeValue(resp.getWriter(), new ApiResult(false,
						"Email service is not configured on this server. Please contact your administrator."));
				return;
			}

			DbxCentralUser user = CentralPersistReader.getInstance().getDbxCentralUserByEmail(email);
			if (user == null)
			{
				_logger.info("UserRecoverPasswordServlet: No user found for email '{}' -- ignoring.", email);
				om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK));
				return;
			}

			CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();
			if (writer == null)
			{
				om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK));
				return;
			}

			String tmpPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

			// Send email BEFORE updating DB -- password only changes if the email was delivered.
			try
			{
				MailUtil.createHtmlEmail(mailConfig)
						.setTo(email)
						.setSubject("DbxTune -- password recovery")
						.setMessage(
								"<p>Hello <b>" + user.getUsername() + "</b>,</p>"
								+ "<p>Your temporary password is: <code>" + tmpPassword + "</code></p>"
								+ "<p>Please log in and change your password immediately.</p>"
								+ "<p>-- DbxTune</p>")
						.setTextMessage(
								"Hello " + user.getUsername() + ",\n\n"
								+ "Your temporary password is: " + tmpPassword + "\n\n"
								+ "Please log in and change your password immediately.\n\n"
								+ "-- DbxTune")
						.send();
			}
			catch (MailException e)
			{
				_logger.error("UserRecoverPasswordServlet: Failed to send recovery email to '{}'.", email, e);
				om.writeValue(resp.getWriter(), new ApiResult(false,
						"Failed to send recovery email. Please check the email configuration on this server."));
				return;
			}

			writer.updateDbxCentralUserPassword(user.getUsername(), PasswordHashUtil.hashPassword(tmpPassword));
			_logger.info("UserRecoverPasswordServlet: Temporary password sent and password updated for user '{}'.", user.getUsername());
			om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK));
		}
		catch (SQLException e)
		{
			_logger.error("UserRecoverPasswordServlet: Database error for email '{}'.", email, e);
			om.writeValue(resp.getWriter(), new ApiResult(true, MSG_GENERIC_OK)); // don't expose DB internals
		}
	}

	@JsonPropertyOrder({"success", "message"})
	private static class ApiResult
	{
		public boolean success;
		public String  message;
		ApiResult(boolean success, String message) { this.success = success; this.message = message; }
	}
}
