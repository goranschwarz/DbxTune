package com.asetune.sql;

public class DbmsDataTypeResolver
{
	// TODO: Implement this
	
	// Empty for the moment...
	// This should have methods to translate "source" DBMS column types to "other" datatypes
	
	// Primary called from: CounterSample.getSample(...)
	//    - cm.createResultSetMetaData() -> new ResultSetMetaDataCached() -> remap()
	
	// Instead of using DBMS Product name, a DbmsDataTypeResolver should be instansiated in CounterController or similar
}
