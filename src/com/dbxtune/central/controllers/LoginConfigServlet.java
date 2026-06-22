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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.oauth.OAuthProviderRegistry;
import com.dbxtune.utils.Configuration;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns login-related configuration flags consumed by {@code dbxLoginModal.js}
 * to decide which tab/form to show (Create Account vs Request Access, OAuth fields, etc.).
 *
 * <p>Mapped to {@code GET /api/login/config} — always public (whitelisted in MandatoryLoginFilter
 * via the {@code /api/login/} prefix).
 *
 * <h3>Response</h3>
 * <pre>
 * {
 *   "mandatory":       false,
 *   "requireApproval": false,
 *   "oauthEnabled":    true
 * }
 * </pre>
 */
public class LoginConfigServlet extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@JsonPropertyOrder({ "mandatory", "requireApproval", "oauthEnabled" })
	public static class LoginConfig
	{
		public boolean mandatory;
		public boolean requireApproval;
		public boolean oauthEnabled;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		boolean mandatory       = Configuration.getCombinedConfiguration().getBooleanProperty(DbxTuneCentral.PROPKEY_login_mandatory, DbxTuneCentral.DEFAULT_login_mandatory);
		boolean requireApproval = DbxTuneCentral.isNewAccountRequireApproval();

		boolean oauthEnabled = !OAuthProviderRegistry.getInstance().getEnabledProviders().isEmpty();

		LoginConfig result = new LoginConfig();
		result.mandatory       = mandatory;
		result.requireApproval = requireApproval;
		result.oauthEnabled    = oauthEnabled;

		_logger.debug("LoginConfigServlet: mandatory={}, requireApproval={}, oauthEnabled={}",
				mandatory, requireApproval, oauthEnabled);

		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store");
		ObjectMapper om = Helper.createObjectMapper();
		om.writeValue(resp.getWriter(), result);
	}
}
