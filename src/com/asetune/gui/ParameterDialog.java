/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package com.asetune.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class ParameterDialog
	extends JDialog
	implements ActionListener
{
	private static final long serialVersionUID = -6264488571520005143L;

	private Map<String, String>     _inputMap       = null; 
	private Map<String, String>     _outputMap      = null; 
	private Map<String, JTextField> _componentMap   = null; 

	private JButton                 _ok             = new JButton("OK");
	private JButton                 _cancel         = new JButton("Cancel");
	private JButton                 _apply          = new JButton("Apply");
	private boolean                 _showApply      = true;

	private ParameterDialog(Window owner, String title, LinkedHashMap<String, String> input, boolean showApply)
	{
		super(owner, title);
		setModal(true);
		_showApply = showApply;
		_inputMap = input;
		initComponents();
		pack();
		
		// Focus to 'OK', escape to 'CANCEL'
		SwingUtils.installEscapeButton(this, _cancel);
		SwingUtils.setFocus(_ok);
	}


	public static Map<String, String> showParameterDialog(Window owner, String title, LinkedHashMap<String, String> input, boolean showApply)
	{
		ParameterDialog params = new ParameterDialog(owner, title, input, showApply);
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

		_componentMap = new LinkedHashMap<String, JTextField>();

		for (Map.Entry<String,String> entry : _inputMap.entrySet()) 
		{
			String key   = entry.getKey();
			String value = entry.getValue();

			JLabel     label = new JLabel(key);
			JTextField comp  = new JTextField(StringUtil.escapeControlChars(value));
			
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
		if (_showApply)
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

		_outputMap = new LinkedHashMap<String, String>();
		for (Map.Entry<String,JTextField> entry : _componentMap.entrySet()) 
		{
			String     key   = entry.getKey();
			JTextField value = entry.getValue();
			//System.out.println("key="+key+", value="+value);

			_outputMap.put(key, StringUtil.unEscapeControlChars(value.getText()));
		}
	}

	@Override
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
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			checkForChanges();
		}
	};
	private KeyListener        _keyListener  = new KeyListener()
	{
		 // Changes in the fields are visible first when the key has been released.
		@Override public void keyPressed (KeyEvent keyevent) {}
		@Override public void keyTyped   (KeyEvent keyevent) {}
		@Override public void keyReleased(KeyEvent keyevent) { checkForChanges(); }
	};

	private void checkForChanges()
	{
		boolean enabled = false;

		for (Map.Entry<String,JTextField> entry : _componentMap.entrySet()) 
		{
			String key      = entry.getKey();
			String valueNow = entry.getValue().getText();
			String valueIn  = _inputMap.get(key);

			if ( ! valueNow.equals(valueIn) )
			{
				enabled = true;
				break;
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

		LinkedHashMap<String,String> in = new LinkedHashMap<String,String>();
		in.put("Input Fileld One", "123");
		in.put("Ke theis",         "1");
		in.put("And thrre",        "nisse");
		in.put("Yupp",             "kalle");
		Map<String,String> results = showParameterDialog(null, "Test Parameters", in, true);
		
		if (results == null)
			System.out.println("Cancel");
		else
		{
			for (Map.Entry<String,String> entry : results.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();

				System.out.println("key='"+key+"', val='"+val+"'.");
			}
		}
	}
}

