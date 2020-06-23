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
import java.util.List;

public interface IMessageAware
{
	boolean hasMessages();
	List<Message> getMessages();
	void clearMessages();

	void addMessages(List<Message> msgList);
	void addMessage(Message msg);

	void addTraceMessage  (String msg);
	void addDebugMessage  (String msg);
	void addDryRunMessage (String msg);
	void addInfoMessage   (String msg);
	void addWarningMessage(String msg);
	void addErrorMessage  (String msg);
	void addPlainMessage  (String msg);

	void      setGuiOwner(Component owner);
	Component getGuiOwner();
	Window    getGuiOwnerAsWindow();
}
