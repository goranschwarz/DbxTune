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
package com.asetune.ui.tooltip.suppliers;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

public class TtpEntryCompletionProvider
{
	private static Logger _logger = Logger.getLogger(TtpEntryCompletionProvider.class);

	public static CompletionProvider installAutoCompletion(TextEditorPane textPane)
	{
		_logger.info("Installing Syntax and AutoCompleation for TtpEntry ("+SyntaxConstants.SYNTAX_STYLE_XML+").");
		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);

		CompletionProvider provider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(provider);
		ac.install(textPane);
		
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
		ac.setDescriptionWindowSize(600, 600);
		
		return provider;
	}

	/**
	 * Create a simple provider that adds some Java-related completions.
	 * 	
	 * @return The completion provider.
	 */
	private static CompletionProvider createCompletionProvider() 
	{
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

//		provider.addCompletion(new BasicCompletion(provider, "abstract"));

		String cmdName     = "    <CmdName>    _FIXME_</CmdName> \n";
		String module      = "    <Module>     _FIXME_</Module> \n";
		String section     = "    <Section>    _FIXME_</Section> \n";
		String fromVersion = "    <FromVersion>_FIXME_</FromVersion> \n";
		String description = "    <Description>_FIXME_</Description> \n";
		String syntax =
			"    <Syntax> \n" +
			"        <![CDATA[ \n" +
			"        <PRE> \n" +
			"_FIXME_\n" +
			"        </PRE> \n" +
			"        ]]> \n" +
			"    </Syntax> \n";
		String parameters =
			"    <Parameters> \n" +
			"        <![CDATA[ \n" +
			"        <UL> \n" +
			"            <LI><B>param 1</B> Description</LI> \n" +
			"            <LI><B>param 2</B> Description</LI> \n" +
			"            <LI><B>param 3</B> Description</LI> \n" +
			"        </UL> \n" +
			"        ]]> \n" +
			"    </Parameters> \n";
		String example =
			"    <Example> \n" +
			"        <![CDATA[ \n" +
			"        <UL> \n" +
			"            <LI> \n" +
			"                <B>Example 1</B>: <I>describe</I><BR> \n" +
			"                <CODE>example_code</CODE> \n" +
			"            </LI> \n" +
			"            <LI> \n" +
			"                <B>Example 2</B>: <I>describe</I><BR> \n" +
			"                <CODE>example_code</CODE> \n" +
			"            </LI> \n" +
			"            <LI> \n" +
			"                <B>Example 3</B>: <I><FONT COLOR=\"green\">describe in green</FONT> then black again...</I><BR> \n" +
			"                <CODE>example_code</CODE> \n" +
			"            </LI> \n" +
			"        </UL> \n" +
			"        ]]> \n" +
			"    </Example> \n";
		String usage =
			"    <Usage> \n" +
			"        <![CDATA[ \n" +
			"        <UL> \n" +
			"            <LI>area 1</LI>\n" +
			"            <LI>area 2</LI>\n" +
			"            <LI>area 3</LI>\n" +
			"        </UL> \n" +
			"        ]]> \n" +
			"    </Usage> \n";
		String permissions = "    <Permissions>_FIXME_</Permissions> \n";
		String seeAlso =
			"    <SeeAlso> \n" +
			"        <![CDATA[ \n" +
			"        <UL> \n" +
			"            <LI>command 1</LI>\n" +
			"            <LI>command 2</LI>\n" +
			"            <LI>Configure Memory - <A HREF=\"http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc31644.1570/html/sag2/X22466.htm\">System Admin Guide, Volume 2, Configure Memory</A></LI>\n" +
			"        </UL> \n" +
			"        ]]> \n" +
			"    </SeeAlso> \n";

		String entry1 = 
			"<Entry>\n" +
			cmdName +
			module +
			description +
			syntax +
			"</Entry>";

		String entry2 = 
			"<Entry>\n" +
			cmdName +
			module +
			section +
			fromVersion +
			description +
			syntax +
			parameters +
			example +
			usage +
			permissions +
			seeAlso +
			"</Entry>";
		
		provider.addCompletion(new ShorthandCompletion(provider, "Entry1", entry1, "<Entry> tag with mandatory subtags"));
		provider.addCompletion(new ShorthandCompletion(provider, "Entry2", entry2, "<Entry> tag with ALL subtags"));
		
		provider.addCompletion(new ShorthandCompletion(provider, "CmdName",     cmdName,     "<CmdName> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Module",      module,      "<Module> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Section",     section,     "<Section> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "FromVersion", fromVersion, "<FromVersion> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Description", description, "<Description> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Syntax",      syntax,      "<Syntax> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Parameters",  parameters,  "<Parameters> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Example",     example,     "<Example> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Usage",       usage,       "<Usage> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "Permissions", permissions, "<Permissions> Tag"));
		provider.addCompletion(new ShorthandCompletion(provider, "SeeAlso",     seeAlso,     "<SeeAlso> Tag"));

		
		String htmlTable =
			"<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=0 WIDTH=\"100%\"> \n" +
			"<TR ALIGN=\"left\" VALIGN=\"middle\"> \n" +
			"    <TH> head_1 </TH> \n" +
			"    <TH> head_2 </TH> \n" +
			"</TR> \n" +
			"<TR ALIGN=\"left\" VALIGN=\"middle\"> \n" +
			"    <TD> col1_row1 </TD> \n" +
			"    <TD> col2_row1 </TD> \n" +
			"</TR> \n" +
			"<TR ALIGN=\"left\" VALIGN=\"middle\"> \n" +
			"    <TD> col1_row2 </TD> \n" +
			"    <TD> col2_row2 </TD> \n" +
			"</TR> \n" +
			"</TABLE> \n";

		String htmlComment2 =
			"<!-- ============================================================================================ --> \n" +
			"<!-- == SECTION ================================================================================= --> \n" +
			"<!-- ============================================================================================ --> \n";

		
		// some HTML completions
		provider.addCompletion(new ShorthandCompletion(provider, "A",        "<A HREF=\"http://www.acme.com/page.html\">description</A>", "HTTP Link"));
		provider.addCompletion(new ShorthandCompletion(provider, "B",        "<B></B>", "Bold"));
		provider.addCompletion(new ShorthandCompletion(provider, "I",        "<I></I>", "Italic"));
		provider.addCompletion(new ShorthandCompletion(provider, "BR",       "<BR>", "Newline"));
		provider.addCompletion(new ShorthandCompletion(provider, "HR",       "<HR>", "Horizontal Rule"));
		provider.addCompletion(new ShorthandCompletion(provider, "LT",       "&lt;", "< Less Than Character inside a XML tag, if not using CDATA"));
		provider.addCompletion(new ShorthandCompletion(provider, "GT",       "&gt;", "> Greater Than Character inside a XML tag, if not using CDATA"));
		provider.addCompletion(new ShorthandCompletion(provider, "UL",       "<UL>\n<LI></LI>\n<LI></LI>\n<LI></LI>\n</UL>", "Unordered List"));
		provider.addCompletion(new ShorthandCompletion(provider, "CODE",     "<CODE></CODE>", "Code Section"));
		provider.addCompletion(new ShorthandCompletion(provider, "PRE",      "<PRE>\n</PRE>", "Code Section, which allows newlines... Preformatted code"));
		provider.addCompletion(new ShorthandCompletion(provider, "CDATA",    "<![CDATA[___]]>", "Any text (if you want to have <html> tags inside a XML tag...)"));
		provider.addCompletion(new ShorthandCompletion(provider, "COLOR",    "<FONT COLOR=\"green\">green_text</FONT>", "Text with another color"));
		provider.addCompletion(new ShorthandCompletion(provider, "TABLE",    htmlTable, "Table"));
		provider.addCompletion(new ShorthandCompletion(provider, "COMMENT1", "<!-- _do_not_use_dash_or_minus_chars_in_here -->", "Comment"));
		provider.addCompletion(new ShorthandCompletion(provider, "COMMENT2", htmlComment2, "Comment to use when starting a new section in the file"));


		return provider;
   }
}
