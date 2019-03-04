/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXTable;

import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CompletionPropertiesDialog
extends JDialog
implements ActionListener, FocusListener
{
	private static final long serialVersionUID = 1L;

	private JButton    _ok             = new JButton("OK");
	private JButton    _cancel         = new JButton("Cancel");

//	private JCheckBox            _cc_stat_chk      = new JCheckBox("Get Static Commands");
//	private JCheckBox            _cc_misc_chk      = new JCheckBox("Get Miscelanious");
//	private JCheckBox            _cc_db_chk        = new JCheckBox("Get Database Info");
//	private JCheckBox            _cc_tn_chk        = new JCheckBox("Get Table Name Info");
//	private JCheckBox            _cc_tc_chk        = new JCheckBox("Get Table Column Info");
//	private JCheckBox            _cc_pn_chk        = new JCheckBox("Get Procedure Info");
//	private JCheckBox            _cc_pp_chk        = new JCheckBox("Get Procedure Parameter Info");
//	private JCheckBox            _cc_spn_chk       = new JCheckBox("Get System Procedure Info");
//	private JCheckBox            _cc_spp_chk       = new JCheckBox("Get System Procedure Parameter Info");
	private JCheckBox            _cc_stat_chk      = new JCheckBox("<html>Static Commands                 - <i>Get Static Commands or Templates that can be used </i> <code><b>Ctrl+Space   </b><code/> </html>");
	private JCheckBox            _cc_misc_chk      = new JCheckBox("<html>Miscelanious                    - <i>Get Miscelanious Info, like ASE Monitoring tables </i> </html>");
	private JCheckBox            _cc_db_chk        = new JCheckBox("<html>Database Info                   - <i>Get Database Info, prev word is                   </i> <code><b>use          </b><code/> </html>");
	private JCheckBox            _cc_tn_chk        = new JCheckBox("<html>Table Name Info                 - <i>Get Table Name Info, use                          </i> <code><b>Ctrl+Space   </b><code/> </html>");
	private JCheckBox            _cc_tc_chk        = new JCheckBox("<html>Table Column Info               - <i>Get Table Column Info, current word start with    </i> <code><b>tableAlias.  </b><code/> </html>");
	private JCheckBox            _cc_snt_chk       = new JCheckBox("<html>Schemas that dosn't have tables - <i>Get Schema names that do not have any tables      </i> <code><b>:s           </b><code/> </html>");
	private JCheckBox            _cc_fn_chk        = new JCheckBox("<html>Function Name Info              - <i>Get Function Name Info, use                       </i> <code><b>Ctrl+Space   </b><code/> </html>");
	private JCheckBox            _cc_fc_chk        = new JCheckBox("<html>Function Column Info            - <i>Get Function Column Info, current word start with </i> <code><b>funcAlias.   </b><code/> </html>");
	private JCheckBox            _cc_pn_chk        = new JCheckBox("<html>Procedure Info                  - <i>Get Procedure Info, prev word is                  </i> <code><b>exec         </b><code/> </html>");
	private JCheckBox            _cc_pp_chk        = new JCheckBox("<html>Procedure Parameter Info        - <i>Get Procedure Parameter Info                      </i> </html>");
	private JCheckBox            _cc_spn_chk       = new JCheckBox("<html>System Procedure Info           - <i>Get System Procedure Info, prev word is           </i> <code><b>exec sp_     </b><code/> </html>");
	private JCheckBox            _cc_spp_chk       = new JCheckBox("<html>System Procedure Parameter Info - <i>Get System Procedure Parameter Info               </i> </html>");

	private JCheckBox            _serialize_chk    = new JCheckBox("If refresh takes longer than", false);
	private JTextField           _serialize_txt    = new JTextField(5);
	private JLabel               _serialize_lbl    = new JLabel("ms, save it to a file as a cache.");

	private JCheckBox            _saveQuestion_chk = new JCheckBox("Ask a question, before it's saved to file.", true);

	private JLabel               _vendor_lbl       = new JLabel("Connected to Vendor");
	private JTextField           _vendor_txt       = new JTextField("");

	private JLabel               _tableType_lbl    = new JLabel("What Table Types do you want to Include for below Vendor");
	private JXTable              _tableType_tab    = new JXTable();

	private CompletionProviderAbstract _completionProviderAbstract = null;

	
	private CompletionPropertiesDialog(Window owner, CompletionProviderAbstract completionProviderAbstract)
	{
		super(owner, "Code Completion Properties", ModalityType.APPLICATION_MODAL);

		_completionProviderAbstract = completionProviderAbstract;
		
		initComponents();
		pack();
	}

	/**
	 * 
	 * @param owner
	 * @param completionProviderAbstract
	 * @return return true if OK was pressed, else false
	 */
	public static void showDialog(Window owner, CompletionProviderAbstract completionProviderAbstract)
	{
		CompletionPropertiesDialog dialog = new CompletionPropertiesDialog(owner, completionProviderAbstract);
		dialog.setLocationRelativeTo(owner);
		dialog.setFocus();
		dialog.setVisible(true);
		dialog.dispose();
	}

	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());   // insets Top Left Bottom Right

		_cc_stat_chk.setToolTipText("<html>Get Static Commands or Templates that can be used <code><b>Ctrl+Space</b><code/></html>");
		_cc_misc_chk.setToolTipText("<html>Get Miscelanious Info, like ASE Monitoring tables</html>");
		_cc_db_chk  .setToolTipText("<html>Get Database Info, prev word is <code><b>use</b><code/></html>");
		_cc_tn_chk  .setToolTipText("<html>Get Table Name Info, use <code><b>Ctrl+Space</b><code/></html>");
		_cc_tc_chk  .setToolTipText("<html>Get Table Column Info, current word start with <code><b>tableAlias.</b><code/></html>");
		_cc_snt_chk .setToolTipText("<html>Get Schema names that do not have any tables</html>");
		_cc_fn_chk  .setToolTipText("<html>Get Function Name Info, use <code><b>Ctrl+Space</b><code/></html>");
		_cc_fc_chk  .setToolTipText("<html>Get Function Column Info, current word start with <code><b>functionAlias.</b><code/></html>");
		_cc_pn_chk  .setToolTipText("<html>Get Procedure Info, prev word is <code><b>exec</b><code/></html>");
		_cc_pp_chk  .setToolTipText("<html>Get Procedure Parameter Info</html>");
		_cc_spn_chk .setToolTipText("<html>Get System Procedure Info, prev word is <code><b>exec sp_</b><code/></html>");
		_cc_spp_chk .setToolTipText("<html>Get System Procedure Parameter Info</html>");

		_tableType_lbl.setToolTipText("What type of Tables does the DBMS Vendor support, and what types do you want to include in the Code Completion");
		_tableType_tab.setToolTipText(_tableType_lbl.getToolTipText());

		JPanel subPanel;

		subPanel = SwingUtils.createPanel("Generic stuff", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_serialize_chk,       "split");
		subPanel.add(_serialize_txt,       "");
		subPanel.add(_serialize_lbl,       "wrap");
		subPanel.add(_saveQuestion_chk,    "gapleft 20, wrap 15");
		
		subPanel.add(_cc_stat_chk,         "wrap");
		subPanel.add(_cc_misc_chk,         "wrap");
		panel.add(subPanel,                "growx, pushx, wrap");
		
		subPanel = SwingUtils.createPanel("DB or Catalog", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_cc_db_chk,           "wrap");
		panel.add(subPanel,                "growx, pushx, wrap");

		subPanel = SwingUtils.createPanel("Table", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_cc_tn_chk,           "wrap");
		subPanel.add(_cc_tc_chk,           "gapleft 20, wrap");
		subPanel.add(_cc_snt_chk,          "gapleft 20, wrap 10");
		subPanel.add(_tableType_lbl,       "wrap");
		subPanel.add(_vendor_lbl,          "split");
		subPanel.add(_vendor_txt,          "growx, pushx, wrap 5");
		subPanel.add(new JScrollPane(_tableType_tab), "height 50:120:n, push, grow, wrap");
		panel.add(subPanel,                "grow, push, wrap");

		subPanel = SwingUtils.createPanel("Function", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_cc_fn_chk,           "wrap");
		subPanel.add(_cc_fc_chk,           "gapleft 20, wrap");
		panel.add(subPanel,                "growx, pushx, wrap");

		subPanel = SwingUtils.createPanel("Procedure", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_cc_pn_chk,           "wrap");
		subPanel.add(_cc_pp_chk,           "gapleft 20, wrap");
		panel.add(subPanel,                "growx, pushx, wrap");

		subPanel = SwingUtils.createPanel("Sysytem Procedure", true, new MigLayout("insets 0 0, gap 0"));
		subPanel.add(_cc_spn_chk,          "wrap");
		subPanel.add(_cc_spp_chk,          "gapleft 20, wrap");
		panel.add(subPanel,                "growx, pushx, wrap");


		_vendor_txt.setEnabled(false);

//		JScrollPane scroll = new JScrollPane();
//		scroll.setViewportView(_tableType_tab);
		
		// ADD the OK, Cancel, Apply buttons
//		panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_cancel, "tag cancel,                   split, bottom");
		JPanel okCancel = new JPanel(new MigLayout());
		okCancel.add(new JLabel(), "pushx, growx");
		okCancel.add(_ok,          "tag ok");
		okCancel.add(_cancel,      "tag cancel");
		panel.add(okCancel, "pushx, growx, wrap");

		setContentPane(panel);

		// Fill in some start values
		_cc_stat_chk.setSelected(_completionProviderAbstract.isLookupStaticCmds());
		_cc_misc_chk.setSelected(_completionProviderAbstract.isLookupMisc());
		_cc_db_chk  .setSelected(_completionProviderAbstract.isLookupDb());
		_cc_tn_chk  .setSelected(_completionProviderAbstract.isLookupTableName());
		_cc_tc_chk  .setSelected(_completionProviderAbstract.isLookupTableColumns());
		_cc_snt_chk .setSelected(_completionProviderAbstract.isLookupSchemaWithNoTables());
		_cc_snt_chk .setSelected(_completionProviderAbstract.isLookupSchemaWithNoTables());
		_cc_fn_chk  .setSelected(_completionProviderAbstract.isLookupFunctionName());
		_cc_fc_chk  .setSelected(_completionProviderAbstract.isLookupFunctionColumns());
		_cc_pn_chk  .setSelected(_completionProviderAbstract.isLookupProcedureName());
		_cc_pp_chk  .setSelected(_completionProviderAbstract.isLookupProcedureColumns());
		_cc_spn_chk .setSelected(_completionProviderAbstract.isLookupSystemProcedureName());
		_cc_spp_chk .setSelected(_completionProviderAbstract.isLookupSystemProcedureColumns());

		_serialize_chk.setSelected(_completionProviderAbstract.isSaveCacheEnabled());
		_serialize_txt.setText    (_completionProviderAbstract.getSaveCacheTimeInMs()+"");

		_saveQuestion_chk.setSelected(_completionProviderAbstract.isSaveCacheQuestionEnabled());

		_tableType_tab.setModel(_completionProviderAbstract.getLookupTableTypesModel());
//System.out.println("CompletionPropertiesDialog: Columns="+_tableType_tab.getColumnCount()+", rows="+_tableType_tab.getRowCount()+".");

		_vendor_txt.setText(_completionProviderAbstract.getDbProductName());

		_tableType_tab.setSortable(true);
		_tableType_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_tableType_tab.packAll(); // set size so that all content in all cells are visible
		_tableType_tab.setColumnControlVisible(true);
		_tableType_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_tableType_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// ADD KEY listeners

		// ADD ACTIONS TO COMPONENTS
		_ok                   .addActionListener(this);
		_cancel               .addActionListener(this);

		// ADD Focus Listeners
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			_completionProviderAbstract.setLookupStaticCmds            (_cc_stat_chk.isSelected());
			_completionProviderAbstract.setLookupMisc                  (_cc_misc_chk.isSelected());
			_completionProviderAbstract.setLookupDb                    (_cc_db_chk  .isSelected());
			_completionProviderAbstract.setLookupTableName             (_cc_tn_chk  .isSelected());
			_completionProviderAbstract.setLookupTableColumns          (_cc_tc_chk  .isSelected());
			_completionProviderAbstract.setLookupSchemaWithNoTables    (_cc_snt_chk .isSelected());
			_completionProviderAbstract.setLookupFunctionName          (_cc_fn_chk  .isSelected());
			_completionProviderAbstract.setLookupFunctionColumns       (_cc_fc_chk  .isSelected());
			_completionProviderAbstract.setLookupProcedureName         (_cc_pn_chk  .isSelected());
			_completionProviderAbstract.setLookupProcedureColumns      (_cc_pp_chk  .isSelected());
			_completionProviderAbstract.setLookupSystemProcedureName   (_cc_spn_chk .isSelected());
			_completionProviderAbstract.setLookupSystemProcedureColumns(_cc_spp_chk .isSelected());

			_completionProviderAbstract.setSaveCacheEnabled            (_serialize_chk.isSelected());
			_completionProviderAbstract.setSaveCacheTimeInMs           (StringUtil.parseInt(_serialize_txt.getText(), CompletionProviderAbstract.DEFAULT_CODE_COMP_saveCacheTimeInMs));
			_completionProviderAbstract.setSaveCacheQuestionEnabled    (_saveQuestion_chk.isSelected());

			List<String> tableTypes = new ArrayList<String>();
			for (int r=0; r<_tableType_tab.getRowCount(); r++)
			{
				Boolean isSelected = (Boolean)_tableType_tab.getValueAt(r, 0);
				String  tableType  = (String) _tableType_tab.getValueAt(r, 1);
				
				if (isSelected)
					tableTypes.add(tableType);
			}
			// Well if everything is selected: then make it empty = all will be selected
			if (_tableType_tab.getRowCount() == tableTypes.size())
				tableTypes.clear();

			// Save the changes
			_completionProviderAbstract.setLookupTableTypes(tableTypes);

			// demand a refresh
			_completionProviderAbstract.setNeedRefresh(true);
			_completionProviderAbstract.setNeedRefreshSystemInfo(true);
			_completionProviderAbstract.clearSavedCache();
			
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
//		Object source = null;
//		if (e != null)
//			source = e.getSource();
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
				//_className_txt.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}
}






///*----------------------------------------------------------------------
//** BEGIN: Code Completions Option Button
//**----------------------------------------------------------------------*/ 
//private JPopupMenu createCodeCompletionOptionPopupMenu()
//{
//	// Do PopupMenu
//	final JPopupMenu popupMenu = new JPopupMenu();
//
//	final JMenu ttProvider_m = new JMenu("<html><b>ToolTip Provider</b> - <i><font color='green'>Hower over words in the editor to get help</font></i></html>");
//	
//	// Menu items for Code Completion
//	//---------------------------------------------------
////	JMenu codeCompl_m = new JMenu("<html><b>Code Completion/Assist</b> - <i><font color='green'>Use <code><b>Ctrl+Space</b></code> to get Code Completion</font></i></html>");
////	popupMenu.add(codeCompl_m);
//	
//	// When the Code Completion popup becoms visible, the menu are refreshed/recreated
////	final JPopupMenu codeComplPopupMenu = codeCompl_m.getPopupMenu();
//	popupMenu.addPopupMenuListener(new PopupMenuListener()
//	{
//		@Override
//		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//		{
//			// remove all old items (if any)
//			popupMenu.removeAll();
//
//			JMenuItem cc_exec_mi   = new JMenuItem("<html><b>Open</b>      - <i><font color='green'>Open the Code Completion window. Just like pressing <code><b>Ctrl+Space</b><code/></font></i></html>");
//			JMenuItem cc_reset_mi  = new JMenuItem("<html><b>Clear</b>     - <i><font color='green'>Clear the in memory cache for the Code Completion.</font></i></html>");
//			JMenuItem cc_config_mi = new JMenuItem("<html><b>Configure</b> - <i><font color='green'>Configure what types of objects should be fetched.</font></i></html>");
//
//			JMenuItem cc_stat_mi   = new JCheckBoxMenuItem("<html><b>Static Commands</b>                 - <i><font color='green'>Get Static Commands or Templates that can be used <code><b>Ctrl+Space</b><code/></font></i></html>", _compleationProviderAbstract.isLookupStaticCmds());
//			JMenuItem cc_misc_mi   = new JCheckBoxMenuItem("<html><b>Miscelanious</b>                    - <i><font color='green'>Get Miscelanious Info, like ASE Monitoring tables</font></i></html>",                                _compleationProviderAbstract.isLookupMisc());
//			JMenuItem cc_db_mi     = new JCheckBoxMenuItem("<html><b>Database Info</b>                   - <i><font color='green'>Get Database Info, prev word is <code><b>use</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupDb());
//			JMenuItem cc_tn_mi     = new JCheckBoxMenuItem("<html><b>Table Name Info</b>                 - <i><font color='green'>Get Table Name Info, use <code><b>Ctrl+Space</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupTableName());
//			JMenuItem cc_tc_mi     = new JCheckBoxMenuItem("<html><b>Table Column Info</b>               - <i><font color='green'>Get Table Column Info, current word start with <code><b>tableAlias.</b><code/></font></i></html>",   _compleationProviderAbstract.isLookupTableColumns());
//			JMenuItem cc_pn_mi     = new JCheckBoxMenuItem("<html><b>Procedure Info</b>                  - <i><font color='green'>Get Procedure Info, prev word is <code><b>exec</b><code/></font></i></html>",                        _compleationProviderAbstract.isLookupProcedureName());
//			JMenuItem cc_pp_mi     = new JCheckBoxMenuItem("<html><b>Procedure Parameter Info</b>        - <i><font color='green'>Get Procedure Parameter Info</font></i></html>",                                                     _compleationProviderAbstract.isLookupProcedureColumns());
//			JMenuItem cc_spn_mi    = new JCheckBoxMenuItem("<html><b>System Procedure Info</b>           - <i><font color='green'>Get System Procedure Info, prev word is <code><b>exec sp_</b><code/></font></i></html>",             _compleationProviderAbstract.isLookupSystemProcedureName());
//			JMenuItem cc_spp_mi    = new JCheckBoxMenuItem("<html><b>System Procedure Parameter Info</b> - <i><font color='green'>Get System Procedure Parameter Info</font></i></html>",                                              _compleationProviderAbstract.isLookupSystemProcedureColumns());
//
//			// exec/open action
//			cc_exec_mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(final ActionEvent e)
//				{
//					SwingUtilities.invokeLater( new Runnable()
//					{
//						@Override
//						public void run()
//						{
//							// hmmm, this doesnt seem to work...
//							//_query_txt.requestFocusInWindow();
//							//KeyEvent ctrlSpace = new KeyEvent(_query_txt, KeyEvent.KEY_TYPED, EventQueue.getMostRecentEventTime(), KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_SPACE, ' ');
//							//_query_txt.dispatchEvent(ctrlSpace);
//
//							// But this worked, but a bit ugly
//							_query_txt.requestFocusInWindow();
//							//ActionListener al = _query_txt.getActionForKeyStroke(AutoCompletion.getDefaultTriggerKey());
//							ActionListener al = _query_txt.getActionMap().get("AutoComplete");
//							al.actionPerformed(e);
//						}
//					});
//				}
//			});
//
//			// Reset action
//			cc_reset_mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					// mark code completion for refresh
//					if (_compleationProviderAbstract != null)
//					{
//						_compleationProviderAbstract.setNeedRefresh(true);
//						_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
//					}
//				}
//			});
//
//			// Configure action
//			cc_config_mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					_compleationProviderAbstract.configure();
//				}
//			});
//
//			// All other actions
//			cc_stat_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupStaticCmds            (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_misc_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupMisc                  (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_db_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupDb                    (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_tn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableName             (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_tc_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableColumns          (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_pn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureName         (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_pp_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureColumns      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_spn_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureName   (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//			cc_spp_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureColumns(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//
//			// Add it to the Code Completion popup menu
//			popupMenu.add(cc_exec_mi);
//			popupMenu.add(cc_reset_mi);
//			popupMenu.add(cc_config_mi);
//			popupMenu.add(new JSeparator());
//			popupMenu.add(cc_stat_mi);
//			popupMenu.add(cc_misc_mi);
//			popupMenu.add(cc_db_mi);
//			popupMenu.add(cc_tn_mi);
//			popupMenu.add(cc_tc_mi);
//			popupMenu.add(cc_pn_mi);
//			popupMenu.add(cc_pp_mi);
//			popupMenu.add(cc_spn_mi);
//			popupMenu.add(cc_spp_mi);
//			popupMenu.add(new JSeparator());
//			popupMenu.add(ttProvider_m);
//		}
//		@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//		@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
//	});
//
//	// Menu items for ToolTip Provider
//	//---------------------------------------------------
//	
//	// When the Code Completion popup becoms visible, the menu are refreshed/recreated
//	final JPopupMenu ttProviderPopupMenu = ttProvider_m.getPopupMenu();
//	ttProviderPopupMenu.addPopupMenuListener(new PopupMenuListener()
//	{
//		@Override
//		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//		{
//			// remove all old items (if any)
//			ttProviderPopupMenu.removeAll();
//
//			JMenuItem cc_reset_mi = new JMenuItem("<html><b>Clear</b> - <i><font color='green'>Clear the in memory cache for the Code Completion / ToolTip Provider.</font></i></html>");
//
//			JMenuItem cc_show_mi  = new JCheckBoxMenuItem("<html><b>Show Table/Column information</b> - <i><font color='green'>Show table/column information when mouse is over a table name</font></i></html>", (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getShowTableInformation() : ToolTipSupplierAbstract.DEFAULT_SHOW_TABLE_INFO);
////			JMenuItem cc_xxxx_mi  = new JCheckBoxMenuItem("<html><b>describeme</b>                    - <i><font color='green'>describeme</font></i></html>",                                                    (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getXXX() : ToolTipSupplierAbstract.DEFAULT_XXX);
//
//			// Reset action
//			cc_reset_mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					// mark code completion for refresh
//					if (_compleationProviderAbstract != null)
//					{
//						_compleationProviderAbstract.setNeedRefresh(true);
//						_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
//					}
//				}
//			});
//
//			// All other actions
//			cc_show_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setShowTableInformation(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
////			cc_xxxx_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setSomeMethodName      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//
//			// Add it to the Code Completion popup menu
//			ttProviderPopupMenu.add(cc_reset_mi);
//			ttProviderPopupMenu.add(new JSeparator());
//			ttProviderPopupMenu.add(cc_show_mi);
////			ttProviderPopupMenu.add(cc_xxxx_mi);
//		}
//		@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//		@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
//	});
//
//	return popupMenu;
//}
//
///**
// * Create a JButton that can enable/disable Application Executions Options
// * @param button A instance of JButton, if null is passed a new Jbutton will be created.
// * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
// * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
// */
//private JButton createCodeCompletionOptionButton(JButton button)
//{
//	if (button == null)
//		button = new JButton();
//
//	button.setIcon(SwingUtils.readImageIcon(Version.class, "images/code_completion.png"));
////	button.setText("Code Completion");
//	button.setToolTipText(
//		"<html>" +
//		"<h3>Code Completion</h3>" +
//		"Set various Options related to Code Completion/Assist<br>" +
//		"Use <b>Ctrl-Space</b> to activate Code Completion/Assist<br>" +
//		"<br>" +
//		"The second time you press <b>Ctrl-Space</b> the window will show <i>sql templates</i><br>" +
//		"<br>" +
//		"<b>Various Tips how it can be used</b>:<br>" +
//		"<ul>" +
//		"  <li><code>aaa</code><b>&lt;Ctrl-Space&gt;</b>                              - <i><font color='green'>Get list of tables/views/etc that starts with <b><code>aaa</code></b>   </font></i></li>" +
//		"  <li><code>select t.<b>&lt;Ctrl-Space&gt;</b> from tabname t</code>         - <i><font color='green'>Get column names for the table aliased as <b><code>t</code></b>   </font></i></li>" +
//		"  <li><code>select * from tabname t where t.<b>&lt;Ctrl-Space&gt;</b></code> - <i><font color='green'>Get column names for the table aliased as <b><code>t</code></b>   </font></i></li>" +
//		"  <li><code>exec</code> <b>&lt;Ctrl-Space&gt;</b>                            - <i><font color='green'>Get stored procedures   </font></i></li>" +
//		"  <li><code>:s</code><b>&lt;Ctrl-Space&gt;</b>                               - <i><font color='green'>Get schemas   </font></i></li>" +
//		"  <li><code>use </code><b>&lt;Ctrl-Space&gt;</b>                             - <i><font color='green'>Get list of databases/catalogs  </font></i></li>" +
//		"</ul>" +
//		"</html>");
//
//	JPopupMenu popupMenu = createCodeCompletionOptionPopupMenu();
//	button.setComponentPopupMenu(popupMenu);
//
//	// If we click on the button, display the popup menu
//	button.addActionListener(new ActionListener()
//	{
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			Object source = e.getSource();
//			if (source instanceof JButton)
//			{
//				JButton but = (JButton)source;
//				JPopupMenu pm = but.getComponentPopupMenu();
//				pm.show(but, 14, 14);
//				pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
//			}
//		}
//	});
//	
//	return button;
//}
///*----------------------------------------------------------------------
//** END: Code Completions Option Button
//**----------------------------------------------------------------------*/ 
//
