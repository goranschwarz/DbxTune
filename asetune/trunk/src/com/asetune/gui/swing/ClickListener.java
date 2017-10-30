package com.asetune.gui.swing;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ClickListener extends MouseAdapter 
implements ActionListener
{
	// 500 is the default on Windows, so if the value isn't found, use that
	private final static int clickInterval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") == null ? 500 : (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");

	MouseEvent lastEvent;
	Timer      timer;

	public ClickListener()
	{
		this(clickInterval);
	}

	public ClickListener(int delay)
	{
		timer = new Timer(delay, this);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (SwingUtilities.isLeftMouseButton(e)) 
		{
			if ( e.getClickCount() > 2 )
				return;
			
			MouseEvent prev = lastEvent;
			lastEvent = e;
			
			if ( timer.isRunning() )
			{
				// Check that the "double click" click is in close proximity with the previous click
				if (prev != null)
				{
					double distance = prev.getPoint().distance( e.getPoint() );
					//System.out.println("distance="+distance);
					if (distance > 10)
					{
						// restart the timer, or make it a single click...
						timer.restart();
						return;
					}
				}

				timer.stop();
				doubleClick(lastEvent);
			}
			else
			{
				timer.restart();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		timer.stop();
		singleClick(lastEvent);
	}

	public void singleClick(MouseEvent e)
	{
	}

	public void doubleClick(MouseEvent e)
	{
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("Double Click Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addMouseListener(new ClickListener()
		{
			@Override
			public void singleClick(MouseEvent e)
			{
				System.out.println("single");
			}

			@Override
			public void doubleClick(MouseEvent e)
			{
				System.out.println("double");
			}
		});
		frame.setSize(200, 200);
		frame.setVisible(true);
	}
}
