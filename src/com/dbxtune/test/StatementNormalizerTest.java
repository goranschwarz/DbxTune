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
package com.dbxtune.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.sql.norm.NormalizerCompiler;
import com.dbxtune.sql.norm.StatementFixerManager;
import com.dbxtune.sql.norm.StatementNormalizer;
import com.dbxtune.sql.norm.UserDefinedNormalizerManager;

public class StatementNormalizerTest
{

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

//		StatementNormalizer stmntNorm = new StatementNormalizer();

		List<String> list = new ArrayList<>();
		
//		list.add("insert into tempdb..sek_mxg_p01#342118#Z0007_TMP (_bulk_, _with_, arrayinsert) values(1)");
		list.add("SELECT COUNT(1) AS [count] FROM SEK_DATAMART_prod.MUREXDB.RTA_ALTERNATE_ID_REP WHERE CONVERT (VARCHAR (10), ISNULL (M_MX_REF_JOB, 0)) + '.' + CONVERT (VARCHAR (10), ISNULL (M_REF_DATA,0)) = '1247901.692314'");
		list.add("set nocount on select meid from MxGJobStatus  where pid=770594 and soj>=convert(datetime,\"             Nov  4 2020 12:01AM \",112)  and eoj is null");
		list.add("set nocount on select meid from MxGJobStatus where pid=7897 and soj>=convert(datetime,\" Nov 4 2020 12:11AM \",112) and eoj is null");
		list.add("set nocount on declare @smeid varchar(50) select @smeid = meid from MxGJobStatus where pid=7417 and eoj is null if @smeid is null select @smeid = '0' insert into MxGJobStatus (meid, pid, smeid, systemDate, soj, scriptname, scriptpath, scriptargs, jobname ) values (\"20201105_001126_7897\",7897,@smeid,convert(datetime,\"20201105\",112),getdate(),\"SEK_EOD_MLC_WAIT_DWH_TICKS.sh\",\"/eod/mlc\",\"-r MX_DEFAULT\", \"default\")");
		list.add("commit transaction BATCH");
		list.add("insert bulk tempdb..sek_mxg_p01#342118#Z0007_TMP with arrayinsert");
		list.add("insert bulk MxGdB_prod..ETA_8DN_FLW_MX3_DBF with arrayinsert, nodescribe");
		list.add("delete top 1000 from tableName");
		list.add("select t1.* from t1, t2 where t1.id *= t2.id");
		list.add("select t1.* \nfrom t1, t2 \nwhere t1.id *= t2.id \nand t2.id is not null \norder by t1.name");
//		list.add("begin transaction xxx");
//		list.add("THIS SHOULD FAIL");
		list.add(""
				+ "-- Lets decide what Database Copmpession to used based on the ASE Version we are currently using. \n"
				+ "declare @compression varchar(10) \n"
				+ "set @compression = '2' \n"
				+ "if (@@version_number >= 15500) -- compression 100, 101 was intruduced in ASE 15.5 \n"
				+ "        set @compression = '100' \n"
				+ "print 'Using database dump compression: %1!', @compression \n"
				+ " \n"
				+ "execute(' \n"
				+ "    dump database MLCdB_prod to \"/sybdev/mx3/sybdump/db/DBDUMP1.13880\" \n"
				+ "    stripe on \"/sybdev/mx3/sybdump/db/DBDUMP2.13880\" \n"
				+ "    stripe on \"/sybdev/mx3/sybdump/db/DBDUMP3.13880\" \n"
				+ "    stripe on \"/sybdev/mx3/sybdump/db/DBDUMP4.13880\" \n"
				+ "    with compression = ' + @compression \n"
				+ ") \n");

		list.add(""
				+ " \n"
				+ "/* \n"
				+ "client.sek.cpy_rating.router.mainRouter \n"
				+ " \n"
				+ "0v01 pebr@sek.se Aug 26 2017 Initial version \n"
				+ " \n"
				+ "*/ \n"
				+ " \n"
				+ "declare @label varchar(47),  \n"
				+ "        @date datetime, \n"
				+ "        @issuer varchar(15), \n"
				+ "        @rating varchar(20), \n"
				+ "        @labelExists numeric, \n"
				+ "        @ratingExists numeric, \n"
				+ "        @recordExists numeric, \n"
				+ "        @partyExists numeric, \n"
				+ "        @issuerSet numeric \n"
				+ " \n"
				+ "set		@label = 'Moodys'  --'Internal' \n"
				+ "set     @date = '20201105'  --'20170521' \n"
				+ "set     @issuer = '102579'  -- 'STENA'  \n"
				+ "set     @rating = 'Ba3'  -- 'BB-' \n"
				+ " \n"
				+ "set @labelExists = (select count(*) from RT_RTNG_DBF where M_RTNG_A = @label) \n"
				+ " \n"
				+ "set @ratingExists = (select count(*) from RT_RTNG_DBF where M_RTNG_A = @label and M_RTNG = @rating) \n"
				+ " \n"
				+ "set @partyExists =  \n"
				+ "(select count(*) \n"
				+ "from TRN_CPDF_DBF c \n"
				+ "where c.M_LABEL = @issuer) \n"
				+ " \n"
				+ "set @issuerSet =  \n"
				+ "(select count(*) \n"
				+ "from TRN_CPDF_DBF c, CTP_TYPES_DBF t, PARTY_TYPE_DBF p \n"
				+ "where c.M_ID = t.M_CTN \n"
				+ "and t.M_REF = p.M_REFERENCE \n"
				+ "and c.M_LABEL = @issuer \n"
				+ "and p.M_LABEL in ('ISSUER')) \n"
				+ " \n"
				+ "set @recordExists =  \n"
				+ "(select count(*) \n"
				+ "from MPX_RATG_DBF x, MPY_RATG_DBF y \n"
				+ "inner join RT_RTNG_DBF z on y.M_RATING=z.M_REFERENCE \n"
				+ "where 1=1 \n"
				+ "and x.M__INDEX_ = y. M__INDEX_ \n"
				+ "and x.M_LABEL  = @label \n"
				+ "and x.M__DATE_ = @date  \n"
				+ "and y.M_ISSUER = @issuer \n"
				+ "and z.M_RTNG = @rating) \n"
				+ " \n"
				+ "--select @labelExists as labelExists, @recordExists as recordExists, @partyExists as partyExists, @issuerSet as issuerSet, @ratingExists as ratingExists, \n"
				+ " \n"
				+ "select case \n"
				+ "  when @labelExists = 0 then 'noLabel' \n"
				+ "  when @partyExists = 0 then 'noCounterparty' \n"
				+ "  when @issuerSet = 0 then 'issuerNotSet' \n"
				+ "  when @ratingExists = 0 then 'noRating' \n"
				+ "  when @recordExists = 0 then 'insertion' \n"
				+ "  else 'modification' \n"
				+ "end as status \n"
				+ " \n");
		
		
		
		list.add("/* DYNAMIC-SQL: */ DELETE top 1000 FROM PS_CASH_NC_SNAP_E WHERE PS_CASH_NC_SNAP_E.LOGICAL_KEY_ID = 283");
		list.add("/* DYNAMIC-SQL: */ UPDATE INDEX STATISTICS LIULOG_5980_DBF");
		list.add("commit transaction ADJUSTMENTS");
		list.add("delete top 200000 from MxGdB_prod..ETA_8DN_FLW_MX3_DBF  where (M_REF_DATA in (806652,806653,806654,806655,806656,806657,806658,806659))");
		list.add("dump transaction MxGdB_prod to \"/sybdev/mx3/sybdump/tran/TRANDUMP.9523\"");
		list.add("DYNAMIC_SQL dyn146:");
		list.add("FETCH_CURSOR     jconnect_implicit_120:");
		list.add("insert bulk SEK_DATAMART_prod..IB_PLVAR_HIST_REP with arrayinsert, nodescribe");
		list.add("OPEN_CURSOR      jconnect_implicit_375:");
		list.add("select T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE,T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE \n"
				+ "from MPX_VOLMAT_DBF T1 ,MPY_VOLMAT_DBF T2 \n"
				+ "where T1.M_REFERENCE  *= T2.M_REFERENCE \n"
				+ "  and T1.M__DATE_  *= T2.M__DATE_ \n"
				+ "  and T1.M__ALIAS_  *= T2.M__ALIAS_ \n"
				+ "  and ((((T1.M__DATE_='20161020') and (T1.M__ALIAS_='BO')) and ((T1.M_SETNAME = 'BSKM1' and T1.M_US_TYPE = 15) and T1.M_TYPE = 0)) and ((T2.M__DATE_='20161020') and (T2.M__ALIAS_='BO'))) order by T1.M_SETNAME asc ,T1.M_US_TYPE asc ,T1.M_TYPE asc \n");
		list.add("set nocount on \n"
				+ "declare @smeid  varchar(50) \n"
				+ "select @smeid = meid from MxGJobStatus where pid=104789 and eoj is null \n"
				+ " \n"
				+ "if @smeid is null \n"
				+ "  select @smeid = '0'  \n"
				+ " \n"
				+ "insert into MxGJobStatus \n"
				+ "(meid, pid, smeid, systemDate, soj, scriptname, scriptpath, scriptargs, jobname ) \n"
				+ "values \n"
				+ "(\"20201105_004816_105277\",105277,@smeid,convert(datetime,\"20201105\",112),getdate(),\"SEK_EOD_MLC_WAIT_DWH_RC_TICKS.sh\",\"/eod/mlc\",\"-r MX_DEFAULT\", \"default\") \n");
		list.add("SET NOCOUNT ON\nSELECT jobname from MxGJobStatus where meid=\"20201104_230651_516213\"");
		list.add("set nocount on\nselect meid from MxGJobStatus\nwhere pid=140190\nand soj>=convert(datetime,\"             Nov  4 2020  7:11AM \",112)\nand eoj is null");
		list.add("set nocount on\nupdate MxGJobStatus\nset\neoj=getdate()\nwhere meid=\"20201105_031619_467240\"");
		list.add("set nocount on\nupdate MxGJobStatus\nset\njobname='mdrs' , status='DONE' , ret_code=0\nwhere meid=\"20201105_120014_696568\"");
		list.add("set nocount on\nupdate MxGJobStatus\nset\nstatus='DONE'\nwhere meid=\"20201104_235517_745714\"");
		
		NormalizerCompiler          .getInstance();
		UserDefinedNormalizerManager.getInstance();
		StatementFixerManager       .getInstance();

		StatementNormalizer.NormalizeParameters normalizeParameters = null;

		for (String sqlText : list)
		{
			if (normalizeParameters == null)
				normalizeParameters = new StatementNormalizer.NormalizeParameters();

			List<String> tableList = new ArrayList<>();
			String normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, normalizeParameters, tableList);

			int addStatus = normalizeParameters.addStatus.getIntValue();
			System.out.println("normalized: addStatus=[" + addStatus + "]" + normalizeParameters.addStatus + ", TableList=" + tableList + ", SqlText=|" + normalizedSqlText + "|.");
		}
	}
}

//		for (String sqlText : list)
//		{
//			System.out.println("");
//			System.out.println("================================================================");
//			System.out.println(">> SQL: " + sqlText);
//			System.out.println("----------------------------------------------------------------");
//			String normalizedSqlText = null;
//			try
//			{
//				// Parse and normalize
//				normalizedSqlText = stmntNorm.normalizeStatement(sqlText);
//			}
//			catch(JSQLParserException ex)
//			{
//				System.out.println("   ## INITIAL Parser error: " + ex.getCause());
//				// go into "FIX/Workaround" the parse problem
//				// - Rewrite the SQL Statement, and comment out "known" sections where we know that the parser wont work.
//				// - Then parse the Statement again
//				// - set the "AddStatus" to XXX to indicate that this was made.
//				
//				List<String> rewriteComments = new ArrayList<>();
//				boolean isRewritten = false;
//
//				for (IStatementFixer fixer : StatementFixerManager.getInstance().getFixerEntries())
//				{
//					if (fixer.isRewritable(sqlText))
//					{
//						sqlText = fixer.rewrite(sqlText);
//						rewriteComments.add(fixer.getComment());
//						isRewritten = true;
//
//						System.out.println("  ++ SQL REWITE TO: " + sqlText);
//					}
//					else
//					{
//						System.out.println("<< SKIPPING REWRITE: name='" + fixer.getName() + "', sqlText=|" + sqlText +"|");
//					}
//				}
//				if (isRewritten)
//				{
//					System.out.println("  ++ FINAL +++++ SQL REWITE TO: " + sqlText);
//
//					try
//					{
//						// Parse and normalize
//						normalizedSqlText = stmntNorm.normalizeStatement(sqlText);
//							
//						if (StringUtil.hasValue(normalizedSqlText))
//						{
//							System.out.println("  >> SECOND LEVEL NORMALIZED VALUE: |" + normalizedSqlText + "|");
//						}
//					}
//					catch(JSQLParserException ex2)
//					{
//						System.out.println("  !! SECOND LEVEL PARSER EXCEPTION: " + ex);
//						ex.printStackTrace();
//					}
//				}
//				if (StringUtil.hasValue(normalizedSqlText))
//				{
//					if ( ! rewriteComments.isEmpty() )
//					{
//						normalizedSqlText = "/***DBXTUNE-REWRITE: " + rewriteComments + "***/ " + normalizedSqlText;
//					}
//				}
//			}
//
//			System.out.println("NORMALIZED VALUE: |" + normalizedSqlText + "|");
//		}
//	}
//}
