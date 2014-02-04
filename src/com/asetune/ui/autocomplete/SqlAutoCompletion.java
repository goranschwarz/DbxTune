package com.asetune.ui.autocomplete;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.RoundRobinAutoCompletion;

public class SqlAutoCompletion
//extends AutoCompletion
extends RoundRobinAutoCompletion
{
	public SqlAutoCompletion(CompletionProvider provider)
	{
		super(provider);
	}

	// FIXME: insert, update, delete
	// if INSERT: <insert into tabname> (c1, c2, c3 ,c4) 
	//            values(
	//                  '',   -- c1 - varchar(30), does NOT allow null
	//                  -1,   -- c2 - int,         does NOT allow null
	//                  null, -- c4 - int,         allow null
	//                  null, -- c4 - int,         allow null
	//            )
	//
	// if DELETE: <delete from tabname> 
	//             where pk1 = '' -- pk1 - varchar(30)
	//               and pk2 = -1 -- pk2 - int
	//
	// if UPDATE: <update tabname>
	//                set c1 = '',    -- c1 - varchar(30), does NOT allow null
	//                    c2 = -1,    -- c2 - int,         does NOT allow null
	//                    c3 = null,  -- c3 - int,         allow null
	//                    c4 = null   -- c4 - int,         allow null
	//             where pk1 = '' -- pk1 - varchar(30)
	//               and pk2 = -1 -- pk2 - int
	// 
	// if SELECT: <select tabname>
	//             select
	//                    c1, -- c1 - varchar(30), does NOT allow null
	//                    c2, -- c2 - int,         does NOT allow null
	//                    c3, -- c3 - int,         allow null
	//                    c4  -- c4 - int,         allow null
	//             from <tabname>
	//             where pk1 = '' -- pk1 - varchar(30)
	//               and pk2 = -1 -- pk2 - int
	//
//	@Override
//	protected void insertCompletion(Completion c, boolean typedParamListStartChar) 
//	{
//		super.insertCompletion(c, typedParamListStartChar);
//	}

//	/**
//	 * Inserts a completion.  Any time a code completion event occurs, the
//	 * actual text insertion happens through this method.
//	 *
//	 * @param c A completion to insert.  This cannot be <code>null</code>.
//	 */
//	@Override
////	protected void insertCompletion(Completion c) 
//	protected void insertCompletion(Completion c, boolean typedParamListStartChar) 
//	{
//		JTextComponent textComp = getTextComponent();
//		String alreadyEntered = c.getAlreadyEntered(textComp);
//		hideChildWindows();
//		Caret caret = textComp.getCaret();
//
//		int dot = caret.getDot();
//		int len = alreadyEntered.length();
//		int start = dot-len;
//		String replacement = getReplacementText(c, textComp.getDocument(), start, len);
//
//		if (c instanceof ReplacementCompletion && textComp instanceof RTextArea)
//		{
//			String complOriginWord = ((ReplacementCompletion)c)._originWord;
//			try
//			{
////System.out.println("BEFORE: RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
//				start = RSyntaxUtilitiesX.getWordStart((RTextArea)textComp, dot, complOriginWord);
//				dot   = RSyntaxUtilitiesX.getWordEnd(  (RTextArea)textComp, dot, complOriginWord);
//			}
//			catch (BadLocationException e)
//			{
//				e.printStackTrace();
//			}
//		}
////System.out.println("RepServerAutoCompletion.insertCompletion(): start='"+start+"', dot='"+dot+"', replacement='"+replacement+"'.");
//		caret.setDot(start);
//		caret.moveDot(dot);
//		textComp.replaceSelection(replacement);
//	}
}

