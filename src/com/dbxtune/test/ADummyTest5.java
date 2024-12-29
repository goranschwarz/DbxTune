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
package com.dbxtune.test;

import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.StringUtil;

public class ADummyTest5
{

	public static String getNewH2SpillOverUrl(String lastUsedUrl)
	{
		H2UrlHelper urlHelper = new H2UrlHelper(lastUsedUrl);
		
		String currentFilename = urlHelper.getFilename();
		int spillDbId = 1;
		if (currentFilename.matches(".*-SPILL-OVER-DB-[0-9]+"))
		{
			int startPos = currentFilename.lastIndexOf("-SPILL-OVER-DB-");
			int endPos = currentFilename.length();
			if (startPos >= 0)
			{
				String filePrefix = currentFilename.substring(0, startPos);
				startPos += "-SPILL-OVER-DB-".length();

				String spillDbIdStr = currentFilename.substring(startPos, endPos);

				// Remove everything but numbers (if there are any)
				spillDbIdStr = spillDbIdStr.replaceAll("[^0-9]", "");

				// Get the new number as an INT
				spillDbId = StringUtil.parseInt(spillDbIdStr, spillDbId) + 1;
				
				// Strip off the "-SPILL-OVER-DB-" from the origin file
				currentFilename = filePrefix;
			}
		}
		currentFilename += "-SPILL-OVER-DB-" + spillDbId;
		
		String localJdbcUrl = urlHelper.getNewUrl(currentFilename);
//System.out.println("---");
//System.out.println(">>> input >>>> |"+lastUsedUrl+"|.");
//System.out.println("<<< output <<< |"+localJdbcUrl+"|.");

		return localJdbcUrl;
	}

	public static void main(String[] args)
	{
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/dbxtune_recordings_temp/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/dbxtune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-1;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/dbxtune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-2;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/dbxtune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-2_xxxxx;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
	}

}
