package com.asetune.sql;

public class DbmsVersionHelperSimple
implements IDbmsVersionHelper
{
	@Override
	public String versionNumToStr(long version, int major, int minor, int maintenance, int servicePack, int patchLevel)
	{
		String verStr = "";
		if (maintenance <= 0)
			verStr = major + "." + minor;
		else
			verStr = major + "." + minor + "." + maintenance;

		return verStr;
	}
}
