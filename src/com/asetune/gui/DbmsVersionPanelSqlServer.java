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

import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class DbmsVersionPanelSqlServer 
extends DbmsVersionPanelAbstract
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsVersionPanelSqlServer.class);

	public DbmsVersionPanelSqlServer(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
		
		setLabelAndTooltipMajor  (true,  2008, 0, 2099, 1, "Major",         "<html>Major version of the Server, Example: <b>2017</b>SP2 CU1</html>");
		setLabelAndTooltipMinor  (true,     0, 0,    2, 2, "Rel",           "<html>This is only valid for 2008 R2, if it is 2008 R2, we will have a 2 here</html>");
		setLabelAndTooltipMaint  (false,    0, 0,   99, 1, "",              "");
		setLabelAndTooltipSp     (true,     0, 0,   99, 1, "SP",            "<html>Service Pack of the Server, Example: 2017 SP<b>2</b> CU1</html>");
		setLabelAndTooltipPl     (true,     0, 0,   99, 1, "CU",            "<html>Cumulative Update of the Server, Example: 2017 SP2 CU<b>1</b></html>");
		
		setLabelAndTooltipEdition(true,  "Azure Edition", "<html>Generate SQL Information for a SQL-Server Azure</html>");
	}

	@Override
	public void loadFieldsUsingVersion(long srvVersion, boolean isCeEnabled)
	{
		super.loadFieldsUsingVersion(srvVersion, isCeEnabled);

		// Enable 'Major' field only for SQL-Server 2008
		int major = Ver.versionNumPart(srvVersion, Ver.VERSION_MAJOR);
		_versionMinor_sp.setEnabled(major == 2008);
	}
	
	@Override
	public long parseVersionStringToNum(String versionStr)
	{
//		if (StringUtil.isNullOrBlank(versionStr) || "0.0.0".equals(versionStr))
//			versionStr = "00.0.0"; // then the below sybVersionStringToNumber() work better
//		
//		// if 1.2 --> 01.2
//		if (versionStr.matches("^[0-9]\\.[0-9].*"))
//			versionStr = "0" + versionStr; // then the below sybVersionStringToNumber() work better


		// Microsoft SQL Server 2018 R2 (RTM-CU9) (KB4341265)
		// Microsoft SQL Server 2017 (RTM-CU9) (KB4341265)

		String w1 = StringUtil.word(versionStr, 0);
		String w2 = StringUtil.word(versionStr, 1);
		String w3 = StringUtil.word(versionStr, 2);
		String w4 = StringUtil.word(versionStr, 3);
		
		if (w2 == null) w2 = "";
		if (w3 == null) w3 = "";
		if (w4 == null) w4 = "";
		
		w2 = w2.toUpperCase();
		w3 = w3.toUpperCase();
		w4 = w4.toUpperCase();
		
		if (w2.toUpperCase().startsWith("R"))
		{
			w1 += " " + w2;
			w2 = w3;
			w3 = w4;
		}
		
		String msVersionString = "Microsoft SQL Server " + w1 + " (" + w2 + w3 + ") - yes_we_expected_a_dash_after_the_paranteses";

		long version = Ver.sqlServerVersionStringToNumber(msVersionString);

		_logger.debug("MS-parseVersionStringToNum(versionStr='"+versionStr+"'): msVersionString='"+msVersionString+"', <<<<<< returns: "+version);
		return version;
	}
	
	@Override
	public String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl)
	{
		String srvVersionStr = Ver.versionNumToStr(srvVersion);
		return srvVersionStr;
	}

	@Override
	public long getMinVersion()
	{
		return Ver.ver(2008);
	}
}
