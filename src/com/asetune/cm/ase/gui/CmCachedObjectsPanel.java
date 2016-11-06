package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmCachedObjects;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmCachedObjectsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmCachedObjectsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmCachedObjects.CM_NAME;

	public CmCachedObjectsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// ORANGE = Index id > 0
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

		// BLOB (text/image columns)
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blob");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() == 255)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorConstants.COLOR_DATATYPE_BLOB), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
		int     defaultIntOpt;

		// Top Rows (top #)
		defaultOpt    = conf == null ? CmCachedObjects.DEFAULT_sample_topRows      : conf.getBooleanProperty(CmCachedObjects.PROPKEY_sample_topRows,      CmCachedObjects.DEFAULT_sample_topRows);
		defaultIntOpt = conf == null ? CmCachedObjects.DEFAULT_sample_topRowsCount : conf.getIntProperty    (CmCachedObjects.PROPKEY_sample_topRowsCount, CmCachedObjects.DEFAULT_sample_topRowsCount);
		final JCheckBox  sampleTopRows_chk      = new JCheckBox("Limit number of rows (top #)", defaultOpt);
		final JTextField sampleTopRowsCount_txt = new JTextField(Integer.toString(defaultIntOpt), 5);

		sampleTopRows_chk.setName(CmCachedObjects.PROPKEY_sample_topRows);
		sampleTopRows_chk.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmCachedObjects.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRowsCount_txt.setName(CmCachedObjects.PROPKEY_sample_topRowsCount);
		sampleTopRowsCount_txt.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmCachedObjects.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		sampleTopRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmCachedObjects.PROPKEY_sample_topRows, ((JCheckBox)e.getSource()).isSelected());
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
				int    intVal = CmCachedObjects.DEFAULT_sample_topRowsCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmCachedObjects.DEFAULT_sample_topRowsCount;
					SwingUtils.showWarnMessage(CmCachedObjectsPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					sampleTopRowsCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmCachedObjects.PROPKEY_sample_topRowsCount, intVal);
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
		panel.add(sampleTopRows_chk,      "split");
		panel.add(sampleTopRowsCount_txt, "wrap");

		return panel;
	}
}
