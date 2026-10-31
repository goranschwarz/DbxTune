/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * One entry in the model dropdown: a model of a provider, plus the settings to call it with.
 * <p>
 * Built by {@link LlmClientAbstract#getModels()} from the provider's comma separated {@code model} list:
 * <pre>
 * DbxCentral.llm.claude.model=opus-max, claude-sonnet-5-5
 * DbxCentral.llm.claude.opus-max.model=claude-opus-5-5                         -- 'opus-max' is a name of our own, the model id is here
 * DbxCentral.llm.claude.opus-max.label=Opus 5.5 (max effort)
 * DbxCentral.llm.claude.opus-max.extraBody={"output_config":{"effort":"max"}}
 * </pre>
 * The browser only ever sends the entry's {@link #getName() name}: everything that is sent to the
 * provider comes from the configuration.
 */
public class LlmModelEntry
{
	private final String             _name;
	private final String             _modelId;
	private final String             _label;
	private final int                _maxTokens;
	private final int                _timeoutSeconds;
	private final boolean            _stream;
	private final String             _format;
	private final List<ObjectNode>   _extraBodies;
	private final Map<String,String> _extraHeaders;
	private final String             _configProblem;

	public LlmModelEntry(String name, String modelId, String label, int maxTokens, int timeoutSeconds, boolean stream, String format, List<ObjectNode> extraBodies, Map<String,String> extraHeaders, String configProblem)
	{
		_name          = name;
		_modelId       = modelId;
		_label         = label;
		_maxTokens     = maxTokens;
		_timeoutSeconds = timeoutSeconds;
		_stream        = stream;
		_format        = format;
		_extraBodies   = extraBodies  == null ? Collections.emptyList() : extraBodies;
		_extraHeaders  = extraHeaders == null ? Collections.emptyMap()  : extraHeaders;
		_configProblem = configProblem;
	}

	/** The name in the provider's 'model' list - what the dropdown and the request use. */
	public String getName()          { return _name; }

	/** The model id sent to the provider (the name itself, unless {@code <provider>.<name>.model} is set). */
	public String getModelId()       { return _modelId; }

	/** Text in the dropdown. */
	public String getLabel()         { return _label; }

	/** Output token cap, used by the Claude (Messages API) format and Bedrock Converse. */
	public int    getMaxTokens()     { return _maxTokens; }

	/** How long to wait for the provider's answer, in seconds - the HTTP request timeout of the LLM call. */
	public int    getTimeoutSeconds() { return _timeoutSeconds; }

	/** {@link #getTimeoutSeconds()} as a Duration, for HttpRequest.Builder.timeout() */
	public Duration getTimeout()     { return Duration.ofSeconds(_timeoutSeconds); }

	/** Ask the provider to stream its answer (setting 'stream', default true): progress while we wait. Claude format only, for now. */
	public boolean isStream()        { return _stream; }

	/** Request format when it can't be derived from the model id (Vertex: anthropic|gemini, Bedrock: messages|converse), or null. */
	public String getFormat()        { return _format; }

	/** JSON objects to deep-merge into the request body, in order (provider level, then model level). */
	public List<ObjectNode> getExtraBodies() { return _extraBodies; }

	/** Extra or replacing HTTP headers. Never logged or sent to the browser (may hold secrets). */
	public Map<String,String> getExtraHeaders() { return _extraHeaders; }

	/** A configuration error (for example bad JSON in extraBody), or null. A call with this entry fails with this message. */
	public String getConfigProblem() { return _configProblem; }

	@Override
	public String toString()
	{
		return _name.equals(_modelId) ? _name : _name + " (" + _modelId + ")";
	}
}
