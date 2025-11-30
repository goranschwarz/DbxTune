/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Timer;
import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.JXTreeTable;

public class TreeTableNavigationEnhancer
implements KeyListener, MouseListener, ActionListener
{
	private JXTreeTable    _treeTable;
	private ActionExecutor _action;
	Timer _moveTimer = new Timer(350, this);

	public interface ActionExecutor
	{
		public void doActionShow();
	}

	public TreeTableNavigationEnhancer(JXTreeTable treeTable, ActionExecutor action) 
	{
		this._treeTable = treeTable;
		this._action    = action;
		
		treeTable.addKeyListener(this);
		treeTable.addMouseListener(this);
	}

	@Override public void keyTyped   (KeyEvent e) { }
	@Override public void keyReleased(KeyEvent e) { }

	@Override
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
		else if (e.getKeyCode() == KeyEvent.VK_UP)
		{
			// Start clock, do _action.doActionShow(); when timer expires
			if ( ! _moveTimer.isRunning() )
				_moveTimer.start();
			else
				_moveTimer.restart();
		}
		else if (e.getKeyCode() == KeyEvent.VK_DOWN)
		{
			// Start clock, do _action.doActionShow(); when timer expires
			if ( ! _moveTimer.isRunning() )
				_moveTimer.start();
			else
				_moveTimer.restart();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		_action.doActionShow();
		_moveTimer.stop();
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

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2 && !e.isConsumed()) 
		{
			e.consume();
			_action.doActionShow();
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
}
