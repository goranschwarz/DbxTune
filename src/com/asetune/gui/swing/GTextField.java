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
package com.asetune.gui.swing;

import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.Document;

import org.fife.rsta.ui.ContentAssistable;
import org.fife.rsta.ui.search.AbstractSearchDialog;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

import com.asetune.gui.focusabletip.FocusableTip;

/**
 * Same as JTextField, but also implements
 * <ul>
 *   <li>Focusable Tool tip</li>
 *   <li>Auto Completion, by ctrl+space</li>
 * </ul>
 *
 */
public class GTextField 
extends JTextField
implements ContentAssistable // Not sure how this is used.
{
	private static final long serialVersionUID = 1L;

	public static final String FOCUSABLE_TIPS_PROPERTY				= "RSTA.focusableTips";

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean _useFocusableTips = true;

	/** The last focusable tip displayed. */
	private FocusableTip _focusableTip = null;

	/** Whether content assist is enabled. */
	private boolean			   _acIsEnabled;

	/** The auto-completion instance for this text field. */
	private AutoCompletion	   _ac;

	/** Provides the completions for this text field. */
	private CompletionProvider _provider;


	//--------------------------------------------------------
	// BEGIN: constructors
	//--------------------------------------------------------
	
	public GTextField()
	{
		super();
		init();
	}

	public GTextField(Document paramDocument, String paramString, int paramInt)
	{
		super(paramDocument, paramString, paramInt);
		init();
	}

	public GTextField(int paramInt)
	{
		super(paramInt);
		init();
	}

	public GTextField(String paramString, int paramInt)
	{
		super(paramString, paramInt);
		init();
	}

	public GTextField(String paramString)
	{
		super(paramString);
		init();
	}


	/**
	 * Called by constructors to initialize common properties
	 */
	protected void init() 
	{
//		ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
//		toolTipManager.registerComponent(this);
		
		setUseFocusableTips(true);
	}
	//--------------------------------------------------------
	// END: constructors
	//--------------------------------------------------------

	/**
	 * Returns whether "focusable" tool tips are used instead of standard
	 * ones.  Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and click links in.
	 *
	 * @return Whether to use focusable tool tips.
	 * @see #setUseFocusableTips(boolean)
	 * @see FocusableTip
	 */
	public boolean getUseFocusableTips() 
	{
		return _useFocusableTips;
	}

	/**
	 * Sets whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and clink links in.
	 *
	 * @param use Whether to use focusable tool tips.
	 * @see #getUseFocusableTips()
	 * @see FocusableTip
	 */
	public void setUseFocusableTips(boolean use) 
	{
		if (use != _useFocusableTips) 
		{
			_useFocusableTips = use;
			firePropertyChange(FOCUSABLE_TIPS_PROPERTY, !use, use);
		}
	}

	/**
	 * Returns the tool tip to display for a mouse event at the given
	 * location.  This method is overridden to give a registered parser a
	 * chance to display a tool tip (such as an error description when the
	 * mouse is over an error highlight).
	 *
	 * @param e The mouse event.
	 */
	@Override
	public String getToolTipText(MouseEvent e) 
	{
		// Check parsers for tool tips first.
		String text = super.getToolTipText(e);

		// Do we want to use "focusable" tips?
		if (getUseFocusableTips()) 
		{
			if (text != null) 
			{
				if (_focusableTip == null) 
					_focusableTip = new FocusableTip(this);

				_focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text; // Standard tool tips
	}
	@Override
	public String getToolTipText() 
	{
		return super.getToolTipText();
	}
	@Override
	public void setToolTipText(String text)
	{
		super.setToolTipText(text);
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	// BEGIN - AUTO COMPLETE
	//////////////////////////////////////////////////////////////////////////
	/**
	 * Adds the completions known to this text field.
	 *
	 * @param provider
	 *            The completion provider to add to.
	 */
	public void addCompletions(CompletionProvider provider)
	{
	}

	/**
	 * Adds the completion text known to this text field.
	 */
	public void addCompletion(Completion completion)
	{
		setAutoCompleteEnabled(true);
		
		if (_provider instanceof DefaultCompletionProvider)
		{
			DefaultCompletionProvider p = (DefaultCompletionProvider) _provider;
			p.addCompletion( completion );
		}
		else
		{
			throw new RuntimeException("Installed Completion Provider is not based on DefaultCompletionProvider.");
		}
	}

	/**
	 * Adds the completion text known to this text field.
	 */
	public void addCompletion(String text)
	{
//System.out.println("GTextField.addCompletion(): text='"+text+"'.");
		setAutoCompleteEnabled(true);
		
		if (_provider instanceof DefaultCompletionProvider)
		{
			DefaultCompletionProvider p = (DefaultCompletionProvider) _provider;
			p.addCompletion( new BasicCompletion(_provider, text) );
		}
		else
		{
			throw new RuntimeException("Installed Completion Provider is not based on DefaultCompletionProvider.");
		}
	}

	public void addCompletion(final JTable table)
	{
//System.out.println("GTextField.addCompletion()");
		setAutoCompleteEnabled(true);

		for (int c=0; c<table.getColumnCount(); c++)
		{
			String colName = table.getColumnName(c);
			if (colName.contains(" "))
				colName = "[" + colName + "]";
			addCompletion( colName );
		}
		
//		table.addPropertyChangeListener(new PropertyChangeListener()
//		{
//			@Override
//			public void propertyChange(PropertyChangeEvent evt)
//			{
//				if ("model".equals(evt.getPropertyName()) && evt.getNewValue() != null && !evt.getNewValue().equals(evt.getOldValue()))
//				{
//					System.out.println("GTextField(table).propertyChange(): tableName='"+table.getName()+"', getPropertyName()="+evt.getPropertyName());
//					refreshCompletion(table);
//				}
//			}
//		});
	}

	public void refreshCompletion(JTable table)
	{
//System.out.println("GTextField.refreshCompletion()");
		if (_provider instanceof DefaultCompletionProvider)
		{
			DefaultCompletionProvider p = (DefaultCompletionProvider) _provider;
			p.clear();
		}
		else
		{
			throw new RuntimeException("Installed Completion Provider is not based on DefaultCompletionProvider.");
		}

		addCompletion(table);
	}



	/**
	 * Lazily creates the AutoCompletion instance this text field uses.
	 *
	 * @return The auto-completion instance.
	 */
	private AutoCompletion getAutoCompletion()
	{
		if ( _ac == null )
		{
			_ac = new AutoCompletion(getCompletionProvider());
		}
		return _ac;
	}

	/**
	 * Creates the shared completion provider instance.
	 *
	 * @return The completion provider.
	 */
	protected synchronized CompletionProvider getCompletionProvider()
	{
		if ( _provider == null )
		{
			_provider = new DefaultCompletionProvider();
			addCompletions(_provider);
		}
		return _provider;
	}

	/**
	 * Returns whether auto-complete is enabled.
	 *
	 * @return Whether auto-complete is enabled.
	 * @see #setAutoCompleteEnabled(boolean)
	 */
	public boolean isAutoCompleteEnabled()
	{
		return _acIsEnabled;
	}

	/**
	 * Toggles whether regex auto-complete is enabled. This method will fire a
	 * property change event of type {@link ContentAssistable#ASSISTANCE_IMAGE}.
	 * 
	 * @param enabled
	 *            Whether regex auto complete should be enabled.
	 * @see #isAutoCompleteEnabled()
	 */
	public void setAutoCompleteEnabled(boolean enabled)
	{
		if ( this._acIsEnabled != enabled )
		{
			this._acIsEnabled = enabled;
			if ( enabled )
			{
				AutoCompletion ac = getAutoCompletion();
				ac.install(this);
			}
			else
			{
				_ac.uninstall();
			}
			String prop = ContentAssistable.ASSISTANCE_IMAGE;
			// Must take care how we fire the property event, as Swing
			// property change support won't fire a notice if old and new are
			// both non-null and old.equals(new).
			if ( enabled )
			{
				firePropertyChange(prop, null, AbstractSearchDialog.getContentAssistImage());
			}
			else
			{
				firePropertyChange(prop, null, null);
			}
		}
	}
	//////////////////////////////////////////////////////////////////////////
	// END - AUTO COMPLETE
	//////////////////////////////////////////////////////////////////////////

}
