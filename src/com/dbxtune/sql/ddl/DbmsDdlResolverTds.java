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
package com.dbxtune.sql.ddl;

import java.sql.Types;

import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;

public abstract class DbmsDdlResolverTds 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverTds(DbxConnection conn)
	{
		super(conn);
	}

	@Override
	public boolean supportsIfNotExists()
	{
		return false;
	}

//	/**
//	 * For Sybase ASE and SQL-Server escaping square brackets inside a column name or an identifier is done using<br>
//	 * <code>[Bring-The-[AAAA]]-Game]</code> is equal to <code>Bring-The-[AAAA]-Game</code> <br>
//	 * so all ']' should be escaped as ']]'... but starting brackets '[' should NOT be touched
//	 * <p>
//	 * The passed value of name is WITHOUT the surrounding brackets, for the above example the input value is <code>Bring-The-[AAAA]-Game</code>
//	 * <p>
//	 * <b>Example:</b> <code>escapeQuotedIdentifier("Bring-The-[AAAA]-Game")</code><br>
//	 * <b>Returns:</b> <code>"Bring-The-[AAAA]]-Game"</code> 
//	 */
//	@Override
//	public String escapeQuotedIdentifier(String name)
//	{
//		if (name.indexOf(']') != -1)
//			name = name.replace("]", "]]");
//
//		return name;
//	}

	@Override
	public boolean skipCreateSchemaWithName(String schemaName)
	{
		if (schemaName == null)
			return true;

		// for Sybase ASE and Microsoft SQL-Server
		if (schemaName.equalsIgnoreCase("dbo")) return true;

		// Everything else... create the schema
		return false;
	}


	
	

	/*
	 * Below is a couple of SELECT statements with data type conventions
	 * to extract what the ResultSetMetaData says about them for Sybase ASE 16.0
	 * ---------------------------------------------------------------------------------------------------

        --https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc32300.1600/doc/html/san1390612189874.html
        --https://help.sap.com/docs/SAP_ASE/b65d6a040c4a4709afd93068071b2a76/aa351f76bc2b1014ab5ac21dae4e7dcd.html?q=system%20suplied%20datatypes
        select unicharsize = @@unicharsize, ncharsize = @@ncharsize
        --------- strings: char, varchar, nchar, nvarchar, unichar, univarchar
        select 
        	 char_10        = cast('x' as char(10))
        	,char_255       = cast('x' as char(255))
        	,char_256       = cast('x' as char(256))
        	,char_4k        = cast('x' as char(4096))
        
        	,varchar_10     = cast('x' as varchar(10))
        	,varchar_255    = cast('x' as varchar(255))
        	,varchar_256    = cast('x' as varchar(256))
        	,varchar_4k     = cast('x' as varchar(4096))
        
        	,nchar_10       = cast('x' as nchar(10))
        	,nchar_85       = cast('x' as nchar(85))
        	,nchar_86       = cast('x' as nchar(86))
        	,nchar_255      = cast('x' as nchar(255))
        	,nchar_256      = cast('x' as nchar(256))
        	,nchar_4k       = cast('x' as nchar(4096))
        
        	,nvarchar_10    = cast('x' as nvarchar(10))
        	,nvarchar_85    = cast('x' as nvarchar(85))
        	,nvarchar_86    = cast('x' as nvarchar(86))
        	,nvarchar_255   = cast('x' as nvarchar(255))
        	,nvarchar_256   = cast('x' as nvarchar(256))
        	,nvarchar_4k    = cast('x' as nvarchar(4096))
        
        	,unichar_10     = cast('x' as unichar(10))
        	,unichar_255    = cast('x' as unichar(255))
        	,unichar_256    = cast('x' as unichar(256))
        	,unichar_4k     = cast('x' as unichar(4096))
        
        	,univarchar_10  = cast('x' as univarchar(10))
        	,univarchar_255 = cast('x' as univarchar(255))
        	,univarchar_256 = cast('x' as univarchar(256))
        	,univarchar_4k  = cast('x' as univarchar(4096))
        
        	,text_c         = cast('x' as text)
        	,unitext_c      = cast('x' as unitext)
        
        RS> Col# Label          Column Type Name Column Display Size Precision Scale JDBC Type Name            
        RS> ---- -------------- ---------------- ------------------- --------- ----- --------------------------
        RS> 1    char_10        char                              10         0     0 java.sql.Types.CHAR            OK
        RS> 2    char_255       char                             255         0     0 java.sql.Types.CHAR            OK
        RS> 3    char_256       varchar                          256         0     0 java.sql.Types.LONGVARCHAR     --> CHAR
        RS> 4    char_4k        varchar                         4096         0     0 java.sql.Types.LONGVARCHAR     --> CHAR
        RS> 5    varchar_10     varchar                           10         0     0 java.sql.Types.VARCHAR         OK
        RS> 6    varchar_255    varchar                          255         0     0 java.sql.Types.VARCHAR         OK
        RS> 7    varchar_256    varchar                          256         0     0 java.sql.Types.LONGVARCHAR     --> VARCHAR
        RS> 8    varchar_4k     varchar                         4096         0     0 java.sql.Types.LONGVARCHAR     --> VARCHAR
        RS> 9    nchar_10       char                              30         0     0 java.sql.Types.NCHAR           OK (div by 3)
        RS> 10   nchar_85       char                             255         0     0 java.sql.Types.NCHAR            OK (div by 3)
        RS> 11   nchar_86       char                             258         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NCHAR
        RS> 12   nchar_255      varchar                          765         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NCHAR
        RS> 13   nchar_256      varchar                          768         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NCHAR
        RS> 14   nchar_4k       varchar                        12288         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NCHAR
        RS> 15   nvarchar_10    varchar                           30         0     0 java.sql.Types.NVARCHAR        OK (div by 3)
        RS> 16   nvarchar_85    varchar                          255         0     0 java.sql.Types.NVARCHAR        OK (div by 3)
        RS> 17   nvarchar_86    varchar                          258         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NVARCHAR
        RS> 18   nvarchar_255   varchar                          765         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NVARCHAR
        RS> 19   nvarchar_256   varchar                          768         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NVARCHAR
        RS> 20   nvarchar_4k    varchar                        12288         0     0 java.sql.Types.LONGVARCHAR     --> ??? ... should be NVARCHAR
        RS> 21   unichar_10     unichar                           10         0     0 java.sql.Types.CHAR            --> NCHAR
        RS> 22   unichar_255    unichar                          255         0     0 java.sql.Types.CHAR            --> NCHAR
        RS> 23   unichar_256    unichar                          256         0     0 java.sql.Types.CHAR            --> NCHAR
        RS> 24   unichar_4k     unichar                         4096         0     0 java.sql.Types.CHAR            --> NCHAR
        RS> 25   univarchar_10  univarchar                        10         0     0 java.sql.Types.VARCHAR         --> NVARCHAR
        RS> 26   univarchar_255 univarchar                       255         0     0 java.sql.Types.VARCHAR         --> NVARCHAR
        RS> 27   univarchar_256 univarchar                       256         0     0 java.sql.Types.VARCHAR         --> NVARCHAR
        RS> 28   univarchar_4k  univarchar                      4096         0     0 java.sql.Types.VARCHAR         --> NVARCHAR
        RS> 29   text_c         text                      2147483647         0     0 java.sql.Types.LONGVARCHAR     --> CLOB
        RS> 30   unitext_c      unitext                   1073741823         0     0 java.sql.Types.LONGVARCHAR     --> NCLOB
        
        
        
        --------- binary: binary, varbinary, image
        select 
        	 bit_c         = cast(1 as bit)
        
        	,binary_10     = cast(1 as binary(10))
        	,binary_255    = cast(1 as binary(255))
        	,binary_256    = cast(1 as binary(256))
        	,binary_4k     = cast(1 as binary(4096))
        
        	,varbinary_10  = cast(1 as varbinary(10))
        	,varbinary_255 = cast(1 as varbinary(255))
        	,varbinary_256 = cast(1 as varbinary(256))
        	,varbinary_4k  = cast(1 as varbinary(4096))
        
        	,image_c       = cast('xxx' as image)
        
        RS> Col# Label         Column Type Name Column Display Size Precision Scale JDBC Type Name                 
        RS> ---- ------------- ---------------- ------------------- --------- ----- ----------------------------   
        RS> 1    bit_c         bit                                1         0     0 java.sql.Types.BIT             OK
        RS> 2    binary_10     binary                            20         0     0 java.sql.Types.BINARY          OK (Precision = displayLen/2)
        RS> 3    binary_255    binary                           510         0     0 java.sql.Types.BINARY          OK (Precision = displayLen/2)
        RS> 4    binary_256    varbinary                        512         0     0 java.sql.Types.LONGVARBINARY   --> BINARY (Precision = displayLen/2)
        RS> 5    binary_4k     varbinary                       8192         0     0 java.sql.Types.LONGVARBINARY   --> BINARY (Precision = displayLen/2)
        RS> 6    varbinary_10  varbinary                         20         0     0 java.sql.Types.VARBINARY       OK (Precision = displayLen/2)
        RS> 7    varbinary_255 varbinary                        510         0     0 java.sql.Types.VARBINARY       OK (Precision = displayLen/2)
        RS> 8    varbinary_256 varbinary                        512         0     0 java.sql.Types.LONGVARBINARY   --> VARBINARY (Precision = displayLen/2)
        RS> 9    varbinary_4k  varbinary                       8192         0     0 java.sql.Types.LONGVARBINARY   --> VARBINARY (Precision = displayLen/2)
        RS> 10   image_c       image                              1         0     0 java.sql.Types.LONGVARBINARY   --> BLOB
        
        
        
        --------- exact numbers
        select 
        	 tinyint_c    = cast(1 as tinyint)
        	,smallint_c   = cast(1 as smallint)
        	,u_smallint_c = cast(1 as unsigned smallint)
        	,int_c        = cast(1 as int)
        	,u_int_c      = cast(1 as unsigned int)
        	,bigint_c     = cast(1 as bigint)
        	,u_bigint_c   = cast(1 as unsigned bigint)
        
        RS> Col# Label         Column Type Name   Column Display Size Precision Scale JDBC Type Name               
        RS> ---- ------------- ------------------ ------------------- --------- ----- ---------------------------- 
        RS> 1    tinyint_c     tinyint                              3         3     0 java.sql.Types.TINYINT         OK
        RS> 2    smallint_c    smallint                             6         5     0 java.sql.Types.SMALLINT        OK
        RS> 3    u_smallint_c  unsigned smallint                    6         5     0 java.sql.Types.SMALLINT        --> possibly to INTEGER
        RS> 4    int_c         int                                 11        10     0 java.sql.Types.INTEGER         OK
        RS> 5    u_int_c       unsigned int                        11        10     0 java.sql.Types.INTEGER         --> possibly to BIGINT
        RS> 6    bigint_c      bigint                              20        19     0 java.sql.Types.BIGINT          OK
        RS> 7    u_bigint_c    unsigned bigint                     20        20     0 java.sql.Types.BIGINT          --> possibly to NUMERIC with prec=20, scale=0
        
        
        
        --------- exact numeric and decimal
        select 
        	 numeric_10_0    = cast(1.12345 as numeric(10))
        	,numeric_10_2    = cast(1.12345 as numeric(10,2))
        	,numeric_38_2    = cast(1.12345 as numeric(38,2))
        
        	,decimal_10_0    = cast(1.12345 as decimal(10))
        	,decimal_10_2    = cast(1.12345 as decimal(10,2))
        	,decimal_38_2    = cast(1.12345 as decimal(38,2))
        
        RS> Col# Label        Column Type Name   Column Display Size Precision Scale JDBC Type Name               
        RS> ---- ------------ ------------------ ------------------- --------- ----- ---------------------------- 
        RS> 1    numeric_10_0 numeric                             12        10     0 java.sql.Types.NUMERIC        OK
        RS> 2    numeric_10_2 numeric                             12        10     2 java.sql.Types.NUMERIC        OK
        RS> 3    numeric_38_2 numeric                             40        38     2 java.sql.Types.NUMERIC        OK
        RS> 4    decimal_10_0 decimal                             12        10     0 java.sql.Types.DECIMAL        OK
        RS> 5    decimal_10_2 decimal                             12        10     2 java.sql.Types.DECIMAL        OK
        RS> 6    decimal_38_2 decimal                             40        38     2 java.sql.Types.DECIMAL        OK
        
        
        
        --------- Approximate numeric
        select 
        	 float_c    = cast(1.12345 as float)
        	,double_c   = cast(1.12345 as double precision)
        	,real_c     = cast(1.12345 as real)
        
        RS> Col# Label        Column Type Name   Column Display Size Precision Scale JDBC Type Name               
        RS> ---- ------------ ------------------ ------------------- --------- ----- ---------------------------- 
        RS> 1    float_c      double precision                    85        15     0 java.sql.Types.DOUBLE         OK
        RS> 2    double_c     double precision                    85        15     0 java.sql.Types.DOUBLE         OK
        RS> 3    real_c       real                                46         7     0 java.sql.Types.REAL           OK
        
        
        
        --------- Money
        select 
        	 smallmoney_c = cast(1.12345 as smallmoney)
        	,money_c      = cast(1.12345 as money)
        
        RS> Col# Label        Column Type Name   Column Display Size Precision Scale JDBC Type Name               
        RS> ---- ------------ ------------------ ------------------- --------- ----- ---------------------------- 
        RS> 1    smallmoney_c smallmoney                          12        10     4 java.sql.Types.DECIMAL        OK
        RS> 2    money_c      money                               21        19     4 java.sql.Types.DECIMAL        OK
        
        
        
        --------- Date/time
        select 
        	 smalldatetime_c = cast('2022-11-26 21:21:21.123456' as smalldatetime)
        	,datetime_c      = cast('2022-11-26 21:21:21.123456' as datetime)
        	,date_c          = cast('2022-11-26 21:21:21.123456' as date)
        	,time_c          = cast('2022-11-26 21:21:21.123456' as time)
        	,bigdatetime_c   = cast('2022-11-26 21:21:21.123456' as bigdatetime)
        	,bigtime_c       = cast('2022-11-26 21:21:21.123456' as bigtime)
        
        RS> Col# Label           Column Type Name   Column Display Size Precision Scale JDBC Type Name               
        RS> ---- --------------- ------------------ ------------------- --------- ----- ---------------------------- 
        RS> 1    smalldatetime_c smalldatetime                       25         0     0 java.sql.Types.TIMESTAMP      OK  ?is Precision & Scale OK?
        RS> 2    datetime_c      datetime                            25         3     0 java.sql.Types.TIMESTAMP      OK  ?is Precision & Scale OK?
        RS> 3    date_c          date                                10         0     0 java.sql.Types.DATE           OK  ?is Precision & Scale OK?
        RS> 4    time_c          time                                 8         3     0 java.sql.Types.TIME           OK  ?is Precision & Scale OK?
        RS> 5    bigdatetime_c   bigdatetime                         29         6     6 java.sql.Types.TIMESTAMP      OK  ?is Precision & Scale OK?
        RS> 6    bigtime_c       bigtime                             15         6     6 java.sql.Types.TIME           OK  ?is Precision & Scale OK?
        
	 * ---------------------------------------------------------------------------------------------------
	 */
	
	
	
	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		// This various from 1 and 3 depending on the ASE Charset, it's detected via @@ncharsize
		int ncharsize = 1;

		// The below dosn't seem to work, since getConnection() mosltly is NULL
		// So 'ncharsize' will be hard coded to 1, which will only affect NCHAR/NVARCHAR columns if the ASE CharSet is utf8
		// The result will be that NCHAR and NVARCHAR will be 3 times larger if the ASE CharSet is utf8... lets solve this in the future
		//if (getConnection() != null && getConnection() instanceof AseConnection)
		//{
		//	ncharsize = ((AseConnection)getConnection()).getNcharSize();
		//}

		//-----------------------------------------------------------------------------
		// Adjustments for: Character
		// Note: char(256) and above will be handled as "varchar", I can't figure out how to detect  
		// note: nchar/nvarchar seems to be a bit hard to detect (see the above select ... to see all the variants)
		//-----------------------------------------------------------------------------
		if ("char".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			if (Types.NCHAR == entry.getColumnType())
			{
				entry.setColumnTypeName("nchar");
				entry.setPrecision( entry.getColumnDisplaySize() / ncharsize );
			}
			else
			{
				entry.setPrecision( entry.getColumnDisplaySize() );
			}
		}

		if ("varchar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			if (Types.NVARCHAR == entry.getColumnType())
			{
				entry.setColumnTypeName("nvarchar");
				entry.setPrecision( entry.getColumnDisplaySize() / ncharsize );
			}
			else
			{
				entry.setPrecision( entry.getColumnDisplaySize() );
				entry.setColumnType(Types.VARCHAR); 
					// note: char(256) and above goes as varchar... but the jdbcType is LONGVARCHAR, which is reset to VARCHAR
					// note: nchar(86) and above goes as varchar... but the jdbcType is LONGVARCHAR, which is reset to VARCHAR
					//       nchar(86) and nvarchar(86) and above will become 3 times larger (because I can't detect that it's a NCHAR)
			}
		}

		if ("unichar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NCHAR); // Note: "unichar" has CHAR, which should be NCHAR
			entry.setPrecision( entry.getColumnDisplaySize() );
		}

		if ("univarchar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NVARCHAR); // Note: "univarchar" has VARCHAR, which should be NVARCHAR
			entry.setPrecision( entry.getColumnDisplaySize() );
		}

		if ("text".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.CLOB); // Note: "text" has LONGVARCHAR, which should be CLOB
		}

		if ("unitext".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NCLOB); // Note: "unitext" has LONGVARCHAR, which should be NCLOB
		}

		
		//-----------------------------------------------------------------------------
		// Adjustments for: binary, varbinary & image
		// Note: binary(256) and above will be handled as "varbinary", I can't figure out how to detect  
		//-----------------------------------------------------------------------------
		if ("binary".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setPrecision( entry.getColumnDisplaySize() / 2 );
		}
		if ("varbinary".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setPrecision( entry.getColumnDisplaySize() / 2 );
			entry.setColumnType(Types.VARBINARY); // Note: binary(256) and above will have LONGVARBINARY, so setting this to VARBINARY
		}
		if ("image".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.BLOB); // Note: "image" has LONGVARBINARY, which should be BLOB
		}

		//-----------------------------------------------------------------------------
		// Adjustments for: Exact numeric, integers
		//-----------------------------------------------------------------------------
		if ("unsigned smallint".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.SMALLINT && !entry.isSigned()) )
		{
			entry.setColumnType(Types.INTEGER);
			entry.setColumnTypeName("int");
			entry.setSigned(true);
		}

		if ("unsigned int".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.INTEGER && !entry.isSigned()) )
		{
			entry.setColumnType(Types.BIGINT);
			entry.setColumnTypeName("bigint");
			entry.setSigned(true);
		}

		if ("unsigned bigint".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.BIGINT && !entry.isSigned()) )
		{
		 	// If we want to "push" this "up" into a numeric instead
			boolean convert__unsigned_bigint__to__numeric = false;
			if (convert__unsigned_bigint__to__numeric)
			{
				entry.setColumnType(Types.NUMERIC);
				entry.setColumnTypeName("numeric");
				entry.setSigned(true);
				entry.setPrecision(20);
				entry.setScale(0);
			}
		}
	}

//	@Override
//	public void dbmsVendorDataTypeResolverForSource(Entry entry) // This is a OLD version...
//	{
//	
////////////////////////////////////////////////////////////////////////////////////////////
//MOST of the below does NOT seems to be correct;
//ErrorMessage from monErrorLog seems to be wrong. should be 512, but is 170
//RS> 10   ErrorMessage java.sql.Types.LONGVARCHAR varchar           master.dbo.monErrorLog
//                                     ^^^^^^^^^^^;
//, z2=cast('yyyyy' as varchar(255)) /// RS> 15   z2           java.sql.Types.VARCHAR     varchar(255)      -none-                
//, z3=cast('yyyyy' as varchar(256)) /// RS> 16   z3           java.sql.Types.LONGVARCHAR varchar           -none-                
//Maybe this has changed since I switched jConnect to a later version
//Re-test is needed
////////////////////////////////////////////////////////////////////////////////////////////
// Also: Sybase ASE -- Top Table
////////////////////////////////////////////////////////////////////////////////////////////
//
//		// Should we FIX the size of precision if it's ZERO for some data types ???
//		if (entry.getPrecision() == 0)
//		{
//			// Some vendors 'Sybase', for instance has 'precision=0', while the length is in 'columnDisplaySize' ... for CHAR/VARCHAR
//			// Sybase fucks it up even greater
//			// -- CHAR/VARCHAR       is OK in Types... but Precision is ZERO and "length" is really in columnDisplaySize
//			// -- UNICHAR/UNIVARCHAR is returned as Types.LONGVARCHAR, with a 'columnDisplaySize'*3    so nchar(99)     is 297
//			// -- NCHAR/NVARCHAR     is returned as Types.LONGVARCHAR, with a 'columnDisplaySize'*3    so nchar(99)     is 297
//			// -- BINARY/VARBINARY   is OK in Types... but 'columnDisplaySize'*2                       so varbinary(99) is 198
//
//			// Below is a SQL that will show the STRING data types
//			// This seems to be a **MESS** which is "not-resolvable"
//			
//			switch (entry.getColumnType())
//			{
//
//			// Set Precision using ColumnDisplaySize
//			case Types.CHAR:
//			case Types.VARCHAR:
////			case Types.LONGVARCHAR:
//				entry.setPrecision( entry.getColumnDisplaySize() );
//				break;
//
//			// Set Precision using ColumnDisplaySize / 3   (display size seems to be 3 for every "character")
//			case Types.NCHAR:
//			case Types.NVARCHAR:
////			case Types.LONGNVARCHAR:
//				entry.setPrecision( entry.getColumnDisplaySize() / 3 );
//				break;
//
//			// Set Precision using ColumnDisplaySize / 2  (display size is obviously 2 for every BYTE value)
//			case Types.BINARY:
//			case Types.VARBINARY:
////			case Types.LONGVARBINARY:
//				entry.setPrecision( entry.getColumnDisplaySize() / 2 );
//				break;
//			}
//		}
//
//		// Sybase UNICHAR is really like a nchar  
//		if ("unichar".equalsIgnoreCase(entry.getColumnTypeName()))
//		{
//			entry.setColumnType(Types.NCHAR);
//			entry.setColumnTypeName("nchar");
//		}
//
//		// Sybase UNICHAR is really like a nchar  
//		if ("univarchar".equalsIgnoreCase(entry.getColumnTypeName()))
//		{
//			entry.setColumnType(Types.NVARCHAR);
//			entry.setColumnTypeName("nvarchar");
//		}
//
//		// Sybase NCHAR is MetaData: char and Types.LONGVARCHAR
//		if (entry.getColumnType() == Types.LONGVARCHAR && "char".equalsIgnoreCase(entry.getColumnTypeName()))
//		{
//			entry.setColumnType(Types.NCHAR);
//			entry.setColumnTypeName("nchar");
//			entry.setPrecision( entry.getColumnDisplaySize() / 3 );
//		}
//
//		// Sybase NVARCHAR is MetaData: varchar and Types.LONGVARCHAR
//		if (entry.getColumnType() == Types.LONGVARCHAR && "varchar".equalsIgnoreCase(entry.getColumnTypeName()))
//		{
//			entry.setColumnType(Types.NVARCHAR);
//			entry.setColumnTypeName("nvarchar");
//			entry.setPrecision( entry.getColumnDisplaySize() / 3 );
//		}
//
//		if ("unsigned smallint".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.SMALLINT && !entry.isSigned()) )
//		{
//			entry.setColumnType(Types.INTEGER);
//			entry.setColumnTypeName("int");
//			entry.setSigned(true);
//		}
//		
//		if ("unsigned int".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.INTEGER && !entry.isSigned()) )
//		{
//			entry.setColumnType(Types.BIGINT);
//			entry.setColumnTypeName("bigint");
//			entry.setSigned(true);
//		}
//	}


	/**
	 * Resolve JDBC Types -->> Sybase: ASE, ASA, RAX, RepServer, DA, DRA 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "bit";
		case java.sql.Types.TINYINT:                 return "tinyint";
		case java.sql.Types.SMALLINT:                return "smallint";
		case java.sql.Types.INTEGER:                 return "int";
		case java.sql.Types.BIGINT:                  return "bigint";
		case java.sql.Types.FLOAT:                   return "float";
		case java.sql.Types.REAL:                    return "real";
		case java.sql.Types.DOUBLE:                  return "double precision";
		case java.sql.Types.NUMERIC:                 return "numeric("+length+","+scale+")";
		case java.sql.Types.DECIMAL:                 return "decimal("+length+","+scale+")";
		case java.sql.Types.CHAR:                    return "char("+length+")";
		case java.sql.Types.VARCHAR:                 return "varchar("+length+")";
		case java.sql.Types.LONGVARCHAR:             return "text";
		case java.sql.Types.DATE:                    return "date";
		case java.sql.Types.TIME:                    return "time";
		case java.sql.Types.TIMESTAMP:               return "datetime";
		case java.sql.Types.BINARY:                  return "binary("+length+")";
		case java.sql.Types.VARBINARY:               return "varbinary("+length+")";
		case java.sql.Types.LONGVARBINARY:           return "image";
		case java.sql.Types.NULL:                    return "image";                      // Not really supported just use 'image'
		case java.sql.Types.OTHER:                   return "image";                      // Not really supported just use 'image'
		case java.sql.Types.JAVA_OBJECT:             return "image";                      // Not really supported just use 'image'
		case java.sql.Types.DISTINCT:                return "image";                      // Not really supported just use 'image'
		case java.sql.Types.STRUCT:                  return "image";                      // Not really supported just use 'image'
		case java.sql.Types.ARRAY:                   return "image";                      // Not really supported just use 'image'
		case java.sql.Types.BLOB:                    return "image";
		case java.sql.Types.CLOB:                    return "text";
		case java.sql.Types.REF:                     return "image";                      // Not really supported just use 'image'
		case java.sql.Types.DATALINK:                return "image";                      // Not really supported just use 'image'
		case java.sql.Types.BOOLEAN:                 return "bit";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "varchar(20)";                 // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "unichar("+length+")";
		case java.sql.Types.NVARCHAR:                return "univarchar("+length+")";
		case java.sql.Types.LONGNVARCHAR:            return "unitext";
		case java.sql.Types.NCLOB:                   return "unitext";
		case java.sql.Types.SQLXML:                  return "unitext";                     // NOTE: if Charset UTF-8 is used then 'text' otherwise use 'unitext'

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "varchar(40)";                 // Not really supported, should we use varchar(40) instead ??? -->>             hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 40 chars 
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "varchar(50)";                 // Not really supported, should we use varchar(50) instead ??? -->>  YYYY-MM-DD hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 50 chars

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "varchar(36)";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "text";

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPNS
		case -101:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPTZ
		case -102:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPLTZ
		case -103:                                   return null;    // oracle.jdbc.OracleTypes.INTERVALYM
		case -104:                                   return null;    // oracle.jdbc.OracleTypes.INTERVALDS
		case  -10:                                   return null;    // oracle.jdbc.OracleTypes.CURSOR
		case  -13:                                   return null;    // oracle.jdbc.OracleTypes.BFILE
		case 2007:                                   return null;    // oracle.jdbc.OracleTypes.OPAQUE
		case 2008:                                   return null;    // oracle.jdbc.OracleTypes.JAVA_STRUCT
		case  -14:                                   return null;    // oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE
		case  100:                                   return null;    // oracle.jdbc.OracleTypes.BINARY_FLOAT
		case  101:                                   return null;    // oracle.jdbc.OracleTypes.BINARY_DOUBLE
//		case    2:                                   return null;    // oracle.jdbc.OracleTypes.NUMBER             // same as: java.sql.Types.NUMERIC
//		case   -2:                                   return null;    // oracle.jdbc.OracleTypes.RAW                // same as: java.sql.Types.BINARY
		case  999:                                   return null;    // oracle.jdbc.OracleTypes.FIXED_CHAR

		default:
			return null;
		}
	}
}
