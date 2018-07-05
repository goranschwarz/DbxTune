package com.asetune.cm.ase.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sp_sysmon.SpSysmon;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.SwingUtils;

public class CmSysmonPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmSysmonPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSysmon.CM_NAME;

	public CmSysmonPanel(CountersModel cm)
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
	protected int getDefaultMainSplitPaneDividerLocation()
	{
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane == null)
			return 0;

		// Use 60% for table, 40% for graph;
		return 6 * (mainSplitPane.getSize().width/10);
	}

	@Override
	protected void updateExtendedInfoPanel()
	{
		JPanel  panel = getExtendedInfoPanel();
		if (panel == null) 
			return;

		// If the panel is so small, make it bigger 
		int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
		if (dividerLocation == 0)
		{
			JSplitPane mainSplitPane = getMainSplitPane();
			if (mainSplitPane != null)
				mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
		}
		
//		if ( ! isMonConnected() )
//			return;

		CountersModel cm = getDisplayCm();
		if (cm == null)
			cm = getCm();

//System.out.println(CM_NAME__SYSMON+" -- CM="+cm);
		if (cm == null)
			return;

//System.out.println(CM_NAME__SYSMON+" -- cm.hasDiffData()="+cm.hasDiffData());
		if ( ! cm.hasDiffData() )
			return;

		RTextArea textArea = (RTextArea)getClientProperty("textArea");
		if (textArea == null)
			return;

		int caretPosition = textArea.getCaretPosition();

		SpSysmon sysmon = new SpSysmon(cm);
		sysmon.calc();
//		sysmon.printReport();
		String report = sysmon.getReportText();

		textArea.setText(report);
		if (caretPosition > 0 && caretPosition < textArea.getDocument().getLength())
			textArea.setCaretPosition(caretPosition);
	}

	@Override
	protected JPanel createExtendedInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);

		panel.setLayout(new BorderLayout());
//		panel.add(new JScrollPane(createTreeSpSysmon()), BorderLayout.CENTER);

		RSyntaxTextArea textArea = new RSyntaxTextArea();
		RTextScrollPane textScroll = new RTextScrollPane(textArea, true);

		RSyntaxUtilitiesX.installRightClickMenuExtentions(textScroll, this);

		textArea.setText("empty sp_sysmon");
//		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
//		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
//		textArea.setCodeFoldingEnabled(true);

		putClientProperty("textArea",   textArea);
		putClientProperty("textScroll", textScroll);

		panel.add(textScroll, BorderLayout.CENTER);

//		panel.setPreferredSize(new Dimension(0, 0));
//		panel.setMinimumSize(new Dimension(0, 0));
//		panel.setSize(new Dimension(0, 500));
//		mainSplitPane.setDividerLocation(150);

		// Size of the panel
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane != null)
			mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());

		return panel;
	}

	@Override
	protected void setSplitPaneOptions(JSplitPane mainSplitPane, JPanel dataPanel, JPanel extendedInfoPanel)
	{
		mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setBorder(null);
		mainSplitPane.add(dataPanel,         JSplitPane.LEFT);
		mainSplitPane.add(extendedInfoPanel, JSplitPane.RIGHT);
		mainSplitPane.setDividerSize(5);

		mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
	}

//	private JTree createTreeSpSysmon()
//	{
//		DefaultMutableTreeNode top = new DefaultMutableTreeNode("sp_sysmon");
//		DefaultMutableTreeNode heading = new DefaultMutableTreeNode("");
//		DefaultMutableTreeNode subHead = new DefaultMutableTreeNode("");
//
//		heading = new DefaultMutableTreeNode("Kernel Utilization");
//		top.add(heading);
//		subHead = new DefaultMutableTreeNode("Config");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Runnable Process Search Count"));
//		subHead.add(new DefaultMutableTreeNode("I/O Polling Process Count"));
//
//		subHead = new DefaultMutableTreeNode("Engine Busy Utilization");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Engine 0"));
//
//		subHead = new DefaultMutableTreeNode("CPU Yields by Engine");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Engine 0"));
//
//		subHead = new DefaultMutableTreeNode("Network Checks");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Non-Blocking"));
//		subHead.add(new DefaultMutableTreeNode("Blocking"));
//		subHead.add(new DefaultMutableTreeNode("Total Network I/O Checks"));
//		subHead.add(new DefaultMutableTreeNode("Avg Net I/Os per Check"));
//
//		subHead = new DefaultMutableTreeNode("Disk I/O Checks");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Total Disk I/O Checks"));
//		subHead.add(new DefaultMutableTreeNode("Checks Returning I/O"));
//		subHead.add(new DefaultMutableTreeNode("Avg Disk I/Os Returned"));
//
//		heading = new DefaultMutableTreeNode("Worker Process Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Parallel Query Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Task Management");
//		top.add(heading);
//
//		subHead = new DefaultMutableTreeNode("Task Context Switches by Engine");
//		heading.add(subHead);
//		subHead.add(new DefaultMutableTreeNode("Engine 0"));
//
//		subHead = new DefaultMutableTreeNode("Task Context Switches Due To");
//		heading.add(subHead);
//
//		subHead.add(new DefaultMutableTreeNode("Voluntary Yields"));
//		subHead.add(new DefaultMutableTreeNode("Cache Search Misses"));
//		subHead.add(new DefaultMutableTreeNode("Exceeding I/O batch size"));
//		subHead.add(new DefaultMutableTreeNode("System Disk Writes"));
//		subHead.add(new DefaultMutableTreeNode("Logical Lock Contention"));
//		subHead.add(new DefaultMutableTreeNode("Address Lock Contention"));
//		subHead.add(new DefaultMutableTreeNode("Latch Contention"));
//		subHead.add(new DefaultMutableTreeNode("Log Semaphore Contention"));
//		subHead.add(new DefaultMutableTreeNode("PLC Lock Contention"));
//		subHead.add(new DefaultMutableTreeNode("Group Commit Sleeps"));
//		subHead.add(new DefaultMutableTreeNode("Last Log Page Writes"));
//		subHead.add(new DefaultMutableTreeNode("Modify Conflicts"));
//		subHead.add(new DefaultMutableTreeNode("I/O Device Contention"));
//		subHead.add(new DefaultMutableTreeNode("Network Packet Received"));
//		subHead.add(new DefaultMutableTreeNode("Network Packet Sent"));
//		subHead.add(new DefaultMutableTreeNode("Network services"));
//		subHead.add(new DefaultMutableTreeNode("Other Causes"));
//
//		heading = new DefaultMutableTreeNode("Application Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("ESP Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Transaction Profile");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Transaction Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Index Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Metadata Cache Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Lock Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Data Cache Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Procedure Cache Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Memory Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Recovery Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Disk I/O Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Network I/O Management");
//		top.add(heading);
//
//		heading = new DefaultMutableTreeNode("Replication Agent");
//		top.add(heading);
//
//		return new JTree(new DefaultTreeModel(top));
//	}
}