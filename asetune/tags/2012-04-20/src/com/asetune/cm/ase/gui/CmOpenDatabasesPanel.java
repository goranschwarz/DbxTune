package com.asetune.cm.ase.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.TextAnchor;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmOpenDatabases;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmOpenDatabasesPanel
extends TabularCntrPanel
{
	private static final Logger  _logger	           = Logger.getLogger(CmOpenDatabasesPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmOpenDatabases.CM_NAME;

	private static final String  PROPKEY_generateDummy = PROP_PREFIX + ".graph.generate.dummyWhenNotConnected";
	private static final boolean DEFAULT_generateDummy = false;

	private static final String  PROPKEY_plotOrientation          = PROP_PREFIX + ".graph.PlotOrientation";
	private static final String  VALUE_plotOrientation_AUTO       = "AUTO";
	private static final String  VALUE_plotOrientation_VERTICAL   = "VERTICAL";
	private static final String  VALUE_plotOrientation_HORIZONTAL = "HORIZONTAL";
	private static final String  DEFAULT_plotOrientation          = VALUE_plotOrientation_AUTO;


	public CmOpenDatabasesPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// BLUE = in 'DUMP DATABASE'
		if (conf != null) colorStr = conf.getProperty(getName()+".color.dumpdb");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number backupInProg = (Number) adapter.getValue(adapter.getColumnIndex("BackupInProgress"));
				return backupInProg != null && backupInProg.intValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.BLUE), null));

		// PINK = TRANSACTION LOG at 90%
		if (conf != null) colorStr = conf.getProperty(getName()+".color.almostFullTranslog");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number almostLogFull = (Number) adapter.getValue(adapter.getColumnIndex("LogSizeUsedPct"));
				return almostLogFull != null && almostLogFull.doubleValue() > 90.0;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = FULL TRANSACTION LOG
		if (conf != null) colorStr = conf.getProperty(getName()+".color.fullTranslog");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number isLogFull = (Number) adapter.getValue(adapter.getColumnIndex("TransactionLogFull"));
				return isLogFull != null && isLogFull.intValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	private CategoryDataset createDataset(GTable dataTable)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean generateDummy = conf.getBooleanProperty(PROPKEY_generateDummy, DEFAULT_generateDummy);

		DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

		if (dataTable != null)
		{
			int DBName_pos          = dataTable.findViewColumn("DBName");
			int LogSizeFreeInMb_pos = dataTable.findViewColumn("LogSizeFreeInMb"); // numeric(10,1)
			int LogSizeUsedPct_pos  = dataTable.findViewColumn("LogSizeUsedPct");  // numeric(10,1)

			CountersModel cm = getDisplayCm();
			if (cm == null)
				cm = getCm();

			if (cm != null)
			{
				Map<String, Integer> dbList = DbSelectionForGraphsDialog.getDbsInGraphList(cm);
				for(int r=0; r<dataTable.getRowCount(); r++)
				{
					String DBName          = (String)dataTable.getValueAt(r, DBName_pos);
					Number LogSizeFreeInMb = (Number)dataTable.getValueAt(r, LogSizeFreeInMb_pos);
					Number LogSizeUsedPct  = (Number)dataTable.getValueAt(r, LogSizeUsedPct_pos);

					if (_logger.isDebugEnabled())
						_logger.debug("createDataset():GRAPH-DATA: "+getName()+": DBName("+DBName_pos+")='"+DBName+"', LogSizeFreeInMb("+LogSizeFreeInMb_pos+")='"+LogSizeFreeInMb+"', LogSizeUsedPct("+LogSizeUsedPct_pos+")='"+LogSizeUsedPct+"'.");

					if (dbList.keySet().contains(DBName))
						categoryDataset.addValue(LogSizeUsedPct, LogSizeFreeInMb+" MB FREE", DBName);
				}
			}
		}
		else
		{
			if (generateDummy)
			{
				for(int i=1; i<=30; i++)
				{
					double freePct = Math.random() * 100.0;
					double usedPct = 100.0 - freePct;
					BigDecimal freeMb = new BigDecimal(Math.random() * 1000.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

					categoryDataset.addValue(usedPct, freeMb+" MB FREE", "dummy_db_"+i);
				}
			}
		}

		return categoryDataset;
	}

	private JFreeChart createChart(CategoryDataset dataset)
	{
		// Get Graph Orientation: VERTICAL or HORIZONTAL
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_AUTO);

		PlotOrientation orientation = PlotOrientation.VERTICAL;
		if (orientationStr.equals(VALUE_plotOrientation_VERTICAL))   orientation = PlotOrientation.VERTICAL;
		if (orientationStr.equals(VALUE_plotOrientation_HORIZONTAL)) orientation = PlotOrientation.HORIZONTAL;
		if (orientationStr.equals(VALUE_plotOrientation_AUTO))
		{
			if (dataset.getRowCount() <= 10)
				orientation = PlotOrientation.HORIZONTAL;
		}

		JFreeChart chart = ChartFactory.createStackedBarChart(
				null,                     // Title
				null,                     // categoryAxisLabel
				null,                     // valueAxisLabel
				dataset,                  // dataset
				orientation,              // orientation
				false,                    // legend
				true,                     // tooltips
				false);                   // urls

		chart.setBackgroundPaint(getBackground());
		chart.setTitle(new TextTitle("Transaction Log Space Usage in Percent", TextTitle.DEFAULT_FONT));

		//------------------------------------------------
		// code from TabularCntrlPanel
		//------------------------------------------------
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setNoDataMessage("No data available");
		StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();

		if (orientation.equals(PlotOrientation.VERTICAL))
		{
			ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0);
			renderer.setBasePositiveItemLabelPosition(p1);
			
			ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0);
			renderer.setPositiveItemLabelPositionFallback(p2);

			// Tilt DBName 45 degree left
			CategoryAxis domainAxis = plot.getDomainAxis();
			if (orientation.equals(PlotOrientation.VERTICAL))
				domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		}
		else
		{
			ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0);
			renderer.setBasePositiveItemLabelPosition(p1);
			
			ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, 0);
			renderer.setPositiveItemLabelPositionFallback(p2);
		}

		
		StandardCategoryItemLabelGenerator scilg = new StandardCategoryItemLabelGenerator("{0}", NumberFormat.getInstance());
		renderer.setBaseItemLabelGenerator(scilg);
		
		renderer.setBaseItemLabelsVisible(true);

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setUpperBound(100);

//		for (int r=0; r<dataset.getRowCount(); r++)
//			for (int c=0; c<dataset.getColumnCount(); c++)
//				System.out.println("dataset investigate: r="+r+", c="+c+", val="+dataset.getValue(r, c));

		for (int r=0; r<dataset.getRowCount(); r++)
		{
			Number val = dataset.getValue(r, r);
			if (val == null)
				continue;
			if (val.intValue() >= 90)
				renderer.setSeriesPaint(r, Color.RED);
			else if (val.intValue() >= 80)
				renderer.setSeriesPaint(r, Color.ORANGE);
			else
				renderer.setSeriesPaint(r, Color.GREEN);
		}

		return chart;
	}


	@Override
	protected int getDefaultMainSplitPaneDividerLocation()
	{
		return 150;
	}

	@Override
	protected JPanel createExtendedInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);
		panel.setLayout(new BorderLayout());

		// Create Graph
		JFreeChart chart = createChart(createDataset(null));
		panel.add( new ChartPanel(chart) );

		// Size of the panel
		JSplitPane mainSplitPane = getMainSplitPane();
		if (mainSplitPane != null)
			mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());

		return panel;
	}

	@Override
	protected void updateExtendedInfoPanel()
	{
		JPanel panel     = getExtendedInfoPanel();
		GTable dataTable = getDataTable();
		if (panel     == null) return;

		// If the panel is so small, make it bigger 
		int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
		if (dividerLocation == 0)
		{
			JSplitPane mainSplitPane = getMainSplitPane();
			if (mainSplitPane != null)
				mainSplitPane.setDividerLocation(getDefaultMainSplitPaneDividerLocation());
		}
		
//		if ( ! isMonConnected() )
//			dataTable = null;

		panel.removeAll();

		// Create Graph
		JFreeChart chart = createChart(createDataset(dataTable));
		panel.add( new ChartPanel(chart) );
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		final JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings");
		final JButton dblist_but         = new JButton("Set 'Graph' databases");

		String[] graphTypeArr = {"Auto", "Vertical", "Horizontal"};
		final JLabel    graphType_lbl    = new JLabel("Graph Orientation");
		final JComboBox graphType_cbx    = new JComboBox(graphTypeArr);

		String tooltip =
			"<html>" +
			"Reset the option: To automatically switch to this tab when any Database(s) Transaction log is <b>full</b>.<br>" +
			"Next time this happens, a popup will ask you what you want to do." +
			"</html>";
		resetMoveToTab_but.setToolTipText(tooltip);

		dblist_but   .setToolTipText("<html>What databases should be apart of the graphs, both in the Summary Panel, and this local graph area.<br></html>");
		tooltip = 
			"<html>" +
			"Do you want the 'Transaction Log Size Graph' to be presented 'Standing' or 'Laying'.<br>" +
			"'Auto' means: " +
			"<ul>" +
			"    <li>'Laying' if <b>less</b> than 10 databases is included.</li>" +
			"    <li>'Standing' if <b>more</b> than 10 databases is included.</li>" +
			"</ul>" +
			"</html>";
		graphType_lbl.setToolTipText(tooltip);
		graphType_cbx.setToolTipText(tooltip);

		// Set initial value for Graph Orientation
		Configuration conf = Configuration.getCombinedConfiguration();
		String orientationStr = conf.getProperty(PROPKEY_plotOrientation, DEFAULT_plotOrientation);
		String orientation = graphTypeArr[0]; // set as default
		if (orientationStr.equals(VALUE_plotOrientation_AUTO))       orientation = graphTypeArr[0];
		if (orientationStr.equals(VALUE_plotOrientation_VERTICAL))   orientation = graphTypeArr[1];
		if (orientationStr.equals(VALUE_plotOrientation_HORIZONTAL)) orientation = graphTypeArr[2];
		graphType_cbx.setSelectedItem(orientation);
		
		// ACTION events
		resetMoveToTab_but.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				ChangeToJTabDialog.resetSavedSettings(getPanelName());
			}
		});
		dblist_but.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = getDisplayCm();
				if (cm == null)
					cm = getCm();

				int rc = DbSelectionForGraphsDialog.showDialog(MainFrame.getInstance(), cm);
				if (rc == JOptionPane.OK_OPTION)
					updateExtendedInfoPanel();
			}
		});
		graphType_cbx.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
					return;
				
				String type = graphType_cbx.getSelectedItem().toString();
				if (type.equals("Auto"))       conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_AUTO);
				if (type.equals("Vertical"))   conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_VERTICAL);
				if (type.equals("Horizontal")) conf.setProperty(PROPKEY_plotOrientation, VALUE_plotOrientation_HORIZONTAL);
				conf.save();
				
				updateExtendedInfoPanel();
			}
		});

		// ADD to panel
		panel.add(resetMoveToTab_but, "wrap 5");
		panel.add(dblist_but,         "wrap 5");
		panel.add(graphType_lbl,      "split");
		panel.add(graphType_cbx,      "wrap 5");

		return panel;
	}
}
