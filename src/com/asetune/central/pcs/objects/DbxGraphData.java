package com.asetune.central.pcs.objects;

import java.sql.Timestamp;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"cmName", "sessionSampleTime", "graphName", "graphLabel", "graphCategory", "percentGraph", "data"}, alphabetic = true)
public class DbxGraphData
{
	private String    _cmName          ;
	private String    _graphName       ;
	private String    _graphLabel      ;
	private String    _graphCategory   ;
	private boolean   _isPercentGraph  ;
	private Timestamp _sessionSampleTime;
	private Map<String, Double> _data;

	public String    getCmName           () { return _cmName           ;  }
	public String    getGraphName        () { return _graphName        ;  }
	public String    getGraphLabel       () { return _graphLabel       ;  }
	public String    getGraphCategory    () { return _graphCategory    ;  }
	public boolean   isPercentGraph      () { return _isPercentGraph   ;  }
	public Timestamp getSessionSampleTime() { return _sessionSampleTime; }
	public Map<String, Double>    getData() { return _data             ;  }

	public void setCmName           (String     cmName           ) { _cmName            = cmName           ; }
	public void setGraphName        (String     graphName        ) { _graphName         = graphName        ; }
	public void setGraphLabel       (String     graphLabel       ) { _graphLabel        = graphLabel       ; }
	public void setGraphCategory    (String     graphCategory    ) { _graphCategory     = graphCategory       ; }
	public void setIsPercentGraph   (boolean    isPercentGraph   ) { _isPercentGraph    = isPercentGraph   ; }
	public void setSessionSampleTime(Timestamp  sessionSampleTime) { _sessionSampleTime = sessionSampleTime; }
	public void setData             (Map<String, Double> data    ) { _data              = data             ; }

	public DbxGraphData(String cmName, String graphName, Timestamp sessionSampleTime, String graphLabel, String graphCategory, boolean isPercentGraph, Map<String, Double> labelAndDataMap)
	{
		super();

		_cmName            = cmName;
		_graphName         = graphName;
		_sessionSampleTime = sessionSampleTime;
		_graphLabel        = graphLabel;
		_graphCategory     = graphCategory;
		_isPercentGraph    = isPercentGraph;
		
		_data = labelAndDataMap;
	}
}
