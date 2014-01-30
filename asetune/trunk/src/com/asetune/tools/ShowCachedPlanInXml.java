package com.asetune.tools;

import java.io.File;
import java.util.List;

/**
 * THIS DOES NOT YET WORK
 * 
 * @author gorans
 */
public class ShowCachedPlanInXml
{

	public static Entry parse(String xml)
	{
		return null;
	}
	public static Entry parse(File file)
	{
		return null;
	}

	public static List<Entry> parse(List<String> xmlList)
	{
		return null;
	}

	/**
	 * A entry for a XML instance
	 * 
	 * @author gorans
	 */
	public static class Entry
	{
		private int    _planId          = -1;
		private String _planStatus      = null;
		private String _planSharing     = null;
		private long   _execCount       = -1;
		private long   _maxTime         = -1;
		private long   _avgTime         = -1;
		private long   _maxPreQueryTime = -1;
		private long   _avgPreQueryTime = -1;
		private long   _maxExecTime     = -1;
		private long   _avgExecTime     = -1;

		private List<?> _compileParameters = null;
		private List<?> _execParameters    = null;
		private List<?> _opTree            = null;
	}
}
