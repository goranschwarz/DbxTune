package com.asetune.sql.conn.dtr;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import com.asetune.utils.DbUtils;

public class JdbcDataTypeResolver
{

	public String relsolve(String sourceDbms, String destDbms, ResultSetMetaData rsmd, int column)
	throws SQLException
	{
		String retDatatype = "";

		int srcJdbcType   = rsmd.getColumnType(column);
		int srcPrec       = rsmd.getPrecision (column);  // decimal(p,s), numeric(p,s)
		int srcScale      = rsmd.getScale     (column);
		int srcIsNullable = rsmd.isNullable   (column);

		//------------------------------------------------------------------
		// Transform any SOURCE RDBMS into some other JDBC Type
		//------------------------------------------------------------------
		// some RDBMS especially Oracle handles INTEGER in a strange way... the JDBC Types isn't Types.INTEGER, 
		// Instead it's Types.NUMERIC with some strange precision & scale
		if (DbUtils.isProductName(sourceDbms, DbUtils.DB_PROD_NAME_ORACLE))
		{
			switch (srcJdbcType)
			{
			case Types.NUMERIC:
				// An int (up to 2^31 = 2.1B) can hold any NUMBER(10, 0) value (up to 10^9 = 1B). 
				// NUMBER(38, 0) is conventionally used in Oracle for integers of unspecified precision, 
				// so let's be bold and assume that they can fit into an int.
				//
				// Oracle also seems to sometimes represent integers as (type=NUMERIC, precision=0, scale=-127) for reasons unknown.
				//
//				if ( (srcPrec == 0 && srcScale == -127) || (srcPrec == 38 && srcScale == 0) ) 
				if ((srcScale == 0 || srcScale == -127) && (srcPrec <= 9 || srcPrec == 38)) 
					srcJdbcType = Types.INTEGER;
				break;

			default:
				break;
			}
		}

		//------------------------------------------------------------------
		// At the end get a RDBMS storage data type for the destination 
		//------------------------------------------------------------------
		switch (srcJdbcType)
		{
		case Types.INTEGER: return ""; // get the specific destination RDBMS data type using the DbxConnection implementers...
		}
		return "";
	}

//	public final static int BIT 		=  -7;
//	public final static int TINYINT 	=  -6;
//	public final static int SMALLINT	=   5;
//	public final static int INTEGER 	=   4;
//	public final static int BIGINT 		=  -5;
//	public final static int FLOAT 		=   6;
//	public final static int REAL 		=   7;
//	public final static int DOUBLE 		=   8;
//	public final static int NUMERIC 	=   2;
//	public final static int DECIMAL		=   3;
//	public final static int CHAR		=   1;
//	public final static int VARCHAR 	=  12;
//	public final static int LONGVARCHAR 	=  -1;
//	public final static int DATE 		=  91;
//	public final static int TIME 		=  92;
//	public final static int TIMESTAMP 	=  93;
//	public final static int BINARY		=  -2;
//	public final static int VARBINARY 	=  -3;
//	public final static int LONGVARBINARY 	=  -4;
//	public final static int NULL		=   0;
//	public final static int OTHER		= 1111;
//
//        
//    /* since 1.2 */
//    public final static int JAVA_OBJECT         = 2000;
//    public final static int DISTINCT            = 2001;
//    public final static int STRUCT              = 2002;
//    public final static int ARRAY               = 2003;
//    public final static int BLOB                = 2004;
//    public final static int CLOB                = 2005;
//    public final static int REF                 = 2006;
//        
//    /* since 1.4 */
//    public final static int DATALINK = 70;
//    public final static int BOOLEAN = 16;
//    
//    //------------------------- JDBC 4.0 -----------------------------------
//    
//    /* since 1.6 */
//    public final static int ROWID = -8;
//    public static final int NCHAR = -15;
//    public static final int NVARCHAR = -9;
//    public static final int LONGNVARCHAR = -16;
//    public static final int NCLOB = 2011;
//    public static final int SQLXML = 2009;

}
