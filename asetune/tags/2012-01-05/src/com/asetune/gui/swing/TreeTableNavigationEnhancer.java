package com.asetune.gui.swing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.JXTreeTable;

public class TreeTableNavigationEnhancer
implements KeyListener 
{
	private JXTreeTable    _treeTable;
	private ActionExecutor _action;

	public interface ActionExecutor
	{
		public void doActionShow();
	}

	public TreeTableNavigationEnhancer(JXTreeTable treeTable, ActionExecutor action) 
	{
		this._treeTable = treeTable;
		this._action    = action;
	}

	public void keyTyped   (KeyEvent e) { }
	public void keyReleased(KeyEvent e) { }

	public synchronized void keyPressed (KeyEvent e) 
	{
//		System.out.println("keyPressed():KeyEvent="+e);

		// If return was pressed, do same as "SHOW"
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
//			int row = _treeTable.getSelectedRow();
//			System.out.println("<---RETURN---> was pressed, currentRow="+row+", value='"+_treeTable.getValueAt(row, 0)+"'.");
			
			_action.doActionShow();
		}

		
		// If selected node is already expanded and if this node has children, select the first of these child nodes.
		// If selected node is NOT expanded, expand it
		if (e.getKeyCode() == KeyEvent.VK_RIGHT)
		{
			if (_treeTable.isExpanded( _treeTable.getSelectedRow()) )
			{
				Object o = _treeTable.getPathForRow(_treeTable.getSelectedRow()).getLastPathComponent();
//				System.out.println("TreeTableNavigationEnhancer2: VK_RIGHT & isExpanded("+_treeTable.getSelectedRow()+"): o = "+o.getClass().getName());
				if ( o instanceof TreeNode)
					if ( ((TreeNode)o).getChildCount() > 0 )
						_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()+1, _treeTable.getSelectedRow()+1);
//				if ( ((TreeNode)_treeTable.getPathForRow(_treeTable.getSelectedRow()).getLastPathComponent()).getChildCount() > 0)
//					_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()+1, _treeTable.getSelectedRow()+1);
			}
			else
				_treeTable.expandRow(_treeTable.getSelectedRow());
		}
		// If selected node is expanded, collapse it
		// If selected node is NOT expanded, and if the node has a parent, select the parent node
		else if (e.getKeyCode() == KeyEvent.VK_LEFT)
		{
			if(_treeTable.isExpanded( _treeTable.getSelectedRow() ))
				_treeTable.collapseRow(_treeTable.getSelectedRow());
			else
			{
				int parentRow = getParentRow(_treeTable.getSelectedRow());
				_treeTable.getSelectionModel().setSelectionInterval(parentRow, parentRow);
//				if (_treeTable.getPathForRow(_treeTable.getSelectedRow()).getParentPath().getPathCount() > 1)
//					_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()-1, _treeTable.getSelectedRow()-1);
			}
		}
	}

	protected int getParentRow(int row) 
	{
		for (int i=row-1; i>=0; i--) 
		{
			if (_treeTable.isExpanded(i)) 
				return i;
		}
		return row;
	}
//	protected int getParentRow(int row) 
//	{
//		TreeTableModel m = _treeTable.getTreeTableModel();
//		Object childValue = _treeTable.getValueAt(row,0);
//		for (int i=row-1; i>=0; i--) 
//		{
//			if (_treeTable.isExpanded(i)) 
//			{
//				System.out.println(_treeTable.getValueAt(i,0));
//				System.out.println("\t" + childValue);
//				Object p = _treeTable.getValueAt(i,0);
//				int x = m.getChildCount(p);
//				for (int j=0; j<x; j++)
//				{
//					if (m.getChild(p,j).equals(childValue))
//						return i;
//				}
//
//				if (m.getIndexOfChild(_treeTable.getValueAt(i,0),childValue) >= 0) 
//				{
//					return i;
//				}
//			}
//		}
//		return row;
//	}
}
