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
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.swing.GButton;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class TrendGraphDashboardPanel
extends JPanel
{
	private static final long	serialVersionUID	= 1L;
	static Logger _logger = Logger.getLogger(TrendGraphDashboardPanel.class);

	private final String GRAPH_LAYOUT_PROP = "push, grow, hidemode 3, wrap";
	private final String FACEBOOK_URL      = "http://www.facebook.com/pages/AseTune/223112614400980"; 
	private final String SOURCEFORGE_URL   = "http://sourceforge.net/projects/asetune/"; 
	private final String DONATE_URL        = "http://www.asetune.com/donate.html"; 

	private JLabel             _maxChartHistoryInMinutes_lbl = new JLabel();
	private SpinnerNumberModel _maxChartHistoryInMinutes_spm = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxChartHistoryInMinutes_sp  = new JSpinner(_maxChartHistoryInMinutes_spm);

	private JLabel             _maxInMemHistoryInMinutes_lbl = new JLabel();
	private SpinnerNumberModel _maxInMemHistoryInMinutes_spm = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxInMemHistoryInMinutes_sp  = new JSpinner(_maxInMemHistoryInMinutes_spm);
	private JCheckBox          _maxInMemHistoryInMinutes_chk = new JCheckBox("Same as Graph History", true);

	private JButton            _donate_but                   = new GButton();
	private JButton            _sourceforge_but              = new GButton();
	private JButton            _facebook_but                 = new GButton();
	
	private List<String>       _savedGraphNameOrder = new ArrayList<String>();

	private LinkedHashMap<String, TrendGraph> _graphOriginOrderMap    = new LinkedHashMap<String, TrendGraph>();
	private LinkedHashMap<String, TrendGraph> _graphCurrentOrderMap   = new LinkedHashMap<String, TrendGraph>();

	// Used to launch WEB BROWSER
	private Desktop            _desktop = null;

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
	}

	private void initComponents() 
	{
		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		_maxChartHistoryInMinutes_lbl.setText("Trends Graph History in Minutes");
		_maxChartHistoryInMinutes_lbl.setToolTipText("How many minutes should be represented in the graphs.");
		_maxChartHistoryInMinutes_sp .setToolTipText("How many minutes should be represented in the graphs.");

		_maxInMemHistoryInMinutes_lbl.setText("In-Memory History in Minutes");
		_maxInMemHistoryInMinutes_lbl.setToolTipText("<html>How many minutes should be available in the in-memory history.<br>To view in-memory history, click the magnifying/db button on the tool bar.</html>");
		_maxInMemHistoryInMinutes_sp .setToolTipText("<html>How many minutes should be available in the in-memory history.<br>To view in-memory history, click the magnifying/db button on the tool bar.</html>");
		_maxInMemHistoryInMinutes_chk.setToolTipText("Use same value for 'In-Memory History' as for 'Trends Graph History'.");

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
		JPanel topPanel = new JPanel(new MigLayout("insets 0 0 0 0", "", ""));

		topPanel.add(_maxChartHistoryInMinutes_lbl, "split 5, center, gaptop 10");
		topPanel.add(_maxChartHistoryInMinutes_sp,  "");

		topPanel.add(_maxInMemHistoryInMinutes_lbl, "gap 10");
		topPanel.add(_maxInMemHistoryInMinutes_sp,  "gap 5");
		topPanel.add(_maxInMemHistoryInMinutes_chk, "pushx");

		topPanel.add(_donate_but,      "hidemode 3"); _donate_but.setVisible(false);
		topPanel.add(_sourceforge_but, "hidemode 2");
		topPanel.add(_facebook_but,    "hidemode 2");

		add(topPanel, "pushx, growx, wrap");

		
		String tooltip = "";
		setToolTipText(tooltip);
	}
	
	private void initComponentActions() 
	{
		// HISTORY
		_maxChartHistoryInMinutes_spm.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent ce)
			{
				int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
				_logger.debug("maxChartHistoryInMinutes: " + minutes);
				setChartMaxHistoryTimeInMinutes(minutes);
				
				saveProps();
			}
		});
		
		// IN-MEMORY-HISTORY
		_maxInMemHistoryInMinutes_spm.addChangeListener(new ChangeListener()
		{
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
	 * this should relly be implemented better...<br>
	 * Current implementation is not enable/disable, it's just disable=setToOneMinute, enable=setToDefault
	 * 
	 * @param enable
	 */
	public void setInMemHistoryEnable(boolean enable)
	{
		if ( ! enable )
		{
			// in-sync_chkbox=false, spinner=enabled
			_maxInMemHistoryInMinutes_chk.setSelected(false);
			_maxInMemHistoryInMinutes_sp.setEnabled(true);

			setInMemMaxHistoryTimeInMinutes(1);
		}
		else
		{
			// in-sync_chkbox=true, spinner=disabled
			_maxInMemHistoryInMinutes_chk.setSelected(true);
			_maxInMemHistoryInMinutes_sp.setEnabled(false);

			// get chart history and use that
			int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
			setInMemMaxHistoryTimeInMinutes(minutes);
		}
		saveProps();
	}

	private void setInMemMaxHistoryTimeInMinutes(int minutes)
	{
		_logger.debug("set-In-Mem-MaxHistoryTimeInMinutes(minutes="+minutes+")");

		// Set the spinner
		_maxInMemHistoryInMinutes_spm.setValue( new Integer(minutes) );

		// set the Handler
		InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
		if (imch != null)
			imch.setHistoryLengthInMinutes(minutes);
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
			add(tg.getPanel(), GRAPH_LAYOUT_PROP);
		}
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		// Graph history
		String str = _maxChartHistoryInMinutes_spm.getNumber().toString();
		conf.setProperty("graph.history", str);

		// In memory history
		str = _maxInMemHistoryInMinutes_spm.getNumber().toString();
		conf.setProperty("in-memory.history", str);

		str = _maxInMemHistoryInMinutes_chk.isSelected()+"";
		conf.setProperty("in-memory.history.in-sync-with-graph-history", str);

		// Graph Order
		str = StringUtil.toCommaStr(getGraphOrderStrList());
		conf.setProperty("graph.order", str);

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// in-memory history (this should be before "Graph history")
		boolean inMemHistSync = conf.getBooleanProperty("in-memory.history.in-sync-with-graph-history", true);
		_maxInMemHistoryInMinutes_chk.setSelected(inMemHistSync);
		_maxInMemHistoryInMinutes_sp.setEnabled( ! inMemHistSync );

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

		// Graph Order
		String str = conf.getProperty("graph.order", "");
		_savedGraphNameOrder = StringUtil.parseCommaStrToList(str);
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
	 * Set the order of the graphs, but do not modify if they shgould be visibale or not.
	 * @param newGraphOrder A collection of graph names
	 */
	public void setGraphOrder(Collection<String> newGraphOrder, boolean printWarnings)
	{
		// remove all Graphs from the Panels Layout Manager
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			remove(tg.getPanel());

		_graphCurrentOrderMap.clear();

		for (String graphName : newGraphOrder)
		{
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
					add(graphPanel, GRAPH_LAYOUT_PROP);
			}
		}
	}
	public void setGraphOrder(LinkedHashMap<String, Boolean> newGraphOrderMap)
	{
		// remove all Graphs from the Panels Layout Manager
		for (TrendGraph tg : _graphCurrentOrderMap.values())
			remove(tg.getPanel());

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
				add(tg.getPanel(), GRAPH_LAYOUT_PROP);
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
