package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmSpinlockSum;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSpinlockSumPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmSpinlockSumPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSpinlockSum.CM_NAME;

	JCheckBox _sampleSystemThreads_chk;
	JCheckBox _sampleField_fglockspins_chk;

	public static final String  TOOLTIP_sample_resetAfter = 
		"<html>Clear counters after we have sampled data from master..syscounters.<br>" +
		   "<b>Note</b>: If sp_sysmon is executed at the same time, this will probably mess up Spinlock Contention Calculations for sp_sysmon. <br>" +
		   "<b>Executes</b>: DBCC monitor('clear', 'spinlock_s', 'on')." +
		"</html>";

	public static final String  TOOLTIP_sample_fglockspins = 
		"<html>Include or Exclude rows with fields_name 'fglockspins' in the calculation.<br>" +
		   "<b>Note 1</b>: In ASE Cluster Edition there are <b>a lot</b> of those fields, and if we do average calculations over thousands of records, the contention on individual instances will not show anyway...<br>" +
		   "<b>Note 2</b>: The field 'fglockspins' has to do with lock promotion (pageLock to tableLock, or rowLock to tableLock) if I'm correct...<br>" +
		   "<b>Note 3</b>: Test the difference in performance when Include or Exclude this specific counter.<br>" +
		   "<b>Note 4</b>: This option is <b>only</b> applicable if you are connected to a ASE Cluster Edition.<br>" +
		"</html>";


	public CmSpinlockSumPanel(CountersModel cm)
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
		_sampleSystemThreads_chk     = new JCheckBox("Clear Counters",              conf == null ? CmSpinlockSum.DEFAULT_sample_resetAfter  : conf.getBooleanProperty(CmSpinlockSum.PROPKEY_sample_resetAfter,  CmSpinlockSum.DEFAULT_sample_resetAfter));
		_sampleField_fglockspins_chk = new JCheckBox("Include Field 'fglockspins'", conf == null ? CmSpinlockSum.DEFAULT_sample_fglockspins : conf.getBooleanProperty(CmSpinlockSum.PROPKEY_sample_fglockspins, CmSpinlockSum.DEFAULT_sample_fglockspins));

		_sampleSystemThreads_chk.setName(CmSpinlockSum.PROPKEY_sample_resetAfter);
		_sampleSystemThreads_chk.setToolTipText(TOOLTIP_sample_resetAfter);
		panel.add(_sampleSystemThreads_chk, "wrap");

		_sampleField_fglockspins_chk.setName(CmSpinlockSum.PROPKEY_sample_fglockspins);
		_sampleField_fglockspins_chk.setToolTipText(TOOLTIP_sample_fglockspins);
		panel.add(_sampleField_fglockspins_chk, "wrap, hidemode 3");

		_sampleSystemThreads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpinlockSum.PROPKEY_sample_resetAfter, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		_sampleField_fglockspins_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmSpinlockSum.PROPKEY_sample_fglockspins, ((JCheckBox)e.getSource()).isSelected());
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
				boolean visible = cm.isClusterEnabled();
				_sampleField_fglockspins_chk.setVisible(visible);

			} // end isRuntimeInitialized

		} // end (cm != null)
	}
}
