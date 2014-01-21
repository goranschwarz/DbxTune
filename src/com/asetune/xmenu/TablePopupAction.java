/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;

import com.asetune.utils.ConnectionProvider;


public class TablePopupAction
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(TablePopupAction.class);

	private String             _menuName;
	private String             _connName;
	private String             _classname;
	private String             _config;
	private List<Set<String>>  _params;
	private Properties         _menuProps;
	private Properties         _allProps;
	private XmenuAction        _xmenuObject;
	private JTable             _table;
	private ConnectionProvider  _connFactory;
	private boolean            _closeConnOnExit;
	private Window             _owner;

	public TablePopupAction(String menuName, String connName, String classname, List<Set<String>> params, 
			String config, Properties menuProps, Properties allProps, 
			JTable table, ConnectionProvider connFactory, boolean closeConnOnExit, Window owner)
	{
		_menuName    = menuName;
		_connName    = connName != null ? connName : classname;
		_classname   = classname;
		_params      = params;
		_config      = config;
		_menuProps   = menuProps;
		_allProps    = allProps;
		_table       = table;
		_connFactory = connFactory;
		_closeConnOnExit = closeConnOnExit;
		_owner       = owner;
	}

	@Override 
	public void actionPerformed(ActionEvent e)
	{
		JMenuItem source = (JMenuItem)(e.getSource());
		String s = "Action event detected."
					+ "    Event source: " + source.getText()
					+ " (an instance of " + source + ")";
		_logger.debug("PopupMenuAction: "+s);
		_logger.debug("PopupMenuAction._classname: "+_classname);
		_logger.debug("PopupMenuAction._params: "+_params.toString());

		LinkedHashMap<String,String> paramsVal = getParamValues();
		if (paramsVal == null)
			return;

		// Now
		// - Instantiate the class
		// - set properties at it
		// - Execute the thing

		this.loadClass();

		if (_xmenuObject != null)
		{
			_xmenuObject.setMenuName(_menuName);
			_xmenuObject.setParamValues(paramsVal);
			_xmenuObject.setConfig(_config);
			_xmenuObject.setMenuProperties(_menuProps);
			_xmenuObject.setAllProperties(_allProps);
			_xmenuObject.setOwner(_owner);

			if (_xmenuObject.createConnectionOnStart())
			{
				_xmenuObject.setConnection( _connFactory.getNewConnection(_connName) );
				_xmenuObject.setCloseConnOnExit( _closeConnOnExit );
			}
			else
			{
//				_xmenuObject.setConnection(null);
				_xmenuObject.setConnection(_connFactory.getConnection());
				_xmenuObject.setCloseConnOnExit(false);
			}
//			_xmenuObject.setConnection( MainFrame.getMonConnection() );


			_xmenuObject.doWork();
		}
	}

	public int getParamCount()
	{
		return _params.size();
	}
	public Set<String> getParamSet(int i)
	{
		return _params.get(i);
	}
	public void loadClass()
	{
		_xmenuObject = null;
		try
		{
			Class<?> c = Class.forName( _classname );
			_xmenuObject = (XmenuAction) c.newInstance();
		}
		catch (Exception e)
		{
//			JOptionPane.showMessageDialog(_window, "Trying to load classname '"+_classname+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(null, "Trying to load classname '"+_classname+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	public LinkedHashMap<String,String> getParamValues()
	{
		LinkedHashMap<String,String> paramValues = new LinkedHashMap<String,String>();

		int selectedRow = _table.getSelectedRow();
		int row = selectedRow;
		if (row < 0) 
			return null; /* No row was selected */
		if (_table instanceof JXTable)
			row = ((JXTable)_table).convertRowIndexToModel(selectedRow);

		_logger.debug("PopupMenuAction: selected row is "+row);

		// Loop parameters and try to get the position of the
		for (int p=0; p<_params.size(); p++)
		{
			Set<String> paramSet = _params.get(p);
			String paramUsed = null;
			boolean isOptional = false;
			int colPos = -1;
			for (String param : paramSet)
			{
				if (param.equalsIgnoreCase(TablePopupFactory.OPTIONAL_PARAM))
				{
					isOptional = true;
					continue;
				}

				// get column position
				for (int c=0; c<_table.getColumnCount(); c++)
				{
					String colName = _table.getColumnName(c);
					if ( colName.equalsIgnoreCase(param) )
					{
						paramUsed = param;
						colPos = c;
						break; // break the column loop
					}
				}
				if (colPos > 0)
					break;
			}


			_logger.debug("PopupMenuAction: column name '"+paramSet+"' has table index "+colPos+".");

			TableModel model = _table.getModel();
			if (colPos >= 0)
			{
				Object obj = model.getValueAt(row, colPos);
				String val = "";
				if (obj != null)
				{
					val = obj.toString().trim();
				}
				else
				{
					JOptionPane.showMessageDialog(null, "The value for column '"+paramUsed+"' can not be a NULL or empty.", "Error", JOptionPane.ERROR_MESSAGE);
					return null;
				}

				paramValues.put(paramUsed, val);

				_logger.debug("PopupMenuAction: column name '"+paramUsed+"' has value '"+val+"'.");
			}
			else
			{
				if ( ! isOptional )
				{
					_logger.debug("PopupMenuAction: looking for column name(s) '"+paramSet+"' which could NOT be found.");
					JOptionPane.showMessageDialog(null, "looking for column name(s) '"+paramSet+"' which could NOT be found in the current result set.", "Error", JOptionPane.ERROR_MESSAGE);

					return null;
				}
			}
		}
		_logger.debug("PopupMenuAction: getParamValues() returns: "+paramValues);
		return paramValues;
	}

	public Window getOwner()
	{
		return _owner;
	}
}
