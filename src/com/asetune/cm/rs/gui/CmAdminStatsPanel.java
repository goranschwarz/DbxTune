package com.asetune.cm.rs.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.CmAdminStats;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmAdminStatsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmAdminStatsPanel.class);
	private static final long    serialVersionUID      = 1L;

	JCheckBox _sample_resetAfter_chk;

	public static final String  TOOLTIP_sample_resetAfter = 
		"<html>Clear counters after we have sampled data from RepServer.<br>" +
		   "<b>Executes</b>: admin statistics, 'RESET'" +
		"</html>";

	public CmAdminStatsPanel(CountersModel cm)
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
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		_sample_resetAfter_chk     = new JCheckBox("Clear Counters",  conf == null ? CmAdminStats.DEFAULT_sample_resetAfter  : conf.getBooleanProperty(CmAdminStats.PROPKEY_sample_resetAfter,  CmAdminStats.DEFAULT_sample_resetAfter));

		_sample_resetAfter_chk.setName(CmAdminStats.PROPKEY_sample_resetAfter);
		_sample_resetAfter_chk.setToolTipText(TOOLTIP_sample_resetAfter);
		panel.add(_sample_resetAfter_chk, "wrap");

		_sample_resetAfter_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmAdminStats.PROPKEY_sample_resetAfter, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}
}
