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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.CmObjectActivity;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmObjectActivityPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmObjectActivity.CM_NAME;

	private JCheckBox l_sampleSystemTables_chk;
	private JCheckBox l_sampleRowCount_chk;
	private JCheckBox l_sampleTempdbWorkTables_chk;

	public CmObjectActivityPanel(CountersModel cm)
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

		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// BLOB (text/image columns)
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blob");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() == 255)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorConstants.COLOR_DATATYPE_BLOB), null));

		// WORK TABLES (in tempdb)
		if (conf != null) colorStr = conf.getProperty(getName()+".color.tempdbWorkTables");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String LockScheme = adapter.getString(adapter.getColumnIndex("LockScheme"));
				if ( "WORK-TABLE".equals(LockScheme))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorConstants.COLOR_TEMPDB_WORK_TABLE), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				l_sampleRowCount_chk.setSelected(conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, CmObjectActivity.DEFAULT_sample_tabRowCount));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
		int     defaultIntOpt;

		//-----------------------------------------
		// RowCount
		//-----------------------------------------
		defaultOpt = conf == null ? CmObjectActivity.DEFAULT_sample_tabRowCount : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, CmObjectActivity.DEFAULT_sample_tabRowCount);
//		JCheckBox sampleRowCount_chk = new JCheckBox("Sample Table Row Count", defaultOpt);
		l_sampleRowCount_chk = new JCheckBox("Sample Table Row Count", defaultOpt);

		l_sampleRowCount_chk.setName(CmObjectActivity.PROPKEY_sample_tabRowCount);
		l_sampleRowCount_chk.setToolTipText("<html>" +
				"Sample Table Row Count using ASE functions <code>row_count()</code> and <code>data_pages()</code>.<br>" +
				"<b>Note 1</b>: Only in ASE 15.0.2 or higher.<br>" +
				"<b>Note 2</b>: You can also set the property '"+CmObjectActivity.PROPKEY_sample_tabRowCount+"'=true|false' in the configuration file.<br>" +
				"<b>Note 3</b>: To check if this is enabled or not, use the Properties dialog in this tab pane, right click + properties...<br>" +
				"<b>Note 4</b>: Warning this may <b>block</b> collection if anyone holds <b>exclusive table locks</b>. In ASE 15.7 SP130 or above function row_count() has option 'noblock', which will be used.<br>" +
				"</html>");

		l_sampleRowCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		//-----------------------------------------
		// Top Rows (top #)
		//-----------------------------------------
		defaultOpt    = conf == null ? CmObjectActivity.DEFAULT_sample_topRows      : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_topRows,      CmObjectActivity.DEFAULT_sample_topRows);
		defaultIntOpt = conf == null ? CmObjectActivity.DEFAULT_sample_topRowsCount : conf.getIntProperty    (CmObjectActivity.PROPKEY_sample_topRowsCount, CmObjectActivity.DEFAULT_sample_topRowsCount);
		final JCheckBox  sampleTopRows_chk      = new JCheckBox("Limit number of rows (top #)", defaultOpt);
		final JTextField sampleTopRowsCount_txt = new JTextField(Integer.toString(defaultIntOpt), 5);

		sampleTopRows_chk.setName(CmObjectActivity.PROPKEY_sample_topRows);
		sampleTopRows_chk.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmObjectActivity.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRowsCount_txt.setName(CmObjectActivity.PROPKEY_sample_topRowsCount);
		sampleTopRowsCount_txt.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmObjectActivity.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmObjectActivity.PROPKEY_sample_topRows, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		final ActionListener sampleTopRowsCount_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = sampleTopRowsCount_txt.getText();
				int    intVal = CmObjectActivity.DEFAULT_sample_topRowsCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmObjectActivity.DEFAULT_sample_topRowsCount;
					SwingUtils.showWarnMessage(CmObjectActivityPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					sampleTopRowsCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmObjectActivity.PROPKEY_sample_topRowsCount, intVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		sampleTopRowsCount_txt.addActionListener(sampleTopRowsCount_action);
		sampleTopRowsCount_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleTopRowsCount_txt, so we don't have to duplicate code.
				sampleTopRowsCount_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		//-----------------------------------------
		// sample system tables: traceon(3650)
		//-----------------------------------------
		defaultOpt = conf == null ? CmObjectActivity.DEFAULT_sample_systemTables : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_systemTables, CmObjectActivity.DEFAULT_sample_systemTables);
//		JCheckBox sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultOpt);
		l_sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultOpt);

		l_sampleSystemTables_chk.setName(CmObjectActivity.PROPKEY_sample_systemTables);
		l_sampleSystemTables_chk.setToolTipText("<html>" +
				"Include system tables in the output<br>" +
				"<b>Note 1</b>: This is enabled with dbcc traceon(3650).<br>" +
				"<b>Note 2</b>: The traceflag 3650 is <b>not</b> checked/set before every sample, it's only set when this checkbox is touched.<br>" +
				"<b>A word of warning</b>: This traceflag may/will cause performance degradation in the server, so <b>disable it when you are done</b>.<br>" +
				"</html>");

		l_sampleSystemTables_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean isSelected = ((JCheckBox)e.getSource()).isSelected();
				
				if (isSelected)
				{
					String htmlMsg = "<html>"
							+ "<h3>Warning</h3>"
							+ "This option will enable trace flag 3650 at the server.<br>"
							+ "<br>"
							+ "Running with trace flag 3650 will add contention and lower overall system performance.<br>"
							+ "It is advised that you run this for short durations of time, generally less than 30 minutes.<br>"
							+ "<br>"
							+ "<b>Please do not forget to disable this before you disconnect!</b><br>"
							+ "<br>"
							+ "Do you still want to enable this option?<br>"
//							+ "Are you shure you want to do this?<br>"
							+ "</html>";
					int yesNo = JOptionPane.showConfirmDialog(CmObjectActivityPanel.this, 
							htmlMsg, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if ( yesNo == JOptionPane.NO_OPTION )
					{
						((JCheckBox)e.getSource()).setSelected(false);
						return;
					}
				}
				
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmObjectActivity.PROPKEY_sample_systemTables, isSelected);
				conf.save();


//				FIXME:
				String sql = isSelected ? "dbcc traceon(3650)" : "dbcc traceoff(3650)";
				try
				{
					Connection conn = getCm().getCounterController().getMonConnection();
					Statement stmnt = conn.createStatement();
					stmnt.executeUpdate(sql);
					stmnt.close();
				}
				catch (SQLException sqle)
				{
					String htmlMsg = "<html>"
							+ "<h3>Problems toggling traceflag 3650</h3>"
							+ "" + sqle.getMessage() + "<br>"
							+ "</html>";
					SwingUtils.showErrorMessage(CmObjectActivityPanel.this, "Problems setting on/off traceflag(3650)",htmlMsg, sqle);
				}

//				// This will force the CM to re-initialize the SQL statement.
//				CountersModel cm = getCm().getCounterController().getCmByName(getName());
//				if (cm != null)
//					cm.setSql(null);
			}
		});
		
		//-----------------------------------------
		// sample tempdb 'work tables'
		//-----------------------------------------
		defaultOpt = conf == null ? CmObjectActivity.DEFAULT_sample_tempdbWorkTables : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_tempdbWorkTables, CmObjectActivity.DEFAULT_sample_tempdbWorkTables);
		l_sampleTempdbWorkTables_chk = new JCheckBox("Include tempdb Work Tables", defaultOpt);

		l_sampleTempdbWorkTables_chk.setName(CmObjectActivity.PROPKEY_sample_tempdbWorkTables);
		l_sampleTempdbWorkTables_chk.setToolTipText("<html>" +
				"Include tempdb 'work table' in the output<br>" +
				"<b>Note 1</b>: This will add a second SELECT statement on table 'monProcessObject'.<br>" +
				"</html>");

		l_sampleTempdbWorkTables_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean isSelected = ((JCheckBox)e.getSource()).isSelected();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);

				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf != null)
				{
					conf.setProperty(CmObjectActivity.PROPKEY_sample_tempdbWorkTables, isSelected);
					conf.save();
				}
			}
		});

		// LAYOUT
		panel.add(l_sampleRowCount_chk,         "wrap");
		
		panel.add(sampleTopRows_chk,            "split");
		panel.add(sampleTopRowsCount_txt,       "wrap");

		panel.add(l_sampleSystemTables_chk,     "wrap");
		panel.add(l_sampleTempdbWorkTables_chk, "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// PROPKEY_sample_systemTables
		boolean confProp = conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_systemTables, CmObjectActivity.DEFAULT_sample_systemTables);
		boolean guiProp  = l_sampleSystemTables_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSystemTables_chk.setSelected(confProp);

		
		confProp = conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, CmObjectActivity.DEFAULT_sample_tabRowCount);
		guiProp  = l_sampleRowCount_chk.isSelected();

		if (confProp != guiProp)
			l_sampleRowCount_chk.setSelected(confProp);
	}

	@Override
	public boolean ddlRequestInfo()
	{
		return true;
	}
	@Override
	public void ddlRequestInfoSave(JTable table)
	{
		if (table == null)
			return;

		if ( ! PersistentCounterHandler.hasInstance() )
			return;

		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

		int DBName_pos     = -1;
		int ObjectName_pos = -1;
//		int IndexID_pos    = -1;
		for (int c=0; c<table.getColumnCount(); c++)
		{
			if ( "DBName".equals(table.getColumnName(c)) )
				DBName_pos = c;

			if ( "ObjectName".equals(table.getColumnName(c)) )
				ObjectName_pos = c;

//			if ( "IndexID".equals(table.getColumnName(c)) )
//				IndexID_pos = c;

//			if (DBName_pos >= 0 && ObjectName_pos >= 0 && IndexID_pos >= 0)
//				break;
			if (DBName_pos >= 0 && ObjectName_pos >= 0)
				break;
		}

		// HOW MANY TOP ROWS SHOULD WE GRAB FROM THE JTABLE
		int NUM_OF_DDLS_TO_PERSIST = 10;
		
		int rows = Math.min(NUM_OF_DDLS_TO_PERSIST, table.getRowCount());
		for (int r=0; r<rows; r++)
		{
			Object DBName_obj     = table.getValueAt(r, DBName_pos);
			Object ObjectName_obj = table.getValueAt(r, ObjectName_pos);
//			Object IndexID_obj    = table.getValueAt(r, IndexID_pos);

			// Skip index rows... (change the loop to do this)
//			if (IndexID_obj instanceof Number)
//			{
//				if ( ((Number)IndexID_obj).intValue() > 0 )
//					continue;
//			}
			if (DBName_obj instanceof String && ObjectName_obj instanceof String)
				pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".guiSorted, row="+r);
		}
		
	}
}
