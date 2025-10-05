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
package com.dbxtune.sql;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * Helper class to put in a object name, and get all the individual parts.
 * @author gorans
 *
 */
public class SqlObjectName
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	// If DBMS Stores the Table Names in UPPERCASE then: This value is Translated to upper case 
//	private String _fullName  = "";
	private String _catName   = "";
	private String _schName   = "";
	private String _objName   = "";
	
	// Origin == Keep Origin Case Sensitivity
//	private String _fullNameOrigin = "";
	private String _catNameOrigin  = "";
	private String _schNameOrigin  = "";
	private String _objNameOrigin  = "";
	
	// Raw Origin == with quotes etc
//	private String _fullNameUnModified = "";
	private String _catNameUnModified  = "";
	private String _schNameUnModified  = "";
	private String _objNameUnModified  = "";
	
	private String _dbExtraNameCharacters = null;
	
	//---------------------------------------------------------------------
	// Left and Right quote characters
	//---------------------------------------------------------------------
	public String getLeftQuote()  { return (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(_dbProductName)) ? "[" : _dbIdentifierQuoteString; }
	public String getRightQuote() { return (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(_dbProductName)) ? "]" : _dbIdentifierQuoteString; }

	/**
	 * Add DBMS Quoted Identifier Chars around the input string<br>
	 * if input string is empty it will simply be returned untouched.
	 * 
	 * @param str
	 * @return
	 */
	public String quotify(String name)  
	{
		if (StringUtil.isNullOrBlank(name))
			return name;
		
		StringBuilder sb = new StringBuilder();

		sb.append(getLeftQuote()).append(name).append(getRightQuote());
		
		return sb.toString();
	}

	/**
	 * Add DBMS Quoted Identifier Chars around the input string, if it needs it...<br>
	 * if input string is empty it will simply be returned untouched.
	 * 
	 * @param str
	 * @return
	 */
	public String quotifyIfNeeded(String name)  
	{ 
		if (StringUtil.isNullOrBlank(name))
			return name;

		boolean normalChars = true;
		for (int i=0; i<name.length(); i++)
		{
			char c = name.charAt(i);

			// goto next character for any "allowed" characters
			if ( Character.isLetterOrDigit(c) ) continue;
			if ( c == '_' )                     continue;

			if (_dbExtraNameCharacters != null && _dbExtraNameCharacters.indexOf(c) >= 0) continue;

			// if any other chars, then break and signal "non-normal-char" detected
			normalChars = false;
			break;
		}
		
		if ( normalChars )
			return name;
		
		return quotify(name);
	}

	//---------------------------------------------------------------------
	// "transformed"
	//---------------------------------------------------------------------
	/** 
	 * Get the full table name prefixed with catalog/database schema (if any of them existed when created the object)<br>
	 * If the input names are quoted using DBMS Quoted Identifier Char or [], the quotes will be Stripped.<br>
	 * If the DBMS is storing the names in the DBMS Dictionary as upper case, then the returned names will be in upper.<br>
	 * 
	 * @return [catname.][schema.]tablename
	 */
	public String getFullName()
	{
		StringBuilder sb = new StringBuilder();

		String cat = getCatalogName();
		String sch = getSchemaName();
		String obj = getObjectName();
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);
		
		return sb.toString();
	}
	/** Get Catalog/DB Name part from the input, quotes are <b>stripped</b>, to <b>upper</b> if storesUpperCaseIdentifiers() is true */
	public String getCatalogName()       { return _catName; }

	/** Get Schema Name part from the input, quotes are <b>stripped</b>, to <b>upper</b> if storesUpperCaseIdentifiers() is true */
	public String getSchemaName ()       { return _schName; }

	/** Get Object Name part from the input, quotes are <b>stripped</b>, to <b>upper</b> if storesUpperCaseIdentifiers() is true */
	public String getObjectName ()       { return _objName; }

	//---------------------------------------------------------------------
	// ORIGIN
	//---------------------------------------------------------------------
	/** 
	 * Get the full table name prefixed with catalog/database schema (if any of them existed when created the object)<br>
	 * If the input names are quoted using DBMS Quoted Identifier Char or [], the quotes will be Stripped.<br>
	 * The names Upper/Lower case will <b>not</b> be changed, it will be the same as the entered when creating the object<br>
	 * 
	 * @return [catname.][schema.]tablename
	 */
	public String getFullNameOrigin()
	{
		StringBuilder sb = new StringBuilder();

		String cat = getCatalogNameOrigin();
		String sch = getSchemaNameOrigin();
		String obj = getObjectNameOrigin();
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);
		
		return sb.toString();
	}
	/** Get Catalog/DB Name part from the input, quotes are <b>stripped</b>, unmodified upper/lower case */
	public String getCatalogNameOrigin() { return _catNameOrigin; }

	/** Get Schema Name part from the input, quotes are <b>stripped</b>, unmodified upper/lower case */
	public String getSchemaNameOrigin () { return _schNameOrigin; }

	/** Get Object Name part from the input, quotes are <b>stripped</b>, unmodified upper/lower case */
	public String getObjectNameOrigin () { return _objNameOrigin; }

	//---------------------------------------------------------------------
	// RAW ORIGIN
	//---------------------------------------------------------------------
	/** 
	 * Get the full table name prefixed with catalog/database schema (if any of them existed when created the object)<br>
	 * If the input names will be the same as entered. Hence the Origin RAW<br>
	 * No Upper/Lower changes<br>
	 * No Stripping of Quoted Identifier Chars<br>
	 * 
	 * @return [catname.][schema.]tablename
	 */
	public String getFullNameUnModified()
	{
		StringBuilder sb = new StringBuilder();

		String cat = getCatalogNameUnModified();
		String sch = getSchemaNameUnModified();
		String obj = getObjectNameUnModified();
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);
		
		return sb.toString();
	}
	/** Get Catalog/DB Name part from the input, input are "untouched" */
	public String getCatalogNameUnModified() { return _catNameUnModified; }

	/** Get Schema Name part from the input, input are "untouched" */
	public String getSchemaNameUnModified () { return _schNameUnModified; }

	/** Get Object Name part from the input, input are "untouched" */
	public String getObjectNameUnModified () { return _objNameUnModified; }

	//---------------------------------------------------------------------
	// "transformed" NULL return if no value
	//---------------------------------------------------------------------
	/** returns NULL if Catalog, Schema or Object name is empty otherwise same as getFullName() */
	public String getFullNameNull   ()       { return (StringUtil.isNullOrBlank(_catName) && StringUtil.isNullOrBlank(_schName) && StringUtil.isNullOrBlank(_objName)) ? null : getFullName(); }

	/** returns NULL if Catalog name is empty otherwise same as getCatalogName() */
	public String getCatalogNameNull()       { return StringUtil.hasValue(_catName ) ? _catName  : null; }

	/** returns NULL if Schema name is empty otherwise same as getSchemaName() */
	public String getSchemaNameNull ()       { return StringUtil.hasValue(_schName ) ? _schName  : null; }

	/** returns NULL if Object name is empty otherwise same as getObjectName() */
	public String getObjectNameNull ()       { return StringUtil.hasValue(_objName ) ? _objName  : null; }

	//---------------------------------------------------------------------
	// ORIGIN
	//---------------------------------------------------------------------
	/** returns NULL if Origin Catalog, Schema or Object name is empty otherwise same as getFullNameOrigin() */
	public String getFullNameOriginNull   () { return (StringUtil.isNullOrBlank(_catNameOrigin) && StringUtil.isNullOrBlank(_schNameOrigin) && StringUtil.isNullOrBlank(_objNameOrigin)) ? null : getFullNameOrigin(); }

	/** returns NULL if Origin Catalog name is empty otherwise same as getCatalogNameOrigin() */
	public String getCatalogNameOriginNull() { return StringUtil.hasValue(_catNameOrigin ) ? _catNameOrigin  : null; }
	
	/** returns NULL if Origin Schema name is empty otherwise same as getSchemaNameOrigin() */
	public String getSchemaNameOriginNull () { return StringUtil.hasValue(_schNameOrigin ) ? _schNameOrigin  : null; }
	
	/** returns NULL if Origin Object name is empty otherwise same as getObjectNameOrigin() */
	public String getObjectNameOriginNull () { return StringUtil.hasValue(_objNameOrigin ) ? _objNameOrigin  : null; }

	//---------------------------------------------------------------------
	// RAW ORIGIN
	//---------------------------------------------------------------------
	/** returns NULL if UnModified Catalog, Schema or Object name is empty otherwise same as getFullNameUnModified() */
	public String getFullNameUnModifiedNull   () { return (StringUtil.isNullOrBlank(_catNameUnModified) && StringUtil.isNullOrBlank(_schNameUnModified) && StringUtil.isNullOrBlank(_objNameUnModified)) ? null : getFullNameUnModified(); }

	/** returns NULL if UnModified Catalog name is empty otherwise same as getCatalogNameUnModified() */
	public String getCatalogNameUnModifiedNull() { return StringUtil.hasValue(_catNameUnModified ) ? _catNameUnModified  : null; }

	/** returns NULL if UnModified Schema name is empty otherwise same as getSchemaNameUnModified() */
	public String getSchemaNameUnModifiedNull () { return StringUtil.hasValue(_schNameUnModified ) ? _schNameUnModified  : null; }

	/** returns NULL if UnModified Object name is empty otherwise same as getObjectNameUnModified() */
	public String getObjectNameUnModifiedNull () { return StringUtil.hasValue(_objNameUnModified ) ? _objNameUnModified  : null; }

	//---------------------------------------------------------------------
	// "transformed" WITH QUOTES
	//---------------------------------------------------------------------
	/** 
	 * Add DBMS Vendor Specific Quoted Identifier Chars around each part (if DBMS Vendor is Sybase or MS SQL-Server, use brackets) <br>
	 * If Catalog/DB Name or Schema Name is empty they will be omitted
	 * 
	 * @returns "cat"."schema"."table" 
	 */
	public String getFullNameQuoted()
	{
		StringBuilder sb = new StringBuilder();

		String cat = quotify( getCatalogName() );
		String sch = quotify( getSchemaName()  );
		String obj = quotify( getObjectName()  );
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);
		
		return sb.toString();
	}

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getCatalogNameQuoted() { return StringUtil.isNullOrBlank(getCatalogName()) ? "" : quotify( getCatalogName() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getSchemaNameQuoted () { return StringUtil.isNullOrBlank(getSchemaName())  ? "" : quotify( getSchemaName() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getObjectNameQuoted () { return StringUtil.isNullOrBlank(getObjectName())  ? "" : quotify( getObjectName() ); }


	//---------------------------------------------------------------------
	// ORIGIN "transformed" WITH QUOTES
	//---------------------------------------------------------------------
	/** 
	 * Add DBMS Vendor Specific Quoted Identifier Chars around each part (if DBMS Vendor is Sybase or MS SQL-Server, use brackets) <br>
	 * If Catalog/DB Name or Schema Name is empty they will be omitted
	 * 
	 * @returns "cat"."schema"."table" 
	 */
	public String getFullNameOriginQuoted()
	{
		StringBuilder sb = new StringBuilder();

		String cat = quotify( getCatalogNameOrigin() );
		String sch = quotify( getSchemaNameOrigin()  );
		String obj = quotify( getObjectNameOrigin()  );
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);

		return sb.toString();
	}
	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getCatalogNameOriginQuoted() { return StringUtil.isNullOrBlank(getCatalogNameOrigin()) ? "" : quotify( getCatalogNameOrigin() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getSchemaNameOriginQuoted () { return StringUtil.isNullOrBlank(getSchemaNameOrigin())  ? "" : quotify( getSchemaNameOrigin()  ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getObjectNameOriginQuoted () { return StringUtil.isNullOrBlank(getObjectNameOrigin())  ? "" : quotify( getObjectNameOrigin()  ); }


	//---------------------------------------------------------------------
	// "transformed" WITH QUOTES
	//---------------------------------------------------------------------
	/** 
	 * Add DBMS Vendor Specific Quoted Identifier Chars around each part, but only if the names contains any strange chars otherwise not quoted (if DBMS Vendor is Sybase or MS SQL-Server, use brackets) <br>
	 * If Catalog/DB Name or Schema Name is empty they will be omitted
	 * 
	 * @returns "cat"."schema"."table" 
	 */
	public String getFullNameQuotedIfNeeded()
	{
		StringBuilder sb = new StringBuilder();

		String cat = quotifyIfNeeded( getCatalogName() );
		String sch = quotifyIfNeeded( getSchemaName()  );
		String obj = quotifyIfNeeded( getObjectName()  );
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);
		
		return sb.toString();
	}

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getCatalogNameQuotedIfNeeded() { return StringUtil.isNullOrBlank(getCatalogName()) ? "" : quotifyIfNeeded( getCatalogName() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getSchemaNameQuotedIfNeeded () { return StringUtil.isNullOrBlank(getSchemaName())  ? "" : quotifyIfNeeded( getSchemaName() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getObjectNameQuotedIfNeeded () { return StringUtil.isNullOrBlank(getObjectName())  ? "" : quotifyIfNeeded( getObjectName() ); }


	//---------------------------------------------------------------------
	// ORIGIN "transformed" WITH QUOTES
	//---------------------------------------------------------------------
	/** 
	 * Add DBMS Vendor Specific Quoted Identifier Chars around each part, but only if the names contains any strange chars otherwise not quoted (if DBMS Vendor is Sybase or MS SQL-Server, use brackets) <br>
	 * If Catalog/DB Name or Schema Name is empty they will be omitted
	 * 
	 * @returns "cat"."schema"."table" 
	 */
	public String getFullNameOriginQuotedIfNeeded()
	{
		StringBuilder sb = new StringBuilder();

		String cat = quotifyIfNeeded( getCatalogNameOrigin() );
		String sch = quotifyIfNeeded( getSchemaNameOrigin()  );
		String obj = quotifyIfNeeded( getObjectNameOrigin()  );
		
		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);

		return sb.toString();
	}
	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getCatalogNameOriginQuotedIfNeeded() { return StringUtil.isNullOrBlank(getCatalogNameOrigin()) ? "" : quotifyIfNeeded( getCatalogNameOrigin() ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getSchemaNameOriginQuotedIfNeeded () { return StringUtil.isNullOrBlank(getSchemaNameOrigin())  ? "" : quotifyIfNeeded( getSchemaNameOrigin()  ); }

	/** Add DBMS Vendor Specific Quoted Identifier Chars around the name */
	public String getObjectNameOriginQuotedIfNeeded () { return StringUtil.isNullOrBlank(getObjectNameOrigin())  ? "" : quotifyIfNeeded( getObjectNameOrigin()  ); }


	//---------------------------------------------------------------------
	// RAW ORIGIN "transformed" WITH QUOTES
	//---------------------------------------------------------------------
	// We do NOT want RAW origin WITH QUOTES
	// the RAW in the "untouched" input
//	public String getFullNameOriginRawQuoted()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		String cat = getRawOriginCatalogName();
//		String sch = getRawOriginSchemaName();
//		String obj = getRawOriginObjectName();
//		
//		if (StringUtil.hasValue(cat)) sb.append(getLeftQuote()).append(cat).append(getRightQuote()).append(".");
//		if (StringUtil.hasValue(sch)) sb.append(getLeftQuote()).append(sch).append(getRightQuote()).append(".");
//		if (StringUtil.hasValue(obj)) sb.append(getLeftQuote()).append(obj).append(getRightQuote());
//		
//		return sb.toString();
//	}
//	public String getCatalogNameOriginRawQuoted() { return StringUtil.isNullOrBlank(getRawOriginCatalogName()) ? "" : getLeftQuote() + getRawOriginCatalogName() + getRightQuote(); }
//	public String getSchemaNameOriginRawQuoted () { return StringUtil.isNullOrBlank(getRawOriginSchemaName())  ? "" : getLeftQuote() + getRawOriginSchemaName()  + getRightQuote(); }
//	public String getObjectNameOriginRawQuoted () { return StringUtil.isNullOrBlank(getRawOriginObjectName())  ? "" : getLeftQuote() + getRawOriginObjectName()  + getRightQuote(); }




	public String  _dbProductName                = null;
	public String  _dbIdentifierQuoteString      = null;
	public boolean _dbStoresUpperCaseIdentifiers = false;
	public boolean _dbStoresLowerCaseIdentifiers = false;
	public boolean _dbSupportsSchema             = true;

	private boolean _autoAddDboForSybaseAndSqlServer = true;
	
	/** 
	 * constructor using individual name catalog, schema, object 
	 */
	public SqlObjectName(Connection conn, final String cat, final String schema, final String table)
	{
		this(conn, toString(cat, schema, table));
	}
	/** 
	 * constructor using full name [catalog.][schema.][object] 
	 */
	public SqlObjectName(Connection conn, final String name)
	{
		String  dbProductName                = null;
		String  dbIdentifierQuoteString      = null;
		boolean dbStoresUpperCaseIdentifiers = true;
		boolean dbStoresLowerCaseIdentifiers = false;
		boolean dbSupportsSchema             = true;
		String  dbExtraNameCharacters        = null;
		
		if (conn instanceof DbxConnection)
		{
			DbxConnection dbxconn = (DbxConnection) conn; 
			try { dbProductName                = dbxconn.getDatabaseProductName();                   } catch (SQLException ex) { _logger.error("Problems executing MetaData: getDatabaseProductName()"); }
			      dbIdentifierQuoteString      = dbxconn.getDbQuotedIdentifierChar();
			try { dbStoresUpperCaseIdentifiers = dbxconn.getMetaData().storesUpperCaseIdentifiers(); } catch (SQLException ex) { _logger.error("Problems executing MetaData: storesUpperCaseIdentifiers()"); }
			try { dbStoresLowerCaseIdentifiers = dbxconn.getMetaData().storesLowerCaseIdentifiers(); } catch (SQLException ex) { _logger.error("Problems executing MetaData: storesLowerCaseIdentifiers()"); }
			try { dbSupportsSchema             = DbUtils.isSchemaSupported(dbxconn);                 } catch (SQLException ex) { _logger.error("Problems executing DbUtils.isSchemaSupported(conn)"); }
			      dbExtraNameCharacters        = dbxconn.getDbExtraNameCharacters();

//FIXME: implement some extra methods (that are cached) in DbxConnection
//	- conn.getMetaData().storesUpperCaseIdentifiers();
//	- dbmd.getSchemaTerm()
//	- dbmd.getMaxSchemaNameLength()
//	- possible: rename add methods that get the data from conn.getMetaData().*  to be named: dbmdGet...
		}
		else
		{
			try { dbProductName                = conn.getMetaData().getDatabaseProductName();     } catch (SQLException ex) { _logger.error("Problems executing MetaData: getDatabaseProductName()"); }
			try { dbIdentifierQuoteString      = conn.getMetaData().getIdentifierQuoteString();   } catch (SQLException ex) { _logger.error("Problems executing MetaData: getIdentifierQuoteString()"); }
			try { dbStoresUpperCaseIdentifiers = conn.getMetaData().storesUpperCaseIdentifiers(); } catch (SQLException ex) { _logger.error("Problems executing MetaData: storesUpperCaseIdentifiers()"); }
			try { dbStoresLowerCaseIdentifiers = conn.getMetaData().storesLowerCaseIdentifiers(); } catch (SQLException ex) { _logger.error("Problems executing MetaData: storesLowerCaseIdentifiers()"); }
			try { dbSupportsSchema             = DbUtils.isSchemaSupported(conn);                 } catch (SQLException ex) { _logger.error("Problems executing DbUtils.isSchemaSupported(conn)"); }
			try { dbExtraNameCharacters        = conn.getMetaData().getExtraNameCharacters();     } catch (SQLException ex) { _logger.error("Problems executing MetaData: getExtraNameCharacters()"); }
		}


	
		      
		_dbProductName                = dbProductName;
		_dbIdentifierQuoteString      = dbIdentifierQuoteString;
		_dbStoresUpperCaseIdentifiers = dbStoresUpperCaseIdentifiers;
		_dbStoresLowerCaseIdentifiers = dbStoresLowerCaseIdentifiers;
		_dbSupportsSchema             = dbSupportsSchema;
		_dbExtraNameCharacters        = dbExtraNameCharacters;
		
		_autoAddDboForSybaseAndSqlServer = true;
		
		setFullName(name);
	}

	/** 
	 * constructor using full name [catalog.][schema.][object] 
	 */
	public SqlObjectName(final String name, String dbProductName, String dbIdentifierQuoteString, boolean dbStoresUpperCaseIdentifiers, boolean dbStoresLowerCaseIdentifiers, boolean dbSupportsSchema)
	{
		_dbProductName                = dbProductName;
		_dbIdentifierQuoteString      = dbIdentifierQuoteString;
		_dbStoresUpperCaseIdentifiers = dbStoresUpperCaseIdentifiers;
		_dbStoresLowerCaseIdentifiers = dbStoresLowerCaseIdentifiers;
		_dbSupportsSchema             = dbSupportsSchema;
		
		_autoAddDboForSybaseAndSqlServer = true;
		
		setFullName(name);
	}

	/** 
	 * constructor using full name [catalog.][schema.][object] 
	 */
	public SqlObjectName(final String name, String dbProductName, String dbIdentifierQuoteString, boolean dbStoresUpperCaseIdentifiers, boolean dbStoresLowerCaseIdentifiers, boolean dbSupportsSchema, boolean autoAddDboForSybaseAndSqlServer)
	{
		_dbProductName                = dbProductName;
		_dbIdentifierQuoteString      = dbIdentifierQuoteString;
		_dbStoresUpperCaseIdentifiers = dbStoresUpperCaseIdentifiers;
		_dbStoresLowerCaseIdentifiers = dbStoresLowerCaseIdentifiers;
		_dbSupportsSchema             = dbSupportsSchema;
		
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
	public void setFullName(String name) 
	{ 
		// Don't need to continue if it's empty...
		if (StringUtil.isNullOrBlank(name))
			return;

//		_rawOriginFullName = name;
		_catNameUnModified  = "";
		_schNameUnModified  = "";
		_objNameUnModified  = name;
		
		int dot1 = name.indexOf('.');
		if (dot1 >= 0)
		{
			_schNameUnModified = name.substring(0, dot1);
			_objNameUnModified = name.substring(dot1+1);

			int dot2 = name.indexOf('.', dot1+1);
			if (dot2 >= 0)
			{
				_catNameUnModified = name.substring(0, dot1);
				_schNameUnModified = name.substring(dot1+1, dot2);
				_objNameUnModified = name.substring(dot2+1);
			}
		}
		
		// in some cases check schema/owner name
		if (_autoAddDboForSybaseAndSqlServer && (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(_dbProductName)))
		{
			// if empty schema/owner, add 'dbo'
			if (StringUtil.isNullOrBlank(_schNameUnModified))
				_schNameUnModified = "dbo";
		}

		// Special case for MySQL -- set schema as catalog name
		if (DbUtils.isProductName(_dbProductName, DbUtils.DB_PROD_NAME_MYSQL))
		{
			setCatalogName(_schNameUnModified);
			setSchemaName (null); // null or "" ???
			setObjectName (_objNameUnModified);
		}
		else
		{
			setCatalogName(_catNameUnModified);
			setSchemaName (_schNameUnModified);
			setObjectName (_objNameUnModified);
		}
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
		_catNameUnModified = name;
		_catNameOrigin    = stripQuote( name, _dbIdentifierQuoteString );
		_catName          = stripQuote( name, _dbIdentifierQuoteString );

		if (_catName != null && _dbStoresUpperCaseIdentifiers)
			_catName = _catName.toUpperCase();

		if (_catName != null && _dbStoresLowerCaseIdentifiers)
			_catName = _catName.toLowerCase();

		if (_catName != null && _catName.equalsIgnoreCase("null"))
			_catName = null;
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
	public void setSchemaName(String name) 
	{
		_schNameUnModified = name;
		_schNameOrigin    = stripQuote( name, _dbIdentifierQuoteString );
		_schName          = stripQuote( name, _dbIdentifierQuoteString );

		if (_schName != null && _dbStoresUpperCaseIdentifiers)
			_schName = _schName.toUpperCase();

		if (_schName != null && _dbStoresLowerCaseIdentifiers)
			_schName = _schName.toLowerCase();

		if (_schName != null && _schName.equalsIgnoreCase("null"))
			_schName = null;
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
	public void setObjectName(String name) 
	{
		_objNameUnModified = name;
		_objNameOrigin    = stripQuote( name, _dbIdentifierQuoteString );
		_objName          = stripQuote( name, _dbIdentifierQuoteString );

		if (_objName != null && _dbStoresUpperCaseIdentifiers)
			_objName = _objName.toUpperCase();

		if (_objName != null && _dbStoresLowerCaseIdentifiers)
			_objName = _objName.toLowerCase();

		if (_objName != null && _objName.equalsIgnoreCase("null"))
			_objName = null;
	}

//	/** make: schemaName -> catalaogName and objectName -> schemaName and blank-out objectName */
//	public void shiftLeft()
//	{
//		_catNameOrigin = _schNameOrigin;
//		_schNameOrigin = _objNameOrigin;
//		_objNameOrigin = "";
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

	/**
	 * Constructs a string with [catalogName.][schemaName.]objectName <br>
	 * @param cat         Catalog name
	 * @param sch         Schema name
	 * @param obj         Table or Object name
	 * @return never null
	 */
	public static String toString(String cat, String sch, String obj)
	{
		return toString(cat, sch, obj, "", "");
	}

	/**
	 * Constructs a string with ["catalogName".]["schemaName".]"objectName" <br>
	 * where " is the quote input string
	 * @param cat         Catalog name
	 * @param sch         Schema name
	 * @param obj         Table or Object name
	 * @param quote       What to use as a left and right quote (if you don't want this pass an empty string or null)
	 * @return never null
	 */
	public static String toString(String cat, String sch, String obj, String quote)
	{
		return toString(cat, sch, obj, quote, quote);
	}

	/**
	 * Constructs a string with ["catalogName".]["schemaName".]"objectName" <br>
	 * where " is the right and left quote
	 * @param cat         Catalog name
	 * @param sch         Schema name
	 * @param obj         Table or Object name
	 * @param leftQuote   What to use as a left  quote (if you don't want this pass an empty string or null)
	 * @param rightQuote  What to use as a right quote (if you don't want this pass an empty string or null)
	 * @return never null
	 */
	public static String toString(String cat, String sch, String obj, String leftQuote, String rightQuote)
	{
		if (leftQuote  == null) leftQuote  = "";
		if (rightQuote == null) rightQuote = "";
		
		StringBuilder sb = new StringBuilder();
		
		// Check some extra stuff
		// in some cases the obj name contains 'XXX.YYY.tabname', so check if 'cat' and 'sch' is part of 'obj', then just reset cat and obj
		if (StringUtil.hasValue(obj))
		{
			if (StringUtil.hasValue(cat) && obj.startsWith(cat+"."))
				cat = "";
			if (StringUtil.hasValue(sch) && obj.indexOf(sch+".") != -1)
				sch = "";
		}

		if (StringUtil.hasValue(cat)) sb.append(leftQuote).append(cat).append(rightQuote).append(".");
		if (StringUtil.hasValue(sch)) sb.append(leftQuote).append(sch).append(rightQuote).append(".");
		if (StringUtil.hasValue(obj)) sb.append(leftQuote).append(obj).append(rightQuote);

		return sb.toString();
	}
	@Override
	public int hashCode()
	{
		final int prime  = 31;
		int       result = 1;
		result = prime * result + ((_catName == null) ? 0 : _catName.hashCode());
		result = prime * result + ((_objName == null) ? 0 : _objName.hashCode());
		result = prime * result + ((_schName == null) ? 0 : _schName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		SqlObjectName other = (SqlObjectName) obj;
		if ( _catName == null )
		{
			if ( other._catName != null )
				return false;
		}
		else if ( !_catName.equals(other._catName) )
			return false;
		if ( _objName == null )
		{
			if ( other._objName != null )
				return false;
		}
		else if ( !_objName.equals(other._objName) )
			return false;
		if ( _schName == null )
		{
			if ( other._schName != null )
				return false;
		}
		else if ( !_schName.equals(other._schName) )
			return false;
		return true;
	}
}
