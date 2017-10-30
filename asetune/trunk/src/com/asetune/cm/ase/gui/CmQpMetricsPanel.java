package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmQpMetrics;
import com.asetune.config.ui.AseConfigMonitoringDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GLabel;
import com.asetune.gui.swing.GTextField;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmQpMetricsPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmQpMetricsPanel.class);
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

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("QP Metrics", true);
		panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"Use this panel to control the Query Plan Metrics subsystem.<br>" +
			"</html>");

		String filterStr = Configuration.getCombinedConfiguration().getProperty(CmQpMetrics.PROPKEY_sample_filter, CmQpMetrics.DEFAULT_sample_filter);
		
		final JButton    drop_but   = new JButton("Reset QP Metrics");
		final JButton    flush_but  = new JButton("Flush QP Metrics Now");
		final JCheckBox  flush_chk  = new JCheckBox("Flush QP Metrics on every refresh", CmQpMetrics.DEFAULT_onSample_flush);
		final GLabel     filter_lbl = new GLabel("Filter:");
		final GTextField filter_txt = new GTextField(filterStr);
		final JButton    aseCfg_but = new JButton("Server Config");

		drop_but   .setToolTipText("<html>This will drop/purge all sampled Query Plan Metrics in all databases<br>execute <code>sp_metrics 'drop', '1'</code> in every database</html>");
		flush_but  .setToolTipText("<html>This will flush all 'in-memory' Query Plan Metrics <br>execute <code>sp_metrics 'flush'</code></html>");
		flush_chk  .setToolTipText("<html>Before we get data, on every sample. Flush all 'in-memory' Query Plan Metrics<br>execute <code>sp_metrics 'flush'</code></html>");
		filter_lbl.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10</code><br><br><b>Note:</b> Press &lt;ENTER&gt; in the filter textfield to apply this filter <br><b>Tip:</b> To check what filter is active check the SQL executed: Right click on the 'QP Metrics' tab, and choose 'Properties...'.</html>");
		filter_txt.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10</code><br><br><b>Note:</b> Press &lt;ENTER&gt; in the filter textfield to apply this filter <br><b>Tip:</b> To check what filter is active check the SQL executed: Right click on the 'QP Metrics' tab, and choose 'Properties...'.</html>");
		aseCfg_but.setToolTipText("<html>Set filter in the ASE Server...<br>This means the ASE does <b>not</b> store/writes unnececary statemenst <br>to the system tables, avoiding excessive catalog writes for simple queries.<br><br>Simply open the Ase Configure dialog.</html>");

		panel.add( drop_but,   "wrap");
		
		panel.add( flush_chk,  "split");
		panel.add( flush_but,  "wrap 5");
		
		panel.add( filter_lbl, "split");
		panel.add( filter_txt, "growx, pushx, wrap 5");
		
		panel.add( aseCfg_but, "wrap");

		
		Configuration conf = Configuration.getCombinedConfiguration();
		flush_chk.setSelected(conf.getBooleanProperty(CmQpMetrics.PROPKEY_onSample_flush, CmQpMetrics.DEFAULT_onSample_flush));

		drop_but.addActionListener(new ActionListener()
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

		flush_but.addActionListener(new ActionListener()
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

		flush_chk.addActionListener(new ActionListener()
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

		filter_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;

				String str = filter_txt.getText().trim();

				conf.setProperty(CmQpMetrics.PROPKEY_sample_filter, str);
				conf.save();
				getCm().setSql(null); // Causes SQL Statement to be recreated
			}
		});

		aseCfg_but.addActionListener(new ActionListener()
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
