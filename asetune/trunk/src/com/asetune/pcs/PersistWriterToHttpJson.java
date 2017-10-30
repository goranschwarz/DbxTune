package com.asetune.pcs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;

import com.asetune.cm.CountersModel;
import com.asetune.pcs.sqlcapture.SqlCaptureDetails;
import com.asetune.utils.Configuration;
import com.google.gson.stream.JsonWriter;

public class PersistWriterToHttpJson 
extends PersistWriterBase
//implements IPersistWriter
{

	@Override
	public void close()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Configuration getConfig()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConfigStr()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveSample(PersistContainer cont)
	{
		StringWriter sw = new StringWriter();
		JsonWriter w = new JsonWriter(sw);
		w.setIndent("    ");

		System.out.println();
		System.out.println("#### BEGIN JSON #######################################################################");
		System.out.println("getSessionStartTime="+cont.getSessionStartTime());
		System.out.println("getMainSampleTime="+cont.getMainSampleTime());
		System.out.println("getServerName="+cont.getServerName());
		System.out.println("getOnHostname="+cont.getOnHostname());
		try
		{
			w.beginObject();
			
			w.name("SessionStartTime")             .value(cont.getSessionStartTime() +"");
			w.name("MainSampleTime")               .value(cont.getMainSampleTime()   +"");
			w.name("ServerName")                   .value(cont.getServerName());
			w.name("OnHostname")                   .value(cont.getOnHostname());

			w.name("Collectors");
			w.beginArray();

			//--------------------------------------
			// COUNTERS
			//--------------------------------------
			for (CountersModel cm : cont._counterObjects)
			{
				cm.toJson(w, true, true);
			}

			w.endArray();
			w.endObject();
			w.close();
			
//			System.out.println(sw.toString());
			File toFileName = new File("c:\\tmp\\PersistWriterToHttpJson.tmp.json");
			System.out.println("Writing JSON to file: "+toFileName.getAbsolutePath());
			FileUtils.writeStringToFile(toFileName, sw.toString());
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		System.out.println("#### END JSON #######################################################################");
	}

	@Override
	public void saveSqlCaptureDetails(SqlCaptureDetails sqlCaptureDetails)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startServices() throws Exception
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stopServices(int maxWaitTimeInMs)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public void init(Configuration props) throws Exception
	{
		System.out.println(getName()+": INIT.....................................");
	}

}
