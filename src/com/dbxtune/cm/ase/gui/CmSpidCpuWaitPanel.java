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
package com.dbxtune.cm.ase.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.CmSpidCpuWait;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmSpidCpuWaitPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_monSqlText       = "<html>Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.<br>    This will help us to diagnose what SQL the client sent to the server.</html>";
	public static final String  TOOLTIP_sample_dbccSqlText      = "<html>Do 'dbcc sqltext(spid)' on every row in the table.<br>     This will help us to diagnose what SQL the client sent to the server.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>";
	public static final String  TOOLTIP_sample_procCallStack    = "<html>Do 'select * from monProcessProcedures where SPID=spid.<br>This will help us to diagnose what stored procedure called before we ended up here.</html>";
	public static final String  TOOLTIP_sample_showplan         = "<html>Do 'sp_showplan spid' on every row in the table.<br>       This will help us to diagnose if the current SQL statement is doing something funky.</html>";
	public static final String  TOOLTIP_sample_dbccStacktrace   = "<html>do 'dbcc stacktrace(spid)' on every row in the table.<br>  This will help us to diagnose what peace of code the ASE Server is currently executing.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>";
	public static final String  TOOLTIP_sample_freezeMda        = 
		"<html>" +
		"Freeze MDA Counters while querying the MDA tables in this Performance Counter.<br>" +
		"<br>" +
		"This will stop MDA counter updates during the freeze period so that data from multiple tables will be 'in sync'.<br>" +
		"The negative side effect of this is that some counter incrementation wont happen while executing the SQL Statement that fetches data.<br>" +
		"<br>" +
		"<b>Note:</b> This only works on ASE Version "+Ver.versionNumToStr(CmSpidCpuWait.NEED_SRV_VERSION_sample_freezeMda)+" or higher, and you need to have 'sa_role' as well." +
		"</html>";
	public static final String  TOOLTIP_sample_systemSpids      = "<html>Include system SPID's</html>";
	public static final String  TOOLTIP_sample_extraWhereClause = 
		"<html>" +
		"Add extra where clause to the query that fetches information for SPID's<br>" +
		"To check SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
		"<br>" +
		"<b>Examples:</b><br>" +
		"<b>- Only users with the login 'sa'</b><br>" +
		"<code>A.SPID in (select spid from master..sysprocesses where suser_name(suid) = 'sa')                     </code><br>" +
		"<br>" +
		"<b>- Same as above, but in a more efficent way</b><br>" +
		"<code>suser_name(A.ServerUserID) = 'sa'                                                                   </code><br>" +
		"<br>" +
		"<b>- Only with programs that has logged in via 'isql'</b><br>" +
		"<code>A.SPID in (select spid from master..sysprocesses where program_name = 'isql')                       </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the host 'host99'</b><br>" +
		"<code>A.SPID in (select spid from master..sysprocesses where hostname = 'host99')                         </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the IP address '192.168.0.1'</b><br>" +
		"<code>A.SPID in (select spid from master..sysprocesses where ipaddr = '192.168.0.123')                    </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in to ASE in the last 60 seconds</b><br>" +
		"<code>A.SPID in (select spid from master..sysprocesses where datediff(ss,loggedindatetime,getdate()) < 60)</code><br>" +
		"</html>";
		
	public CmSpidCpuWaitPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// HIGHLIGHTER that changes color when a new SPID number is on next row...

		if (conf != null) 
			colorStr = conf.getProperty(getName()+".color.group");

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			boolean[] _rowIsHighlighted = new boolean[0];
			int       _lastRowId        = 0;     // Used to sheet on table refresh

			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.row == 0)
					return false;

				// Resize array if it's to small
				if (_rowIsHighlighted.length < adapter.getRowCount())
					_rowIsHighlighted = new boolean[adapter.getRowCount()];

				// Lets try to sheet a bit, if we are of some row as last invocation, reuse that decision
				if (_lastRowId == adapter.row)
					return _rowIsHighlighted[adapter.row];
				_lastRowId = adapter.row;

				// Lets get values of "change color" column
				int    spidCol      = adapter.getColumnIndex("SPID");
				int    thisModelRow = adapter.convertRowIndexToModel(adapter.row);
				int    prevModelRow = adapter.convertRowIndexToModel(adapter.row - 1);

				Object thisSpid    = adapter.getValueAt(thisModelRow, spidCol);
				Object prevSpid    = adapter.getValueAt(prevModelRow, spidCol);

				// Previous rows highlight will be a decision to keep or invert the highlight
				boolean prevRowIsHighlighted = _rowIsHighlighted[adapter.row - 1];

				if (thisSpid != null && thisSpid.equals(prevSpid))
				{
					// Use same highlight value as previous row
					boolean isHighlighted = prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
				else
				{
					// Invert previous highlight value
					boolean isHighlighted = ! prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
			}
		}, SwingUtils.parseColor(colorStr, HighlighterFactory.GENERIC_GRAY), null));
	}

	private JCheckBox        _sampleMonSqltext_chk;
	private JCheckBox        _sampleDbccSqltext_chk;
	private JCheckBox        _sampleProcCallStack_chk;
	private JCheckBox        _sampleShowplan_chk;
	private JCheckBox        _sampleDbccStacktrace_chk;
	private JCheckBox        _sampleFreezeMda_chk;
	private JCheckBox        _sampleSystemSpids_chk;
	private RSyntaxTextAreaX _sampleExtraWhereClause_txt;
	private JButton          _sampleExtraWhereClause_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				_sampleMonSqltext_chk    .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_monSqlText,       CmSpidCpuWait.DEFAULT_sample_monSqlText    ));
				_sampleDbccSqltext_chk   .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_dbccSqlText,      CmSpidCpuWait.DEFAULT_sample_dbccSqlText   ));
				_sampleProcCallStack_chk .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_procCallStack,    CmSpidCpuWait.DEFAULT_sample_procCallStack ));
				_sampleShowplan_chk      .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_showplan,         CmSpidCpuWait.DEFAULT_sample_showplan      ));
				_sampleDbccStacktrace_chk.setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_dbccStacktrace,   CmSpidCpuWait.DEFAULT_sample_dbccStacktrace));
				_sampleFreezeMda_chk     .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_freezeMda,        CmSpidCpuWait.DEFAULT_sample_freezeMda     ));
				_sampleSystemSpids_chk   .setSelected(conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_systemSpids,      CmSpidCpuWait.DEFAULT_sample_systemSpids   ));
				
				_sampleExtraWhereClause_txt.setText(  conf.getProperty       (CmSpidCpuWait.PROPKEY_sample_extraWhereClause, CmSpidCpuWait.DEFAULT_sample_extraWhereClause));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("flowy, ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
				"<br>" +
				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		_sampleMonSqltext_chk       = new JCheckBox("Get Monitored SQL Text",   conf == null ? CmSpidCpuWait.DEFAULT_sample_monSqlText     : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_monSqlText,     CmSpidCpuWait.DEFAULT_sample_monSqlText    ));
		_sampleDbccSqltext_chk      = new JCheckBox("Get DBCC SQL Text",        conf == null ? CmSpidCpuWait.DEFAULT_sample_dbccSqlText    : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_dbccSqlText,    CmSpidCpuWait.DEFAULT_sample_dbccSqlText   ));
		_sampleProcCallStack_chk    = new JCheckBox("Get Procedure Call Stack", conf == null ? CmSpidCpuWait.DEFAULT_sample_procCallStack  : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_procCallStack,  CmSpidCpuWait.DEFAULT_sample_procCallStack ));
		_sampleShowplan_chk         = new JCheckBox("Get Showplan",             conf == null ? CmSpidCpuWait.DEFAULT_sample_showplan       : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_showplan,       CmSpidCpuWait.DEFAULT_sample_showplan      ));
		_sampleDbccStacktrace_chk   = new JCheckBox("Get ASE Stacktrace",       conf == null ? CmSpidCpuWait.DEFAULT_sample_dbccStacktrace : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_dbccStacktrace, CmSpidCpuWait.DEFAULT_sample_dbccStacktrace));
		_sampleFreezeMda_chk        = new JCheckBox("Freeze MDA Counters",      conf == null ? CmSpidCpuWait.DEFAULT_sample_freezeMda      : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_freezeMda,      CmSpidCpuWait.DEFAULT_sample_freezeMda     ));
		_sampleSystemSpids_chk      = new JCheckBox("Get System SPID's",        conf == null ? CmSpidCpuWait.DEFAULT_sample_systemSpids    : conf.getBooleanProperty(CmSpidCpuWait.PROPKEY_sample_systemSpids,    CmSpidCpuWait.DEFAULT_sample_systemSpids   ));
		_sampleExtraWhereClause_txt = new RSyntaxTextAreaX();
		_sampleExtraWhereClause_but = new JButton("Apply Extra Where Clause");

//		_sampleMonSqltext_chk      .setName(CmSpidCpuWait.PROPKEY_sample_monSqlText);
//		_sampleDbccSqltext_chk     .setName(CmSpidCpuWait.PROPKEY_sample_dbccSqlText);
//		_sampleProcCallStack_chk   .setName(CmSpidCpuWait.PROPKEY_sample_procCallStack);
//		_sampleShowplan_chk        .setName(CmSpidCpuWait.PROPKEY_sample_showplan);
//		_sampleSystemSpids_chk     .setName(CmSpidCpuWait.PROPKEY_sample_systemSpids);
//		_sampleExtraWhereClause_txt.setName(CmSpidCpuWait.PROPKEY_sample_extraWhereClause);
		
		_sampleMonSqltext_chk      .setToolTipText(TOOLTIP_sample_monSqlText);
		_sampleDbccSqltext_chk     .setToolTipText(TOOLTIP_sample_dbccSqlText);
		_sampleProcCallStack_chk   .setToolTipText(TOOLTIP_sample_procCallStack);
		_sampleShowplan_chk        .setToolTipText(TOOLTIP_sample_showplan);
		_sampleDbccStacktrace_chk  .setToolTipText(TOOLTIP_sample_dbccStacktrace);
		_sampleFreezeMda_chk       .setToolTipText(TOOLTIP_sample_freezeMda);
		_sampleExtraWhereClause_but.setToolTipText(TOOLTIP_sample_extraWhereClause);
		_sampleExtraWhereClause_txt.setToolTipText(TOOLTIP_sample_extraWhereClause);
		_sampleSystemSpids_chk     .setToolTipText(TOOLTIP_sample_systemSpids);


		// Set initial values for some fields
		String sampleExtraWhereClause = (conf == null ? CmSpidCpuWait.DEFAULT_sample_extraWhereClause : conf.getProperty(CmSpidCpuWait.PROPKEY_sample_extraWhereClause, CmSpidCpuWait.DEFAULT_sample_extraWhereClause));

		_sampleExtraWhereClause_txt.setText(sampleExtraWhereClause);
		_sampleExtraWhereClause_txt.setHighlightCurrentLine(false);
		_sampleExtraWhereClause_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

//		panel.add(_sampleMonSqltext_chk,       "");                 // row 1 cell 1
//		panel.add(_sampleProcCallStack_chk,    "wrap");             // row 1 cell 2
//		panel.add(_sampleDbccSqltext_chk,      "");                 // row 2 cell 1
//		panel.add(_sampleSystemSpids_chk,      "wrap");             // row 2 cell 2
//		panel.add(_sampleShowplan_chk,         "");                 // row 3 cell 1
//		panel.add(_sampleExtraWhereClause_txt, "grow, push, wrap"); // row 3 cell 2
//		panel.add(_sampleDbccStacktrace_chk,   "");                 // row 4 cell 1
//		panel.add(_sampleExtraWhereClause_but, "wrap");             // row 4 cell 2

		panel.add(_sampleMonSqltext_chk,       "");
		panel.add(_sampleDbccSqltext_chk,      "");
		panel.add(_sampleShowplan_chk,         "");
		panel.add(_sampleDbccStacktrace_chk,   "wrap");

		panel.add(_sampleProcCallStack_chk,    "");
		panel.add(_sampleSystemSpids_chk,      "");
		panel.add(_sampleFreezeMda_chk,        "wrap");

		panel.add(_sampleExtraWhereClause_txt, "spany 3, grow, push");
		panel.add(_sampleExtraWhereClause_but, "wrap");

		
		_sampleMonSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_monSqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		_sampleDbccSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_dbccSqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		_sampleProcCallStack_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_procCallStack, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		_sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_showplan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		_sampleDbccStacktrace_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_dbccStacktrace, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		
		_sampleFreezeMda_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_freezeMda, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		_sampleSystemSpids_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_systemSpids, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		_sampleExtraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpidCpuWait.PROPKEY_sample_extraWhereClause, _sampleExtraWhereClause_txt.getText().trim());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			if (cm.isRuntimeInitialized())
			{
				// disable some options if we do not have 'sybase_ts_role'
				if ( cm.isServerRoleOrPermissionActive(AseConnectionUtils.SYBASE_TS_ROLE) )
				{
					_sampleDbccSqltext_chk   .setEnabled(true);
					_sampleDbccStacktrace_chk.setEnabled(true);
				}
				else
				{
					_sampleDbccSqltext_chk   .setEnabled(false);
					_sampleDbccStacktrace_chk.setEnabled(false);

					_sampleDbccSqltext_chk   .setSelected(false);
					_sampleDbccStacktrace_chk.setSelected(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmSpidCpuWait.PROPKEY_sample_dbccSqlText,    false);
						conf.setProperty(CmSpidCpuWait.PROPKEY_sample_dbccStacktrace, false);
					}
				}

				// disable some options if we do not have ASE RELEASE 12.5.4 and 'sa_role'
				if ( cm.getServerVersion() >= CmSpidCpuWait.NEED_SRV_VERSION_sample_freezeMda 
				     && cm.isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE) )
				{
					_sampleFreezeMda_chk.setEnabled(true);
				}
				else
				{
					_sampleFreezeMda_chk.setEnabled(false);
					_sampleFreezeMda_chk.setSelected(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmSpidCpuWait.PROPKEY_sample_freezeMda, false);
						cm.setSql(null);
					}
				}
			} // end isRuntimeInitialized
		} // end (cm != null)
	}
}
