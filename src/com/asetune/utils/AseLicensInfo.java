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
package com.asetune.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

public class AseLicensInfo
{
	private static Logger _logger = Logger.getLogger(AseLicensInfo.class);

//	1> select * from master.dbo.monLicense
//	RS> Col# Label         JDBC Type Name           Guessed DBMS type Source Table         
//	RS> ---- ------------- ------------------------ ----------------- ---------------------
//	RS> 1    InstanceID    java.sql.Types.TINYINT   tinyint           master.dbo.monLicense
//	RS> 2    Quantity      java.sql.Types.INTEGER   int               master.dbo.monLicense
//	RS> 3    Name          java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
//	RS> 4    Edition       java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
//	RS> 5    Type          java.sql.Types.VARCHAR   varchar(64)       master.dbo.monLicense
//	RS> 6    Version       java.sql.Types.VARCHAR   varchar(16)       master.dbo.monLicense
//	RS> 7    Status        java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
//	RS> 8    LicenseExpiry java.sql.Types.TIMESTAMP datetime          master.dbo.monLicense
//	RS> 9    GraceExpiry   java.sql.Types.TIMESTAMP datetime          master.dbo.monLicense
//	RS> 10   LicenseID     java.sql.Types.VARCHAR   varchar(150)      master.dbo.monLicense
//	RS> 11   Filter        java.sql.Types.VARCHAR   varchar(14)       master.dbo.monLicense
//	RS> 12   Attributes    java.sql.Types.VARCHAR   varchar(64)       master.dbo.monLicense
//	+----------+--------+--------+-----------------+----------------------------+---------+------+-------------+---------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+------------------------------------------------+
//	|InstanceID|Quantity|Name    |Edition          |Type                        |Version  |Status|LicenseExpiry|GraceExpiry          |LicenseID                                                                                                                                            |Filter     |Attributes                                      |
//	+----------+--------+--------+-----------------+----------------------------+---------+------+-------------+---------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+------------------------------------------------+
//	|0         |1       |ASE_CORE|(NULL)           |(NULL)                      |(NULL)   |graced|(NULL)       |2019-12-27 00:36:00.0|(NULL)                                                                                                                                               |PE=EE;LT=DT|(NULL)                                          |
//	|0         |2       |ASE_CORE|Developer Edition|Development and test license|2013.1231|OK    |(NULL)       |(NULL)               |0A1F EC50 7138 C903 8D79 F4B9 499B C658 6275 ABE7 32CF F29D A8A2 B9D7 63AF 07DD 38A9 A181 789D 57E6 459D 56D2 B305 0052 B73E 0E82 79CF C471 B49B AF0E|PE=DE      |CO=Sybase, Inc.;V=15.0;AS=A;ME=1;MC=25;MP=0;CP=0|
//	+----------+--------+--------+-----------------+----------------------------+---------+------+-------------+---------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+------------------------------------------------+
	
//	private String    _status;
//	private Timestamp _graceExpiry;
//	private boolean   _hasGracePeriodExpired;
//	private String    _name;
//	private String    _edition;
//	private String    _type;

	private String    _srvName;
	private boolean   _inGracePeriod;
	private boolean   _licensSoonExpire;

    private int       _instanceID   ;   // InstanceID    java.sql.Types.TINYINT   tinyint           master.dbo.monLicense
    private int       _quantity     ;   // Quantity      java.sql.Types.INTEGER   int               master.dbo.monLicense
    private String    _name         ;   // Name          java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
    private String    _edition      ;   // Edition       java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
    private String    _type         ;   // Type          java.sql.Types.VARCHAR   varchar(64)       master.dbo.monLicense
    private String    _version      ;   // Version       java.sql.Types.VARCHAR   varchar(16)       master.dbo.monLicense
    private String    _status       ;   // Status        java.sql.Types.VARCHAR   varchar(30)       master.dbo.monLicense
    private Timestamp _licenseExpiry;   // LicenseExpiry java.sql.Types.TIMESTAMP datetime          master.dbo.monLicense
    private Timestamp _graceExpiry  ;   // GraceExpiry   java.sql.Types.TIMESTAMP datetime          master.dbo.monLicense
    private String    _licenseID    ;   // LicenseID     java.sql.Types.VARCHAR   varchar(150)      master.dbo.monLicense
    private String    _filter       ;   // Filter        java.sql.Types.VARCHAR   varchar(14)       master.dbo.monLicense
    private String    _attributes   ;   // Attributes    java.sql.Types.VARCHAR   varchar(64)       master.dbo.monLicense
	
	public String    getSrvName              () { return _srvName              ; }
	public boolean   isInGracePeriod         () { return _inGracePeriod        ; }
	public boolean   isLicensSoonExpire      () { return _licensSoonExpire     ; }
	public boolean   isInGraceOrSoonExpire   () { return _inGracePeriod || _licensSoonExpire; }

	public int       getInstanceID           () { return _instanceID           ; }
	public int       getQuantity             () { return _quantity             ; }
	public String    getName                 () { return _name                 ; }
	public String    getEdition              () { return _edition              ; }
	public String    getType                 () { return _type                 ; }
	public String    getVersion              () { return _version              ; }
	public String    getStatus               () { return _status               ; }
	public Timestamp getLicenseExpiry        () { return _licenseExpiry        ; }
	public Timestamp getGraceExpiry          () { return _graceExpiry          ; }
	public String    getLicenseID            () { return _licenseID            ; }
	public String    getFilter               () { return _filter               ; }
	public String    getAttributes           () { return _attributes           ; }

	
	/**
	 * Get info from <code>master.dbo.monLicense</code>
	 * 
	 * @param conn
	 * @param onlyInGraceOrSoonExpire     only return entries that are in Grace Period or License will soon expire (90 days)
	 * @return null on errors otherwise all (see onlyInGraceOrSoonExpire) entries in monLicense
	 */
	public static List<AseLicensInfo> getLicensEntries(DbxConnection conn, boolean onlyInGraceOrSoonExpire)
	{
		if (conn == null)
			return null;

		// master.dbo.monLicense does only exists if ASE is above 15.0
		if (conn.getDbmsVersionNumber() < Ver.ver(15,0))
			return Collections.emptyList();

		String sql = "" +
				"select \n" +
				"     srvName=@@servername \n" +
				"    ,InstanceID \n" +
				"    ,Quantity \n" +
				"    ,Name \n" +
				"    ,Edition \n" +
				"    ,Type \n" +
				"    ,Version \n" +
				"    ,Status \n" +
				"    ,LicenseExpiry \n" +
				"    ,GraceExpiry \n" +
				"    ,LicenseID \n" +
				"    ,Filter \n" +
				"    ,Attributes \n" +
				"from master.dbo.monLicense \n" +
				"";
		try 
		{
			List<AseLicensInfo> list = new ArrayList<>();
			
			// do dummy select, which will return 0 rows
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			int col = 1;
			while (rs.next()) 
			{
				AseLicensInfo li = new AseLicensInfo();
				
				li._srvName       = rs.getString   (col++);
				li._instanceID    = rs.getInt      (col++);
				li._quantity      = rs.getInt      (col++);
				li._name          = rs.getString   (col++);
				li._edition       = rs.getString   (col++);
				li._type          = rs.getString   (col++);
				li._version       = rs.getString   (col++);
				li._status        = rs.getString   (col++);
				li._licenseExpiry = rs.getTimestamp(col++);
				li._graceExpiry   = rs.getTimestamp(col++);
				li._licenseID     = rs.getString   (col++);
				li._filter        = rs.getString   (col++);
				li._attributes    = rs.getString   (col++);

				// GRACE PERIOD
				li._inGracePeriod = "graced".equalsIgnoreCase(li._status);

				// LICENS EXPIRE
				if (li._licenseExpiry != null)
				{
					long timeToExpireInMs = li._licenseExpiry.getTime() - System.currentTimeMillis();
					
					li._licensSoonExpire = TimeUnit.MILLISECONDS.toDays(timeToExpireInMs) > 90;
				}

				if (onlyInGraceOrSoonExpire)
				{
					if (li._inGracePeriod || li._licensSoonExpire)
						list.add(li);
				}
				else
				{
					list.add(li);
				}
			}
			rs.close();
			stmt.close();
			
			return list;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when checking grace period. SQL issued '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
			return null;
		}
	}

	/**
	 * Check if the ASE is is grace period
	 * @param conn 
	 * @return null if OK, otherwise a String with the warning message
	 */
	public static String getAseGracePeriodWarning(DbxConnection conn)
	{
		if (conn == null)
			return null;

		// master.dbo.monLicense does only exists if ASE is above 15.0
		if (conn.getDbmsVersionNumber() < Ver.ver(15,0))
			return null;


		String sql = "select Status, GraceExpiry, Name, Edition, Type, srvName=@@servername from master.dbo.monLicense";

		try 
		{
			String warningStr = null;
			
			// do dummy select, which will return 0 rows
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) 
			{
				String    licStatus      = rs.getString   (1);
				Timestamp licGraceExpiry = rs.getTimestamp(2);
				String    licName        = rs.getString   (3);
				String    licEdition     = rs.getString   (4);
				String    licType        = rs.getString   (5);
				String    aseSrvName     = rs.getString   (6);

				if ("graced".equalsIgnoreCase(licStatus))
				{
					// add newline if we have several rows
					warningStr = warningStr == null ? "" : warningStr + "\n";

					warningStr += "Server '"+aseSrvName+"' is in grace period and will stop working at '"+licGraceExpiry+"'. (licName='"+licName+"', licEdition='"+licEdition+"', licType='"+licType+"').";
				}
			}
			rs.close();
			stmt.close();
			
			return warningStr;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when checking grace period. SQL issued '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
			return "Problems when checking grace period. ("+StringUtil.stripNewLine(ex.getMessage()+").");
		}
	}
}
