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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

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
		
//		setLabelAndTooltipEdition(true,  "Azure Edition", "<html>Generate SQL Information for a SQL-Server Azure</html>");
	}

//	private static final String ON_PREM                 = "On Prem / Normal SQL Server";
	private static final String ON_PREM                 = "Normal SQL Server -- On Prem";
	private static final String AZURE_SQL_DATABASE      = "Azure SQL Database";
	private static final String AZURE_SYNAPSE_ANALYTICS = "Azure Synapse/Analytics";
	private static final String AZURE_MANAGED_INSTANCE  = "Azure Managed Instance";
	
	private JComboBox<String> _instanceType = new JComboBox<>(new String[] {ON_PREM, AZURE_SQL_DATABASE, AZURE_SYNAPSE_ANALYTICS, AZURE_MANAGED_INSTANCE});
	
	@Override
	protected JPanel createDbmsPropertiesPanel()
	{
		JPanel p = new JPanel(new MigLayout());

		p.add(_instanceType, "");

		_instanceType.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				DbmsVersionPanelSqlServer.super.stateChanged(null);
			}
		});

		return p;
	}

	
	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoSqlServer(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		String instanceTypeStr = _instanceType.getSelectedItem() + "";

		// If we SWITCHED from any AZURE to ON-PREM and we are CONNECTED; then get the Version Info from the Connected Server
		int major = Ver.versionNumPart(ver, Ver.VERSION_MAJOR);
		if (ON_PREM.equals(instanceTypeStr) && major == 9999)
		{
			DbxConnection conn = CounterController.getInstance().getMonConnection();
			if (conn != null)
			{
				// If we are connected to Azure, then get MIN version
				// otherwise get it from the connected SQL Server 
				DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
				if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics() || ssVersionInfo.isAzureManagedInstance())
					ver = getMinVersion();
				else
					ver = ssVersionInfo.getLongVersion();
			}
			else
			{
				ver = getMinVersion();
			}
		}

		// Create a DBMS Server specific version object
		DbmsVersionInfoSqlServer ssVersionInfo = new DbmsVersionInfoSqlServer(ver);

		// Set SQL Server specifics (from any extended GUI fields)
		ssVersionInfo.setAzureDb              (AZURE_SQL_DATABASE     .equals(instanceTypeStr));
		ssVersionInfo.setAzureSynapseAnalytics(AZURE_SYNAPSE_ANALYTICS.equals(instanceTypeStr));
		ssVersionInfo.setAzureManagedInstance (AZURE_MANAGED_INSTANCE .equals(instanceTypeStr));
		
		if (ssVersionInfo.isAzureDb()              ) { ssVersionInfo.setLongVersion(DbmsVersionInfoSqlServer.VERSION_AZURE_SQL_DB); }
		if (ssVersionInfo.isAzureSynapseAnalytics()) { ssVersionInfo.setLongVersion(DbmsVersionInfoSqlServer.VERSION_AZURE_SYNAPSE_ANALYTICS); }
		if (ssVersionInfo.isAzureManagedInstance() ) { ssVersionInfo.setLongVersion(DbmsVersionInfoSqlServer.VERSION_AZURE_MANAGED_INSTANCE); }
		
//System.out.println("createDbmsVersionInfo() <<<<---- returns: versionInfo="+ssVersionInfo+")");
		return ssVersionInfo;
	}

	@Override
	public void loadFieldsUsingVersion(DbmsVersionInfo versionInfo)
	{
		super.loadFieldsUsingVersion(versionInfo);

		// Set local fields
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		boolean isAnyAzure = false;
		if (ssVersionInfo.isAzureDb())               { isAnyAzure = true; _instanceType.setSelectedItem(AZURE_SQL_DATABASE);      }
		if (ssVersionInfo.isAzureSynapseAnalytics()) { isAnyAzure = true; _instanceType.setSelectedItem(AZURE_SYNAPSE_ANALYTICS); }
		if (ssVersionInfo.isAzureManagedInstance())  { isAnyAzure = true; _instanceType.setSelectedItem(AZURE_MANAGED_INSTANCE);  }

		_versionMajor_sp.setEnabled( ! isAnyAzure );
		_versionMinor_sp.setEnabled( ! isAnyAzure );
		_versionMaint_sp.setEnabled( ! isAnyAzure );
		_versionEsd_sp  .setEnabled( ! isAnyAzure );
		_versionPl_sp   .setEnabled( ! isAnyAzure );

		// Enable 'Major' field only for SQL-Server 2008
		int major = Ver.versionNumPart(versionInfo.getLongVersion(), Ver.VERSION_MAJOR);
		_versionMinor_sp.setEnabled(major == 2008 && !isAnyAzure);
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
