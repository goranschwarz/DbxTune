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

import java.awt.Window;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.ConnectionProvider;

/**
 * @author gorans
 */
public interface XmenuAction 
{
	public void setMenuProperties(Properties p);
	public void setAllProperties(Properties p);
	public void setParamValues(LinkedHashMap<String,String> map);
	public void addParamValue(String key, String val);
	public void setMenuName(String name);
	public void setConfig(String name);
//	public void setConnection(Connection conn);
	public void setConnection(DbxConnection conn);
	public void setConnectionProvider(ConnectionProvider connProvider);
	public void setCloseConnOnExit(boolean b);
	public void setOwner(Window window);

	public Properties                   getMenuProperties();
	public Properties                   getAllProperties();
	public LinkedHashMap<String,String> getParamValues();
	public Iterator<String>             getParamIterator();
	public String                       getParamValue(String param);
	public String                       getParamValue(int param);
	public String                       getMenuName();
	public String                       getConfig();
	public Window                       getOwner();

	/** True if we should make a new connection on startup */
	public boolean                      createConnectionOnStart();

	public Connection                   getConnection();
	public boolean                      isCloseConnOnExit();

	public ConnectionProvider           getConnectionProvider();

	public abstract void doWork();

}
