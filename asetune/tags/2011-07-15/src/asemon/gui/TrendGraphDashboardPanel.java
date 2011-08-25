package asemon.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import asemon.pcs.InMemoryCounterHandler;
import asemon.utils.Configuration;
import asemon.utils.StringUtil;

public class TrendGraphDashboardPanel
extends JPanel
{
	private static final long	serialVersionUID	= 1L;
	static Logger _logger = Logger.getLogger(TrendGraphDashboardPanel.class);

	private JLabel             _maxChartHistoryInMinutes_lbl = new JLabel();
	private SpinnerNumberModel _maxChartHistoryInMinutes_spm = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxChartHistoryInMinutes_sp  = new JSpinner(_maxChartHistoryInMinutes_spm);

	private JLabel             _maxInMemHistoryInMinutes_lbl = new JLabel();
	private SpinnerNumberModel _maxInMemHistoryInMinutes_spm = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxInMemHistoryInMinutes_sp  = new JSpinner(_maxInMemHistoryInMinutes_spm);
	private JCheckBox          _maxInMemHistoryInMinutes_chk = new JCheckBox("Same as Graph History", true);

	private List<String>       _savedGraphNameOrder = new ArrayList<String>();

	private LinkedHashMap<String, TrendGraph> _graphOriginOrderMap    = new LinkedHashMap<String, TrendGraph>();
	private LinkedHashMap<String, TrendGraph> _graphCurrentOrderMap   = new LinkedHashMap<String, TrendGraph>();
	
	public TrendGraphDashboardPanel()
	{
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

		add(_maxChartHistoryInMinutes_lbl, "gaptop 10, center, split 5");
		add(_maxChartHistoryInMinutes_sp , "");
		
		add(_maxInMemHistoryInMinutes_lbl, "gap 10");
		add(_maxInMemHistoryInMinutes_sp , "gap 5");
		add(_maxInMemHistoryInMinutes_chk, "wrap 10");
		
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
			setGraphOrder(_savedGraphNameOrder, false);
		}
		else
		{
			// Add the graph
			add(tg.getPanel(), GRAPH_LAYOUT_PROP);
		}
	}
	private final String GRAPH_LAYOUT_PROP = "push, grow, hidemode 3, wrap";

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

				if (tg.isGraphEnabled())
					add(tg.getPanel(), GRAPH_LAYOUT_PROP);
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
				if (enabled)
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
