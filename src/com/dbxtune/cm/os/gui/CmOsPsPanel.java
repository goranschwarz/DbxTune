/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.os.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

public class CmOsPsPanel
extends CmOsGenericPanel
{
	private static final long    serialVersionUID      = 1L;

	public CmOsPsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private static final Integer constant_ioOk = 1;
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// mark cells where IO Operations is not to be trusted 
		if (conf != null) colorStr = conf.getProperty(getName()+".color.ioIsNotOk");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int ioOk_pos = adapter.getColumnIndex("ioOk");
				if (ioOk_pos != -1)
				{
					if ( ! constant_ioOk.equals(adapter.getValue(ioOk_pos)) )
					{
						String thisColName = adapter.getColumnName(adapter.convertColumnIndexToModel(adapter.column));
						if (thisColName.startsWith("io") || StringUtil.equalsAny(thisColName, "avgIoSizeKb", "avgRIoSizeKb", "avgWIoSizeKb"))
							return true;
					}
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.LIGHT_GRAY), null));
	}
	

	private boolean _tableCellRendersIsInitialized = false;

	@Override
	public void setTableCellRenders()
	{
		// TODO Auto-generated method stub
		super.setTableCellRenders();

		if (_tableCellRendersIsInitialized)
			return;

		// Add a Boolean Renderer to "ioOk"
		GTable table = getDataTable();

		// Installing Special Integer Boolean Renderer for column 'ioOk' -- That shows if the IO Columns are to be trusted or not
		TableColumnExt isOkCol = table.getColumnExt("ioOk");
		if (isOkCol != null)
		{
			isOkCol.setCellRenderer(new DefaultTableCellRenderer()
			{
				private static final long serialVersionUID = 1L;
				private final JCheckBox checkbox = new JCheckBox();

				@Override
				public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					checkbox.setHorizontalAlignment(SwingConstants.CENTER);
					checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

					if (value instanceof Integer)
					{
						checkbox.setSelected( (Integer)value == 1);
					}
					return checkbox;
				}
			});
			
			_tableCellRendersIsInitialized = true;
		}
	}

	private JLabel     _sample_topRows_lbl;
	private JTextField _sample_topRows_txt;

	private JLabel     _sample_minCpuPctUsage_lbl;
	private JTextField _sample_minCpuPctUsage_txt;
	private JCheckBox  _sample_useSudo_cbx;

	@Override
	protected JPanel configLocalOptionsPanel(JPanel panel)
	{
		if (panel instanceof LocalOptionsConfigPanel)
		{
			((LocalOptionsConfigPanel)panel).setLocalOptionsConfigChanges(new LocalOptionsConfigChanges()
			{
				@Override
				public void configWasChanged(String propName, String propVal)
				{
					Configuration conf = Configuration.getCombinedConfiguration();
					
//					list.add(new CmSettingsHelper("Top Rows", PROPKEY_top, Integer.class, conf.getIntProperty(PROPKEY_top, DEFAULT_top), DEFAULT_top, "Number of top rows."));

					_sample_topRows_txt       .setText(""+ conf.getIntProperty    (CmOsPs.PROPKEY_top           , CmOsPs.DEFAULT_top));
					_sample_minCpuPctUsage_txt.setText(""+ conf.getDoubleProperty (CmOsPs.PROPKEY_minCpuPctUsage, CmOsPs.DEFAULT_minCpuPctUsage));
					_sample_useSudo_cbx   .setSelected(    conf.getBooleanProperty(CmOsPs.PROPKEY_linux_useSudo , CmOsPs.DEFAULT_linux_useSudo));
				}
			});
		}

		Configuration conf = Configuration.getCombinedConfiguration();

		_sample_topRows_lbl = new JLabel("Top Rows");
		_sample_topRows_txt = new JTextField(""+ conf.getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top), 5);

		_sample_topRows_lbl.setToolTipText("<html>Only first # rows from ps<br><br>Note: To get everything use: -1</html>");
		_sample_topRows_txt.setToolTipText("<html>Only first # rows from ps<br><br>Note: To get everything use: -1</html>");
		
		_sample_topRows_lbl.setVisible(true);
		_sample_topRows_txt.setVisible(true);


		_sample_minCpuPctUsage_lbl = new JLabel("Above CPU%");
		_sample_minCpuPctUsage_txt = new JTextField(""+ conf.getDoubleProperty(CmOsPs.PROPKEY_minCpuPctUsage, CmOsPs.DEFAULT_minCpuPctUsage), 5);

		_sample_minCpuPctUsage_lbl.setToolTipText("<html>Only show/collect rows when '%cpu' is above this value<br><br>Note: To get everything use: -1.0</html>");
		_sample_minCpuPctUsage_txt.setToolTipText("<html>Only show/collect rows when '%cpu' is above this value<br><br>Note: To get everything use: -1.0</html>");

		_sample_minCpuPctUsage_lbl.setVisible(true);
		_sample_minCpuPctUsage_txt.setVisible(true);

		
		_sample_useSudo_cbx = new JCheckBox("Use sudo", conf.getBooleanProperty(CmOsPs.PROPKEY_linux_useSudo , CmOsPs.DEFAULT_linux_useSudo));
		_sample_useSudo_cbx.setToolTipText("<html>Use <code>sudo</code> to execute the command (use this on Linux if column 'ioOk' is false...)<br><br>Note: Also look at the tooltip for column 'ioOk' to get more details.</html>");
		_sample_useSudo_cbx.setVisible(true);

		
//		panel.add( _sample_topRows_lbl, "split, hidemode 3");  // or: span
//		panel.add( _sample_topRows_txt, "hidemode 3, wrap");
//
//		panel.add( _sample_minCpuPctUsage_lbl, "split, hidemode 3");  // or: span
//		panel.add( _sample_minCpuPctUsage_txt, "hidemode 3, wrap");

		panel.add( _sample_topRows_lbl, "split, hidemode 3");  // or: span
		panel.add( _sample_topRows_txt, "hidemode 3");

		panel.add( _sample_minCpuPctUsage_lbl, "hidemode 3");
		panel.add( _sample_minCpuPctUsage_txt, "hidemode 3");
		
		panel.add( _sample_useSudo_cbx, "hidemode 3");

		_sample_topRows_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsPs.PROPKEY_top, _sample_topRows_txt.getText());
					tempConf.save();
				}
			}
		});

		_sample_minCpuPctUsage_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsPs.PROPKEY_minCpuPctUsage, _sample_minCpuPctUsage_txt.getText());
					tempConf.save();
				}
			}
		});
		
		_sample_useSudo_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsPs.PROPKEY_linux_useSudo, _sample_useSudo_cbx.isSelected());
					tempConf.save();
				}
			}
		});
		
		return panel;
	}
}
