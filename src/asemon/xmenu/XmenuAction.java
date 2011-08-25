/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.xmenu;

import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * @author gorans
 */
public interface XmenuAction 
{
	public void setMenuProperties(Properties p);
	public void setAllProperties(Properties p);
	public void setParamValues(LinkedHashMap map);
	public void addParamValue(String key, String val);
	public void setMenuName(String name);
	public void setConfig(String name);
	public void setConnection(Connection conn);
	public void setCloseConnOnExit(boolean b);

	public Properties    getMenuProperties();
	public Properties    getAllProperties();
	public LinkedHashMap getParamValues();
	public Iterator      getParamIterator();
	public String        getParamValue(String param);
	public String        getParamValue(int param);
	public String        getMenuName();
	public String        getConfig();
	public Connection    getConnection();
	public boolean       isCloseConnOnExit();

	public abstract void doWork();

}
