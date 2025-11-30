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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.CmQpMetrics;
import com.dbxtune.config.ui.AseConfigMonitoringDialog;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.gui.swing.GTextField;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.gui.swing.WaitForExecDialog.BgExecutor;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmQpMetricsPanel
extends TabularCntrPanel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmQpMetrics.CM_NAME;

	public CmQpMetricsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}

	private JButton    l_drop_but;
	private JButton    l_flush_but;
	private JCheckBox  l_flush_chk;
	private GLabel     l_filter_lbl;
	private GTextField l_filter_txt;
	private JButton    l_aseCfg_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("QP Metrics", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				l_flush_chk.setSelected(conf.getBooleanProperty(CmQpMetrics.PROPKEY_onSample_flush, CmQpMetrics.DEFAULT_onSample_flush));
				l_filter_txt.setText(""+conf.getProperty       (CmQpMetrics.PROPKEY_sample_filter , CmQpMetrics.DEFAULT_sample_filter));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("QP Metrics", true);
		panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"Use this panel to control the Query Plan Metrics subsystem.<br>" +
			"</html>");

		String filterStr = Configuration.getCombinedConfiguration().getProperty(CmQpMetrics.PROPKEY_sample_filter, CmQpMetrics.DEFAULT_sample_filter);
		
		l_drop_but   = new JButton("Reset QP Metrics");
		l_flush_but  = new JButton("Flush QP Metrics Now");
		l_flush_chk  = new JCheckBox("Flush QP Metrics on every refresh", CmQpMetrics.DEFAULT_onSample_flush);
		l_filter_lbl = new GLabel("Filter:");
		l_filter_txt = new GTextField(filterStr);
		l_aseCfg_but = new JButton("Server Config");

		l_drop_but   .setToolTipText("<html>This will drop/purge all sampled Query Plan Metrics in all databases<br>execute <code>sp_metrics 'drop', '1'</code> in every database</html>");
		l_flush_but  .setToolTipText("<html>This will flush all 'in-memory' Query Plan Metrics <br>execute <code>sp_metrics 'flush'</code></html>");
		l_flush_chk  .setToolTipText("<html>Before we get data, on every sample. Flush all 'in-memory' Query Plan Metrics<br>execute <code>sp_metrics 'flush'</code></html>");
		l_filter_lbl.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10</code><br><br><b>Note:</b> Press &lt;ENTER&gt; in the filter textfield to apply this filter <br><b>Tip:</b> To check what filter is active check the SQL executed: Right click on the 'QP Metrics' tab, and choose 'Properties...'.</html>");
		l_filter_txt.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10</code><br><br><b>Note:</b> Press &lt;ENTER&gt; in the filter textfield to apply this filter <br><b>Tip:</b> To check what filter is active check the SQL executed: Right click on the 'QP Metrics' tab, and choose 'Properties...'.</html>");
		l_aseCfg_but.setToolTipText("<html>Set filter in the ASE Server...<br>This means the ASE does <b>not</b> store/writes unnececary statemenst <br>to the system tables, avoiding excessive catalog writes for simple queries.<br><br>Simply open the Ase Configure dialog.</html>");

		panel.add( l_drop_but,   "wrap");
		
		panel.add( l_flush_chk,  "split");
		panel.add( l_flush_but,  "wrap 5");
		
		panel.add( l_filter_lbl, "split");
		panel.add( l_filter_txt, "growx, pushx, wrap 5");
		
		panel.add( l_aseCfg_but, "wrap");

		
		Configuration conf = Configuration.getCombinedConfiguration();
		l_flush_chk.setSelected(conf.getBooleanProperty(CmQpMetrics.PROPKEY_onSample_flush, CmQpMetrics.DEFAULT_onSample_flush));

		l_drop_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( ! getCm().isConnected() )
				{
					SwingUtils.showInfoMessage("Not Connected", "Sorry not connected to the database.");
					return;
				}

				WaitForExecDialog wait = new WaitForExecDialog(SwingUtilities.getWindowAncestor(CmQpMetricsPanel.this), "Dropping QP Metrics");

				// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
				BgExecutor bgExec = new BgExecutor(wait)
				{
					@Override
					public Object doWork()
					{
						String sql = "exec sp_asetune_qp_metrics 'drop'";

						getWaitDialog().setState(sql);

						try
						{
							Statement stmnt = getCm().getCounterController().getMonConnection().createStatement();
							stmnt.executeUpdate(sql);
							stmnt.close();
						}
						catch (SQLException ex)
						{
							SwingUtils.showErrorMessage("Problems", "Problems when dropping/purging the Query Plan Metrics", ex);
							_logger.warn("Problems execute SQL '"+sql+"', Caught: " + ex.toString() );
						}

						getWaitDialog().setState("Done");

						return null;
					}
				};
				wait.execAndWait(bgExec);
			
			}
		});

		l_flush_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( ! getCm().isConnected() )
				{
					SwingUtils.showInfoMessage("Not Connected", "Sorry not connected to the database.");
					return;
				}
				
				WaitForExecDialog wait = new WaitForExecDialog(SwingUtilities.getWindowAncestor(CmQpMetricsPanel.this), "Flushing QP Metrics");

				// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
				BgExecutor bgExec = new BgExecutor(wait)
				{
					@Override
					public Object doWork()
					{
						String sql = "exec sp_asetune_qp_metrics 'flush'";

						getWaitDialog().setState(sql);

						try
						{
							Statement stmnt = getCm().getCounterController().getMonConnection().createStatement();
							stmnt.executeUpdate(sql);
							stmnt.close();
						}
						catch (SQLException ex)
						{
							SwingUtils.showErrorMessage("Problems", "Problems when flushing the Query Plan Metrics", ex);
							_logger.warn("Problems execute SQL '"+sql+"', Caught: " + ex.toString() );
						}

						getWaitDialog().setState("Done");

						return null;
					}
				};
				wait.execAndWait(bgExec);
			}
		});

		l_flush_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;
				
				boolean b = ((JCheckBox)e.getSource()).isSelected();

				conf.setProperty(CmQpMetrics.PROPKEY_onSample_flush, b);
				conf.save();
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});

		l_filter_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;

				String str = l_filter_txt.getText().trim();

				conf.setProperty(CmQpMetrics.PROPKEY_sample_filter, str);
				conf.save();
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});

		l_aseCfg_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				AseConfigMonitoringDialog.showDialog(MainFrame.getInstance(), getCm().getCounterController().getMonConnection(), -1, true);
			}
		});

		return panel;
	}
}
