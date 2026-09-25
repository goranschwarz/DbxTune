/*******************************************************************************
 * Copyright (C) 2010-2026 Goran Schwarz
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
// NOTE: Lives in the AutoComplete package because AutoCompleteDescWindow (and its constructor) is package-private.
//       This is a NEW class (a subclass), it does NOT shadow any class in autocomplete.jar
package org.fife.ui.autocomplete;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.fife.ui.rsyntaxtextarea.focusabletip.TipUtil;

import com.jidesoft.swing.Resizable;
import com.jidesoft.swing.ResizablePanel;

/**
 * The AutoComplete "description window" with some DbxTune extras on top of the upstream one:
 * <ul>
 *   <li>Resizable from all edges/corners (not only the size grip)</li>
 *   <li>Movable by dragging the bottom bar</li>
 *   <li>A "pin" checkbox: when checked the window is NOT moved every time a new completion entry is selected (see AutoCompletePopupWindow)</li>
 * </ul>
 * Installed with {@link #install(AutoCompletion)}, which uses the AutoComplete 4.x <code>setDescWindowFactory()</code>
 */
public class DbxAutoCompleteDescWindow
extends AutoCompleteDescWindow
{
	private static final long serialVersionUID = 1L;

	private JCheckBox      _isPinned = new JCheckBox("", false);
	private ResizablePanel _resizablePanel;

	/**
	 * Show the description window (to the right of the completion list) and use this class as the description window
	 * @param ac
	 */
	public static void install(AutoCompletion ac)
	{
		ac.setDescWindowVisibility(DescWindowVisibility.ALWAYS);
		ac.setDescWindowFactory(owner -> new DbxAutoCompleteDescWindow(owner, ac));

		// AutoComplete 4.x does 'descArea.setBackground(ac.getDescWindowColor())' which is null by default,
		// so the window got the (grey) panel background instead of the tooltip (yellow) background as in 3.x
		if (ac.getDescWindowColor() == null)
			ac.setDescriptionWindowColor(TipUtil.getToolTipBackground());
	}

	public DbxAutoCompleteDescWindow(Window owner, AutoCompletion ac)
	{
		super(owner, ac);

		// Wrap the upstream content pane in a panel that can be resized from all edges
		JPanel upstreamCp = (JPanel) getContentPane();
		upstreamCp.setBorder(BorderFactory.createEmptyBorder());

		_resizablePanel = new ResizablePanel(new BorderLayout())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Resizable createResizable()
			{
				return new Resizable(this)
				{
					@Override
					public boolean isTopLevel()
					{
						// This is a TOP window, so X Y coordinates should be on screen instead of inside the component
						return true;
					}

					@Override
					public void resizing(int resizeCorner, int newX, int newY, int newW, int newH)
					{
						DbxAutoCompleteDescWindow.this.setBounds(newX, newY, newW, newH);
					}
				};
			}
		};
		_resizablePanel.add(upstreamCp, BorderLayout.CENTER);
		setContentPane(_resizablePanel);
		setDbxBorder();

		// Add the "pin" checkbox to the navigation toolbar (back/forward buttons)
		JToolBar navBar = findNavBar(upstreamCp);
		if (navBar != null)
		{
			_isPinned.setToolTipText("Check if you do NOT want the Description window to move everytime a new entry is selected.");
			_isPinned.setBorder(null);
			_isPinned.setOpaque(false);
			navBar.add(_isPinned);

			// Move the window by dragging the bottom bar (the toolbar's parent) or the toolbar itself
			JComponent bottomPanel = (JComponent) navBar.getParent();
			MouseInputAdapter moveWindow = new MouseInputAdapter()
			{
				private Point lastPoint;

				@Override
				public void mousePressed(MouseEvent e)
				{
					lastPoint = e.getLocationOnScreen();
				}

				@Override
				public void mouseDragged(MouseEvent e)
				{
					Point p = e.getLocationOnScreen();
					if (lastPoint != null)
						setLocation(getX() + p.x - lastPoint.x, getY() + p.y - lastPoint.y);
					lastPoint = p;
				}
			};
			bottomPanel.addMouseListener      (moveWindow);
			bottomPanel.addMouseMotionListener(moveWindow);
			navBar     .addMouseListener      (moveWindow);
			navBar     .addMouseMotionListener(moveWindow);
		}
	}

	/** @return true if the user has "pinned" the window, meaning it should NOT be re-positioned when a new entry is selected */
	public boolean isPinned()
	{
		return _isPinned.isSelected();
	}

	@Override
	public void updateUI()
	{
		super.updateUI(); // Note: upstream sets the tooltip border on the content pane, which is our resizable panel
		setDbxBorder();
	}

	private void setDbxBorder()
	{
		if (_resizablePanel == null) // updateUI() may be called from the super constructor
			return;

		_resizablePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(Color.BLACK, Color.GRAY),
				BorderFactory.createLineBorder(_resizablePanel.getBackground(), 2))); // EXTREME LIGHT GRAY
	}

	/** The upstream navigation toolbar is private, so find it in the component tree */
	private static JToolBar findNavBar(Container c)
	{
		for (Component comp : c.getComponents())
		{
			if (comp instanceof JToolBar)
				return (JToolBar) comp;
			if (comp instanceof Container)
			{
				JToolBar tb = findNavBar((Container) comp);
				if (tb != null)
					return tb;
			}
		}
		return null;
	}
}
