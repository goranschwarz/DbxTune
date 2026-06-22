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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * When {@code DbxTuneCentral.login.mandatory = true}, blocks all unauthenticated
 * requests except those matching the built-in whitelist or the configurable extra
 * whitelist ({@code DbxTuneCentral.login.whitelist.extra}).
 *
 * <p>Browser requests ({@code Accept: text/html}) are redirected to
 * {@code /index.html?login=open}; API/curl requests receive
 * {@code 401 + JSON {"error":"Login required"}}.
 */
public class MandatoryLoginFilter implements Filter
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Built-in path prefixes that are always open, regardless of the mandatory-login setting.
	 * Entries must start with '/'.  A request path is considered whitelisted when it starts
	 * with any entry in this set (case-sensitive).
	 *
	 * <p>To add a permanent product path: extend this set here.
	 * To add a site-specific path without recompiling: use
	 * {@code DbxTuneCentral.login.whitelist.extra} in DBX_CENTRAL.conf.
	 */
	private static final Set<String> BUILTIN_WHITELIST = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
			"/index.html",
			"/j_security_check",   // Jetty form-login POST endpoint — must never be blocked
			"/login-check",
			"/login",
			"/logout",
			"/oauth/",
			"/api/login/",
			"/api/healthcheck",
			"/api/pcs/receiver",
			"/api/user/request-access",
			"/lpp/",
			"/.well-known/",
			"/css/",
			"/img/",
			"/images/",
			"/favicon.ico",
			"/webjars/",
			"/scripts/"
	)));

	/** Combined whitelist: built-in + runtime extras from config. */
	private Set<String> _whitelist;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		Set<String> combined = new LinkedHashSet<>(BUILTIN_WHITELIST);

		String extra = Configuration.getCombinedConfiguration().getProperty(DbxTuneCentral.PROPKEY_login_whitelist_extra, DbxTuneCentral.DEFAULT_login_whitelist_extra);
		if (StringUtil.hasValue(extra))
		{
			for (String path : extra.split(","))
			{
				String p = path.trim();
				if (!p.isEmpty())
					combined.add(p);
			}
		}

		_whitelist = Collections.unmodifiableSet(combined);
		_logger.info("MandatoryLoginFilter initialized. mandatory={}, whitelist={}", isMandatory(), _whitelist);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	throws IOException, ServletException
	{
		if (!isMandatory())
		{
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest  req  = (HttpServletRequest)  request;
		HttpServletResponse resp = (HttpServletResponse) response;

		String path = req.getRequestURI();
		String contextPath = req.getContextPath();
		if (path.startsWith(contextPath))
			path = path.substring(contextPath.length());

		if (isWhitelisted(path))
		{
			chain.doFilter(request, response);
			return;
		}

		if (req.getRemoteUser() != null)
		{
			chain.doFilter(request, response);
			return;
		}

		// Unauthenticated, not whitelisted — block
		String accept = req.getHeader("Accept");
		if (accept != null && accept.contains("text/html"))
		{
			resp.sendRedirect(contextPath + "/index.html?login=open");
		}
		else
		{
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write("{\"error\":\"Login required\"}");
		}
	}

	@Override
	public void destroy()
	{
		_logger.info("MandatoryLoginFilter destroyed.");
	}

	private boolean isMandatory()
	{
		return Configuration.getCombinedConfiguration()
				.getBooleanProperty(DbxTuneCentral.PROPKEY_login_mandatory,
				                    DbxTuneCentral.DEFAULT_login_mandatory);
	}

	private boolean isWhitelisted(String path)
	{
		for (String prefix : _whitelist)
		{
			if (path.equals(prefix) || path.startsWith(prefix))
				return true;
		}
		return false;
	}
}
