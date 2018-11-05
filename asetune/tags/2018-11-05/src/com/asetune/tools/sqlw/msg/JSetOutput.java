package com.asetune.tools.sqlw.msg;

import java.util.Map;

import com.asetune.utils.StringUtil;

public class JSetOutput
extends JPlainResultSet
{
	private static final long serialVersionUID = 1L;
	private String _text = null;

	public JSetOutput(String usedCommand, Map<String, String> variableMap, String messageOutput)
	{
		init();
		
		StringBuilder sb = new StringBuilder();

		_text = messageOutput;
		
		if (variableMap == null)
		{
			//sb.append("Origin Cmd: ").append(usedCommand).append("\n");
			sb.append(messageOutput);
		}
		else
		{
			if (variableMap.isEmpty())
			{
				sb.append("WARNING:\n");
				sb.append(" - No variables has been set.\n");
				sb.append("   Set a variable with \\set varName=someValue\n");
				sb.append("   Then the variable can be referenced in the text as ${varName}\n");
			}
			else
			{
				// First get max Length of key/value
				int maxKeyLen = 0;
				int maxValLen = 0;
				for (String key : variableMap.keySet())
				{
					if (StringUtil.isNullOrBlank(key))
						continue;

					String val = variableMap.get(key);
					
					maxKeyLen = Math.max(maxKeyLen, key.length());
					maxValLen = Math.max(maxValLen, val.length());
				}

				// Print a table like the following
				// +----------------+------------------+
				// |variable        |value             |
				// +----------------+------------------+
				// |someVariableName|the assigned value|
				// +----------------+------------------+

				sb.append("List of variables:\n");
				sb.append("+" + StringUtil.replicate("-", maxKeyLen)   + "+" + StringUtil.replicate("-", maxValLen) + "+\n");
				sb.append("|" + StringUtil.left("variable", maxKeyLen) + "|" + StringUtil.left("value", maxValLen)  + "|\n");
				sb.append("+" + StringUtil.replicate("-", maxKeyLen)   + "+" + StringUtil.replicate("-", maxValLen) + "+\n");
				// Print values
				int count = 0;
				for (String key : variableMap.keySet())
				{
					count++;
					String val = variableMap.get(key);

					sb.append("|" + StringUtil.left(key, maxKeyLen) + "|" + StringUtil.left(val, maxValLen) + "|\n");
				}
				sb.append("+" + StringUtil.replicate("-", maxKeyLen) + "+" + StringUtil.replicate("-", maxValLen) + "+\n");
				sb.append(count).append(" Variables was found\n");
			}
		}

		_text = sb.toString();
		setText(_text);
	}

	@Override
	public String getText()
	{
		return _text;
	}

	@Override
	protected void init()
	{
//		if (_aseMsgFont == null)
//			_aseMsgFont = new Font("Courier", Font.PLAIN, SwingUtils.hiDpiScale(12));
//		setFont(_aseMsgFont);

		setLineWrap(true);
		setWrapStyleWord(true);
	}
}
