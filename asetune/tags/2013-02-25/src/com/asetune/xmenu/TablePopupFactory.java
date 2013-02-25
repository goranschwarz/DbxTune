/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class TablePopupFactory
{
	private static Logger _logger = Logger.getLogger(TablePopupFactory.class);

	public static final String TABLE_PUPUP_MENU_PREFIX = "table.menu.";
	
	public static final String ENABLE_MENU_ALWAYS          = "alwaysEnabled";
	public static final String ENABLE_MENU_IF_ON_A_ROW     = "enabledIfOnrow";
	public static final String ENABLE_MENU_ROW_IS_SELECTED = "enabledIfRowIsSelected";

	public static final String OPTIONAL_PARAM = ":optional:";

	private static Component getPopupMenuInvoker(JMenuItem mi)
	{
		Component invoker = null;
		JPopupMenu popmenu = null; 
		for (Component c = mi.getParent(); c!=null; c=c.getParent())
			if (c instanceof JPopupMenu)
				popmenu = (JPopupMenu) c;
		if (popmenu != null)
			invoker = popmenu.getInvoker();

		return invoker;
	}

	public static JPopupMenu createCopyTable(JPopupMenu popup)
	{
		return createCopyTable(popup, false, false);
	}
	public static JPopupMenu createCopyTable(JPopupMenu popup, boolean addSeparatorBefore, boolean addSeparatorAfter)
	{
		if (popup == null)
			throw new IllegalArgumentException("createCopyTable(): The passed JPopupMenu was NULL.");

		JMenuItem menuItem = null;

		if (addSeparatorBefore)
			popup.addSeparator();

		menuItem = new JMenuItem("Copy Table to clipboard");
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);
		menuItem.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				Component invoker = getPopupMenuInvoker((JMenuItem)e.getSource());
				if (invoker instanceof JTable)
				{
					JTable table = (JTable)invoker;
//					String selection = SwingUtils.tableToString(table.getModel());
					String selection = SwingUtils.tableToString(table);

					StringSelection data = new StringSelection(selection);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, data);
				}
			}
		});
		popup.add(menuItem);

		menuItem = new JMenuItem("Copy Row to clipboard");
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ROW_IS_SELECTED);
		menuItem.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				Component invoker = getPopupMenuInvoker((JMenuItem)e.getSource());
				if (invoker instanceof JTable)
				{
					JTable table = (JTable)invoker;
					int selectedRow = table.getSelectedRow();
//					String selection = SwingUtils.tableToString(table.getModel(), selectedRow);
					String selection = SwingUtils.tableToString(table, selectedRow);

					StringSelection data = new StringSelection(selection);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, data);
				}
			}
		});
		popup.add(menuItem);

		if (addSeparatorAfter)
			popup.addSeparator();

		return popup;
	}

//	/** 
//	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
//	 * If you want to add stuff to the menu, its better to use 
//	 * getTabPopupMenu(), then add entries to the menu. This is much 
//	 * better than subclass the GTabbedPane
//	 */
	/**
	 * @param prefix Should contain a '.' at the end
	 */
	public static JPopupMenu createMenu(Configuration conf, JTable table, ConnectionProvider connFactory, Window owner)
	{
		return createMenu(null, TABLE_PUPUP_MENU_PREFIX, conf, table, connFactory, owner);
	}

	/**
	 * @param prefix Should contain a '.' at the end
	 */
	public static JPopupMenu createMenu(String prefix, Configuration conf, JTable table, ConnectionProvider connFactory, Window owner)
	{
		return createMenu(null, prefix, conf, table, connFactory, owner);
	}
	/**
	 * 
	 * @param menu   if null a new JMenuPopup will be created otherwise it will be appended to it
	 * @param prefix prefix of the property string. Should contain a '.' at the end
	 * @param conf
	 * @param owner 
	 * @return
	 */
	public static JPopupMenu createMenu(JPopupMenu popup, String prefix, Configuration conf, JTable table, ConnectionProvider connFactory, Window owner)
	{
		_logger.debug("createMenu(): prefix='"+prefix+"'.");

		//Create the popup menu.
		if (popup == null)
		{
			popup = new JPopupMenu();
		}

		boolean firstAdd = true;
		for (String prefixStr : conf.getUniqueSubKeys(prefix, true))
		{
			_logger.debug("createDataTablePopupMenu(): found prefix '"+prefixStr+"'.");

			// Read menu name
			String menuItemName = conf.getPropertyRaw(prefixStr+".name");

			// Read menu name
			String menuItemIcon = conf.getPropertyRaw(prefixStr+".icon");

			// Read classname
			String classname = conf.getPropertyRaw(prefixStr+".classname");

			// Read connName
			String connName = conf.getPropertyRaw(prefixStr+".connName");

			// config
			String config = conf.getPropertyRaw(prefixStr+".config");

			// Read parameters
			ArrayList<Set<String>> params = new ArrayList<Set<String>>();
			for (int p=1; true; p++)
			{
				String param = conf.getPropertyRaw(prefixStr+".param."+p);
				if (param == null)
					break;
				else
				{
					Set<String> paramSet = StringUtil.parseCommaStrToSet(param);
					params.add(paramSet);
				}
			}

			// Check that we got everything we needed
			if (menuItemName == null)
			{
				_logger.warn("Missing property '"+prefixStr+".name'");
				continue;
			}
			if (classname == null)
			{
				_logger.warn("Missing property '"+prefixStr+".classname'");
				continue;
			}

			if ("com.asetune.xmenu.SQLWindow".equals(classname))
			{
				if (config == null)
				{
					_logger.warn("Missing property '"+prefixStr+".config', where SQL statement to SQLWindow should be specified.");
					continue;
				}
			}

//			if (params.size() == 0)
//			{
//				_logger.warn("Missing property '"+prefixStr+".param.#'. Replace # with parameter number starting at 1 and then be incremeted for every parameter.");
//				continue;
//			}

			// fix connName if not set, or is to long
			if (connName == null)
				connName = classname.substring( classname.lastIndexOf(".")+1 );
			if (connName.length() > 30)
				connName = connName.substring(0, 30);

			// Create the executor class
			TablePopupAction action = new TablePopupAction(menuItemName, 
					connName, classname, params, config, null, /*menuProp*/null, 
					table, connFactory, true, owner);

			JMenuItem menuItem = new JMenuItem(menuItemName);
			menuItem.addActionListener(action);
			if (menuItemIcon != null)
				menuItem.setIcon(SwingUtils.readImageIcon(Version.class, menuItemIcon));

			if ( firstAdd )
			{
				firstAdd = false;
				if (popup.getComponentCount() > 0)
					popup.addSeparator();
			}
			popup.add(menuItem);
		}

		popup.addPopupMenuListener( createPopupMenuListener() );

		return popup;
	}

	/*---------------------------------------------------
	** BEGIN: implementing: PopupMenuListener
	**---------------------------------------------------
	*/
	public static PopupMenuListener createPopupMenuListener()
	{
		return new TablePopupMenuListener();
	}

	public static class TablePopupMenuListener 
	implements PopupMenuListener
	{
		@Override 
		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
		{
			_logger.trace("popupMenuWillBecomeVisible(), source="+e.getSource());
			JPopupMenu pop = (JPopupMenu)e.getSource();
			Component invokerComp = pop.getInvoker();
			_logger.trace("getInvoker(): "+invokerComp);
			if ( ! (invokerComp instanceof JTable) )
			{
				_logger.debug("It needs to be a instance of JTable.");
			}
			JTable tab = (JTable) invokerComp;
	
			// Loop all menu items
			for (int c=0; c<pop.getComponentCount(); c++)
			{
				Component comp = (Component)pop.getComponent(c);
				if (comp instanceof JMenuItem)
				{
					JMenuItem mi = (JMenuItem) comp;

					String actionName = mi.getActionCommand();
					if ( actionName != null && actionName.equals(ENABLE_MENU_ALWAYS) )
					{
						mi.setEnabled(true);
						continue;
					}

					if ( actionName != null && actionName.equals(ENABLE_MENU_ROW_IS_SELECTED) )
					{
						if (tab.getSelectedRowCount() > 0)
						{
							mi.setEnabled(true);
							continue;
						}
					}

					if ( actionName != null && actionName.equals(ENABLE_MENU_IF_ON_A_ROW) )
					{
						if (tab instanceof GTable)
						{
							GTable gTab = (GTable) tab;
							mi.setEnabled(gTab.isLastMousePressedAtModelRowColValid());
						}
						continue;
					}

					// If now rows were selected, no menu items should be enabled
					if (tab.getSelectedRow() == -1)
					{
						mi.setEnabled(false);
						continue;
					}

					mi.setEnabled(true);
	
					// Loop all ActionListeners for this JMenuItem
					ActionListener[] al = mi.getActionListeners();
					for (int a=0; a<al.length; a++)
					{
						if (al[a] instanceof TablePopupAction)
						{
							TablePopupAction pa = (TablePopupAction)al[a];
							_logger.trace("ActionListener["+a+"] is PopupActions, which has pa._params.size()="+pa.getParamCount());
	
							// Loop all Parameters in the PopupActions
							int foundColumns = 0;
							int optionalColumns = 0;
							for (int p=0; p<pa.getParamCount(); p++)
							{
								Set<String> paramSet = pa.getParamSet(p);
								for (String param : paramSet)
								{
									boolean foundParam = false;
									
									if (param.equalsIgnoreCase(OPTIONAL_PARAM))
									{
										optionalColumns++;
										continue;
									}

									// Loop all colums in the table
									for (int x=0; x<tab.getColumnCount(); x++)
									{
										String colName = tab.getColumnName(x);
										if ( colName.equalsIgnoreCase(param) )
										{
											foundParam = true;
											foundColumns++;
											break; // break the column loop
										}
									}
									if (foundParam)
										break;
								}
							}
							// If not all parameters where found in the table
							if (foundColumns < (pa.getParamCount() - optionalColumns))
							{
								mi.setEnabled(false);
							}
						} // PopupActions
					} // ActionListener
				} // instance of JMenuItem
			} // loop menu items
		}
	
		@Override 
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
		{
			_logger.trace("popupMenuWillBecomeInvisible()");
		}
	
		@Override 
		public void popupMenuCanceled(PopupMenuEvent e)
		{
			_logger.trace("popupMenuCanceled()");
		}
	}

	/*---------------------------------------------------
	** END: implementing: PopupMenuListener
	**---------------------------------------------------
	*/
}
