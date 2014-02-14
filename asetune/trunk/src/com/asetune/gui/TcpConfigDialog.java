package com.asetune.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.CounterController;
import com.asetune.GetCounters;
import com.asetune.GetCountersGui;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class TcpConfigDialog
extends JDialog
implements ActionListener, TableModelListener
{
	private static Logger _logger = Logger.getLogger(OfflineSessionVeiwer.class);
	private static final long	serialVersionUID	= -8717629657711689568L;

//	private Frame                  _owner           = null;

	private JLabel    _templateLoad_lbl = new JLabel("Available Templates");
	private JComboBox _templateLoad_cbx = new JComboBox();
	private JButton   _templateSave_but = new JButton("Save...");
	private JButton   _templateRm_but   = new JButton("Remove...");

	private LocalTable             _table           = null;

	// PANEL: OK-CANCEL
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");
	private JButton                _apply           = new JButton("Apply");
	private boolean                _madeChanges     = false;
	
	private static final String DIALOG_TITLE = "Settings for All Collector Tabs";

	/** Save initial table look, so we can check if it has changed enable/disable the 'Apply' button */
	private PropPropEntry _initialPpe = null;

	/** keep a PPE representation of the templates, so we can check if current Table reflects any template, and so we can set the Combobox to that name...*/
	private LinkedHashMap<String, PropPropEntry> _templatePpeMap = new LinkedHashMap<String, PropPropEntry>();
	
	private static final String NO_TEMPLATE_IS_SELECTED = "<unknown template>";
	private String  _lastChoosenTemplate   = "";
	private boolean _isLoadingFromTemplate = false;

	private String[]  _templateSystemArr = new String[] {
			"System Template - PCS ON - small", 
            "System Template - PCS ON - medium", 
            "System Template - PCS ON - large", 
            "System Template - PCS ON - all",
            "System Template - PCS OFF - <for all types>" 
//            "System Template - PCS OFF - small" 
//            "System Template - PCS OFF - medium", 
//            "System Template - PCS OFF - large", 
//            "System Template - PCS OFF - all"
            };
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	private TcpConfigDialog(Frame owner)
	{
		super(owner, DIALOG_TITLE, true);
//		_owner           = owner;

		initComponents();
	}
	private TcpConfigDialog(Dialog owner)
	{
		super(owner, DIALOG_TITLE, true);
//		_owner           = owner;

		initComponents();
	}

	public static boolean showDialog(Frame owner)
	{
		TcpConfigDialog params = new TcpConfigDialog(owner);
		params.setLocationRelativeTo(owner);
		params.setVisible(true);
		params.dispose();
		return params._madeChanges;
	}
	public static boolean showDialog(Dialog owner)
	{
		TcpConfigDialog params = new TcpConfigDialog(owner);
		params.setLocationRelativeTo(owner);
		params.setVisible(true);
		params.dispose();
		return params._madeChanges;
	}

	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents()
	{
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle(DIALOG_TITLE);

		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		panel.add(createTopPanel(),      "grow, push");
		panel.add(createTablePanel(),    "grow, push, height 100%");
		panel.add(createOkCancelPanel(), "bottom, right, push");

		loadProps();

		setContentPane(panel);

		initComponentActions();
		
		_initialPpe = getPpeFromTable();

		setReflectedTemplateName();
		setFocus(_cancel);
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Templates", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
		panel.setLayout(new MigLayout("", "", ""));

		panel.setToolTipText("<html>" +
				"Templates will help you to set/restore values in the below table...<br>" +
				"<UL>" +
				"<li> To <b>Load</b> a template, just choose one from the drop down list. </li>" +
				"<li> To <b>Save</b> current settings as a template, just press the 'Save..' button and choose a name to save as. </li>" +
				"<li> To <b>Remove</b> a template, just press the 'Remove..' button and button and choose a template name to deleted. </li>" +
				"</UL>" +
				"If all selected values in the table is matching a template, that template name will be displayed in the drop down list.<br>" +
				"<br>" +
				"<b>Note:</b> User Defined Counters is <b>not</b> saved/restored in the templates, only <i>System Performance Counters</i><br>" +
				"</html>");

		String tooltip = "<html>" +
				"Load values into the below table from any of the templates in the drop down list.<br>" +
				"<UL>" +
				"<li> If all selected values in the table is matching a template, that template name will be displayed in the drop down list.</li>" +
				"<li> If template name is '"+NO_TEMPLATE_IS_SELECTED+"', it means that the current selection can't be found within any of the templates.</li>" +
				"</UL>" +
				"</html>";
		_templateLoad_lbl.setToolTipText(tooltip);
		_templateLoad_cbx.setToolTipText(tooltip);
		
		tooltip = "<html>" +
				"Save values from the below table as a template, which can be used/loaded later.<br>" +
				"A popup will be displayed, where you can give the name to save as.<br>" +
				"Note: Reusing any of the 'system' template names is not allowed." +
				"</html>";
		_templateSave_but.setToolTipText(tooltip);
		
		tooltip = "<html>" +
				"A popup will be displayed, where you can choose the template you want to delete.<br>" +
				"Note: Deleting 'system' templates is not allowed.</html>";
		_templateRm_but.setToolTipText(tooltip);
		
		_templateLoad_cbx.setEditable(false);
//		_templateLoad_cbx.setEditable(true);

		// set auto completion
		AutoCompleteDecorator.decorate(_templateLoad_cbx);

		panel.add(_templateLoad_lbl, "");
		panel.add(_templateLoad_cbx, "growx, pushx");
		panel.add(_templateSave_but, "");
		panel.add(_templateRm_but,   "wrap");

		// Load user templates.
		loadSystemAndUserDefinedTemplateNames();

		_templateLoad_cbx.addActionListener(this);
		_templateSave_but.addActionListener(this);
		_templateRm_but  .addActionListener(this);

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");

		_apply.setEnabled(false);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

		return panel;
	}

	private JPanel createTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_table = new LocalTable();
		_table.getModel().addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		JScrollPane scroll = new JScrollPane(_table);
		panel.add(scroll, "push, grow, height 100%, wrap");

		return panel;
	}

	private void initComponentActions()
	{
		//---- Top PANEL -----

		//---- Tab PANEL -----

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			doApply();
			saveProps();
		}

		// --- COMBO BOX: LOAD ---
		if (_templateLoad_cbx.equals(source))
		{
			String templateName = _templateLoad_cbx.getSelectedItem().toString().trim();
			//boolean isSystemTemplate = templateName.indexOf("System Template") >= 0;
			//_templateSave_but.setEnabled( ! isSystemTemplate );
			//_templateRm_but.setEnabled( ! isSystemTemplate );

			//System.out.println("actionPerformed() _templateLoad_cbx: name='"+templateName+"'.");

			// Set isLoading.... so that the dataChanged is NOT kicked off
			_isLoadingFromTemplate = true;

			if (loadTemplate( templateName ))
				setReflectedTemplateName();

			_isLoadingFromTemplate = false;
		}

		// --- BUTTON: SAVE... ---
		if (_templateSave_but.equals(source))
		{
			String currentTemplateName = _templateLoad_cbx.getSelectedItem().toString().trim();
			if ( currentTemplateName.equals(NO_TEMPLATE_IS_SELECTED))
				currentTemplateName = _lastChoosenTemplate;

			// Open save dialog
			String newName = SaveOrRemoveTemplateDialog.showDialog(this, 
					SaveOrRemoveTemplateDialog.SAVE_AS_DIALOG,
					currentTemplateName, getUserDefinedTemplateNames());
			if (newName != null)
			{
				String currentSelectionExistsAsName = currentSelectionIsRefectedInTemplate();
				if (    currentSelectionExistsAsName != null 
				     && ! newName.equals(currentSelectionExistsAsName) 
				     && ! currentTemplateName.equals(newName) 
				   )
				{
					int res = JOptionPane.showConfirmDialog(this,
							"Current selection is already saved as the template '"+currentSelectionExistsAsName+"'.\n" +
							    "Do you still want to save it as '"+newName+"'.\n" +
							    "\n" +
							    "Note: It could be difficult to access the template from the drop down list.\n" +
							    "\n" +
							    "A better way to do this is:\n" +
							    "- First make the selection you intend to do\n" +
							    "- Then press 'Save...' and choose a new name for the template :)", 
							"Template Duplicate Detected",
							JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.NO_OPTION)
						return;
				}
				if (saveTemplate(newName))
				{
					if ( ! currentTemplateName.equals(newName) )
					{
						// Add it att the begining of the list.
						_templateLoad_cbx.insertItemAt(newName, 1);
						_templateLoad_cbx.setSelectedItem(newName);
					}
					setReflectedTemplateName();
				}
			}
		}

		// --- BUTTON: REMOVE... ---
		if (_templateRm_but.equals(source))
		{
			String currentTemplateName = _templateLoad_cbx.getSelectedItem().toString().trim();
			boolean isSystemTemplate = currentTemplateName.indexOf("System Template") >= 0;

			if ( isSystemTemplate )
			{
				SwingUtils.showErrorMessage(this, "Can't remove template", "Can't remove a System Template.", null);
				return;
			}

			// Open remove dialog
			String name = SaveOrRemoveTemplateDialog.showDialog(this, 
					SaveOrRemoveTemplateDialog.REMOVE_DIALOG,
					currentTemplateName, getUserDefinedTemplateNames());
			if (name != null)
			{
				if (removeTemplate(name))
				{
					_templateLoad_cbx.removeItem(name);
					setReflectedTemplateName();
				}
			}
		}
    }

	@Override
	public void tableChanged(TableModelEvent e)
	{
//		System.out.println("tableChanged(): TableModelEvent="+e);
//		_apply.setEnabled(true);
		if ( ! _isLoadingFromTemplate )
			setReflectedTemplateName();
	}
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	/** Should be called when the table selection has changed in some way */
	private void setReflectedTemplateName()
	{
		PropPropEntry ppeNow = getPpeFromTable();

		String foundInTemplate = currentSelectionIsRefectedInTemplate();
		if ( foundInTemplate != null)
		{
			_templateLoad_cbx.setSelectedItem(foundInTemplate);
		}
		else
		{
			// Save last known template, so it can be used when button 'Save...' is pressed
			if ( ! NO_TEMPLATE_IS_SELECTED.equals(_templateLoad_cbx.getSelectedItem()))
				_lastChoosenTemplate = _templateLoad_cbx.getSelectedItem().toString();

			_templateLoad_cbx.setSelectedItem(NO_TEMPLATE_IS_SELECTED);
		}

		// If current settings has changed since we entered the dialog, enable the 'Apply' button
//		boolean hasChanged = ! ppeNow.toString().equals(_initialPpe.toString());
		boolean hasChanged = ! ppeNow.equals(_initialPpe);
		_apply.setEnabled(hasChanged);
	}
	
	/**
	 * @return A template that the currect selection reflects<br>
	 *         NULL if none is reflected.
	 */
	private String currentSelectionIsRefectedInTemplate()
	{
		PropPropEntry ppeNow = getPpeFromTable();
//System.out.println("===========================================================");
//System.out.println("===========================================================");
//System.out.println("===========================================================");
//System.out.println("getPpeFromTable()\n"+ppeNow.toString(25, 6));

		// Check if current settings is in one of the templates,
		for (Map.Entry<String,PropPropEntry> entry : _templatePpeMap.entrySet()) 
		{
			String        tname = entry.getKey();
			PropPropEntry ppe   = entry.getValue();
//System.out.println("_templatePpeMap.name='"+tname+"'\n"+ppe.toString(25, 6));

//			if ( ppeNow.toString().equals(ppe.toString()) )
			if ( ppeNow.equals(ppe) )
				return tname;
		}
		return null;
	}

	private void doApply()
	{
//		TableModel tm = _table.getModel();
		LocalTableModel tm = (LocalTableModel)_table.getModel();

		for (int r=0; r<tm.getRowCount(); r++)
		{
			String  tabName      = (String)  tm.getValueAt(r, TAB_POS_TAB_NAME);

			int     queryTimeout = ((Integer) tm.getValueAt(r, TAB_POS_QUERY_TIMEOUT)).intValue();
			int     postpone     = ((Integer) tm.getValueAt(r, TAB_POS_POSTPONE)).intValue();
			boolean paused       = ((Boolean) tm.getValueAt(r, TAB_POS_PAUSED)).booleanValue();
			boolean bgPoll       = ((Boolean) tm.getValueAt(r, TAB_POS_BG)).booleanValue();
			boolean rnc20        = ((Boolean) tm.getValueAt(r, TAB_POS_RNC20)).booleanValue();

			boolean storePcs     = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_PCS)).booleanValue();
			boolean storeAbs     = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_ABS)).booleanValue();
			boolean storeDiff    = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_DIFF)).booleanValue();
			boolean storeRate    = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_RATE)).booleanValue();

			CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
			
			if (cm == null)
			{
				_logger.warn("The cm named '"+tabName+"' can't be found in the 'GetCounters' object.");
				continue;
			}

			if (_logger.isDebugEnabled())
			{
				String debugStr = "doApply() name="+StringUtil.left("'"+tabName+"'", 30) +
					" "+CounterSetTemplates.PROPKEY_queryTimeout+"="+(tm.isCellChanged(r, TAB_POS_QUERY_TIMEOUT) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_postpone    +"="+(tm.isCellChanged(r, TAB_POS_POSTPONE     ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_paused      +"="+(tm.isCellChanged(r, TAB_POS_PAUSED       ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_bg          +"="+(tm.isCellChanged(r, TAB_POS_BG           ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_resetNC20   +"="+(tm.isCellChanged(r, TAB_POS_RNC20        ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_storePcs    +"="+(tm.isCellChanged(r, TAB_POS_STORE_PCS    ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_pcsAbs      +"="+(tm.isCellChanged(r, TAB_POS_STORE_ABS    ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_pcsDiff     +"="+(tm.isCellChanged(r, TAB_POS_STORE_DIFF   ) ? "X":" ") +
					" "+CounterSetTemplates.PROPKEY_pcsRate     +"="+(tm.isCellChanged(r, TAB_POS_STORE_RATE   ) ? "X":" ");
				_logger.debug(debugStr);
			}

			if (tm.isCellChanged(r, TAB_POS_QUERY_TIMEOUT)) cm.setQueryTimeout(                 queryTimeout, true);
			if (tm.isCellChanged(r, TAB_POS_POSTPONE     )) cm.setPostponeTime(                 postpone,   true);
			if (tm.isCellChanged(r, TAB_POS_PAUSED       )) cm.setPauseDataPolling(             paused,     true);
			if (tm.isCellChanged(r, TAB_POS_BG           )) cm.setBackgroundDataPollingEnabled( bgPoll,     true);
			if (tm.isCellChanged(r, TAB_POS_RNC20        )) cm.setNegativeDiffCountersToZero(   rnc20,      true);

			if (tm.isCellChanged(r, TAB_POS_STORE_PCS    )) cm.setPersistCounters(    storePcs,  true);
			if (tm.isCellChanged(r, TAB_POS_STORE_ABS    )) cm.setPersistCountersAbs( storeAbs,  true);
			if (tm.isCellChanged(r, TAB_POS_STORE_DIFF   )) cm.setPersistCountersDiff(storeDiff, true);
			if (tm.isCellChanged(r, TAB_POS_STORE_RATE   )) cm.setPersistCountersRate(storeRate, true);
		}

		_madeChanges = true;

		_table.resetCellChanges();
		_apply.setEnabled(false);
	}

	private void setFocus(final JComponent comp)
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				comp.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "tcpConfigDialog.";

		if (tmpConf != null)
		{
			tmpConf.setProperty(base + "window.width", this.getSize().width);
			tmpConf.setProperty(base + "window.height", this.getSize().height);
			tmpConf.setProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 820;  // initial window with   if not opened before
		int     height    = 630;  // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = "tcpConfigDialog.";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getIntProperty(base + "window.width",  width);
		height = tmpConf.getIntProperty(base + "window.height", height);
		x      = tmpConf.getIntProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getIntProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/


  	/*---------------------------------------------------
	** BEGIN: Template handling
	**---------------------------------------------------
	*/
	private List<String> getUserDefinedTemplateNames()
	{
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0; i<_templateLoad_cbx.getItemCount(); i++)
		{
			String tname = _templateLoad_cbx.getItemAt(i).toString();
			if ( ! (tname.startsWith(NO_TEMPLATE_IS_SELECTED) || tname.startsWith("System Template")) )
				list.add(tname);
		}
		return list;
	}

	private void loadSystemAndUserDefinedTemplateNames()
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		if (tmpConf == null)
			return;

		_templateLoad_cbx.addItem(NO_TEMPLATE_IS_SELECTED);

		// Load User Defined Templates
		String propKey = "tcpConfigDialog.template.name.";
		List<String> userTemplates = tmpConf.getKeys(propKey);
		Collections.sort(userTemplates);
		for (String key : tmpConf.getKeys(propKey))
		{
			String userTemplate = key.substring(propKey.length());
			//System.out.println(name);
			_templateLoad_cbx.addItem(userTemplate);

			// Add info to _templatePpeMap
			String propVal = tmpConf.getProperty(key);
			PropPropEntry ppe = new PropPropEntry(propVal);
			_templatePpeMap.put(userTemplate, ppe);
		}

		// Add a separator between User and System templates
		//_templateLoad_cbx.addItem(new JSeparator()); // this did NOT work...

		// Load System Templates
		// maybe add a separator, before System Templates ???
		for (String systemTemplate : _templateSystemArr)
		{
			_templateLoad_cbx.addItem(systemTemplate);

			// Add info to _templatePpeMap
			String[] sa = systemTemplate.split(" - ");
			String type = sa[1];
			String name = sa[2];

			if ("PCS ON".equals(type))
			{
				if      ("small" .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL);
				else if ("medium".equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM);
				else if ("large" .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE);
				else if ("all"   .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL);
			}
			else if ("PCS OFF".equals(type))
			{
				if      ("small" .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_SMALL);
				else if ("medium".equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_MEDIUM);
				else if ("large" .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_LARGE);
				else if ("all"   .equals(name)) _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_ALL);
				else                            _templatePpeMap.put(systemTemplate, CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_SMALL);
			}
		}
	}

	private boolean loadTemplate(String templateName)
	{
		if (templateName == null) return false;
		if (templateName.trim().equals("")) return false;
		if (templateName.trim().equals(NO_TEMPLATE_IS_SELECTED)) return false;
		
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		if (tmpConf == null)
			return false;

		PropPropEntry ppe = null;
		if (templateName.startsWith("System Template"))
		{
			String[] sa = templateName.split(" - ");
			String type = sa[1];
			String name = sa[2];

			if ("PCS ON".equals(type))
			{
				if      ("small" .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL;
				else if ("medium".equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM;
				else if ("large" .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE;
				else if ("all"   .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL;
			}
			else if ("PCS OFF".equals(type))
			{
				if      ("small" .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_SMALL;
				else if ("medium".equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_MEDIUM;
				else if ("large" .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_LARGE;
				else if ("all"   .equals(name)) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_ALL;
				else                            ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_OFF_SMALL;
			}
			if (ppe == null)
				throw new RuntimeException("Can't find the SYSTEM TEMPLATE named '"+templateName+"'.");
		}
		else
		{
			String propKey = "tcpConfigDialog.template.name."+templateName;
			String propVal = tmpConf.getProperty(propKey);
			if (propVal == null)
			{
				_logger.warn("The key '"+propKey+"', can't be found in the file '"+tmpConf.getFilename()+"'.");
				SwingUtils.showErrorMessage(this, "Error loading Template", 
						"Can't load the Template Named '"+templateName+"'\n\n" +
						"The key '"+propKey+"', can't be found in the file '"+tmpConf.getFilename()+"'.", null);
				return false;
			}
			ppe = new PropPropEntry(propVal);
		}
//		if (ppe == null)
//			return false;

		// LOAD foreach of the tabs
		for (String name : ppe)
		{
//System.out.println("PPE: for name '"+name+"'.");
			// FIXME: maybe use cm.getDefaultXXX() to be able to load "incomplete/old" template files.
			//        also write the corrected/completed PPE to disk...
			try
			{
				int     queryTimeout = ppe.getIntMandatoryProperty(    name, CounterSetTemplates.PROPKEY_queryTimeout);
				int     postpone     = ppe.getIntMandatoryProperty(    name, CounterSetTemplates.PROPKEY_postpone);
				boolean paused       = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_paused);
				boolean bg           = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_bg);
				boolean resetNC20    = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_resetNC20);
				boolean storePcs     = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_storePcs);
				boolean pcsAbs       = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_pcsAbs);
				boolean pcsDiff      = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_pcsDiff);
				boolean pcsRate      = ppe.getBooleanMandatoryProperty(name, CounterSetTemplates.PROPKEY_pcsRate);
				TableModel tm = _table.getModel();
				for (int r=0; r<tm.getRowCount(); r++)
				{
					String tabName = tm.getValueAt(r, TAB_POS_TAB_NAME).toString();
					if (tabName.equals(name))
					{
//System.out.println("PPE: Setting values for tab '"+tabName+"'.");
						tm.setValueAt(queryTimeout, r, TAB_POS_QUERY_TIMEOUT);
						tm.setValueAt(postpone,     r, TAB_POS_POSTPONE);
						tm.setValueAt(paused,       r, TAB_POS_PAUSED);
						tm.setValueAt(bg,           r, TAB_POS_BG);
						tm.setValueAt(resetNC20,    r, TAB_POS_RNC20);
						tm.setValueAt(storePcs,     r, TAB_POS_STORE_PCS);
						tm.setValueAt(pcsAbs,       r, TAB_POS_STORE_ABS);
						tm.setValueAt(pcsDiff,      r, TAB_POS_STORE_DIFF);
						tm.setValueAt(pcsRate,      r, TAB_POS_STORE_RATE);
					}	
				}
			}
			catch (Exception e) 
			{
				_logger.error("Problem when loading Template '"+templateName+"', for tab '"+name+"', caught: "+e.getMessage());
				return false;
			}
		}
		
		return true;
	}

	private PropPropEntry getPpeFromTable()
	{
		TableModel tm = _table.getModel();

		PropPropEntry ppe = new PropPropEntry();
		for (int r=0; r<_table.getRowCount(); r++)
		{
			String tabName = tm.getValueAt(r, TAB_POS_TAB_NAME).toString();

			// Do not generate UDC User Defined Counters
//			CountersModel cm = GetCounters.getInstance().getCmByDisplayName(tabName);
//			if ( cm != null && ! cm.isSystemCm() )
//				continue;

			ppe.put(tabName, CounterSetTemplates.PROPKEY_queryTimeout, tm.getValueAt(r, TAB_POS_QUERY_TIMEOUT).toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_postpone,     tm.getValueAt(r, TAB_POS_POSTPONE)     .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_paused,       tm.getValueAt(r, TAB_POS_PAUSED)       .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_bg,           tm.getValueAt(r, TAB_POS_BG)           .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_resetNC20,    tm.getValueAt(r, TAB_POS_RNC20)        .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_storePcs,     tm.getValueAt(r, TAB_POS_STORE_PCS)    .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_pcsAbs,       tm.getValueAt(r, TAB_POS_STORE_ABS)    .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_pcsDiff,      tm.getValueAt(r, TAB_POS_STORE_DIFF)   .toString());
			ppe.put(tabName, CounterSetTemplates.PROPKEY_pcsRate,      tm.getValueAt(r, TAB_POS_STORE_RATE)   .toString());
		}
		return ppe;
	}

	private boolean saveTemplate(String templateName)
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf == null)
			return false;

		// One CM entry looks like:
		// Objects={postpone=,paused=,bg=,resetNC20=,storePcs=,pcsAbs=,pcsDiff=,pcsRate=}
		//
		// Full output:
		// CmName1={entry}; CmName2={entry}; CmName3={entry};
		
		String propKey = "tcpConfigDialog.template.name."+templateName;
		PropPropEntry ppe = getPpeFromTable();

		// Set it in the template map... 
		// NOTE: I want to put it at the TOP of the list, if several exists, but this was not possible.
		_templatePpeMap.put(templateName, ppe);

		//System.out.println("tmpConf.setProperty(\"tcpTempate.template.name."+templateName+"\", \""+ppe.toString()+"\");");
		tmpConf.setProperty(propKey, ppe.toString());
		tmpConf.save();
		
		return true;
	}

	private boolean removeTemplate(String templateName)
	{
		if (templateName == null) return false;
		if (templateName.trim().equals("")) return false;
		
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf == null)
			return false;

		String propKey = "tcpConfigDialog.template.name."+templateName;
		String propVal = tmpConf.getProperty(propKey);
		if (propVal == null)
		{
			_logger.warn("The key '"+propKey+"', can't be found in the file '"+tmpConf.getFilename()+"'.");
			SwingUtils.showErrorMessage(this, "Error Removing Template", 
					"Can't remove the Template Named '"+templateName+"'\n\n" +
					"The key '"+propKey+"', can't be found in the file '"+tmpConf.getFilename()+"'.", null);
			return false;
		}
		
		// remove it from the template map...
		_templatePpeMap.remove(templateName);

		tmpConf.remove(propKey);
		tmpConf.save();
		return true;
	}
  	/*---------------------------------------------------
	** END: Template handling
	**---------------------------------------------------
	*/




	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// SUB-CLASSES: SaveOrRemoveTemplateDialog              ////
	////              LocalTable                              ////
	////              LocalTableModel                         ////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	/*---------------------------------------------------
	** BEGIN: class SaveOrRemoveTemplateDialog
	**---------------------------------------------------
	*/
	private static class SaveOrRemoveTemplateDialog
	extends JDialog
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

		public        int  _dialogType     = -1;
		public static int  SAVE_AS_DIALOG  = 1;
		public static int  REMOVE_DIALOG   = 2;

		private JLabel     _templates_lbl  = new JLabel();
		private JComboBox  _templates_cbx  = new JComboBox();

		private JButton    _ok             = new JButton("OK");
		private JButton    _cancel         = new JButton("Cancel");
		private String     _return         = null;
		
		private List<String> _templateList = null;
		private String       _saveAs       = null;

		private SaveOrRemoveTemplateDialog(JDialog owner, int dialogType, String saveAs, List<String> templateList)
		{
			super(owner, "", true);

			_dialogType   = dialogType;
			_saveAs       = saveAs;
			_templateList = templateList;

			initComponents();
			pack();
		}

		public static String showDialog(JDialog owner, int dialogType, String saveAs, List<String> templates)
		{
			SaveOrRemoveTemplateDialog dialog = new SaveOrRemoveTemplateDialog(owner, dialogType, saveAs, templates);
			dialog.setLocationRelativeTo(owner);
			dialog.setVisible(true);
			dialog.dispose();

			return dialog._return;
		}

		protected void initComponents() 
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right

			_templates_lbl.setToolTipText("Type a template name, or choose one from the drop down list");
			_templates_cbx.setToolTipText("Type a template name, or choose one from the drop down list");
			
			if (_dialogType == SAVE_AS_DIALOG)
			{
				setTitle("Save Template");
				_templates_lbl.setText("Save as Template");
				_templates_cbx.setEditable(true);
			}
			else if (_dialogType == REMOVE_DIALOG)
			{
				setTitle("Remove Template");
				_templates_lbl.setText("Remove Template");
				_templates_cbx.setEditable(true);
			}
			else throw new RuntimeException("Unknown Dialog Type");

			AutoCompleteDecorator.decorate(_templates_cbx);
			
			panel.add(_templates_lbl, "");
			panel.add(_templates_cbx,  "grow, wrap");
			
			// ADD the OK, Cancel, Apply buttons
			panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, push");
			panel.add(_cancel, "tag cancel,                   split, bottom");

			setContentPane(panel);

			// Fill in some start values
			for (String tname : _templateList)
			{
				_templates_cbx.addItem(tname);
			}

			String ctname = _saveAs;
			boolean isSystemTemplate = ctname.indexOf("System Template") >= 0;
			
			if ( isSystemTemplate || ctname.equals("") || ctname.equals(NO_TEMPLATE_IS_SELECTED))
				ctname = "write template name here";
			_templates_cbx.setSelectedItem(ctname);
			
			// ADD ACTIONS TO COMPONENTS
			_ok           .addActionListener(this);
			_cancel       .addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
				_return = _templates_cbx.getSelectedItem().toString();
				setVisible(false);
			}

			// --- BUTTON: CANCEL ---
			if (_cancel.equals(source))
			{
				_return = null;
				setVisible(false);
			}
		}
	}
	/*---------------------------------------------------
	** END: class SaveDialog
	**---------------------------------------------------
	*/

//	private static final String[] TAB_HEADER = {"Icon", "Tab Name", "Group Name", "Timeout", "Postpone", "Paused", "Background", "Reset NC20", "Store PCS", "Abs", "Diff", "Rate"};
	private static final String[] TAB_HEADER = {"Icon", "Tab Name", "Group Name", "Timeout", "Postpone", "Paused", "Background", "Reset NC20", "Record-PCS", "Abs", "Diff", "Rate"};
	private static final int TAB_POS_ICON          = 0;
	private static final int TAB_POS_TAB_NAME      = 1;
	private static final int TAB_POS_GROUP_NAME    = 2;
	private static final int TAB_POS_QUERY_TIMEOUT = 3;
	private static final int TAB_POS_POSTPONE      = 4;
	private static final int TAB_POS_PAUSED        = 5;
	private static final int TAB_POS_BG            = 6;
	private static final int TAB_POS_RNC20         = 7;
	private static final int TAB_POS_STORE_PCS     = 8;
	private static final int TAB_POS_STORE_ABS     = 9;
	private static final int TAB_POS_STORE_DIFF    = 10;
	private static final int TAB_POS_STORE_RATE    = 11;

	private static final Color TAB_PCS_COL_BG = new Color(240, 240, 240);
//	private static final Color TAB_PCS_COL_BG = new Color(243, 243, 243);
//	private static final Color TAB_PCS_COL_BG = new Color(245, 245, 245);

	/*---------------------------------------------------
	** BEGIN: class LocalTableModel
	**---------------------------------------------------
	*/
	/** LocalTableModel */
	private static class LocalTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = 1L;

		private Vector<Vector<Boolean>> _changeIndicator = new Vector<Vector<Boolean>>();  /* is a Vector of "row" Vectors, which contains Booleans */

		LocalTableModel()
		{
			super();
			setColumnIdentifiers(TAB_HEADER);
		}

		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			super.setValueAt(value, row, column);

			// hook in to set that a value was changed
			if ( _changeIndicator.size() < getRowCount() )
				_changeIndicator.setSize( getRowCount() );

			// Get the row Vector and check it's size
			Vector<Boolean> changeRowIndicator = _changeIndicator.get(row);
			if (changeRowIndicator == null)
			{
				changeRowIndicator = new Vector<Boolean>(getColumnCount());
				_changeIndicator.set(row, changeRowIndicator);
			}
			if (changeRowIndicator.size() < getColumnCount())
				changeRowIndicator.setSize(getColumnCount());
			
			Boolean changed = changeRowIndicator.get(column);
			
			if ( changed == null )
				changeRowIndicator.set(column, new Boolean(true));
			else if ( ! changed.booleanValue() )
				changeRowIndicator.set(column, new Boolean(true));
		}

		public boolean isCellChanged(int row, int col)
		{
			Vector<Boolean> changeRowIndicator = _changeIndicator.get(row);
			if (changeRowIndicator == null)
				return false;
			Boolean changed = changeRowIndicator.get(col);
			if (changed == null)
				return false;
			return changed.booleanValue();
		}

		public void resetCellChanges()
		{
			_changeIndicator = new Vector<Vector<Boolean>>(getRowCount());
			_changeIndicator.setSize(getRowCount());
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_ICON)          return Icon.class;
			if (column == TAB_POS_QUERY_TIMEOUT) return Integer.class;
			if (column == TAB_POS_POSTPONE)      return Integer.class;
			if (column  > TAB_POS_POSTPONE)      return Boolean.class;
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			if (col == TAB_POS_BG || col > TAB_POS_STORE_PCS)
			{
				// get some values from the MODEL viewRow->modelRow translation should be done before calling isCellEditable
				boolean storePcs    = ((Boolean) getValueAt(row, TAB_POS_STORE_PCS)).booleanValue();
				String tabName      = (String)   getValueAt(row, TAB_POS_TAB_NAME);

				if (_logger.isDebugEnabled())
					_logger.debug("isCellEditable: row="+row+", col="+col+", storePcs="+storePcs+", tabName='"+tabName+"'.");

				// Get CountersModel and check if that model supports editing for Abs, Diff & Rate
				CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
				if (cm != null)
				{
					if (col == TAB_POS_BG)         return cm.isBackgroundDataPollingEditable();

					if (col == TAB_POS_STORE_ABS)  return storePcs && cm.isPersistCountersAbsEditable();
					if (col == TAB_POS_STORE_DIFF) return storePcs && cm.isPersistCountersDiffEditable();
					if (col == TAB_POS_STORE_RATE) return storePcs && cm.isPersistCountersRateEditable();
				}
			}

			return col >= TAB_POS_QUERY_TIMEOUT;
		}
	}
	/*---------------------------------------------------
	** END: class LocalTableModel
	**---------------------------------------------------
	*/


	/*---------------------------------------------------
	** BEGIN: class LocalTable
	**---------------------------------------------------
	*/
	/** Extend the JXTable */
	private class LocalTable extends JXTable
	{
		private static final long serialVersionUID = 0L;
//		protected int           _lastTableHeaderPointX = -1;
		protected int           _lastTableHeaderColumn = -1;
		private   JPopupMenu    _popupMenu             = null;
		private   JPopupMenu    _headerPopupMenu       = null;


		LocalTable()
		{
			super();
			setModel( new LocalTableModel() );

			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
//			setHighlighters(_highliters);

			// Create some PopupMenus and attach them
			_popupMenu = createDataTablePopupMenu();
			setComponentPopupMenu(getDataTablePopupMenu());

			_headerPopupMenu = createDataTableHeaderPopupMenu();
			getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

			// Populate the table
			refreshTable();
			
			// hide 'Group Name' if no child's are found
			if ( MainFrame.hasInstance() && ! MainFrame.getInstance().getTabbedPane().hasChildPanels() )
			{
				TableColumnModelExt tcmx = (TableColumnModelExt)this.getColumnModel();
				tcmx.getColumnExt(TAB_HEADER[TAB_POS_GROUP_NAME]).setVisible(false);
			}
		}

		/** What table header was the last header we visited */
		public int getLastTableHeaderColumn()
		{
			return _lastTableHeaderColumn;
		}

		/** TABLE HEADER tool tip. */
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
			{
                private static final long serialVersionUID = 0L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					String tip = null;

					int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (col < 0) return null;

					int mcol = convertColumnIndexToModel(col);
					if (mcol < 0) return null;

					switch(mcol)
					{
					case TAB_POS_ICON:          tip = null; break;
					case TAB_POS_TAB_NAME:      tip = "Name of Tab/Collector."; break;
					case TAB_POS_GROUP_NAME:    tip = "What Group does this performance counter belong to"; break;
					case TAB_POS_QUERY_TIMEOUT: tip = "The SQL Query Timeout value in seconds. For how long should we wait for a replay from the ASE Server for this specific Performance Counter."; break;
					case TAB_POS_POSTPONE:      tip = "If you want to skip some intermediate samples, Here you can specify minimum seconds between samples."; break;
					case TAB_POS_PAUSED:        tip = "Pause data polling for this Tab. This makes the values easier to read..."; break;
					case TAB_POS_BG:            tip = "Sample this panel even when this Tab is not active."; break;
					case TAB_POS_RNC20:         tip = "If the difference between 'this' and 'previous' data sample has negative counter values, reset them to be <b>zero</b>"; break;
					case TAB_POS_STORE_PCS:     tip = "Save this Counter Set to a Persistent Storage, even when we are in GUI mode.<br>Note: This is only enabled/available if you specified a Counter Storage when you connected."; break;
					case TAB_POS_STORE_ABS:     tip = "Save the Absolute Counters in the Persistent Counter Storage"; break;
					case TAB_POS_STORE_DIFF:    tip = "Save the Difference Counters in the Persistent Counter Storage"; break;
					case TAB_POS_STORE_RATE:    tip = "Save the Rate Counters in the Persistent Counter Storage"; break;
					}

					if (tip == null)
						return null;
					return "<html>" + tip + "</html>";
				}
			};

			// Track where we are in the TableHeader, this is used by the Popup menus
			// to decide what column of the TableHeader we are currently located on.
			tabHeader.addMouseMotionListener(new MouseMotionListener()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
					if (_lastTableHeaderColumn >= 0)
						_lastTableHeaderColumn = convertColumnIndexToModel(_lastTableHeaderColumn);
				}
				@Override
				public void mouseDragged(MouseEvent e) {/*ignore*/}
			});

			return tabHeader;
		}

		/** CELL tool tip */
		@Override
		public String getToolTipText(MouseEvent e)
		{
			String tip = null;
			Point p = e.getPoint();
			int mrow = super.convertRowIndexToModel( rowAtPoint(p)<0 ? 0 : rowAtPoint(p) );
			int mcol = super.convertColumnIndexToModel(columnAtPoint(p));

			if (mcol > TAB_POS_POSTPONE)
			{
				tip = "Right click on the header column to mark or unmark all rows.";
			}
			if (mrow >= 0)
			{
				//TableModel model = getModel();
			}
			if (tip == null)
				return null;
			return "<html>" + tip + "</html>";
		}

//		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
//		public Component prepareRenderer(TableCellRenderer renderer, int row, int col)
//		{
//			int mrow = super.convertRowIndexToModel(row);
//			int mcol = super.convertColumnIndexToModel(col);
//
//			Component c = super.prepareRenderer(renderer, mrow, mcol);
//			if (mcol == TAB_POS_BG)
//			{
//				c.setEnabled( isCellEditable(mrow, mcol) );
//			}
//			if (mcol >= TAB_POS_STORE_PCS)
//			{
//				c.setBackground(TAB_PCS_COL_BG);
//				if (mcol > TAB_POS_STORE_PCS)
//				{
//					// if not editable, lets disable it
//					// calling isCellEditable instead of getModel().isCellEditable(row, column)
//					// does the viewRow->modelRow translation for us.
//					c.setEnabled( isCellEditable(mrow, mcol) );
//				}
//			}
//			return c;
//		}
		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int col)
		{
			int view_TAB_POS_BG         = convertColumnIndexToView(TAB_POS_BG);
			int view_TAB_POS_STORE_PCS  = convertColumnIndexToView(TAB_POS_STORE_PCS);

			Component c = super.prepareRenderer(renderer, row, col);
			if (col == view_TAB_POS_BG)
			{
				c.setEnabled( isCellEditable(row, col) );
			}
			if (col >= view_TAB_POS_STORE_PCS)
			{
				c.setBackground(TAB_PCS_COL_BG);
				if (col > view_TAB_POS_STORE_PCS)
				{
					// if not editable, lets disable it
					// calling isCellEditable instead of getModel().isCellEditable(row, column)
					// does the viewRow->modelRow translation for us.
					c.setEnabled( isCellEditable(row, col) );
				}
			}
			return c;
		}

//		/** Populate information in the table */
//		protected void refreshTable()
//		{
//			Vector<Object> row = new Vector<Object>();
//
//			DefaultTableModel tm = (DefaultTableModel)getModel();
//
//			JTabbedPane tabPane = MainFrame.getTabbedPane();
//			if (tabPane == null)
//				return;
//
//			while (tm.getRowCount() > 0)
//				tm.removeRow(0);
//
//			int tabCount = tabPane.getTabCount();
//			for (int t=0; t<tabCount; t++)
//			{
//				Component comp = tabPane.getComponentAt(t);
//
//				if (comp instanceof TabularCntrPanel)
//				{
//					TabularCntrPanel tcp = (TabularCntrPanel) comp;
//					CountersModel    cm  = tcp.getCm();
//					if (cm != null)
//					{
//						row = new Vector<Object>();
//
//						row.add(cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
//						row.add(cm.getDisplayName());   // TAB_POS_TAB_NAME
//
//						row.add(new Integer( cm.getQueryTimeout()) );
//						row.add(new Integer( cm.getPostponeTime()) );
//						row.add(new Boolean( cm.isDataPollingPaused() ));
//						row.add(new Boolean( cm.isBackgroundDataPollingEnabled() ));
//						row.add(new Boolean( cm.isNegativeDiffCountersToZero() ));
//
//						row.add(new Boolean( cm.isPersistCountersEnabled() ));
//						row.add(new Boolean( cm.isPersistCountersAbsEnabled() ));
//						row.add(new Boolean( cm.isPersistCountersDiffEnabled() ));
//						row.add(new Boolean( cm.isPersistCountersRateEnabled() ));
//
//						tm.addRow(row);
//					}
//				}
//			}
//			resetCellChanges();
//			packAll(); // set size so that all content in all cells are visible
//		}
		/** Populate information in the table */
		protected void refreshTable()
		{
			DefaultTableModel tm = (DefaultTableModel)getModel();

			JTabbedPane tabPane = MainFrame.hasInstance() ? MainFrame.getInstance().getTabbedPane() : null;
			if (tabPane == null)
				return;

			while (tm.getRowCount() > 0)
				tm.removeRow(0);

			refreshTable(tabPane, tm, null);

			resetCellChanges();
			packAll(); // set size so that all content in all cells are visible
		}
		private void refreshTable(JTabbedPane tabPane, DefaultTableModel tm, String groupName)
		{
			for (int t=0; t<tabPane.getTabCount(); t++)
			{
				Component comp    = tabPane.getComponentAt(t);
				String    tabName = tabPane.getTitleAt(t);

				if (comp instanceof JTabbedPane)
					refreshTable((JTabbedPane)comp, tm, tabName);
				else if (comp instanceof TabularCntrPanel)
				{
					TabularCntrPanel tcp = (TabularCntrPanel) comp;
					CountersModel    cm  = tcp.getCm();
					
					if (StringUtil.isNullOrBlank(groupName))
						groupName = tcp.getGroupName();

					if (cm != null)
					{
						Vector<Object> row = new Vector<Object>();

						row.add(cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
						row.add(cm.getDisplayName());   // TAB_POS_TAB_NAME
						row.add(groupName);

						row.add(new Integer( cm.getQueryTimeout()) );
						row.add(new Integer( cm.getPostponeTime()) );
						row.add(new Boolean( cm.isDataPollingPaused() ));
						row.add(new Boolean( cm.isBackgroundDataPollingEnabled() ));
						row.add(new Boolean( cm.isNegativeDiffCountersToZero() ));

						row.add(new Boolean( cm.isPersistCountersEnabled() ));
						row.add(new Boolean( cm.isPersistCountersAbsEnabled() ));
						row.add(new Boolean( cm.isPersistCountersDiffEnabled() ));
						row.add(new Boolean( cm.isPersistCountersRateEnabled() ));

						tm.addRow(row);
					}
				}
			}
		}

//		public boolean isCellChanged(int row, int col)
//		{
//			int mrow = super.convertRowIndexToModel(row);
//			int mcol = super.convertColumnIndexToModel(col);
//			
//			LocalTableModel tm = (LocalTableModel)getModel();
//			return tm.isCellChanged(mrow, mcol);
//		}

		/** typically called from any "apply" button. */
		public void resetCellChanges()
		{
			LocalTableModel tm = (LocalTableModel)getModel();
			tm.resetCellChanges();
			
			// redraw the table
			// Do this so that "check boxes" are pushed via: prepareRenderer()
			repaint();
		}

		
		/*---------------------------------------------------
		** BEGIN: PopupMenu on the table
		**---------------------------------------------------
		*/
		/** Get the JMeny attached to the GTabbedPane */
		public JPopupMenu getDataTablePopupMenu()
		{
			return _popupMenu;
		}

		/**
		 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
		 * If you want to add stuff to the menu, its better to use
		 * getTabPopupMenu(), then add entries to the menu. This is much
		 * better than subclass the GTabbedPane
		 */
		public JPopupMenu createDataTablePopupMenu()
		{
			_logger.debug("createDataTablePopupMenu(): called.");

			JPopupMenu popup = new JPopupMenu();
			JMenuItem show = new JMenuItem("XXX");

			popup.add(show);

			show.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
//					doActionShow();
				}
			});

			if (popup.getComponentCount() == 0)
			{
				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
				return null;
			}
			else
				return popup;
		}

		/** Get the JMeny attached to the JTable header */
		public JPopupMenu getDataTableHeaderPopupMenu()
		{
			return _headerPopupMenu;
		}

		public JPopupMenu createDataTableHeaderPopupMenu()
		{
			_logger.debug("createDataTableHeaderPopupMenu(): called.");
			JPopupMenu popup = new JPopupMenu();
			JMenuItem mark   = new JMenuItem("Mark all rows for this column");
			JMenuItem unmark = new JMenuItem("UnMark all rows for this column");

			popup.add(mark);
			popup.add(unmark);

			mark.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int col = getLastTableHeaderColumn();
					if (col > TAB_POS_POSTPONE)
					{
						TableModel tm = getModel();
						for (int r=0; r<tm.getRowCount(); r++)
						{
							if (tm.isCellEditable(r, col))
								tm.setValueAt(new Boolean(true), r, col);
						}
					}
				}
			});

			unmark.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int col = getLastTableHeaderColumn();
					if (col > TAB_POS_POSTPONE)
					{
						TableModel tm = getModel();
						for (int r=0; r<tm.getRowCount(); r++)
						{
							if (tm.isCellEditable(r, col))
								tm.setValueAt(new Boolean(false), r, col);
						}
					}
				}
			});

			// add something like:
			// popup.preShow()... so we can enable/disable menu items when we are on specific columns
			//popup.add

			if (popup.getComponentCount() == 0)
			{
				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
				return null;
			}
			else
				return popup;
		}

		/*---------------------------------------------------
		** END: PopupMenu on the table
		**---------------------------------------------------
		*/
	}

	/*---------------------------------------------------
	** END: class LocalTable
	**---------------------------------------------------
	*/








	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// MAIN & TEST - MAIN & TEST - MAIN & TEST - MAIN & TEST ///
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// set native L&F
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
		catch (Exception e) {}


		Configuration conf = new Configuration("c:\\OfflineSessionsViewer.tmp.deleteme.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf);

		MainFrame frame = new MainFrame();

		// Create and Start the "collector" thread
		GetCounters getCnt = new GetCountersGui();
		CounterController.setInstance(getCnt);
		try
		{
			getCnt.init();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		getCnt.start();

		frame.pack();

		TcpConfigDialog.showDialog(frame);
	}
}
