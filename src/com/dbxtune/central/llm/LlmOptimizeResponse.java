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

/** Output of {@link LlmClient#optimize(LlmOptimizeRequest)}, serialized as the servlet's JSON response. */
public class LlmOptimizeResponse
{
	private String originSql;
	private String optimizedSql;
	private String explanation;
	private String rawResponse;
	private String providerId;
	private String promptSent;
	private String model;

	public LlmOptimizeResponse()
	{
	}

	public LlmOptimizeResponse(String optimizedSql, String explanation, String rawResponse, String providerId)
	{
		this.optimizedSql = optimizedSql;
		this.explanation  = explanation;
		this.rawResponse  = rawResponse;
		this.providerId   = providerId;
	}

	/** The model's own echo of the SQL it was asked to optimize - see {@code LlmClientAbstract#buildPrompt}'s JSON contract ({@code origin_sql}). Not necessarily identical to the request's SQL if the model reformatted it. */
	public String getOriginSql()    { return originSql; }
	/** The model's suggested rewrite, or {@code null} if it found no rewrite worth suggesting (the model is asked to leave this empty in that case, rather than echo the original back). */
	public String getOptimizedSql() { return optimizedSql; }
	public String getExplanation()  { return explanation; }
	public String getRawResponse()  { return rawResponse; }
	public String getProviderId()   { return providerId; }
	/** The full prompt text sent to the LLM - shown back to the user so they can see exactly what was asked. */
	public String getPromptSent()   { return promptSent; }
	/** The specific model name used (e.g. "claude-sonnet-4-5") - shown alongside providerId. */
	public String getModel()        { return model; }

	public void setOriginSql   (String originSql)    { this.originSql    = originSql; }
	public void setOptimizedSql(String optimizedSql) { this.optimizedSql = optimizedSql; }
	public void setExplanation (String explanation)  { this.explanation  = explanation; }
	public void setRawResponse (String rawResponse)  { this.rawResponse  = rawResponse; }
	public void setProviderId  (String providerId)   { this.providerId   = providerId; }
	public void setPromptSent  (String promptSent)   { this.promptSent   = promptSent; }
	public void setModel       (String model)        { this.model        = model; }
}
