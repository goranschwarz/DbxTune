package com.asetune.ui.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TokenMaker;

/**
 * xxx: grabbed from RSyntaxTextArea
 * <p>
 * The default implementation of <code>TokenMakerFactory</code>.  This factory
 * can create {@link TokenMaker}s for all languages known to
 * {@link RSyntaxTextArea}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class AsetuneTokenMakerFactory extends AbstractTokenMakerFactory
								implements SyntaxConstants {


	public AsetuneTokenMakerFactory()
	{
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initTokenMakerMap() {

		String pkg = "com.asetune.ui.rsyntaxtextarea.modes.";

		putMapping(com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL, pkg + "SybaseTSqlTokenMaker");
		putMapping(com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL,  pkg + "SybaseRclTokenMaker");

		pkg = "org.fife.ui.rsyntaxtextarea.modes.";

		putMapping(SYNTAX_STYLE_NONE,			pkg + "PlainTextTokenMaker");
		putMapping(SYNTAX_STYLE_ACTIONSCRIPT,	pkg + "ActionScriptTokenMaker");
		putMapping(SYNTAX_STYLE_ASSEMBLER_X86,	pkg + "AssemblerX86TokenMaker");
		putMapping(SYNTAX_STYLE_BBCODE,			pkg + "BBCodeTokenMaker");
		putMapping(SYNTAX_STYLE_C,				pkg + "CTokenMaker");
		putMapping(SYNTAX_STYLE_CLOJURE,		pkg + "ClojureTokenMaker");
		putMapping(SYNTAX_STYLE_CPLUSPLUS,		pkg + "CPlusPlusTokenMaker");
		putMapping(SYNTAX_STYLE_CSHARP,			pkg + "CSharpTokenMaker");
		putMapping(SYNTAX_STYLE_CSS,			pkg + "CSSTokenMaker");
		putMapping(SYNTAX_STYLE_DELPHI,			pkg + "DelphiTokenMaker");
		putMapping(SYNTAX_STYLE_DTD,			pkg + "DtdTokenMaker");
		putMapping(SYNTAX_STYLE_FORTRAN,		pkg + "FortranTokenMaker");
		putMapping(SYNTAX_STYLE_GROOVY,			pkg + "GroovyTokenMaker");
		putMapping(SYNTAX_STYLE_HTML,			pkg + "HTMLTokenMaker");
		putMapping(SYNTAX_STYLE_JAVA,			pkg + "JavaTokenMaker");
		putMapping(SYNTAX_STYLE_JAVASCRIPT,		pkg + "JavaScriptTokenMaker");
		putMapping(SYNTAX_STYLE_JSP,			pkg + "JSPTokenMaker");
		putMapping(SYNTAX_STYLE_LATEX,			pkg + "LatexTokenMaker");
		putMapping(SYNTAX_STYLE_LISP,			pkg + "LispTokenMaker");
		putMapping(SYNTAX_STYLE_LUA,			pkg + "LuaTokenMaker");
		putMapping(SYNTAX_STYLE_MAKEFILE,		pkg + "MakefileTokenMaker");
		putMapping(SYNTAX_STYLE_MXML,			pkg + "MxmlTokenMaker");
		putMapping(SYNTAX_STYLE_PERL,			pkg + "PerlTokenMaker");
		putMapping(SYNTAX_STYLE_PHP,			pkg + "PHPTokenMaker");
		putMapping(SYNTAX_STYLE_PROPERTIES_FILE,pkg + "PropertiesFileTokenMaker");
		putMapping(SYNTAX_STYLE_PYTHON,			pkg + "PythonTokenMaker");
		putMapping(SYNTAX_STYLE_RUBY,			pkg + "RubyTokenMaker");
		putMapping(SYNTAX_STYLE_SAS,			pkg + "SASTokenMaker");
		putMapping(SYNTAX_STYLE_SCALA,			pkg + "ScalaTokenMaker");
		putMapping(SYNTAX_STYLE_SQL,			pkg + "SQLTokenMaker");
		putMapping(SYNTAX_STYLE_TCL,			pkg + "TclTokenMaker");
		putMapping(SYNTAX_STYLE_UNIX_SHELL,		pkg + "UnixShellTokenMaker");
		putMapping(SYNTAX_STYLE_WINDOWS_BATCH,	pkg + "WindowsBatchTokenMaker");
		putMapping(SYNTAX_STYLE_XML,			pkg + "XMLTokenMaker");
	}


}