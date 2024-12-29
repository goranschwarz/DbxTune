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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.config.dbms;

import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.dbxtune.gui.ConnectionDialog;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.SwingUtils;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

public class DbmsConfigDiffEngine
{
	private static Logger _logger = Logger.getLogger(DbmsConfigDiffEngine.class);

	private Window _owner;
//	private DbxConnection _localConn;
	private DbxConnection _remoteConn;
	
	private IDbmsConfig _localDbmsConfigObj;
	private IDbmsConfig _remoteDbmsConfigObj;

//	public DbmsConfigDiffEngine(Window owner, DbxConnection localConn, DbxConnection remoteConn)
	public DbmsConfigDiffEngine(Window owner, DbxConnection remoteConn)
	{
		_owner      = owner;
//		_localConn  = localConn;
		_remoteConn = remoteConn;
	}

	public boolean initialize()
	{
		boolean createdRemoteConnection = false;
		
		// Create a remote connection if one wasn't passed
		// AND if we have a GUI
		if (_owner != null && _remoteConn == null)
		{
			_remoteConn = createRemoteConnection();
			if (_remoteConn == null)
			{
				SwingUtils.showInfoMessage(_owner, "No Remote Connection", "No remote connection was created. Sorry no Configuration Comparison could be done.");
				return false;
			}
			createdRemoteConnection = true;
		}

		if (_remoteConn == null)
		{
			throw new RuntimeException("No valid remote-connection, this makes it impossible to do Configuration Diff checks");
		}
		
		// Create a remote IDbmsConfig, using "same" IDbmsConfig class/object-type as in the DbmsConfigManager
		_localDbmsConfigObj  = DbmsConfigManager.getInstance();
		_remoteDbmsConfigObj = null;
		String newClassName = _localDbmsConfigObj.getClass().getName();
		try
		{
			_logger.info("Creating a new IDbmsConfig object for Config Diff, the used classname is '" + newClassName + "'.");

			Class<?> clazz = Class.forName(newClassName);
			Constructor<?> constr = clazz.getConstructor();
			
			_remoteDbmsConfigObj = (IDbmsConfig) constr.newInstance();
		}
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			if (_owner != null)
				SwingUtils.showErrorMessage(_owner, "Create IDbmsConfig object", "Problems creating the 'remote' IDbmsConfig of type '"+newClassName+"'.", ex);

			_logger.error("Problems creating the 'remote' IDbmsConfig of type '"+newClassName+"'.");
			return false;
		}

		// Check if the connected database looks like a OFFLINE database... Just check for a 'set' of known tables.
//		boolean localIsOfflineDb  = PersistReader.isOfflineDb(localConn);
		boolean remoteIsOfflineDb = PersistReader.isOfflineDb(_remoteConn);

		try
		{
			_remoteDbmsConfigObj.initialize(_remoteConn, (_owner != null), remoteIsOfflineDb, null);
		}
		catch (Exception ex)
		{
			String url = null;
			try { url = _remoteConn.getMetaData().getURL(); } catch(SQLException ignore) {}
			
			String srvName = _remoteConn.getDbmsServerNameNoThrow();
			
			if (_owner != null)
			{
				String htmlMsg = "<html>"
						+ "Problems initializing the <b>remote</b> IDbmsConfig of type <code>" + newClassName + "</code><br>"
						+ "Reading from server name: <code>" + srvName + "</code><br>"
						+ "using url: <code>"+url+"</code><br>"
						+ "<br>"
						+ ex.getMessage();

				if (ex instanceof WrongRecordingVendorContent)
				{
					htmlMsg += "<br><br><br>"
							+ "<b>Try another database recording file.</b><br>"
							+ "This will NOT be possible to load here (or connect to a DBMS vendor of same type as the recording).<br>";
				}

				htmlMsg += "</html>";
				
				SwingUtils.showErrorMessage(_owner, "Initialize IDbmsConfig object", htmlMsg, ex);
			}
			else
			{
				_logger.error("Problems initializing the 'remote' IDbmsConfig of type '" + newClassName + "'. " 
						+ "Reading from server name '" + srvName + "'. " 
						+ "using url '" + url + "'. "
						+ "Caught: " + ex);
			}
			
			return false;
		}
		finally
		{
			if (createdRemoteConnection && _remoteConn != null)
			{
				_logger.info("Config-Diff-Engine: Closing Temporary Connection to Remote DBMS." + (_remoteDbmsConfigObj == null ? "" : " DatabaseServerName='"+_remoteDbmsConfigObj.getDbmsServerName()+"', DatabaseProductVersion='"+_remoteDbmsConfigObj.getDbmsVersionStr()+"'.") );
				_remoteConn.closeNoThrow();
			}
		}

		return true;
	}

	public Context checkForDiffrens()
	{
		SortedMap<String, String> leftMap  = new TreeMap<>();
		SortedMap<String, String> rightMap = new TreeMap<>();

		Map<String, ? extends IDbmsConfigEntry> leftTmp  = _localDbmsConfigObj .getDbmsConfigMap();
		Map<String, ? extends IDbmsConfigEntry> rightTmp = _remoteDbmsConfigObj.getDbmsConfigMap();

		for (IDbmsConfigEntry ce : leftTmp.values())
			leftMap.put(ce.getConfigKey(), ce.getConfigValue());

		for (IDbmsConfigEntry ce : rightTmp.values())
			rightMap.put(ce.getConfigKey(), ce.getConfigValue());

		//-------------------------------------------
		// This is where the "magic" happens
		//-------------------------------------------
		MapDifference<String, String> mapDiff = Maps.difference(leftMap, rightMap);
		
		// Create a Context object, which can be checked or passed on. 
		return new Context(this, mapDiff, _localDbmsConfigObj, _remoteDbmsConfigObj);
	}

//------------------------------------------------------------------------------
// save below for "a while" if we want to go back using the DIFF Engine that exists for ResultSet
// I leaved that because: different "sort order" and "case sensitivity" made it harder to compare, while the above Maps.difference(...) do not care about that...
//------------------------------------------------------------------------------
//	private DiffTableModel doConfigDiff(DbxConnection localConn, DbxConnection remoteConn, String localSql, String remoteSql)
//	{
//		DiffContext context = new DiffContext();
//		context.setGuiOwner(this);
//		context.setProgressDialog(null);
//		if (_logger.isDebugEnabled())    context.setMessageDebugLevel(1);
//		if (_logger.isTraceEnabled())    context.setMessageDebugLevel(2);
//		if (false)                       context.setMessageToStdout(true);
//
////		context.setMessageDebugLevel(1);
////		context.setMessageDebugLevel(2);
////		context.setMessageToStdout(true);
//
//		try
//		{
//			Statement leftStmt = localConn.createStatement();
//			ResultSet leftRs   = leftStmt.executeQuery(localConn.quotifySqlString(localSql));
//
//			Statement rightStmt = remoteConn.createStatement();
//			ResultSet rightRs = rightStmt.executeQuery(remoteConn.quotifySqlString(remoteSql));
//
//			List<String> pkList = new ArrayList<>();
//			pkList.add("ConfigName");
//			
//			List<String> diffCols = new ArrayList<>();
//			diffCols.add("CfgValStr");
//			
//			context.setPkColumns  (pkList);
//			context.setDiffColumns(diffCols);
//
//			context.setDiffTable(DiffSide.LEFT,  leftRs,  localSql , null);
//			context.setDiffTable(DiffSide.RIGHT, rightRs, remoteSql, null);
//
//			context.validate();
//			
////			MapDifference<String, String> mapDifference = Maps.difference(geometryClass, gymClass);
////			Map<String, ValueDifference<String>> xxx = mapDifference.entriesDiffering();
////			for (ValueDifference<String> vd : xxx.values())
////			{
////				vd.leftValue();
////				vd.rightValue();
////			}
//
//			int diffCount = context.doDiff();
//			if (diffCount == 0)
//				return null;
//			
//			DiffTableModel diffTableModel = new DiffTableModel(context);
//			return diffTableModel;
//		}
//		catch(Exception ex)
//		{
//ex.printStackTrace();
//		}
//
//		return null;
//	}

	/**
	 * If a Connection wasn't passed on creation (and we have a GUI, create a Connection)
	 * @return
	 */
	private DbxConnection createRemoteConnection()
	{
		ConnectionDialog.Options connDialogOptions = new ConnectionDialog.Options();
		connDialogOptions._dialogTitlePostfix       = "To get Remote DBMS Configuration";
		connDialogOptions._srvExtraChecks           = null;
		connDialogOptions._showAseTab               = true;
		connDialogOptions._showDbxTuneOptionsInTds  = false;
		connDialogOptions._showHostmonTab           = false;
		connDialogOptions._showPcsTab               = false;
		connDialogOptions._showOfflineTab           = true;
		connDialogOptions._showJdbcTab              = true;
		connDialogOptions._showDbxTuneOptionsInJdbc = false;

		// Open a Connection Dialog
		ConnectionDialog connDialog = new ConnectionDialog(_owner, connDialogOptions);

		// Show the dialog and wait for response
		connDialog.setVisible(true);
		connDialog.dispose();

		// Get what was connected to...
		int connType = connDialog.getConnectionType();

		if ( connType == ConnectionDialog.CANCEL)
			return null;

		// Get product info
		try	
		{
			String connectedToProductName    = connDialog.getDatabaseProductName(); 
			String connectedToProductVersion = connDialog.getDatabaseProductVersion(); 
			String connectedToServerName     = connDialog.getDatabaseServerName();
			String connectedAsUser           = connDialog.getUsername();
			String connectedWithUrl          = connDialog.getUrl();

			_logger.info("Config-Diff-Engine: Connected to DatabaseProductName='"+connectedToProductName+"', DatabaseProductVersion='"+connectedToProductVersion+"', DatabaseServerName='"+connectedToServerName+"' with Username='"+connectedAsUser+"', toURL='"+connectedWithUrl+"'.");
		} 
		catch (Throwable ex) 
		{
			if (_logger.isDebugEnabled())
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex, ex);
			else
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex);
		}

		return connDialog.getConnection();
	}

	public static class Context
	{
		private MapDifference<String, String> _mapDiff;
		private IDbmsConfig                   _localDbmsConfigObj;
		private IDbmsConfig                   _remoteDbmsConfigObj;
		private DbmsConfigDiffEngine          _diffEngine;

		public Context(DbmsConfigDiffEngine diffEngine, MapDifference<String, String> mapDiff, IDbmsConfig localDbmsConfigObj, IDbmsConfig remoteDbmsConfigObj)
		{
			_diffEngine          = diffEngine;
			_mapDiff             = mapDiff;
			_localDbmsConfigObj  = localDbmsConfigObj;
			_remoteDbmsConfigObj = remoteDbmsConfigObj;
		}
		
		public DbmsConfigDiffEngine getDiffEngine() { return _diffEngine; }

		public boolean hasDifferences()
		{
			if (_mapDiff.entriesOnlyOnLeft() .size() > 0) return true;
			if (_mapDiff.entriesOnlyOnRight().size() > 0) return true;
			if (_mapDiff.entriesDiffering()  .size() > 0) return true;

			return false;
		}
		
		public Map<String, String>                  getEntriesOnlyInLocalDbms()  { return _mapDiff.entriesOnlyOnLeft(); }
		public Map<String, String>                  getEntriesOnlyInRemoteDbms() { return _mapDiff.entriesOnlyOnRight(); }
		public Map<String, String>                  getEntriesInCommon()         { return _mapDiff.entriesInCommon(); }
		public Map<String, ValueDifference<String>> getEntriesDiffering()        { return _mapDiff.entriesDiffering(); }
		
		public IDbmsConfig getLocalDbmsConfig()  { return _localDbmsConfigObj; }
		public IDbmsConfig getRemoteDbmsConfig() { return _remoteDbmsConfigObj; }
		
		public boolean isLocalOfflineConfig()  { return _localDbmsConfigObj .isOfflineConfig(); }
		public boolean isRemoteOfflineConfig() { return _remoteDbmsConfigObj.isOfflineConfig(); }
	}
}
