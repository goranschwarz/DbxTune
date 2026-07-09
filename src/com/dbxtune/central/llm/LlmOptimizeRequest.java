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
package com.dbxtune.central.llm;

/** Input to {@link LlmClient#optimize(LlmOptimizeRequest)}, deserialized from the servlet's JSON body. */
public class LlmOptimizeRequest
{
	private String sql;
	private String ddlContext;
	private String plan;
	private String dbVendor;
	private String provider;

	public LlmOptimizeRequest()
	{
	}

	public LlmOptimizeRequest(String sql, String ddlContext, String plan, String dbVendor)
	{
		this.sql        = sql;
		this.ddlContext = ddlContext;
		this.plan       = plan;
		this.dbVendor   = dbVendor;
	}

	public String getSql()        { return sql; }
	public String getDdlContext() { return ddlContext; }
	public String getPlan()       { return plan; }
	public String getDbVendor()   { return dbVendor; }
	public String getProvider()   { return provider; }

	public void setSql       (String sql)        { this.sql        = sql; }
	public void setDdlContext(String ddlContext) { this.ddlContext = ddlContext; }
	public void setPlan      (String plan)        { this.plan       = plan; }
	public void setDbVendor  (String dbVendor)    { this.dbVendor   = dbVendor; }
	public void setProvider  (String provider)    { this.provider   = provider; }
}
