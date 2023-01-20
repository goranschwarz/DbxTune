/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs.report.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.asetune.CounterController;
import com.asetune.utils.StringUtil;

public class ReportContent
{
	private String _serverName;
//	private String _contentText = "";
	private String _contentShortMessage; // used for Mail message or other "short messages"
	private boolean _isShortMessageHtml = true;
	private String _contentHtml;
	private File   _contentFile;
	private File   _lastSavedReportFile;
	private boolean _hasNothingToReport = false;

	public String getServerName()   { return _serverName  == null ? "" : _serverName; }
//	public String getReportAsText() { return _contentText == null ? "" : _contentText; }
//	public String getReportAsHtml() { return _contentHtml == null ? "" : _contentHtml; }

	public void setServerName(String serverName) { _serverName = serverName; }
//	public void setReportAsText(String text)     { _contentText = text; }
	public void setReportAsHtml(String text)     { _contentHtml = text; }

	/**
	 * Get "serverDisplayName" or "serverName" 
	 * @return
	 */
	public String getDisplayOrServerName()
	{
		// FIXME: Should we grab "serverDisplayName" from CounterController... or should we start to store info like we do in DbxCentral ...

		String name = null;
		if (CounterController.hasInstance())
		{
			name = CounterController.getInstance().getServerDisplayName();

			if (StringUtil.isNullOrBlank(name))
				name = CounterController.getInstance().getServerAliasName();
		}
		
		if (StringUtil.isNullOrBlank(name))
			name = getServerName();
		
		return name;
	}

	
	public void setNothingToReport(boolean b)
	{
		_hasNothingToReport = b;
	}
	public boolean hasNothingToReport()
	{
		return _hasNothingToReport;
	}

	/**
	 * Get any short message that can be used for email message and other shorter messages writers
	 * <p>
	 * NOTE: The full Report content can still be attached in emails etc...<br>
	 * use <code>getReportFile()</code> to get a handle to the full report
	 * 
	 * @param message
	 * @return never null, if short report is not available, then "" will be returned. 
	 */
	public String getShortMessage()
	throws IOException
	{
		if (_contentShortMessage == null)
			return "";

		return _contentShortMessage;
	}

	/**
	 * Set any short message that can be used for email message and other shorter messages writers
	 * <p>
	 * NOTE: The full Report content can still be attached in emails etc...
	 * 
	 * @param message
	 */
	public void setShortMessage(String message)
	{
		_contentShortMessage = message;
	}

	/** Does the short message (if we have any" is of HTML Content */
	public boolean isShortMessageOfHtml()
	{
		return _isShortMessageHtml;
	}

	/** Indicate the the short message (if we have any" is of HTML Content */
	public void setShortMessageOfHtml(boolean isShortMessageHtml)
	{
		_isShortMessageHtml = isShortMessageHtml;
	}



	public String getReportAsHtml()
	throws IOException
	{
		if (_contentHtml != null)
			return _contentHtml; 
		
		if (hasReportFile())
		{
			return FileUtils.readFileToString(_contentFile, StandardCharsets.UTF_8);
		}
		
		return "";
	}

	
	public File getLastSavedReportFile()
	{
		return _lastSavedReportFile;
	}

	public void saveReportAsFile(File file)
	throws IOException
	{
		if (_contentFile != null)
		{
			FileUtils.copyFile(_contentFile, file);
			_lastSavedReportFile = file;
		}
		else if (_contentHtml != null)
		{
			FileUtils.write(file, _contentHtml, StandardCharsets.UTF_8.name());
			_lastSavedReportFile = file;
		}
		else
		{
			throw new IOException("No report content has been set. [_contentFile=" + _contentFile + ", _contentHtml=" + _contentHtml + "]");
		}
	}
	
	public void toPrintStream(PrintStream out)
	throws IOException
	{
		if (_contentFile != null)
		{
			// AutoClose the FileInputStream
			try( FileInputStream fis = new FileInputStream(_contentFile) )
			{
				int oneByte;
				while ((oneByte = fis.read()) != -1) 
				{
					out.write(oneByte);
				}
			}
		}
		else if (_contentHtml != null)
		{
			out.append(_contentHtml);
		}
		else
		{
			throw new IOException("No report content has been set. [_contentFile=" + _contentFile + ", _contentHtml=" + _contentHtml + "]");
		}
		out.flush();
	}

	public boolean hasReportFile()
	{
		return _contentFile != null;
	}
	public void setReportFile(File file)
	{
		_contentFile = file;
	}
	public File getReportFile()
	{
		return _contentFile;
	}
}
