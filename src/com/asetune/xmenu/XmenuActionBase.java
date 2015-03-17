/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.Window;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.asetune.AseTune;
import com.asetune.CounterController;
import com.asetune.utils.ConnectionProvider;

/**
 * @author gorans
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class XmenuActionBase 
implements XmenuAction
{
	Properties                   _menuProps       = null;
	Properties                   _allProps        = null;
	LinkedHashMap<String,String> _paramValues     = null;
	String                       _menuName        = null;
	String                       _config          = null;
	Connection                   _conn            = null;
	ConnectionProvider           _connProvider    = null;
	boolean                      _closeConnOnExit = true;
	Window                       _owner           = null;

	/**
	 * 
	 */
	public XmenuActionBase() 
	{
		super();
	}

	@Override public void setMenuProperties(Properties p)                     { _menuProps       = p; }
	@Override public void setAllProperties(Properties p)                      { _allProps        = p; }
	@Override public void setParamValues(LinkedHashMap<String,String> map)    { _paramValues     = map; }
	@Override public void addParamValue(String k, String v)                   { _paramValues.put(k, v); }
	@Override public void setConfig(String name)                              { _config          = name; }
	@Override public void setMenuName(String name)                            { _menuName        = name; }
	@Override public void setConnectionProvider(ConnectionProvider connProv)  { _connProvider    = connProv; }
	@Override public void setConnection(Connection conn)                      { _conn            = conn; }
	@Override public void setCloseConnOnExit(boolean b)                       { _closeConnOnExit = b; }
	@Override public void setOwner(Window window)                             { _owner           = window; }

	@Override public Properties                   getMenuProperties()         { return _menuProps; }
	@Override public Properties                   getAllProperties()          { return _allProps; }
	@Override public LinkedHashMap<String,String> getParamValues()            { return _paramValues; }
	@Override public Iterator<String>             getParamIterator()          { return _paramValues.values().iterator(); }
	@Override public String                       getParamValue(String param) { return (String)_paramValues.get(param); }
	@Override public String                       getConfig()                 { return _config; }
	@Override public String                       getMenuName()               { return _menuName; }
	@Override public ConnectionProvider           getConnectionProvider()     { return _connProvider; }
	@Override public Connection                   getConnection()             { return _conn; }
	@Override public boolean                      isCloseConnOnExit()         { return _closeConnOnExit; }
	@Override public Window                       getOwner()                  { return _owner; }

	@Override 
	public boolean createConnectionOnStart()
	{
		// FIXME: this isn't very generic
//		if (AseTune.hasCounterCollector())
//			return AseTune.getCounterCollector().isMonConnected();
		if (CounterController.hasInstance())
			return CounterController.getInstance().isMonConnected();
		return false;
	}

	@Override 
	public String getParamValue(int pos)
	{
		return getParamValue(pos, null);
	}
	
	public String getParamValue(int pos, String defaultValue)
	{
		if ( pos < 0  &&  pos > _paramValues.size() )
		{
			if (defaultValue == null)
				throw new IndexOutOfBoundsException();
			else
				return defaultValue;
		}

		int n=0;
		Iterator<String> it = getParamIterator();
		while (it.hasNext())
		{
			Object val = it.next();

			if (pos == n)
				return val.toString();
		    n++;
		}
		if (defaultValue == null)
			throw new IndexOutOfBoundsException();
		else
			return defaultValue;
	}

	@Override
	public abstract void doWork();
}
