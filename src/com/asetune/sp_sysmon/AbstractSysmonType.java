package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public abstract class AbstractSysmonType
{
	protected StringBuilder _reportText = new StringBuilder();

	protected int _aseVersion       = 0;

	protected int _NumEngines       = 0;
	protected int _NumXacts         = 0;
	protected int _NumElapsedMs     = 0;

	protected int _fieldName_pos  = -1;
	protected int _groupName_pos  = -1;
	protected int _instanceid_pos = -1;
	protected int _field_id_pos   = -1;
	protected int _value_pos      = -1;

	private List<List<Object>> _data;

	
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

	public AbstractSysmonType(CountersModel cm)
	{
		//		if (_cm == null)
//			throw new xxxx;
//
//		if ( ! _cm.hasDiffData() )
//			throw new xxxx;

		_fieldName_pos  = cm.findColumn("field_name");
		_groupName_pos  = cm.findColumn("group_name");
		_instanceid_pos = cm.findColumn("instanceid");
		_field_id_pos   = cm.findColumn("field_id");
		_value_pos      = cm.findColumn("value");

		_NumElapsedMs   = cm.getLastSampleInterval();

		if (cm.isRuntimeInitialized())
			_aseVersion = cm.getServerVersion();

		setData( cm.getDataCollection(CountersModel.DATA_DIFF) );
	}

	public AbstractSysmonType(int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		_fieldName_pos  = fieldName_pos;
		_groupName_pos  = groupName_pos;
		_instanceid_pos = instanceId_pos;
//		_field_id_pos   = fieldId_pos;
		_value_pos      = value_pos;

		_NumElapsedMs   = sampleTimeInMs;
		
		_aseVersion     = aseVersion;

		setData( data );
	}

	public abstract void calc();

	protected void addReportLn(String line)
	{
//		System.out.println(line);

		_reportText.append(line);
		_reportText.append("\n");
	}
	protected void addReportLn(String name, int counter)
	{
		if (_NumXacts == 0)
			_NumXacts = 1;

		double perSec  = counter / (_NumElapsedMs  *1.0) / 1000.0;
		double perTran = counter / (_NumXacts      * 1.0);
		double total   = counter;
//			BigDecimal xPerSec  = new BigDecimal(perSec) .setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			BigDecimal xPerTran = new BigDecimal(perTran).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			BigDecimal xTotal   = new BigDecimal(total)  .setScale(1, BigDecimal.ROUND_HALF_EVEN);
		
		String line = String.format("%1$-50s cnt=%2$10d, perSec='%3$3.1f', perTran='%4$3.1f', total='%5$3.1f'.", 
//		String line = String.format("%1$-50s %2$10d %3$3.1f %4$3.1f %5$3.1f", 
				name, counter, perSec, perTran, total);

		_reportText.append(line);
		_reportText.append("\n");

//		System.out.println(line);
		

//			System.out.println(StringUtil.left(name, 50) + " cnt="+StringUtil.right(counter+"", 10)+", perSec='"+xPerSec+"', perTran='"+xPerTran+"', total='"+xTotal+"'.");
	}
	protected void addReportLn(String name, int counter, int NumElapsedMs, int NumXacts, int NumTaskSwitch)
	{
		if (NumXacts == 0)
			NumXacts = 1;

		double perSec  = counter / (NumElapsedMs  *1.0) / 1000.0;
		double perTran = counter / (NumXacts      * 1.0);
		double total   = 100.0 * (counter / (NumTaskSwitch * 1.0));
//			BigDecimal xPerSec  = new BigDecimal(perSec) .setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			BigDecimal xPerTran = new BigDecimal(perTran).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			BigDecimal xTotal   = new BigDecimal(total)  .setScale(1, BigDecimal.ROUND_HALF_EVEN);
		
		String line = String.format("%1$-50s cnt=%2$10d, perSec='%3$3.1f', perTran='%4$3.1f', total='%5$3.1f'.", 
//		String line = String.format("%1$-50s %2$10d %3$3.1f %4$3.1f %5$3.1f", 
				name, counter, perSec, perTran, total);
//		System.out.println(line);
		_reportText.append(line);
		_reportText.append("\n");

//			System.out.println(StringUtil.left(name, 50) + " cnt="+StringUtil.right(counter+"", 10)+", perSec='"+xPerSec+"', perTran='"+xPerTran+"', total='"+xTotal+"'.");

//			select @rptline = "    Network services" + space(9) +
//			str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//			space(2) +
//			str(@tmp_int / convert(real, @NumXacts),12,1) +
//			space(2) +
//			str(@tmp_int, 10) + space(5) +
//			str(100.0 * @tmp_int / @NumTaskSwitch,5,1) +
//			@psign
	}

	public String getReport()
	{
		if (hasReportText())
		{
			String rptHead = getReportHead();
			String rptText = getReportText();
			return rptHead + rptText;
		}
		return "";
	}

	public abstract String getReportHead();

	public boolean hasReportText()
	{
		return _reportText.length() > 0;
	}
	public String getReportText()
	{
		return _reportText.toString();
	}
}
