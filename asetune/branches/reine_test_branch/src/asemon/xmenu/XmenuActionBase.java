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
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class XmenuActionBase 
implements XmenuAction
{
	Properties    _menuProps       = null;
	Properties    _allProps        = null;
	LinkedHashMap _paramValues     = null;
	String        _menuName        = null;
	String        _config          = null;
	Connection    _conn            = null;
	boolean       _closeConnOnExit = true;

	/**
	 * 
	 */
	public XmenuActionBase() 
	{
		super();
	}

	public void setMenuProperties(Properties p)      { _menuProps = p; }
	public void setAllProperties(Properties p)       { _allProps = p; }
	public void setParamValues(LinkedHashMap map)    { _paramValues = map; }
	public void addParamValue(String k, String v)    { _paramValues.put(k, v); }
	public void setConfig(String name)               { _config = name; }
	public void setMenuName(String name)             { _menuName = name; }
	public void setConnection(Connection conn)       { _conn = conn; }
	public void setCloseConnOnExit(boolean b)        { _closeConnOnExit = b; }

	public Properties    getMenuProperties()         { return _menuProps; }
	public Properties    getAllProperties()          { return _allProps; }
	public LinkedHashMap getParamValues()            { return _paramValues; }
	public Iterator      getParamIterator()          { return _paramValues.values().iterator(); }
	public String        getParamValue(String param) { return (String)_paramValues.get(param); }
	public String        getConfig()                 { return _config; }
	public String        getMenuName()               { return _menuName; }
	public Connection    getConnection()             { return _conn; }
	public boolean       isCloseConnOnExit()         { return _closeConnOnExit; }

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
		Iterator it = getParamIterator();
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

	public abstract void doWork();
}
