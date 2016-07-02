package com.asetune.pcs;

public class ObjectLookupQueueEntry
{
	public String _dbname;
	public String _objectName;
	public String _source;
	public String _dependParent;
	public int    _dependLevel;

	public ObjectLookupQueueEntry(String dbname, String objectName, String source, String dependParent, int dependLevel)
	{
		_dbname       = dbname;
		_objectName   = objectName;
		_source       = source;
		_dependParent = dependParent;
		_dependLevel  = dependLevel;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(_dbname).append(":").append(_objectName);
		return sb.toString(); 
	}
}
