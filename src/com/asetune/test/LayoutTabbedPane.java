/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.test;

import java.awt.Component;
import java.awt.Dimension;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

public class LayoutTabbedPane
extends JFrame
{
	private static final long serialVersionUID = 1L;

	public LayoutTabbedPane()
	{
		setTitle( "Dummy Tabbed Pane Layout test" );

		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0") );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setLeftComponent(createLeftPanel());
		splitPane.setRightComponent(createRightPanel());
		splitPane.setDividerLocation(400);

		JScrollPane scroll =  new JScrollPane(splitPane);  // should actually just be a VERTICAL Split Pane (Scroll at right hand side)

		panel.add( scroll,               "push, grow, wrap" );
		panel.add( createOkCancelPanel(), "right" );
		
		getContentPane().add( panel );

		pack();
	}

	public JPanel createRightPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0") );

		// Create a tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab( "Page 1", createPage1() );
		tabbedPane.addTab( "Page 2", createPage2() );
		tabbedPane.addTab( "Page 3", createPage3() );
		tabbedPane.addTab( "Page 4", createPage4() );

		panel.add(tabbedPane, "push, grow");

		return panel;
	}

	public JPanel createLeftPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1","grow",""));   // insets Top Left Bottom Right

//		panel.add( new JButton("Create a Profile"));
//		panel.add( new JLabel("Connection Profiles"));
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Connection Profiles");

		DefaultMutableTreeNode n1   = new DefaultMutableTreeNode("Production");
		DefaultMutableTreeNode n1c1 = new DefaultMutableTreeNode("GORAN_1_DS");
		DefaultMutableTreeNode n1c2 = new DefaultMutableTreeNode("XXX_DS");
		DefaultMutableTreeNode n1c3 = new DefaultMutableTreeNode("YYY_DS");
		n1.add(n1c1);
		n1.add(n1c2);
		n1.add(n1c3);

		DefaultMutableTreeNode n2   = new DefaultMutableTreeNode("Test");
		DefaultMutableTreeNode n2c1 = new DefaultMutableTreeNode("TEST_SERVER_1");
		DefaultMutableTreeNode n2c2 = new DefaultMutableTreeNode("TEST_SERVER_2");
		DefaultMutableTreeNode n2c3 = new DefaultMutableTreeNode("TEST_SERVER_3");
		n2.add(n2c1);
		n2.add(n2c2);
		n2.add(n2c3);

		DefaultMutableTreeNode n3   = new DefaultMutableTreeNode("XXXX");
		DefaultMutableTreeNode n3c1 = new DefaultMutableTreeNode("XXX_1_DS");
		DefaultMutableTreeNode n3c2 = new DefaultMutableTreeNode("XXX_2_DS");
		DefaultMutableTreeNode n3c3 = new DefaultMutableTreeNode("XXX_3_DS");
		n3.add(n3c1);
		n3.add(n3c2);
		n3.add(n3c3);

        root.add(n1);
        root.add(n2);
        root.add(n3);
         
		JTree connProfileTree = new JTree(root);
//		connProfileTree.setRootVisible(false);
		connProfileTree.expandRow(0);
		connProfileTree.expandRow(1);
		connProfileTree.expandRow(2);
		
		panel.add( new JCheckBox("Add on Connect", true));
		panel.add( connProfileTree, "push, grow" );
		
		return panel;
	}

	public JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("") );

		panel.add( new JButton("OK"),     "tag ok" );
		panel.add( new JButton("Cancel"), "tag cancel" );

		return panel;
	}

	public JPanel createPage1()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0, wrap 1") );

		panel.add( createSubPanel(), "push, grow" );

		return panel;
	}

	public JPanel createPage2()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0, wrap 1") );

		panel.add( createSubPanel(), "pushx, growx" );
		panel.add( createSubPanel(), "pushx, growx" );
		panel.add( createSubPanel(), "pushx, growx" );
		
		return panel;
	}

	public JPanel createPage3()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0, wrap 1") );

		DefaultTableModel tm = new DefaultTableModel();
		tm.addColumn("col1");
		tm.addColumn("col2");
		tm.addColumn("col3");
		tm.addColumn("col4");
		tm.addColumn("col5");

		for (int i=0; i<8; i++)
		{
			Vector<String> row = new Vector<String>();
			row.add("row - " + i);
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			
			tm.addRow(row);
		}
		JTable table = new JTable(tm);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		calcColumnWidths(table, 0, true);

		table.setPreferredScrollableViewportSize(new Dimension(400, 100));

		panel.add( createSubPanel(),        "grow" );
		panel.add( createSubPanel(),        "grow" );
		panel.add( new JScrollPane(table),  "push, grow" );
		panel.add( new JButton("Some But"), "tag ok" );

		return panel;
	}
	
	public JPanel createPage4()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout("insets 0 0 0 0, wrap 1") );

		DefaultTableModel tm = new DefaultTableModel();
		tm.addColumn("col1");
		tm.addColumn("col2");
		tm.addColumn("col3");
		tm.addColumn("col4");
		tm.addColumn("col5");

		for (int i=0; i<5; i++)
		{
			Vector<String> row = new Vector<String>();
			row.add("row - " + i);
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			row.add(UUID.randomUUID().toString());
			
			tm.addRow(row);
		}
		JTable table = new JTable(tm);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		calcColumnWidths(table, 0, true);

		table.setPreferredScrollableViewportSize(new Dimension(400, 100));

		panel.add( createSubPanel(),       "grow" );
		panel.add( new JScrollPane(table), "push, grow" );
		panel.add( new JButton("XXX But"), "tag ok" );

		return panel;
	}

	public JPanel createSubPanel()
	{
		JPanel panel = new JPanel();
		Border border = BorderFactory.createTitledBorder("Some Title in the border");
		panel.setBorder(border);

		panel.setLayout( new MigLayout() );

		panel.add( new JLabel( "Some Label"       ), "" ); panel.add( new JTextField(""), "pushx, growx, wrap");
		panel.add( new JLabel( "Another Label"    ), "" ); panel.add( new JTextField(""), "pushx, growx, wrap");
		panel.add( new JLabel( "Well a third one" ), "" ); panel.add( new JTextField(""), "pushx, growx, wrap");
		panel.add( new JLabel( "And again"        ), "" ); panel.add( new JTextField(""), "pushx, growx, wrap");
		panel.add( new JLabel( "Some other"       ), "" ); panel.add( new JTextField(""), "pushx, growx, wrap");
		
		return panel;
	}
	
	public static void main(String[] args)
	{
        Runnable runGui = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					System.out.println("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				LayoutTabbedPane mainFrame	= new LayoutTabbedPane();
				mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				mainFrame.setVisible( true );
			}
		};
		
		SwingUtilities.invokeLater(runGui);
	}

	
	public static void calcColumnWidths(JTable table, int onlyXLastRows, boolean onlyIncreaseWith)
	{
		JTableHeader header = table.getTableHeader();
		TableCellRenderer defaultHeaderRenderer = null;
		if (header != null)
			defaultHeaderRenderer = header.getDefaultRenderer();
		TableColumnModel columns = table.getColumnModel();
		TableModel data = table.getModel();
		int margin = columns.getColumnMargin(); // only JDK1.3
		int totalRowCount = data.getRowCount();
		int stopAtRow = 0;
		if (onlyXLastRows > 0)
		{
			stopAtRow = Math.max(stopAtRow, (totalRowCount-onlyXLastRows));
		}
		int totalWidth = 0;
		for (int i = columns.getColumnCount() - 1; i >= 0; --i)
		{
			TableColumn column = columns.getColumn(i);
			int columnIndex = column.getModelIndex();
			int width = -1;
			TableCellRenderer h = column.getHeaderRenderer();
			if (h == null)
				h = defaultHeaderRenderer;
			Component columnHeader = null;
			if (h != null) // Not explicitly impossible
			{
				columnHeader = h.getTableCellRendererComponent(table, 
					column.getHeaderValue(), false, false, -1, i);
				width = columnHeader.getPreferredSize().width;
			}
			for (int row = totalRowCount - 1; row >= stopAtRow; --row)
			{
				TableCellRenderer r = table.getCellRenderer(row, i);
				Component c = r.getTableCellRendererComponent(table, 
					data.getValueAt(row, columnIndex), false, false, row, i);
				width = Math.max(width, c.getPreferredSize().width);
			}
			if (width >= 0)
			{
				column.setPreferredWidth(width + margin); // <1.3: without margin
				// The below didnt seem to work that well
				//if (onlyIncreaseWith && columnHeader != null)
				//{
				//	Dimension dim = columnHeader.getPreferredSize();
				//	dim.width = width + margin;
				//	columnHeader.setPreferredSize(dim);
				//}
			}
			else
				; // ???
			totalWidth += column.getPreferredWidth();
		}
	}
}
