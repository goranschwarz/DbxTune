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
package com.dbxtune.config.dict;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.DbxTune;
import com.dbxtune.Version;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;


public class MonTablesDictionaryAse
extends MonTablesDictionary
{
    /** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	/** Character used for quoted identifier */
//	public static String  qic = "\"";

	private static String FROM_TAB_NAME             = "?FROM_TAB_NAME?";
	private static String TAB_NAME                  = "?TAB_NAME?";
//	private static String SQL_TABLES                = "select TableID, Columns, Parameters, Indicators, Size, TableName, Description from master..monTables";
//	private static String SQL_COLUMNS               = "select TableID, ColumnID, TypeID, Precision, Scale, Length, Indicators, TableName, ColumnName, TypeName, Description from master..monTableColumns where TableName = '?TAB_NAME?'";
	private static String SQL_TABLES                = "select [TableID], [Columns], [Parameters], [Indicators], [Size], [TableName], [Description] from "+FROM_TAB_NAME;
	private static String SQL_COLUMNS               = "select [TableID], [ColumnID], [TypeID], [Precision], [Scale], [Length], [Indicators], [TableName], [ColumnName], [TypeName], [Description] from "+FROM_TAB_NAME+" where [TableName] = '"+TAB_NAME+"'";
//	private static String SQL_TABLES                = "select TableID, Columns, Parameters, Indicators, Size, TableName, Description from "+FROM_TAB_NAME;
//	private static String SQL_COLUMNS               = "select TableID, ColumnID, TypeID, Precision, Scale, Length, Indicators, TableName, ColumnName, TypeName, Description from "+FROM_TAB_NAME+" where TableName = '"+TAB_NAME+"'";
//	private static String SQL_TABLES                = "select * from "+FROM_TAB_NAME;
//	private static String SQL_COLUMNS               = "select * from "+FROM_TAB_NAME+" where TableName = '"+TAB_NAME+"'";
	private static String SQL_MON_WAIT_CLASS_INFO_1 = "select max(WaitClassID) from master..monWaitClassInfo";
	private static String SQL_MON_WAIT_CLASS_INFO   = "select WaitClassID, Description from master..monWaitClassInfo";
	private static String SQL_MON_WAIT_EVENT_INFO_1 = "select max(WaitEventID) from master..monWaitEventInfo";
	private static String SQL_MON_WAIT_EVENT_INFO   = "select WaitEventID, WaitClassID, Description from master..monWaitEventInfo";
	private static String SQL_VERSION               = "select @@version";
	private static String SQL_VERSION_NUM           = "select @@version_number";
//	private static String SQL_SP_VERSION            = "sp_version 'installmontables'";
	private static String SQL_SP_VERSION            = "sybsystemprocs..sp_version";

	/**
	 * yes, save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return true;
	}
	
	/** 
	 * if version is above 15.0.x, then use installmaster as the monTable version, since installmaster 
	 * installs the monTables and sp_version 'montables' doesn't include the ESD level
	 * <p>
	 * Also if we failed to execute sp_version, then use the binary version<br> 
	 * sp_version only exists in ASE 12.5.4 or above (manuals says 12.5.3 esd#?), then use the binary version 
	 */
	@Override
	public long getDbmsMonTableVersionNum()
	{
		long version = super.getDbmsMonTableVersionNum(); 

		if (getDbmsExecutableVersionNum() >= Ver.ver(15,0,2))
			version = getDbmsInstallMasterVersionNum();

		// If _installmasterVersionNum or _montablesVersionNum is 0
		// sp_version has not been executed properly, needs ASE 12.5.(4), or if the execution fails for some reason
		if (version == 0)
			version = getDbmsExecutableVersionNum();

		return version;
	}
	
	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		if (conn == null)
			return;
		setGui(hasGui);

		initializeVersionInfo(conn, hasGui);
		
		// Do more things if user has MON_ROLE
		boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);

		String sql = null;

		//------------------------------------
		// get values from monTables & monTableColumns
		if (hasMonRole)
			initializeMonTabColHelper(conn, false);

		//------------------------------------
		// monWaitClassInfo
		if (hasMonRole)
		{
			try
			{
				sql = SQL_MON_WAIT_CLASS_INFO_1;
				if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
					sql += " where Language = 'en_US'";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				int max_waitClassId = 0; 
				while ( rs.next() )
				{
					max_waitClassId = rs.getInt(1); 
				}
				rs.close();
	
				MonWaitClassInfoEntry[] monWaitClassInfo = new MonWaitClassInfoEntry[max_waitClassId+1];

				sql = SQL_MON_WAIT_CLASS_INFO;
				if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
					sql += " where Language = 'en_US'";
				rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MonWaitClassInfoEntry entry = new MonWaitClassInfoEntry();
					int pos = 1;
	
					entry._waitClassId  = rs.getInt(pos++);
					entry._description  = rs.getString(pos++);
	
					_logger.debug("Adding WaitClassInfo: " + entry);
	
					monWaitClassInfo[entry._waitClassId] = entry;
				}
				rs.close();
				
				setMonWaitClassInfo(monWaitClassInfo);
			}
			catch (SQLException ex)
			{
				_logger.error("MonTablesDictionary:initialize, _monWaitClassInfo", ex);
				if (hasGui)
					SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				return;
			}
	
			// _monWaitEventInfo
			try
			{
				sql = SQL_MON_WAIT_EVENT_INFO_1;
				if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
					sql += " where Language = 'en_US'";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				int max_waitEventId = 0; 
				while ( rs.next() )
				{
					max_waitEventId = rs.getInt(1); 
				}
				rs.close();
	
				MonWaitEventInfoEntry[] monWaitEventInfo = new MonWaitEventInfoEntry[max_waitEventId+1];
	
				sql = SQL_MON_WAIT_EVENT_INFO;
				if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
					sql += " where Language = 'en_US'";
				rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MonWaitEventInfoEntry entry = new MonWaitEventInfoEntry();
					int pos = 1;
	
					entry._waitEventId  = rs.getInt(pos++);
					entry._waitClassId  = rs.getInt(pos++);
					entry._description  = rs.getString(pos++);
					
					_logger.debug("Adding WaitEventInfo: " + entry);
	
					monWaitEventInfo[entry._waitEventId] = entry;
				}
				rs.close();

				setMonWaitEventInfo(monWaitEventInfo);
			}
			catch (SQLException ex)
			{
				_logger.error("MonTablesDictionary:initialize, _monWaitEventInfo", ex);
				if (hasGui)
					SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				return;
			}
		}

		//------------------------------------
		// @@servername
		setDbmsServerName( AseConnectionUtils.getAseServername(conn) );
		
		//------------------------------------
		// @@version_number
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
			while ( rs.next() )
			{
				setDbmsExecutableVersionNum( rs.getInt(1) );
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.debug("MonTablesDictionary:initialize, @@version_number, probably an early ASE version", ex);
		}

		//------------------------------------
		// version
		try
		{
			sql = SQL_VERSION;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				setDbmsExecutableVersionStr( rs.getString(1) );
			}
			rs.close();

			long srvVersionNumFromVerStr = Ver.sybVersionStringToNumber( getDbmsExecutableVersionStr() );
			setDbmsExecutableVersionNum( Math.max(getDbmsExecutableVersionNum(), srvVersionNumFromVerStr) );

			// Check if the ASE binary is Cluster Edition Enabled
			setClusterEnabled( AseConnectionUtils.isClusterEnabled(conn) );
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize, @@version", ex);
			if (hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			return;
		}

		//------------------------------------
		// SORT order ID and NAME
		try
		{
			sql="declare @sortid tinyint, @charid tinyint \n" +
				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
				"select @charid = value from master..syscurconfigs where config = 131  \n" +
				"\n" +
				"select 'sortorder', id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @sortid and csid = @charid \n" +
				"\n" +
				"UNION ALL \n" +
				"\n" +
				"select 'charset', id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @charid \n";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				String type = rs.getString(1);
				String id   = rs.getString(2);
				String name = rs.getString(3);

				if ("sortorder".equals(type))
				{
					setDbmsSortId  (id);
					setDbmsSortName(name);
				}
				if ("charset".equals(type))
				{
					setDbmsCharsetId  (id);
					setDbmsCharsetName(name);
				}
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, Sort order information", ex);
		}

		//------------------------------------
		// Can this possible be a SAP Business Suite System
		try
		{
			String sapSystemInfo = "";

			// CHECK for login 'sapsa'
			// ----------------------
			sql="select suid from master..syslogins where name = 'sapsa'";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				sapSystemInfo += "USER:sapsa=true, ";

				@SuppressWarnings("unused")
				int suid = rs.getInt(1);
			}
			rs.close();
			stmt.close();

			// CHECK for db 'saptools'
			// ----------------------
			sql="select dbid from master..sysdatabases where name = 'saptools'";
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				sapSystemInfo += "DB:saptools=true, ";

				@SuppressWarnings("unused")
				int dbid = rs.getInt(1);
			}
			rs.close();
			stmt.close();
			
			sapSystemInfo = StringUtil.removeLastComma(sapSystemInfo);
			setSapSystemInfo(sapSystemInfo);
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, When Probing if this looks like a SAP system, Caught exception.", ex);
		}

		//------------------------------------
		// sp_version
		if (getDbmsExecutableVersionNum() >= Ver.ver(12,5,4))
		{
			try
			{
				sql = SQL_SP_VERSION;
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					String spVersion_scriptName = rs.getString(1);
					String spVersion_versionStr = rs.getString(2);
					String spVersion_status     = rs.getString(3);
	
					if (spVersion_scriptName.endsWith("montables")) // could be: installmontables or montables
					{
						setDbmsMonTableVersionStr(spVersion_versionStr);
						setDbmsMonTableStatusStr (spVersion_status);
						setDbmsMonTableVersionNum(Ver.sybVersionStringToNumber( getDbmsMonTableVersionStr() ));
		
						if ( ! getDbmsMonTableStatusStr().equalsIgnoreCase("Complete") )
							setDbmsMonTableStatusStr ("incomplete");
					}
	
					if (spVersion_scriptName.equals("installmaster"))
					{
						setDbmsInstallMasterVersionStr(spVersion_versionStr);
						setDbmsInstallMasterStatusStr (spVersion_status);
						setDbmsInstallMasterVersionNum(Ver.sybVersionStringToNumber( getDbmsInstallMasterVersionStr() ));

						if ( ! getDbmsInstallMasterStatusStr().equalsIgnoreCase("Complete") )
							setDbmsInstallMasterStatusStr("incomplete");
					}
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				// Msg 2812, Level 16, State 5:
				// Server 'GORAN_12503_DS', Line 1:
				// Stored procedure 'sp_version' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
				if (ex.getErrorCode() == 2812)
				{
					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"'. " +
							"The stored procedure 'sp_version' was introduced in ASE 12.5.4, which I can't find in the connected ASE, this implies that 'installmaster' has not been applied after upgrade. " +
							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
					_logger.error(msg);
	
					String msgHtml = 
						"<html>" +
						"ASE 'installmaster' script may be of a faulty version. <br>" +
						"<br>" +
						"ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"'.<br>" +
						"<br>" +
						"The stored procedure 'sp_version' was introduced in ASE 12.5.4, which I can't find in the connected ASE, <br>" +
						"this implies that 'installmaster' has <b>not</b> been applied after upgrade.<br>" +
						"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status by executing: <code>sp_version</code>. <br>" +
						"<br>" +
						"Do the following on the machine that hosts the ASE:<br>" +
						"<font size='4'>" +
						"  <code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
						"</font>" +
						"<br>" +
						"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
						"<br>" +
						"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
						"</html>";
					if (hasGui)
						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - MonTablesDictionary - Initialize", msgHtml, null);
				}
				else
				{
					_logger.warn("MonTablesDictionary:initialize, problems executing: "+SQL_SP_VERSION+ ". Exception: "+ex.getMessage());

					String msgHtml = 
						"<html>" +
						"Problems when executing sp_version. <br>" +
						"Msg: <code>"+ex.getErrorCode()+"</code><br>" +
						"Text: <code>"+ex.getMessage()+"</code><br>" +
						"<br>" +
						"ASE 'installmaster' script may be of a faulty version. <br>" +
						"Or the stored procedure 'sp_version' has been replaced with a customer specific one.<br>" +
						"<br>" +
						"ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"'.<br>" +
						"<br>" +
						"To fix the issue Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' again and check it's status by executing: <code>sp_version</code>. <br>" +
						"<br>" +
						"Do the following on the machine that hosts the ASE:<br>" +
						"<font size='4'>" +
						"  <code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
						"</font>" +
						"<br>" +
						"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
						"<br>" +
						"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
						"</html>";
					if (hasGui)
						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - MonTablesDictionary - Initialize", msgHtml, null);
					return;
				}
			}
		} // end: if (srvVersionNum >= 12.5.4)

		_logger.info("ASE 'montables'     for sp_version shows: Status='"+getDbmsMonTableStatusStr()     +"', VersionNum='"+getDbmsMonTableVersionNum()     +"', VersionStr='"+getDbmsMonTableVersionStr()+"'.");
		_logger.info("ASE 'installmaster' for sp_version shows: Status='"+getDbmsInstallMasterStatusStr()+"', VersionNum='"+getDbmsInstallMasterVersionNum()+"', VersionStr='"+getDbmsInstallMasterVersionStr()+"'.");

		//-------- montables ------
		// is installed monitor tables fully installed.
		if (hasMonRole)
		{
			if (getDbmsMonTableStatusStr().equals("incomplete"))
			{
				String msg = "ASE Monitoring tables has not been completely installed. Please check it's status with: sp_version";
				if (DbxTune.hasGui())
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
				_logger.warn(msg);
			}
		}

		//------------------------------------
		// is installed monitor tables version different than ASE version
		if (getDbmsMonTableVersionNum() > 0)
		{
			// strip off the ROLLUP VERSION  (divide by 10 takes away last digit)
			if (getDbmsExecutableVersionNum()/100000 != getDbmsMonTableVersionNum()/100000) // Ver.ver(...) can we use that in some way here... if VER "length" changes the xx/100000 needs to be changed
			{
				String msg = "ASE Monitoring tables may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"' while MonTables version is '"+Ver.versionNumToStr(getDbmsMonTableVersionNum())+"'. Please check it's status with: sp_version";
				if (hasGui)
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
				_logger.warn(msg);
			}
		}

		//-------- installmaster ------
		// is installmaster fully installed.
		if (hasMonRole)
		{
			if (getDbmsInstallMasterStatusStr().equals("incomplete"))
			{
				String msg = "ASE 'installmaster' script has not been completely installed. Please check it's status with: sp_version";
				if (hasGui)
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
				_logger.error(msg);
			}
		}

		//------------------------------------
		// is 'installmaster' version different than ASE version
		if (hasMonRole)
		{
			if (getDbmsInstallMasterVersionNum() > 0)
			{
				if (getDbmsExecutableVersionNum() != getDbmsInstallMasterVersionNum())
				{
					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"' while 'installmaster' version is '"+Ver.versionNumToStr(getDbmsInstallMasterVersionNum())+"'. Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
					_logger.warn(msg);
	
					if (hasGui)
					{
						String msgHtml = 
							"<html>" +
							"ASE 'installmaster' script may be of a faulty version. <br>" +
							"<br>" +
							"ASE Version is '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"' while 'installmaster' version is '"+Ver.versionNumToStr(getDbmsInstallMasterVersionNum())+"'. <br>" +
							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version. <br>" +
							"<br>" +
							"Do the following on the machine that hosts the ASE:<br>" +
							"<code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
							"<br>" +
							"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
							"<br>" +
							"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
							"<br>" +
							"<hr>" + // horizontal ruler
							"<br>" +
							"<center><b>Choose what Version you want to initialize the Performance Counters with</b></center><br>" +
							"</html>";

//						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - connect check", msgHtml, null);
						//JOptionPane.showMessageDialog(MainFrame.getInstance(), msgHtml, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);

						Configuration config = Configuration.getInstance(Configuration.USER_TEMP);
						if (config != null)
						{
							Object[] options = {
									"ASE montables/installmaster Version " + Ver.versionNumToStr(getDbmsInstallMasterVersionNum()),
									"ASE binary Version "                  + Ver.versionNumToStr(getDbmsExecutableVersionNum())
									};
							int answer = JOptionPane.showOptionDialog(MainFrame.getInstance(), 
//								"ASE Binary and 'installmaster' is out of sync...\n" +
//									"What Version of ASE would you like to initialize the Performance Counters with?", // message
								msgHtml,
								"Initialize Performance Counters Using ASE Version", // title
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null,     //do not use a custom Icon
								options,  //the titles of buttons
								options[0]); //default button title

							setTrustMonTablesVersion(answer == 0);

							if (trustMonTablesVersion())
								_logger.warn("ASE Binary and 'montables/installmaster' is out of sync, installmaster has not been applied. The user decided to use the 'current installmaster version'. The used MDA table layout will be '"+Ver.versionNumToStr(getDbmsInstallMasterVersionNum())+"'. ASE Binary version was '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"'.");
							else
								_logger.warn("ASE Binary and 'montables/installmaster' is out of sync, installmaster has not been applied. The user decided to use the 'ASE Binary version'. The used MDA table layout will be '"+Ver.versionNumToStr(getDbmsExecutableVersionNum())+"'. ASE installmaster version was '"+Ver.versionNumToStr(getDbmsInstallMasterVersionNum())+"'.");
						}
					}
				}
			}
		}
		
		initExtraMonTablesDictionary();

		// finally MARK it as initialized
		setInitialized(true);
	}
	

	
	@Override
	public void initializeVersionInfo(DbxConnection conn, boolean hasGui)
	{
		if (conn == null)
			return;

		if (isVersionInfoInitialized())
			return;

		
		String sql = null;

		// @@servername
		setDbmsServerName( AseConnectionUtils.getAseServername(conn) );

		// srvVersionStr = @@version
		// srvVersionNum = @@version -> to an integer
		// isClusterEnabled...
		try
		{
			sql = SQL_VERSION;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				setDbmsExecutableVersionStr(rs.getString(1));
			}
			rs.close();
			stmt.close();

			setDbmsExecutableVersionNum( Ver.sybVersionStringToNumber(getDbmsExecutableVersionStr()) );
			setClusterEnabled( AseConnectionUtils.isClusterEnabled(conn) );
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, @@version", ex);
			if (hasGui())
				SwingUtils.showErrorMessage("MonTablesDictionary - initializeVersionInfo", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			return;
		}
		setEarlyVersionInfo(true);

		// SORT order ID and NAME
		try
		{
			sql="declare @sortid tinyint, @charid tinyint \n" +
				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
				"select @charid = value from master..syscurconfigs where config = 131  \n" +
				"\n" +
				"select 'sortorder', id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @sortid and csid = @charid \n" +
				"\n" +
				"UNION ALL \n" +
				"\n" +
				"select 'charset', id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @charid \n";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				String type = rs.getString(1);
				String id   = rs.getString(2);
				String name = rs.getString(3);

				if ("sortorder".equals(type))
				{
					setDbmsSortId  ( id );
					setDbmsSortName( name );
				}
				if ("charset".equals(type))
				{
					setDbmsCharsetId  ( id );
					setDbmsCharsetName( name );
				}
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, Sort order information", ex);
		}

		setVersionInfoInitialized(true);
	}

	
	
	/**
	 * Initialize 
	 * @param conn
	 */
	@Override
	public void initializeMonTabColHelper(DbxConnection conn, boolean offline)
	{
		if (conn == null)
			return;

		HashMap<String,MonTableEntry> monTablesMap = new HashMap<String,MonTableEntry>();

		String monTables       = "master.dbo.monTables";
		String monTableColumns = "master.dbo.monTableColumns";
		if (offline)
		{
			String schemaName = null;
			monTables       = PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_MON_TAB_DICT,     null, true);
			monTableColumns = PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_MON_TAB_COL_DICT, null, true);
		}
		
		String sql = null;
		try
		{
			Statement stmt = conn.createStatement();
			sql = SQL_TABLES.replace(FROM_TAB_NAME, monTables);
			if ( ! offline )
			{
				if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
					sql += " where Language = 'en_US' ";

				//sql = sql.replace("\"", "");
			}

			// replace all '[' and ']' into DBMS Vendor Specific Chars
			sql = conn.quotifySqlString(sql);

			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				MonTableEntry entry = new MonTableEntry();

				int pos = 1;
				entry._tableID      = rs.getInt   (pos++);
				entry._columns      = rs.getInt   (pos++);
				entry._parameters   = rs.getInt   (pos++);
				entry._indicators   = rs.getInt   (pos++);
				entry._size         = rs.getInt   (pos++);
				entry._tableName    = rs.getString(pos++);
				entry._description  = rs.getString(pos++);
				
				// Create substructure with the columns
				// This is filled in BELOW (next SQL query)
				entry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();

				monTablesMap.put(entry._tableName, entry);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			if (offline && ex.getMessage().contains("not found"))
			{
				_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
				return;
			}
			_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
			return;
		}

		for (Map.Entry<String,MonTableEntry> mapEntry : monTablesMap.entrySet()) 
		{
		//	String        key           = mapEntry.getKey();
			MonTableEntry monTableEntry = mapEntry.getValue();
			
			if (monTableEntry._monTableColumns == null)
			{
				monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
			}
			else
			{
				monTableEntry._monTableColumns.clear();
			}

			try
			{
				Statement stmt = conn.createStatement();
				sql = SQL_COLUMNS.replace(FROM_TAB_NAME, monTableColumns);
				sql = sql.replace(TAB_NAME, monTableEntry._tableName);
				if ( ! offline )
				{
					if (getDbmsExecutableVersionNum() >= Ver.ver(15,7))
						sql += " and Language = 'en_US' ";

					//sql = sql.replace("\"", "");
				}

				// replace all '[' and ']' into DBMS Vendor Specific Chars
				sql = conn.quotifySqlString(sql);

				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MonTableColumnsEntry entry = new MonTableColumnsEntry();

					int pos = 1;
					entry._tableID      = rs.getInt   (pos++);
					entry._columnID     = rs.getInt   (pos++);
					entry._typeID       = rs.getInt   (pos++);
					entry._precision    = rs.getInt   (pos++);
					entry._scale        = rs.getInt   (pos++);
					entry._length       = rs.getInt   (pos++);
					entry._indicators   = rs.getInt   (pos++);
					entry._tableName    = rs.getString(pos++);
					entry._columnName   = rs.getString(pos++);
					entry._typeName     = rs.getString(pos++);
					entry._description  = rs.getString(pos++);
					
					monTableEntry._monTableColumns.put(entry._columnName, entry);
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				if (offline && ex.getMessage().contains("not found"))
				{
					_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
					return;
				}
				_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
				return;
			}
		}

		setMonTablesDictionaryMap(monTablesMap);
	}

	


	
	
	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 */
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			if (mtd == null)
				return;

			String clientXxx = 
			"<html>" +
				"The clientname, clienthostname and clientapplname is assigned by client code with individual information.<br>" +
				"This is useful for differentiating among clients in a system where many clients connect to Adaptive Server using the same name, host name, or application name.<br>" +
				"It can also be used by the client as a trace to indicate what part of the client code that is executing.<br>" +
				"<br>" +
				"Client code has probably executed:<br>" +
				"<code>set [clientname client_name | clienthostname host_name | clientapplname application_name] value</code>" +
			"</html>";

			
			mtd.addTable("sysprocesses",  "Holds information about SybasePID or threads/users logged in to the ASE.");
			mtd.addColumn("sysprocesses", "spid",           "Session Process Identifier.");
			mtd.addColumn("sysprocesses", "kpid",           "Kernel Process Identifier.");
			mtd.addColumn("sysprocesses", "enginenum",      "Number of engine on which process is being executed.");
			mtd.addColumn("sysprocesses", "status",         "Status that the SPID is currently in. \n'recv sleep'=waiting for incomming network trafic, \n'sleep'=various reasons but usually waiting for disk io, \n'running'=currently executing on a engine, \n'runnable'=waiting for an engine to execute its work, \n'lock sleep'=waiting for table/page/row locks, \n'sync sleep'=waiting for childs to finnish when parallel execution. \n'...'=and more statuses");
			mtd.addColumn("sysprocesses", "suid",           "Sybase User ID of the user which the client logged in with.");
			mtd.addColumn("sysprocesses", "fid",            "SPID of the parent process, Family ID.");
			mtd.addColumn("sysprocesses", "program_name",   "Name of the client program, the client application has to set this to a value otherwise it will be empty.");
			mtd.addColumn("sysprocesses", "dbname",         "Name of the database this user is currently using.");
			mtd.addColumn("sysprocesses", "login",          "Username that is logged in.");
			mtd.addColumn("sysprocesses", "cmd",            "What are we doing. \n'SELECT/INSERT/UPDATE/DELETE'=quite obvious, \n'LOG SUSP'=waiting for log space to be available, \n'COND'=On a IF or equivalent SQL statement, \n'...'=and more");
			mtd.addColumn("sysprocesses", "tran_name",      "More info about what the ASE is doing. \nIf we are in CREATE INDEX it will tell you the index name. \nIf we are in BCP it will give you the tablename etc. \nThis is a good place to look at when you issue ASE administrational commands and want to know whet it really does.");
			mtd.addColumn("sysprocesses", "physical_io",    "Total number of reads/writes, this is flushed in a strange way, so do not trust the Absolute value to much...");
			mtd.addColumn("sysprocesses", "memusage",       "Amount of memory allocated to process.");
			mtd.addColumn("sysprocesses", "procName",       "If a procedure is executing, this will be the name of the proc. \nif you execute a sp_ proc, it could give you a procedure name that is uncorrect. \nThis due to the fact that I'm using object_name(id,dbid) and dbid is the database the SPID is currently in, while the procId is really reflecting the id of the sp_ proc which usually is located in sybsystemprocs.");
			mtd.addColumn("sysprocesses", "stmtnum",        "Statement number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator.");
			mtd.addColumn("sysprocesses", "linenum",        "Line number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator. \nIf this does NOT move between samples you may have a HEAVY SQL statement to optimize or you may waiting for a blocking lock.");
			mtd.addColumn("sysprocesses", "blocked",        "0 is a good value, otherwise it will be the SPID that we are blocked by, meaning we are waiting for that SPID to release it's locks on some objetc.");
			mtd.addColumn("sysprocesses", "time_blocked",   "Number of seconds we have been blocked by other SPID's. \nThis is not a summary, it shows you how many seconds we have been waiting since we started to wait for the other SPID to finnish.");
			mtd.addColumn("sysprocesses", "hostname",       "hostname of the machine where the clinet was started. (This can be filled in by the client, meaning it could be used for something else)");
			mtd.addColumn("sysprocesses", "ipaddr",         "IP address of the connected client");
			mtd.addColumn("sysprocesses", "hostprocess",    "hostprocess on the machine which the clinet was started at. (This can be filled in by the client, meaning it could be used for something else)");
			mtd.addColumn("sysprocesses", "cpu",            "cumulative cpu time used by a process in 'ticks'. This is periodically flushed by the system (see sp_configure 'cpu accounting flush interval'");
			mtd.addColumn("sysprocesses", "clientname",     clientXxx);
			mtd.addColumn("sysprocesses", "clienthostname", clientXxx);
			mtd.addColumn("sysprocesses", "clientapplname", clientXxx);

//mtd.addColumn("sysprocesses", "spid",				"Process ID.");
//mtd.addColumn("sysprocesses", "kpid",				"Kernel process ID.");
//mtd.addColumn("sysprocesses", "enginenum",			"Number of engine on which process is being executed.");
//mtd.addColumn("sysprocesses", "status",				"Process ID status.");
//mtd.addColumn("sysprocesses", "suid",				"Server user ID of user who issued command.");
//mtd.addColumn("sysprocesses", "hostname",			"Name of host computer.");
//mtd.addColumn("sysprocesses", "program_name",		"Name of front-end module.");
//mtd.addColumn("sysprocesses", "hostprocess",		"Host process ID number..");
//mtd.addColumn("sysprocesses", "cmd",				"Command or process currently being executed. Evaluation of a conditional statement, such as an if or while loop, returns cond.");
//mtd.addColumn("sysprocesses", "cpu",				"Cumulative CPU time for process in ticks");
//mtd.addColumn("sysprocesses", "physical_io",		"Number of disk reads and writes for current command.");
//mtd.addColumn("sysprocesses", "memusage",			"Amount of memory allocated to process.");
//mtd.addColumn("sysprocesses", "blocked",			"Process ID of blocking process, if any.");
//mtd.addColumn("sysprocesses", "dbid",				"Database ID.");
//mtd.addColumn("sysprocesses", "uid",				"ID of user who executed command.");
//mtd.addColumn("sysprocesses", "gid",				"Group ID of user who executed command.");
//mtd.addColumn("sysprocesses", "tran_name",			"Name of the active transaction.");
//mtd.addColumn("sysprocesses", "time_blocked",		"Time blocked in seconds.");
//mtd.addColumn("sysprocesses", "network_pktsz",		"Current connection�s network packet size.");
//mtd.addColumn("sysprocesses", "fid",				"Process ID of the worker process parent.");
//mtd.addColumn("sysprocesses", "execlass",			"Execution class that the process is bound to.");
//mtd.addColumn("sysprocesses", "priority",			"Base priority associated with the process.");
//mtd.addColumn("sysprocesses", "affinity",			"Name of the engine to which the process has affinity.");
//mtd.addColumn("sysprocesses", "id",					"Object ID of the currently running procedure (or 0 if no procedure is running).");
//mtd.addColumn("sysprocesses", "stmtnum",			"The current statement number within the running procedure (or the SQL batch statement number if no procedure is running).");
//mtd.addColumn("sysprocesses", "linenum",			"The line number of the current statement within the running stored procedure (or the line number of the current SQL batch statement if no procedure is running).");
//mtd.addColumn("sysprocesses", "origsuid",			"Original server user ID. If this value is not NULL, a user with ansuid of origsuid executed set proxy or set session authorization to impersonate the user who executed the command.");
//mtd.addColumn("sysprocesses", "block_xloid",		"Unique lock owner ID of a lock that is blocking a transaction.");
//mtd.addColumn("sysprocesses", "clientname",			"Name by which the user is know for the current session. (set clientname somename)");
//mtd.addColumn("sysprocesses", "clienthostname",		"Name by which the host is known for the current session. (set clienthostname somename)");
//mtd.addColumn("sysprocesses", "clientapplname",		"Name by which the application is known for the current session. (set clientapplname somename)");
//mtd.addColumn("sysprocesses", "sys_id",				"Unique identity of companion node.");
//mtd.addColumn("sysprocesses", "ses_id",				"Unique identity of each client session.");
//mtd.addColumn("sysprocesses", "loggedindatetime",	"Shows the time and date when the client connected to the SAP ASE server. See �Row-level access control� in Chapter 11, �Managing User Permissions� of the Security Administration Guide for more information.");
//mtd.addColumn("sysprocesses", "ipaddr",				"IP address of the client where the login is made. See �Row-level access control� in Chapter 11, �Managing User Permissions� of the Security Administration Guide for more information.");
//mtd.addColumn("sysprocesses", "nodeid",				"Reserved for future use (not available for cluster environments).");
//mtd.addColumn("sysprocesses", "instanceid",			"ID of the instance (available only for cluster environments).");
//mtd.addColumn("sysprocesses", "pad",				"(Cluster Edition) Column added for alignment purposes.");
//mtd.addColumn("sysprocesses", "lcid",				"(Cluster Edition) ID of the cluster.");

			
			mtd.addColumn("sysprocesses", "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("sysprocesses", "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");
			mtd.addColumn("sysprocesses", "pssinfo_tempdb_pages_diff", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");
			mtd.addColumn("sysprocesses", "WaitClassDesc",        "Short description of what 'group' the WaitEventID is grouped in.");
			mtd.addColumn("sysprocesses", "WaitEventDesc",        "Short description of what this specific WaitEventID stands for.");
			mtd.addColumn("sysprocesses", "BatchIdDiff",          "How many 'SQL Batches' has the client sent to the server since the last sample, in Diff or Rate");

			mtd.addColumn("monProcess",   "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("monProcess",   "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");

			
//			mtd.addColumn("monDeviceIO", "AvgServ_ms", "Calculated column, Service time on the disk. Formula is: AvgServ_ms = IOTime / (Reads + Writes). If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default");

			mtd.addColumn("monProcessStatement", "ExecTime", "The statement has been executing for ## seconds. Calculated value. <b>Formula</b>: ExecTime=datediff(ss, StartTime, getdate())");
			mtd.addColumn("monProcessStatement", "ExecTimeInMs", "The statement has been executing for ## milliseconds. Calculated value. <b>Formula</b>: ExecTimeInMs=datediff(ms, StartTime, getdate())");

			mtd.addTable("sysmonitors",   "This is basically where sp_sysmon gets it's counters.");
			mtd.addColumn("sysmonitors",  "name",         "The internal name of the counter, this is strictly Sybase ASE INTERNAL name. If the description is NOT set it's probably an 'unknown' or not that important counter.");
			mtd.addColumn("sysmonitors",  "instances",    "How many instances of this spinslock actually exists. For examples for 'default data cache' it's the number of 'cache partitions' the cache has.");
//			mtd.addColumn("sysmonitors",  "grabs",        "How many times we where able to succesfuly where able to GET the spinlock.");
//			mtd.addColumn("sysmonitors",  "waits",        "How many times we had to wait for other engines before we where able to grab the spinlock.");
//			mtd.addColumn("sysmonitors",  "spins",        "How many times did we 'wait/spin' for other engies to release the lock. This is basically too many engines spins of the same resource.");
			mtd.addColumn("sysmonitors",  "grabs",        "Spinlock grabs as in attempted grabs for the spinlock - includes waits");
			mtd.addColumn("sysmonitors",  "waits",        "Spinlock waits - usually a good sign of contention");
			mtd.addColumn("sysmonitors",  "spins",        "Spinlock spins - this is the CPU spins that drives up CPU utilization. The higher the spin count, the more CPU usage and the more serious the performance impact of the spinlock on other processes not waiting");
			mtd.addColumn("sysmonitors",  "contention",   "waits / grabs, but in the percentage form. If this goes beond 10-20% then try to add more spinlock instances.");
			mtd.addColumn("sysmonitors",  "spinsPerWait", "spins / waits, so this is numer of times we had to 'spin' before cound grab the spinlock. If we had to 'spin/wait' for other engines that hold the spinlock. Then how many times did we wait/spin' for other engies to release the lock. If this is high (more than 100 or 200) we might have to lower the numer of engines.");
			mtd.addColumn("sysmonitors",  "description",  "If it's a known sppinlock, this field would have a valid description.");
			mtd.addColumn("sysmonitors",  "id",           "The internal ID of the spinlock, for most cases this would just be a 'number' that identifies the spinlock if the spinlock itself are 'partitioned', meaning the spinlocks itselv are partitioned using some kind of 'hash' algorithm or simular.");

			// Add all "known" counter name descriptions
			mtd.addSpinlockDescription("tablockspins",              "xxxx: tablockspins,  'lock table spinlock ratio'");
			mtd.addSpinlockDescription("fglockspins",               "xxxx: fglockspins,   'lock spinlock ratio'");
			mtd.addSpinlockDescription("addrlockspins",             "xxxx: addrlockspins, 'lock address spinlock ratio'");
			mtd.addSpinlockDescription("Resource->rdesmgr_spin",    "Object Manager Spinlock Contention");
			mtd.addSpinlockDescription("Des Upd Spinlocks",         "Object Spinlock Contention");
			mtd.addSpinlockDescription("Ides Spinlocks",            "Index Spinlock Contention");
			mtd.addSpinlockDescription("Ides Chain Spinlocks",      "Index Hash Spinlock Contention");
			mtd.addSpinlockDescription("Pdes Spinlocks",            "Partition Spinlock Contention");
			mtd.addSpinlockDescription("Pdes Chain Spinlocks",      "Partition Hash Spinlock Contention");
			mtd.addSpinlockDescription("Resource->rproccache_spin", "Procedure Cache Spinlock");
			mtd.addSpinlockDescription("Resource->rprocmgr_spin",   "Procedure Cache Manager Spinlock");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
			
			
//
//			sp_configure "spinlock"
//			go
//
//			Parameter Name                 Default     Memory Used Config Value Run Value    Unit                 Type       
//			--------------                 -------     ----------- ------------ ---------    ----                 ----       
//			lock address spinlock ratio            100           0          100          100 ratio                static     
//			lock spinlock ratio                     85           0           85           85 ratio                static     
//			lock table spinlock ratio               20           0           20           20 ratio                static     
//			open index hash spinlock ratio         100           0          100          100 ratio                dynamic    
//			open index spinlock ratio              100           0          100          100 ratio                dynamic    
//			open object spinlock ratio             100           0          100          100 ratio                dynamic    
//			partition spinlock ratio                10           6           10           10 ratio                dynamic    
//			user log cache spinlock ratio           20           0           20           20 ratio                dynamic    
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in '" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

}
