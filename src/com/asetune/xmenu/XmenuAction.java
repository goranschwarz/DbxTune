/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.Window;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.asetune.utils.ConnectionProvider;

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
	public void setConnection(Connection conn);
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
