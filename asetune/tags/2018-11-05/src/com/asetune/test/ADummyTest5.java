package com.asetune.test;

import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.StringUtil;

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
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/asetune_recordings_temp/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/asetune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-1;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/asetune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-2;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
		System.out.println(getNewH2SpillOverUrl("jdbc:h2:file:C:/projects/asetune_recordings_temp/spam_prod_b_2014-09-23.15-SPILL-OVER-DB-2_xxxxx;IFEXISTS=TRUE;AUTO_SERVER=TRUE"));
	}

}
