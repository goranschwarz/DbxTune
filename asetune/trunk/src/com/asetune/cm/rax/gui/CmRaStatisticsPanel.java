package com.asetune.cm.rax.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rax.CmRaStatistics;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmRaStatisticsPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmRaStatisticsPanel.class);
	private static final long    serialVersionUID      = 1L;

	JCheckBox _sample_resetAfter_chk;
	JButton   _resetNow_but;

	public static final String  TOOLTIP_sample_resetAfter = 
		"<html>Clear counters after we have sampled data from RepServer.<br>" +
		   "<b>Executes</b>: ra_statistics reset" +
		"</html>";

	public CmRaStatisticsPanel(CountersModel cm)
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
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();

		_sample_resetAfter_chk = new JCheckBox("Clear Counters",  conf == null ? CmRaStatistics.DEFAULT_sample_resetAfter  : conf.getBooleanProperty(CmRaStatistics.PROPKEY_sample_resetAfter,  CmRaStatistics.DEFAULT_sample_resetAfter));
		_resetNow_but          = new JButton("Clear Counters NOW");

		_sample_resetAfter_chk.setName(CmRaStatistics.PROPKEY_sample_resetAfter);
		_sample_resetAfter_chk.setToolTipText(TOOLTIP_sample_resetAfter);

		panel.add(_sample_resetAfter_chk, "wrap");
		panel.add(_resetNow_but,          "wrap");

		_sample_resetAfter_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmRaStatistics.PROPKEY_sample_resetAfter, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		_resetNow_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Connection conn = getConnection();
				if (conn != null)
				{
					try
					{
						Statement stmnt = conn.createStatement();
						stmnt.executeUpdate("ra_statistics reset");
						stmnt.close();
					}
					catch(SQLException ex)
					{
						_logger.warn("Problems executing 'ra_statistics reset'. Caught: "+ex);
					}
				}
			}
		});
		
		return panel;
	}
}
