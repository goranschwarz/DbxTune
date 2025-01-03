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
package com.dbxtune.alarm;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.JavaSourceClassLoader;
//import org.codehaus.janino.DebuggingInformation;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;


public class UserDefinedAlarmHandler
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public final static String PROPKEY_sourceDir         = "UserDefinedAlarmHandler.source.dir";
//	public final static String DEFAULT_sourceDir         = "resources/alarm-handler-src";
	public final static String DEFAULT_sourceDir         = "${DBXTUNE_UD_ALARM_SOURCE_DIR:-}/resources/alarm-handler-src"; // default for ${DBXTUNE_UD_ALARM_SOURCE_DIR} is ''
	
	public final static String PROPKEY_packetBaseName    = "UserDefinedAlarmHandler.packet.base.name";
//	public final static String DEFAULT_packetBaseName    = "com.dbxtune.cm.alarm";
	public final static String DEFAULT_packetBaseName    = Version.getAppName().toLowerCase();
	
	public final static String PROPKEY_fallbackClassName = "UserDefinedAlarmHandler.fallback.class.name";
//	public final static String DEFAULT_fallbackClassName = "com.dbxtune.cm.alarm.GenericFallbackAlarmInterrogator";
	public final static String DEFAULT_fallbackClassName = Version.getAppName().toLowerCase() + ".GenericFallbackAlarmInterrogator";
//	public final static String DEFAULT_fallbackClassName = "com.dbxtune.cm.alarm.ase.Xxx";
	
	@SuppressWarnings("unused")
	private Configuration _conf = null;

//	private Thread   _thread      = null;
	private boolean  _initialized = false;
//	private boolean  _running     = false;

	// implements singleton pattern
	private static UserDefinedAlarmHandler _instance = null;

	private String _classSrcDirStr  = null;
	private File   _classSrcDirFile = null;
	private JavaSourceClassLoader _classLoader = null;

	private String _packetBaseName    = null;
	private String _fallbackClassName = null;

	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static UserDefinedAlarmHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(UserDefinedAlarmHandler inst)
	{
		_instance = inst;
	}

	public void init(Configuration conf) throws Exception
	{
		if (conf == null)
			conf = new Configuration();

		_conf = conf; 
		
		_logger.info("Initializing User Defined Alarm Handler.");

//        File janinoSourceDirs = new File("janino-src");
//        File[] srcDirs = new File[]{janinoSourceDirs};
//        String encoding = null;
//        ClassLoader parentClassLoader = getClass().getClassLoader();
//        ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding, DebuggingInformation.NONE);
//        
//        Command xc = (Command) cl.loadClass("org.example.svenehrke.janino.command.MyCommand").newInstance();
//        xc.execute();

		// if DBXTUNE_UD_ALARM_SOURCE_DIR, is NOT set: set it to ${HOME}/.dbxtune
		if ( ! conf.hasProperty("DBXTUNE_UD_ALARM_SOURCE_DIR") )
		{
			_logger.warn("The environment variable 'DBXTUNE_UD_ALARM_SOURCE_DIR' is NOT set. Setting this to '"+AppDir.getAppStoreDir()+"'.");
			System.setProperty("DBXTUNE_UD_ALARM_SOURCE_DIR", AppDir.getAppStoreDir());
		}

		
		// Read configuration.
		_classSrcDirStr    = conf.getProperty(PROPKEY_sourceDir, DEFAULT_sourceDir);
		_classSrcDirStr    = StringUtil.envVariableSubstitution(_classSrcDirStr); // resolv any environment variables into a value
		_classSrcDirFile   = new File(_classSrcDirStr);

		_packetBaseName    = conf.getProperty(PROPKEY_packetBaseName,    DEFAULT_packetBaseName);
		_fallbackClassName = conf.getProperty(PROPKEY_fallbackClassName, DEFAULT_fallbackClassName);

//		_logger.info("Base Source Code Directory for User Defined Alarm Handler is '" + getSourceDir() + "'.");
		_logger.info("Configuration for UserDefinedAlarmHandler");
		_logger.info("                  "+PROPKEY_sourceDir+"          = "+getSourceDir());
		_logger.info("                  "+PROPKEY_packetBaseName+"     = "+_packetBaseName);
		_logger.info("                  "+PROPKEY_fallbackClassName+"  = "+_fallbackClassName);

		if ( ! _classSrcDirFile.exists() )
		{
			_logger.warn("The Directory '" + getSourceDir() + "' does NOT exists. User Defined Alarm Handling will be DISABLED.");
		}
		File[] srcDirs = new File[]{ _classSrcDirFile };
		String encoding = null;
		ClassLoader parentClassLoader = getClass().getClassLoader();
//		ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding, DebuggingInformation.NONE);
//		ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding);
		_classLoader = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding);

		_initialized = true;

//		setInstance(this);
	}

	public String getSourceDir()
	{
		if (_classSrcDirFile != null)
			return _classSrcDirFile.getAbsolutePath();

		return _classSrcDirStr;
	}

	/** @return packetname.CmName */
	public String getJavaClassName(CountersModel cm)
	{
		return getJavaClassName(cm.getName());
	}
	/** @return packetname.CmName */
	public String getJavaClassName(String cmName)
	{
		String className = _packetBaseName + "." + cmName;
		return className;
	}

	/** @return packetname/CmName.java */
	public String getJavaBaseFileName(CountersModel cm)
	{
		return getJavaBaseFileName(cm.getName());
	}
	/** @return packetname/CmName.java */
	public String getJavaBaseFileName(String cmName)
	{
		String className = _packetBaseName + "/" + cmName + ".java";
		return className;
	}


	public String getJavaFileName(CountersModel cm)
	{
		String className = _packetBaseName + "." + cm.getName();
		String javaFileName = getSourceDir() + File.separatorChar + className.replace('.', File.separatorChar) + ".java";

		return javaFileName;
	}
	public String getJavaFileName(String cmName)
	{
		String className = _packetBaseName + "." + cmName;
		String javaFileName = getSourceDir() + File.separatorChar + className.replace('.', File.separatorChar) + ".java";

		return javaFileName;
	}


	public IUserDefinedAlarmInterrogator newClassInstance(CountersModel cm)
	{
		// If it's not initialized a RuntimeException will be thrown
		isInitialized();
		
		if (cm == null)
			throw new IllegalArgumentException("CounteModel can't be null.");

//		String cmPkgClassName   = cm.getClass().getPackage().getName();
//		String cmShortClassName = cm.getClass().getSimpleName();
//		String[] sa = cmPkgClassName.split("\\.");
//		String dbmsPkg = sa[sa.length-1];
//System.out.println("newClassInstance(cm='"+cm.getName()+"'): cmPkgClassName='"+cmPkgClassName+"', dbmsPkg='"+dbmsPkg+"'.");

//		String className = _packetBaseName + "." + dbmsPkg + "." + cm.getName();
		String className = getJavaClassName(cm.getName());

		if (System.getProperty("UserDefinedAlarmHandler.debug", "false").equalsIgnoreCase("true"))
			System.out.println("UserDefinedAlarmHandler: Trying to compile object '" + className + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");

		_logger.debug("Trying to compile object '" + className + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");

		try
		{
			IUserDefinedAlarmInterrogator interrogator = (IUserDefinedAlarmInterrogator) _classLoader.loadClass(className).newInstance();
			_logger.info("Success compiling object '" + className + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");
			return interrogator;
		}
		catch(ClassNotFoundException e)
		{
//Caused by: org.codehaus.commons.compiler.CompileException: File 'resources\alarm-handler-src\com\dbxtune\cm\alarm\ase\Xxx.java', Line 8, Column 11: Cannot determine simple type name "IUserDefinedAlarmInterrogatorXX"
			Throwable cause = e.getCause();
			if (cause instanceof CompileException)
			{
				_logger.error("Errors was found when trying to compile object '" + className + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'. Reason: " + cause);
			}
			if (cause == null)
			{
				
//				_logger.info("User Defined Alarm will NOT be enabled for "+StringUtil.left("'"+cm.getName()+"'.", 25)+" Class-Not-Found when trying to compiling object " + StringUtil.left("'"+className+"'.", 25+Version.getAppName().length()+1) + " Using source directory '" + getSourceDir() + "'. To implement User Defined Alarms for this CM: Create the java file '"+getJavaFileName(cm)+"'.");
				_logger.info("User   Defined Alarms are NOT enabled for "+StringUtil.left("'"+cm.getName()+"'.", 25)+" Class-Not-Found when trying to compiling object " + StringUtil.left("'"+className+"'.", 25+Version.getAppName().length()+1) + " Using source directory '" + getSourceDir() + "'. To implement User Defined Alarms for this CM: Create the java file '"+getJavaFileName(cm)+"'.");
				_logger.debug("Class-Not-Found when trying to compiling object '" + className + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");
			}


			//-------------------------------------------------------------
			// Fallback and try to use the "generic" Interrogator
			//-------------------------------------------------------------
			try
			{
				_logger.debug("FALLBACK: Trying to compile object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");
				
				IUserDefinedAlarmInterrogator interrogator = (IUserDefinedAlarmInterrogator) _classLoader.loadClass(_fallbackClassName).newInstance();

				_logger.info("FALLBACK: Success compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");
				return interrogator;
			}
			catch(ClassNotFoundException e2)
			{
				cause = e2.getCause();
				if (cause instanceof CompileException)
				{
					_logger.error("FALLBACK: Errors was found when trying to compile object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'. Reason: " + cause);
				}
				if (cause == null)
					_logger.debug("FALLBACK: Class-Not-Found when trying to compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.");
				return null;
			}
			catch (InstantiationException e2)
			{
				_logger.debug("FALLBACK: InstantiationException when trying to compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.", e2);
				return null;
			}
			catch (IllegalAccessException e2)
			{
				_logger.debug("FALLBACK: IllegalAccessException when trying to compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.", e2);
				return null;
			}
		}
		catch (InstantiationException e)
		{
			_logger.debug("Caught: InstantiationException when trying to compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.", e);
			return null;
		}
		catch (IllegalAccessException e)
		{
			_logger.debug("Caught: IllegalAccessException when trying to compiling object '" + _fallbackClassName + "' for CM '" + cm.getName() + "'. Using source directory '" + getSourceDir() + "'.", e);
			return null;
		}
	}
	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The '"+this.getClass().getSimpleName()+"' module has NOT yet been initialized.");
		}
	}

//	public void start()
//	{
//		if (_writerClasses.size() == 0)
//		{
//			_logger.warn("No Alarm Writers has been installed, The service thread will NOT be started and NO alarms will be propagated.");
//			return;
//		}
//
//		isInitialized();
//
//		// Start the Container Persist Thread
//		_thread = new Thread(this);
//		_thread.setName(this.getClass().getSimpleName());
//		_thread.setDaemon(true);
//		_thread.start();
//	}
	
	public static void main(String[] args)
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		try
		{
			UserDefinedAlarmHandler ah = new UserDefinedAlarmHandler();
			ah.init(null);
			UserDefinedAlarmHandler.setInstance(ah);

			CountersModel cm = new CountersModel();
//			cm.getAbsRowCount();
//			cm.getCounterSampleAbs();
			
			IUserDefinedAlarmInterrogator inst = UserDefinedAlarmHandler.getInstance().newClassInstance(cm);
			if (inst != null)
			{
				inst.interrogateCounterData(cm);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public static void mainx(String[] args)
//	{
//		try
//		{
//			interogater = (IUserDefinedAlarmInterrogator) cl.loadClass("com.dbxtune.cm.alarm.ase.Yyy").newInstance();
//			System.out.println("yyy interogater="+interogater);
//			if (interogater != null)
//			{
//				interogater.interrogateCounterData(null, null, null);
//			}
//			
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
