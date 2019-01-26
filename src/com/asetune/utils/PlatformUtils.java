/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.utils;

import org.apache.log4j.Logger;

public class PlatformUtils 
{
	private static Logger _logger = Logger.getLogger(PlatformUtils.class);

//	public enum Platform {WIN, LINUX, MAC_OS, SOLARIS, OTHER} 
//	public enum Desktop  {WIN, KDE, GNOME, MAC_OS, OTHER} 
//	public enum Browser  {IE, FIREFOX} 

	public final static int Platform_WIN     = 0;
	public final static int Platform_LINUX   = 1;
	public final static int Platform_MAC_OS  = 2;
	public final static int Platform_SOLARIS = 3;
	public final static int Platform_OTHER   = 4;

	public final static int Desktop_WIN      = 10;
	public final static int Desktop_KDE      = 11;
	public final static int Desktop_GNOME    = 12;
	public final static int Desktop_MAC_OS   = 13;
	public final static int Desktop_OTHER    = 14;

	public final static int Browser_IE       = 20;
	public final static int Browser_FIREFOX  = 21;
	
	/*************************************************************************
	 * Gets the platform we are currently running on.
	 * @return a platform code.
	 ************************************************************************/
//	public static Platform getCurrentPlattform() 
	public static int getCurrentPlattform() 
	{
		String osName = System.getProperty("os.name", "unknown");
		
		if (osName.startsWith("Windows")) 
		{
			_logger.trace("Detected Windows platform '"+osName+"'.");
			return Platform_WIN;
		} 
		if (osName.startsWith("Linux")) 
		{
			_logger.trace("Detected Linux platform '"+osName+"'.");
			return Platform_LINUX;
		} 
		if (osName.startsWith("MacOS") || osName.startsWith("Mac OS X")) 
		{
			_logger.trace("Detected Mac OS platform '"+osName+"'.");
			return Platform_MAC_OS;
		} 
		if (osName.startsWith("Solaris")) 
		{
			_logger.trace("Detected Solaris platform '"+osName+"'.");
			return Platform_SOLARIS;
		}
		
		return Platform_OTHER;
	}
	
	/*************************************************************************
	 * Gets the ID for the platform default browser.
	 * @return a browser ID, null if no supported browser was detected.
	 ************************************************************************/	
//	public static Browser getDefaultBrowser() 
	public static int getDefaultBrowser() 
	{
		// Use better logic to detect default browser?
		if (getCurrentPlattform() == Platform_WIN) 
		{
			_logger.trace("Detected Browser is InternetExplorer.");
			return Browser_IE;
		} 
		else 
		{
			_logger.trace("Detected Browser Firefox. (or possible just: Fallback?)");
			return Browser_FIREFOX;
		}
	}
	
	/*************************************************************************
	 * Gets the desktop that we are running on.
	 * @return the desktop identifier.
	 ************************************************************************/	
//	public static Desktop getCurrentDesktop() 
	public static int getCurrentDesktop() 
	{
		String osName = System.getProperty("os.name", "unknown");
		
		if (osName.startsWith("Windows")) 
		{
			_logger.trace("Detected Windows desktop.");
			return Desktop_WIN;
		} 
		if (osName.startsWith("MacOS") || osName.startsWith("Mac OS X")) 
		{
			_logger.trace("Detected Mac OS desktop.");
			return Desktop_MAC_OS;
		} 

		if (   osName.startsWith("Linux") 
		    || osName.contains("Unix") 
		    || osName.startsWith("Solaris") 
		   )
		{
			if (isKDE()) 
			{
				_logger.trace("Detected KDE desktop.");
				return Desktop_KDE;
			}
			if (isGnome()) 
			{
				_logger.trace("Detected Gnome desktop.");
				return Desktop_GNOME;
			}
		} 
		_logger.trace("Detected Unknown desktop (fallback).");
		return Desktop_OTHER;
	}

	/*************************************************************************
	 * Checks if we are currently running under Gnome desktop.
	 * @return true if it is a Gnome else false.
	 ************************************************************************/
	private static boolean isGnome() 
	{
		return System.getenv("GNOME_DESKTOP_SESSION_ID") != null;
	}

	/*************************************************************************
	 * Checks if we are currently running under KDE desktop.
	 * @return true if it is a KDE else false. 
	 ************************************************************************/
	private static boolean isKDE() 
	{
		return System.getenv("KDE_SESSION_VERSION") != null;
	}
	

//	import sun.jvmstat.monitor.*;
	/**
	 * Get number if started JVM's that is running a specific main class
	 * @return
	 */
//	public static int getJvmProgramCount(String mainClassName)
//	{
//		RuntimeMXBean xxx = ManagementFactory.getRuntimeMXBean();
//		MonitoredHost
//	}
}

// CODE FROM: JPS
// But it needs tools.jar, which isn't available if you only have a JRE
//		try
//		{
//			HostIdentifier hostId = arguments.hostId();
//			MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(hostId);
//
//			// get the set active JVMs on the specified host.
//			Set jvms = monitoredHost.activeVms();
//
//			for (Iterator j = jvms.iterator(); j.hasNext(); /* empty */)
//			{
//				StringBuilder output = new StringBuilder();
//				Throwable lastError = null;
//
//				int lvmid = ((Integer) j.next()).intValue();
//
//				output.append(String.valueOf(lvmid));
//
//				if ( arguments.isQuiet() )
//				{
//					System.out.println(output);
//					continue;
//				}
//
//				MonitoredVm vm = null;
//				String vmidString = "//" + lvmid + "?mode=r";
//
//				try
//				{
//					VmIdentifier id = new VmIdentifier(vmidString);
//					vm = monitoredHost.getMonitoredVm(id, 0);
//				}
//				catch (URISyntaxException e)
//				{
//					// unexpected as vmidString is based on a validated hostid
//					lastError = e;
//					assert false;
//				}
//				catch (Exception e)
//				{
//					lastError = e;
//				}
//				finally
//				{
//					if ( vm == null )
//					{
//						/*
//						 * we ignore most exceptions, as there are race
//						 * conditions where a JVM in 'jvms' may terminate before
//						 * we get a chance to list its information. Other
//						 * errors, such as access and I/O exceptions should stop
//						 * us from iterating over the complete set.
//						 */
//						output.append(" -- process information unavailable");
//						if ( arguments.isDebug() )
//						{
//							if ( (lastError != null) && (lastError.getMessage() != null) )
//							{
//								output.append("\n\t");
//								output.append(lastError.getMessage());
//							}
//						}
//						System.out.println(output);
//						if ( arguments.printStackTrace() )
//						{
//							lastError.printStackTrace();
//						}
//						continue;
//					}
//				}
//
//				output.append(" ");
//				output.append(MonitoredVmUtil.mainClass(vm, arguments.showLongPaths()));
//
//				if ( arguments.showMainArgs() )
//				{
//					String mainArgs = MonitoredVmUtil.mainArgs(vm);
//					if ( mainArgs != null && mainArgs.length() > 0 )
//					{
//						output.append(" ").append(mainArgs);
//					}
//				}
//				if ( arguments.showVmArgs() )
//				{
//					String jvmArgs = MonitoredVmUtil.jvmArgs(vm);
//					if ( jvmArgs != null && jvmArgs.length() > 0 )
//					{
//						output.append(" ").append(jvmArgs);
//					}
//				}
//				if ( arguments.showVmFlags() )
//				{
//					String jvmFlags = MonitoredVmUtil.jvmFlags(vm);
//					if ( jvmFlags != null && jvmFlags.length() > 0 )
//					{
//						output.append(" ").append(jvmFlags);
//					}
//				}
//
//				System.out.println(output);
//
//				monitoredHost.detach(vm);
//			}
//		}
//		catch (MonitorException e)
//		{
//			if ( e.getMessage() != null )
//			{
//				System.err.println(e.getMessage());
//			}
//			else
//			{
//				Throwable cause = e.getCause();
//				if ( (cause != null) && (cause.getMessage() != null) )
//				{
//					System.err.println(cause.getMessage());
//				}
//				else
//				{
//					e.printStackTrace();
//				}
//			}
//		}
//	}
