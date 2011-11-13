package com.asetune.pcs;

import java.sql.Timestamp;

public class DdlDetails
{
	private String    _dbname        = null;
	private String    _owner         = null;
	private String    _objectName    = null;
	private String    _type          = null;
	private Timestamp _crdate        = null;
	private String    _objectText    = null;
	private String    _dependsText   = null;
	private String    _optdiagText   = null;
	private String    _extraInfoText = null;

	public DdlDetails()
	{
	}
	public DdlDetails(String dbname, String objectName)
	{
		_dbname      = dbname;
		_objectName  = objectName;
	}

	public String    getDbname()        { return _dbname; }
	public String    getOwner()         { return _owner; }
	public String    getObjectName()    { return _objectName; }
	public String    getType()          { return _type; }
	public Timestamp getCrdate()        { return _crdate; }
	public String    getObjectText()    { return _objectText; }
	public String    getDependsText()   { return _dependsText; }
	public String    getOptdiagText()   { return _optdiagText; }
	public String    getExtraInfoText() { return _extraInfoText; }

	public void setDbname       (String    dbname)      { _dbname        = dbname     == null ? null : dbname    .trim(); }
	public void setOwner        (String    owner)       { _owner         = owner      == null ? null : owner     .trim(); }
	public void setObjectName   (String    objectName)  { _objectName    = objectName == null ? null : objectName.trim(); }
	public void setType         (String    type)        { _type          = type       == null ? null : type      .trim(); }
	public void setCrdate       (Timestamp crdate)      { _crdate        = crdate; }
	public void setObjectText   (String    objectText)  { _objectText    = objectText; }
	public void setDependsText  (String    dependsText) { _dependsText   = dependsText; }
	public void setOptdiagText  (String    optdiagText) { _optdiagText   = optdiagText; }
	public void setExtraInfoText(String  extraInfoText) { _extraInfoText = extraInfoText; }

	public String getFullObjectName()
	{
		return getDbname() + "." + getOwner() + "." + getObjectName();
	}

	public String toStringDebug()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("====BEGIN==================================================").append("\n");
		sb.append(" Dbname     = '").append( getDbname()     ).append("'\n");
		sb.append(" Owner      = '").append( getOwner()      ).append("'\n");
		sb.append(" ObjectName = '").append( getObjectName() ).append("'\n");
		sb.append(" Type       = '").append( getType()       ).append("'\n");
		sb.append(" Crdate     = '").append( getCrdate()     ).append("'\n");
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
}
