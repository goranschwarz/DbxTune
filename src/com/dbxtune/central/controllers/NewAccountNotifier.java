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

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.writers.AlarmWriterAbstract;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MailUtil;
import com.dbxtune.utils.StringUtil;

/**
 * Sends an email notification to admins whenever a new account requests access,
 * either via OAuth first-login or self-registration.
 *
 * <p>Recipient resolution order:
 * <ol>
 *   <li>Config key {@code DbxTuneCentral.login.newAccount.notifyEmail} (comma-separated)</li>
 *   <li>All admin-role users in DbxCentralUsers who have a non-empty Email</li>
 *   <li>If still nobody: log a prominent IMPORTANT message</li>
 * </ol>
 */
public class NewAccountNotifier
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Send notification about a new account request. Never throws — all errors are logged. */
	public static void sendAdminNotification(String username, String fullName, String email, String source, String reason)
	{
		List<String> recipients = resolveRecipients();

		if (recipients.isEmpty())
		{
			logImportant(username, email, source);
			return;
		}

		try
		{
			String displayName = StringUtil.hasValue(fullName) ? fullName + " (" + email + ")" : email;
			String subject = "DbxCentral: New account request - " + username;

			StringBuilder body = new StringBuilder();
			body.append("<p>A new account has requested access to DbxCentral.</p>");
			body.append("<table>");
			body.append("<tr><td><b>Username:</b></td><td>").append(escHtml(username)    ).append("</td></tr>");
			body.append("<tr><td><b>Name:</b></td><td>"    ).append(escHtml(displayName) ).append("</td></tr>");
			body.append("<tr><td><b>Source:</b></td><td>"  ).append(escHtml(source)      ).append("</td></tr>");
			if (StringUtil.hasValue(reason))
				body.append("<tr><td><b>Reason:</b></td><td>").append(escHtml(reason)).append("</td></tr>");
			body.append("</table>");
			String adminUrl = AlarmWriterAbstract.static_getDbxCentralUrl() + "/admin/admin.html#dbxc-user";
			body.append("<p>Log in to DbxCentral as admin and go to <b>Admin &rarr; User Administration</b> to approve or reject:</p>");
			body.append("<p><a href='").append(adminUrl).append("'>").append(adminUrl).append("</a></p>");

			MailUtil.MailBuilder mail = MailUtil.createHtmlEmail();
			for (String r : recipients)
				mail.addTo(r.trim());
			mail.setSubject(subject)
			    .setMessage(body.toString())
			    .send();

			_logger.info("NewAccountNotifier: notification sent to {} for new user '{}' (source='{}')", recipients, username, source);
		}
		catch (Exception ex)
		{
			_logger.warn("NewAccountNotifier: failed to send notification for new user '{}': {}", username, ex.getMessage(), ex);
		}
	}

	private static List<String> resolveRecipients()
	{
		// 1. Config key
		String configEmail = Configuration.getCombinedConfiguration()
				.getProperty(DbxTuneCentral.PROPKEY_login_newAccount_notifyEmail,
				             DbxTuneCentral.DEFAULT_login_newAccount_notifyEmail);
		if (StringUtil.hasValue(configEmail))
		{
			List<String> list = new ArrayList<>(Arrays.asList(configEmail.split(",")));
			list.removeIf(s -> s.trim().isEmpty());
			if (!list.isEmpty())
				return list;
		}

		// 2. Admin users with email from DB
		if (CentralPersistReader.hasInstance())
		{
			try
			{
				List<DbxCentralUser> admins = CentralPersistReader.getInstance().getDbxCentralAdminUsersWithEmail();
				List<String> emails = new ArrayList<>();
				for (DbxCentralUser u : admins)
					if (StringUtil.hasValue(u.getEmail()))
						emails.add(u.getEmail());
				if (!emails.isEmpty())
					return emails;
			}
			catch (SQLException ex)
			{
				_logger.warn("NewAccountNotifier: could not query admin emails from DB", ex);
			}
		}

		return new ArrayList<>();
	}

	private static void logImportant(String username, String email, String source)
	{
		_logger.warn("##########################################################################");
		_logger.warn("##########################################################################");
		_logger.warn("## IMPORTANT: New account request received but NO admin email found!   ##");
		_logger.warn("## User '{}' ({}) requested access via {}.", username, email, source);
		_logger.warn("## Set DbxTuneCentral.login.newAccount.notifyEmail in DBX_CENTRAL.conf ##");
		_logger.warn("## or add an Email to an admin user in DbxCentralUsers.                ##");
		_logger.warn("## NO notification was sent.                                           ##");
		_logger.warn("##########################################################################");
		_logger.warn("##########################################################################");
	}

	/**
	 * Sends an approval or rejection email directly to the user.
	 * Never throws — all errors are logged.
	 */
	public static void sendUserDecisionNotification(String username, String userEmail, boolean approved, String approvedBy)
	{
		if (!StringUtil.hasValue(userEmail))
		{
			_logger.info("NewAccountNotifier.sendUserDecisionNotification: no email for '{}', skipping notification.", username);
			return;
		}

		try
		{
			String subject;
			String body;

			if (approved)
			{
				String loginUrl = AlarmWriterAbstract.static_getDbxCentralUrl();
				subject = "DbxCentral: Your account has been approved";
				body    = "<p>Hello <b>" + escHtml(username) + "</b>,</p>"
				        + "<p>Your DbxCentral account request has been <b>approved</b> by <i>" + escHtml(approvedBy) + "</i>.</p>"
				        + "<p>You can now log in: <a href='" + loginUrl + "'>" + loginUrl + "</a></p>";
			}
			else
			{
				subject = "DbxCentral: Your account request has been rejected";
				body    = "<p>Hello <b>" + escHtml(username) + "</b>,</p>"
				        + "<p>Unfortunately your DbxCentral account request has been <b>rejected</b>.</p>"
				        + "<p>Please contact your administrator if you believe this is a mistake.</p>";
			}

			MailUtil.MailBuilder mail = MailUtil.createHtmlEmail();
			mail.addTo(userEmail.trim())
			    .setSubject(subject)
			    .setMessage(body)
			    .send();

			_logger.info("NewAccountNotifier.sendUserDecisionNotification: {} notification sent to '{}' ({})", approved ? "approval" : "rejection", username, userEmail);
		}
		catch (Exception ex)
		{
			_logger.warn("NewAccountNotifier.sendUserDecisionNotification: failed to send {} notification to '{}': {}", approved ? "approval" : "rejection", username, ex.getMessage(), ex);
		}
	}

	private static String escHtml(String s)
	{
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
