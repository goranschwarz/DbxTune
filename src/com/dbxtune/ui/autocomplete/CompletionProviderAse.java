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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.autocomplete.RoundRobinAutoCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.parser.QueryWindowMessageParser;
import com.dbxtune.ui.autocomplete.completions.CompletionTemplate;
import com.dbxtune.ui.autocomplete.completions.DbInfo;
import com.dbxtune.ui.autocomplete.completions.ProcedureInfo;
import com.dbxtune.ui.autocomplete.completions.ProcedureParameterInfo;
import com.dbxtune.ui.autocomplete.completions.TableColumnInfo;
import com.dbxtune.ui.autocomplete.completions.TableInfo;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.CollectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.Ver;

public class CompletionProviderAse
extends CompletionProviderAbstractSql
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected Map<String, String> _aseMonTableDesc = new HashMap<String, String>();

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, RTextScrollPane scroll, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for Sybase ASE ("+AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL+").");
		textPane.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

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
	public CompletionProviderAse(Window owner, ConnectionProvider connectionProvider)
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
		// Whether templates are enabled is a global property affecting all
		// RSyntaxTextAreas, so this method is static.
		RSyntaxTextArea.setTemplatesEnabled(true);

//		// Code templates are shared among all RSyntaxTextAreas. You add and
//		// remove templates through the shared CodeTemplateManager instance.
//		CodeTemplateManager ctm = RSyntaxTextArea.getCodeTemplateManager();
//System.out.println("CodeTemplateManager.trigger='"+ctm.getInsertTrigger()+"', '"+ctm.getInsertTriggerString()+"'.");
//
//		// Remove all templates, since it's shared between all RSyntaxTextAreas
////		for (CodeTemplate ct : ctm.getTemplates())
////			ctm.removeTemplate(ct);
//
//		// StaticCodeTemplates are templates that insert static text before and
//		// after the current caret position. This template is basically shorthand
//		// for "System.out.println(".
//		CodeTemplate ct = new StaticCodeTemplate("sout", "System.out.println(", null);
//		ctm.addTemplate(ct);
//
//		// This template is for a for-loop. The caret is placed at the upper
//		// bound of the loop.
//		ct = new StaticCodeTemplate("fb", "for (int i=0; i<", "; i++) {\n\t\n}\n");
//		ctm.addTemplate(ct);
		
//System.out.println("ASE: createCompletionProvider()");
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		CompletionProviderAse provider = new CompletionProviderAse(window, connectionProvider);
		
		provider.refreshCompletionForStaticCmds();
		
//String template = "for (int ${i} = 0; ${i} < ${array}.length; ${i}++) {\n\t${cursor}\n}";
//TemplateCompletion tc = new TemplateCompletion(provider, "for", "for-loop", template);
//provider.addStaticCompletion(tc);

		return provider;
	}


	//##############################################################################
	//##############################################################################
	//##############################################################################
//	@Override
//	protected void refreshCompletionForStaticCmds()
//	{
//		resetStaticCompletion();
//
//		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
//		addStaticCompletion(new BasicCompletion(this, "SELECT * FROM "));
//		addStaticCompletion(new BasicCompletion(this, "SELECT row_count(db_id()), object_id('') "));
//		addStaticCompletion(new BasicCompletion(this, "CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));
//		addStaticCompletion(new BasicCompletion(this, "SELECT * FROM master..monTables ORDER BY TableName"));
//		addStaticCompletion(new BasicCompletion(this, "SELECT * FROM master..monTableColumns WHERE TableName = 'monXXX' ORDER BY ColumnID"));
//
//		// Add a couple of "shorthand" completions. These completions don't
//		// require the input text to be the same thing as the replacement text.
//		addStaticCompletion(new ShorthandCompletion(this, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', '#G'",                                                  "Cache Size"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', 'cache_partitions=#'",                                  "Cache Partitions"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_bindcache",       "exec sp_bindcache 'cache name', 'dbname' -- [,tab_name [,index_name]]",                           "Bind db/object to cache"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_unbindcache_all", "exec sp_unbindcache_all 'cache name'",                                                            "Unbind all from cache"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'sizeM|G', 'toPool_K' --[,'fromPool_K']",                "Pool Size"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'wash=size[P|K|M|G]'",                 "Pool Wash Size"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'local async prefetch limit=percent'", "Pool Local Async Prefetch Limit"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_configure",       "exec sp_configure 'memory'",                                                                      "Memory left for reconfigure"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_configure",       "exec sp_configure 'Monitoring'",                                                                  "Check Monitor configuration"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_configure",       "exec sp_configure 'nondefault'",                                                                  "Get changed configuration parameters"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_helptext",        "exec sp_helptext 'procname', NULL/*startRow*/, NULL/*numOfRows*/, 'showsql,linenumbers'",          "Get procedure text, with line numbers"));
//		addStaticCompletion(new ShorthandCompletion(this, "sp_helptext",        "exec sp_helptext 'procname', NULL, NULL, 'showsql,ddlgen'",                                        "Get procedure text, as DDL"));
//
//		addStaticCompletion(new ShorthandCompletion(this, "sp_password",        "sp_password caller_password, new_password [,login_name]",                                         "Change password"));
//
//		addStaticCompletion(new ShorthandCompletion(this, "sp_cacheconfig",
//				"/*  \n" +
//				"** Below is commands/instructions to setup a log cache on a 2K server \n" +
//				"** If you have another server page size, values needs to be changed \n" +
//				"** select @@maxpagesize/1024 to get the servers page size \n" +
//				"*/ \n" +
//				"-- Create a cache that holds transaction log(s) \n" +
//				"exec sp_cacheconfig 'log_cache', '500M', 'logonly', 'relaxed', 'cache_partition=1' \n" +
//				" \n" +
//				"-- Most of the memory should be in the 4K pool (2 pages per IO) \n" +
//				"sp_poolconfig 'log_cache', '495M', '4K', '2K' -- size the 4K pool to #MB, grab memory from the 2K pool \n" +
//				" \n" +
//				"-- and maybe some in the 16K (8 pages) pool \n" +
//				"-- sp_poolconfig 'log_cache', '#M', '16K', '2K'  \n" +
//				" \n" +
//				"-- To bind a database transaction log to a Named Cache, it has to be in single user mode \n" +
//				"exec sp_dboption  'dbname', 'single', 'true' \n" +
//				"exec sp_bindcache 'log_cache', 'dbname', 'syslogs' \n" +
//				"exec sp_dboption  'dbname', 'single', 'false' \n" +
//				" \n" +
//				"-- Change the LOG IO SIZE (default is 4K or: 2 pages per IO) \n" +
//				"--dbname..sp_logiosize '8' -- to use the 8K memory pool (4 pages per IO) \n" +
//				"",
//				"Create a 'log cache' and bind database(s) to it."));
//
//		addStaticCompletion(new ShorthandCompletion(this, "alter",   "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//		addStaticCompletion(new ShorthandCompletion(this, "engines", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//		addStaticCompletion(new ShorthandCompletion(this, "threads", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//
//		// monTables
//		addStaticCompletion(new ShorthandCompletion(this, 
//				"monTables",  
//				"select TableID, TableName, Columns, Description from monTables where TableName like 'mon%'", 
//				"Get monitor tables in this system."));
//		// monColumns
//		addStaticCompletion(new ShorthandCompletion(this, 
//				"monColumns", 
//				"select TableName, ColumnName, TypeName, Length, Description from monTableColumns where TableName like 'mon%'", 
//				"Get monitor tables and columns in this system."));
//		
//		// \exec  and \rpc templates
//		addStaticCompletion(new ShorthandCompletion(this, "exec", "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		addStaticCompletion(new ShorthandCompletion(this, "rpc",   "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//	}

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
//		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
//		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM "));
//		provider.addCompletion(new BasicCompletion(provider, "SELECT row_count(db_id()), object_id('') "));
//		provider.addCompletion(new BasicCompletion(provider, "CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));
//		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM master..monTables ORDER BY TableName"));
//		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM master..monTableColumns WHERE TableName = 'monXXX' ORDER BY ColumnID"));
//
//		// Add a couple of "shorthand" completions. These completions don't
//		// require the input text to be the same thing as the replacement text.
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', '#G'",                                                  "Cache Size"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', 'cache_partitions=#'",                                  "Cache Partitions"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_bindcache",       "exec sp_bindcache 'cache name', 'dbname' -- [,tab_name [,index_name]]",                           "Bind db/object to cache"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_unbindcache_all", "exec sp_unbindcache_all 'cache name'",                                                            "Unbind all from cache"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'sizeM|G', 'toPool_K' --[,'fromPool_K']",                "Pool Size"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'wash=size[P|K|M|G]'",                 "Pool Wash Size"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'local async prefetch limit=percent'", "Pool Local Async Prefetch Limit"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",       "exec sp_configure 'memory'",                                                                      "Memory left for reconfigure"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",       "exec sp_configure 'Monitoring'",                                                                  "Check Monitor configuration"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",       "exec sp_configure 'nondefault'",                                                                  "Get changed configuration parameters"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_helptext",        "exec sp_helptext 'procname', NULL/*startRow*/, NULL/*numOfRows*/, 'showsql,linenumbers'",          "Get procedure text, with line numbers"));
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_helptext",        "exec sp_helptext 'procname', NULL, NULL, 'showsql,ddlgen'",                                        "Get procedure text, as DDL"));
//
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_password",        "sp_password caller_password, new_password [,login_name]",                                         "Change password"));
//
//		provider.addCompletion(new ShorthandCompletion(provider, "sp_cacheconfig",
//				"/*  \n" +
//				"** Below is commands/instructions to setup a log cache on a 2K server \n" +
//				"** If you have another server page size, values needs to be changed \n" +
//				"** select @@maxpagesize/1024 to get the servers page size \n" +
//				"*/ \n" +
//				"-- Create a cache that holds transaction log(s) \n" +
//				"exec sp_cacheconfig 'log_cache', '500M', 'logonly', 'relaxed', 'cache_partition=1' \n" +
//				" \n" +
//				"-- Most of the memory should be in the 4K pool (2 pages per IO) \n" +
//				"sp_poolconfig 'log_cache', '495M', '4K', '2K' -- size the 4K pool to #MB, grab memory from the 2K pool \n" +
//				" \n" +
//				"-- and maybe some in the 16K (8 pages) pool \n" +
//				"-- sp_poolconfig 'log_cache', '#M', '16K', '2K'  \n" +
//				" \n" +
//				"-- To bind a database transaction log to a Named Cache, it has to be in single user mode \n" +
//				"exec sp_dboption  'dbname', 'single', 'true' \n" +
//				"exec sp_bindcache 'log_cache', 'dbname', 'syslogs' \n" +
//				"exec sp_dboption  'dbname', 'single', 'false' \n" +
//				" \n" +
//				"-- Change the LOG IO SIZE (default is 4K or: 2 pages per IO) \n" +
//				"--dbname..sp_logiosize '8' -- to use the 8K memory pool (4 pages per IO) \n" +
//				"",
//				"Create a 'log cache' and bind database(s) to it."));
//
//		provider.addCompletion(new ShorthandCompletion(provider, "alter",   "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//		provider.addCompletion(new ShorthandCompletion(provider, "engines", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//		provider.addCompletion(new ShorthandCompletion(provider, "threads", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
//
//		// monTables
//		provider.addCompletion(new ShorthandCompletion(provider, 
//				"monTables",  
//				"select TableID, TableName, Columns, Description from monTables where TableName like 'mon%'", 
//				"Get monitor tables in this system."));
//		// monColumns
//		provider.addCompletion(new ShorthandCompletion(provider, 
//				"monColumns", 
//				"select TableName, ColumnName, TypeName, Length, Description from monTableColumns where TableName like 'mon%'", 
//				"Get monitor tables and columns in this system."));
//		
//		// \exec  and \rpc templates
//		provider.addCompletion(new ShorthandCompletion(provider, "exec", "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		provider.addCompletion(new ShorthandCompletion(provider, "rpc",   "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//
//		return provider;
//	}

	@Override
	public List<CompletionTemplate> createCompletionTemplates()
	{
//		List<CompletionTemplate> list = new ArrayList<CompletionTemplate>();

		List<CompletionTemplate> list = CompletionProviderStaticTemplates.createCompletionTemplates();
		
		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
//		list.add( new CompletionTemplate("SELECT * FROM "));
		list.add( new CompletionTemplate("SELECT row_count(db_id()), object_id('') "));
//		list.add( new CompletionTemplate("CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));
		list.add( new CompletionTemplate("SELECT * FROM master..monTables ORDER BY TableName"));
		list.add( new CompletionTemplate("SELECT * FROM master..monTableColumns WHERE TableName = 'monXXX' ORDER BY ColumnID"));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		list.add( new CompletionTemplate( "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', '#G'",                                                  "Cache Size"));
		list.add( new CompletionTemplate( "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', 'cache_partitions=#'",                                  "Cache Partitions"));
		list.add( new CompletionTemplate( "sp_bindcache",       "exec sp_bindcache 'cache name', 'dbname' -- [,tab_name [,index_name]]",                           "Bind db/object to cache"));
		list.add( new CompletionTemplate( "sp_unbindcache_all", "exec sp_unbindcache_all 'cache name'",                                                            "Unbind all from cache"));
		list.add( new CompletionTemplate( "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'sizeM|G', 'toPool_K' --[,'fromPool_K']",                "Pool Size"));
		list.add( new CompletionTemplate( "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'wash=size[P|K|M|G]'",                 "Pool Wash Size"));
		list.add( new CompletionTemplate( "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'local async prefetch limit=percent'", "Pool Local Async Prefetch Limit"));
		list.add( new CompletionTemplate( "sp_configure",       "exec sp_configure 'memory'",                                                                      "Memory left for reconfigure"));
		list.add( new CompletionTemplate( "sp_configure",       "exec sp_configure 'Monitoring'",                                                                  "Check Monitor configuration"));
		list.add( new CompletionTemplate( "sp_configure",       "exec sp_configure 'nondefault'",                                                                  "Get changed configuration parameters"));
		list.add( new CompletionTemplate( "sp_helptext",        "exec sp_helptext 'procname', NULL/*startRow*/, NULL/*numOfRows*/, 'showsql,linenumbers'",          "Get procedure text, with line numbers"));
		list.add( new CompletionTemplate( "sp_helptext",        "exec sp_helptext 'procname', NULL, NULL, 'showsql,ddlgen'",                                        "Get procedure text, as DDL"));

		list.add( new CompletionTemplate( "sp_password",        "sp_password caller_password, new_password [,login_name]",                                         "Change password"));

		list.add( new CompletionTemplate( "sp_cacheconfig",
				"/*  \n" +
				"** Below is commands/instructions to setup a log cache on a 2K server \n" +
				"** If you have another server page size, values needs to be changed \n" +
				"** select @@maxpagesize/1024 to get the servers page size \n" +
				"*/ \n" +
				"-- Create a cache that holds transaction log(s) \n" +
				"exec sp_cacheconfig 'log_cache', '500M', 'logonly', 'relaxed', 'cache_partition=1' \n" +
				" \n" +
				"-- Most of the memory should be in the 4K pool (2 pages per IO) \n" +
				"sp_poolconfig 'log_cache', '495M', '4K', '2K' -- size the 4K pool to #MB, grab memory from the 2K pool \n" +
				" \n" +
				"-- and maybe some in the 16K (8 pages) pool \n" +
				"-- exec sp_poolconfig 'log_cache', '#M', '16K', '2K'  \n" +
				" \n" +
				"-- To bind a database transaction log to a Named Cache, it has to be in single user mode \n" +
				"exec sp_dboption  'dbname', 'single', 'true' \n" +
				"exec dbname..sp_bindcache 'log_cache', 'dbname', 'syslogs' \n" +
				"exec sp_dboption  'dbname', 'single', 'false' \n" +
				" \n" +
				"-- Change the LOG IO SIZE (default is 4K or: 2 pages per IO) \n" +
				"--exec dbname..sp_logiosize '8' -- to use the 8K memory pool (4 pages per IO) \n" +
				"",
				"Create a 'log cache' and bind database(s) to it."));

		list.add( new CompletionTemplate( "sp_cacheconfig",
				"/*  \n" +
				"** Below is commands/instructions to setup a tempdb cache on a 2K server \n" +
				"** If you have another server page size, values needs to be changed \n" +
				"** select @@maxpagesize/1024 to get the servers page size \n" +
				"*/ \n" +
				"-- Create a cache that holds tempdb \n" +
				"exec sp_cacheconfig 'tempdb_cache', '500M', 'mixed', 'relaxed', 'cache_partition=#' \n" +
				" \n" +
				"-- and maybe some in the 16K (8 pages) pool \n" +
				"-- exec sp_poolconfig 'tempdb_cache', '#M', '16K' \n" +
				" \n" +
				"-- Bind the tempdb to the cache \n" +
				"exec sp_bindcache 'tempdb_cache', 'tempdb' \n" +
				"---------------------------------------------------------------------------\n" +
				"-- NOTE: ASE needs to be rebooted for the tempdb binding to take effect... \n" +
				"---------------------------------------------------------------------------\n" +
				"",
				"Create a 'tempdb_cache' and bind database(s) to it."));

		list.add( new CompletionTemplate( "alter",   "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
		list.add( new CompletionTemplate( "engines", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));
		list.add( new CompletionTemplate( "threads", "alter thread pool syb_default_pool with thread count = #", "Alter number of threads in 15.7"));

		// monTables
		list.add( new CompletionTemplate( 
				"monTables",  
				"select TableID, TableName, Columns, Description from master.dbo.monTables where TableName like 'mon%'", 
				"Get monitor tables in this system."));
		// monColumns
		list.add( new CompletionTemplate( 
				"monColumns", 
				"select TableName, ColumnName, TypeName, Length, Description from master.dbo.monTableColumns where TableName like 'mon%'", 
				"Get monitor tables and columns in this system."));
		
//		// \exec  and \rpc templates
//		list.add( new CompletionTemplate( "exec",    "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "\\exec",  "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "rpc",     "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "\\rpc",   "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "call",    "\\call procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "\\call",  "\\call procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using RPC method"));
//		list.add( new CompletionTemplate( "prep",    "\\prep insert into t1 values(?, ?) :( string = '1', int = 99 ) -- Prepared SQL with 2 parameters", "Execute a SQL Statement using java PreparedStatement method"));
//		list.add( new CompletionTemplate( "\\prep",  "\\prep insert into t1 values(?, ?) :( string = '1', int = 99 ) -- Prepared SQL with 2 parameters", "Execute a SQL Statement using java PreparedStatement method"));
//
		
		list.add( new CompletionTemplate( "procedure",
				"------------------------------------------------------------- \n" +
				"-- Procedure: <PROCNAME> \n" +
				"------------------------------------------------------------- \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"if ((select object_id('<PROCNAME>')) is not null) \n" +
				"begin \n" +
				"	print 'drop   procedure ''%1!.%2!.%3!''. SUCCEEDED', @dbname, 'dbo', '<PROCNAME>' \n" +
				"	drop procedure <PROCNAME> \n" +
				"end \n" +
				"go \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"print 'create procedure ''%1!.%2!.%3!''.', @dbname, 'dbo', '<PROCNAME>' \n" +
				"go \n" +
				" \n" +
				"/*=====================================================================** \n" +
				"** PROCEDURE: <PROCNAME> \n" +
				"**---------------------------------------------------------------------** \n" +
				"** Description: \n" +
				"** \n" +
				"** FIXME \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** Input parameters: \n" +
				"** \n" +
				"** @p1   fixme \n" +
				"** @p2   fixme \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** output parameters: \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** output select: \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** Return codes: \n" +
				"** \n" +
				"** 0	- ok. \n" +
				"** 1 	- error \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** Error codes: \n" +
				"** \n" +
				"**---------------------------------------------------------------------** \n" +
				"** History: \n" +
				"** \n" +
				"** YYYY-mm-dd  x.y.z  Firstname Lastname \n" +
				"**                    Created \n" +
				"**---------------------------------------------------------------------*/ \n" +
				" \n" +
				"create proc <PROCNAME> \n" +
				"( \n" +
				"	@p1 int = _defaultValue1_, \n" +
				"	@p2 int = _defaultValue2_ \n" +
				") \n" +
				"as \n" +
				"begin \n" +
				"	-- code \n" +
				"end \n" +
				"go \n" +
				" \n" +
				"-- Who should be able to execute this proc \n" +
				"grant exec on <PROCNAME> to public|user_name|group_name|role_name \n" +
				"go \n" +
				" \n" +
				"-- Check if we succeeded creating it \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"if ((select object_id('<PROCNAME>')) is not null) \n" +
				"	print 'create procedure ''%1!.%2!.%3!''. SUCCEEDED', @dbname, 'dbo', '<PROCNAME>' \n" +
				"else \n" +
				"	print 'create procedure ''%1!.%2!.%3!''. FAILED', @dbname, 'dbo', '<PROCNAME>' \n" +
				"go \n" +
				"",
				"Create stored procedure with a head."));

//		list.add( new CompletionTemplate( "cursor",
//				"declare @c_c1 varchar(255) \n" +
//				"declare @c_c2 varchar(20) \n" +
//				" \n" +
//				"DECLARE <CURSOR_NAME> cursor for \n" +
//				"	SELECT c1, c2 \n" +
//				"	FROM tabname \n" +
//				"	WHERE c3 = 'val' \n" +
//				"FOR READ ONLY \n" +
//				" \n" +
//				"OPEN <CURSOR_NAME> \n" +
//				"FETCH <CURSOR_NAME> into @c_c1, @c_c2 \n" +
//				" \n" +
//				"while (@@sqlstatus = 0)  \n" +
//				"begin \n" +
//				"	-- Do something... \n" +
//				"	FETCH <CURSOR_NAME> into @c_c1, @c_c2 \n" +
//				"end \n" +
//				"CLOSE <CURSOR_NAME> \n" +
//				"DEALLOCATE cursor <CURSOR_NAME> \n" +
//				"",
//				"Add cursor"));
//				
		list.add( new CompletionTemplate( "cursor",
				"declare @c_c1 varchar(255) \n" +
				"declare @c_c2 varchar(20) \n" +
				" \n" +
				"DECLARE <CURSOR_NAME> cursor for \n" +
				"	SELECT c1, c2 \n" +
				"	FROM tabname \n" +
				"	WHERE c3 = 'val' \n" +
				"FOR READ ONLY \n" +
				" \n" +
				"OPEN <CURSOR_NAME> \n" +
				" \n" +
				"while (1=1)  \n" +
				"begin \n" +
				"    -- get row into variables \n" +
				"	 FETCH <CURSOR_NAME> into @c_c1, @c_c2 \n" +
				" \n" +
				"    -- get out of here if no more rows \n" +
				"    if (@@sqlstatus != 0) \n" +
				"        break \n" +
				" \n" +
				"	 -- Do something... \n" +
				"end \n" +
				"CLOSE <CURSOR_NAME> \n" +
				"DEALLOCATE cursor <CURSOR_NAME> \n" +
				"",
				"Add cursor"));
				
		list.add( new CompletionTemplate( "table",
				"------------------------------------------------------------- \n" +
				"-- Table: <TABNAME> \n" +
				"------------------------------------------------------------- \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"if ((select object_id('<TABNAME>')) is not null) \n" +
				"begin \n" +
				"	print 'drop   table ''%1!.%2!.%3!''. SUCCEEDED', @dbname, 'dbo', '<TABNAME>' \n" +
				"	drop table <TABNAME> \n" +
				"end \n" +
				"go \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"print 'create table ''%1!.%2!.%3!''.', @dbname, 'dbo', '<TABNAME>' \n" +
				"go \n" +
				" \n" +
				"create table <TABNAME> \n" +
				"( \n" +
				"	id      int             not null,\n" +
				"	--id    bigint          identity,\n" +
				"	c1      varchar(30)     not null,\n" +
				"	c2      varchar(30)     not null,\n" +
				"	c3      varchar(30)     not null,\n" +
				"	c4      varchar(30)     not null,\n" +
				"	c5      varchar(30)     not null,\n" +
				"	primary key(id) \n" +
				") \n" +
				"--lock datarows|datapages|allpages\n" +
				"--with identity_gap = ####\n" +
				" \n" +
				"-- Who should be able to access the table \n" +
				"grant select, insert, update, delete on <TABNAME> to public|user_name|group_name|role_name \n" +
				"go \n" +
				" \n" +
				"-- Check if we succeeded creating it \n" +
				"declare @dbname varchar(255) \n" +
				"select @dbname = db_name() \n" +
				"if ((select object_id('<TABNAME>')) is not null) \n" +
				"	print 'create table ''%1!.%2!.%3!''. SUCCEEDED', @dbname, 'dbo', '<TABNAME>' \n" +
				"else \n" +
				"	print 'create table ''%1!.%2!.%3!''. FAILED', @dbname, 'dbo', '<TABNAME>' \n" +
				"go \n" +
				"",
				"Create table"));

		list.add( new CompletionTemplate( "index",
				"create index <TABNAME>_ix1 on <TABNAME>(c1, c2)\n" +
				"create unique index <TABNAME>_ix1 on <TABNAME>(c1, c2)\n" +
				"create unique clustered index <TABNAME>_ix1 on <TABNAME>(c1, c2)\n" +
				"",
				"Create index"));

		list.add( new CompletionTemplate( "identity",
				"------------------------------------------------------------------------------------------------\n" +
				"-- Set a new MAX value for identity column for a specific table \n" +
				"------------------------------------------------------------------------------------------------\n" +
				"exec sp_chgattribute 'dbo.table_name', 'identity_burn_max', 0, 'new-identity-value' \n" +
				"-- This command sets the identity counter to the specified new value. To avoid interference by user access, \n" +
				"-- sp_chgattribute first takes out an exclusive-table lock on the table. \n" +
				"-- While this command finally makes it easy to fix identity gaps, note the following: sp_chgattribute actually checks whether there is a row in the table \n" + 
				"-- with a higher identity column value than the new value for the identify counter. When such a higher value exists (which is likely when you're repairing \n" +
				"-- an identity gap), sp_chgattribute refuses to change the identity counter in order to protect you against the risk of generating duplicate identity column \n" +
				"-- value at some point in the future. \n" + 
				"-- \n" + 
				"-- Should this check-for-a-higher-value be a problem, it can be bypassed by running the following dbcc command directly \n" +
				"--     dbcc set_identity_burn_max('database_name', 'table_name', 'new-identity-value') \n" +
				"-- \n" + 
				"------------------------------------------------------------------------------------------------\n" +
				"-- Note: it's probably a good idea to also change the 'identity_gap' at a table level so we don't check-out a large chunk \n" +
				"--     exec sp_chgattribute 'dbo.table_name', 'identity_gap', 1000 \n" +
				"-- \n" + 
				"-- Use below sql to generate 'sp_chgattribute' for all tables that has a identity field\n" +
				"--     select 'exec sp_chgattribute ''' + user_name(so.uid) + '.' + so.name + ''', ''identity_gap'', 1000' \n" +
				"--     from  sysobjects so \n" +
				"--     where so.sysstat2 & 64  = 64   -- table has identity column \n" +
				"--     order by 1 \n" +
				"-- \n" + 
				"------------------------------------------------------------------------------------------------\n" +
				"-- To check what next value of an identity indsert will be use: \n" +
				"--     select next_identity('dbo.table_name') \n" +
				"-- \n" + 
				"-- Use below sql to get next identity values for all tables that has identity fields\n" +
				"--     select dbname            = db_name() \n" +
				"--           ,owner             = user_name(o.uid) \n" +
				"--           ,tablename         = o.name \n" +
				"--           ,identity_col      = c.name \n" +
				"--           ,next_identity_val = convert(numeric,next_identity(user_name(o.uid)+'.'+o.name)) \n" +
				"--           ,tab_row_count     = row_count(db_id(), o.id) \n" +
				"--     from  sysobjects o inner join syscolumns c on o.id = c.id \n" +
				"--     where o.sysstat2 & 64  = 64   -- table has identity column \n" +
				"--       and c.status   & 128 = 128  -- column is an identity column \n" +
				"--     order by 1, 2, 3 \n" +
				"-- \n" + 
				"------------------------------------------------------------------------------------------------\n" +
				"-- Below might be usable to figgure out the id<->next_identity difference: next_identity('dbo.table_name')-1 - MAX(id) \n" +
				"-- select 'select ' \n" +
				"--      + '   tab_name          = ''' + user_name(o.uid) + '.' + o.name + '''' \n" +
				"--      + '  ,id_gap            = (convert(numeric, next_identity(''' + user_name(o.uid) + '.' + o.name + '''))-1) - max('+c.name+')' \n" +
				"--      + '  ,max_tab_id_val    = max('+c.name+') ' \n" +
				"--      + '  ,next_identity_val = convert(numeric, next_identity(''' + user_name(o.uid) + '.' + o.name + '''))' \n" +
				"--      + '  ,tab_row_count     = row_count(db_id(), object_id(''' + user_name(o.uid) + '.' + o.name + ''')) ' \n" +
				"--      + 'from '+ user_name(o.uid) + '.' + o.name \n" +
				"-- from  sysobjects o inner join syscolumns c on o.id = c.id  \n" +
				"-- where o.sysstat2 & 64  = 64   -- table has identity column  \n" +
				"--   and c.status   & 128 = 128  -- column is an identity column  \n" +
				"-- order by user_name(o.uid), o.name \n" +
				"-- \n" + 
				"------------------------------------------------------------------------------------------------\n" +
				"-- To insert or update hard coded identity values use the following: \n" +
				"--     set identity_insert dbo.table_name on|off \n" +
				"--     set identity_update dbo.table_name on|off \n" +
				"-- \n" +
				"",
				"set identity identity_burn_max"));

		return list;
	}
	/**
	 * REFRESH miscellaneous
	 */
	@Override
	protected void refreshCompletionForMisc(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForMisc()");
		if (waitDialog.wasCancelPressed())
			return;

		// get some basic things
		super.refreshCompletionForMisc(conn, waitDialog);

		// Obtain a DatabaseMetaData object from our current connection        
//		DatabaseMetaData dbmd = conn.getMetaData();

		boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);
		long srvVersionNum  = AseConnectionUtils.getAseVersionNumber(conn);

		// if ASE go and get monTables description
		if (CollectionUtils.isNullOrEmpty(_aseMonTableDesc) && hasMonRole)
		{
			waitDialog.setState("Getting MDA Table Description information");

			_aseMonTableDesc = new HashMap<String, String>();
			String sql = "select TableName, Description from master.dbo.monTables ";
//			if (srvVersionNum >= 15700)
//			if (srvVersionNum >= 1570000)
			if (srvVersionNum >= Ver.ver(15,7))
				sql += " where Language = 'en_US' ";

			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					String tabName = StringUtils.trim(rs.getString(1));
					String tabDesc = StringUtils.trim(rs.getString(2));
//						System.out.println("_aseMonTableDesc.put('"+tabName+"', '"+tabDesc+"')");
					_aseMonTableDesc.put(tabName, tabDesc);

					if (waitDialog.wasCancelPressed())
						return;
				}
				rs.close();
				stmnt.close();
			}
			catch (SQLException sqle)
			{
				_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
			}
		}
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH database list
	 */
	@Override
	protected List<DbInfo> refreshCompletionForDbs(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		return refreshCompletionForDbs(conn, waitDialog, false);
	}

	protected List<DbInfo> refreshCompletionForDbs(Connection conn, WaitForExecDialog waitDialog, boolean simple)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForDbs()");
		waitDialog.setState("Getting ASE Database information");

		// Check if we need to do "big" refresh, or if we can keep current dictionary
		boolean quickCheck = true;
		if (quickCheck && _dbInfoList != null)
		{
			String sql = "select count(*) from master..sysdatabases";

			int dbCount = 0;
			
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
				dbCount = rs.getInt(1);
			rs.close();
			stmnt.close();

			// If Count number of databases hasn't change, then we don't need to refresh this again.
			if (dbCount == _dbInfoList.size())
				return _dbInfoList;
		}


		List<DbInfo> dbInfoList = new ArrayList<DbInfo>();

		String sql = "exec sp_helpdb";

		if (simple)
			sql = "select name, db_size='unknown', dbid, owner=suser_name(suid), created=crdate, status='unknown' from master.dbo.sysdatabases";

		try
		{
    		Statement stmnt = conn.createStatement();
    		ResultSet rs = stmnt.executeQuery(sql);
    		while(rs.next())
    		{
    			DbInfo di = new DbInfo();
    
    			di._dbName     = StringUtils.trim(rs.getString("name"));
    			di._dbSize     = StringUtils.trim(rs.getString("db_size"));
    			di._dbId       =                  rs.getInt   ("dbid");
    			di._dbOwner    = StringUtils.trim(rs.getString("owner"));
    			di._dbCrDate   = StringUtils.trim(rs.getString("created"));
    			di._dbType     = "User Database";
    			di._dbRemark   = StringUtils.trim(rs.getString("status"));
    
    			if      (di._dbName.startsWith("sybsystem")) di._dbType = "System Database";
    			else if (di._dbName.equals("master"))        di._dbType = "System Database";
    			else if (di._dbName.equals("model"))         di._dbType = "System Database";
    			else if (di._dbName.indexOf("tempdb") >= 0)  di._dbType = "Temporary Database"; // hhmm a bit ugly
    
    			dbInfoList.add(di);
    
    			if (waitDialog.wasCancelPressed())
    				return dbInfoList;
    		}
    		rs.close();
    		stmnt.close();
		}
		catch (SQLException e)
		{
			// If already in "simple" mode, lets throw the exception
			if (simple)
				throw e;

			// If we are in "full" mode, calling sp_helpdb and we FAILED, try to do it in the "simple" way...
			return refreshCompletionForDbs(conn, waitDialog, true);
		}

		return dbInfoList;
	}


	//##############################################################################
	//##############################################################################
	//##############################################################################
//	/**
//	 * REFRESH schema list
//	 */
//	@Override
//	protected List<SchemaInfo> refreshCompletionForSchemas(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName)
//	throws SQLException
//	{
//		final String stateMsg = "Getting Schema information";
//		waitDialog.setState(stateMsg);
//
//		ArrayList<SchemaInfo> schemaInfoList = new ArrayList<SchemaInfo>();
//
//		String catalog = "";
//		if (catalogName != null)
//			catalog = catalogName + ".dbo.";
//		
//		if (schemaName != null)
//		{
//			schemaName = schemaName.replace('*', '%').trim();
//			if ( ! schemaName.endsWith("%") )
//				schemaName += "%";
//		}
//
//		String sql = "select name from "+catalog+"sysusers where suid > 0 and name like '"+schemaName+"'";
//
//		Statement stmnt = conn.createStatement();
//		ResultSet rs = stmnt.executeQuery(sql);
//		while(rs.next())
//		{
//			SchemaInfo si = new SchemaInfo();
//
//			si._cat  = StringUtil.isNullOrBlank(catalogName) ? _currentCatalog :  catalogName;
//			si._name = rs.getString(1);
//
//			schemaInfoList.add(si);
//
//			if (waitDialog.wasCancelPressed())
//				return schemaInfoList;
//		}
//		rs.close();
//		stmnt.close();
//
//		return schemaInfoList;
//	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH table list
	 */
	@Override
	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		// Let the SUPER do most of the job
		List<TableInfo> tableInfoList = super.refreshCompletionForTables(conn, waitDialog);

		waitDialog.setState("Getting Table information, for ASE Tables");

		for (TableInfo ti : tableInfoList)
		{
			// if ASE, dbname=master, tabname=mon*
			// Replace Remark with description from MDA description
			if (_aseMonTableDesc != null)
			{
				if ("master".equals(ti._tabCat) && ti._tabName.startsWith("mon"))
				{
					String tabDesc = _aseMonTableDesc.get(ti._tabName);

					ti._tabType   = "MDA Table";
					ti._tabRemark = tabDesc;
				}
			}
		}

		return tableInfoList;
	}

	@Override
	protected void enrichCompletionForTables(Connection conn, WaitForExecDialog waitDialog) 
	throws SQLException
	{
		// If Column lookup is enabled, no need to do this here
		if ( isLookupTableColumns() )
			return;

		List<TableInfo> mdaTableInfoList = new ArrayList<TableInfo>();

		waitDialog.setState("Getting Table Column information, for ASE MDA Tables");

		for (TableInfo ti : _tableInfoList)
		{
			if ("master".equals(ti._tabCat) && ti._tabName.startsWith("mon"))
				if (mdaTableInfoList != null)
					mdaTableInfoList.add(ti);
		}

// I don't think the below is needed anymore... the column lookup will be done "later on"... so lets NOT waste time to do it here
//		// Get column description for MDA Tables (even if the Column check is disabled), it's not "that" many rows...
//		if (mdaTableInfoList != null && mdaTableInfoList.size() > 0)
//			refreshCompletionForTableColumns(conn, waitDialog, mdaTableInfoList, false);
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH table columns list
	 */
	@Override
	protected void refreshCompletionForTableColumns(Connection conn, WaitForExecDialog waitDialog, List<TableInfo> tableInfoList, boolean bulkGetColumns)
	throws SQLException
	{
		boolean letSuperDoMostWork = true;

		if ( letSuperDoMostWork )
		{
//System.out.println("ASE:refreshCompletionForTableColumns(): letSuperDoMostWork=TRUE");
			// Let the SUPER do most of the job
			super.refreshCompletionForTableColumns(conn, waitDialog, tableInfoList, bulkGetColumns);
		}
		else
		{
//System.out.println("ASE:refreshCompletionForTableColumns(): tableInfoList.size()="+tableInfoList.size()+", bulkGetColumns="+bulkGetColumns);
			waitDialog.setState("Getting ASE Table Column information");

			// FIXME: loop all tableInfoList to get databases, add the database names to a list, loop the db.list and execute below SQL.
			//        but below SQL also needs to be changed to be prefixed with the database names.
			String sql =
				"select \n" +
				"    objectName = o.name,     \n" +
				"    colName    = c.name,     \n" +
				"    colPos     = c.colid,    \n" +
				"    colType    = t.name,     \n" +
//				"    colType2   = d.type_name,\n" +
				"    length     = c.length,   \n" +
				"    scale      = c.scale,    \n" +
				"    nullable   = convert(smallint, convert(bit, c.status & 8)),  \n" +
				"    xxx        = 'END'       \n" +
//				"from sysobjects o, syscolumns c, systypes t, sybsystemprocs.dbo.spt_datatype_info d  \n" +
				"from sysobjects o, syscolumns c, systypes t  \n" +
				"where 1=1  \n" +
				"  and o.type     in('U', 'V', 'S')  \n" + // User, View, System
				"  and o.id       = c.id \n" +
				"  and c.usertype = t.usertype  \n" +
//				"  and t.type     = d.ss_dtype  \n" +
				"order by 1, 3";
	
			String prevTabName = "";
			TableInfo tabInfo = null;
	
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				int c=1;
				String tabName = StringUtils.trim(rs.getString(c++));
	
				TableColumnInfo ci = new TableColumnInfo();
				ci._colName       = StringUtils.trim(rs.getString(c++));
				ci._colPos        =                  rs.getInt   (c++);
				ci._colType       = StringUtils.trim(rs.getString(c++));
//				ci._colType2      = StringUtils.trim(rs.getString(c++);
				ci._colLength     =                  rs.getInt   (c++);
				ci._colScale      =                  rs.getInt   (c++);
				ci._colIsNullable =                  rs.getInt   (c++);
	//			ci._colRemark     = StringUtils.trim(rs.getString());
	//			ci._colDefault    = StringUtils.trim(rs.getString());

//				// Get "real" datatype (if it was a user defined type)
//				if ( ! ci._colType.equals(ci._colType2) )
//					ci._colType = ci._colType + " ("+ci._colType2+")";

				if ( ! prevTabName.equals(tabName) )
				{
					prevTabName = tabName;
					tabInfo = getTableInfo(tabName);
				}
				if (tabInfo == null)
					continue;
	
				tabInfo.addColumn(ci);
	
				if (waitDialog.wasCancelPressed())
					return;
			}
			rs.close();
			stmnt.close();
		}


		//----------------------------------------------
		// get MDA table columns descriptions
		//----------------------------------------------
		// if ASE, check if we have monrole and get MDA Descriptions
		boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);
		long srvVersionNum  = AseConnectionUtils.getAseVersionNumber(conn);

		if (hasMonRole)
		{
			boolean doBulkRefresh = false;

			// Check if we have any MDA tables that we need to get description for
			for (TableInfo ti : tableInfoList)
			{
				// update DESCRIPTION for MDA Columns
				// is ASE, db=master, table mon*
				if ("master".equals(ti._tabCat) && ti._tabName.startsWith("mon") )
					doBulkRefresh = true;
			}

			if (doBulkRefresh)
			{
				waitDialog.setState("Getting MDA Column Descriptions.");
				
				String sql = "select TableName, ColumnName, Description from master.dbo.monTableColumns ";
//				if (srvVersionNum >= 15700)
//				if (srvVersionNum >= 1570000)
				if (srvVersionNum >= Ver.ver(15,7))
					sql += " where Language = 'en_US' ";

				try
				{
					String prevTabName = "";
					TableInfo tabInfo = null;

					Statement stmnt = conn.createStatement();
					ResultSet rs = stmnt.executeQuery(sql);
					while(rs.next())
					{
						String tabName = StringUtils.trim(rs.getString(1));
						String colName = StringUtils.trim(rs.getString(2));
						String colDesc = StringUtils.trim(rs.getString(3));

						if ( ! prevTabName.equals(tabName) )
						{
							prevTabName = tabName;
							tabInfo = getTableInfo(tabName);
						}
						if (tabInfo == null)
							continue;
			
						TableColumnInfo ci = tabInfo.getColumnInfo(colName);
						if (ci != null)
							ci._colRemark = colDesc;

						if (waitDialog.wasCancelPressed())
							return;
					}
					rs.close();
					stmnt.close();
				}
				catch (SQLException sqle)
				{
					_logger.info("Problems when getting ASE monTableColumns dictionary, skipping this and continuing. Caught: "+sqle);
				}
			} // end: bulkRefresh
		} // end: hasMonRole
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH procedure list
	 */
	@Override
	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		return refreshCompletionForProcedures(conn, waitDialog, null, null, null, isWildcatdMath());
	}
	@Override
	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String procName, boolean wildcardSearch)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForProcedures()");
		waitDialog.setState("Getting Procedure information");

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		String catColSql = "db_name()";
		if (catalogName == null)
			catalogName = "";
		else
		{
			catColSql = "'"+catalogName+"'"; // simply use the input parameter as the result
			catalogName += "..";
		}
		
		String schColSql = "user_name(uid)";
		if (schemaName == null)
			schemaName = "";
		else
		{
			schColSql = "'"+schemaName+"'"; // simply use the input parameter as the result
//			schemaName = " and uid = user_id('"+schemaName+"')";
			schemaName = " and uid = (select uid from "+catalogName+"sysusers where name = '"+schemaName+"')";
		}
		
		if (procName == null)
			procName = "";
		else
		{
			procName = procName.replace('*', '%').trim();
//			if ( ! procName.endsWith("%") )
			if (wildcardSearch && ! procName.endsWith("%") )
				procName += "%";
			procName = " and name like '"+procName+"'";
		}

//		String sql = "select db_name(), user_name(uid), name from "+catalogName+"sysobjects where type in('P', 'SF') " + procName; // SF = Function
		String sql = "select "+catColSql+", "+schColSql+", name from "+catalogName+"sysobjects where type in('P') " + procName + schemaName;
//System.out.println("ASE: refreshCompletionForProcedures() SQL: "+sql);

		Statement stmnt = conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while(rs.next())
		{
			ProcedureInfo pi = new ProcedureInfo();
			pi._procCat     = StringUtils.trim(rs.getString(1));
			pi._procSchema  = StringUtils.trim(rs.getString(2));
			pi._procName    = StringUtils.trim(rs.getString(3));
			pi._procType    = "Procedure";
			pi._procRemark  = "";

			procInfoList.add(pi);

			if (waitDialog.wasCancelPressed())
				return procInfoList;
		}
		rs.close();
		stmnt.close();

//System.out.println("ASE: refreshCompletionForProcedures(). fetched "+procInfoList.size()+" USER procedures");
		return procInfoList;
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH procedure parameters
	 */
	@Override
	protected void refreshCompletionForProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList, boolean bulkMode)
	throws SQLException
	{
		boolean letSuperDoMostWork = true;

		if ( letSuperDoMostWork )
		{
//System.out.println("ASE:refreshCompletionForProcedureParameters(): letSuperDoMostWork=TRUE");
			// Let the SUPER do most of the job
			super.refreshCompletionForProcedureParameters(conn, waitDialog, procedureInfoList, bulkMode);
		}
		else
		{
//System.out.println("ASE: refreshCompletionForProcedureParameters()");
//System.out.println("refreshCompletionForProcedureParameters(): procedureInfoList.size()="+procedureInfoList.size()+", bulkMode="+bulkMode);
			waitDialog.setState("Getting Procedure Parameter information");
	
			String sql =
				"select \n" +
				"    objectName = o.name,     \n" +
				"    colName    = c.name,     \n" +
				"    colPos     = c.colid,    \n" +
				"    colType    = t.name,     \n" +
//				"    colType2   = d.type_name,\n" +
				"    length     = c.length,   \n" +
				"    scale      = c.scale,    \n" +
				"    nullable   = convert(smallint, convert(bit, c.status & 8)),  \n" +
				"    xxx        = 'END'       \n" +
//				"from sysobjects o, syscolumns c, systypes t, sybsystemprocs.dbo.spt_datatype_info d  \n" +
				"from sysobjects o, syscolumns c, systypes t  \n" +
				"where 1=1  \n" +
				"  and o.type     = 'P'  \n" +
				"  and o.id       = c.id \n" +
				"  and c.usertype = t.usertype  \n" +
//				"  and t.type     = d.ss_dtype  \n" +
				"order by 1, 3";
	
			String prevProcName = "";
			ProcedureInfo procInfo = null;
	
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				int c=1;
				String procName = StringUtils.trim(rs.getString(c++));
	
				ProcedureParameterInfo pi = new ProcedureParameterInfo();
				pi._paramName       = StringUtils.trim(rs.getString(c++));
				pi._paramPos        = rs.getInt   (c++);
				pi._paramType       = StringUtils.trim(rs.getString(c++));
//				pi._paramType2      = StringUtils.trim(rs.getString(c++));
				pi._paramLength     = rs.getInt   (c++);
				pi._paramScale      = rs.getInt   (c++);
				pi._paramIsNullable = rs.getInt   (c++);
//				pi._paramRemark     = StringUtils.trim(rs.getString());
//				pi._paramDefault    = StringUtils.trim(rs.getString());
	
//				// Get "real" datatype (if it was a user defined type)
//				if ( ! pi._paramType.equals(pi._paramType2) )
//					pi._paramType = pi._paramType + " ("+pi._paramType2+")";
	
				if ( ! prevProcName.equals(procName) )
				{
					prevProcName = procName;
					procInfo = getProcedureInfo(procName);
				}
				if (procInfo == null)
					continue;
	
				procInfo.addParameter(pi);
	
				if (waitDialog.wasCancelPressed())
					return;
			}
			rs.close();
			stmnt.close();
	
			return;
//			// ADD Column information
//			for (ProcedureInfo pi : procedureInfoList)
//			{
//				if (waitDialog.wasCancelPressed())
//					return;
//	
//				waitDialog.setState("Getting Parameter information for Procedure '"+pi._procName+"'.");
//			}
		}
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH system procedure list
	 */
	@Override
	protected List<ProcedureInfo> refreshCompletionForSystemProcedures(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedures()");
		waitDialog.setState("Getting Procedure information");

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		String sql = 
//			"select db_name(), user_name(uid), name from         master.dbo.sysobjects where type = 'P' and name like 'sp_%' \n" +
//			"union \n" +
			"select db_name(), user_name(uid), name from sybsystemprocs.dbo.sysobjects where type = 'P' and name like 'sp_%' \n" +
			"";

		Statement stmnt = conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while(rs.next())
		{
			ProcedureInfo pi = new ProcedureInfo();
			pi._procCat     = StringUtils.trim(rs.getString(1));
			pi._procSchema  = StringUtils.trim(rs.getString(2));
			pi._procName    = StringUtils.trim(rs.getString(3));
			pi._procType    = "System Procedure";
			pi._procRemark  = getSpDescription(pi._procName);

			procInfoList.add(pi);

			if (waitDialog.wasCancelPressed())
				return procInfoList;
		}
		rs.close();
		stmnt.close();

//System.out.println("ASE: refreshCompletionForSystemProcedures(). fetched "+procInfoList.size()+" SYSTEM procedures");
		return procInfoList;
	}

	//##############################################################################
	//##############################################################################
	//##############################################################################
	/**
	 * REFRESH system procedure parameters
	 */
	@Override
	protected void refreshCompletionForSystemProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedureParameters()");
		waitDialog.setState("Getting System Procedure Parameter information");

		String sql =
			"select \n" +
			"    objectName = o.name,     \n" +
			"    colName    = c.name,     \n" +
			"    colPos     = c.colid,    \n" +
			"    colType    = t.name,     \n" +
//			"    colType2   = d.type_name,\n" +
			"    length     = c.length,   \n" +
			"    scale      = c.scale,    \n" +
			"    nullable   = convert(smallint, convert(bit, c.status & 8)),  \n" +
			"    xxx        = 'END'       \n" +
//			"from sybsystemprocs.dbo.sysobjects o, sybsystemprocs.dbo.syscolumns c, sybsystemprocs.dbo.systypes t, sybsystemprocs.dbo.spt_datatype_info d  \n" +
			"from sybsystemprocs.dbo.sysobjects o, sybsystemprocs.dbo.syscolumns c, sybsystemprocs.dbo.systypes t  \n" +
			"where 1=1  \n" +
			"  and o.type     = 'P'  \n" +
			"  and o.id       = c.id \n" +
			"  and c.usertype = t.usertype\n" +
//			"  and t.type     = d.ss_dtype  \n" +
			"order by 1, 3";

		String prevProcName = "";
		ProcedureInfo procInfo = null;

		Statement stmnt = conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while(rs.next())
		{
			int c=1;
			String procName = StringUtils.trim(rs.getString(c++));

			ProcedureParameterInfo pi = new ProcedureParameterInfo();
			pi._paramName       = StringUtils.trim(rs.getString(c++));
			pi._paramPos        =                  rs.getInt   (c++);
			pi._paramType       = StringUtils.trim(rs.getString(c++));
//			pi._paramType2      = StringUtils.trim(rs.getString(c++);
			pi._paramLength     =                  rs.getInt   (c++);
			pi._paramScale      =                  rs.getInt   (c++);
			pi._paramIsNullable =                  rs.getInt   (c++);
//			pi._paramRemark     = StringUtils.trim(rs.getString());
//			pi._paramDefault    = StringUtils.trim(rs.getString());

//			// Get "real" datatype (if it was a user defined type)
//			if ( ! pi._paramType.equals(pi._paramType2) )
//				pi._paramType = pi._paramType + " ("+pi._paramType2+")";

			if ( ! prevProcName.equals(procName) )
			{
				prevProcName = procName;
				procInfo = getSystemProcedureInfo(procName);
			}
			if (procInfo == null)
				continue;

			procInfo.addParameter(pi);

			if (waitDialog.wasCancelPressed())
				return;
		}
		rs.close();
		stmnt.close();

		return;
	}

	

	/**
	 * The below values are grabbed from: Adaptive Server Enterprise 15.5 > Reference Manual: Procedures > System Procedures
	 * http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc36273.1550/html/sprocs/CEGHCFJF.htm
	 */
	private static String getSpDescription(String name)
	{
		if (name.equals("sp_activeroles")) 			return "Displays all active roles granted to a user�s login.";
		if (name.equals("sp_add_qpgroup")) 			return "Adds an abstract plan group.";
		if (name.equals("sp_add_resource_limit")) 	return "Creates a limit on the amount of server resources that a login or application can use to execute a query, query batch, or transaction.";
		if (name.equals("sp_add_time_range")) 		return "Adds a named time range to Adaptive Server.";
		if (name.equals("sp_addalias")) 			return "Allows an Adaptive Server user to be known in a database as another user.";
		if (name.equals("sp_addauditrecord")) 		return "Allows users to enter user-defined audit records (comments) into the audit trail.";
		if (name.equals("sp_addaudittable")) 		return "Adds another system audit table after auditing is installed. ";
		if (name.equals("sp_addengine")) 			return "Adds an engine to an existing engine group or, if the group does not exist, creates an engine group and adds the engine.";
		if (name.equals("sp_addexeclass")) 			return "Creates or updates a user-defined execution class that you can bind to client applications, logins, and stored procedures. ";
		if (name.equals("sp_addextendedproc")) 		return "Creates an extended stored procedure (ESP) in the master database. ";
		if (name.equals("sp_addexternlogin")) 		return "Creates an alternate login account and password to use when communicating with a remote server through Component Integration Services.";
		if (name.equals("sp_addgroup")) 			return "Adds a group to a database. Groups are used as collective names in granting and revoking privileges.";
		if (name.equals("sp_addlanguage")) 			return "Defines the names of the months and days, and the date format, for an alternate language.";
		if (name.equals("sp_addlogin")) 			return "Adds a new user account to Adaptive Server.";
		if (name.equals("sp_addmessage")) 			return "Adds user-defined messages to sysusermessages for use by stored procedure print and raiserror calls and by sp_bindmsg.";
		if (name.equals("sp_addobjectdef")) 		return "Specifies the mapping between a local table and an external storage location.";
		if (name.equals("sp_addremotelogin")) 		return "Authorizes a new remote server user by adding an entry to master.dbo.sysremotelogins.";
		if (name.equals("sp_addsegment")) 			return "Defines a segment on a database device in the current database.";
		if (name.equals("sp_addserver")) 			return "Defines a remote server or defines the name of the local server.";
		if (name.equals("sp_addthreshold")) 		return "Creates a threshold to monitor space on a database segment. When free space on the segment falls below the specified level, Adaptive Server executes the associated stored procedure.";
		if (name.equals("sp_addtype")) 				return "Creates a user-defined datatype.";
		if (name.equals("sp_addumpdevice")) 		return "Adds a dump device to Adaptive Server.";
		if (name.equals("sp_adduser")) 				return "Adds a new user to the current database.";
		if (name.equals("sp_altermessage")) 		return "Enables and disables the logging of a specific system-defined or user-defined message in the Adaptive Server error log.";
		if (name.equals("sp_audit")) 				return "Allows a System Security Officer to configure auditing options.";
		if (name.equals("sp_autoconnect")) 			return "Defines a passthrough connection to a remote server for a specific user, which allows the named user to enter passthrough mode automatically at login.";
		if (name.equals("sp_autoformat")) 			return "Produces readable result set data, reformatting the width of variable-length character data to display only non-blank characters. Trailing blanks are truncated in the output.";
		if (name.equals("sp_bindcache")) 			return "Binds a database, table, index, text object, or image object to a data cache.";
		if (name.equals("sp_bindefault")) 			return "Binds a user-defined default to a column or user-defined datatype.";
		if (name.equals("sp_bindexeclass")) 		return "Associates an execution class with a client application, login, or stored procedure.";
		if (name.equals("sp_bindmsg")) 				return "Binds a user message to a referential integrity constraint or check constraint.";
		if (name.equals("sp_bindrule")) 			return "Binds a rule to a column or user-defined datatype.";
		if (name.equals("sp_cacheconfig")) 			return "Creates, configures, reconfigures, drops, and provides information about data caches.";
		if (name.equals("sp_cachestrategy")) 		return "Enables or disables prefetching (large I/O) and MRU cache replacement strategy for a table, index, text object, or image object.";
		if (name.equals("sp_changedbowner")) 		return "Changes the owner of a database. ";
		if (name.equals("sp_changegroup")) 			return "Changes a user�s group.";
		if (name.equals("sp_checknames")) 			return "Checks the current database for names that contain characters not in the 7-bit ASCII set.";
		if (name.equals("sp_checkreswords")) 		return "Detects and displays identifiers that are Transact-SQL reserved words. Checks server names, device names, database names, segment names, user-defined datatypes, object names, column names, user names, login names, and remote login names.";
		if (name.equals("sp_checksource")) 			return "Checks for the existence of the source text of the compiled object.";
		if (name.equals("sp_chgattribute")) 		return "Changes the max_rows_per_page value for future space allocations of a table or index.";
		if (name.equals("sp_clearpsexe")) 			return "Clears the execution attributes of the client application, login, or stored procedure that was set by sp_setpsexe. ";
		if (name.equals("sp_clearstats")) 			return "Initiates a new accounting period for all server users or for a specified user. Prints statistics for the previous period by executing sp_reportstats.";
		if (name.equals("sp_client_addr")) 			return "Displays the IP address of every Adaptive Server task with an attached client application, including the spid and the client host name.";
		if (name.equals("sp_clusterlockusage")) 	return "Cluster environment only Reports on the free, used, and retained locks in the cluster.";
		if (name.equals("sp_cluster")) 				return "Cluster environment only Performs various functions pertaining to a cluster environment.";
		if (name.equals("sp_cmp_all_qplans")) 		return "Compares all abstract plans in two abstract plan groups.";
		if (name.equals("sp_cmp_qplans")) 			return "Compares two abstract plans.";
		if (name.equals("sp_commonkey")) 			return "Defines a common key columns that are frequently joined between two tables or views.";
		if (name.equals("sp_companion")) 			return "Performs cluster operations such as configuring Adaptive Server as a secondary companion in a high availability system and moving a companion server from one failover mode to another";
		if (name.equals("sp_compatmode")) 			return "Verify whether full compatibility mode can be used.";
		if (name.equals("sp_configure")) 			return "Displays or changes configuration parameters.";
		if (name.equals("sp_copy_all_qplans")) 		return "Copies all plans for one abstract plan group to another group.";
		if (name.equals("sp_copy_qplan")) 			return "Copies one abstract plan to an abstract plan group.";
		if (name.equals("sp_countmetadata")) 		return "Displays the number of indexes, objects, or databases in Adaptive Server. ";
		if (name.equals("sp_cursorinfo")) 			return "Reports information about a specific cursor or all cursors that are active for your session, whether scrollable or non-scrollable.";
		if (name.equals("sp_dbextend")) 			return "Allows you to:" +
																"<ul>" +
																"    <li>Install automatic database expansion procedures on database/segment pairs and devices.</li>" +
																"    <li>Define site-specific policies for individual segments and devices.</li>" +
																"    <li>Simulate execution of the database expansion machinery, to study the operation before engaging large volume loads.</li>" +
																"</ul>";
		if (name.equals("sp_dboption")) 			return "Displays or changes database options. ";
		if (name.equals("sp_dbrecovery_order")) 	return "Specifies the order in which user databases are recovered and lists the user-defined recovery order of a database or all databases.";
		if (name.equals("sp_dbremap")) 				return "Forces Adaptive Server to recognize changes made by alter database. Run this procedure only when instructed to do so by an Adaptive Server message.";
		if (name.equals("sp_defaultloc")) 			return "Component Integration Services only Defines a default storage location for objects in a local database.";
		if (name.equals("sp_deletesmobj")) 			return "Tivoli Storage Manager license only Removes current server�s database backup objects from the IBM Tivoli Storage Manager.";
		if (name.equals("sp_depends")) 				return "Displays information about database object dependencies the view(s), trigger(s), and procedure(s) that depend on a specified table or view, and the table(s) and view(s) that the specified view, trigger, or procedure depends on.";
		if (name.equals("sp_deviceattr")) 			return "Changes the device parameter settings of an existing database device file.";
		if (name.equals("sp_diskdefault")) 			return "Specifies whether or not a database device can be used for database storage if the user does not specify a database device or specifies default with the create database or alter database commands.";
		if (name.equals("sp_displayaudit")) 		return "Displays the status of audit options. ";
		if (name.equals("sp_displaylevel")) 		return "Sets or shows which Adaptive Server configuration parameters appear in sp_configure output.";
		if (name.equals("sp_displaylogin")) 		return "Displays information about a login account.";
		if (name.equals("sp_displayroles")) 		return "Displays all roles granted to another role, or displays the entire hierarchy tree of roles in table format. ";
		if (name.equals("sp_downgrade")) 			return "Validates readiness for downgrade to an earlier 15.0.x release. Also downgrades the system catalog changes Adaptive Server 15.0.2 modified.";
		if (name.equals("sp_dropalias")) 			return "Removes the alias user name identity established with sp_addalias.";
		if (name.equals("sp_drop_all_qplans")) 		return "Deletes all abstract plans in an abstract plan group.";
		if (name.equals("sp_drop_qplan")) 			return "Drops an abstract plan.";
		if (name.equals("sp_drop_resource_limit")) 	return "Removes one or more resource limits from Adaptive Server.";
		if (name.equals("sp_drop_time_range")) 		return "Removes a user-defined time range from Adaptive Server.";
		if (name.equals("sp_dropdevice")) 			return "Drops an Adaptive Server database device or dump device.";
		if (name.equals("sp_dropengine")) 			return "Drops an engine from a specified engine group or, if the engine is the last one in the group, drops the engine group.";
		if (name.equals("sp_dropexeclass")) 		return "Drops a user-defined execution class.";
		if (name.equals("sp_dropextendedproc")) 	return "Removes an ESP from the master database.";
		if (name.equals("sp_dropexternlogin")) 		return "Component Integration Services only Drops the definition of a remote login previously defined by sp_addexternlogin.";
		if (name.equals("sp_dropglockpromote")) 	return "Removes lock promotion values from a table or database.";
		if (name.equals("sp_dropgroup")) 			return "Drops a group from a database.";
		if (name.equals("sp_dropkey")) 				return "Removes a key defined with sp_primarykey, sp_foreignkey, or sp_commonkey from the syskeys table.";
		if (name.equals("sp_droplanguage")) 		return "Drops an alternate language from the server and removes its row from master.dbo.syslanguages.";
		if (name.equals("sp_droplogin")) 			return "Drops an Adaptive Server user login by deleting the user�s entry in master.dbo.syslogins.";
		if (name.equals("sp_dropmessage")) 			return "Drops user-defined messages from sysusermessages.";
		if (name.equals("sp_dropobjectdef")) 		return "Component Integration Services only Deletes the external storage mapping provided for a local object";
		if (name.equals("sp_dropremotelogin")) 		return "Drops a remote user login.";
		if (name.equals("sp_droprowlockpromote")) 	return "Removes row lock promotion threshold values from a database or table.";
		if (name.equals("sp_dropsegment")) 			return "Drops a segment from a database or unmaps a segment from a particular database device.";
		if (name.equals("sp_dropserver")) 			return "Drops a server from the list of known servers.";
		if (name.equals("sp_dropthreshold")) 		return "Removes a free-space threshold from a segment.";
		if (name.equals("sp_droptype")) 			return "Drops a user-defined datatype.";
		if (name.equals("sp_dropuser")) 			return "Drops a user from the current database.";
		if (name.equals("sp_dumpoptimize")) 		return "Specifies the amount of data dumped by Backup Server during the dump database operation.";
		if (name.equals("sp_encryption")) 			return "Reports encryption information.";
		if (name.equals("sp_engine")) 				return "Enables you to bring an engine online or offline.";
		if (name.equals("sp_estspace")) 			return "Estimates the amount of space required for a table and its indexes, and the time needed to create the index.";
		if (name.equals("sp_export_qpgroup")) 		return "Exports all plans for a specified user and abstract plan group to a user table.";
		if (name.equals("sp_extendsegment")) 		return "Extends the range of a segment to another database device.";
		if (name.equals("sp_extengine")) 			return "Starts and stops EJB Server. Displays status information about EJB Server.";
		if (name.equals("sp_extrapwdchecks")) 		return "A custom stored procedure that can contain user-defined logic for password complexity checks. You can configure sp_extrapwdchecks according to your security needs. Install sp_extrapwdchecks in the master database.";
		if (name.equals("sp_familylock")) 			return "Reports information about all the locks held by a family (coordinating process and its worker processes) executing a statement in parallel. ";
		if (name.equals("sp_find_qplan")) 			return "Finds an abstract plan, given a pattern from the query text or plan text.";
		if (name.equals("sp_fixindex")) 			return "Repairs the index on one of your system tables when it has been corrupted.";
		if (name.equals("sp_flushstats")) 			return "Flushes statistics from in-memory storage to the systabstats system table. ";
		if (name.equals("sp_forceonline_db")) 		return "Provides access to all the pages in a database that were previously taken offline by recovery.";
		if (name.equals("sp_forceonline_object")) 	return "Provides access to an index previously marked suspect by recovery.";
		if (name.equals("sp_forceonline_page")) 	return "Provides access to pages previously taken offline by recovery.";
		if (name.equals("sp_foreignkey")) 			return "Defines a foreign key on a table or view in the current database.";
		if (name.equals("sp_freedll")) 				return "Unloads a dynamic link library (DLL) that was previously loaded into XP Server memory to support the execution of an ESP.";
		if (name.equals("sp_getmessage")) 			return "Retrieves stored message strings from sysmessages and sysusermessages for print and raiserror statements.";
		if (name.equals("sp_grantlogin")) 			return "Windows NT only When Integrated Security mode or Mixed mode (with Named Pipes) is active, assigns Adaptive Server roles or default permissions to Windows NT users and groups.";
		if (name.equals("sp_ha_admin")) 			return "Performs administrative tasks on Adaptive Servers configured with Sybase Failover in a high availability system. sp_ha_admin is installed with the installhavss script (insthasv on Windows NT).";
		if (name.equals("sp_help")) 				return "Reports information about a database object (any object listed in sysobjects) and about Adaptive Server-supplied or user-defined datatypes.";
		if (name.equals("sp_help_resource_limit")) 	return "Reports information about all resource limits, limits for a given login or application, limits in effect at a given time or day of the week, or limits with a given scope or action.";
		if (name.equals("sp_help_qpgroup")) 		return "Reports information on an abstract plan group.";
		if (name.equals("sp_help_qplan")) 			return "Reports information about an abstract plan.";
		if (name.equals("sp_helpapptrace")) 		return "Determines which sessions Adaptive Server is tracing. sp_helpapptrace returns the server process IDs (spids) for all the sessions Adaptive Server is tracing, the spids of the sessions tracing them, and the name of the tracefile.";
		if (name.equals("sp_helpartition")) 		return "Lists partition information for a specified table, index, or partition, or for all partitions in the database.";
		if (name.equals("sp_helpcache")) 			return "Displays information about the objects that are bound to a data cache or the amount of overhead required for a specified cache size.";
		if (name.equals("sp_helpcomputedcolumn")) 	return "Reports information on the computed columns in a specified table.";
		if (name.equals("sp_helpconfig")) 			return "Reports help information on configuration parameters.";
		if (name.equals("sp_helpconstraint")) 		return "Reports information about integrity constraints used in the specified tables.";
		if (name.equals("sp_helpdb")) 				return "Reports information about a particular database or about all databases.";
		if (name.equals("sp_helpdevice")) 			return "Reports information about a particular device or about all Adaptive Server database devices and dump devices.";
		if (name.equals("sp_helpextendedproc")) 	return "Displays ESPs registered in the current database, along with their associated DLL files.";
		if (name.equals("sp_helpexternlogin")) 		return "Component Integration Services only Reports information about external login names.";
		if (name.equals("sp_helpgroup")) 			return "Reports information about a particular group or about all groups in the current database.";
		if (name.equals("sp_helpindex")) 			return "Reports information about the indexes created on a table.";
		if (name.equals("sp_helpjava")) 			return "Displays information about Java classes and associated JARs that are installed in the database.";
		if (name.equals("sp_helpjoins")) 			return "Lists the columns in two tables or views that are likely join candidates.";
		if (name.equals("sp_helpkey")) 				return "Reports information about a primary, foreign, or common key of a particular table or view, or about all keys in the current database.";
		if (name.equals("sp_helplanguage")) 		return "Reports information about a particular alternate language or about all languages.";
		if (name.equals("sp_helplog")) 				return "Reports the name of the device that contains the first page of the transaction log.";
		if (name.equals("sp_helpobjectdef")) 		return "Component Integration Services only Reports information about remote object definitions. Shows owners, objects, type, and definition.";
		if (name.equals("sp_helpremotelogin")) 		return "Reports information about a particular remote server�s logins or about all remote servers� logins.";
		if (name.equals("sp_helprotect")) 			return "Reports information about permissions for database objects, users, groups, or roles.";
		if (name.equals("sp_helpsegment")) 			return "Reports information about a particular segment or about all segments in the current database.";
		if (name.equals("sp_helpserver")) 			return "Reports information about a particular remote server or about all remote servers.";
		if (name.equals("sp_helpsort")) 			return "Displays Adaptive Server�s default sort order and character set.";
		if (name.equals("sp_helptext")) 			return "Prints the text of a system procedure, trigger, view, default, rule, or integrity check constraint, and adds the number parameter, which is an integer identifying an individual procedure, when objname represents a group of procedures. This parameter tells sp_helptext to display the source text for a specified procedure in the group.";
		if (name.equals("sp_helpthreshold")) 		return "Reports the segment, free-space value, status, and stored procedure associated with all thresholds in the current database or all thresholds for a particular segment.";
		if (name.equals("sp_helpuser")) 			return "Reports information about a particular user or about all users in the current database.";
		if (name.equals("sp_hidetext")) 			return "Hides the source text for the specified compiled object.";
		if (name.equals("sp_import_qpgroup")) 		return "Imports abstract plans from a user table into an abstract plan group.";
		if (name.equals("sp_indsuspect")) 			return "Checks user tables for indexes marked as suspect during recovery following a sort order change.";
		if (name.equals("sp_jreconfig")) 			return "Manages the Java PCA/JVM, enabling arguments, directives, and configuration values, and changes, reports for sp_jreconfig and sp_pciconfig.";
		if (name.equals("sp_ldapadmin")) 			return "Creates or lists an LDAP URL search string; verifies an LDAP URL search string or login; specifies the access accounts and tunable LDAPUA-rel;ated parameters..";
		if (name.equals("sp_listener")) 			return "Dynamically starts and stops listeners on Adaptive Server on any given port on a per-engine basis.";
		if (name.equals("sp_listsuspect_db")) 		return "Lists all databases that have offline pages because of corruption detected on recovery.";
		if (name.equals("sp_listsuspect_object")) 	return "Lists all indexes in a database that are currently offline because of corruption detected on recovery.";
		if (name.equals("sp_listsuspect_page")) 	return "Lists all pages that are currently offline because of corruption detected on recovery.";
		if (name.equals("sp_lmconfig")) 			return "Configures license management-related information on Adaptive Server. The configuration options set by sp_lmconfig are stored in the sylapi properties file.";
		if (name.equals("sp_lock")) 				return "Reports information about processes that currently hold locks.";
		if (name.equals("sp_locklogin")) 			return "Locks an Adaptive Server account so that the user cannot log in, or displays a list of all locked accounts.";
		if (name.equals("sp_logdevice")) 			return "Moves the transaction log of a database with log and data on the same device to a separate database device.";
		if (name.equals("sp_loginconfig")) 			return "Windows NT only Displays the value of one or all integrated security parameters.";
		if (name.equals("sp_logininfo")) 			return "Windows NT only Displays all roles granted to Windows NT users and groups with sp_grantlogin.";
		if (name.equals("sp_logiosize")) 			return "Changes the log I/O size used by Adaptive Server to a different memory pool when it is doing I/O for the transaction log of the current database.";
		if (name.equals("sp_logintrigger")) 		return "Sets and displays the global login trigger. This global login trigger has the same characteristics as a personal login script. It is executed before any personal login script for every user that tries to log in, including system administrators and security officers.";
		if (name.equals("sp_maplogin")) 			return "Maps external users to Adaptive Server logins.";
		if (name.equals("sp_metrics")) 				return "Backs up, drops, and flushes QP metrics always captured in the default running group, which is group 1 in each respective database and their statistics on queries.";
		if (name.equals("sp_modify_resource_limit")) return "Changes a resource limit by specifying a new limit value or the action to take when the limit is exceeded, or both.";
		if (name.equals("sp_modify_time_range")) 	return "Changes the start day, start time, end day, and/or end time associated with a named time range.";
		if (name.equals("sp_modifylogin")) 			return "Modifies the default database, default language, default role activation, or full name for an Adaptive Server login account.";
		if (name.equals("sp_modifystats")) 			return "Allows the System Administrator to modify the density values of a column or columns in sysstatistics.";
		if (name.equals("sp_modifythreshold")) 		return "Modifies a threshold by associating it with a different threshold procedure, free-space level, or segment name. You cannot use sp_modifythreshold to change the amount of free space or the segment name for the last-chance threshold.";
		if (name.equals("sp_monitor")) 				return "Displays statistics about Adaptive Server.";
		if (name.equals("sp_monitorconfig")) 		return "Monitors more than 30 resources compared to the 6 resources it monitored in earlier versions.";
		if (name.equals("sp_object_stats")) 		return "Shows lock contention, lock wait-time, and deadlock statistics for tables and indexes.";
		if (name.equals("sp_options")) 				return "Show option values.";
		if (name.equals("sp_passthru")) 			return "Component Integration Services only Allows the user to pass a SQL command buffer to a remote server.";
		if (name.equals("sp_password")) 			return "Adds or changes a password for an Adaptive Server login account.";
		if (name.equals("sp_passwordpolicy")) 		return "An interface that a user with sso_role can use to configure login and password policies. This information is stored in the master.dbo.sysattributes table.";
		if (name.equals("sp_pciconfig")) 			return "Manages the Java PCI Bridge, enabling arguments, directives, and configuration values.";
		if (name.equals("sp_placeobject")) 			return "Puts future space allocations for a table or an index on a particular segment.";
		if (name.equals("sp_plan_dbccdb")) 			return "Recommends suitable sizes for new dbccdb and dbccalt databases, lists suitable devices for dbccdb and dbccalt, and suggests a cache size and a suitable number of worker processes for the target database.";
		if (name.equals("sp_poolconfig")) 			return "Creates, drops, resizes, and provides information about memory pools within data caches.";
		if (name.equals("sp_post_xpload")) 			return "Checks and rebuilds indexes after a cross-platform load database where the endian types are different.";
		if (name.equals("sp_primarykey")) 			return "Defines a primary key on a table or view.";
		if (name.equals("sp_processmail")) 			return "Windows NT only Reads, processes, sends, and deletes messages in the Adaptive Server message inbox.";
		if (name.equals("sp_procxmode")) 			return "Displays or changes the transaction modes associated with stored procedures.";
		if (name.equals("sp_querysmobj")) 			return "Tivoli Storage Manager license only Queries the Tivoli Storage Manager for a list of the current server�s database backup objects.";
		if (name.equals("sp_recompile")) 			return "Causes each stored procedure and trigger that uses the named table to be recompiled the next time it runs.";
		if (name.equals("sp_refit_admin")) 			return "Cluster environment only Provides an interface to perform disk refit-related actions.";
		if (name.equals("sp_remap")) 				return "Remaps a stored procedure, trigger, rule, default, or view from releases later than 4.8 and earlier than 10.0 to be compatible with releases 10.0 and later. Use sp_remap on pre-release 11.0 objects that the release 11.0 upgrade procedure failed to remap.";
		if (name.equals("sp_remoteoption")) 		return "Displays or changes remote login options.";
		if (name.equals("sp_remotesql")) 			return "Component Integration Services only Establishes a connection to a remote server, passes a query buffer to the remote server from the client, and relays the results back to the client.";
		if (name.equals("sp_rename")) 				return "Changes the name of a user-created object or user-defined datatype in the current database.";
		if (name.equals("sp_rename_qpgroup")) 		return "Renames an abstract plan group.";
		if (name.equals("sp_renamedb")) 			return "Changes the name of a database. You cannot rename system databases or databases with external referential integrity constraints.";
		if (name.equals("sp_reportstats")) 			return "Reports statistics on system usage.";
		if (name.equals("sp_revokelogin")) 			return "Windows NT only When Integrated Security mode or Mixed mode (with Named Pipes) is active, revokes Adaptive Server roles and default permissions from Windows NT users and groups.";
		if (name.equals("sp_role")) 				return "Grants or revokes system roles to an Adaptive Server login account.";
		if (name.equals("sp_sendmsg")) 				return "Sends a message to a User Datagram Protocol (UDP) port.";
		if (name.equals("sp_serveroption")) 		return "Displays or changes remote server options.";
		if (name.equals("sp_set_qplan")) 			return "Changes the text of the abstract plan of an existing plan without changing the associated query.";
		if (name.equals("sp_setlangalias")) 		return "Assigns or changes the alias for an alternate language.";
		if (name.equals("sp_setpglockpromote")) 	return "Sets or changes the lock promotion thresholds for a database, for a table, or for Adaptive Server.";
		if (name.equals("sp_setpsexe")) 			return "Sets custom execution attributes on the fly for an active client application, login, or stored procedure. ";
		if (name.equals("sp_setrowlockpromote")) 	return "Sets or changes row-lock promotion thresholds for a datarows-locked table, for all datarows-locked tables in a database, or for all datarows-locked tables on a server.";
		if (name.equals("sp_setsuspect_granularity")) return "Displays and sets the recovery fault isolation mode.";
		if (name.equals("sp_setsuspect_threshold")) return "On recovery, sets the maximum number of suspect pages that Adaptive Server will allow in the specified database before taking the entire database offline. ";
		if (name.equals("sp_setup_table_transfer")) return "Run once in each database containing the tables marked for incremental transfer to create the spt_TableTransfer table in this database.";
		if (name.equals("sp_show_options")) 		return "Prints all the server options that have been set in the current session. @@options the array of bits corresponding to server options. For every option, \"low\" is the byte number in @@options, and \"high\" is the bit within that byte corresponding to the option. If the bit is set, print name of that option.";
		if (name.equals("sp_showcontrolinfo")) 		return "Displays information about engine group assignments, bound client applications, logins, and stored procedures.";
		if (name.equals("sp_showexeclass")) 		return "Displays the execution class attributes and the engines in any engine group associated with the specified execution class.";
		if (name.equals("sp_showplan")) 			return "Displays the query plan for any user connection for the current SQL statement (or a previous statement in the same batch). The query plan is displayed in showplan format.";
		if (name.equals("sp_showpsexe")) 			return "Displays execution class, current priority, and affinity for all processes running on Adaptive Server. ";
		if (name.equals("sp_spaceusage")) 			return "Reports the space usage for a table, index, or transaction log and estimates the amount of fragmentation for tables and indexes in a database.";
		if (name.equals("sp_spaceused")) 			return "Displays estimates of the number of rows, the number of data pages, and the space used by one table or by all tables in the current database.";
		if (name.equals("sp_ssladmin")) 			return "Adds, deletes, or displays a list of server certificates for Adaptive Server.";
		if (name.equals("sp_syntax")) 				return "Displays the syntax of Transact-SQL statements, system procedures, utilities, and other routines, depending on which products and corresponding sp_syntax scripts exist on Adaptive Server.";
		if (name.equals("sp_sysmon")) 				return "Displays performance information.";
		if (name.equals("sp_tab_suspectptn")) 		return "Lists tables with suspect partitions. A range-partitioned table on character-based partition keys can become suspect after a sort-order change, and hash-partitioned tables can become suspect after a cross-platform dump load.";
		if (name.equals("sp_tempdb")) 				return "Creates the default temporary database group, binds temporary databases to the default temporary database group, binds users and applications to the default temporary database group or to specific temporary databases, and provides the binding interface for maintaining bindings in sysattributes that are related to the multiple temporary database.";
		if (name.equals("sp_tempdb_markdrop")) 		return "Cluster environment only Places a local system temporary database in the drop state.";
		if (name.equals("sp_thresholdaction")) 		return "Executes automatically when the number of free pages on the log segment falls below the last-chance threshold, unless the threshold is associated with a different procedure. Sybase does not provide this procedure.";
		if (name.equals("sp_tran_dumpable_status")) return "If you cannot make a transaction dump on a database, sp_tran_dumpable_status displays the reasons the dump is not possible.";
		if (name.equals("sp_transactions")) 		return "Reports information about active transactions.";
		if (name.equals("sp_unbindcache")) 			return "Unbinds a database, table, index, text object, or image object from a data cache.";
		if (name.equals("sp_unbindcache_all")) 		return "Unbinds all objects that are bound to a cache.";
		if (name.equals("sp_unbindefault")) 		return "Unbinds a created default value from a column or from a user-defined datatype.";
		if (name.equals("sp_unbindexeclass")) 		return "Removes the execution class attribute previously associated with an client application, login, or stored procedure for the specified scope.";
		if (name.equals("sp_unbindmsg")) 			return "Unbinds a user-defined message from a constraint.";
		if (name.equals("sp_unbindrule")) 			return "Unbinds a rule from a column or from a user-defined datatype.";
		if (name.equals("sp_version")) 				return "Returns the version information of the installation scripts (installmaster, installdbccdb, and so on) that was last run and whether it was successful.";
		if (name.equals("sp_volchanged")) 			return "Notifies the Backup Server that the operator performed the requested volume handling during a dump or load.";
		if (name.equals("sp_webservices")) 			return "Creates and manages the proxy tables used in the Adaptive Server Web Services Engine.";
		if (name.equals("sp_who")) 					return "Reports information about all current Adaptive Server users and processes or about a particular user or process.";
		return "";
	}
}
