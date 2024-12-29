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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.gui;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

public class DbmsVersionPanelDb2
extends DbmsVersionPanelAbstract
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsVersionPanelOracle.class);

	public DbmsVersionPanelDb2(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
		
		setLabelAndTooltipMajor  (true, 10,  0, 99,  1, "Major",   "<html>Major version of the Server, Example: <b>10</b>.5.0.7</html>");
		setLabelAndTooltipMinor  (true,  0,  0, 9,   1, "Minor",   "<html>Minor version of the Server, Example: 10.<b>5</b>.0.7</html>");
		setLabelAndTooltipMaint  (true,  0,  0, 9,   1, "Maint",   "<html>Mintenance version of the Server, Example: 10.5.<b>0</b>.7</html>");
		setLabelAndTooltipSp     (true,  0,  0, 99,  1, "Patch",   "<html>Patch level, Example: 10.5.0.<b>7</b></html>");
		setLabelAndTooltipPl     (false, 0,  0, 99,  1, "xxx",     "<html>...</html>");

//		setLabelAndTooltipEdition(false, "Cluster Edition", "<html>Generate SQL Information for a Cluster Edition Server</html>");
	}

	@Override
	public long parseVersionStringToNum(String versionStr)
	{
		String[] sa = versionStr.split("\\.");
		
		String tmpVerStr = "";
		if (sa.length == 4)
		{
			tmpVerStr = StringUtil.toCommaStr(sa, ".");
		}
		else
		{
			String[] tmpArr = new String[4];
			for (int i=0; i<sa.length; i++)
				tmpArr[i] = sa[i];

			for (int i=sa.length; i<tmpArr.length; i++)
				tmpArr[i] = "0";

			tmpVerStr = StringUtil.toCommaStr(tmpArr, ".");
		}

		long version = Ver.db2VersionStringToNumber("DB2 v" + tmpVerStr);

		_logger.debug("DB2-parseVersionStringToNum(versionStr='"+versionStr+"'): tmpVerStr='"+tmpVerStr+"', <<<<<< returns: "+version);
		return version;
	}
	
	@Override
	public String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl)
	{
//		String srvVersionStr = Ver.versionNumToStr(srvVersion);
//		return srvVersionStr;
		return major + "." + minor + "." + maint + "." + sp;
	}

	@Override
	public long getMinVersion()
	{
		return Ver.ver(10);
	}

	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoDb2(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		// Create a DBMS Server specific version object
		DbmsVersionInfoDb2 versionInfo = new DbmsVersionInfoDb2(ver);

		return versionInfo;
	}

}
