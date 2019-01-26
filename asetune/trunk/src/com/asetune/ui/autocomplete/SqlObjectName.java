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
package com.asetune.ui.autocomplete;

import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

/**
 * Helper class to put in a object name, and get all the individual parts.
 * @author gorans
 *
 */
public class SqlObjectName
{
	public String _fullName  = "";
	public String _catName   = "";
	public String _schName   = "";
	public String _objName   = "";
	
	public String _originFullName = "";
	public String _originCatName  = "";
	public String _originSchName  = "";
	public String _originObjName  = "";
	
	public String getFullName   ()       { return _fullName; }
	public String getCatalogName()       { return _catName; }
	public String getSchemaName ()       { return _schName; }
	public String getObjectName ()       { return _objName; }

	public String getOriginFullName   () { return _originFullName; }
	public String getOriginCatalogName() { return _originCatName; }
	public String getOriginSchemaName () { return _originSchName; }
	public String getOriginObjectName () { return _originObjName; }

	public String getFullNameN   ()       { return StringUtil.hasValue(_fullName) ? _fullName : null; }
	public String getCatalogNameN()       { return StringUtil.hasValue(_catName ) ? _catName  : null; }
	public String getSchemaNameN ()       { return StringUtil.hasValue(_schName ) ? _schName  : null; }
	public String getObjectNameN ()       { return StringUtil.hasValue(_objName ) ? _objName  : null; }

	public String getOriginFullNameN   () { return StringUtil.hasValue(_originFullName) ? _originFullName : null; }
	public String getOriginCatalogNameN() { return StringUtil.hasValue(_originCatName ) ? _originCatName  : null; }
	public String getOriginSchemaNameN () { return StringUtil.hasValue(_originSchName ) ? _originSchName  : null; }
	public String getOriginObjectNameN () { return StringUtil.hasValue(_originObjName ) ? _originObjName  : null; }

	private String  _dbProductName = null;
	private String  _dbIdentifierQuoteString = null;
	private boolean _dbStoresUpperCaseIdentifiers = false;

	private boolean _autoAddDboForSybaseAndSqlServer = true;
	
	/** 
	 * constructor using full name [catalog.][schema.][object] 
	 */
	public SqlObjectName(final String name, String dbProductName, String dbIdentifierQuoteString, boolean dbStoresUpperCaseIdentifiers)
	{
		_dbProductName                = dbProductName;
		_dbIdentifierQuoteString      = dbIdentifierQuoteString;
		_dbStoresUpperCaseIdentifiers = dbStoresUpperCaseIdentifiers;
		
		_autoAddDboForSybaseAndSqlServer = true;
		
		setFullName(name);
	}

	/** 
	 * constructor using full name [catalog.][schema.][object] 
	 */
	public SqlObjectName(final String name, String dbProductName, String dbIdentifierQuoteString, boolean dbStoresUpperCaseIdentifiers, boolean autoAddDboForSybaseAndSqlServer)
	{
		_dbProductName                = dbProductName;
		_dbIdentifierQuoteString      = dbIdentifierQuoteString;
		_dbStoresUpperCaseIdentifiers = dbStoresUpperCaseIdentifiers;
		
		_autoAddDboForSybaseAndSqlServer = autoAddDboForSybaseAndSqlServer;
		
		setFullName(name);
	}

	/**
	 * Set the fullname, which will be parsed to set all the individual parts<br>
	 * <br>
	 * Strip out quote characters and square brackets at start/end of the 
	 * string '"name"' and '[name]' will be 'name' <br>
	 * <br>
	 * The "unstriped" names is available in methods getOrigin{Full|Catalog|Schema|Object}Name()
	 *
	 * @param name [catalog.][schema.][object]
	 */
	public void setFullName   (String name) 
	{ 
		// Dont need to continue if it's empty...
		if (StringUtil.isNullOrBlank(name))
			return;

		_originFullName = name;
		_originCatName  = "";
		_originSchName  = "";
		_originObjName  = name;
		
		int dot1 = name.indexOf('.');
		if (dot1 >= 0)
		{
			_originSchName = name.substring(0, dot1);
			_originObjName = name.substring(dot1+1);

			int dot2 = name.indexOf('.', dot1+1);
			if (dot2 >= 0)
			{
				_originCatName = name.substring(0, dot1);
				_originSchName = name.substring(dot1+1, dot2);
				_originObjName = name.substring(dot2+1);
			}
		}
		
		// in some cases check schema/owner name
		if (_autoAddDboForSybaseAndSqlServer && (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(_dbProductName)))
		{
			// if empty schema/owner, add 'dbo'
			if (StringUtil.isNullOrBlank(_originSchName))
				_originSchName = "dbo";
		}
		
		_fullName = stripQuote( _originFullName, _dbIdentifierQuoteString );
		setCatalogName(_originCatName);
		setSchemaName (_originSchName);
		setObjectName (_originObjName);
	}

	/**
	 * Set the catalog name<br>
	 * <br>
	 * Strip out quote characters and square brackets at start/end of the 
	 * string '"name"' and '[name]' will be 'name' <br>
	 * <br>
	 * The "unstriped" names is available in methods getOriginCatalogName()
	 *
	 * @param name catalog name
	 */
	public void setCatalogName(String name) 
	{
		_originCatName = name;
		_catName       = stripQuote( name, _dbIdentifierQuoteString );

		if (_catName != null && _dbStoresUpperCaseIdentifiers)
			_catName = _catName.toUpperCase();
	}

	/**
	 * Set the schema name<br>
	 * <br>
	 * Strip out quote characters and square brackets at start/end of the 
	 * string '"name"' and '[name]' will be 'name' <br>
	 * <br>
	 * The "unstriped" names is available in methods getOriginSchemaName()
	 *
	 * @param name schema name
	 */
	public void setSchemaName (String name) 
	{
		_originSchName = name;
		_schName       = stripQuote( name, _dbIdentifierQuoteString );

		if (_schName != null && _dbStoresUpperCaseIdentifiers)
			_schName = _schName.toUpperCase();
	}

	/**
	 * Set the object name<br>
	 * <br>
	 * Strip out quote characters and square brackets at start/end of the 
	 * string '"name"' and '[name]' will be 'name' <br>
	 * <br>
	 * The "unstriped" names is available in methods getOriginObjectName
	 *
	 * @param name object name
	 */
	public void setObjectName (String name) 
	{
		_originObjName = name;
		_objName       = stripQuote( name, _dbIdentifierQuoteString );

		if (_objName != null && _dbStoresUpperCaseIdentifiers)
			_objName = _objName.toUpperCase();
	}

//	/** make: schemaName -> catalaogName and objectName -> schemaName and blank-out objectName */
//	public void shiftLeft()
//	{
//		_originCatName = _originSchName;
//		_originSchName = _originObjName;
//		_originObjName = "";
//
//		_catName = _schName;
//		_schName = _objName;
//		_objName = "";
//	}

	public boolean hasCatalogName() { return ! StringUtil.isNullOrBlank(_catName); }
	public boolean hasSchemaName()  { return ! StringUtil.isNullOrBlank(_schName); }
	public boolean hasObjectName()  { return ! StringUtil.isNullOrBlank(_objName); }

	/** true if it has CatalogName and SchemaName and ObjectName
	 * @return hasCatalogName() && hasScemaName() */
	public boolean isFullyQualifiedObject()  { return hasCatalogName() && hasSchemaName(); }
	
	/** true if it has schemaName and objectName, but NOT catalogName <br>
	 *  @return !hasCatalogName() && hasScemaName() */
	public boolean isSchemaQualifiedObject()  { return !hasCatalogName() && hasSchemaName(); }
	
	/** true if it has objectName, but NOT catalogName and schemaName <br>
	 *  @return !hasCatalogName() && !hasScemaName() */
	public boolean isSimpleQualifiedObject()  { return !hasCatalogName() && !hasSchemaName(); }
	
	@Override
	public String toString() 
	{
		return super.toString() + " catName='"+_catName+"', schName='"+_schName+"', objName='"+_objName+"', isFullyQualifiedObject="+isFullyQualifiedObject()+", isSchemaQualifiedObject="+isSchemaQualifiedObject()+", isSimpleQualifiedObject="+isSimpleQualifiedObject()+".";
	}
	
	
	
	/** 
	 * Strip out quote characters and square brackets at start/end of the string<br>
	 * '"colname"' and '[colname]' will be 'colname'
	 * @param str
	 * @return
	 */
	public static String stripQuote(String str, String dbIdentifierQuoteString)
	{
		if (str == null)
			return str;

		String quoteStr = dbIdentifierQuoteString;
		if (StringUtil.isNullOrBlank(quoteStr))
			quoteStr = "\"";

		// Strip leading/trailing '"' or whatever chars the database are using as quoted identifiers
		if (str.startsWith(quoteStr) && str.endsWith(quoteStr))
			str = str.substring(str.indexOf(quoteStr)+1, str.lastIndexOf(quoteStr));
		
		// Strip leading '"' or whatever chars the database are using as quoted identifiers
		else if (str.startsWith(quoteStr))
			str = str.substring(str.indexOf(quoteStr)+1);
		
		// Strip leading/trailing '[' and ']' 
		else if (str.startsWith("[") && str.endsWith("]"))
			str = str.substring(1, str.lastIndexOf(']'));

		// Strip leading '['
		else if (str.startsWith("["))
			str = str.substring(1);

		return str;
	}


}
