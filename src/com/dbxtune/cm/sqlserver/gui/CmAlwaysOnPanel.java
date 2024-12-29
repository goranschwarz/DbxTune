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
package com.dbxtune.cm.sqlserver.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmAlwaysOn;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.tools.WindowType;
import com.dbxtune.tools.sqlw.QueryWindow;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmAlwaysOnPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOpenTransactions.CM_NAME;

	private JCheckBox  l_updateActive_chk;

	private JLabel     l_updateActiveInterval_lbl;
	private JTextField l_updateActiveInterval_txt;

	private JCheckBox  l_showRemoteRows_chk;
	private JCheckBox  l_sampleLiveRemoteData_chk;
	private JCheckBox  l_showLiveRemoteData_chk;
	
	public CmAlwaysOnPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

//		// PINK = spid is BLOCKED by some other user
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number BlockedBySpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockedBySpid"));
//				if ( BlockedBySpid != null && BlockedBySpid.intValue() != 0 )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = PROBLEMS
		if (conf != null) colorStr = conf.getProperty(getName()+".color.problems");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int    modelCol = adapter.convertColumnIndexToModel(adapter.column);
				String colName  = adapter.getColumnName(modelCol);
				
				//-----------------------------------------------------------
				if ("role_desc".equals(colName))
				{
					return ! StringUtil.equalsAny(adapter.getString(), "", CmAlwaysOn.PRIMARY_ROLE, CmAlwaysOn.SECONDARY_ROLE);
				}

				//-----------------------------------------------------------
				if ("Validated".equals(colName))
				{
					return ! StringUtil.equalsAny(adapter.getString(), "", "YES", "-");
				}

				//-----------------------------------------------------------
				if ("BagPct".equals(colName))
				{
					Object obj = adapter.getValue();
					if (obj != null && obj instanceof Number)
					{
						int val = ((Number)obj).intValue();
						if ( val < 100 && val != -1 )
							return true;
					}
					return false;
				}

				//-----------------------------------------------------------
				if ("operational_state_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;
					return ! "ONLINE".equals(adapter.getString());
				}

				//-----------------------------------------------------------
				else if ("connected_state_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;
					return ! "CONNECTED".equals(adapter.getString());
				}

				//-----------------------------------------------------------
				else if ("recovery_health_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;
					return ! "ONLINE".equals(adapter.getString());
				}

				//-----------------------------------------------------------
				else if ("synchronization_health_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;
					return ! "HEALTHY".equals(adapter.getString());
				}

				//-----------------------------------------------------------
				else if ("synchronization_state_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;

					if (isSyncCommit(adapter))
						return ! "SYNCHRONIZED".equals(adapter.getString());

					if (isAsyncCommit(adapter))
						return ! "SYNCHRONIZING".equals(adapter.getString());
				}

				//-----------------------------------------------------------
				else if ("suspended_reason_desc".equals(colName))
				{
					// should be: null
					return adapter.getValue() != null;
				}

				//-----------------------------------------------------------
				else if ("database_state_desc".equals(colName))
				{
					if (isRemote(adapter) && adapter.getValue() == null)
						return false;
					return ! "ONLINE".equals(adapter.getString());
				}

				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

		
		// GREEN = Validated
		if (conf != null) colorStr = conf.getProperty(getName()+".color.Validated");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int    modelCol = adapter.convertColumnIndexToModel(adapter.column);
				String colName  = adapter.getColumnName(modelCol);
				
				//-----------------------------------------------------------
				if ("role_desc".equals(colName))
				{
					if ( StringUtil.equalsAny(adapter.getString(), CmAlwaysOn.PRIMARY_ROLE, CmAlwaysOn.SECONDARY_ROLE) )
					{
						int mcol = adapter.getColumnIndex("Validated");
						if (mcol != -1)
						{
							return "YES".equals(adapter.getString(mcol));
						}
						return false;
					}
				}

				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));
	}
	
	private static boolean isRemote(ComponentAdapter adapter)
	{
		int mcol = adapter.getColumnIndex("locality");
		if (mcol != -1)
		{
			return CmAlwaysOn.COLVAL_LOCALITY_REMOTE.equals(adapter.getValue(mcol));
		}
		return false;
	}
	private static boolean isLocal(ComponentAdapter adapter)
	{
		int mcol = adapter.getColumnIndex("locality");
		if (mcol != -1)
		{
			return CmAlwaysOn.COLVAL_LOCALITY_LOCAL.equals(adapter.getValue(mcol));
		}
		return false;
	}
	private static boolean isAsyncCommit(ComponentAdapter adapter)
	{
		int mcol = adapter.getColumnIndex("availability_mode_desc");
		if (mcol != -1)
		{
			return "ASYNCHRONOUS_COMMIT".equals(adapter.getValue(mcol));
		}
		return false;
	}
	private static boolean isSyncCommit(ComponentAdapter adapter)
	{
		int mcol = adapter.getColumnIndex("availability_mode_desc");
		if (mcol != -1)
		{
			return "SYNCHRONOUS_COMMIT".equals(adapter.getValue(mcol));
		}
		return false;
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Update Primary DB",                     PROPKEY_update_primary              , Boolean.class, conf.getBooleanProperty(PROPKEY_update_primary               , DEFAULT_update_primary               ), DEFAULT_update_primary              , "Update Active DB" ));
//				list.add(new CmSettingsHelper("Update Primary DB Interval",            PROPKEY_update_primaryIntervalInSec , Long   .class, conf.getLongProperty   (PROPKEY_update_primaryIntervalInSec  , DEFAULT_update_primaryIntervalInSec  ), DEFAULT_update_primaryIntervalInSec , "Update Active DB, Every X second." ));
//
//				list.add(new CmSettingsHelper("Sample Live Remote Data",               PROPKEY_sample_liveRemoteData       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveRemoteData        , DEFAULT_sample_liveRemoteData        ), DEFAULT_sample_liveRemoteData       , "Fetch Live Data from SECONDARY Server, so we can check for 'SPLIT-BRAIN' (role PRIMARY in more than one instance) or similar issues." ));
//				list.add(new CmSettingsHelper("Sample Live Remote Data Perf Counters", PROPKEY_sample_liveRemoteData       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveRemoteDataPerfCnt , DEFAULT_sample_liveRemoteDataPerfCnt ), DEFAULT_sample_liveRemoteDataPerfCnt, "Fetch Live Data (PerfCounters) from SECONDARY Server, so we evaluate Remote Performance Counters on the local server side. NOTE: This also needs 'Sample Live Remote Data'." ));

				l_updateActive_chk        .setSelected(conf.getBooleanProperty(CmAlwaysOn.PROPKEY_update_primary             , CmAlwaysOn.DEFAULT_update_primary));
				l_updateActiveInterval_txt.setText(""+ conf.getLongProperty   (CmAlwaysOn.PROPKEY_update_primaryIntervalInSec, CmAlwaysOn.DEFAULT_update_primaryIntervalInSec));

				l_showRemoteRows_chk      .setSelected(conf.getBooleanProperty(CmAlwaysOn.PROPKEY_show_RemoteRows      , CmAlwaysOn.DEFAULT_show_RemoteRows));
				l_sampleLiveRemoteData_chk.setSelected(conf.getBooleanProperty(CmAlwaysOn.PROPKEY_sample_liveRemoteData, CmAlwaysOn.DEFAULT_sample_liveRemoteData));
				l_showLiveRemoteData_chk  .setSelected(conf.getBooleanProperty(CmAlwaysOn.PROPKEY_show_liveRemoteData  , CmAlwaysOn.DEFAULT_show_liveRemoteData));

				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		JLabel  getAlwaysOnHealth_lbl     = new JLabel ("<html>Get Extended Events Data for <b>AlwaysOn_health</b></html>");
		JButton getAlwaysOnHealth_all_but = new JButton("ALL");
		JButton getAlwaysOnHealth_err_but = new JButton("Errors");
		JButton getAlwaysOnHealth_stc_but = new JButton("State Changes");
		JButton getAlwaysOnHealth_ddl_but = new JButton("Executed DDL");

		l_updateActive_chk         = new JCheckBox("Update Primary DB,", conf.getBooleanProperty(CmAlwaysOn.PROPKEY_update_primary, CmAlwaysOn.DEFAULT_update_primary));
		l_updateActiveInterval_lbl = new JLabel("Interval in Seconds");
		l_updateActiveInterval_txt = new JTextField(conf.getLongProperty(CmAlwaysOn.PROPKEY_update_primaryIntervalInSec, CmAlwaysOn.DEFAULT_update_primaryIntervalInSec)+"", 5);

		l_showRemoteRows_chk       = new JCheckBox("Show 'REMOTE'",                                            conf.getBooleanProperty(CmAlwaysOn.PROPKEY_show_RemoteRows      , CmAlwaysOn.DEFAULT_show_RemoteRows));
		l_sampleLiveRemoteData_chk = new JCheckBox("Sample '"+CmAlwaysOn.COLVAL_LOCALITY_REMOTE_LIVE_DATA+"'", conf.getBooleanProperty(CmAlwaysOn.PROPKEY_sample_liveRemoteData, CmAlwaysOn.DEFAULT_sample_liveRemoteData));
		l_showLiveRemoteData_chk   = new JCheckBox("Show '"  +CmAlwaysOn.COLVAL_LOCALITY_REMOTE_LIVE_DATA+"'", conf.getBooleanProperty(CmAlwaysOn.PROPKEY_show_liveRemoteData  , CmAlwaysOn.DEFAULT_show_liveRemoteData));

		getAlwaysOnHealth_lbl.setToolTipText("<html>"
				+ "Open a QueryWindow where a SQL Statement will be executed to the <b>current</b> SQL-Server<br>"
				+ "and get <i>REPLACE</i> fields for the Extended Events Session <b>AlwaysOn_health</b><br>"
				+ "<br>"
				+ "Note: This SQL Statement can also be used for other Extended Event Sessions, if you want to extract <b>all</b> fields for a session."
				+ "</html>");
		getAlwaysOnHealth_all_but.setToolTipText(getAlwaysOnHealth_lbl.getToolTipText().replace("REPLACE", "all"));
		getAlwaysOnHealth_err_but.setToolTipText(getAlwaysOnHealth_lbl.getToolTipText().replace("REPLACE", "error"));
		getAlwaysOnHealth_stc_but.setToolTipText(getAlwaysOnHealth_lbl.getToolTipText().replace("REPLACE", "state changes"));
		getAlwaysOnHealth_ddl_but.setToolTipText(getAlwaysOnHealth_lbl.getToolTipText().replace("REPLACE", "DDL Execution"));

		l_updateActive_chk        .setToolTipText("<html>If we are Connect to PRIMARY server of the Availability Group, then Update a dummy table, this to introduce network traffic in a <i>calm</i> system.</html>");
		l_updateActiveInterval_lbl.setToolTipText("<html>Do not update primary side every time, but wait for x second between updates.</html>");
		l_updateActiveInterval_txt.setToolTipText(l_updateActiveInterval_lbl.getToolTipText());

		l_showRemoteRows_chk      .setToolTipText("<html>Show Remote Rows (where column 'locality' = 'REMOTE' )... or simply filter it out.</html>");
		l_sampleLiveRemoteData_chk.setToolTipText("<html>Connect to <b>all</b> the SECONDARY servers and get role and status information from the Remote Server. This so we can check for <i>SPLIT-BRAIN</i> (role PRIMARY in more than one instance) or similar issues. </html>");
		l_showLiveRemoteData_chk  .setToolTipText("<html>Show the Live Data (where column 'locality' = '" + CmAlwaysOn.COLVAL_LOCALITY_REMOTE_LIVE_DATA + "' ) from the Remote servers in the table... or simply filter it out. These rows are mostly to check for <i>SPLIT-BRAIN</i> (role PRIMARY in more than one instance) or similar issues.</html>");

		panel.add(getAlwaysOnHealth_lbl,      "wrap");
		panel.add(getAlwaysOnHealth_all_but,  "split");
		panel.add(getAlwaysOnHealth_err_but,  "");
		panel.add(getAlwaysOnHealth_stc_but,  "");
		panel.add(getAlwaysOnHealth_ddl_but,  "wrap 10");
		
		panel.add(l_updateActive_chk,         "split");
		panel.add(l_updateActiveInterval_lbl, "");
		panel.add(l_updateActiveInterval_txt, "wrap");
		
		panel.add(l_showRemoteRows_chk,       "split");
		panel.add(l_sampleLiveRemoteData_chk, "");
		panel.add(l_showLiveRemoteData_chk,   "wrap");

		
		getAlwaysOnHealth_all_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String sql = XEvent_Shredder_sql.replace("<XE-SESSION-NAME>", "AlwaysOn_health");
				openQueryWindow(sql);
			}
		});
		
		getAlwaysOnHealth_err_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String sql = XEvent_AlwaysOn_health_errors;
				openQueryWindow(sql);
			}
		});
		
		getAlwaysOnHealth_stc_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String sql = XEvent_AlwaysOn_health_stateChange;
				openQueryWindow(sql);
			}
		});
		
		getAlwaysOnHealth_ddl_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String sql = XEvent_AlwaysOn_health_ddl;
				openQueryWindow(sql);
			}
		});


		
		l_updateActive_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});
		
		l_updateActiveInterval_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});
		l_updateActiveInterval_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				applyLocalSettings();
			}
			
			@Override
			public void focusGained(FocusEvent e) {}
		});

		
		
		l_showRemoteRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});

		l_sampleLiveRemoteData_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});
		
		l_showLiveRemoteData_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});


		addRowFilter(new RowFilter<TableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
			{
				if ( l_showRemoteRows_chk.isSelected() && l_showLiveRemoteData_chk.isSelected())
				{
					return true;
				}
				else
				{
					String str = entry.getStringValue(CmAlwaysOn.COLPOS_LOCALITY);

					if (CmAlwaysOn.COLVAL_LOCALITY_REMOTE.equals(str) && ! l_showRemoteRows_chk.isSelected())
						return false;

					if (CmAlwaysOn.COLVAL_LOCALITY_REMOTE_LIVE_DATA.equals(str) && ! l_showLiveRemoteData_chk.isSelected())
						return false;

					return true;
				}
			}
		});
		
		return panel;
	}


	private void applyLocalSettings()
	{
		// Need TMP since we are going to save the configuration somewhere
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		conf.setProperty(CmAlwaysOn.PROPKEY_update_primary             , l_updateActive_chk.isSelected());
		conf.setProperty(CmAlwaysOn.PROPKEY_update_primaryIntervalInSec, StringUtil.parseLong(l_updateActiveInterval_txt.getText(), CmAlwaysOn.DEFAULT_update_primaryIntervalInSec));

		conf.setProperty(CmAlwaysOn.PROPKEY_show_RemoteRows            , l_showRemoteRows_chk.isSelected());
		conf.setProperty(CmAlwaysOn.PROPKEY_sample_liveRemoteData      , l_sampleLiveRemoteData_chk.isSelected());
		conf.setProperty(CmAlwaysOn.PROPKEY_show_liveRemoteData        , l_showLiveRemoteData_chk.isSelected());
		
		conf.save();
		
		// If the 'l_showRemoteRows_chk' or 'l_showLiveRemoteData_chk' the table needs to be updated
		getCm().fireTableDataChanged();
		
		// This will force the CM to re-initialize the SQL statement.
//		CountersModel cm = getCm().getCounterController().getCmByName(getName());
//		if (cm != null)
//			cm.setSql(null);
	}


	private void openQueryWindow(String sql)
	{
		try 
		{
			DbxConnection conn = DbxConnection.connect(SwingUtilities.getWindowAncestor(CmAlwaysOnPanel.this), Version.getAppName()+"-AlwaysOn_health");
			final QueryWindow qw = new QueryWindow(conn, sql, null, true, WindowType.JDIALOG, null);
			qw.setSize(1700, 800);
			qw.setLocationRelativeTo(CmAlwaysOnPanel.this);
			qw.setVisible(true);

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					qw.toFront();
				}
			});
		}
		catch (Exception ex) 
		{
			JOptionPane.showMessageDialog(
				CmAlwaysOnPanel.this, 
				"Problems open SQL Query Window\n" + ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public static final String XEvent_AlwaysOn_health_errors = ""
		+ "WITH XEvents AS \n"
		+ "( \n"
		+ "    SELECT object_name, CAST(event_data AS XML) AS event_data \n"
		+ "    FROM sys.fn_xe_file_target_read_file ( 'AlwaysOn_health*.xel', NULL, NULL, NULL ) \n"
		+ ") \n"
		+ "SELECT \n"
		+ "     event_data.value ('(/event/@timestamp)[1]',                         'DATETIME')      AS [timestamp] \n"
		+ "    ,event_data.value ('(/event/data[@name=''category'']/text)[1]',      'NVARCHAR(MAX)') AS [category] \n"
		+ "    ,event_data.value ('(/event/data[@name=''destination'']/text)[1]',   'NVARCHAR(MAX)') AS [destination] \n"
		+ "    ,event_data.value ('(/event/data[@name=''error_number'']/value)[1]', 'INT')           AS [error_number] \n"
		+ "    ,event_data.value ('(/event/data[@name=''severity'']/value)[1]',     'INT')           AS [severity] \n"
		+ "    ,event_data.value ('(/event/data[@name=''state'']/value)[1]',        'INT')           AS [state] \n"
		+ "    ,event_data.value ('(/event/data[@name=''message'']/value)[1]',      'NVARCHAR(MAX)') AS [message] \n"
		+ "FROM XEvents \n"
		+ "WHERE [object_name] = 'error_reported' \n"
		+ "ORDER BY [timestamp]; \n"
		+ "";

	
	public static final String XEvent_AlwaysOn_health_stateChange = ""
		+ "WITH XEvents AS \n"
		+ "( \n"
		+ "    SELECT object_name, CAST(event_data AS XML) AS event_data \n"
		+ "    FROM sys.fn_xe_file_target_read_file ( 'AlwaysOn_health*.xel', NULL, NULL, NULL ) \n"
		+ ") \n"
		+ "SELECT \n"
		+ "     object_name \n"
		+ "    ,event_data.value ('(/event/@timestamp)[1]',                                      'DATETIME')         AS [timestamp] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_group_name'']/value)[1]',   'NVARCHAR(MAX)')    AS [availability_group_name] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_replica_name'']/value)[1]', 'NVARCHAR(MAX)')    AS [availability_replica_name] \n"
		+ "    ,event_data.value ('(/event/data[@name=''previous_state'']/text)[1]',             'NVARCHAR(MAX)')    AS [previous_state] \n"
		+ "    ,event_data.value ('(/event/data[@name=''current_state'']/text)[1]',              'NVARCHAR(MAX)')    AS [current_state] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_group_id'']/value)[1]',     'UNIQUEIDENTIFIER') AS [availability_group_id] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_replica_id'']/value)[1]',   'UNIQUEIDENTIFIER') AS [availability_replica_id] \n"
		+ "FROM XEvents \n"
		+ "WHERE [object_name] IN ('availability_replica_manager_state_change', 'availability_replica_state', 'availability_replica_state_change') \n"
		+ "ORDER BY [timestamp] \n"
		+ "";
				
	public static final String XEvent_AlwaysOn_health_ddl = ""
		+ "WITH XEvents AS \n"
		+ "( \n"
		+ "    SELECT object_name, CAST(event_data AS XML) AS event_data \n"
		+ "    FROM sys.fn_xe_file_target_read_file ( 'AlwaysOn_health*.xel', NULL, NULL, NULL ) \n"
		+ ") \n"
		+ "SELECT \n"
		+ "     event_data.value ('(/event/@timestamp)[1]',                                    'DATETIME')         AS [timestamp] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_group_name'']/value)[1]', 'NVARCHAR(MAX)')    AS [availability_group_name] \n"
		+ "    ,event_data.value ('(/event/data[@name=''availability_group_id'']/value)[1]',   'UNIQUEIDENTIFIER') AS [availability_group_id] \n"
		+ "    ,event_data.value ('(/event/data[@name=''client_app_name'']/value)[1]',         'NVARCHAR(MAX)')    AS [client_app_name] \n"
		+ "    ,event_data.value ('(/event/data[@name=''client_hostname'']/value)[1]',         'NVARCHAR(MAX)')    AS [client_hostname] \n"
		+ "    ,event_data.value ('(/event/data[@name=''nt_username'']/value)[1]',             'NVARCHAR(MAX)')    AS [nt_username] \n"
		+ "    ,event_data.value ('(/event/data[@name=''ddl_action'']/text)[1]',               'NVARCHAR(MAX)')    AS [ddl_action] \n"
		+ "    ,event_data.value ('(/event/data[@name=''ddl_phase'']/text)[1]',                'NVARCHAR(MAX)')    AS [ddl_phase] \n"
		+ "    ,event_data.value ('(/event/data[@name=''statement'']/value)[1]',               'NVARCHAR(MAX)')    AS [statement] \n"
		+ "FROM XEvents \n"
		+ "WHERE [object_name] IN ('alwayson_ddl_executed') \n"
		+ "ORDER BY [timestamp] \n"
		+ "";
		

	public static final String XEvent_Shredder_sql = ""
		+ "--------------------------------------------------------------------- \n"
		+ "-- The below SQL Code I used (with some small modifications) from: \n"
		+ "-- https://github.com/BeginTry/XEvent-Shredder \n"
		+ "--------------------------------------------------------------------- \n"
		+ "-- to include the XML source for each record, uncomment ',event_data' near the end of the SQL Satement \n"
		+ "--------------------------------------------------------------------- \n"
		+ " \n"
		+ "DECLARE @XESessionName SYSNAME = '<XE-SESSION-NAME>'; \n"
		+ "DECLARE @Tsql NVARCHAR(MAX) = ''; \n"
		+ " \n"
		+ "DECLARE @XE_TSQL_TypeXref TABLE \n"
		+ "( \n"
		+ "    XE_type              nvarchar(256), \n"
		+ "    XE_type_description  nvarchar(3072), \n"
		+ "    capabilities         int, \n"
		+ "    capabilities_desc    nvarchar(256), \n"
		+ "    XE_type_size         int, \n"
		+ "    SqlDataType          varchar(16) \n"
		+ "); \n"
		+ " \n"
		+ "INSERT INTO @XE_TSQL_TypeXref \n"
		+ "SELECT \n"
		+ "    o.name XE_type, o.description XE_type_description, \n"
		+ "    o.capabilities, o.capabilities_desc, o.type_size XE_type_size, \n"
		+ "    CASE type_name \n"
		+ "        --These mappings should be safe. \n"
		+ "        --They correspond almost directly to each other. \n"
		+ "        WHEN 'ansi_string'        THEN 'VARCHAR(MAX)' \n"
		+ "        WHEN 'binary_data'        THEN 'VARBINARY(MAX)' \n"
		+ "        WHEN 'boolean'            THEN 'BIT' \n"
		+ "        WHEN 'char'               THEN 'VARCHAR(MAX)' \n"
		+ "        WHEN 'guid'               THEN 'UNIQUEIDENTIFIER' \n"
		+ "        WHEN 'int16'              THEN 'SMALLINT' \n"
		+ "        WHEN 'int32'              THEN 'INT' \n"
		+ "        WHEN 'int64'              THEN 'BIGINT' \n"
		+ "        WHEN 'int8'               THEN 'SMALLINT' \n"
		+ "        WHEN 'uint16'             THEN 'INT' \n"
		+ "        WHEN 'uint32'             THEN 'BIGINT' \n"
		+ "        WHEN 'uint64'             THEN 'BIGINT'	--possible overflow? \n"
		+ "        WHEN 'uint8'              THEN 'SMALLINT' \n"
		+ "        WHEN 'unicode_string'     THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'xml'                THEN 'XML' \n"
		+ "\n"
		+ "        --These mappings are based off of descriptions and type_size. \n"
		+ "        WHEN 'cpu_cycle'          THEN 'BIGINT' \n"
		+ "        WHEN 'filetime'           THEN 'BIGINT' \n"
		+ "        WHEN 'wchar'              THEN 'NVARCHAR(2)' \n"
		+ "\n"
		+ "        --How many places of precision? \n"
		+ "        WHEN 'float32'            THEN 'NUMERIC(30, 4)' \n"
		+ "        WHEN 'float64'            THEN 'NUMERIC(30, 4)' \n"
		+ "\n"
		+ "        --These mappings? No clue. Default to NVARCHAR(MAX). \n"
		+ "        WHEN 'activity_id'        THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'activity_id_xfer'   THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'ansi_string_ptr'    THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'callstack'          THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'guid_ptr'           THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'null'               THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'ptr'                THEN 'NVARCHAR(MAX)' \n"
		+ "        WHEN 'unicode_string_ptr' THEN 'NVARCHAR(MAX)' \n"
		+ "    END AS SqlDataType \n"
		+ "FROM sys.dm_xe_objects o \n"
		+ "WHERE o.object_type = 'type'; \n"
		+ " \n"
		+ " \n"
		+ "WITH AllSessionEventFields AS \n"
		+ "( \n"
		+ "    --Unique Global Fields (Actions) across all events for the session. \n"
		+ "    SELECT DISTINCT \n"
		+ "        sa.name EventField, \n"
		+ "        'action' AS XmlNodeName, \n"
		+ "        CASE WHEN x.SqlDataType IS NULL THEN 'text' ELSE 'value' END AS XmlSubNodeName, \n"
		+ "        'Global Fields (Action)' AS FieldType, \n"
		+ "        o.type_name XE_type, \n"
		+ "        COALESCE(x.SqlDataType, 'NVARCHAR(MAX)') AS SqlDataType \n"
		+ "    FROM sys.server_event_sessions s \n"
		+ "    JOIN sys.server_event_session_events   se ON se.event_session_id = s.event_session_id \n"
		+ "    JOIN sys.server_event_session_actions  sa ON sa.event_session_id = s.event_session_id AND sa.event_id = se.event_id \n"
		+ "    JOIN sys.dm_xe_objects                 o  ON o.name = sa.name AND o.object_type = 'action' \n"
		+ "    LEFT JOIN @XE_TSQL_TypeXref            x  ON x.XE_type = o.type_name \n"
		+ "    WHERE s.name = @XESessionName \n"
		+ " \n"
		+ "    UNION \n"
		+ " \n"
		+ "    --Unique Event Fields across all events for the session. \n"
		+ "    SELECT DISTINCT \n"
		+ "        c.name EventField, \n"
		+ "        'data' AS XmlNodeName, \n"
		+ "        CASE WHEN x.SqlDataType IS NULL THEN 'text' ELSE 'value' END AS XmlSubNodeName, \n"
		+ "        'Event Fields' AS FieldType, \n"
		+ "        c.type_name XE_type, \n"
		+ "        COALESCE(x.SqlDataType, 'NVARCHAR(MAX)') AS SqlDataType \n"
		+ "    FROM sys.server_event_sessions s \n"
		+ "    JOIN sys.server_event_session_events  se ON se.event_session_id = s.event_session_id \n"
		+ "    JOIN sys.dm_xe_object_columns         c  ON c.object_name = se.name AND c.column_type = 'data' \n"
		+ "    LEFT JOIN @XE_TSQL_TypeXref           x  ON x.XE_type = c.type_name \n"
		+ "    WHERE s.name = @XESessionName \n"
		+ ") \n"
		+ "  \n"
		+ "SELECT @Tsql = @Tsql + CHAR(9) + \n"
		+ "    CASE \n"
		+ "        WHEN f.SqlDataType = 'XML' \n"
		+ "        THEN 'event_data.query (''(/event/' + f.XmlNodeName + '[@name=''''' + f.EventField + ''''']/' + \n"
		+ "            f.XmlSubNodeName + \n"
		+ "\n"
		+ "            --The XML chunk is wrapped in a [f.XmlSubNodeName] root node. Clicking on the XML \n"
		+ "            --in SSMS pops open a new tab showing the XML data. That's normal behavior. \n"
		+ "            --For event field \"showplan_xml\", we want SSMS to pop open a new tab showing the Execution Plan. \n"
		+ "            --To do that, we need to exclude the \"value\" root node. We'll use an XML wildcard here. \n"
		+ "            CASE WHEN f.EventField = 'showplan_xml' THEN '/*' ELSE '' END + \n"
		+ "\n"
		+ "            ')[1]'') AS [' + f.EventField + '],' + CHAR(13) + CHAR(10) \n"
		+ "        ELSE \n"
		+ "            'event_data.value (''(/event/' + f.XmlNodeName + '[@name=''''' + f.EventField + ''''']/' + \n"
		+ "            f.XmlSubNodeName + ')[1]'', ''' + f.SqlDataType + ''') AS [' + f.EventField + '],' + CHAR(13) + CHAR(10) \n"
		+ "    END \n"
		+ "FROM AllSessionEventFields f \n"
		+ "ORDER BY f.EventField \n"
		+ " \n"
		+ "SELECT @Tsql = LEFT(@Tsql, LEN(@Tsql) - 3); \n"
		+ "SELECT @Tsql = 'WITH XEvents AS \n"
		+ "( \n"
		+ "    SELECT object_name, CAST(event_data AS XML) AS event_data \n"
		+ "    FROM sys.fn_xe_file_target_read_file ( ''' + @XESessionName + '*.xel'', NULL, NULL, NULL ) \n"
		+ ") \n"
		+ "SELECT object_name,' + CHAR(13) + CHAR(10) + ' \n"
		+ "    event_data.value (''(/event/@timestamp)[1]'', ''DATETIME'') AS [timestamp],' + CHAR(13) + CHAR(10) + @Tsql + ' \n"
		+ "--   ,event_data \n"
		+ "FROM XEvents \n"
		+ "ORDER BY [timestamp];'; \n"
		+ " \n"
		+ "EXEC(@Tsql); \n"
		+ " \n"
		+ "select @Tsql; \n"
		+ "";
	
}
