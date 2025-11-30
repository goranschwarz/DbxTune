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
package com.dbxtune.pcs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.utils.StringUtil;

public class DdlDetails
{
	private String    _searchDbname     = null;
	private String    _searchObjectName = null; // the object name send for lookup, may contain schemaName before the objectName NOT STORED in the DDL Storage
	private String    _dbname           = null;
	private String    _owner            = null; // schema or owner name
	private String    _objectName       = null; // the object name "stripped" from any schemaName
	private int       _objectId         = -1;   // possibly used
	private String    _type             = null;
	private Timestamp _crdate           = null;
	private Timestamp _sampleTime       = null;
	private String    _source           = null; // Subsystem Source
	private String    _dependParent     = null; // if _dependLevel is above 0, then I want to trace back... 
	private int       _dependLevel      = 0;    // 0=First level, 1=second level etc...
	private List<String> _dependList    = null;
	private String    _objectText       = null;
	private String    _dependsText      = null;
	private String    _optdiagText      = null;
	private String    _extraInfoText    = null;
                                        
	private boolean   _doSleepOption    = true;

	// Used for debugging or print extra information
	private boolean _isPrintInfo = false;
	public void    setPrintInfo(boolean b)	{ _isPrintInfo = b; }
	public boolean isPrintInfo()	        { return _isPrintInfo; }

	public DdlDetails()
	{
	}
	public DdlDetails(String dbname, String objectName)
	{
		_searchDbname = dbname;
		_dbname       = dbname;
		_objectName   = objectName;
	}

	public String    getSearchDbname()     { return _searchDbname     != null ? _searchDbname     : _dbname; }
	public String    getSearchObjectName() { return _searchObjectName != null ? _searchObjectName : _objectName; }
	public String    getDbname()           { return _dbname; }
	public String    getOwner()            { return _owner; } // same as getSchemaName()
	public String    getSchemaName()       { return _owner; } // same as getOwner()
	public String    getObjectName()       { return _objectName; }
	public int       getObjectId()         { return _objectId; }
	public String    getType()             { return _type; }
	public Timestamp getCrdate()           { return _crdate; }
	public Timestamp getSampleTime()       { return _sampleTime; }
	public String    getSource()           { return _source; }
	public String    getDependParent()     { return _dependParent; }
	public int       getDependLevel()      { return _dependLevel; }
	public List<String> getDependList()    { return _dependList; }
	public String    getObjectText()       { return _objectText; }
	public String    getDependsText()      { return _dependsText; }
	public String    getOptdiagText()      { return _optdiagText; }
	public String    getExtraInfoText()    { return _extraInfoText; }
                                           
	public boolean   hasObjectText()       { return StringUtil.hasValue( _objectText    ); }
	public boolean   hasDependsText()      { return StringUtil.hasValue( _dependsText   ); }
	public boolean   hasOptdiagText()      { return StringUtil.hasValue( _optdiagText   ); }
	public boolean   hasExtraInfoText()    { return StringUtil.hasValue( _extraInfoText ); }
	public boolean   isSleepOptionSet()    { return _doSleepOption; }

	public void setSearchDbname    (String    searchDbname)    { _searchDbname      = searchDbname     == null ? null : searchDbname    .trim(); }
	public void setSearchObjectName(String    searchObjectName){ _searchObjectName  = searchObjectName == null ? null : searchObjectName.trim(); }
	public void setDbname          (String    dbname)          { _dbname            = dbname           == null ? null : dbname          .trim(); }
	public void setOwner           (String    owner)           { _owner             = owner            == null ? null : owner           .trim(); } // same as setSchemaName()
	public void setSchemaName      (String    owner)           { _owner             = owner            == null ? null : owner           .trim(); } // same as setOwner()
	public void setObjectName      (String    objectName)      { _objectName        = objectName       == null ? null : objectName      .trim(); }
	public void setObjectId        (int       objectId)        { _objectId          = objectId; }
	public void setType            (String    type)            { _type              = type             == null ? null : type            .trim(); }
	public void setCrdate          (Timestamp crdate)          { _crdate            = crdate; }
	public void setSampleTime      (Timestamp sampleTime)      { _sampleTime        = sampleTime; }
	public void setSource          (String    source)          { _source            = source; }
	public void setDependParent    (String    dependParent)    { _dependParent      = dependParent; }
	public void setDependLevel     (int       dependLevel)     { _dependLevel       = dependLevel; }
	public void setDependList      (List<String> dependList)   { _dependList        = dependList; }
	public void setObjectText      (String    objectText)      { _objectText        = objectText; }
	public void setDependsText     (String    dependsText)     { _dependsText       = dependsText; }
	public void setOptdiagText     (String    optdiagText)     { _optdiagText       = optdiagText; }
	public void setExtraInfoText   (String  extraInfoText)     { _extraInfoText     = extraInfoText; }
	public void setSleepOption     (boolean doSleepOption)     { _doSleepOption     = doSleepOption; }

	public void addDependList(String depends)
	{
		if (StringUtil.isNullOrBlank(depends))
			return;

		if (_dependList == null)
			_dependList = new ArrayList<>();

		_dependList.add(depends);
	}
	public void addDependList(List<String> dependList)
	{
		if (dependList == null)
			return;

		if (_dependList == null)
			_dependList = new ArrayList<>();

		_dependList.addAll(dependList); 
	}
	public void addDependsText(String dependsText)
	{
		if (dependsText == null)
			return;

		if (_dependsText == null)
			_dependsText = "";

		_dependsText += dependsText; 
	}
	
	public String getSchemaAndObjectName()
	{
		return getOwner() + "." + getObjectName();
	}

	public String getFullObjectName()
	{
		return getDbname() + "." + getOwner() + "." + getObjectName();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(super.toString()).append(": ")
			.append("dbname='")         .append(getDbname())       .append("', ")
			.append("owner='")          .append(getOwner())        .append("', ")
			.append("object='")         .append(getObjectName())   .append("', ")
			.append("hasObjectText=")   .append(hasObjectText())   .append(", ")
			.append("hasDependsText=")  .append(hasDependsText())  .append(", ")
			.append("hasOptdiagText=")  .append(hasOptdiagText())  .append(", ")
			.append("hasExtraInfoText=").append(hasExtraInfoText()).append(", ")
			.append("isSleepOptionSet=").append(isSleepOptionSet()).append(".")
			;

		return sb.toString();
	}
	
	public String toStringDebug(int maxWidth)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("====BEGIN==================================================").append("\n");
		sb.append(" Dbname      = '").append( getDbname()      ).append("'\n");
		sb.append(" Owner/Schema= '").append( getOwner()       ).append("'\n");
		sb.append(" ObjectName  = '").append( getObjectName()  ).append("'\n");
		sb.append(" Type        = '").append( getType()        ).append("'\n");
		sb.append(" CrDate      = '").append( getCrdate()      ).append("'\n");
		sb.append(" SampleTime  = '").append( getSampleTime()  ).append("'\n");
		sb.append(" SubsSource  = '").append( getSource()      ).append("'\n");
		sb.append(" DependParent= '").append( getDependParent()).append("'\n");
		sb.append(" DependLevel = ") .append( getDependLevel() ).append("\n");
		sb.append(" DependList  = '").append( getDependList()  ).append("'\n");
		sb.append("-----ObjectText--------------------------------------------").append("\n");
		sb.append(" ").append( StringUtil.truncate(getObjectText(), maxWidth, true, null) ).append("\n");
		sb.append("-----DependsText-------------------------------------------").append("\n");
		sb.append("").append( StringUtil.truncate(getDependsText(), maxWidth, true, null) ).append("\n");
		sb.append("-----OptDiagText-------------------------------------------").append("\n");
		sb.append("").append( StringUtil.truncate(getOptdiagText(), maxWidth, true, null) ).append("\n");
		sb.append("-----ExtraInfoText-----------------------------------------").append("\n");
		sb.append("").append( StringUtil.truncate(getExtraInfoText(), maxWidth, true, null) ).append("\n");
		sb.append("____END____________________________________________________").append("\n");
		
		return sb.toString();
	}
	
	public boolean isEmpty()
	{
		boolean objectText_empty    = StringUtil.isNullOrBlank(_objectText);
		boolean dependsText_empty   = StringUtil.isNullOrBlank(_dependsText);
		boolean optdiagText_empty   = StringUtil.isNullOrBlank(_optdiagText);
		boolean extraInfoText_empty = StringUtil.isNullOrBlank(_extraInfoText);

		// Check some other special cases
		if (_objectText != null)
		{
			String objectText = _objectText.trim();

			// If DBCC execution with no results... then consider it to be empty.
			if (objectText.equals("DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role."))
				objectText_empty = true;
		}

		if (objectText_empty && dependsText_empty && optdiagText_empty && extraInfoText_empty)
			return true;
		
		return false;
	}
}
