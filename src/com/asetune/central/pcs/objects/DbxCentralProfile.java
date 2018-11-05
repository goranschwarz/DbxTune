package com.asetune.central.pcs.objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

//1> select * from DbxCentralGraphProfiles
//RS> Col# Label               JDBC Type Name         Guessed DBMS type Source Table                                     
//RS> ---- -------------       ---------------------- ----------------- -------------------------------------------------
//RS> 1    ProductString       java.sql.Types.VARCHAR VARCHAR(30)       DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//RS> 2    UserName            java.sql.Types.VARCHAR VARCHAR(30)       DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//RS> 3    ProfileName         java.sql.Types.VARCHAR VARCHAR(30)       DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//RS> 4    ProfileDescription  java.sql.Types.CLOB    CLOB              DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//RS> 5    ProfileValue        java.sql.Types.CLOB    CLOB              DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//RS> 6    ProfileUrlOptions   java.sql.Types.VARCHAR VARCHAR(1024)     DBXTUNE_CENTRAL_DB.PUBLIC.DbxCentralGraphProfiles
//+-------------+--------+-----------+------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------+
//|ProductString|UserName|ProfileName|ProfileDescription|ProfileValue                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |ProfileUrlOptions|
//+-------------+--------+-----------+------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------+
//|AseTune      |        |           |                  |[{"graph":"CmSummary_aaCpuGraph"},{"graph":"CmSummary_OldestTranInSecGraph"},{"graph":"CmSummary_SumLockCountGraph"},{"graph":"CmSummary_BlockingLocksGraph"},{"graph":"CmProcessActivity_BatchCountGraph"},{"graph":"CmProcessActivity_ExecTimeGraph"},{"graph":"CmSpidWait_spidClassName"},{"graph":"CmSpidWait_spidWaitName"},{"graph":"CmExecutionTime_TimeGraph"},{"graph":"CmSysLoad_EngineRunQLengthGraph"},{"graph":"CmStatementCache_RequestPerSecGraph"},{"graph":"CmOsIostat_IoWait"},{"graph":"CmOsUptime_AdjLoadAverage"}]|                 |
//+-------------+--------+-----------+------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------+
//Rows 1
//(1 rows affected)
//Client Exec Time: 00:00.005 (sqlExec=00:00.000, readResults=00:00.001, other=00:00.004), at '2018-02-18 14:44:31.060', for SQL starting at Line 2876

@JsonPropertyOrder(value = {"productString", "userName", "profileName", "profileDescription", "profileValue"}, alphabetic = true)
public class DbxCentralProfile
{
	private String _productString;
	private String _userName     ;
	private String _profileName  ;
	private String _profileDescription;
	private String _profileValue ;
	private String _profileUrlOptions;

	public String getProductString              () { return _productString     ;  }
	public String getUserName                   () { return _userName          ;  }
	public String getProfileName                () { return _profileName       ;  }
	public String getProfileDescription         () { return _profileDescription;  }
	@JsonRawValue public String getProfileValue () { return _profileValue      ;  }
	public String getProfileUrlOptions          () { return _profileUrlOptions;  }

	public void setProductString     (String productString     ) { _productString      = productString     ; }
	public void setUserName          (String userName          ) { _userName           = userName          ; }
	public void setProfileName       (String profileName       ) { _profileName        = profileName       ; }
	public void setProfileDescription(String profileDescription) { _profileDescription = profileDescription; }
	public void setProfileValue      (String profileValue      ) { _profileValue       = profileValue      ; }
	public void setProfileUrlOptions (String profileUrlOptions ) { _profileUrlOptions  = profileUrlOptions ; }

	public DbxCentralProfile(String productString, String userName, String profileName, String profileDescription, String profileValue, String profileUrlOptions)
	{
		super();

		_productString      = productString     ;
		_userName           = userName          ;
		_profileName        = profileName       ;
		_profileDescription = profileDescription;
		_profileValue       = profileValue      ;
		_profileUrlOptions  = profileUrlOptions ;
	}
}