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
import java.lang.invoke.MethodHandles;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class DbmsVersionPanelOracle
extends DbmsVersionPanelAbstract
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected JCheckBox _versionIsRac_chk = new JCheckBox("RAC", false);

	
	public DbmsVersionPanelOracle(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
		
		setLabelAndTooltipMajor  (true,    10, 0, 99, 1, "Major",  "<html>The first digit is the most general identifier. It represents a major new version of the software that contains significant new functionality. Example: <b>10</b>.1.0.1.0</html>");
		setLabelAndTooltipMinor  (true,     0, 0,  9, 1, "Maint",  "<html>The second digit represents a maintenance release level. Some new features may also be included. Example: 10.<b>1</b>.0.1.0</html>");
		setLabelAndTooltipMaint  (true,     0, 0,  9, 1, "App",    "<html>The third digit reflects the release level of the Oracle Application Server (OracleAS). Example: 10.1.<b>0</b>.1.0</html>");
		setLabelAndTooltipSp     (true,     0, 0,  9, 1, "Comp",   "<html>The fourth digit identifies a release level specific to a component. Different components can have different numbers in this position depending upon, for example, component patch sets or interim releases. Example: 10.1.0.<b>1</b>.0</html>");
		setLabelAndTooltipPl     (true,     0, 0,  9, 1, "Platf",  "<html>The fifth digit identifies a platform-specific release. Usually this is a patch set. When different platforms require the equivalent patch set, this digit will be the same across the affected platforms. Example: 10.1.0.1.<b>0</b></html>");
		
//		setLabelAndTooltipEdition(true,  "RAC", "<html>Generate SQL Information for a RAC Server</html>");
	}

	@Override
	protected JPanel createDbmsPropertiesPanel()
	{
		JPanel p = new JPanel(new MigLayout());
		
		_versionIsRac_chk  .setToolTipText("<html>Generate SQL Information for a RAC Server</html>");

		p.add(_versionIsRac_chk, "");
		
		_versionIsRac_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				DbmsVersionPanelOracle.super.stateChanged(null);
			}
		});

		return p;
	}

	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoOracle(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		// Create a DBMS Server specific version object
		DbmsVersionInfoOracle versionInfo = new DbmsVersionInfoOracle(ver);

		// Set Oracle specifics (from any extended GUI fields)
		versionInfo.setRac(_versionIsRac_chk.isSelected());
		
		return versionInfo;
	}

	@Override
	public void loadFieldsUsingVersion(DbmsVersionInfo versionInfo)
	{
		super.loadFieldsUsingVersion(versionInfo);

		// Set local fields
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		_versionIsRac_chk.setSelected(oraVersionInfo.isRac());
	}
	

	@Override
	public long parseVersionStringToNum(String versionStr)
	{
		String[] sa = versionStr.split("\\.");
		
		String oraVerStr = "";
		if (sa.length == 5)
		{
			oraVerStr = StringUtil.toCommaStr(sa, ".");
		}
		else
		{
			String[] oraArr = new String[5];
			for (int i=0; i<sa.length; i++)
				oraArr[i] = sa[i];

			for (int i=sa.length; i<oraArr.length; i++)
				oraArr[i] = "0";

			oraVerStr = StringUtil.toCommaStr(oraArr, ".");
		}

		long version = Ver.oracleVersionStringToNumber(oraVerStr);

		_logger.debug("ORACLE-parseVersionStringToNum(versionStr='"+versionStr+"'): oraVerStr='"+oraVerStr+"', <<<<<< returns: "+version);
		return version;
	}
	
	@Override
	public String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl)
	{
//		String srvVersionStr = Ver.versionNumToStr(srvVersion);
//		return srvVersionStr;
		return major + "." + minor + "." + maint + "." + sp + "." + pl;
	}

	@Override
	public long getMinVersion()
	{
		return Ver.ver(10);
	}
}
