package com.asetune.test;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class BusyCusorTest extends JFrame
{
	JPanel	panel	= new JPanel();
	JButton	wait1	= new JButton("Wait 1/3 of a second");
	JButton	wait2	= new JButton("Wait 2/3 of a second");
	JButton	wait3	= new JButton("Wait 1 second");
	JButton	execWait	= new JButton("Exec Wait");
	JButton	startWait	= new JButton("Start Wait");
	JButton	stopWait	= new JButton("Stop Wait");

	public BusyCusorTest()
	{
		setTitle("Busy Cursor Test");
		setSize(400, 400);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		GridLayout layout = new GridLayout(3, 1);
		panel.setLayout(layout);
		panel.add(wait1);
		panel.add(wait2);
		panel.add(wait3);
		panel.add(execWait);
		panel.add(startWait);
		panel.add(stopWait);
		getContentPane().add(panel);

		ActionListener wait1ActionListener = delayActionListener(333);
		ActionListener wait2ActionListener = delayActionListener(666);
		ActionListener wait3ActionListener = delayActionListener(1000);

		// Add in the busy cursor
		ActionListener busy1ActionListener = CursorController.createListener(this, wait1ActionListener);
		ActionListener busy2ActionListener = CursorController.createListener(this, wait2ActionListener);
		ActionListener busy3ActionListener = CursorController.createListener(this, wait3ActionListener);

		wait1.addActionListener(busy1ActionListener);
		wait2.addActionListener(busy2ActionListener);
		wait3.addActionListener(busy3ActionListener);

		execWait.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
			    Frame.getFrames()[0].setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//			    panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				doProcessing();
				try {Thread.sleep(2000);}
				catch(InterruptedException ignore) {}
			    Frame.getFrames()[0].setCursor(Cursor.getDefaultCursor());
//				panel.setCursor(Cursor.getDefaultCursor());
			}
		});

		startWait.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
			    Frame.getFrames()[0].setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
		});

		stopWait.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
			    Frame.getFrames()[0].setCursor(Cursor.getDefaultCursor());
//				panel.setCursor(Cursor.getDefaultCursor());
			}
		});

		setVisible(true);
	}

	/**
	 * Creates an actionListener that waits for the specified number of
	 * milliseconds.
	 */
	private ActionListener delayActionListener(final int delay)
	{
		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				try
				{
					System.out.printf("Waiting for %d milliseconds\n", new Integer(delay));
					Thread.sleep(delay);
				}
				catch (InterruptedException ie)
				{
					ie.printStackTrace();
				}
			}
		};
		return listener;
	}

	public static class CursorController
	{
		public static final Cursor	busyCursor		= new Cursor(Cursor.WAIT_CURSOR);
		public static final Cursor	defaultCursor	= new Cursor(Cursor.DEFAULT_CURSOR);
		public static final int		delay			= 500; // in milliseconds

		private CursorController()
		{
		}

		public static ActionListener createListener(final Component component, final ActionListener mainActionListener)
		{
			ActionListener actionListener = new ActionListener()
			{
				public void actionPerformed(final ActionEvent ae)
				{
					TimerTask timerTask = new TimerTask()
					{
						public void run()
						{
							component.setCursor(busyCursor);
						}
					};
					Timer timer = new Timer();

					try
					{
						timer.schedule(timerTask, delay);
						mainActionListener.actionPerformed(ae);
					}
					finally
					{
						timer.cancel();
						component.setCursor(defaultCursor);
					}
				}
			};
			return actionListener;
		}
	}

	public static void main(String[] args)
	{
		BusyCusorTest cursorTest = new BusyCusorTest();
	}
}
