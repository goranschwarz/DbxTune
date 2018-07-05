package com.asetune.alarm.ui.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.ui.config.AlarmWritersTableModel.AlarmWriterEntry;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;

public class AlarmWritersTablePanel
extends JPanel 
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	private AlarmWritersTable      _alarmWritersTable;
	private AlarmWritersTableModel _alarmWritersTableModel;

//	private AlarmWriterSettingsPanel _alarmWriterSettingsPanel;
	private AlarmWriterDetailsPanel _alarmWriterDetailsPanel;

	private JLabel _warning_lbl = new JLabel();
	private JLabel _debug_lbl   = new JLabel();

//	public AlarmWritersTablePanel(AlarmWriterSettingsPanel alarmWriterSettingsPanel)
	public AlarmWritersTablePanel(AlarmWriterDetailsPanel alarmWriterDetailsPanel)
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Available Alarms Writers");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
//		_alarmWriterSettingsPanel = alarmWriterSettingsPanel;
//		_alarmWritersTableModel   = new AlarmWritersTableModel();
//		_alarmWritersTable        = new AlarmWritersTable(_alarmWritersTableModel, _alarmWriterSettingsPanel);
//		_alarmWritersTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed
		_alarmWriterDetailsPanel  = alarmWriterDetailsPanel;
		_alarmWritersTableModel   = new AlarmWritersTableModel();
		_alarmWritersTable        = new AlarmWritersTable(_alarmWritersTableModel, _alarmWriterDetailsPanel);
		_alarmWritersTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		GTableFilter filter = new GTableFilter(_alarmWritersTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
//		filter.setText("where ["+AlarmTableModel.TAB_HEADER[AlarmTableModel.TAB_POS_HAS_SYSTEM]+"] = 'true' or ["+AlarmTableModel.TAB_HEADER[AlarmTableModel.TAB_POS_HAS_UD]+"] = 'true'");

		JScrollPane scroll = new JScrollPane(_alarmWritersTable);
		add(filter,       "pushx, growx, gapleft 10, gapright 10, wrap");
		add(_warning_lbl, "split, pushx, growx, hidemode 2");
		add(_debug_lbl,   "hidemode 2, wrap");
		add(scroll,       "push, grow, height 100%, wrap");
		
		_warning_lbl.setVisible(false);
		_debug_lbl  .setVisible(false);

		_alarmWritersTable.refreshTable();

		// Select first row
		if (_alarmWritersTable.getRowCount() > 0)
			_alarmWritersTable.getSelectionModel().setSelectionInterval(0, 0);
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		checkContent();
		firePropertyChange("tableChanged", "alarmWritersTable", e.getSource());
	}

	public void checkContent()
	{
		_debug_lbl.setText("ChangedRowCount=" + _alarmWritersTableModel.getModifiedRowCount());

		_warning_lbl.setText("");
		_warning_lbl.setVisible(false);
		_warning_lbl.setForeground(UIManager.getColor("Label.foreground"));

		String problem = _alarmWritersTableModel.getProblem();
		if (StringUtil.hasValue(problem))
		{
			_warning_lbl.setText(problem);
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.RED);
		}
			
//		if (_alarmWritersTableModel.getUsedCount() == 0)
//		{
//			_warning_lbl.setText("<html>"
//					+ "You need to <i>select</i> what Writers you want to use. Click the <i>Use</i> cell.  Note: You can select <b>several</b> writers...<html>");
//			_warning_lbl.setVisible(true);
//			_warning_lbl.setForeground(Color.RED);
//			return;
//		}

//		if (_alarmWritersTableModel.checkForMandatoryData())
//		{
//			_warning_lbl.setText("Mandatory values need to be replaced");
//			_warning_lbl.setVisible(true);
//			_warning_lbl.setForeground(Color.RED);
//			return;
//		}
	}

	/** Check that at least ONE Writer has been selected, and ALL settings is valid for the selected Writers */
	public String getProblem()
	{ 
		return _alarmWritersTableModel.getProblem();
	}

	public boolean isDirty()
	{
		return _alarmWritersTableModel.isDirty();
	}

	public Configuration getConfig()
	{
		Configuration conf = new Configuration();

		// Get all selected AlarmWriterClasses
		List<String> alarmWriterClasses = new ArrayList<>();
		for (int r=0; r<_alarmWritersTableModel.getRowCount(); r++)
		{
			AlarmWriterEntry awe = _alarmWritersTableModel.getWriterEntryForRow(r);
			if (awe._selected)
				alarmWriterClasses.add(awe._className);
		}
		
		// No writer classes...
		if (alarmWriterClasses.isEmpty())
			return conf;

		// Set config for WRITERS
		conf.setProperty(AlarmHandler.PROPKEY_WriterClass, StringUtil.toCommaStr(alarmWriterClasses));

		
		// Set config for WRITERS SETTINGS
		for (int r=0; r<_alarmWritersTableModel.getRowCount(); r++)
		{
			AlarmWriterEntry awe = _alarmWritersTableModel.getWriterEntryForRow(r);
			if (awe._selected)
			{
//				for (AlarmWriterSettingsEntry awse : awe._settings)
//				{
//					if (awse._selected)
//					{
//						String key   = awse._propKey;
//						String value = awse._value;
//
//						// encrypt fields with password
//						boolean doEncrypt = false;
//						if (awse._name != null && awse._name.toLowerCase().indexOf("password") != -1)
//							doEncrypt = true;
//
//						conf.setProperty(key, value, doEncrypt);
//					}
//				}
				for (CmSettingsHelper awse : awe._settings)
				{
					if (awse.isSelected())
					{
						String key   = awse.getPropName();
						String value = awse.getStringValue();

						// encrypt fields with password
						boolean doEncrypt = false;
						if (awse.getName() != null && awse.getName().toLowerCase().indexOf("password") != -1)
							doEncrypt = true;

						conf.setProperty(key, value, doEncrypt);
					}
				}
			}
		}
		
		// Set config for WRITERS FILTERS
		for (int r=0; r<_alarmWritersTableModel.getRowCount(); r++)
		{
			AlarmWriterEntry awe = _alarmWritersTableModel.getWriterEntryForRow(r);
			if (awe._selected)
			{
				for (CmSettingsHelper awse : awe._filters)
				{
					if (awse.isSelected())
					{
						String key   = awse.getPropName();
						String value = awse.getStringValue();

						// encrypt fields with password
						boolean doEncrypt = false;
						if (awse.getName() != null && awse.getName().toLowerCase().indexOf("password") != -1)
							doEncrypt = true;

						conf.setProperty(key, value, doEncrypt);
					}
				}
			}
		}
		
		return conf;
	}
}
