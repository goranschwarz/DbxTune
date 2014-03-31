/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package asemon.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

public class ParameterDialog
	extends JDialog
	implements ActionListener
{
	private static final long serialVersionUID = -6264488571520005143L;

	private Map                _inputMap       = null; 
	private Map                _outputMap      = null; 
	private Map                _componentMap   = null; 

	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");
	private JButton            _apply          = new JButton("Apply");

	private ParameterDialog(Frame owner, String title, LinkedHashMap input)
	{
		super(owner, title, true);
		_inputMap = input;
		initComponents();
		pack();
	}


	public static Map showParameterDialog(Frame owner, String title, LinkedHashMap input)
	{
		ParameterDialog params = new ParameterDialog(owner, title, input);
		params.setLocationRelativeTo(owner);
		params.setVisible(true);
		params.dispose();

		return params._outputMap;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right

		_componentMap = new LinkedHashMap();

		for (Iterator iter = _inputMap.keySet().iterator(); iter.hasNext();)
		{
			String key   = (String) iter.next();
			String value = (String) _inputMap.get(key);

			JLabel     label = new JLabel(key);
			JTextField comp  = new JTextField(value);
			
			// Listen for "return" and any other "key pressing"
			// This simply enables/disable the "apply" button if anything was changed.
			comp.addActionListener(_actionListener);
			comp.addKeyListener   (_keyListener);

			_componentMap.put(key, comp);
			
			panel.add(label, "");
			panel.add(comp,  "grow, wrap");
		}
		
		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, push");
		panel.add(_cancel, "tag cancel,                   split, bottom");
		panel.add(_apply,  "tag apply,                    split, bottom");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	
	private void apply()
	{
		_apply.setEnabled(false);

		_outputMap = new LinkedHashMap();
		for (Iterator iter = _componentMap.keySet().iterator(); iter.hasNext();)
		{
			String key   = (String) iter.next();
			Object value = (Object) _componentMap.get(key);
			
			if (value instanceof JTextField)
			{
				_outputMap.put(key, ((JTextField)value).getText());
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}
	}

	private ActionListener     _actionListener  = new ActionListener()
	{
		public void actionPerformed(ActionEvent actionevent)
		{
			checkForChanges();
		}
	};
	private KeyListener        _keyListener  = new KeyListener()
	{
		 // Changes in the fields are visible first when the key has been released.
		public void keyPressed (KeyEvent keyevent) {}
		public void keyTyped   (KeyEvent keyevent) {}
		public void keyReleased(KeyEvent keyevent) { checkForChanges(); }
	};

	private void checkForChanges()
	{
		boolean enabled = false;

		for (Iterator iter = _componentMap.keySet().iterator(); iter.hasNext();)
		{
			String key   = (String) iter.next();

			Object valueIn  = (Object) _inputMap    .get(key);
			Object valueNow = (Object) _componentMap.get(key);
			
			if (valueNow instanceof JTextField)
			{
				String strIn  = (valueIn instanceof String) ? (String) valueIn : null;
				String strNow = ((JTextField)valueNow).getText();

				//System.out.println("checkForChanges: key='"+key+"', in='"+strIn+"', now='"+strNow+"'.");

				if ( ! strNow.equals(strIn) )
				{
					enabled = true;
					break;
				}
			}
		}
		_apply.setEnabled(enabled);
	}

	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		LinkedHashMap in = new LinkedHashMap();
		in.put("Input Fileld One", "123");
		in.put("Ke theis",         "1");
		in.put("And thrre",        "nisse");
		in.put("Yupp",             "kalle");
		Map results = showParameterDialog(null, "Test Parameters", in);
		
		if (results == null)
			System.out.println("Cancel");
		else
		{
			for (Iterator iter = results.keySet().iterator(); iter.hasNext();)
	        {
		        String key = (String) iter.next();
		        String val = (String) results.get(key);
		        
		        System.out.println("key='"+key+"', val='"+val+"'.");
	        }
		}
	}
}
