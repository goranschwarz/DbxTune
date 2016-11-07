package com.asetune.gui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.gui.focusabletip.FocusableTipExtention;
import com.asetune.gui.swing.VerticalScrollPane;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class TrendGraphDashboardPanel
extends JPanel
{
	private static final long	serialVersionUID	= 1L;
	static Logger _logger = Logger.getLogger(TrendGraphDashboardPanel.class);

	private final String GRAPH_LAYOUT_CONSTRAINT = "insets 0 0 0 0, fill, hidemode 3, wrap "; // + _graphLayoutColumns
//	private final String GRAPH_LAYOUT_PROP       = "push, grow, hidemode 3";
	private final String GRAPH_LAYOUT_PROP       = "grow";

	private final String FACEBOOK_URL            = "http://www.facebook.com/pages/AseTune/223112614400980"; 
	private final String SOURCEFORGE_URL         = "http://sourceforge.net/projects/asetune/"; 
	private final String DONATE_URL              = "http://www.asetune.com/donate.html"; 

	private final static String  PROPKEY_inMemoryHistoryEnabled     = "in-memory.history.enabled"; 
	private final static boolean DEFAULT_inMemoryHistoryEnabled     = false; 

	private final static String  PROPKEY_graphLayoutColumns         = "TrendGraphDashboardPanel.graphLayoutColumns"; 
	private final static int     DEFAULT_graphLayoutColumns         = 2; 

	static
	{
		Configuration.registerDefaultValue(PROPKEY_inMemoryHistoryEnabled, DEFAULT_inMemoryHistoryEnabled);
		Configuration.registerDefaultValue(PROPKEY_graphLayoutColumns,     DEFAULT_graphLayoutColumns);
	}

	private JButton            _graphs_but                    = new JButton();

	private int                _graphLayoutColumns            = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_graphLayoutColumns, DEFAULT_graphLayoutColumns);
	private JLabel             _graphLayoutColumns_lbl        = new JLabel("Graph Columns");
	private SpinnerNumberModel _graphLayoutColumns_spm        = new SpinnerNumberModel(_graphLayoutColumns, 1, 10, 1);
	private JSpinner           _graphLayoutColumns_sp         = new JSpinner(_graphLayoutColumns_spm);

	private JLabel             _maxChartHistoryInMinutes_lbl  = new JLabel("Trends Graph History in Minutes");
	private SpinnerNumberModel _maxChartHistoryInMinutes_spm  = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxChartHistoryInMinutes_sp   = new JSpinner(_maxChartHistoryInMinutes_spm);

	private JCheckBox          _maxInMemHistoryEnabled_chk    = new JCheckBox("Enable In-Memory History", DEFAULT_inMemoryHistoryEnabled);
	private JLabel             _maxInMemHistoryInMinutes_lbl  = new JLabel("In Minutes");
	private SpinnerNumberModel _maxInMemHistoryInMinutes_spm  = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxInMemHistoryInMinutes_sp   = new JSpinner(_maxInMemHistoryInMinutes_spm);
	private JCheckBox          _maxInMemHistoryInMinutes_chk  = new JCheckBox("Same as Graph History", true);

	private JButton            _donate_but                    = new JButton();
	private JButton            _sourceforge_but               = new JButton();
	private JButton            _facebook_but                  = new JButton();
	
	private List<String>       _savedGraphNameOrder = new ArrayList<String>();

	private LinkedHashMap<String, TrendGraph> _graphOriginOrderMap    = new LinkedHashMap<String, TrendGraph>();
	private LinkedHashMap<String, TrendGraph> _graphCurrentOrderMap   = new LinkedHashMap<String, TrendGraph>();

	// Used to launch WEB BROWSER
	private Desktop            _desktop = null;
	private boolean            _initialized = false;

	private JPanel             _topPanel   = null;
	private JPanel             _graphPanel = null;
	private JScrollPane        _graphPanelScroll = null;

	
	public TrendGraphDashboardPanel()
	{
		if (Desktop.isDesktopSupported())
		{
			_desktop = Desktop.getDesktop();
			if ( ! _desktop.isSupported(Desktop.Action.BROWSE) ) 
				_desktop = null;
		}

		initComponents();
		loadProps();
		initComponentActions();
		
		setComponentVisibility();
		_initialized = true;
	}

	private void setComponentVisibility()
	{
		boolean toVisible = _maxInMemHistoryEnabled_chk.isSelected();
		_maxInMemHistoryInMinutes_lbl.setVisible(toVisible);
		_maxInMemHistoryInMinutes_sp .setVisible(toVisible);
		_maxInMemHistoryInMinutes_chk.setVisible(toVisible);
	}

	private void initComponents() 
	{
		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		_graphs_but.setName("GRAPH_ORDER"); // just for trace
		_graphs_but.setToolTipText("<html>Open Graph properties Dialog</html>");
		_graphs_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/graph_order_32.png"));
		_graphs_but.setContentAreaFilled(false);
		_graphs_but.setMargin( new Insets(0,0,0,0) );
		_graphs_but.setToolTipText("<html>Open Graph properties Dialog</html>");

		_graphLayoutColumns_lbl.setToolTipText("<html>How many Graphs do you want <i>side by side</i>. </html>");
		_graphLayoutColumns_sp .setToolTipText("<html>How many Graphs do you want <i>side by side</i>. </html>");
		
		_maxChartHistoryInMinutes_lbl.setToolTipText("How many minutes should be represented in the graphs.");
		_maxChartHistoryInMinutes_sp .setToolTipText("How many minutes should be represented in the graphs.");
		
		_maxInMemHistoryEnabled_chk.setToolTipText(
				"<html>" +
				  "Is the 'in-memory' counter history enabled or disabled.<br>" +
				  "<br>" +
				  "To view in-memory history, click the <i>Magnifying/DB Button</i> on the <i>toolbar</i>.<br>" +
				  "Then navigate in the history using the <i>'slider'</i> in the <i>toolbar</i>, right next to the <i>Magnifying/DB Button</i><br>" +
				  "Or simply <i>double-click</i> on any of the Trend Graphs to navigate in the 'timeline'.<br>" +
				  "<br>" +
				  "<b>Note:</b> Think about memory consumption here. <br>" +
				  "<b>All</b> sampled performance counters are stored in memory for X number of minutes." +
				"</html>");

		_maxInMemHistoryInMinutes_lbl.setToolTipText("How many minutes should be available in the in-memory history.");
		_maxInMemHistoryInMinutes_sp .setToolTipText("How many minutes should be available in the in-memory history.");
		_maxInMemHistoryInMinutes_chk.setToolTipText("Use same value for 'In-Memory History' as for 'Trends Graph History'.");

		// CheckBox on the right side
		_maxInMemHistoryEnabled_chk  .setHorizontalTextPosition(SwingConstants.LEFT);
		_maxInMemHistoryInMinutes_chk.setHorizontalTextPosition(SwingConstants.LEFT);

		_donate_but.setName("DONATE"); // just for trace
		_donate_but.setToolTipText("<html>Goto "+Version.getAppName()+" home page where you can read more about how you can support the project.<br><br>"+DONATE_URL+"<html>");
		_donate_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/donate.gif"));
		_donate_but.setContentAreaFilled(false);
		_donate_but.setMargin( new Insets(0,0,0,0) );
		
		_sourceforge_but.setName("SF"); // just for trace
		_sourceforge_but.setToolTipText("<html>Goto "+Version.getAppName()+" at Sourceforge. Here you can: <UL> <LI>Recommend "+Version.getAppName()+" to others</LI> <LI>Make comments/recommendations</LI> <LI>Add: Support Questions</LI> <LI>Add: Bug Reports</LI> <LI>Add: Feature Requests</LI> </UL>"+SOURCEFORGE_URL+"<html>");
		_sourceforge_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/sourceforge_logo.png"));
		_sourceforge_but.setContentAreaFilled(false);
		_sourceforge_but.setMargin( new Insets(0,0,0,0) );
		
		_facebook_but.setName("FB"); // just for trace
		_facebook_but.setToolTipText("<html>Goto "+Version.getAppName()+" Facebook Page <UL> <LI>Press 'like' to get information about upcoming changes.</LI> <LI>Or write something on the wall.</LI> </UL>"+FACEBOOK_URL+"</html>");
//		_facebook_but.setText("Join now");
		_facebook_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/facebook_button.png"));
//		_facebook_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/facebook_favicon.png"));
		_facebook_but.setContentAreaFilled(false);
		_facebook_but.setMargin( new Insets(0,0,0,0) );

		if (_desktop == null)
		{
			_sourceforge_but.setVisible(false);
			_facebook_but   .setVisible(false);
		}

		// Create a separate Panel for the things to position at the top...
		_topPanel = new JPanel(new MigLayout("insets 0 0 0 0", "", ""));

		_topPanel.add(_graphs_but,                   "align left, gapleft 5");

		_topPanel.add(new JLabel(""),                "span 10, align center, gaptop 10, pushx"); // Dummy to fill up space

		_topPanel.add(_graphLayoutColumns_lbl,       "");
		_topPanel.add(_graphLayoutColumns_sp,        "");

		_topPanel.add(_maxChartHistoryInMinutes_lbl, "gap 20");
//		_topPanel.add(_maxChartHistoryInMinutes_lbl, "split 7, align center, gaptop 10");
		_topPanel.add(_maxChartHistoryInMinutes_sp,  "");

		_topPanel.add(_maxInMemHistoryEnabled_chk,   "gap 10");
		_topPanel.add(_maxInMemHistoryInMinutes_chk, "gap 10,  hidemode 2");
		_topPanel.add(_maxInMemHistoryInMinutes_lbl, "gap 5,   hidemode 2");
		_topPanel.add(_maxInMemHistoryInMinutes_sp,  "gap 5,   hidemode 2");
		_topPanel.add(new JLabel(""),                "pushx"); // Dummy to fill up space

		_topPanel.add(_donate_but,      "hidemode 3"); _donate_but.setVisible(false);
		_topPanel.add(_sourceforge_but, "hidemode 2");
		_topPanel.add(_facebook_but,    "hidemode 2");

		_graphPanel = new JPanel(new MigLayout(GRAPH_LAYOUT_CONSTRAINT+_graphLayoutColumns, "", ""));
//		_graphPanelScroll = new JScrollPane(_graphPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		_graphPanelScroll = new JScrollPane(_graphPanel);
		_graphPanelScroll = new VerticalScrollPane(_graphPanel);
		_graphPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

		add(_topPanel,         "pushx, growx, wrap");
		add(_graphPanelScroll, "push, grow, wrap");

		// Install some focusable tooltip
		FocusableTipExtention.install(_donate_but);
		FocusableTipExtention.install(_sourceforge_but);
		FocusableTipExtention.install(_facebook_but);
		
		String tooltip = "";
		setToolTipText(tooltip);
	}
	
	private void initComponentActions() 
	{
		// SOURCEFORGE
		_graphs_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int ret = TrendGraphPanelReorderDialog.showDialog(MainFrame.getInstance(), CounterController.getSummaryPanel().getGraphPanel());
				if (ret == JOptionPane.OK_OPTION)
				{
				}
			}
		});

		// GRAPH COLUMNS
		_graphLayoutColumns_spm.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				_graphLayoutColumns = _graphLayoutColumns_spm.getNumber().intValue();

				MigLayout layoutManager = (MigLayout)_graphPanel.getLayout();
				layoutManager.setLayoutConstraints(GRAPH_LAYOUT_CONSTRAINT+_graphLayoutColumns);

				reLayout();

				saveProps();
			}
		});
		
		// HISTORY
		_maxChartHistoryInMinutes_spm.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
				_logger.debug("maxChartHistoryInMinutes: " + minutes);
				setChartMaxHistoryTimeInMinutes(minutes);
				
				saveProps();
			}
		});
		
		// IN-MEMORY-HISTORY: ENABLE|DISABLE
		_maxInMemHistoryEnabled_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{

				setInMemHistoryEnable( _maxInMemHistoryEnabled_chk.isSelected() );

				saveProps();
			}
		});
//		_maxInMemHistoryEnabled_chk.addChangeListener(new ChangeListener()
//		{
//			@Override
//			public void stateChanged(ChangeEvent ce)
//			{
//				setInMemHistoryEnable( _maxInMemHistoryEnabled_chk.isSelected() );
//
//				saveProps();
//			}
//		});
		// IN-MEMORY-HISTORY
		_maxInMemHistoryInMinutes_spm.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				int minutes = _maxInMemHistoryInMinutes_spm.getNumber().intValue();
				_logger.debug("maxInMemHistoryInMinutes: " + minutes);
				setInMemMaxHistoryTimeInMinutes(minutes);
				
				saveProps();
			}
		});
		// IN-MEMORY-HISTORY - IN-SYNC
		_maxInMemHistoryInMinutes_chk.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				// enable/disable the Spinner
				boolean inSync = _maxInMemHistoryInMinutes_chk.isSelected();
				_maxInMemHistoryInMinutes_sp.setEnabled( ! inSync );

				// Set Chart value if we should be in sync
				if (inSync)
				{
					int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
					setInMemMaxHistoryTimeInMinutes(minutes);
				}
				
				saveProps();
			}
		});
		
		// FACEBOOK
		_facebook_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_desktop == null)
					return;

				try
				{
					_desktop.browse(new URI(FACEBOOK_URL));
				}
				catch (Exception ex)
				{
					_logger.error("Problems when open the Facebook page '"+FACEBOOK_URL+"'. Caught: "+e);
				}
			}
		});

		// SOURCEFORGE
		_sourceforge_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_desktop == null)
					return;

				try
				{
					_desktop.browse(new URI(SOURCEFORGE_URL));
				}
				catch (Exception ex)
				{
					_logger.error("Problems when open the SourceForge page '"+SOURCEFORGE_URL+"'. Caught: "+e);
				}
			}
		});

		// SOURCEFORGE
		_donate_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_desktop == null)
					return;

				try
				{
					_desktop.browse(new URI(DONATE_URL));
				}
				catch (Exception ex)
				{
					_logger.error("Problems when open the donate page '"+DONATE_URL+"'. Caught: "+e);
				}
			}
		});
	}

	private void setChartMaxHistoryTimeInMinutes(int minutes)
	{
		_logger.debug("set-Chart-MaxHistoryTimeInMinutes(minutes="+minutes+")");

		// Set the Spinner model
		_maxChartHistoryInMinutes_spm.setValue( new Integer(minutes) );

		// Set all the individual graphs
		for (TrendGraph tg : _graphCurrentOrderMap.values())
		{
			if (tg != null)
			{
				tg.setChartMaxHistoryTimeInMinutes(minutes);
			}
		}
		
		// set the in-memory history, if it should be in sync
		if (_maxInMemHistoryInMinutes_chk.isSelected())
		{
			setInMemMaxHistoryTimeInMinutes( minutes );
		}
	}


	/**
	 * this should rely be implemented better...<br>
	 * Current implementation is not enable/disable, it's just disable=setToOneMinute, enable=setToDefault
	 * 
	 * @param enable
	 */
	public void setInMemHistoryEnable(boolean enable)
	{
		if ( enable )
		{
			_maxInMemHistoryEnabled_chk.setSelected(true);

			// in-sync_chkbox=true, spinner=disabled
			_maxInMemHistoryInMinutes_chk.setSelected(true);
			_maxInMemHistoryInMinutes_sp.setEnabled(false);

			// get chart history and use that
			int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
			setInMemMaxHistoryTimeInMinutes(minutes);
		}
		else
		{
			_maxInMemHistoryEnabled_chk.setSelected(false);

			// in-sync_chkbox=false, spinner=enabled
			_maxInMemHistoryInMinutes_chk.setSelected(false);
			_maxInMemHistoryInMinutes_sp.setEnabled(true);

			setInMemMaxHistoryTimeInMinutes(1);
		}
		setComponentVisibility();
		saveProps();
	}

	private void setInMemMaxHistoryTimeInMinutes(int minutes)
	{
		_logger.debug("set-In-Mem-MaxHistoryTimeInMinutes(minutes="+minutes+")");

		// Set the spinner
		_maxInMemHistoryInMinutes_spm.setValue( new Integer(minutes) );

		// set the Handler
		// During startup the in-memory handler might not yet have an instance...
		// so if not, save the property directly
		if (InMemoryCounterHandler.hasInstance())
			InMemoryCounterHandler.getInstance().setHistoryLengthInMinutes(minutes);
		else
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				conf.setProperty(InMemoryCounterHandler.PROPKEY_HISTORY_SIZE_IN_SECONDS, minutes*60);
				conf.save();
			}
		}
	}

	
	public void refreshHistoryTimeInMinutes()
	{
		Integer minutes = (Integer) _maxChartHistoryInMinutes_spm.getValue();

		setChartMaxHistoryTimeInMinutes( minutes.intValue() );
	}

	public void clearGraph()
	{
		for (TrendGraph tg : _graphCurrentOrderMap.values())
		{
			if (tg != null)
			{
				tg.clearGraph();
			}
		}		
	}

	public void add(TrendGraph tg)
	{
		_graphOriginOrderMap .put(tg.getName(), tg);
		_graphCurrentOrderMap.put(tg.getName(), tg);

		// Set the Graph history time...
		Integer minutes = (Integer) _maxChartHistoryInMinutes_spm.getValue();
		tg.setChartMaxHistoryTimeInMinutes( minutes.intValue() );

		if (_savedGraphNameOrder.contains(tg.getName()))
		{
			// The graph panel will be added in here, but in the correct order
			setGraphOrder(_savedGraphNameOrder, false);
		}
		else
		{
			// Add the graph
			_graphPanel.add(tg.getPanel(), GRAPH_LAYOUT_PROP);
		}
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		// In memory history: enabled | disabled
		conf.setProperty(PROPKEY_graphLayoutColumns, _graphLayoutColumns);

		// Graph history
		String str = _maxChartHistoryInMinutes_spm.getNumber().toString();
		conf.setProperty("graph.history", str);

		// In memory history: enabled | disabled
		conf.setProperty(PROPKEY_inMemoryHistoryEnabled, _maxInMemHistoryEnabled_chk.isSelected());

		// In memory history
		str = _maxInMemHistoryInMinutes_spm.getNumber().toString();
		conf.setProperty("in-memory.history", str);

		str = _maxInMemHistoryInMinutes_chk.isSelected()+"";
		conf.setProperty("in-memory.history.in-sync-with-graph-history", str);

		// Graph Order
		// Only save if we are initialized... otherwise we will overwrite the "graph.order"
		if (_initialized)
		{
			str = StringUtil.toCommaStr(getGraphOrderStrList());
			conf.setProperty("graph.order", str);
		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// in-memory history (this should be before "Graph history")
		boolean inMemHistSync = conf.getBooleanProperty("in-memory.history.in-sync-with-graph-history", true);
		_maxInMemHistoryInMinutes_chk.setSelected(inMemHistSync);
		_maxInMemHistoryInMinutes_sp.setEnabled( ! inMemHistSync );

		// In memory history
		int inMemHist = conf.getIntProperty("in-memory.history", -1);
		if (inMemHist != -1)
		{
			setInMemMaxHistoryTimeInMinutes( inMemHist );
		}

		// Graph history
		int hist = conf.getIntProperty("graph.history", -1);
		if (hist != -1)
		{
			setChartMaxHistoryTimeInMinutes( hist );
		}

		// In memory history: enabled | disabled
		boolean inMemHistoryEnabled = conf.getBooleanProperty(PROPKEY_inMemoryHistoryEnabled, DEFAULT_inMemoryHistoryEnabled);
		setInMemHistoryEnable( inMemHistoryEnabled );

		// Graph Order
		String str = conf.getProperty("graph.order", "");
		_savedGraphNameOrder = StringUtil.parseCommaStrToList(str);
	}


	/**
	 * remove/add all graphs from the GraphPanel, this so we can change number of columns in the layout etc...
	 */
	private void reLayout()
	{
		// remove all Graphs from the Panels Layout Manager
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			_graphPanel.remove(tg.getPanel());

		// Then add them again... 
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			_graphPanel.add(tg.getPanel(), GRAPH_LAYOUT_PROP);
	}

	/**
	 * Enable or disable a graph.
	 * @param graphName
	 * @param enabled
	 */
	public void setEnableGraph(String graphName, Boolean enabled)
	{
		TrendGraph tg = _graphCurrentOrderMap.get(graphName);
		if (tg == null)
			throw new RuntimeException("Couldn't find graph name '"+graphName+"' in the list of graphs: "+_graphCurrentOrderMap);
		tg.setEnable(enabled);
	}

	/**
	 * Set the order of the graphs, but do not modify if they should be visible or not.
	 * @param newGraphOrder A collection of graph names
	 */
	public void setGraphOrder(Collection<String> newGraphOrder, boolean printWarnings)
	{
		// Used to keep graphs that is NOT in the 'newGraphOrder' list
		LinkedHashMap<String, TrendGraph> savedCurrentOrder  = new LinkedHashMap<String, TrendGraph>(_graphCurrentOrderMap);

		// remove all Graphs from the Panels Layout Manager
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			_graphPanel.remove(tg.getPanel());

		_graphCurrentOrderMap.clear();

		for (String graphName : newGraphOrder)
		{
			// remove it from the save list, since it's "handled" included in the new layout
			savedCurrentOrder.remove(graphName);

			TrendGraph tg = _graphOriginOrderMap.get(graphName);
			if (tg == null)
			{
				// hmm problems... not in origin...
				if (printWarnings)
					_logger.warn("Graph name '"+graphName+"' could not be found in the original map.");
			}
			else
			{
				_graphCurrentOrderMap.put(tg.getName(), tg);

				JPanel graphPanel = tg.getPanel();

				// Check if the graphPanel is part of the THIS JPanel  
				boolean inPanel = false;
				for (Component comp : getComponents())
				{
					if (comp.equals(graphPanel))
					{
						inPanel = true;
						break;
					}
				}

				// Should we show it or not?
				graphPanel.setVisible(tg.isGraphEnabled());

				// Add it to panel if not already part of it
				if ( ! inPanel )
					_graphPanel.add(graphPanel, GRAPH_LAYOUT_PROP);
			}
		}
		
		// All graphs, that was NOT in the newGraphOrder list, just add them 
		for (String graphName : savedCurrentOrder.keySet())
		{
			TrendGraph tg = _graphOriginOrderMap.get(graphName);
			JPanel graphPanel = tg.getPanel();

			_graphCurrentOrderMap.put(tg.getName(), tg);
			_graphPanel.add(graphPanel, GRAPH_LAYOUT_PROP);
		}
	}
	public void setGraphOrder(LinkedHashMap<String, Boolean> newGraphOrderMap)
	{
		// remove all Graphs from the Panels Layout Manager
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			_graphPanel.remove(tg.getPanel());

		_graphCurrentOrderMap.clear();

		for (Map.Entry<String, Boolean> entry : newGraphOrderMap.entrySet()) 
		{
			String  graphName = entry.getKey();
			boolean enabled   = entry.getValue();

			TrendGraph tg = _graphOriginOrderMap.get(graphName);
			if (tg == null)
			{
				// hmm problems... not in origin...
				_logger.warn("Graph name '"+graphName+"' could not be found in the original map.");
			}
			else
			{
				_graphCurrentOrderMap.put(tg.getName(), tg);

				tg.setEnable(enabled);
				_graphPanel.add(tg.getPanel(), GRAPH_LAYOUT_PROP);
			}
		}
		//FIXME: what if the newGraphOrderMap is "less" than _graphOriginOrderMap... this means that the passed newGraphOrderMap is not complete... should we handle this
	}

	/**
	 * Get the current Graph Order
	 * @return a List<String> of the names
	 */
	public List<String> getGraphOrderStrList()
	{
		return getGraphOrder(_graphCurrentOrderMap);
	}

	/**
	 * Get the Origin Graph Order
	 * @return a List<String> of the names
	 */
	public List<String> getOriginGraphOrderStrList()
	{
		return getGraphOrder(_graphOriginOrderMap);
	}

	private List<String> getGraphOrder(Map<String, TrendGraph> in)
	{
		ArrayList<String> list = new ArrayList<String>();

		for (TrendGraph tg : in.values())
			list.add(tg.getName());

		return list;
	}

	/**
	 * Get Current Graph Order
	 * @return a Collection<TrendGraph> of the graph objects
	 */
	public Collection<TrendGraph> getGraphOrder()
	{
		return _graphCurrentOrderMap.values();
	}

	/**
	 * Get Origin Graph Order
	 * @return a Collection<TrendGraph> of the graph objects
	 */
	public Collection<TrendGraph> getOriginGraphOrder()
	{
		return _graphOriginOrderMap.values();
	}

	/**
	 * 
	 */
	public void saveOrderAndVisibility()
	{
		saveProps();
	}

	/**
	 * 
	 */
	public void removeOrderAndVisibility()
	{
		setGraphOrder(getOriginGraphOrderStrList(), true);

		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf != null)
		{
			conf.remove("graph.order");
			conf.save();
		}
	}
}
