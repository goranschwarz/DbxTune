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
package com.asetune.tools.sqlw.msg;

import com.asetune.sql.pipe.IPipeCommand;
import com.asetune.sql.pipe.PipeMessage;
import com.asetune.sql.pipe.PipeMessage.Severity;
import com.asetune.utils.ColorUtils;

public class JPipeMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JPipeMessage(final PipeMessage pmsg, IPipeCommand pipeCommand)
	{
		super(pmsg.toString(), pipeCommand.getSqlString());

		Severity s = pmsg.getSeverity();
		if      (Severity.DEBUG  .equals(s)) setForeground(ColorUtils.DEBUG);
		else if (Severity.INFO   .equals(s)) /* do nothing */;
		else if (Severity.WARNING.equals(s)) setForeground(ColorUtils.WARNING);
		else if (Severity.ERROR  .equals(s)) setForeground(ColorUtils.ERROR);
	}
}
