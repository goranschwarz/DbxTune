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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

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
		//-------------------------------------------------
		if (osName.startsWith("Windows"))
		{
			String ps = ""
				+ "$ProgressPreference = 'SilentlyContinue' \n"
				+ "$o  = Get-CimInstance Win32_OperatingSystem \n"
				+ "$cs = Get-CimInstance Win32_ComputerSystem \n"
				+ "$c  = @(Get-CimInstance Win32_Processor) \n"
				+ "$r  = Get-ItemProperty 'HKLM:\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion' \n"
				+ "$rel = if ($r.DisplayVersion) { $r.DisplayVersion } else { $r.ReleaseId } \n"
				+ "'" + KEY_OS_NAME        + ":        ' + $o.Caption \n"
				+ "'" + KEY_OS_VERSION     + ":     ' + $rel \n"
				+ "'" + KEY_KERNEL_BUILD   + ":   ' + $o.Version + '.' + $r.UBR \n"
				+ "'" + KEY_ARCHITECTURE   + ":   ' + $o.OSArchitecture \n"
				+ "'" + KEY_HARDWARE       + ":       ' + $cs.Manufacturer + ' ' + $cs.Model \n"
				+ "'" + KEY_CPU_MODEL      + ":      ' + $c[0].Name.Trim() \n"
				+ "'" + KEY_CPU_SOCKETS    + ":    ' + $c.Count \n"
				+ "'" + KEY_CPU_CORES      + ":      ' + ($c | Measure-Object -Property NumberOfCores             -Sum).Sum \n"
				+ "'" + KEY_CPU_THREADS    + ":    ' + ($c | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum \n"
				+ "'" + KEY_MEMORY_TOTAL   + ":   ' + [math]::Round($cs.TotalPhysicalMemory / 1GB, 1) + ' GB' \n"
				+ "'" + KEY_LAST_BOOT      + ":      ' + $o.LastBootUpTime.ToString('yyyy-MM-dd HH:mm:ss') \n"
				+ "'' \n"
				+ "'----- Win32_OperatingSystem' \n"
				+ "($o | Format-List Caption, Version, BuildNumber, OSArchitecture, OSLanguage, InstallDate, LastBootUpTime | Out-String -Width 200).Trim() \n"
				+ "'----- Win32_Processor' \n"
				+ "($c | Format-List Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed | Out-String -Width 200).Trim() \n"
				+ "";

			String encoded = Base64.getEncoder().encodeToString(ps.getBytes(StandardCharsets.UTF_16LE));
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
