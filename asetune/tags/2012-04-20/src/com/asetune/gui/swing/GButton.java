package com.asetune.gui.swing;

import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import com.asetune.gui.focusabletip.FocusableTip;



/**
 * Just a dummy class on button to extend it a bit
 * <p>
 * Focusable ToolTip, this didn't work as expected... continue to lab on this later...
 * This needs ALOT of more work.
 * @author gorans
 *
 */
public class GButton extends JButton
{
	private static final long	serialVersionUID	= 1L;

	public static final String FOCUSABLE_TIPS_PROPERTY				= "RSTA.focusableTips";

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean _useFocusableTips = false;

	/** The last focusable tip displayed. */
	private FocusableTip _focusableTip = null;

	//--------------------------------------------------------
	// BEGIN: constructors
	//--------------------------------------------------------
	public GButton()
	{
		super();
		init();
	}

	public GButton(Action a)
	{
		super(a);
		init();
	}

	public GButton(Icon icon)
	{
		super(icon);
		init();
	}

	public GButton(String text, Icon icon)
	{
		super(text, icon);
		init();
	}

	public GButton(String text)
	{
		super(text);
		init();
	}

	/**
	 * Called by constructors to initialize common properties
	 */
	protected void init() 
	{
//		setUseFocusableTips(true);
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
	 * Returns the tool tip to display for a mouse event at the given location.  
	 *
	 * @param e The mouse event.
	 */
	@Override
	public String getToolTipText(MouseEvent e) 
	{
//System.out.println(getName()+": ftip="+_focusableTip+", e="+e);
		String text = null;
		if (text == null) 
		{
			text = super.getToolTipText(e);
		}

		// Do we want to use "focusable" tips?
		if (getUseFocusableTips()) 
		{
			if (text != null) 
			{
				if (_focusableTip == null) 
					_focusableTip = new FocusableTip(this);

				_focusableTip.toolTipRequested(e, text);
			}
			// No tooltip text at new location - hide tip window if one is currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text; // Standard tool tips
	}
}
