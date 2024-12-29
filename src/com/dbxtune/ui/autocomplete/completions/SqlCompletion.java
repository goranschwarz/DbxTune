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
package com.dbxtune.ui.autocomplete.completions;

import java.io.Serializable;

import org.fife.ui.autocomplete.CompletionProvider;

import com.dbxtune.utils.StringUtil;

public class SqlCompletion
extends ShorthandCompletionX
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public SqlCompletion(CompletionProvider provider, String inputText, String replacementText)
	{
		super(provider, inputText, replacementText);
	}

	protected String stripMultiLineHtml(String str)
	{
//		return str;
//		System.out.println("stripMultiLineHtml(): >>>> "+str);
//		System.out.println("stripMultiLineHtml(): <<<< "+StringUtil.stripHtml(str));
		return StringUtil.stripHtml(str);
	}
}
