package com.asetune.config.dbms;

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
	 */
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts);

	/**
	 * refresh 
	 * @param conn
	 */
	public void refresh(DbxConnection conn, Timestamp ts);

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
}
