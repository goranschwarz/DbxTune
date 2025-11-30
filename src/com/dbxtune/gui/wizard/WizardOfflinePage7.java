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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.netbeans.spi.wizard.WizardPage;

import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.pcs.PersistWriterBase;

import net.miginfocom.swing.MigLayout;


//PAGE 4
public class WizardOfflinePage7
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "sample-time";
	private static final String WIZ_DESC = "Sample time";
//	private static final String WIZ_HELP = "How long should we sleep between samples.\nThis is specified in seconds.";

	private JTextField _sampleTime           = new JTextField("60");
	private JTextField _startRecordingAtTime = new JTextField("");
	private JTextField _shutdownAfterXHours  = new JTextField("");

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage7()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_sampleTime          .setName("sampleTime");
		_startRecordingAtTime.setName("startRecordingAtTime");
		_shutdownAfterXHours .setName("shutdownAfterXHours");

		// SLEEP TIME
		String label = "<html>How long should we sleep between samples. Specified in seconds.</html>";

		add( new MultiLineLabel(label), "span, wrap 10" );
		add(new JLabel("Sample time"));
		add(_sampleTime, "growx");
		add(new JLabel("Seconds"), "wrap 40");

		// START
		label = "<html>Start recording at a specific time. Format is <code>HH[:MM]</code></html>";

		add( new MultiLineLabel(label), "span, wrap 10" );
		add(new JLabel("Start the recording at"));
		add(_startRecordingAtTime, "growx");
		add(new JLabel("Hour[:Minute]"), "wrap 40");

		// STOP
		label = "<html>Shutdown or stop the no-gui process after X number after it has been started.</html>";

		add( new MultiLineLabel(label), "span, wrap 10" );
		add(new JLabel("Shutdown after # hours"));
		add(_shutdownAfterXHours, "growx");
		add(new JLabel("Hours"), "wrap");

		// Command line switches
		String cmdLineSwitched = 
			"<html>" +
			"The above options can be overridden or specified using the following command line switches" +
			"<table>" +
			"<tr><code>-i,--interval &lt;seconds&gt;</code><td></td>sample Interval, time between samples.</tr>" +
			"<tr><code>-e,--enable &lt;hh[:mm]&gt;  </code><td></td>enable/start the recording at Hour(00-23) Minute(00-59)</tr>" +
			"<tr><code>-f,--finish &lt;hh[:mm]&gt;  </code><td></td>shutdown/stop the no-gui service after # hours.</tr>" +
			"</table>" +
			"</html>";
		add( new JLabel(""), "span, wrap 30" );
		add( new MultiLineLabel(cmdLineSwitched), "span, wrap" );

		initData();
	}

	private void initData()
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
		if ( _sampleTime.getText().trim().length() <= 0) problem += "Sample time, ";

		// Check if it's a integer
		if (_sampleTime.getText().trim().length() > 0)
		{
			try { Integer.parseInt( _sampleTime.getText().trim() ); }
			catch (NumberFormatException e)
			{
				return "Sample time needs to be a number.";
			}
		}
		
		// Check _startRecordingAtTime
		if (_startRecordingAtTime.getText().trim().length() > 0)
		{
			try 
			{
				PersistWriterBase.getRecordingStartTime(_startRecordingAtTime.getText().trim());
			} 
			catch (Exception e) 
			{
				return "'Start the recording at' needs to be in format hh[:mm] "+e.getMessage();
			}
		}

		// Check if _shutdownAfterXHours is integer
		if (_shutdownAfterXHours.getText().trim().length() > 0)
		{
			try 
			{
				PersistWriterBase.getRecordingStopTime(null, _shutdownAfterXHours.getText().trim());
			} 
			catch (Exception e) 
			{
				return "'Shutdown after # hours' needs to be in format hh[:mm] "+e.getMessage();
			}
		}
		
		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		
		return problem.length() == 0 ? null : "Following fields can't be empty: "+problem;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);
	}
}

