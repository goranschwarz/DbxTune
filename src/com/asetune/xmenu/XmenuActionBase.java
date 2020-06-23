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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.Window;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.asetune.CounterController;
import com.asetune.sql.conn.DbxConnection;
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
//	Connection                   _conn            = null;
	DbxConnection                _conn            = null;
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
//	@Override public void setConnection(Connection conn)                      { _conn            = conn; }
	@Override public void setConnection(DbxConnection conn)                   { _conn            = conn; }
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
//	@Override public Connection                   getConnection()             { return _conn; }
	@Override public DbxConnection                getConnection()             { return _conn; }
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
