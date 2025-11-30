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
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.parser.QueryWindowMessageParser;
import com.dbxtune.ui.autocomplete.completions.CompletionTemplate;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;

public class CompletionProviderJdbc
extends CompletionProviderAbstractSql
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
		return CompletionProviderStaticTemplates.createCompletionTemplates();
	}
}
