/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.xmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;

import asemon.utils.ConnectionFactory;

public class TablePopupAction
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(TablePopupAction.class);

	private String      _menuName;
	private String      _connName;
	private String      _classname;
	private String      _config;
	private List        _params;
	private Properties  _menuProps;
	private Properties  _allProps;
	private XmenuAction _xmenuObject;
	private JTable      _table;
	private ConnectionFactory  _connFactory;
	private boolean     _closeConnOnExit;

	public TablePopupAction(String menuName, String connName, String classname, List params, 
			String config, Properties menuProps, Properties allProps, 
			JTable table, ConnectionFactory connFactory, boolean closeConnOnExit)
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
	}

	public void actionPerformed(ActionEvent e)
	{
		JMenuItem source = (JMenuItem)(e.getSource());
		String s = "Action event detected."
					+ "    Event source: " + source.getText()
					+ " (an instance of " + source + ")";
		_logger.debug("PopupMenuAction: "+s);
		_logger.debug("PopupMenuAction._classname: "+_classname);
		_logger.debug("PopupMenuAction._params: "+_params.toString());

		LinkedHashMap paramsVal = getParamValues();
		if (paramsVal == null)
			return;

		// Now
		// - instansiate the class
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

			_xmenuObject.setConnection( _connFactory.getConnection(_connName) );
			_xmenuObject.setCloseConnOnExit( _closeConnOnExit );
//			_xmenuObject.setConnection( MainFrame.getMonConnection() );


			_xmenuObject.doWork();
		}
	}

	public int getParamCount()
	{
		return _params.size();
	}
	public String getParam(int i)
	{
		return (String)_params.get(i);
	}
	public void loadClass()
	{
		_xmenuObject = null;
		try
		{
			Class c = Class.forName( _classname );
			_xmenuObject = (XmenuAction) c.newInstance();
		}
		catch (Exception e)
		{
//			JOptionPane.showMessageDialog(_window, "Trying to load classname '"+_classname+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(null, "Trying to load classname '"+_classname+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	public LinkedHashMap getParamValues()
	{
		LinkedHashMap paramValues = new LinkedHashMap();

		int selectedRow = _table.getSelectedRow();
		int row = selectedRow;
		if (_table instanceof JXTable)
			row = ((JXTable)_table).convertRowIndexToModel(selectedRow);

		_logger.debug("PopupMenuAction: selected row is "+row);

		// Loop parameters and try to get the position of the
		for (int p=0; p<_params.size(); p++)
		{
			String param = (String) _params.get(p);

			// get column position
			int colPos = -1;
			for (int c=0; c<_table.getColumnCount(); c++)
			{
				String colName = _table.getColumnName(c);
				if ( colName.equalsIgnoreCase(param) )
				{
					colPos = c;
					break; // break the column loop
				}
			}

			_logger.debug("PopupMenuAction: column name '"+param+"' has table index "+colPos+".");

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
					JOptionPane.showMessageDialog(null, "The value for column '"+param+"' can not be a NULL or empty.", "Error", JOptionPane.ERROR_MESSAGE);
					return null;
				}

				paramValues.put(param, val);

				_logger.debug("PopupMenuAction: column name '"+param+"' has value '"+val+"'.");
			}
			else
			{
				_logger.debug("PopupMenuAction: looking for column name '"+param+"' which could NOT be found.");
				JOptionPane.showMessageDialog(null, "looking for column name '"+param+"' which could NOT be found in the current result set.", "Error", JOptionPane.ERROR_MESSAGE);

				return null;
			}
		}
		return paramValues;
	}

}
