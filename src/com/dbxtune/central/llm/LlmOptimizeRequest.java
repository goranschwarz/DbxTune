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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Input to {@link LlmClient#optimize(LlmOptimizeRequest)}, deserialized from the servlet's JSON body.
 * <p>
 * NOTE: 'ignoreUnknown = true' is important -- Helper.createObjectMapper() does NOT disable Jackson's
 *       FAIL_ON_UNKNOWN_PROPERTIES, so without it, a NEWER DbxCentral page posting a field that THIS
 *       (older) server doesn't know about would fail the whole request with HTTP 400.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmOptimizeRequest
{
	private String sql;
	private String ddlContext;
	private String plan;
	private String dbVendor;
	private String provider;

	/**
	 * Plain-text execution statistics / usage pattern for the statement over the reported period.
	 * <p>
	 * Built by 'dbxLlmAdvice.js' from the sparkline sub-table it harvests out of the Daily Summary
	 * Report at click time (see SparklineHelper.getWorkloadHarvesterJs()). Tells the LLM how often and
	 * <i>when</i> the statement actually runs, which changes what good advice looks like.
	 */
	private String workloadProfile;

	// Defaults false, so the real POST /api/llm/optimize-sql flow (whose caller parses the reply as
	// the {origin_sql, optimized_sql, explanation} JSON object - see LlmClientAbstract.buildPrompt())
	// is unaffected unless a caller explicitly opts in. Only GET /api/llm/optimize-sql's "no exec"
	// prompt-preview path (LlmSqlOptimizeServlet.doGet()) sets this true - a human pasting the prompt
	// into an LLM chat UI by hand wants a normal readable answer, not raw JSON.
	private boolean preview;

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

	public String  getSql()             { return sql; }
	public String  getDdlContext()      { return ddlContext; }
	public String  getPlan()            { return plan; }
	public String  getDbVendor()        { return dbVendor; }
	public String  getProvider()        { return provider; }
	public String  getWorkloadProfile() { return workloadProfile; }
	public boolean isPreview()          { return preview; }

	public void setSql            (String sql)             { this.sql             = sql; }
	public void setDdlContext     (String ddlContext)      { this.ddlContext      = ddlContext; }
	public void setPlan           (String plan)            { this.plan            = plan; }
	public void setDbVendor       (String dbVendor)        { this.dbVendor        = dbVendor; }
	public void setProvider       (String provider)        { this.provider        = provider; }
	public void setWorkloadProfile(String workloadProfile) { this.workloadProfile = workloadProfile; }
	public void setPreview        (boolean preview)        { this.preview         = preview; }
}
