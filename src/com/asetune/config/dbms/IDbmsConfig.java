/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.config.dbms;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.swing.table.TableModel;

import com.asetune.sql.conn.DbxConnection;

public interface IDbmsConfig
extends TableModel
{

//	/** get the Map */
//	public Map<String, AseConfigEntry> getDbmsConfigMap();

	/** check if the AseConfig is initialized or not */
	public boolean isInitialized();

	/**
	 * Initialize 
	 * @param conn
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop initializing...
	 */
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts) 
	throws SQLException;

	/**
	 * refresh 
	 * @param conn
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop refreshing...
	 */
	public void refresh(DbxConnection conn, Timestamp ts) 
	throws SQLException;

	/**
	 * Check the configuration collected in refresh() and add/create a issue/anomaly if anything strange was found. 
	 */
	public void checkConfig(DbxConnection conn);

	/** add an issue */
	public void addConfigIssue(DbmsConfigIssue issue);

	/** Get configuration issues */
	public List<DbmsConfigIssue> getConfigIssues();
	
	/** Check if we have any configuration issues */
	public boolean hasConfigIssues();
	
	/**
	 * Get description for the configuration name
	 * @param colfigName
	 * @return the description
	 */
	public String getDescription(String configName);

//	/**
//	 * Get run value
//	 * @param colfigName
//	 * @return the description
//	 */
//	public int getRunValue(String configName);

	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	public List<String> getSectionList();

	/**
	 * When was the data refreshed
	 * @return Timestamp
	 */
	public Timestamp getTimestamp();

	//--------------------------------------------------------------------------------
	// BEGIN: implement: AbstractTableModel
	//--------------------------------------------------------------------------------
	@Override public Class<?> getColumnClass(int columnIndex);
	@Override public int getColumnCount();
	@Override public String getColumnName(int columnIndex);
	@Override public int getRowCount();
	@Override public Object getValueAt(int rowIndex, int columnIndex);
	@Override public boolean isCellEditable(int rowIndex, int columnIndex);
	public int findColumn(String columnName);
	//--------------------------------------------------------------------------------
	// END: implement: AbstractTableModel
	//--------------------------------------------------------------------------------

	public String getColumnToolTip(String colName);
	public String getCellToolTip(int mrow, int mcol);

	public String getSqlDataType(String colName);

	public String getSqlDataType(int colIndex);

	/**
	 * Get name for the Label of the TabbedPane
	 * @return
	 */
	public String getTabLabel();

	/** return a column name that indicate if this column has "pending" changes (used for filtering), return null if you do not have such a column */
	public String getColName_pending();

	/** return a column name that indicate if this column is a section name (used for filtering), return null if you do not have such a column */
	public String getColName_sectionName();

	/** return a column name that indicate if this column is a configuration name (used for filtering), return null if you do not have such a column */
	public String getColName_configName();

	/** return a column name that indicate if this column contains a non-default configuration value (used for filtering), return null if you do not have such a column */
	public String getColName_nonDefault();

	/** Get how much memory that are free to reconfigure, or just some extra text on the GUI left lover side */
	public String getFreeMemoryStr();

	/** Reset the underlying data structure */
	void reset();

	/** Should we show the table right click menu 'Reverse Engineer the configuration...' */
	public boolean isReverseEngineeringPossible();

	/**
	 * Return a String with the reverse engineer DDL statements
	 * @param modelRow null means ALL rows, otherwise an array of model rows that should be generated 
	 * @return A String with the SQL Statements that should be executed...
	 */
	public String reverseEngineer(int[] modelRows);
}
