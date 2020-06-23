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
package com.asetune.gui.focusabletip;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.log4j.Logger;



/**
 * Generates an Focusable ToolTip, which is a pop-up displaying a short description
 * of the component (ToolTip) but also, when focused, allows for displaying a longer help text, 
 * which you can copy past from
 */
public class FocusableTipExtention
implements MouseListener, FocusListener
{
	private static Logger _logger = Logger.getLogger(FocusableTipExtention.class);
	
	/** JCompontent the ToolTip was attached to */
	private JComponent _owner;

	/** JCompontent the ToolTip was attached to */
	private String _tooltip;

	/** The last focusable tip displayed. */
	private FocusableTip _focusableTip = null;
	
	/** Remember last MouseEvent */
	private MouseEvent _mouseEnterEvent = null;

	/** initial delay, should be *slightly* before the ordinary tooltip, so we can "remove" the ordinary tooltip */
	private int _initialDelay = ToolTipManager.sharedInstance().getInitialDelay() - 50;
	
	/** Timer is started on component entry, to display tooltip after X milliseconds. */
	private Timer _openTimer = new Timer(_initialDelay, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			_logger.debug("OpenTimer::::ACTION");

			// Stop the timer
			_openTimer.stop();

			// If someone has changed the tooltip since last time, lets use that one
			String text = _owner.getToolTipText(_mouseEnterEvent);
			if (text != null)
			{
				_tooltip = text;
				_owner.setToolTipText(null);
			}

			text = _tooltip;
			if (text != null) 
			{
				if (_logger.isDebugEnabled())
					_logger.debug("OpenTimer::::ACTION.toolTipRequested(): owner="+_owner);

				_focusableTip.toolTipRequested(_mouseEnterEvent, text);
			}
			else 
			{
				if (_logger.isDebugEnabled())
					_logger.debug("OpenTimer::::ACTION.possiblyDisposeOfTipWindow(): owner="+_owner);

				// No tool tip text at new location - hide tip window if one is currently visible
				_focusableTip.possiblyDisposeOfTipWindow();
			}
		}
	});
	
	/**
	 * Install a component to the focusable tooltip.
	 * @param comp
	 */
	public static void install(JComponent comp)
	{
		new FocusableTipExtention(comp);
	}

	/**
	 * Generates the two display panels that are shown
	 * @param toolTipText
	 * @param helpText
	 * @param owner
	 */
	public FocusableTipExtention(JComponent owner)
	{
		_owner = owner;
		_focusableTip = new FocusableTip(owner);

		// Remember the initial tooltip.
		// Reset the original tooltip, so that the ordinary tooltip system wont show...
		// If the tooltip changes, somebody did setTooltipText(), That will be picked up later when the tooltip is about to be displayed.
		_tooltip = owner.getToolTipText();
		owner.setToolTipText(null);
		
		/* Attach mouseListener to component.
		 * If we attach the toolTip to a JComboBox our MouseListener is not
		 * used, we therefore need to attach the MouseListener to each 
		 * component in the JComboBox
		 */
		if(owner instanceof JComboBox)
		{
			for (int i=0; i<owner.getComponentCount(); i++)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("addMouseListener() to["+i+"]: "+owner.getComponent(i));

				owner.getComponent(i).addMouseListener(this);
				
//				owner.getComponent(i).getAccessibleContext().
			}
		}
		else
		{
			if (_logger.isDebugEnabled())
				_logger.debug("addMouseListener(): "+owner);

			owner.addMouseListener(this) ;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) 
	{
	}

	@Override
	public void mouseEntered(final MouseEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("mouseEntered() e="+e);

//		e.consume();
		_openTimer.start();
		_mouseEnterEvent = e;
	}

	/**
	 * Hide popUp if not 'd' was pressed before and the help popUp is now displayed
	 */
	@Override
	public void mouseExited(MouseEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("mouseExited() e="+e);

		/* stop timer because mouse was moved away from the object */
//		e.consume();
		_openTimer.stop();
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("mousePressed() e="+e);

		_openTimer.stop();
		
		// Close already opened tooltip, since we clicked in the Component
		_focusableTip.possiblyDisposeOfTipWindow();
	}

	@Override
	public void mouseReleased(MouseEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("mouseReleased() e="+e);
	}

	/**
	 * If the focus is lost (user clicked somwhere else) hide popUp.
	 */
	@Override
	public void focusLost(FocusEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("focusLost() e="+e);

		_openTimer.stop();
	}

	@Override
	public void focusGained(FocusEvent e) 
	{
		if (_logger.isDebugEnabled())
			_logger.debug("focusGained() e="+e);
	}

	
	/** 
	 * some simple test code
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		JFrame demo = new JFrame();
		demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		JPanel panel = new JPanel();
		
		//normal Button
		JButton b1 = new JButton("I'm normal Tooltip");
		b1.setToolTipText("This button has a normal toolTip");
		panel.add(b1);
		
		//Button with expandable toolTip
		JButton b2 = new JButton("I'm special Tooltip");
		b2.setToolTipText("Button 2 tooltip");
		FocusableTipExtention.install(b2);
		panel.add(b2);
		
		//JComboBox with expandable toolTip
		String[] cont = {"entry 1", "entry 2", "entry 3"};
		JComboBox<String> cbx1 = new JComboBox<String>(cont);
		cbx1.setToolTipText("Combobox 1 tooltip");
		panel.add(cbx1);
		FocusableTipExtention.install(cbx1);

		//JComboBox with expandable toolTip
		JComboBox<String> cbx2 = new JComboBox<String>(cont);
		cbx2.addItem("String 1");
		cbx2.addItem("String 2");
		cbx2.addItem("String 3");
		cbx2.addItem("String 4");
		
		cbx2.setEditable(true);
		cbx2.setToolTipText("Combobox 2 tooltip");
		panel.add(cbx2);
		FocusableTipExtention.install(cbx2);

		demo.add(panel);
	    demo.pack();
	    demo.setVisible(true);
	}
}
