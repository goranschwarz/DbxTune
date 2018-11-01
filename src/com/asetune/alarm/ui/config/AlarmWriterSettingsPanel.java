package com.asetune.alarm.ui.config;

import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.asetune.cm.CmSettingsHelper;
import com.asetune.graph.TrendGraphColors;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class AlarmWriterSettingsPanel
extends JPanel
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	AlarmWriterSettingsTable      _alarmWriterSettingsTable;
	AlarmWriterSettingsTableModel _alarmWriterSettingsTableModel;
	
	private JLabel _warning_lbl = new JLabel();
	private JLabel _debug_lbl   = new JLabel();
	
	public AlarmWriterSettingsPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Available Settings for the Alarm Writer");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_alarmWriterSettingsTableModel = new AlarmWriterSettingsTableModel();
		_alarmWriterSettingsTable      = new AlarmWriterSettingsTable(_alarmWriterSettingsTableModel);
		_alarmWriterSettingsTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		// DISABLE HTML RENDERING in the cells for String
		//_alarmWriterSettingsTable.putClientProperty("html.disable", Boolean.TRUE);
		// The above did NOT work, neither  setDefaultRenderer(... putClientProperty("html.disable") ... )
//		_alarmWriterSettingsTable.setDefaultRenderer(StringValue.class, new DefaultTableCellRenderer()
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//			{
//				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//System.out.println("getTableCellRendererComponent: c="+c);
//				if (c instanceof JComponent)
//				{
//					((JComponent)c).putClientProperty("html.disable", Boolean.TRUE);
//				}
//				return c;
//			}
//		});
		
		GTableFilter filter = new GTableFilter(_alarmWriterSettingsTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		filter.setText("");

		JScrollPane scroll = new JScrollPane(_alarmWriterSettingsTable);
		add(filter,       "pushx, growx, gapleft 10, gapright 10, wrap");
		add(_warning_lbl, "split, pushx, growx, hidemode 2");
		add(_debug_lbl,   "hidemode 2, wrap");
		add(scroll,       "push, grow, height 100%, wrap");

		_warning_lbl.setVisible(false);
		_debug_lbl  .setVisible(false);
		
		_alarmWriterSettingsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				checkContent();
			}
		});
	}

//	public void setWriterSettings(List<AlarmWriterSettingsEntry> settings)
//	{
//		_alarmWriterSettingsTable.refreshTable(settings);
//		checkContent();
//	}
	public void setWriterSettings(List<CmSettingsHelper> settings)
	{
		_alarmWriterSettingsTable.refreshTable(settings);
		checkContent();
	}
//	public void setWriterName(String className)
//	{
//		_alarmWriterSettingsTable.refreshTable(className);
//		checkContent();
//	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		checkContent();
		firePropertyChange("tableChanged", "alarmWriterSettingsTable", e.getSource());
	}

	public void checkContent()
	{
		_debug_lbl.setText("ChangedRowCount=" + _alarmWriterSettingsTableModel.getModifiedRowCount());
		
		_warning_lbl.setText("");
		_warning_lbl.setVisible(false);
		_warning_lbl.setForeground(UIManager.getColor("Label.foreground"));

		if (_alarmWriterSettingsTableModel.getRowCount() == 0)
		{
			_warning_lbl.setText("No settings was available for this writer");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.BLUE);
			return;
		}

		if (_alarmWriterSettingsTableModel.checkForMandatoryData())
		{
			_warning_lbl.setText("Mandatory values need to be filled in");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.RED);
			return;
		}

		if (_alarmWriterSettingsTableModel.checkForProbableData())
		{
			_warning_lbl.setText("Optional/Template values need to be replaced with real values");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.RED);
			return;
		}

		if (_alarmWriterSettingsTableModel.getUsedCount() == 0)
		{
			_warning_lbl.setText("<html>You need to <i>select</i> what settings you want to use. Otherwise all settings will use defaults. Click the <i>Use</i> cell<html>");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.ORANGE);
			return;
		}

		if (_alarmWriterSettingsTable.getSelectedRow() != -1)
		{
			int selectedRow = _alarmWriterSettingsTable.getSelectedRow();
			int mrow = _alarmWriterSettingsTable.convertRowIndexToModel(selectedRow);
			String val = _alarmWriterSettingsTableModel.getValueAt(mrow, AlarmSettingsTableModel.TAB_POS_PROPERTY_KEY) + "";
			if (val.toLowerCase().indexOf("template") != -1)
			{
				_warning_lbl.setText("<html>TIP: The selected row is a <b>template</b>. Right click on the row and choose <b>Template Edit</b>... There you can see the translated content.<html>");
				_warning_lbl.setVisible(true);
				_warning_lbl.setForeground(TrendGraphColors.VERY_DARK_GREEN);
			}
		}
	}

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return _alarmWriterSettingsTable.isDirty();
//		return false;
	}

	/**
	 * return true if this panel has any configurable data
	 * @return
	 */
	public boolean hasData()
	{
		return _alarmWriterSettingsTable.getModel().getRowCount() > 0;
	}

	public boolean hasCheckedRows()
	{
		return _alarmWriterSettingsTableModel.getSelectedRowCount() > 0;
	}

	/**
	 * Save settings
	 */
	public void save()
	{
		System.out.println(this.getClass().getSimpleName() + ": SAVE - not yet implemented");
	}

	public Configuration getConfig(String className)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
