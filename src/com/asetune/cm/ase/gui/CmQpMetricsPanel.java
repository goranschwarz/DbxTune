package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.AseConfigMonitoringDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmQpMetricsPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmQpMetricsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmQpMetrics.CM_NAME;

	public CmQpMetricsPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

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

		String filterStr = "lio_avg > 100 and elap_max > 10";
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
			filterStr = tmpConf.getProperty(getName()+".show.filter", "");

		if ( ! filterStr.trim().equals("") )
			getCm().setSqlWhere("'show', '"+filterStr+"'");
		
		final JButton    drop_but   = new JButton("Reset QP Metrics");
		final JLabel     filter_lbl = new JLabel("Filter:");
		final JTextField filter_txt = new JTextField(filterStr);
		final JButton    aseCfg_but = new JButton("Server Config");

		drop_but   .setToolTipText("<html>This will drop/purge all sampled Query Plan Metrics in all databases<br>execute <code>sp_metrics 'drop', '1'</code> in every database</html>");
		filter_lbl.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10<code> </html>");
		filter_txt.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10<code> </html>");
		aseCfg_but.setToolTipText("<html>Set filter in the ASE Server...<br>This means the ASE does <b>not</b> store/writes unnececary statemenst <br>to the system tables, avoiding excessive catalog writes for simple queries.<br><br>Simply open the Ase Configure dialog.</html>");

		panel.add( drop_but,   "wrap 10");
		panel.add( filter_lbl, "split");
		panel.add( filter_txt, "growx, pushx, wrap 10");
		panel.add( aseCfg_but, "wrap");

		drop_but.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String sql = "sp_asetune_qp_metrics 'drop'";
				if ( ! getCm().getCounterController().isMonConnected() )
				{
					SwingUtils.showInfoMessage("Not Connected", "Sorry not connected to the database.");
					return;
				}
				
				try
				{
					Statement stmnt = getCm().getCounterController().getMonConnection().createStatement();
					stmnt.executeUpdate(sql);
					stmnt.close();
				}
				catch (SQLException ex)
				{
					SwingUtils.showErrorMessage("Problems", "Problems when dropping/purging the Query Plab Metrics", ex);
					_logger.warn("Problems execute SQL '"+sql+"', Caught: " + e.toString() );
				}
			}
		});

		filter_txt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String filterStr = filter_txt.getText().trim();
				getCm().setSqlWhere("'show', '"+filterStr+"'");
				
				// Save config...
				Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tmpConf != null)
				{
					tmpConf.setProperty(getName()+".show.filter", filterStr);
					tmpConf.save();
				}
			}
		});

		aseCfg_but.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				AseConfigMonitoringDialog.showDialog(MainFrame.getInstance(), getCm().getCounterController().getMonConnection(), -1);
			}
		});

		return panel;
	}
}
