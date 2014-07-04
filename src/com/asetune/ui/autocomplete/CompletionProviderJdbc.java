package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.RoundRobinAutoCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.parser.QueryWindowMessageParser;
import com.asetune.utils.ConnectionProvider;

public class CompletionProviderJdbc
extends CompletionProviderAbstractSql
{
	private static Logger _logger = Logger.getLogger(CompletionProviderJdbc.class);

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, RTextScrollPane scroll, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for JDBC ("+SyntaxConstants.SYNTAX_STYLE_SQL+").");
		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		CompletionProviderAbstract acProvider = createCompletionProvider(window, connectionProvider);
		RoundRobinAutoCompletion ac = new SqlAutoCompletion(acProvider);
		ac.setListCellRenderer(acProvider.createDefaultCompletionCellRenderer());
		ac.addCompletionProvider(acProvider.createTemplateProvider());
		ac.install(textPane);
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
//		ac.setChoicesWindowSize(600, 600);
		ac.setDescriptionWindowSize(600, 600);
		
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
	public CompletionProviderJdbc(Window owner, ConnectionProvider connectionProvider)
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
		_logger.debug("JDBC: createCompletionProvider()");

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		CompletionProviderJdbc provider = new CompletionProviderJdbc(window, connectionProvider);

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
		ArrayList<CompletionTemplate> list = new ArrayList<CompletionTemplate>();
		
		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		list.add( new CompletionTemplate("SELECT * FROM "));
		
		return list;
	}
}
