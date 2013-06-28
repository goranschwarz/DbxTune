package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.Util;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.sybase.jdbcx.SybConnection;

public class CompletionProviderRepServer
extends CompletionProviderAbstract
{
	private static Logger _logger = Logger.getLogger(CompletionProviderRepServer.class);

	private final CompletionProviderAbstract thisOuter = this;
	
	private ArrayList<RsCompletion> _rsdbList = new ArrayList<RsCompletion>();

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for Sybase Replication Server ("+AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL+").");
		textPane.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL);
//		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		CompletionProviderAbstract acProvider = createCompletionProvider(window, connectionProvider);
//		AutoCompletion ac = new AutoCompletion(acProvider);
		AutoCompletion ac = new RepServerAutoCompletion(acProvider);
		ac.install(textPane);
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
//		ac.setChoicesWindowSize(600, 600);
		ac.setDescriptionWindowSize(600, 600);

		return acProvider;
	}

	/**
	 * Constructor
	 * @param owner
	 * @param connectionProvider
	 */
	public CompletionProviderRepServer(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	private static class RepServerAutoCompletion
	extends AutoCompletion
	{
		public RepServerAutoCompletion(CompletionProvider provider)
		{
			super(provider);
		}

		/**
		 * Inserts a completion.  Any time a code completion event occurs, the
		 * actual text insertion happens through this method.
		 *
		 * @param c A completion to insert.  This cannot be <code>null</code>.
		 */
		@Override
//		protected void insertCompletion(Completion c) 
		protected void insertCompletion(Completion c, boolean typedParamListStartChar) 
		{
			JTextComponent textComp = getTextComponent();
			String alreadyEntered = c.getAlreadyEntered(textComp);
			hideChildWindows();
			Caret caret = textComp.getCaret();

			int dot = caret.getDot();
			int len = alreadyEntered.length();
			int start = dot-len;
			String replacement = getReplacementText(c, textComp.getDocument(), start, len);

			if (c instanceof ReplacementCompletion && textComp instanceof RTextArea)
			{
				String complOriginWord = ((ReplacementCompletion)c)._originWord;
				try
				{
//System.out.println("BEFORE: RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
					start = RSyntaxUtilitiesX.getWordStart((RTextArea)textComp, dot, complOriginWord);
					dot   = RSyntaxUtilitiesX.getWordEnd(  (RTextArea)textComp, dot, complOriginWord);
				}
				catch (BadLocationException e)
				{
					e.printStackTrace();
				}
			}
//System.out.println("RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
			caret.setDot(start);
			caret.moveDot(dot);
			textComp.replaceSelection(replacement);
		}
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
		CompletionProviderRepServer provider = new CompletionProviderRepServer(window, connectionProvider);

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		provider.addCompletion(new BasicCompletion(provider, "admin health"));
		provider.addCompletion(new BasicCompletion(provider, "admin who_is_down"));
		provider.addCompletion(new BasicCompletion(provider, "resume connection to "));
		provider.addCompletion(new BasicCompletion(provider, "suspend connection to "));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"resume connection",  
					"resume connection to <srv.db>\n" +
					"\t--skip transaction \n" +
					"\t--skip ## transaction /* skip first ## transactions  */\n" +
					"\t--execute transaction /* executes system transaction */\n", 
				"Full syntax of the resume connection"));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"suspend connection",  
					"suspend connection to <srv.db>\n" +
					"\t--with nowait /* suspends the connection without waiting for current DSI tran to complete */\n",
				"Full syntax of the suspend connection"));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"sysadmin dump_queue",  
					"sysadmin dump_queue, <dbid>, 0, -1, -2, -1, client\n",
				"Dump first queue block, for an outbound queue."));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"connect rssd",  
					"connect rssd\n" +
					"go\n" +
					"select * from rs_databases\n" +
					"go\n" +
					"disconnect\n" +
					"go\n", 
				"Connect to RSSD, SQL Statement, disconnect."));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"create connection ",  
					"create connection to \"ACTIVE_ASE\".\"dbname\"\n" +
					"\tset error class to \"rs_sqlserver_error_class\" \n" +
					"\tset function string class to \"rs_sqlserver_function_class\" \n" +
					"\tset username \"dbname_maint\" \n" +
					"\tset password \"dbname_maint_ps\" \n" +
					"\t--set database_config_param to 'on' \n" +
					"\twith log transfer on \n" +
					"\tas active for <lsrv.db> \n", 
				"Create ACTIVE WS Connection"));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"create connection ",  
					"create connection to \"STANDBY_ASE\".\"dbname\"\n" +
					"\tset error class to \"rs_sqlserver_error_class\" \n" +
					"\tset function string class to \"rs_sqlserver_function_class\" \n" +
					"\tset username \"dbname_maint\" \n" +
					"\tset password \"dbname_maint_ps\" \n" +
					"\t--set database_config_param to 'on' \n" +
					"\twith log transfer on \n" +
					"\tas standby for <lsrv.db> \n" +
					"\tuse dump marker \n", 
				"Create STANDBY WS Connection"));

		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"trace dsi_buf_dump on ",  
				"trace 'on', 'dsi', 'dsi_buf_dump'",
				"Turn ON: Write SQL statements executed by the DSI Threads to the RS log"));
		provider.addStaticCompletion(new ShorthandCompletion(provider, 
				"trace dsi_buf_dump off ",  
				"trace 'off', 'dsi', 'dsi_buf_dump'",
				"Turn OFF: Write SQL statements executed by the DSI Threads to the RS log"));

//		provider.addCommandCompletion("aminCommand", "subcommand", "htmlShortDesc", "full template", "htmlLongDesc");
		return provider;
	}

	private static class ShorthandCompletionHtml
	extends ShorthandCompletion
	{

		public ShorthandCompletionHtml(CompletionProvider provider, String inputText, String replacementText, String shortDesc)
		{
			super(provider, inputText, replacementText, shortDesc);
		}
		@Override
		public String toString()
		{
			String shortDesc = getShortDescription();
			if (shortDesc==null) {
				return getInputText();
			}
			return "<html>" + getInputText() + " -- <i><font color=\"green\">" + shortDesc + "</font></i></html>";
		}
	}

	public void addRsCompletion(Completion c)
	{
		super.addCompletion(c);
	}
	public void addRsCompletions(List<RsCompletion> list)
	{
		super.addCompletions(list);
	}

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp)
	{
		RSyntaxTextArea textArea = (RSyntaxTextArea)comp;

//System.out.println();

//		String allowChars = "_.<>";
		setCharsAllowedInWordCompletion("_.<>");
		// Parse 
		String enteredText = getAlreadyEnteredText(textArea);
		String currentWord = getCurrentWord(textArea);
//		String nextWord1   = getRelativeWord(textArea,  1);
//		String nextWord2   = getRelativeWord(textArea,  2);
		String prevWord1   = getRelativeWord(textArea, -1);
		String prevWord2   = getRelativeWord(textArea, -2);
		String prevWord3   = getRelativeWord(textArea, -3);
		String prevWord4   = getRelativeWord(textArea, -4);

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
		{
			// Clear old completions
			clear();

			// Restore the "saved" completions
//			super.addCompletions(_savedComplitionList);
			super.addCompletions(getStaticCompletions());

			// DO THE REFRESH
			List<RsCompletion> list = refreshCompletion();

			if (list != null && list.size() > 0)
				addRsCompletions(list);

			setNeedRefresh(false);
		} // end _needRefresh


		// Complete: closed <tags>, which will make a ReplacementCompletion
		if (currentWord.startsWith("<") && currentWord.endsWith(">"))
		{
			ArrayList<Completion> cList = new ArrayList<Completion>();

			if (currentWord.equals("<dbid>"))
			{
//				System.out.println("::::::::::::::: do: <dbid> lookup");

				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDatabase)
					{
						RsDatabase x = (RsDatabase) rsc._rsInfo;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.dbid+"", "<dbid>", x.dsname+"."+x.dbname+" - "+x._type+" - "+x._desc);
						cList.add(rc);
//						cList.add(new BasicCompletion(this, rsdb.dbid+"", rsdb.dsname+"."+rsdb.dbname+" - "+rsdb._type+" - "+rsdb._desc));
					}
				}
				return cList;
			}
			else if (currentWord.equals("<srv.db>"))
			{
//				System.out.println("::::::::::::::: do: <srv.db> lookup");
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDatabase)
					{
						RsDatabase x = (RsDatabase) rsc._rsInfo;

						if ( ! "CONN".equals(x._type) )
							continue;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.dsname+"."+x.dbname, "<srv.db>", x.dsname+"."+x.dbname+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else if (currentWord.equals("<lsrv.db>"))
			{
//				System.out.println("::::::::::::::: do: <lsrv.db> lookup");

				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDatabase)
					{
						RsDatabase x = (RsDatabase) rsc._rsInfo;

						if ( ! "LCONN".equals(x._type) )
							continue;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.dsname+"."+x.dbname, "<lsrv.db>", x.dsname+"."+x.dbname+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else if (currentWord.equals("<repdef>"))
			{
//				System.out.println("::::::::::::::: do: <repdef> lookup");
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsTableRepDef)
					{
						RsTableRepDef x = (RsTableRepDef) rsc._rsInfo;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.name+"", "<repdef>", x.name+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else if (currentWord.equals("<sub>"))
			{
//				System.out.println("::::::::::::::: do: <sub> lookup");
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsTableSub)
					{
						RsTableSub x = (RsTableSub) rsc._rsInfo;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.name+"", "<sub>", x.name+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else if (currentWord.equals("<dbrepdef>"))
			{
//				System.out.println("::::::::::::::: do: <dbrepdef> lookup");
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDbRepDef)
					{
						RsDbRepDef x = (RsDbRepDef) rsc._rsInfo;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.name+"", "<repdef>", x.name+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else if (currentWord.equals("<dbsub>"))
			{
//				System.out.println("::::::::::::::: do: <dbsub> lookup");
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDbSub)
					{
						RsDbSub x = (RsDbSub) rsc._rsInfo;

						ReplacementCompletion rc = new ReplacementCompletion(this, x.name+"", "<repdef>", x.name+" - "+x._type+" - "+x._desc);
						cList.add(rc);
					}
				}
				return cList;
			}
			else
			{
//				System.out.println("::::::::::::::: do: uknown tag '"+currentWord+"' lookup");
			}
			return cList;
		}

		// Complete: started, but not finished <tags
		if (enteredText.startsWith("<"))
		{
			ArrayList<Completion> cList = new ArrayList<Completion>();
			if (Util.startsWithIgnoreCase("<dbid>",     enteredText)) cList.add(new BasicCompletion(this, "<dbid>",     "Translate a SRV.db to the repserver dbid"));
			if (Util.startsWithIgnoreCase("<srv.db>",   enteredText)) cList.add(new BasicCompletion(this, "<srv.db>",   "Get list of PHYSICAL databases configured in repserver"));
			if (Util.startsWithIgnoreCase("<lsrv.db>",  enteredText)) cList.add(new BasicCompletion(this, "<lsrv.db>",  "Get list of LOGICAL databases configured in repserver"));
			if (Util.startsWithIgnoreCase("<sub>",      enteredText)) cList.add(new BasicCompletion(this, "<sub>",      "Get list of Table SUBSCRIPTIONS created in repserver"));
			if (Util.startsWithIgnoreCase("<repdef>",   enteredText)) cList.add(new BasicCompletion(this, "<repdef>",   "Get list of Table REPLICATION DEFINITIONS created in repserver"));
			if (Util.startsWithIgnoreCase("<dbsub>",    enteredText)) cList.add(new BasicCompletion(this, "<dbsub>",    "Get list of DB SUBSCRIPTIONS created in repserver"));
			if (Util.startsWithIgnoreCase("<dbrepdef>", enteredText)) cList.add(new BasicCompletion(this, "<dbrepdef>", "Get list of DB REPLICATION DEFINITIONS created in repserver"));
			return cList;
		}

		// Complete: RESUME
		if ("resume".equalsIgnoreCase(prevWord4) && "connection".equalsIgnoreCase(prevWord3) && "to".equalsIgnoreCase(prevWord2))
		{
			ArrayList<Completion> cList = new ArrayList<Completion>();
			cList.add(new BasicCompletion(this, "skip transaction"));
			cList.add(new BasicCompletion(this, "skip 1 transaction", "Skip First ## number of transactions."));
			cList.add(new BasicCompletion(this, "execute transaction", "Execute first system transaction."));
			return cList;
		}

		// Complete: TO SRV.DB
		if ("to".equalsIgnoreCase(prevWord1))
		{
//			System.out.println("########## show CONNECTION NAMES #################");
			// Get SERVERS
			if (enteredText.indexOf('.') == -1)
			{
				String dsname = enteredText;
				
//				System.out.println("SRV search: dsname='"+dsname+"'.");

				ArrayList<Completion> cList = new ArrayList<Completion>();
				LinkedHashSet<String> srvList = new LinkedHashSet<String>();
				
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDatabase)
					{
						RsDatabase rsdb = (RsDatabase) rsc._rsInfo;

						if ("".equals(dsname) || Util.startsWithIgnoreCase(rsdb.dsname, dsname))
							srvList.add(rsdb.dsname);
					}
				}
				for (String str : srvList)
					cList.add(new BasicCompletion(this, str+"."));
				return cList;
			}
			// Get DBNAMES
			else
			{
				String[] sa = enteredText.split("\\.");
				String dsname = sa[0];
				String dbname = sa.length <= 1 ? "" : sa[1];

//				System.out.println("SRV.DB search: dsname='"+dsname+"', dbname='"+dbname+"'.");

				ArrayList<Completion> cList = new ArrayList<Completion>();
				LinkedHashSet<String> srvList = new LinkedHashSet<String>();
	
				for (RsCompletion rsc : _rsdbList)
				{
					if (rsc._rsInfo instanceof RsDatabase)
					{
						RsDatabase rsdb = (RsDatabase) rsc._rsInfo;

						if ("".equals(dsname) || Util.startsWithIgnoreCase(rsdb.dsname, dsname))
							if ("".equals(dbname) || Util.startsWithIgnoreCase(rsdb.dbname, dbname))
								srvList.add(rsdb.dsname+"."+rsdb.dbname);
					}
				}
				for (String str : srvList)
					cList.add(new BasicCompletion(this, str));
				return cList;
			}
		}

//		return getCompletionsSimple(comp);

		// completions is defined in org.fife.ui.autocomplete.AbstractCompletionProvider
		return getCompletionsFrom(completions, enteredText);
	}

//    private List getCompletionsSimple(JTextComponent comp)
//	{
//		List retList = new ArrayList();
//		String text = getAlreadyEnteredText(comp);
//
//		if ( text != null )
//		{
//			for (Object o : completions)
//			{
//				if (o instanceof Completion)
//				{
//					Completion c = (Completion) o;
//					if (Util.startsWithIgnoreCase(c.getInputText(), text))
//						retList.add(c);
//				}
//			}
//		}
//
//		return retList;
//	}

    /**
	 * Go and get the Completions for the underlying RepServer Connection
	 * @return
	 */
	protected List<RsCompletion> refreshCompletion()
	{
		final Connection conn = _connectionProvider.getConnection();
		if (conn == null)
			return null;

		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing RCL Completion");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
			ArrayList<RsCompletion> completionList = new ArrayList<RsCompletion>();
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
					if (conn instanceof SybConnection)
						((SybConnection)conn).cancel();
				}
				catch (SQLException ignore)
				{
				}
			};
			
			@Override
			public Object doWork()
			{
				try
				{
					boolean quoteNames = false;
//					List<String> list = null;

//					int srvVersionNum = RepServerUtils.getVersionNumber(conn);

					/*==============================================================
					 * RS_DATABASES
					 * +---------------+-------+------+----+-----+-----+
					   |Column_name    |Type   |Length|Prec|Scale|Nulls|
					   +---------------+-------+------+----+-----+-----+
					   |dsname         |varchar|30    |NULL|NULL |0    |
					   |dbname         |varchar|30    |NULL|NULL |0    |
					   |dbid           |int    |4     |NULL|NULL |0    |
					   |dist_status    |int    |4     |NULL|NULL |0    |
					   |src_status     |int    |4     |NULL|NULL |0    |
					   |attributes     |tinyint|1     |NULL|NULL |0    |
					   |errorclassid   |rs_id  |8     |NULL|NULL |0    |
					   |funcclassid    |rs_id  |8     |NULL|NULL |0    |
					   |prsid          |int    |4     |NULL|NULL |0    |
					   |rowtype        |tinyint|1     |NULL|NULL |0    |
					   |sorto_status   |tinyint|1     |NULL|NULL |0    |
					   |ltype          |char   |1     |NULL|NULL |0    |
					   |ptype          |char   |1     |NULL|NULL |0    |
					   |ldbid          |int    |4     |NULL|NULL |0    |
					   |enable_seq     |int    |4     |NULL|NULL |0    |
					   |rs_errorclassid|rs_id  |8     |NULL|NULL |0    |
					   +---------------+-------+------+----+-----+-----+
					 */
					getWaitDialog().setState("Getting Database information (rs_databases)");
					_rsdbList = new ArrayList<RsCompletion>();

					String sql;
//					_aseMonTableDesc = new HashMap<String, String>();
//					String sql = "select * from rs_databases ";
//					select 
//						lconn_name          = (select d2.dsname+"."+d2.dbname from rs_databases d2 where d2.dbid = d.ldbid and d.ldbid!=d.dbid),
//						*, 
//						errorclassid_str    = (select c.classname from rs_classes c where c.classid = d.errorclassid) ,
//						funcclassid_str     = (select c.classname from rs_classes c where c.classid = d.funcclassid),
//						rs_errorclassid_str = (select c.classname from rs_classes c where c.classid = d.rs_errorclassid),
//						prs_name            = (select s.name from rs_sites s where s.id = d.prsid)
//					from rs_databases d

					try
					{
						RepServerUtils.connectGwRssd(conn);

						Statement stmnt = conn.createStatement();
						ResultSet rs;

						/*-------------------------------------------------
						 * Get database information
						 */
						sql = "select * from rs_databases ";
						rs = stmnt.executeQuery(sql);
						while(rs.next())
						{
							RsDatabase rsdb = new RsDatabase();
							rsdb.dsname          = rs.getString("dsname");
							rsdb.dbname          = rs.getString("dbname");
							rsdb.dbid            = rs.getInt   ("dbid");
							rsdb.dist_status     = rs.getInt   ("dist_status");
							rsdb.src_status      = rs.getInt   ("src_status");
							rsdb.attributes      = rs.getInt   ("attributes");
//							rsdb.errorclassid    = rs.getInt   ("errorclassid");
//							rsdb.funcclassid     = rs.getInt   ("funcclassid");
							rsdb.prsid           = rs.getInt   ("prsid");
							rsdb.rowtype         = rs.getInt   ("rowtype");
							rsdb.sorto_status    = rs.getInt   ("sorto_status");
							rsdb.ltype           = rs.getString("ltype");
							rsdb.ptype           = rs.getString("ptype");
							rsdb.ldbid           = rs.getInt   ("ldbid");
							rsdb.enable_seq      = rs.getInt   ("enable_seq");
//							rsdb.rs_errorclassid = rs.getInt   ("rs_errorclassid");

							rsdb._name = rsdb.dsname + "." + rsdb.dbname;
							if ("L".equals(rsdb.ltype) && "L".equals(rsdb.ptype))
							{
								rsdb._type = "LCONN";
								rsdb._desc = "Logical Database Connection";
							}
							else
							{
								rsdb._type = "CONN";
								rsdb._desc = "Physical Database Connection";
							}

							RsCompletion c = new RsCompletion(thisOuter, rsdb, quoteNames);
//							completionList.add(c);

							_rsdbList.add(c);
							if (cancel)
								return null;
						}
						rs.close();

						/*-------------------------------------------------
						 * Get: Table Replication Definition
						 */
						getWaitDialog().setState("Getting Table Replication Definition information");
						sql = "exec rs_helprep";
						rs = stmnt.executeQuery(sql);
						while(rs.next())
						{
							RsTableRepDef x = new RsTableRepDef();

							int col = 1;
							x.name   = rs.getString(col++);
							x.prs    = rs.getString(col++);
							x.pDsDb  = rs.getString(col++);
							x.pTable = rs.getString(col++);
							x.rTable = rs.getString(col++);
							x.type   = rs.getString(col++);

							x._type = "TRD";
							x._desc = "Table Replication Definition";

							RsCompletion c = new RsCompletion(thisOuter, x, quoteNames);

							_rsdbList.add(c);
							if (cancel)
								return null;
						}
						rs.close();

						/*-------------------------------------------------
						 * Get: Table Subscription
						 */
						getWaitDialog().setState("Getting Table Subscription information");
						sql = "exec rs_helpsub";
						rs = stmnt.executeQuery(sql);
						while(rs.next())
						{
							RsTableSub x = new RsTableSub();

							int col = 1;
							x.name           = rs.getString(col++);
							x.rdName         = rs.getString(col++);
							x.rDsDb          = rs.getString(col++);
							x.autoCorrection = rs.getString(col++);
							x.rRs            = rs.getString(col++);
							x.pRs            = rs.getString(col++);
							x.dynamicSql     = rs.getString(col++);

							x._type = "TSUB";
							x._desc = "Table Subscription";

							RsCompletion c = new RsCompletion(thisOuter, x, quoteNames);

							_rsdbList.add(c);
							if (cancel)
								return null;
						}
						rs.close();

						/*-------------------------------------------------
						 * Get: DB Replication Definition
						 */
						getWaitDialog().setState("Getting DB Replication Definition information");
						sql = "exec rs_helpdbrep";
						rs = stmnt.executeQuery(sql);
						while(rs.next())
						{
							RsDbRepDef x = new RsDbRepDef();

							int col = 1;
							x.name     = rs.getString(col++);
							x.pDsDb    = rs.getString(col++);
							x.pRs      = rs.getString(col++);
							x.repDDL   = rs.getString(col++);
							x.repSys   = rs.getString(col++);
							x.repTable = rs.getString(col++);
							x.repFunc  = rs.getString(col++);
							x.repTran  = rs.getString(col++);
							x.repUpd   = rs.getString(col++);
							x.repDel   = rs.getString(col++);
							x.repIns   = rs.getString(col++);
							x.repSel   = rs.getString(col++);
							x.crDate   = rs.getString(col++);

							x._type = "DBRD";
							x._desc = "Database Replication Definition";

							RsCompletion c = new RsCompletion(thisOuter, x, quoteNames);

							_rsdbList.add(c);
							if (cancel)
								return null;
						}
						rs.close();

						/*-------------------------------------------------
						 * Get: DB Subscription
						 */
						getWaitDialog().setState("Getting DB Subscription information");
						sql = "exec rs_helpdbsub";
						rs = stmnt.executeQuery(sql);
						while(rs.next())
						{
							RsDbSub x = new RsDbSub();

							int col = 1;
							x.name        = rs.getString(col++);
							x.rDsDb       = rs.getString(col++);
							x.rRs         = rs.getString(col++);
							x.statusAtRrs = rs.getString(col++);
							x.dbRdName    = rs.getString(col++);
							x.pDsDb       = rs.getString(col++);
							x.pRs         = rs.getString(col++);
							x.statusAtPrs = rs.getString(col++);
							x.method      = rs.getString(col++);
							x.truncTable  = rs.getString(col++);
							x.crDate      = rs.getString(col++);

							x._type = "DBSUB";
							x._desc = "Database Subscription";

							RsCompletion c = new RsCompletion(thisOuter, x, quoteNames);

							_rsdbList.add(c);
							if (cancel)
								return null;
						}
						rs.close();

						// close statement
						stmnt.close();
					}
					catch (SQLException sqle)
					{
						_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
					}
					finally
					{
						RepServerUtils.disconnectGw(conn);
					}
					

//					wait.setState("Getting Table information");

//					if (cancel)
//						break;

					getWaitDialog().setState("Creating Code Completions.");
//					for (TableInfo ti : tableInfoList)
//					{
//						JdbcTableCompletion c = new JdbcTableCompletion(thisOuter, ti, quoteTableNames);
//						completionList.add(c);
//					}
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Table code completion.", e);
				}

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<RsCompletion> list = (ArrayList)wait.execAndWait(doWork);

		return list;
    }



    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	private static class RsCompletion
	extends ShorthandCompletion
	{
		private RsInfo _rsInfo = null;
		
		public RsCompletion(CompletionProviderAbstract provider, RsInfo ri, boolean quoteNames)
		{
			super(provider, 
				ri._name,
				! quoteNames 
					? ri._name 
					: "\""+ri._name+"\"");

			_rsInfo = ri;

			String shortDesc = 
				"<font color=\"blue\">"+ri._type+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ri._desc) ? "No Description" : ri._desc) + "</font></i>";
				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ri._desc) ? "" : ri._desc) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_rsInfo.toHtmlString());
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

	/**
	 * Holds information about tables
	 */
	private static class RsInfo
	{
		public String _type     = null;
		public String _name     = null;
		public String _desc     = null;

		public String toHtmlString()
		{
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class RsDatabase extends RsInfo
	{
		public String dsname          = null;
		public String dbname          = null;
		public int    dbid            = -1;
		public int    dist_status     = -1;
		public int    src_status      = -1;
		public int    attributes      = -1;
		public int    errorclassid    = -1;
		public int    funcclassid     = -1;
		public int    prsid           = -1;
		public int    rowtype         = -1;
		public int    sorto_status    = -1;
		public String ltype           = null;
		public String ptype           = null;
		public int    ldbid           = -1;
		public int    enable_seq      = -1;
		public int    rs_errorclassid = -1;
	}
	@SuppressWarnings("unused")
	private static class RsTableRepDef extends RsInfo
	{
		public String name   = null;
		public String prs    = null;
		public String pDsDb  = null;
		public String pTable = null;
		public String rTable = null;
		public String type   = null;
	}
	@SuppressWarnings("unused")
	private static class RsTableSub extends RsInfo
	{
		public String name           = null;
		public String rdName         = null;
		public String rDsDb          = null;
		public String autoCorrection = null;
		public String rRs            = null;
		public String pRs            = null;
		public String dynamicSql     = null;
	}
	@SuppressWarnings("unused")
	private static class RsDbRepDef extends RsInfo
	{
		public String name     = null;
		public String pDsDb    = null;
		public String pRs      = null;
		public String repDDL   = null;
		public String repSys   = null;
		public String repTable = null;
		public String repFunc  = null;
		public String repTran  = null;
		public String repUpd   = null;
		public String repDel   = null;
		public String repIns   = null;
		public String repSel   = null;
		public String crDate   = null;
	}
	@SuppressWarnings("unused")
	private static class RsDbSub extends RsInfo
	{
		public String name        = null;
		public String rDsDb       = null;
		public String rRs         = null;
		public String statusAtRrs = null;
		public String dbRdName    = null;
		public String pDsDb       = null;
		public String pRs         = null;
		public String statusAtPrs = null;
		public String method      = null;
		public String truncTable  = null;
		public String crDate      = null;
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
//		sb.append("-end-");
//		
//		return sb.toString();
		return null;
	}
	
	private static class ReplacementCompletion
	extends BasicCompletion
	{
		private String _originWord;

		public ReplacementCompletion(CompletionProvider provider, String replacementText, String originWord, String description)
		{
			super(provider, replacementText, description);
			_originWord = originWord;
		}
	}
}
