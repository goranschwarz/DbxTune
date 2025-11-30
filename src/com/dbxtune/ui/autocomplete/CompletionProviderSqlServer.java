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
package com.dbxtune.ui.autocomplete;

import java.awt.Window;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.autocomplete.RoundRobinAutoCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.parser.QueryWindowMessageParser;
import com.dbxtune.ui.autocomplete.completions.CompletionTemplate;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;

public class CompletionProviderSqlServer
extends CompletionProviderAbstractSql
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, RTextScrollPane scroll, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for MS SQL-Server ("+AsetuneSyntaxConstants.SYNTAX_STYLE_MSSQL_TSQL+").");
		textPane.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_MSSQL_TSQL);

		CompletionProviderAbstract acProvider = createCompletionProvider(window, connectionProvider);
		RoundRobinAutoCompletion ac = new SqlAutoCompletion(acProvider);
		ac.setListCellRenderer(acProvider.createDefaultCompletionCellRenderer());
		ac.addCompletionProvider(acProvider.createTemplateProvider());
		ac.install(textPane);
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
		ac.setChoicesWindowSize(
				Configuration.getCombinedConfiguration().getIntProperty("completionProvider.setChoicesWindowSize.width", 600), 
				Configuration.getCombinedConfiguration().getIntProperty("completionProvider.setChoicesWindowSize.height", 600));
		ac.setDescriptionWindowSize(
				Configuration.getCombinedConfiguration().getIntProperty("completionProvider.setDescriptionWindowSize.width", 600), 
				Configuration.getCombinedConfiguration().getIntProperty("completionProvider.setDescriptionWindowSize.height", 600));
		
		textPane.addParser(new QueryWindowMessageParser(scroll));

		// enable the ErrorStripe ????
		errorStrip.setVisible(true);
		
		// enable the "icon" area on the left side
		scroll.setIconRowHeaderEnabled(true);

		// Install any extra items in the Editors Right Click Menu
		acProvider.installEditorPopupMenuExtention(textPane);

		return acProvider;
	}

	/**
	 * Constructor
	 * @param owner
	 * @param connectionProvider
	 */
	public CompletionProviderSqlServer(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	/**
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	public static CompletionProviderAbstract createCompletionProvider(Window window, ConnectionProvider connectionProvider)
	{
		_logger.debug("MS SQL-Server: createCompletionProvider()");

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		CompletionProviderSqlServer provider = new CompletionProviderSqlServer(window, connectionProvider);

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		provider.refreshCompletionForStaticCmds();
		
		return provider;
	}

//	@Override
//	protected void refreshCompletionForStaticCmds()
//	{
//		resetStaticCompletion();
//
//		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
//		addStaticCompletion(new BasicCompletion(this, "SELECT * FROM "));
//	}
//
//	@Override
//	public DefaultCompletionProvider createTemplateProvider()
//	{
//		// A DefaultCompletionProvider is the simplest concrete implementation
//		// of CompletionProvider. This provider has no understanding of
//		// language semantics. It simply checks the text entered up to the
//		// caret position for a match against known completions. This is all
//		// that is needed in the majority of cases.
//		DefaultCompletionProvider provider = new DefaultCompletionProvider();
//
//		// Add completions for all Java keywords. A BasicCompletion is just
//		// a straightforward word completion.
//		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM "));
//
//		return provider;
//	}

	@Override
	public List<CompletionTemplate> createCompletionTemplates()
	{
		List<CompletionTemplate> list = CompletionProviderStaticTemplates.createCompletionTemplates();

	
		list.add( new CompletionTemplate( "cursor",
				"declare @c_c1 varchar(255) \n" +
				"declare @c_c2 varchar(20) \n" +
				" \n" +
				"DECLARE <CURSOR_NAME> cursor STATIC LOCAL for \n" +
				"	SELECT c1, c2 \n" +
				"	FROM tabname \n" +
				"	WHERE c3 = 'val' \n" +
				"	ORDER BY ... \n" +
				" \n" +
				"OPEN <CURSOR_NAME> \n" +
				" \n" +
				"while (1=1)  \n" +
				"begin \n" +
				"    /* get row into variables */ \n" +
				"	 FETCH <CURSOR_NAME> into @c_c1, @c_c2 \n" +
				" \n" +
				"    /* get out of here if no more rows */ \n" +
				"    if (@@fetch_status != 0) \n" +
				"        break \n" +
				" \n" +
				"	 /* Do something... */ \n" +
				"end \n" +
				"CLOSE <CURSOR_NAME> \n" +
				"DEALLOCATE cursor <CURSOR_NAME> \n" +
				"",
				"Add cursor"));

		list.add( new CompletionTemplate( "index",
				"CREATE INDEX index_name ON table_name(c1, c2, c3) WITH (SORT_IN_TEMPDB = ON, DATA_COMPRESSION = PAGE)",
				"Create index"));
				
		list.add( new CompletionTemplate( "create_index",
				"CREATE INDEX index_name ON table_name(c1, c2, c3) WITH (SORT_IN_TEMPDB = ON, DATA_COMPRESSION = PAGE)",
				"Create index"));
				
		list.add( new CompletionTemplate( "clear_plan_cache",
				"DBCC FREEPROCCACHE WITH NO_INFOMSGS /* NOTE: DO NOT USE IN PRODUCTION SYSTEMS */ ",
				"Removes all elements from the plan cache. New plans has to be created for all compiled objects."));

		list.add( new CompletionTemplate( "clear_data_cache",
				"DBCC DROPCLEANBUFFERS WITH NO_INFOMSGS /* NOTE: DO NOT USE IN PRODUCTION SYSTEMS */ ",
				"Removes all clean buffers from the buffer pool. Pages/Data has to be read from disk again."));

		list.add( new CompletionTemplate( "opentran",
				"DBCC OPENTRAN WITH TABLERESULTS, NO_INFOMSGS /* NOTE: You need to be in the desired database */ ",
				"Identify active transactions that may be preventing log truncation."));
		
		list.add( new CompletionTemplate( "sp_WhoIsActive",
				"/*** The procedure can be installed from: 'SQL*' Button on the Toolbar --> Install some extra system stored procedures --> Adam Machanic ***/ \n" +
					"exec sp_WhoIsActive \n" +
					"     @format_output        = 1   /* Default: 1. Formats some of the output columns in a more 'human readable' form. 0='disables outfput format', 1='formats the output for variable-width fonts' 2='formats the output for fixed-width fonts' */ \n" +
					"    ,@show_system_spids    = 0   /* Default: 0. Retrieve data about system sessions */ \n" +
					"    ,@get_memory_info      = 0   /* Default: 0. Get additional information related to workspace memory requested_memory, granted_memory, max_used_memory, and memory_info. */ \n" +
					"    ,@get_locks            = 0   /* Default: 0. Gets associated locks for each request, aggregated in an XML format */ \n" +
					"    ,@get_transaction_info = 0   /* Default: 0. Enables pulling transaction log write info, transaction duration, and the implicit_transaction identification column */ \n" +
					"    ,@get_outer_command    = 0   /* Default: 0. Get the associated outer ad hoc query or stored procedure call, if available */ \n" +
					"    ,@get_plans            = 0   /* Default: 0. Get associated query plans for running tasks, if available. 1='gets the plan based on the requests statement offset' 2='gets the entire plan based on the requests plan_handle' */ \n" +
					"    ,@find_block_leaders   = 0   /* Default: 0. Walk the blocking chain and count the number of total SPIDs blocked all the way down by a given session Also enables task_info Level 1, if @get_task_info is set to 0 */ \n" +
					"    ,@get_additional_info  = 0   /* Default: 0. If @get_task_info is set to 2 and a lock wait is detected, a subnode called block_info will be populated with some or all of the following: lock_type, database_name, object_id, file_id, hobt_id, applock_hash, metadata_resource, metadata_class_id, object_name, schema_name */ \n" +
					"    ,@show_sleeping_spids  = 1   /* Default: 1. Controls how sleeping SPIDs are handled, based on the idea of levels of interest. 0='does not pull any sleeping SPIDs', 1='pulls only those sleeping SPIDs that also have an open transaction', 2='pulls all sleeping SPIDs' */ \n" +
					"    ,@get_task_info        = 1   /* Default: 1. Get information on active tasks, based on three interest levels. 0='does not pull any task-related information', 1='is a lightweight mode that pulls the top non-CXPACKET wait, giving preference to blockers', 2='pulls all available task-based metrics, including: number of active tasks, current wait stats, physical I/O, context switches, and blocker information' */ \n" +
					"",
				"sp_WhoIsActive"));

		list.add( new CompletionTemplate( "sp_HumanEvents_all",
				"/*** see: https://erikdarling.com/sp_humanevents/ or https://github.com/erikdarlingdata/DarlingData/tree/main/sp_HumanEvents ***/ \n" +
				"/*** >>>> The procedure can be installed from: 'SQL*' Button on the Toolbar --> Install some extra system stored procedures --> Erik Darling... ***/ \n" +
					"\n" +
					"/* To capture all types of 'completed' queries that have run for at least one second, for 20 seconds, from a specific database */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'query', @query_duration_ms = 1000, @seconds_sample = 20, @database_name = 'YourMom'; \n" +
					"\n" +
					"/* Maybe you want to filter out queries that have asked for a bit of memory */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'query', @query_duration_ms = 1000, @seconds_sample = 20, @requested_memory_mb = 1024; \n" +
					"\n" +
					"/* Or maybe you want to find unparameterized queries from a poorly written app that constructs strings in ugly ways, but it generates a lot of queries so you only want data on about a third of them. */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'compilations', @client_app_name = N'GL00SNIFR', @session_id = 'sample', @sample_divisor = 3; \n" +
					"\n" +
					"/* Perhaps you think queries recompiling are the cause of your problems! Heck, they might be. Have you tried removing recompile hints? */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'recompilations', @seconds_sample = 30; \n" +
					"\n" +
					"/* Look, blocking is annoying. Just turn on RCSI, you goblin. Unless you're not allowed to */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'blocking', @seconds_sample = 60, @blocking_duration_ms = 5000; \n" +
					"\n" +
					"/* If you want to track wait stats, this'll work pretty well. Keep in mind 'all' is a focused list of 'interesting' waits to queries, not every wait stat */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'waits', @wait_duration_ms = 10, @seconds_sample = 100, @wait_type = N'all'; \n" +
					"\n" +
					"/* Note that THREADPOOL is SOS_WORKER in xe-land. why? -- https://www.sqlskills.com/blogs/jonathan/mapping-wait-types-in-dm_os_wait_stats-to-extended-events/ */ \n" + 
					"EXEC dbo.sp_HumanEvents @event_type = 'waits', @wait_duration_ms = 10, @seconds_sample = 100, @wait_type = N'SOS_WORKER,RESOURCE_SEMAPHORE'; \n" +
					"\n" +
					"/* WARNING: If you want trace SHORT queries, you might need to specify: @gimme_danger = 1 */ \n" + 
					"EXEC sp_HumanEvents @event_type = N'query', @query_duration_ms = 1, @gimme_danger = 1; \n" +
					"",
				"sp_HumanEvents with some examples"));
		// TODO: sp_HumanEvents_query
		// TODO: sp_HumanEvents_compilations
		// TODO: sp_HumanEvents_recompilations
		// TODO: sp_HumanEvents_blocking
		// TODO: sp_HumanEvents_waits

		list.add( new CompletionTemplate( "sp_Blitz_all",
				"/*** see: https://www.brentozar.com/training/how-i-use-the-first-responder-kit/ or https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit ***/ \n" +
				"/*** >>>> The procedure can be installed from: 'SQL*' Button on the Toolbar --> Install some extra system stored procedures --> Brent Ozar... ***/ \n" +
					"\n" +
					"/* Do a server-wide health check */ \n" + 
					"EXEC sp_Blitz @CheckServerInfo = 1; \n" +
					"\n" +
					"/* Look at your server's wait stats and storage throughput (since it started up: @SinceStartup = 1) */ \n" + 
					"EXEC sp_BlitzFirst @ExpertMode = 0, @Seconds = 5, @SinceStartup = 0; \n" +
					"\n" +
					"/* Find the queries causing your top wait types, using my favorite parameters for deeper diagnosis and trending */ \n" + 
					"EXEC sp_BlitzCache @SortOrder = 'cpu', @MinutesBack = NULL; /* @SortOrder can be: 'CPU', 'Reads', 'Writes', 'Duration', 'Executions', 'Recent Compilations', 'Memory Grant', 'Unused Grant', 'Spills', 'Query Hash', 'Duplicate'. Additionally, the word 'Average' or 'Avg' can be used to sort on averages rather than total. 'Executions per minute' and 'Executions / minute' can be used to sort by execution per minute. For the truly lazy, 'xpm' can also be used. Note that when you use all or all avg, the only parameters you can use are @Top and @DatabaseName. All others will be ignored */ \n" +
					"\n" +
					"/* Diagnose the most urgent indexing issues specifically related to your top wait types */ \n" + 
					"EXEC sp_BlitzIndex -- FIXME; \n" +
					"\n" +
					"/* Check deadlocks */ \n" + 
					"EXEC sp_BlitzLock -- FIXME; \n" +
					"\n" +
					"/* Kind of sp_WhoIsActive ... */ \n" + 
					"EXEC sp_BlitzWho \n" +
					"",
				"sp_Blitz with some examples of the various procedures"));

//		TODO; // Fix above text for sp_Blitz...

		// TODO: sp_Blitz
		// TODO: sp_BlitzFirst
		// TODO: sp_BlitzCache
		// TODO: sp_BlitzLock
		// TODO: sp_BlitzWho

//		TODO; // Continue to look at "Connection Wait Dialog" -- Why it seems a bit "off" ... wait=0, vs: wait=10_000 

		return list;
	}
}
