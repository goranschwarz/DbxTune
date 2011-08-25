package asemon.gui;

import java.sql.SQLException;

import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybMessageHandler;

public class AseMessageHandler
	implements SybMessageHandler
{

	private static final int   DB_BASE_ERR = 11;
	private boolean	          _intercepting;
	private StringBuffer      _messages;

	AseMessageHandler()
	{
		_intercepting = false;
		_messages     = null;
	}

	public SQLException messageHandler(SQLException sqlexception)
	{
		boolean flag = handleASEJConnectMessage(sqlexception);
		if (flag)
			sqlexception = null;
		return sqlexception;
	}

	void startIntercepting()
	{
		_intercepting = true;
		_messages = new StringBuffer();
	}

	String stopIntercepting()
	{
		_intercepting = false;
		String s = _messages.toString();
		_messages = null;
		return s;
	}

	private boolean handleASEJConnectMessage(SQLException sqlexception)
	{
		boolean flag = false;
		if (sqlexception != null && (sqlexception instanceof EedInfo))
		{
			EedInfo eedinfo = (EedInfo) sqlexception;
			String s = sqlexception.getMessage();
			if (_intercepting)
			{
				_messages.append(s);
				return true;
			}
			if (eedinfo.getSeverity() >= DB_BASE_ERR)
				return false;
			if (s != null && s.length() > 0)
			{
				if (s.endsWith("\n"))
					s = s.substring(0, s.length() - 1);
//				ASEISQLPlugin.getPlugin().getISQLHost().processAsynchronousMessage(_connection, s, sybase.isql.ISQLHost.AsyncMsgType.DEFERRED_MESSAGE);
			}
		}
		flag = true;
		return flag;
	}
}
