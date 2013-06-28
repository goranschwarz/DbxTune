package com.asetune.cm;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybMessageHandler;

public class CmSybMessageHandler 
implements SybMessageHandler
{
	private static Logger _logger = Logger.getLogger(CmSybMessageHandler.class);

	private CountersModel _cm = null;
	private String        _logPrefix = "";
	private List<Integer> _discardMsgNum = new LinkedList<Integer>(); // list of <Integer>
	private List<String>  _discardMsgStr = new LinkedList<String>(); // list of <String>

	private boolean       _discardMsgZero = true; // discard print messages, This might cause problems...
	private boolean       _discardMsgZeroPrintInfo = true; 

	private boolean       _discardMsgLoadDb = true; // discard messages that has with 'in load db', etc...
	private boolean       _discardMsgLoadDbPrintInfo = true; 

	static
	{
//		_logger.setLevel(Level.DEBUG);
	}

	public CmSybMessageHandler(CountersModel cm, boolean addSomeDefaultDiscards)
	{
		if (cm == null)
			throw new IllegalArgumentException("CountersModel can't be null, for constructor.");
		_cm = cm;

		String cmName = _cm.getName();

		Configuration conf = Configuration.getCombinedConfiguration();

		// Change the default behavior for _discardMsgZero
		_discardMsgZero          = conf.getBooleanProperty(cmName+".discardMsgZero",          _discardMsgZero);
		_discardMsgZeroPrintInfo = conf.getBooleanProperty(cmName+".discardMsgZeroPrintInfo", _discardMsgZeroPrintInfo);
		_logger.debug("CmSybMessageHandler: Config for CM '"+cmName+"': discardMsgZero="+_discardMsgZero+", discardMsgZeroPrintInfo="+_discardMsgZeroPrintInfo);

		// Change the default behavior for _discardMsgLoadDb
		_discardMsgLoadDb          = conf.getBooleanProperty(cmName+".discardMsgLoadDb",          _discardMsgLoadDb);
		_discardMsgLoadDbPrintInfo = conf.getBooleanProperty(cmName+".discardMsgLoadDbPrintInfo", _discardMsgLoadDbPrintInfo);
		_logger.debug("CmSybMessageHandler: Config for CM '"+cmName+"': discardMsgLoadDb="+_discardMsgLoadDb+", discardMsgLoadDbPrintInfo="+_discardMsgLoadDbPrintInfo);

		// LogName
		setLogPrefix(cmName);

		// MsgNum='10351', Severity='14', Msg: Server user id %d is not a valid user in database '%s'
		addDiscardMsgNum(10351);

		// MsgNum='924', Severity='??', Msg: Database '%s' is already open and can only have one user at a time.
		addDiscardMsgNum(924); 

		// MsgNum='936', Severity='14', Msg: The Model database is unavailable. It is being used to create a new database.
		addDiscardMsgNum(936); 

		// MsgNum='962', Severity='14', Msg: Database with ID '%d' is not available. Please try again later.
		addDiscardMsgNum(962); 
		
		// MsgNum='969', Severity='14', Msg: You can access database '%.*s' only from its owner instance '%.*s'. You cannot access local temporary databases from non-owner instances except to use CREATE DATABASE and DROP DATABASE with local system temporary databases.
		// This is a Cluster message, thrown by object_name(id, DBID) if it's a nonlocal tempdb, and the object_name() returns NULL instead
		addDiscardMsgNum(969); 
		
		// Cluster wide monitor command succeeded.
		addDiscardMsgStr("Cluster wide monitor command succeeded.");
	}

	
	public void resetDiscardMsgNum()
	{
		_discardMsgNum.clear();
	}
	public List<Integer> getDiscardMsgNum()
	{
		return _discardMsgNum;
	}
	public void addDiscardMsgNum(int msgNum)
	{
		Integer msgNumInt = new Integer(msgNum);
		if ( ! _discardMsgNum.contains(msgNumInt))
			_discardMsgNum.add(msgNumInt);
	}
	
	public void resetDiscardMsgStr()
	{
		_discardMsgStr.clear();
	}
	public List<String> getDiscardMsgStr()
	{
		return _discardMsgStr;
	}
	public void addDiscardMsgStr(String regexp)
	{
		if ( ! _discardMsgStr.contains(regexp))
			_discardMsgStr.add(regexp);
	}
	
	public void setLogPrefix(String msgPrefix)
	{
		_logPrefix = msgPrefix;
		if (_logPrefix == null)
			_logPrefix = "";

		if ( ! _logPrefix.endsWith(": ") )
			_logPrefix += ": ";
	}

	public String getLogPrefix()
	{
		return _logPrefix;
	}
	
	@Override
	public SQLException messageHandler(SQLException sqle)
	{
		// Take care of some specific messages...
		int           code    = sqle.getErrorCode();
		Integer       codeInt = new Integer(code);
		String        msgStr  = sqle.getMessage();
		StringBuilder sb      = new StringBuilder();
		StringBuilder logMsg  = new StringBuilder();
		StringBuilder isqlMsg = new StringBuilder();

		// Msg 10353:               You must have any of the following role(s) to execute this command/procedure: 'mon_role' . Please contact a user with the appropriate role for help.
		// Msg 12052:               Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
		// Msg 12036: ASE 12.5.0.3: Collection of monitoring data for table '%.*s' requires that the '%s' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
		//     12036: ASE 15.7.0.0: Incomplete configuration on instance ID %d. Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
		// so lets reinitialize CM, to check again for next sample
		if (code == 12052 || code == 12036)
		{
			if (_cm.isNonConfiguredMonitoringAllowed())
			{
				_cm.setNonConfiguredMonitoringHappened(true);
				_cm.addNonConfiguedMonitoringMessage(code + ": " + msgStr);

				// Downgrade into an SQLWarning or simply return null...
				return null;
			}
		}
		

		if (sqle instanceof EedInfo)
		{
			EedInfo m = (EedInfo) sqle;
//			m.getServerName();
//			m.getSeverity();
//			m.getState();
//			m.getLineNumber();
//			m.getStatus();
//			sqle.getMessage();
//			sqle.getErrorCode();
			
			logMsg.append("Server='")  .append(m.getServerName())   .append("', ");
			logMsg.append("MsgNum='")  .append(sqle.getErrorCode()) .append("', ");
			logMsg.append("Severity='").append(m.getSeverity())     .append("', ");
			logMsg.append("State='")   .append(m.getState())        .append("', ");
			logMsg.append("Status='")  .append(m.getStatus())       .append("', ");
			logMsg.append("Proc='")    .append(m.getProcedureName()).append("', ");
			logMsg.append("Line='")    .append(m.getLineNumber())   .append("', ");
			logMsg.append("Msg: ")     .append(sqle.getMessage());
			// If new-line At the end, remove it
			if ( logMsg.charAt(logMsg.length()-1) == '\n' )
				logMsg.deleteCharAt(logMsg.length()-1);

			if (_logger.isDebugEnabled())
				_logger.debug(getLogPrefix() + logMsg.toString());
			
			if (m.getSeverity() <= 10)
			{
				sb.append(sqle.getMessage());
				
				// Discard empty messages
				String str = sb.toString();
				if (str == null || (str != null && str.trim().equals("")) )
					return null;
			}
			else
			{
				// Msg 222222, Level 16, State 1:
				// Server 'GORANS_1_DS', Line 1:
				//	mmmm

				isqlMsg.append("Msg ").append(sqle.getErrorCode())
					.append(", Level ").append(m.getSeverity())
					.append(", State ").append(m.getState())
					.append(":\n");

				boolean addComma = false;
				String str = m.getServerName();
				if ( str != null && !str.equals(""))
				{
					addComma = true;
					isqlMsg.append("Server '").append(str).append("'");
				}

				str = m.getProcedureName();
				if ( str != null && !str.equals(""))
				{
					if (addComma) isqlMsg.append(", ");
					addComma = true;
					isqlMsg.append("Procedure '").append(str).append("'");
				}

				str = m.getLineNumber() + "";
				if ( str != null && !str.equals(""))
				{
					if (addComma) isqlMsg.append(", ");
					addComma = true;
					isqlMsg.append("Line ").append(str).append(":");
					addComma = false;
					isqlMsg.append("\n");
				}
				isqlMsg.append(sqle.getMessage());
			}

			// If new-line At the end, remove it
			if ( isqlMsg.length() > 0 && isqlMsg.charAt(isqlMsg.length()-1) == '\n' )
			{
				isqlMsg.deleteCharAt(isqlMsg.length()-1);
			}
		}
		
		//if (code == 987612) // Just a dummy example
		//{
		//	_logger.info(getPreStr()+"Downgrading " + code + " to a warning");
		//	sqle = new SQLWarning(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
		//}

		//-------------------------------
		// TREAT DIFFERENT MESSAGES
		//-------------------------------

		// 3604 Duplicate key was ignored.
		if (code == 3604)
		{
//			_logger.debug(getPreStr()+"Ignoring ASE message " + code + ": Duplicate key was ignored.");
//			super.messageAdd("INFO: Ignoring ASE message " + code + ": Duplicate key was ignored.");
//			return null;
		}


		// Not Yet Recovered
		// 921: Database 'xxx' has not been recovered yet - please wait and try again.
		// 950: Database 'xxx' is currently offline. Please wait and try your command again later.
		if (code == 921 || code == 950)
		{
		}

		// DEADLOCK
		if (code == 1205)
		{
		}

		// LOCK-TIMEOUT
		if (code == 12205)
		{
		}


		//
		// Write some extra info in some cases
		//
		// error   severity description
		// ------- -------- -----------
		//    208        16 %.*s not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
		//    504        11 Stored procedure '%.*s' not found.
		//   2501        16 Table named %.*s not found; check sysobjects
		//   2812        16 Stored procedure '%.*s' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
		//   9938        16 Table with ID %d not found; check sysobjects.
		//  10303        14 Object named '%.*s' not found; check sysobjects.
		//  10337        16 Object '%S_OBJID' not found.
		//  11901        16 Table '%.*s' was not found.
		//  11910        16 Index '%.*s' was not found.
		//  18826         0 Procedure '%1!' not found.

		if (    code == 208 
		     || code == 504 
		     || code == 2501 
		     || code == 2812 
		     || code == 9938 
		     || code == 10303 
		     || code == 10337 
		     || code == 11901 
		     || code == 11910 
		     || code == 18826 
		   )
		{
//			_logger.info("MessageHandler for SPID "+getSpid()+": Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
//			super.messageAdd("INFO: Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
		}

		// Loop MSG NUM to check if to discard the message
		if (_logger.isDebugEnabled())
			_logger.debug(getLogPrefix() + "INFO Discard message number list: "+_discardMsgNum);

		if (_discardMsgNum.contains(codeInt))
		{
			_logger.debug(getLogPrefix() + ">>>>> Discarding message: "+logMsg.toString());
			return null;
		}

		// Loop MSG STR to check if to discard the message
		if (_logger.isDebugEnabled())
			_logger.debug(getLogPrefix() + "INFO Discard message text list: "+_discardMsgStr);

		for (String regexp : _discardMsgStr)
		{
//			if (msgStr.matches(regexp))
			if (msgStr.indexOf(regexp) >= 0)
			{
				_logger.debug(getLogPrefix() + ">>>>> Discarding message: "+logMsg.toString());
				return null;
			}
		}

		// If the above _discardMsgNum & _discardMsgStr logic did not discard the message
		// then go for some hard coded values
		
		// Discard Msg 0 ????
		if (code == 0 && _discardMsgZero)
		{
			if (_discardMsgZeroPrintInfo)
			{
				String msg = sqle.getMessage();
				if (msg != null && msg.endsWith("\n"))
					msg = msg.substring(0, msg.length()-1);
				_logger.info(getLogPrefix() + "Discarding Msg 0, Str '"+msg+"'.");
			}
			return null;
		}

		// Discard some other messages
		if ( _discardMsgLoadDb && AseConnectionUtils.isInLoadDbException(sqle))
		{
			if (_discardMsgLoadDbPrintInfo)
			{
				String msg = sqle.getMessage();
				if (msg != null && msg.endsWith("\n"))
					msg = msg.substring(0, msg.length()-1);
				_logger.info(getLogPrefix() + "Discarding Msg "+code+", Str '"+msg+"'.");
			}
			return null;
		}

		// Pass the Exception on.
		return sqle;
	}
}
