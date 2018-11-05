package com.asetune.alarm.ui.view;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEvent.Category;
import com.asetune.alarm.events.AlarmEvent.ServiceState;
import com.asetune.alarm.events.AlarmEvent.Severity;
import com.asetune.alarm.events.AlarmEventDummy;
import com.asetune.gui.swing.GButton;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class DummyEventDialog
extends JDialog
//extends JFrame
implements ActionListener, FocusListener
{
	private static final long serialVersionUID = 1L;

//	private JButton    _ok             = new JButton("OK");
//	private JButton    _cancel         = new JButton("Cancel");
	private JButton    _close          = new JButton("Close");

	private JLabel               _className_lbl           = new JLabel("<html><b>Alarm Class Name</b></html>");
	private JTextField           _className_txt           = new JTextField();

	private JLabel               _serviceType_lbl         = new JLabel("<html><b>Service Type</b></html>");
	private JTextField           _serviceType_txt         = new JTextField();

	private JLabel               _serviceName_lbl         = new JLabel("<html><b>Service Name</b></html>");
	private JTextField           _serviceName_txt         = new JTextField();

	private JLabel               _serviceInfo_lbl         = new JLabel("<html><b>Service Info</b></html>");
	private JTextField           _serviceInfo_txt         = new JTextField();

	private JLabel               _extraInfo_lbl           = new JLabel("<html><b>Extra Info</b></html>");
	private JTextField           _extraInfo_txt           = new JTextField();

	private JLabel               _category_lbl            = new JLabel("<html><b>Category</b></html>");
	private JComboBox<AlarmEvent.Category> _category_cbx  = new JComboBox<>();

	private JLabel               _severity_lbl            = new JLabel("<html><b>Severity</b></html>");
	private JComboBox<AlarmEvent.Severity> _severity_cbx  = new JComboBox<>();

	private JLabel               _state_lbl               = new JLabel("<html><b>State</b></html>");
	private JComboBox<AlarmEvent.ServiceState> _state_cbx = new JComboBox<>();

	private JLabel               _timeToLive_lbl          = new JLabel("Time To Live");
	private JTextField           _timeToLive_txt          = new JTextField();

	private JLabel               _raiseDelay_lbl          = new JLabel("Raise Delay");
	private JTextField           _raiseDelay_txt          = new JTextField();

	private JLabel               _data_lbl                = new JLabel("Data");
	private JTextField           _data_txt                = new JTextField();

	private JLabel               _description_lbl         = new JLabel("Description");
	private JTextField           _description_txt         = new JTextField();

	private JLabel               _extendedDesc_lbl        = new JLabel("Extended Description");
	private JTextField           _extendedDesc_txt        = new JTextField();

	private GButton              _setEvent_but            = new GButton("Set Alarm");
	private GButton              _sendEvent_but           = new GButton("Send Alarm");
	private GButton              _sendEos_but             = new GButton("Send End-Of-Scan");

	private AlarmEventSetCallback  _callback = null;

	public interface AlarmEventSetCallback
	{
		public void setAlarmEvent(AlarmEvent alarmEvent);
	}

	private DummyEventDialog(Window owner, AlarmEventSetCallback callback)
	{
//		super();
		super(owner, "Add Dummy Alarm Event", ModalityType.MODELESS);
		_callback = callback;
		initComponents();
		pack();
	}

	public static void showDialog(Window owner)
	{
		showDialog(owner, null);
	}

	public static void showDialog(Window owner, AlarmEventSetCallback callback)
	{
		DummyEventDialog dialog = new DummyEventDialog(owner, callback);
		dialog.setLocationRelativeTo(owner);
		dialog.setFocus();
		dialog.setVisible(true);
//		dialog.dispose();

		return;
	}

	protected void initComponents()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20"));   // insets Top Left Bottom Right

		setTitle("Add Dummy Alarm Event");

		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/alarm_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/alarm_view_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

		_className_lbl  .setToolTipText("<html></html>");
		_className_txt  .setToolTipText(_className_lbl.getToolTipText());

		_description_lbl.setToolTipText("<html></html>");
		_description_txt.setToolTipText(_description_lbl.getToolTipText());

		
		_setEvent_but.setToolTipText("Set the AlarmEvent in the Dialog that called this dialog");

		_sendEvent_but.setToolTipText("<html>"
				+ "Create and Send an Alarm event<br>"
				+ "If you send several <i>Alarm Events</i> before you press <i>EndOfScan</i> they will be filtered out or marked as <i>RE-RAISED</i><br>"
				+ "So <i>EndOfScan</i> has to be pressed to <i>end</i> a <i>check loop</i>.<br>"
				+ "At <i>EndOfScan</i> cancelation(s) will be propegated.<br>"
				+ "Also see: button <i>EndOfScan</i> for more help."
				+ "</html>");

		_sendEos_but  .setToolTipText("<html>"
				+ "Create and Send an <b>EndOfScan</b> event.<br>"
				+ "- EndOfScan is send <i>at-the-end</i> of every check loop, when all CM's has been sampled.<br>"
				+ "This is where we can determen if we need or can Cancel any Alarms.<br>"
				+ "An Alarm is Canceled if the Alarm has <b>not</b> been repeated in two samples (no overlapp), <br>"
				+ "or when the <i>Time To Live</i> property has been exhausted <br>"
				+ "(Time To Live is set by CM's that has enabled the <i>postpone</i> refresh option, which means that the CM wont be refreshed until the postpone time expires)."
				+ "</html>");

		_sendEvent_but.setUseFocusableTips(true);
		_sendEvent_but.setUseFocusableTipsSize(0);
		_sendEos_but.setUseFocusableTips(true);
		_sendEos_but.setUseFocusableTipsSize(0);
		
		
		_className_txt  .setEditable(false);
		_serviceType_txt.setEditable(false);


//		String desc = "<html>The <b>bold</b> fields are what uniquely identifies an Alarm.</html>";
		String desc = "The bold fields are what uniquely identifies an Alarm.";
		
		panel.add(new JLabel(desc),   "span, wrap 20");

		panel.add(_className_lbl,     "");
		panel.add(_className_txt,     "pushx, growx, wrap");

		panel.add(_serviceType_lbl,   "");
		panel.add(_serviceType_txt,   "pushx, growx, wrap");

		panel.add(_serviceName_lbl,   "");
		panel.add(_serviceName_txt,   "pushx, growx, wrap");

		panel.add(_serviceInfo_lbl,   "");
		panel.add(_serviceInfo_txt,   "pushx, growx, wrap");

		panel.add(_extraInfo_lbl,     "");
		panel.add(_extraInfo_txt,     "pushx, growx, wrap");

		panel.add(_category_lbl,      "");
		panel.add(_category_cbx,      "pushx, growx, wrap");

		panel.add(_severity_lbl,      "");
		panel.add(_severity_cbx,      "pushx, growx, wrap");

		panel.add(_state_lbl,         "");
		panel.add(_state_cbx,         "pushx, growx, wrap 20");

		panel.add(_timeToLive_lbl,    "");
		panel.add(_timeToLive_txt,    "pushx, growx, wrap");

		panel.add(_raiseDelay_lbl,    "");
		panel.add(_raiseDelay_txt,    "pushx, growx, wrap");

		panel.add(_data_lbl,          "");
		panel.add(_data_txt,          "pushx, growx, wrap");

		panel.add(_description_lbl,   "");
		panel.add(_description_txt,   "pushx, growx, wrap");

		panel.add(_extendedDesc_lbl,  "");
		panel.add(_extendedDesc_txt,  "pushx, growx, wrap");

		panel.add(_setEvent_but,      "gap top 20, skip, split, hidemode 3");
		panel.add(_sendEvent_but,     "gap top 20, skip, split, hidemode 3");
		panel.add(new JLabel(),       "growx, pushx");
		panel.add(_sendEos_but,       "wrap, hidemode 3");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_close,             "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_ok,                "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_cancel,            "tag cancel,                   split, bottom");

		setContentPane(panel);

		// Fill in some start values
		_className_txt  .setText("AlarmEventDummy");
		_serviceType_txt.setText(Version.getAppName());
		
		_category_cbx.addItem(Category.OTHER);
		_category_cbx.addItem(Category.CPU);
		_category_cbx.addItem(Category.DOWN);
//		_category_cbx.addItem(Category.INTERNAL);
		_category_cbx.addItem(Category.LOCK);
		_category_cbx.addItem(Category.SPACE);
		_category_cbx.addItem(Category.SRV_CONFIG);

		_severity_cbx.addItem(Severity.INFO);
		_severity_cbx.addItem(Severity.WARNING);
		_severity_cbx.addItem(Severity.ERROR);

		_state_cbx.addItem(ServiceState.UP);
		_state_cbx.addItem(ServiceState.AFFECTED);
		_state_cbx.addItem(ServiceState.DOWN);
		
		_serviceName_txt  .setText(StringUtil.getHostname());
		_serviceInfo_txt  .setText("CmDummy");
		_timeToLive_txt   .setText("-1");
		_raiseDelay_txt   .setText("0");
		_description_txt  .setText("A Dummy Description");

		// ADD KEY listeners

		// ADD ACTIONS TO COMPONENTS
		_setEvent_but     .addActionListener(this);
		_sendEvent_but    .addActionListener(this);
		_sendEos_but      .addActionListener(this);

		_close            .addActionListener(this);
//		_ok               .addActionListener(this);
//		_cancel           .addActionListener(this);

		// ADD Focus Listeners
		_className_txt    .addFocusListener(this);
		_serviceType_txt  .addFocusListener(this);
		_serviceName_txt  .addFocusListener(this);
		_serviceInfo_txt  .addFocusListener(this);
		_extraInfo_txt    .addFocusListener(this);
		_timeToLive_txt   .addFocusListener(this);
		_raiseDelay_txt   .addFocusListener(this);
		_data_txt         .addFocusListener(this);
		_description_txt  .addFocusListener(this);
		_extendedDesc_txt .addFocusListener(this);
		
		// what should be visible...
		_setEvent_but .setVisible(_callback != null);
		_sendEvent_but.setVisible(_callback == null);
		_sendEos_but  .setVisible(_callback == null);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: Close ---
		if (_close.equals(source))
		{
			setVisible(false);
		}

//		// --- BUTTON: OK ---
//		if (_ok.equals(source))
//		{
//			setVisible(false);
//		}

//		// --- BUTTON: CANCEL ---
//		if (_cancel.equals(source))
//		{
//			setVisible(false);
//		}

		// --- BUTTON: Set Event ---
		if (_setEvent_but.equals(source))
		{
			String serviceName                   = _serviceName_txt .getText();
			String serviceInfo                   = _serviceInfo_txt .getText();
			String extraInfo                     = _extraInfo_txt   .getText();
			AlarmEvent.Category     category     = (AlarmEvent.Category)    _category_cbx.getSelectedItem();
			AlarmEvent.Severity     severity     = (AlarmEvent.Severity)    _severity_cbx.getSelectedItem();
			AlarmEvent.ServiceState state        = (AlarmEvent.ServiceState)_state_cbx   .getSelectedItem();
			int    timeToLive                    = StringUtil.parseInt(_timeToLive_txt.getText(), -1);
			int    raiseDelay                    = StringUtil.parseInt(_raiseDelay_txt.getText(), -1);
			String data                          = _data_txt        .getText();
			String description                   = _description_txt .getText();
			String extendedDesc                  = _extendedDesc_txt.getText();

			AlarmEventDummy alarmEvent = new AlarmEventDummy(serviceName, serviceInfo, extraInfo, category, severity, state, timeToLive, data, description, extendedDesc);

			if (raiseDelay > 0)
				alarmEvent.setRaiseDelayInSec(raiseDelay);
			
			_callback.setAlarmEvent(alarmEvent);
		}

		// --- BUTTON: Send Event ---
		if (_sendEvent_but.equals(source))
		{
			String serviceName                   = _serviceName_txt .getText();
			String serviceInfo                   = _serviceInfo_txt .getText();
			String extraInfo                     = _extraInfo_txt   .getText();
			AlarmEvent.Category     category     = (AlarmEvent.Category)    _category_cbx.getSelectedItem();
			AlarmEvent.Severity     severity     = (AlarmEvent.Severity)    _severity_cbx.getSelectedItem();
			AlarmEvent.ServiceState state        = (AlarmEvent.ServiceState)_state_cbx   .getSelectedItem();
			int    timeToLive                    = StringUtil.parseInt(_timeToLive_txt.getText(), -1);
			int    raiseDelay                    = StringUtil.parseInt(_raiseDelay_txt.getText(), -1);
			String data                          = _data_txt        .getText();
			String description                   = _description_txt .getText();
			String extendedDesc                  = _extendedDesc_txt.getText();

			AlarmEventDummy alarmEvent = new AlarmEventDummy(serviceName, serviceInfo, extraInfo, category, severity, state, timeToLive, data, description, extendedDesc);

			if (raiseDelay > 0)
				alarmEvent.setRaiseDelayInSec(raiseDelay);
			
//			AlarmHandler.getInstance().addAlarmToQueue(alarmEvent);
			AlarmHandler.getInstance().addAlarm(alarmEvent);
		}

		// --- BUTTON: Send EOS ---
		if (_sendEos_but.equals(source))
		{
//			AlarmHandler.getInstance().addEndOfScanToQueue();
			AlarmHandler.getInstance().endOfScan();
		}
//System.out.println("DummyEventDialog: actionPerformed: source="+source);
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		Object source = null;
		if (e != null)
			source = e.getSource();

		if (_className_txt.equals(source) || source == null)
		{
		}

		if (_timeToLive_txt.equals(source))
		{
			// Check if it's a number
		}
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
			@Override
			public void run()
			{
				_description_txt.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}
}
