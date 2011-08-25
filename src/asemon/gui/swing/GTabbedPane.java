/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
					JMenu m = (JMenu) comp;
		
					if ( name.equals(m.getName()) )
					{
						_logger.debug("Found JMenu for name '"+name+"'.");
						return m;
					}
				}
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
	public JButton getDockOrUndockButton(int index)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
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
			"Show the content of this panel in it's own window (and disapear from the TabbedPane).\n" +
			" To put the panel back on the TabbedPane, just close the window.");
	}
	/**
	 * Sets a buttom created by you that should react do dock/undock
	 * @param button        The button itself
	 * @param setDefaultGui true if we should set the GUI behaviour to:<br>
	 *                           Icon               - calls getWindowUndockIcon() to set image.<br>
	 *                           "Unframed" button  - setContentAreaFilled(), no border around the button<br>
	 *                           setMargin(3,3,3,3) - 3 pixels as a margin around it.<br>
	 *                           setToolTipText(...a default value...)<br>
	 */
	public void setDockOrUndockButton(int index, JButton button, boolean setDefaultGui)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
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
		setDockOrUndockButton(getExtendedEntryIndex(tabName), button, setDefaultGui);
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
	 * If you want to add stuff to the meny, its better to use 
	 * getTabPopupMenu(), then add entries to the menu. This is much 
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createTabPopupMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();

		// Un-Dock
		JMenuItem undock_mi     = new JMenuItem("Un Dock, show content in a window");
		JMenuItem dock_mi       = new JMenuItem("Dock, bring back the window into the tab");
		JMenuItem ontop_mi      = new JMenuItem("On Top, bring un docked window to front");
		JMenuItem tabScroll_mi  = new JRadioButtonMenuItem("Tab Layout Scroll");
		JMenuItem tabWrap_mi    = new JRadioButtonMenuItem("Tab Layout Wrap");
		JMenu     gotoTab_m     = new JMenu("Goto Tab Name");

		// Name the items
		undock_mi   .setName("UNDOCK");
		dock_mi     .setName("DOCK");
		ontop_mi    .setName("ONTOP");
		tabScroll_mi.setName("SCROLL");
		tabWrap_mi  .setName("WRAP");
		gotoTab_m   .setName("GOTO");

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
				TabExtendedEntry xe = getExtendedEntry(_lastMouseClickAtTabIndex);
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

		// Add items to the menu
		popupMenu.add(undock_mi);
		popupMenu.add(dock_mi);
		popupMenu.add(ontop_mi);
		popupMenu.addSeparator();
		popupMenu.add(tabScroll_mi);
		popupMenu.add(tabWrap_mi);
		popupMenu.addSeparator();
		popupMenu.add(gotoTab_m);

		return popupMenu;
	}

//	private void windowOpenCloseForSelectedTab()
//	{
//		int    index   = getSelectedIndex();
//		String tabName = getTitleAt(index);
//		_logger.debug("windowOpenClose(): getSelectedIndex()="+index+", title='"+tabName+"'.");
//		windowOpenClose( index );
//	}
	private void windowOpenClose(int index)
	{
		_logger.debug("windowOpenClose(index="+index+"): title='"+getTitleAt(index)+"'.");

		TabExtendedEntry xe = getExtendedEntry(index);
		if (xe == null)
		{
			_logger.debug("windowOpenClose(index="+index+"): title='"+getTitleAt(index)+"'. NO TabExtendedEntry WAS FOUND, leaving method at top.");
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
							"The tab named '"+xe._tabName+"' cant be UnDocked.\n" +
								"The decision for this was taken by the underlaying component.",
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
							_logger.info("The internal ExtendedEntry for '"+name+"' cant be found. Cant undock the window...");
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
							"The tab named '"+xe._tabName+"' cant be Docked.\n" +
								"The decision for this was taken by the underlaying component.",
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
	private TabExtendedEntry getExtendedEntry(int index)
	{
		_logger.trace("getExtendedEntry(index="+index+"), _extEntry.size() = "+_extEntry.size());

		Object o = _extEntry.get(index);

		TabExtendedEntry xe = null;
		if (o instanceof UndockedTabHolder)
			xe = ((UndockedTabHolder)o)._xe;
		else
			xe = (TabExtendedEntry) o;
		
		_logger.trace("getExtendedEntry(index="+index+"), found entry("+(o instanceof UndockedTabHolder ? "UndockedTabHolder" : "TabExtendedEntry")+"): "+xe);
		return xe;
	}

	private int getExtendedEntryIndex(String name)
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
	        	return xe._tabIndex;
	        }
        }
    	_logger.debug("getExtendedEntryIndex(name='"+name+"') - NOT FOUND -");
		return -1; 
	}

	private TabExtendedEntry getExtendedEntry(String name)
	{
		for (Enumeration iter = _extEntry.elements(); iter.hasMoreElements();)
        {
			Object o = iter.nextElement();

			TabExtendedEntry xe = null;
			if (o instanceof UndockedTabHolder)
				xe = ((UndockedTabHolder)o)._xe;
			else
				xe = (TabExtendedEntry) o;

        	_logger.debug("getExtendedEntry(name='"+name+"') - xe._saveName='"+xe._tabName+"'.");
	        if (name.equals(xe._tabName))
	        {
	        	_logger.debug("getExtendedEntry(name='"+name+"') - FOUND ENTRY: "+xe);
	        	return xe;
	        }
        }
    	_logger.debug("getExtendedEntry(name='"+name+"') - NOT FOUND -");
		return null; 
	}



	/*---------------------------------------------------
	** BEGIN: overloaded methods from: JTabbedPane
	**---------------------------------------------------
	*/

	/** 
	 * This also Updates the pupup menu in what mode we are in. 
	 * Then it calls super... 
	 */
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
			xe._isDocked = true;
			xe._tabIndex = index;
			xe._tabName  = title;
			xe._icon     = icon;
			xe._toolTip  = tip;
			xe._comp     = component;
			xe._undockedFrame = null;

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
	public void removeTabAt(int index)
	{
		_logger.trace("removeTabAt(index): index="+index);

		Component component = getComponentAt(index);
		if (component instanceof UndockedTabHolder)
		{
		}
		else if (component instanceof TabExtendedEntry)
		{
		}
		else
		{
			_logger.trace("removeTabAt(index): index="+index+", comp='"+component.getClass().getName()+"', REMOVING TabExtendedEntry.");
			_extEntry.removeElementAt(index);
		}

		super.removeTabAt(index);
	}

	/** Sets the icon at index to icon which can be null. This does not set disabled icon at icon. If the new Icon is different than the current Icon and disabled icon is not explicitly set, the LookAndFeel will be asked to generate a disabled Icon. To explicitly set disabled icon, use setDisableIconAt(). An internal exception is raised if there is no tab at that index. */
	public void setIconAt(int index, Icon icon)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
		if (xe != null)
			xe._icon = icon;

	    super.setIconAt(index, icon);
	}

	/** Sets the tooltip text at index to toolTipText which can be null. */
	public void setToolTipTextAt(int index, String toolTipText)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
		if (xe != null)
			xe._toolTip = toolTipText;

		super.setToolTipTextAt(index, toolTipText);
	}
	
	/** Sets the component at index to component. */
	public void setComponentAt(int index, Component component)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
		if (xe != null)
			xe._comp = component;

		super.setComponentAt(index, component);
	}

	/** Sets the title at index to title which can be null. */
	public void setTitleAt(int index, String title)
	{
		TabExtendedEntry xe = getExtendedEntry(index);
		if (xe != null)
			xe._tabName = title;

		super.setTitleAt(index, title);
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

		// For some operations we cant use "current selected tab"
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
											int index = ((Integer)cp).intValue();
											if (isEnabledAt(index))
												setSelectedIndex( index );
											else
											{
												TabExtendedEntry xe = getExtendedEntry( index );
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
					"The tab named '"+tabName+"' cant be Un Docked.\n" +
					"It needs to be a JPanel or implements the interface 'GTabbedPane.DockUndockManagement'.", 
					"Un Dock", JOptionPane.ERROR_MESSAGE);
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
		private int        _tabIndex      = -1;
		private String     _tabName       = null;
		private Icon       _icon          = null;
		private String     _toolTip       = null;
		private Component  _comp          = null;

		private JButton    _winOpenCloseButton = null;

		public String getText()
		{
			return "The content for the tab '"+_tabName+"' is undocked.";
		}
		
		public String toString()
		{
			return "_tabIndex="+_tabIndex+", _isDocked="+_isDocked+", _tabName='"+_tabName+"', _icon='"+_icon+"', _comp="+_comp;
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

}
