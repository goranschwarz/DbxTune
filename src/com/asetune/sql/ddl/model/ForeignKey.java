/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.sql.ddl.model;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import com.asetune.utils.StringUtil;

public class ForeignKey
{
	protected Table     _table;
	
	protected List<ForeignKeyColumn> _columns = new ArrayList<>();

	String  _fkName      = null;
	int     _deleteRule;
	String  _deleteRuleText  = null;
	int     _updateRule;
	String  _updateRuleText  = null;
	String  _pkTabCat    = null;
	String  _pkTabSchema = null;
	String  _pkTabName   = null;
	String  _fkTabCat    = null;
	String  _fkTabSchema = null;
	String  _fkTabName   = null;

	List<String> _fkColumns = new ArrayList<>();
	List<String> _pkColumns = new ArrayList<>();

//	private int    _updateRule;
//	private int    _deleteRule;

	public Table   getTable()       { return _table; }
	public Schema  getSchema()      { return _table.getSchema(); }
	public Table   getParent()      { return _table; }

	public String  getForeignKeyName() { return _fkName; }
	
//	public String  getCatName()    { return _fkTabCat; }
	public String  getSchemaName() { return _fkTabSchema; }
	public String  getTableName()  { return _fkTabName; }

//	public String  getDestCatName()    { return _pkTabCat; }
	public String  getDestSchemaName() { return _pkTabSchema; }
	public String  getDestTableName()  { return _pkTabName; }

	public List<String> getColumnNames()    { return _fkColumns; }
	public List<String> getDestColumnNames()  { return _pkColumns; }

	public int    getUpdateRule()     { return _updateRule; }
	public int    getDeleteRule()     { return _deleteRule;  }

	public String getUpdateRuleText() { return _updateRuleText; }
	public String getDeleteRuleText() { return _deleteRuleText;  }
	
	public ForeignKey(Table table, String fkName, int deferrability, int deleteRule, int updateRule, String pkTabCat, String pkTabSchema, String pkTabName, String fkTabCat, String fkTabSchema, String fkTabName)
	{
		_table  = table;
		_fkName = fkName;
		
		_deleteRule = deleteRule;
		_updateRule = updateRule;
		
		if      (deleteRule == DatabaseMetaData.importedKeyCascade)  _deleteRuleText = "CASCADE";
		else if (deleteRule == DatabaseMetaData.importedKeyRestrict) _deleteRuleText = "RESTRICT";
		else if (deleteRule == DatabaseMetaData.importedKeySetNull)  _deleteRuleText = "SET NULL";
		else                                                         _deleteRuleText = "NO ACTION";

		if      (updateRule == DatabaseMetaData.importedKeyCascade)  _updateRuleText = "CASCADE";
		else if (updateRule == DatabaseMetaData.importedKeyRestrict) _updateRuleText = "RESTRICT";
		else if (updateRule == DatabaseMetaData.importedKeySetNull)  _updateRuleText = "SET NULL";
		else                                                         _updateRuleText = "NO ACTION";

		_pkTabCat    = pkTabCat;
		_pkTabSchema = pkTabSchema;
		_pkTabName   = pkTabName;
		_fkTabCat    = fkTabCat;
		_fkTabSchema = fkTabSchema;
		_fkTabName   = fkTabName;
	}
	public void addFkColumn(String name)
	{
		_fkColumns.add(name);
	}
	public void addPkColumn(String name)
	{
		_pkColumns.add(name);
	}
	private String objName(String cat, String schema, String name)
	{
		StringBuilder sb = new StringBuilder();
		if (StringUtil.hasValue(cat))    sb.append(cat)   .append(".");
		if (StringUtil.hasValue(schema)) sb.append(schema).append(".");
		sb.append(name);
		return sb.toString();
	}
	public String getDdl()
	{
		// constraint fk_AppServer    foreign key(versionId, resourceAppServer) references Resources_AppServer(versionId, resourceName),
		// alter table XXX add constraint XXX foreign key(c1,c2) references XXX(c1,c2)
		StringBuilder sb = new StringBuilder();
		sb.append("alter table ").append(objName(_fkTabCat, _fkTabSchema, _fkTabName));
		sb.append(" add constraint ").append(_fkName);
		sb.append(" foreign key(<FONT color='blue'>").append(StringUtil.toCommaStr(_fkColumns)).append("</FONT>)");
		sb.append("<BR>");
		sb.append(" references ").append(objName(_pkTabCat, _pkTabSchema, _pkTabName)).append("(<FONT color='blue'>").append(StringUtil.toCommaStr(_pkColumns)).append("</FONT>)");
		
		sb.append("<BR>");
		sb.append("<FONT color='green'>");
		sb.append(" -- ");
		sb.append(" on update ").append(_updateRuleText);
		sb.append(" on delete ").append(_deleteRuleText);
		sb.append("</FONT>");
		
		return sb.toString();
	}




	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	// Basic methods (generated by eclipse)
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		final int prime  = 31;
		int       result = 1;
		result = prime * result + ((_fkName == null) ? 0 : _fkName.hashCode());
		result = prime * result + ((_table == null) ? 0 : _table.hashCode());
		return result;
	}
	
	/**
	 * Uses member 'fkName', 'Table object' as the equality 
	 */
	@Override
	public boolean equals(Object obj)
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		ForeignKey other = (ForeignKey) obj;
		if ( _fkName == null )
		{
			if ( other._fkName != null )
				return false;
		}
		else if ( !_fkName.equals(other._fkName) )
			return false;
		if ( _table == null )
		{
			if ( other._table != null )
				return false;
		}
		else if ( !_table.equals(other._table) )
			return false;
		return true;
	}
}
