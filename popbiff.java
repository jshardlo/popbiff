//----------------------------------------------------------------------------
// A biff program for POP3 mail drops
//
// Checks the status of multiple mail drops at regular intervals and gives
// an on-screen indication of which mail drops should be accessed
//
// John Shardlow
// 25/3/1998
//
// with enormous additions by Kev Hall
//----------------------------------------------------------------------------

import java.awt.event.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.swing.*;

public class popbiff
{
    public static final String ACK_STRING = "Acknowledge";
    public static final String PASSWORD = "Password";
    public static final String SUSPEND = "Suspend";
    public static final String RESTART = "Restart";

    public static Frame frame = null;

	public static String laf1 = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
	public static String laf2 = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
	public static String laf3 = "javax.swing.plaf.metal.MetalLookAndFeel";

	public static void main(String[] args)
	{
		Vector v;
		StreamTokenizer st;
		FileReader fr;
		int i,howmany;
		POPAccount pa;
		int stat;
		String acname, achost, aclogin, acpass;
		String conffile;
		int acport, actime;

		if (args.length > 1)
		{
			System.out.println("Usage: java popbiff configfile");
			System.exit(0);
		}
		if (args.length == 0)
		{
			conffile = "popbiff.cfg";
		} else {
			conffile = args[0];
		}
		

		try
		{
			UIManager.setLookAndFeel(laf3);
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		v = new Vector();

		howmany=0;

		try
		{
			fr = new FileReader(conffile);
                        
			st = new StreamTokenizer(fr);
			st.whitespaceChars(9,9);
			st.wordChars(33,127);
			st.commentChar('#');
			st.eolIsSignificant(true);
            String tokens[] = {"","","","","","","",""};
            int count = 0;
            while (st.nextToken() != StreamTokenizer.TT_EOF)
            {
                //
                // If End of the line is detected then process the
                // read tokens otherwise get the next token.
                //
                if (st.ttype == StreamTokenizer.TT_EOL) {
                    if(! tokens[4].equals("")) {
					    acname = tokens[0];
    					achost = tokens[1];
    					acport = toInt(tokens[2]);
    					actime = toInt(tokens[3]);
    					aclogin = tokens[4];
    					acpass = tokens[5];
        				pa = new POPAccount(acname,achost,acport,
        				                  actime,aclogin,acpass);
    	    			v.addElement(pa);
    		    		howmany++;
    		    	}
		    		tokens[0] = "";
		    		tokens[1] = "";
		    		tokens[2] = "";
		    		tokens[3] = "60";
		    		tokens[4] = "";
		    		tokens[5] = "PASSWORD";
		    		count = 0;
                } else  {
                    if (st.ttype == StreamTokenizer.TT_NUMBER) {
                        tokens[count++] =  String.valueOf(st.nval);
                    } else {
                        tokens[count++] = st.sval;
                    }
                }
  			}
			fr.close();
			popbiffWindow w = new popbiffWindow(howmany);

			Enumeration list0 = v.elements();

			while(list0.hasMoreElements())
			{
				POPAccount thisac = (POPAccount)list0.nextElement();
				w.addAccount(thisac);
				thisac.setWindow(w);
				thisac.start();
			}

			w.exitbutton();


		}
		catch (FileNotFoundException e1)
		{
			System.out.println("Config file "+conffile+" not found");
			System.exit(0);
		}
		catch(IOException e2)
		{
			System.out.println(e2);
			System.exit(0);
		}
	}

    static int toInt (String s) {
        int result;
        s = s.substring(0, s.indexOf("."));
        try {
            result = Integer.valueOf(s).intValue();
        } catch (Exception e ) {
            result = 0;
        }
        return result;
    }

}

class POPAccount extends Thread implements MouseListener, ActionListener
{
    popbiffWindow window = null;
	public String name, host, login, pass;
	int port;
	int timer = 60;
    int ackedMail = 0;
    boolean acked = false;
    int numMails = 0;
    boolean suspend = false;
    Label label = null;
    PasswordDialog pwd = null;
    String errorText = "";
    PopBiffTipText tipText = null;

	public POPAccount(String name,
				String host, int port, int time,
				String login, String pass)
	{
		this.name = name;
		this.host = host;
		this.port = port;
		this.timer = time * 1000;
		this.login = login;
		this.pass = pass;


	}

    public void setWindow(popbiffWindow w) {
        window = w;
    }

    public void run() {
        int count;

        while(true) {
            count = 0;
            if(!isSuspended())
    			count = checkForMail();

			window.updateAccount(this, count);

    		// Wait for a minute

        	try
		    {
			    Thread.sleep(timer);
		    }
		    catch(InterruptedException e3)
		    {
			    // Don't care
		    }

		}

    }

    public void setErrorText(String text) {
        errorText = text;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setSuspend() {
        suspend = true;
	errorText = "";
    }

    public void restart() {
        suspend = false;
        errorText= "";
    }

    public boolean isSuspended() {
        return suspend;
    }

    public void ackMail(int count) {
        ackedMail = count;
        acked = true;
    }

    public void resetMail() {
        ackedMail = 0;
        acked = false;
        errorText="";
    }

    public int numAcked () {
        return ackedMail;
    }

    public boolean acked() {
        return acked;
    }

	public int checkForMail()
	{
	    int n=0;
	    InputStreamReader in;
	    OutputStreamWriter out;
    	char dummy1[];
    	char dummy2[];
    	char dummy3[];
	char dummy4[];
        char buffer[];
        String output;

    	dummy1=new char[255];
    	dummy2=new char[255];
    	dummy3=new char[255];
	dummy4=new char[255];
        buffer=new char[255];

	    try
	    {
	        Socket s = new Socket(host, port);
	        in = new InputStreamReader(s.getInputStream());
	        out = new OutputStreamWriter(s.getOutputStream());

	        in.read(dummy1,0,255);
//		System.out.println(dummy1);



            output = "user "+login+"\r\n";

            out.write(output,0,output.length());
    		out.flush();

            in.read(dummy2,0,255);
//		System.out.println(dummy2);



    		output = "pass "+pass+"\r\n";
            out.write(output,0,output.length());
	    	out.flush();

		    in.read(dummy3,0,255);
//		System.out.println(dummy3);


    		output = "stat\r\n";
            out.write(output,0,output.length());
	    	out.flush();

            in.read(buffer,0,255);
//		System.out.println(buffer);

// Oops. Better send it a quit as some servers are too dumb to
// realise that if the socket closes then we've gone away

		output = "quit\r\n";
            out.write(output,0,output.length());
	    	out.flush();

		    in.read(dummy4,0,255);

//		System.out.println(dummy4);

	        s.close();

		// Parse the buffer for the number of new mails

    		if ( buffer[0] == '+' )
    		{
    			Integer inewmail;
    			String newmail,s2;
    			int i=4;

    			// Got an OK response

    			newmail = new String(buffer);
                	setErrorText("");

    			s2 = newmail.substring(4);

    			inewmail = new Integer(s2.substring(0,s2.indexOf(32)));
    			n = inewmail.intValue();
    		}
    		else
    		{
    		    n = -1;

                if( dummy1[0] == '-' ) {
        		    setErrorText((new String(dummy1)).trim());
                } else if( dummy2[0] == '-' ) {
                    setErrorText((new String(dummy2)).trim());
                } else if( dummy3[0] == '-' ) {
                    setErrorText((new String(dummy3)).trim());
                } else {
                         setErrorText((new String(buffer)).trim());
        		}
    		}
        }
        catch (Exception e1)
        {
    		n = -1;
   		    setErrorText("Unable to connect to server");
        }

        numMails = n;
        return(n);
	}

// mouse listener stuff

    public void mouseClicked (MouseEvent me) {
        PopupMenu pupmen = new PopupMenu("Options");
        MenuItem item1 = new MenuItem(popbiff.ACK_STRING);
        item1.addActionListener(this);
        pupmen.add(item1);

        pupmen.addSeparator();
        MenuItem item2 = new MenuItem(popbiff.SUSPEND);
        item2.addActionListener(this);
        pupmen.add(item2);

        MenuItem item3 = new MenuItem(popbiff.RESTART);
        item3.addActionListener(this);
        pupmen.add(item3);

        pupmen.addSeparator();
        MenuItem item4 = new MenuItem(popbiff.PASSWORD);
        item4.addActionListener(this);
        pupmen.add(item4);

        popbiffWindow.instance().getFrame().add(pupmen);
        pupmen.show(me.getComponent(),10,10);

    }

    public void mousePressed (MouseEvent me) {
    }

    public void mouseReleased (MouseEvent me) {
    }

    public void mouseEntered (MouseEvent me) {
	if (!getErrorText().equals("")) {
	    tipText = new PopBiffTipText(getErrorText(),me.getX(), me.getY());
	}
    }

    public void mouseExited (MouseEvent me) {
        if (tipText != null) {
	    tipText.dispose();
	    tipText = null;
	}
    }

// action listener stuff

  	public void actionPerformed(ActionEvent event)	{

        String action = event.getActionCommand();
        if(action != null) {
            if (action.equals(popbiff.ACK_STRING))
            {
                ackMail(numMails);
                popbiffWindow.instance().updateAccount(this, numMails);
            }
            if (action.equals(popbiff.SUSPEND))
            {
                setSuspend();
                popbiffWindow.instance().updateAccount(this, numMails);
            }
            if (action.equals(popbiff.RESTART))
            {
                restart();
                popbiffWindow.instance().updateAccount(this, numMails);
            }
            if (action.equals(popbiff.PASSWORD))
            {
                pwd = new PasswordDialog(this);
            }
        }
        if(event.getSource() == pwd) {
            if(action.equals("OK")) {
                this.pass = pwd.getPassword();
                pwd.dispose();
                this.numMails = 0;
                popbiffWindow.instance().updateAccount(this, numMails);
            }
        }
    }

}

class PopBiffTipText  extends JWindow
{
	public PopBiffTipText(String text, int x, int y)
	{
		super(popbiffWindow.instance().getFrame());
		Frame fr = popbiffWindow.instance().getFrame();
		JPanel p = new JPanel((new GridLayout(1,1)));
		JLabel t = new JLabel(text);
		t.setHorizontalAlignment(JLabel.LEFT);
		getContentPane().add(p);
		int len = text.length() * 7;
		setSize(len ,20);
		p.setSize(len, 20);
		t.setSize(len, 20);
		p.add(t);
		int posX = fr.getX() + x;
		if ((posX + len) > 1020) posX-=len;
		int posY = fr.getY() + y;
		setLocation(posX, posY);
		setVisible(true);
	}
}


class popbiffWindow implements ActionListener
{
	JFrame f;
	JPanel top;

    public static popbiffWindow _instance = null;

    public static popbiffWindow instance() {
        if(_instance == null)
            _instance = new popbiffWindow(1);
        return _instance;
    }

    public JFrame getFrame() {
        return f;
    }

	public popbiffWindow(int rows)
	{
		int y = rows * 28 + 40;

		f = new JFrame("popbiff");
		popbiff.frame = f;
		top = new JPanel(new GridLayout(rows+1,2));
		f.setSize(168,y);
		f.setLocation(1024,768-y);
		top.setSize(148,y);
		f.getContentPane().add(top);
		f.addWindowListener(new WindowAdapter(){

		public void windowClosing(WindowEvent winevent)
		{
			System.exit(0);
		}

		});
		_instance = this;
	}


	public void addAccount(POPAccount ac)
	{
	    String name = ac.name;
		JLabel nam,num;

		nam = new JLabel(name);
		nam.setOpaque(true);
		nam.addMouseListener(ac);
		nam.setHorizontalAlignment(JLabel.LEFT);
		nam.setSize(100,20);
		nam.setBackground(Color.black);
		nam.setForeground(Color.white);
                ac.setErrorText("Initialising connection...Please wait");
		num = new JLabel("?");
		num.setOpaque(true);
		num.addMouseListener(ac);
		num.setHorizontalAlignment(JLabel.RIGHT);
		num.setSize(42,20);
		num.setBackground(Color.black);
		num.setForeground(Color.white);

		top.add(nam);
		top.add(num);
	}

	public void updateAccount(POPAccount ac, int count)
	{
	    String name = ac.name;
		Integer newmails=new Integer(count);
		JLabel l,l2;
		int idx=0;
		Component[] comparray = top.getComponents();

		while(idx < (top.getComponentCount() - 2))
		{
			l = (JLabel)comparray[idx];

			if((l.getText()).compareTo(name) == 0)
			{
				l2 = (JLabel)comparray[idx+1];

                if(! ac.isSuspended()) { // NOT suspended
    				if(count >0)
    				{
                        if(ac.acked()) {
                            if(ac.numAcked() != count) {
            					l2.setText(newmails.toString());
            					l.setBackground(Color.red);
        					    l.setForeground(Color.white);
        	    				l2.setBackground(Color.red);
        					    l2.setForeground(Color.white);
        	    			} else {
            					l2.setText(newmails.toString());
            					l.setBackground(Color.cyan);
            					l.setForeground(Color.black);
        	    				l2.setBackground(Color.cyan);
            					l2.setForeground(Color.black);
        	    			}
                        } else {
        					l2.setText(newmails.toString());
        					l.setBackground(Color.red);
        					l.setForeground(Color.white);
        					l2.setBackground(Color.red);
    					    l2.setForeground(Color.white);
                        }
    				}
    				else if (count < 0)
    				{
                        if((ac.pass).equals("PASSWORD"))
        					l2.setText("NOPASS");
        		        else
        					l2.setText("Error");
    					l.setBackground(Color.yellow);
    					l.setForeground(Color.black);
    					l2.setBackground(Color.yellow);
    					l2.setForeground(Color.black);
    				}
    				else
    				{
    					l2.setText(newmails.toString());
    					l.setForeground(Color.white);
    					l.setBackground(Color.black);
    					l2.setForeground(Color.white);
    					l2.setBackground(Color.black);
    					ac.resetMail();
    				}
                } else { // IS suspended
  					l2.setText("n/a");
   					l.setForeground(Color.white);
   					l.setBackground(Color.black);
   					l2.setForeground(Color.white);
   					l2.setBackground(Color.black);
                }
		String err = ac.getErrorText();
                l2.validate();
                l.validate();
                f.repaint();
			}
			idx += 2;
		}
	}

	public void exitbutton()
	{
		JButton b1,b2;
		b1 = new JButton("Exit");
		b2 = new JButton("About");
		top.add(b1);
		b1.setSize(70,20);
		b2.setSize(70,20);

		top.add(b2);
		b1.addActionListener(this);
		b2.addActionListener(this);
		f.setVisible(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		String action;

		action = (e.paramString()).substring(21);

		if(action.startsWith("Exit"))
		{
			System.exit(0);
		}
		else if(action.startsWith("About"))
		{
			aboutWindow a = new aboutWindow();
		}
		else
		{
                    System.out.println("Unknown action "+action);
			System.out.println("Unknown action "+e);
		}
	}
}

class aboutWindow implements ActionListener
{
	JFrame f;
	static boolean vis=false;

	public aboutWindow()
	{
		if(vis) return;
		f = new JFrame("About popbiff");
		JPanel p = new JPanel();
		TextArea t = new TextArea(
"popbiff 1.13\n\nWritten by John Shardlow and Kev Hall\n\nLast Updated 11 February 2019\n\nA program to check multiple POP mailboxes \nand report the number of unread messages"
			,9,40);
		f.setSize(360,275);
		p.setSize(360,275);
		f.getContentPane().add(p);
		p.add(t);
		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		p.add(ok);
		f.setVisible(true);
		vis=true;
	}

	public void actionPerformed(ActionEvent e)
	{
		f.dispose();
		vis=false;
	}
}

class PasswordDialog extends JFrame implements ActionListener
{

    //private member data
    private TextField _password;
    private JPanel _mainPanel;
    private JButton _buttonOk;
    private JButton _buttonCancel;
    private JPanel _buttonBar;
    private ActionListener _actionListener;
    private JLabel _message = new JLabel();

    //constructor
    public PasswordDialog(POPAccount ac)
    {
        super("Password");
        getContentPane().setLayout(new BorderLayout());

        _actionListener = ac;
	    addInputBoxes();
	    addButtonBar();

        setSize(200,100);
        setVisible(true);
    }


    /**
    * Get the password typed by the user
    */
    public String getPassword()
    {
        return _password.getText();
    }

    private void addInputBoxes()
    {
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        _mainPanel = new JPanel();
        _mainPanel.setLayout(gridBag);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10,10,10,10);

        c.gridwidth = 1;

        c.gridwidth = 1;
        JLabel label2 = new JLabel(_strPassword + ":", Label.LEFT);
        gridBag.setConstraints(label2, c);
        _mainPanel.add(label2);

        _password = new TextField(10);
        _password.setEchoChar('*');
        _password.addActionListener(this);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(_password, c);
        _mainPanel.add(_password);

        getContentPane().add("Center", _mainPanel);
    }


    private void addButtonBar()
    {
        JPanel panel = new JPanel();

        GridLayout layout = new GridLayout(1,2);
        layout.setHgap(40);
        panel.setLayout(layout);

        _buttonOk = new JButton(_strOk);
        _buttonOk.addActionListener(this);
        _buttonOk.setSize(50, 28);
        panel.add(_buttonOk);

        _buttonCancel = new JButton(_strCancel);
        _buttonCancel.addActionListener(this);
        _buttonCancel.setSize(50, 28);
        panel.add(_buttonCancel);
        getContentPane().add("South", panel);
    }

    private void okButtonPressed()
    {

        if(_actionListener != null)
        {
            _actionListener.actionPerformed(new ActionEvent(this, 0, "OK"));
        }
    }

    private void cancelButtonPressed()
    {
        setVisible(false);

        if(_actionListener != null)
        {
            _actionListener.actionPerformed(new ActionEvent(this, 0, "CANCEL"));
        }
    }

    /**** ActionListener Interface ****/
    public void actionPerformed(ActionEvent event)
    {
        if(event.getSource() == _password)
        {
            okButtonPressed();
        }
        else if(event.getSource() == _buttonOk)
        {
            okButtonPressed();
        }
        else if(event.getSource() == _buttonCancel)
        {
            cancelButtonPressed();
        }
    }
    /**** End of ActionListener Interface ****/

    private static final String _strPassword = "Password";
    private static final String _strOk = "Ok";
    private static final String _strCancel = "Cancel";

}

