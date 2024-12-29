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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public abstract class DbmsVersionPanelAbstract
extends JPanel
implements ActionListener, ChangeListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsVersionPanelAbstract.class);
	
	protected JLabel             _version_lbl      = new JLabel("Server Version");
	protected JLabel             _versionMajor_lbl = new JLabel("Major");
	protected SpinnerNumberModel _versionMajor_spm = new SpinnerNumberModel(12, 12, 99, 1); // value, min, max, step
	protected JSpinner           _versionMajor_sp  = new JSpinner(_versionMajor_spm);

	protected JLabel             _versionMinor_lbl = new JLabel("Minor");
	protected SpinnerNumberModel _versionMinor_spm = new SpinnerNumberModel(5, 0, 9, 1); // value, min, max, step
	protected JSpinner           _versionMinor_sp  = new JSpinner(_versionMinor_spm);

	protected JLabel             _versionMaint_lbl = new JLabel("Maint");
	protected SpinnerNumberModel _versionMaint_spm = new SpinnerNumberModel(0, 0, 9, 1); // value, min, max, step
	protected JSpinner           _versionMaint_sp  = new JSpinner(_versionMaint_spm);

	protected JLabel             _versionEsd_lbl   = new JLabel("ESD#/SP");
	protected SpinnerNumberModel _versionEsd_spm   = new SpinnerNumberModel(3, 0, 999, 1); // value, min, max, step
	protected JSpinner           _versionEsd_sp    = new JSpinner(_versionEsd_spm);

	protected JLabel             _versionPl_lbl    = new JLabel("Patch Level");
	protected SpinnerNumberModel _versionPl_spm    = new SpinnerNumberModel(0, 0, 99, 1); // value, min, max, step
	protected JSpinner           _versionPl_sp     = new JSpinner(_versionPl_spm);

	protected JPanel             _versionPropsPanel = null;
//	protected JCheckBox          _versionIsCe_chk   = new JCheckBox("Cluster Edition", false);

	protected JLabel             _versionShort_lbl   = new JLabel("Server Short Version");
	protected JTextField         _versionShort_txt   = new JTextField();
	protected JTextField         _versionInt_txt     = new JTextField();

	private ShowCmPropertiesDialog _propDialog;

	public DbmsVersionPanelAbstract(ShowCmPropertiesDialog propDialog)
	{
		super();
		_propDialog = propDialog;
		init();
	}
	
	private void init()
	{
		setLayout(new MigLayout("", "", ""));

		Border border = BorderFactory.createTitledBorder("Server Version");
		setBorder(border);

		setToolTipText("<html>Change Server Version to see what SQL will be used on other Server Versions</html>");
	
		setLabelAndTooltipMajor  (true, 12, 12, 99,  1, "Major",           "<html>Major version of the Server, Example: <b>15</b>.0.3</html>");
		setLabelAndTooltipMinor  (true,  5,  0, 9,   1, "Minor",           "<html>Minor version of the Server, Example: 15.<b>0</b>.3</html>");
		setLabelAndTooltipMaint  (true,  0,  0, 9,   1, "Maint",           "<html>Mintenance version of the Server, Example: 15.0.<b>3</b></html>");
		setLabelAndTooltipSp     (true,  3,  0, 999, 1, "ESD#/SP",         "<html>ESD or SP (ESD = Electronic Software Distribution, SP = Service Pack) Level of the Server, Example: 15.0.3 <b>ESD#2</b> or <b>SP100</b><br>SAP is using Service Packs to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>The Service Pack consist of three numbers. Here I will try to simulate ESD into SP. Example ESD#4 will be SP040 and ESD#4.1 will be SP041<br>In the summer of 2013 Sybase/SAP changed from ESD into SP.</html>");
		setLabelAndTooltipPl     (true,  0,  0, 99,  1, "Patch Level",     "<html>PL -Patch Level of the Server Version, Example: 16.0 SP01 <b>PL01</b><br>SAP is using Patch Level to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>Note: This is introduced in ASE 16.0</html>");
//		setLabelAndTooltipEdition(true, "Cluster Edition", "<html>Generate SQL Information for a Cluster Edition Server</html>");
		
		_version_lbl      .setToolTipText("<html>Specify what Server Version you want to see information about</html>");
		
//		_versionMajor_lbl .setToolTipText("<html>Major version of the Server, Example: <b>15</b>.0.3</html>");
//		_versionMajor_sp  .setToolTipText("<html>Major version of the Server, Example: <b>15</b>.0.3</html>");
//
//		_versionMinor_lbl .setToolTipText("<html>Minor version of the Server, Example: 15.<b>0</b>.3</html>");
//		_versionMinor_sp  .setToolTipText("<html>Minor version of the Server, Example: 15.<b>0</b>.3</html>");
//
//		_versionMaint_lbl .setToolTipText("<html>Mintenance version of the Server, Example: 15.0.<b>3</b></html>");
//		_versionMaint_sp  .setToolTipText("<html>Mintenance version of the Server, Example: 15.0.<b>3</b></html>");
//
//		_versionEsd_lbl   .setToolTipText("<html>ESD or SP (ESD = Electronic Software Distribution, SP = Service Pack) Level of the Server, Example: 15.0.3 <b>ESD#2</b> or <b>SP100</b><br>SAP is using Service Packs to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>The Service Pack consist of three numbers. Here I will try to simulate ESD into SP. Example ESD#4 will be SP040 and ESD#4.1 will be SP041<br>In the summer of 2013 Sybase/SAP changed from ESD into SP.</html>");
//		_versionEsd_sp    .setToolTipText("<html>ESD or SP (ESD = Electronic Software Distribution, SP = Service Pack) Level of the Server, Example: 15.0.3 <b>ESD#2</b> or <b>SP100</b><br>SAP is using Service Packs to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>The Service Pack consist of three numbers. Here I will try to simulate ESD into SP. Example ESD#4 will be SP040 and ESD#4.1 will be SP041<br>In the summer of 2013 Sybase/SAP changed from ESD into SP.</html>");
//
//		_versionPl_lbl    .setToolTipText("<html>PL -Patch Level of the Server Version, Example: 16.0 SP01 <b>PL01</b><br>SAP is using Patch Level to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>Note: This is introduced in ASE 16.0</html>");
//		_versionPl_sp     .setToolTipText(_versionPl_lbl.getToolTipText());
//
//		_versionIsCe_chk  .setToolTipText("<html>Generate SQL Information for a Cluster Edition Server</html>");

		_versionShort_lbl .setToolTipText("<html>Here you can specify a Version string, which will be parsed into a number.</html>");
		_versionShort_txt .setToolTipText("<html>Here you can specify a Version string, which will be parsed into a number.</html>");
		_versionInt_txt   .setToolTipText("<html>Internal NUMBER used as version number.</html>");

		_versionInt_txt.setEditable(false);
	}

	/** Called from ShowCmPropertiesDialog */
	protected void addFieldsToPanel()
	{
		String majorWidh = "10mm";
		if (_versionMajor_spm.getValue() instanceof Number && ((Number)_versionMajor_spm.getValue()).intValue() >= 1000)
			majorWidh = "15mm";
		
		add(_versionMajor_lbl, "w "+majorWidh+", skip 1, span, hidemode 2, split");
		add(_versionMinor_lbl, "w 10mm, hidemode 2");
		add(_versionMaint_lbl, "w 10mm, hidemode 2");
		add(_versionEsd_lbl,   "w 10mm, hidemode 2");
		add(_versionPl_lbl,    "w 10mm, hidemode 2, wrap");

		add(_version_lbl,      "");
		add(_versionMajor_sp,  "w "+majorWidh+", span, hidemode 2, split");
		add(_versionMinor_sp,  "w 10mm, hidemode 2");
		add(_versionMaint_sp,  "w 10mm, hidemode 2");
		add(_versionEsd_sp,    "w 10mm, hidemode 2");
		add(_versionPl_sp,     "w 10mm, hidemode 2");
//		add(_versionIsCe_chk,  "wrap, hidemode 2");

		_versionPropsPanel = createDbmsPropertiesPanel();
		if (_versionPropsPanel != null)
			add(_versionPropsPanel,  "hidemode 2");
		
		add(new JLabel(),      "wrap"); // Add empty to get newline after PL or _versionPropsPanel

		add(_versionShort_lbl, "");
		add(_versionShort_txt, "growx, pushx");
		add(_versionInt_txt,   "wrap");
	}

	protected JPanel createDbmsPropertiesPanel()
	{
//		JPanel p = new JPanel(new MigLayout());
//		
//		p.add(_versionIsCe_chk, "");
//		
//		return p;
		return null;
	}

	/** Called from ShowCmPropertiesDialog */
	protected void addActionsListeners()
	{
		_versionMajor_spm.addChangeListener(this);
		_versionMinor_spm.addChangeListener(this);
		_versionMaint_spm.addChangeListener(this);
		_versionEsd_spm  .addChangeListener(this);
		_versionPl_spm   .addChangeListener(this);

		_versionShort_txt .addActionListener(this);
//		_versionIsCe_chk  .addActionListener(this);
	}

	/**
	 * Set the MAJOR Version part
	 * 
     * @param visible    If this part should be visible or not in the dialog
     * @param value      the current value of the model
     * @param value      the current value of the model
     * @param minimum    the first number in the sequence
     * @param maximum    the last number in the sequence
     * @param stepSize   the difference between elements of the sequence
     * @param label      Label of this field
     * @param tooltip    Tool tip text
     * @throws IllegalArgumentException if the following expression is false:
     *     <code>minimum &lt;= value &lt;= maximum</code>
     */
	public void setLabelAndTooltipMajor(boolean visible, int value, int minimum, int maximum, int stepSize, String label, String tooltip) 
	{ 
		_versionMajor_lbl.setVisible(visible);
		_versionMajor_sp .setVisible(visible);

		_versionMajor_spm.setValue(value);
		_versionMajor_spm.setMinimum(minimum);
		_versionMajor_spm.setMaximum(maximum);
		_versionMajor_spm.setStepSize(stepSize);
		
		_versionMajor_lbl.setText(label);

		_versionMajor_lbl.setToolTipText(tooltip);
		_versionMajor_sp .setToolTipText(tooltip);
	}

	public void setLabelAndTooltipMinor(boolean visible, int value, int minimum, int maximum, int stepSize, String label, String tooltip) 
	{ 
		_versionMinor_lbl.setVisible(visible);
		_versionMinor_sp .setVisible(visible);

		_versionMinor_spm.setValue(value);
		_versionMinor_spm.setMinimum(minimum);
		_versionMinor_spm.setMaximum(maximum);
		_versionMinor_spm.setStepSize(stepSize);
		
		_versionMinor_lbl.setText(label);

		_versionMinor_lbl.setToolTipText(tooltip);
		_versionMinor_sp .setToolTipText(tooltip);
	}

	public void setLabelAndTooltipMaint(boolean visible, int value, int minimum, int maximum, int stepSize, String label, String tooltip) 
	{ 
		_versionMaint_lbl.setVisible(visible);
		_versionMaint_sp .setVisible(visible);

		_versionMaint_spm.setValue(value);
		_versionMaint_spm.setMinimum(minimum);
		_versionMaint_spm.setMaximum(maximum);
		_versionMaint_spm.setStepSize(stepSize);
		
		_versionMaint_lbl.setText(label);

		_versionMaint_lbl.setToolTipText(tooltip);
		_versionMaint_sp .setToolTipText(tooltip);
	}

	public void setLabelAndTooltipSp(boolean visible, int value, int minimum, int maximum, int stepSize, String label, String tooltip) 
	{ 
		_versionEsd_lbl.setVisible(visible);
		_versionEsd_sp .setVisible(visible);

		_versionEsd_spm.setValue(value);
		_versionEsd_spm.setMinimum(minimum);
		_versionEsd_spm.setMaximum(maximum);
		_versionEsd_spm.setStepSize(stepSize);
		
		_versionEsd_lbl.setText(label);

		_versionEsd_lbl.setToolTipText(tooltip);
		_versionEsd_sp .setToolTipText(tooltip);
	}

	public void setLabelAndTooltipPl(boolean visible, int value, int minimum, int maximum, int stepSize, String label, String tooltip) 
	{ 
		_versionPl_lbl.setVisible(visible);
		_versionPl_sp .setVisible(visible);

		_versionPl_spm.setValue(value);
		_versionPl_spm.setMinimum(minimum);
		_versionPl_spm.setMaximum(maximum);
		_versionPl_spm.setStepSize(stepSize);
		
		_versionPl_lbl.setText(label);

		_versionPl_lbl.setToolTipText(tooltip);
		_versionPl_sp .setToolTipText(tooltip);
	}

//	public void setLabelAndTooltipEdition(boolean visible, String label, String tooltip) 
//	{ 
//		_versionIsCe_chk.setVisible(visible);
//
//		_versionIsCe_chk.setText(label);
//
//		_versionIsCe_chk.setToolTipText(label);
//	}


	protected long getVersionNumberFromSpinners()
	{
		int major = _versionMajor_spm.getNumber().intValue();
		int minor = _versionMinor_spm.getNumber().intValue();
		int maint = _versionMaint_spm.getNumber().intValue();
		int esd   = _versionEsd_spm  .getNumber().intValue();
		int pl    = _versionPl_spm   .getNumber().intValue();

		long ver = Ver.ver(major, minor, maint, esd, pl);
		
		return ver;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
//		long ver = getVersionNumberFromSpinners();
//		boolean isCeEnabled = _versionIsCe_chk.isSelected();

//		loadFieldsUsingVersion(ver, isCeEnabled);
//		_propDialog.loadFieldsUsingVersion(ver, isCeEnabled);

		DbmsVersionInfo versionInfo = createDbmsVersionInfo();
		
		loadFieldsUsingVersion(versionInfo);
		_propDialog.loadFieldsUsingVersion(versionInfo);
	}

	/** 
	 * The DBMS Vendor implementation should create the version info
	 * @return
	 */
	protected abstract DbmsVersionInfo createDbmsVersionInfo();


	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

//		if ( _versionIsCe_chk.equals(source) ) 
//			stateChanged(null);

		if ( _versionShort_txt.equals(source) )
		{
			String versionStr = _versionShort_txt.getText();
			parseVersionString(versionStr);
		}
	}

	public abstract long parseVersionStringToNum(String versionStr);
	public abstract String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl);
	
	public void parseVersionString(String versionStr)
	{
		long version = parseVersionStringToNum(versionStr);
		
		int major = Ver.versionNumPart(version, Ver.VERSION_MAJOR);
		int minor = Ver.versionNumPart(version, Ver.VERSION_MINOR);
		int maint = Ver.versionNumPart(version, Ver.VERSION_MAINTENANCE);
		int esd   = Ver.versionNumPart(version, Ver.VERSION_SERVICE_PACK);
		int pl    = Ver.versionNumPart(version, Ver.VERSION_PATCH_LEVEL);

		_logger.debug("parseVersionString: versionStr='"+versionStr+"', version="+version+", major="+major+", minor="+minor+", maint="+maint+", esd="+esd+", pl="+pl+".");
		
		_versionMajor_spm.setValue(major);
		_versionMinor_spm.setValue(minor);
		_versionMaint_spm.setValue(maint);
		_versionEsd_spm  .setValue(esd);
		_versionPl_spm   .setValue(pl);

//		_versionShort_txt.setText(AseConnectionUtils.versionNumToStr(version));
//		_versionInt_txt  .setText(" " + Long.toString(version) );
		
		String versionIntFormatedStr = String.format("%d %02d %02d %04d %04d", major, minor, maint, esd, pl);
		

		_versionInt_txt.setText(versionIntFormatedStr);
	}

	public void loadFieldsUsingVersion(DbmsVersionInfo versionInfo)
	{
//System.out.println("loadFieldsUsingVersion(versionInfo="+versionInfo+")");

		long srvVersion = versionInfo.getLongVersion();
		
		int major = Ver.versionNumPart(srvVersion, Ver.VERSION_MAJOR);
		int minor = Ver.versionNumPart(srvVersion, Ver.VERSION_MINOR);
		int maint = Ver.versionNumPart(srvVersion, Ver.VERSION_MAINTENANCE);
		int sp    = Ver.versionNumPart(srvVersion, Ver.VERSION_SERVICE_PACK);
		int pl    = Ver.versionNumPart(srvVersion, Ver.VERSION_PATCH_LEVEL);
		
		String srvVersionStr = versionNumToString(srvVersion, major, minor, maint, sp, pl);

		_logger.debug("loadFieldsUsingVersion(): version="+srvVersion+", srvVersionStr='"+srvVersionStr+"'.");
		
		_versionShort_txt.setText(srvVersionStr);
		parseVersionString(srvVersionStr);

//		_versionIsCe_chk.setSelected(isCeEnabled);
	}

//	public void loadFieldsUsingVersion(long srvVersion, boolean isCeEnabled)
//	{
//		int major = Ver.versionNumPart(srvVersion, Ver.VERSION_MAJOR);
//		int minor = Ver.versionNumPart(srvVersion, Ver.VERSION_MINOR);
//		int maint = Ver.versionNumPart(srvVersion, Ver.VERSION_MAINTENANCE);
//		int sp    = Ver.versionNumPart(srvVersion, Ver.VERSION_SERVICE_PACK);
//		int pl    = Ver.versionNumPart(srvVersion, Ver.VERSION_PATCH_LEVEL);
//		
//		String srvVersionStr = versionNumToString(srvVersion, major, minor, maint, sp, pl);
//
//
//		_logger.debug("loadFieldsUsingVersion(): version="+srvVersion+", srvVersionStr='"+srvVersionStr+"'.");
//		
//		_versionShort_txt.setText(srvVersionStr);
//		parseVersionString(srvVersionStr);
//
//		_versionIsCe_chk.setSelected(isCeEnabled);
//	}

//	public long getMinVersion()
//	{
//		return 0;
//	}
	public abstract long getMinVersion();

	protected abstract DbmsVersionInfo createEmptyDbmsVersionInfo();
}
