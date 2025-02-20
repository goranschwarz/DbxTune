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
package com.dbxtune.hostmon;

import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class MonitorDiskSpaceAllOs
extends HostMonitor
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MonitorDiskSpaceAllOs()
	{
		this(-1, null);
	}
	public MonitorDiskSpaceAllOs(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorDiskSpaceAllOs";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		
		if (StringUtil.hasValue(cmd))
			return cmd;
		
		if (isConnectedToVendor(OsVendor.Windows))
		{
//			return "powershell \"gwmi win32_logicaldisk | Format-Table -HideTableHeaders"
//					+ " DeviceId"
//					+ ", @{n='SizeKb'; e={[math]::Round($_.Size/1KB,2)}}"
//					+ ", @{n='UsedKb'; e={[math]::Round(($_.Size-$_.FreeSpace)/1KB)}}"
//					+ ", @{n='AvailableKb';e={[math]::Round($_.FreeSpace/1KB,2)}}"
//					+ ", @{n='UsedPct'; e={[math]::Round(($_.Size-$_.FreeSpace)/$_.Size*100.0,2)}}"
//				//	+ ", DeviceId" // use DeviceId both as 'FileSystem' and 'MountedOn'
//					+ ", @{n='MountedOn'; e={($_.DeviceId+' ['+$_.VolumeName+']').Replace('[]','').Replace(' ','&nbsp;')}}" // MountedOn == Can be used to get: "D: [Disk Label]"
//					+ "\" ";
			
			return "powershell \"Get-CimInstance Win32_LogicalDisk | Format-Table -HideTableHeaders"
					+ " DeviceId"
					+ ", @{n='SizeKb'; e={[math]::Round($_.Size/1KB,2)}}"
					+ ", @{n='UsedKb'; e={[math]::Round(($_.Size-$_.FreeSpace)/1KB)}}"
					+ ", @{n='AvailableKb';e={[math]::Round($_.FreeSpace/1KB,2)}}"
					+ ", @{n='UsedPct'; e={[math]::Round(($_.Size-$_.FreeSpace)/$_.Size*100.0,2)}}"
					+ ", @{n='MountedOn'; e={($_.DeviceId+' ['+$_.VolumeName+']').Replace('[]','').Replace(' ','&nbsp;')}}" // MountedOn == Can be used to get: "D: [Disk Label]"
					+ "\" ";

			// NOTE: the reader splits on "whitespace", so simulate spaces with "&nbsp;", which will be translated back again in parseRow() method 
			
			// TODO; Possibly use: Get-PSDrive -PSProvider FileSystem | Select-Object Name, @{Name="Used(KB)";Expression={($_.Used / 1KB).ToString("F2")}}, @{Name="Free(KB)";Expression={($_.Free / 1KB).ToString("F2")}}, @{Name="Total(KB)";Expression={($_.Used + $_.Free) / 1KB}}
			//                 OR: Get-CimInstance Win32_LogicalDisk | Where-Object {$_.DriveType -eq 3} | Select-Object DeviceID, @{Name="FreeSpace(KB)";Expression={($_.FreeSpace / 1KB).ToString("F2")}}, @{Name="Size(KB)";Expression={($_.Size / 1KB).ToString("F2")}}
			//                     Get-CimInstance Win32_LogicalDisk | Where-Object {$_.DriveType -eq 3} | Select-Object DeviceID, VolumeName, @{Name="FreeSpace(KB)";Expression={($_.FreeSpace / 1KB).ToString("F2")}}, @{Name="Size(KB)";Expression={($_.Size / 1KB).ToString("F2")}}
			//        since it do NOT require Administrator Privileges

		}
		else
		{
			return "df -kP | sed '1d'";
		}
	}

//	gorans@gorans-ub2:/etc/postgresql/9.5/main$ df -k
//	Filesystem     1K-blocks     Used Available Use% Mounted on
//	udev             8137404        0   8137404   0% /dev
//	tmpfs            1631564   157804   1473760  10% /run
//	/dev/sda2       98334332 91646324   1669796  99% /
//	tmpfs            8157816      180   8157636   1% /dev/shm
//	tmpfs               5120        4      5116   1% /run/lock
//	tmpfs            8157816        0   8157816   0% /sys/fs/cgroup
//	/dev/sda1         523248     3480    519768   1% /boot/efi
//	cgmfs                100        0       100   0% /run/cgmanager/fs
//	tmpfs            1631564       72   1631492   1% /run/user/1000
	
	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("Filesystem",       1, 1, false, 100,   "Type of filesystem");
		md.addLongColumn("Size-KB",          2, 2, false,        "Total size of usable space (KB) in the file system");
		md.addLongColumn("Used-KB",          3, 3, false,        "Amount of space used in KB");
		md.addLongColumn("Available-KB",     4, 4, false,        "Amount of space available for use, in KB");
		md.addIntColumn ("Size-MB",          5, 0, false,        "Total size of usable space (MB) in the file system");
		md.addIntColumn ("Used-MB",          6, 0, false,        "Amount of space used in MB");
		md.addIntColumn ("Available-MB",     7, 0, false,        "Amount of space available for use, in MB");
		md.addDecColumn ("UsedPct",          8, 5, true,  5, 2,  "<html>Amount of space used, as a percentage of the total capacity<br><br><b>Formula:</b> 100.0 - (availableKB / sizeKB * 100.0)</html>");
		md.addStrColumn ("MountedOn",        9, 6, true,  400,   "Mount point");
                                                 
		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPercentCol("UsedPct");

		md.setPkCol("MountedOn");
		
		md.setDiffCol( "Used-KB", "Available-KB", "Used-MB", "Available-MB" );

		// Skip "loop" devices
		if (Configuration.getCombinedConfiguration().getBooleanProperty("hostmon.MonitorDiskSpace.skip.loop.devices", true))
		{
//			md.setSkipRows("Filesystem", "^loop[0-9]+");
			md.setSkipRows("Filesystem", "^/dev/loop[0-9]+");
		}

		// Skip "tmpfs" devices
		if (Configuration.getCombinedConfiguration().getBooleanProperty("hostmon.MonitorDiskSpace.skip.tmpfs.devices", true))
		{
			md.setSkipRows("Filesystem", "^devtmpfs");
			md.setSkipRows("Filesystem", "^tmpfs");
		}

		// Skip the header line
//		md.setSkipRows("Filesystem", "Filesystem");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if (type == SshConnection.STDERR_DATA)
			return null;

		// Let "super" do it's intended work
		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		// Strip off *trailing* '%' characters in 'UsedPct' fields
		int usedPct_pos = 4; // 5-1
		if (usedPct_pos < data.length)
		{
			if (data[usedPct_pos].endsWith("%"))
				data[usedPct_pos] = data[usedPct_pos].substring(0, data[usedPct_pos].length()-1);
		}

		// in Windows for column 'MountedOn' we use '&nbsp;' to simulate spaces... since we split on spaces 
		// Change them back to normal spaces 
		int mountPoint_pos = 5; // 6-1
		if (mountPoint_pos < data.length)
		{
			if (data[mountPoint_pos].contains("&nbsp;"))
				data[mountPoint_pos] = data[mountPoint_pos].replace("&nbsp;", " ").trim();
		}

		return data;
	}
	
	
}
