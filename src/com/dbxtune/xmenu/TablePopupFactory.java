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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.xmenu;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;


public class TablePopupFactory
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String TABLE_PUPUP_MENU_PREFIX = "table.menu.";
	
	public static final String ENABLE_MENU_ALWAYS          = "alwaysEnabled";
	public static final String ENABLE_MENU_IF_ON_A_ROW     = "enabledIfOnrow";
	public static final String ENABLE_MENU_ROW_IS_SELECTED = "enabledIfRowIsSelected";

	public static final String OPTIONAL_PARAM = ":optional:";

	public static Component getPopupMenuInvoker(JMenuItem mi)
	{
		Component invoker = null;
		JPopupMenu popmenu = null; 
		for (Component c = mi.getParent(); c!=null; c=c.getParent())
		{
			if (c instanceof JPopupMenu)
				popmenu = (JPopupMenu) c;
		}
		if (popmenu != null)
			invoker = popmenu.getInvoker();

		if (invoker != null && invoker instanceof JMenu)
			invoker = getPopupMenuInvoker((JMenuItem)invoker);

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
					int[] selectedRows = table.getSelectedRows();
//					String selection = SwingUtils.tableToString(table.getModel(), selectedRow);
					String selection = SwingUtils.tableToString(table, selectedRows);

					StringSelection data = new StringSelection(selection);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, data);
				}
			}
		});
		popup.add(menuItem);

		menuItem = new JMenuItem("Copy Cell to clipboard");
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
					int[] selectedRows = table.getSelectedRows();
					int   selectedcol  = table.getSelectedColumn();
//					String selection = SwingUtils.tableToString(table.getModel(), selectedRow);
//					String selection = SwingUtils.tableToString(table, selectedRows);
					String selection = "";
					
					for (int r=0; r<selectedRows.length; r++)
					{
						Object obj = table.getValueAt(selectedRows[r], selectedcol);
						selection += (obj == null) ? "NULL" : obj.toString() + "\n";
					}
					selection = StringUtil.removeLastNewLine(selection);

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

			// Create a Properties with all "Sub keys", and trim of the prefix... keeping just the "sub key"
			Properties entryProps = new Properties();
			for (String key : conf.getKeys(prefixStr))
			{
				String val = conf.getProperty(key);

				// Take away the prefix
				String shortKey = key.replaceFirst(prefixStr+".", "");

				// Stuff it in the Properties 
				entryProps.put(shortKey, val);
			}
			
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
			ArrayList<LinkedHashSet<String>> params = new ArrayList<LinkedHashSet<String>>();
			for (int p=1; true; p++)
			{
				String param = conf.getPropertyRaw(prefixStr+".param."+p);
				if (param == null)
					break;
				else
				{
					LinkedHashSet<String> paramSet = StringUtil.parseCommaStrToSet(param);
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

			if ("com.dbxtune.xmenu.SQLWindow".equals(classname))
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
					connName, classname, params, config, 
					null, /*menuProp*/
					entryProps, /*allProp*/
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

									// Loop all columns in the table
									for (int x=0; x<tab.getColumnCount(); x++)
									{
										String colName = tab.getColumnName(x);
										if ( colName != null && colName.equalsIgnoreCase(param) )
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
