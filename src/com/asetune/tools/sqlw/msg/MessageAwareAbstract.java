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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.tools.sqlw.msg;

import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import com.asetune.tools.sqlw.msg.Message.Severity;

public class MessageAwareAbstract 
implements IMessageAware
{
	protected List<Message> _msgList = new ArrayList<>();
	protected Component _guiOwner;

	@Override 
	public boolean hasMessages()
	{
		if (_msgList == null)
			return false;

		return _msgList.size() > 0;
	}
	
	@Override 
	public List<Message> getMessages()
	{
		return _msgList;
	}

	@Override 
	public void clearMessages()
	{
		_msgList = new ArrayList<>();
	}
	
	@Override public void addMessages      (List<Message> msgList) { _msgList.addAll(msgList); }
	@Override public void addMessage       (Message msg)           { _msgList.add(msg); }
	
//	@Override public void addDebugMessage  (String msg) { _msgList.add(new Message(Severity.DEBUG  , msg)); }
//	@Override public void addInfoMessage   (String msg) { _msgList.add(new Message(Severity.INFO   , msg)); }
//	@Override public void addWarningMessage(String msg) { _msgList.add(new Message(Severity.WARNING, msg)); }
//	@Override public void addErrorMessage  (String msg) { _msgList.add(new Message(Severity.ERROR  , msg)); }
//	@Override public void addPlainMessage  (String msg) { _msgList.add(new Message(Severity.PLAIN  , msg)); }

	private int     _debugLevel = 0;
	private boolean _isToStdoutEnabled = false;

	public void setMessageDebugLevel(int level) { _debugLevel        = level; }
	public void setMessageToStdout(boolean val) { _isToStdoutEnabled = val; }

	public boolean isStdoutOutEnabled() { return _isToStdoutEnabled; }
	public boolean isTraceEnabled()   { return _debugLevel >= 2; }
	public boolean isDebugEnabled()   { return _debugLevel >= 1; }
	public boolean isDryRunEnabled()  { return true; }
	public boolean isInfoEnabled()    { return true; }
	public boolean isWarningEnabled() { return true; }
	public boolean isErrorEnabled()   { return true; }

	@Override public void addTraceMessage  (String msg) { if (isTraceEnabled  ()) _msgList.add(new Message(Severity.TRACE  , msg)); if (isStdoutOutEnabled()) System.out.println("TRACE: "   + msg); }
	@Override public void addDebugMessage  (String msg) { if (isDebugEnabled  ()) _msgList.add(new Message(Severity.DEBUG  , msg)); if (isStdoutOutEnabled()) System.out.println("DEBUG: "   + msg); }
	@Override public void addDryRunMessage (String msg) { if (isDryRunEnabled ()) _msgList.add(new Message(Severity.DRYRUN , msg)); if (isStdoutOutEnabled()) System.out.println("DRYRUN: "  + msg); }
	@Override public void addInfoMessage   (String msg) { if (isInfoEnabled   ()) _msgList.add(new Message(Severity.INFO   , msg)); if (isStdoutOutEnabled()) System.out.println("INFO: "    + msg); }
	@Override public void addWarningMessage(String msg) { if (isWarningEnabled()) _msgList.add(new Message(Severity.WARNING, msg)); if (isStdoutOutEnabled()) System.out.println("WARNING: " + msg); }
	@Override public void addErrorMessage  (String msg) { if (isErrorEnabled  ()) _msgList.add(new Message(Severity.ERROR  , msg)); if (isStdoutOutEnabled()) System.out.println("ERROR: "   + msg); }
	@Override public void addPlainMessage  (String msg) { _msgList.add(new Message(Severity.PLAIN  , msg)); if (isStdoutOutEnabled()) System.out.println("PLAIN: "   + msg); }


	@Override
	public void setGuiOwner(Component guiOwner)
	{
		_guiOwner = guiOwner;
	}

	@Override
	public Component getGuiOwner()
	{
		return _guiOwner;
	}

	@Override
	public Window getGuiOwnerAsWindow()
	{
		Component tmpGuiOwner = getGuiOwner();
		Window guiOwner = null;
		if (tmpGuiOwner != null && tmpGuiOwner instanceof Window)
			guiOwner = (Window) tmpGuiOwner;

		return guiOwner;
	}
	
	
}
