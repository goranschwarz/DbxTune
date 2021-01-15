/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.sql.norm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.JavaSourceClassLoader;
//import org.codehaus.janino.DebuggingInformation;

import com.asetune.AppDir;
import com.asetune.Version;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


public class NormalizerCompiler
{
	private static Logger _logger = Logger.getLogger(NormalizerCompiler.class);

	public final static String PROPKEY_sourceDir         = "NormalizerCompiler.source.dir";
//	public final static String DEFAULT_sourceDir         = "resources/normalizer-src";
	public final static String DEFAULT_sourceDir         = "${DBXTUNE_NORMALIZER_SOURCE_DIR:-}/resources/normalizer-src"; // default for ${DBXTUNE_NORMALIZER_SOURCE_DIR} is ''
	
	public final static String PROPKEY_packetBaseName    = "NormalizerCompiler.packet.base.name";
//	public final static String DEFAULT_packetBaseName    = "com.asetune.sql.norm";
	public final static String DEFAULT_packetBaseName    = Version.getAppName().toLowerCase();
	
	@SuppressWarnings("unused")
	private Configuration _conf = null;

	private boolean  _initialized = false;

	// implements singleton pattern
	private static NormalizerCompiler _instance = null;

	private String _classSrcDirStr  = null;
	private File   _classSrcDirFile = null;
	private JavaSourceClassLoader _classLoader = null;

	private String _packetBaseName    = null;

	Set<IStatementFixer>       _statmentFixers = new LinkedHashSet<>();
	Set<IUserDefinedNormalizer> _udNormalizers = new LinkedHashSet<>();

	public Set<IStatementFixer>        getStatementFixers()        { return _statmentFixers; }
	public Set<IUserDefinedNormalizer> getUserDefinedNormalizers() { return _udNormalizers; }

	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static NormalizerCompiler getInstance()
	{
		if (_instance == null)
		{
			NormalizerCompiler inst = new NormalizerCompiler();
			inst.init(null);
			setInstance(inst);
		}

		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(NormalizerCompiler inst)
	{
		_instance = inst;
	}

	public void init(Configuration conf)// throws Exception
	{
		if (conf == null)
			conf = new Configuration();

		_conf = conf; 
		
		_logger.info("Initializing Normalizer Compiler.");

//        File janinoSourceDirs = new File("janino-src");
//        File[] srcDirs = new File[]{janinoSourceDirs};
//        String encoding = null;
//        ClassLoader parentClassLoader = getClass().getClassLoader();
//        ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding, DebuggingInformation.NONE);
//        
//        Command xc = (Command) cl.loadClass("org.example.svenehrke.janino.command.MyCommand").newInstance();
//        xc.execute();

		// if DBXTUNE_NORMALIZER_SOURCE_DIR, is NOT set: set it to ${HOME}/.dbxtune
		if ( ! conf.hasProperty("DBXTUNE_NORMALIZER_SOURCE_DIR") )
		{
			_logger.warn("The environment variable 'DBXTUNE_NORMALIZER_SOURCE_DIR' is NOT set. Setting this to '"+AppDir.getAppStoreDir()+"'.");
			System.setProperty("DBXTUNE_NORMALIZER_SOURCE_DIR", AppDir.getAppStoreDir());
		}

		
		// Read configuration.
		_classSrcDirStr    = conf.getProperty(PROPKEY_sourceDir, DEFAULT_sourceDir);
		_classSrcDirStr    = StringUtil.envVariableSubstitution(_classSrcDirStr); // resolv any environment variables into a value
		_classSrcDirFile   = new File(_classSrcDirStr);

		_packetBaseName    = conf.getProperty(PROPKEY_packetBaseName,    DEFAULT_packetBaseName);
//		_fallbackClassName = conf.getProperty(PROPKEY_fallbackClassName, DEFAULT_fallbackClassName);

//		_logger.info("Base Source Code Directory for User Defined Alarm Handler is '" + getSourceDir() + "'.");
		_logger.info("Configuration for User Defined Normalizer Compiler");
		_logger.info("                  "+PROPKEY_sourceDir+"          = "+getSourceDir());
		_logger.info("                  "+PROPKEY_packetBaseName+"     = "+_packetBaseName);
//		_logger.info("                  "+PROPKEY_fallbackClassName+"  = "+_fallbackClassName);

		if ( ! _classSrcDirFile.exists() )
		{
			_logger.warn("The Directory '" + getSourceDir() + "' does NOT exists. No User Defined Normalizer classes will be Compiled.");
		}
		File[] srcDirs = new File[]{ _classSrcDirFile };
		String encoding = null;
		ClassLoader parentClassLoader = getClass().getClassLoader();
//		ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding, DebuggingInformation.NONE);
//		ClassLoader cl = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding);
		_classLoader = new JavaSourceClassLoader(parentClassLoader, srcDirs, encoding);

		if ( _classSrcDirFile.exists() )
		{
			compileFiles();
		}

		_initialized = true;
	}

	private void compileFiles()
	{
		for (File sourceFile : getSourceFiles())
		{
			_logger.debug("Trying to compile file '" + sourceFile + "', source directory '" + getSourceDir() + "'.");

			String className = _packetBaseName + "." + FilenameUtils.removeExtension(sourceFile.getName());

			try
			{
				Object newObj = _classLoader.loadClass(className).newInstance();
				
				if (newObj instanceof IStatementFixer)
				{
					_logger.info("Success compiling object '" + className + "', of 'IStatementFixer', at source directory '" + getSourceDir() + "'.");
					_statmentFixers.add( (IStatementFixer) newObj );
					//StatementFixerManager.getInstance().add( (IStatementFixer) newObj );
				}
				else if (newObj instanceof IUserDefinedNormalizer)
				{
					_logger.info("Success compiling object '" + className + "', of 'IUserDefinedNormalizer', at source directory '" + getSourceDir() + "'.");
					//UserDefinedNormalizerManager.getInstance().add( (IUserDefinedNormalizer) newObj );
					_udNormalizers.add( (IUserDefinedNormalizer) newObj );
				}
				else
				{
					_logger.warn("Compiled the Source object '" + className + "', but is was NOT a instance of 'IStatementFixer' or 'IUserDefinedNormalizer'. source directory '" + getSourceDir() + "'.");
				}
			}
			catch(ClassNotFoundException e)
			{
				Throwable cause = e.getCause();
				if (cause instanceof CompileException)
				{
					_logger.error("Errors was found when trying to compile object '" + className + "', source directory '" + getSourceDir() + "'. Reason: " + cause);
				}
			}
			catch (InstantiationException e)
			{
				_logger.warn("Caught: InstantiationException when trying to compiling object '" + className + "', source directory '" + getSourceDir() + "'.", e);
			}
			catch (IllegalAccessException e)
			{
				_logger.warn("Caught: IllegalAccessException when trying to compiling object '" + className + "', source directory '" + getSourceDir() + "'.", e);
			}
			
		}
	}

//	public List<File> getSourceFiles()
//	{
//		String directory = getSourceDir() + "/" + _packetBaseName; 
//
//		List<File> fileNames = new ArrayList<>();
//		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory, "*.java")))
//		{
//			for (Path path : directoryStream)
//			{
//				fileNames.add(path.toFile());
//
////				File file = path.toFile();
////
////				if (! file.isFile() )
////					continue;
////
////				if (file.getName().endsWith(".java"))
////				{
////					fileNames.add(file);
////				}
//			}
//		}
//		catch (IOException ex)
//		{
//			_logger.error("Problems reading files in directory '" + directory + "'.", ex);
//			ex.printStackTrace();
//		}
//
//		// Sort the list
//		Collections.sort(fileNames);
//		
//		return fileNames;
//	}

	public List<File> getSourceFiles()
	{
		Path directory = Paths.get(getSourceDir());

		try (Stream<Path> stream = Files.walk(directory, Integer.MAX_VALUE)) 
		{
			List<File> fileNames = stream
					.filter(p -> p.toFile().isFile())
					.filter(p -> p.toString().toLowerCase().endsWith(".java"))
					.map(p -> p.toFile())
					.sorted()
					.collect(Collectors.toList());

//			fileNames.forEach(System.out::println);

			return fileNames;
		}
		catch (IOException ex)
		{
			_logger.error("Problems reading files in directory '" + directory + "'.", ex);
			return Collections.emptyList();
		}
	}
	
	public String getSourceDir()
	{
		if (_classSrcDirFile != null)
			return _classSrcDirFile.getAbsolutePath();

		return _classSrcDirStr;
	}

//	private void isInitialized()
//	{
//		if ( ! _initialized )
//		{
//			throw new RuntimeException("The '"+this.getClass().getSimpleName()+"' module has NOT yet been initialized.");
//		}
//	}
	public boolean isInitialized()
	{
		return _initialized;
	}

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			NormalizerCompiler.getInstance();
			
			StatementFixerManager.getInstance();
			
			UserDefinedNormalizerManager.getInstance();

//			NormalizerCompiler compiler = new NormalizerCompiler();
//			compiler.init(null);
//			NormalizerCompiler.setInstance(compiler);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
