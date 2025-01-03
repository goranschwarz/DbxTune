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
package com.dbxtune.sql.ddl.model;

import java.util.List;

import com.dbxtune.sql.conn.DbxConnection;

public class View
{
	private Catalog _catalog;
	private Schema  _schema;
	
	private String _catalogName;
	private String _schemaName;
	private String _viewName;
	private String _description;

	private String _textDefinition;
	private List<String> _references;

	public Catalog getCatalog()     { return _catalog; }
	public Schema  getSchema()      { return _schema; }
	public Schema  getParent()      { return _schema; }

	public String  getCatalogName() { return _catalogName; }
	public String  getSchemaName()  { return _schemaName; }
	public String  getViewName()    { return _viewName; }
	public String  getDescription() { return _description; }

	public static View create(DbxConnection conn, String catalogName, String schemaName, String viewName)
	{
		View v = new View();
		
		v._catalogName = catalogName;
		v._schemaName  = schemaName;
		v._viewName    = viewName;

//		v._textDefinition = conn.getViewDefinition(catalogName, schemaName, viewName);
		v._references     = conn.getViewReferences(catalogName, schemaName, viewName);
		
		return v;
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
		result = prime * result + ((_catalogName == null) ? 0 : _catalogName.hashCode());
		result = prime * result + ((_schemaName == null) ? 0 : _schemaName.hashCode());
		result = prime * result + ((_viewName == null) ? 0 : _viewName.hashCode());
		return result;
	}
	
	/**
	 * Uses member 'catalogName', 'schemaName', 'viewName' as the equality 
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
		View other = (View) obj;
		if ( _catalogName == null )
		{
			if ( other._catalogName != null )
				return false;
		}
		else if ( !_catalogName.equals(other._catalogName) )
			return false;
		if ( _schemaName == null )
		{
			if ( other._schemaName != null )
				return false;
		}
		else if ( !_schemaName.equals(other._schemaName) )
			return false;
		if ( _viewName == null )
		{
			if ( other._viewName != null )
				return false;
		}
		else if ( !_viewName.equals(other._viewName) )
			return false;
		return true;
	}
}