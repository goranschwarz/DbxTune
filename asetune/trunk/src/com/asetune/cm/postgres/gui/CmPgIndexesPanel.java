package com.asetune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.CmPgIndexes;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgIndexesPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgIndexesPanel.class);
	private static final long    serialVersionUID      = 1L;

	private JCheckBox l_sampleSystemTables_chk;

	public CmPgIndexesPanel(CountersModel cm)
	{
		super(cm);

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
		boolean defaultOpt;
//		int     defaultIntOpt;

		//-----------------------------------------
		// sample system tables:
		//-----------------------------------------
		defaultOpt = conf == null ? CmPgIndexes.DEFAULT_sample_systemTables : conf.getBooleanProperty(CmPgIndexes.PROPKEY_sample_systemTables, CmPgIndexes.DEFAULT_sample_systemTables);
		l_sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultOpt);

		l_sampleSystemTables_chk.setName(CmPgIndexes.PROPKEY_sample_systemTables);
		l_sampleSystemTables_chk.setToolTipText("<html>" +
				"Include system tables in the output<br>" +
				"</html>");

		l_sampleSystemTables_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmPgIndexes.PROPKEY_sample_systemTables, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		// LAYOUT
		panel.add(l_sampleSystemTables_chk, "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean confProp = conf.getBooleanProperty(CmPgIndexes.PROPKEY_sample_systemTables, CmPgIndexes.DEFAULT_sample_systemTables);
		boolean guiProp  = l_sampleSystemTables_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSystemTables_chk.setSelected(confProp);
	}
}
