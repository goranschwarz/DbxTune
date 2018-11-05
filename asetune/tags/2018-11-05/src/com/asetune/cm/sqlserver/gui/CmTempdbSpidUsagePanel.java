package com.asetune.cm.sqlserver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmTempdbSpidUsage;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmTempdbSpidUsagePanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmTempdbSpidUsagePanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_systemThreads = "<html>Sample System SPID's that executes in the SQL Server.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";

	public CmTempdbSpidUsagePanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;
//
//		// YELLOW = SYSTEM process
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String sid = (String) adapter.getValue(adapter.getColumnIndex("sid"));
//				if ("0x0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".equals(sid))
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));
	}


	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		JCheckBox sampleSystemThreads_chk = new JCheckBox("Show system processes", conf == null ? CmTempdbSpidUsage.DEFAULT_sample_systemThreads : conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads, CmTempdbSpidUsage.DEFAULT_sample_systemThreads));

		sampleSystemThreads_chk.setName(CmTempdbSpidUsage.PROPKEY_sample_systemThreads);
		sampleSystemThreads_chk.setToolTipText(TOOLTIP_sample_systemThreads);
		panel.add(sampleSystemThreads_chk, "wrap");

		sampleSystemThreads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}
}
