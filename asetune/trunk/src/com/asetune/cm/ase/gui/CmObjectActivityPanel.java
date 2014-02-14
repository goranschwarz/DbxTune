package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmCachedObjects;
import com.asetune.cm.ase.CmObjectActivity;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmObjectActivityPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmObjectActivityPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmObjectActivity.CM_NAME;

	public CmObjectActivityPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
		int     defaultIntOpt;

		// RowCount
		defaultOpt = conf == null ? CmObjectActivity.DEFAULT_sample_tabRowCount : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, CmObjectActivity.DEFAULT_sample_tabRowCount);
		JCheckBox sampleRowCount_chk = new JCheckBox("Sample Table Row Count", defaultOpt);

		sampleRowCount_chk.setName(CmObjectActivity.PROPKEY_sample_tabRowCount);
		sampleRowCount_chk.setToolTipText("<html>" +
				"Sample Table Row Count using ASE functions <code>row_count()</code> and <code>data_pages()</code>.<br>" +
				"<b>Note 1</b>: Only in ASE 15.0.2 or higher.<br>" +
				"<b>Note 2</b>: You can also set the property '"+CmObjectActivity.PROPKEY_sample_tabRowCount+"'=true|false' in the configuration file.<br>" +
				"<b>Note 3</b>: To check if this is enabled or not, use the Properties dialog in this tab pane, right click + properties...<br>" +
				"</html>");

		sampleRowCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmObjectActivity.PROPKEY_sample_tabRowCount, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		// Top Rows (top #)
		defaultOpt    = conf == null ? CmObjectActivity.DEFAULT_sample_topRows      : conf.getBooleanProperty(CmObjectActivity.PROPKEY_sample_topRows,      CmObjectActivity.DEFAULT_sample_topRows);
		defaultIntOpt = conf == null ? CmObjectActivity.DEFAULT_sample_topRowsCount : conf.getIntProperty    (CmObjectActivity.PROPKEY_sample_topRowsCount, CmObjectActivity.DEFAULT_sample_topRowsCount);
		final JCheckBox  sampleTopRows_chk      = new JCheckBox("Limit number of rows (top #)", defaultOpt);
		final JTextField sampleTopRowsCount_txt = new JTextField(Integer.toString(defaultIntOpt), 5);

		sampleTopRows_chk.setName(CmObjectActivity.PROPKEY_sample_topRows);
		sampleTopRows_chk.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmCachedObjects.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRowsCount_txt.setName(CmObjectActivity.PROPKEY_sample_topRowsCount);
		sampleTopRowsCount_txt.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmCachedObjects.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmObjectActivity.PROPKEY_sample_topRows, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		final ActionListener sampleTopRowsCount_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = sampleTopRowsCount_txt.getText();
				int    intVal = CmObjectActivity.DEFAULT_sample_topRowsCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmObjectActivity.DEFAULT_sample_topRowsCount;
					SwingUtils.showWarnMessage(CmObjectActivityPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					sampleTopRowsCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmObjectActivity.PROPKEY_sample_topRowsCount, intVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		sampleTopRowsCount_txt.addActionListener(sampleTopRowsCount_action);
		sampleTopRowsCount_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleTopRowsCount_txt, so we don't have to duplicate code.
				sampleTopRowsCount_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		// LAYOUT
		panel.add(sampleRowCount_chk,     "wrap");
		
		panel.add(sampleTopRows_chk,      "split");
		panel.add(sampleTopRowsCount_txt, "wrap");

		return panel;
	}

	@Override
	public boolean ddlRequestInfo()
	{
		return true;
	}
	@Override
	public void ddlRequestInfoSave(JTable table)
	{
		if (table == null)
			return;

		if ( ! PersistentCounterHandler.hasInstance() )
			return;

		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

		int DBName_pos     = -1;
		int ObjectName_pos = -1;
//		int IndexID_pos    = -1;
		for (int c=0; c<table.getColumnCount(); c++)
		{
			if ( "DBName".equals(table.getColumnName(c)) )
				DBName_pos = c;

			if ( "ObjectName".equals(table.getColumnName(c)) )
				ObjectName_pos = c;

//			if ( "IndexID".equals(table.getColumnName(c)) )
//				IndexID_pos = c;

//			if (DBName_pos >= 0 && ObjectName_pos >= 0 && IndexID_pos >= 0)
//				break;
			if (DBName_pos >= 0 && ObjectName_pos >= 0)
				break;
		}

		// HOW MANY TOP ROWS SHOULD WE GRAB FROM THE JTABLE
		int NUM_OF_DDLS_TO_PERSIST = 10;
		
		int rows = Math.min(NUM_OF_DDLS_TO_PERSIST, table.getRowCount());
		for (int r=0; r<rows; r++)
		{
			Object DBName_obj     = table.getValueAt(r, DBName_pos);
			Object ObjectName_obj = table.getValueAt(r, ObjectName_pos);
//			Object IndexID_obj    = table.getValueAt(r, IndexID_pos);

			// Skip index rows... (change the loop to do this)
//			if (IndexID_obj instanceof Number)
//			{
//				if ( ((Number)IndexID_obj).intValue() > 0 )
//					continue;
//			}
			if (DBName_obj instanceof String && ObjectName_obj instanceof String)
				pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".guiSorted, row="+r);
		}
		
	}
}
