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
package com.dbxtune.gui.swing;

import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;

/**
 * This is licensed under LGPL.  License can be found here:  http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * This is provided as is.  If you have questions please direct them to charlie.hubbard at gmail dot you know what.
 */
public class DateTimePicker extends JXDatePicker 
{
	private static final long serialVersionUID = 1L;

	private JSpinner timeSpinner;
    private JPanel timePanel;
    private DateFormat timeFormat;

    public DateTimePicker() {
        super();
        getMonthView().setSelectionModel(new SingleDaySelectionModel());
    }

    public DateTimePicker( Date d ) {
        this();
        setDate(d);
    }

    @Override
	public void commitEdit() throws ParseException {
        commitTime();
        super.commitEdit();
    }

    @Override
	public void cancelEdit() {
        super.cancelEdit();
        setTimeSpinners();
    }

    @Override
    public JPanel getLinkPanel() {
        super.getLinkPanel();
        if( timePanel == null ) {
            timePanel = createTimePanel();
        }
        setTimeSpinners();
        return timePanel;
    }

    private JPanel createTimePanel() {
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new FlowLayout());
        //newPanel.add(panelOriginal);

        SpinnerDateModel dateModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(dateModel);
        if( timeFormat == null ) timeFormat = DateFormat.getTimeInstance( DateFormat.SHORT );
        updateTextFieldFormat();
        newPanel.add(new JLabel( "Time:" ) );
        newPanel.add(timeSpinner);
        newPanel.setBackground(Color.WHITE);
        return newPanel;
    }

    private void updateTextFieldFormat() {
        if( timeSpinner == null ) return;
        JFormattedTextField tf = ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter) factory.getDefaultFormatter();
        // Change the date format to only show the hours
        formatter.setFormat( timeFormat );
    }

    private void commitTime() {
        Date date = getDate();
        if (date != null) {
            Date time = (Date) timeSpinner.getValue();
            GregorianCalendar timeCalendar = new GregorianCalendar();
            timeCalendar.setTime( time );

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get( Calendar.HOUR_OF_DAY ) );
            calendar.set(Calendar.MINUTE, timeCalendar.get( Calendar.MINUTE ) );
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Date newDate = calendar.getTime();
            setDate(newDate);
        }

    }

    private void setTimeSpinners() {
        Date date = getDate();
        if (date != null) {
            timeSpinner.setValue( date );
        }
    }

    public DateFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(DateFormat timeFormat) {
        this.timeFormat = timeFormat;
        updateTextFieldFormat();
    }

    public static void main(String[] args) {
        Date date = new Date();
        JFrame frame = new JFrame();
        frame.setTitle("Date Time Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DateTimePicker dateTimePicker = new DateTimePicker();
        dateTimePicker.setFormats( DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM ) );
        dateTimePicker.setTimeFormat( DateFormat.getTimeInstance( DateFormat.MEDIUM ) );

        dateTimePicker.setDate(date);

        frame.getContentPane().add(dateTimePicker);
        frame.pack();
        frame.setVisible(true);
    }

	public void setText(String str)
	{
		if (str == null) return;
		if (str.trim().equals("")) return;

//		try
//		{
//			Timestamp ts = Timestamp.valueOf(str);
//			setDate( new Date(ts.getTime()) );
//		}
//		catch(IllegalArgumentException ex)
//		{
//			System.out.println(this.getClass().getName() + ": setText(str='"+str+"'). Caught: "+ex);
//		}
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			Date date = sdf.parse(str);
			setDate( date );
		}
		catch(ParseException ex)
		{
			System.out.println(this.getClass().getName() + ": setText(str='"+str+"'). Caught: "+ex);
		}
	}

	public String getText()
	{
		Date date = getDate();
		if (date == null)
			return "";

		Timestamp ts = new Timestamp(date.getTime());
		return ts.toString();
	}
}
