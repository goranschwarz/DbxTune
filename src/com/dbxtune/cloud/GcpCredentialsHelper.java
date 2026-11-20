/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cloud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.hostmon.HostMonitorConnectionCloud;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ExternalAccountCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;

/**
 * Helper for loading Google Cloud OAuth2 credentials (Service Account JSON, gcloud ADC authorized_user JSON,
 * Workload Identity Federation external_account, impersonated_service_account, or Application Default Credentials)
 * and obtaining a valid Bearer token for Cloud Monitoring and Cloud Logging REST APIs.
 */
public class GcpCredentialsHelper
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Scope covering Cloud Monitoring (read), Cloud Logging (read), and Cloud SQL Admin / IAM DB Auth. */
	public static final List<String> DEFAULT_SCOPES = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");

	public static final String PROPKEY_dbmsIamAuth         = "conn.dbms.cloud.iamAuth";
	public static final String PROPKEY_dbmsCredentialsFile = "conn.dbms.cloud.credentialsFile";

	/** Shared cache of credential helpers keyed by normalized credentials file path ("" = Application Default Credentials). */
	private static final Map<String, GcpCredentialsHelper> HELPER_CACHE = new ConcurrentHashMap<>();

	private final String            _credentialsFile;
	private       GoogleCredentials _credentials;

	public GcpCredentialsHelper(String credentialsFile)
	{
		_credentialsFile = credentialsFile != null ? credentialsFile.trim() : "";
	}

	/**
	 * Returns a valid OAuth2 access token string, refreshing it automatically when expired.
	 */
	public synchronized String getAccessToken()
	throws IOException
	{
		if (_credentials == null)
		{
			_credentials = loadCredentials(_credentialsFile);
		}

		_credentials.refreshIfExpired();
		AccessToken token = _credentials.getAccessToken();
		if (token == null || StringUtil.isNullOrBlank(token.getTokenValue()))
		{
			// Force refresh if token was not yet populated
			token = _credentials.refreshAccessToken();
		}
		if (token == null || StringUtil.isNullOrBlank(token.getTokenValue()))
		{
			throw new IOException("GoogleCredentials returned an empty access token.");
		}
		return token.getTokenValue();
	}

	/**
	 * Load {@link GoogleCredentials} from an explicit JSON file or from Application Default Credentials (ADC).
	 */
	public static GoogleCredentials loadCredentials(String credentialsFile)
	throws IOException
	{
		GoogleCredentials creds;
		if (StringUtil.hasValue(credentialsFile))
		{
			Path path = Path.of(credentialsFile);
			if (!Files.exists(path))
			{
				throw new IOException("GCP credentials file does not exist: " + path.toAbsolutePath());
			}
			creds = loadCredentialsFile(path);
		}
		else
		{
			try
			{
				creds = GoogleCredentials.getApplicationDefault();
			}
			catch (IOException ex)
			{
				throw new IOException("Could not load Google Application Default Credentials (ADC). "
					+ "Either configure a Service Account JSON key file, set GOOGLE_APPLICATION_CREDENTIALS, "
					+ "or run 'gcloud auth application-default login'. Cause: " + ex.getMessage(), ex);
			}
		}

		return creds.createScoped(DEFAULT_SCOPES);
	}

	/**
	 * Parse a credentials JSON file by its "type" field directly (avoids ServiceLoader issues in custom classpaths).
	 */
	public static GoogleCredentials loadCredentialsFile(Path path)
	throws IOException
	{
		if ( ! isGcpCredentialsFile(path.toString()) )
			throw new IOException("The file '" + path + "' is not a GCP credentials JSON file (a Service Account key file has \"type\": \"service_account\"). Is it an SSH private key?");

		byte[] bytes = Files.readAllBytes(path);
		JsonNode root = MAPPER.readTree(bytes);
		String type = root.path("type").asText("");

		if (_logger.isDebugEnabled())
			_logger.debug("Loading GCP credentials file '" + path + "' of type '" + type + "'.");

		switch (type)
		{
			case "service_account":
				return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(bytes));
			case "authorized_user":
				return UserCredentials.fromStream(new ByteArrayInputStream(bytes));
			case "external_account":
				return ExternalAccountCredentials.fromStream(new ByteArrayInputStream(bytes));
			case "impersonated_service_account":
				return ImpersonatedCredentials.fromStream(new ByteArrayInputStream(bytes));
			default:
				return GoogleCredentials.fromStream(new ByteArrayInputStream(bytes));
		}
	}

	/**
	 * Is the file a GCP credentials JSON file (it has a "type": service_account, authorized_user, external_account...)?<br>
	 * The Host Monitor 'Key File' field is also used for SSH private keys: those must not be used as GCP credentials.
	 * Nothing from the file is logged.
	 */
	public static boolean isGcpCredentialsFile(String file)
	{
		if (StringUtil.isNullOrBlank(file))
			return false;
		try
		{
			Path path = Path.of(file.trim());
			if ( ! Files.isRegularFile(path) || Files.size(path) > 1024 * 1024 )
				return false;

			JsonNode root = MAPPER.readTree(Files.readAllBytes(path));
			return root != null && root.isObject() && StringUtil.hasValue(root.path("type").asText(""));
		}
		catch (Exception ex)
		{
			return false; // not JSON (for example an SSH private key), or not readable
		}
	}

	/**
	 * Try to read the project_id or quota_project_id from a credentials JSON file if present.
	 * Returns empty string if not found or on error.
	 */
	public static String detectProjectIdFromFile(String credentialsFile)
	{
		if (StringUtil.isNullOrBlank(credentialsFile))
			return "";

		try
		{
			Path path = Path.of(credentialsFile.trim());
			if (!Files.exists(path))
				return "";

			String json = Files.readString(path, StandardCharsets.UTF_8);
			JsonNode root = MAPPER.readTree(json);
			String projectId = root.path("project_id").asText("");
			if (StringUtil.isNullOrBlank(projectId))
				projectId = root.path("quota_project_id").asText("");
			return projectId.trim();
		}
		catch (Exception ignore)
		{
			return "";
		}
	}

	/**
	 * Extract the GCP Project ID from an IAM Service Account database username
	 * (e.g. <code>dbxtune-monitor@my-prod-project.iam</code> or
	 * <code>dbxtune-monitor@my-prod-project.iam.gserviceaccount.com</code> -&gt; <code>my-prod-project</code>).
	 * Returns empty string if not a matching IAM username.
	 */
	public static String detectProjectIdFromIamUsername(String username)
	{
		if (!isGcpIamUsername(username))
			return "";

		String normalized = normalizeGcpIamPostgresUsername(username);
		int atPos = normalized.indexOf('@');
		if (atPos > 0 && normalized.toLowerCase().endsWith(".iam"))
		{
			String proj = normalized.substring(atPos + 1, normalized.length() - ".iam".length()).trim();
			return proj;
		}
		return "";
	}

	/**
	 * Try to detect the GCP Project ID from standard environment variables
	 * (<code>GOOGLE_CLOUD_PROJECT</code>, <code>GCLOUD_PROJECT</code>, <code>CLOUDSDK_CORE_PROJECT</code>,
	 * <code>GOOGLE_APPLICATION_CREDENTIALS</code>) and optionally from the GCP Compute Engine VM Metadata Server.
	 */
	public static String detectProjectIdFromEnvironment(boolean queryGceMetadataServer)
	{
		String[] envVars = new String[] { "GOOGLE_CLOUD_PROJECT", "GCLOUD_PROJECT", "CLOUDSDK_CORE_PROJECT" };
		for (String env : envVars)
		{
			String val = System.getenv(env);
			if (StringUtil.hasValue(val))
				return val.trim();
		}

		String adcFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
		if (StringUtil.hasValue(adcFile))
		{
			String proj = detectProjectIdFromFile(adcFile);
			if (StringUtil.hasValue(proj))
				return proj;
		}

		if (queryGceMetadataServer)
		{
			try
			{
				HttpClient client = HttpClient.newBuilder()
						.connectTimeout(Duration.ofMillis(1500))
						.build();
				HttpRequest req = HttpRequest.newBuilder()
						.uri(URI.create("http://metadata.google.internal/computeMetadata/v1/project/project-id"))
						.timeout(Duration.ofMillis(1500))
						.header("Metadata-Flavor", "Google")
						.GET()
						.build();
				HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
				if (resp.statusCode() == 200 && StringUtil.hasValue(resp.body()))
				{
					return resp.body().trim();
				}
			}
			catch (Exception ignore)
			{
				// Not running on a GCP Compute Engine VM
			}
		}

		return "";
	}

	/**
	 * Returns <code>true</code> if the database username looks like a GCP Cloud SQL IAM Service Account
	 * (e.g. <code>dbxtune-monitor@my-project.iam</code> or <code>dbxtune-monitor@my-project.iam.gserviceaccount.com</code>).
	 */
	public static boolean isGcpIamUsername(String username)
	{
		if (StringUtil.isNullOrBlank(username))
			return false;

		String u = username.trim().toLowerCase();
		return u.endsWith(".iam") || u.endsWith(".iam.gserviceaccount.com");
	}

	/**
	 * Cloud SQL for PostgreSQL strips the <code>.gserviceaccount.com</code> suffix from IAM Service Account
	 * database usernames (so <code>sa@project.iam.gserviceaccount.com</code> becomes <code>sa@project.iam</code>).
	 * This method normalizes the username automatically if the full email was supplied.
	 */
	public static String normalizeGcpIamPostgresUsername(String username)
	{
		if (username == null)
			return null;

		String u = username.trim();
		String suffix = ".gserviceaccount.com";
		if (u.toLowerCase().endsWith(".iam.gserviceaccount.com"))
		{
			return u.substring(0, u.length() - suffix.length());
		}
		return u;
	}

	/**
	 * Returns <code>true</code> if the password is empty or an explicit GCP IAM token placeholder
	 * (<code>""</code>, <code>"gcp-iam"</code>, <code>"iam"</code>, or <code>"gcp-iam:/path/to/key.json"</code>).
	 */
	public static boolean isGcpIamPasswordTrigger(String password)
	{
		if (password == null || password.trim().isEmpty())
			return true;

		String p = password.trim();
		return "gcp-iam".equalsIgnoreCase(p)
			|| "iam".equalsIgnoreCase(p)
			|| p.regionMatches(true, 0, "gcp-iam:", 0, 8);
	}

	/**
	 * Returns <code>true</code> if this PostgreSQL JDBC connection should authenticate using a
	 * dynamically refreshed GCP OAuth2 Access Token (Cloud SQL IAM Database Authentication).
	 */
	public static boolean isGcpIamPostgresAuth(String url, String username, String password)
	{
		return isGcpIamPostgresAuth(url, username, password, false);
	}

	/**
	 * The user of the monitored connection, when it logged in with IAM because of the property <code>conn.dbms.cloud.iamAuth=true</code>.<br>
	 * The property only applies to that user: also for the collector's other connections to the monitored server (same user),
	 * but not for other connections with an empty password (for example a Postgres PCS store).
	 */
	private static volatile String _iamFlagMonitoredUser = null;

	/**
	 * @param monitoredConnection true for the connection to the monitored server (the property <code>conn.dbms.cloud.iamAuth</code> applies to it)
	 */
	public static boolean isGcpIamPostgresAuth(String url, String username, String password, boolean monitoredConnection)
	{
		if (url != null && !url.regionMatches(true, 0, "jdbc:postgresql:", 0, 16))
			return false;

		if (password != null)
		{
			String p = password.trim();
			if ("gcp-iam".equalsIgnoreCase(p) || p.regionMatches(true, 0, "gcp-iam:", 0, 8))
				return true;
		}

		// The property: only for the monitored connection, or the same user as it (not every connection with an empty password)
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null && conf.getBooleanProperty(PROPKEY_dbmsIamAuth, false) && isGcpIamPasswordTrigger(password))
		{
			String monUser = _iamFlagMonitoredUser;
			if (monitoredConnection || (monUser != null && username != null && monUser.equalsIgnoreCase(normalizeGcpIamPostgresUsername(username))))
				return true;
		}

		return isGcpIamUsername(username) && isGcpIamPasswordTrigger(password);
	}

	/**
	 * Resolve which Service Account JSON credentials file to use (or <code>""</code> for Application Default Credentials).
	 */
	public static String resolveCredentialsFile(String password, String fallbackKeyFile)
	{
		if (password != null && password.trim().regionMatches(true, 0, "gcp-iam:", 0, 8))
		{
			String inlinePath = password.trim().substring(8).trim();
			if (StringUtil.hasValue(inlinePath))
				return inlinePath;
		}

		// The Host Monitor 'Key File' field: only if it is a GCP credentials file (not an SSH private key)
		if (isGcpCredentialsFile(fallbackKeyFile))
			return fallbackKeyFile.trim();

		if (CounterController.hasInstance())
		{
			HostMonitorConnection hostMonConn = CounterController.getInstance().getHostMonConnection();
			if (hostMonConn instanceof HostMonitorConnectionCloud)
			{
				CloudConfig cloudCfg = ((HostMonitorConnectionCloud) hostMonConn).getCloudConfig();
				if (cloudCfg != null && StringUtil.hasValue(cloudCfg.getCredentialsFile()))
					return cloudCfg.getCredentialsFile().trim();
			}
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
		{
			String f = conf.getProperty(PROPKEY_dbmsCredentialsFile, "");
			if (StringUtil.hasValue(f))
				return f.trim();

			f = conf.getProperty(CloudConfig.PROPKEY_credentialsFile, "");
			if (StringUtil.hasValue(f))
				return f.trim();

			// The SSH key file property: only if it is a GCP credentials file (not an SSH private key)
			f = conf.getProperty("conn.sshKeyFile", "");
			if (isGcpCredentialsFile(f))
				return f.trim();
		}

		return "";
	}

	/**
	 * Obtain a valid (cached or freshly refreshed) OAuth2 access token for the given credentials file
	 * (or Application Default Credentials when <code>credentialsFile</code> is empty).
	 */
	public static String getCachedAccessToken(String credentialsFile)
	throws IOException
	{
		String key = credentialsFile != null ? credentialsFile.trim() : "";
		GcpCredentialsHelper helper = HELPER_CACHE.computeIfAbsent(key, GcpCredentialsHelper::new);
		return helper.getAccessToken();
	}

	/**
	 * Clear the cached credential helpers so the next call forces a fresh token reload.
	 */
	public static void clearCache()
	{
		HELPER_CACHE.clear();
	}

	/**
	 * If <code>url</code> and <code>props</code> indicate GCP Cloud SQL IAM Database Authentication for PostgreSQL,
	 * normalize the username (stripping <code>.gserviceaccount.com</code> if present) and inject a fresh OAuth2
	 * access token into <code>props.setProperty("password", token)</code>.
	 *
	 * @return <code>true</code> if GCP IAM Auth was applied, <code>false</code> otherwise.
	 */
	public static boolean applyGcpIamPostgresAuth(String url, Properties props, String fallbackKeyFile)
	throws SQLException
	{
		return applyGcpIamPostgresAuth(url, props, fallbackKeyFile, false);
	}

	/**
	 * @param monitoredConnection true for the connection to the monitored server (the property <code>conn.dbms.cloud.iamAuth</code> applies to it)
	 */
	public static boolean applyGcpIamPostgresAuth(String url, Properties props, String fallbackKeyFile, boolean monitoredConnection)
	throws SQLException
	{
		if (props == null)
			return false;

		String user   = props.getProperty("user");
		String passwd = props.getProperty("password");

		if (!isGcpIamPostgresAuth(url, user, passwd, monitoredConnection))
			return false;

		String normalizedUser = normalizeGcpIamPostgresUsername(user);
		if (monitoredConnection)
			_iamFlagMonitoredUser = normalizedUser;
		if (normalizedUser != null && !normalizedUser.equals(user))
		{
			_logger.info("GCP IAM DB Auth: Normalized PostgreSQL username from '" + user + "' to '" + normalizedUser + "' (Cloud SQL omits '.gserviceaccount.com').");
			props.setProperty("user", normalizedUser);
		}

		String credFile = resolveCredentialsFile(passwd, fallbackKeyFile);
		try
		{
			String token = getCachedAccessToken(credFile);
			props.setProperty("password", token);
			_logger.info("GCP IAM DB Auth: Injected fresh OAuth2 access token as JDBC password for PostgreSQL user '" + normalizedUser
					+ "' using " + (StringUtil.hasValue(credFile) ? "credentialsFile='" + credFile + "'" : "Application Default Credentials (ADC)") + ".");
			return true;
		}
		catch (IOException ex)
		{
			String msg = "GCP IAM DB Auth: Failed to obtain OAuth2 access token for PostgreSQL user '" + normalizedUser + "'. " + ex.getMessage();
			_logger.error(msg, ex);
			throw new SQLException(msg, "28000", ex);
		}
	}
}
