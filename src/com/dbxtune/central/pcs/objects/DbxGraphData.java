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
package com.dbxtune.central.pcs.objects;

import java.sql.Timestamp;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"cmName", "sessionSampleTime", "graphName", "graphLabel", "graphProps", "graphCategory", "percentGraph", "data"}, alphabetic = true)
public class DbxGraphData
{
	private String    _cmName          ;
	private String    _graphName       ;
	private String    _graphLabel      ;
	private String    _graphProps      ;
	private String    _graphCategory   ;
	private boolean   _isPercentGraph  ;
	private Timestamp _sessionSampleTime;
	private Map<String, Double> _data;

	public String    getCmName           () { return _cmName           ;  }
	public String    getGraphName        () { return _graphName        ;  }
	public String    getGraphLabel       () { return _graphLabel       ;  }
	public String    getGraphProps       () { return _graphProps       ;  }
	public String    getGraphCategory    () { return _graphCategory    ;  }
	public boolean   isPercentGraph      () { return _isPercentGraph   ;  }
	public Timestamp getSessionSampleTime() { return _sessionSampleTime; }
	public Map<String, Double>    getData() { return _data             ;  }

	public void setCmName           (String     cmName           ) { _cmName            = cmName           ; }
	public void setGraphName        (String     graphName        ) { _graphName         = graphName        ; }
	public void setGraphLabel       (String     graphLabel       ) { _graphLabel        = graphLabel       ; }
	public void setGraphProps       (String     graphProps       ) { _graphProps        = graphProps       ; }
	public void setGraphCategory    (String     graphCategory    ) { _graphCategory     = graphCategory    ; }
	public void setIsPercentGraph   (boolean    isPercentGraph   ) { _isPercentGraph    = isPercentGraph   ; }
	public void setSessionSampleTime(Timestamp  sessionSampleTime) { _sessionSampleTime = sessionSampleTime; }
	public void setData             (Map<String, Double> data    ) { _data              = data             ; }

	public DbxGraphData(String cmName, String graphName, Timestamp sessionSampleTime, String graphLabel, String graphProps, String graphCategory, boolean isPercentGraph, Map<String, Double> labelAndDataMap)
	{
		super();

		_cmName            = cmName;
		_graphName         = graphName;
		_sessionSampleTime = sessionSampleTime;
		_graphLabel        = graphLabel;
		_graphProps        = graphProps;
		_graphCategory     = graphCategory;
		_isPercentGraph    = isPercentGraph;
		
		_data = labelAndDataMap;
	}

	public void removeSerieName(String label)
	{
		_data.remove(label);
	}
}
