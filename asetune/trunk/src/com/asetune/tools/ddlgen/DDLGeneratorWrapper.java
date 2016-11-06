package com.asetune.tools.ddlgen;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
//import com.sybase.ddlgen.DDLBaseException;
//import com.sybase.ddlgen.DDLGenerator;
//import com.sybase.ddlgen.Initializer;

/**
 * Just some tests that was done
 * 
 * @author gorans
 *
 */
public class DDLGeneratorWrapper
{
	private String[]      _args   = null;
//	private DDLGenerator  _ddlGen = null;
	private Object  _ddlGen = null;
	private DbxConnection _conn   = null;

//	private DDLGenerator loadDDLGenerator() 
//	throws Exception
//	{
//		File jarFile = null;
//		int dbmsVersion = _conn.getDbmsVersionNumber();
//
//		// ASE 12.5.4 or below, use the 1254 jar version on DDLGen
//		if (dbmsVersion < Ver.ver(15, 0))
//		{
//			jarFile = new File("C:/projects/AseTune/lib/DDLGen_1254.jar");
//		}
//		// ASE 15.0 and above, use the 16 jar version on DDLGen 
//		else
//		{
//			jarFile = new File("C:/projects/AseTune/lib/DDLGen_160.jar");
//		}
//
//		System.out.println("JAR file: "+jarFile);
//		System.out.println("JAR file.exists(): "+jarFile.exists());
//		System.out.println("JAR file.toURI().toURL(): "+jarFile.toURI().toURL());
//
//		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
//		Class<?> clazz = classLoader.loadClass("com.sybase.ddlgen.DDLGenerator");
//		Constructor<?> cons = clazz.getDeclaredConstructor(String[].class);
////		DDLGenerator ddlGen = (DDLGenerator) cons.newInstance( new Object[] {_args} );
//		DDLGenerator ddlGen = (DDLGenerator) cons.newInstance( (Object)_args );
////		DDLGenerator ddlGen = (DDLGenerator) clazz.newInstance();
//		
//		System.out.println("ddlGen.getVersion():" + ddlGen.getVersion());
//
////		classLoader.close();
//		return ddlGen;
//		//return new DDLGenerator(_args);
//	}
//	public DDLGeneratorWrapper(DbxConnection conn, String[] args) 
//	throws Exception
//	{
//		_args = args;
//		_conn = conn;
//		_ddlGen = loadDDLGenerator();
//	}
//
//	public void setParams(String[] args)
//	{
//		_ddlGen.setParams(args);
//	}
//
//	public Object generateDDL() throws DDLBaseException
//	{
//		return _ddlGen.generateDDL();
//	}
//
//	public static String getVersion() 
//	throws Exception
//	{
//		return Initializer.getFullVersionString();
//	}
//
//	public static String getShortVersion() 
//	throws Exception
//	{
////		return DDLGenerator.getVersion();
//		return Initializer.getVersionString();
//	}
	

	private Object loadDDLGenerator() 
	throws Exception
	{
		File jarFile = null;
		int dbmsVersion = _conn.getDbmsVersionNumber();

		// ASE 12.5.4 or below, use the 1254 jar version on DDLGen
		if (dbmsVersion < Ver.ver(15, 0))
		{
			jarFile = new File("C:/projects/AseTune/lib/DDLGen_1254.jar");
		}
		// ASE 15.0 and above, use the 16 jar version on DDLGen 
		else
		{
			jarFile = new File("C:/projects/AseTune/lib/DDLGen_160.jar");
		}
		jarFile = new File("C:/projects/AseTune/lib/DDLGen_1254.jar");
//		jarFile = new File("C:/projects/AseTune/lib/DDLGen_1501.jar");
//		jarFile = new File("C:/projects/AseTune/lib/DDLGen_155.jar");

		System.out.println("JAR file: "+jarFile);
		System.out.println("JAR file.exists(): "+jarFile.exists());
		System.out.println("JAR file.toURI().toURL(): "+jarFile.toURI().toURL());

		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
		Class<?> clazz = classLoader.loadClass("com.sybase.ddlgen.DDLGenerator");
		Constructor<?> cons = clazz.getDeclaredConstructor(String[].class);
		Object ddlGen = cons.newInstance( (Object)_args );
		
//		classLoader.close();
		return ddlGen;
	}

	public DDLGeneratorWrapper(DbxConnection conn, String[] args) 
	throws Exception
	{
		_args = args;
		_conn = conn;
		_ddlGen = loadDDLGenerator();
	}

	public void setParams(String[] args)
	throws Exception
	{
		Method m = _ddlGen.getClass().getMethod("setParams", String[].class);
		m.invoke(_ddlGen, (Object)args);
//		_ddlGen.setParams(args);
	}

	public Object generateDDL() //throws DDLBaseException
	throws Exception
	{
		Method m = _ddlGen.getClass().getMethod("generateDDL");
		String ddl = (String) m.invoke(_ddlGen);
		return ddl;
//		return _ddlGen.generateDDL();
	}

	public String getVersion() 
	throws Exception
	{
		Method m = _ddlGen.getClass().getMethod("getVersion");
		String version = (String) m.invoke(_ddlGen);
		return version;
	}

	public static void main(String[] args)
	{
		dummyTest1( new String[] {"-Usa", "-Psybase", "-S192.168.0.110:1600", "-TDB", "-Dmaster", "-Jiso_1"} );
//		dummyTest2();
	}

	public static void dummyTest1(String[] args)
	{
		System.out.println("args: "+StringUtil.toCommaStr(args));
		try
		{
			ConnectionProp connProp = new ConnectionProp();
			connProp.setDriverClass("com.sybase.jdbc4.jdbc.SybDriver");
			connProp.setUrl("jdbc:sybase:Tds:192.168.0.110:1600?ENCRYPT_PASSWORD=true");
			connProp.setUsername("sa");
			connProp.setPassword("sybase");
			DbxConnection conn = DbxConnection.connect(null, connProp);
			System.out.println("DBMS Product Name:    "+conn.getDatabaseProductName());
			System.out.println("DBMS Product Version: "+conn.getDatabaseProductVersion());

			DDLGeneratorWrapper ddlGen = new DDLGeneratorWrapper(conn, args);
			ddlGen.setParams(args);
			System.out.println("ddlGen.getVersion():"  + ddlGen.getVersion() );
			System.out.println("ddlGen.generateDDL():" + ddlGen.generateDDL() );
		}
		catch(Exception e)
		{
			e.printStackTrace();

			if (e.getCause() != null)
			{
				System.out.println("Exception.getCause(): ");
				e.getCause().printStackTrace();
			}
			System.out.println("------XXXXXXXXXXXXXXXXX-----------");
			Throwable xxx = e;
			while (xxx != null)
			{
				System.out.println("------ "+xxx.getClass().getName());
				xxx.printStackTrace();
				xxx = xxx.getCause();
			}
		}
	}

	public static void dummyTest2()
	{
		try
		{
//			File f = new File("C:/projects/AseTune/lib/DDLGen_1254.jar");
			File f = new File("C:/projects/AseTune/lib/DDLGen_160.jar");
			System.out.println("file: "+f);
			System.out.println("file.exists(): "+f.exists());
			System.out.println("file.toURI().toURL(): "+f.toURI().toURL());

//			URL[] urls = new URL[] {new URL("file:///C:/projects/AseTune/lib/DDLGen_160.jar")};
//			ClassLoader classLoader = new URLClassLoader(urls);
//			ClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
//			ClassLoader classLoader = new URLClassLoader(new URL[] {new File("C:/projects/AseTune/lib/DDLGen_160.jar").toURI().toURL()}, Thread.currentThread().getContextClassLoader());
//			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();            

			URLClassLoader classLoader = new URLClassLoader(new URL[] {f.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
			
			Class<?> clazz = classLoader.loadClass("com.sybase.ddlgen.DDLGenerator");
//			Constructor<?> cons = clazz.getDeclaredConstructor(String[].class);
			Constructor<?> cons = clazz.getConstructor(String[].class);
			String[] xxx = new String[] {"-Usa", "-Pzxczc", "-Sxxxx"};
//			DDLGenerator ddlGen = (DDLGenerator) cons.newInstance( (Object)xxx );
			Object ddlGen = cons.newInstance( (Object)xxx );
			
			// Call getVersion();
			Method m_getVersion = ddlGen.getClass().getMethod("getVersion");
			String version = (String) m_getVersion.invoke(ddlGen);
//			System.out.println("ddlGen.getVersion():" + ddlGen.getVersion());
			System.out.println("ddlGen.getVersion():" + version);

			// Call setParams();
			Method m_setParams = ddlGen.getClass().getMethod("setParams", String[].class);
			m_setParams.invoke(ddlGen, xxx);
//			ddlGen.setParams(xxx);

//	        Class<?> callerClass = classLoader.loadClass("com.sybase.ddlgen.sql.ASConnection"); 
////	        Class<?> callerClass = classLoader.loadClass("com.sybase.ddlgen.ASConnection"); 
//	        Object o = callerClass.newInstance();
//			System.out.println("o="+o);
			
////			URLClassLoader classLoader = new URLClassLoader(new URL[] {new File("C:/projects/AseTune/lib/DDLGen_160.jar").toURI().toURL()}, Thread.currentThread().getContextClassLoader());
//			URLClassLoader classLoader = new URLClassLoader(new URL[] {new File("lib/DDLGen_160.jar").toURI().toURL()}, Thread.currentThread().getContextClassLoader());
//			Class<?> clazz = classLoader.loadClass("com.sybase.ddlgen.ASConnection");
//			Object o = clazz.newInstance();
//			System.out.println("o="+o);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
