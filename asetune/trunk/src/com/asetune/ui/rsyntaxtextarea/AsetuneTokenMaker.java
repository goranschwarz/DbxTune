package com.asetune.ui.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;


public class AsetuneTokenMaker
{
	public static void init()
	{
//		System.setProperty(TokenMakerFactory.PROPERTY_DEFAULT_TOKEN_MAKER_FACTORY, "com.asetune.ui.rsyntaxtextarea.AsetuneTokenMakerFactory");
//		TokenMakerFactory tmf = new AsetuneTokenMakerFactory();
//		TokenMakerFactory.setDefaultInstance(tmf);

		// Some of the below code was taken from:
		// https://umlet.googlecode.com/svn-history/r297/trunk/Baselet/src/com/baselet/gui/OwnSyntaxPane.java
		
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();

		String pkg = "com.asetune.ui.rsyntaxtextarea.modes.";

		atmf.putMapping(com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL,  pkg + "SybaseRclTokenMaker");
		atmf.putMapping(com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL, pkg + "SybaseTSqlTokenMaker");
	}


	public static String getTokenString(int type)
	{
		switch (type)
		{
		case TokenTypes.NULL                         : return "NULL";

		case TokenTypes.COMMENT_EOL                  : return "COMMENT_EOL";
		case TokenTypes.COMMENT_MULTILINE            : return "COMMENT_MULTILINE";
		case TokenTypes.COMMENT_DOCUMENTATION        : return "COMMENT_DOCUMENTATION";
		case TokenTypes.COMMENT_KEYWORD              : return "COMMENT_KEYWORD";
		case TokenTypes.COMMENT_MARKUP               : return "COMMENT_MARKUP";

		case TokenTypes.RESERVED_WORD                : return "RESERVED_WORD";
		case TokenTypes.RESERVED_WORD_2              : return "RESERVED_WORD_2";

		case TokenTypes.FUNCTION                     : return "FUNCTION";

		case TokenTypes.LITERAL_BOOLEAN              : return "LITERAL_BOOLEAN";
		case TokenTypes.LITERAL_NUMBER_DECIMAL_INT   : return "LITERAL_NUMBER_DECIMAL_INT";
		case TokenTypes.LITERAL_NUMBER_FLOAT         : return "LITERAL_NUMBER_FLOAT";
		case TokenTypes.LITERAL_NUMBER_HEXADECIMAL   : return "LITERAL_NUMBER_HEXADECIMAL";
		case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE  : return "LITERAL_STRING_DOUBLE_QUOTE";
		case TokenTypes.LITERAL_CHAR                 : return "LITERAL_CHAR";
		case TokenTypes.LITERAL_BACKQUOTE            : return "LITERAL_BACKQUOTE";

		case TokenTypes.DATA_TYPE                    : return "DATA_TYPE";

		case TokenTypes.VARIABLE                     : return "VARIABLE";

		case TokenTypes.REGEX                        : return "REGEX";

		case TokenTypes.ANNOTATION                   : return "ANNOTATION";

		case TokenTypes.IDENTIFIER                   : return "IDENTIFIER";

		case TokenTypes.WHITESPACE                   : return "WHITESPACE";

		case TokenTypes.SEPARATOR                    : return "SEPARATOR";

		case TokenTypes.OPERATOR                     : return "OPERATOR";

		case TokenTypes.PREPROCESSOR                 : return "PREPROCESSOR";

		case TokenTypes.MARKUP_TAG_DELIMITER         : return "MARKUP_TAG_DELIMITER";
		case TokenTypes.MARKUP_TAG_NAME              : return "MARKUP_TAG_NAME";
		case TokenTypes.MARKUP_TAG_ATTRIBUTE         : return "MARKUP_TAG_ATTRIBUTE";
		case TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE   : return "MARKUP_TAG_ATTRIBUTE_VALUE";
		case TokenTypes.MARKUP_PROCESSING_INSTRUCTION: return "MARKUP_PROCESSING_INSTRUCTION";
		case TokenTypes.MARKUP_CDATA                 : return "MARKUP_CDATA";

		case TokenTypes.ERROR_IDENTIFIER             : return "ERROR_IDENTIFIER";
		case TokenTypes.ERROR_NUMBER_FORMAT          : return "ERROR_NUMBER_FORMAT";
		case TokenTypes.ERROR_STRING_DOUBLE          : return "ERROR_STRING_DOUBLE";
		case TokenTypes.ERROR_CHAR                   : return "ERROR_CHAR";

		case TokenTypes.NUM_TOKEN_TYPES              : return "NUM_TOKEN_TYPES";
		}
		return "-UNKNOWN-";
	}
}



