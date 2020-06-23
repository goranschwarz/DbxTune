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

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class TableMenu extends JFrame
{
	public static void main(String[] args)
	{
		TableMenu app = new TableMenu();
		app.getTableModel().addItem("Apple", 1.39, 3);
		app.getTableModel().addItem("Pear", 2.19, 2);
		app.getTableModel().addItem("Banana", 1.52, 4);
		app.setVisible(true);
	}

	private static final String	PROP_CHANGE_QUANTITY	= "CHANGE_QUANTITY";

	private static String getClipboardContents(Object requestor)
	{
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(requestor);
		if ( t != null )
		{
			DataFlavor df = DataFlavor.stringFlavor;
			if ( df != null )
			{
				try
				{
					Reader r = df.getReaderForText(t);
					char[] charBuf = new char[512];
					StringBuffer buf = new StringBuffer();
					int n;
					while ((n = r.read(charBuf, 0, charBuf.length)) > 0)
					{
						buf.append(charBuf, 0, n);
					}
					r.close();
					return (buf.toString());
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
				catch (UnsupportedFlavorException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		return null;
	}

	private static boolean isClipboardContainingText(Object requestor)
	{
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(requestor);
		return t != null && (t.isDataFlavorSupported(DataFlavor.stringFlavor) || t.isDataFlavorSupported(DataFlavor.plainTextFlavor));
	}

	private static void setClipboardContents(String s)
	{
		StringSelection selection = new StringSelection(s);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}

	private JPanel				jContentPane;

	private JScrollPane			jScrollPane;

	private JTable				jTable;

	private ExampleTableModel	tableModel;

	public TableMenu()
	{
		super();
		initialize();
	}

	private JTable getJTable()
	{
		if ( jTable == null )
		{
			jTable = new JTable();
			jTable.setModel(getTableModel());
			jTable.addMouseListener(new MouseAdapter()
			{

				private void maybeShowPopup(MouseEvent e)
				{
					if ( e.isPopupTrigger() && jTable.isEnabled() )
					{
						Point p = new Point(e.getX(), e.getY());
						int col = jTable.columnAtPoint(p);
						int row = jTable.rowAtPoint(p);

						// translate table index to model index
						int mcol = jTable.getColumn(jTable.getColumnName(col)).getModelIndex();

						if ( row >= 0 && row < jTable.getRowCount() )
						{
							cancelCellEditing();

							// create popup menu...
							JPopupMenu contextMenu = createContextMenu(row, mcol);

							// ... and show it
							if ( contextMenu != null && contextMenu.getComponentCount() > 0 )
							{
								contextMenu.show(jTable, p.x, p.y);
							}
						}
					}
				}

				public void mousePressed(MouseEvent e)
				{
					maybeShowPopup(e);
				}

				public void mouseReleased(MouseEvent e)
				{
					maybeShowPopup(e);
				}
			});
		}
		return jTable;
	}

	private void cancelCellEditing()
	{
		CellEditor ce = getJTable().getCellEditor();
		if ( ce != null )
		{
			ce.cancelCellEditing();
		}
	}

	private JPopupMenu createContextMenu(final int rowIndex, final int columnIndex)
	{
		JPopupMenu contextMenu = new JPopupMenu();

		JMenuItem copyMenu = new JMenuItem();
		copyMenu.setText("Copy");
		copyMenu.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Object value = getTableModel().getValueAt(rowIndex, columnIndex);
				setClipboardContents(value == null ? "" : value.toString());
			}
		});
		contextMenu.add(copyMenu);

		JMenuItem pasteMenu = new JMenuItem();
		pasteMenu.setText("Paste");
		if ( isClipboardContainingText(this) && getTableModel().isCellEditable(rowIndex, columnIndex) )
		{
			pasteMenu.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					String value = getClipboardContents(TableMenu.this);
					getTableModel().setValueAt(value, rowIndex, columnIndex);
				}
			});
		}
		else
		{
			pasteMenu.setEnabled(false);
		}
		contextMenu.add(pasteMenu);

		switch (columnIndex)
		{
		case ExampleTableModel.COLUMN_NAME:
			break;
		case ExampleTableModel.COLUMN_PRICE:
			break;
		case ExampleTableModel.COLUMN_QUANTITY:
			contextMenu.addSeparator();
			ActionListener changer = new ActionListener()
			{

				public void actionPerformed(ActionEvent e)
				{
					JMenuItem sourceItem = (JMenuItem) e.getSource();
					Object value = sourceItem.getClientProperty(PROP_CHANGE_QUANTITY);
					if ( value instanceof Integer )
					{
						Integer changeValue = (Integer) value;
						Integer currentValue = (Integer) getTableModel().getValueAt(rowIndex, columnIndex);
						getTableModel().setValueAt(new Integer(currentValue.intValue() + changeValue.intValue()), rowIndex, columnIndex);
					}
				}
			};
			JMenuItem changeItem = new JMenuItem();
			changeItem.setText("+1");
			changeItem.putClientProperty(PROP_CHANGE_QUANTITY, new Integer(1));
			changeItem.addActionListener(changer);
			contextMenu.add(changeItem);

			changeItem = new JMenuItem();
			changeItem.setText("-1");
			changeItem.putClientProperty(PROP_CHANGE_QUANTITY, new Integer(-1));
			changeItem.addActionListener(changer);
			contextMenu.add(changeItem);

			changeItem = new JMenuItem();
			changeItem.setText("+10");
			changeItem.putClientProperty(PROP_CHANGE_QUANTITY, new Integer(10));
			changeItem.addActionListener(changer);
			contextMenu.add(changeItem);

			changeItem = new JMenuItem();
			changeItem.setText("-10");
			changeItem.putClientProperty(PROP_CHANGE_QUANTITY, new Integer(-10));
			changeItem.addActionListener(changer);
			contextMenu.add(changeItem);

			changeItem = null;
			break;
		case ExampleTableModel.COLUMN_AMOUNT:
			break;
		default:
			break;
		}
		return contextMenu;
	}

	private JPanel getJContentPane()
	{
		if ( jContentPane == null )
		{
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

	private JScrollPane getJScrollPane()
	{
		if ( jScrollPane == null )
		{
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getJTable());
		}
		return jScrollPane;
	}

	private ExampleTableModel getTableModel()
	{
		if ( tableModel == null )
		{
			tableModel = new ExampleTableModel();
		}
		return tableModel;
	}

	private void initialize()
	{
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(300, 200);
		this.setContentPane(getJContentPane());
		this.setTitle("Application");
	}

}

class ExampleTableModel extends AbstractTableModel
{
	private static class Item
	{
		private String	name;

		private double	price;

		private int		quantity;

		public Item(String name, double price, int quantity)
		{
			this.name = name;
			this.price = price;
			this.quantity = quantity;
		}

		public double getAmount()
		{
			return quantity * price;
		}
	}

	public static final int	COLUMN_AMOUNT	= 3;

	public static final int	COLUMN_NAME		= 0;

	public static final int	COLUMN_PRICE	= 1;

	public static final int	COLUMN_QUANTITY	= 2;

	private List			items			= new ArrayList();

	public void addItem(String name, double price, int quantity)
	{
		items.add(new Item(name, price, quantity));
	}

	public Class getColumnClass(int columnIndex)
	{
		switch (columnIndex)
		{
		case COLUMN_NAME:
			return String.class;
		case COLUMN_PRICE:
		case COLUMN_AMOUNT:
			return Double.class;
		case COLUMN_QUANTITY:
			return Integer.class;
		default:
			return Object.class;
		}
	}

	public int getColumnCount()
	{
		return 4;
	}

	public String getColumnName(int columnIndex)
	{
		switch (columnIndex)
		{
		case COLUMN_NAME:
			return "Name";
		case COLUMN_PRICE:
			return "Price";
		case COLUMN_QUANTITY:
			return "Quantity";
		case COLUMN_AMOUNT:
			return "Amount";

		default:
			return "# COLUMN " + columnIndex + " #";
		}
	}

	public int getRowCount()
	{
		return items.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Item item = (Item) items.get(rowIndex);
		switch (columnIndex)
		{
		case COLUMN_NAME:
			return item.name;
		case COLUMN_PRICE:
			return new Double(item.price);
		case COLUMN_QUANTITY:
			return new Integer(item.quantity);
		case COLUMN_AMOUNT:
			return new Double(item.getAmount());
		default:
			return null;
		}
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		switch (columnIndex)
		{
		case COLUMN_NAME:
		case COLUMN_PRICE:
		case COLUMN_QUANTITY:
			return true;
		case COLUMN_AMOUNT:
		default:
			return false;
		}
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		Item item = (Item) items.get(rowIndex);
		switch (columnIndex)
		{
		case COLUMN_NAME:
			item.name = aValue.toString();
			fireTableCellUpdated(rowIndex, columnIndex);
			break;
		case COLUMN_PRICE:
			try
			{
				item.price = Double.parseDouble(aValue.toString());
			}
			catch (NumberFormatException ex)
			{
				ex.printStackTrace();
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			fireTableCellUpdated(rowIndex, COLUMN_AMOUNT);
			break;
		case COLUMN_QUANTITY:
			try
			{
				item.quantity = Integer.parseInt(aValue.toString());
			}
			catch (NumberFormatException ex)
			{
				ex.printStackTrace();
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			fireTableCellUpdated(rowIndex, COLUMN_AMOUNT);
			break;
		case COLUMN_AMOUNT:
		default:
			return;
		}
	}
}
