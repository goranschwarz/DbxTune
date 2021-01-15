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
package com.asetune.sql.norm;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class UserDefinedNormalizerManager
{
	private static Logger _logger = Logger.getLogger(UserDefinedNormalizerManager.class);

	private Set<IUserDefinedNormalizer> _userDefinedNormalizeEntries = new LinkedHashSet<>();

	//----------------------------------------------------------------
	// BEGIN: instance
	private static UserDefinedNormalizerManager _instance = null;
	public static UserDefinedNormalizerManager getInstance()
	{
		if (_instance == null)
		{
			UserDefinedNormalizerManager instance = new UserDefinedNormalizerManager();
			setInstance(instance);
		}
		return _instance;
	}
	public static void setInstance(UserDefinedNormalizerManager instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	// END: instance
	//----------------------------------------------------------------

	//----------------------------------------------------------------
	// BEGIN: Constructors
	public UserDefinedNormalizerManager()
	{
		init();
	}
	// END: Constructors
	//----------------------------------------------------------------

	private void init()
	{
		//------------------------------------------------------------
		// DUMP TRAN|DATABASE ...
		// dump transaction MxGdB_prod to "/sybdev/mx3/sybdump/tran/TRANDUMP.5416"
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "dump-tran-or-db"; }
			@Override public boolean isHandled(String sql) { return StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "dump "); }
			@Override public String  getComment()          { return "Removed: everything after 'to'"; }
			@Override public String  normalize(String sql) { return getPrefix() + StringUtil.substringBeforeIgnoreCase(sql, " to "); }
		});
		
		//------------------------------------------------------------
		// DUMP DATABASE ... but embedded in a longer SQL text/batch (keep only: dump database {dbname} to)
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "dump-db-embedded"; }
			@Override public boolean isHandled(String sql) { return StringUtils.indexOfIgnoreCase(sql, "dump database ") != -1; }
			@Override public String  getComment()          { return "Removed: everything before and after 'dump database {dbname} to'"; }
			@Override public String  normalize(String sql) 
			{
				int startPos = StringUtils.indexOfIgnoreCase(sql, "dump database ");
				if (startPos >= 0)
				{
					startPos += "dump database ".length();
					int endPos = StringUtils.indexOfIgnoreCase(sql, " to ", startPos);
					if (startPos >= 0 && endPos > startPos)
					{
						String dbname = sql.substring(startPos, endPos);
						return getPrefix() + "... dump database " + dbname + " to ...";
					}
				}
				return sql;
			}
		});

		//------------------------------------------------------------
		// writetext bulk SEK_DATAMART_prod..BATCH_REP.M_ITEMS 0x09a209000000000009090000987de0d9 timestamp = 0x00000909d9e07d98 with log
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "writetext"; }
			@Override public boolean isHandled(String sql) { return StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "writetext "); }
			@Override public String  getComment()          { return "Removed: everything after 'table name'"; }
			@Override public String  normalize(String sql) { return getPrefix() + StringUtil.substringBeforeIgnoreCase(sql, " 0x"); }
		});

		//------------------------------------------------------------
		// insert bulk SEK_DATAMART_prod..ETA_ALTERNATE_ID_REP with arrayinsert, nodescribe 
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "insert-bulk"; }
			@Override public boolean isHandled(String sql) { return StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "insert bulk "); }
			@Override public String  getComment()          { return "Noop: return origin SQL, it's unique enough"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql; }
		});

		//------------------------------------------------------------
		// /* DYNAMIC-SQL: */ UPDATE INDEX STATISTICS LIULOG_5980_DBF
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "update-index-statistics"; }
			@Override public boolean isHandled(String sql) { return StringUtils.indexOfIgnoreCase(sql, "UPDATE INDEX STATISTICS") != -1; }
			@Override public String  getComment()          { return "Noop: return origin SQL, it's unique enough"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql; }
		});
		
		//------------------------------------------------------------
		// commit transaction ADJUSTMENTS
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "commit-transaction"; }
			@Override public boolean isHandled(String sql) { return StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "commit tran"); }
			@Override public String  getComment()          { return "Noop: return origin SQL, it's unique enough"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql; }
		});

		//------------------------------------------------------------
		// DYNAMIC_SQL dyn146:
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "dynamic-sql"; }
			@Override public boolean isHandled(String sql) { return sql.startsWith("DYNAMIC_SQL "); }
			@Override public String  getComment()          { return "Removed: numbers"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql.replaceAll("[0-9]", ""); }
		});

		//------------------------------------------------------------
		// FETCH_CURSOR     jconnect_implicit_120:
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer()
		{
			@Override public String  getName()             { return "fetch-cursor"; }
			@Override public boolean isHandled(String sql) { return sql.startsWith("FETCH_CURSOR "); }
			@Override public String  getComment()          { return "Removed: numbers"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql.replaceAll("[0-9]", ""); }
		});

		//------------------------------------------------------------
		// OPEN_CURSOR      jconnect_implicit_375:
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer() 
		{
			@Override public String  getName()             { return "open-cursor"; }
			@Override public boolean isHandled(String sql) { return sql.startsWith("OPEN_CURSOR "); }
			@Override public String  getComment()          { return "Removed: numbers"; }
			@Override public String  normalize(String sql) { return getPrefix() + sql.replaceAll("[0-9]", ""); }
		});

		//------------------------------------------------------------
		// Queue Service Execution -- This is used at some customers to schedule/execute 'update statistics & reorg rebuild' highly parallel
		// ... exec tempdb.dbo.qs_UpdStatQueueGet @cmd=@cmd OUT, @dbname=@dbname OUT, @qid=@qid OUT ...
		//------------------------------------------------------------
		add( new IUserDefinedNormalizer() 
		{
			@Override public String  getName()             { return "queue-service-exec"; }
			@Override public boolean isHandled(String sql) { return sql.indexOf("exec tempdb.dbo.qs_UpdStatQueueGet @cmd=@cmd OUT, @dbname=@dbname OUT, @qid=@qid OUT") != -1; }
			@Override public String  getComment()          { return "Removed: boiler plate code for QueueService"; }
			@Override public String  normalize(String sql) { return getPrefix() + " -- Queue Service Execution - 'update statistics' and/or 'reorg rebuild'"; }
		});

		


		//------------------------------------------------------------
		// Get USER Defined from configuration. 
		//------------------------------------------------------------
		Configuration conf = Configuration.getCombinedConfiguration();
		
		List<String> removeList = StringUtil.parseCommaStrToList(conf.getProperty("UserDefinedNormalizerManager.remove.list", ""));
		for (String name : removeList)
		{
			if ("all".equalsIgnoreCase(name))
				_userDefinedNormalizeEntries.clear();

			for (Iterator<IUserDefinedNormalizer> iterator = _userDefinedNormalizeEntries.iterator(); iterator.hasNext();)
			{
				IUserDefinedNormalizer entry = iterator.next();

				if (name.equalsIgnoreCase(entry.getName()))
					iterator.remove();
			}
		}
			
//		for (String key : conf.getKeys("UserDefinedNormalizerManager."))
//		{
//			// FIXME: parse UserDefinedNormalizerManager entries, and instantiate entries
//		}

		//-----------------------------------------------------------
		// Get System names for the below output
		//-----------------------------------------------------------
		Set<String> systemNames = new LinkedHashSet<>();
		for (IUserDefinedNormalizer entry : _userDefinedNormalizeEntries)
			systemNames.add(entry.getName());
		int systemNamesCount = systemNames.size();

		//------------------------------------------------------------
		// Get USER Defined Fixers (from Java source files). 
		//------------------------------------------------------------
		Set<IUserDefinedNormalizer> compiledUd = NormalizerCompiler.getInstance().getUserDefinedNormalizers();
		_userDefinedNormalizeEntries.addAll(compiledUd);

		Set<String> udNames = new LinkedHashSet<>();
		for (IUserDefinedNormalizer entry : compiledUd)
			udNames.add(entry.getName());
		int udNamesCount = udNames.size();

		_logger.info("UserDefinedNormalizerManager was initialized with " + systemNamesCount + " System Entries " + systemNames + ", and " + udNamesCount + " User Defined Entries " + udNames);
	}


	/**
	 * Get entries
	 * @return
	 */
	public Set<IUserDefinedNormalizer> getUserDefinedEntries()
	{
		return _userDefinedNormalizeEntries;
	}

	public void add(IUserDefinedNormalizer normilizer)
	{
		_userDefinedNormalizeEntries.add(normilizer);
	}
}


//fix below stuff

//###############################################################
//
///*
//client.sek.cpy_rating.router.mainRouter
//
//0v01 pebr@sek.se Aug 26 2017 Initial version
//
//*/
//
//declare @label varchar(47), 
//        @date datetime,
//        @issuer varchar(15),
//        @rating varchar(20),
//        @labelExists numeric,
//        @ratingExists numeric,
//        @recordExists numeric,
//        @partyExists numeric,
//        @issuerSet numeric
//
//set		@label = 'Moodys'  --'Internal'
//set     @date = '20201105'  --'20170521'
//set     @issuer = '102579'  -- 'STENA' 
//set     @rating = 'Ba3'  -- 'BB-'
//
//set @labelExists = (select count(*) from RT_RTNG_DBF where M_RTNG_A = @label)
//
//set @ratingExists = (select count(*) from RT_RTNG_DBF where M_RTNG_A = @label and M_RTNG = @rating)
//
//set @partyExists = 
//(select count(*)
//from TRN_CPDF_DBF c
//where c.M_LABEL = @issuer)
//
//set @issuerSet = 
//(select count(*)
//from TRN_CPDF_DBF c, CTP_TYPES_DBF t, PARTY_TYPE_DBF p
//where c.M_ID = t.M_CTN
//and t.M_REF = p.M_REFERENCE
//and c.M_LABEL = @issuer
//and p.M_LABEL in ('ISSUER'))
//
//set @recordExists = 
//(select count(*)
//from MPX_RATG_DBF x, MPY_RATG_DBF y
//inner join RT_RTNG_DBF z on y.M_RATING=z.M_REFERENCE
//where 1=1
//and x.M__INDEX_ = y. M__INDEX_
//and x.M_LABEL  = @label
//and x.M__DATE_ = @date 
//and y.M_ISSUER = @issuer
//and z.M_RTNG = @rating)
//
//--select @labelExists as labelExists, @recordExists as recordExists, @partyExists as partyExists, @issuerSet as issuerSet, @ratingExists as ratingExists,
//
//select case
//  when @labelExists = 0 then 'noLabel'
//  when @partyExists = 0 then 'noCounterparty'
//  when @issuerSet = 0 then 'issuerNotSet'
//  when @ratingExists = 0 then 'noRating'
//  when @recordExists = 0 then 'insertion'
//  else 'modification'
//end as status
//
//
//
//
//###############################################################
///* DYNAMIC-SQL: */ DELETE top 1000 FROM PS_CASH_NC_SNAP_E WHERE PS_CASH_NC_SNAP_E.LOGICAL_KEY_ID = 283
//
//
//
//###############################################################
///* DYNAMIC-SQL: */ UPDATE INDEX STATISTICS LIULOG_5980_DBF
//
//
//
//###############################################################
//commit transaction ADJUSTMENTS
//commit transaction BATCH
//commit transaction CHUNK_TRANSACTION
//
//
//
//###############################################################
//delete top 200000 from MxGdB_prod..ETA_8DN_FLW_MX3_DBF  where (M_REF_DATA in (806652,806653,806654,806655,806656,806657,806658,806659))
//
//
//###############################################################
//dump transaction MxGdB_prod to "/sybdev/mx3/sybdump/tran/TRANDUMP.9523"
//
//
//
//###############################################################
//DYNAMIC_SQL dyn146:
//DYNAMIC_SQL dyn169:
//
//
//###############################################################
//FETCH_CURSOR     jconnect_implicit_120:
//FETCH_CURSOR     jconnect_implicit_199:
//
//
//
//###############################################################
//insert bulk SEK_DATAMART_prod..IB_PLVAR_HIST_REP with arrayinsert, nodescribe
//
//
//
//###############################################################
//-- Lets decide what Database Copmpession to used based on the ASE Version we are currently using.
//declare @compression varchar(10)
//set @compression = '2'
//if (@@version_number >= 15500) -- compression 100, 101 was intruduced in ASE 15.5
//        set @compression = '100'
//print 'Using database dump compression: %1!', @compression
//
//execute('
//    dump database MLCdB_prod to "/sybdev/mx3/sybdump/db/DBDUMP1.13880"
//    stripe on "/sybdev/mx3/sybdump/db/DBDUMP2.13880"
//    stripe on "/sybdev/mx3/sybdump/db/DBDUMP3.13880"
//    stripe on "/sybdev/mx3/sybdump/db/DBDUMP4.13880"
//    with compression = ' + @compression
//)
//
//
//
//###############################################################
//OPEN_CURSOR      jconnect_implicit_375:
//OPEN_CURSOR      jconnect_implicit_477:
//
//
//
//###############################################################
//select T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE,T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE 
//from MPX_VOLMAT_DBF T1 ,MPY_VOLMAT_DBF T2
//where T1.M_REFERENCE  *= T2.M_REFERENCE
//  and T1.M__DATE_  *= T2.M__DATE_
//  and T1.M__ALIAS_  *= T2.M__ALIAS_
//  and ((((T1.M__DATE_='20161020') and (T1.M__ALIAS_='BO')) and ((T1.M_SETNAME = 'BSKM1' and T1.M_US_TYPE = 15) and T1.M_TYPE = 0)) and ((T2.M__DATE_='20161020') and (T2.M__ALIAS_='BO'))) order by T1.M_SETNAME asc ,T1.M_US_TYPE asc ,T1.M_TYPE asc
//
//
//###############################################################
//set nocount on
//declare @smeid  varchar(50)
//select @smeid = meid from MxGJobStatus where pid=104789 and eoj is null
//
//if @smeid is null
//  select @smeid = '0' 
//
//insert into MxGJobStatus
//(meid, pid, smeid, systemDate, soj, scriptname, scriptpath, scriptargs, jobname )
//values
//("20201105_004816_105277",105277,@smeid,convert(datetime,"20201105",112),getdate(),"SEK_EOD_MLC_WAIT_DWH_RC_TICKS.sh","/eod/mlc","-r MX_DEFAULT", "default")
//
//
//
//
//###############################################################
//SET NOCOUNT ON
//SELECT jobname from MxGJobStatus where meid="20201104_230651_516213"
//
//
//
//###############################################################
//set nocount on
//select meid from MxGJobStatus 
//where pid=140190
//and soj>=convert(datetime,"             Nov  4 2020  7:11AM ",112) 
//and eoj is null
//
//
//
//###############################################################
//set nocount on
//update MxGJobStatus
//set 
//eoj=getdate()
//where meid="20201105_031619_467240"
//
//
//
//###############################################################
//set nocount on
//update MxGJobStatus
//set 
//jobname='mdrs' , status='DONE' , ret_code=0 
//where meid="20201105_120014_696568"
//
//
//
//###############################################################
//set nocount on
//update MxGJobStatus
//set 
//status='DONE' 
//where meid="20201104_235517_745714"
//
//
//###############################################################
//
//
//
//###############################################################
//
//
//
//###############################################################
//
//
//
//###############################################################
