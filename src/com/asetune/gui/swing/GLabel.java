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
package com.asetune.gui.swing;

import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.asetune.gui.focusabletip.FocusableTip;

public class GLabel extends JLabel
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
	public GLabel()
	{
		init();
	}

	public GLabel(String text)
	{
		super(text);
		init();
	}

	public GLabel(Icon image)
	{
		super(image);
		init();
	}

	public GLabel(String text, int horizontalAlignment)
	{
		super(text, horizontalAlignment);
		init();
	}

	public GLabel(Icon image, int horizontalAlignment)
	{
		super(image, horizontalAlignment);
		init();
	}

	public GLabel(String text, Icon icon, int horizontalAlignment)
	{
		super(text, icon, horizontalAlignment);
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
