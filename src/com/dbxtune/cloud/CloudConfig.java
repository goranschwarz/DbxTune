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

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Configuration holder for Cloud Host Monitoring and Cloud ErrorLog (GCP Cloud SQL, AWS RDS, Azure Database).
 */
public class CloudConfig
{
	public static final String PROPKEY_provider             = "conn.hostmon.cloud.provider";
	public static final String DEFAULT_provider             = "GCP";

	public static final String PROPKEY_project              = "conn.hostmon.cloud.project";
	public static final String DEFAULT_project              = "";

	public static final String PROPKEY_region               = "conn.hostmon.cloud.region";
	public static final String DEFAULT_region               = "";

	public static final String PROPKEY_instance             = "conn.hostmon.cloud.instance";
	public static final String DEFAULT_instance             = "";

	public static final String PROPKEY_credentialsFile      = "conn.hostmon.cloud.credentialsFile";
	public static final String DEFAULT_credentialsFile      = "";

	public static final String PROPKEY_cacheTtlSec          = "conn.hostmon.cloud.cacheTtlSec";
	public static final int    DEFAULT_cacheTtlSec          = 30;

	public static final String PROPKEY_logFilter            = "conn.hostmon.cloud.logFilter";
	public static final String DEFAULT_logFilter            = "";

	public static final String PROPKEY_maxLogRowsPerSample  = "conn.hostmon.cloud.maxLogRowsPerSample";
	public static final int    DEFAULT_maxLogRowsPerSample  = 500;

	private CloudProvider _provider            = CloudProvider.GCP;
	private String        _projectId           = "";
	private String        _region              = "";
	private String        _instanceId          = "";
	private String        _credentialsFile     = "";
	private int           _cacheTtlSec         = DEFAULT_cacheTtlSec;
	private String        _logFilter           = "";
	private int           _maxLogRowsPerSample = DEFAULT_maxLogRowsPerSample;

	public CloudConfig()
	{
	}

	public CloudConfig(CloudProvider provider, String projectId, String region, String instanceId, String credentialsFile)
	{
		_provider        = provider != null ? provider : CloudProvider.GCP;
		_projectId       = projectId       != null ? projectId.trim()       : "";
		_region          = region          != null ? region.trim()          : "";
		_instanceId      = instanceId      != null ? instanceId.trim()      : "";
		_credentialsFile = credentialsFile != null ? credentialsFile.trim() : "";

		normalizeInstanceIdentifier();
	}

	/**
	 * Load configuration from {@link Configuration#getCombinedConfiguration()}, optionally parsing a shorthand
	 * string such as <code>project:region:instance</code> or <code>project:instance</code>.
	 */
	public static CloudConfig fromConfiguration(Configuration conf, String shorthandInstance)
	{
		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		CloudConfig cfg = new CloudConfig();
		cfg.setProvider(CloudProvider.fromString(conf.getProperty(PROPKEY_provider, DEFAULT_provider)));
		cfg.setProjectId      (conf.getProperty   (PROPKEY_project,             DEFAULT_project));
		cfg.setRegion         (conf.getProperty   (PROPKEY_region,              DEFAULT_region));
		cfg.setInstanceId     (conf.getProperty   (PROPKEY_instance,            DEFAULT_instance));
		cfg.setCredentialsFile(conf.getProperty   (PROPKEY_credentialsFile,     DEFAULT_credentialsFile));
		cfg.setCacheTtlSec    (conf.getIntProperty(PROPKEY_cacheTtlSec,         DEFAULT_cacheTtlSec));
		cfg.setLogFilter      (conf.getProperty   (PROPKEY_logFilter,           DEFAULT_logFilter));
		cfg.setMaxLogRowsPerSample(conf.getIntProperty(PROPKEY_maxLogRowsPerSample, DEFAULT_maxLogRowsPerSample));

		if (StringUtil.hasValue(shorthandInstance))
		{
			cfg.applyShorthand(shorthandInstance);
		}
		else
		{
			cfg.normalizeInstanceIdentifier();
		}
		return cfg;
	}

	/**
	 * Load configuration from {@link Configuration} combined with SSH/HostMon fields
	 * (<code>sshHostname</code>, <code>sshUsername</code>, <code>sshKeyFile</code>).
	 * Returns <code>null</code> if neither Cloud properties nor a Cloud shorthand identifier is present.
	 */
	public static CloudConfig fromConfiguration(Configuration conf, String sshHostname, String sshUsername, String sshKeyFile)
	{
		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		boolean hasExplicitCloudProps = conf != null && (
				StringUtil.hasValue(conf.getProperty(PROPKEY_provider, "")) ||
				StringUtil.hasValue(conf.getProperty(PROPKEY_project,  "")) ||
				StringUtil.hasValue(conf.getProperty(PROPKEY_instance, ""))
		);

		CloudConfig shorthandCfg = fromShorthand(sshHostname, sshUsername, sshKeyFile);
		if (!hasExplicitCloudProps && shorthandCfg == null)
			return null;

		CloudConfig cfg = fromConfiguration(conf, null);
		if (shorthandCfg != null)
		{
			if (                    shorthandCfg.getProvider() != null) cfg.setProvider       (shorthandCfg.getProvider());
			if (StringUtil.hasValue(shorthandCfg.getProjectId()))       cfg.setProjectId      (shorthandCfg.getProjectId());
			if (StringUtil.hasValue(shorthandCfg.getRegion()))          cfg.setRegion         (shorthandCfg.getRegion());
			if (StringUtil.hasValue(shorthandCfg.getInstanceId()))      cfg.setInstanceId     (shorthandCfg.getInstanceId());
			if (StringUtil.hasValue(shorthandCfg.getCredentialsFile())) cfg.setCredentialsFile(shorthandCfg.getCredentialsFile());
		}
		else if (GcpCredentialsHelper.isGcpCredentialsFile(sshKeyFile) && StringUtil.isNullOrBlank(cfg.getCredentialsFile()))
		{
			// The Host Monitor 'Key File' field: only if it is a GCP credentials file (not an SSH private key)
			cfg.setCredentialsFile(sshKeyFile);
		}

		// If instanceId is still blank (e.g. JDBC host was a raw IP address like 10.1.2.3 and user passed -u gcp -A pg-orders-01),
		// fall back to conn.dbmsServerAlias (-A) or conn.dbmsDisplayName
		if (StringUtil.isNullOrBlank(cfg.getInstanceId()) && conf != null)
		{
			String alias = conf.getProperty("conn.dbmsServerAlias", "");
			if (StringUtil.isNullOrBlank(alias))
				alias = conf.getProperty("conn.dbmsDisplayName", "");
			if (StringUtil.hasValue(alias) && !isIpv4Address(alias.trim()))
			{
				cfg.applyShorthand(alias.trim());
			}
		}

		// If GCP projectId is still blank, auto-detect from credentials file, IAM DBMS username (sa@project.iam), or environment
		if (StringUtil.isNullOrBlank(cfg.getProjectId()) && cfg.getProvider() == CloudProvider.GCP)
		{
			if (StringUtil.hasValue(cfg.getCredentialsFile()))
			{
				String proj = GcpCredentialsHelper.detectProjectIdFromFile(cfg.getCredentialsFile());
				if (StringUtil.hasValue(proj))
					cfg.setProjectId(proj);
			}
			if (StringUtil.isNullOrBlank(cfg.getProjectId()) && conf != null)
			{
				String dbmsUser = conf.getProperty("conn.dbmsUsername", "");
				String proj = GcpCredentialsHelper.detectProjectIdFromIamUsername(dbmsUser);
				if (StringUtil.hasValue(proj))
					cfg.setProjectId(proj);
			}
			if (StringUtil.isNullOrBlank(cfg.getProjectId()))
			{
				String proj = GcpCredentialsHelper.detectProjectIdFromEnvironment(false);
				if (StringUtil.hasValue(proj))
					cfg.setProjectId(proj);
			}
		}

		return cfg;
	}

	private static boolean isIpv4Address(String s)
	{
		return s != null && s.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
	}

	/**
	 * Parse a shorthand HostMon host/user/keyFile tuple into a {@link CloudConfig}, or return <code>null</code>
	 * if the parameters look like a standard SSH host rather than a Cloud instance identifier.
	 */
	public static CloudConfig fromShorthand(String sshHostname, String sshUsername, String sshKeyFile)
	{
		boolean userIsCloud = StringUtil.hasValue(sshUsername) 
				&& (   "gcp"     .equalsIgnoreCase(sshUsername.trim())
					|| "cloudsql".equalsIgnoreCase(sshUsername.trim())
					|| "aws"     .equalsIgnoreCase(sshUsername.trim())
					|| "azure"   .equalsIgnoreCase(sshUsername.trim())
		           );

		if (StringUtil.isNullOrBlank(sshHostname))
		{
			if (!userIsCloud)
				return null;

			CloudConfig cfg = new CloudConfig();
			cfg.setProvider(CloudProvider.fromString(sshUsername.trim()));
			if (StringUtil.hasValue(sshKeyFile))
				cfg.setCredentialsFile(sshKeyFile);
			return cfg;
		}

		String h = sshHostname.trim();
		boolean explicitScheme = 
				   h.regionMatches(true, 0, "gcp://"     , 0, 6)
				|| h.regionMatches(true, 0, "cloudsql://", 0, 11)
				|| h.regionMatches(true, 0, "aws://"     , 0, 6)
				|| h.regionMatches(true, 0, "azure://"   , 0, 8);

		String stripped = h;
		if      (h.regionMatches(true, 0, "gcp://"     , 0, 6))  stripped = h.substring(6) .trim();
		else if (h.regionMatches(true, 0, "cloudsql://", 0, 11)) stripped = h.substring(11).trim();
		else if (h.regionMatches(true, 0, "aws://"     , 0, 6))  stripped = h.substring(6) .trim();
		else if (h.regionMatches(true, 0, "azure://"   , 0, 8))  stripped = h.substring(8) .trim();

		String[] parts = stripped.split(":");
		boolean threePartCloud = (parts.length == 3 && !parts[0].isEmpty() && !parts[1].isEmpty() && !parts[2].isEmpty());

		if (!explicitScheme && !threePartCloud && !userIsCloud)
			return null;

		CloudConfig cfg = new CloudConfig();
		if (userIsCloud)
			cfg.setProvider(CloudProvider.fromString(sshUsername.trim()));
		cfg.applyShorthand(h);
		if (StringUtil.hasValue(sshKeyFile))
			cfg.setCredentialsFile(sshKeyFile);

		return cfg;
	}

	/**
	 * Parse a shorthand identifier like <code>project:region:instance</code>, <code>project:instance</code>,
	 * or a single hostname/instance name (when <code>-u gcp</code> is used and the hostname was auto-picked from JDBC).
	 */
	public void applyShorthand(String shorthand)
	{
		if (StringUtil.isNullOrBlank(shorthand))
			return;

		String s = shorthand.trim();
		// Strip optional prefix like "gcp://" or "cloudsql://"
		if (s.regionMatches(true, 0, "gcp://", 0, 6))
		{
			_provider = CloudProvider.GCP;
			s = s.substring(6).trim();
		}
		else if (s.regionMatches(true, 0, "cloudsql://", 0, 11))
		{
			_provider = CloudProvider.GCP;
			s = s.substring(11).trim();
		}
		else if (s.regionMatches(true, 0, "aws://", 0, 6))
		{
			_provider = CloudProvider.AWS;
			s = s.substring(6).trim();
		}
		else if (s.regionMatches(true, 0, "azure://", 0, 8))
		{
			_provider = CloudProvider.AZURE;
			s = s.substring(8).trim();
		}

		String[] parts = s.split(":");
		if (parts.length == 3)
		{
			if (StringUtil.isNullOrBlank(_projectId)) _projectId = parts[0].trim();
			if (StringUtil.isNullOrBlank(_region))    _region    = parts[1].trim();
			_instanceId = parts[2].trim();
		}
		else if (parts.length == 2)
		{
			// Check if parts[1] is a numeric port (e.g. host:5432 auto-copied from DBMS host:port)
			boolean secondPartIsPort = false;
			try
			{
				Integer.parseInt(parts[1].trim());
				secondPartIsPort = true;
			}
			catch (NumberFormatException ignore) {}

			if (secondPartIsPort)
			{
				applySingleHostOrInstance(parts[0].trim());
			}
			else
			{
				if (StringUtil.isNullOrBlank(_projectId)) _projectId = parts[0].trim();
				_instanceId = parts[1].trim();
			}
		}
		else
		{
			applySingleHostOrInstance(s);
		}
	}

	private void applySingleHostOrInstance(String hostOrInstance)
	{
		if (StringUtil.isNullOrBlank(hostOrInstance) || isIpv4Address(hostOrInstance))
			return;

		// If it's a FQDN like "pg-orders-01.corp.internal", take the short hostname "pg-orders-01"
		int dotPos = hostOrInstance.indexOf('.');
		if (dotPos > 0)
			_instanceId = hostOrInstance.substring(0, dotPos).trim();
		else
			_instanceId = hostOrInstance.trim();
	}

	/**
	 * If {@link #_instanceId} was entered as <code>project:region:instance</code> or <code>project:instance</code>,
	 * split it into projectId, region, and instanceId.
	 */
	public void normalizeInstanceIdentifier()
	{
		if (StringUtil.hasValue(_instanceId) && _instanceId.contains(":"))
		{
			applyShorthand(_instanceId);
		}
	}

	public CloudProvider getProvider()                         { return _provider; }
	public void          setProvider(CloudProvider provider)   { _provider = provider != null ? provider : CloudProvider.GCP; }

	public String        getProjectId()                        { return _projectId; }
	public void          setProjectId(String projectId)        { _projectId = projectId != null ? projectId.trim() : ""; }

	public String        getRegion()                           { return _region; }
	public void          setRegion(String region)              { _region = region != null ? region.trim() : ""; }

	public String        getInstanceId()                       { return _instanceId; }
	public void          setInstanceId(String instanceId)      { _instanceId = instanceId != null ? instanceId.trim() : ""; normalizeInstanceIdentifier(); }

	public String        getCredentialsFile()                  { return _credentialsFile; }
	public void          setCredentialsFile(String file)       { _credentialsFile = file != null ? file.trim() : ""; }

	public int           getCacheTtlSec()                      { return _cacheTtlSec; }
	public void          setCacheTtlSec(int sec)               { _cacheTtlSec = sec > 0 ? sec : DEFAULT_cacheTtlSec; }
	public long          getCacheTtlMs()                       { return _cacheTtlSec * 1000L; }

	public String        getLogFilter()                        { return _logFilter; }
	public void          setLogFilter(String filter)           { _logFilter = filter != null ? filter.trim() : ""; }

	public int           getMaxLogRowsPerSample()              { return _maxLogRowsPerSample; }
	public void          setMaxLogRowsPerSample(int maxRows)   { _maxLogRowsPerSample = maxRows > 0 ? maxRows : DEFAULT_maxLogRowsPerSample; }

	/**
	 * For GCP Cloud SQL, returns <code>project:instance</code> used in Cloud Monitoring / Cloud Logging resource labels.
	 */
	public String getGcpDatabaseId()
	{
		return _projectId + ":" + _instanceId;
	}

	/**
	 * Returns a human-readable connection name (e.g., <code>project:region:instance</code> or <code>project:instance</code>).
	 */
	public String getFullResourceName()
	{
		if (StringUtil.hasValue(_projectId) && StringUtil.hasValue(_region) && StringUtil.hasValue(_instanceId))
			return _projectId + ":" + _region + ":" + _instanceId;
		if (StringUtil.hasValue(_projectId) && StringUtil.hasValue(_instanceId))
			return _projectId + ":" + _instanceId;
		return _instanceId;
	}

	public String getDisplayHostname()
	{
		return getFullResourceName();
	}

	public String getHostname()
	{
		return getFullResourceName();
	}

	public boolean isValid()
	{
		if (_provider == CloudProvider.GCP)
			return StringUtil.hasValue(_projectId) && StringUtil.hasValue(_instanceId);
		return StringUtil.hasValue(_instanceId);
	}

	@Override
	public String toString()
	{
		return "CloudConfig[provider=" + _provider
			+ ", project=" + _projectId
			+ ", region=" + _region
			+ ", instance=" + _instanceId
			+ ", credentialsFile=" + (StringUtil.hasValue(_credentialsFile) ? _credentialsFile : "<ADC>")
			+ ", cacheTtlSec=" + _cacheTtlSec
			+ "]";
	}
}
