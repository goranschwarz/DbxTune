package com.asetune.sql;

import com.asetune.utils.Ver;

public class DbmsVersionHelperSybase
implements IDbmsVersionHelper
{
	@Override
	public String versionNumToStr(long version, int major, int minor, int maintenance, int servicePack, int patchLevel)
	{
		return Ver.defaultVersionNumToStr(version, major, minor, maintenance, servicePack, patchLevel);
	}
}
