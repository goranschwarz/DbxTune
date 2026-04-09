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
package com.dbxtune.central.oauth;

/**
 * OAuth2/OIDC provider for <b>Microsoft Entra ID</b> (formerly Azure AD).
 *
 * <h3>Required configuration properties</h3>
 * <pre>
 * DbxCentral.login.entra.enabled      = true
 * DbxCentral.login.entra.clientId     = &lt;Application (client) ID&gt;
 * DbxCentral.login.entra.clientSecret = &lt;Client secret value&gt;
 * DbxCentral.login.entra.tenantId     = &lt;Directory (tenant) ID&gt;
 *                                       (use "common" for multi-tenant)
 * </pre>
 *
 * <h3>App registration in Azure portal</h3>
 * <ol>
 *   <li>Azure Active Directory -> App registrations -> New registration</li>
 *   <li>Redirect URI: {@code https://<your-host>/oauth/callback}</li>
 *   <li>Certificates &amp; secrets -> New client secret</li>
 *   <li>API permissions: {@code openid}, {@code email}, {@code profile} (all delegated, Microsoft Graph)</li>
 * </ol>
 */
public class EntraIdOAuthProvider
extends AbstractOAuthProvider
{
	public static final String PROPKEY_ENABLED       = "DbxCentral.login.entra.enabled";
	public static final String PROPKEY_CLIENT_ID     = "DbxCentral.login.entra.clientId";
	public static final String PROPKEY_CLIENT_SECRET = "DbxCentral.login.entra.clientSecret";
	public static final String PROPKEY_TENANT_ID     = "DbxCentral.login.entra.tenantId";

	@Override public String getProviderId()   { return "entra"; }
	@Override public String getDisplayName()  { return "Microsoft"; }

	@Override
	public String getIconHtml()
	{
		// Simple SVG Microsoft logo (4 coloured squares)
		return "<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 21 21' style='vertical-align:middle;margin-right:6px'>"
		     + "<rect x='1'  y='1'  width='9' height='9' fill='#f25022'/>"
		     + "<rect x='11' y='1'  width='9' height='9' fill='#7fba00'/>"
		     + "<rect x='1'  y='11' width='9' height='9' fill='#00a4ef'/>"
		     + "<rect x='11' y='11' width='9' height='9' fill='#ffb900'/>"
		     + "</svg>";
	}

	@Override
	protected String getAuthorizationEndpoint()
	{
		String tenantId = cfg(PROPKEY_TENANT_ID);
		if (tenantId.isEmpty())
			tenantId = "common";
		return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize";
	}

	@Override
	protected String getTokenEndpoint()
	{
		String tenantId = cfg(PROPKEY_TENANT_ID);
		if (tenantId.isEmpty())
			tenantId = "common";
		return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
	}

	@Override protected String getScope()           { return "openid email profile"; }
	@Override protected String getPropEnabled()     { return PROPKEY_ENABLED; }
	@Override protected String getPropClientId()    { return PROPKEY_CLIENT_ID; }
	@Override protected String getPropClientSecret(){ return PROPKEY_CLIENT_SECRET; }

	@Override
	public boolean isEnabled()
	{
		// Also require tenantId (or accept empty -> "common")
		return super.isEnabled();
	}
}
