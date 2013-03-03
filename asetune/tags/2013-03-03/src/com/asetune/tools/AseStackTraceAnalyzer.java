package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.CollectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

/**
 * Used to analyze sybmon Stack Trace output
 * @author gorans
 */
public class AseStackTraceAnalyzer
{
	private static Logger _logger = Logger.getLogger(AseStackTraceAnalyzer.class);

	/** Different ways to be used when filtering on function names */
	public enum FunctionFilterType {EXACT, INDEXOF, REGEXP};
	
	/**
	 * Class that reads/parser a sybmon stack trace file
	 * @author gorans
	 */
	private static class AseStackTraceReader
	{
		private String  _filename           = null;
		private int     _sampleIterations   = 0;
		private int     _sampleInterval     = 0;

		private int     _startRead          = 0;
		private int     _stopRead           = Integer.MAX_VALUE;
		private int     _engine             = -1;

		private String  _functionFilter     = null;
		private String  _functionStackStart = null;
		private boolean _keepCppInstance    = false;
		private FunctionFilterType _functionFilterType = FunctionFilterType.INDEXOF;

		private StackEntry _root            = new StackEntry(null, "root");


		public int    getSampleIterations() { return _sampleIterations; }
		public int    getSampleInterval()   { return _sampleInterval; }
		public String getFile()             { return _filename; }

		public void setFile              (String filename)      { _filename           = filename; }
		public void setFunctionFilter    (String  functionName) { _functionFilter     = functionName; }
		public void setFunctionFilterType(FunctionFilterType t) { _functionFilterType = t; }
		public void setFunctionStackStart(String  functionName) { _functionStackStart = functionName; }
		public void setKeepCppInstance   (boolean keep)         { _keepCppInstance    = keep; }
		public void setStartRead         (int startRead)        { _startRead          = startRead; }
		public void setStopRead          (int stopRead)         { _stopRead           = stopRead; }
		public void setEngine            (int engine)           { _engine             = engine; }

		public AseStackTraceReader()
		{
		}

		/**
		 * Read/parse the file
		 */
		public void readFile()
		throws Exception
		{
			if (StringUtil.isNullOrBlank(_filename))
				throw new Exception("Filename has not yet been set.");

			try
			{
				// Open the file that is the first 
				// command line parameter
				FileInputStream fstream = new FileInputStream(_filename);
				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				// keep stacks...
				ArrayList<String> stackList = new ArrayList<String>();

				int sample = 0;
				int engine = 0;
				String row;
				while ((row = br.readLine()) != null)   
				{
					// Performing server sample: 500 iterations, 100ms interval, no registers
					if (row.startsWith("Performing server sample: "))
					{
						try
						{
							String sa[] = row.split("\\s+");
							_sampleIterations = Integer.parseInt(sa[3]);
							_sampleInterval   = Integer.parseInt(sa[5].replace("ms", ""));
							// get how many samples we consists of
						} 
						catch(RuntimeException rte) 
						{
							_logger.warn("Problem parsing: Iteration and Interval. row='"+row+"', Caught="+rte);
						}
					}
					else if (row.startsWith("---- Sample"))
					{
						// Reset and start new
						stackList.clear();

						try
						{
							// ---- Sample 1  Engine 0  ----
							String sa[] = row.split("\\s+");
							if ("Kernel".equals(sa[3]))
								continue;

							sample = Integer.parseInt(sa[2]);
							engine = Integer.parseInt(sa[4]);

						} 
						catch(RuntimeException rte) 
						{
							_logger.warn("Problem parsing: sample & engine number. row='"+row+"', Caught="+rte);
						}
					}
					else if (row.startsWith("******** End of stack trace"))
					{
						// Close a stack
						
						if (row.startsWith("******** End of stack trace, spid"))
							stackList.add("SPID");
						else if (row.startsWith("******** End of stack trace, kernel"))
							stackList.add("KERNEL");

						boolean addThis = true;
						if (sample < _startRead)               addThis = false;
						if (sample > _stopRead)                addThis = false;
						if (_engine >= 0 && _engine != engine) addThis = false;

						if (addThis)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("ADD SAMPLE: -- sample ="+sample+", engine="+engine+". _start="+_startRead+", _stop="+_stopRead+", _engine="+_engine+": stack="+stackList);

							Collections.reverse(stackList);
							addStackEntry(stackList);
						}

						stackList.clear();
					}
					else if (row.startsWith("pc: "))
					{
						// pc: 0x0000000080f980d0 _coldstart(0x0000

						int startPos = 23;

						int oxPos       = row.indexOf("0x", startPos)-1;
//						int firstPlus   = row.indexOf('+');
//						int firstParant = row.indexOf('(');
//						int first       = firstParant;
//						if (firstPlus >= 0 && firstPlus < firstParant)
//							first = firstPlus;

						int endPos   = oxPos;
						
						String f = "";
						// Add to current
						try 
						{
							f = row.substring(23, endPos);
						}
						catch (RuntimeException rte)
						{
							_logger.warn("Problem parsing: 'pc: ' entry. substring(startPos="+startPos+", endPos="+endPos+"), row='"+row+"', Caught="+rte);
						}

						if (f.length() > 0)
							stackList.add(f);
					}
				}
				in.close();
			}
			catch (Exception e)
			{
				_logger.error("Problems reading file: " + e.getMessage(), e);
				throw e;
			}		
		}


//		private void makeDummyTree()
//		{
//			ArrayList<String> stackList;
//			
//			stackList = new ArrayList<String>();
//			stackList.add("_coldstart");
//			stackList.add("a");
//			stackList.add("b");
//			stackList.add("c");
//			addStackEntry(stackList);
//
//			stackList = new ArrayList<String>();
//			stackList.add("_coldstart");
//			stackList.add("x");
//			stackList.add("y");
//			stackList.add("z");
//			addStackEntry(stackList);
//
//			stackList = new ArrayList<String>();
//			stackList.add("_coldstart");
//			stackList.add("x");
//			stackList.add("y");
//			stackList.add("q");
//			addStackEntry(stackList);
//
//			stackList = new ArrayList<String>();
//			stackList.add("xxx");
//			stackList.add("a");
//			stackList.add("b");
//			stackList.add("c");
//			addStackEntry(stackList);
//
//			stackList = new ArrayList<String>();
//			stackList.add("xxx");
//			stackList.add("a");
//			stackList.add("b");
//			stackList.add("c");
//			stackList.add("d");
//			addStackEntry(stackList);
//		}

//		public void printTree()
//		{
//			System.out.println("======= root ==================");
//			printTree(_root);
//		}
//		public void printTree(StackEntry se)
//		{
//			for (String name : se._childMap.keySet())
//			{
//				StackEntry c = se._childMap.get(name);
//				System.out.println(StringUtil.replicate("> ", c._depth) + name+":"+c._count);
//				
//				if (c.hasChildren())
//					printTree(c);
//			}
//		}

		public TreeModel createTreeModel()
		{
			DefaultTreeModel tm = new DefaultTreeModel(createTree());
			return tm;
		}
		public TreeNode createTree()
		{
			DefaultMutableTreeNode tnRoot = new DefaultMutableTreeNode(_root);
			createTreeNode(_root, tnRoot);
			return tnRoot;
		}
		private void createTreeNode(StackEntry se, DefaultMutableTreeNode tn)
		{
			for (String name : se._childMap.keySet())
			{
				StackEntry c = se._childMap.get(name);
				
				DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(c);
				tn.add(cnode);
				
				if (c.hasChildren())
					createTreeNode(c, cnode);
			}
		}

		/**
		 * Sort all child's, based on _count descending
		 */
		public void sort()
		{
			sort(_root);
		}
		private void sort(StackEntry se)
		{
			// do sorting on child map, based on _count
			se._childMap = CollectionUtils.sortByValuesDesc(se._childMap, false);

			// do sorting on discard map, if it contains more than one entry
			if (se._functionStackStartDiscardMap.size() > 1 )
				se._functionStackStartDiscardMap = CollectionUtils.sortByValuesDesc(se._functionStackStartDiscardMap, false);
			
			// Do sorting on any children
			for (String name : se._childMap.keySet())
			{
				StackEntry c = se._childMap.get(name);

				if (c.hasChildren())
					sort(c);
			}
		}

		private boolean matchFunctionFilter(String functionName)
		{
			if (_functionFilterType == FunctionFilterType.EXACT)
			{
				if (functionName.equals(_functionFilter))
					return true;
			}
			else if (_functionFilterType == FunctionFilterType.INDEXOF)
			{
				if (functionName.indexOf(_functionFilter) >= 0)
					return true;
			}
			else if (_functionFilterType == FunctionFilterType.REGEXP)
			{
				if (functionName.matches(_functionFilter))
					return true;
			}
			return false;
		}

		private void addStackEntry(ArrayList<String> stackList)
		{
			StackEntry insertionPoint = _root;

			// Check if the function exists within this StackTarce
			boolean functionFilterMatch = false;
			if ( StringUtil.hasValue(_functionFilter) )
			{
				for (String funcName : stackList)
				{
					if (matchFunctionFilter(funcName))
					{
						functionFilterMatch = true;
						break;
					}
				}
				if ( ! functionFilterMatch )
					return;
			}

			boolean addToTree            = true;
			boolean doFunctionStackStart = false;
			String  stackStartStr        = "";    // if "stack start" is enabled, remember the different stackPath that called this function
			if ( StringUtil.hasValue(_functionStackStart) )
			{
				addToTree            = false;
				doFunctionStackStart = true;
			}

			for (String funcName : stackList)
			{
				if ( ! _keepCppInstance )
				{
					// should we strip of any C++ stuff from the name
					// pc: 0x000000008041f8d8 _$o1cexoR0.s_execute+0x2f78(...)
					if (funcName.startsWith("_$"))
					{
						int firstDot = funcName.indexOf('.') + 1;
						if (firstDot > 0)
							funcName = funcName.substring(firstDot);
					}
				}

				if (doFunctionStackStart && !addToTree)
				{
					stackStartStr += funcName + " -> ";
					if (funcName.equals(_functionStackStart))
					{
						addToTree = true;

						// Increment the callers stack counter
						Integer discardCount = _root._functionStackStartDiscardMap.get(stackStartStr);
						if (discardCount == null)
							discardCount = new Integer(0);
						discardCount++;
						_root._functionStackStartDiscardMap.put(stackStartStr, discardCount);
					}
				}

				if (addToTree)
				{
					insertionPoint = insertionPoint.insertEntry(_root, funcName);
					if (functionFilterMatch && matchFunctionFilter(funcName))
						insertionPoint.setFilterFunctionMatch(true);
				}
			}
		}
	}


	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	/**
	 * Class that holds one "level" in the stack trace<br>
	 * The counter is incremented every time this specific level is executed or seen at a specific place in the stack trace 
	 * @author gorans
	 */
	private static class StackEntry
	implements Comparable<StackEntry>
	{
		String _name = "";
		int _depth = 0;
		int _count = 0;

		// What child functions has this function called
		Map<String, StackEntry> _childMap = new LinkedHashMap<String, StackEntry>();

		// When a function is used as a "stack start", then keep "discarded top" calls in this map
		// This is only available in the "root" object
		Map<String, Integer> _functionStackStartDiscardMap = new LinkedHashMap<String, Integer>();
		
		// Always points to root, for easy access, use in tool tip to reach _functionStackStartDiscardMap
		StackEntry _root = null;

		// Is used when FuctionMatching is used, true if this function matches the creteria
		// This so we can make the font "bold" in the tree view
		boolean _isFilterFunctionMatch = false;
		
		public StackEntry(StackEntry root, String name)
		{
			_root = root;
			_name = name;

			if ("root".equalsIgnoreCase(name))
				_root = this;
		}

		public boolean hasChildren()
		{
			return _childMap.size() > 0;
		}

		public void setFilterFunctionMatch(boolean match)
		{
			_isFilterFunctionMatch = match;
		}

		public StackEntry insertEntry(StackEntry root, String name)
		{
			StackEntry se = _childMap.get(name);
			if (se == null)
			{
				se = new StackEntry(root, name);
				_childMap.put(name, se);
			}
			se._count++;
			se._depth = this._depth + 1;
			
			return se;
		}

		/**
		 * This is used by the Tree View to print the content of this done.
		 */
		@Override
		public String toString()
		{
//			return _name + "(count=" + _count + ")";
//			return _name + ":" + _count;
			return "<html>" + 
				(_isFilterFunctionMatch ? "<b>" : "") +
				_name + 
				(_isFilterFunctionMatch ? "</b>" : "") +
				" : <b><font color=\"blue\">" + 
				_count + 
				"</font></b></html>";
		}

		@Override
		public int compareTo(StackEntry o)
		{
			int c = _count - o._count;
			if (c > 0) return 1;
			if (c < 0) return -1;
			return 0;
//			return _count - o._count;
		}
	}


	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	/**
	 * The graphical representation of the parsed sybmon stack trace file.
	 * @author gorans
	 */
	private static class AseStackTreeView
	extends JFrame
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;
		
		private static final String ACTION_DO_PARSE = "DO_PARSE";

		private AseStackTraceReader _str = null;

		private JCheckBox    _sampleFilter_chk       = new JCheckBox("Filter on Samples");
		private JLabel       _sampleStart_lbl        = new JLabel("Start Read at sample number");
		private JTextField   _sampleStart_txt        = new JTextField();
		private JLabel       _sampleStop_lbl         = new JLabel("Stop Read after sample number");
		private JTextField   _sampleStop_txt         = new JTextField();
		private JLabel       _sampleEngine_lbl       = new JLabel("Only Sample Engine Number");
		private JTextField   _sampleEngine_txt       = new JTextField();

		private JCheckBox    _functionFilter_chk     = new JCheckBox("Filter on function");
		private JTextField   _functionFilter_txt     = new JTextField();
		private JRadioButton _funcFilterExact_rb     = new JRadioButton("Exact",       false);
		private JRadioButton _funcFilterIndexOf_rb   = new JRadioButton("IndexOf",     true);
		private JRadioButton _funcFilterRegExp_rb    = new JRadioButton("Java RegExp", false);

		private JCheckBox    _functionStackStart_chk = new JCheckBox("Start Stack at function");
		private JTextField   _functionStackStart_txt = new JTextField();
		private JCheckBox    _keepCppInstance_chk    = new JCheckBox("Preserve C++ Instance");

		private JLabel       _tracefile_lbl          = new JLabel("sybmon file");
		private JTextField   _tracefile_txt          = new JTextField();
		private JButton      _tracefile_but          = new JButton("...");

		private JButton      _loadRefresh_but        = new JButton("Load/Refresh");
		private JLabel       _sampleCount_lbl        = new JLabel("Number of samples in file");
		private JTextField   _sampleCount_txt        = new JTextField();
		private JLabel       _sampleSleep_lbl        = new JLabel("Sleep time between samples");
		private JTextField   _sampleSleep_txt        = new JTextField();

		private JButton      _sybmonExample_but      = new JButton("Example Script to do Stack Trace");

		private JTree        _treeView               = null;

		private FocusableTip _panelFocusableTip        = null;
		private boolean      _usePanelFocusableTooltip = true;

		private FocusableTip _treeFocusableTip         = null;
		private boolean      _useTreeFocusableTooltip  = true;


		public AseStackTreeView(String stackfile)
		{
			super();
			if (stackfile != null)
			{
				_tracefile_txt.setText(stackfile);
			}
			init();
		}

		private void init()
		{
			setTitle("Simple ASE Stack Trace Viewer");

			setLayout(new BorderLayout());
			add(createTopPanel(),      BorderLayout.NORTH);
			add(createTreeViewPanel(), BorderLayout.CENTER);

			addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					saveProps();
					dispose();
				}
			});
			pack();
			loadProps();
			getSavedWindowProps();

//			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			setSize(1024, 768);
		}

		private JPanel createTopPanel()
		{
			//JPanel panel = SwingUtils.createPanel("Trace File Information", true);
			JPanel panel = new JPanel()
			{
				private static final long serialVersionUID = 1L;

				/**
				 * Returns the tool tip to display for a mouse event at the given location.  
				 * @param e The mouse event.
				 */
				@Override
				public String getToolTipText(MouseEvent e) 
				{
					String text = null;
					if (text == null) 
						text = super.getToolTipText(e);

					// Do we want to use "focusable" tips?
					if (_usePanelFocusableTooltip)
					{
						if (text != null) 
						{
							if (_panelFocusableTip == null) 
								_panelFocusableTip = new FocusableTip(this);

							_panelFocusableTip.toolTipRequested(e, text);
						}
						// No tooltip text at new location - hide tip window if one is currently visible
						else if (_panelFocusableTip != null) 
							_panelFocusableTip.possiblyDisposeOfTipWindow();
						return null;
					}
					return text; // Standard tool tips
				}
			};
			panel.setBorder(BorderFactory.createTitledBorder("Trace File Information"));
			panel.setLayout(new MigLayout("insets 0 0 0 0"));

			_sampleFilter_chk      .setToolTipText("<html>If you only want to read specific samples or engines</html>");
			_sampleStart_lbl       .setToolTipText("<html>First sample to start read from<br>First sample is 1<br>Default = 1</html>"); 
			_sampleStart_txt       .setToolTipText("<html>First sample to start read from<br>First sample is 1<br>Default = 1</html>");
			_sampleStop_lbl        .setToolTipText("<html>Last sample to read, if start=10, and end=10, only 1 sample will be read.<br>Default = read to the end, or Integer.MAX_VALUE</html>");
			_sampleStop_txt        .setToolTipText("<html>Last sample to read, if start=10, and end=10, only 1 sample will be read.<br>Default = read to the end, or Integer.MAX_VALUE</html>");
			_sampleEngine_lbl      .setToolTipText("<html>Only read stack traces for this engine number.<br>Engine start at 0<br>Default: read all engines.</html>"); 
			_sampleEngine_txt      .setToolTipText("<html>Only read stack traces for this engine number.<br>Engine start at 0<br>Default: read all engines.</html>");

			_functionFilter_chk    .setToolTipText("<html>Only read stack traces if it contains this function, or part of the function.<br>Java RegExp can be used.</html>"); 
			_functionFilter_txt    .setToolTipText("<html>Only read stack traces if it contains this function, or part of the function.<br>Java RegExp can be used.</html>");
			_funcFilterExact_rb    .setToolTipText("<html>Use <b>exact</b> matching: <code>funcname.equal(filterStr)</code></html>");
			_funcFilterIndexOf_rb  .setToolTipText("<html>Use <b>index</b> matching: <code>funcname.indexOf(filterStr)</code></html>");
			_funcFilterRegExp_rb   .setToolTipText("<html>Use <b>Java RegExp</b> matching: <code>funcname.matches(filterStr)</code></html>");
			_functionStackStart_chk.setToolTipText("<html>Use this <b>exact</b> function name as a root. The function calls \"above\" this function will not be displayed.</html>"); 
			_functionStackStart_txt.setToolTipText("<html>Use this <b>exact</b> function name as a root. The function calls \"above\" this function will not be displayed.</html>"); 
			_keepCppInstance_chk   .setToolTipText("<html>Strip off or Keep the C++ instance. If function looks like '_$o1cexoL0.kpsched', if strip=true, it fill become 'kpsched'</html>"); 

			_tracefile_lbl         .setToolTipText("<html>'sybmon' trace file to be read/parsed</html>"); 
			_tracefile_txt         .setToolTipText("<html>'sybmon' trace file to be read/parsed</html>");
			_tracefile_but         .setToolTipText("<html>Open a file browser and choose a file.</html>"); 

			_loadRefresh_but       .setToolTipText("<html>Load/Parse the selected file.</html>"); 

			_sampleCount_lbl       .setToolTipText("<html>How many Intervals did sybmon sample</html>"); 
			_sampleCount_txt       .setToolTipText("<html>How many Intervals did sybmon sample</html>"); 
			_sampleSleep_lbl       .setToolTipText("<html>How long time was it between the samples</html>"); 
			_sampleSleep_txt       .setToolTipText("<html>How long time was it between the samples</html>"); 

			_sybmonExample_but     .setToolTipText("<html>Show a script example of how to do stack trace using 'sybmon'.</html>");

			_sampleCount_txt.setEditable(false);
			_sampleSleep_txt.setEditable(false);

			ButtonGroup group = new ButtonGroup();
			group.add(_funcFilterExact_rb);
			group.add(_funcFilterIndexOf_rb);
			group.add(_funcFilterRegExp_rb);

			
			panel.add(_sampleFilter_chk,       "");
			panel.add(_sampleStart_lbl,        "span, split 6");
			panel.add(_sampleStart_txt,        "w 50");
			panel.add(_sampleStop_lbl,         "");
			panel.add(_sampleStop_txt,         "w 50");
			panel.add(_sampleEngine_lbl,       "");
			panel.add(_sampleEngine_txt,       "w 50, wrap");

			panel.add(_functionFilter_chk,     "");
			panel.add(_functionFilter_txt,     "split, push, grow");
			panel.add(_funcFilterExact_rb,     "");
			panel.add(_funcFilterIndexOf_rb,   "");
			panel.add(_funcFilterRegExp_rb,    "wrap");

			panel.add(_functionStackStart_chk, "");
			panel.add(_functionStackStart_txt, "push, grow, wrap");

			panel.add(_keepCppInstance_chk,    "wrap");

			panel.add(_tracefile_lbl,          "");
			panel.add(_tracefile_txt,          "split, push, grow");
			panel.add(_tracefile_but,          "wrap");

			panel.add(_loadRefresh_but,        "");
//			panel.add(new JLabel(),            "split, push, grow");
			panel.add(_sampleCount_lbl,        "split");
			panel.add(_sampleCount_txt,        "w 50");
			panel.add(_sampleSleep_lbl,        "");
			panel.add(_sampleSleep_txt,        "w 50");
			panel.add(new JLabel(""),          "push, grow");
			panel.add(_sybmonExample_but,      "wrap");

			// set action
			_loadRefresh_but       .setActionCommand(ACTION_DO_PARSE);
			_sampleStart_txt       .setActionCommand(ACTION_DO_PARSE);
			_sampleStop_txt        .setActionCommand(ACTION_DO_PARSE);
			_sampleEngine_txt      .setActionCommand(ACTION_DO_PARSE);
			_functionFilter_txt    .setActionCommand(ACTION_DO_PARSE);
			_functionStackStart_txt.setActionCommand(ACTION_DO_PARSE);
			_tracefile_txt         .setActionCommand(ACTION_DO_PARSE);

			// Add action listener
			_tracefile_but         .addActionListener(this);
			_loadRefresh_but       .addActionListener(this);
			_sampleStart_txt       .addActionListener(this);
			_sampleStop_txt        .addActionListener(this);
			_sampleEngine_txt      .addActionListener(this);
			_functionFilter_txt    .addActionListener(this);
			_functionStackStart_txt.addActionListener(this);
			_tracefile_txt         .addActionListener(this);
			_sybmonExample_but     .addActionListener(this);

			// Focus action listener

			return panel;
			
		}

		private JPanel createTreeViewPanel()
		{
			JPanel panel = SwingUtils.createPanel("Stack Trace Tree View", false);
			panel.setLayout(new MigLayout("insets 0 0 0 0"));

			_treeView = createTreeview();
			panel.add(new JScrollPane(_treeView), "push, grow, wrap");

			if (_str == null)
				_treeView.setModel(createTreeModelEmpty());
			else
				_treeView.setModel(_str.createTreeModel());

			// Add action listener

			// Focus action listener

			return panel;
			
		}

		/**
		 * Create an empty tree model
		 * @return
		 */
		private TreeModel createTreeModelEmpty()
		{
			DefaultMutableTreeNode tnRoot = new DefaultMutableTreeNode("Empty, no file has yet been read/parsed");
			DefaultTreeModel tm = new DefaultTreeModel(tnRoot);
			return tm;
		}

		/**
		 * Create a JTree, this one extends the tooltip for example
		 * @return
		 */
		private JTree createTreeview()
		{
			JTree tree = new JTree()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getToolTipText(MouseEvent event)
				{
					String tip = null;

					if ( event != null )
					{
						Point p = event.getPoint();
						int selRow = getRowForLocation(p.x, p.y);
						TreeCellRenderer r = getCellRenderer();

						if ( selRow != -1 && r != null )
						{
							TreePath path = getPathForRow(selRow);
							Object lastPath = path.getLastPathComponent();

							if ( lastPath instanceof DefaultMutableTreeNode)
							{
								DefaultMutableTreeNode tn = (DefaultMutableTreeNode) lastPath;
								lastPath = tn.getUserObject();
							}

							if ( lastPath instanceof StackEntry )
							{
								StackEntry se = (StackEntry) lastPath;
								tip = se._name;
								if (_functionStackStart_chk.isSelected())
								{
									tip = "<html>Stack Traces Start:" + se._name+"<br>";
									tip += "Callers of this function, which are filtered out:" + se._name + "<br>";
									tip += "Number of callers: "+se._root._functionStackStartDiscardMap.size()+"<br>";
									tip += "<br>";
									for (Map.Entry<String,Integer> entry : se._root._functionStackStartDiscardMap.entrySet()) 
									{
										String key = entry.getKey();
										int    val = entry.getValue();

										tip += "Parent Stack='"+key+"', count=<b><font color=\"blue\">" + val + "</font></b><br>";
									}
									tip += "</html>";
								}
							}
						}
					}
					// No tip from the renderer get our own tip
					if ( tip == null )
						tip = super.getToolTipText(event);

					
					// Do we want to use "focusable" tips?
					if (_useTreeFocusableTooltip)
					{
						if (tip != null) 
						{
							if (_treeFocusableTip == null) 
								_treeFocusableTip = new FocusableTip(this);

							_treeFocusableTip.toolTipRequested(event, tip);
						}
						// No tooltip text at new location - hide tip window if one is currently visible
						else if (_treeFocusableTip != null) 
							_treeFocusableTip.possiblyDisposeOfTipWindow();
						return null;
					}
					return tip; // Standard tool tips
				}
			};
			ToolTipManager.sharedInstance().registerComponent(tree);
			return tree;
		}

		/**
		 * Set a new parsed Stack Trace Reader and show the Tree
		 * @param str
		 */
		public void setStackTraceReader(AseStackTraceReader str)
		{
			_str = str;
			if (_str != null)
			{
				_sampleCount_txt.setText(_str.getSampleIterations()+"");
				_sampleSleep_txt.setText(_str.getSampleInterval()+"");

				_treeView.setModel(_str.createTreeModel());
			}
		}

		/**
		 * Actions in the GUI
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			String action = e.getActionCommand();

			// BUT: trace file "..."
			if (_tracefile_but.equals(source))
			{
				String baseDir = _tracefile_txt.getText();
				JFileChooser fc = new JFileChooser(baseDir);

			//	fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int returnVal = fc.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					String newFile = fc.getSelectedFile().toString().replace('\\', '/');
					_tracefile_txt.setText(newFile);
				}
			}

			// BUT: Example of SYBMON script
			if (_sybmonExample_but.equals(source))
			{
				showSybmonExampleScript();
			}
			
			// BUT: load trace file
//			if (_loadRefresh_but.equals(source))
			if (ACTION_DO_PARSE.equals(action))
			{
				final AseStackTraceReader str = new AseStackTraceReader();

				if (_sampleFilter_chk.isSelected())
				{
					if (StringUtil.hasValue(_sampleStart_txt.getText()))
						str.setStartRead(Integer.parseInt(_sampleStart_txt.getText()));
	
					if (StringUtil.hasValue(_sampleStop_txt.getText()))
						str.setStopRead(Integer.parseInt(_sampleStop_txt.getText()));
	
					if (StringUtil.hasValue(_sampleEngine_txt.getText()))
						str.setEngine(Integer.parseInt(_sampleEngine_txt.getText()));
				}

				if (_functionFilter_chk.isSelected())
				{
					str.setFunctionFilter(_functionFilter_txt.getText());
					
					FunctionFilterType type = FunctionFilterType.INDEXOF;
					if (_funcFilterExact_rb  .isSelected()) type = FunctionFilterType.EXACT;
					if (_funcFilterIndexOf_rb.isSelected()) type = FunctionFilterType.INDEXOF;
					if (_funcFilterRegExp_rb .isSelected()) type = FunctionFilterType.REGEXP;
					str.setFunctionFilterType(type);
				}

				if (_functionStackStart_chk.isSelected())
					str.setFunctionStackStart(_functionStackStart_txt.getText());

				str.setKeepCppInstance(_keepCppInstance_chk.isSelected());

				str.setFile(_tracefile_txt.getText());

				WaitForExecDialog waitfor = new WaitForExecDialog(this, "Reading/parsing the \"sybmon\" StackTrace file.");
				WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(waitfor)
				{
					@Override
					public Object doWork()
					{
						try
						{
							str.readFile();
							str.sort();
						}
						catch (Exception e)
						{
							String msg = 
								"<html>" +
								"Problems when reading/parsing file<br>" +
								"Filename '"+str.getFile()+"'.<br>" +
								"<br>" +
								"Problem: " + e.getMessage() +
								"</html>";
							SwingUtils.showErrorMessage("Problems Parsing File", msg, e);
						}
						return null;
					}
				};
				waitfor.execAndWait(doWork);

				setStackTraceReader(str);
			}
		}

		
		private void showSybmonExampleScript()
		{
			String exampleStr =
				"#!/bin/ksh\n" +
				"\n" +
				"#--- Setup environment if this is not done from the caller of the script \n" +
				"#export DSQUERY=ase_server_name \n" +
				"#export SYBASE=/opt/sybase15 \n" +
				"#export SYBASE_ASE=ASE-15_0 \n" +
				"#export SYBASE_OCS=OCS-15_0 \n" +
				" \n" +
				"#--- Setup LD_LIBRARY_PATH (Solaris, Linux), SHLIB (hp), LIBPATH (aix),  \n" + 
				"#export LD_LIBRARY_PATH=$SYBASE/$SYBASE_ASE/bin:$SYBASE/$SYBASE_ASE/lib:$LD_LIBRARY_PATH \n" +
				"#export LD_LIBRARY_PATH=$SYBASE/$SYBASE_OCS/bin:$SYBASE/$SYBASE_OCS/lib:$SYBASE/$SYBASE_OCS/lib3p64:$LD_LIBRARY_PATH \n" +
				" \n" +
				" \n" +
				"echo 'Starting' \n" +
				" \n" +
				"dttm=$(date +%Y%m%d_%H%M%S) \n" +
				" \n" +
				"$SYBASE/$SYBASE_ASE/bin/dataserver -X -Pquine 2>/dev/null << SYBMON \n" +
				"cat $SYBASE/$SYBASE_ASE \n" +
				"attach $DSQUERY \n" +
				"log on sample_${dttm}.out \n" +
				"set display off \n" +
				"sample count=500 interval=100 context=y \n" +
				"log close \n" +
				"quit \n" +
				"SYBMON \n" +
				" \n" +
				"if [ ! -f sample_${dttm}.out ] \n" +
				"then \n" +
				"	echo 'An error occured. The file has not been created' \n" +
				"	exit -1 \n" +
				"fi \n" +
				"";

			// Top panel
			JPanel top = SwingUtils.createPanel("Script example", false);
			top.setLayout(new MigLayout());
			top.add(new JLabel("Below is an example script of how to use 'sybmon' to generate X number of Stack Traces for a ASE Server"), "wrap");
			top.add(new JLabel("Change 'count=500', to any value depending on how many stack traces you want."), "wrap 15");
			top.add(new JLabel("Copy and Paste the following into a Unix/Linux shell script, and execute it with the user 'sybase' or whoever is running ASE."), "wrap");

			// The Example text panel
			RSyntaxTextArea example_txt    = new RSyntaxTextArea();
			RTextScrollPane example_scroll = new RTextScrollPane(example_txt);
			example_txt   .setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
			example_txt   .setText(exampleStr);
			example_scroll.setLineNumbersEnabled(true);

			// Bottom panel
			JPanel bottom = SwingUtils.createPanel("", false);
			JButton close = new JButton("Close");
			bottom.setLayout(new MigLayout());
			bottom.add(new JLabel(""), "push, grow");
			bottom.add(close,          "wrap");

			// Create a dialog window
			final JDialog dialog = new JDialog(this);
			dialog.setLayout(new BorderLayout());
			dialog.add(top,            BorderLayout.NORTH);
			dialog.add(example_scroll, BorderLayout.CENTER);
			dialog.add(bottom,         BorderLayout.SOUTH);

			RSyntaxUtilitiesX.installRightClickMenuExtentions(example_scroll, this);

			// Add action to close
			close.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					dialog.setVisible(false);
				}
			});

			// Show the window
			dialog.pack();
			dialog.setVisible(true);
		}

		/*---------------------------------------------------
		** BEGIN: implementing saveProps & loadProps
		**---------------------------------------------------
		*/	
		private void saveProps()
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
			{
				_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
				return;
			}
			FunctionFilterType filterType = FunctionFilterType.INDEXOF;
			if (_funcFilterExact_rb  .isSelected()) filterType = FunctionFilterType.EXACT;
			if (_funcFilterIndexOf_rb.isSelected()) filterType = FunctionFilterType.INDEXOF;
			if (_funcFilterRegExp_rb .isSelected()) filterType = FunctionFilterType.REGEXP;

			//----------------------------------
			// Settings
			//----------------------------------
			conf.setProperty("AseStackTraceAnalyzer.info.sample.filter.isSelected",      _sampleFilter_chk.isSelected());
			conf.setProperty("AseStackTraceAnalyzer.info.sample.start",                  _sampleStart_txt .getText());
			conf.setProperty("AseStackTraceAnalyzer.info.sample.stop",                   _sampleStop_txt  .getText());        
			conf.setProperty("AseStackTraceAnalyzer.info.sample.engine",                 _sampleEngine_txt.getText());

			conf.setProperty("AseStackTraceAnalyzer.info.functionFiler.isSelected",      _functionFilter_chk.isSelected());
			conf.setProperty("AseStackTraceAnalyzer.info.functionFiler.text",            _functionFilter_txt.getText());
			conf.setProperty("AseStackTraceAnalyzer.info.functionFiler.type",            filterType.toString());
			conf.setProperty("AseStackTraceAnalyzer.info.functionStackStart.isSelected", _functionStackStart_chk.isSelected());
			conf.setProperty("AseStackTraceAnalyzer.info.functionStackStart.text",       _functionStackStart_txt.getText());
			conf.setProperty("AseStackTraceAnalyzer.info.keepCppInstance.isSelected",    _keepCppInstance_chk.isSelected());

			conf.setProperty("AseStackTraceAnalyzer.info.tracefile.text",                _tracefile_txt.getText());

			//------------------
			// WINDOW
			//------------------
			conf.setProperty("AseStackTraceAnalyzer.dialog.window.width",  this.getSize().width);
			conf.setProperty("AseStackTraceAnalyzer.dialog.window.height", this.getSize().height);
			conf.setProperty("AseStackTraceAnalyzer.dialog.window.pos.x",  this.getLocationOnScreen().x);
			conf.setProperty("AseStackTraceAnalyzer.dialog.window.pos.y",  this.getLocationOnScreen().y);

			conf.save();
		}

		private void loadProps()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			if (conf == null)
			{
				_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
				return;
			}

			//----------------------------------
			// Settings
			//----------------------------------
			_sampleFilter_chk.      setSelected(conf.getBooleanProperty("AseStackTraceAnalyzer.info.sample.filter.isSelected",      _sampleFilter_chk.isSelected()));
			_sampleStart_txt.           setText(conf.getProperty       ("AseStackTraceAnalyzer.info.sample.start",                  _sampleStart_txt .getText()));
			_sampleStop_txt.            setText(conf.getProperty       ("AseStackTraceAnalyzer.info.sample.stop",                   _sampleStop_txt  .getText()));        
			_sampleEngine_txt.          setText(conf.getProperty       ("AseStackTraceAnalyzer.info.sample.engine",                 _sampleEngine_txt.getText()));

			_functionFilter_chk.    setSelected(conf.getBooleanProperty("AseStackTraceAnalyzer.info.functionFiler.isSelected",      _functionFilter_chk    .isSelected()));
			_functionFilter_txt.        setText(conf.getProperty       ("AseStackTraceAnalyzer.info.functionFiler.text",            _functionFilter_txt    .getText()));
			_functionStackStart_chk.setSelected(conf.getBooleanProperty("AseStackTraceAnalyzer.info.functionStackStart.isSelected", _functionStackStart_chk.isSelected()));
			_functionStackStart_txt.    setText(conf.getProperty       ("AseStackTraceAnalyzer.info.functionStackStart.text",       _functionStackStart_txt.getText()));
			_keepCppInstance_chk.   setSelected(conf.getBooleanProperty("AseStackTraceAnalyzer.info.keepCppInstance.isSelected",    _keepCppInstance_chk   .isSelected()));

			// If nothing has been passed in the constructor
			if (StringUtil.isNullOrBlank(_tracefile_txt.getText()))
					_tracefile_txt     .setText(conf.getProperty(       "AseStackTraceAnalyzer.info.tracefile.text",                _tracefile_txt         .getText()));

			// FunctionFilterType
			String str = conf.getProperty("AseStackTraceAnalyzer.info.functionFiler.type");
			if (str != null)
			{
				FunctionFilterType filterType = FunctionFilterType.valueOf(str);
				if (filterType == FunctionFilterType.EXACT)   _funcFilterExact_rb  .setSelected(true);
				if (filterType == FunctionFilterType.INDEXOF) _funcFilterIndexOf_rb.setSelected(true);
				if (filterType == FunctionFilterType.REGEXP)  _funcFilterRegExp_rb .setSelected(true);
			}

		}
		private void getSavedWindowProps()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			if (conf == null)
			{
				_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
				return;
			}
			//----------------------------------
			// TAB: Offline
			//----------------------------------
			int width  = conf.getIntProperty("AseStackTraceAnalyzer.dialog.window.width",  1024);
			int height = conf.getIntProperty("AseStackTraceAnalyzer.dialog.window.height", 768);
			int x      = conf.getIntProperty("AseStackTraceAnalyzer.dialog.window.pos.x",  -1);
			int y      = conf.getIntProperty("AseStackTraceAnalyzer.dialog.window.pos.y",  -1);
			if (width != -1 && height != -1)
			{
				this.setSize(width, height);
			}
			if (x != -1 && y != -1)
			{
				if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
					this.setLocation(x, y);
			}
		}
		/*---------------------------------------------------
		** END: implementing saveProps & loadProps
		**---------------------------------------------------
		*/	
	}
	
	
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// MAIN CODE
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	public static void main(String[] args)
	{
		System.out.println("Usage: [sybmonStackFile]");

		String stackfile = null;
		
		if (args.length > 0) 
			stackfile = args[0];

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf = new Configuration(System.getProperty("user.home") + File.separator + "aseStackTraceAnalyzer.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf);
		
		Configuration.setSearchOrder(Configuration.USER_TEMP);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
		}

		
		AseStackTreeView view = new AseStackTreeView(stackfile);
		view.setVisible(true);

	}
}
