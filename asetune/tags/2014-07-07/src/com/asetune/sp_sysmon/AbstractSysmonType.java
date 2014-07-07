package com.asetune.sp_sysmon;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import com.asetune.cm.CountersModel;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public abstract class AbstractSysmonType
{
	protected StringBuilder _reportText = new StringBuilder();

	protected SpSysmon _sysmon      = null;

	protected int _aseVersion       = 0;

//	protected int _NumEngines       = 0;
//	protected int _NumXacts         = 0;
//	protected int _NumElapsedMs     = 0;

	public void setNumEngines(int numEngines)     { _sysmon.setNumEngines(numEngines); }
	public void setNumXacts(int numXacts)         { _sysmon.setNumXacts(numXacts); }
	public void setNumElapsedMs(int numElapsedMs) { _sysmon.setNumElapsedMs(numElapsedMs); }
	
	public int getNumEngines()   { return _sysmon.getNumEngines(); }
	public int getNumXacts()     { return _sysmon.getNumXacts(); }
	public int getNumElapsedMs() { return _sysmon.getNumElapsedMs(); }

	protected int _fieldName_pos   = -1;
	protected int _groupName_pos   = -1;
	protected int _instanceid_pos  = -1;
	protected int _field_id_pos    = -1;
	protected int _value_pos       = -1;
	protected int _description_pos = -1;

	private List<List<Object>> _data;

	                                       //|  Xxxxxx xxxxxxxxxxxx             per sec      per xact       count  % of total
	private static final String cntLine    = "  -------------------------  ------------  ------------  ----------  ----------";
	private static final String cntFmt     = "  %1$-25s %2$12.1d %3$12.1f %4$10f' %5$10.1f";
//	private static final String cntFmt     = "  %1$-25s cnt=%2$10d, perSec='%3$3.1f', perTran='%4$3.1f', total='%5$3.1f'.";

	private static final String sumLine    = "  -------------------------  ------------  ------------  ----------";
	private static final String sumFmt     = "  %1$-25s %2$12.1d %3$12.1f %4$10f'";
//	private static final String sumFmt     = "%1$-50s cnt=%2$10d, perSec='%3$3.1f', perTran='%4$3.1f', total='%5$3.1f'.";

	private static final String na_str     = "n/a";
	private static final String section    = "===============================================================================";
	private static final String subsection = "-------------------------------------------------------------------------------";

	public void printReport()
	{
	}

	public void setData(List<List<Object>> data)
	{
		_data = data;
	}
	public List<List<Object>> getData()
	{
		return _data;
	}

	public AbstractSysmonType(SpSysmon sysmon, CountersModel cm)
	{
		//		if (_cm == null)
//			throw new xxxx;
//
//		if ( ! _cm.hasDiffData() )
//			throw new xxxx;

		_sysmon = sysmon;
		
		_fieldName_pos   = cm.findColumn("field_name");
		_groupName_pos   = cm.findColumn("group_name");
		_instanceid_pos  = cm.findColumn("instanceid");
		_field_id_pos    = cm.findColumn("field_id");
		_value_pos       = cm.findColumn("value");
		_description_pos = cm.findColumn("description");

		_sysmon.setNumElapsedMs( (int) cm.getSampleInterval() );

		int       interval  = (int) cm.getSampleInterval();
		Timestamp startTime = null;
		Timestamp endTime   = cm.getSampleTime();
		if (endTime != null)
			startTime = new Timestamp( endTime.getTime() - interval);
		
		_sysmon.setAseVersionStr   (cm.getServerName());
		_sysmon.setAseServerNameStr(cm.isRuntimeInitialized() ? cm.getServerVersionStr() : "unknown");
		_sysmon.setRunDate         (startTime);
		_sysmon.setSampleStartTime (startTime);
		_sysmon.setSampleEndTime   (endTime);
		_sysmon.setSampleInterval  (interval);
		_sysmon.setSampleMode      ("unknown");
		_sysmon.setCounterClearTime(cm.getCounterClearTime());

		
		if (cm.isRuntimeInitialized())
			_aseVersion = cm.getServerVersion();

		setData( cm.getDataCollection(CountersModel.DATA_DIFF) );
	}

	public AbstractSysmonType(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		_sysmon = sysmon;
		
		_fieldName_pos  = fieldName_pos;
		_groupName_pos  = groupName_pos;
		_instanceid_pos = instanceId_pos;
//		_field_id_pos   = fieldId_pos;
		_value_pos      = value_pos;

		_sysmon.setNumElapsedMs(sampleTimeInMs);
		
		_aseVersion     = aseVersion;

		_sysmon.setAseVersionStr   (Ver.versionIntToStr(aseVersion));
		_sysmon.setAseServerNameStr("UNKNOWN");
		_sysmon.setRunDate         (null);
		_sysmon.setSampleStartTime (null);
		_sysmon.setSampleEndTime   (null);
		_sysmon.setSampleInterval  (sampleTimeInMs);
		_sysmon.setSampleMode      ("unknown");
		_sysmon.setCounterClearTime(null);

		setData( data );
	}

	public abstract void calc();

	/**
	 * Write a HEADER
	 * <pre>
	 * The Passed Parameter Text           per sec    per xact       count  % of total
	 * ------------------------------- ----------- ----------- ----------- -----------
	 * </pre>
	 */
	protected void addReportHead(String txt)
	{
		String headTxt = StringUtil.left(txt, 32, false);
		_reportText.append(headTxt).append(                "    per sec    per xact       count  % of total\n");
		_reportText.append("------------------------------- ----------- ----------- ----------- -----------\n");
	}

	/**
	 * Write a HEADER (2 spaces at the left)
	 * <pre>
	 *   The Passed Parameter Text           per sec    per xact       count  % of total
	 *   ------------------------------- ----------- ----------- ----------- -----------
	 * </pre>
	 */
	protected void addReportHead2(String txt)
	{
		String headTxt = StringUtil.left(txt, 32, false);
		_reportText.append(headTxt).append(                "    per sec    per xact       count  % of total\n");
		_reportText.append("  ----------------------------- ----------- ----------- ----------- -----------\n");
	}

	/**
	 * <pre>
	 *   - This section has not yet been implemented by AseTune...
	 * </pre>
	 */
	protected void addReportLnNotYetImplemented()
	{
		_reportText.append("  - This section has not yet been implemented by AseTune...\n");
	}

	protected void addReportLn()
	{
		_reportText.append("\n");
	}
	
	/**
	 * <pre>
	 * this input parameter goes here
	 * </pre>
	 */
	protected void addReportLn(String line)
	{
		_reportText.append(line);
		_reportText.append("\n");
	}

	/**
	 * Write Report Line Summary
	 * <pre>
	 * ------------------------------- ----------- ----------- -----------
	 * </pre>
	 */
	protected void addReportLnSum()
	{
		_reportText.append("------------------------------- ----------- ----------- -----------\n");
	}

	/**
	 * Write Report Line Summary (with 2 spaces to the left)
	 * <pre>
	 *   ----------------------------- ----------- ----------- -----------
	 * </pre>
	 */
	protected void addReportLnSum2()
	{
		_reportText.append("  ----------------------------- ----------- ----------- -----------\n");
	}

	/**
	 * Write COUNTER
	 * <pre>
	 *    input param goes here             ###           ###         ###       n/a
	 * </pre>
	 */
	protected void addReportLnCnt(String name, int counter)
	{
		addReportLnCnt(name, counter, true);
	}

	/**
	 * Write COUNTER Summary (same as addReportLnCnt but without the 'n/a' as the last col)
	 * <pre>
	 *    input param goes here             ###           ###         ###
	 * </pre>
	 */
	protected void addReportLnCntSum(String name, int counter)
	{
		addReportLnCnt(name, counter, false);
	}

	/**
	 * Write COUNTER (printNA==true: 'n/a' as the last col)
	 * <pre>
	 *    input param goes here             ###           ###         ###       n/a
	 * </pre>
	 */
	private void addReportLnCnt(String name, int counter, boolean printNA)
	{
		int NumElapsedMs = _sysmon.getNumElapsedMs();
		if (NumElapsedMs == 0)
			NumElapsedMs = 1;
		int NumXacts     = _sysmon.getNumXacts();
		if (NumXacts == 0)
			NumXacts = 1;

//System.out.println(getReportHead()+":cnt.NumElapsedMs  = "+NumElapsedMs);
		double dPerSec  = counter / ((NumElapsedMs * 1.0) / 1000.0);
		double dPerTran = counter /  (NumXacts     * 1.0);
		double dTotal   = counter;
		BigDecimal perSec  = new BigDecimal(dPerSec) .setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal perTran = new BigDecimal(dPerTran).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal total   = new BigDecimal(dTotal)  .setScale(1, BigDecimal.ROUND_HALF_EVEN);

		String na = "";
		if (printNA)
			na = "n/a  ";
			
        //|  Xxxxxx xxxxxxxxxxxx             per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ----------
		String line = //"  " +
			StringUtil.left(name       , 32, false) +
			StringUtil.right(perSec +"", 11) + " " +
			StringUtil.right(perTran+"", 11) + " " +
			StringUtil.right(counter+"", 11) + " " +
			StringUtil.right(na        , 11);

		_reportText.append(line).append("\n");
	}

	/**
	 * Write COUNTER with PERCENT Values
	 * <pre>
	 *    input param goes here             ###           ###         ###      ###.# %
	 * </pre>
	 */
	protected void addReportLnPct(String name, int counter, int divideBy)
	{
		int NumElapsedMs = _sysmon.getNumElapsedMs();
		if (NumElapsedMs == 0)
			NumElapsedMs = 1;
		int NumXacts     = _sysmon.getNumXacts();
		if (NumXacts == 0)
			NumXacts = 1;
		if (divideBy == 0)
			divideBy = 1;

//System.out.println(getReportHead()+":pct.NumElapsedMs  = "+NumElapsedMs);
		double dPerSec  = counter / ((NumElapsedMs  * 1.0) / 1000.0);
		double dPerTran = counter /  (NumXacts      * 1.0);
		double dPct     = 100.0 * (counter / (divideBy * 1.0));
		BigDecimal perSec  = new BigDecimal(dPerSec) .setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal perTran = new BigDecimal(dPerTran).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal pct     = new BigDecimal(dPct)    .setScale(1, BigDecimal.ROUND_HALF_EVEN);
		
		//|  Xxxxxx xxxxxxxxxxxx             per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ----------
		String line = //"  " +
			StringUtil.left(name       , 32, false) +
			StringUtil.right(perSec +"", 11) + " " +
			StringUtil.right(perTran+"", 11) + " " +
			StringUtil.right(counter+"", 11) + " " +
			StringUtil.right(pct +" %" , 11);

		_reportText.append(line).append("\n");
	}

	/**
	 * Write SINGLE COUNTER (not calculated per second, transaction)
	 * <pre>
	 *    input param goes here             n/a           n/a         ###       n/a %
	 * </pre>
	 */
	protected void addReportLnSC(String name, int counter)
	{
		String line = //"  " +
			StringUtil.left(name       , 32, false) +
			StringUtil.right("n/a"     , 11) + " " +
			StringUtil.right("n/a"     , 11) + " " +
			StringUtil.right(counter+"", 11) + " " +
			StringUtil.right("n/a %"   , 11);

		_reportText.append(line).append("\n");
	}

	/**
	 * Write SINGLE COUNTER use Divide (not calculated per second, transaction)
	 * <pre>
	 *    input param goes here             n/a           n/a         ###       n/a %
	 * </pre>
	 */
	protected void addReportLnScD(String name, int counter, int divideby, int scale)
	{
		addReportLnScDiv(name, counter, divideby, scale);
	}
	/**
	 * Write SINGLE COUNTER use Divide (not calculated per second, transaction)
	 * <pre>
	 *    input param goes here             n/a           n/a         ###       n/a % 
	 * </pre>
	 */
	protected void addReportLnScDiv(String name, int counter, int divideby, int scale)
	{
		double dCalc = 0;
		if (divideby > 0)
			dCalc = counter / (divideby * 1.0);

		BigDecimal calc = new BigDecimal(dCalc).setScale(scale, BigDecimal.ROUND_HALF_EVEN);

		String line = //"  " +
			StringUtil.left(name    , 32, false) +
			StringUtil.right("n/a"  , 11) + " " +
			StringUtil.right("n/a"  , 11) + " " +
			StringUtil.right(calc+"", 11) + " " +
			StringUtil.right("n/a %", 11);

		_reportText.append(line).append("\n");
	}
	/**
	 * Summary per second
	 * <pre>
	 *    input param goes here        #.##### seconds 
	 * </pre>
	 */
	protected void addReportLnSec(String name, int counter, int divideby, int scale)
	{
		double dCalc = 0;
		if (divideby > 0)
			dCalc = counter / (divideby * 1.0);

		BigDecimal calc = new BigDecimal(dCalc).setScale(scale, BigDecimal.ROUND_HALF_EVEN);

		String line = //"  " +
			StringUtil.left(name,     32, false) +
			StringUtil.right(""+calc, 11) + " seconds";

		_reportText.append(line).append("\n");
	}

	protected String getReport()
	{
		if (hasReportText())
		{
//			String rptHead = 
//				"\n" +
//				"===============================================================================\n" +
//				" "+getReportHead()+" \n" +
//				"-------------------------------------------------------------------------------\n";

			// Center the report name
//			String reportName = getReportName();
//			int leftPad = (80 - 2 - reportName.length()) / 2;
//			reportName = StringUtil.replicate("-", leftPad) + " " + reportName;
//
//			int rightPad = (80 - reportName.length() - 2);
//			reportName = reportName + " " + StringUtil.replicate("-", rightPad);
//
//			String rptHead =
//				"\n" +
//				"===============================================================================\n" +
//				reportName+"\n" +
//				"===============================================================================\n";
//			String rptText = getReportText();

			// Left side: the report name
			String reportName = "-- " + getReportName();
			int rightPad = (80 - reportName.length() - 2);
			reportName = reportName + " " + StringUtil.replicate("-", rightPad);

			String rptHead =
				"\n" +
				"===============================================================================\n" +
				reportName+"\n" +
				"===============================================================================\n";
			String rptText = getReportText();

			return rptHead + rptText;
		}
		return "";
	}

	public abstract String getReportName();

	public boolean hasReportText()
	{
		return _reportText.length() > 0;
	}
	public String getReportText()
	{
		return _reportText.toString();
	}
}
