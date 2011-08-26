package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class ChangeToJTabDialog extends JDialog implements ActionListener
{
	// private static Logger _logger =
	// Logger.getLogger(ChangeToJTabDialog.class);
	private static final long	serialVersionUID				= 802663031446167220L;

	// PROPERTIES
	private static final String	PROP_PREFIX						= "ChangeToJTabDialog";

	private static final String	PROP_ACTION_CHANGE				= "CHANGE";
	private static final String	PROP_ACTION_STAY				= "STAY";

	private static final String	PROP_NEXT_TIME_ALWAYS_REMEMBER	= "ALWAYS_REMEMBER";
	private static final String	PROP_NEXT_TIME_REMEMBER_SESSION	= "REMEMBER_SESSION";
	private static final String	PROP_NEXT_TIME_QUESTION			= "ASK";

	// This is a Map of <tabName> <null> which indicates if the tab has been
	// visible for this session.
	private static HashMap<String, Boolean> _hasBeenVisible     = new HashMap<String, Boolean>();

	private String				_message						= null;
	private String				_toTabName						= null;
	private JTabbedPane			_tabPane						= null;

	private JLabel				_message_lbl					= new JLabel("");

	// ACTION
	private JRadioButton		_action_change_rb				= new JRadioButton("", true);
	private JRadioButton		_action_stay_rb					= new JRadioButton();
	private ButtonGroup			_action_rbg						= new ButtonGroup();

	// NEXT TIME
	private JRadioButton		_next_alwaysRemember_rb			= new JRadioButton();
	private JRadioButton		_next_rememberSession_rb		= new JRadioButton();
	private JRadioButton		_next_alwaysAsk_rb				= new JRadioButton("", true);
	private ButtonGroup			_next_rbg						= new ButtonGroup();

	private JButton				_ok								= new JButton("OK");
	private JButton				_cancel							= new JButton("Cancel");

	// private JButton _apply = new JButton("Apply");

	ChangeToJTabDialog(Frame owner, String message, JTabbedPane tabPane, String toTabName)
	{
		super(owner, "Found Blocking Locks", false);

		_message = message;
		_tabPane = tabPane;
		_toTabName = toTabName;

		// ImageIcon icon = SwingUtils.readImageIcon(Version.class,
		// "images/sql_query_window.png");
		// ((Frame)this.getOwner()).setIconImage(icon.getImage());

		initComponents();
		loadProps();
		pack();

		setLocationRelativeTo(owner);
		setVisible(true);
	}

	/*---------------------------------------------------
	 ** BEGIN: component initialization
	 **---------------------------------------------------
	 */
	protected JPanel createActionPanel()
	{
		JPanel panel = SwingUtils.createPanel("Action", true);
		panel.setLayout(new MigLayout("insets 0", "", "")); // insets Top Left
															// Bottom Right
		_action_change_rb.setToolTipText("Set the selected active tab to '"+_toTabName+"' when this event happens.");
		_action_stay_rb  .setToolTipText("Do nothing when this event happens.");

		_action_change_rb.setText("<html>Change focus to tab '<b>" + _toTabName + "</b>'.</html>");
		_action_stay_rb.setText("<html>Stay in <b>current</b> tab, or simply, do nothing.</html>");
		_action_rbg.add(_action_change_rb);
		_action_rbg.add(_action_stay_rb);

		panel.add(_action_change_rb, "wrap 0");
		panel.add(_action_stay_rb, "wrap 0");

		return panel;
	}

	protected JPanel createNextTimePanel()
	{
		JPanel panel = SwingUtils.createPanel("Next time this happens", true);
		panel.setLayout(new MigLayout("insets 0", "", "")); // insets Top Left Bottom Right

		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String configFile = "asetune.save.properties";
		if ( tmpConf != null )
			configFile = tmpConf.getFilename();

		_next_alwaysRemember_rb .setToolTipText("<html>" +
		                                        "You will newer have to see this popup again when this event happens.<br>" +
		                                        "The selected action will always be executed without any popup question.<br>" +
		                                        "<br>" +
		                                        "Note: To reset this option you will need to edit the file '"+configFile+"'<br>and remove all entries for '"+PROP_PREFIX+"."+_toTabName+"'.</p>" +
		                                        "</html>");
		_next_rememberSession_rb.setToolTipText("So this popup wont be displayed until next time you start the application and the same event happens.");
		_next_alwaysAsk_rb      .setToolTipText("This popup will be shown every time this event happens.");

		_next_alwaysRemember_rb.setText("Remember to Always make the above choice, newer ask me again.");
		_next_rememberSession_rb.setText("Remember the above choise until the application restarts.");
		_next_alwaysAsk_rb.setText("Always show this dialog when this happens.");
		_next_rbg.add(_next_alwaysRemember_rb);
		_next_rbg.add(_next_rememberSession_rb);
		_next_rbg.add(_next_alwaysAsk_rb);

		panel.add(_next_alwaysRemember_rb, "wrap 0");
		panel.add(_next_rememberSession_rb, "wrap 0");
		panel.add(_next_alwaysAsk_rb, "wrap 0");

		return panel;
	}

	protected void initComponents()
	{
		JPanel panel = new JPanel();
		// panel.setLayout(new MigLayout("","","")); // insets Top Left Bottom
		// Right
		panel.setLayout(new MigLayout("insets 10 10", "", "")); // insets Top
																// Left Bottom
																// Right

		_message_lbl.setText("<html>" + "<h4>" + _message + "</h4>" +
		// "Do you want to change input focus to the tab <b>'"+_toTabName+"'</b>."
		// +
				"Choose an <b>Action</b> below that you find suitable." + "</html>");

		// Get the swings default question mark
//		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		Icon icon = UIManager.getIcon("OptionPane.warningIcon");


		// ADD icon and message
		panel.add(new JLabel(icon), "gapx 10, gapy 10, split");
		panel.add(_message_lbl, "gapx 15, wrap 20");

		// Add Action and NextTime panel
		panel.add(createActionPanel(), "push, grow, wrap");
		panel.add(createNextTimePanel(), "push, grow, wrap");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok, "tag ok,     gap top 20, split, bottom, right, push");
		panel.add(_cancel, "tag cancel, split, bottom");
		// panel.add(_apply, "tag apply,  bottom");

		// Initial state for buttons
		// _apply.setEnabled(false);

		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok.addActionListener(this);
		_cancel.addActionListener(this);
		// _apply .addActionListener(this);

	}

	/**
	 * Set focus to a good field or button
	 */
	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			public void run()
			{
				_ok.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/*---------------------------------------------------
	 ** END: component initialization
	 **---------------------------------------------------
	 */

	public void setVisible(boolean visible)
	{
		if ( visible == false )
		{
			super.setVisible(false);
			return;
		}
		
		// just flash the window, if it's already visible/active
		if (isVisible())
		{
			super.setVisible(true);
			return;
		}

		boolean firstTime = !_hasBeenVisible.containsKey(_toTabName);
		_hasBeenVisible.put(_toTabName, null);

		if (firstTime)
			setFocus();

		// No need to continue, we are already in the correct tab
		String currentSelectedTab = _tabPane.getTitleAt(_tabPane.getSelectedIndex());
		if ( _toTabName.equals(currentSelectedTab) )
		{
			return;
		}

		// Always do ask what we want to do
		if ( _next_alwaysAsk_rb.isSelected() )
		{
			super.setVisible(true);
			return;
		}
		// Always remember
		else if ( _next_alwaysRemember_rb.isSelected() )
		{
			if ( _action_stay_rb.isSelected() )
				return;

			if ( _action_change_rb.isSelected() )
				_tabPane.setSelectedIndex(_tabPane.indexOfTab(_toTabName));

			return;
		}
		// Remember session
		else if ( _next_rememberSession_rb.isSelected() )
		{
			if ( firstTime )
			{
				super.setVisible(true);
				return;
			}
			else
			{
				if ( _action_stay_rb.isSelected() )
					return;

				if ( _action_change_rb.isSelected() )
					_tabPane.setSelectedIndex(_tabPane.indexOfTab(_toTabName));

				return;
			}
		}

		// well if nothing in the above if statements, we will end up here
		super.setVisible(true);
	}

	private void loadProps()
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		if ( tmpConf == null )
			return;

		String propPrefix = PROP_PREFIX + "." + _toTabName + ".";

		String action = tmpConf.getProperty(propPrefix + "action", "DEFAULT");
		String nextTime = tmpConf.getProperty(propPrefix + "nextTime", "DEFAULT");

		// ACTION
		if ( action.equals(PROP_ACTION_CHANGE) )
			_action_change_rb.setSelected(true);
		else if ( action.equals(PROP_ACTION_STAY) )
			_action_stay_rb.setSelected(true);
		else
			_action_change_rb.setSelected(true);

		// NEXT TIME
		if ( nextTime.equals(PROP_NEXT_TIME_ALWAYS_REMEMBER) )
			_next_alwaysRemember_rb.setSelected(true);
		else if ( nextTime.equals(PROP_NEXT_TIME_REMEMBER_SESSION) )
			_next_rememberSession_rb.setSelected(true);
		else if ( nextTime.equals(PROP_NEXT_TIME_QUESTION) )
			_next_alwaysAsk_rb.setSelected(true);
		else
			_next_alwaysAsk_rb.setSelected(true);
	}

	private void saveProps()
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if ( tmpConf == null )
			return;

		String propPrefix = PROP_PREFIX + "." + _toTabName + ".";

		String action = "DEFAULT";
		if ( _action_change_rb.isSelected() )
			action = PROP_ACTION_CHANGE;
		if ( _action_stay_rb.isSelected() )
			action = PROP_ACTION_STAY;

		String nextTime = "DEFAULT";
		if ( _next_alwaysRemember_rb.isSelected() )
			nextTime = PROP_NEXT_TIME_ALWAYS_REMEMBER;
		if ( _next_rememberSession_rb.isSelected() )
			nextTime = PROP_NEXT_TIME_REMEMBER_SESSION;
		if ( _next_alwaysAsk_rb.isSelected() )
			nextTime = PROP_NEXT_TIME_QUESTION;

		// set props
		tmpConf.setProperty(propPrefix + "action", action);
		tmpConf.setProperty(propPrefix + "nextTime", nextTime);

		tmpConf.save();
	}

	private void apply()
	{
		// _apply.setEnabled(false);
		if ( _action_change_rb.isSelected() )
			_tabPane.setSelectedIndex(_tabPane.indexOfTab(_toTabName));

		saveProps();
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: OK ---
		if ( _ok.equals(source) )
		{
			apply();
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if ( _cancel.equals(source) )
		{
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		// if (_apply.equals(source))
		// {
		// apply();
		// }
	}

	/**
	 * Reset the saved settings for a specific tab name
	 * @param toTabName
	 */
	public static void resetSavedSettings(String toTabName)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		conf.removeAll(ChangeToJTabDialog.PROP_PREFIX+"."+toTabName+".");
		conf.save();
	}

	// --------------------------------------------------
	// TEST-CODE
	// --------------------------------------------------
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

		Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		JFrame frame = new JFrame("Test Frame")
		{
			private static final long	serialVersionUID	= 1L;

			ChangeToJTabDialog	_toTab1	= null;
			ChangeToJTabDialog	_toTab2	= null;
			ChangeToJTabDialog	_toTab3	= null;
			ChangeToJTabDialog	_toTab4	= null;
			JTabbedPane			_tab	= null;

			{
				setLayout(new BorderLayout());

				JPanel butPan = new JPanel(new MigLayout());
				JButton but;

				but = new JButton("to-Tab-1");
				but.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if ( _toTab1 == null )
							_toTab1 = new ChangeToJTabDialog(null, "Found Blocking Locks in the ASE Server", _tab, "tab1");
						_toTab1.setVisible(true);
					}
				});
				butPan.add(but);

				but = new JButton("to-Tab-2");
				but.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if ( _toTab2 == null )
							_toTab2 = new ChangeToJTabDialog(null, "Found Blocking Locks in the ASE Server", _tab, "tab2");
						_toTab2.setVisible(true);
					}
				});
				butPan.add(but);

				but = new JButton("to-Tab-3");
				but.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if ( _toTab3 == null )
							_toTab3 = new ChangeToJTabDialog(null, "Found Blocking Locks in the ASE Server", _tab, "tab3");
						_toTab3.setVisible(true);
					}
				});
				butPan.add(but);

				but = new JButton("to-Tab-4");
				but.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if ( _toTab4 == null )
							_toTab4 = new ChangeToJTabDialog(null, "Found Blocking Locks in the ASE Server", _tab, "tab4");
						_toTab4.setVisible(true);
					}
				});
				butPan.add(but);

				add(butPan, BorderLayout.NORTH);

				_tab = new JTabbedPane();
				_tab.addTab("tab1", new JLabel("tab-1"));
				_tab.addTab("tab2", new JLabel("tab-2"));
				_tab.addTab("tab3", new JLabel("tab-3"));
				_tab.addTab("tab4", new JLabel("tab-4"));

				// getContentPane().add(_tab);
				add(_tab, BorderLayout.CENTER);
				setSize(300, 300);
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		};

		frame.setVisible(true);
	}
}
