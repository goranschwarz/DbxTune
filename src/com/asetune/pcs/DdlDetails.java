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
package com.asetune.pcs;

import java.sql.Timestamp;
import java.util.List;

import com.asetune.utils.StringUtil;

public class DdlDetails
{
	private String    _searchDbname  = null;
	private String    _dbname        = null;
	private String    _owner         = null;
	private String    _objectName    = null;
	private String    _type          = null;
	private Timestamp _crdate        = null;
	private Timestamp _sampleTime    = null;
	private String    _source        = null; // Subsystem Source
	private String    _dependParent  = null; // if _dependLevel is above 0, then I want to trace back... 
	private int       _dependLevel   = 0;    // 0=First level, 1=second level etc...
	private List<String> _dependList = null;
	private String    _objectText    = null;
	private String    _dependsText   = null;
	private String    _optdiagText   = null;
	private String    _extraInfoText = null;

	private boolean   _doSleepOption = true;

	public DdlDetails()
	{
	}
	public DdlDetails(String dbname, String objectName)
	{
		_searchDbname = dbname;
		_dbname       = dbname;
		_objectName   = objectName;
	}

	public String    getSearchDbname()  { return _searchDbname != null ? _searchDbname : _dbname; }
	public String    getDbname()        { return _dbname; }
	public String    getOwner()         { return _owner; }
	public String    getObjectName()    { return _objectName; }
	public String    getType()          { return _type; }
	public Timestamp getCrdate()        { return _crdate; }
	public Timestamp getSampleTime()    { return _sampleTime; }
	public String    getSource()        { return _source; }
	public String    getDependParent()  { return _dependParent; }
	public int       getDependLevel()   { return _dependLevel; }
	public List<String> getDependList() { return _dependList; }
	public String    getObjectText()    { return _objectText; }
	public String    getDependsText()   { return _dependsText; }
	public String    getOptdiagText()   { return _optdiagText; }
	public String    getExtraInfoText() { return _extraInfoText; }

	public boolean   hasObjectText()    { return _objectText    != null; }
	public boolean   hasDependsText()   { return _dependsText   != null; }
	public boolean   hasOptdiagText()   { return _optdiagText   != null; }
	public boolean   hasExtraInfoText() { return _extraInfoText != null; }
	public boolean   isSleepOptionSet() { return _doSleepOption; }

	public void setSearchDbname (String    searchDbname){ _searchDbname  = searchDbname == null ? null : searchDbname.trim(); }
	public void setDbname       (String    dbname)      { _dbname        = dbname       == null ? null : dbname      .trim(); }
	public void setOwner        (String    owner)       { _owner         = owner        == null ? null : owner       .trim(); }
	public void setObjectName   (String    objectName)  { _objectName    = objectName   == null ? null : objectName  .trim(); }
	public void setType         (String    type)        { _type          = type         == null ? null : type        .trim(); }
	public void setCrdate       (Timestamp crdate)      { _crdate        = crdate; }
	public void setSampleTime   (Timestamp sampleTime)  { _sampleTime    = sampleTime; }
	public void setSource       (String    source)      { _source        = source; }
	public void setDependParent (String    dependParent){ _dependParent  = dependParent; }
	public void setDependLevel  (int       dependLevel) { _dependLevel   = dependLevel; }
	public void setDependList(List<String> dependList)  { _dependList    = dependList; }
	public void setObjectText   (String    objectText)  { _objectText    = objectText; }
	public void setDependsText  (String    dependsText) { _dependsText   = dependsText; }
	public void setOptdiagText  (String    optdiagText) { _optdiagText   = optdiagText; }
	public void setExtraInfoText(String  extraInfoText) { _extraInfoText = extraInfoText; }
	public void setSleepOption  (boolean doSleepOption) { _doSleepOption = doSleepOption; }

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
	
	public String toStringDebug()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("====BEGIN==================================================").append("\n");
		sb.append(" Dbname      = '").append( getDbname()      ).append("'\n");
		sb.append(" Owner       = '").append( getOwner()       ).append("'\n");
		sb.append(" ObjectName  = '").append( getObjectName()  ).append("'\n");
		sb.append(" Type        = '").append( getType()        ).append("'\n");
		sb.append(" CrDate      = '").append( getCrdate()      ).append("'\n");
		sb.append(" SampleTime  = '").append( getSampleTime()  ).append("'\n");
		sb.append(" SubsSource  = '").append( getSource()      ).append("'\n");
		sb.append(" DependParent= '").append( getDependParent()).append("'\n");
		sb.append(" DependLevel = ") .append( getDependLevel() ).append("\n");
		sb.append(" DependList  = '").append( getDependList()  ).append("'\n");
		sb.append("-----ObjectText--------------------------------------------").append("\n");
		sb.append(" ").append( getObjectText() ).append("\n");
		sb.append("-----DependsText-------------------------------------------").append("\n");
		sb.append("").append( getDependsText() ).append("\n");
		sb.append("-----OptDiagText-------------------------------------------").append("\n");
		sb.append("").append( getOptdiagText() ).append("\n");
		sb.append("-----ExtraInfoText-----------------------------------------").append("\n");
		sb.append("").append( getExtraInfoText() ).append("\n");
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
