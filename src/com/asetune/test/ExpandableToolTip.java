package com.asetune.test;

/**
 * Copyright (2008)
 * Denis Bauer
 * 
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


/**
 * Generates an expandable ToolTip, which is a popup dispaying a short description
 * of the component (ToolTip) but also, when focused, allows for displaying a 
 * longer help text
 * @author d.bauer
 *
 */
public class ExpandableToolTip  implements KeyListener, MouseListener, FocusListener, HyperlinkListener {
	JFrame popup;				// PopUp
	JPanel toolTip;				// panel holding the tooltip and information how to expand
	JScrollPane help;			// panel holding the help text
	JEditorPane h;				// editor containing the help text (hyperlinks)
	JComponent owner;			// JCompontent the ToolTip was attached to
	PopupFactory factory;		// factory for generating the different popUps
	int x;						// X location for popUp
	int y;						// Y location for popUp
	boolean helpActive=false;	// switch indicating if the interactiv help popUp is active
	Thread t;					// sleeping thread 
	
	int WIDTH_HTML=250;			// width of table in the HTML code
	int WIDTH_SC=300;			// associated width of the help window
	int HEIGHT_SC=200;			// height of the help window
	int WIDTH_TT=300;			// width of the toolTip window
	int HEIGHT_TT=50;			// height of the toolTip window
	/**
	 * Generates the two display panels that are shown
	 * @param toolTipText
	 * @param helpText
	 * @param owner
	 */
	public ExpandableToolTip(String toolTipText, String helpText,JComponent owner){
		this.owner = owner;
		
		/* Attach mouseListener to component.
		 * If we attach the toolTip to a JComboBox our MouseListener is not
		 * used, we therefore need to attach the MouseListener to each 
		 * component in the JComboBox
		 */
		if(owner instanceof JComboBox){
			for (int i=0; i<owner.getComponentCount(); i++) {
				owner.getComponent(i).addMouseListener(this);
			}
		}
		else{
			owner.addMouseListener(this) ;
		}
		
		/* generate toolTip panel */
		toolTip = new JPanel(new GridLayout(3,1));
		toolTip.setPreferredSize(new Dimension(WIDTH_TT, HEIGHT_TT));
		toolTip.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		toolTip.setBackground(Color.getHSBColor(15, 3, 99));
		toolTip.add(new JLabel(toolTipText));
		toolTip.add(new JSeparator(SwingConstants.HORIZONTAL));
		JLabel more = new JLabel("press 'd' for more details");
		more.setForeground(Color.DARK_GRAY);
		more.setFont(new Font(null,1,10));
		toolTip.add(more);
		
		/* generate help panel */
		JPanel helpContent = new JPanel();
		helpContent.setBackground(Color.WHITE);
		
		/* generate editor to display html help text and put in scrollpane*/
		h = new JEditorPane();
		h.setContentType("text/html");
		h.addHyperlinkListener(this);
		String context = "<html><body><table width='"+WIDTH_HTML+"'><tr><td><p><font size=+1>"+toolTipText+"</font></p>"+helpText+"</td></tr></table></body></html>";
		h.setText(context);
		h.setEditable(true);
		h.addHyperlinkListener(this);
		helpContent.add(h);
		help = new JScrollPane(helpContent);
		help.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		help.setPreferredSize(new Dimension(WIDTH_SC,HEIGHT_SC));
		
		popup=new JFrame();
		popup.setUndecorated(true);
		
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	/**
	 * If 'd' was typed swap toolTip-popUp with help-popup and
	 * set helpActive to true to prevent that the mouseExited
	 * event causes the popup to go away.
	 */
	@Override
	public void keyTyped(KeyEvent arg0) {
		if(arg0.getKeyChar()=='d'){
			helpActive=true;
			try{
				popup.remove(toolTip);
			}
			catch(Exception e){}
			popup.setLocation(x,y);
			popup.add(help);
			popup.pack();
			popup.setVisible(true);
			
			/* request Focus in editor so that it can be hidden when focus is lost */
			h.requestFocus();
			h.addFocusListener(this);
			
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

		/* get Position of the mouse */
		Point pos = new Point(arg0.getX(), arg0.getY());
		SwingUtilities.convertPointToScreen(pos, owner);
		x=((int)pos.getX()+10);
		y=((int)pos.getY()+10);

		/* ensure that it does not go off the screen
		 * if the coordinate of the position exceeds the window size of the
		 * default screen it always opens on the left.
		 * TODO fix the two screen to be not too strict.
		 */
		Dimension screenSize = Toolkit.getDefaultToolkit ().getScreenSize();
//		boolean exceed = false;
		if((x+this.WIDTH_SC)>screenSize.getWidth()){
			x=(x-10-this.WIDTH_SC);
		}
		if((y+this.HEIGHT_SC)>screenSize.getHeight()){
			y=(y-10-this.HEIGHT_SC);
		}
	
		/* wait for mouse to say ontop of the component for a while to ensure that 
		 * user really wanted to see to tooltip. Generates the sleeping thread
		 */
		t = new Thread(new Runnable(){

			@Override
			public void run() {
				boolean cont =true;	// indicating if thread was interrupted
				
				/* sleep */
				try {
					Thread.sleep(1300);
				} catch (InterruptedException e1) {
					cont=false;
				}
				
				/* if mouse stayed ontop of the component (mouse event is not consumed) 
				 * create toolTip popup. 
				 */
				if(cont && !helpActive){
					
					try{
						popup.remove(help);
					}
					catch(Exception e){}
					popup.setLocation(x,y);
					popup.add(toolTip);
					popup.pack();
					popup.setVisible(true);
				}
			}
		});
		t.start();
		
		/* keylistener can not be in thread */
		popup.addKeyListener(this);	
		
	}

	/**
	 * Hide popUp if not 'd' was pressed before and the help popUp is now displayed
	 */
	@Override
	public void mouseExited(MouseEvent arg0) {
		
		/* interrupt sleep because mouse was moved away from the object */
		arg0.consume();
		t.interrupt();
		
		if(!helpActive){
			popup.setVisible(false);
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		t.interrupt();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	@Override
	public void focusGained(FocusEvent arg0) {
	}

	/**
	 * If the focus is lost (user clicked somwhere else) hide popUp.
	 */
	@Override
	public void focusLost(FocusEvent arg0) {
		helpActive=false;
		popup.setVisible(false);
	}

	/**
	 * Do something with the Hyperlink
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent event) {
		//TODO add open link in default browser
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				h.setPage(event.getURL());
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
	}

	
	
	
	public static void main(String[] args){

		JFrame demo = new JFrame();
		demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		JPanel pnl = new JPanel();
		
		//normal Button
		JButton el1 = new JButton("I'm normal");
		el1.setToolTipText("This button has a normal toolTip");
		pnl.add(el1);
		
		//Button with expandable toolTip
		JButton el2 = new JButton("I'm special");
		String t2="Expandable toolTips for buttons";
		String h2="<p>The <b>expandable toolTip</b> can be used for all <font color='red'>JComponents</font>.</p>The <b>expandable toolTip</b> is here shown for <i>JButtons</i><p><font size=-2>More info at <a href='http://www.allPower.de'>www.allPower.de</a></font></p>";
		new ExpandableToolTip(t2,h2,el2);
		pnl.add(el2);
		
		//JComboBox with expandable toolTip
		String[] cont = {"This Box is special but I'm not","I'm not special","neither am I"};
		JComboBox<String> el3 = new JComboBox<String>(cont);
		String t3="Expandable toolTips for JComboBoxes";
		String h3="<p>The <b>expandable toolTip</b> can be used for all <font color='red'>JComponents</font>.</p>The <b>expandable toolTip</b> is here shown for <i>JComboBox</i><p><font size=-2>More info at <a href='http://www.allPower.de'>www.allPower.de</a></font></p>";
		new ExpandableToolTip(t3,h3,el3);
		pnl.add(el3);
	
		demo.add(pnl);
	    demo.pack();
	    demo.setVisible(true);
		
	}


}
