package com.asetune.central.pcs.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.asetune.central.DbxTuneCentral;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// from the file: config/SERVER_LIST
//
// ##===========================================================================
// ## Fields in this file
// ## 1 - ASE SERVERNAME
// ## 2 - 1=Enabled, 0=Disabled
// ## 3 - Some explanation for the role of this server
// ##===========================================================================
// 
// PROD_A_ASE ; 1 ; PROD Active Side
// PROD_B_ASE ; 1 ; PROD Standby Side
// DEV_ASE    ; 1 ; DEV
// SYS_ASE    ; 0 ; SYS
// INT_ASE    ; 0 ; INT
// STAGE_ASE  ; 0 ; STAGE
// PROD_A1_ASE; 1 ; PROD Active Side - New
// PROD_B1_ASE; 1 ; PROD Standby Side - New



@JsonPropertyOrder(value = {"serverName", "enabled", "description", "extraInfo"}, alphabetic = true)
public class DbxCentralServerDescription
{
	private String  _serverName;
	private boolean _isEnabled;
	private String  _description;
	private String  _extraInfo;

	public String  getServerName () { return _serverName ;  }
	public boolean isEnabled     () { return _isEnabled  ;  }
	public String  getDescription() { return _description;  }
	public String  getExtraInfo  () { return _extraInfo;  }

	public void setServerName (String  serverName ) { _serverName  = serverName ; }
	public void setEnabled    (boolean enabled    ) { _isEnabled   = enabled    ; }
	public void setDescription(String  description) { _description = description; }
	public void setExtraInfo  (String  extraInfo)   { _extraInfo   = extraInfo  ; }

	public DbxCentralServerDescription(String serverName, boolean enabled, String description, String extraInfo)
	{
		super();

		_serverName  = serverName;
		_isEnabled   = enabled;
		_description = description;
		_extraInfo   = extraInfo;
	}


	public static String getDefaultFile()
	{
		String filename = StringUtil.hasValue(DbxTuneCentral.getAppConfDir()) ? DbxTuneCentral.getAppConfDir() + "/SERVER_LIST" : "conf/SERVER_LIST";
		return filename;
	}
	
	public static Map<String, DbxCentralServerDescription> getFromFile()
	throws IOException
	{
		return getFromFile(null);
	}

	/**
	 * Returns all entries from file 'conf/SERVER_LIST' as a Map in the order of the file.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Map<String, DbxCentralServerDescription> getFromFile(String filename)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getDefaultFile();

		LinkedHashMap<String, DbxCentralServerDescription> map = new LinkedHashMap<>();

		File f = new File(filename);
		if (f.exists())
		{
			try( BufferedReader bufferreader = new BufferedReader(new FileReader(f)) )
			{
				String line;
				while ((line = bufferreader.readLine()) != null)
				{
					line = line.trim();
					if (line.startsWith("#") || line.equals(""))
						continue;
					
					String[] sa = line.split(";"); // FIX NEW "TABLE" LAYOUT
					if (sa.length > 0)
					{
						String  serverName  = sa[0].trim();
						boolean enabled     = sa.length > 1 ? sa[1].trim().equalsIgnoreCase("1") : false;
						String  description = sa.length > 2 ? sa[2].trim()                       : "";
						String  extraInfo   = sa.length > 3 ? sa[3].trim()                       : "";
						
						DbxCentralServerDescription entry = new DbxCentralServerDescription(serverName, enabled, description, extraInfo);
						map.put(entry.getServerName(), entry);
					}
				}
			}
		}
		else
		{
			throw new FileNotFoundException("DbxCentral Server Configuration File '"+filename+"' did not exist.");
		}
		
		return map;
	}

	/**
	 * Remove or comment-out a server in the SERVER_LIST
	 * 
	 * @param filename
	 * @param removeServerName
	 * 
	 * @throws IOException
	 */
	public static void removeFromFile(String filename, String removeServerName, boolean asComment)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getDefaultFile();

		File f = new File(filename);
		if (f.exists())
		{
			List<String> readList  = FileUtils.readLines(f, Charset.defaultCharset());
			List<String> writeList = new ArrayList<>();

			for (String line : readList)
			{
				line = line.trim();
				if (line.startsWith("#") || line.equals(""))
				{
					writeList.add(line); // write origin line
					continue;
				}
				
				String[] sa = line.split(";");
				if (sa.length > 0)
				{
					String rowServerName  = sa[0].trim();
					if (rowServerName.equals(removeServerName))
					{
						if (asComment)
							writeList.add("#" + line); // Comment out this line
					}
					else
						writeList.add(line); // write origin line
				}
				else
					writeList.add(line); // write origin line
			}
			
			// WRITE the file back again
			FileUtils.writeLines(f, writeList);
		}
		else
		{
			throw new FileNotFoundException("DbxCentral Server Configuration File '"+filename+"' did not exist.");
		}
	}
}
