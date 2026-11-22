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

import com.dbxtune.utils.StringUtil;

/**
 * Supported Cloud Providers for Cloud Host Monitoring and Cloud ErrorLog.
 */
public enum CloudProvider
{
	GCP  ("GCP",   "Google Cloud SQL"),
	AWS  ("AWS",   "Amazon RDS / Aurora"),
	AZURE("AZURE", "Azure Database");

	private final String _code;
	private final String _displayName;

	CloudProvider(String code, String displayName)
	{
		_code        = code;
		_displayName = displayName;
	}

	public String getCode()        { return _code; }
	public String getDisplayName() { return _displayName; }

	/**
	 * Parse a provider string (case-insensitive). Returns {@link #GCP} if null/blank or unrecognized.
	 */
	public static CloudProvider fromString(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return GCP;

		String s = str.trim().toUpperCase();
		if (s.startsWith("GCP") || s.contains("GOOGLE") || s.contains("CLOUDSQL"))
			return GCP;
		if (s.startsWith("AWS") || s.contains("AMAZON") || s.contains("RDS") || s.contains("AURORA"))
			return AWS;
		if (s.startsWith("AZURE") || s.contains("MICROSOFT"))
			return AZURE;

		return valueOf(s);
	}
}
