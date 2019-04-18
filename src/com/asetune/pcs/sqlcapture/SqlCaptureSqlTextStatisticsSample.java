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
package com.asetune.pcs.sqlcapture;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.h2.tools.SimpleResultSet;

import com.asetune.utils.StringUtil;

/**
 * Holds SQL Text Statistics 
 */
public class SqlCaptureSqlTextStatisticsSample
{
	private Map<String, StatCounter> _statMap = new HashMap<>();
	
	public static class StatCounter
	{
		public StatCounter(String name, String operation)
		{
			this.name              = name;
			this.operation         = operation;
			this.count             = 0;
		}
		
		public String name;
		public String operation;
		public long   count;
	}
	
	public SqlCaptureSqlTextStatisticsSample()
	{
		synchronized (this)
		{
			_statMap = new LinkedHashMap<>();
		}
	}
	
	/** Small helper to get key for... This to not spread this in the code... meaning: easier to change the key */
	private String createKey(String dynamicName, String operation)
	{
		return dynamicName + "-" + operation;
	}
	public void addSqlTextInternal(String dynamicName, String operation)
	{
//System.out.println("addSqlTextInternal(): dynamicName='"+dynamicName+"', op='"+operation+"'.");

		String key = createKey(dynamicName, operation);

		// Increment counter for the TYPE
		StatCounter st = _statMap.get(key);
		if (st == null)
		{
			st = new StatCounter(dynamicName, operation);
			_statMap.put(key, st);
		}
		st.count ++;
	}


	private static final String CREATE_PROC_TXT = "create proc "; 
	private static final int    CREATE_PROC_LEN = CREATE_PROC_TXT.length();
	
	private static final String DYNAMIC_SQL_TXT = "DYNAMIC_SQL "; 
	private static final int    DYNAMIC_SQL_LEN = DYNAMIC_SQL_TXT.length();

	public static final String OPERATION_CR    = "CR"; 
	public static final String OPERATION_CR_OP = "CR-OP"; 
	public static final String OPERATION_X     = "X"; 
	
	public void addSqlTextStats(int sPID, int kPID, int batchID, int sequenceInBatch, String sqlText)
	{
		if (StringUtil.isNullOrBlank(sqlText))
			return;
		
		if (sqlText.startsWith(CREATE_PROC_TXT))
		{
			// "create proc dyn*" -- jConnect Dynamic prepare 
			// "create proc OPL*" -- ODBC (or at least PML 4D ODBC calls)
			// so lets use the first 3 chars to "discover" the source of the DYNAMIC SQL

			String dynamicName = sqlText.substring(CREATE_PROC_LEN, CREATE_PROC_LEN+3);
			
			// Skip if the name looks like "sp_" it could be a "installmaster" or similar
			// I don't know how to filter out creation of "normal" stored procedures, except that it's small words in "create proc "
			// TODO: maybe filter out procs that do NOT contain any number (and hope that most Dynamic SQL creations has a number in them)
			if (dynamicName.equalsIgnoreCase("sp_"))
				return;
			
			// Increment counter for the TYPE
			addSqlTextInternal(dynamicName, OPERATION_CR);
			
			
			// Some apps seems to append there operation_id... should we take that into account???
			//   create proc dyn15100 as /* CdocDAO.exist */ Select id from cdoc.CounselingSessionStats where id = ?
			//   create proc dyn15103 as /* CdocDAO.insertIntoSessionStats */ insert into cdoc.CounselingSessionStats (id, visitType, status, sessionType, behalfOf, counselingDate, brokerSSN, customerSSN, livingAbroad, pep, counselingCompany, savingsSection, suspectedLaun
			//   create proc dyn45292 as /* MaxFonderUserDao.findMaxfonderUserByUid */ select mu.ClientID, mu.ClientNo, mu.FirstName, mu.LastName, mu.CompanyName, mu.PhoneCellular, mu.Email from dbo.MFEXUser mu where mu.ClientNo = ?
			
			int asPos = sqlText.indexOf(" as ");
			if (asPos != -1)
			{
				String startOfProcText = sqlText.substring(asPos+4, Math.min(100, sqlText.length())).trim();
				if (startOfProcText.startsWith("/*"))
				{
					// Grab /* xxxxxx */ the things inside the comments, which could be an OPERATION
					int endPos = startOfProcText.indexOf("*/");
					if (endPos != -1)
					{
						String insideComments = startOfProcText.substring(2, endPos).trim();
						if (insideComments.length() < 70)
							addSqlTextInternal(dynamicName + " - /* " + insideComments + " */", OPERATION_CR_OP);
					}
				}
				else
				{
//System.out.println(">>>>>>>>>>>>>>>: startOfProcText=|"+startOfProcText+"|.");
					// Grab first word
//					int endPos = startOfProcText.indexOf(" ");
//					if (endPos != -1)
//					{
//						String firstWord = startOfProcText.substring(0, endPos).trim();
////System.out.println("XXXXXXXXXXXXXX: firstWord=|"+firstWord+"|, startOfProcText=|"+startOfProcText+"|.");
//						if (firstWord.length() < 20)
//							addSqlTextInternal(dynamicName + " - " + firstWord, OPERATION_CR_OP);
//					}

					// Remove newlines
					startOfProcText = startOfProcText.replace('\n', ' ');
					startOfProcText = startOfProcText.replace('\r', ' ');

					// Take only first 60 chars (if truncated: add "..." at the end)
					int maxLen = 60;
					int len = Math.min(maxLen, startOfProcText.length());
					String firstPart = startOfProcText.substring(0, len).trim();
					if (len >= maxLen)
						firstPart = firstPart + "...";

					addSqlTextInternal(dynamicName + " - " + firstPart, OPERATION_CR_OP);
				}
			}
		}
		else if (sqlText.startsWith(DYNAMIC_SQL_TXT))
		{
			// The below are parameter_send/executions/close ???
			// DYNAMIC_SQL jtds000001: 
			// DYNAMIC_SQL OPLzQREz$rE1Um9oqR6PIYHaA0113: 
			// DYNAMIC_SQL dyn1163: 

			String type = sqlText.substring(DYNAMIC_SQL_LEN, DYNAMIC_SQL_LEN+3);
			
			// Increment counter for the TYPE
			addSqlTextInternal(type, OPERATION_X);
		}
	}
	
	
	public ResultSet toResultSet()
	{
		SimpleResultSet rs = new SimpleResultSet();

		rs.addColumn("name",                 Types.VARCHAR, 100, 0);
		rs.addColumn("operation",            Types.VARCHAR,  10, 0);
		rs.addColumn("xOverCr",              Types.NUMERIC,  10, 2);
		rs.addColumn("totalCount",           Types.BIGINT,    0, 0);
		rs.addColumn("diffCount",            Types.BIGINT,    0, 0);

		for (StatCounter sc : _statMap.values())
		{
			BigDecimal xOverCr = null;
			if (OPERATION_X.equals(sc.operation))
			{
				StatCounter crEntry = _statMap.get(createKey(sc.name, OPERATION_CR));
				if (crEntry != null)
				{
					if (crEntry.count > 0)
					{
						double calcVal = (sc.count * 1.0) / (crEntry.count * 1.0);
						xOverCr = new BigDecimal(calcVal).setScale(2, BigDecimal.ROUND_HALF_EVEN);
					}
				}
			}

			rs.addRow(
				sc.name, 
				sc.operation, 
				xOverCr,
				sc.count,
				sc.count
			);
		}

		return rs;
	}
}
