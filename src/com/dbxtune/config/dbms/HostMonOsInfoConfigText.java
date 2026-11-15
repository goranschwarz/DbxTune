/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.config.dbms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.StringUtil;

/**
 * Exact Operating System information (distribution/version/build, hardware, CPU and memory) from the Host Monitoring connection.
 * <p>
 * Same for all DBMS vendors, so it is registered by every <code>XxxConfigText.createAndRegisterAllInstances()</code>.
 * <p>
 * The output starts with normalized <code>Key: value</code> lines (see {@link #parseKeyValues(String)}, used by the DSR),
 * followed by the raw OS output.
 */
public class HostMonOsInfoConfigText
extends DbmsConfigTextAbstract
{
	public static final String CONFIG_NAME = "HostMonOsInfo";

	// Normalized keys (same for all OS types, a key is left out or empty when the OS can't tell)
	public static final String KEY_OS_NAME        = "OS Name";
	public static final String KEY_OS_VERSION     = "OS Version";
	public static final String KEY_KERNEL_BUILD   = "Kernel/Build";
	public static final String KEY_ARCHITECTURE   = "Architecture";
	public static final String KEY_HARDWARE       = "Hardware";
	public static final String KEY_VIRTUALIZATION = "Virtualization";
	public static final String KEY_CPU_MODEL      = "CPU Model";
	public static final String KEY_CPU_SOCKETS    = "CPU Sockets";
	public static final String KEY_CPU_CORES      = "CPU Cores";
	public static final String KEY_CPU_THREADS    = "CPU Threads";
	public static final String KEY_MEMORY_TOTAL   = "Memory Total";
	public static final String KEY_LAST_BOOT      = "Last Boot";

	@Override public    String getTabLabel()                          { return "OS Info"; }
	@Override public    String getName()                              { return CONFIG_NAME; }
	@Override public    String getConfigType()                        { return getName(); }
	@Override protected String getSqlCurrentConfig(DbmsVersionInfo v) { return ""; } // Only done with OS Command
	@Override public    String getSyntaxEditingStyle()                { return SyntaxConstants.SYNTAX_STYLE_NONE; }

	@Override
	public String checkRequirements(DbxConnection conn, HostMonitorConnection hostMonConn)
	{
		if (hostMonConn == null || ! hostMonConn.isConnected())
			return "Host Monitoring is not enabled (or not connected), so no Operating System information is available.";

		return null;
	}

	@Override
	protected String getOsCurrentConfig(DbmsVersionInfo v, String osName)
	{
		if (osName == null)
			osName = "";

		//-------------------------------------------------
		// Windows (CMD or PowerShell as login shell)
		// Sent as -EncodedCommand (Base64 of UTF-16LE), so there are no quoting issues with: cmd.exe, PowerShell or sshd's 'cmd /c "..."'
		//
		// WMI/CIM is often denied for a non-admin HostMon user over SSH (an SSH logon is a network logon, which needs WMI 'Remote Enable').
		// Then fall back to what needs no WMI: registry (OS name/build, BIOS hardware, CPU name), .NET (thread count, total memory)
		// and the 'System Up Time' performance counter (the HostMon user already reads counters with typeperf).
		// CPU sockets/cores are only available from WMI. The WMI error is written in the output, so it can be seen why.
		//-------------------------------------------------
		if (osName.startsWith("Windows"))
		{
			String ps = ""
				+ "$ProgressPreference = 'SilentlyContinue' \n"
				+ "$cimErr = $null; $o = $null; $cs = $null; $c = @() \n"
				+ "try { $o  = Get-CimInstance Win32_OperatingSystem -ErrorAction Stop; "
				+       "$cs = Get-CimInstance Win32_ComputerSystem  -ErrorAction Stop; "
				+       "$c  = @(Get-CimInstance Win32_Processor     -ErrorAction Stop) } "
				+ "catch { $cimErr = $_.Exception.Message; $o = $null; $cs = $null; $c = @() } \n"
				+ "$r    = Get-ItemProperty 'HKLM:\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion' \n"
				+ "$bios = Get-ItemProperty 'HKLM:\\HARDWARE\\DESCRIPTION\\System\\BIOS' -ErrorAction SilentlyContinue \n"
				+ "$cpu0 = Get-ItemProperty 'HKLM:\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0' -ErrorAction SilentlyContinue \n"
				+ "$rel  = if ($r.DisplayVersion) { $r.DisplayVersion } else { $r.ReleaseId } \n"
				// The registry 'ProductName' still says 'Windows 10' on Windows 11 (build 22000 and above)
				+ "$name = if ($o) { $o.Caption } else { $r.ProductName } \n"
				+ "if (-not $o -and [int]$r.CurrentBuild -ge 22000 -and $name -like 'Windows 10*') { $name = $name -replace '^Windows 10', 'Windows 11' } \n"
				+ "if (-not $o -and $name -notlike 'Microsoft*') { $name = 'Microsoft ' + $name } \n" // same as WMI Caption: 'Microsoft Windows Server 2025 Datacenter'
				+ "$ver  = if ($o) { $o.Version } else { '' + $r.CurrentMajorVersionNumber + '.' + $r.CurrentMinorVersionNumber + '.' + $r.CurrentBuild } \n"
				+ "$arch = if ($o) { $o.OSArchitecture } elseif ([Environment]::Is64BitOperatingSystem) { '64-bit' } else { '32-bit' } \n"
				+ "$hw   = if ($cs) { $cs.Manufacturer + ' ' + $cs.Model } else { '' + $bios.SystemManufacturer + ' ' + $bios.SystemProductName } \n"
				+ "$cpuName = if ($c.Count -gt 0) { $c[0].Name } else { $cpu0.ProcessorNameString } \n"
				+ "$sockets = if ($c.Count -gt 0) { $c.Count } else { '' } \n"
				+ "$cores   = if ($c.Count -gt 0) { ($c | Measure-Object -Property NumberOfCores             -Sum).Sum } else { '' } \n"
				+ "$threads = if ($c.Count -gt 0) { ($c | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum } else { [Environment]::ProcessorCount } \n"
				+ "$memBytes = if ($cs) { $cs.TotalPhysicalMemory } else { try { Add-Type -AssemblyName Microsoft.VisualBasic; (New-Object Microsoft.VisualBasic.Devices.ComputerInfo).TotalPhysicalMemory } catch { 0 } } \n"
				+ "$mem  = if ($memBytes -gt 0) { '' + [math]::Round($memBytes / 1GB, 1) + ' GB' } else { '' } \n"
				+ "$boot = if ($o) { $o.LastBootUpTime } else { try { (Get-Date).AddSeconds(-(Get-Counter '\\System\\System Up Time' -ErrorAction Stop).CounterSamples[0].CookedValue) } catch { $null } } \n"
				+ "$bootStr = if ($boot) { $boot.ToString('yyyy-MM-dd HH:mm:ss') } else { '' } \n"
				+ "'" + KEY_OS_NAME        + ":        ' + $name \n"
				+ "'" + KEY_OS_VERSION     + ":     ' + $rel \n"
				+ "'" + KEY_KERNEL_BUILD   + ":   ' + $ver + '.' + $r.UBR \n"
				+ "'" + KEY_ARCHITECTURE   + ":   ' + $arch \n"
				+ "'" + KEY_HARDWARE       + ":       ' + $hw \n"
				+ "'" + KEY_CPU_MODEL      + ":      ' + ('' + $cpuName).Trim() \n"
				+ "'" + KEY_CPU_SOCKETS    + ":    ' + $sockets \n"
				+ "'" + KEY_CPU_CORES      + ":      ' + $cores \n"
				+ "'" + KEY_CPU_THREADS    + ":    ' + $threads \n"
				+ "'" + KEY_MEMORY_TOTAL   + ":   ' + $mem \n"
				+ "'" + KEY_LAST_BOOT      + ":      ' + $bootStr \n"
				+ "'' \n"
				+ "if ($cimErr) { \n"
				+ "  '----- WMI/CIM is not available, values above are from the registry/.NET (CPU sockets/cores need WMI)' \n"
				+ "  'WMI/CIM error: ' + $cimErr \n"
				+ "} else { \n"
				+ "  '----- Win32_OperatingSystem' \n"
				+ "  ($o | Format-List Caption, Version, BuildNumber, OSArchitecture, OSLanguage, InstallDate, LastBootUpTime | Out-String -Width 200).Trim() \n"
				+ "  '----- Win32_Processor' \n"
				+ "  ($c | Format-List Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed | Out-String -Width 200).Trim() \n"
				+ "} \n"
				+ "";

			// The command line is limited to 8191 chars (cmd.exe, also over SSH: sshd runs 'cmd /c ...'), and -EncodedCommand makes it ~2.7 times the script length.
			// So: gzip the script, and -EncodedCommand only a small bootstrap that unzips and runs it.
			byte[] gzipped;
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream())
			{
				try (GZIPOutputStream gz = new GZIPOutputStream(bos))
				{
					gz.write(ps.getBytes(StandardCharsets.UTF_8));
				}
				gzipped = bos.toByteArray();
			}
			catch (IOException ex) // can't happen with a ByteArrayOutputStream
			{
				throw new RuntimeException(ex);
			}
			String bootstrap = ""
				+ "$ProgressPreference = 'SilentlyContinue'; " // no '#< CLIXML' progress records on stderr
				+ "$ms = New-Object IO.MemoryStream(,[Convert]::FromBase64String('" + Base64.getEncoder().encodeToString(gzipped) + "')); "
				+ "$sr = New-Object IO.StreamReader((New-Object IO.Compression.GZipStream($ms, [IO.Compression.CompressionMode]::Decompress)), [Text.Encoding]::UTF8); "
				+ "Invoke-Expression $sr.ReadToEnd()";

			String encoded = Base64.getEncoder().encodeToString(bootstrap.getBytes(StandardCharsets.UTF_16LE));
			return "powershell -NoProfile -NonInteractive -EncodedCommand " + encoded;
		}

		//-------------------------------------------------
		// Linux
		//-------------------------------------------------
		if (osName.equals("Linux"))
		{
			return ""
				+ "echo \"" + KEY_OS_NAME        + ":        $(grep -s '^PRETTY_NAME=' /etc/os-release | cut -d= -f2- | tr -d '\"')\" \n"
				+ "echo \"" + KEY_OS_VERSION     + ":     $(grep -s '^VERSION_ID='  /etc/os-release | cut -d= -f2- | tr -d '\"')\" \n"
				+ "echo \"" + KEY_KERNEL_BUILD   + ":   $(uname -r)\" \n"
				+ "echo \"" + KEY_ARCHITECTURE   + ":   $(uname -m)\" \n"
				+ "echo \"" + KEY_HARDWARE       + ":       $(cat /sys/class/dmi/id/sys_vendor 2>/dev/null) $(cat /sys/class/dmi/id/product_name 2>/dev/null)\" \n"
				+ "echo \"" + KEY_VIRTUALIZATION + ": $(systemd-detect-virt 2>/dev/null)\" \n"
				// lscpu is missing on some systems (Alpine/busybox): then fall back to /proc/cpuinfo and nproc for model and thread count
				+ "LC_ALL=C lscpu 2>/dev/null | awk -F: "
				+     "-v mn=\"$(grep -m1 '^model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2- | sed 's/^ *//')\" "
				+     "-v np=\"$(nproc 2>/dev/null || grep -c '^processor' /proc/cpuinfo)\" "
				+     "'BEGIN { m=\"\"; t=\"\" } { gsub(/^[ \\t]+/, \"\", $2) } "
				+                                      "$1==\"Model name\" && m==\"\" { m=$2 } "
				+                                      "$1==\"Socket(s)\" { s=$2 } "
				+                                      "$1==\"Core(s) per socket\" { c=$2 } "
				+                                      "$1==\"CPU(s)\" && t==\"\" { t=$2 } "
				+                                      "END { if (m==\"\") m=mn; if (t==\"\") t=np; "
				+                                            "print \"" + KEY_CPU_MODEL   + ":      \" m; "
				+                                            "print \"" + KEY_CPU_SOCKETS + ":    \" s; "
				+                                            "print \"" + KEY_CPU_CORES   + ":      \" (s*c > 0 ? s*c : \"\"); "
				+                                            "print \"" + KEY_CPU_THREADS + ":    \" t }' \n"
				+ "awk '/^MemTotal:/ { printf \"" + KEY_MEMORY_TOTAL + ":   %.1f GB\\n\", $2/1024/1024 }' /proc/meminfo \n"
				+ "echo \"" + KEY_LAST_BOOT      + ":      $(uptime -s 2>/dev/null)\" \n"
				+ "echo '' \n"
				+ "echo '----- uname -a' \n"
				+ "uname -a \n"
				+ "echo '----- /etc/os-release' \n"
				+ "cat /etc/os-release 2>/dev/null \n"
				+ "";
		}

		//-------------------------------------------------
		// Solaris
		//-------------------------------------------------
		if (osName.equals("SunOS"))
		{
			return ""
				+ "echo \"" + KEY_OS_NAME        + ":        $(head -1 /etc/release | sed 's/^ *//')\" \n"
				+ "echo \"" + KEY_OS_VERSION     + ":     $(uname -v)\" \n"
				+ "echo \"" + KEY_KERNEL_BUILD   + ":   $(uname -r)\" \n"
				+ "echo \"" + KEY_ARCHITECTURE   + ":   $(uname -p)\" \n"
				+ "echo \"" + KEY_HARDWARE       + ":       $(uname -i)\" \n"
				+ "echo \"" + KEY_CPU_SOCKETS    + ":    $(psrinfo -p 2>/dev/null)\" \n"
				+ "echo \"" + KEY_CPU_THREADS    + ":    $(psrinfo 2>/dev/null | wc -l | tr -d ' ')\" \n"
				+ "prtconf 2>/dev/null | awk '/^Memory size:/ { printf \"" + KEY_MEMORY_TOTAL + ":   %.1f GB\\n\", $3/1024 }' \n"
				+ "echo '' \n"
				+ "echo '----- uname -a' \n"
				+ "uname -a \n"
				+ "echo '----- /etc/release' \n"
				+ "cat /etc/release \n"
				+ "";
		}

		//-------------------------------------------------
		// AIX
		//-------------------------------------------------
		if (osName.equals("AIX"))
		{
			return ""
				+ "echo \"" + KEY_OS_NAME        + ":        AIX $(oslevel)\" \n"
				+ "echo \"" + KEY_OS_VERSION     + ":     $(oslevel -s)\" \n"
				+ "echo \"" + KEY_KERNEL_BUILD   + ":   $(uname -v).$(uname -r)\" \n"
				+ "echo \"" + KEY_ARCHITECTURE   + ":   $(uname -p)\" \n"
				+ "echo \"" + KEY_HARDWARE       + ":       $(uname -M)\" \n"
				+ "echo \"" + KEY_CPU_THREADS    + ":    $(bindprocessor -q 2>/dev/null | awk -F: '{ print split($2, a, \" \") }')\" \n"
				+ "lsattr -El sys0 -a realmem 2>/dev/null | awk '{ printf \"" + KEY_MEMORY_TOTAL + ":   %.1f GB\\n\", $2/1024/1024 }' \n"
				+ "echo '' \n"
				+ "echo '----- uname -a' \n"
				+ "uname -a \n"
				+ "";
		}

		//-------------------------------------------------
		// HP-UX
		//-------------------------------------------------
		if (osName.equals("HP-UX"))
		{
			return ""
				+ "echo \"" + KEY_OS_NAME        + ":        HP-UX $(uname -r)\" \n"
				+ "echo \"" + KEY_KERNEL_BUILD   + ":   $(uname -r)\" \n"
				+ "echo \"" + KEY_ARCHITECTURE   + ":   $(uname -m)\" \n"
				+ "echo \"" + KEY_HARDWARE       + ":       $(model 2>/dev/null)\" \n"
				+ "echo '' \n"
				+ "echo '----- uname -a' \n"
				+ "uname -a \n"
				+ "echo '----- machinfo' \n"
				+ "machinfo 2>/dev/null \n"
				+ "";
		}

		return "echo 'Operating System information is not supported on OS Name: " + osName + "'";
	}

	/**
	 * Parse the normalized <code>Key: value</code> lines from the stored configuration text (only the KEY_* keys, first occurrence)
	 *
	 * @param configText  The text stored in MonSessionDbmsConfigText for configName 'HostMonOsInfo'
	 * @return A Map (never null), only keys with a value
	 */
	public static Map<String, String> parseKeyValues(String configText)
	{
		Map<String, String> map = new LinkedHashMap<>();
		if (StringUtil.isNullOrBlank(configText))
			return map;

		String[] keys = { KEY_OS_NAME, KEY_OS_VERSION, KEY_KERNEL_BUILD, KEY_ARCHITECTURE, KEY_HARDWARE, KEY_VIRTUALIZATION,
		                  KEY_CPU_MODEL, KEY_CPU_SOCKETS, KEY_CPU_CORES, KEY_CPU_THREADS, KEY_MEMORY_TOTAL, KEY_LAST_BOOT };

		for (String line : configText.split("\\r?\\n"))
		{
			int pos = line.indexOf(':');
			if (pos <= 0)
				continue;

			String key = line.substring(0, pos).trim();
			String val = line.substring(pos + 1).trim();

			for (String k : keys)
			{
				if (k.equals(key) && StringUtil.hasValue(val) && ! map.containsKey(k))
					map.put(k, val);
			}
		}
		return map;
	}
}
