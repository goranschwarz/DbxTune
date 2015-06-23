package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmDeviceIo;
import com.asetune.cm.ase.CmEngines;
import com.asetune.cm.ase.CmIoControllers;
import com.asetune.cm.ase.CmThreads;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.swing.GCheckBox;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

public class CmEnginesPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmEnginesPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmEngines.CM_NAME;

	private GCheckBox _collapse_IoCpuTime_to_IdleCpuTime_chk;

	public static final String  TOOLTIP_collapse_IoCpuTime_to_IdleCpuTime = 
		"<html>"
		   + "This option is <b>only</b> available in ASE 15.7 or higer and you are running in <i><b>threaded</b> kernel mode</i>.<br>"
		   + "<ul>"
		   + "   <li>When ASE is runing in <i><b>threaded</b> kernel mode</i> IO operations are processed by the IO Thread (multiple ones can be configured).<br>"
		   + "       In this case discarding <code>IOCPUTime</code> would make sense... (or actually Collapse <code>IOCPUTime</code> into <code>IdleCPUTime</code>) <br>"
		   + "       To view how efficient the IO Thread is, you can look in Performance Counter '"+CmIoControllers.SHORT_NAME+"', '"+CmThreads.SHORT_NAME+"(ThreadType=DiskController, columns=*Ticks)' or '"+CmDeviceIo.SHORT_NAME+"'.</li>"
		   + "   <li>When ASE is runing in <i><b>process</b> kernel mode</i>, or in releases earlier than 15.7, IO operations are processed by the <code>engine</code> which issued the IO Operation.<br>"
		   + "       Then the column <code>IOCPUTime</code> is vital information for checking if a specific engine is doing IO's</li>"
		   + "</ul>"
		   + "This option simply <i>moves</i> the <code>IOCPUTime</code> column into the <code>IdleCPUTime</code>, and also sets <code>IOCPUTime</code> to <code>-1</code>.<br>"
		   + "<br>"
		   + "<br>"
		   + "<h3>Below is description of ASE ChangeRequest CR# 757246 </h3>"
		   + "<pre>"
		   + "Lot of customer is logging cases and say sp_sysmon show high IO Busy or some of the tools seem \n"
		   + "to sum up USER, SYSTEM, IO BUSY as Engine busy which get very wrong. \n"
		   + "\n"
		   + "This is clear from the manual how this work so this is not a documentation bug but it is very confusing to use \n"
		   + "word Busy for a Engine that is idling.  \n"
		   + "http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc00842.1570/html/spsysmon/X68954.htm\n"
		   + "\n"
		   + "When sp_sysmon samples the counters (by default, every 100 milliseconds), each engine indicates what it is currently doing. \n"
		   + "sp_sysmon reports how busy engines are, distinguishing between user and system tasks: \n"
		   + "  * 'User Busy' – user tasks, for example, user connections.\n"
		   + "  * 'System Busy' – internal tasks, for example, the housekeeper.\n"
		   + " \n"
		   + "For example, if executing a task, the engine reports 'CPU Busy'; or, if idling, the engine reports 'Idle'. \n"
		   + "Engines are counted as 'I/O Busy' if Adaptive Server has any I/O outstanding and the engine is idle. \n"
		   + "If there is one I/O outstanding and three engines sitting idle, each engine is counted as 'I/O Busy'. <-----\n"
		   + "\n"
		   + "=============================================================================== \n"
		   + "      Sybase Adaptive Server Enterprise System Performance Report\n"
		   + "=============================================================================== \n"
		   + " \n"
		   + "Server Version:        Adaptive Server Enterprise/15.7/EBF 21708 SMP SP110 /P/x \n"
		   + "Run Date:              Jan 27, 2014                                             \n"
		   + "Sampling Started at:   Jan 27, 2014 04:15:40                                    \n"
		   + "Sampling Ended at:     Jan 27, 2014 04:16:00                                    \n"
		   + "Sample Interval:       00:00:20                                                 \n"
		   + "Sample Mode:           Reset Counters                                           \n"
		   + "Server Name:           ASE157                                                   \n"
		   + "=============================================================================== \n"
		   + " \n"
		   + "Kernel Utilization\n"
		   + "------------------\n"
		   + " \n"
		   + " \n"
		   + "  Engine Utilization (Tick %)   User Busy   System Busy    I/O Busy        Idle\n"
		   + "  -------------------------  ------------  ------------  ----------  ---------- \n"
		   + "  ThreadPool : syb_default_pool                                                 \n"
		   + "   Engine 0                         0.0 %         0.0 %      93.0 %       7.0 % \n"
		   + "   Engine 1                         1.0 %         0.0 %      92.0 %       7.0 % \n"
		   + "   Engine 2                         3.0 %         1.5 %      89.0 %       6.5 % \n"
		   + "   Engine 3                         7.0 %         2.5 %      86.0 %       4.5 % \n"
		   + "   Engine 4                         0.0 %         0.0 %      93.5 %       6.5 % \n"
		   + "   Engine 5                         0.0 %         0.0 %      94.0 %       6.0 % \n"
		   + "  -------------------------  ------------  ------------  ----------  ---------- \n"
		   + "  Pool Summary        Total        11.0 %         4.0 %     547.5 %      37.5 % \n"
		   + "                    Average         1.8 %         0.7 %      91.3 %       6.3 % \n"
		   + " \n"
		   + "  -------------------------  ------------  ------------  ----------  ---------- \n"
		   + "  Server Summary      Total        11.0 %         4.0 %     547.5 %      37.5 % \n"
		   + "                    Average         1.8 %         0.7 %      91.3 %       6.3 % \n"
		   + "\n"
		   + "					Disk I/O Management\n"
		   + "-------------------\n"
		   + " \n"
		   + "Max Outstanding I/Os            per sec      per xact       count  % of total\n"
		   + "-------------------------  ------------  ------------  ----------  ---------- \n"
		   + "    Server                            n/a           n/a         171       n/a   \n"
		   + "    Engine 0                          n/a           n/a           0       n/a   \n"
		   + "    Engine 1                          n/a           n/a         112       n/a   \n"
		   + "    Engine 2                          n/a           n/a         111       n/a   \n"
		   + "    Engine 3                          n/a           n/a         171       n/a   \n"
		   + "    Engine 4                          n/a           n/a          96       n/a   \n"
		   + "    Engine 5                          n/a           n/a           0       n/a   \n"
		   + " \n"
		   + " \n"
		   + "Seem odd to customer that an Engine 0 and 5 can be as sample 93.0 % IO Busy and have 0 Max Outstanding I/Os \n" 
		   + "you would have expect a higher value this is just the extreme show case.\n"
		   + "\n"
		   + "This request is to remove 'I/O Busy' for normal thread mode when it confuse people. \n"
		   + "</pre>"
		+ "</html>";

	public CmEnginesPanel(CountersModel cm)
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
		_collapse_IoCpuTime_to_IdleCpuTime_chk = new GCheckBox("Collapse IOCPUTime into IdleCPUTime", conf == null ? CmEngines.DEFAULT_collapse_IoCpuTime_to_IdleCpuTime  : conf.getBooleanProperty(CmEngines.PROPKEY_collapse_IoCpuTime_to_IdleCpuTime,  CmEngines.DEFAULT_collapse_IoCpuTime_to_IdleCpuTime));

		_collapse_IoCpuTime_to_IdleCpuTime_chk.setName(CmEngines.PROPKEY_collapse_IoCpuTime_to_IdleCpuTime);
		_collapse_IoCpuTime_to_IdleCpuTime_chk.setToolTipText(TOOLTIP_collapse_IoCpuTime_to_IdleCpuTime);
		panel.add(_collapse_IoCpuTime_to_IdleCpuTime_chk, "wrap");

		_collapse_IoCpuTime_to_IdleCpuTime_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmEngines.PROPKEY_collapse_IoCpuTime_to_IdleCpuTime, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
				
				// Reset attached graphs (this will reinitialize, graph series since they will change)
				for (TrendGraph tg : getCm().getTrendGraphs().values())
					tg.resetGraph();
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
//				boolean visible = cm.getServerVersion() >= 1570100;
				boolean visible = cm.getServerVersion() >= Ver.ver(15,7,0,100);

				_collapse_IoCpuTime_to_IdleCpuTime_chk.setVisible(visible);

				// If sp_configure 'kernel mode' IS NOT 'threaded'
				// Disable the option
				boolean inThreadedMode = ((CmEngines)cm).inThreadedMode();
				if ( ! inThreadedMode)
					_collapse_IoCpuTime_to_IdleCpuTime_chk.setEnabled(false);

				//System.out.println("checkLocalComponents(): inThreadedMode="+inThreadedMode);

			} // end isRuntimeInitialized

		} // end (cm != null)
	}
}
