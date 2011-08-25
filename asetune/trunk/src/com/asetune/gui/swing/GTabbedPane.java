/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;





public class GTabbedPane
    extends JTabbedPane
    implements MouseListener
{
	private static Logger _logger = Logger.getLogger(GTabbedPane.class);
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/
	private static final long serialVersionUID = 6963407617181792852L;

	private ImageIcon _undockedFrameIcon  = new ImageIcon(GTabbedPane.class.getResource("images/undocked_frame_icon.gif"));
	private ImageIcon _iconWinPlus        = new ImageIcon(GTabbedPane.class.getResource("images/window_plus.gif"));
	private ImageIcon _iconWinMinus       = new ImageIcon(GTabbedPane.class.getResource("images/window_minus.gif"));


	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	private JPopupMenu _tabMenu            = null;
	private Vector     _extEntry           = new Vector();
	private int        _lastMouseClickAtTabIndex = -1;
	private GTabbedPane _thisGTabbedPane   = null;

	static
	{
//		_logger.setLevel(Level.TRACE);
	}

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public GTabbedPane()
	{
		super();
		init();
	}
	public GTabbedPane(int tabPlacement)
	{
		super(tabPlacement);
		init();
	}
	public GTabbedPane(int tabPlacement, int tabLayoutPolicy) 
	{
		super(tabPlacement, tabLayoutPolicy);
		init();
	}
	private synchronized void init()
	{
		_thisGTabbedPane = this;
		addMouseListener(this);
		
		_tabMenu = createTabPopupMenu();

		setTabLayoutPolicy( super.getTabLayoutPolicy() );
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	/**
	 * if the menu item is named something using setName() on the Component.
	 * @param name
	 * @return JMenuItem that is named using Component.setName()
	 */
	public JMenuItem getMenuItemNamed(String name)
	{
		JPopupMenu tabMenu = getTabPopupMenu();
		if (tabMenu != null)
		{
			for (int i=0; i<tabMenu.getComponentCount(); i++)
			{
				Component comp = tabMenu.getComponent(i);
				if (comp instanceof JMenuItem)
				{
					JMenuItem mi = (JMenuItem) comp;
		
					if ( name.equals(mi.getName()) )
					{
						_logger.debug("Found JMenuItem for name '"+name+"'.");
						return mi;
					}
				}
			}
		}
		return null;
	}

	/**
	 * if the menu is named something using setName() on the Component.
	 * @param name
	 * @return JMenu that is named using Component.setName()
	 */
	public JMenu getMenuNamed(String name)
	{
		JPopupMenu tabMenu = getTabPopupMenu();
		if (tabMenu != null)
		{
			for (int i=0; i<tabMenu.getComponentCount(); i++)
			{
				Component comp = tabMenu.getComponent(i);
				if (comp instanceof JMenu)
				{
					JMenu m = getMenuNamed((JMenu) comp, name);
					if (m != null)
						return m;
				}
			}
		}
		return null;
	}
	private JMenu getMenuNamed(JMenu menu, String name)
	{
		_logger.trace("getMenuNamed(): name='"+name+"', jmenu.getName()='"+menu.getName()+"'.");
		if ( name.equals(menu.getName()) )
		{
			_logger.debug("Found JMenu for name '"+name+"'.");
			return menu;
		}
		_logger.trace("getMenuNamed(): name='"+name+"', menu.getMenuComponentCount()='"+menu.getMenuComponentCount()+"'.");
		for (int i=0; i<menu.getMenuComponentCount(); i++)
		{
			Component comp = menu.getMenuComponent(i);
			if (comp instanceof JMenu)
			{
				JMenu m = getMenuNamed((JMenu) comp, name);
				if (m != null)
					return m;
			}
		}
		return null;
	}

	public ImageIcon getUndockedFrameIcon(){ return _undockedFrameIcon; }
	public ImageIcon getWindowUndockIcon() { return _iconWinPlus; }
	public ImageIcon getWindowDockIcon()	  { return _iconWinMinus; }
	public void setUndockedFrameIcon(ImageIcon icon) { _undockedFrameIcon = icon; }
	public void setWindowUndockIcon (ImageIcon icon) { _iconWinPlus  = icon; }
	public void setWindowDockIcon   (ImageIcon icon) { _iconWinMinus = icon; }

	/** get the JButtom assigned for to a tab that handles dock/undock of a JPanel to a JFrame */
	public JButton getDockOrUndockButton(int viewIndex)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		return (xe == null) ? null : xe._winOpenCloseButton;
	}
	public JButton getDockOrUndockButton(String tabName)
	{
		TabExtendedEntry xe = getExtendedEntry(tabName);
		return (xe == null) ? null : xe._winOpenCloseButton;
	}

	public void setDockOrUndockButtonGui(JButton button)
	{
		button.setIcon( getWindowUndockIcon() );
		button.setText(null);
		button.setContentAreaFilled(false);
		button.setMargin( new Insets(3,3,3,3) );
		button.setToolTipText(
			"Show the content of this panel in it's own window (and disappear from the TabbedPane).\n" +
			" To put the panel back on the TabbedPane, just close the window.");
	}
	/**
	 * Sets a buttom created by you that should react do dock/undock
	 * @param button        The button itself
	 * @param setDefaultGui true if we should set the GUI behavior to:<br>
	 *                           Icon               - calls getWindowUndockIcon() to set image.<br>
	 *                           "Unframed" button  - setContentAreaFilled(), no border around the button<br>
	 *                           setMargin(3,3,3,3) - 3 pixels as a margin around it.<br>
	 *                           setToolTipText(...a default value...)<br>
	 */
	public void setDockOrUndockButton(int index, JButton button, boolean setDefaultGui)
	{
		TabExtendedEntry xe = getViewExtendedEntry(index);
		if (xe == null)
			return;

		xe._winOpenCloseButton = button;
		if (xe._winOpenCloseButton == null)
			return;

		if (setDefaultGui)
		{
			setDockOrUndockButtonGui(xe._winOpenCloseButton);
		}

		xe._winOpenCloseButton.putClientProperty("TabExtendedEntry", xe);
		
		xe._winOpenCloseButton.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JButton button = (JButton) e.getSource();
				TabExtendedEntry xe = (TabExtendedEntry) button.getClientProperty("TabExtendedEntry");
				
				if (xe != null)
					windowOpenClose(xe._tabIndex);
			}
		});
	}
	public void setDockOrUndockButton(String tabName, JButton button, boolean setDefaultGui)
	{
		setDockOrUndockButton(getModelExtendedEntryIndex(tabName), button, setDefaultGui);
	}

	/** Creates the Button, this can be overrided by a subclass. */
	public JButton createDockOrUndockButton()
	{
		JButton button = new JButton();
		setDockOrUndockButtonGui(button);
		return button;
	}

	/** Get the JMeny attached to the GTabbedPane */
	public JPopupMenu getTabPopupMenu()
	{
		return _tabMenu;
	}

	/** 
	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
	 * If you want to add stuff to the menu, its better to use 
	 * getTabPopupMenu(), then add entries to the menu. This is much 
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createTabPopupMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();

		// Un-Dock
		JMenuItem undock_mi        = new JMenuItem("Un Dock, show content in a window");
		JMenuItem dock_mi          = new JMenuItem("Dock, bring back the window into the tab");
		JMenuItem ontop_mi         = new JMenuItem("On Top, bring un docked window to front");
		JMenuItem tabScroll_mi     = new JRadioButtonMenuItem("Tab Layout Scroll");
		JMenuItem tabWrap_mi       = new JRadioButtonMenuItem("Tab Layout Wrap");
		JMenu     tabOptions_m     = new JMenu("Tab Options");
		JMenuItem tabViewDialog_mi = new JMenuItem("Open Tab View Dialog...");
		JMenuItem hideThisTab_mi   = new JMenuItem("Hide this Tab");
		JMenu     showHideTab_m    = new JMenu("Show or Hide Tab Named");
		JMenu     gotoTab_m        = new JMenu("Goto Tab Name");
		JMenuItem props_mi         = new JMenuItem("Properties...");

		// Name the items
		undock_mi       .setName("UNDOCK");
		dock_mi         .setName("DOCK");
		ontop_mi        .setName("ONTOP");
		tabScroll_mi    .setName("SCROLL");
		tabWrap_mi      .setName("WRAP");
		tabOptions_m    .setName("TAB_OPTIONS");
		gotoTab_m       .setName("GOTO");
		props_mi        .setName("PROPS");

		// sub menus to TAB_OPTIONS
		tabViewDialog_mi.setName("OPEN_TAB_VIEW_DIALOG");
		hideThisTab_mi  .setName("HIDE_THIS_TAB");
		showHideTab_m   .setName("SHOW_OR_HIDE");
		
		// Actions for DOCK/UNDOCK
		ActionListener dockAl = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				windowOpenClose(_lastMouseClickAtTabIndex);
			}
		};
		undock_mi.addActionListener(dockAl);
		dock_mi  .addActionListener(dockAl);

		// Actions ONTOP
		ontop_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TabExtendedEntry xe = getViewExtendedEntry(_lastMouseClickAtTabIndex);
				if (xe == null)
					return;

				if (xe._undockedFrame != null && !xe._isDocked)
					xe._undockedFrame.setVisible(true);
			}
		});

		// Actions SCROLL
		tabScroll_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			}
		});
		// Actions WRAP
		tabWrap_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
			}
		});

		// Actions OPEN_TAB_VIEW_DIALOG
		tabViewDialog_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				GTabbedPaneViewDialog.showDialog(null, _thisGTabbedPane);
			}
		});

		// Actions HIDE_THIS_TAB
		hideThisTab_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TabExtendedEntry xe = getViewExtendedEntry(_lastMouseClickAtTabIndex);
				if (xe == null)
					return;
				setVisibleAtModel(xe._modelIndex, false);
			}
		});

		// Actions PROPS
		props_mi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TabExtendedEntry xe = getViewExtendedEntry(_lastMouseClickAtTabIndex);
				if (xe == null)
					return;

				if (xe._comp != null && xe._comp instanceof ShowProperties)
				{
					ShowProperties sp = (ShowProperties) xe._comp;
					sp.showProperties();
				}
			}
		});

		// Add items to the menu
		popupMenu.add(undock_mi);
		popupMenu.add(dock_mi);
		popupMenu.add(ontop_mi);
		popupMenu.addSeparator();
		popupMenu.add(tabScroll_mi);
		popupMenu.add(tabWrap_mi);
		popupMenu.addSeparator();
		popupMenu.add(tabOptions_m);
		popupMenu.add(gotoTab_m);
		popupMenu.add(props_mi);

		tabOptions_m.add(tabViewDialog_mi);
		tabOptions_m.add(hideThisTab_mi);
		tabOptions_m.add(showHideTab_m);

		return popupMenu;
	}

//	private void windowOpenCloseForSelectedTab()
//	{
//		int    index   = getSelectedIndex();
//		String tabName = getTitleAt(index);
//		_logger.debug("windowOpenClose(): getSelectedIndex()="+index+", title='"+tabName+"'.");
//		windowOpenClose( index );
//	}
	private void windowOpenClose(int viewIndex)
	{
		_logger.debug("windowOpenClose(viewIndex="+viewIndex+"): title='"+getTitleAt(viewIndex)+"'.");

		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe == null)
		{
			_logger.debug("windowOpenClose(viewIndex="+viewIndex+"): title='"+getTitleAt(viewIndex)+"'. NO TabExtendedEntry WAS FOUND, leaving method at top.");
			return;
		}

		if (xe._isDocked)
		{
			if (xe._comp instanceof DockUndockManagement)
			{
				boolean allowed = ((DockUndockManagement)xe._comp).beforeUndock();
				if ( ! allowed )
				{
					JOptionPane.showMessageDialog(this, 
							"The tab named '"+xe._tabName+"' Can't be UnDocked.\n" +
								"The decision for this was taken by the underlying component.",
							"Un Dock", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			// OPEN THE WINDOW
			if (xe._winOpenCloseButton != null)
				xe._winOpenCloseButton.setIcon( getWindowDockIcon() );

			if (xe._undockedFrame == null)
			{
				xe._undockedFrame = new JFrame( xe._tabName );
				xe._undockedFrame.setName(      xe._tabName );
				xe._undockedFrame.setTitle(     xe._tabName );

				// Set frame Icon
				boolean noIcon = true;
				if (xe._icon != null)
				{
					if (xe._icon instanceof ImageIcon )
					{
						xe._undockedFrame.setIconImage( ((ImageIcon) xe._icon).getImage() );
						noIcon = false;
					}
				}
				if (noIcon)
					xe._undockedFrame.setIconImage( getUndockedFrameIcon().getImage() );

				// Size of the new frame
				if (xe._comp != null)
				{
					// get window properties
					GTabbedPaneWindowProps wp = null;
					if (xe._comp instanceof DockUndockManagement)
					{
						wp = ((DockUndockManagement)xe._comp).getWindowProps();
					}
					if (wp == null)
						wp = new GTabbedPaneWindowProps();

					// No saved size was found... go and get preferred size
					if (wp.width == -1 && wp.height == -1)
					{
						Dimension dim = xe._comp.getPreferredSize();
						if (dim.height < 100) dim.height = 100;
						if (dim.width  < 300) dim.width  = 300;
						
						wp.height = dim.height;
						wp.width  = dim.width;
					}
					
					// Now set the size
					xe._undockedFrame.setSize(wp.width, wp.height);

					// Set the position, if we had them saved
					if (wp.posX != -1 && wp.posY != -1)
						xe._undockedFrame.setLocation(wp.posX, wp.posY);
				}

				//------------------------------------
				// Add a Window action listener
				//------------------------------------
				xe._undockedFrame.addWindowListener(new WindowAdapter()
				{
					private void saveWindowSize(boolean open, WindowEvent e)
					{
						String name = e.getWindow().getName();

						TabExtendedEntry xe = getExtendedEntry(name);
						if (xe != null)
						{
							// Set the size and position of the window
							if (xe._comp instanceof DockUndockManagement)
							{
								GTabbedPaneWindowProps wp = new GTabbedPaneWindowProps();
								wp.undocked = open;
								wp.height = e.getWindow().getSize().height;
								wp.width  = e.getWindow().getSize().width;
								wp.posX   = e.getWindow().getX();
								wp.posY   = e.getWindow().getY();
								((DockUndockManagement)xe._comp).saveWindowProps(wp);
							}
						}
					}

					// windowDeactivated is called after windowClosing()...
					//public void windowDeactivated(WindowEvent e) { saveWindowSize(true, e); }
					public void windowActivated  (WindowEvent e) { saveWindowSize(true, e); }
					public void windowOpened     (WindowEvent e) { saveWindowSize(true, e); }

					// DOCK the window when it's closed.
					public void windowClosing(WindowEvent e)
					{
						String name = e.getWindow().getName();
						_logger.debug("FRAME.windowClosing: name = '"+name+"'.");

						TabExtendedEntry xe = getExtendedEntry(name);
						if (xe == null)
						{
							_logger.info("The internal ExtendedEntry for '"+name+"' Can't be found. Can't undock the window...");
						}
						else
						{
							saveWindowSize(false, e);

							// call after undock
							removeTabAt( xe._tabIndex );
							insertTab( xe._tabName, xe._icon, xe, xe._toolTip, xe._tabIndex);
							setSelectedIndex( xe._tabIndex );
							xe._isDocked = true;
							if (xe._winOpenCloseButton != null)
								xe._winOpenCloseButton.setIcon( getWindowUndockIcon() );

							// call after undock
							if (xe._comp instanceof DockUndockManagement)
							{
								((DockUndockManagement)xe._comp).afterDock();
							}
						}
					}
				});
			}

			//------------------------------
			// -- THIS IS WHERE WE TRANSITION FROM TAB -> WINDOW
			//------------------------------
			// When "xe._undockedFrame.getContentPane().add( xe._comp );"
			// is called, the removeTabAt(index) will be called, deleting
			// the Object from the JTabbedPane content
			xe._isDocked = false;
			xe._undockedFrame.getContentPane().add( xe._comp );
			xe._undockedFrame.setVisible(true);

			// Insert a dummy "tabName" under the JTabedPane as a place holder
			// and "gray" it out...
			UndockedTabHolder holder = new UndockedTabHolder(xe);
			insertTab(xe._tabName, xe._icon, holder, xe._toolTip, xe._tabIndex);
			setEnabledAt(xe._tabIndex, false);	

			// Position on next TAB that is ACTIVE.
			boolean foundOneActive = false;
			for(int i=xe._tabIndex; i<getTabCount(); i++)
			{
				if ( isEnabledAt(i) )
				{
					foundOneActive = true;
					setSelectedIndex(i);
					break;
				}
			}
			// If last tab is disabled, position us on the FIRST one
			if ( ! foundOneActive  &&  getTabCount() > 0 )
				setSelectedIndex(0);

			// call after undock
			if (xe._comp instanceof DockUndockManagement)
			{
				((DockUndockManagement)xe._comp).afterUndock();
			}

		}
		else // DOCK the window into the JTabbedPane
		{
			// check if we are allowed to dock 
			if (xe._comp instanceof DockUndockManagement)
			{
				boolean allowed = ((DockUndockManagement)xe._comp).beforeDock();
				if ( ! allowed )
				{
					JOptionPane.showMessageDialog(this, 
							"The tab named '"+xe._tabName+"' Can't be Docked.\n" +
								"The decision for this was taken by the underlying component.",
							"Dock", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			// CLOSE THE WINDOW (or send a message to close it)
			xe._undockedFrame.dispatchEvent( new WindowEvent(xe._undockedFrame, WindowEvent.WINDOW_CLOSING));
		}
	}


	
	
	/*---------------------------------------------------
	** Maintain the "extra" object/slot for every Tab.
	** This will hold extra "stuff" about docked/undocked panels etc...  
	**---------------------------------------------------
	*/
	private TabExtendedEntry getViewExtendedEntry(int viewIndex)
	{
		_logger.trace("getViewExtendedEntry(index="+viewIndex+"), _extEntry.size() = "+_extEntry.size());

		for (Enumeration iter = _extEntry.elements(); iter.hasMoreElements();)
        {
			Object o = iter.nextElement();

			TabExtendedEntry xe = null;
			if (o instanceof UndockedTabHolder)
				xe = ((UndockedTabHolder)o)._xe;
			else
				xe = (TabExtendedEntry) o;

			if (xe != null)
			{
				_logger.debug("getViewExtendedEntry(viewIndex="+viewIndex+") - xe._saveName='"+xe._tabName+"'.");
		        if (xe._tabIndex == viewIndex)
		        {
		        	_logger.debug("getViewExtendedEntry(viewIndex="+viewIndex+") - FOUND ENTRY: "+xe);
		        	return xe;
		        }
			}
        }
    	_logger.debug("getViewExtendedEntry(viewIndex="+viewIndex+") - NOT FOUND -");
		return null; 
	}

	private TabExtendedEntry getModelExtendedEntry(int modelIndex)
	{
		//_logger.trace("getModelExtendedEntry(modelIndex="+modelIndex+"), _extEntry.size() = "+_extEntry.size());

		Object o = _extEntry.get(modelIndex);

		TabExtendedEntry xe = null;
		if (o instanceof UndockedTabHolder)
			xe = ((UndockedTabHolder)o)._xe;
		else
			xe = (TabExtendedEntry) o;
		
		_logger.trace("getModelExtendedEntry(modelIndex="+modelIndex+"), found entry("+(o instanceof UndockedTabHolder ? "UndockedTabHolder" : "TabExtendedEntry")+"): "+xe);
		return xe;
	}

	private int getModelExtendedEntryIndex(String name)
	{
		for (Enumeration iter = _extEntry.elements(); iter.hasMoreElements();)
        {
			Object o = iter.nextElement();

			TabExtendedEntry xe = null;
			if (o instanceof UndockedTabHolder)
				xe = ((UndockedTabHolder)o)._xe;
			else
				xe = (TabExtendedEntry) o;

			_logger.debug("getExtendedEntryIndex(name='"+name+"') - xe._saveName='"+xe._tabName+"'.");
	        if (name.equals(xe._tabName))
	        {
	        	_logger.debug("getExtendedEntryIndex(name='"+name+"') - FOUND ENTRY: "+xe);
//	        	return xe._tabIndex;
	        	return xe._modelIndex;
	        }
        }
    	_logger.debug("getExtendedEntryIndex(name='"+name+"') - NOT FOUND -");
		return -1; 
	}

	private TabExtendedEntry getExtendedEntry(String name)
	{
		if (name == null)
			return null;

		for (Enumeration iter = _extEntry.elements(); iter.hasMoreElements();)
        {
			Object o = iter.nextElement();

			TabExtendedEntry xe = null;
			if (o instanceof UndockedTabHolder)
				xe = ((UndockedTabHolder)o)._xe;
			else
				xe = (TabExtendedEntry) o;

			if (xe != null)
			{
				_logger.debug("getExtendedEntry(name='"+name+"') - xe._saveName='"+xe._tabName+"'.");
		        if (name.equals(xe._tabName))
		        {
		        	_logger.debug("getExtendedEntry(name='"+name+"') - FOUND ENTRY: "+xe);
		        	return xe;
		        }
			}
        }
    	_logger.debug("getExtendedEntry(name='"+name+"') - NOT FOUND -");
		return null; 
	}


	/*---------------------------------------------------
	** BEGIN: get info from the TabExtendedEntry
	**---------------------------------------------------
	*/
	/** if tab is un-docked, meaning it's in  it's own window */
	public boolean isTabUnDocked(String name)
	{
		TabExtendedEntry xe = getExtendedEntry(name);
		if (xe == null)
			return false;
		return ! xe._isDocked;
	}
	/** if tab is un-docked, meaning it's in  it's own window */
	public boolean isTabUnDocked(int viewIndex)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe == null)
			return false;
		return ! xe._isDocked;
	}
	/** if tab is un-docked, meaning it's in  it's own window, get the Window 
	 * if the tab is docked (not in it's own window) return null*/
	public JFrame getTabUnDockedFrame(String name)
	{
		TabExtendedEntry xe = getExtendedEntry(name);
		if (xe == null)
			return null;
		return (! xe._isDocked) ? xe._undockedFrame : null;
	}
	/** if tab is un-docked, meaning it's in  it's own window, get the Window 
	 * if the tab is docked (not in it's own window) return null*/
	public JFrame getTabUnDockedFrame(int viewIndex)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe == null)
			return null;
		return (! xe._isDocked) ? xe._undockedFrame : null;
	}
	/*---------------------------------------------------
	** END: get info from the TabExtendedEntry
	**---------------------------------------------------
	*/

	private void printExtendedEntryTable(String preStr)
	{
		for (Enumeration iter = _extEntry.elements(); iter.hasMoreElements();)
        {
			Object o = iter.nextElement();
			System.out.println(preStr + o);
        }
	}

	/*---------------------------------------------------
	** BEGIN: extending JTabbedPane methods
	**---------------------------------------------------
	*/
	public void setVisibleAtModel(List<String> tabNameList, boolean toVisible)
	{
		for (String tabName : tabNameList)
			setVisibleAtModel(tabName, toVisible);
	}
	public void setVisibleAtModel(String tabName, boolean toVisible)
	{
		int modelIndex = getModelExtendedEntryIndex(tabName);
		if (modelIndex < 0)
		{
			_logger.warn("setVisibleAtModel(tabName='"+tabName+"', toVisible="+toVisible+") Couldn't find tabName. modelIndex="+modelIndex);
			return; // FIXME: maybe throw an exception here
		}
		setVisibleAtModel(modelIndex, toVisible);
	}
	public void setVisibleAtModel(int modelIndex, boolean toVisible)
	{
		TabExtendedEntry xe = getModelExtendedEntry(modelIndex);
		if (xe == null)
			return;

		// Already in this visibly state
		if (xe._isVisible == toVisible)
			return;

		// Make it visible
		if (toVisible)
		{
			// Try to figure out where to insert the visible tab.
			int atIndex = getInvisibleTabInsertionPoint(getTabOrder(), xe);
			super.insertTab(xe._tabName, xe._icon, xe._comp, xe._toolTip, atIndex);
			xe._isVisible = true;
		//	xe._tabIndex = atIndex;
			fixTabExtendedEntry();
		}
		// HIDE it
		else
		{
//printExtendedEntryTable("BEFORE: ");

			// Get next tabName, and set that to "I was removed before tabName"
			int nextIndex = indexOfTab(xe._tabName) + 1;
			xe._rmBeforeTab = (nextIndex >= getTabCount()) ? null : getTitleAt(nextIndex);

			super.removeTabAt(xe._tabIndex);
			xe._isVisible = false;
		//	xe._tabIndex  = -1;
			fixTabExtendedEntry();
//
//			for (int i=modelIndex+1; i<_extEntry.size(); i++)
//			{
//				Object o = _extEntry.get(i);
//				if (o instanceof TabExtendedEntry)
//				{
//					xe = (TabExtendedEntry)o;
//					xe._tabIndex--;
////					xe._viewIndex--;
//				}
//			}
//printExtendedEntryTable("AFTER: ");
		}
	}

	private void fixTabExtendedEntry()
	{
		List<String> visibleTabs = getTabOrder();
		for (int i=0; i<_extEntry.size(); i++)
		{
			TabExtendedEntry xe = getModelExtendedEntry(i);
			if (xe != null)
			{
				xe._modelIndex = i;
				xe._tabIndex   = visibleTabs.indexOf(xe._tabName);
			}
		}
	}

	private int getInvisibleTabInsertionPoint(List<String> colList, TabExtendedEntry xe)
	{
		if (colList == null)
			throw new RuntimeException("getInvisibleTabInsertionPoint() colList, can't be null.");
		if (xe == null || (xe != null && xe._rmBeforeTab == null) )
			return colList.size();
		String rmBeforeTab = xe._rmBeforeTab;

		int atIndex = -1;

		// loop X times, if current 'rmBeforeTab' is NOT in visible list
		// Try to get the "hidden model entry" and get that _rmBeforeTab 
		// to check if that is within the visible list
		for (int m=0; m<256; m++)
		{
			if (rmBeforeTab == null)
				break;

			atIndex = colList.indexOf(rmBeforeTab);

			// Entry FOUND
			if (atIndex >= 0)
				break;

			// NOT found, get the hidden entry and try again 
			TabExtendedEntry x1 = getExtendedEntry(rmBeforeTab);
			if (x1 != null)
				rmBeforeTab = x1._rmBeforeTab;
			else
				rmBeforeTab = null;
		}
			
		if (atIndex < 0 || atIndex > colList.size())
			atIndex = colList.size();

		return atIndex;
	}

	public boolean isVisibleAtModel(String tabName)
	{
		int modelIndex = getModelExtendedEntryIndex(tabName);
		if (modelIndex < 0)
		{
			_logger.warn("isVisibleAtModel(tabName='"+tabName+"') Couldn't find tabName. modelIndex="+modelIndex);
			return false; // FIXME: maybe throw an exception here
		}
		return isVisibleAtModel(modelIndex);
	}
	public boolean isVisibleAtModel(int modelIndex)
	{
		TabExtendedEntry xe = getModelExtendedEntry(modelIndex);
		if (xe == null)
			return false;
		return xe._isVisible;
	}

	/**
	 * Returns the number of visible tabs in this <code>tabbedpane</code>.
	 */
	public int getVisibleTabCount() 
	{
//		int count = 0;
//		for (int i=0; i<_extEntry.size(); i++)
//		{
//			Object o = _extEntry.get(i);
//			if (o instanceof TabExtendedEntry)
//			{
//				if (((TabExtendedEntry)o)._isVisible)
//					count++;
//			}
//			else
//				count++;
//		}
//		return count;
		return getTabCount();
	}

	/**
	 * Returns the number of components attached to this <code>tabbedpane</code> visible or non visible.
	 */
	public int getModelTabCount() 
	{
		return _extEntry.size();
	}

	/**
	 * Returns the tab title at <code>modelIndex</code>.
	 */
	public String getTitleAtModel(int modelIndex) 
	{
		return ((TabExtendedEntry)_extEntry.get(modelIndex))._tabName;
	}

	/**
	 * Returns the tab icon at <code>modelIndex</code>.
	 */
	public Icon getIconAtModel(int modelIndex) 
	{
		return ((TabExtendedEntry)_extEntry.get(modelIndex))._icon;
	}

	/**
	 * Returns the tab tooltip text at <code>modelIndex</code>.
	 */
	public String getToolTipTextAtModel(int modelIndex) 
	{
		return ((TabExtendedEntry)_extEntry.get(modelIndex))._toolTip;
	}


	/** 
	 * Returns the component at <code>modelIndex</code>. 
	 */
	public Component getComponentAtModel(int modelIndex) 
	{
		return ((TabExtendedEntry)_extEntry.get(modelIndex))._comp;
	}

	/**
	 * Get the tab names in which order they were added.
	 * @return A list of Strings (tab names)
	 */
	public List<String> getModelTabOrder()
	{
		List<String> tabOrder = new ArrayList<String>();

		for (int t=0; t<getModelTabCount(); t++)
			tabOrder.add(getTitleAtModel(t));

		return tabOrder;
	}

	/**
	 * Get the tab names in view order (non visible tabs wont be in the list)
	 * @return A list of Strings (tab names)
	 * @see getTabOrder(boolean includeNonVisible)
	 */
	public List<String> getTabOrder()
	{
		return getTabOrder(false);
	}

	/**
	 * Get the tab names in view order
	 * @param includeNonVisible true if you also want the non visible columns in the list
	 * @return A list of Strings (tab names)
	 */
	public List<String> getTabOrder(boolean includeNonVisible)
	{
		List<String> tabOrder = new ArrayList<String>();

		// Add the visible ones
		for (int t=0; t<getTabCount(); t++)
			tabOrder.add(getTitleAt(t));

		// Then, Add the non-visible ones
		if (includeNonVisible)
		{
			for (int t=0; t<getModelTabCount(); t++)
			{
				TabExtendedEntry xe = getModelExtendedEntry(t);
				if (xe != null && ! xe._isVisible)
				{
//					tabOrder.add(getTitleAtModel(t));
					int atIndex = getInvisibleTabInsertionPoint(tabOrder, xe);
					tabOrder.add(atIndex, getTitleAtModel(t));
				}
			}
		}

		return tabOrder;
	}


	/**
	 * Set the order of the tabs
	 * <p>
	 * If the passed array doesn't contain all the columns, those columns will be at the end.
	 * 
	 * @param newTabOrder a String[] of columns in which order they should be displayed.
	 */
	public void setTabOrder(String[] newTabOrder)
	{
		List<String> newTabOrderList = new ArrayList<String>();
		for (String str : newTabOrder)
			newTabOrderList.add(str);
		setTabOrder(newTabOrderList);
	}

	/**
	 * Set the order of the tabs
	 * <p>
	 * If the passed list doesn't contain all the columns, those columns will be at the end.
	 * 
	 * @param newTabOrder a List of columns in which order they should be displayed.
	 */
	public void setTabOrder(List<String> newTabOrder)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("New tab order START: "+newTabOrder);

		// Grab current tab order, and add the ones that are not included in input 'newTabOrder'
		List<String> currentTabOrder = getTabOrder();
		currentTabOrder.removeAll(newTabOrder);
		newTabOrder.addAll(currentTabOrder);

		if (_logger.isDebugEnabled())
			_logger.debug("New tab order FIXED: "+newTabOrder);

		// remove ChangeListener(s) while we delete/inserts the tables, then restore them again
		ChangeListener[] saveListeners = getChangeListeners();
		for (ChangeListener cl : saveListeners)
			removeChangeListener(cl);

		try
		{
			// We invoke removeTabAt for each tab, otherwise we may end up
			// removing Components added by the UI.
			int tabCount = getTabCount();
			while (tabCount-- > 0) 
			{
				// Use the super to remove all tabs, then we will add them again
				super.removeTabAt(tabCount);
			}
	
			int newIndex = 0;
			for (String tabName : newTabOrder)
			{
				TabExtendedEntry xe = getExtendedEntry(tabName);
				if (xe == null)
				{
					_logger.info("Can't find tab named '"+tabName+"' in the current tab pane, skipping this one when setting tab order.");
				}
				else
				{
					if (xe._isVisible)
						super.insertTab(xe._tabName, xe._icon, xe._comp, xe._toolTip, newIndex++);
				}
			}
			
			// fix all TabExtendedEntry members, to make sure they are correct...
			fixTabExtendedEntry();
		}
		catch(Throwable t)
		{
			_logger.warn("While remove/restoring all the tabs an Exception occurred, which will be discarded. Now I will restore all ChangeListeners. Caught: "+t, t);
		}
		finally
		{
			// restore the ChangeListener(s)
			for (ChangeListener cl : saveListeners)
				addChangeListener(cl);
		}
	}

	/**
	 * Set the Order of how tabs are displayed<br>
	 * If the entry in the list looks like 'TabName={true|false}' it means the visibility can be changed.
	 * @param entries in the form: "TabName1[={true|false}], TabName2[={true|false}]"
	 */
	public void setTabOrderAndVisibility(String entries)
	{
		if (entries == null)
			return;

		List<String> listEntries = new ArrayList<String>();
		String[] sa = entries.split(",");
		for (int i=0; i<sa.length; i++)
			listEntries.add(sa[i].trim());

		setTabOrderAndVisibility(listEntries);
	}

	/**
	 * Set the Order of how tabs are displayed<br>
	 * If the entry in the list looks like 'TabName={true|false}' it means the visibility can be changed.
	 * @param entryList in the form: TabName1[={true|false}], TabName2[={true|false}]
	 */
	public void setTabOrderAndVisibility(String[] entries)
	{
		if (entries == null)
			return;

		List<String> entryList = new ArrayList<String>();
		for (String str : entries)
			entryList.add(str);

		setTabOrderAndVisibility(entryList);
	}

	/**
	 * Set the Order of how tabs are displayed<br>
	 * If the entry in the list looks like 'TabName={true|false}' it means the visibility can be changed.
	 * <p>
	 * If the tab is NOT part if the list, but part of the JTabbedPane the tab will be set to visible.
	 * @param entryList in the form: TabName1[={true|false}], TabName2[={true|false}]
	 */
	public void setTabOrderAndVisibility(List<String> entryList)
	{
		if (entryList == null)
			return;

		List<String> tabOrder = new ArrayList<String>(); // store in what order to display tabs
		List<String> showTabs = new ArrayList<String>(); // store tab names that should be visible
		List<String> hideTabs = new ArrayList<String>(); // store tab names that should be hidden

		// A good starting point is that "all" tabs should be showed (even non visible ones).
		showTabs.addAll(getTabOrder(true));

		// loop the list
		for (String entry : entryList)
		{
			// split the entry at '='
			String[] sa = entry.split("=");
			String tabName = sa[0].trim();
			String tabOpt = "true";
			if (sa.length >= 2)
				tabOpt = sa[1].trim();

			// Add to tabOrder, and remove from showTabs if it shouldn't be visible. 
			tabOrder.add(tabName);
			if (tabOpt.equalsIgnoreCase("false"))
			{
				showTabs.remove(tabName);
				hideTabs.add(tabName);
			}
		}

		// Grab current tab order, and add the ones that are not included in input 'newTabOrder'
//		List<String> currentTabOrder = getTabOrder();
//		currentTabOrder.removeAll(tabOrder);
//		tabOrder.addAll(currentTabOrder);

		if (_logger.isTraceEnabled())
		{
			_logger.trace("setTabOrderAndVisibility(List): showTabs="+showTabs);
			_logger.trace("setTabOrderAndVisibility(List): tabOrder="+tabOrder);
			_logger.trace("setTabOrderAndVisibility(List): hideTabs="+hideTabs);
		}

		// now make the actions
//System.out.println("setVisibleAtModel(showTabs, true); showTabs="+showTabs);
		setVisibleAtModel(showTabs, true);
//System.out.println("setTabOrder(tabOrder); tabOrder="+tabOrder);
		setTabOrder(tabOrder);
//System.out.println("setVisibleAtModel(hideTabs, false); hideTabs="+hideTabs);
		setVisibleAtModel(hideTabs, false);
//printExtendedEntryTable("BEFORE EXIT:setTabOrderAndVisibility()");
	}

	/**
	 * Return a String representation of the tab order and it's visiblility.
	 * @return a string in the form: tabName1={true|false}, ...<br>
	 * So if the tab had 3 tabs "t1, t2, t3", where only "t1, t3" was visible, the out string would be:
	 * "t1=true, t2=false, t3=true"
	 */
	public String getTabOrderAndVisibility()
	{
		List<String> tabOrderList = getTabOrder(true);
		StringBuilder sb = new StringBuilder();

		for (Iterator<String> it = tabOrderList.iterator(); it.hasNext();)
		{
			String tabName = it.next();

			sb.append(tabName).append("=").append(isVisibleAtModel(tabName));
			if (it.hasNext())
				sb.append(", ");
		}
		return sb.toString();
	}

	
//  public void moveTab(int fromIndex, int toIndex)
//    {
//    	// FIXME: implement this
//    }

	/*---------------------------------------------------
	** END: extending JTabbedPane methods
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: overloaded methods from: JTabbedPane
	**---------------------------------------------------
	*/

	/** 
	 * This also Updates the pupup menu in what mode we are in. 
	 * Then it calls super... 
	 */
	@Override
	public void setTabLayoutPolicy(int policy)
	{
		JMenuItem wrap   = getMenuItemNamed("WRAP");
		JMenuItem scroll = getMenuItemNamed("SCROLL");

		if (wrap   != null &&   wrap instanceof JRadioButtonMenuItem)
			((JRadioButtonMenuItem)wrap)  .setSelected( policy == JTabbedPane.WRAP_TAB_LAYOUT );

		if (scroll != null && scroll instanceof JRadioButtonMenuItem)
			((JRadioButtonMenuItem)scroll).setSelected( policy == JTabbedPane.SCROLL_TAB_LAYOUT );
		
		super.setTabLayoutPolicy(policy);
	}

//	/**	  Adds a component with a tab title defaulting to the name of the component which is the result of calling component.getName. */
//	public Component add(Component component)
//	{
//		_logger.trace("add(comp): comp="+component);
//		return super.add(component);
//	}
//
//	/**	  Adds a component at the specified tab index with a tab title defaulting to the name of the component. */
//	public Component add(Component component, int index)
//	{
//		_logger.trace("add(comp, index): component="+component);
//		return super.add(component, index);
//	}
//
//	/**	  Adds a component to the tabbed pane. */
//	public void add(Component component, Object constraints)
//	{
//		_logger.trace("add(comp, constraints): component="+component+", constraints="+constraints);
//		super.add(component, constraints);
//	}
//
//	/**	  Adds a component at the specified tab index. */
//	public void add(Component component, Object constraints, int index)
//	{
//		_logger.trace("add(comp, constraints, index): index="+index+", component="+component+", constraints="+constraints);
//		super.add(component, constraints, index);
//	}
//
//	/**	  Adds a component with the specified tab title. */
//	public Component add(String title, Component component)
//	{
//		_logger.trace("add(title, comp): title="+title+", component="+component);
//		return super.add(title, component);
//	}
//
//	/**	  Adds a component represented by a title and no icon. */
//	public void addTab(String title, Component component)
//	{
//		_logger.trace("addTab(title, comp): title="+title+", component="+component);
//		super.addTab(title, component);
//	}
//
//	/**	  Adds a component represented by a title and/or icon, either of which can be null. */
//	public void addTab(String title, Icon icon, Component component)
//	{
//		_logger.trace("addTab(title, icon, comp): title="+title+", icon='"+icon+"', component="+component);
//		super.addTab(title, icon, component);
//	}
//
//	/**	  Adds a component and tip represented by a title and/or icon, either of which can be null. */
//	public void addTab(String title, Icon icon, Component component, String tip)
//	{
//		_logger.trace("addTab(title, icon, comp, tip): title="+title+", icon='"+icon+"', component="+component+", tip="+tip);
//		super.addTab(title, icon, component, tip);
//	}



	/** Inserts a component, at index, represented by a title and/or icon, either of which may be null. */
	@Override
	public void insertTab(String title, Icon icon, Component component, String tip, int index)
	{
		_logger.trace("insertTab(title, icon, comp, tip, index): index="+index+", title="+title+", icon='"+icon+"', component="+component+", tip="+tip);

		boolean newComponent = false;
		if (component instanceof UndockedTabHolder)
		{
			// Put the UndockedTabHolder in the Map
			// Otherwise an entry will be missing... 
			// _extEntry.size() would be smaller than JTabbedPane."TabCount"() 
			_extEntry.insertElementAt(component, index);
		}
		else if (component instanceof TabExtendedEntry)
		{
			TabExtendedEntry xe = (TabExtendedEntry) component;

			// When undocked window is closing we will pass in the 
			// TabExtendedEntry here... so we need to set component
			// to the "real" component...
			component = xe._comp;

			// Replace UndockedTabHolder place-holder object
			// with the real TabExtendedEntry.
			_extEntry.set(index, xe);
		}
		else
		{
			TabExtendedEntry xe = new TabExtendedEntry();
			_extEntry.insertElementAt(xe, index);
			
			newComponent = true;

			// Initialize the added entry
			xe._isDocked      = true;
			xe._tabIndex      = index;
			xe._tabName       = title;
			xe._icon          = icon;
			xe._toolTip       = tip;
			xe._comp          = component;
			xe._undockedFrame = null;
			xe._isVisible     = true;
			xe._rmBeforeTab   = null;
			xe._modelIndex    = index;

			xe._winOpenCloseButton = createDockOrUndockButton();
			if (xe._winOpenCloseButton != null)
				setDockOrUndockButton(index, xe._winOpenCloseButton, true);
			
			// 
			if (component instanceof DockUndockManagement)
			{
				JButton button = ((DockUndockManagement)component).getDockUndockButton();
				if (button != null)
					setDockOrUndockButton(index, button, true);
			}

		}

		// Add it to JTabbedPane
		super.insertTab(title, icon, component, tip, index);
		fixTabExtendedEntry();


		// get window properties
		if (newComponent)
		{
			if (component instanceof DockUndockManagement)
			{
				GTabbedPaneWindowProps wp = ((DockUndockManagement)component).getWindowProps();
				if (wp != null)
				{
					if (wp.undocked)
						windowOpenClose(index);
				}
			}
		}

	}
	
//	/**	  Removes the specified Component from the JTabbedPane. */
//	public void remove(Component component)
//	{
//		_logger.trace("remove(comp): component="+component);
//		int index = indexOfComponent(component);
//
//		super.remove(component);
//	}
//
//	/**	  Removes the tab and component which corresponds to the specified index. */
//	public void remove(int index)
//	{
//		_logger.trace("remove(index): index="+index);
//		super.remove(index);
//	}
//
//	/**	  Removes all the tabs and their corresponding components from the tabbedpane. */
//	public void removeAll()
//	{
//		_logger.trace("removeAll()");
//		super.removeAll();
//	}

	/**	  
	 * Removes the tab at index.<p>
	 * This is called by all other add* methods
	 */
	@Override
	public void removeTabAt(int viewIndex)
	{
		_logger.trace("removeTabAt(index): viewIndex="+viewIndex);

//printExtendedEntryTable("BEFORE: ");
		Component component = getComponentAt(viewIndex);
		if (component instanceof UndockedTabHolder)
		{
		}
		else if (component instanceof TabExtendedEntry)
		{
		}
		else
		{
//			TabExtendedEntry xeAtViewIndex = getViewExtendedEntry(viewIndex);
//			xeAtViewIndex._lastViewIndex = viewIndex;

			_logger.trace("removeTabAt(index): viewIndex="+viewIndex+", comp='"+component.getClass().getName()+"', REMOVING TabExtendedEntry.");
			_extEntry.removeElementAt(viewIndex);

			fixTabExtendedEntry();
//			// For higher TabExtendedEntry entries decrease some members
//			for (int i=viewIndex+1; i<_extEntry.size(); i++)
//			{
//				Object o = _extEntry.get(i);
//				if (o instanceof TabExtendedEntry)
//				{
//					TabExtendedEntry xe = (TabExtendedEntry)o;
//					xe._modelIndex--;
//					xe._tabIndex--;
////					xe._lastViewIndex--;
//				}
//			}
		}
//printExtendedEntryTable("AFTER: ");

		super.removeTabAt(viewIndex);
	}

	/** Sets the icon at index to icon which can be null. This does not set disabled icon at icon. If the new Icon is different than the current Icon and disabled icon is not explicitly set, the LookAndFeel will be asked to generate a disabled Icon. To explicitly set disabled icon, use setDisableIconAt(). An internal exception is raised if there is no tab at that index. */
	@Override
	public void setIconAt(int viewIndex, Icon icon)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe != null)
			xe._icon = icon;

	    super.setIconAt(viewIndex, icon);
	}

	/** Sets the tooltip text at index to toolTipText which can be null. */
	@Override
	public void setToolTipTextAt(int viewIndex, String toolTipText)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe != null)
			xe._toolTip = toolTipText;

		super.setToolTipTextAt(viewIndex, toolTipText);
	}
	
	/** Sets the component at index to component. */
	@Override
	public void setComponentAt(int viewIndex, Component component)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe != null)
			xe._comp = component;

		super.setComponentAt(viewIndex, component);
	}

	/** Sets the title at index to title which can be null. */
	@Override
	public void setTitleAt(int viewIndex, String title)
	{
		TabExtendedEntry xe = getViewExtendedEntry(viewIndex);
		if (xe != null)
			xe._tabName = title;

		super.setTitleAt(viewIndex, title);
	}

	/*---------------------------------------------------
	** END: overloaded methods from: JTabbedPane
	**---------------------------------------------------
	*/

	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing: MouseListener
	**---------------------------------------------------
	*/
	public void mouseEntered (MouseEvent e) {}
	public void mouseExited  (MouseEvent e) {}
	public void mousePressed (MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked (MouseEvent e)
	{
		String    tabName  = null;
		Component tabComp  = null;
		int       tabIndex = -1;

		Point p = e.getPoint();
		tabIndex = indexAtLocation(p.x, p.y);
		if (tabIndex != -1)
		{
			tabName = getTitleAt(tabIndex);
			tabComp = getComponentAt(tabIndex);
		}

		// For some operations we can't use "current selected tab"
		// so wee need to remember the last tab index where a mouse 
		// button was pressed...
		// This is for example Right Click, then choosing "dock" on a disabled tab
		_lastMouseClickAtTabIndex = tabIndex;
		
		// if is RIGHT CLICK
		if ( SwingUtilities.isRightMouseButton(e) )
//		if (e.getButton() != MouseEvent.BUTTON1)
		{
			if (e.getClickCount() == 1)
			{
				_logger.debug("SINGLE-RIGHT-CLICK");
				JPopupMenu tabMenu = getTabPopupMenu();
	
				if (tabMenu != null)
				{
					JMenuItem dock   = getMenuItemNamed("DOCK");
					JMenuItem undock = getMenuItemNamed("UNDOCK");
					JMenuItem ontop  = getMenuItemNamed("ONTOP");
					JMenuItem props  = getMenuItemNamed("PROPS");
	
					// Not OVER any tab, disable some menu items.
					if (tabIndex == -1)
					{
						_logger.debug("SINGLE-RIGHT-CLICK: NOT OVER TAB: Disables menu items 'dock, undock, show'");
						setEnabledX(dock,   false);
						setEnabledX(undock, false);
						setEnabledX(ontop,  false);
					}
					else // we ARE OVER a tab item when mouse was pressed
					{ 
						if ( isEnabledAt(tabIndex) )
						{
							_logger.debug("SINGLE-RIGHT-CLICK: OVER ENABLED TAB: "+tabName);
							setEnabledX(dock,   false);
							setEnabledX(undock, true);
							setEnabledX(ontop,  false);
						}
						else
						{
							_logger.debug("SINGLE-RIGHT-CLICK: OVER UN-ENABLED TAB: "+tabName);
							setEnabledX(dock,   true);
							setEnabledX(undock, false);
							setEnabledX(ontop,  true);
						}

						if (tabComp instanceof ShowProperties)
							setEnabledX(props, true);
						else
							setEnabledX(props, false);
					}
					
					JMenu gotoMenu = getMenuNamed("GOTO");
					if (gotoMenu != null)
					{
						gotoMenu.removeAll();
						for(int i=0; i<getTabCount(); i++)
						{
							JMenuItem mi = new JMenuItem(getTitleAt(i), getIconAt(i));
							mi.putClientProperty("tabIndex", new Integer(i));
							mi.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									Object o = e.getSource();
									if (o instanceof JMenuItem)
									{
										JMenuItem mi = (JMenuItem) o;
										Object cp = mi.getClientProperty("tabIndex");
										if (cp != null && cp instanceof Integer)
										{
											int viewIndex = ((Integer)cp).intValue();
											if (isEnabledAt(viewIndex))
												setSelectedIndex( viewIndex );
											else
											{
												TabExtendedEntry xe = getViewExtendedEntry( viewIndex );
												if (xe == null)
													return;

												if (xe._undockedFrame != null && !xe._isDocked)
													xe._undockedFrame.setVisible(true);
												// windowOpenClose(index);
											}
										}
										_logger.debug("GOTO: "+mi.getText());
									}
								}
							});
							gotoMenu.add(mi);
						}
					}
	
					JMenu showHideMenu = getMenuNamed("SHOW_OR_HIDE");
//					System.out.println("SHOW_OR_HIDE MENU="+showHideMenu);
					if (showHideMenu != null)
					{
						System.out.println("SHOW_OR_HIDE: ");
						showHideMenu.removeAll();
						for(int modelIndex=0; modelIndex<getModelTabCount(); modelIndex++)
						{
							TabExtendedEntry xe = getModelExtendedEntry(modelIndex);
							JMenuItem mi = new JCheckBoxMenuItem(xe._tabName, xe._icon, xe._isVisible);
							mi.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									Object o = e.getSource();
									if (o instanceof JCheckBoxMenuItem)
									{
										JCheckBoxMenuItem cmi = (JCheckBoxMenuItem) o;
										TabExtendedEntry xei = getExtendedEntry(cmi.getText());

//										System.out.println("SHOW_OR_HIDE: '"+cmi.getText()+"', cmi.isSelected()="+cmi.isSelected());
										setVisibleAtModel(xei._modelIndex, cmi.isSelected());
									}
								}
							});
							showHideMenu.add(mi);
						}
					}

					// display popup near location of mouse click
					tabMenu.show(e.getComponent(), e.getX(), e.getY() - 10);
				}
			} // end: single-click
		} // end: right-click
		else if ( SwingUtilities.isLeftMouseButton(e) )
		{ // begin LEFT-CLICK

			// SINGLE click
			if (e.getClickCount() == 1)
			{
				_logger.debug("GTabbedPane.MouseListener: SINGLE-Click on index="+tabIndex+", name='"+tabName+"', tabComp="+tabComp);

				// If the tab is expanded into its own window: show the window
				if ( tabComp instanceof  UndockedTabHolder )
				{
					UndockedTabHolder undocked = (UndockedTabHolder) tabComp;
					TabExtendedEntry xe = undocked._xe;
					if (xe._undockedFrame != null)
						xe._undockedFrame.setVisible(true);
					return;
				}
			}
			else // DOUBLE click
			{
				if (_logger.isDebugEnabled())
				{
					String className = "";
					if (tabComp != null)
						className = tabComp.getClass().getName();
					_logger.debug("GTabbedPane.MouseListener: DOUBLE-Click on index="+tabIndex+", name='"+tabName+"', tabCompClassName='"+className+"', tabComp="+tabComp);
				}

				// If we double clicked on UndockedTabHolder, then dock window
				if ( tabComp instanceof UndockedTabHolder )
				{
					windowOpenClose(tabIndex);
					return;
				}

				// If we double clicked on it, locate it into its own window
				if ( tabComp instanceof JPanel )
				{
					windowOpenClose(tabIndex);
					return;
				}

				// Bring it back to the TabbedPane, in the same location as before.
				if ( tabComp instanceof TabExtendedEntry )
				{
					TabExtendedEntry xe = (TabExtendedEntry) tabComp;
					windowOpenClose(xe._tabIndex);
					return;
				}

				JOptionPane.showMessageDialog(this, 
					"The tab named '"+tabName+"' Can't be UnDocked.\n" +
					"It needs to be a JPanel or implements the interface 'GTabbedPane.DockUndockManagement'.", 
					"UnDock", JOptionPane.ERROR_MESSAGE);
				return;
			} // end: double-click
		} // end: left-click
	}
	
	/* small helper method for the above v*/
	private void setEnabledX(JComponent comp, boolean enable)
	{
		if (comp != null)
			comp.setEnabled(enable);
	}

	/*---------------------------------------------------
	** END: implementing: MouseListener
	**---------------------------------------------------
	*/



	
	
	/*-------------------------------------------------------
	**-------------------------------------------------------
	**---- SUBCLASSES ----- SUBCLASSES ----- SUBCLASSES ----- 
	**-------------------------------------------------------
	**-------------------------------------------------------
	*/
	private class TabExtendedEntry
	extends JLabel
	{
        private static final long serialVersionUID = 1L;

        private JFrame     _undockedFrame = null;
		private boolean    _isDocked      = true;
		private int        _tabIndex      = -1;   // index in JTable, if < 0, it means that it's not in the JTable
		private String     _tabName       = null;
		private Icon       _icon          = null;
		private String     _toolTip       = null;
		private Component  _comp          = null;
		
		private int        _modelIndex    = -1; // index in internal tab storage, this should be the same as Vector or List index of the _extEntries
		private String     _rmBeforeTab   = null; // When the tab was removed,it was located before the tabName 
		private boolean    _isVisible     = true; // the name says it all

		private JButton    _winOpenCloseButton = null;

		public String getText()
		{
			return "The content for the tab '"+_tabName+"' is undocked.";
		}
		
		public String toString()
		{
			return "_tabIndex="+_tabIndex+", _modelIndex="+_modelIndex+", _rmBeforeTab="+_rmBeforeTab+", _isVisible="+_isVisible+", _isDocked="+_isDocked+", _tabName='"+_tabName+"', _icon='"+_icon+"', _comp="+_comp;
		}
	}

	private class UndockedTabHolder 
	extends JComponent
	{
        private static final long serialVersionUID = 1L;

		TabExtendedEntry _xe  = null;

		private UndockedTabHolder(TabExtendedEntry xe)
		{
			_xe  = xe;
		}
	}

	public interface ShowProperties
	{
		public void showProperties();	
	}


	// BEGIN: TabOrderAndVisibility interface, members and methods
	private TabOrderAndVisibilityListener _tabOrderAndVisibilityListener = null;
	public interface TabOrderAndVisibilityListener
	{
		public void saveTabOrderAndVisibility(String tabOptions);
		public void removeTabOrderAndVisibility();
	}
	public void setTabOrderAndVisibilityListener(TabOrderAndVisibilityListener listener)
	{
		_tabOrderAndVisibilityListener = listener;
	}
	public TabOrderAndVisibilityListener getTabOrderAndVisibilityListener()
	{
		return _tabOrderAndVisibilityListener;
	}
	// Call the interface if anyone is implementing it.
	public void removeTabOrderAndVisibility()
	{
		if (_tabOrderAndVisibilityListener == null)
		{
			_logger.warn("The 'removeTabOrderAndVisibilityListener' has not been set, can't do this action. at removeTabOrderAndVisibility()");
			return;
		}
		_tabOrderAndVisibilityListener.removeTabOrderAndVisibility();
	}
	// Call the interface if anyone is implementing it.
	public void saveTabOrderAndVisibility()
	{
		if (_tabOrderAndVisibilityListener == null)
		{
			_logger.warn("The 'tabOrderAndVisibilityListener' has not been set, can't do this action. at saveTabOrderAndVisibility()");
			return;
		}
		_tabOrderAndVisibilityListener.saveTabOrderAndVisibility(getTabOrderAndVisibility());
	}



	public interface DockUndockManagement
	{
//		/** Sets the button that could be used to dock/undock */
//		public void setDockUndockButton(JButton button);

		/** 
		 * Get a button that should be used to dock/undock<p> 
		 * The default GUI rules will be applied for the button. 
		 * Default GUI = no text, no border, Icon is fetched using getWindow{Dock|Undock}Icon() 
		 */
		public JButton getDockUndockButton();

		/**
		 * called just before the component is docked back into the TabbedPane
		 * @return true if we allow the dock operation
		 */
		public boolean beforeDock();

		/**
		 * called after the component has been docked back into the TabbedPane
		 */
		public void afterDock();

		/**
		 * called just before the component is Undocked to its own frame
		 * @return true if we allow the undock operation
		 */
		public boolean beforeUndock();

		/**
		 * called after the component has been undocked to its own frame
		 */
		public void afterUndock();

		/**
		 * 
		 */
		public void saveWindowProps(GTabbedPaneWindowProps winProps);
		public GTabbedPaneWindowProps getWindowProps();

	}

	
	
	/*---------------------------------------------------
	** BEGIN: TabInfoDecorator
	**---------------------------------------------------
	*/
//	class TabInfoDecorator extends AbstractComponentDecorator 
//	{
//		private final int SIZE = 16;
//		public TabInfoDecorator(JComponent target) 
//		{
//			super(target);
//		}
//
//		/** Position the badge at the right-most edge. */
//		public Rectangle getDecorationBounds() 
//		{
//			Rectangle r = super.getDecorationBounds();
//			Insets insets = getComponent().getInsets();
//			r.x += r.width - SIZE - 1;
//			r.y += (r.height - SIZE) / 2;
//			if (insets != null) {
//			    r.x -= insets.right;
//			}
//			return r;
//		}
//		public void paint(Graphics graphics) 
//		{
//			Rectangle r = getDecorationBounds();
//			Graphics2D g = (Graphics2D)graphics;
//			GeneralPath triangle = new GeneralPath();
//			triangle.moveTo(r.x + SIZE/2, r.y);
//			triangle.lineTo(r.x + SIZE-1, r.y + SIZE-1);
//			triangle.lineTo(r.x, r.y + SIZE-1);
//			triangle.closePath();
//			g.setColor(Color.yellow);
//			g.fill(triangle);
//			g.setColor(Color.black);
//			g.draw(triangle);
//			g.drawLine(r.x + SIZE/2, r.y + 3, r.x + SIZE/2, r.y + SIZE*3/4 - 2);
//			g.drawLine(r.x + SIZE/2, r.y + SIZE*3/4+1, r.x + SIZE/2, r.y + SIZE - 4);
//		}
//	}
	/*---------------------------------------------------
	** END: TabInfoDecorator
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: special paint code
	**---------------------------------------------------
	*/
	public interface SpecialTabPainter
	{
		public void paintTabHeader(Graphics2D g);
	}

	/** Overrides the painter on the JComponent */
	@Override
	public void paintComponent(Graphics g) 
	{
		super.paintComponent(g);
		paintSpecial();
	}
	private void paintSpecial() 
	{
		int tabCount = getTabCount();
		for (int tabIndex=0; tabIndex<tabCount; tabIndex++)
		{
			Component tabComp = getComponentAt(tabIndex);
			if (tabComp instanceof SpecialTabPainter)
			{
				SpecialTabPainter stp = (SpecialTabPainter)tabComp;

				Rectangle r = getUI().getTabBounds(this, tabIndex);
				Graphics g = this.getGraphics().create(r.x, r.y, r.width, r.height);
//				System.out.println("getTabBounds(xxx,"+tabIndex+"): Rectangle="+r+", g="+g);

				stp.paintTabHeader((Graphics2D)g);
			}
		}
	}
	/*---------------------------------------------------
	** END: special paint code
	**---------------------------------------------------
	*/
}
