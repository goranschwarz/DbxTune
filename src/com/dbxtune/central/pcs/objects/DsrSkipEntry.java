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
package com.dbxtune.central.pcs.objects;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"srvName", "className", "entryType", "numberVal", "stringVal", "description", "sqlTextExample"}, alphabetic = true)
public class DsrSkipEntry
{
	private String    _srvName        ;
	private String    _className      ;
	private String    _entryType      ;
//	private long      _numberVal      ;
	private String    _stringVal      ;
	private String    _description    ;
	private String    _sqlTextExample ;

	public String    getSrvName       () { return _srvName        ;  }
	public String    getClassName     () { return _className      ;  }
	public String    getEntryType     () { return _entryType      ;  }
//	public long      getNumberVal     () { return _numberVal      ;  }
	public String    getStringVal     () { return _stringVal      ;  }
	public String    getDescription   () { return _description    ;  }
	public String    getSqlTextExample() { return _sqlTextExample ;  }

	public void setSrvName       (String     srvName        ) { _srvName        = srvName        ; }
	public void setClassName     (String     className      ) { _className      = className      ; }
	public void setEntryType     (String     entryType      ) { _entryType      = entryType      ; }
//	public void setNumberVal     (long       numberVal      ) { _numberVal      = numberVal      ; }
	public void setStringVal     (String     stringVal      ) { _stringVal      = stringVal      ; }
	public void setDescription   (String     description    ) { _description    = description    ; }
	public void setSqlTextExample(String     sqlTextExample ) { _sqlTextExample = sqlTextExample ; }

	public DsrSkipEntry()
	{
		// JSON Deserializer needs this
	}
	
//	public DsrSkipEntry(String srvName, String className,String entryType, long numberVal, String stringVal, String sqlTextExample)
	public DsrSkipEntry(String srvName, String className,String entryType, String stringVal, String description, String sqlTextExample)
	{
		super();

		_srvName        = srvName        ;
		_className      = className      ;
		_entryType      = entryType      ;
//		_numberVal      = numberVal      ;
		_stringVal      = stringVal      ;
		_description    = description    ;
		_sqlTextExample = sqlTextExample ;
		
	}
	
	/**
	 * Validate that we have mandatory fields!
	 * @return null on OK, otherwise a error message
	 */
	public String validateMandatoryFields()
	{
		if (StringUtil.isNullOrBlank(_srvName) || StringUtil.isNullOrBlank(_className) || StringUtil.isNullOrBlank(_entryType) || StringUtil.isNullOrBlank(_stringVal))
		{
			return "Missing mandatory parameter(s), all of the following must have values: srvName='" + _srvName + "', className='" + _className + "', entryType='" + _entryType + "', stringVal='" + _stringVal + "'.";
		}

		return null;
	}
}
