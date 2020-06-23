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
package com.asetune.tools.ddlgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.asetune.utils.StringUtil;

/**
 * Just some tests that was done
 * 
 * @author gorans
 *
 */
public class DDLGeneratorXxx
{
	public String genDdl(String[] args)
	throws Exception
	{
		char xxx= File.pathSeparatorChar;
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome +
				File.separator + "bin" +
				File.separator + "java";
		String classpath = System.getProperty("java.class.path");
		List<String> classpathList = new ArrayList<String>( Arrays.asList( classpath.split(System.getProperty("path.separator", ".")) ) );
classpathList.add(0, "C:/projects/AseTune/lib/DDLGen.jar");
//classpathList.add(0, "C:/projects/AseTune/lib/DDLGen_160.jar");
//classpathList.add(0, "C:/projects/AseTune/lib/DDLGen_1254.jar");
//classpathList.add(0, "C:/projects/AseTune/lib/DDLGen_1501.jar");
//classpathList.add(0, "C:/projects/AseTune/lib/jconn3.jar");
		classpath = StringUtil.toCommaStr(classpathList, System.getProperty("path.separator"));
System.out.println("classpath: "+classpath);

		
		List<String> pbCmd = new ArrayList<String>();
		pbCmd.add("java");
		pbCmd.add("-cp");
		pbCmd.add(classpath);
		pbCmd.add("com.sybase.ddlgen.DDLGenerator");
		for (String param : args)
			pbCmd.add(param);

		System.out.println("exec: "+pbCmd);
		ProcessBuilder builder = new ProcessBuilder(pbCmd);
//		builder.redirectErrorStream(true); // redirect stderr to stdout
		Process process = builder.start();
		process.waitFor();
		int osRetCode = process.exitValue();
		System.out.println("genDdl.osRetcode="+osRetCode);

		String stdout = readStream(process.getInputStream());
		String stderr = readStream(process.getErrorStream());

		System.out.println("genDdl.stdout=|||"+stdout+"|||");
		System.out.println("genDdl.stderr=|||"+stderr+"|||");

		return stdout;
	}
	public String readStream(InputStream inputStream) 
	throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ( (line = reader.readLine()) != null) 
		{
			sb.append(line);
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}
	public static void main(String[] args)
	{
		try
		{
			new DDLGeneratorXxx().genDdl( new String[] {"-Usa", "-Psybase", "-S192.168.0.110:1600", "-Dgoran_16"} );
//			new DDLGeneratorXxx().genDdl( new String[] {"-Usa", "-Psybase", "-S192.168.0.110:15505", "-Dmaster"} );
//			new DDLGeneratorXxx().genDdl( new String[] {"-Usa", "-Psybase", "-S192.168.0.110:1600", "-TDB", "-Dmaster", "-Jiso_1"} );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}


//ProcessBuilder pb = new ProcessBuilder(cmdLineParams);
//Map<String, String> env = pb.environment();
//env.put("SQLW_CLONE_CONNECT_PROPS", propPropEntryStr);
//pb.directory(new File(SQLW_HOME));
//pb.redirectErrorStream(true);
//Process p = pb.start();
//
//// If we don't read the stream(s) from Process, it may simply not "start" or start slowly
//final InputStream stdOut = p.getInputStream();
//Thread stdInOutReaderThread = new Thread(new Runnable()
//{
//	@Override
//	public void run()
//	{
//		try
//		{
//			byte[] buffer = new byte[8192];
//			int len = -1;
//			while((len = stdOut.read(buffer)) > 0)
////				; // just throw the buffer away, so we don't block on outputs...
//				System.out.write(buffer, 0, len);
//		}
//		catch (IOException ignore) {}
//	}
//});
//stdInOutReaderThread.setDaemon(true);
//stdInOutReaderThread.setName("cloneConnect:stdInOutReader");
//stdInOutReaderThread.start();




//public final class JavaProcess {
//
//    private JavaProcess() {}        
//
//    public static int exec(Class klass) throws IOException,
//                                               InterruptedException {
//        String javaHome = System.getProperty("java.home");
//        String javaBin = javaHome +
//                File.separator + "bin" +
//                File.separator + "java";
//        String classpath = System.getProperty("java.class.path");
//        String className = klass.getCanonicalName();
//
//        ProcessBuilder builder = new ProcessBuilder(
//                javaBin, "-cp", classpath, className);
//
//        Process process = builder.start();
//        process.waitFor();
//        return process.exitValue();
//    }
//
//}
