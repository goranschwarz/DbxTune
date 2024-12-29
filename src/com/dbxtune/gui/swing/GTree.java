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

import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import com.dbxtune.gui.focusabletip.FocusableTip;

public class GTree 
extends JTree
{
	private static final long serialVersionUID = 1L;

	public static final String FOCUSABLE_TIPS_PROPERTY				= "RSTA.focusableTips";

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean _useFocusableTips = true;

	/** The last focusable tip displayed. */
	private FocusableTip _focusableTip = null;


	//--------------------------------------------------------
	// BEGIN: constructors
	//--------------------------------------------------------
	public GTree()
	{
		init();
	}

	public GTree(Object[] value)
	{
		super(value);
		init();
	}

	public GTree(Vector<?> value)
	{
		super(value);
		init();
	}

	public GTree(Hashtable<?, ?> value)
	{
		super(value);
		init();
	}

	public GTree(TreeNode root)
	{
		super(root);
		init();
	}

	public GTree(TreeModel newModel)
	{
		super(newModel);
		init();
	}

	public GTree(TreeNode root, boolean asksAllowsChildren)
	{
		super(root, asksAllowsChildren);
		init();
	}
	/**
	 * Called by constructors to initialize common properties
	 */
	protected void init() 
	{
//		ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
//		toolTipManager.registerComponent(this);
		
		setUseFocusableTips(true);
	}
	//--------------------------------------------------------
	// END: constructors
	//--------------------------------------------------------

	/**
	 * Returns whether "focusable" tool tips are used instead of standard
	 * ones.  Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and click links in.
	 *
	 * @return Whether to use focusable tool tips.
	 * @see #setUseFocusableTips(boolean)
	 * @see FocusableTip
	 */
	public boolean getUseFocusableTips() 
	{
		return _useFocusableTips;
	}

	/**
	 * Sets whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and clink links in.
	 *
	 * @param use Whether to use focusable tool tips.
	 * @see #getUseFocusableTips()
	 * @see FocusableTip
	 */
	public void setUseFocusableTips(boolean use) 
	{
		if (use != _useFocusableTips) 
		{
			_useFocusableTips = use;
			firePropertyChange(FOCUSABLE_TIPS_PROPERTY, !use, use);
		}
	}

	/**
	 * Returns the tool tip to display for a mouse event at the given
	 * location.  This method is overridden to give a registered parser a
	 * chance to display a tool tip (such as an error description when the
	 * mouse is over an error highlight).
	 *
	 * @param e The mouse event.
	 */
	@Override
	public String getToolTipText(MouseEvent e) 
	{
		// Check parsers for tool tips first.
		String text = super.getToolTipText(e);

		// Do we want to use "focusable" tips?
		if (getUseFocusableTips()) 
		{
			if (text != null) 
			{
				if (_focusableTip == null) 
					_focusableTip = new FocusableTip(this);

				_focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text; // Standard tool tips
	}
	@Override
	public String getToolTipText() 
	{
		return super.getToolTipText();
	}
	@Override
	public void setToolTipText(String text)
	{
		super.setToolTipText(text);
	}
}
