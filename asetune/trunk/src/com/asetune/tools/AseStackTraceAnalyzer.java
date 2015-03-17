package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
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
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.GTable;
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
	
	/** Different ways to be used when filtering on function names */
	public enum SampleHighlightType {KPID, SPID, SUID};
	
	/**
	 * Class that reads/parser a sybmon stack trace file
	 * @author gorans
	 */
	private static class AseStackTraceReader
	{
		private String  _filename           = null;
		private int     _sampleIterations   = 0;
		private int     _sampleInterval     = 0;

		private int     _actualSamples      = 0;
		private int     _actualSampleSlots  = 0;
		private int     _engineCount        = 0;

		private int     _startRead          = 0;
		private int     _stopRead           = Integer.MAX_VALUE;
		private int     _engine             = -1;

		private String  _functionFilter     = null;
		private String  _functionStackStart = null;
		private boolean _keepCppInstance    = false;
		private FunctionFilterType _functionFilterType = FunctionFilterType.INDEXOF;

		private StackEntry _root            = new StackEntry(null, "root");


		public int    getSampleIterations()    { return _sampleIterations; }
		public int    getSampleInterval()      { return _sampleInterval; }
		public String getFile()                { return _filename; }

		public int    getActualSamples()       { return _actualSamples; }
		public int    getActualSampleSlots()   { return _actualSampleSlots; }
		public int    getExpectedSampleSlots() { return getSampleIterations() * getEngineCount(); }
		public int    getEngineCount()         { return _engineCount; }
		
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
				ArrayList<String>     stackList     = new ArrayList<String>();
				ArrayList<ObjectInfo> objectList    = new ArrayList<ObjectInfo>();
				ArrayList<String>     sampleContent = new ArrayList<String>();

				int    sample = 0;
				int    engine = 0;
				int    sampleStartRow = 0;
				int    rowNum = 0;
				String row;
				while ((row = br.readLine()) != null)   
				{
					rowNum++;
					sampleContent.add(row);

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
						// Remove last entry from the sampleContent
						sampleContent.remove( sampleContent.size() - 1 );

						// Reset and start new
						stackList     = new ArrayList<String>();
						objectList    = new ArrayList<ObjectInfo>();
						sampleContent = new ArrayList<String>();
						sampleStartRow = rowNum;

						// add current row after the reset...
						sampleContent.add(row);

						try
						{
							// ---- Sample 1  Engine 0  ----
							String sa[] = row.split("\\s+");
							if ("Kernel".equals(sa[3]))
								continue;

							sample = Integer.parseInt(sa[2]);
							engine = Integer.parseInt(sa[4]);

							if (sample > _actualSamples)
								_actualSamples = sample;
							_actualSampleSlots++;
						} 
						catch(RuntimeException rte) 
						{
							_logger.warn("Problem parsing: sample & engine number. row='"+row+"', Caught="+rte);
						}
					}
					else if (row.indexOf(" is offline and will not be sampled") >= 0)
					{
						// subtract offline engines from the "number of slots" counter
						_actualSampleSlots--;
					}
					else if (row.startsWith("******** End of stack trace"))
					{
						//******** End of stack trace, spid 2519, kpid 1275595420, suid 2127
						//******** End of stack trace, kernel service process: kpid 1179666
						int pos;
						int kpid = -1;
						int spid = -1;
						int suid = -1;

						// Close a stack
						if (row.startsWith("******** End of stack trace, spid"))
						{
							stackList.add("SPID");

							// get spid
							pos = row.indexOf(" spid ");
							if (pos >= 0)
							{
								pos += " spid ".length();
								try { spid = Integer.parseInt( row.substring(pos, row.indexOf(",", pos)) ); }
								catch(NumberFormatException nfe) {System.out.println("Parse-spid SPID '"+row+"', caught: "+nfe);}
							}

							// get kpid
							pos = row.indexOf(" kpid ");
							if (pos >= 0)
							{
								pos += " kpid ".length();
								try { kpid = Integer.parseInt( row.substring(pos, row.indexOf(",", pos)) ); }
								catch(NumberFormatException nfe) {System.out.println("Parse-spid KPID '"+row+"', caught: "+nfe);}
							}

							// get suid
							pos = row.indexOf(" suid ");
							if (pos >= 0)
							{
								pos += " suid ".length();
								try { suid = Integer.parseInt( row.substring(pos) ); }
								catch(NumberFormatException nfe) {System.out.println("Parse-spid SUID '"+row+"', caught: "+nfe);}
							}
						}
						else if (row.startsWith("******** End of stack trace, kernel"))
						{
							stackList.add("KERNEL");

							// get kpid
							pos = row.indexOf(" kpid ");
							if (pos >= 0)
							{
								pos += " kpid ".length();
								try { kpid = Integer.parseInt( row.substring(pos) ); }
								catch(NumberFormatException nfe) {System.out.println("Parse-kpid KPID '"+row+"', caught: "+nfe);}
							}
						}

						boolean addThis = true;
						if (sample < _startRead)               addThis = false;
						if (sample > _stopRead)                addThis = false;
						if (_engine >= 0 && _engine != engine) addThis = false;

						_engineCount = Math.max(_engineCount, engine + 1); // engine number starts at 0, so lets add 1 here 

						if (addThis)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("ADD SAMPLE: -- sample ="+sample+", engine="+engine+". _start="+_startRead+", _stop="+_stopRead+", _engine="+_engine+": stack="+stackList);

							Collections.reverse(stackList);
							addStackEntry(stackList, sample, engine, kpid, spid, suid, sampleStartRow, rowNum, objectList, sampleContent);
						}

//						stackList     = new ArrayList<String>();
//						objectList    = new ArrayList<ObjectInfo>();
//						sampleContent = new ArrayList<String>();
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
//					else if (row.startsWith("-- dbid: ")) // parse for used table names (available using option: context=y)
					else if (row.indexOf("dbid: ") >= 0) // parse for used table names (available using option: context=y)
					{
						// /home/sybase/gorans [1094]# cat stacktrace_Y160_20140505_153529.out | grep '\-\- dbid:'
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x155433048, des: 0x0x154ac0270
						// -- dbid: 1, objid: 4, name: 'systypes', sdes: 0x0x1554336a0, des: 0x0x154b59af0
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x155433cf8, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x155434350, des: 0x0x154ac0270
						// -- dbid: 1, objid: 329049177, name: 'spt_mda', sdes: 0x0x155398228, des: 0x0x154b24428
						// -- dbid: 2, objid: -1179650, name: 'temp worktable', sdes: 0x0x155398880, des: 0x0x159217800
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b71c8, des: 0x0x154ac0270
						// -- dbid: 1, objid: 4, name: 'systypes', sdes: 0x0x1553b7820, des: 0x0x154b59af0
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b7e78, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b84d0, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b8b28, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b9180, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b97d8, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553b9e30, des: 0x0x154ac0270
						// -- dbid: 2, objid: -1245192, name: 'temp worktable', sdes: 0x0x1553ba488, des: 0x0x15921c000
						// -- dbid: 2, objid: -1441793, name: 'temp worktable', sdes: 0x0x1554140a8, des: 0x0x15924e748
						// -- dbid: 2, objid: -1441796, name: 'temp worktable', sdes: 0x0x155414700, des: 0x0x1591d2e78
						// -- dbid: 2, objid: -1441794, name: 'temp worktable', sdes: 0x0x155414d58, des: 0x0x159195000
						// -- dbid: 2, objid: -1441798, name: 'temp worktable', sdes: 0x0x1554153b0, des: 0x0x159250e90
						// -- dbid: 2, objid: -1441797, name: 'temp worktable', sdes: 0x0x155416060, des: 0x0x1591fc920
						// -- dbid: 2, objid: -1441795, name: 'temp worktable', sdes: 0x0x1554166b8, des: 0x0x1583d9e90
						// -- dbid: 2, objid: -1441800, name: 'temp worktable', sdes: 0x0x155417368, des: 0x0x15923f000
						// -- dbid: 2, objid: -1441801, name: 'temp worktable', sdes: 0x0x1554179c0, des: 0x0x15921c7c8
						// -- dbid: 1, objid: 33, name: 'syslogins', sdes: 0x0x1553a8768, des: 0x0x154b4e4c8
						// -- dbid: 1, objid: 49, name: 'sysloginroles', sdes: 0x0x1553a8dc0, des: 0x0x154b54948
						// -- dbid: 1, objid: 21, name: 'sysattributes', sdes: 0x0x1553a9418, des: 0x0x154b605b8
						// -- dbid: 1, objid: 329049177, name: 'spt_mda', sdes: 0x0x155398228, des: 0x0x154b24428
						// -- dbid: 2, objid: -1179650, name: 'temp worktable', sdes: 0x0x155398880, des: 0x0x157cb6000
						// -- dbid: 2, objid: -1441793, name: 'temp worktable', sdes: 0x0x1554140a8, des: 0x0x159215000
						// -- dbid: 1, objid: 33, name: 'syslogins', sdes: 0x0x1554814c8, des: 0x0x154b4e4c8
						// -- dbid: 1, objid: 329049177, name: 'spt_mda', sdes: 0x0x155398228, des: 0x0x154b24428
						// -- dbid: 2, objid: -1179650, name: 'temp worktable', sdes: 0x0x155398880, des: 0x0x1591db000
						// -- dbid: 1, objid: 329049177, name: 'spt_mda', sdes: 0x0x155549ce8, des: 0x0x154b24428
						// -- dbid: 2, objid: -2097154, name: 'temp worktable', sdes: 0x0x15554a340, des: 0x0x159215000
						// -- dbid: 1, objid: 33, name: 'syslogins', sdes: 0x0x1554bf408, des: 0x0x154b4e4c8
						// -- dbid: 1, objid: 33, name: 'syslogins', sdes: 0x0x15551c2e8, des: 0x0x154b4e4c8
						// -- dbid: 1, objid: 49, name: 'sysloginroles', sdes: 0x0x15551c940, des: 0x0x154b54948
						// -- dbid: 1, objid: 48, name: 'syssrvroles', sdes: 0x0x15551cf98, des: 0x0x154b54300
						// -- dbid: 1, objid: 33, name: 'syslogins', sdes: 0x0x1553c7708, des: 0x0x154b4e4c8
						// -- dbid: 1, objid: 49, name: 'sysloginroles', sdes: 0x0x1553c7d60, des: 0x0x154b54948
						// -- dbid: 1, objid: 48, name: 'syssrvroles', sdes: 0x0x1553c83b8, des: 0x0x154b54300
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d6168, des: 0x0x154ac0270
						// -- dbid: 1, objid: 4, name: 'systypes', sdes: 0x0x1553d67c0, des: 0x0x154b59af0
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d6e18, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d7470, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d7ac8, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d8120, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d8778, des: 0x0x154ac0270
						// -- dbid: 31514, objid: 546097955, name: 'spt_jdbc_datatype_info', sdes: 0x0x1553d8dd0, des: 0x0x154ac0270
						// -- dbid: 2, objid: -1310728, name: 'temp worktable', sdes: 0x0x1553d9428, des: 0x0x159222800
						String searchStr;
						String endStr;
						int    pos;
						int    dbid  = -1;
						int    objid = -1;
						String name  = null;
						String sdes  = null;

						searchStr = "dbid: ";
						endStr    = ",";
						pos = row.indexOf(searchStr);
						if (pos >= 0)
						{
							pos += searchStr.length();
							try { dbid = Integer.parseInt( row.substring(pos, row.indexOf(endStr, pos)) ); }
							catch(RuntimeException rte) {_logger.warn("Problems Parse-object(dbid) '"+row+"', caught: "+rte);}
						}

						searchStr = " objid: ";
						endStr    = ",";
						pos = row.indexOf(searchStr);
						if (pos >= 0)
						{
							pos += searchStr.length();
							try { objid = Integer.parseInt( row.substring(pos, row.indexOf(endStr, pos)) ); }
							catch(RuntimeException rte) {_logger.warn("Problems Parse-object(objid) '"+row+"', caught: "+rte);}
						}

//						searchStr = " name: '";
//						endStr    = "',";
						searchStr = " name: ";
						endStr    = ",";
						pos = row.indexOf(searchStr);
						if (pos >= 0)
						{
							pos += searchStr.length();
							try { 
								name = row.substring(pos, row.indexOf(endStr, pos));
								if (name.startsWith("'"))
									name = name.substring(1);
								if (name.endsWith("'"))
									name = name.substring(0, name.length()-1);
							}
							catch(RuntimeException rte) {_logger.warn("Problems Parse-object(name) '"+row+"', caught: "+rte);}
						}

						searchStr = " sdes: ";
						endStr    = ",";
						pos = row.indexOf(searchStr);
						if (pos >= 0)
						{
							pos += searchStr.length();
							try { sdes = row.substring(pos, row.indexOf(endStr, pos)); }
							catch(RuntimeException rte) {_logger.warn("Problems Parse-object(sdes) '"+row+"', caught: "+rte);}
						}

//						if (dbid != -1 && objid != -1 && name != null)
						if (name != null)
						{
							ObjectInfo oi = new ObjectInfo(dbid, objid, name, sdes);
							objectList.add(oi);
						}
					}
				}
				in.close();
				
				// Set count at root level (grab count at first level)
				int rootLevelCount = 0;
				for (String key : _root._childMap.keySet())
					rootLevelCount += _root._childMap.get(key)._count;
				_root._count = rootLevelCount;
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
//			se._childMap = CollectionUtils.sortByValuesDesc(se._childMap, false);
			se._childMap = CollectionUtils.sortByMapValue(se._childMap, false);

			// do sorting on discard map, if it contains more than one entry
//			if (se._functionStackStartDiscardMap.size() > 1 )
//				se._functionStackStartDiscardMap = CollectionUtils.sortByValuesDesc(se._functionStackStartDiscardMap, false);
			if (se._functionStackStartDiscardMap.size() > 1 )
				se._functionStackStartDiscardMap = CollectionUtils.sortByMapValue(se._functionStackStartDiscardMap, false);
			
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

		private void addStackEntry(ArrayList<String> stackList, int sample, int engine, int kpid, int spid, int suid, int sampleStartRow, int sampleEndRow, ArrayList<ObjectInfo> objectList, ArrayList<String> sampleContent)
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
			String  stackStartStr        = "";
			String  stackStartDiscardStr = "";    // if "stack start" is enabled, remember the different stackPath that called this function
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
					stackStartDiscardStr += funcName + " -> ";
					if (funcName.equals(_functionStackStart))
					{
						addToTree = true;

						// Increment the callers stack counter
						Integer discardCount = _root._functionStackStartDiscardMap.get(stackStartDiscardStr);
						if (discardCount == null)
							discardCount = new Integer(0);
						discardCount++;
						_root._functionStackStartDiscardMap.put(stackStartDiscardStr, discardCount);
					}
				}

				if (addToTree)
				{
					stackStartStr += funcName;

					insertionPoint = insertionPoint.insertEntry(_root, funcName, stackStartStr, stackList, sample, engine, kpid, spid, suid, sampleStartRow, sampleEndRow, objectList, sampleContent);
					if (functionFilterMatch && matchFunctionFilter(funcName))
						insertionPoint.setFilterFunctionMatch(true);

					stackStartStr += " -> "; // add ' -> ' at the end, for next append...
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static class ObjectInfo
	{
		int     _dbid;
		int     _objid;
		String  _name;
		String  _sdes;
		boolean _isTempWorktable;
		
		public ObjectInfo(int dbid, int objectid, String name, String sdes)
		{
			_dbid  = dbid;
			_objid = objectid;
			_name  = name;
			_sdes  = sdes;
			
			// Remove first 0x if there are duplicates
			if (_sdes != null && _sdes.startsWith("0x0x"))
				_sdes = _sdes.substring(2);
			
			_isTempWorktable = "temp worktable".equals(_name);
		}
		
		@Override
		public String toString()
		{
//			return _dbid + ":" + _objid + ":" + _name;
			return _name + "(" + _dbid + ":" + _objid + ")";
		}
	}

	@SuppressWarnings("unused")
	private static class SampleEngineDetailes
	{
		int    _sample;
		int    _engine;
		int    _kpid;
		int    _spid;
		int    _suid;
		int    _sampleStartRow;
		int    _sampleEndRow;
		String _stackStartStr;
		ArrayList<String>     _stackList;
		ArrayList<ObjectInfo> _objectList;
		ArrayList<String>     _sampleContent;

		public String getKey()
		{
			return _sample + ":" + _engine;
		}
		public static String getKey(int sample, int engine)
		{
			return sample + ":" + engine;
		}

		public SampleEngineDetailes(int sample, int engine, int kpid, int spid, int suid, int sampleStartRow, int sampleEndRow, String stackStartStr, ArrayList<String> stackList, ArrayList<ObjectInfo> objectList, ArrayList<String> sampleContent)
		{
			_sample         = sample;
			_engine         = engine;
			_kpid           = kpid;
			_spid           = spid;
			_suid           = suid;
			_sampleStartRow = sampleStartRow;
			_sampleEndRow   = sampleEndRow;
			_stackStartStr  = stackStartStr;
			_stackList      = stackList;
			_objectList     = objectList;
			_sampleContent  = sampleContent;
		}
		
		public String getFullCallStack()
		{
			String stackPath = "";
			for (String fname : _stackList)
				stackPath += fname + " -> ";

			// Remove last " -> "
			if (stackPath.endsWith(" -> "))
				stackPath = stackPath.substring(0, stackPath.length()-" -> ".length());

			return stackPath;
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
		
		int _maxSample = 0;
		int _maxEngine = 0;
		
		LinkedHashMap<String, SampleEngineDetailes> _sampleEngineDetailes = new LinkedHashMap<String, AseStackTraceAnalyzer.SampleEngineDetailes>();

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

		// Stack so far
		private String _stackStartStr;
		
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

		public StackEntry insertEntry(StackEntry root, String name, String stackStartStr, ArrayList<String> stackList, int sample, int engine, int kpid, int spid, int suid, int sampleStartRow, int sampleEndRow, ArrayList<ObjectInfo> objectList, ArrayList<String> sampleContent)
		{
			StackEntry se = _childMap.get(name);
			if (se == null)
			{
				se = new StackEntry(root, name);
				_childMap.put(name, se);
			}
			se._count++;
			se._depth = this._depth + 1;
			se._stackStartStr = stackStartStr;
			
			SampleEngineDetailes sampleDetailes = new SampleEngineDetailes(sample, engine, kpid, spid, suid, sampleStartRow, sampleEndRow, stackStartStr, stackList, objectList, sampleContent);
			_sampleEngineDetailes.put(sampleDetailes.getKey(), sampleDetailes);
			
			_maxSample = Math.max(_maxSample, sample);
			_maxEngine = Math.max(_maxEngine, engine);
					
			return se;
		}

		public String getStackStartStr()
		{
			return _stackStartStr;
		}

		/**
		 * This is used by the Tree View to print the content of this done.
		 */
		@Override
		public String toString()
		{
//			return _name + "(count=" + _count + ")";
//			return _name + ":" + _count;

			String pctValStr = "";
			if (_root._count >= 0 && !this.equals(_root))
			{
				try
				{
					BigDecimal pctVal = new BigDecimal( (_count*1.0 / _root._count) * 100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					if ( pctVal.doubleValue() >= 0.1 )
						pctValStr = " <i><font color=\"green\">(" + pctVal + "%)</font></i>";
				}
				catch (RuntimeException rte) {}
			}

			return "<html>" + 
				(_isFilterFunctionMatch ? "<b>" : "") +
				_name + 
				(_isFilterFunctionMatch ? "</b>" : "") +
				" : <b><font color=\"blue\">" + 
				_count + 
				"</font></b>" +
				pctValStr +
				"</html>";
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

		public SampleEngineDetailes getSampleEngineDetailes(int row, int col)
		{
			return _sampleEngineDetailes.get(SampleEngineDetailes.getKey(row, col));
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
	public static class AseStackTreeView
	extends JFrame
	implements ActionListener, MouseListener
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

		private JCheckBox    _tooltipStackMatrix_chk = new JCheckBox("Show Stack Matrix Overview on tooltip", true);

		private JLabel       _tracefile_lbl          = new JLabel("sybmon file");
		private JTextField   _tracefile_txt          = new JTextField();
		private JButton      _tracefile_but          = new JButton("...");

		private JButton      _loadRefresh_but        = new JButton("Load/Refresh");
		private JLabel       _sampleCount_lbl        = new JLabel("Number of samples in file");
		private JTextField   _sampleCount_txt        = new JTextField();
		private JTextField   _actualSampleCount_txt  = new JTextField();
		private JLabel       _sampleSleep_lbl        = new JLabel("Sleep time between samples");
		private JTextField   _sampleSleep_txt        = new JTextField();

		private JLabel       _sampleSlots_lbl        = new JLabel("Sample Slots");
		private JTextField   _expectedSampleSlots_txt= new JTextField();
		private JTextField   _actualSampleSlots_txt  = new JTextField();

		private JButton      _sybmonExample_but      = new JButton("Example Script to do Stack Trace");

		private JTree        _treeView               = null;
		private JPopupMenu   _treeViewPopupMenu      = null;

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

			ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ase_stack_trace_tool.png");
			ImageIcon icon32 = null; //= SwingUtils.readImageIcon(Version.class, "images/ase_stack_trace_tool_32.png");
			if (icon16 != null || icon32 != null)
			{
				ArrayList<Image> iconList = new ArrayList<Image>();
				if (icon16 != null) iconList.add(icon16.getImage());
				if (icon32 != null) iconList.add(icon32.getImage());

				setIconImages(iconList);
			}

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

			_functionFilter_chk     .setToolTipText("<html>Only read stack traces if it contains this function, or part of the function.<br>Java RegExp can be used.</html>"); 
			_functionFilter_txt     .setToolTipText("<html>Only read stack traces if it contains this function, or part of the function.<br>Java RegExp can be used.</html>");
			_funcFilterExact_rb     .setToolTipText("<html>Use <b>exact</b> matching: <code>funcname.equal(filterStr)</code></html>");
			_funcFilterIndexOf_rb   .setToolTipText("<html>Use <b>index</b> matching: <code>funcname.indexOf(filterStr)</code></html>");
			_funcFilterRegExp_rb    .setToolTipText("<html>Use <b>Java RegExp</b> matching: <code>funcname.matches(filterStr)</code></html>");
			_functionStackStart_chk .setToolTipText("<html>Use this <b>exact</b> function name as a root. The function calls \"above\" this function will not be displayed.</html>"); 
			_functionStackStart_txt .setToolTipText("<html>Use this <b>exact</b> function name as a root. The function calls \"above\" this function will not be displayed.</html>"); 
			_keepCppInstance_chk    .setToolTipText("<html>Strip off or Keep the C++ instance. If function looks like '_$o1cexoL0.kpsched', if strip=true, it fill become 'kpsched'</html>"); 
			
			_tooltipStackMatrix_chk .setToolTipText("<html>Show some kind of <i>map</i> to show in what sample/engine this stacktrace is visible at.</html>");

			_tracefile_lbl          .setToolTipText("<html>'sybmon' trace file to be read/parsed</html>"); 
			_tracefile_txt          .setToolTipText("<html>'sybmon' trace file to be read/parsed</html>");
			_tracefile_but          .setToolTipText("<html>Open a file browser and choose a file.</html>"); 

			_loadRefresh_but        .setToolTipText("<html>Load/Parse the selected file.</html>"); 

			_sampleCount_lbl        .setToolTipText("<html>How many Intervals was sybmon expected to sample</html>"); 
			_sampleCount_txt        .setToolTipText("<html>How many Intervals was sybmon expected to sample</html>");
			_actualSampleCount_txt  .setToolTipText("<html>How many Intervals did sybmon <b>actually</b> sample?   <br>If this is <b>lower</b> than the expected, check the source file... it is probably <b>not</b> complete.</html>");
			
			_sampleSleep_lbl        .setToolTipText("<html>How long time was it between the samples</html>"); 
			_sampleSleep_txt        .setToolTipText("<html>How long time was it between the samples</html>"); 

			_sampleSlots_lbl        .setToolTipText("<html>how many <i>slots</i> where sampled.</html>"); 
			_expectedSampleSlots_txt.setToolTipText("<html>how many <i>slots</i> did we <b>expect</b> to sampled?  <br><b>Formula:</b> <code>samples * engines</code></html>"); 
			_actualSampleSlots_txt  .setToolTipText("<html>how many <i>slots</i> did we <b>actually</b> sample?   <br>If this is <b>lower</b> than the expected, check the source file... it is probably <b>not</b> complete.</html>"); 

			_sybmonExample_but      .setToolTipText("<html>Show a script example of how to do stack trace using 'sybmon'.</html>");

			_sampleCount_txt        .setEditable(false);
			_actualSampleCount_txt  .setEditable(false);
			_sampleSleep_txt        .setEditable(false);
			_expectedSampleSlots_txt.setEditable(false);
			_actualSampleSlots_txt  .setEditable(false);

			ButtonGroup group = new ButtonGroup();
			group.add(_funcFilterExact_rb);
			group.add(_funcFilterIndexOf_rb);
			group.add(_funcFilterRegExp_rb);

			
			panel.add(_sampleFilter_chk,        "");
			panel.add(_sampleStart_lbl,         "span, split 6");
			panel.add(_sampleStart_txt,         "w 50");
			panel.add(_sampleStop_lbl,          "");
			panel.add(_sampleStop_txt,          "w 50");
			panel.add(_sampleEngine_lbl,        "");
			panel.add(_sampleEngine_txt,        "w 50, wrap");

			panel.add(_functionFilter_chk,      "");
			panel.add(_functionFilter_txt,      "split, push, grow");
			panel.add(_funcFilterExact_rb,      "");
			panel.add(_funcFilterIndexOf_rb,    "");
			panel.add(_funcFilterRegExp_rb,     "wrap");

			panel.add(_functionStackStart_chk,  "");
			panel.add(_functionStackStart_txt,  "push, grow, wrap");

			panel.add(_keepCppInstance_chk,     "");
			panel.add(new JLabel(),             "split, pushx, growx");
			panel.add(_tooltipStackMatrix_chk,  "wrap");

			panel.add(_tracefile_lbl,           "");
			panel.add(_tracefile_txt,           "split, push, grow");
			panel.add(_tracefile_but,           "wrap");

			panel.add(_loadRefresh_but,         "");
//			panel.add(new JLabel(),             "split, push, grow");
			panel.add(_sampleCount_lbl,         "split");
			panel.add(_sampleCount_txt,         "w 50");
			panel.add(_actualSampleCount_txt,   "w 50");
			panel.add(_sampleSleep_lbl,         "");
			panel.add(_sampleSleep_txt,         "w 50");
			panel.add(_sampleSlots_lbl,         "");
			panel.add(_expectedSampleSlots_txt, "w 50");
			panel.add(_actualSampleSlots_txt,   "w 50");
			panel.add(new JLabel(""),           "push, grow");
			panel.add(_sybmonExample_but,       "wrap");

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

			_treeView.addMouseListener(this);
			_treeViewPopupMenu = new JPopupMenu();
			
			_treeViewPopupMenu.add(new StackMatrixTableAction(this));

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

								boolean buildMatix = _tooltipStackMatrix_chk.isSelected();
								if (buildMatix)
								{
									StringBuilder sb = new StringBuilder();
									boolean[][] matrix = new boolean[se._maxSample+1][se._maxEngine+1];
									for (SampleEngineDetailes sed : se._sampleEngineDetailes.values())
										matrix[sed._sample][sed._engine] = true;
									
									sb.append("<html><b>Current stack:</b> <code>"+se._stackStartStr+"</code><br><b>Was found in following samples/engines</b><br>");
									sb.append("<br>");

									sb.append("<table border=1 cellspacing=1 cellpadding=0>");
									sb.append("<tr> <th>Sample</th>");
									for (int e=0; e<=se._maxEngine; e++)
										sb.append("<th>E").append(e).append("</th>");
									sb.append("</tr>");

									for (int s=1; s<matrix.length; s++)
									{
										sb.append("<tr> <td>").append(s).append("</td>");
										for (int e=0; e<matrix[s].length; e++)
										{
											sb.append("<td align=\"center\">");
											if (matrix[s][e])
												sb.append("<b><font color=\"red\">&bull;</font></b>");
											sb.append("</td>");
										}
										sb.append("</tr>");
									}
									sb.append("</table>");
									sb.append("</html>");
									tip = sb.toString();
								}

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
				_sampleCount_txt        .setText(_str.getSampleIterations()+"");
				_actualSampleCount_txt  .setText(_str.getActualSamples()+"");
				_sampleSleep_txt        .setText(_str.getSampleInterval()+"");
				_expectedSampleSlots_txt.setText(_str.getExpectedSampleSlots()+"");
				_actualSampleSlots_txt  .setText(_str.getActualSampleSlots()+"");

				// if we are "missing" samples, mark it as RED
				_actualSampleCount_txt.setBackground(_sampleCount_txt.getBackground());
				if (_str.getActualSamples() < _str.getSampleIterations())
					_actualSampleCount_txt.setBackground(Color.RED);

				_actualSampleSlots_txt.setBackground(_sampleCount_txt.getBackground());
				if (_str.getActualSampleSlots() < _str.getExpectedSampleSlots())
					_actualSampleSlots_txt.setBackground(Color.RED);

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
					
					// Load the file
					_loadRefresh_but.doClick();
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

		

		/*---------------------------------------------------
		** BEGIN: implementing MouseListener
		**---------------------------------------------------
		*/
		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (SwingUtilities.isRightMouseButton(e)) 
			{
				int row = _treeView.getClosestRowForLocation(e.getX(), e.getY());
				_treeView.setSelectionRow(row);
				_treeViewPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}
		/*---------------------------------------------------
		** END: implementing MouseListener
		**---------------------------------------------------
		*/

		//-------------------------------------------------------------------
		// Local ACTION classes
		//-------------------------------------------------------------------
		private class StackMatrixTableAction
		extends AbstractAction
		{
			private static final long serialVersionUID = 1L;

			private static final String NAME = "Open 'Stack Matrix Table'...";
//			private static final String ICON = "images/delete.png";

			private JFrame _owner = null;
			public StackMatrixTableAction(JFrame owner)
			{
//				super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
				super(NAME);
				_owner = owner;
			}

			@Override
			public void actionPerformed(ActionEvent e)
			{
//				System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//				System.out.println("source: "+e.getSource());
//				System.out.println("     e: "+e);
				TreePath path = _treeView.getSelectionPath();
				if (path == null)
					return;	

//				String stackPath = "";
//				Object[] oa = path.getPath();
//				for (int i=0; i<oa.length; i++)
//				{
//					StackEntry se = (StackEntry) ((DefaultMutableTreeNode)oa[i]).getUserObject();
//					stackPath += se._name + " -> ";
//				}
//				// Remove last " -> "
//				if (stackPath.endsWith(" -> "))
//					stackPath = stackPath.substring(0, stackPath.length()-" -> ".length());

				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				Object o = node.getUserObject();
				if (o instanceof StackEntry)
				{
					StackEntry se = (StackEntry) o;
//					System.out.println("     se: "+se);
					
					if (_stackDialog == null)
						_stackDialog = new StackMatrixTableDialog(_owner);

					_stackDialog.setStackEntry(se);
					
					_stackDialog.setVisible(true);
				}
			}
		}

		
		
		private StackMatrixTableDialog _stackDialog = null;
		
		@SuppressWarnings("unused")
		private static class StackMatrixTableDialog
		extends JDialog
		implements ListSelectionListener, ActionListener
		{
			private static final long serialVersionUID = 1L;

			private JFrame                _owner = null;

			private JSplitPane            _splitPane = new JSplitPane();

			private JPanel                _infoPane = new JPanel(new MigLayout());

			private RSyntaxTextArea       _textArea = new RSyntaxTextArea();
			private RTextScrollPane       _textScroll = new RTextScrollPane(_textArea);

			private StackEntry            _se;
			private GTable                _table;
			private StackMatrixTableModel _tm;
			private JScrollPane           _tableScroll;
			
//			public StackMatrixTableDialog(JFrame owner, StackMatrixTableModel tm)
			public StackMatrixTableDialog(JFrame owner)
			{
				super(owner, "Stack Matrix Table", ModalityType.MODELESS);
				
				_owner = owner;
//				_tm = tm;
				
				init();

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

				SwingUtils.setSizeWithingScreenLimit(this, 5);
			}

			private JLabel       _openStackName_lbl      = new JLabel("Open at Stack Path");
			private JTextField   _openStackName_txt      = new JTextField();

			private JLabel       _currStackName_lbl      = new JLabel("Current Stack Path");
			private JTextField   _currStackName_txt      = new JTextField();

			private JLabel       _currSample_lbl         = new JLabel("Current Sample");
			private JTextField   _currSample_txt         = new JTextField(4);

			private JLabel       _currEngine_lbl         = new JLabel("Current Engine");
			private JTextField   _currEngine_txt         = new JTextField(4);

			private JLabel       _currKpid_lbl           = new JLabel("Current KPID");
			private JTextField   _currKpid_txt           = new JTextField(12);
			private JComboBox    _currKpid_cbx           = new JComboBox();
			private int          _currKpid               = -1;
			private int          _lastKpid               = -1;

			private JLabel       _currSpid_lbl           = new JLabel("Current SPID");
			private JTextField   _currSpid_txt           = new JTextField(5);
			private JComboBox    _currSpid_cbx           = new JComboBox();
			private int          _currSpid               = -1;
			private int          _lastSpid               = -1;

			private JLabel       _currSuid_lbl           = new JLabel("Current SUID");
			private JTextField   _currSuid_txt           = new JTextField(5);
			private JComboBox    _currSuid_cbx           = new JComboBox();
			private int          _currSuid               = -1;
			private int          _lastSuid               = -1;

			
			private JLabel       _sampleHighlight_lbl    = new JLabel("Highlight Cell(s) based on");
			private JRadioButton _sampleHighlightKPID_rb = new JRadioButton("kpid", true);
			private JRadioButton _sampleHighlightSPID_rb = new JRadioButton("spid", false);
			private JRadioButton _sampleHighlightSUID_rb = new JRadioButton("suid", false);

			private JLabel       _currObjid_lbl          = new JLabel("Current Object ID's");
			private JTextField   _currObjid_txt          = new JTextField();
			private JComboBox    _currObjid_cbx          = new JComboBox();


			public void setOpenStackName(String stackPath)
			{
				_openStackName_txt.setText(stackPath);
			}

			public void setCurrentStackName(String stackPath)
			{
				_currStackName_txt.setText(stackPath);
				_currStackName_txt.setCaretPosition(0);
			}

			public void setCurrentSample(int sample)
			{
				_currSample_txt.setText( sample < 0 ? "" : Integer.toString(sample));
			}

			public void setCurrentEngine(int engine)
			{
				_currEngine_txt.setText( engine < 0 ? "" : Integer.toString(engine));
			}

			public void setCurrentKpid(int kpid)
			{
				_currKpid = kpid;
				_currKpid_txt.setText( kpid < 0 ? "" : Integer.toString(kpid));
			}

			public void setCurrentSpid(int spid)
			{
				_currSpid = spid;
				_currSpid_txt.setText( spid < 0 ? "" : Integer.toString(spid));
			}

			public void setCurrentSuid(int suid)
			{
				_currSuid = suid;
				_currSuid_txt.setText( suid < 0 ? "" : Integer.toString(suid));
			}

			private void setCurrentObjectInfo(ArrayList<ObjectInfo> objectList)
			{
				if (objectList == null)
				{
					_currObjid_txt.setText("");
					return;
				}

//System.out.println("objectList.size="+objectList.size());
//System.out.println("objectList     ="+objectList);
				String oiStr = "";
				for (ObjectInfo oi : objectList)
					oiStr += oi.toString() + ", ";

				// Remove last ", "
				if (oiStr.length() > 2)
					oiStr = oiStr.substring(0, oiStr.length() - ", ".length());

				_currObjid_txt.setText(oiStr);
			}


			private void init()
			{
				setLayout(new BorderLayout());

				_openStackName_txt.setEditable(false);
				_currStackName_txt.setEditable(false);
				_currSample_txt   .setEditable(false);
				_currEngine_txt   .setEditable(false);
				_currKpid_txt     .setEditable(false);
				_currSpid_txt     .setEditable(false);
				_currSuid_txt     .setEditable(false);
				_currObjid_txt    .setEditable(false);
				
				_currKpid_cbx .setMaximumRowCount(50);
				_currSpid_cbx .setMaximumRowCount(50);
				_currSuid_cbx .setMaximumRowCount(50);
				_currObjid_cbx.setMaximumRowCount(50);
				
				_currKpid_cbx .addActionListener(this);
				_currSpid_cbx .addActionListener(this);
				_currSuid_cbx .addActionListener(this);
				_currObjid_cbx.addActionListener(this);
				
				_sampleHighlightKPID_rb.addActionListener(this);
				_sampleHighlightSPID_rb.addActionListener(this);
				_sampleHighlightSUID_rb.addActionListener(this);

//				_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
				_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

				ButtonGroup group = new ButtonGroup();
				group.add(_sampleHighlightKPID_rb);
				group.add(_sampleHighlightSPID_rb);
				group.add(_sampleHighlightSUID_rb);
				
				
				_infoPane.add(_openStackName_lbl,        "");
				_infoPane.add(_openStackName_txt,        "pushx, growx, wrap");
				
				_infoPane.add(_currStackName_lbl,        "");
				_infoPane.add(_currStackName_txt,        "pushx, growx, wrap");
				
				_infoPane.add(_currSample_lbl,           "");
				_infoPane.add(_currSample_txt,           "split");
				_infoPane.add(_currEngine_lbl,           "gap 20");
				_infoPane.add(_currEngine_txt,           "");
				_infoPane.add(_sampleHighlight_lbl,      "gap 20");
				_infoPane.add(_sampleHighlightKPID_rb,   "");
				_infoPane.add(_sampleHighlightSPID_rb,   "");
				_infoPane.add(_sampleHighlightSUID_rb,   "wrap");
				
				_infoPane.add(_currKpid_lbl,             "");
				_infoPane.add(_currKpid_txt,             "split");
				_infoPane.add(_currKpid_cbx,             "");
				_infoPane.add(_currSpid_lbl,             "gap 20");
				_infoPane.add(_currSpid_txt,             "");
				_infoPane.add(_currSpid_cbx,             "");
				_infoPane.add(_currSuid_lbl,             "gap 20");
				_infoPane.add(_currSuid_txt,             "");
				_infoPane.add(_currSuid_cbx,             "wrap");

				_infoPane.add(_currObjid_lbl,            "");
				_infoPane.add(_currObjid_txt,            "split, pushx, growx");
				_infoPane.add(_currObjid_cbx,            "wrap");
				
				_table = new GTable("", Color.WHITE); // display NULL values as "" empty strings
				_table.setSortable(false);
//				_table.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
				_table.packAll(); // set size so that all content in all cells are visible
//				_table.setColumnControlVisible(true);
				_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				_table.setColumnSelectionAllowed(true);
				_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				_table.getSelectionModel().addListSelectionListener(this);                   // Change ROW
				_table.getColumnModel().getSelectionModel().addListSelectionListener(this);  // CHange CELL
//				setModel(_tm);

				_table.addHighlighter(new ColorHighlighter(new HighlightPredicate()
				{
					@Override
					public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
					{
						SampleEngineDetailes sde = _tm._se.getSampleEngineDetailes(adapter.row+1, adapter.column-1);
						
						if (_sampleHighlightKPID_rb.isSelected())
						{
    						if (sde != null && _currKpid == sde._kpid)
    							return true;
    						return false;
						}

						if (_sampleHighlightSPID_rb.isSelected())
						{
    						if (sde != null && _currSpid == sde._spid)
    							return true;
    						return false;
						}

						if (_sampleHighlightSUID_rb.isSelected())
						{
    						if (sde != null && _currSuid == sde._suid)
    							return true;
    						return false;
						}

						return false;
					}
				}, Color.ORANGE, null));
				
//				_table.setDefaultRenderer(Boolean.class, new CheckBoxTableCellRenderer());
				
				_tableScroll = new JScrollPane(_table);
				_splitPane.setTopComponent(_tableScroll);
				_splitPane.setBottomComponent(_textScroll);
				
				add(_infoPane,  BorderLayout.NORTH);
				add(_splitPane, BorderLayout.CENTER);

				int divLoc = getSize().width / 2;
				if (divLoc <= 10)
					divLoc = 500;
				_splitPane.setDividerLocation(divLoc);

			}
			
//			public void setModel(StackMatrixTableModel tm)
//			{
//				_tm = tm;
//				_table.setModel(_tm);
//				_table.packAll();
//				
//				// get all KPID, SPID, SUID and it's count
//				
//			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();

				if (_currKpid_cbx.equals(source))
				{
					String str = _currKpid_cbx.getSelectedItem().toString();
					str = str.substring("KPID = ".length(), str.indexOf(","));
					try 
					{
						setCurrentKpid(Integer.parseInt(str));
						_sampleHighlightKPID_rb.setSelected(true);
						_table.repaint();
					} 
					catch (NumberFormatException ignore) {}
				}

				if (_currSpid_cbx.equals(source))
				{
					String str = _currSpid_cbx.getSelectedItem().toString();
					str = str.substring("SPID = ".length(), str.indexOf(","));
					try 
					{
						setCurrentSpid(Integer.parseInt(str));
						_sampleHighlightSPID_rb.setSelected(true);
						_table.repaint();
					} 
					catch (NumberFormatException ignore) {}
				}

				if (_currSuid_cbx.equals(source))
				{
					String str = _currSuid_cbx.getSelectedItem().toString();
					str = str.substring("SUID = ".length(), str.indexOf(","));
					try 
					{
						setCurrentSuid(Integer.parseInt(str));
						_sampleHighlightSUID_rb.setSelected(true);
						_table.repaint();
					} 
					catch (NumberFormatException ignore) {}
				}
				
				if (_sampleHighlightKPID_rb.equals(source)) _table.repaint();
				if (_sampleHighlightSPID_rb.equals(source)) _table.repaint();
				if (_sampleHighlightSUID_rb.equals(source)) _table.repaint();
			}

			public void setStackEntry(StackEntry se)
			{
				String sampleHighligtType = getCurrentHighligtType();
				
				StackMatrixTableModel tm = new StackMatrixTableModel(se);
				_tm = tm;
				_table.setModel(_tm);
				_table.getColumnModel().setColumnMargin(0);
//				_table.packAll();
				SwingUtils.calcColumnWidths(_table);

				
				setOpenStackName(se.getStackStartStr());

				Map<Integer, Integer> kpidCount = new HashMap<Integer, Integer>();
				Map<Integer, Integer> spidCount = new HashMap<Integer, Integer>();
				Map<Integer, Integer> suidCount = new HashMap<Integer, Integer>();

				Map<String, Integer> objidCount = new HashMap<String, Integer>();

				// get all KPID, SPID, SUID and it's count
				for (SampleEngineDetailes sed : se._sampleEngineDetailes.values())
				{
					Integer count;

					// kpid
					count = kpidCount.get(sed._kpid);
					if (count == null)
						kpidCount.put(sed._kpid, 1);
					else
						kpidCount.put(sed._kpid, count + 1);

					// spid
					count = spidCount.get(sed._spid);
					if (count == null)
						spidCount.put(sed._spid, 1);
					else
						spidCount.put(sed._spid, count + 1);

					// suid
					count = suidCount.get(sed._suid);
					if (count == null)
						suidCount.put(sed._suid, 1);
					else
						suidCount.put(sed._suid, count + 1);

					// objid
					for (ObjectInfo oi : sed._objectList)
					{
						String oiStr = oi.toString();
						
    					count = objidCount.get(oiStr);
    					if (count == null)
    						objidCount.put(oiStr, 1);
    					else
    						objidCount.put(oiStr, count + 1);
					}
				}

				kpidCount  = CollectionUtils.sortByMapValue(kpidCount,  false);
				spidCount  = CollectionUtils.sortByMapValue(spidCount,  false);
				suidCount  = CollectionUtils.sortByMapValue(suidCount,  false);
				objidCount = CollectionUtils.sortByMapValue(objidCount, false);
				
//				System.out.println("--kpid--Count-Map:" + StringUtil.toCommaStr(kpidCount));
//				System.out.println("--spid--Count-Map:" + StringUtil.toCommaStr(spidCount));
//				System.out.println("--suid--Count-Map:" + StringUtil.toCommaStr(suidCount));
//				System.out.println("--objid-Count-Map:" + StringUtil.toCommaStr(objidCount));

				_currKpid_cbx.removeActionListener(this);;
				_currKpid_cbx.removeAllItems();
				_currKpid_cbx.addActionListener(this);
			    for(Entry<Integer, Integer> e : kpidCount.entrySet()) 
			    {
			        Integer key = e.getKey();
			        Integer val = e.getValue();
				    _currKpid_cbx.addItem("KPID = " + key + ", count = " + val);
			    }

				_currSpid_cbx.removeActionListener(this);;
				_currSpid_cbx.removeAllItems();
				_currSpid_cbx.addActionListener(this);
			    for(Entry<Integer, Integer> e : spidCount.entrySet()) 
			    {
			        Integer key = e.getKey();
			        Integer val = e.getValue();
				    _currSpid_cbx.addItem("SPID = " + key + ", count = " + val);
			    }

				_currSuid_cbx.removeActionListener(this);;
				_currSuid_cbx.removeAllItems();
				_currSuid_cbx.addActionListener(this);
			    for(Entry<Integer, Integer> e : suidCount.entrySet()) 
			    {
			        Integer key = e.getKey();
			        Integer val = e.getValue();
				    _currSuid_cbx.addItem("SUID = " + key + ", count = " + val);
			    }

			    _currObjid_cbx.removeActionListener(this);;
			    _currObjid_cbx.removeAllItems();
			    _currObjid_cbx.addActionListener(this);
			    for(Entry<String, Integer> e : objidCount.entrySet()) 
			    {
			    	String  key = e.getKey();
			        Integer val = e.getValue();
			        _currObjid_cbx.addItem("OBJ = " + key + ", count = " + val);
			    }
			    
				setCurrentHighligtType(sampleHighligtType);
			}

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;
				int row = _table.getSelectionModel().getLeadSelectionIndex();
				int col = _table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
				
				// Adjust
				row++;
				col--;

				StackMatrixTableModel tm = (StackMatrixTableModel)_table.getModel();
				String text = tm.getSampleText(row, col);
				_textArea.setText(text);
				_textArea.setCaretPosition(0);
				
				SampleEngineDetailes sde = tm._se.getSampleEngineDetailes(row, col);
				if (sde == null)
				{
					setCurrentStackName("");
					setCurrentSample(-1);
					setCurrentEngine(-1);
					setCurrentKpid(-1);
					setCurrentSpid(-1);
					setCurrentSuid(-1);
					setCurrentObjectInfo(null);
				}
				else
				{
					setCurrentStackName(sde.getFullCallStack());
					setCurrentSample(sde._sample);
					setCurrentEngine(sde._engine);
					setCurrentKpid(sde._kpid);
					setCurrentSpid(sde._spid);
					setCurrentSuid(sde._suid);
					setCurrentObjectInfo(sde._objectList);
				}
				
//				System.out.println("TABLE REPAINT: _currKpid != _lastKpid ("+_currKpid+"!="+_lastKpid+")   ||   _currSpid() != _lastSpid ("+_currSpid+"!="+_lastSpid+")   ||   _currSuid != _lastSuid ("+_currSuid+"!="+_lastSuid+")");

				// request Repaint if we choose a new KPID, SPID or SUID
				if (_sampleHighlightKPID_rb.isSelected() && _currKpid != _lastKpid) _table.repaint();
				if (_sampleHighlightSPID_rb.isSelected() && _currSpid != _lastSpid) _table.repaint();
				if (_sampleHighlightSUID_rb.isSelected() && _currSuid != _lastSuid) _table.repaint();

				_lastKpid = _currKpid;
				_lastSpid = _currSpid;
				_lastSuid = _currSuid;

//				System.out.println(String.format("Lead: %d, %d. ", 
//						_table.getSelectionModel().getLeadSelectionIndex(),
//						_table.getColumnModel().getSelectionModel().getLeadSelectionIndex()));
//
//				System.out.print("Rows: ");
//				for (int c : _table.getSelectedRows()) {
//					System.out.print(String.format("%d, ", c));
//				}
//				System.out.println();
//
//				System.out.print("Columns: ");
//				for (int c : _table.getSelectedColumns()) {
//					System.out.print(String.format("%d, ", c));
//				}
//				System.out.println();
			}

			private String getCurrentHighligtType()
			{
				SampleHighlightType highlightType = SampleHighlightType.KPID;
				if (_sampleHighlightKPID_rb.isSelected()) highlightType = SampleHighlightType.KPID;
				if (_sampleHighlightSPID_rb.isSelected()) highlightType = SampleHighlightType.SPID;
				if (_sampleHighlightSUID_rb.isSelected()) highlightType = SampleHighlightType.SUID;

				return highlightType.toString();
			}
	
			private void setCurrentHighligtType(String type)
			{
				if (type != null)
				{
					SampleHighlightType highlightType = SampleHighlightType.valueOf(type);
					if (highlightType == SampleHighlightType.KPID) _sampleHighlightKPID_rb.setSelected(true);
					if (highlightType == SampleHighlightType.SPID) _sampleHighlightSPID_rb.setSelected(true);
					if (highlightType == SampleHighlightType.SUID) _sampleHighlightSUID_rb.setSelected(true);
				}
			}
			
			private void saveProps()
			{
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null)
				{
					_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
					return;
				}
				//----------------------------------
				// Settings
				//----------------------------------
				conf.setProperty("StackMatrixTable.sample.highlight.type", getCurrentHighligtType());

				//------------------
				// WINDOW
				//------------------
				conf.setProperty("StackMatrixTable.dialog.window.width",  this.getSize().width);
				conf.setProperty("StackMatrixTable.dialog.window.height", this.getSize().height);
				conf.setProperty("StackMatrixTable.dialog.window.pos.x",  this.getLocationOnScreen().x);
				conf.setProperty("StackMatrixTable.dialog.window.pos.y",  this.getLocationOnScreen().y);

				conf.setProperty("StackMatrixTable.dialog.divLocation",   _splitPane.getDividerLocation());

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

				// FunctionFilterType
				setCurrentHighligtType( conf.getProperty("StackMatrixTable.sample.highlight.type", SampleHighlightType.KPID.toString()) );
			}

			private void getSavedWindowProps()
			{
				Configuration conf = Configuration.getCombinedConfiguration();
				if (conf == null)
				{
					_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
					return;
				}

				int width  = conf.getIntProperty("StackMatrixTable.dialog.window.width",  1024);
				int height = conf.getIntProperty("StackMatrixTable.dialog.window.height", 768);
				int x      = conf.getIntProperty("StackMatrixTable.dialog.window.pos.x",  -1);
				int y      = conf.getIntProperty("StackMatrixTable.dialog.window.pos.y",  -1);
				if (width != -1 && height != -1)
				{
					this.setSize(width, height);
				}
				if (x != -1 && y != -1)
				{
					if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
						this.setLocation(x, y);
				}
				
				int divLoc = conf.getIntProperty("StackMatrixTable.dialog.divLocation",  -1);
				if (divLoc >= 0)
					_splitPane.setDividerLocation(divLoc);
			}
		}

//		private static class CheckBoxTableCellRenderer extends JCheckBox implements TableCellRenderer
//		{
//
//			private static final long serialVersionUID = 1L;
//
//			Border noFocusBorder;
//			Border focusBorder;
//
//			public CheckBoxTableCellRenderer()
//			{
//				super();
//				setOpaque(true);
//				setBorderPainted(true);
//				setHorizontalAlignment(SwingConstants.CENTER);
//				setVerticalAlignment(SwingConstants.CENTER);
//			}
//
//			@Override
//			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//			{
//
//				if ( isSelected )
//				{
//					setForeground(table.getSelectionForeground());
//					setBackground(table.getSelectionBackground());
//				}
//				else
//				{
//					setForeground(table.getForeground());
//					setBackground(table.getBackground());
//				}
//
//				if ( hasFocus )
//				{
//					if ( focusBorder == null )
//					{
//						focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
//					}
//					setBorder(focusBorder);
//				}
//				else
//				{
//					if ( noFocusBorder == null )
//					{
//						noFocusBorder = new EmptyBorder(1, 1, 1, 1);
//					}
//					setBorder(noFocusBorder);
//				}
//
//				setSelected(Boolean.TRUE.equals(value));
//				setVisible(Boolean.TRUE.equals(value));
//				return this;
//			}
//		}
		
		private static class StackMatrixTableModel
		extends AbstractTableModel
		{
			private static final long serialVersionUID = 1L;

//			private boolean _bySample = true;  // BySample or ByEngine on the X-axis (left-hand-side)
			private StackEntry _se;
			
			private ArrayList<String>            _headers = new ArrayList<String>();
//			private ArrayList<ArrayList<Object>> _rows    = new ArrayList<ArrayList<Object>>();
			Boolean[][] _matrix;

			public StackMatrixTableModel(StackEntry se)
			{
				_se = se;
				init();
			}
			
			public String getSampleText(int row, int col)
			{
				SampleEngineDetailes sed = _se.getSampleEngineDetailes(row, col);
				if (sed == null)
					return "None for row="+row+", col="+col;
				
//				System.out.println("SED _sample:               "+sed._sample);
//				System.out.println("SED _engine:               "+sed._engine);
//				System.out.println("SED _sampleStartRow:       "+sed._sampleStartRow);
//				System.out.println("SED _sampleEndRow:         "+sed._sampleEndRow);
//				System.out.println("SED _sampleContent.size(): "+sed._sampleContent.size());
//				System.out.println("");

				StringBuilder sb = new StringBuilder();
				for (String str : sed._sampleContent)
					sb.append(str).append("\n");
				
				return sb.toString();
			}

			private void init()
			{
//				boolean[][] matrix;

//				if (_bySample)
//				{
					_matrix = new Boolean[_se._maxSample][_se._maxEngine+1];
    				for (SampleEngineDetailes sed : _se._sampleEngineDetailes.values())
    					_matrix[sed._sample-1][sed._engine] = true;

    				_headers.add("Sample");
    				String enginePrefix = "E";
					for (int e=0; e<=_se._maxEngine; e++)
					{
						// Skip the 'E' prefix on engine number 10 and above
						// when we have more that 32 engines (to save some space on the screen)
						if (e >= 10 && _se._maxEngine > 32)
							enginePrefix = "";
	    				_headers.add(enginePrefix + e);
					}
//				}
//				else
//				{
//					_matrix = new Boolean[_se._maxEngine+1][_se._maxSample+1];
//    				for (SampleEngineDetailes sed : _se._sampleEngineDetailes.values())
//    					_matrix[sed._engine][sed._sample] = true;
//
//    				_headers.add("Engine");
//					for (int s=0; s<=_se._maxSample; s++)
//	    				_headers.add("S"+s);
//				}
				
			}

			@Override
			public int getRowCount()
			{
				return _matrix.length;
			}

			@Override
			public int getColumnCount()
			{
				return _headers.size();
			}

			@Override
			public String getColumnName(int column)
			{
				return _headers.get(column);
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
					return Integer.class;

				return Boolean.class;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex)
			{
				if (columnIndex == 0)
					return rowIndex + 1;

				// TODO Auto-generated method stub
				return _matrix[rowIndex][columnIndex-1];
			}
		}


		
		private void showSybmonExampleScript()
		{
			String exampleStr =
				"#!/bin/bash\n" +
				"\n" +
				"#--- Use input params for setup\n" +
				"export DSQUERY=${1:-$DSQUERY}\n" +
				"\n" +
				"#--- Setup/override environment if this is not done from the caller of the script\n" +
				"#export DSQUERY=ase_server_name\n" +
				"#export SYBASE=/opt/sybase\n" +
				"#export SYBASE_ASE=ASE-15_0\n" +
				"#export SYBASE_OCS=OCS-15_0\n" +
				"\n" +
				"#--- Setup LD_LIBRARY_PATH (Solaris, Linux), SHLIB (hp), LIBPATH (aix),\n" +
				"#export LD_LIBRARY_PATH=$SYBASE/$SYBASE_ASE/bin:$SYBASE/$SYBASE_ASE/lib:$LD_LIBRARY_PATH\n" +
				"#export LD_LIBRARY_PATH=$SYBASE/$SYBASE_OCS/bin:$SYBASE/$SYBASE_OCS/lib:$SYBASE/$SYBASE_OCS/lib3p64:$LD_LIBRARY_PATH\n" +
				"\n" +
				"#--- Setup how many times we should stacktrace 'sampleCount'\n" +
				"#--- and the sleeptime between stacktraces 'sampleInterval'\n" +
				"sampleCount=${2:-500}\n" +
				"sampleInterval=${3:-100}\n" +
				"secItWillTake=$(( ${sampleCount} * ${sampleInterval} / 1000 ))\n" +
				"\n" +
				"ts=$(date +%Y%m%d_%H%M%S)\n" +
				"logFile=\"stacktrace_${DSQUERY}_${ts}.out\"\n" +
				"\n" +
				"if [ \"\" = \"$DSQUERY\" ]\n" +
				"then\n" +
				"        echo ''\n" +
				"        echo \"Usage: $(basename $0) [servername] [sampleCount] [sampleInterval]\"\n" +
				"        echo '   or environment variable DSQUERY has to be set.'\n" +
				"        echo ''\n" +
				"        exit 1\n" +
				"fi\n" +
				"\n" +
				"echo ''\n" +
				"echo '############################################################################################'\n" +
				"echo 'Starting stacktrace of ASE'\n" +
				"echo \"  servername      = ${DSQUERY}\"\n" +
				"echo \"  sample count    = ${sampleCount}\"\n" +
				"echo \"  sample interval = ${sampleInterval}\"\n" +
				"echo ''\n" +
				"echo \"  SYBASE          = ${SYBASE}\"\n" +
				"echo \"  SYBASE_ASE      = ${SYBASE_ASE}\"\n" + 
				"echo \"  SYBASE_OCS      = ${SYBASE_OCS}\"\n" + 
				"echo \"  LD_LIBRARY_PATH = ${LD_LIBRARY_PATH}\"\n" + 
				"echo ''\n" +
				"echo \"This will take approx ${secItWillTake} seconds\"\n" +
				"echo ''\n" +
				"echo 'The result will be found in: '\n" +
				"echo \"  directory = $(pwd)\"\n" +
				"echo \"  logFile   = ${logFile}\"\n" +
				"echo '############################################################################################'\n" +
				"echo ''\n" +
				"\n" +
//				"$SYBASE/$SYBASE_ASE/bin/dataserver -X -Pquine 2>/dev/null << SYBMON\n" +
				"$SYBASE/$SYBASE_ASE/bin/dataserver -X -Pquine << SYBMON\n" +
				"\n" +
				"catalog $SYBASE/$SYBASE_ASE\n" +
				"attach $DSQUERY\n" +
				"log on ${logFile}\n" +
				"set display off\n" +
				"sample count=${sampleCount} interval=${sampleInterval} context=y reg=n\n" +
				"log close\n" +
				"quit\n" +
				"SYBMON\n" +
				"\n" +
				"if [ ! -f ${logFile} ]\n" +
				"then\n" +
				"        echo 'An error occured. The file has not been created'\n" +
				"        exit -1\n" +
				"fi\n" +
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

			conf.setProperty("AseStackTraceAnalyzer.info.tooltip.show.stackMatrix",      _tooltipStackMatrix_chk.isSelected());

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

			_tooltipStackMatrix_chk.setSelected(conf.getBooleanProperty("AseStackTraceAnalyzer.info.tooltip.show.stackMatrix",      _tooltipStackMatrix_chk.isSelected()));

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
