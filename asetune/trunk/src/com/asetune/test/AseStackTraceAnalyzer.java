package com.asetune.test;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import net.miginfocom.swing.MigLayout;

import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class AseStackTraceAnalyzer
{
	private String _filename = null;

	private void setFile(String filename)
	{
		_filename = filename;
	}

	private void readFile()
	{
		if (StringUtil.isNullOrBlank(_filename))
			throw new RuntimeException("Filename has not yet been set.");

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

			String row;
			while ((row = br.readLine()) != null)   
			{
				// Performing server sample: 500 iterations, 100ms interval, no registers
				if (row.startsWith("Performing server sample: "))
				{
					// get how many samples we consists of
				}
				else if (row.startsWith("---- Sample"))
				{
					// Reset and start new
					stackList.clear();
				}
				else if (row.startsWith("******** End of stack trace"))
				{
					// Close a stack
					
					if (row.startsWith("******** End of stack trace, spid"))
						stackList.add("SPID");
					else if (row.startsWith("******** End of stack trace, kernel"))
						stackList.add("KERNEL");

					Collections.reverse(stackList);
					addStackEntry(stackList);
					
					stackList.clear();
				}
				else if (row.startsWith("pc: "))
				{
					// pc: 0x0000000080f980d0 _coldstart(0x0000

					int startPos = 23;

					int oxPos       = row.indexOf("0x", startPos)-1;
//					int firstPlus   = row.indexOf('+');
//					int firstParant = row.indexOf('(');
//					int first       = firstParant;
//					if (firstPlus >= 0 && firstPlus < firstParant)
//						first = firstPlus;

					int endPos   = oxPos;
					
					String f = "";
					// Add to current
					try 
					{
						f = row.substring(23, endPos);
					}
					catch (RuntimeException rte)
					{
						System.out.println("PROBLEMS at row/entry endPos="+endPos+", '"+row+"'.");
						rte.printStackTrace();
					}

//					if (f.indexOf(' ')>0)
//						System.out.println("function='"+f+"'.");
					if (f.length() > 0)
						stackList.add(f);
				}
			}
			in.close();
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}		
	}


	private void makeDummyTree()
	{
		ArrayList<String> stackList;
		
		stackList = new ArrayList<String>();
		stackList.add("_coldstart");
		stackList.add("a");
		stackList.add("b");
		stackList.add("c");
		addStackEntry(stackList);

		stackList = new ArrayList<String>();
		stackList.add("_coldstart");
		stackList.add("x");
		stackList.add("y");
		stackList.add("z");
		addStackEntry(stackList);

		stackList = new ArrayList<String>();
		stackList.add("_coldstart");
		stackList.add("x");
		stackList.add("y");
		stackList.add("q");
		addStackEntry(stackList);

		stackList = new ArrayList<String>();
		stackList.add("xxx");
		stackList.add("a");
		stackList.add("b");
		stackList.add("c");
		addStackEntry(stackList);

		stackList = new ArrayList<String>();
		stackList.add("xxx");
		stackList.add("a");
		stackList.add("b");
		stackList.add("c");
		stackList.add("d");
		addStackEntry(stackList);
	}

	private void printTree()
	{
		System.out.println("======= root ==================");
		printTree(_root);
	}
	private void printTree(StackEntry se)
	{
		for (String name : se._childMap.keySet())
		{
			StackEntry c = se._childMap.get(name);
			System.out.println(StringUtil.replicate("> ", c._depth) + name+":"+c._count);
			
			if (c.hasChildren())
				printTree(c);
		}
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
//			System.out.println(StringUtil.replicate("> ", c._depth) + name+":"+c._count);
			
			DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(c);
			tn.add(cnode);
			
			if (c.hasChildren())
				createTreeNode(c, cnode);
		}
	}



	StackEntry _root = new StackEntry("root");

	private void addStackEntry(ArrayList<String> stackList)
	{
		StackEntry insertionPoint = _root;

		for (String funcName : stackList)
		{
//			if (stackEntry.indexOf(' ')>0)
//				System.out.println("function='"+stackEntry+"'.");

			// should we strip of any C++ stuff from the name
			// pc: 0x000000008041f8d8 _$o1cexoR0.s_execute+0x2f78(0x00000000000081cc, 0x000001017879c478, 0x0000000000000000, 0x0000000000000000, 0x00000000000000c1)
			if (funcName.startsWith("_$"))
			{
				int firstDot = funcName.indexOf('.') + 1;
				if (firstDot > 0)
					funcName = funcName.substring(firstDot);
			}

			insertionPoint = insertionPoint.insertEntry(funcName);
		}
	}


	private static class StackEntry
	{
		String _name = "";
		int _depth = 0;
		int _count = 0;
		Map<String, StackEntry> _childMap = new LinkedHashMap<String, StackEntry>();

		public StackEntry(String name)
		{
			_name = name;
		}

		public boolean hasChildren()
		{
			return _childMap.size() > 0;
		}

		public StackEntry insertEntry(String name)
		{
			StackEntry se = _childMap.get(name);
			if (se == null)
			{
				se = new StackEntry(name);
				_childMap.put(name, se);
			}
			se._count++;
			se._depth = this._depth + 1;
			
			return se;
		}

		@Override
		public String toString()
		{
//			return _name + "(count=" + _count + ")";
			return _name + ":" + _count;
		}
	}

//	private static class StackLevel
//	{
//		Map<String, StackEntry> _stackMap = new LinkedHashMap<String, StackEntry>();
//
//		public void add(String stackEntry)
//		{
//			StackEntry se = _stackMap.get(stackEntry);
//			if (se == null)
//			{
//				se = new StackEntry(stackEntry);
//				_stackMap.put(stackEntry, se);
//			}
//			se.inc();
//		}
//
//		@Override
//		public String toString()
//		{
//			return StringUtil.toCommaStr(_stackMap, ":", ", ");
//		}
//	}

//	private static class StackEntry
//	{
//		int _count = 0;
////		Map<String, Integer> _stackEntry = new LinkedHashMap<String, Integer;
//
//		public StackEntry(String string)
//		{
//		}
//
//		public StackEntry()
//		{
//			// TODO Auto-generated constructor stub
//		}
//
//		public void inc()
//		{
//			_count++;
//		}
//		
//		@Override
//		public String toString()
//		{
//			return ""+_count;
//		}
//	}
	private static class AseStackTreeView
	extends JFrame
	{
		AseStackTraceAnalyzer _sta = null;
		JTree                 _treeView = new JTree();

		public AseStackTreeView(AseStackTraceAnalyzer sta)
		{
			super();
			_sta = sta;
			init();
		}

		private void init()
		{
			setTitle("Simple ASE Stack Trace Viewer");

			setLayout(new BorderLayout());
			add(createTreeView(), BorderLayout.CENTER);

			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setSize(1024, 768);
		}

		private JPanel createTreeView()
		{
			JPanel panel = SwingUtils.createPanel("Stack Trace Tree View", false);
			panel.setLayout(new MigLayout("insets 0 0 0 0"));

			panel.add(new JScrollPane(_treeView), "push, grow, wrap");

			_treeView.setModel(createTreeModel());

			// Add action listener

			// Focus action listener

			return panel;
			
		}

		private TreeModel createTreeModel()
		{
			DefaultTreeModel tm = new DefaultTreeModel(_sta.createTree());
			return tm;
		}
		
	}
	
	public static void main(String[] args)
	{
		String stackfile = null;
		
		if (args.length > 0) 
			stackfile = args[0];

		if (stackfile == null)
		{
			System.out.println("Usage: sybmonStackFile");
			System.exit(1);
		}
		System.out.println("Stack file is '"+stackfile+"'.");
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			System.out.println("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			System.out.println("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.");
			e.printStackTrace();
		}


		
		AseStackTraceAnalyzer xxx = new AseStackTraceAnalyzer();
		xxx.setFile(stackfile);
		xxx.readFile();
//		xxx.makeDummyTree();
//		xxx.printTree();
		
		AseStackTreeView view = new AseStackTreeView(xxx);
		view.setVisible(true);

	}
}
