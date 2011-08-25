/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.swing;


//import java.awt.SystemColor;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class MultiLineLabel
    extends JTextArea
{

    private static final long serialVersionUID = 1L;
	protected static final JLabel	A_LABEL	= new JLabel();

	public MultiLineLabel()
	{
		_init();
	}

	public MultiLineLabel(final String s)
	{
		super(s);
		_init();
	}

	private void _init()
	{
		super.setEnabled(true);
		super.setEditable(false);
		setFont(UIManager.getFont("Label.font"));
		setLineWrap(true);
		setWrapStyleWord(true);
		setBackground(A_LABEL.getBackground());
		setForeground(A_LABEL.getForeground());

		//		setWrapStyleWord(!ASEUtils.getBundleMgr().isSupportedAsianLanguage());
//		if (ASEUtils.OS_MAC_OS_X)
//		{
//			if (ASEUtils.JAVA_13)
//				setBackground(UIManager.getColor("window"));
//			else
//				setBackground(SystemColor.control);
//		}
//		else
//		{
//			setBackground(A_LABEL.getBackground());
//			setForeground(A_LABEL.getForeground());
//		}
	}

	public boolean isFocusable()
	{
		return false;
	}

	public boolean isRequestFocusEnabled()
	{
		return false;
	}
}
