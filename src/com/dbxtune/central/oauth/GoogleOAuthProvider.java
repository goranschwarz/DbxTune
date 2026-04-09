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
 * OAuth2/OIDC provider for <b>Google</b>.
 *
 * <h3>Required configuration properties</h3>
 * <pre>
 * DbxCentral.login.google.enabled      = true
 * DbxCentral.login.google.clientId     = &lt;OAuth 2.0 Client ID&gt;
 * DbxCentral.login.google.clientSecret = &lt;OAuth 2.0 Client Secret&gt;
 * </pre>
 *
 * <h3>Setup in Google Cloud Console</h3>
 * <ol>
 *   <li>APIs &amp; Services → Credentials → Create credentials → OAuth client ID</li>
 *   <li>Application type: Web application</li>
 *   <li>Authorized redirect URI: {@code https://&lt;your-host&gt;/oauth/callback}</li>
 *   <li>OAuth consent screen: add scopes {@code email} and {@code profile}</li>
 * </ol>
 */
public class GoogleOAuthProvider
extends AbstractOAuthProvider
{
	public static final String PROPKEY_ENABLED       = "DbxCentral.login.google.enabled";
	public static final String PROPKEY_CLIENT_ID     = "DbxCentral.login.google.clientId";
	public static final String PROPKEY_CLIENT_SECRET = "DbxCentral.login.google.clientSecret";

	@Override public String getProviderId()   { return "google"; }
	@Override public String getDisplayName()  { return "Google"; }

	@Override
	public String getIconHtml()
	{
		// Compact coloured "G" SVG
		return "<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 48 48' style='vertical-align:middle;margin-right:6px'>"
		     + "<path fill='#EA4335' d='M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z'/>"
		     + "<path fill='#4285F4' d='M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z'/>"
		     + "<path fill='#FBBC05' d='M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z'/>"
		     + "<path fill='#34A853' d='M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z'/>"
		     + "<path fill='none' d='M0 0h48v48H0z'/>"
		     + "</svg>";
	}

	@Override protected String getAuthorizationEndpoint() { return "https://accounts.google.com/o/oauth2/v2/auth"; }
	@Override protected String getTokenEndpoint()         { return "https://oauth2.googleapis.com/token"; }
	@Override protected String getScope()                 { return "openid email profile"; }
	@Override protected String getPropEnabled()           { return PROPKEY_ENABLED; }
	@Override protected String getPropClientId()          { return PROPKEY_CLIENT_ID; }
	@Override protected String getPropClientSecret()      { return PROPKEY_CLIENT_SECRET; }
}
