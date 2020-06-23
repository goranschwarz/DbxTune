/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.ui.tooltip.suppliers;

import org.apache.commons.lang3.StringUtils;
import org.fife.ui.autocomplete.Completion;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.StringUtil;

/*
<Entries>
  <Entry>
    <CmdName>xp_cmdshell</CmdName>
    <Module>ESP</Module>
    <FromVersion>15.7.0 ESD#2</Module>
    <Description>xp_cmdshell - Executes a native operating system command on the host system running Adaptive Server.</Description>
    <Syntax>
        xp_cmdshell command [, no_output]
    </Syntax>
  </Entry>
</Entries>
*/
public class TtpEntry
{
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	protected static final String       XML_TAG_ENTRIES         = "Entries";
	protected static final String XML_BEGIN_TAG_ENTRIES         = "<"  + XML_TAG_ENTRIES + ">";
	protected static final String XML_END___TAG_ENTRIES         = "</" + XML_TAG_ENTRIES + ">";
	
	protected static final String       XML_TAG_ENTRY           = "Entry";
	protected static final String XML_BEGIN_TAG_ENTRY           = "<"  + XML_TAG_ENTRY + ">";
	protected static final String XML_END___TAG_ENTRY           = "</" + XML_TAG_ENTRY + ">";

	protected static final String       XML_SUBTAG_CMD_NAME     = "CmdName";
	protected static final String XML_BEGIN_SUBTAG_CMD_NAME     = "<"  + XML_SUBTAG_CMD_NAME + ">";
	protected static final String XML_END___SUBTAG_CMD_NAME     = "</" + XML_SUBTAG_CMD_NAME + ">";

	protected static final String       XML_SUBTAG_MODULE       = "Module";
	protected static final String XML_BEGIN_SUBTAG_MODULE       = "<"  + XML_SUBTAG_MODULE + ">";
	protected static final String XML_END___SUBTAG_MODULE       = "</" + XML_SUBTAG_MODULE + ">";

	protected static final String       XML_SUBTAG_SECTION      = "Section";
	protected static final String XML_BEGIN_SUBTAG_SECTION      = "<"  + XML_SUBTAG_SECTION + ">";
	protected static final String XML_END___SUBTAG_SECTION      = "</" + XML_SUBTAG_SECTION + ">";

	protected static final String       XML_SUBTAG_FROM_VERSION = "FromVersion";
	protected static final String XML_BEGIN_SUBTAG_FROM_VERSION = "<"  + XML_SUBTAG_FROM_VERSION + ">";
	protected static final String XML_END___SUBTAG_FROM_VERSION = "</" + XML_SUBTAG_FROM_VERSION + ">";

	protected static final String       XML_SUBTAG_DESCRIPTION  = "Description";
	protected static final String XML_BEGIN_SUBTAG_DESCRIPTION  = "<"  + XML_SUBTAG_DESCRIPTION + ">";
	protected static final String XML_END___SUBTAG_DESCRIPTION  = "</" + XML_SUBTAG_DESCRIPTION + ">";

	protected static final String       XML_SUBTAG_SYNTAX       = "Syntax";
	protected static final String XML_BEGIN_SUBTAG_SYNTAX       = "<"  + XML_SUBTAG_SYNTAX + ">";
	protected static final String XML_END___SUBTAG_SYNTAX       = "</" + XML_SUBTAG_SYNTAX + ">";

	protected static final String       XML_SUBTAG_PARAMETERS   = "Parameters";
	protected static final String XML_BEGIN_SUBTAG_PARAMETERS   = "<"  + XML_SUBTAG_PARAMETERS + ">";
	protected static final String XML_END___SUBTAG_PARAMETERS   = "</" + XML_SUBTAG_PARAMETERS + ">";

	protected static final String       XML_SUBTAG_EXAMPLE      = "Example";
	protected static final String XML_BEGIN_SUBTAG_EXAMPLE      = "<"  + XML_SUBTAG_EXAMPLE + ">";
	protected static final String XML_END___SUBTAG_EXAMPLE      = "</" + XML_SUBTAG_EXAMPLE + ">";

	protected static final String       XML_SUBTAG_USAGE        = "Usage";
	protected static final String XML_BEGIN_SUBTAG_USAGE        = "<"  + XML_SUBTAG_USAGE + ">";
	protected static final String XML_END___SUBTAG_USAGE        = "</" + XML_SUBTAG_USAGE + ">";

	protected static final String       XML_SUBTAG_PERMISSIONS  = "Permissions";
	protected static final String XML_BEGIN_SUBTAG_PERMISSIONS  = "<"  + XML_SUBTAG_PERMISSIONS + ">";
	protected static final String XML_END___SUBTAG_PERMISSIONS  = "</" + XML_SUBTAG_PERMISSIONS + ">";

	protected static final String       XML_SUBTAG_SEE_ALSO     = "SeeAlso";
	protected static final String XML_BEGIN_SUBTAG_SEE_ALSO     = "<"  + XML_SUBTAG_SEE_ALSO + ">";
	protected static final String XML_END___SUBTAG_SEE_ALSO     = "</" + XML_SUBTAG_SEE_ALSO + ">";

	protected static final String       XML_SUBTAG_SOURCE_URL   = "SourceURL";
	protected static final String XML_BEGIN_SUBTAG_SOURCE_URL   = "<"  + XML_SUBTAG_SOURCE_URL + ">";
	protected static final String XML_END___SUBTAG_SOURCE_URL   = "</" + XML_SUBTAG_SOURCE_URL + ">";

	//---------------------------------------------------
	// Below is members
	//---------------------------------------------------
	private String _cmdName     = null;
	private String _module      = null;
	private String _section     = null;
	private String _version     = null;
	private String _desciption  = null;
	private String _syntax      = null;
	private String _parameters  = null;
	private String _example     = null;
	private String _usage       = null;
	private String _permissions = null;
	private String _seeAlso     = null;
	private String _sourceUrl   = null;

	//---------------------------------------------------
	// Constructors
	//---------------------------------------------------
	public TtpEntry()
	{
	}
	public TtpEntry(String cmdName, String module, String desc, String syntax)
	{
		setCmdName(cmdName);
		setModule(module);
		setDescription(desc);
		setSyntax(syntax);
	}

	//---------------------------------------------------
	// Methods
	//---------------------------------------------------
	public String  getCmdName()                { return _cmdName     == null ? "" : _cmdName;     }
	public String  getModule()                 { return _module      == null ? "" : _module;      }
	public String  getSection()                { return _section     == null ? "" : _section;     }
	public String  getFromVersion()            { return _version     == null ? "" : _version;     }
	public String  getDescription()            { return _desciption  == null ? "" : _desciption;  }
	public String  getSyntax()                 { return _syntax      == null ? "" : _syntax;      }
	public String  getParameters()             { return _parameters  == null ? "" : _parameters;  }
	public String  getExample()                { return _example     == null ? "" : _example;     }
	public String  getUsage()                  { return _usage       == null ? "" : _usage;       }
	public String  getPermissions()            { return _permissions == null ? "" : _permissions; }
	public String  getSeeAlso()                { return _seeAlso     == null ? "" : _seeAlso;     }
	public String  getSourceUrl()              { return _sourceUrl   == null ? "" : _sourceUrl;   }

	public void    setCmdName     (String str) { _cmdName     = str; }
	public void    setModule      (String str) { _module      = str; }
	public void    setSection     (String str) { _section     = str; }
	public void    setFromVersion (String str) { _version     = str; }
	public void    setDescription (String str) { _desciption  = str; }
	public void    setSyntax      (String str) { _syntax      = str; }
	public void    setParameters  (String str) { _parameters  = str; }
	public void    setExample     (String str) { _example     = str; }
	public void    setUsage       (String str) { _usage       = str; }
	public void    setPermissions (String str) { _permissions = str; }
	public void    setSeeAlso     (String str) { _seeAlso     = str; }
	public void    setSourceUrl   (String str) { _sourceUrl   = str; }

	public boolean hasSection    ()           { return ! StringUtil.isNullOrBlank(getSection());     }
	public boolean hasFromVersion()           { return ! StringUtil.isNullOrBlank(getFromVersion()); }
	public boolean hasParameters ()           { return ! StringUtil.isNullOrBlank(getParameters());  }
	public boolean hasExample    ()           { return ! StringUtil.isNullOrBlank(getExample());     }
	public boolean hasUsage      ()           { return ! StringUtil.isNullOrBlank(getUsage());       }
	public boolean hasPermissions()           { return ! StringUtil.isNullOrBlank(getPermissions()); }
	public boolean hasSeeAlso    ()           { return ! StringUtil.isNullOrBlank(getSeeAlso());     }
	public boolean hasSourceUrl  ()           { return ! StringUtil.isNullOrBlank(getSourceUrl());   }

	@Override
	public String toString()
	{
		return super.toString() + " " +
			"CmdName='"     + getCmdName()     + "', " +
			"Module='"      + getModule()      + "', " +
			"Section='"     + getSection()     + "', " +
			"FromVersion='" + getFromVersion() + "', " +
			"Description='" + getDescription() + "', " +
			"Syntax='"      + getSyntax()      + "', " +
			"Parameters='"  + getParameters()  + "', " +
			"Example='"     + getExample()     + "', " +
			"Usage='"       + getUsage()       + "', " +
			"Permissions='" + getPermissions() + "', " +
			"SeeAlso='"     + getSeeAlso()     + "', " +
			"SourceUrl='"   + getSourceUrl()   + "'.";
	}

	/**
	 * Get a HTML entry of this
	 * @return
	 */
	public String toHtml()
	{
		return toHtml(false, null);
	}
	/**
	 * Get a HTML entry of this
	 * @param addHtmlBeginEnd add &lt;html&gt; &lt;/html&gt; around the returned text
	 * @param preStr for example if you want to add a Horizontal Ruler "&lt;hr&gt;" before this entry
	 * @return
	 */
	public String toHtml(boolean addHtmlBeginEnd, String preStr)
	{
		StringBuilder sb = new StringBuilder();

		if (addHtmlBeginEnd)
			sb.append("<html>");
			
		if (preStr != null)
			sb.append(preStr);

		sb.append("<b>Topic:       </b>").append(getCmdName()).append("<br>");
		sb.append("<b>Module:      </b>").append(getModule()).append("<br>");

		if (hasSection())
			sb.append("<b>Section:     </b>").append(getSection()).append("<br>");
		if (hasFromVersion())
			sb.append("<b>FromVersion: </b>").append(getFromVersion()).append("<br>");

		sb.append("<b>Description: </b>").append(getDescription()).append("<br>");

		// Syntax needs to be HTML formated
		sb.append("<br>");
		sb.append("<b>Syntax: </b><br>");
		sb.append(getSyntax()).append("<br>");

		if ( hasParameters() )
		{
			sb.append("<b>Parameters:</b><br>");
			sb.append(getParameters()).append("<br>");
		}

		if ( hasExample() )
		{
			sb.append("<b>Example(s):</b><br>");
			sb.append(getExample()).append("<br>");
		}
		
		if ( hasUsage() )
		{
			sb.append("<b>Usage(s):</b><br>");
			sb.append(getUsage()).append("<br>");
		}
		
		if ( hasPermissions() )
		{
			sb.append("<b>Permissions:</b><br>");
			sb.append(getPermissions()).append("<br>");
		}
		
		if ( hasSeeAlso() )
		{
			sb.append("<br>");
			sb.append("<b>See Also:</b><br>");
			sb.append(getSeeAlso()).append("<br>");
		}

		if ( hasSourceUrl() )
		{
			sb.append("<br>");
			sb.append("<b>Source URL:</b> ").append("<A HREF=\"").append(getSourceUrl()).append("\">").append(getSourceUrl()).append("</A>");
		}

		if (addHtmlBeginEnd)
			sb.append("</html>");
			
		sb.append("\n");

		return sb.toString();
	}

	/**
	 * Get a XML entry of this
	 * @return
	 */
	public String toXml()
	{
		// Add entry
		StringBuilder sb = new StringBuilder();
			
		sb.append("\n");
		sb.append("    ").append(XML_BEGIN_TAG_ENTRY).append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_CMD_NAME)    .append(StringUtil.xmlSafe(getCmdName()     )).append(XML_END___SUBTAG_CMD_NAME)    .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_MODULE)      .append(StringUtil.xmlSafe(getModule()      )).append(XML_END___SUBTAG_MODULE)      .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_SECTION)     .append(StringUtil.xmlSafe(getSection()     )).append(XML_END___SUBTAG_SECTION)     .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_FROM_VERSION).append(StringUtil.xmlSafe(getFromVersion() )).append(XML_END___SUBTAG_FROM_VERSION).append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_DESCRIPTION) .append(StringUtil.xmlSafe(getDescription() )).append(XML_END___SUBTAG_DESCRIPTION) .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_SYNTAX)      .append(StringUtil.xmlSafe(getSyntax()      )).append(XML_END___SUBTAG_SYNTAX)      .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_PARAMETERS)  .append(StringUtil.xmlSafe(getParameters()  )).append(XML_END___SUBTAG_PARAMETERS)  .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_EXAMPLE)     .append(StringUtil.xmlSafe(getExample()     )).append(XML_END___SUBTAG_EXAMPLE)     .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_USAGE)       .append(StringUtil.xmlSafe(getUsage()       )).append(XML_END___SUBTAG_USAGE)       .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_PERMISSIONS) .append(StringUtil.xmlSafe(getPermissions() )).append(XML_END___SUBTAG_PARAMETERS)  .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_SEE_ALSO)    .append(StringUtil.xmlSafe(getSeeAlso()     )).append(XML_END___SUBTAG_SEE_ALSO)    .append("\n");
		sb.append("        ").append(XML_BEGIN_SUBTAG_SOURCE_URL)  .append(StringUtil.xmlSafe(getSourceUrl()   )).append(XML_END___SUBTAG_SOURCE_URL)  .append("\n");
		sb.append("    ").append(XML_END___TAG_ENTRY).append("\n");

		return sb.toString();
	}

	/**
	 * Create a Completion for this entry
	 * @param compleationProvider
	 * @return
	 */
	public Completion getCompletion(CompletionProviderAbstract compleationProvider)
	{
		String replacementText = stripPre(getSyntax());
		String shortDesc       = getDescription();
		String summary         = toHtml(true, null);
		
		Completion compl = new ToolTipSupplierCompletion(compleationProvider, replacementText, shortDesc, summary);
		return compl;
	}

	/** remove any start/ending &lt;pre&gt; html strings. */
	private String stripPre(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return "";

		String startStr = str.substring(0, Math.min(20, str.length())).trim();
		if (StringUtils.startsWithIgnoreCase(startStr, "<pre>"))
		{
			int startPos = str.indexOf('>');
			int endPos   = str.lastIndexOf('<');
			if (startPos >= 0 && endPos >= 0)
				str = str.substring(startPos+1, endPos).trim();
		}
		
		return str;
	}
}
