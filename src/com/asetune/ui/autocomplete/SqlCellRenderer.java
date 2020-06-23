/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;

import com.asetune.Version;
import com.asetune.ui.autocomplete.completions.BasicCompletionX;
import com.asetune.ui.autocomplete.completions.CompletionTemplate;
import com.asetune.ui.autocomplete.completions.SavedCacheCompletionWarning;
import com.asetune.ui.autocomplete.completions.SqlColumnCompletion;
import com.asetune.ui.autocomplete.completions.SqlDbCompletion;
import com.asetune.ui.autocomplete.completions.SqlProcedureCompletion;
import com.asetune.ui.autocomplete.completions.SqlSchemaCompletion;
import com.asetune.ui.autocomplete.completions.SqlTableCompletion;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierCompletion;
import com.asetune.utils.SwingUtils;

public class SqlCellRenderer
extends DefaultListCellRenderer
{
	private static final long serialVersionUID = 1L;

	public final static ImageIcon ICON_UNKNOWN_TYPE     = SwingUtils.readImageIcon(Version.class, "images/cc_unknown_type.png");

	public final static ImageIcon ICON_CC_SAVE_WARNING  = SwingUtils.readImageIcon(Version.class, "images/cc_save_warning.png");

	public final static ImageIcon ICON_TEMPLATE         = SwingUtils.readImageIcon(Version.class, "images/cc_template.png");

	public final static ImageIcon ICON_DATABASE         = SwingUtils.readImageIcon(Version.class, "images/cc_database.png");
	public final static ImageIcon ICON_SCHEMA           = SwingUtils.readImageIcon(Version.class, "images/cc_schema.png");
	public final static ImageIcon ICON_SYSTEM_TABLE     = SwingUtils.readImageIcon(Version.class, "images/cc_system_table.png");
	public final static ImageIcon ICON_MDA_TABLE        = SwingUtils.readImageIcon(Version.class, "images/cc_mda_table.png");
	public final static ImageIcon ICON_TABLE            = SwingUtils.readImageIcon(Version.class, "images/cc_table.png");
	public final static ImageIcon ICON_GLOAB_TEMP_TABLE = SwingUtils.readImageIcon(Version.class, "images/cc_global_temp_table.png");
	public final static ImageIcon ICON_LOCAL_TEMP_TABLE = SwingUtils.readImageIcon(Version.class, "images/cc_local_temp_table.png");
	public final static ImageIcon ICON_COLUMN           = SwingUtils.readImageIcon(Version.class, "images/cc_column.png");
	public final static ImageIcon ICON_INDEX            = SwingUtils.readImageIcon(Version.class, "images/cc_index.png");
	public final static ImageIcon ICON_TRIGGER          = SwingUtils.readImageIcon(Version.class, "images/cc_trigger.png");
	public final static ImageIcon ICON_PARTITION        = SwingUtils.readImageIcon(Version.class, "images/cc_partition.png");
	public final static ImageIcon ICON_CONSTRAINT       = SwingUtils.readImageIcon(Version.class, "images/cc_constraint.png");
	public final static ImageIcon ICON_CONSTRAINT_REF   = SwingUtils.readImageIcon(Version.class, "images/cc_constraint_by_reference.png");
	public final static ImageIcon ICON_VIEW             = SwingUtils.readImageIcon(Version.class, "images/cc_view.png");
	public final static ImageIcon ICON_SYNONYM          = SwingUtils.readImageIcon(Version.class, "images/cc_synonym.png");
	public final static ImageIcon ICON_ALIAS            = SwingUtils.readImageIcon(Version.class, "images/cc_alias.png");
	public final static ImageIcon ICON_SEQUENCE         = SwingUtils.readImageIcon(Version.class, "images/cc_sequence.png");
	public final static ImageIcon ICON_PRIMARY_KEY      = SwingUtils.readImageIcon(Version.class, "images/cc_primary_key.png");
	public final static ImageIcon ICON_FUNCTION         = SwingUtils.readImageIcon(Version.class, "images/cc_function.png");
	public final static ImageIcon ICON_PROCEDURE        = SwingUtils.readImageIcon(Version.class, "images/cc_procedure.png");
	public final static ImageIcon ICON_SYSTEM_PROCEDURE = SwingUtils.readImageIcon(Version.class, "images/cc_system_procedure.png");
	public final static ImageIcon ICON_CODE_COMPLETION  = SwingUtils.readImageIcon(Version.class, "images/cc_code_completion.png");


	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof BasicCompletion && component instanceof JLabel)
		{
			JLabel           label = (JLabel)component;
			BasicCompletion  bc    = (BasicCompletion)value;
			
			Icon icon = getIcon(bc);
			if (icon != null)
				label.setIcon(icon);
		}

		if (value instanceof BasicCompletionX && component instanceof JLabel)
		{
			JLabel           label = (JLabel)component;
			BasicCompletionX bc    = (BasicCompletionX)value;
			
			Icon icon = getIcon(bc);
			if (icon != null)
				label.setIcon(icon);
		}
		return component;
	}
	
	public Icon getIcon(Completion completion)
	{
		if (completion instanceof SqlSchemaCompletion) 
		{
			return ICON_SCHEMA;
		}
		else if (completion instanceof SqlDbCompletion) 
		{
			return ICON_DATABASE;
		}
		else if (completion instanceof SqlTableCompletion)
		{
			SqlTableCompletion info = (SqlTableCompletion) completion;
			
			if      (info.getType().equalsIgnoreCase("MDA Table"))        return ICON_MDA_TABLE;
			else if (info.getType().equalsIgnoreCase("SYNONYM"))          return ICON_SYNONYM;
			else if (info.getType().equalsIgnoreCase("TABLE"))            return ICON_TABLE;
			else if (info.getType().equalsIgnoreCase("SYSTEM TABLE"))     return ICON_SYSTEM_TABLE;
			else if (info.getType().equalsIgnoreCase("VIEW"))             return ICON_VIEW;
			else if (info.getType().equalsIgnoreCase("ALIAS"))            return ICON_ALIAS;
			else if (info.getType().equalsIgnoreCase("SYNONYME"))         return ICON_SYNONYM;
			else if (info.getType().equalsIgnoreCase("LOCAL TEMPORARY"))  return ICON_LOCAL_TEMP_TABLE;
			else if (info.getType().equalsIgnoreCase("GLOBAL TEMPORARY")) return ICON_GLOAB_TEMP_TABLE;

			// HANA Specifics
			else if (info.getType().equalsIgnoreCase("OLAP VIEW"))        return ICON_UNKNOWN_TYPE;
			else if (info.getType().equalsIgnoreCase("JOIN VIEW"))        return ICON_UNKNOWN_TYPE;
			else if (info.getType().equalsIgnoreCase("CALC VIEW"))        return ICON_UNKNOWN_TYPE;
			else if (info.getType().equalsIgnoreCase("HIERARCHY VIEW"))   return ICON_UNKNOWN_TYPE;
			else if (info.getType().equalsIgnoreCase("USER DEFINED"))     return ICON_UNKNOWN_TYPE;

			else return ICON_UNKNOWN_TYPE;
		}
		else if (completion instanceof SqlProcedureCompletion)
		{
			SqlProcedureCompletion info = (SqlProcedureCompletion) completion;

			if      (info.getName().startsWith("sp_"))                    return ICON_SYSTEM_PROCEDURE;
			else if (info.getType().startsWith("Procedure") && info.getRemark().equalsIgnoreCase("Packaged function")) return ICON_FUNCTION;
			else if (info.getType().equalsIgnoreCase("PROCEDURE"))        return ICON_PROCEDURE;
			else if (info.getType().startsWith      ("Procedure"))        return ICON_PROCEDURE;
			else if (info.getType().equalsIgnoreCase("FUNCTION"))         return ICON_FUNCTION;
			else if (info.getType().startsWith      ("Function"))         return ICON_FUNCTION;
			else if (info.getType().equalsIgnoreCase("SYSTEM PROCEDURE")) return ICON_SYSTEM_PROCEDURE;
			else return ICON_UNKNOWN_TYPE;

		}
		else if (completion instanceof ToolTipSupplierCompletion)
		{
			return ICON_CODE_COMPLETION;
		}
		else if (completion instanceof SqlColumnCompletion)
		{
			return ICON_COLUMN;
		}
//		else if (completion instanceof TemplateCompletion)
//		{
//			return ICON_TEMPLATE;
//		}
		else if (completion instanceof SavedCacheCompletionWarning)
		{
			return ICON_CC_SAVE_WARNING;
		}
		else if (completion instanceof CompletionTemplate.ShorthandCompletionHtml)
		{
			CompletionTemplate.ShorthandCompletionHtml ct = (CompletionTemplate.ShorthandCompletionHtml) completion;

			Icon icon = ct.getIcon();
			if ( icon != null )
				return icon;

			return ICON_TEMPLATE;
		}

		if (completion.getIcon() != null)
			return completion.getIcon();

		return ICON_TEMPLATE;
//		return ICON_UNKNOWN_TYPE;
	}
}
