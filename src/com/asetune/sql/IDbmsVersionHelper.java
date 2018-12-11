package com.asetune.sql;

public interface IDbmsVersionHelper
{
	/**
	 * Produce a Version String that looks like any Vendor Speicific Version String
	 * 
	 * @param version
	 * @param major
	 * @param minor
	 * @param maintenance
	 * @param servicePack
	 * @param patchLevel
	 * @return
	 */
	
	public String versionNumToStr(long version, int major, int minor, int maintenance, int servicePack, int patchLevel);
}
