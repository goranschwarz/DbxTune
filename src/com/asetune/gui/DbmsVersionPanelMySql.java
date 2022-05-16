/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoMySql;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
import com.asetune.utils.VersionShort;

public class DbmsVersionPanelMySql
extends DbmsVersionPanelAbstract
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsVersionPanelOracle.class);

	public DbmsVersionPanelMySql(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
		
		setLabelAndTooltipMajor  (true,  5,  0, 99,  1, "Major",   "<html>Major version of the Server, Example: <b>5</b>.6.14</html>");
		setLabelAndTooltipMinor  (true,  7,  0, 99,  1, "Minor",   "<html>Minor version of the Server, Example: 5.<b>6</b>.14</html>");
		setLabelAndTooltipMaint  (true,  0,  0, 99,  1, "Maint",   "<html>Mintenance version of the Server, Example: 5.6.<b>14</b></html>");
		setLabelAndTooltipSp     (false, 0,  0, 99,  1, "Patch",   "<html>...</html>");
		setLabelAndTooltipPl     (false, 0,  0, 99,  1, "xxx",     "<html>...</html>");

//		setLabelAndTooltipEdition(false, "XXX Edition", "<html>Generate SQL Information for a XXX Edition Server</html>");
	}

	@Override
	public long parseVersionStringToNum(String versionStr)
	{
		String[] sa = versionStr.split("\\.");
		
		String tmpVerStr = "";
		if (sa.length == 3)
		{
			tmpVerStr = StringUtil.toCommaStr(sa, ".");
		}
		else
		{
			String[] tmpArr = new String[3];
			for (int i=0; i<sa.length; i++)
				tmpArr[i] = sa[i];

			for (int i=sa.length; i<tmpArr.length; i++)
				tmpArr[i] = "0";

			tmpVerStr = StringUtil.toCommaStr(tmpArr, ".");
		}

		int shortVerNum = VersionShort.parse(tmpVerStr);
		long version = Ver.shortVersionStringToNumber(shortVerNum);

		_logger.debug("MYSQL-parseVersionStringToNum(versionStr='"+versionStr+"'): tmpVerStr='"+tmpVerStr+"', <<<<<< returns: "+version);
		return version;
	}
	
	@Override
	public String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl)
	{
//		String srvVersionStr = Ver.versionNumToStr(srvVersion);
//		return srvVersionStr;
		return major + "." + minor + "." + maint;
	}

	@Override
	public long getMinVersion()
	{
		return Ver.ver(2,6);
	}

	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoMySql(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		// Create a DBMS Server specific version object
		DbmsVersionInfoMySql versionInfo = new DbmsVersionInfoMySql(ver);

		return versionInfo;
	}
}
