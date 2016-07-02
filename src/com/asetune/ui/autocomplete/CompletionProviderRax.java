package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.RoundRobinAutoCompletion;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.parser.QueryWindowMessageParser;
import com.asetune.sql.conn.TdsConnection;
import com.asetune.ui.autocomplete.completions.CompletionTemplate;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;
import com.sybase.jdbcx.SybConnection;

public class CompletionProviderRax
extends CompletionProviderAbstract
{
	private static Logger _logger = Logger.getLogger(CompletionProviderRax.class);

	private final CompletionProviderAbstract thisOuter = this;
	
//	private ArrayList<RaxCompletion> _rsdbList = new ArrayList<RaxCompletion>();

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, RTextScrollPane scroll, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for Sybase Replication Agent X ("+AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL+").");
		textPane.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL);
//		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		CompletionProviderAbstract acProvider = createCompletionProvider(window, connectionProvider);
//		AutoCompletion ac = new AutoCompletion(acProvider);
		RoundRobinAutoCompletion ac = new RaxAutoCompletion(acProvider);
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

		return acProvider;
	}

	/**
	 * Constructor
	 * @param owner
	 * @param connectionProvider
	 */
	public CompletionProviderRax(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	private static class RaxAutoCompletion
	extends RoundRobinAutoCompletion
	{
		public RaxAutoCompletion(CompletionProvider provider)
		{
			super(provider);
		}

//		/**
//		 * Inserts a completion.  Any time a code completion event occurs, the
//		 * actual text insertion happens through this method.
//		 *
//		 * @param c A completion to insert.  This cannot be <code>null</code>.
//		 */
//		@Override
////		protected void insertCompletion(Completion c) 
//		protected void insertCompletion(Completion c, boolean typedParamListStartChar) 
//		{
//			JTextComponent textComp = getTextComponent();
//			String alreadyEntered = c.getAlreadyEntered(textComp);
//			hideChildWindows();
//			Caret caret = textComp.getCaret();
//
//			int dot = caret.getDot();
//			int len = alreadyEntered.length();
//			int start = dot-len;
//			String replacement = getReplacementText(c, textComp.getDocument(), start, len);
//
//			if (c instanceof ReplacementCompletion && textComp instanceof RTextArea)
//			{
//				String complOriginWord = ((ReplacementCompletion)c)._originWord;
//				try
//				{
////System.out.println("BEFORE: RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
//					start = RSyntaxUtilitiesX.getWordStart((RTextArea)textComp, dot, complOriginWord);
//					dot   = RSyntaxUtilitiesX.getWordEnd(  (RTextArea)textComp, dot, complOriginWord);
//				}
//				catch (BadLocationException e)
//				{
//					e.printStackTrace();
//				}
//			}
////System.out.println("RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
//			caret.setDot(start);
//			caret.moveDot(dot);
//			textComp.replaceSelection(replacement);
//		}
	}

	/**
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	public static CompletionProviderAbstract createCompletionProvider(Window window, ConnectionProvider connectionProvider)
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		CompletionProviderRax provider = new CompletionProviderRax(window, connectionProvider);

		provider.refreshCompletionForStaticCmds();

		return provider;
	}

	/**
	 * Cell renderer for SQL Completions
	 * 
	 * @return
	 */
	@Override
	public ListCellRenderer createDefaultCompletionCellRenderer()
	{
		return new RepServerCellRenderer();
	}


	@Override
	public List<CompletionTemplate> createCompletionTemplates()
	{
		ArrayList<CompletionTemplate> list = new ArrayList<CompletionTemplate>();
		
//		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
//		list.add( new CompletionTemplate( "admin health"));
//		list.add( new CompletionTemplate( "admin who_is_down"));
//		list.add( new CompletionTemplate( "resume connection to "));
//		list.add( new CompletionTemplate( "suspend connection to "));
//
//		// Add a couple of "shorthand" completions. These completions don't
//		// require the input text to be the same thing as the replacement text.
//
//		list.add( new CompletionTemplate( 
//				"resume connection",  
//					"resume connection to <srv.db>\n" +
//					"\t--skip transaction \n" +
//					"\t--skip ## transaction /* skip first ## transactions  */\n" +
//					"\t--execute transaction /* executes system transaction */\n", 
//				"Full syntax of the resume connection"));
//
//		list.add( new CompletionTemplate( 
//				"suspend connection",  
//					"suspend connection to <srv.db>\n" +
//					"\t--with nowait /* suspends the connection without waiting for current DSI tran to complete */\n",
//				"Full syntax of the suspend connection"));
//
//		list.add( new CompletionTemplate( 
//				"sysadmin dump_queue",  
//					"sysadmin dump_queue, <dbid>, 0, -1, -2, -1, client\n",
//				"Dump first queue block, for an outbound queue."));
//
//		list.add( new CompletionTemplate( 
//				"connect rssd",  
//					"connect rssd\n" +
//					"go\n" +
//					"select * from rs_databases\n" +
//					"go\n" +
//					"disconnect\n" +
//					"go\n", 
//				"Connect to RSSD, SQL Statement, disconnect."));
//
//		list.add( new CompletionTemplate( 
//				"create logical connection ",  
//					"create logical connection to \"L_SRV\".\"dbname\"", 
//				"Create LOGICAL Connection"));
//
//		list.add( new CompletionTemplate( 
//				"create connection ",  
//					"create connection to \"ACTIVE_ASE\".\"dbname\"\n" +
//					"\tset error class to \"rs_sqlserver_error_class\" \n" +
//					"\tset function string class to \"rs_sqlserver_function_class\" \n" +
//					"\tset username \"dbname_maint\" \n" +
//					"\tset password \"dbname_maint_ps\" \n" +
//					"\t--set database_config_param to 'on' \n" +
//					"\twith log transfer on \n" +
//					"\tas active for <lsrv.db> \n", 
//				"Create ACTIVE WS Connection"));
//
//		list.add( new CompletionTemplate( 
//				"create connection ",  
//					"create connection to \"STANDBY_ASE\".\"dbname\"\n" +
//					"\tset error class to \"rs_sqlserver_error_class\" \n" +
//					"\tset function string class to \"rs_sqlserver_function_class\" \n" +
//					"\tset username \"dbname_maint\" \n" +
//					"\tset password \"dbname_maint_ps\" \n" +
//					"\t--set database_config_param to 'on' \n" +
//					"\twith log transfer on \n" +
//					"\tas standby for <lsrv.db> \n" +
//					"\tuse dump marker \n", 
//				"Create STANDBY WS Connection"));
//
//		// Create replication definition "template"
//		list.add( new CompletionTemplate( 
//				"create replication definition ",  
//					"create replication definition REPDEF_NAME \n" +
//					"\twith primary at <srv.db> \n" +
//					"\t[with all       tables named [table_owner.]'table_name' [quoted] | \n" +
//					"\t[with primary   table named  [table_owner.]'table_name'] \n" +
//					"\t with replicate table named  [table_owner.]'table_name'] [quoted]] \n" +
//					"\t( \n" +
//					"\t    column_name [as replicate_column_name] [datatype [null | not null] [datatype [null | not null] [map to published_datatype]] [quoted] \n" +
//					"\t  [,column_name [as replicate_column_name] [map to published_datatype]] [quoted]...) [references [table_owner.]table_name [(column_name)]] \n" +
//					"\t) \n" +
//					"\tprimary key (column_name [, column_name]...) \n" +
//					"\t[searchable columns (column_name [, column_name]...)] \n" +
//					"\t[send standby [{all | replication definition} columns]] \n" +
//					"\t[replicate {minimal | all} columns] \n" +
//					"\t[replicate {SQLDML ['off'] | 'options'}] \n" +
//					"\t[replicate_if_changed (column_name [, column_name]...)] \n" +
//					"\t[always_replicate (column_name [, column_name]...)] \n" +
//					"\t[with dynamic sql | without dynamic sql] \n",
//				"Create replication definition template"));
//
//		// Create subscription "template"
//		list.add( new CompletionTemplate( 
//				"create subscription ",  
//					"create subscription SUB_NAME \n" +
//					"\tfor {<repdef> | func_repdef | publication pub | database replication definition <dbrepdef> } [with primary at <srv.db>] \n" +
//					"\twith replicate at <srv.db> \n" +
//					"\t[where {column_name | @param_name} {< | > | >= | <= | = | &} value \n" +
//					"\t  [and {column_name | @param_name} {< | > | >= | <= | = | &} value]...] \n" +
//					"\t[without materialization | without holdlock [direct_load [user username password pass]] | incrementally] \n" +
//					"\t[subscribe to truncate table] \n" +
//					"\t[for new articles] \n",
//				"Create subscription template"));
//
//		
//		// Create DATABASE replication definition
//		list.add( new CompletionTemplate( 
//				"create database replication definition ",  
//					"create database replication definition DB_REPDEF_NAME \n" +
//					"\twith primary at <srv.db> \n" +
//					"\treplicate DDL \n" +
//					"\treplicate tables \n" +
//					"\treplicate functions \n" +
//					"\treplicate transactions \n" +
//					"\treplicate system procedures \n" +
//					"--[[not] replicate DDL] \n" +
//					"--[[not] replicate setname setcont] \n" +
//					"--[[not] replicate setname setcont] \n" +
//					"--[[not] replicate setname setcont] \n" +
//					"--[[not] replicate setname setcont] \n" +
//					"--[[not] replicate {SQLDML | DML_options} [in table_list]] \n" +
//					"--setname  ::= {tables | functions | transactions | system procedures} \n" +
//					"--setcont  ::= [[in] ([owner1.]name1[, [owner2.]name2 [, ... ]])] \n",
//				"Create database replication definition"));
//
//		// dsi_buf_dump on/off
//		list.add( new CompletionTemplate( 
//				"trace dsi_buf_dump on ",  
//				"trace 'on', 'dsi', 'dsi_buf_dump'",
//				"Turn ON: Write SQL statements executed by the DSI Threads to the RS log"));
//		list.add( new CompletionTemplate( 
//				"trace dsi_buf_dump off ",  
//				"trace 'off', 'dsi', 'dsi_buf_dump'",
//				"Turn OFF: Write SQL statements executed by the DSI Threads to the RS log"));
//
//		// dsi_buf_dump on/off for Express Connect @ connection level
//		list.add( new CompletionTemplate( 
//				"trace dsi_buf_dump on: for \"express connect\" at connection level",
//				"alter connection to <srv.db> set trace to 'econn, dsi_buf_dump, on'",
//				"Turn ON: for Express Connect: Write SQL statements executed by the DSI Threads to the RS log"));
//		list.add( new CompletionTemplate( 
//				"trace dsi_buf_dump off: for \"express connect\" at connection level",
//				"alter connection to <srv.db> set trace to 'econn, dsi_buf_dump, off'",
//				"Turn OFF: for Express Connect: Write SQL statements executed by the DSI Threads to the RS log"));
//
//		list.add(new CompletionTemplate("&lt;dbid&gt;",     "<dbid>",     "Translate a SRV.db to the repserver dbid"));
//		list.add(new CompletionTemplate("&lt;srv.db&gt;",   "<srv.db>",   "Get list of PHYSICAL databases configured in repserver"));
//		list.add(new CompletionTemplate("&lt;lsrv.db&gt;",  "<lsrv.db>",  "Get list of LOGICAL databases configured in repserver"));
//		list.add(new CompletionTemplate("&lt;sub&gt;",      "<sub>",      "Get list of Table SUBSCRIPTIONS created in repserver"));
//		list.add(new CompletionTemplate("&lt;repdef&gt;",   "<repdef>",   "Get list of Table REPLICATION DEFINITIONS created in repserver"));
//		list.add(new CompletionTemplate("&lt;dbsub&gt;",    "<dbsub>",    "Get list of DB SUBSCRIPTIONS created in repserver"));
//		list.add(new CompletionTemplate("&lt;dbrepdef&gt;", "<dbrepdef>", "Get list of DB REPLICATION DEFINITIONS created in repserver"));

//		addCommandCompletion("aminCommand", "subcommand", "htmlShortDesc", "full template", "htmlLongDesc");
		
		return list;
	}

//	private static class ShorthandCompletionHtml
//	extends ShorthandCompletion
//	{
//
//		public ShorthandCompletionHtml(CompletionProvider provider, String inputText, String replacementText, String shortDesc)
//		{
//			super(provider, inputText, replacementText, shortDesc);
//		}
//		@Override
//		public String toString()
//		{
//			String shortDesc = getShortDescription();
//			if (shortDesc==null) {
//				return getInputText();
//			}
//			return "<html>" + getInputText() + " -- <i><font color=\"green\">" + shortDesc + "</font></i></html>";
//		}
//	}

	public void addRaxCompletion(Completion c)
	{
		super.addCompletion(c);
	}
//	public void addRaxCompletions(List<RaxCompletion> list)
//	{
//		super.addCompletions(list);
//	}
	public void addRaxCompletions(List<RaxCompletion> list)
	{
		for (RaxCompletion c : list)
			super.addCompletion(c);
	}

	@Override
	public void refresh()
	{
		// Clear old completions
		clear();

		if (getStaticCompletions().size() == 0)
			refreshCompletionForStaticCmds();

		// Restore the "saved" completions
//		super.addCompletions(_savedComplitionList);
		super.addCompletions(getStaticCompletions());

		// DO THE REFRESH
		List<RaxCompletion> list = refreshCompletion();

		if (list != null && list.size() > 0)
			addRaxCompletions(list);

		setNeedRefresh(false);
	} // end _needRefresh

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp)
	{
		RSyntaxTextArea textArea = (RSyntaxTextArea)comp;

//System.out.println();

//		setCharsAllowedInWordCompletion("_.<>");
		setCharsAllowedInWordCompletion("_.");

		// Parse 
		String enteredText = getAlreadyEnteredText(textArea);
//		String currentWord = getCurrentWord(textArea);
////		String nextWord1   = getRelativeWord(textArea,  1);
////		String nextWord2   = getRelativeWord(textArea,  2);
//		String prevWord1   = getRelativeWord(textArea, -1);
//		String prevWord2   = getRelativeWord(textArea, -2);
//		String prevWord3   = getRelativeWord(textArea, -3);
//		String prevWord4   = getRelativeWord(textArea, -4);

//		System.out.println("-----------------------------------");
//		System.out.println("enteredText = '"+enteredText+"'.");
//		System.out.println("currentWord = '"+currentWord+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("nextWord1   = '"+nextWord1+"'.");
//		System.out.println("nextWord2   = '"+nextWord2+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("prevWord1   = '"+prevWord1+"'.");
//		System.out.println("prevWord2   = '"+prevWord2+"'.");
//		System.out.println("prevWord3   = '"+prevWord3+"'.");
//		System.out.println("prevWord4   = '"+prevWord4+"'.");
//		System.out.println("-----------------------------------");

		if (needRefresh())
			refresh();

		// completions is defined in org.fife.ui.autocomplete.AbstractCompletionProvider
		return getCompletionsFrom(completions, enteredText);
	}

    /**
	 * Go and get the Completions for the underlying RepServer Connection
	 * @return
	 */
	protected List<RaxCompletion> refreshCompletion()
	{
		final Connection conn = _connectionProvider.getConnection();
		if (conn == null)
			return null;

		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing RAX Completion");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
			ArrayList<RaxCompletion> completionList = new ArrayList<RaxCompletion>();
			boolean cancel = false;
			
			@Override
			public boolean canDoCancel() { return true; };
			
			@Override
			public void cancel() 
			{
				_logger.info("refreshCompletion(), CANCEL has been called.");
				cancel = true; 
				
				try
				{
					if (conn instanceof SybConnection) ((SybConnection)conn).cancel();
					if (conn instanceof TdsConnection) ((TdsConnection)conn).cancel();
				}
				catch (SQLException ignore)
				{
				}
			};
			
			@Override
			public Object doWork()
			{
				boolean quoteNames = false;

				getWaitDialog().setState("Getting Command information (rs_help)");

				String sql;
				try
				{
					Statement stmnt = conn.createStatement();
					ResultSet rs;

					/*-------------------------------------------------
					 * Get database information
					 */
					sql = "ra_help";
					rs = stmnt.executeQuery(sql);
					while(rs.next())
					{
						String cmd    = rs.getString(1).trim();
						String params = rs.getString(2).trim();
						String desc   = rs.getString(3).trim();
						
						if (params.equals("(none)"))
							params = "";
						else
							params = " "+params;
							
						RaxInfo raxInfo = new RaxInfo();
						raxInfo._name        = cmd + params;
						raxInfo._type        = "cmd";
						raxInfo._desc        = desc;

						RaxCompletion c = new RaxCompletion(thisOuter, raxInfo, quoteNames);

						completionList.add(c);

						if (cancel)
							return null;
					}
					rs.close();

					// close statement
					stmnt.close();
				}
				catch (SQLException sqle)
				{
					_logger.info("Problems when getting COMMAND dictionary, skipping this and continuing. Caught: "+sqle);
				}
				

				getWaitDialog().setState("Creating Code Completions.");

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<RaxCompletion> list = (ArrayList)wait.execAndWait(doWork);

		return list;
    }



    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	private static class RaxCompletion
	extends ShorthandCompletion
	{
		private RaxInfo _raxInfo = null;
		
		public RaxCompletion(CompletionProviderAbstract provider, RaxInfo ri, boolean quoteNames)
		{
			super(provider, 
				ri._name,
				! quoteNames 
					? ri._name 
					: "\""+ri._name+"\"");

			_raxInfo = ri;

			String shortDesc = 
				"<font color=\"blue\">"+ri._type+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ri._desc) ? "No Description" : ri._desc) + "</font></i>";
				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ri._desc) ? "" : ri._desc) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_raxInfo.toHtmlString());
		}

		/**
		 * Make it HTML aware
		 */
		@Override
		public String toString()
		{
			return "<html>" + super.toString() + "</html>";
		}
	}


	public String toHtmlString()
	{
//		StringBuilder sb = new StringBuilder();
//		sb.append("<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color=\"blue\">").append(_tabType).append(" - COLUMN").append("</font>");
//		sb.append("<HR>"); // add Horizontal Ruler: ------------------
//		sb.append("<BR>");
//		sb.append("<B>Table Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
//		sb.append("<BR>");
//		sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(ci._colRemark) ? "not available" : ci._colRemark).append("<BR>");
//		sb.append("<BR>");
//		sb.append("<B>Name:</B> ")       .append(ci._colName)      .append("<BR>");
//		sb.append("<B>Type:</B> ")       .append(ci._colType)      .append("<BR>");
//		sb.append("<B>Length:</B> ")     .append(ci._colLength)    .append("<BR>");
//		sb.append("<B>Is Nullable:</B> ").append(ci._colIsNullable).append("<BR>");
//		sb.append("<B>Pos:</B> ")        .append(ci._colPos)       .append("<BR>");
//		sb.append("<B>Default:</B> ")    .append(ci._colDefault)   .append("<BR>");
//		sb.append("<HR>");
//		sb.append("-end-<BR><BR>");
//		
//		return sb.toString();
		return null;
	}

//	private static class ReplacementCompletion
//	extends BasicCompletion
//	{
//		private String _originWord;
//
//		public ReplacementCompletion(CompletionProvider provider, String replacementText, String originWord, String description)
//		{
//			super(provider, replacementText, description);
//			_originWord = originWord;
//		}
//	}

	private static class RaxInfo
	{
		public String _type     = null;
		public String _name     = null;
		public String _desc     = null;

		public String toHtmlString()
		{
			return null;
		}
	}
}
