package com.asetune.tools.sqlw.msg;

public class JSshOutput
extends JPlainResultSet
{
	private static final long serialVersionUID = 1L;
	private String _text = null;

	public JSshOutput(String usedCommand, String ddlText)
	{
		init();
		
		_text = ddlText;
		setText(ddlText);
		insert("-- ssh: "+usedCommand +"\nreset\n\n", 0);
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
